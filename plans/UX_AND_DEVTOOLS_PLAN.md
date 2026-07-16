# Camera UX and Development Tools Plan

## Baseline and scope

- Branch: `feature/camera-ux-devtools`, based on `main` commit `188481ee04f00da2ac4a914132fe012d7b8e8f97`.
- Runtime target: Minecraft 1.21.1 and Java 21, with NeoForge primary and Fabric secondary.
- Scope is limited to camera zoom input, a dedicated creative tab, local client development tooling, and an audit/design record for future map art. Gallery, currency, shop, and map-art gameplay remain unimplemented milestones.

## Audit findings

### Camera zoom

- `PictureTaker` owns a mutable zoom level. Its FOV curve is already nonlinear, but mouse sensitivity currently interpolates linearly from `1.0` to a single `zoomMouseSensitivity` value.
- Fabric and NeoForge scroll hooks both change zoom while the camera is active. Neither hook explicitly excludes open screens.
- Common mouse-look modification applies the same factor to horizontal and vertical input while the camera is active.
- Several render paths reset the public field directly. Disconnect and world-join paths do not consistently reset it.
- Client config schema version 3 exposes one 10–100% Cloth Config slider.

### Creative inventory

- Both loaders currently append Camera and Album to vanilla Tools.
- There is no dedicated Camerapture item group. The physical Picture item is not added to Tools and must stay out of the new tab.

### Development dependencies

- Cloth Config, Jade, and First Person Model are compile-only common compatibility APIs. Fabric currently declares Mod Menu as `modApi`, which gives it formal dependency semantics even though it is optional compatibility.
- Loom's existing `localRuntime` feeds the general runtime classpath, including server runs, so it is unsuitable for client-only developer tools.
- Loom 1.7 exposes remap-configuration settings and mutable run-task classpaths. The implementation will create a dedicated remapped `devClientRuntime` configuration and attach it only to `runClient` when `-PdevMods.enabled=true` (the checked-in default). It will not extend `runtimeClasspath`, `modImplementation`, `modApi`, `include`, or `shadow`.
- Official Modrinth metadata for Minecraft 1.21.1 was checked before pinning. Planned artifacts are:
  - JEI 19.39.0.368: Fabric `vPkfuKVX`, NeoForge `bEGnP8IF`.
  - Mouse Tweaks: Fabric 1.21-2.26 `ylmBQ38A`, NeoForge 1.21-2.26.1 `9I21YYxf`.
  - Cloth Config 15.0.140: Fabric `HpMb5wGb`, NeoForge `izKINKFg`.
  - Mod Menu 11.0.4, Fabric only: `v6Xx3fbU`.
  - First Person Model 2.7.2: Fabric `fSfRdYJ6`, NeoForge `wcETD2Bu`.
  - Jade 15.10.5: Fabric `5Sbkzz4O`, NeoForge `yd8FKCmx`.
  - Required development-only companions: Placeholder API 2.4.2+1.21 Fabric `U5bhVym2`; Not Enough Animations 1.12.4 Fabric `HyecdWuC`, NeoForge `eYNogep3`; upstream TRansition 1.0.21 and TRender 1.0.15 loader-specific 1.21.1 snapshots required by the remapped tr7zw mods.

### Album, Gallery, and map art

- Album is a 36-slot item with three client pages of twelve slots. `AlbumScreen` edits/page-navigates album contents; `PictureScreen` views a picture and can save its PNG locally.
- There is no virtual Gallery persistence, Gallery browsing UI, map-palette conversion, `MapState` allocation, 3×3/5×5 slicing, transaction, or rollback implementation.
- Map art remains Milestone 7 and depends on the future server-authoritative Gallery (M4), currency/transaction service (M5), and persisted photo metadata. This task will document the absence and design only; it will not add placeholder controls or pretend gameplay exists.

## Implementation plan

### 1. Zoom sensitivity and lifecycle

- Add a loader-neutral pure `ZoomSensitivityCurve` utility with validated minimum sensitivity and exponent ranges.
- Use a normalized curve coordinated with the existing FOV interval: map the FOV modifier from `[0.1, 1.0]` to `[0, 1]`, apply the exponent, then interpolate from the configured floor to `1.0`. This gives exactly the floor at maximum zoom, exactly `1.0` with no zoom, monotonic behavior, and no discontinuity.
- Upgrade client config to version 4 with `minimumZoomSensitivity=0.10` and `zoomSensitivityExponent=0.85`. Keep the old field as a deprecated deserialization-only migration source, validate/clamp migrated and loaded values, and omit the old key on save.
- Replace the old Cloth Config slider with minimum-sensitivity and exponent controls plus English and Simplified Chinese tooltips.
- Make zoom state private behind `resetZoom()`, `getZoomLevel()`, and `zoom(delta)`. Reset when camera use ends, on connection join/disconnect, and on loader render fallbacks. Prevent scroll capture while a GUI is open and keep the same X/Y look multiplier.
- Add JUnit 5 tests for endpoints, monotonicity, floor correction, invalid exponent correction, and a reasonable middle value.

### 2. Photo Expedition creative tab

- Define the item-group identifier/key, translation key, camera icon supplier, and ordered allow-list (Camera, then Album) in common code without loader APIs.
- Register the actual item group through Fabric and NeoForge registry APIs.
- Remove modifications to vanilla Tools. Do not expose Picture.
- Add English and Simplified Chinese translations and verify dedicated-server class loading remains client-safe.

### 3. Local client development mods

- Add `devMods.enabled=true` and exact per-loader version IDs to `gradle.properties`.
- Keep compatibility APIs compile-only. Change Fabric Mod Menu from formal `modApi` to compile-only compatibility and supply its implementation only through the development-client configuration.
- Add remapped, non-consumable `devClientRuntime` configurations per loader and attach them only to `runClient` when enabled.
- Ensure CI/release/build documentation invokes distributable verification with `-PdevMods.enabled=false`.
- Verify dependency reports, both client run classpaths/logs, both server runs with development mods disabled, dual-loader builds, and final JAR contents.

### 4. Map-art audit and design

- Add `docs/MAP_ART_AUDIT.md` with concrete current Album/Picture UI behavior and evidence that map art does not exist.
- Add `docs/MAP_ART_DESIGN.md` specifying a server-authoritative request flow, 128×128 tiles, 384/640 preprocessing, row-major slicing, map IDs/components/state, bounded worker-thread conversion, main-server-thread mutation, cost/inventory transactions, compensation/rollback, persistence, abuse limits, and at least fifteen future tests.
- Update repository rules and milestone/development/configuration/testing documentation without changing M7 from unstarted.

## Verification and delivery

- Run pure unit tests, shared/module compilation, strict resource validation, common loader-import scan, and clean dual-loader artifact builds under Java 21.
- Inspect final Fabric and NeoForge JARs to confirm no JEI, Mouse Tweaks, Cloth Config, Mod Menu, Jade, or First Person Model classes/JARs are packaged.
- Run Fabric and NeoForge client smoke tests with development mods enabled. Interaction checks for zoom feel, GUI scroll behavior, tab ordering, JEI, and Mouse Tweaks will be reported as manual-only unless actually performed.
- Run Fabric and NeoForge dedicated-server smoke tests with development mods disabled and record only observed results.
- Commit intentionally, push `feature/camera-ux-devtools`, and open a Draft PR to `main`; do not merge it.

## Observed verification results

- `./gradlew clean buildAll :common:test -PdevMods.enabled=false`: passed, including 27 JUnit tests (8 new zoom/config migration tests) and both remapped loader artifacts.
- Both `devClientRuntimeClasspath` reports contain the exact Loom-remapped development mods. Fabric reports JEI, Mouse Tweaks, Cloth Config, Mod Menu, Placeholder API, Jade, First Person Model, Not Enough Animations, TRansition, and TRender. NeoForge reports the same applicable set without Fabric-only Mod Menu/Placeholder API.
- `:fabric:runClient` and `:neoforge:runClient` reached resource reload, sound initialization, Camerapture Jade plugin loading, and GUI/JEI atlas creation with the development mods. Both long-running clients were intentionally interrupted after the smoke point.
- `:fabric:runServer -PdevMods.enabled=false` and `:neoforge:runServer -PdevMods.enabled=false` reached `Done`; their mod lists contained no development mods. Servers were stopped after the smoke point (the inherited Camerapture executor keeps Gradle's process alive, so the wrapper was then interrupted).
- Strict resource validation passed with zero warnings against the merged NeoForge resources. Common source import scan found no Fabric/NeoForge/Forge imports.
- Final Fabric and NeoForge distributable JARs contain only the intended bundled `webp4j` dependency; no development-mod JAR or external package is present.
- Manual interaction was not performed. Zoom feel, equal X/Y feel in play, GUI scroll behavior, creative-tab visibility/order, and interactive JEI/Mouse Tweaks behavior remain reviewer checks.
