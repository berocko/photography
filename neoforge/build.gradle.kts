import net.fabricmc.loom.task.AbstractRunTask

fun Project.hasProp(namespace: String, key: String) = hasProperty("$namespace.$key")
fun Project.prop(namespace: String, key: String) = property("$namespace.$key") as String

val devClient by sourceSets.creating {
    compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
    runtimeClasspath += output + compileClasspath + sourceSets.main.get().runtimeClasspath
}

architectury {
    platformSetupLoomIde()
    neoForge()
}

loom {
    accessWidenerPath = project(":common").loom.accessWidenerPath
    runConfigs.all { ideConfigGenerated(false) }
    runConfigs["client"].runDir = "../run"
    runConfigs["server"].runDir = "../run/server"

    createRemapConfigurations(devClient)
}

val common: Configuration by configurations.creating {
    configurations.compileClasspath.get().extendsFrom(this)
    configurations.runtimeClasspath.get().extendsFrom(this)
    configurations.getByName("developmentNeoForge").extendsFrom(this)
}

repositories {
    maven("https://maven.neoforged.net/releases/")
    maven("https://maven.tr7zw.dev/repository/maven-public/") { content { includeGroup("dev.tr7zw") } }
    ivy("https://cdn.modrinth.com/data") {
        patternLayout { artifact("[organisation]/versions/[revision]/[module](-[classifier]).[ext]") }
        metadataSources { artifact() }
    }
}

dependencies {
    "neoForge"("net.neoforged:neoforge:${rootProject.prop("neoforge", "version")}")

    if (rootProject.prop("devMods", "enabled").toBoolean()) {
        "modDevClientRuntimeOnly"("u6dRKJwZ:jei-1.21.1-neoforge-19.39.0.368:${rootProject.prop("devMods.neoforge", "jei")}")
        "modDevClientRuntimeOnly"("aC3cM3Vq:MouseTweaks-neoforge-mc1.21-2.26.1:${rootProject.prop("devMods.neoforge", "mouseTweaks")}")
        "modDevClientRuntimeOnly"("9s6osm5g:cloth-config-15.0.140-neoforge:${rootProject.prop("devMods.neoforge", "clothConfig")}")
        "modDevClientRuntimeOnly"("nvQzSEkH:Jade-1.21.1-NeoForge-15.10.5:${rootProject.prop("devMods.neoforge", "jade")}")
        "modDevClientRuntimeOnly"("H5XMjpHi:firstperson-neoforge-2.7.2-mc1.21.1:${rootProject.prop("devMods.neoforge", "firstPersonModel")}")
        "modDevClientRuntimeOnly"("MPCX6s5C:notenoughanimations-neoforge-1.12.4-mc1.21.1:${rootProject.prop("devMods.neoforge", "notEnoughAnimations")}")
        "modDevClientRuntimeOnly"("dev.tr7zw:TRansition:${rootProject.prop("devMods", "transition")}-1.21.1-neoforge-SNAPSHOT@jar")
        "modDevClientRuntimeOnly"("dev.tr7zw:TRender:${rootProject.prop("devMods", "trender")}-1.21.1-neoforge-SNAPSHOT@jar")
    }

    include("dev.matrixlab:webp4j:1.3.0")
    forgeRuntimeLibrary("dev.matrixlab:webp4j:1.3.0")

    common(project(":common", "namedElements")) { isTransitive = false }
    shadowCommon(project(":common", "transformProductionNeoForge")) { isTransitive = false }
}

tasks.named<AbstractRunTask>("runClient") {
    if (rootProject.prop("devMods", "enabled").toBoolean()) {
        classpath(devClient.runtimeClasspath)
    }
}

tasks.processResources {
    from(project(":common").sourceSets.map { it.resources })

    // We construct our minecraft dependency string based on the versions provided in gradle.properties
    val gameVersions = rootProject.prop("platform", "versions").split(",")
    val first = gameVersions.firstOrNull()!!
    val last = gameVersions.lastOrNull()!!
    val minecraftDependency = if (gameVersions.size == 1) "[$first]" else "[$first, $last]"

    // For neoforge.mods.toml and pack.mcmeta, we source some properties from gradle.properties.
    filesMatching(listOf("META-INF/neoforge.mods.toml", "pack.mcmeta")) {
        expand(
            "modName" to rootProject.prop("mod", "name"),
            "version" to rootProject.prop("mod", "version"),
            "minecraftDependency" to minecraftDependency,
        )
    }
}
