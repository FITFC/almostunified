@file:Suppress("UnstableApiUsage")

val modId: String by project
val modName: String by project
val junitVersion: String by project
val extraModsDirectory: String by project
val forgeRecipeViewer: String by project
val minecraftVersion: String by project
val forgeVersion: String by project
val reiVersion: String by project
val jeiVersion: String by project
val kubejsVersion: String by project
val mappingsChannel: String by project
val mappingsVersion: String by project

val baseArchiveName = "$modId-forge"
val commonTests: SourceSetOutput = project(":Common").sourceSets["test"].output

plugins {
    id("dev.architectury.loom") version "0.12.0-SNAPSHOT"
}

base {
    archivesName.set(baseArchiveName)
}

loom {
    silentMojangMappingsLicense()

    runs {
        named("client") {
            client()
            configName = "Forge Client"
            ideConfigGenerated(true)
            runDir("run")
            vmArgs("-XX:+IgnoreUnrecognizedVMOptions", "-XX:+AllowEnhancedClassRedefinition")
        }
        named("server") {
            server()
            configName = "Forge Server"
            ideConfigGenerated(true)
            runDir("run")
            vmArgs("-XX:+IgnoreUnrecognizedVMOptions", "-XX:+AllowEnhancedClassRedefinition")
        }
    }

    forge {
        mixinConfig("$modId-common.mixins.json")
    }

    mixin {
        defaultRefmapName.set("$modId.refmap.json")
    }
}

dependencies {
    compileOnly(project(":Common", "namedElements")) { isTransitive = false }

    compileOnly("com.google.auto.service:auto-service:1.0.1")
    annotationProcessor("com.google.auto.service:auto-service:1.0.1")

    minecraft("com.mojang:minecraft:$minecraftVersion")
    forge("net.minecraftforge:forge:$minecraftVersion-$forgeVersion")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:$mappingsChannel-$minecraftVersion:$mappingsVersion@zip")
    })

    // required for common rei plugin | api does not work here
    modCompileOnly("me.shedaniel:RoughlyEnoughItems-forge:$reiVersion")
    // required for common jei plugin and mixin, transitivity is off because it breaks the forge runtime
    modCompileOnly("mezz.jei:jei-$minecraftVersion-forge:$jeiVersion") { isTransitive = false }

    // runtime only
    when (forgeRecipeViewer) {
        "rei" -> modLocalRuntime("me.shedaniel:RoughlyEnoughItems-forge:$reiVersion")
        "jei" -> modLocalRuntime("mezz.jei:jei-$minecraftVersion-forge:$jeiVersion") { isTransitive = false }
        else -> throw GradleException("Invalid forgeRecipeViewer value: $forgeRecipeViewer")
    }

    // required for common kubejs plugin and forge runtime
    modCompileOnly(modLocalRuntime("dev.latvian.mods:kubejs-forge:$kubejsVersion")!!)

    fileTree("$extraModsDirectory-$minecraftVersion") { include("**/*.jar") }
        .forEach { f ->
            val sepIndex = f.nameWithoutExtension.lastIndexOf('-')
            if (sepIndex == -1) {
                throw IllegalArgumentException("Invalid mod name: ${f.nameWithoutExtension}")
            }
            val mod = f.nameWithoutExtension.substring(0, sepIndex)
            val version = f.nameWithoutExtension.substring(sepIndex + 1)
            println("Extra mod $mod with version $version detected")
            modLocalRuntime("$extraModsDirectory:$mod:$version")
        }

    // JUnit Tests
    testImplementation(project(":Common"))
    testImplementation(commonTests)
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
}

tasks {
    processResources {
        from(project(":Common").sourceSets.main.get().resources)
    }
    withType<JavaCompile> {
        source(project(":Common").sourceSets.main.get().allSource)
    }
}

publishing {
    publications {
        register("mavenJava", MavenPublication::class) {
            artifactId = baseArchiveName
            from(components["java"])
        }
    }

    repositories {
        maven("file://${System.getenv("local_maven")}")
    }
}
