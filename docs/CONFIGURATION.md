# Configuration

The baseline uses `camerapture.client.json` and `camerapture.server.json`. Server config version 6 adds the `expedition` section while preserving the existing versioned upgrade path. `Config.Server.gameplayConfig()` converts the mutable Gson document into validated immutable domain records before gameplay code consumes it.

Implemented server shape (defaults shown):

```json
{
  "version": 6,
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
    }
  }
}
```

The immutable `GameplayConfig` Codec carries schema version 1 and rejects future versions. Negative amounts, non-finite weights, inverted ranges, and invalid caps are rejected. Provider selection is restart-scoped.

Future sections add scan/cache controls; frustum, distance, visibility and target limits; biome chunk dedupe; Gallery and album capacities; film/creative rules; image rates and quotas; map-art costs/dithering; shops; and debug logging.
