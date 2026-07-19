package me.chrr.camerapture.domain.valuation;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

/** Complete read-only explanation for one catalog value. */
public record ValuationBreakdown(
        OptionalLong automaticValue,
        OptionalLong runtimeObservationValue,
        Optional<String> matchedRuleResource,
        Optional<ValuationRule> matchedRule,
        String fallbackSource,
        long finalValue,
        String cacheStatus
) {
    public ValuationBreakdown {
        Objects.requireNonNull(automaticValue, "automaticValue");
        Objects.requireNonNull(runtimeObservationValue, "runtimeObservationValue");
        Objects.requireNonNull(matchedRuleResource, "matchedRuleResource");
        Objects.requireNonNull(matchedRule, "matchedRule");
        Objects.requireNonNull(fallbackSource, "fallbackSource");
        Objects.requireNonNull(cacheStatus, "cacheStatus");
        if (finalValue < 0) {
            throw new IllegalArgumentException("final value must be non-negative");
        }
    }
}
