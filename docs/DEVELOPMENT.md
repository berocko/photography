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
- Build both distributable jars: `./gradlew buildAll`
- Module builds: `./gradlew :common:build :fabric:build :neoforge:build`
- Loader run tasks and GameTest tasks must be copied here only after task enumeration and a successful invocation.

Local command results and environmental exceptions are recorded in `plans/MASTER_PLAN.md`.

