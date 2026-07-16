import net.fabricmc.loom.task.AbstractRunTask

fun Project.hasProp(namespace: String, key: String) = hasProperty("$namespace.$key")
fun Project.prop(namespace: String, key: String) = property("$namespace.$key") as String

val devClient by sourceSets.creating {
    compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
    runtimeClasspath += output + compileClasspath + sourceSets.main.get().runtimeClasspath
}

architectury {
    platformSetupLoomIde()
    fabric()
}

loom {
    accessWidenerPath = project(":common").loom.accessWidenerPath
    runConfigs.all { ideConfigGenerated(false) }
    runConfigs["client"].runDir = "../run"
    runConfigs["server"].runDir = "../run/server"

    @Suppress("UnstableApiUsage")
    mixin.useLegacyMixinAp = false

    createRemapConfigurations(devClient)
}

val common: Configuration by configurations.creating {
    configurations.compileClasspath.get().extendsFrom(this)
    configurations.runtimeClasspath.get().extendsFrom(this)
    configurations.getByName("developmentFabric").extendsFrom(this)
}

repositories {
    maven("https://maven.terraformersmc.com/releases/") { content { includeGroup("com.terraformersmc") } }
    maven("https://maven.tr7zw.dev/repository/maven-public/") { content { includeGroup("dev.tr7zw") } }
    ivy("https://cdn.modrinth.com/data") {
        patternLayout { artifact("[organisation]/versions/[revision]/[module](-[classifier]).[ext]") }
        metadataSources { artifact() }
    }
}

dependencies {
    modImplementation("net.fabricmc:fabric-loader:${rootProject.prop("fabric", "loaderVersion")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${rootProject.prop("fabric", "apiVersion")}")

    modCompileOnly("com.terraformersmc:modmenu:${rootProject.prop("modmenu", "version")}")

    if (rootProject.prop("devMods", "enabled").toBoolean()) {
        "modDevClientRuntimeOnly"("u6dRKJwZ:jei-1.21.1-fabric-19.39.0.368:${rootProject.prop("devMods.fabric", "jei")}")
        "modDevClientRuntimeOnly"("aC3cM3Vq:MouseTweaks-fabric-mc1.21-2.26:${rootProject.prop("devMods.fabric", "mouseTweaks")}")
        "modDevClientRuntimeOnly"("9s6osm5g:cloth-config-15.0.140-fabric:${rootProject.prop("devMods.fabric", "clothConfig")}")
        "modDevClientRuntimeOnly"("mOgUt4GM:modmenu-11.0.4:${rootProject.prop("devMods.fabric", "modMenu")}")
        "modDevClientRuntimeOnly"("eXts2L7r:placeholder-api-2.4.2+1.21:${rootProject.prop("devMods.fabric", "placeholderApi")}")
        "modDevClientRuntimeOnly"("nvQzSEkH:Jade-1.21.1-Fabric-15.10.5:${rootProject.prop("devMods.fabric", "jade")}")
        "modDevClientRuntimeOnly"("H5XMjpHi:firstperson-fabric-2.7.2-mc1.21.1:${rootProject.prop("devMods.fabric", "firstPersonModel")}")
        "modDevClientRuntimeOnly"("MPCX6s5C:notenoughanimations-fabric-1.12.4-mc1.21.1:${rootProject.prop("devMods.fabric", "notEnoughAnimations")}")
        "modDevClientRuntimeOnly"("dev.tr7zw:TRansition:${rootProject.prop("devMods", "transition")}-1.21.1-fabric-SNAPSHOT@jar")
        "modDevClientRuntimeOnly"("dev.tr7zw:TRender:${rootProject.prop("devMods", "trender")}-1.21.1-fabric-SNAPSHOT@jar")
    }

    include("dev.matrixlab:webp4j:1.3.0")
    implementation("dev.matrixlab:webp4j:1.3.0")

    common(project(":common", "namedElements")) { isTransitive = false }
    shadowCommon(project(":common", "transformProductionFabric")) { isTransitive = false }
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
    val minecraftDependency = if (gameVersions.size == 1) first else ">=$first <=$last"

    // For fabric.mod.json, we source some properties from gradle.properties.
    filesMatching("fabric.mod.json") {
        expand(
            "modName" to rootProject.prop("mod", "name"),
            "version" to rootProject.prop("mod", "version"),
            "minecraftDependency" to minecraftDependency,
        )
    }
}
