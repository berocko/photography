package me.chrr.camerapture.domain.observation;

import me.chrr.camerapture.domain.config.BiomeObservationConfig;

import java.util.Objects;

/** Smoothing and bounded rarity policy for observed biome samples. */
public final class BiomeObservationPolicy {
    private final BiomeObservationConfig config;

    public BiomeObservationPolicy(BiomeObservationConfig config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    public double smoothedFrequency(long count, long total, int observedBiomeKinds) {
        if (count < 0 || total < count || observedBiomeKinds < 0) {
            throw new IllegalArgumentException("invalid biome sample counts");
        }
        if (total == 0 || observedBiomeKinds == 0) {
            return 0.0;
        }
        return (count + config.smoothingAlpha())
                / (total + config.smoothingAlpha() * observedBiomeKinds);
    }

    public double rarityMultiplier(long count, long total, int observedBiomeKinds) {
        if (total < config.minimumSamples() || observedBiomeKinds == 0) {
            return 1.0;
        }
        double frequency = smoothedFrequency(count, total, observedBiomeKinds);
        double relativeToUniform = frequency * observedBiomeKinds;
        double raw = relativeToUniform <= 0.0 ? config.maximumMultiplier() : 1.0 / relativeToUniform;
        return Math.max(config.minimumMultiplier(), Math.min(config.maximumMultiplier(), raw));
    }
}
