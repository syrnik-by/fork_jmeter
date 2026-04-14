/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

val skipMavenPublication = setOf(
    ":src:bshclient",
    ":src:dist",
    ":src:dist-check",
    ":src:examples",
    ":src:generator",
    ":src:licenses",
    ":src:protocol",
    ":src:release",
    ":src:testkit",
    ":src:testkit-wiremock"
)

fun Project.boolProp(name: String) =
    findProperty(name)
        ?.let { it as? String }
        ?.equals("false", ignoreCase = true)?.not()

val skipJavadoc by extra {
    boolProp("skipJavadoc") ?: false
}

subprojects {
    if (path == ":src:bom") {
        return@subprojects
    }

    val groovyUsed = file("src/main/groovy").isDirectory || file("src/test/groovy").isDirectory
    val testsPresent = file("src/test").isDirectory

    apply<JavaLibraryPlugin>()
    if (groovyUsed) {
        apply<GroovyPlugin>()
    }
    if (project.path !in skipMavenPublication) {
        apply<MavenPublishPlugin>()
    }
    apply<JacocoPlugin>()

    dependencies {
        // IMPORTANT: use add("api", ...) — NOT 'val api by configurations; api(...)'
        // The latter captures Configuration as a local variable and calling it as a function
        // does NOT add the dependency (it invokes Configuration.invoke which is a no-op here).
        // Only add() / the typed DSL accessors actually register the platform constraint.
        add("api", platform(project(":src:bom")))

        if (!testsPresent) {
            return@dependencies
        }
        add("testImplementation", "org.junit.jupiter:junit-jupiter-api")
        add("testImplementation", "org.junit.jupiter:junit-jupiter-params")
        add("testImplementation", "org.hamcrest:hamcrest")
        add("testRuntimeOnly", "org.junit.jupiter:junit-jupiter-engine")
        add("testRuntimeOnly", "org.junit.vintage:junit-vintage-engine")
        add("testImplementation", "junit:junit")
        add("testImplementation", testFixtures(project(":src:testkit")))
        if (groovyUsed) {
            add("testImplementation", "org.spockframework:spock-core")
        }
        add("testRuntimeOnly", "cglib:cglib-nodep") {
            because("""
                org.spockframework.mock.CannotCreateMockException: Cannot create mock for
                 class org.apache.jmeter.report.processor.AbstractSummaryConsumer${'$'}SummaryInfo.
                 Mocking of non-interface types requires a code generation library.
                 Please put an up-to-date version of byte-buddy or cglib-nodep on the class path.""".trimIndent())
        }
        add("testRuntimeOnly", "org.objenesis:objenesis") {
            because("""
                org.spockframework.mock.CannotCreateMockException: Cannot create mock for
                 class org.apache.jmeter.report.core.Sample. To solve this problem,
                 put Objenesis 1.2 or higher on the class path (recommended),
                 or supply constructor arguments (e.g. 'constructorArgs: [42]') that allow to construct
                 an object of the mocked type.""".trimIndent())
        }
    }

    val sourcesJar by tasks.registering(Jar::class) {
        val sourceSets: SourceSetContainer by project
        from(sourceSets["main"].allJava)
        archiveClassifier.set("sources")
    }

    val javadocJar by tasks.registering(Jar::class) {
        from(tasks.named(JavaPlugin.JAVADOC_TASK_NAME))
        archiveClassifier.set("javadoc")
    }

    val testClasses by configurations.creating {
        extendsFrom(configurations["testRuntimeClasspath"])
    }

    if (testsPresent) {
        val testJar by tasks.registering(Jar::class) {
            val sourceSets: SourceSetContainer by project
            archiveClassifier.set("test")
            from(sourceSets["test"].output)
        }

        (artifacts) {
            testClasses(testJar)
        }
    }

    val archivesBaseName = when (name) {
        "jorphan", "bshclient" -> name
        "launcher" -> "ApacheJMeter"
        else -> "ApacheJMeter_$name"
    }
    setProperty("archivesBaseName", archivesBaseName)

    if (project.path in skipMavenPublication) {
        return@subprojects
    }

    configure<PublishingExtension> {
        publications {
            create<MavenPublication>(project.name) {
                artifactId = archivesBaseName
                version = rootProject.version.toString()
                from(components["java"])

                if (!skipJavadoc) {
                    artifact(sourcesJar.get())
                    artifact(javadocJar.get())
                }

                versionMapping {
                    usage(Usage.JAVA_RUNTIME) {
                        fromResolutionResult()
                    }
                    usage(Usage.JAVA_API) {
                        fromResolutionOf("runtimeClasspath")
                    }
                }

                pom {
                    withXml {
                        val sb = asString()
                        var s = sb.toString()
                        s = s.replace("<scope>compile</scope>", "")
                        s = s.replace(
                            Regex(
                                "<dependencyManagement>.*?</dependencyManagement>",
                                RegexOption.DOT_MATCHES_ALL
                            ),
                            ""
                        )
                        sb.setLength(0)
                        sb.append(s)
                        asNode()
                    }
                    name.set("НТ Мастер ${project.name.capitalize()}")
                    description.set(project.description)
                    inceptionYear.set("1998")
                    url.set("http://jmeter.apache.org/")
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                            comments.set("A business-friendly OSS license")
                        }
                    }
                }
            }
        }
    }
}
