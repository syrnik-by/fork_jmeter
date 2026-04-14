/*
 * local-dist.gradle.kts
 *
 * Сборка локального дистрибутива JMeter из исходников.
 *
 * Берёт артефакты fork-модулей (:src:core, :src:functions, и т.д.),
 * upstream JMeter JAR и плагины — и раскладывает их по lib/ и bin/.
 *
 * Таски:
 *   createDist  — главная таска: кладёт все JAR в lib/ и bin/
 *
 * Используется как зависимость в:
 *   assembleArtifact, runGui, dist*Zip/Tar (distribution archives)
 *
 * ВАЖНО: configurations, dependencies и pluginClasspath объявлены
 * в build.gradle.kts (оркестратор) — сюда не переносить!
 */

import com.github.vlsi.gradle.crlf.CrLfSpec
import com.github.vlsi.gradle.crlf.LineEndings

// ── CopySpecs ─────────────────────────────────────────────────────────────────
// Три корзины: lib/ | lib/ext/ | bin/
// Заполняются в populateLibs, затем используются в copyLibs/copyBinLibs.
val libsSpec = copySpec {}
val libsExt  = copySpec {}
val binLibs  = copySpec {}

// ── populateLibs ──────────────────────────────────────────────────────────────
// Разбирает resolvedArtifacts и раскладывает по трём корзинам.
// Плагины идут в libsExt (lib/ext), остальное — в libsSpec (lib/).
val populateLibs by tasks.registering {
    dependsOn(configurations.runtimeClasspath)
    doLast {
        val launcherProject  = project(":src:launcher").path
        val bshclientProject = project(":src:bshclient").path
        val jorphanProject   = project(":src:jorphan").path

        // pluginModuleIds доступен через extra, выставленный в build.gradle.kts
        // Используем pluginClasspath напрямую — конфигурация объявлена там же
        val pluginModuleIds: Set<String> = pluginClasspath
            .resolvedConfiguration.resolvedArtifacts
            .map { "${it.moduleVersion.id.group}:${it.moduleVersion.id.name}" }
            .toSet()

        val deps = configurations.runtimeClasspath.get().resolvedConfiguration.resolvedArtifacts
        for (dep in deps) {
            logger.debug("dep --> $dep")
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
                launcherProject              -> binLibs
                jorphanProject,
                bshclientProject             -> libsSpec
                else                         -> libsExt
            }.from(dep.file) {
                rename { dep.name + "." + dep.extension }
            }
        }

        // Плагины: resolvedArtifacts из pluginClasspath → lib/ext
        val pluginArtifacts = pluginClasspath.resolvedConfiguration.resolvedArtifacts
        for (dep in pluginArtifacts) {
            logger.debug("plugin --> $dep")
            val compId   = dep.id.componentIdentifier
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

// ── copyLibs ──────────────────────────────────────────────────────────────────
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

// ── copyBinLibs ───────────────────────────────────────────────────────────────
val copyBinLibs by tasks.registering(Copy::class) {
    rootSpec.into("$rootDir/bin")
    with(binLibs)
}

// ── createDist ────────────────────────────────────────────────────────────────
// Главная таска этого скрипта.
val createDist by tasks.registering {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Собирает fork JAR и раскладывает их в lib/ и bin/"
    dependsOn(copyLibs, copyBinLibs)
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

fun CopySpec.printableDocumentation() {
    into("docs") { docCssAndImages() }
}

fun CopySpec.excludeLicenseFromSourceRelease() {
    exclude("licenses/**")
    exclude("LICENSE")
}

val distributionGroup = "distribution"
val baseFolder = "apache-jmeter-${rootProject.version}"

// ── Javadoc aggregate ─────────────────────────────────────────────────────────
val javadocAggregate by tasks.registering(Javadoc::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Генерирует агрегированный Javadoc для всех fork-модулей"
    // jars объявлен в build.gradle.kts
    val sourceSets = jars.map { project(it).sourceSets.main }
    classpath = files(sourceSets.map { set -> set.map { it.output + it.compileClasspath } })
    setSource(sourceSets.map { set -> set.map { it.allJava } })
    setDestinationDir(file("$buildDir/docs/javadocAggregate"))
}

fun CopySpec.javadocs() = from(javadocAggregate)

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

// ── Distribution archives (Zip + Tar) ─────────────────────────────────────────
val skipDist: Boolean by rootProject.extra

for (type in listOf("binary", "source")) {
    if (skipDist) break
    for (archive in listOf(Zip::class, Tar::class)) {
        val taskName = "dist${archive.simpleName}${type.replace("binary", "").capitalize()}"
        tasks.register(taskName, archive) {
            dependsOn(createDist)
            val eol = if (archive == Tar::class) LineEndings.LF else LineEndings.CRLF
            group = distributionGroup
            description = "Создаёт $type дистрибутив"
            if (this is Tar) compression = Compression.GZIP
            archiveBaseName.set(
                "apache-jmeter-${rootProject.version}${if (type == "source") "_src" else ""}"
            )
            archiveVersion.set("")
            CrLfSpec(eol).run { binaryLayout() }
        }
    }
}
