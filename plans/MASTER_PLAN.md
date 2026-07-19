# Master Plan

Status values: **未开始**, **进行中**, **已完成**, **被阻塞**. A milestone is complete only when its listed validation has actually passed.

## Milestone status

| Milestone | Status | Scope | Validation / blockers |
|---|---|---|---|
| 0 Upstream audit and baseline | 已完成 | Pin upstream, audit capture/storage/network/modules, baseline builds, docs and license notices | JDK 21 task/build, NeoForge client/server smoke, Fabric artifact build passed |
| 1 Domain model and configuration | 已完成 | Photo/scene/score/currency models, config schema, data-pack codecs, pure scoring, unit tests | 19 JUnit tests and clean dual-loader build passed |
| 2 Registry scan and valuation | 未开始 | Entity/biome scans, fingerprints, overrides, bounded observation cache | Next milestone |
| 3 Scene snapshot and film save | 未开始 | Server frustum validation, preview/save split, film rollback, thumbnails | First playable slice |
| 4 Virtual Gallery | 未开始 | Persistent ownership, paging, thumbnails, filters, capacity | — |
| 5 Submission and credits | 未开始 | Internal/item currency, atomic submission, dedupe and decay | — |
| 6 Shop | 未开始 | Data-driven offers and atomic shared transaction service | — |
| 7 Map art | 未开始 | 3×3/5×5 palette conversion, cost and rollback | — |
| 8 Multiloader completion | 未开始 | Feature parity and dual-loader client/server smoke tests | — |
| 9 CI and release preparation | 未开始 | Java 21 matrix, unit/GameTest, artifacts, license/secret checks | Publishing remains prohibited |

## Cross-milestone camera UX and developer tooling — 已完成

This bounded branch improves the inherited camera zoom curve/config migration, registers a loader-parity Photo Expedition creative tab, and isolates pinned optional compatibility/development mods to local client runs. It also audits the current Album/Picture screens and records the future server-authoritative M7 design. It does not advance Gallery, currency, shop, or map-art milestone status.

Implementation and observed verification results are tracked in `plans/UX_AND_DEVTOOLS_PLAN.md`. Clean dual-loader artifacts and 27 JUnit tests passed; both clients loaded the isolated development tools, and both dedicated servers reached `Done` with those tools disabled. Interactive UX checks remain manual. M7 remains **未开始** because no map-art gameplay or placeholder UI is added.

## Milestone 0 — 已完成

### Actual work

- Verified project Skills and locked provenance at `Jahrome907/minecraft-agent-skills` commit `82c38979adcbf6244c4ee835e26c3460ea1f97e4`.
- Added `upstream=https://github.com/chrrs/camerapture.git`, imported full history, and based local `main` on `old/1.21.1` commit `e6760db17d82bc3b384e0fffb9538c1c0207596b` without committing or pushing.
- Audited modules, capture/upload/download flow, picture storage, physical picture/album items, config, client source sets, loader adapters, and release automation.
- Corrected Gradle repository routing for plugin and Minecraft tool dependencies without upgrading the pinned toolchain.
- Removed the common module's Fabric `@Environment` imports and compile-only loader dependency. A final import scan finds no Fabric, NeoForge, or Forge API in common.
- Corrected item model parent IDs and the NeoForge 1.21.1 resource pack format. Strict resource validation passes.
- Replaced the publishing workflow with a manual build-only readiness workflow using read-only permissions and artifacts only.
- Added required architecture, storage, protocol, security, compatibility, configuration, testing, upstream, release, asset, and decision documentation. Upstream MIT license/history remain unchanged.

### Validation results

- Shell-default Java 25 `./gradlew tasks --all`: **failed** during configuration with `25.0.2`; the project requires Java 21.
- JDK 21 `./gradlew tasks --all`: **passed** (`BUILD SUCCESSFUL`).
- JDK 21 `./gradlew buildAll`: **passed**; Fabric and NeoForge remapped artifacts produced.
- JDK 21 `./gradlew :neoforge:runServer`: **smoke passed**; Camerapture was discovered and the dedicated server reached `Done`. The long-running process was then intentionally interrupted.
- JDK 21 `./gradlew :neoforge:runClient`: **smoke passed**; OpenGL initialized, Camerapture loaded, resources reloaded, sound initialized, and GUI/texture atlases were created. No crash report was generated; the long-running process was intentionally interrupted.
- Resource-pack validator `--strict`: **passed** with zero warnings after model and pack metadata fixes.
- Multiloader skill version-sanity script: **not compatible with the inherited property schema**; it expects unnamespaced keys such as `minecraft_version` and `enabled_platforms`, while this build uses `platform.versions`, `fabric.apiVersion`, and related namespaced keys. It reports 9 missing-key errors and 2 derivative warnings. Version alignment was instead verified from actual properties, clean dual-loader artifacts, and both NeoForge run configurations; duplicate alias keys were not added because they could drift.

### Actual modified files

- Build/safety: `settings.gradle.kts`, `.github/workflows/release.yml`, `.gitignore`.
- Loader boundary/resources: `common/build.gradle.kts`, `common/.../PlatformAdapter.java`, `common/.../net/NetworkAdapter.java`, item model JSON files, `neoforge/src/main/resources/pack.mcmeta`.
- Governance/docs: `AGENTS.md`, `THIRD_PARTY_NOTICES.md`, `.agents/`, `docs/`, `plans/`, `scripts/`.

### Remaining risks

- Architectury Loom 1.7 is outdated and Gradle reports APIs that will be incompatible with Gradle 9; upgrades need their own compatibility pass.
- The second server smoke reused a development world containing a changing generated userdev mod ID, producing a harmless development-version mismatch warning.
- Existing upload reservation/allocation/refund/concurrency risks remain for Milestone 3.

## Milestone 1 — 已完成

### Actual work

- Added immutable `PhotoId`, `PhotoMetadata`, `PhotoRecord`, `SceneSnapshot`, `EntityObservation`, `BiomeObservation`, and `ScoreBreakdown` records.
- Added schema version 1 Minecraft Codecs for photo/scene/config records, future-version rejection, and a guard that converts validation exceptions into ordinary per-entry Codec errors.
- Added validated `GameplayConfig`, `CurrencyConfig`, `EntityValueConfig`, and `ScoringConfig`; integrated server JSON config version 6 through `Config.Server.gameplayConfig()`.
- Added loader-neutral `CurrencyProvider`, audit context/result records, and checked balance arithmetic rejecting negative values, insufficient funds, and overflow.
- Added valuation-rule Codec and deterministic exact > tag > namespace > runtime > automatic > global resolver with saturated arithmetic.
- Added logarithmically compressed entity valuation and deterministic photo scoring with primary/secondary/tertiary weights, discovery multipliers, UUID instance cap, independent type/biome caps, inverse decay, reward clamps, and persisted breakdown notes.

### Validation results

- `./gradlew :common:test`: **passed**, 19 tests, zero failures/errors/skips.
- `./gradlew clean buildAll :common:test`: **passed**, 25 tasks executed from clean outputs.
- Common forbidden-loader import scan: **passed**, no Fabric/NeoForge/Forge imports.
- Testing layout validator, non-strict: **passed** with one warning that only plain unit tests exist.
- Testing layout validator, strict: **failed only on that expected GameTest/MockBukkit warning**. GameTests begin with world-dependent milestones; this result is not represented as a pass.

### Actual modified files

- `common/build.gradle.kts`, `common/src/main/java/me/chrr/camerapture/config/Config.java`.
- `common/src/main/java/me/chrr/camerapture/domain/{config,currency,photo,scoring,valuation}/`.
- `common/src/test/java/me/chrr/camerapture/domain/{config,currency,photo,scoring,valuation}/`.
- `docs/{ARCHITECTURE,CONFIGURATION,DATA_PACK_FORMATS,ROADMAP,SCORING_MODEL,TESTING}.md` and this plan.

### Remaining risks

- Domain records are not yet connected to the live capture/upload path; that is intentionally deferred to M2/M3.
- `CurrencyProvider` is only the stable boundary. Internal and item-backed persistence/atomic transaction implementations are M5.
- Valuation rules have a Codec/resolver but no resource reload listener or registry validation until M2.
- Biome type cap exists in scoring; per-player same-biome/same-chunk dedupe and bounded observation persistence remain M2/M5.
- GameTests are not present yet because M1 logic is pure and has no world fixtures.

## Next milestone gate

Milestone 2 may start from this clean M1 boundary. It must implement safe registry scanning without unconditional entity instantiation, bounded biome observation data, version/fingerprint invalidation, data-pack reload/error isolation, and tests before modifying capture gameplay.
