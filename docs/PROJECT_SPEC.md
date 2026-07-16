# Project Specification

## Identity

- Current upstream display name: **Camerapture**.
- Current Mod ID: `camerapture`.
- Current Maven group and base package: `me.chrr` / `me.chrr.camerapture`.
- License: MIT, retained unchanged from upstream.

The requested development codename **Photo Expedition** / `photoexpedition` is not applied because the imported base already has an identity. A rename is a separate migration affecting saved items, components, packets, assets, data packs, and world photo paths and therefore requires owner approval and a compatibility plan.

## Platform baseline

- Minecraft: 1.21 and 1.21.1 metadata range; development target is **1.21.1**.
- Java: 21.
- Primary loader: NeoForge `21.1.84`.
- Secondary loader: Fabric Loader `0.16.9`, Fabric API `0.110.0+1.21.1`.
- Architecture: Architectury Loom-style `common` / `fabric` / `neoforge` modules using Yarn mappings patched for NeoForge.
- Environments: single-player and dedicated server.

## Product goal

Create a server-authoritative loop around photography exploration, personal Gallery collection, appraisal rewards in Expedition Credits, a data-driven shop, and 3×3/5×5 map-art conversion while reusing Camerapture's capture, WebP transport, world storage, picture rendering, and physical album.

## Delivery order

The first playable vertical slice is capture preview → server scene snapshot → consume film on save → persistent Gallery. GUI breadth, shop terminals, NPC integrations, and map art follow only after the domain, persistence, and transaction foundations are tested.

Optional currency integrations and team-shared balances are not hard dependencies. No public release is authorized under the current identity or any provisional identity.

