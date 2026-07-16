# Development

## Toolchain

- JDK 21 (required; Java 25 fails during this baseline's Gradle configuration)
- Gradle Wrapper 8.10
- Minecraft 1.21.1
- Architectury plugin 3.4-SNAPSHOT and Loom 1.7-SNAPSHOT as pinned by upstream
- Modules: `common`, `fabric`, `neoforge`

Always set the shell/IDE Gradle JVM to JDK 21 and use `./gradlew`. Do not silently upgrade upstream snapshot plugins during feature work.

## Confirmed or pending commands

- Enumerate actual tasks: `./gradlew tasks --all`
- Build both distributable jars: `./gradlew buildAll -PdevMods.enabled=false`
- Module builds: `./gradlew :common:build :fabric:build :neoforge:build`
- Loader run tasks and GameTest tasks must be copied here only after task enumeration and a successful invocation.

Local command results and environmental exceptions are recorded in `plans/MASTER_PLAN.md`.

## Client development mods

`devMods.enabled=true` is the local default. `:fabric:runClient` and `:neoforge:runClient` receive exact pinned JEI, Mouse Tweaks, Cloth Config, Jade, First Person Model, and required companion versions; Fabric also receives Mod Menu. A dedicated Loom-remapped configuration is attached only to the client run task. It does not extend the ordinary runtime classpath and is not published, included, shaded, or copied into artifacts.

Use `-PdevMods.enabled=false` for CI, release readiness, artifact verification, and every dedicated-server run. Changing a pin requires checking Minecraft 1.21.1 and loader compatibility in the upstream/Modrinth metadata and re-running both client smokes.
