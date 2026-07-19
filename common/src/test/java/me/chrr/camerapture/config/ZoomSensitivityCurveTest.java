package me.chrr.camerapture.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZoomSensitivityCurveTest {
    private static final float EPSILON = 0.0001f;

    @Test
    void noZoomUsesFullSensitivity() {
        assertEquals(1.0f, ZoomSensitivityCurve.modifier(1.0f, 0.1f, 0.85f), EPSILON);
    }

    @Test
    void maximumZoomUsesConfiguredFloor() {
        assertEquals(0.1f, ZoomSensitivityCurve.modifier(0.1f, 0.1f, 0.85f), EPSILON);
    }

    @Test
    void curveIsMonotonicAcrossFovRange() {
        float previous = 0.0f;
        for (int step = 0; step <= 90; step++) {
            float value = ZoomSensitivityCurve.modifier(0.1f + step / 100.0f, 0.1f, 0.85f);
            assertTrue(value + EPSILON >= previous);
            previous = value;
        }
    }

    @Test
    void minimumIsClampedOnBothSides() {
        assertEquals(0.01f, ZoomSensitivityCurve.sanitizeMinimum(-1.0f), EPSILON);
        assertEquals(1.0f, ZoomSensitivityCurve.sanitizeMinimum(2.0f), EPSILON);
    }

    @Test
    void invalidExponentIsCorrected() {
        assertEquals(ZoomSensitivityCurve.DEFAULT_EXPONENT,
                ZoomSensitivityCurve.sanitizeExponent(Float.NaN), EPSILON);
        assertEquals(ZoomSensitivityCurve.EXPONENT_LOWER_BOUND,
                ZoomSensitivityCurve.sanitizeExponent(0.0f), EPSILON);
        assertEquals(ZoomSensitivityCurve.EXPONENT_UPPER_BOUND,
                ZoomSensitivityCurve.sanitizeExponent(10.0f), EPSILON);
    }

    @Test
    void middleZoomRemainsBetweenFloorAndFullSensitivity() {
        float middle = ZoomSensitivityCurve.modifier(0.55f, 0.1f, 0.85f);
        assertTrue(middle > 0.1f && middle < 1.0f);
        assertEquals(0.599f, middle, 0.01f);
    }
}
