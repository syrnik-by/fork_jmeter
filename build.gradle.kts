import com.github.vlsi.gradle.properties.dsl.props

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
buildscript {
    dependencies {
        classpath("org.jetbrains.gradle.plugin.idea-ext:org.jetbrains.gradle.plugin.idea-ext.gradle.plugin:0.5")
    }
}
plugins {
    java
    jacoco
    checkstyle
    id("org.jetbrains.gradle.plugin.idea-ext") apply false
    id("org.nosphere.apache.rat")
    id("com.github.vlsi.gradle-extensions")
    publishing
}
val appversion: String by project

version = appversion

fun Project.boolProp(name: String) =
    findProperty(name)
        ?.let { it as? String }
        ?.equals("false", ignoreCase = true)?.not()

println("Building JMeter $version")

val lastEditYear by extra { "2026" }

val skipDist by extra {
    boolProp("skipDist") ?: false
}

allprojects {
    group = "org.apache.jmeter"
    version = rootProject.version

    repositories {
        mavenCentral()
        maven {
            url = uri("https://repo.cuba-platform.com/content/groups/work/")
        }
    }

    tasks.register("printAllDependencies", DependencyReportTask::class) {}

    plugins.withType<JavaPlugin> {
        java {
            sourceCompatibility = JavaVersion.VERSION_1_8
        }

        tasks {
            withType<JavaCompile>().configureEach {
                options.encoding = "UTF-8"
            }

            withType<Jar>().configureEach {
                manifest {
                    attributes["Bundle-License"] = "Apache-2.0"
                    attributes["Specification-Title"] = "НТ Мастер"
                    attributes["Specification-Vendor"] = "Apache Software Foundation"
                    attributes["Implementation-Vendor"] = "Apache Software Foundation"
                    attributes["Implementation-Vendor-Id"] = "org.apache"
                    attributes["Implementation-Version"] = rootProject.version
                }
            }
        }
    }
}

subprojects {
    val bomProject = ":src:bom"
    if (path != bomProject) {
        // Use plugins.withType<JavaPlugin> without afterEvaluate so the BOM platform
        // is added as soon as the java (or java-library) plugin is applied.
        // Use "implementation" instead of "api" because "api" only exists in modules
        // with the java-library plugin; "implementation" is available in all Java modules.
        plugins.withType<JavaPlugin> {
            dependencies {
                add("implementation", platform(project(bomProject)))
            }
        }
    }
}
