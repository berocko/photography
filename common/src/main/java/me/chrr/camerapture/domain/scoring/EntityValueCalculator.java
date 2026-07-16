package me.chrr.camerapture.domain.scoring;

import me.chrr.camerapture.domain.config.EntityValueConfig;

/** Pure, bounded attribute valuation used by the later registry scanner. */
public final class EntityValueCalculator {
    private EntityValueCalculator() {
    }

    public static long calculate(
            double maxHealth,
            double armor,
            double attackDamage,
            double specialScore,
            double rarityMultiplier,
            double hostileMultiplier,
            double bossMultiplier,
            double dimensionMultiplier,
            EntityValueConfig config
    ) {
        double danger = config.healthWeight() * compress(maxHealth)
                + config.armorWeight() * compress(armor)
                + config.attackWeight() * compress(attackDamage)
                + config.specialWeight() * nonNegativeFinite(specialScore);
        double value = danger
                * nonNegativeFinite(rarityMultiplier)
                * nonNegativeFinite(hostileMultiplier)
                * nonNegativeFinite(bossMultiplier)
                * nonNegativeFinite(dimensionMultiplier);
        return boundedRound(value, config.minimumValue(), config.maximumValue());
    }

    public static double compress(double value) {
        return Math.log1p(nonNegativeFinite(value));
    }

    private static double nonNegativeFinite(double value) {
        return Double.isFinite(value) && value > 0.0 ? value : 0.0;
    }

    private static long boundedRound(double value, long minimum, long maximum) {
        if (!Double.isFinite(value)) {
            return maximum;
        }
        double clamped = Math.max(minimum, Math.min(maximum, value));
        return clamped >= Long.MAX_VALUE ? Long.MAX_VALUE : Math.round(clamped);
    }
}
