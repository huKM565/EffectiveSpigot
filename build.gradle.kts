plugins {
    kotlin("jvm") version "1.9.22"
    id("com.gradleup.shadow") version "8.3.0"
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.18"
    `maven-publish`
}

group = "ru.hukm"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    paperweight.paperDevBundle("1.21.6-R0.1-SNAPSHOT")
}

tasks {
    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion("1.21")
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()
        }
    }
}

val copyJarToServer = tasks.register<Copy>("copyJarToServer") {
    dependsOn("shadowJar")
    from(tasks.named("shadowJar"))
    into("/mnt/ssd/перенос/server/plugins/")
}

val copySourceToDecomp = tasks.register<Copy>("copySourceToDecomp") {
    from("src/main/kotlin/ru")
    into("../jarToTxt/decomp")
}

tasks.build {
    finalizedBy(copyJarToServer, copySourceToDecomp, "publishToMavenLocal")
}

tasks.named("publishToMavenLocal") {
    dependsOn("shadowJar")
}

val targetJavaVersion = 21
kotlin {
    jvmToolchain(targetJavaVersion)
}

tasks.build {
    dependsOn("shadowJar")
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}
