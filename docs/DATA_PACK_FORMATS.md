# Data-Pack Formats

Roots reserved for reload listeners are `data/camerapture/photo_values/entities/`, `biomes/`, `tags/`, and `data/camerapture/shops/`. `ValuationRule.CODEC` is implemented in Milestone 1; registry reload wiring lands in Milestone 2.

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

Parsing uses Minecraft Codecs. Milestone 2 must add per-resource error reporting, registry existence checks, built-in examples, and reload tests. A bad entry will be skipped with its resource path and Codec error instead of aborting reload. Shop offer codecs remain Milestone 6 scope.
