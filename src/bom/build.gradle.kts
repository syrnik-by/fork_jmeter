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

plugins {
    `java-platform`
}

val catalog = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")

/**
 * Resolves a library alias from the version catalog and returns "group:artifact:version".
 * Throws a clear error if the alias is not found.
 */
fun lib(alias: String): String {
    val provider = catalog.findLibrary(alias).orElseThrow {
        GradleException("Library alias '$alias' not found in libs version catalog")
    }
    val dep = provider.get()
    return "${dep.module.group}:${dep.module.name}:${dep.versionConstraint.requiredVersion}"
}

// Note: Gradle allows to declare dependency on "bom" as "api",
// and it makes the constraints to be transitively visible
// However Maven can't express that, so the approach is to use Gradle resolution
// and generate pom files with resolved versions
// See https://github.com/gradle/gradle/issues/9866

fun DependencyConstraintHandlerScope.apiv(alias: String) =
    "api"(lib(alias))

fun DependencyConstraintHandlerScope.runtimev(alias: String) =
    "runtime"(lib(alias))

javaPlatform {
    allowDependencies()
}

dependencies {
    api(platform(lib("groovy-bom")))

    // Parenthesis are needed here: https://github.com/gradle/gradle/issues/9248
    (constraints) {
        // api means "the dependency is for both compilation and runtime"
        // runtime means "the dependency is only for runtime, not for compilation"
        // In other words, marking dependency as "runtime" would avoid accidental
        // dependency on it during compilation
        runtimev("tika-parsers")
        runtimev("asm")

        // activemq-all should not be used as it provides secondary slf4j binding
        runtimev("activemq-broker")
        runtimev("activemq-client")
        runtimev("activemq-spring")
        runtimev("spring-context")
        runtimev("spring-beans")

        apiv("bsf")
        apiv("cglib-nodep")
        apiv("jackson-annotations")
        apiv("jackson-core")
        apiv("jackson-databind")
        apiv("rsyntaxtextarea")
        apiv("svgSalamander")
        apiv("caffeine")
        apiv("wiremock-jre8")
        apiv("darklaf-core")
        apiv("darklaf-theme")
        apiv("darklaf-property-loader")
        apiv("darklaf-extensions")
        apiv("ph-commons")
        apiv("ph-css")
        apiv("json-path")
        apiv("miglayout-core")
        apiv("miglayout-swing")
        apiv("javax-activation-sun")
        apiv("xstream")
        apiv("commons-codec")
        apiv("commons-collections")
        apiv("commons-io")
        apiv("commons-lang")
        apiv("commons-net")
        apiv("dnsjava")
        apiv("jmespath-core")
        apiv("jmespath-jackson")
        apiv("javax-activation-api")
        apiv("mail")
        apiv("jcharts")
        apiv("junit4")
        apiv("junit-jupiter-api")
        apiv("junit-jupiter-params")
        runtimev("junit-jupiter-engine")
        runtimev("junit-vintage-engine")
        apiv("accessors-smart")
        apiv("json-smart")
        apiv("jtidy")
        apiv("saxon-he")
        apiv("equalsverifier")
        apiv("bsh")
        apiv("commons-collections4")
        apiv("commons-dbcp2")
        apiv("commons-jexl3")
        apiv("commons-jexl")
        apiv("commons-lang3")
        apiv("commons-math3")
        apiv("commons-pool2")
        apiv("commons-text")
        apiv("ftplet-api")
        apiv("ftpserver-core")
        apiv("geronimo-jms")
        apiv("httpasyncclient")
        apiv("httpclient")
        apiv("httpcore-nio")
        apiv("httpcore")
        apiv("httpmime")
        apiv("log4j-1-2-api")
        apiv("log4j-api")
        apiv("log4j-core")
        apiv("log4j-slf4j-impl")
        apiv("mina-core")
        apiv("tika-core")
        apiv("velocity")
        apiv("xmlgraphics-commons")
        apiv("apiguardian-api")
        apiv("bcmail")
        apiv("bcpkix")
        apiv("bcprov")
        apiv("dec")
        apiv("hamcrest-date")
        apiv("freemarker")
        apiv("hamcrest")
        apiv("hamcrest-core")
        apiv("hamcrest-library")
        apiv("hsqldb")
        apiv("jdom")
        apiv("jodd-core")
        apiv("jodd-lagarto")
        apiv("jodd-log")
        apiv("jodd-props")
        apiv("jsoup")
        apiv("mongo-java-driver")
        apiv("rhino")
        apiv("neo4j-java-driver")
        apiv("objenesis")
        apiv("jcl-over-slf4j")
        apiv("slf4j-api")
        apiv("spock-core")
        apiv("oro")
        apiv("xalan-serializer")
        apiv("xalan")
        apiv("xercesImpl")
        apiv("xml-apis")
        apiv("xmlpull")
        apiv("xpp3-min")
    }
}
