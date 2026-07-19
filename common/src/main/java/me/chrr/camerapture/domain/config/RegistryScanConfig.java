package me.chrr.camerapture.domain.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.chrr.camerapture.domain.CodecSafety;

/** Validated restart-scoped registry scan settings. */
public record RegistryScanConfig(
        boolean enabled,
        boolean rebuildOnFingerprintChange,
        boolean includeNonLivingEntities,
        long globalEntityDefault,
        long globalBiomeDefault,
        double hostileMultiplier
) {
    public static final RegistryScanConfig DEFAULT = new RegistryScanConfig(
            true, true, true, 10L, 10L, 1.25
    );

    public static final Codec<RegistryScanConfig> CODEC = CodecSafety.guard(RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("enabled", DEFAULT.enabled()).forGetter(RegistryScanConfig::enabled),
            Codec.BOOL.optionalFieldOf("rebuild_on_fingerprint_change", DEFAULT.rebuildOnFingerprintChange()).forGetter(RegistryScanConfig::rebuildOnFingerprintChange),
            Codec.BOOL.optionalFieldOf("include_non_living_entities", DEFAULT.includeNonLivingEntities()).forGetter(RegistryScanConfig::includeNonLivingEntities),
            Codec.LONG.optionalFieldOf("global_entity_default", DEFAULT.globalEntityDefault()).forGetter(RegistryScanConfig::globalEntityDefault),
            Codec.LONG.optionalFieldOf("global_biome_default", DEFAULT.globalBiomeDefault()).forGetter(RegistryScanConfig::globalBiomeDefault),
            Codec.DOUBLE.optionalFieldOf("hostile_multiplier", DEFAULT.hostileMultiplier()).forGetter(RegistryScanConfig::hostileMultiplier)
    ).apply(instance, RegistryScanConfig::new)), "invalid registry scan config");

    public RegistryScanConfig {
        if (globalEntityDefault < 0 || globalBiomeDefault < 0
                || !Double.isFinite(hostileMultiplier) || hostileMultiplier < 0.0) {
            throw new IllegalArgumentException("invalid registry scan config");
        }
    }
}
