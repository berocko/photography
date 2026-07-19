# Data-Pack Formats

Valuation reload roots are `data/<namespace>/photo_values/entities/`, `biomes/`, and `tags/`. `ValuationRule.CODEC`, per-resource error isolation, registry validation, and atomic catalog replacement are implemented. `data/camerapture/shops/` remains reserved for Milestone 6.

```json
{
  "selector": "exact",
  "target": "examplemod:ancient_dragon",
  "mode": "override",
  "base_value": 1200,
  "multiplier": 4.0,
  "enabled": true,
  "priority": 0
}
```

`selector` is `exact`, `tag`, or `namespace`. `mode` is `override`, `add`, `multiply`, or `disable`. Defaults are exact/override, base value 0, multiplier 1, enabled, and priority 0. Values must be non-negative and finite.

Resolution is deterministic: exact object > tag > namespace > runtime observation > automatic attributes > global default. Priority breaks ties within a selector level. Addition and multiplication saturate rather than overflowing.

Parsing uses Minecraft Codecs. A bad entry is skipped with its resource path and Codec/validation error instead of aborting reload. `tags/` resources add `object_type: entity|biome`; see `docs/VALUATION_DATA_PACK.md` for the complete contract and built-in examples. Shop offer codecs remain Milestone 6 scope.
