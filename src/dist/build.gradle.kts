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
import de.undercouch.gradle.tasks.download.Download
import de.undercouch.gradle.tasks.download.Verify
import org.gradle.api.internal.artifacts.dependencies.DefaultDependencyConstraint.strictly
import org.gradle.internal.impldep.org.junit.experimental.categories.Categories.CategoryFilter.exclude
import java.io.File

plugins {
    java
    id("de.undercouch.download") version "4.1.2"
    id("com.github.vlsi.crlf")
    `maven-publish`
}

// ── Version catalog accessor ──────────────────────────────────────────────────
val catalog = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")

fun lib(alias: String): MinimalExternalModuleDependency =
    catalog.findDependency(alias).orElseThrow {
        GradleException("Library alias '$alias' not found in libs version catalog")
    }.get()

fun ver(alias: String): String =
    catalog.findVersion(alias).orElseThrow {
        GradleException("Version alias '$alias' not found in libs version catalog")
    }.requiredVersion

/** Returns "group:name:version" string for use where Provider<*> notation doesn't support lambdas */
fun gav(alias: String): String = lib(alias).run { "${module.group}:${module.name}:${versionConstraint.requiredVersion}" }

// ── Configurations ────────────────────────────────────────────────────────────
configurations.runtimeClasspath {
    exclude(group = "org.apache.jmeter", module = "bom")
    exclude(group = "", module = "commons-pool2")
    exclude(group = "javax.jms", module = "jms")
}

// ── Versions ──────────────────────────────────────────────────────────────────
val jmeterVersion = ver("jmeter")

// ── Fork submodules (built from source) ───────────────────────────────────────
val jars = arrayOf(
    ":src:bshclient",
    ":src:core",
    ":src:functions",
    ":src:protocol:native",
    ":src:launcher"
)

// ── Upstream JMeter JARs (versioned via jmeterVersion) ────────────────────────
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

// ── Extra dist JARs (not in BOM — resolved via catalog) ───────────────────────
val extraLibs = listOf(
    "jna",
    "jxlayer",
    "ojdbc6",
    "pdfbox",
    "mssql-jdbc",
    "rabbitmq-amqp-client",
    "javatuples",
    "jsch",
    "hdrhistogram",
    "osgi-core",
    "ibm-mq",
    "darcula-bulenkov"
)

// ── Plugin JARs (not in BOM — resolved via catalog) ───────────────────────────
val pluginAliases = listOf(
    "plugin-cmn",
    "plugin-manager",
    "plugin-casutg",
    "plugin-perfmon",
    "plugin-tst",
    "plugin-cmdrunner",
    "plugin-dummy",
    "plugin-functions",
    "plugin-redis",
    "plugin-synthesis",
    "plugin-xml",
    "plugin-graphs-ggl",
    "plugin-bzm-csv",
    "plugin-bzm-wsc",
    "plugin-ffw",
    "plugin-fifo",
    "plugin-graphs-basic",
    "plugin-graphs-additional",
    "plugin-websocket",
    "plugin-prometheus",
    "plugin-bzm-http2",
    "plugin-iso8583",
    "plugin-pack-listener",
    "plugin-common-io",
    "plugin-dir-listing",
    "plugin-autostop",
    "plugin-plancheck",
    "plugin-prmctl",
    "plugin-httpraw",
    "plugin-dbmon",
    "plugin-graphs-dist",
    "plugin-cmd",
    "plugin-bzm-parallel",
    "plugin-udp",
    "plugin-wssecurity"
)

// Set of "group:name" strings for plugin detection in populateLibs
val pluginModuleIds: Set<String> by lazy {
    pluginAliases.map { alias ->
        val dep = lib(alias)
        "${dep.module.group}:${dep.module.name}"
    }.toSet()
}

// ── Extra configurations ───────────────────────────────────────────────────────
val buildDocs by configurations.creating { isCanBeConsumed = false }
val generatorJar by configurations.creating { isCanBeConsumed = false }
val binLicense by configurations.creating { isCanBeConsumed = false }
val allTestClasses by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
}

// ── Dependencies ──────────────────────────────────────────────────────────────
dependencies {
    // BOM — provides version constraints for all managed libs
    implementation(platform(project(":src:bom")))

    // Fork submodules
    for (p in jars) {
        implementation(project(p))
    }

    // Upstream JMeter JARs
    for (z in jarsDeps) {
        implementation(z) {
            exclude(group = "org.apache.jmeter")
        }
    }

    // Extra dist JARs (versions from catalog, constraints from BOM where applicable)
    for (alias in extraLibs) {
        implementation(gav(alias)) {
            exclude(group = "org.apache.jmeter")
        }
    }

    // Plugin JARs → lib/ext (testCompileOnly so they don't pollute runtimeClasspath)
    for (alias in pluginAliases) {
        testCompileOnly(gav(alias)) {
            exclude(group = "org.apache.jmeter")
            exclude(group = "commons-io")
            exclude(group = "commons-collections")
        }
    }

    implementation(project(":plugins:jmeter-plugins-table-server-5.0")) {
        exclude(group = "org.apache.jmeter")
    }

    // Strict version pinning: use String notation "g:a:v" so the { version{} } lambda
    // resolves unambiguously to DependencyHandler.implementation(String, Action<...>)
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

// ── Clean ─────────────────────────────────────────────────────────────────────
tasks.named(BasePlugin.CLEAN_TASK_NAME).configure {
    doLast {
        delete(fileTree("$rootDir/bin") { include("ApacheJMeter.jar") })
        delete(fileTree("$rootDir/lib") { include("*.jar") })
        delete(fileTree("$rootDir/lib/ext") { include("ApacheJMeter*.jar") })
    }
}

// ── CopySpecs ─────────────────────────────────────────────────────────────────
// Renamed from 'libs' to 'libsSpec' to avoid shadowing the version catalog extension
val libsSpec = copySpec {}
val libsExt  = copySpec {}
val binLibs  = copySpec {}

// ── populateLibs ─────────────────────────────────────────────────────────────
val populateLibs by tasks.registering {
    dependsOn(configurations.runtimeClasspath)
    doLast {
        val launcherProject  = project(":src:launcher").path
        val bshclientProject = project(":src:bshclient").path
        val jorphanProject   = project(":src:jorphan").path

        val deps = configurations.runtimeClasspath.get().resolvedConfiguration.resolvedArtifacts
        println(deps)

        for (dep in deps) {
            println("-->$dep")
            val compId = dep.id.componentIdentifier
            if (compId !is ProjectComponentIdentifier || !compId.build.isCurrentBuild) {
                if (!dep.name.contains("ApacheJmeter", ignoreCase = true)) {
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

        // Plugin JARs: match by "group:name" (version-independent)
        val pluginConf = configurations.testCompileOnly.get().resolvedConfiguration.resolvedArtifacts
        for (dep in pluginConf) {
            println("plugin --> $dep")
            val compId = dep.id.componentIdentifier
            val moduleId = "${dep.moduleVersion.id.group}:${dep.moduleVersion.id.name}"
            if (moduleId in pluginModuleIds) {
                if (compId !is ProjectComponentIdentifier || !compId.build.isCurrentBuild) {
                    libsExt.from(dep.file)
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

// ── Copy tasks ────────────────────────────────────────────────────────────────
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

// ── Docs helpers ──────────────────────────────────────────────────────────────
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
    into("docs") { docCssAndImages() }
}

val distributionGroup = "distribution"
val baseFolder = "apache-jmeter-${rootProject.version}"

fun CopySpec.javadocs() = from(javadocAggregate)

fun CopySpec.excludeLicenseFromSourceRelease() {
    exclude("licenses/**")
    exclude("LICENSE")
}

fun CrLfSpec.binaryLayout() = copySpec {
    into(baseFolder) {
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
        into("bin") { with(binLibs) }
        into("lib") {
            with(libsSpec)
            into("ext") { with(libsExt) }
        }
        printableDocumentation()
        into("docs/api") { javadocs() }
    }
}

// ── Javadoc aggregate ─────────────────────────────────────────────────────────
val javadocAggregate by tasks.registering(Javadoc::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Generates aggregate javadoc for all the artifacts"
    val sourceSets = jars.map { project(it).sourceSets.main }
    classpath = files(sourceSets.map { set -> set.map { it.output + it.compileClasspath } })
    setSource(sourceSets.map { set -> set.map { it.allJava } })
    setDestinationDir(file("$buildDir/docs/javadocAggregate"))
}

// ── Distribution archives ─────────────────────────────────────────────────────
val skipDist: Boolean by rootProject.extra

for (type in listOf("binary", "source")) {
    if (skipDist) break
    for (archive in listOf(Zip::class, Tar::class)) {
        val taskName = "dist${archive.simpleName}${type.replace("binary", "").capitalize()}"
        tasks.register(taskName, archive) {
            dependsOn(createDist)
            val eol = if (archive == Tar::class) LineEndings.LF else LineEndings.CRLF
            group = distributionGroup
            description = "Creates $type distribution"
            if (this is Tar) compression = Compression.GZIP
            archiveBaseName.set("apache-jmeter-${rootProject.version}${if (type == "source") "_src" else ""}")
            archiveVersion.set("")
            CrLfSpec(eol).run { binaryLayout() }
        }
    }
}

// ── Publishing / Nexus ────────────────────────────────────────────────────────
val snapshotsRepoUrl: String by project
val releasesRepoUrl: String by project
val mavenUsername: String by project
val mavenPassword: String by project
//val MVN_USER = System.getenv("MVN_USER") ?: mavenUsername
//val MVN_PASS = System.getenv("MVN_PASS") ?: mavenPassword
val urlSite = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl

val resourcesDir = rootProject.rootDir

group = "ru.nt_master"

val artifactId = "nt_master"
val artifactVersion = project.findProperty("version") as? String ?: project.findProperty("appversion")
val artifactFileName = "${artifactId}-${artifactVersion}.zip"
val nexusDownloadUrl = "${urlSite}ru/${artifactId}/${artifactVersion}/${artifactFileName}"

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
//    username(MVN_USER)
//    password(MVN_PASS)
    onlyIfModified(true)
    useETag(true)
    doFirst { downloadDir.mkdirs() }
}

tasks.register<Copy>("unzipArtifact") {
    description = "Распаковывает ZIP"
    dependsOn("downloadArtifactZip")
    from(zipTree(zipFile))
    into(resourcesDir)
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    includeEmptyDirs = false
    doFirst {
        logger.lifecycle("Распаковка ${zipFile.name} -> ${resourcesDir.path}")
        resourcesDir.mkdirs()
    }
    doLast {
        logger.lifecycle("Распаковано в: ${resourcesDir.path}")
        logger.lifecycle("Всего файлов: ${fileTree(resourcesDir).count()}")
    }
}

tasks.register<Verify>("verifyDownload") {
    description = "Проверяет целостность скачанного ZIP"
    dependsOn("downloadArtifactZip")
    src(zipFile)
    algorithm("MD5")
}

tasks.register<Zip>("assembleArtifact") {
    doFirst { CrLfSpec().run { binaryLayout() } }
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
//    username(MVN_USER)
//    password(MVN_PASS)
    doFirst { logger.lifecycle("Принудительное скачивание: $urlSite") }
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

tasks.build { dependsOn("unzipArtifact") }
tasks.clean { dependsOn("cleanResources", "cleanDownloads") }

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
//                username = MVN_USER
//                password = MVN_PASS
            }
        }
    }
}

// ── Run GUI ───────────────────────────────────────────────────────────────────
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
    for (e in props.propertyNames() as java.util.Enumeration<String>) {
        if (e.startsWith("jmeter.") || e.startsWith("darklaf.")) passProperty(e)
        if (e == "darklaf.native") {
            systemProperty("darklaf.decorations", "true")
            systemProperty("darklaf.allowNativeCode", "true")
        }
    }
}
