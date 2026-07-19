package me.chrr.camerapture.domain.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.chrr.camerapture.domain.CodecSafety;

/** Validated settings for fixed-memory biome observation sampling. */
public record BiomeObservationConfig(
        boolean enabled,
        int intervalTicks,
        long minimumSamples,
        double smoothingAlpha,
        double minimumMultiplier,
        double maximumMultiplier,
        int filterBits
) {
    public static final BiomeObservationConfig DEFAULT = new BiomeObservationConfig(
            true, 100, 100L, 1.0, 0.5, 4.0, 1 << 20
    );

    public static final Codec<BiomeObservationConfig> CODEC = CodecSafety.guard(RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("enabled", DEFAULT.enabled()).forGetter(BiomeObservationConfig::enabled),
            Codec.INT.optionalFieldOf("interval_ticks", DEFAULT.intervalTicks()).forGetter(BiomeObservationConfig::intervalTicks),
            Codec.LONG.optionalFieldOf("minimum_samples", DEFAULT.minimumSamples()).forGetter(BiomeObservationConfig::minimumSamples),
            Codec.DOUBLE.optionalFieldOf("smoothing_alpha", DEFAULT.smoothingAlpha()).forGetter(BiomeObservationConfig::smoothingAlpha),
            Codec.DOUBLE.optionalFieldOf("minimum_multiplier", DEFAULT.minimumMultiplier()).forGetter(BiomeObservationConfig::minimumMultiplier),
            Codec.DOUBLE.optionalFieldOf("maximum_multiplier", DEFAULT.maximumMultiplier()).forGetter(BiomeObservationConfig::maximumMultiplier),
            Codec.INT.optionalFieldOf("filter_bits", DEFAULT.filterBits()).forGetter(BiomeObservationConfig::filterBits)
    ).apply(instance, BiomeObservationConfig::new)), "invalid biome observation config");

    public BiomeObservationConfig {
        if (intervalTicks <= 0 || minimumSamples < 0 || !Double.isFinite(smoothingAlpha) || smoothingAlpha <= 0.0
                || !Double.isFinite(minimumMultiplier) || minimumMultiplier < 0.0
                || !Double.isFinite(maximumMultiplier) || maximumMultiplier < minimumMultiplier
                || filterBits < 1_024 || filterBits > (1 << 26) || (filterBits & 7) != 0) {
            throw new IllegalArgumentException("invalid biome observation config");
        }
    }
}
