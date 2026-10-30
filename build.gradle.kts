import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.2.21"
    id("com.gradleup.shadow") version "8.3.5"
}

group = "dev.etran"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    // BedWars1058 is intentionally NOT a compile-time dependency: its API artifact is not published
    // anywhere reachable (repo.andrei1058.dev now redirects to an unrelated site). See BedWarsHook
    // for the reflection bridge.
}

dependencies {
    // Lowest common denominator API: written against 1.8.8, version-independent calls only.
    compileOnly("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT") { exclude(group = "net.md-5") }

    implementation(kotlin("stdlib"))
}

kotlin {
    // Java 8 bytecode so the same jar loads on 1.8 servers (Java 8) up to the newest (Java 25).
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
        freeCompilerArgs.add("-Xjdk-release=8")
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.withType<JavaCompile> {
    options.release.set(8)
}

tasks.processResources {
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filesMatching("plugin.yml") { expand(props) }
}

tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("")
    relocate("kotlin", "dev.etran.bedwars1058generatorfix.libs.kotlin")
    relocate("org.jetbrains", "dev.etran.bedwars1058generatorfix.libs.jetbrains")
    relocate("org.intellij", "dev.etran.bedwars1058generatorfix.libs.intellij")
    mergeServiceFiles()
}

// The plain jar has no Kotlin runtime; only the shaded jar should be installed.
tasks.jar {
    archiveClassifier.set("plain")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
