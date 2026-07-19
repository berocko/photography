package me.chrr.camerapture.registry;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Versioned primitive-only world cache payload. */
public record ValuationCachePayload(
        int schemaVersion,
        int algorithmVersion,
        String registryFingerprint,
        String ruleDigest,
        Map<Identifier, Long> entityAutomaticValues,
        Map<Identifier, Long> biomeAutomaticValues,
        Map<Identifier, Long> biomeObservationCounts,
        int observationFilterBits,
        byte[] observationFilter,
        long lastRebuildEpochMillis,
        long lastSuccessfulReloadEpochMillis
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    public static final int CURRENT_ALGORITHM_VERSION = 1;

    public ValuationCachePayload {
        entityAutomaticValues = Map.copyOf(entityAutomaticValues);
        biomeAutomaticValues = Map.copyOf(biomeAutomaticValues);
        biomeObservationCounts = Map.copyOf(biomeObservationCounts);
        observationFilter = observationFilter.clone();
    }

    public static ValuationCachePayload empty(int filterBits) {
        return new ValuationCachePayload(
                CURRENT_SCHEMA_VERSION, CURRENT_ALGORITHM_VERSION, "", "",
                Map.of(), Map.of(), Map.of(), filterBits, new byte[filterBits / 8], 0L, 0L
        );
    }

    public NbtCompound toNbt() {
        NbtCompound root = new NbtCompound();
        root.putInt("schema_version", schemaVersion);
        root.putInt("algorithm_version", algorithmVersion);
        root.putString("registry_fingerprint", registryFingerprint);
        root.putString("rule_digest", ruleDigest);
        root.put("entity_automatic_values", writeValues(entityAutomaticValues));
        root.put("biome_automatic_values", writeValues(biomeAutomaticValues));
        root.put("biome_observation_counts", writeValues(biomeObservationCounts));
        root.putInt("observation_filter_bits", observationFilterBits);
        root.putByteArray("observation_filter", observationFilter);
        root.putLong("last_rebuild_epoch_millis", lastRebuildEpochMillis);
        root.putLong("last_successful_reload_epoch_millis", lastSuccessfulReloadEpochMillis);
        return root;
    }

    public static DecodeResult fromNbt(NbtCompound root) {
        int schemaVersion = root.getInt("schema_version");
        if (schemaVersion > CURRENT_SCHEMA_VERSION) {
            throw new UnsupportedSchemaException("unsupported valuation cache schema version " + schemaVersion);
        }
        if (schemaVersion <= 0) {
            throw new IllegalArgumentException("missing or invalid valuation cache schema version");
        }
        List<String> warnings = new ArrayList<>();
        int filterBits = root.getInt("observation_filter_bits");
        byte[] filter = root.getByteArray("observation_filter");
        if (filterBits < 8 || (filterBits & 7) != 0 || filter.length != filterBits / 8) {
            warnings.add("invalid observation filter; reset to default size");
            filterBits = 1 << 20;
            filter = new byte[filterBits / 8];
        }
        ValuationCachePayload payload = new ValuationCachePayload(
                schemaVersion,
                Math.max(0, root.getInt("algorithm_version")),
                root.getString("registry_fingerprint"),
                root.getString("rule_digest"),
                readValues(root.getList("entity_automatic_values", NbtElement.COMPOUND_TYPE), "entity automatic", warnings),
                readValues(root.getList("biome_automatic_values", NbtElement.COMPOUND_TYPE), "biome automatic", warnings),
                readValues(root.getList("biome_observation_counts", NbtElement.COMPOUND_TYPE), "biome observation", warnings),
                filterBits,
                filter,
                Math.max(0L, root.getLong("last_rebuild_epoch_millis")),
                Math.max(0L, root.getLong("last_successful_reload_epoch_millis"))
        );
        return new DecodeResult(payload, warnings);
    }

    @Override
    public byte[] observationFilter() {
        return observationFilter.clone();
    }

    private static NbtList writeValues(Map<Identifier, Long> values) {
        NbtList list = new NbtList();
        values.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            NbtCompound item = new NbtCompound();
            item.putString("id", entry.getKey().toString());
            item.putLong("value", entry.getValue());
            list.add(item);
        });
        return list;
    }

    private static Map<Identifier, Long> readValues(NbtList list, String label, List<String> warnings) {
        Map<Identifier, Long> values = new HashMap<>();
        for (int index = 0; index < list.size(); index++) {
            NbtCompound item = list.getCompound(index);
            Identifier id = Identifier.tryParse(item.getString("id"));
            long value = item.getLong("value");
            if (id == null || value < 0) {
                warnings.add("skipped corrupt " + label + " entry at index " + index);
                continue;
            }
            values.put(id, value);
        }
        return Map.copyOf(values);
    }

    public record DecodeResult(ValuationCachePayload payload, List<String> warnings) {
        public DecodeResult {
            warnings = List.copyOf(warnings);
        }
    }

    public static final class UnsupportedSchemaException extends IllegalArgumentException {
        public UnsupportedSchemaException(String message) {
            super(message);
        }
    }
}
