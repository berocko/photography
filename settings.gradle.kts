pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.fabricmc.net/") {
            content {
                includeGroup("fabric-loom")
                includeGroupByRegex("net\\.fabricmc(\\..*)?")
            }
        }
        maven("https://maven.architectury.dev") {
            content {
                includeGroup("architectury-plugin")
                includeGroupByRegex("dev\\.architectury(\\..*)?")
            }
        }
        maven("https://libraries.minecraft.net") {
            content {
                includeGroup("com.mojang")
            }
        }
        maven("https://maven.neoforged.net/releases/") {
            content {
                includeGroupByRegex("net\\.neoforged(\\..*)?")
                includeGroupByRegex("net\\.minecraftforge(\\..*)?")
                includeGroup("de.oceanlabs.mcp")
            }
        }
    }
}

include("common", "fabric", "neoforge")
