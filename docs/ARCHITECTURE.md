# Architecture

## Current modules

```text
common    shared registration objects, items/entities, packet models, capture/storage rules,
          shared resources, plus a split client source set for screens/rendering/capture
fabric    Fabric entrypoints, events, mixins, networking and platform adapter
neoforge  NeoForge @Mod entrypoints, events, networking and platform adapter
```

The build shadows `common` into each loader artifact. NeoForge is the primary implementation target; Fabric must continue compiling.

## Existing capture call graph

```text
loader attack hook
  -> PictureTaker.takePicture/renderTickEnd (client screenshot)
  -> NewPicturePacket
  -> Camerapture.registerPacketHandlers (server checks camera, consumes paper)
  -> ServerPictureStore.reserveId
  -> RequestUploadPacket
  -> PictureTaker.uploadStoredPicture (WebP + chunks)
  -> UploadPartialPicturePacket[]
  -> ByteCollector
  -> ServerPictureStore.put(world/camerapture/<uuid>.webp)
  -> PictureItem.create + inventory offer/drop
```

Downloads use `RequestDownloadPacket` → `DownloadQueue` → `DownloadPartialPicturePacket[]` → `ClientPictureStore`. Physical albums currently store complete picture item stacks in a vanilla container component.

## Target domain boundaries

Milestone 1 adds immutable schema-versioned records under `domain/photo`, validated config records under `domain/config`, the loader-neutral `CurrencyProvider` boundary and checked balance arithmetic under `domain/currency`, data-pack valuation rules under `domain/valuation`, and pure scoring under `domain/scoring`.

Loader modules own registration, lifecycle events, platform persistence hooks, packet registration, and client integration. Optional integrations load only through guarded adapters and may not reference absent classes during common class loading.

## Milestone 2 valuation runtime

Fabric and NeoForge register equivalent server start/stop, data reload, end-tick, and command callbacks. Those callbacks delegate to one common `ServerValuationManager`. The manager safely reads `DefaultAttributeRegistry` without calling `EntityType#create`, scans dynamic biomes from the active server registry manager, and publishes a `RegistryScanSnapshot` plus `ValuationCatalog` through one atomic reference.

The world cache is vanilla `PersistentState` named `camerapture_valuation_cache` in the primary overworld. Schema version 1 stores primitive IDs/values, fingerprint/rule digests, automatic caches, bounded biome counts/filter bytes, and timestamps. A fixed Bloom filter hashes dimension plus chunk coordinates; no unbounded chunk set is retained. Fingerprint changes prune absent registry IDs while preserving compatible observation counts.

Rules are prepared and validated per resource, then a complete immutable Catalog is built before publication. Data-pack-only reloads reuse the existing attribute scan. Full attribute scans occur at server start/cache creation, fingerprint rebuild, or administrator rebuild, never per player, tick, or photo.

## Known baseline debt

- Upload reservations are global UUIDs rather than player-bound sessions and have no timeout.
- The first chunk can cause allocation based on client-declared remaining length before a strict pre-allocation bound.
- Paper is removed before upload and not refunded on compression, disconnect, or persistence failure.
- A successful upload immediately creates a physical picture item; there is no Gallery ownership record or save/submit separation.
- `ServerPictureStore` cache and reservation collections need explicit synchronization and lifecycle cleanup.
- Gradle still declares upstream publishing tasks, but the GitHub workflow is build-only and supplies no publish tokens. Publishing remains prohibited.
- Architectury Loom 1.7 and parts of the Gradle scripts use deprecated APIs; upgrades need a separate compatibility milestone, not an incidental snapshot bump.
