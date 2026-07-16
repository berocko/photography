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
}
