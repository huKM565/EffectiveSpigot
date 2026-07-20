plugins {
  kotlin("jvm") version "2.2.0"
  id("com.gradleup.shadow") version "8.3.6"
  id("xyz.jpenilla.run-paper") version "2.3.1"
  `maven-publish`
}

group = "ru.hukm"

// Базовая версия без суффикса. По умолчанию сборка = снапшот;
// `./gradlew build -Prelease` = релиз (чистая версия, уходит в maven-releases).
val baseVersion = "1.0.1"
version = if (project.hasProperty("release")) baseVersion else "$baseVersion-SNAPSHOT"

val nexusUrl = (findProperty("nexusUrl") as String?) ?: "https://maven.hukm.dev"

repositories {
  mavenLocal()
  mavenCentral()
  maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
  maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }
  maven { url = uri("$nexusUrl/repository/maven-public/") }
}

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
  compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")

  implementation("com.github.shynixn.mccoroutine:mccoroutine-bukkit-api:2.22.0")
  implementation("com.github.shynixn.mccoroutine:mccoroutine-bukkit-core:2.22.0")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
  implementation("xyz.jpenilla:reflection-remapper:0.1.1")
}

kotlin { jvmToolchain(21) }
// TODO(дочерний плагин (без all) не видит релокацию)

tasks {
  shadowJar {
    archiveClassifier.set("all")

    // Релокация
    relocate("kotlinx", "ru.hukm.effectiveSpigot.libs.kotlinx")
    relocate("kotlin", "ru.hukm.effectiveSpigot.libs.kotlin")
    relocate("org.jetbrains", "ru.hukm.effectiveSpigot.libs.org.jetbrains")
    relocate(
        "com.github.shynixn.mccoroutine",
        "ru.hukm.effectiveSpigot.libs.com.github.shynixn.mccoroutine"
    )
    relocate("xyz.jpenilla.reflectionremapper", "ru.hukm.effectiveSpigot.libs.xyz.jpenilla.reflectionremapper")
    relocate("net.fabricmc.mappingio", "ru.hukm.effectiveSpigot.libs.net.fabricmc.mappingio")

    mergeServiceFiles()

    // Помогаем Gradle понять, что дубликаты — это не всегда плохо
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
  }

  jar {
    enabled = true
    archiveClassifier.set("dev")

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
  }

  runServer { minecraftVersion("1.21.11") }

  build { dependsOn(shadowJar) }
}

publishing {
  publications {
    create<MavenPublication>("maven") {
      // Публикуем именно dev-артефакт (без релокации)
      artifact(tasks.jar.get()) {
        classifier =
                "" // Убираем "dev" из имени в репозитории, чтобы дочерние плагины видели его как
        // основной
      }
    }
  }

  repositories {
    maven {
      name = "nexus"
      val isSnapshot = version.toString().endsWith("SNAPSHOT")
      url = uri(
          if (isSnapshot) "$nexusUrl/repository/maven-snapshots/"
          else "$nexusUrl/repository/maven-releases/"
      )

      credentials {
        username = (findProperty("nexusUsername") as String?) ?: System.getenv("NEXUS_USER")
        password = (findProperty("nexusPassword") as String?) ?: System.getenv("NEXUS_PASSWORD")
      }
    }
  }
}

// --- ИСПРАВЛЕННЫЕ ЗАДАЧИ ---

// 1. Копирование на сервер (с защитой от ошибок доступа к чужим файлам)
val copyJarToServer =
        tasks.register<Copy>("copyJarToServer") {
          dependsOn(tasks.shadowJar)

          // ВАЖНО: говорим Gradle не анализировать папку назначения на изменения.
          // Это уберет ошибку с MD5 хешами временных файлов Spark.
          doNotTrackState("Папка сервера содержит динамические файлы")

          val destFolder = file("/mnt/sda2/перенос/server/plugins/")

          from(tasks.shadowJar.get().archiveFile)
          into(destFolder)

          // На всякий случай игнорируем всё лишнее в папке назначения
          exclude("**/spark/**")
          exclude("**/*.tmp")
          exclude("**/*.jfr")

          doFirst {
            if (!destFolder.exists()) {
              throw GradleException("Папка сервера не найдена по пути: ${destFolder.absolutePath}")
            }
          }
        }

// 2. Копирование исходников
val copySourceToDecomp =
        tasks.register<Copy>("copySourceToDecomp") {
          from(file("src/main/kotlin/ru/hukm"))
          into(rootProject.file("/home/hukm/pluginLibs"))
          duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }

// Финальная цепочка
tasks.build { finalizedBy(copySourceToDecomp, "publishMavenPublicationToNexusRepository", copyJarToServer) }
