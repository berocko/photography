# Decisions

## ADR-0001: Pinned platform baseline

- Status: Accepted
- Decision: Minecraft 1.21.1, Java 21, NeoForge primary, Fabric required to compile, upstream commit `e6760db17d82bc3b384e0fffb9538c1c0207596b`.

## ADR-0002: Preserve upstream identity for now

- Status: Accepted pending owner decision
- Decision: Retain `camerapture`, `me.chrr.camerapture`, existing resource IDs and world path. A Photo Expedition rename requires a migration ADR.

## ADR-0003: Incremental shared-domain architecture

- Status: Accepted
- Decision: Add immutable, schema-versioned shared domain models and pure scoring before changing the stable capture pipeline. Loader APIs remain isolated; existing Fabric annotations in common are recorded debt.

## ADR-0004: Server authority and transactional mutations

- Status: Accepted
- Decision: Scene targets, values, ownership, balances, submissions, shop grants, and map output are validated and mutated on the logical server. Failure leaves or restores all resources.

## ADR-0005: No publishing

- Status: Accepted
- Decision: Keep only build/readiness automation. No platform or GitHub release is authorized.

