# Milestone 2 Registry Scan and Valuation Plan

## Baseline and scope

- Baseline commit: `9e00e9f504ae6dbadf7f0281bf2ec582bc689dc3`
- Working branch: `feature/m2-registry-valuation`
- Status: **进行中**
- In scope: entity/biome registry scans, automatic valuation, data-pack rules, deterministic fingerprints, bounded biome observations, versioned world persistence, administrator diagnostics, and loader lifecycle wiring.
- Out of scope: photo frustum capture, film, Virtual Gallery, balances, shops, submissions, and map art.

## Registries and loader boundaries

- Static registry: every registered `EntityType`, its identifier, tags, spawn group, and registered default living attributes.
- Dynamic registry: every biome in the active server `RegistryAccess`, including identifier and tags.
- `common` owns immutable scan records, valuation/catalog logic, observation math, persistence payload codecs, scan orchestration, command behavior, and read-only query interfaces.
- `fabric` and `neoforge` own only lifecycle, resource-reload, tick, command-registration, mod-list/data-pack discovery, and server storage adapters.
- Exact Minecraft 1.21.1/Yarn-patched event and registry APIs will be confirmed from the resolved source/JAR before implementation.

## Reload and lifecycle entry points

- Server start: initialize the world-scoped service, load SavedData, compute the fingerprint, and rebuild only when absent/invalid.
- Data reload: each loader registers an equivalent server-data reload listener; parsing and validation use one common transactional builder.
- Server tick: loaders call one common bounded sampler at the configured interval.
- Commands: loader command-registration events call one common Brigadier command tree.
- Server stop: release the active service reference after world persistence has been dirtied through the storage adapter.

## Persistence

- A versioned common payload stores schema/algorithm versions, fingerprint and rule digest, automatic entity/biome values, bounded observation filter/counts, and rebuild/reload timestamps.
- The payload is mounted in the primary overworld SavedData through a loader-neutral adapter; only identifiers, primitive values, versions, and hashes are persisted.
- Future schema versions are rejected. Invalid map/list entries are isolated and logged while valid entries survive; unrecoverable headers trigger a safe cache rebuild.

## Scan safety

- Never invoke `EntityType#create`, constructors, world insertion, or temporary spawning.
- Read only registered default attribute containers exposed by the 1.21.1 API.
- Per-entry failures become `NOT_LIVING`, `NO_DEFAULT_ATTRIBUTES`, `MISSING_ATTRIBUTE`, or `READ_ERROR` and cannot abort the scan.
- Non-living entities have no automatic value by default, while exact data-pack rules may still assign one.

## Observation structure and error model

- Use a fixed-size Bloom filter with deterministic hashes over dimension ID plus chunk X/Z and bounded biome counters.
- False positives can only omit a new observation; they cannot cause duplicate counting or memory growth.
- Persist exactly `filter_bits / 8` bytes plus bounded counters for currently valid biome IDs.
- Smoothed frequency is `(count + alpha) / (total + alpha * observedBiomeKinds)`.
- Runtime rarity remains `1.0` below the minimum sample threshold and otherwise clamps an inverse-frequency multiplier to configured bounds.

## Fingerprint composition

- Minecraft version and valuation algorithm version.
- Sorted mod ID/version pairs and enabled data-pack IDs.
- Canonical validated server valuation configuration.
- Digest of successfully loaded rules.
- Sorted entity and biome registry identifiers.
- All inputs are length-delimited, sorted where order is semantically irrelevant, and SHA-256 hashed.

## Test matrix

- Pure JUnit: deterministic fingerprints; version/rule changes; automatic-value fallbacks; existing calculator bounds; selector precedence/priority; isolated JSON errors and atomic reload retention; biome smoothing/clamps; fixed-memory and dimension-aware dedupe; persistence schema round-trip/future rejection/corrupt-entry isolation.
- Runtime integration: vanilla registry counts, no entity construction during scan, legal/illegal reload behavior, restart persistence, rebuild/query commands, and equivalent catalogs on both loaders.
- Static validation: no loader imports in `common`, resource validation, testing-layout validation, and CI workflow command coverage.
- Builds/smoke: `:common:test`, clean `buildAll`, NeoForge dedicated server, Fabric dedicated server, command/reload/restart checks.

## Required commands and results

Commands are recorded here only after execution; no planned command is represented as passing.

| Command | Result |
|---|---|
| `./gradlew tasks --all` | passed with Java 21; confirmed both server run tasks and no dedicated GameTest task |
| `./gradlew :common:test -PdevMods.enabled=false --no-daemon` | passed: 48 tests, 0 failures, 0 skipped |
| `./gradlew clean buildAll :common:test -PdevMods.enabled=false --no-daemon` | passed: 25 tasks executed |
| `./gradlew :neoforge:runServer -PdevMods.enabled=false` | passed: 131 entities, 64 biomes, 6 rules; status/entity/biome/reload/rebuild/dump and restart cache reuse verified |
| `./gradlew :fabric:runServer -PdevMods.enabled=false` | passed: 131 entities, 64 biomes, 6 rules; status/entity/biome/reload/rebuild verified |
| malformed-rule dedicated-server integration | passed: one invalid JSON resource was skipped with its resource ID, `/reload` completed, six valid rules and zombie value 175 remained active |
| resource/test-layout/common-import validation | valuation JSON and asset references passed; test layout passed with one plain-JUnit warning; common loader-import scan passed; generic resource-pack validator reports the pre-existing absence of standalone `pack.mcmeta` |
| GitHub Actions | pending |

## Completion checklist

- [x] Safe entity and biome scans implemented.
- [x] Automatic values reuse `EntityValueCalculator`.
- [x] Immutable O(1) catalog and atomic replacement implemented.
- [x] Rules load per resource with validation and isolation.
- [x] Deterministic fingerprint invalidates only compatible cache portions.
- [x] Versioned world cache and bounded observations survive restart.
- [x] Administrator status/rebuild/query/dump/reset commands work.
- [x] Unit and runtime integration coverage pass.
- [x] Both loaders build and dedicated servers start.
- [ ] Documentation and CI match the implementation (documentation and local CI command verified; GitHub Actions pending).

## Failures and remaining issues

- Running Gradle under the host Java 25 failed during configuration with `25.0.2`; all recorded Gradle verification uses the project-required Homebrew OpenJDK 21.0.11.
- The generic multiloader version-sanity script expects underscore-style properties that this established repository does not use; the real dotted properties still resolve and both loader builds pass.
- The generic resource-pack validator checks a standalone resource pack and therefore reports the pre-existing missing `pack.mcmeta` in the common mod resources. Its JSON and asset-reference checks pass, and all six valuation data files separately pass `jq empty`.
- NeoForge and Fabric development launches generate random synthetic `generated_*` mod IDs. Loader adapters deliberately omit only those development IDs from the production fingerprint; two NeoForge launches produced the same fingerprint and the second reported `fingerprint_match`.
- The Loom run task wrapper remains alive after a normal Minecraft `stop`; verification interrupts that already-stopped Gradle wrapper after the world reports all dimensions saved.
