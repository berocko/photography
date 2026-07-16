package me.chrr.camerapture.domain.valuation;

import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.Comparator;
import java.util.OptionalLong;
import java.util.Set;

/** Implements exact > tag > namespace > runtime > automatic > global precedence. */
public final class ValuationResolver {
    private ValuationResolver() {
    }

    public static long resolve(
            Identifier id,
            Set<Identifier> tags,
            Collection<ValuationRule> rules,
            OptionalLong runtimeValue,
            OptionalLong automaticValue,
            long globalDefault
    ) {
        if (globalDefault < 0 || runtimeValue.stream().anyMatch(value -> value < 0)
                || automaticValue.stream().anyMatch(value -> value < 0)) {
            throw new IllegalArgumentException("valuation inputs must be non-negative");
        }
        long fallback = runtimeValue.orElseGet(() -> automaticValue.orElse(globalDefault));
        return rules.stream()
                .filter(ValuationRule::enabled)
                .filter(rule -> matches(rule, id, tags))
                .max(Comparator.comparingInt(ValuationResolver::precedence)
                        .thenComparingInt(ValuationRule::priority))
                .map(rule -> apply(rule, fallback))
                .orElse(fallback);
    }

    private static boolean matches(ValuationRule rule, Identifier id, Set<Identifier> tags) {
        return switch (rule.selector()) {
            case EXACT -> id.toString().equals(rule.target());
            case TAG -> tags.stream().anyMatch(tag -> tag.toString().equals(rule.target()));
            case NAMESPACE -> id.getNamespace().equals(rule.target());
        };
    }

    private static int precedence(ValuationRule rule) {
        return switch (rule.selector()) {
            case EXACT -> 3;
            case TAG -> 2;
            case NAMESPACE -> 1;
        };
    }

    private static long apply(ValuationRule rule, long fallback) {
        return switch (rule.mode()) {
            case DISABLE -> 0L;
            case OVERRIDE -> rule.baseValue();
            case ADD -> saturatedAdd(fallback, rule.baseValue());
            case MULTIPLY -> boundedRound(fallback * rule.multiplier());
        };
    }

    private static long saturatedAdd(long left, long right) {
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }

    private static long boundedRound(double value) {
        return !Double.isFinite(value) || value >= Long.MAX_VALUE ? Long.MAX_VALUE : Math.round(value);
    }
}
