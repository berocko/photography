package me.chrr.camerapture.registry;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValuationCachePayloadTest {
    @Test
    void schemaRoundTrips() {
        Identifier zombie = Identifier.of("minecraft", "zombie");
        ValuationCachePayload payload = new ValuationCachePayload(
                1, 1, "fingerprint", "rules", Map.of(zombie, 42L), Map.of(), Map.of(),
                1_024, new byte[128], 100L, 200L
        );
        ValuationCachePayload decoded = ValuationCachePayload.fromNbt(payload.toNbt()).payload();
        assertEquals(payload.registryFingerprint(), decoded.registryFingerprint());
        assertEquals(payload.entityAutomaticValues(), decoded.entityAutomaticValues());
        assertEquals(payload.observationFilterBits(), decoded.observationFilterBits());
    }

    @Test
    void futureSchemaIsRejected() {
        NbtCompound nbt = ValuationCachePayload.empty(1_024).toNbt();
        nbt.putInt("schema_version", 2);
        assertThrows(ValuationCachePayload.UnsupportedSchemaException.class, () -> ValuationCachePayload.fromNbt(nbt));
    }

    @Test
    void corruptEntryDoesNotDestroyValidEntries() {
        NbtCompound nbt = ValuationCachePayload.empty(1_024).toNbt();
        NbtList values = new NbtList();
        NbtCompound valid = new NbtCompound();
        valid.putString("id", "minecraft:zombie");
        valid.putLong("value", 50L);
        values.add(valid);
        NbtCompound corrupt = new NbtCompound();
        corrupt.putString("id", "not an id");
        corrupt.putLong("value", -1L);
        values.add(corrupt);
        nbt.put("entity_automatic_values", values);

        ValuationCachePayload.DecodeResult decoded = ValuationCachePayload.fromNbt(nbt);
        assertEquals(Map.of(Identifier.of("minecraft", "zombie"), 50L), decoded.payload().entityAutomaticValues());
        assertTrue(decoded.warnings().stream().anyMatch(message -> message.contains("corrupt")));
    }
}
