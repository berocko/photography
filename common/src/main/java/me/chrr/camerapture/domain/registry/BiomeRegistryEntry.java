package me.chrr.camerapture.domain.registry;

import net.minecraft.util.Identifier;

import java.util.Objects;
import java.util.Set;

/** Immutable view of one biome and its observation-derived valuation. */
public record BiomeRegistryEntry(
        Identifier id,
        Set<Identifier> tags,
        long automaticBaseValue,
        long observedCount,
        double smoothedObservedFrequency,
        double runtimeRarityMultiplier,
        long finalResolvedValue
) {
    public BiomeRegistryEntry {
        Objects.requireNonNull(id, "id");
        tags = Set.copyOf(tags);
        if (automaticBaseValue < 0 || observedCount < 0 || finalResolvedValue < 0
                || !Double.isFinite(smoothedObservedFrequency) || smoothedObservedFrequency < 0.0
                || !Double.isFinite(runtimeRarityMultiplier) || runtimeRarityMultiplier < 0.0) {
            throw new IllegalArgumentException("invalid biome registry entry");
        }
    }
}
