package me.chrr.camerapture.domain.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Validated restart-scoped diagnostics settings. */
public record ValuationDebugConfig(
        boolean logUnknownEntities,
        boolean logEmptyTags,
        boolean debugCommands
) {
    public static final ValuationDebugConfig DEFAULT = new ValuationDebugConfig(true, true, true);

    public static final Codec<ValuationDebugConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("log_unknown_entities", DEFAULT.logUnknownEntities()).forGetter(ValuationDebugConfig::logUnknownEntities),
            Codec.BOOL.optionalFieldOf("log_empty_tags", DEFAULT.logEmptyTags()).forGetter(ValuationDebugConfig::logEmptyTags),
            Codec.BOOL.optionalFieldOf("debug_commands", DEFAULT.debugCommands()).forGetter(ValuationDebugConfig::debugCommands)
    ).apply(instance, ValuationDebugConfig::new));
}
