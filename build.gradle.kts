plugins {
  kotlin("jvm") version "1.9.22"
  id("com.gradleup.shadow") version "8.3.0"
  id("xyz.jpenilla.run-paper") version "2.3.1"
  `maven-publish`
}

allprojects {
  group = "ru.hukm"
  version = "1.0-SNAPSHOT"

  repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
    maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }
  }
}

subprojects {
  apply(plugin = "org.jetbrains.kotlin.jvm")

  dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
  }

  kotlin { jvmToolchain(21) }
}

dependencies {
  implementation(project(":core"))
  implementation(project(":mcv:v1_21_9"))
  implementation(project(":mcv:v1_21_11"))
}
// TODO(дочерний плагин (без all) не видит релокацию)

tasks {
  shadowJar {
    archiveClassifier.set("all")

    // Релокация
    relocate("kotlin", "ru.hukm.effectiveSpigot.libs.kotlin")
    relocate("org.jetbrains", "ru.hukm.effectiveSpigot.libs.org.jetbrains")

    mergeServiceFiles()

    // Помогаем Gradle понять, что дубликаты — это не всегда плохо
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
  }

  jar {
    enabled = true
    archiveClassifier.set("dev")

    // Собираем классы и ресурсы из всех подпроектов
    val subprojectsOutputs = subprojects.map { it.sourceSets.main.get().output }
    from(subprojectsOutputs)

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
          from(project(":core").file("src/main/kotlin/ru/hukm"))
          into(rootProject.file("/home/hukm/pluginLibs"))
          duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }

// Финальная цепочка
tasks.build { finalizedBy(copySourceToDecomp, "publishToMavenLocal", copyJarToServer) }