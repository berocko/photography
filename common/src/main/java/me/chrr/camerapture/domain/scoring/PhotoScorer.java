package me.chrr.camerapture.domain.scoring;

import me.chrr.camerapture.domain.config.ScoringConfig;
import me.chrr.camerapture.domain.photo.EntityObservation;
import me.chrr.camerapture.domain.photo.SceneSnapshot;
import me.chrr.camerapture.domain.photo.ScoreBreakdown;

import java.util.ArrayList;
import java.util.List;

/** Deterministic scoring with no world, loader, network, or client dependencies. */
public final class PhotoScorer {
    private PhotoScorer() {
    }

    public static ScoreBreakdown score(SceneSnapshot scene, RewardHistory history, ScoringConfig config) {
        double[] displayed = new double[3];
        double rawEntities = 0.0;
        double afterInstance = 0.0;
        double afterTypeDecay = 0.0;
        double visibilityWeighted = 0.0;
        double compositionWeighted = 0.0;
        List<String> notes = new ArrayList<>();

        for (int index = 0; index < scene.entities().size(); index++) {
            EntityObservation entity = scene.entities().get(index);
            double positionWeight = switch (index) {
                case 0 -> 1.0;
                case 1 -> config.secondaryWeight();
                default -> config.tertiaryWeight();
            };
            double positionedValue = entity.baseValue() * positionWeight;
            displayed[index] = positionedValue;
            rawEntities += positionedValue;
            visibilityWeighted += positionedValue * entity.visibility();
            compositionWeighted += positionedValue * entity.composition();

            boolean instanceEligible = entity.entityId()
                    .map(id -> history.entityInstances().getOrDefault(id, 0) < config.maxPaidPerEntityInstance())
                    .orElse(true);
            if (!instanceEligible) {
                notes.add("entity_instance_cap:" + entity.entityType());
                continue;
            }
            afterInstance += positionedValue;

            int typeCount = history.entityTypes().getOrDefault(entity.entityType(), 0);
            if (typeCount >= config.maxPaidPerEntityType()) {
                notes.add("entity_type_cap:" + entity.entityType());
                continue;
            }
            afterTypeDecay += positionedValue * typeDecay(typeCount, config);
        }

        double visibility = rawEntities == 0.0 ? 1.0 : visibilityWeighted / rawEntities;
        double composition = rawEntities == 0.0 ? 1.0 : compositionWeighted / rawEntities;
        double instanceMultiplier = rawEntities == 0.0 ? 1.0 : afterInstance / rawEntities;
        double typeMultiplier = afterInstance == 0.0 ? 1.0 : afterTypeDecay / afterInstance;

        int biomeCount = history.biomeTypes().getOrDefault(scene.biome().biome(), 0);
        double biomeValue = 0.0;
        if (biomeCount < config.maxPaidPerBiomeType()) {
            biomeValue = scene.biome().baseValue() * typeDecay(biomeCount, config);
        } else {
            notes.add("biome_type_cap:" + scene.biome().biome());
        }

        double discovery = 1.0;
        if (scene.newEntityDiscovery()) {
            discovery *= config.entityDiscoveryMultiplier();
        }
        if (scene.newBiomeDiscovery()) {
            discovery *= config.biomeDiscoveryMultiplier();
        }

        double unclamped = (afterTypeDecay + biomeValue) * visibility * composition * discovery;
        long reward = clampRound(unclamped, config.minimumReward(), config.maximumReward());
        return new ScoreBreakdown(
                config.algorithmVersion(), displayed[0], displayed[1], displayed[2], biomeValue,
                visibility, composition, discovery, instanceMultiplier, typeMultiplier,
                unclamped, reward, notes
        );
    }

    public static double typeDecay(int previousPaidCount, ScoringConfig config) {
        if (previousPaidCount < 0) {
            throw new IllegalArgumentException("previousPaidCount must be non-negative");
        }
        return 1.0 / Math.pow(
                1.0 + config.typeDecayCoefficient() * previousPaidCount,
                config.typeDecayExponent()
        );
    }

    private static long clampRound(double value, long minimum, long maximum) {
        if (Double.isNaN(value) || value <= minimum) {
            return minimum;
        }
        if (!Double.isFinite(value) || value >= maximum) {
            return maximum;
        }
        return Math.round(value);
    }
}
