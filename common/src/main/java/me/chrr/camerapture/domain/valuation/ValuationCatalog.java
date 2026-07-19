package me.chrr.camerapture.domain.valuation;

import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

/** Immutable O(1) valuation lookup catalog. */
public record ValuationCatalog(
        Map<Identifier, ValuationBreakdown> entities,
        Map<Identifier, ValuationBreakdown> biomes,
        String ruleDigest,
        long builtAtEpochMillis
) {
    public ValuationCatalog {
        entities = Map.copyOf(entities);
        biomes = Map.copyOf(biomes);
        Objects.requireNonNull(ruleDigest, "ruleDigest");
    }

    public OptionalLong entityValue(Identifier id) {
        ValuationBreakdown value = entities.get(id);
        return value == null ? OptionalLong.empty() : OptionalLong.of(value.finalValue());
    }

    public OptionalLong biomeValue(Identifier id) {
        ValuationBreakdown value = biomes.get(id);
        return value == null ? OptionalLong.empty() : OptionalLong.of(value.finalValue());
    }

    public Optional<ValuationBreakdown> entityBreakdown(Identifier id) {
        return Optional.ofNullable(entities.get(id));
    }

    public Optional<ValuationBreakdown> biomeBreakdown(Identifier id) {
        return Optional.ofNullable(biomes.get(id));
    }

    public static ValuationCatalog empty() {
        return new ValuationCatalog(Map.of(), Map.of(), LoadedValuationRules.empty().digest(), 0L);
    }
}
