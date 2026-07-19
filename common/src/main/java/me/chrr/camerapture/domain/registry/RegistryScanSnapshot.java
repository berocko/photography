package me.chrr.camerapture.domain.registry;

import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.Objects;

/** Atomically published immutable result of one registry/catalog rebuild. */
public record RegistryScanSnapshot(
        Map<Identifier, EntityRegistryEntry> entities,
        Map<Identifier, BiomeRegistryEntry> biomes,
        RegistryFingerprint fingerprint,
        String ruleDigest,
        long scannedAtEpochMillis,
        long scanDurationMillis
) {
    public RegistryScanSnapshot {
        entities = Map.copyOf(entities);
        biomes = Map.copyOf(biomes);
        Objects.requireNonNull(fingerprint, "fingerprint");
        Objects.requireNonNull(ruleDigest, "ruleDigest");
        if (scannedAtEpochMillis < 0 || scanDurationMillis < 0) {
            throw new IllegalArgumentException("invalid scan timestamps");
        }
    }

    public static RegistryScanSnapshot empty() {
        return new RegistryScanSnapshot(Map.of(), Map.of(),
                RegistryFingerprint.compute(new RegistryFingerprint.Input("empty", 0, java.util.List.of(), java.util.List.of(), "", "", java.util.List.of(), java.util.List.of())),
                "", 0L, 0L);
    }
}
