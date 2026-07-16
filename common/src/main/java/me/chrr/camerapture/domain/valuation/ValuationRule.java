package me.chrr.camerapture.domain.valuation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.chrr.camerapture.domain.CodecSafety;

import java.util.Locale;
import java.util.Objects;

/** One data-pack valuation rule. Target syntax depends on selector kind. */
public record ValuationRule(
        SelectorKind selector,
        String target,
        Mode mode,
        long baseValue,
        double multiplier,
        boolean enabled,
        int priority
) {
    public enum SelectorKind { EXACT, TAG, NAMESPACE }
    public enum Mode { OVERRIDE, ADD, MULTIPLY, DISABLE }

    private static final Codec<SelectorKind> SELECTOR_CODEC = enumCodec(SelectorKind.class);
    private static final Codec<Mode> MODE_CODEC = enumCodec(Mode.class);

    public static final Codec<ValuationRule> CODEC = CodecSafety.guard(RecordCodecBuilder.create(instance -> instance.group(
            SELECTOR_CODEC.optionalFieldOf("selector", SelectorKind.EXACT).forGetter(ValuationRule::selector),
            Codec.STRING.fieldOf("target").forGetter(ValuationRule::target),
            MODE_CODEC.optionalFieldOf("mode", Mode.OVERRIDE).forGetter(ValuationRule::mode),
            Codec.LONG.optionalFieldOf("base_value", 0L).forGetter(ValuationRule::baseValue),
            Codec.DOUBLE.optionalFieldOf("multiplier", 1.0).forGetter(ValuationRule::multiplier),
            Codec.BOOL.optionalFieldOf("enabled", true).forGetter(ValuationRule::enabled),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(ValuationRule::priority)
    ).apply(instance, ValuationRule::new)), "invalid valuation rule");

    public ValuationRule {
        Objects.requireNonNull(selector, "selector");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(mode, "mode");
        if (target.isBlank() || baseValue < 0 || !Double.isFinite(multiplier) || multiplier < 0.0) {
            throw new IllegalArgumentException("invalid valuation rule");
        }
    }

    private static <E extends Enum<E>> Codec<E> enumCodec(Class<E> type) {
        return Codec.STRING.comapFlatMap(
                value -> {
                    try {
                        return DataResult.success(Enum.valueOf(type, value.toUpperCase(Locale.ROOT)));
                    } catch (IllegalArgumentException exception) {
                        return DataResult.error(() -> "unknown " + type.getSimpleName() + " value: " + value);
                    }
                },
                value -> value.name().toLowerCase(Locale.ROOT)
        );
    }
}
