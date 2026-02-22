plugins {
    kotlin("jvm") version "1.9.22"
    id("com.gradleup.shadow") version "8.3.0"
    id("xyz.jpenilla.run-paper") version "2.3.1"
    `maven-publish`
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifact(tasks.named("shadowJar"))
        }
    }
}

tasks.withType<PublishToMavenLocal> {
    dependsOn(tasks.named("jar"))
    dependsOn(tasks.named("shadowJar"))
}

allprojects {
    group = "ru.hukm"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenLocal()
        mavenCentral()
        maven {
            url = uri("https://repo.papermc.io/repository/maven-public/")
        }
        maven {
            url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        }
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    dependencies {
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    }

    val targetJavaVersion = 21
    kotlin {
        jvmToolchain(targetJavaVersion)
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":mcv:v1_21_9"))
    implementation(project(":mcv:v1_21_11"))
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        // Релокация зависимостей если нужно
    }

    runServer {
        minecraftVersion("1.21")
    }

    build {
        dependsOn(shadowJar)
    }
}

val copyJarToServer = tasks.register<Copy>("copyJarToServer") {
    dependsOn("shadowJar")
    from(tasks.named("shadowJar"))
    into("/mnt/ssd/перенос/server/plugins/")
}

val copySourceToDecomp = tasks.register<Copy>("copySourceToDecomp") {
    from("core/src/main/kotlin/ru")
    into("../jarToTxt/decomp")
}

tasks.build {
    finalizedBy(copyJarToServer, copySourceToDecomp, "publishToMavenLocal")
}

tasks.named("publishToMavenLocal") {
    dependsOn("shadowJar")
}
