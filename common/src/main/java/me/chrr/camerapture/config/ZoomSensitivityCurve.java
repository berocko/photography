package me.chrr.camerapture.config;

/** Pure camera sensitivity curve shared by config migration and client input. */
public final class ZoomSensitivityCurve {
    public static final float MIN_FOV_MODIFIER = 0.1f;
    public static final float DEFAULT_MINIMUM = 0.10f;
    public static final float MINIMUM_LOWER_BOUND = 0.01f;
    public static final float MINIMUM_UPPER_BOUND = 1.0f;
    public static final float DEFAULT_EXPONENT = 0.85f;
    public static final float EXPONENT_LOWER_BOUND = 0.25f;
    public static final float EXPONENT_UPPER_BOUND = 3.0f;

    private ZoomSensitivityCurve() {
    }

    public static float sanitizeMinimum(float value) {
        return sanitize(value, DEFAULT_MINIMUM, MINIMUM_LOWER_BOUND, MINIMUM_UPPER_BOUND);
    }

    public static float sanitizeExponent(float value) {
        return sanitize(value, DEFAULT_EXPONENT, EXPONENT_LOWER_BOUND, EXPONENT_UPPER_BOUND);
    }

    public static float modifier(float fovModifier, float minimum, float exponent) {
        float floor = sanitizeMinimum(minimum);
        float curveExponent = sanitizeExponent(exponent);
        float fov = sanitize(fovModifier, 1.0f, MIN_FOV_MODIFIER, 1.0f);
        float normalizedFov = (fov - MIN_FOV_MODIFIER) / (1.0f - MIN_FOV_MODIFIER);
        return floor + (1.0f - floor) * (float) Math.pow(normalizedFov, curveExponent);
    }

    private static float sanitize(float value, float fallback, float minimum, float maximum) {
        if (!Float.isFinite(value)) {
            return fallback;
        }
        return Math.max(minimum, Math.min(maximum, value));
    }
}
