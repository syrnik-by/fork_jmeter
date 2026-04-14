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
////import com.github.spotbugs.snom.SpotBugsTask
////import com.github.vlsi.gradle.crlf.CrLfSpec
////import com.github.vlsi.gradle.crlf.LineEndings
////import com.github.vlsi.gradle.crlf.filter
////import com.github.vlsi.gradle.git.FindGitAttributes
////import com.github.vlsi.gradle.git.dsl.gitignore
////import com.github.vlsi.gradle.properties.dsl.lastEditYear
////import com.github.vlsi.gradle.properties.dsl.props
////import com.github.vlsi.gradle.release.RepositoryType
////import net.ltgt.gradle.errorprone.errorprone
////import org.ajoberstar.grgit.Grgit
////import org.gradle.api.tasks.testing.logging.TestExceptionFormat
////import org.sonarqube.gradle.SonarQubeProperties
buildscript {
    dependencies {
        classpath("org.jetbrains.gradle.plugin.idea-ext:org.jetbrains.gradle.plugin.idea-ext.gradle.plugin:0.5")

     //   classpath("org.ajoberstar.grgit:grgit-core:4.1.0")
        //     classpath("com.github.vlsi.crlf:com.github.vlsi.crlf.gradle.plugin:2.0.0")
    }
}
plugins {
    java
    jacoco
    checkstyle
    id("org.jetbrains.gradle.plugin.idea-ext") apply false
    id("org.nosphere.apache.rat")
   // id("com.github.autostyle")
   // id("com.github.spotbugs")
  //  id("net.ltgt.errorprone") apply false
   // id("org.sonarqube")
    id("com.github.vlsi.gradle-extensions")
    // id("com.github.vlsi.ide")
    //  id("com.github.vlsi.stage-vote-release")
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
        // RAT and Autostyle dependencies
        mavenCentral()
        maven {
            url = uri("https://repo.cuba-platform.com/content/groups/work/")
        }
    }

    tasks.register("printAllDependencies", DependencyReportTask::class) {}

    plugins.withType<JavaPlugin> {
        // This block is executed right after `java` plugin is added to a project
        java {
            sourceCompatibility = JavaVersion.VERSION_1_8
        }

        tasks {
            // Fix: set UTF-8 encoding for all Java compilation tasks so that
            // Cyrillic literals in source files are not misinterpreted as windows-1252
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
