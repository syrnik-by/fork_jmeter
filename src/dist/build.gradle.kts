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

//distributions {
//    main {
//        contents {
//            into ("/")
//            from libsDir
//                    include ‘.jar’
//            rename '..jar’, “${project.name}.jar”
//            from “env”
//            include ‘*.conf’
//        }
//    }
//}

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
    //":src:components",
    //":src:examples",
    //":src:jorphan",
    //":src:protocol:bolt",
    //":src:protocol:ftp",
    //":src:protocol:http",
   // ":src:protocol:java"
    //":src:protocol:jdbc",
    //":src:protocol:jms",
    //":src:protocol:junit",
    //":src:protocol:ldap",
    //":src:protocol:mail",
    //":src:protocol:mongodb",
    //":src:protocol:tcp"
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
    libs.mssql.jdbc.get().toString(),
    libs.rabbitmq.amqp.client.get().toString(),
    libs.javatuples.get().toString(),
    libs.jsch.get().toString(),
    libs.hdrhistogram.get().toString(),
    libs.osgi.core.get().toString(),
    libs.ibm.mq.get().toString(),
    libs.darcula.bulenkov.get().toString()
)

//TODO remove doubles
var plugins = arrayOf(
    // Плагины JMeter — версии из libs.versions.toml
    libs.plugin.cmn.get().toString(),
    libs.plugin.manager.get().toString(),
    libs.plugin.casutg.get().toString(),
    libs.plugin.perfmon.get().toString(),
    libs.plugin.tst.get().toString(),
    libs.plugin.cmdrunner.get().toString(),
    libs.plugin.dummy.get().toString(),
    libs.plugin.functions.get().toString(),
    libs.plugin.redis.get().toString(),
    libs.plugin.synthesis.get().toString(),
    libs.plugin.xml.get().toString(),
    libs.plugin.graphs.ggl.get().toString(),
    libs.plugin.bzm.csv.get().toString(),
    libs.plugin.bzm.wsc.get().toString(),
    libs.plugin.ffw.get().toString(),
    libs.plugin.fifo.get().toString(),
    libs.plugin.graphs.basic.get().toString(),
    libs.plugin.graphs.additional.get().toString(),
    libs.plugin.websocket.get().toString(),
    libs.plugin.prometheus.get().toString(),
    libs.plugin.bzm.http2.get().toString(),
    libs.plugin.iso8583.get().toString(),
    libs.plugin.pack.listener.get().toString(),
    libs.plugin.common.io.get().toString(),
    libs.plugin.dir.listing.get().toString(),
    libs.plugin.autostop.get().toString(),
    libs.plugin.plancheck.get().toString(),
    libs.plugin.prmctl.get().toString(),
    libs.plugin.httpraw.get().toString(),
    libs.plugin.dbmon.get().toString(),
    libs.plugin.graphs.dist.get().toString(),
    libs.plugin.cmd.get().toString(),
    libs.plugin.bzm.parallel.get().toString(),
    libs.plugin.udp.get().toString(),
    libs.plugin.wssecurity.get().toString()
)

//
//// isCanBeConsumed = false ==> other modules must not use the configuration as a dependency
val buildDocs by configurations.creating {
    isCanBeConsumed = false
}
val generatorJar by configurations.creating {
    isCanBeConsumed = false
}
//val junitSampleJar by configurations.creating {
//    isCanBeConsumed = false
//}
val binLicense by configurations.creating {
    isCanBeConsumed = false
}
//val srcLicense by configurations.creating {
//    isCanBeConsumed = false
//}
//
val allTestClasses by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
}
//
//// Note: you can inspect final classpath (list of jars in the binary distribution)  via
//// gw dependencies --configuration runtimeClasspath
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
    implementation(libs.commons.math3) {
        version { strictly(libs.versions.commons.math3.get()) }
    }
    implementation(libs.jline) {
        version { strictly(libs.versions.jline.get()) }
    }
    implementation(libs.commons.io) {
        version { strictly(libs.versions.commons.io.get()) }
    }

    generatorJar(project(":src:generator", "archives"))
}

//
tasks.named(BasePlugin.CLEAN_TASK_NAME).configure {
    doLast {
        // createDist can't yet remove outdated jars (e.g. when dependency is updated to a newer version)
        // so we enhance "clean" task to kill the jars
        delete(fileTree("$rootDir/bin") { include("ApacheJMeter.jar") })
        delete(fileTree("$rootDir/lib") { include("*.jar") })
        delete(fileTree("$rootDir/lib/ext") { include("ApacheJMeter*.jar") })
    }
}
//
//// Libs are populated dynamically since we can't get the full set of dependencies
//// before we execute all the build scripts
val libs = copySpec {
//    // Third-party dependencies + jorphan.jar
}
//
val libsExt = copySpec {
//    // НТ Мастер jars
}

val binLibs = copySpec {
    // ApacheJMeter.jar launcher
}
//
//// Splits jar dependencies to "lib", "lib/ext", and "bin" folders
val populateLibs by tasks.registering {
    dependsOn(configurations.runtimeClasspath)
    doLast {
        val deps = configurations.runtimeClasspath.get().resolvedConfiguration.resolvedArtifacts
        //      println(" ----===configurations.runtimeClasspath")

        println(configurations.runtimeClasspath.get().resolvedConfiguration.resolvedArtifacts)
        // This ensures project exists, if project is renamed, names should be corrected here as wells
        val launcherProject = project(":src:launcher").path
        val bshclientProject = project(":src:bshclient").path
        val jorphanProject = project(":src:jorphan").path
        //listOf(libs, libsExt, binLibs).forEach {
        //    it.fileMode = "644".toInt(8)
        //    it.dirMode = "755".toInt(8)
        //}
        for (dep in deps) {
            println("-->" + dep)
            val compId = dep.id.componentIdentifier
            if ((compId !is ProjectComponentIdentifier
                        || !compId.build.isCurrentBuild)
            ) {
                if (!dep.name.contains("ApacheJmeter", true)) {
                    libs.from(dep.file)
                } else {
                    libsExt.from(dep.file)
                }
                // Move all non-JMeter jars to lib folder
                continue
            }
            // JMeter jars are spread across $root/bin, $root/libs, and $root/libs/ext
            // for historical reasons
            when (compId.projectPath) {
                launcherProject -> binLibs //main jar to run
                jorphanProject, bshclientProject -> libs

                else -> libsExt
            }.from(dep.file) {
                // Remove version from the file name
                rename { dep.name + "." + dep.extension }

            }
        }

        val pluginConf = configurations.testCompileOnly.get().resolvedConfiguration.resolvedArtifacts

        //only plugins without transitive
        for (dep in pluginConf) {
            println("plugin --> $dep")
            val compId = dep.id.componentIdentifier

            if (plugins.any { dep.toString().contains(it) }) {
                if (compId !is ProjectComponentIdentifier || !compId.build.isCurrentBuild) {
                    libsExt.from(dep.file)
                    continue
                }
            } else {
                libs.from(dep.file)
            }

        }
    }
}
//
//val updateExpectedJars by props()
//
//val verifyReleaseDependencies by tasks.registering {
//    description = "Verifies if binary release archive contains the expected set of external jars"
//    group = LifecycleBasePlugin.VERIFICATION_GROUP
//
//    dependsOn(configurations.runtimeClasspath)
//    val expectedLibs = file("src/dist/expected_release_jars.csv")
//    inputs.file(expectedLibs)
//    val actualLibs = File(buildDir, "dist/expected_release_jars.csv")
//    outputs.file(actualLibs)
//    doLast {
//        val caseInsensitive: Comparator<String> = compareBy(String.CASE_INSENSITIVE_ORDER, { it })
//
//        val deps = configurations.runtimeClasspath.get().resolvedConfiguration.resolvedArtifacts
//        val libs = deps.asSequence()
//            .filter {
//                val compId = it.id.componentIdentifier
//                compId !is ProjectComponentIdentifier || !compId.build.isCurrentBuild
//            }
//            .map { it.file.name to it.file.length() }
//            .sortedWith(compareBy(caseInsensitive) { it.first })
//            .associate { it }
//
//        val expected = expectedLibs.readLines().asSequence()
//            .filter { "," in it }
//            .map {
//                val (length, name) = it.split(",", limit = 2)
//                name to length.toLong()
//            }
//            .associate { it }
//
//        if (libs == expected) {
//            return@doLast
//        }
//
//        val sb = StringBuilder()
//        sb.append("External dependencies differ (you could update ${expectedLibs.relativeTo(rootDir)} if you add -PupdateExpectedJars):")
//
//        val sizeBefore = expected.values.sum()
//        val sizeAfter = libs.values.sum()
//        if (sizeBefore != sizeAfter) {
//            sb.append("\n  $sizeBefore => $sizeAfter bytes")
//            sb.append(" (${if (sizeAfter > sizeBefore) "+" else "-"}${(sizeAfter - sizeBefore).absoluteValue} byte")
//            if ((sizeAfter - sizeBefore).absoluteValue > 1) {
//                sb.append("s")
//            }
//            sb.append(")")
//        }
//        if (libs.size != expected.size) {
//            sb.append("\n  ${expected.size} => ${libs.size} files")
//            sb.append(" (${if (libs.size > expected.size) "+" else "-"}${(libs.size - expected.size).absoluteValue})")
//        }
//        sb.appendln()
//        for (dep in (libs.keys + expected.keys).sortedWith(caseInsensitive)) {
//            val old = expected[dep]
//            val new = libs[dep]
//            if (old == new) {
//                continue
//            }
//            sb.append("\n")
//            if (old != null) {
//                sb.append("-").append(old.toString().padStart(8))
//            } else {
//                sb.append("+").append(new.toString().padStart(8))
//            }
//            sb.append(" ").append(dep)
//        }
//        val newline = System.getProperty("line.separator")
//        actualLibs.writeText(
//            libs.map { "${it.value},${it.key}" }.joinToString(newline, postfix = newline)
//        )
//        if (updateExpectedJars) {
//            println("Updating ${expectedLibs.relativeTo(rootDir)}")
//            actualLibs.copyTo(expectedLibs, overwrite = true)
//        } else {
//            throw GradleException(sb.toString())
//        }
//    }
//}
//
//tasks.check {
//    dependsOn(verifyReleaseDependencies)
//}
//
//// This adds dependency on "populateLibs" task
//// This makes uses of these copySpecs transparently depend on the builder task
libs.from(populateLibs)
libsExt.from(populateLibs)
binLibs.from(populateLibs)
//
val copyLibs by tasks.registering(Sync::class) {
    // Can't use $rootDir since Gradle somehow reports .gradle/caches/ as "always modified"
    rootSpec.into("$rootDir/lib")
    with(libs)
    preserve {
        // Sync does not really know which files it copied during previous times, so
        // it just removes everything it sees.
        // We configure it to keep txt files that should be present there (the files come from Git source tree)
        include("**/*.txt")
        // Keep jars in lib/ext so developers don't have to re-install the plugins again and again
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
    //   into("junit") {
    //       from(files(junitSampleJar)) {
    //           rename { "test.jar" }
    //       }
    //   }
}
//
val copyBinLibs by tasks.registering(Copy::class) {
//    // Can't use $rootDir since Gradle somehow reports .gradle/caches/ as "always modified"
    rootSpec.into("$rootDir/bin")
    with(binLibs)
}
//
val createDist by tasks.registering {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Copies JMeter jars and dependencies to projectRoot/lib/ folder"
    dependsOn(copyLibs)
    dependsOn(copyBinLibs)
}
//
//// This task scans the project for gitignore / gitattributes, and that is reused for building
//// source/binary artifacts with the appropriate eol/executable file flags
//val gitProps by rootProject.tasks.existing(FindGitAttributes::class)
//
//fun createAnakiaTask(
//    taskName: String,
//    baseDir: String,
//    extension: String = ".html",
//    style: String,
//    velocityProperties: String,
//    projectFile: String,
//    excludes: Array<String>,
//    includes: Array<String>
//): TaskProvider<Task> {
//    val outputDir = "$buildDir/docs/$taskName"
//
//    val prepareProps = tasks.register("prepareProperties$taskName") {
//        // AnakiaTask can't use relative paths, and it forbids ../, so we create a dedicated
//        // velocity.properties file that contains absolute path
//        inputs.file(velocityProperties)
//        val outputProps = "$buildDir/docProps/$taskName/velocity.properties"
//        outputs.file(outputProps)
//        doLast {
//            // Unfortunately, Velocity does not use Java properties format.
//            // For instance, Properties escape : as \:, however Velocity does not understand that.
//            // Thus it tries to use c\:\path\to\workspace which does not work
//            val p = `java.util`.Properties()
//            file(velocityProperties).reader().use {
//                p.load(it)
//            }
//            p["resource.loader"] = "file"
//            p["file.resource.loader.path"] = baseDir
//            p["file.resource.loader.class"] = "org.apache.velocity.runtime.resource.loader.FileResourceLoader"
//            val specials = Regex("""([,\\])""")
//            val lines = p.entries
//                .map { (it.key as String) + "=" + ((it.value as String).replace(specials, """\\$1""")) }
//                .sorted()
//            file(outputProps).apply {
//                parentFile.run { isDirectory || mkdirs() } || throw IllegalStateException("Unable to create directory $parentFile")
//
//                writer().use {
//                    it.appendln("# Auto-generated from $velocityProperties to pass absolute path to Velocity")
//                    for (line in lines) {
//                        it.appendln(line)
//                    }
//                }
//            }
//        }
//    }
//
//    return tasks.register(taskName) {
//        inputs.file("$baseDir/$style")
//        inputs.file("$baseDir/$projectFile")
//        inputs.files(fileTree(baseDir) {
//            include(*includes)
//            exclude(*excludes)
//        })
//        inputs.property("extension", extension)
//        outputs.dir(outputDir)
//        dependsOn(prepareProps)
//
//        doLast {
//            ant.withGroovyBuilder {
//                "taskdef"("name" to "anakia",
//                        "classname" to "org.apache.velocity.anakia.AnakiaTask",
//                        "classpath" to buildDocs.asPath)
//                "anakia"("basedir" to baseDir,
//                        "destdir" to outputDir,
//                        "extension" to extension,
//                        "style" to style,
//                        "projectFile" to projectFile,
//                        "excludes" to excludes.joinToString(" "),
//                        "includes" to includes.joinToString(" "),
//                        "lastModifiedCheck" to "true",
//                        "velocityPropertiesFile" to prepareProps.get().outputs.files.singleFile)
//            }
//        }
//    }
//}
//
val xdocs = "$rootDir/xdocs"

//
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

//
fun CopySpec.printableDocumentation() {
    into("docs") {
        docCssAndImages()
    }
    //  into("printable_docs") {
    //      from(buildPrintableDoc)
    //      manuals()
    //  }
}
//
// val buildPrintableDoc = createAnakiaTask(
//     "buildPrintableDoc", baseDir = xdocs,
//     style = "stylesheets/site_printable.vsl",
//     velocityProperties = "$xdocs/velocity.properties",
//     projectFile = "stylesheets/printable_project.xml",
//     excludes = arrayOf("**/stylesheets/**", "extending.xml", "extending/*.xml"),
//     includes = arrayOf("**/*.xml")
// )

//val previewPrintableDocs by tasks.registering(Copy::class) {
//    group = JavaBasePlugin.DOCUMENTATION_GROUP
//    description = "Creates preview of a printable documentation to build/docs/printable_preview"
//    into("$buildDir/docs/printable_preview")
//    CrLfSpec().run {
//        gitattributes(gitProps)
//        printableDocumentation()
//    }
//}
//
//val lastEditYear: String by rootProject.extra
//
//fun xslt(
//    subdir: String,
//    outputDir: String,
//    includes: Array<String> = arrayOf("*.xml"),
//    excludes: Array<String> = arrayOf("extending.xml")
//) {
//
//    val relativePath = if (subdir.isEmpty()) "." else ".."
//    ant.withGroovyBuilder {
//        "xslt"("style" to "$xdocs/stylesheets/website-style.xsl",
//            "basedir" to "$xdocs/$subdir",
//            "destdir" to "$outputDir/$subdir",
//            "excludes" to excludes.joinToString(" "),
//            "includes" to includes.joinToString(" ")
//        ) {
//            "param"("name" to "relative-path", "expression" to relativePath)
//            "param"("name" to "subdir", "expression" to subdir)
//            "param"("name" to "year", "expression" to lastEditYear)
//        }
//    }
//}
//
//val processSiteXslt by tasks.registering {
//    val outputDir = "$buildDir/siteXslt"
//    inputs.files(xdocs)
//    inputs.property("year", lastEditYear)
//    outputs.dir(outputDir)
//
//    doLast {
//        for (f in (outputs as TaskOutputsInternal).previousOutputFiles) {
//            f.delete()
//        }
//        for (i in arrayOf("", "usermanual", "localising")) {
//            xslt(i, outputDir)
//        }
//    }
//}
//
//fun CopySpec.siteLayout() {
//    // TODO: generate doap_JMeter.rdf
//    from("$xdocs/download_jmeter.cgi")
//    into("api") {
//        javadocs()
//    }
//    from(processSiteXslt)
//    docCssAndImages()
//    manuals()
//}
//
//// See https://github.com/gradle/gradle/issues/10960
//val previewSiteDir = buildDir.resolve("site")
//val previewSite by tasks.registering(Sync::class) {
//    group = JavaBasePlugin.DOCUMENTATION_GROUP
//    description = "Creates preview of a site to build/docs/site"
//    into(previewSiteDir)
//    CrLfSpec().run {
//        gitattributes(gitProps)
//        siteLayout()
//    }
//}
//
val distributionGroup = "distribution"
val baseFolder = "apache-jmeter-${rootProject.version}"

fun CopySpec.javadocs() = from(javadocAggregate)

fun CopySpec.excludeLicenseFromSourceRelease() {
    // Source release has "/licenses" folder with licenses for third-party dependencies
    // It is populated by "dependencyLicenses" above,
    // so we ignore the folder when building source releases
    exclude("licenses/**")
    exclude("LICENSE")
}

//
fun CrLfSpec.binaryLayout() = copySpec {
    //  gitattributes(gitProps)
    println("binary creation")

    into(baseFolder) {

        println("---->>>>" + baseFolder)
        // Note: license content is taken from "/build/..", so gitignore should not be used
        // Note: this is a "license + third-party licenses", not just Apache-2.0
        // Note: files(...) adds both "files" and "dependency"
        from(files(binLicense))
        from(rootDir) {
            //    gitignore(gitProps)
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
            with(libs)
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

//fun CrLfSpec.sourceLayout() = copySpec {
//    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
//    gitattributes(gitProps)
//    into(baseFolder) {
//        // Note: license content is taken from "/build/..", so gitignore should not be used
//        // Note: this is a "license + third-party licenses", not just Apache-2.0
//        // Note: files(...) adds both "files" and "dependency"
//        from(files(srcLicense))
//        // Include all the source files
//        from(rootDir) {
//            gitignore(gitProps)
//            excludeLicenseFromSourceRelease()
//        }
//    }
//}
//
val javadocAggregate by tasks.registering(Javadoc::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Generates aggregate javadoc for all the artifacts"

    val sourceSets = jars.map { project(it).sourceSets.main }
//
    classpath = files(sourceSets.map { set -> set.map { it.output + it.compileClasspath } })
//    // Aggregate javadoc needs to include generated JMeterVersion class
//    // So we use delay computation of source files
    setSource(sourceSets.map { set -> set.map { it.allJava } })
    setDestinationDir(file("$buildDir/docs/javadocAggregate"))
}
//
val skipDist: Boolean by rootProject.extra
//
//// Generates distZip, distTar, distZipSource, and distTarSource tasks
//// The archives and checksums are put to build/distributions
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
            description = "Creates $type distribution with TODO "// $eol line endings for text files"
            if (this is Tar) {
                compression = Compression.GZIP
            }
            // Gradle does not track "filters" as archive/copy task dependencies,
            // So a mere change of a file attribute won't trigger re-execution of a task
            // So we add a custom property to re-execute the task in case attributes change
            //inputs.property("gitproperties", gitProps.map { it.props.attrs.toString() })

            // Gradle defaults to the following pattern, and JMeter was using apache-jmeter-5.1_src.zip
            // [baseName]-[appendix]-[version]-[classifier].[extension]
            archiveBaseName.set("apache-jmeter-${rootProject.version}${if (type == "source") "_src" else ""}")
            // Discard project version since we want it to be added before "_src"
            archiveVersion.set("")
            CrLfSpec(eol).run {
                //wa1191SetInputs(gitProps)
                //  with(if
                //          (type == "source")
                //      sourceLayout()
                //
                //
                //  else
                binaryLayout()
            }
        }
        // releaseArtifacts {
        //     artifact(archiveTask)
        // }
    }
}
//

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

// Куда скачиваем ZIP
val downloadDir = File(buildDir, "downloaded")
val zipFile = File(downloadDir, artifactFileName)

tasks.register<Download>("downloadArtifactZip") {
    description = "Скачивает ZIP файл из Nexus raw репозитория"

    src(nexusDownloadUrl)

    // Куда сохраняем
    dest(zipFile)

    // Настройки для 4.1.2
    overwrite(false)           // не качать если уже есть
    quiet(false)              // показывать прогресс
    connectTimeout(30000)     // 30 сек
    readTimeout(30000)        // 30 сек

    // Аутентификация
    username(MVN_USER)
    password(MVN_PASS)

    // Умное скачивание (доступно в 4.1.2!)
    onlyIfModified(true)
    useETag(true)

    // Создаем папку для скачивания
    doFirst {
        downloadDir.mkdirs()
    }
}

tasks.register<Copy>("unzipArtifact") {

    description = "Распаковывает ZIP "
    dependsOn("downloadArtifactZip")
    // Берем ZIP и распаковываем
    from(zipTree(zipFile))
    // ПРЯМО В ПАПКУ РЕСУРСОВ В КОРНЕ ПРОЕКТА
    into(resourcesDir)
    // Перезаписываем существующие файлы
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    // Не копируем пустые папки
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
    // Если знаете чексумму - раскомментируйте:
    // checksum("your-md5-hash-here")
}

//creates artifact to load in nexus
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

// Принудительная перезагрузка
tasks.register<Download>("forceDownloadArtifact") {
    description = "Принудительно скачивает ZIP заново"

    src(urlSite)
    dest(zipFile)
    overwrite(true) // ПРИНУДИТЕЛЬНО перезаписываем
    username(MVN_USER)
    password(MVN_PASS)

    doFirst {
        logger.lifecycle("warning Принудительное скачивание: $urlSite")
    }
}

// Очистка ресурсов
tasks.register<Delete>("cleanResources") {
    description = "Удаляет распакованные ресурсы"
    delete(resourcesDir)
}

// Очистка скачанных файлов
tasks.register<Delete>("cleanDownloads") {
    description = "Удаляет скачанные ZIP файлы"
    delete(downloadDir)
}

// Полный цикл подготовки
tasks.register("prepareBundle") {
    description = "Скачивает, проверяет и распаковывает бандл"
    dependsOn("downloadArtifactZip", "verifyDownload", "unzipArtifact")
    group = "bundle"
}

// Привязка к стандартным задачам
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
            //artifactId = "nt_master"
            //artifact( tasks.getByName("distTar"))
            //lib // extras //xdocs //bin
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
        // Pass -Djmeter.* and -Ddarklaf.* properties to the JMeter process
        if (e.startsWith("jmeter.") || e.startsWith("darklaf.")) {
            passProperty(e)
        }
        if (e == "darklaf.native") {
            systemProperty("darklaf.decorations", "true")
            systemProperty("darklaf.allowNativeCode", "true")
        }
    }
}

