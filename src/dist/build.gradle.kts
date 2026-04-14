import org.gradle.internal.impldep.org.hamcrest.CoreMatchers.endsWith

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

import com.github.vlsi.gradle.crlf.CrLfSpec
import com.github.vlsi.gradle.crlf.LineEndings
import java.nio.file.Paths

////import com.github.vlsi.gradle.crlf.LineEndings
////import com.github.vlsi.gradle.git.FindGitAttributes
////import com.github.vlsi.gradle.git.dsl.gitignore
////import com.github.vlsi.gradle.properties.dsl.props
////import kotlin.math.absoluteValue
////import org.gradle.api.internal.TaskOutputsInternal
import de.undercouch.gradle.tasks.download.Download
import de.undercouch.gradle.tasks.download.Verify
import org.gradle.api.internal.artifacts.dependencies.DefaultDependencyConstraint.strictly
import org.gradle.internal.impldep.org.junit.experimental.categories.Categories.CategoryFilter.exclude
import java.io.File
import java.util.Date
import java.text.SimpleDateFormat

plugins {
    id("de.undercouch.download") version "4.1.2"
    id("com.github.vlsi.crlf")
    // id("com.github.vlsi.stage-vote-release")
    `maven-publish`
    //id ("distribution")
}

configurations.runtimeClasspath {
    exclude(group = "org.apache.jmeter", module = "bom")
    exclude(group = "", module = "commons-pool2")
    exclude(group = "javax.jms", module = "jms")
}

val jmeterVersion = libs.versions.jmeter.get()

var jars = arrayOf(
    ":src:bshclient",
    ":src:core",
    ":src:functions",
    ":src:protocol:native",
    ":src:launcher"
)

var jarsDeps = arrayOf(
    // Стоковые JMeter JAR — версия из libs.versions.toml
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
    "org.apache.jmeter:ApacheJMeter_tcp:$jmeterVersion",

    // Дополнительные JAR дистрибутива — версии из libs.versions.toml
    libs.jna.get().toString(),
    libs.jxlayer.get().toString(),
    libs.ojdbc6.get().toString(),
    libs.pdfbox.get().toString(),
    libs.mssqlJdbc.get().toString(),
    libs.rabbitmqAmqpClient.get().toString(),
    libs.javatuples.get().toString(),
    libs.jsch.get().toString(),
    libs.hdrhistogram.get().toString(),
    libs.osgiCore.get().toString(),
    libs.ibmMq.get().toString(),
    libs.darculaBulenkov.get().toString()
)

//TODO remove doubles
var plugins = arrayOf(
    // Плагины JMeter — версии из libs.versions.toml
    libs.pluginCmn.get().toString(),
    libs.pluginManager.get().toString(),
    libs.pluginCasutg.get().toString(),
    libs.pluginPerfmon.get().toString(),
    libs.pluginTst.get().toString(),
    libs.pluginCmdrunner.get().toString(),
    libs.pluginDummy.get().toString(),
    libs.pluginFunctions.get().toString(),
    libs.pluginRedis.get().toString(),
    libs.pluginSynthesis.get().toString(),
    libs.pluginXml.get().toString(),
    libs.pluginGraphsGgl.get().toString(),
    libs.pluginBzmCsv.get().toString(),
    libs.pluginBzmWsc.get().toString(),
    libs.pluginFfw.get().toString(),
    libs.pluginFifo.get().toString(),
    libs.pluginGraphsBasic.get().toString(),
    libs.pluginGraphsAdditional.get().toString(),
    libs.pluginWebsocket.get().toString(),
    libs.pluginPrometheus.get().toString(),
    libs.pluginBzmHttp2.get().toString(),
    libs.pluginIso8583.get().toString(),
    libs.pluginPackListener.get().toString(),
    libs.pluginCommonIo.get().toString(),
    libs.pluginDirListing.get().toString(),
    libs.pluginAutostop.get().toString(),
    libs.pluginPlancheck.get().toString(),
    libs.pluginPrmctl.get().toString(),
    libs.pluginHttpraw.get().toString(),
    libs.pluginDbmon.get().toString(),
    libs.pluginGraphsDist.get().toString(),
    libs.pluginCmd.get().toString(),
    libs.pluginBzmParallel.get().toString(),
    libs.pluginUdp.get().toString(),
    libs.pluginWssecurity.get().toString()
)

val buildDocs by configurations.creating {
    isCanBeConsumed = false
}
val generatorJar by configurations.creating {
    isCanBeConsumed = false
}
val binLicense by configurations.creating {
    isCanBeConsumed = false
}
val allTestClasses by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
}

dependencies {
    // ── Модули форка (собираются из исходников) ──
    for (p in jars) {
        api(project(p))
    }

    // ── Стоковые JMeter JAR ──
    for (z in jarsDeps) {
        implementation(z) {
            exclude(group = "org.apache.jmeter")
        }
    }

    // ── Плагины → lib/ext ──
    for (pl in plugins) {
        testCompileOnly(pl) {
            exclude(group = "org.apache.jmeter")
            exclude(group = "commons-io")
            exclude(group = "commons-collections")
        }
    }

    api(project(":plugins:jmeter-plugins-table-server-5.0")) {
        exclude(group = "org.apache.jmeter")
    }

    // ── Strict version pinning (версии из libs.versions.toml) ──
    implementation(libs.commonsMath3) {
        version { strictly(libs.versions.commonsMath3.get()) }
    }
    implementation(libs.jline) {
        version { strictly(libs.versions.jline.get()) }
    }
    implementation(libs.commonsIo) {
        version { strictly(libs.versions.commonsIo.get()) }
    }

    generatorJar(project(":src:generator", "archives"))
}

tasks.named(BasePlugin.CLEAN_TASK_NAME).configure {
    doLast {
        delete(fileTree("$rootDir/bin") { include("ApacheJMeter.jar") })
        delete(fileTree("$rootDir/lib") { include("*.jar") })
        delete(fileTree("$rootDir/lib/ext") { include("ApacheJMeter*.jar") })
    }
}

// Renamed from 'libs' to 'libsSpec' to avoid shadowing the version catalog extension 'libs'
val libsSpec = copySpec {
    // Third-party dependencies + jorphan.jar
}

val libsExt = copySpec {
    // НТ Мастер jars
}

val binLibs = copySpec {
    // ApacheJMeter.jar launcher
}

val populateLibs by tasks.registering {
    dependsOn(configurations.runtimeClasspath)
    doLast {
        val deps = configurations.runtimeClasspath.get().resolvedConfiguration.resolvedArtifacts

        println(configurations.runtimeClasspath.get().resolvedConfiguration.resolvedArtifacts)
        val launcherProject = project(":src:launcher").path
        val bshclientProject = project(":src:bshclient").path
        val jorphanProject = project(":src:jorphan").path

        for (dep in deps) {
            println("-->" + dep)
            val compId = dep.id.componentIdentifier
            if ((compId !is ProjectComponentIdentifier
                        || !compId.build.isCurrentBuild)
            ) {
                if (!dep.name.contains("ApacheJmeter", true)) {
                    libsSpec.from(dep.file)
                } else {
                    libsExt.from(dep.file)
                }
                continue
            }
            when (compId.projectPath) {
                launcherProject -> binLibs
                jorphanProject, bshclientProject -> libsSpec
                else -> libsExt
            }.from(dep.file) {
                rename { dep.name + "." + dep.extension }
            }
        }

        val pluginConf = configurations.testCompileOnly.get().resolvedConfiguration.resolvedArtifacts

        for (dep in pluginConf) {
            println("plugin --> $dep")
            val compId = dep.id.componentIdentifier

            if (plugins.any { dep.toString().contains(it) }) {
                if (compId !is ProjectComponentIdentifier || !compId.build.isCurrentBuild) {
                    libsExt.from(dep.file)
                    continue
                }
            } else {
                libsSpec.from(dep.file)
            }
        }
    }
}

libsSpec.from(populateLibs)
libsExt.from(populateLibs)
binLibs.from(populateLibs)

val copyLibs by tasks.registering(Sync::class) {
    rootSpec.into("$rootDir/lib")
    with(libsSpec)
    preserve {
        include("**/*.txt")
        include("ext/*.jar")
        exclude("ext/ApacheJMeter*.jar")
    }
    into("ext") {
        with(libsExt)
        from(files(generatorJar)) {
            rename { "ApacheJMeter_generator.jar" }
        }
    }
    into("ext") {
        with(libsExt)
        from(files(generatorJar)) {
            rename { "ApacheJMeter_generator.jar" }
        }
    }
}

val copyBinLibs by tasks.registering(Copy::class) {
    rootSpec.into("$rootDir/bin")
    with(binLibs)
}

val createDist by tasks.registering {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Copies JMeter jars and dependencies to projectRoot/lib/ folder"
    dependsOn(copyLibs)
    dependsOn(copyBinLibs)
}

val xdocs = "$rootDir/xdocs"

fun CopySpec.docCssAndImages() {
    from(xdocs) {
        include(".htaccess")
        include("css/**")
        include("images/**")
    }
}

fun CopySpec.manuals() {
    from(xdocs) {
        include("demos/**")
        include("extending/jmeter_tutorial.pdf")
        include("usermanual/**/*.pdf")
    }
}

fun CopySpec.printableDocumentation() {
    into("docs") {
        docCssAndImages()
    }
}

val distributionGroup = "distribution"
val baseFolder = "apache-jmeter-${rootProject.version}"

fun CopySpec.javadocs() = from(javadocAggregate)

fun CopySpec.excludeLicenseFromSourceRelease() {
    exclude("licenses/**")
    exclude("LICENSE")
}

fun CrLfSpec.binaryLayout() = copySpec {
    println("binary creation")

    into(baseFolder) {
        println("---->>>>" + baseFolder)
        from(files(binLicense))
        from(rootDir) {
            exclude("bin/testfiles")
            exclude("bin/rmi_keystore.jks")
            include("bin/**")
            include("lib/ext/**")
            include("lib/junit/**")
            include("extras/**")
            include("README.md")
            excludeLicenseFromSourceRelease()
        }
        into("bin") {
            with(binLibs)
        }
        println("binLibs:{$binLibs}")
        into("lib") {
            with(libsSpec)
            into("ext") {
                with(libsExt)
            }
            println("libsExt:{$libsExt}")
        }
        printableDocumentation()
        into("docs/api") {
            javadocs()
        }
    }
}

val javadocAggregate by tasks.registering(Javadoc::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Generates aggregate javadoc for all the artifacts"

    val sourceSets = jars.map { project(it).sourceSets.main }
    classpath = files(sourceSets.map { set -> set.map { it.output + it.compileClasspath } })
    setSource(sourceSets.map { set -> set.map { it.allJava } })
    setDestinationDir(file("$buildDir/docs/javadocAggregate"))
}

val skipDist: Boolean by rootProject.extra

for (type in listOf("binary", "source")) {
    if (skipDist) {
        break
    }
    for (archive in listOf(Zip::class, Tar::class)) {
        val taskName = "dist${archive.simpleName}${type.replace("binary", "").capitalize()}"
        val archiveTask = tasks.register(taskName, archive) {
            dependsOn(createDist)

            val eol = if (archive == Tar::class) LineEndings.LF else LineEndings.CRLF
            group = distributionGroup
            description = "Creates $type distribution with TODO "
            if (this is Tar) {
                compression = Compression.GZIP
            }
            archiveBaseName.set("apache-jmeter-${rootProject.version}${if (type == "source") "_src" else ""}")
            archiveVersion.set("")
            CrLfSpec(eol).run {
                binaryLayout()
            }
        }
    }
}

val snapshotsRepoUrl: String by project
val releasesRepoUrl: String by project
val mavenUsername: String by project
val mavenPassword: String by project
val MVN_USER = System.getenv("MVN_USER") ?: mavenUsername
val MVN_PASS = System.getenv("MVN_PASS") ?: mavenPassword
val urlSite = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl

val resourcesDir = rootProject.rootDir

group = "ru.nt_master"

ext {
    val artifactGroup2 = group.toString().replace(".", "/")
}

val artifactGroup = "ru"
val artifactId = "nt_master"
val artifactVersion = project.findProperty("version") as? String ?: project.findProperty("appversion")
val artifactFileName = "${artifactId}-${artifactVersion}.zip"
val nexusDownloadUrl = "${urlSite}${artifactGroup}/${artifactId}/${artifactVersion}/${artifactFileName}"

val downloadDir = File(buildDir, "downloaded")
val zipFile = File(downloadDir, artifactFileName)

tasks.register<Download>("downloadArtifactZip") {
    description = "Скачивает ZIP файл из Nexus raw репозитория"
    src(nexusDownloadUrl)
    dest(zipFile)
    overwrite(false)
    quiet(false)
    connectTimeout(30000)
    readTimeout(30000)
    username(MVN_USER)
    password(MVN_PASS)
    onlyIfModified(true)
    useETag(true)
    doFirst {
        downloadDir.mkdirs()
    }
}

tasks.register<Copy>("unzipArtifact") {
    description = "Распаковывает ZIP "
    dependsOn("downloadArtifactZip")
    from(zipTree(zipFile))
    into(resourcesDir)
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    includeEmptyDirs = false
    doFirst {
        logger.lifecycle("package Распаковка ${zipFile.name} -> ${resourcesDir.path}")
        resourcesDir.mkdirs()
    }
    doLast {
        logger.lifecycle("white_check_mark Распаковано в: ${resourcesDir.path}")
        val fileCount = fileTree(resourcesDir).count()
        logger.lifecycle("   Всего файлов: $fileCount")
    }
}

tasks.register<Verify>("verifyDownload") {
    description = "Проверяет целостность скачанного ZIP"
    dependsOn("downloadArtifactZip")
    src(zipFile)
    algorithm("MD5")
}

tasks.register<Zip>("assembleArtifact") {
    doFirst {
        CrLfSpec().run { binaryLayout() }
    }
    println("create local distribution from #${rootProject.rootDir}")
    archiveBaseName.set("NT_Master")
    destinationDirectory.set(Paths.get("build/distr").toFile())
    from(rootProject.rootDir) {
        include("bin/**")
        include("lib/**")
        include("extras/**")
        include("xdocs/**")
    }
    description = "Assemble distribution archive $archiveName into ${relativePath(destinationDir)}"
}

tasks.register<Download>("forceDownloadArtifact") {
    description = "Принудительно скачивает ZIP заново"
    src(urlSite)
    dest(zipFile)
    overwrite(true)
    username(MVN_USER)
    password(MVN_PASS)
    doFirst {
        logger.lifecycle("warning Принудительное скачивание: $urlSite")
    }
}

tasks.register<Delete>("cleanResources") {
    description = "Удаляет распакованные ресурсы"
    delete(resourcesDir)
}

tasks.register<Delete>("cleanDownloads") {
    description = "Удаляет скачанные ZIP файлы"
    delete(downloadDir)
}

tasks.register("prepareBundle") {
    description = "Скачивает, проверяет и распаковывает бандл"
    dependsOn("downloadArtifactZip", "verifyDownload", "unzipArtifact")
    group = "bundle"
}

tasks.build {
    dependsOn("unzipArtifact")
}

tasks.clean {
    dependsOn("cleanResources", "cleanDownloads")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
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

val runGui by tasks.registering(JavaExec::class) {
    group = "Development"
    description = "Builds and starts JMeter GUI"
    dependsOn(createDist)

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
    for (e in props.propertyNames() as `java.util`.Enumeration<String>) {
        if (e.startsWith("jmeter.") || e.startsWith("darklaf.")) {
            passProperty(e)
        }
        if (e == "darklaf.native") {
            systemProperty("darklaf.decorations", "true")
            systemProperty("darklaf.allowNativeCode", "true")
        }
    }
}
