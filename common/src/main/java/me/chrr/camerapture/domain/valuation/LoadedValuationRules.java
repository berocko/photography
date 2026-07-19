package me.chrr.camerapture.domain.valuation;

import me.chrr.camerapture.domain.registry.RegistryFingerprint;
import net.minecraft.util.Identifier;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Immutable, validated result of one resource reload. */
public record LoadedValuationRules(
        List<LoadedRule> rules,
        List<RuleDiagnostic> skipped,
        List<String> notices,
        String digest,
        long loadedAtEpochMillis
) {
    public enum ObjectType { ENTITY, BIOME }

    public LoadedValuationRules {
        rules = List.copyOf(rules);
        skipped = List.copyOf(skipped);
        notices = List.copyOf(notices);
        Objects.requireNonNull(digest, "digest");
    }

    public static LoadedValuationRules of(
            List<LoadedRule> rules,
            List<RuleDiagnostic> skipped,
            List<String> notices,
            long loadedAtEpochMillis
    ) {
        List<String> canonical = rules.stream()
                .sorted(Comparator.comparing(rule -> rule.resourceId().toString()))
                .map(LoadedRule::canonicalForm)
                .toList();
        return new LoadedValuationRules(rules, skipped, notices,
                RegistryFingerprint.digestStrings(canonical), loadedAtEpochMillis);
    }

    public static LoadedValuationRules empty() {
        return of(List.of(), List.of(), List.of(), 0L);
    }

    public record LoadedRule(Identifier resourceId, ObjectType objectType, ValuationRule rule) {
        public LoadedRule {
            Objects.requireNonNull(resourceId, "resourceId");
            Objects.requireNonNull(objectType, "objectType");
            Objects.requireNonNull(rule, "rule");
        }

        public String canonicalForm() {
            return resourceId + "|" + objectType + "|" + rule.selector() + "|" + rule.target() + "|"
                    + rule.mode() + "|" + rule.baseValue() + "|" + Double.toHexString(rule.multiplier())
                    + "|" + rule.enabled() + "|" + rule.priority();
        }
    }

    public record RuleDiagnostic(Identifier resourceId, String reason) {
        public RuleDiagnostic {
            Objects.requireNonNull(resourceId, "resourceId");
            Objects.requireNonNull(reason, "reason");
        }
    }
}
