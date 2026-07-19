# Repository Agent Guide

## Product and platform

- Target Minecraft version: **1.21.1**.
- Required Java version: **21**.
- NeoForge is the primary integration and runtime platform.
- Fabric is secondary but must remain compilable.
- Supported environments are single-player and dedicated server.
- The repository is based on Camerapture commit `e6760db17d82bc3b384e0fffb9538c1c0207596b`.

## Sources of truth

- Product scope: `docs/PROJECT_SPEC.md`
- Architecture and loader boundaries: `docs/ARCHITECTURE.md`
- Milestone status and verification: `plans/MASTER_PLAN.md`
- Scoring and configuration: `docs/SCORING_MODEL.md`, `docs/CONFIGURATION.md`
- Storage, network, security, and tests: `docs/PHOTO_STORAGE.md`, `docs/NETWORK_PROTOCOL.md`, `docs/SECURITY.md`, `docs/TESTING.md`
- Upstream provenance: `docs/UPSTREAM.md`, `THIRD_PARTY_NOTICES.md`

## Mandatory engineering rules

1. Never import NeoForge APIs into `common`; never import Fabric APIs except where an existing compatibility annotation is being removed or isolated.
2. Put loader registration, events, networking adapters, and client bootstrapping in `fabric` or `neoforge`.
3. Never trust client-provided targets, values, reward amounts, ownership, or submission state. Rewards are server-authoritative.
4. Never hard-code reward or balance parameters in business logic; use validated server configuration or data-pack definitions.
5. Every persisted format has a schema version and a documented migration plan before it changes.
6. Every game-behavior change needs an automated unit, integration, or GameTest appropriate to its boundary.
7. Client classes must not be reachable from dedicated-server class-loading paths.
8. Currency, inventory, submission, and map-art mutations must be server-side and atomic or explicitly compensating.
9. Do not replace stable Camerapture capture, image encoding, transfer, storage, or rendering without an audit and focused regression tests.
10. Do not commit secrets, local absolute paths, run directories, logs, or build outputs.
11. JEI, Mouse Tweaks, Cloth Config, Mod Menu, Jade, First Person Model, and companion development mods are local client-run tools only; never package them or add them to server/formal runtime dependencies.
12. Camerapture items belong in the dedicated Photo Expedition creative tab in the explicit common allow-list order; do not append them to vanilla tabs or expose Picture without a separate product decision.
13. Map-art source pixels, ownership, pricing, palette output, map IDs, allocation, delivery, persistence, and rollback are server-authoritative. Client pixels are never accepted as map-art truth.

## Workflow

- Work one milestone at a time and update `plans/MASTER_PLAN.md` with status, files, commands, results, and remaining risks.
- Detect actual Gradle tasks before invoking them. Use JDK 21 and report failures verbatim.
- Validate both loader builds whenever shared code changes.
- Validate resource references whenever assets or data JSON change.
- Do not commit, push, force-push, publish to Modrinth/CurseForge, or create a GitHub Release unless the owner explicitly authorizes that exact action.
- Never fabricate build, launch, test, or GameTest results.
