package me.chrr.camerapture.domain.scoring;

import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Immutable per-owner counters captured before a submission transaction. */
public record RewardHistory(
        Map<UUID, Integer> entityInstances,
        Map<Identifier, Integer> entityTypes,
        Map<Identifier, Integer> biomeTypes
) {
    public static final RewardHistory EMPTY = new RewardHistory(Map.of(), Map.of(), Map.of());

    public RewardHistory {
        entityInstances = copyNonNegative(entityInstances, "entityInstances");
        entityTypes = copyNonNegative(entityTypes, "entityTypes");
        biomeTypes = copyNonNegative(biomeTypes, "biomeTypes");
    }

    private static <K> Map<K, Integer> copyNonNegative(Map<K, Integer> source, String name) {
        Objects.requireNonNull(source, name);
        source.forEach((key, count) -> {
            Objects.requireNonNull(key, name + " key");
            if (count == null || count < 0) {
                throw new IllegalArgumentException(name + " contains a negative count");
            }
        });
        return Map.copyOf(source);
    }
}
