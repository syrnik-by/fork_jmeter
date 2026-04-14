/*
 * nexus-raw.gradle.kts
 *
 * Работа с Nexus RAW репозиторием (jmeter-raw).
 *
 * Хранит бандл lib/ + bin/ (скрипты, конфиги, сторонние JAR) —
 * всё то, что не генерируется из исходников и не должно лежать в VCS.
 *
 * Без версионности: бандл всегда называется jmeter-libs.zip.
 * При обновлении просто перезаписывается в Nexus через pushLibsBundle.
 *
 * Таски:
 *   pullLibsBundle  — скачать из Nexus RAW и разложить lib/ + bin/ по местам
 *   pushLibsBundle  — упаковать текущие lib/ + bin/ и загрузить в Nexus RAW
 *
 * Сценарии:
 *   Разработчик хочет поправить скрипт или JAR в bin/lib:
 *     ./gradlew :src:dist:pullLibsBundle   # получить актуальный бандл
 *     ... правки ...
 *     ./gradlew :src:dist:pushLibsBundle   # сохранить обратно
 *
 *   Пайплайн / runGui / assembleArtifact:
 *     pullLibsBundle вызывается автоматически через dependsOn.
 */

import de.undercouch.gradle.tasks.download.Download
import de.undercouch.gradle.tasks.download.Verify
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files

// ── Координаты Nexus RAW (из gradle.properties) ───────────────────────────────
val nexusRawRepoUrl: String by project   // http://nexus:8081/repository/jmeter-raw/
val mavenUsername: String by project
val mavenPassword: String by project

val MVN_USER = System.getenv("MVN_USER") ?: mavenUsername
val MVN_PASS  = System.getenv("MVN_PASS")  ?: mavenPassword

// Без версионности — всегда один и тот же файл, перезаписывается при push.
val BUNDLE_NAME = "jmeter-libs.zip"
val bundleUrl   = "${nexusRawRepoUrl}${BUNDLE_NAME}"

val downloadDir    = File(buildDir, "downloaded")
val unpackedDir    = File(buildDir, "unpacked")
val bundleFile     = File(downloadDir, BUNDLE_NAME)
val bundleStageDir = File(buildDir, "bundle-stage")  // staging для pushLibsBundle

// ── pullLibsBundle ────────────────────────────────────────────────────────────

tasks.register<Download>("downloadLibsBundle") {
    description = "[internal] Скачивает бандл jmeter-libs.zip из Nexus RAW"
    src(bundleUrl)
    dest(bundleFile)
    overwrite(false)
    quiet(false)
    onlyIfModified(true)
    useETag(true)
    username(MVN_USER)
    password(MVN_PASS)
    connectTimeout(30_000)
    readTimeout(60_000)
    doFirst { downloadDir.mkdirs() }
}

tasks.register<Copy>("unpackLibsBundle") {
    description = "[internal] Распаковывает бандл в buildDir/unpacked"
    dependsOn("downloadLibsBundle")
    from(zipTree(bundleFile))
    into(unpackedDir)
    duplicatesStrategy = DuplicatesStrategy.OVERWRITE
    includeEmptyDirs = false
    doFirst { logger.lifecycle("Распаковка ${bundleFile.name} -> ${unpackedDir.path}") }
    doLast  { logger.lifecycle("Распаковано в: ${unpackedDir.path}") }
}

tasks.register<Copy>("copyBundleToRoot") {
    description = "[internal] Копирует lib/ и bin/ из unpacked в корень проекта"
    dependsOn("unpackLibsBundle")
    from(unpackedDir) {
        include("bin/**")
        include("lib/**")
    }
    into(rootDir)
    duplicatesStrategy = DuplicatesStrategy.OVERWRITE
    doFirst { logger.lifecycle("Копирование бандла -> ${rootDir.path}") }
    doLast  { logger.lifecycle("Бандл разложен по местам") }
}

// Агрегирующая таска — именно её вызывают другие таски через dependsOn
tasks.register("pullLibsBundle") {
    group = "bundle"
    description = "Скачивает бандл из Nexus RAW и раскладывает lib/ + bin/ по местам"
    dependsOn("downloadLibsBundle", "unpackLibsBundle", "copyBundleToRoot")
    doLast { logger.lifecycle("pullLibsBundle завершён") }
}

// ── pushLibsBundle ────────────────────────────────────────────────────────────

tasks.register<Zip>("packLibsBundle") {
    description = "[internal] Упаковывает текущие lib/ + bin/ в ZIP для загрузки в Nexus RAW"
    archiveFileName.set(BUNDLE_NAME)
    destinationDirectory.set(bundleStageDir)
    // Берём ВСЁ из bin/ и lib/ — скрипты, конфиги, JAR
    from(rootDir) {
        include("bin/**")
        include("lib/**")
    }
    doFirst {
        bundleStageDir.mkdirs()
        logger.lifecycle("Упаковка bin/ + lib/ в ${BUNDLE_NAME}")
    }
    doLast {
        logger.lifecycle("Бандл готов: ${bundleStageDir.resolve(BUNDLE_NAME).path}")
    }
}

tasks.register("pushLibsBundle") {
    group = "bundle"
    description = "Упаковывает lib/ + bin/ и загружает бандл в Nexus RAW (перезаписывает)"
    dependsOn("packLibsBundle")
    doLast {
        val file = bundleStageDir.resolve(BUNDLE_NAME)
        require(file.exists()) { "Бандл не найден: ${file.path}" }

        logger.lifecycle("Загрузка ${file.name} (${file.length() / 1024} KB) -> $bundleUrl")

        val url = URL(bundleUrl)
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "PUT"
            conn.doOutput = true
            conn.connectTimeout = 30_000
            conn.readTimeout    = 120_000
            conn.setRequestProperty("Content-Type", "application/zip")
            conn.setRequestProperty("Content-Length", file.length().toString())

            // Basic Auth
            val creds = java.util.Base64.getEncoder()
                .encodeToString("$MVN_USER:$MVN_PASS".toByteArray())
            conn.setRequestProperty("Authorization", "Basic $creds")

            Files.copy(file.toPath(), conn.outputStream)
            conn.outputStream.flush()

            val code = conn.responseCode
            if (code in 200..299) {
                logger.lifecycle("Загрузка успешна (HTTP $code)")
            } else {
                val body = conn.errorStream?.bufferedReader()?.readText() ?: ""
                throw GradleException("Загрузка завершилась с ошибкой HTTP $code: $body")
            }
        } finally {
            conn.disconnect()
        }
    }
}

// ── Clean ─────────────────────────────────────────────────────────────────────
tasks.named(BasePlugin.CLEAN_TASK_NAME).configure {
    doLast {
        delete(downloadDir)
        delete(unpackedDir)
        delete(bundleStageDir)
        // Чистим только JAR в lib/ext (сгенерированные buildом),
        // не трогаем скрипты и конфиги в bin/ и lib/
        delete(fileTree("$rootDir/lib") { include("*.jar") })
        delete(fileTree("$rootDir/lib/ext") { include("*.jar") })
        delete(fileTree("$rootDir/bin") { include("ApacheJMeter.jar") })
    }
}
