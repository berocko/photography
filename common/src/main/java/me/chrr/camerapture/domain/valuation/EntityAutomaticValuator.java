package me.chrr.camerapture.domain.valuation;

import me.chrr.camerapture.domain.config.EntityValueConfig;
import me.chrr.camerapture.domain.registry.AutomaticValueStatus;
import me.chrr.camerapture.domain.scoring.EntityValueCalculator;

import java.util.Objects;
import java.util.OptionalDouble;
import java.util.OptionalLong;

/** Converts safely extracted attributes through the existing bounded calculator. */
public final class EntityAutomaticValuator {
    private EntityAutomaticValuator() {
    }

    public static Outcome evaluate(AttributeSource source, double hostileMultiplier, EntityValueConfig config) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(config, "config");
        if (!source.living()) {
            return Outcome.unavailable(AutomaticValueStatus.NOT_LIVING, "entity type is not known to be living");
        }
        if (!source.hasDefaultAttributes()) {
            return Outcome.unavailable(AutomaticValueStatus.NO_DEFAULT_ATTRIBUTES, "no registered default attribute container");
        }
        boolean missing = source.maxHealth().isEmpty() || source.armor().isEmpty() || source.attackDamage().isEmpty();
        double health = source.maxHealth().orElse(0.0);
        double armor = source.armor().orElse(0.0);
        double attack = source.attackDamage().orElse(0.0);
        long value = EntityValueCalculator.calculate(
                health, armor, attack, 0.0, 1.0, hostileMultiplier, 1.0, 1.0, config
        );
        return new Outcome(
                OptionalLong.of(value),
                missing ? AutomaticValueStatus.MISSING_ATTRIBUTE : AutomaticValueStatus.AVAILABLE,
                missing ? "one or more combat attributes were absent and safely treated as zero" : "registered default attributes",
                health, armor, attack
        );
    }

    public static Outcome readError(Throwable throwable) {
        String detail = throwable == null || throwable.getMessage() == null
                ? "attribute read failed" : "attribute read failed: " + throwable.getMessage();
        return Outcome.unavailable(AutomaticValueStatus.READ_ERROR, detail);
    }

    public record AttributeSource(
            boolean living,
            boolean hasDefaultAttributes,
            OptionalDouble maxHealth,
            OptionalDouble armor,
            OptionalDouble attackDamage
    ) {
        public AttributeSource {
            Objects.requireNonNull(maxHealth, "maxHealth");
            Objects.requireNonNull(armor, "armor");
            Objects.requireNonNull(attackDamage, "attackDamage");
        }

        public static AttributeSource notLiving() {
            return new AttributeSource(false, false, OptionalDouble.empty(), OptionalDouble.empty(), OptionalDouble.empty());
        }

        public static AttributeSource noDefaultAttributes() {
            return new AttributeSource(true, false, OptionalDouble.empty(), OptionalDouble.empty(), OptionalDouble.empty());
        }
    }

    public record Outcome(
            OptionalLong automaticValue,
            AutomaticValueStatus status,
            String reason,
            double maxHealth,
            double armor,
            double attackDamage
    ) {
        public Outcome {
            Objects.requireNonNull(automaticValue, "automaticValue");
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(reason, "reason");
        }

        public static Outcome unavailable(AutomaticValueStatus status, String reason) {
            return new Outcome(OptionalLong.empty(), status, reason, 0.0, 0.0, 0.0);
        }
    }
}
