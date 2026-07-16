fun Project.hasProp(namespace: String, key: String) = hasProperty("$namespace.$key")
fun Project.prop(namespace: String, key: String) = property("$namespace.$key") as String

architectury {
    common("fabric", "neoforge")
}

loom {
    accessWidenerPath.set(file("src/main/resources/camerapture.accesswidener"))
    splitEnvironmentSourceSets()

    @Suppress("UnstableApiUsage")
    mixin.useLegacyMixinAp = false
}

repositories {
    maven("https://maven.shedaniel.me/") { content { includeGroup("me.shedaniel.cloth") } }
    maven("https://api.modrinth.com/maven") { content { includeGroup("maven.modrinth") } }
}

dependencies {
    // Compat dependencies
    modCompileOnlyApi("me.shedaniel.cloth:cloth-config-fabric:${rootProject.prop("clothconfig", "version")}")
    modCompileOnlyApi("maven.modrinth:jade:${rootProject.prop("jade", "version")}+fabric")
    modCompileOnlyApi("maven.modrinth:first-person-model:${rootProject.prop("firstpersonmodel", "version")}")

    implementation("dev.matrixlab:webp4j:1.3.0")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

// Loom 1.7 injects the mapped Minecraft classpath only into the main source set.
// Domain codec tests need that same mapped classpath without adding a loader dependency.
sourceSets.test {
    compileClasspath += sourceSets.main.get().compileClasspath
    runtimeClasspath += sourceSets.main.get().runtimeClasspath
}
