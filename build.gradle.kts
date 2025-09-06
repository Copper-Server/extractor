import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URL
import java.io.File

plugins {
    kotlin("jvm") version "2.1.10"
    id("fabric-loom") version "1.11-SNAPSHOT"
    id("maven-publish")
}

version = project.property("mod_version") as String
group = project.property("maven_group") as String

base {
    archivesName.set(project.property("archives_base_name") as String)
}

val targetJavaVersion = 21
java {
    toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    withSourcesJar()
}

loom {
    splitEnvironmentSourceSets()
    accessWidenerPath = file("src/main/resources/extractor.accesswidener")

    mods {
        register("extractor") {
            sourceSet("main")
        }
    }
}

repositories {
    // Add repositories to retrieve artifacts from in here.
    // You should only use this when depending on other mods because
    // Loom adds the essential maven repositories to download Minecraft and libraries from automatically.
    // See https://docs.gradle.org/current/userguide/declaring_repositories.html
    // for more information about repositories.
}

dependencies {
    // To change the versions see the gradle.properties file
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    mappings("net.fabricmc:yarn:${project.property("yarn_mappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${project.property("kotlin_loader_version")}")

    modImplementation("net.fabricmc.fabric-api:fabric-api:${project.property("fabric_version")}")

    implementation(kotlin("reflect"))
    implementation("dev.turingcomplete:text-case-converter:2.0.0")
    implementation("dev.turingcomplete:text-case-converter-kotlin-extension:2.0.0")
}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("minecraft_version", project.property("minecraft_version"))
    inputs.property("loader_version", project.property("loader_version"))
    filteringCharset = "UTF-8"

    filesMatching("fabric.mod.json") {
        expand(
            "version" to project.version,
            "minecraft_version" to project.property("minecraft_version"),
            "loader_version" to project.property("loader_version"),
            "kotlin_loader_version" to project.property("kotlin_loader_version")
        )
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(targetJavaVersion)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.fromTarget(targetJavaVersion.toString()))
}

// configure the maven publication
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = project.property("archives_base_name") as String
            from(components["java"])
        }
    }

    // See https://docs.gradle.org/current/userguide/publishing_maven.html for information on how to set up publishing.
    repositories {
        // Add repositories to publish to here.
        // Notice: This block does NOT have the same function as the block in the top level.
        // The repositories here will be used for publishing your artifact, not for
        // retrieving dependencies.
    }
}


val mcVersion = project.property("minecraft_version") as String
val runDir = File(rootDir, "run")
val serverJar = File(runDir, "minecraft-server-$mcVersion.jar")

tasks.register("downloadVanillaServerJar") {
    group = "datagen"
    description = "Downloads the official Minecraft server jar for the current version."
    outputs.file(serverJar)
    doLast {
        if (!runDir.exists()) runDir.mkdirs()
        if (!serverJar.exists()) {
            val manifestUrl = "https://launchermeta.mojang.com/mc/game/version_manifest.json"
            val manifest = URL(manifestUrl).readText()
            val versionRegex = Regex("\"id\"\\s*:\\s*\"$mcVersion\".*?\"url\"\\s*:\\s*\"(.*?)\"", RegexOption.DOT_MATCHES_ALL)
            val versionMatch = versionRegex.find(manifest)
            requireNotNull(versionMatch) { "Could not find Minecraft version $mcVersion in manifest." }
            val versionUrl = versionMatch.groupValues[1]
            val versionJson = URL(versionUrl).readText()
            val serverRegex = Regex("\"server\"\\s*:\\s*\\{[^}]*\"url\"\\s*:\\s*\"(.*?)\"", RegexOption.DOT_MATCHES_ALL)
            val serverMatch = serverRegex.find(versionJson)
            requireNotNull(serverMatch) { "Could not find server jar for $mcVersion." }
            val jarUrl = serverMatch.groupValues[1]
            println("Downloading Minecraft server jar for $mcVersion from $jarUrl")
            URL(jarUrl).openStream().use { input ->
                serverJar.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } else {
            println("Server jar already exists: $serverJar")
        }
    }
}

tasks.register<JavaExec>("runVanillaDatagen") {
    group = "datagen"
    description = "Runs Mojang's native data extractor using the downloaded server jar."
    dependsOn("downloadVanillaServerJar")
    classpath = files(serverJar)
    jvmArgs = listOf("-DbundlerMainClass=net.minecraft.data.Main")
    args = listOf("--all")
    workingDir = runDir
}
