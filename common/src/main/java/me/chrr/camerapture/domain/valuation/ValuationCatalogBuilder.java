package me.chrr.camerapture.domain.valuation;

import me.chrr.camerapture.domain.config.GameplayConfig;
import me.chrr.camerapture.domain.observation.BiomeObservationPolicy;
import me.chrr.camerapture.domain.observation.BiomeObservationStats;
import me.chrr.camerapture.domain.registry.BiomeRegistryEntry;
import me.chrr.camerapture.domain.registry.EntityRegistryEntry;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

/** Builds a complete immutable catalog before it can be atomically published. */
public final class ValuationCatalogBuilder {
    private ValuationCatalogBuilder() {
    }

    public static Result build(
            Map<Identifier, EntityRegistryEntry> entities,
            Map<Identifier, BiomeRegistryEntry> biomes,
            LoadedValuationRules loadedRules,
            BiomeObservationStats observations,
            GameplayConfig config,
            String cacheStatus,
            long now
    ) {
        Map<Identifier, ValuationBreakdown> entityValues = new HashMap<>();
        Map<Identifier, ValuationBreakdown> biomeValues = new HashMap<>();
        Map<Identifier, BiomeRegistryEntry> resolvedBiomes = new HashMap<>();
        List<LoadedValuationRules.LoadedRule> entityRules = loadedRules.rules().stream()
                .filter(rule -> rule.objectType() == LoadedValuationRules.ObjectType.ENTITY)
                .toList();
        List<LoadedValuationRules.LoadedRule> biomeRules = loadedRules.rules().stream()
                .filter(rule -> rule.objectType() == LoadedValuationRules.ObjectType.BIOME)
                .toList();

        for (EntityRegistryEntry entry : entities.values()) {
            ValuationResolver.Resolution resolution = ValuationResolver.resolveDetailed(
                    entry.id(), entry.tags(), entityRules.stream().map(LoadedValuationRules.LoadedRule::rule).toList(),
                    OptionalLong.empty(), entry.automaticValue(), config.registryScan().globalEntityDefault()
            );
            entityValues.put(entry.id(), breakdown(entry.automaticValue(), OptionalLong.empty(), resolution, entityRules, cacheStatus));
        }

        BiomeObservationPolicy policy = new BiomeObservationPolicy(config.biomeObservation());
        long total = observations.total();
        int kinds = observations.observedBiomeKinds();
        for (BiomeRegistryEntry entry : biomes.values()) {
            long count = observations.count(entry.id());
            double frequency = policy.smoothedFrequency(count, total, kinds);
            double multiplier = policy.rarityMultiplier(count, total, kinds);
            OptionalLong runtime = total < config.biomeObservation().minimumSamples()
                    ? OptionalLong.empty()
                    : OptionalLong.of(boundedRound(entry.automaticBaseValue() * multiplier));
            ValuationResolver.Resolution resolution = ValuationResolver.resolveDetailed(
                    entry.id(), entry.tags(), biomeRules.stream().map(LoadedValuationRules.LoadedRule::rule).toList(),
                    runtime, OptionalLong.of(entry.automaticBaseValue()), config.registryScan().globalBiomeDefault()
            );
            biomeValues.put(entry.id(), breakdown(OptionalLong.of(entry.automaticBaseValue()), runtime, resolution, biomeRules, cacheStatus));
            resolvedBiomes.put(entry.id(), new BiomeRegistryEntry(
                    entry.id(), entry.tags(), entry.automaticBaseValue(), count, frequency, multiplier, resolution.value()
            ));
        }

        return new Result(new ValuationCatalog(entityValues, biomeValues, loadedRules.digest(), now), resolvedBiomes);
    }

    private static ValuationBreakdown breakdown(
            OptionalLong automatic,
            OptionalLong runtime,
            ValuationResolver.Resolution resolution,
            List<LoadedValuationRules.LoadedRule> candidates,
            String cacheStatus
    ) {
        Optional<LoadedValuationRules.LoadedRule> loaded = resolution.matchedRule().flatMap(rule ->
                candidates.stream().filter(candidate -> candidate.rule().equals(rule)).findFirst());
        return new ValuationBreakdown(
                automatic,
                runtime,
                loaded.map(rule -> rule.resourceId().toString()),
                resolution.matchedRule(),
                resolution.fallbackSource(),
                resolution.value(),
                cacheStatus
        );
    }

    private static long boundedRound(double value) {
        return !Double.isFinite(value) || value >= Long.MAX_VALUE ? Long.MAX_VALUE : Math.max(0L, Math.round(value));
    }

    public record Result(ValuationCatalog catalog, Map<Identifier, BiomeRegistryEntry> resolvedBiomes) {
        public Result {
            resolvedBiomes = Map.copyOf(resolvedBiomes);
        }
    }
}
