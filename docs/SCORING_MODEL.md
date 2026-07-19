# Scoring Model

Scoring is server-authoritative and operates only on the `SceneSnapshot` bound to a photo UUID. The client never supplies entity values or reward totals.

Entity auto-value uses compressed health, armor, attack, and special scores multiplied by configurable rarity, hostility, boss, and dimension factors. Biome value combines tag/environment defaults, bounded observed rarity, dimension factors, and data-pack overrides.

Precedence is exact object override → tag/namespace rule → runtime observation → automatic attributes → global default. Disabled entries produce no reward.

Milestone 2 supplies these inputs from the live server registries. Living automatic values use registered default max-health, armor, and attack-damage containers without entity construction. Missing attributes are zero-valued with a structured fallback status. Biome runtime rarity uses bounded world observations, remains neutral below the configured minimum sample count, and is clamped before entering the Catalog.

For up to three visible entities:

```text
scene = primary + secondary × 0.50 + tertiary × 0.25 + biome
reward = clamp(scene × visibility × composition × discovery × instance × type_decay)
type_decay(n) = 1 / (1 + coefficient × n) ^ exponent
```

`PhotoScorer` implements this as a deterministic common service. Entity observations remain in server-ranked primary/secondary/tertiary order. UUID instance caps and entity-type caps are independent; a missing UUID remains eligible and is never collapsed into a synthetic identity. Biome type decay/caps are independent of entity caps.

All weights, discovery multipliers, decay coefficients, caps, and reward clamps come from validated `ScoringConfig`. Computation uses `double` intermediates and clamps before converting to `long`, avoiding integer overflow. `ScoreBreakdown` persists algorithm version, component values, aggregate multipliers, unclamped value, final reward, and cap notes. Scene and score schema version 1 are encoded with Minecraft Codecs.
