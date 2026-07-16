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

## Known baseline debt

- Upload reservations are global UUIDs rather than player-bound sessions and have no timeout.
- The first chunk can cause allocation based on client-declared remaining length before a strict pre-allocation bound.
- Paper is removed before upload and not refunded on compression, disconnect, or persistence failure.
- A successful upload immediately creates a physical picture item; there is no Gallery ownership record or save/submit separation.
- `ServerPictureStore` cache and reservation collections need explicit synchronization and lifecycle cleanup.
- Gradle still declares upstream publishing tasks, but the GitHub workflow is build-only and supplies no publish tokens. Publishing remains prohibited.
- Architectury Loom 1.7 and parts of the Gradle scripts use deprecated APIs; upgrades need a separate compatibility milestone, not an incidental snapshot bump.
