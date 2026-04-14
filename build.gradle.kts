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

// Wire BOM to every Java subproject that is NOT the BOM itself.
//
// IMPORTANT: use "api" (not "implementation") for the platform dependency.
// "implementation" platform constraints are NOT exported to consumers:
//   :src:dist depends on :src:core, and when resolving :src:dist's runtimeClasspath
//   Gradle sees :src:core's transitive deps (e.g. bsf:bsf) — but the version
//   constraints from :src:core's "implementation" platform are invisible at that
//   point, resulting in "Could not find bsf:bsf:." (empty version).
// "api" platform exports the constraints transitively so any consumer of :src:core
//   also benefits from the BOM version pins.
subprojects {
    val bomProject = ":src:bom"
    if (path != bomProject) {
        plugins.withType<JavaPlugin> {
            dependencies {
                add("api", platform(project(bomProject)))
            }
        }
    }
}
