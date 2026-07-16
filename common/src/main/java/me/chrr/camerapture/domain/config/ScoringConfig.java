package me.chrr.camerapture.domain.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record ScoringConfig(
        int algorithmVersion,
        double secondaryWeight,
        double tertiaryWeight,
        double entityDiscoveryMultiplier,
        double biomeDiscoveryMultiplier,
        double typeDecayCoefficient,
        double typeDecayExponent,
        int maxPaidPerEntityInstance,
        int maxPaidPerEntityType,
        int maxPaidPerBiomeType,
        long minimumReward,
        long maximumReward
) {
    public static final ScoringConfig DEFAULT = new ScoringConfig(
            1, 0.50, 0.25, 1.25, 1.15, 0.18, 1.0, 1, 10, 10, 0, 1_000_000
    );

    public static final Codec<ScoringConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("algorithm_version", DEFAULT.algorithmVersion).forGetter(ScoringConfig::algorithmVersion),
            Codec.DOUBLE.optionalFieldOf("secondary_weight", DEFAULT.secondaryWeight).forGetter(ScoringConfig::secondaryWeight),
            Codec.DOUBLE.optionalFieldOf("tertiary_weight", DEFAULT.tertiaryWeight).forGetter(ScoringConfig::tertiaryWeight),
            Codec.DOUBLE.optionalFieldOf("entity_discovery_multiplier", DEFAULT.entityDiscoveryMultiplier).forGetter(ScoringConfig::entityDiscoveryMultiplier),
            Codec.DOUBLE.optionalFieldOf("biome_discovery_multiplier", DEFAULT.biomeDiscoveryMultiplier).forGetter(ScoringConfig::biomeDiscoveryMultiplier),
            Codec.DOUBLE.optionalFieldOf("type_decay_coefficient", DEFAULT.typeDecayCoefficient).forGetter(ScoringConfig::typeDecayCoefficient),
            Codec.DOUBLE.optionalFieldOf("type_decay_exponent", DEFAULT.typeDecayExponent).forGetter(ScoringConfig::typeDecayExponent),
            Codec.INT.optionalFieldOf("max_paid_per_entity_instance", DEFAULT.maxPaidPerEntityInstance).forGetter(ScoringConfig::maxPaidPerEntityInstance),
            Codec.INT.optionalFieldOf("max_paid_per_entity_type", DEFAULT.maxPaidPerEntityType).forGetter(ScoringConfig::maxPaidPerEntityType),
            Codec.INT.optionalFieldOf("max_paid_per_biome_type", DEFAULT.maxPaidPerBiomeType).forGetter(ScoringConfig::maxPaidPerBiomeType),
            Codec.LONG.optionalFieldOf("minimum_reward", DEFAULT.minimumReward).forGetter(ScoringConfig::minimumReward),
            Codec.LONG.optionalFieldOf("maximum_reward", DEFAULT.maximumReward).forGetter(ScoringConfig::maximumReward)
    ).apply(instance, ScoringConfig::new));

    public ScoringConfig {
        if (algorithmVersion <= 0 || !nonNegativeFinite(secondaryWeight) || !nonNegativeFinite(tertiaryWeight)
                || !positiveFinite(entityDiscoveryMultiplier) || !positiveFinite(biomeDiscoveryMultiplier)
                || !nonNegativeFinite(typeDecayCoefficient) || !positiveFinite(typeDecayExponent)
                || maxPaidPerEntityInstance < 0 || maxPaidPerEntityType < 0 || maxPaidPerBiomeType < 0
                || minimumReward < 0 || maximumReward < minimumReward) {
            throw new IllegalArgumentException("invalid scoring configuration");
        }
    }

    private static boolean nonNegativeFinite(double value) {
        return Double.isFinite(value) && value >= 0.0;
    }

    private static boolean positiveFinite(double value) {
        return Double.isFinite(value) && value > 0.0;
    }
}
