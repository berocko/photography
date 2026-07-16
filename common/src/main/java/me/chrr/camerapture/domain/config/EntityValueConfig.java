package me.chrr.camerapture.domain.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record EntityValueConfig(
        double healthWeight,
        double armorWeight,
        double attackWeight,
        double specialWeight,
        long minimumValue,
        long maximumValue
) {
    public static final EntityValueConfig DEFAULT = new EntityValueConfig(8.0, 12.0, 15.0, 20.0, 1, 100_000);

    public static final Codec<EntityValueConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.optionalFieldOf("health_weight", DEFAULT.healthWeight).forGetter(EntityValueConfig::healthWeight),
            Codec.DOUBLE.optionalFieldOf("armor_weight", DEFAULT.armorWeight).forGetter(EntityValueConfig::armorWeight),
            Codec.DOUBLE.optionalFieldOf("attack_weight", DEFAULT.attackWeight).forGetter(EntityValueConfig::attackWeight),
            Codec.DOUBLE.optionalFieldOf("special_weight", DEFAULT.specialWeight).forGetter(EntityValueConfig::specialWeight),
            Codec.LONG.optionalFieldOf("minimum_value", DEFAULT.minimumValue).forGetter(EntityValueConfig::minimumValue),
            Codec.LONG.optionalFieldOf("maximum_value", DEFAULT.maximumValue).forGetter(EntityValueConfig::maximumValue)
    ).apply(instance, EntityValueConfig::new));

    public EntityValueConfig {
        if (!nonNegativeFinite(healthWeight) || !nonNegativeFinite(armorWeight)
                || !nonNegativeFinite(attackWeight) || !nonNegativeFinite(specialWeight)
                || minimumValue < 0 || maximumValue < minimumValue) {
            throw new IllegalArgumentException("invalid entity value configuration");
        }
    }

    private static boolean nonNegativeFinite(double value) {
        return Double.isFinite(value) && value >= 0.0;
    }
}
