package me.chrr.camerapture.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientConfigMigrationTest {
    @Test
    void versionThreeSensitivityMigratesAndDeprecatedKeyIsNotSaved() {
        Config.Client client = ConfigManager.GSON.fromJson("""
                {"version":3,"zoom_mouse_sensitivity":0.42}
                """, Config.Client.class);

        client.upgrade();

        assertEquals(4, client.version);
        assertEquals(0.42f, client.minimumZoomSensitivity, 0.0001f);
        assertEquals(ZoomSensitivityCurve.DEFAULT_EXPONENT, client.zoomSensitivityExponent, 0.0001f);

        String saved = ConfigManager.GSON.toJson(client);
        assertTrue(saved.contains("minimum_zoom_sensitivity"));
        assertTrue(saved.contains("zoom_sensitivity_exponent"));
        assertFalse(saved.contains("zoom_mouse_sensitivity"));
    }

    @Test
    void loadedVersionFourValuesAreCorrected() {
        Config.Client client = ConfigManager.GSON.fromJson("""
                {"version":4,"minimum_zoom_sensitivity":-2,"zoom_sensitivity_exponent":9}
                """, Config.Client.class);

        client.upgrade();

        assertEquals(ZoomSensitivityCurve.MINIMUM_LOWER_BOUND, client.minimumZoomSensitivity, 0.0001f);
        assertEquals(ZoomSensitivityCurve.EXPONENT_UPPER_BOUND, client.zoomSensitivityExponent, 0.0001f);
    }
}
