package me.chrr.camerapture.domain.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.chrr.camerapture.domain.CodecSafety;

public record GameplayConfig(
        int schemaVersion,
        CurrencyConfig currency,
        EntityValueConfig entityValues,
        ScoringConfig scoring,
        RegistryScanConfig registryScan,
        BiomeObservationConfig biomeObservation,
        ValuationDebugConfig valuation
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    public static final GameplayConfig DEFAULT = new GameplayConfig(
            CURRENT_SCHEMA_VERSION, CurrencyConfig.DEFAULT, EntityValueConfig.DEFAULT, ScoringConfig.DEFAULT,
            RegistryScanConfig.DEFAULT, BiomeObservationConfig.DEFAULT, ValuationDebugConfig.DEFAULT
    );

    public static final Codec<GameplayConfig> CODEC = CodecSafety.guard(RecordCodecBuilder.<GameplayConfig>create(instance -> instance.group(
            Codec.INT.optionalFieldOf("schema_version", CURRENT_SCHEMA_VERSION).forGetter(GameplayConfig::schemaVersion),
            CurrencyConfig.CODEC.optionalFieldOf("currency", CurrencyConfig.DEFAULT).forGetter(GameplayConfig::currency),
            EntityValueConfig.CODEC.optionalFieldOf("entity_values", EntityValueConfig.DEFAULT).forGetter(GameplayConfig::entityValues),
            ScoringConfig.CODEC.optionalFieldOf("scoring", ScoringConfig.DEFAULT).forGetter(GameplayConfig::scoring),
            RegistryScanConfig.CODEC.optionalFieldOf("registry_scan", RegistryScanConfig.DEFAULT).forGetter(GameplayConfig::registryScan),
            BiomeObservationConfig.CODEC.optionalFieldOf("biome_observation", BiomeObservationConfig.DEFAULT).forGetter(GameplayConfig::biomeObservation),
            ValuationDebugConfig.CODEC.optionalFieldOf("valuation", ValuationDebugConfig.DEFAULT).forGetter(GameplayConfig::valuation)
    ).apply(instance, GameplayConfig::new)).validate(GameplayConfig::validateForCodec), "invalid gameplay config");

    public GameplayConfig {
        if (schemaVersion <= 0) {
            throw new IllegalArgumentException("schemaVersion must be positive");
        }
        java.util.Objects.requireNonNull(currency, "currency");
        java.util.Objects.requireNonNull(entityValues, "entityValues");
        java.util.Objects.requireNonNull(scoring, "scoring");
        java.util.Objects.requireNonNull(registryScan, "registryScan");
        java.util.Objects.requireNonNull(biomeObservation, "biomeObservation");
        java.util.Objects.requireNonNull(valuation, "valuation");
    }

    private static DataResult<GameplayConfig> validateForCodec(GameplayConfig config) {
        return config.schemaVersion() > CURRENT_SCHEMA_VERSION
                ? DataResult.error(() -> "unsupported gameplay config schema version " + config.schemaVersion())
                : DataResult.success(config);
    }
}
