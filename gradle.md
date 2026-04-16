# Gradle: команды и таски

В примерах используется `./gradlew`. Если установлен [gdub](https://github.com/dougborg/gdub), можно использовать сокращение `gw`.

## Сборка и запуск

```sh
# Собрать дистрибутив и запустить GUI НТ Мастер
./gradlew runGui

# Собрать дистрибутив, разложить JAR в lib/ и bin/, запустить
./gradlew createDist && ./bin/jmeter

# Собрать все дистрибутивы (исходники + бинарник)
./gradlew :src:dist:assemble
```

## Nexus RAW bundle

```sh
# Скачать bundle (lib/ + bin/ + extras/) из Nexus RAW и разложить по местам
./gradlew :src:dist:pullLibsBundle

# Упаковать текущие lib/ + bin/ + extras/ и загрузить в Nexus RAW
./gradlew :src:dist:pushLibsBundle
```

## Публикация в Nexus Maven

```sh
# Собрать ZIP-артефакт и опубликовать в Nexus Maven (snapshots или releases)
./gradlew :src:dist:publishAll

# Полный цикл релиза: distZip + publishAll
./gradlew :src:dist:release
```

## Информация о проекте

```sh
# Показать все подмодули
./gradlew projects

# Показать доступные таски текущего модуля
./gradlew tasks
```

## Очистка

Технически `clean` не должен требоваться при обычной разработке. Если он нужен — скорее всего, это признак бага в сборке.

```sh
# Очистить текущий проект
./gradlew clean

# Очистить конкретный субпроект
./gradlew :src:core:clean
```

## Зависимости

```sh
# Показать дерево зависимостей
./gradlew dependencies

# Показать зависимости всех проектов
./gradlew allDependencies

# Анализ: почему проект зависит от org.ow2.asm:asm
./gradlew dependencyInsight --dependency org.ow2.asm:asm

# Обновить ожидаемые контрольные суммы после смены версии зависимости
./gradlew -PupdateExpectedJars check
```

## Статический анализ

```sh
# Запустить Checkstyle для основного кода
./gradlew checkstyleMain

# Запустить Checkstyle для тестового кода
./gradlew checkstyleTest

# Запустить все проверки стиля
./gradlew checkstyleAll

# Проверить форматирование через Spotless
./gradlew spotlessCheck

# Исправить форматирование через Spotless
./gradlew spotlessApply

# Запустить Spotless + Checkstyle одной командой
./gradlew style
```

## Компиляция

```sh
./gradlew compileJava
./gradlew compileTestJava
```

## Сборка

```sh
# Собрать JAR (результат в build/libs/*.jar)
./gradlew jar

# Полная сборка со всеми проверками
./gradlew build

# Сборка без тестов
./gradlew build -x test

# Параллельная сборка
./gradlew build --parallel
```

## Тесты

Gradle автоматически отслеживает зависимости задач. Если изменить файл в `:src:jorphan`, можно запустить `check` в корне или в `core` — Gradle сам пересоберёт только нужные JAR.

```sh
# Запустить все тесты (unit-тесты, checkstyle и т.д.)
./gradlew check

# Только unit-тесты
./gradlew test

# Тесты конкретного субпроекта
./gradlew :src:core:test
```

## Покрытие кода

```sh
# Отчёт покрытия для task test → build/reports/jacoco/test/html
./gradlew jacocoTestReport

# Агрегированный отчёт покрытия
./gradlew jacocoReport
```

## Javadoc

```sh
# Сгенерировать Javadoc → build/docs/javadoc
./gradlew javadoc

# Сгенерировать Javadoc JAR → build/libs/*-javadoc.jar
./gradlew javadocJar
```

## Maven

```sh
# Опубликовать Maven-артефакт в локальный репозиторий
./gradlew publishToMavenLocal

# Сгенерировать pom-файлы (результат в src/**/build/publications/)
./gradlew generatePom
```
