package me.chrr.camerapture.domain.registry;

import net.minecraft.util.Identifier;

import java.util.Objects;
import java.util.OptionalLong;
import java.util.Set;

/** Immutable, serialization-safe view of one registered entity type. */
public record EntityRegistryEntry(
        Identifier id,
        Set<Identifier> tags,
        String spawnGroup,
        boolean hasDefaultAttributes,
        double maxHealth,
        double armor,
        double attackDamage,
        double specialScore,
        OptionalLong automaticValue,
        AutomaticValueStatus automaticValueStatus,
        String automaticValueReason
) {
    public EntityRegistryEntry {
        Objects.requireNonNull(id, "id");
        tags = Set.copyOf(tags);
        Objects.requireNonNull(spawnGroup, "spawnGroup");
        Objects.requireNonNull(automaticValue, "automaticValue");
        Objects.requireNonNull(automaticValueStatus, "automaticValueStatus");
        Objects.requireNonNull(automaticValueReason, "automaticValueReason");
        if (automaticValue.stream().anyMatch(value -> value < 0)) {
            throw new IllegalArgumentException("automatic entity value must be non-negative");
        }
    }
}
