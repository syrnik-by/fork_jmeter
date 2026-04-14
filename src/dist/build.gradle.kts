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

// ─────────────────────────────────────────────────────────────────────────────
// Этот файл — оркестратор. Вся тяжёлая логика вынесена в:
//   gradle/nexus-raw.gradle.kts   — работа с Nexus RAW (upload/download бандла)
//   gradle/local-dist.gradle.kts  — сборка локального дистрибутива (createDist)
// ─────────────────────────────────────────────────────────────────────────────

import com.github.vlsi.gradle.crlf.CrLfSpec
import com.github.vlsi.gradle.crlf.LineEndings
import java.nio.file.Paths
import de.undercouch.gradle.tasks.download.Download
import org.gradle.api.internal.artifacts.dependencies.DefaultDependencyConstraint.strictly
import org.gradle.internal.impldep.org.junit.experimental.categories.Categories.CategoryFilter.exclude
import java.io.File

plugins {
    java
    id("de.undercouch.download") version "4.1.2"
    id("com.github.vlsi.crlf")
    `maven-publish`
}

apply(from = "gradle/nexus-raw.gradle.kts")
apply(from = "gradle/local-dist.gradle.kts")

// ── Version catalog accessor ──────────────────────────────────────────────────
// Объявлены здесь т.к. используются и в dependencies{} и в скриптах через extra.
val catalog = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")

fun lib(alias: String): MinimalExternalModuleDependency =
    catalog.findDependency(alias).orElseThrow {
        GradleException("Library alias '$alias' not found in libs version catalog")
    }.get()

fun ver(alias: String): String =
    catalog.findVersion(alias).orElseThrow {
        GradleException("Version alias '$alias' not found in libs version catalog")
    }.requiredVersion

fun gav(alias: String): String =
    lib(alias).run { "${module.group}:${module.name}:${versionConstraint.requiredVersion}" }

// ── Versions ──────────────────────────────────────────────────────────────────
val jmeterVersion = ver("jmeter")

// ── resolutionStrategy ────────────────────────────────────────────────────────
// КРИТИЧНО: покрывает весь транзитивный граф зависимостей.
// BOM platform() покрывает только прямые зависимости — этот блок закрывает пробелы.
configurations.all {
    resolutionStrategy.eachDependency {
        val g = requested.group
        val n = requested.name
        if (requested.version.isNullOrEmpty()) {
            val resolved = when {
                g == "org.codehaus.groovy" -> ver("groovy")
                g == "org.apache.logging.log4j" -> ver("log4j")
                g == "org.slf4j" -> ver("slf4j")
                g == "com.github.weisj" && n.startsWith("darklaf-extensions") -> ver("darklaf-extensions")
                g == "com.github.weisj" -> ver("darklaf")
                g == "commons-collections" -> ver("commons-collections")
                g == "org.apache.commons" && n == "commons-collections4" -> ver("commons-collections4")
                g == "commons-codec" -> ver("commons-codec")
                g == "commons-io" -> ver("commons-io")
                g == "commons-lang" -> ver("commons-lang")
                g == "org.apache.commons" && n == "commons-lang3" -> ver("commons-lang3")
                g == "org.apache.commons" && n == "commons-math3" -> ver("commons-math3")
                g == "org.apache.commons" && n == "commons-text" -> ver("commons-text")
                g == "org.apache.commons" && n == "commons-jexl" -> ver("commons-jexl")
                g == "org.apache.commons" && n == "commons-jexl3" -> ver("commons-jexl3")
                g == "org.apache.commons" && n == "commons-dbcp2" -> ver("commons-dbcp2")
                g == "org.apache.commons" && n == "commons-pool2" -> ver("commons-pool2")
                g == "bsf" && n == "bsf" -> ver("bsf")
                g == "com.fifesoft" && n == "rsyntaxtextarea" -> ver("rsyntaxtextarea")
                g == "com.formdev" && n == "svgSalamander" -> ver("svgSalamander")
                g == "xalan" -> ver("xalan")
                g == "net.sf.saxon" && n == "Saxon-HE" -> ver("saxon-he")
                g == "org.apache.xmlgraphics" && n == "xmlgraphics-commons" -> ver("xmlgraphics-commons")
                g == "org.freemarker" && n == "freemarker" -> ver("freemarker")
                g == "org.jodd" -> ver("jodd")
                g == "org.mozilla" && n == "rhino" -> ver("rhino")
                g == "xerces" && n == "xercesImpl" -> ver("xercesImpl")
                g == "xml-apis" && n == "xml-apis" -> ver("xml-apis")
                g == "org.apache.httpcomponents" && n == "httpclient" -> ver("httpclient")
                g == "org.apache.httpcomponents" && n == "httpcore" -> ver("httpcore")
                g == "org.apache.httpcomponents" && n == "httpasyncclient" -> ver("httpasyncclient")
                g == "org.apache.httpcomponents" && n == "httpcore-nio" -> ver("httpcore-nio")
                g == "org.apache.httpcomponents" && n == "httpmime" -> ver("httpmime")
                g == "com.fasterxml.jackson.core" && n == "jackson-databind" -> ver("jackson-databind")
                g == "com.fasterxml.jackson.core" -> ver("jackson")
                g == "com.github.ben-manes.caffeine" && n == "caffeine" -> ver("caffeine")
                g == "com.miglayout" -> ver("miglayout")
                g == "org.apache.tika" -> ver("tika")
                g == "net.sf.jtidy" && n == "jtidy" -> ver("jtidy")
                g == "com.thoughtworks.xstream" && n == "xstream" -> ver("xstream")
                g == "org.jsoup" && n == "jsoup" -> ver("jsoup")
                g == "oro" && n == "oro" -> ver("oro")
                g == "org.apache-extras.beanshell" && n == "bsh" -> ver("bsh")
                g == "cglib" && n == "cglib-nodep" -> ver("cglib-nodep")
                g == "org.bouncycastle" -> ver("bouncycastle")
                g == "org.brotli" && n == "dec" -> ver("dec")
                g == "dnsjava" && n == "dnsjava" -> ver("dnsjava")
                g == "io.burt" -> ver("jmespath")
                g == "com.jayway.jsonpath" && n == "json-path" -> ver("json-path")
                g == "net.minidev" -> ver("accessors-smart")
                g == "com.helger" && n == "ph-commons" -> ver("ph-commons")
                g == "com.helger" && n == "ph-css" -> ver("ph-css")
                g == "org.apache.mina" && n == "mina-core" -> ver("mina-core")
                g == "org.apache.velocity" && n == "velocity" -> ver("velocity")
                g == "org.jdom" && n == "jdom" -> ver("jdom")
                g == "org.jline" && n == "jline" -> ver("jline")
                g == "org.mongodb" && n == "mongo-java-driver" -> ver("mongo-java-driver")
                g == "org.neo4j.driver" && n == "neo4j-java-driver" -> ver("neo4j-java-driver")
                g == "org.objenesis" && n == "objenesis" -> ver("objenesis")
                g == "javax.mail" && n == "mail" -> ver("mail")
                g == "org.apache.ftpserver" && n == "ftplet-api" -> ver("ftplet-api")
                g == "org.apache.ftpserver" && n == "ftpserver-core" -> ver("ftpserver-core")
                g == "org.apache.geronimo.specs" -> ver("geronimo-jms")
                g == "org.apache.activemq" -> ver("activemq")
                g == "org.springframework" -> ver("springframework")
                g == "junit" && n == "junit" -> ver("junit4")
                g == "org.junit.jupiter" -> ver("junit5")
                g == "org.junit.vintage" -> ver("junit5")
                g == "org.hamcrest" -> ver("hamcrest")
                g == "javax.activation" -> ver("javax-activation")
                g == "com.sun.activation" -> ver("javax-activation")
                g == "com.github.tomakehurst" && n == "wiremock-jre8" -> ver("wiremock-jre8")
                g == "nl.jqno.equalsverifier" && n == "equalsverifier" -> ver("equalsverifier")
                g == "jcharts" && n == "jcharts" -> ver("jcharts")
                g == "org.hsqldb" && n == "hsqldb" -> ver("hsqldb")
                g == "org.spockframework" && n == "spock-core" -> ver("spock-core")
                g == "xmlpull" && n == "xmlpull" -> ver("xmlpull")
                g == "xpp3" -> ver("xpp3-min")
                g == "org.apiguardian" && n == "apiguardian-api" -> ver("apiguardian-api")
                g == "org.ow2.asm" && n == "asm" -> ver("asm")
                else -> null
            }
            if (resolved != null) useVersion(resolved)
        }
    }
}

// ── Configurations (excludes) ─────────────────────────────────────────────────
configurations.runtimeClasspath {
    exclude(group = "org.apache.jmeter", module = "bom")
    exclude(group = "", module = "commons-pool2")
    exclude(group = "javax.jms", module = "jms")
}

// ── Fork submodules (built from source) ───────────────────────────────────────
val jars = arrayOf(
    ":src:bshclient",
    ":src:core",
    ":src:functions",
    ":src:protocol:native",
    ":src:launcher"
)

// ── Upstream JMeter JARs ──────────────────────────────────────────────────────
val jarsDeps = arrayOf(
    "org.apache.jmeter:ApacheJMeter_components:$jmeterVersion",
    "org.apache.jmeter:jorphan:$jmeterVersion",
    "org.apache.jmeter:ApacheJMeter_bolt:$jmeterVersion",
    "org.apache.jmeter:ApacheJMeter_ftp:$jmeterVersion",
    "org.apache.jmeter:ApacheJMeter_http:$jmeterVersion",
    "org.apache.jmeter:ApacheJMeter_java:$jmeterVersion",
    "org.apache.jmeter:ApacheJMeter_jdbc:$jmeterVersion",
    "org.apache.jmeter:ApacheJMeter_jms:$jmeterVersion",
    "org.apache.jmeter:ApacheJMeter_junit:$jmeterVersion",
    "org.apache.jmeter:ApacheJMeter_ldap:$jmeterVersion",
    "org.apache.jmeter:ApacheJMeter_mail:$jmeterVersion",
    "org.apache.jmeter:ApacheJMeter_mongodb:$jmeterVersion",
    "org.apache.jmeter:ApacheJMeter_tcp:$jmeterVersion"
)

// ── Extra dist JARs (not in BOM) ──────────────────────────────────────────────
val extraLibs = listOf(
    "jna", "jxlayer", "ojdbc6", "pdfbox", "mssql-jdbc",
    "rabbitmq-amqp-client", "javatuples", "jsch",
    "hdrhistogram", "osgi-core", "ibm-mq", "darcula-bulenkov"
)

// ── Plugin JARs (lib/ext) ─────────────────────────────────────────────────────
val pluginAliases = listOf(
    "plugin-cmn", "plugin-manager", "plugin-casutg", "plugin-perfmon",
    "plugin-tst", "plugin-cmdrunner", "plugin-dummy", "plugin-functions",
    "plugin-redis", "plugin-synthesis", "plugin-xml", "plugin-graphs-ggl",
    "plugin-bzm-csv", "plugin-bzm-wsc", "plugin-ffw", "plugin-fifo",
    "plugin-graphs-basic", "plugin-graphs-additional", "plugin-websocket",
    "plugin-prometheus", "plugin-bzm-http2", "plugin-iso8583",
    "plugin-pack-listener", "plugin-common-io", "plugin-dir-listing",
    "plugin-autostop", "plugin-plancheck", "plugin-prmctl", "plugin-httpraw",
    "plugin-dbmon", "plugin-graphs-dist", "plugin-cmd", "plugin-bzm-parallel",
    "plugin-udp", "plugin-wssecurity"
)

// ── Extra configurations ──────────────────────────────────────────────────────
val buildDocs by configurations.creating { isCanBeConsumed = false }
val generatorJar by configurations.creating { isCanBeConsumed = false }
val binLicense by configurations.creating { isCanBeConsumed = false }
val allTestClasses by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
}
val pluginClasspath by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    exclude(group = "org.apache.jmeter")
    exclude(group = "commons-io")
    exclude(group = "commons-collections")
}

// ── Dependencies ──────────────────────────────────────────────────────────────
dependencies {
    implementation(platform(project(":src:bom")))

    for (p in jars) {
        implementation(project(p))
    }

    for (z in jarsDeps) {
        implementation(z) {
            exclude(group = "org.apache.jmeter")
        }
    }

    for (alias in extraLibs) {
        implementation(gav(alias)) {
            exclude(group = "org.apache.jmeter")
        }
    }

    for (alias in pluginAliases) {
        pluginClasspath(gav(alias))
    }

    implementation(project(":plugins:jmeter-plugins-table-server-5.0")) {
        exclude(group = "org.apache.jmeter")
    }

    // Strict version pinning
    implementation(gav("commons-math3")) {
        version { strictly(ver("commons-math3")) }
    }
    implementation(gav("jline")) {
        version { strictly(ver("jline")) }
    }
    implementation(gav("commons-io")) {
        version { strictly(ver("commons-io")) }
    }

    generatorJar(project(":src:generator", "archives"))
}

// ── Nexus coordinates (из gradle.properties или env) ──────────────────────────
val snapshotsRepoUrl: String by project
val releasesRepoUrl: String by project
val mavenUsername: String by project
val mavenPassword: String by project

val MVN_USER = System.getenv("MVN_USER") ?: mavenUsername
val MVN_PASS  = System.getenv("MVN_PASS")  ?: mavenPassword

val urlSite = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl

group = "ru.nt_master"
val artifactId      = "nt_master"
val artifactVersion = project.findProperty("version") as? String
    ?: project.findProperty("appversion") as? String
    ?: "1.0.0"

// ── assembleArtifact ──────────────────────────────────────────────────────────
// pullLibsBundle → createDist → zip → publish
tasks.register<Zip>("assembleArtifact") {
    group = "release"
    description = "Собирает финальный ZIP дистрибутива для публикации в Maven Nexus"
    // pullLibsBundle объявлен в nexus-raw.gradle.kts, createDist — в local-dist.gradle.kts
    dependsOn("pullLibsBundle", "createDist")
    archiveBaseName.set(artifactId)
    archiveVersion.set(artifactVersion)
    destinationDirectory.set(layout.buildDirectory.dir("distr"))
    from(rootProject.rootDir) {
        include("bin/**")
        include("lib/**")
        include("extras/**")
    }
}

// ── publishing ────────────────────────────────────────────────────────────────
publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId    = "ru.nt_master"
            artifactId = artifactId
            version    = artifactVersion
            artifact(tasks.getByName("assembleArtifact"))
        }
    }
    repositories {
        maven {
            url = uri(urlSite)
            credentials {
                username = MVN_USER
                password = MVN_PASS
            }
        }
    }
}

// ── publishAll — одна кнопка для разработчика ─────────────────────────────────
// Транзитивно: pullLibsBundle → createDist → assembleArtifact → publish
tasks.register("publishAll") {
    group = "release"
    description = "Получить бандл из Nexus RAW → собрать → опубликовать в Maven Nexus"
    dependsOn("assembleArtifact", "publish")
}

// ── runGui ────────────────────────────────────────────────────────────────────
// Для локальной разработки: получить бандл, собрать JAR, запустить GUI
val runGui by tasks.registering(JavaExec::class) {
    group = "Development"
    description = "Получает бандл из Nexus RAW, собирает дистрибутив и запускает JMeter GUI"
    // pullLibsBundle → createDist, затем запуск
    dependsOn("pullLibsBundle", "createDist")
    workingDir = File(project.rootDir, "bin")
    main = "org.apache.jmeter.NewDriver"
    classpath("$rootDir/bin/ApacheJMeter.jar")
    jvmArgs("-Xss256k")
    jvmArgs("-XX:MaxMetaspaceSize=256m")

    val osName = System.getProperty("os.name")
    if (osName.contains(Regex("mac os x|darwin|osx", RegexOption.IGNORE_CASE))) {
        jvmArgs("-Xdock:name=JMeter")
        jvmArgs("-Xdock:icon=$rootDir/xdocs/images/jmeter_square.png")
        jvmArgs("-Dapple.laf.useScreenMenuBar=true")
        jvmArgs("-Dapple.eawt.quitStrategy=CLOSE_ALL_WINDOWS")
    }

    fun passProperty(name: String, default: String? = null) {
        val value = System.getProperty(name) ?: default
        value?.let { systemProperty(name, it) }
    }
    passProperty("java.awt.headless")

    val props = System.getProperties()
    @Suppress("UNCHECKED_CAST")
    for (e in props.propertyNames() as java.util.Enumeration<String>) {
        if (e.startsWith("jmeter.") || e.startsWith("darklaf.")) passProperty(e)
        if (e == "darklaf.native") {
            systemProperty("darklaf.decorations", "true")
            systemProperty("darklaf.allowNativeCode", "true")
        }
    }
}
