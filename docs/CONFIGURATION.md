# Configuration

The baseline uses `camerapture.client.json` and `camerapture.server.json`. Client config version 4 replaces `zoomMouseSensitivity` with `minimumZoomSensitivity` (default `0.10`, valid `0.01`–`1.0`) and `zoomSensitivityExponent` (default `0.85`, valid `0.25`–`3.0`). Version 3 values migrate into the new minimum and the deprecated key is omitted on the next save. Loaded values are corrected to safe finite ranges.

The sensitivity curve normalizes the camera FOV modifier from `[0.1, 1.0]` to `[0, 1]`, raises it to the configured exponent, and interpolates from the configured minimum to full sensitivity. Maximum zoom therefore uses the exact floor and no zoom uses `1.0`.

Server config version 7 extends the `expedition` section with registry scanning, bounded biome observations, and valuation diagnostics while preserving the existing versioned upgrade path. `Config.Server.gameplayConfig()` converts the mutable Gson document into validated immutable domain records before gameplay code consumes it.

Implemented server shape (defaults shown):

```json
{
  "version": 7,
  "expedition": {
    "currency": { "provider": "camerapture:internal", "team_shared": false },
    "entity_values": {
      "health_weight": 8.0,
      "armor_weight": 12.0,
      "attack_weight": 15.0,
      "special_weight": 20.0,
      "minimum_value": 1,
      "maximum_value": 100000
    },
    "rewards": {
      "algorithm_version": 1,
      "secondary_weight": 0.5,
      "tertiary_weight": 0.25,
      "entity_discovery_multiplier": 1.25,
      "biome_discovery_multiplier": 1.15,
      "type_decay_coefficient": 0.18,
      "type_decay_exponent": 1.0,
      "max_paid_per_entity_instance": 1,
      "max_paid_per_entity_type": 10,
      "max_paid_per_biome_type": 10,
      "minimum_reward": 0,
      "maximum_reward": 1000000
    },
    "registry_scan": {
      "enabled": true,
      "rebuild_on_fingerprint_change": true,
      "include_non_living_entities": true,
      "global_entity_default": 10,
      "global_biome_default": 10,
      "hostile_multiplier": 1.25
    },
    "biome_observation": {
      "enabled": true,
      "interval_ticks": 100,
      "minimum_samples": 100,
      "smoothing_alpha": 1.0,
      "minimum_multiplier": 0.5,
      "maximum_multiplier": 4.0,
      "filter_bits": 1048576
    },
    "valuation": {
      "log_unknown_entities": true,
      "log_empty_tags": true,
      "debug_commands": true
    }
  }
}
```

The immutable `GameplayConfig` Codec carries schema version 1 and rejects future versions. Negative amounts, non-finite weights, inverted ranges, invalid caps, non-positive intervals, and Bloom filters outside 1,024–67,108,864 byte-aligned bits are rejected. Provider selection and all server JSON settings above are restart-scoped because `/reload` reloads data packs, not `camerapture.server.json`.

`filter_bits` is the exact fixed Bloom-filter memory budget in bits; the default is 1,048,576 bits (131,072 bytes). Changing its size on restart preserves valid biome counters but starts a fresh filter because differently sized bit positions are incompatible. Registry fingerprints include the validated configuration. When `rebuild_on_fingerprint_change` is false, compatible cached automatic values are retained for matching IDs; administrator `valuation rebuild` always forces a fresh attribute scan.

Biome frequency is smoothed as `(count + alpha) / (total + alpha × observedBiomeKinds)`. Runtime rarity is neutral (`1.0`) until `minimum_samples`, then inverse relative frequency is clamped to the configured multiplier range.

Future sections add scan/cache controls; frustum, distance, visibility and target limits; biome chunk dedupe; Gallery and album capacities; film/creative rules; image rates and quotas; map-art costs/dithering; shops; and debug logging.
