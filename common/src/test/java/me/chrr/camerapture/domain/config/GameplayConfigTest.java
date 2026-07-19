package me.chrr.camerapture.domain.config;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameplayConfigTest {
    @Test
    void decodesDefaultsFromVersionOnlyDocument() {
        GameplayConfig decoded = GameplayConfig.CODEC.parse(
                JsonOps.INSTANCE, JsonParser.parseString("{\"schema_version\":1}")
        ).getOrThrow();

        assertEquals(GameplayConfig.DEFAULT, decoded);
    }

    @Test
    void rejectsFutureSchema() {
        assertTrue(GameplayConfig.CODEC.parse(
                JsonOps.INSTANCE, JsonParser.parseString("{\"schema_version\":2}")
        ).error().isPresent());
    }

    @Test
    void malformedNumericConfigDoesNotEscapeAsParserException() {
        var result = assertDoesNotThrow(() -> GameplayConfig.CODEC.parse(
                JsonOps.INSTANCE,
                JsonParser.parseString("{\"scoring\":{\"maximum_reward\":-1}}")
        ));
        assertTrue(result.error().isPresent());
    }

    @Test
    void decodesMilestoneTwoSettings() {
        GameplayConfig decoded = GameplayConfig.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {
                  "registry_scan": {"global_entity_default": 25},
                  "biome_observation": {"interval_ticks": 40, "filter_bits": 2048},
                  "valuation": {"debug_commands": false}
                }
                """)).getOrThrow();
        assertEquals(25, decoded.registryScan().globalEntityDefault());
        assertEquals(40, decoded.biomeObservation().intervalTicks());
        assertEquals(2048, decoded.biomeObservation().filterBits());
        assertEquals(false, decoded.valuation().debugCommands());
    }

    @Test
    void rejectsUnboundedOrMisalignedObservationFilter() {
        assertTrue(GameplayConfig.CODEC.parse(
                JsonOps.INSTANCE,
                JsonParser.parseString("{\"biome_observation\":{\"filter_bits\":1025}}")
        ).error().isPresent());
    }
}
