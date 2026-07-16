package me.chrr.camerapture.domain.scoring;

import me.chrr.camerapture.domain.config.ScoringConfig;
import me.chrr.camerapture.domain.photo.BiomeObservation;
import me.chrr.camerapture.domain.photo.EntityObservation;
import me.chrr.camerapture.domain.photo.SceneSnapshot;
import me.chrr.camerapture.domain.photo.ScoreBreakdown;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhotoScorerTest {
    private static final Identifier ZOMBIE = Identifier.of("minecraft", "zombie");
    private static final Identifier PLAINS = Identifier.of("minecraft", "plains");

    @Test
    void usesPrimarySecondaryAndTertiaryWeights() {
        SceneSnapshot scene = scene(List.of(
                entity(ZOMBIE, Optional.empty(), 100),
                entity(Identifier.of("minecraft", "skeleton"), Optional.empty(), 100),
                entity(Identifier.of("minecraft", "creeper"), Optional.empty(), 100)
        ), 25);

        ScoreBreakdown score = PhotoScorer.score(scene, RewardHistory.EMPTY, neutralConfig(10_000));

        assertEquals(100.0, score.primaryEntityValue());
        assertEquals(50.0, score.secondaryEntityValue());
        assertEquals(25.0, score.tertiaryEntityValue());
        assertEquals(200, score.finalReward());
    }

    @Test
    void appliesConfiguredInverseTypeDecay() {
        ScoringConfig config = new ScoringConfig(1, .5, .25, 1, 1, .18, 1, 1, 10, 10, 0, 10_000);

        assertEquals(1.0, PhotoScorer.typeDecay(0, config));
        assertEquals(1.0 / 1.18, PhotoScorer.typeDecay(1, config), 1.0e-12);
        assertEquals(1.0 / 2.62, PhotoScorer.typeDecay(9, config), 1.0e-12);
    }

    @Test
    void rejectsRepeatedEntityUuidButNeverCollapsesMissingUuids() {
        UUID repeated = UUID.randomUUID();
        RewardHistory history = new RewardHistory(Map.of(repeated, 1), Map.of(), Map.of());

        ScoreBreakdown repeatedScore = PhotoScorer.score(
                scene(List.of(entity(ZOMBIE, Optional.of(repeated), 100)), 0), history, neutralConfig(10_000));
        ScoreBreakdown missingUuidScore = PhotoScorer.score(
                scene(List.of(entity(ZOMBIE, Optional.empty(), 100)), 0), history, neutralConfig(10_000));

        assertEquals(0, repeatedScore.finalReward());
        assertTrue(repeatedScore.notes().getFirst().startsWith("entity_instance_cap:"));
        assertEquals(100, missingUuidScore.finalReward());
    }

    @Test
    void enforcesEntityTypeCapAndRewardMaximumWithoutOverflow() {
        RewardHistory capped = new RewardHistory(Map.of(), Map.of(ZOMBIE, 10), Map.of());
        assertEquals(0, PhotoScorer.score(
                scene(List.of(entity(ZOMBIE, Optional.empty(), 100)), 0), capped, neutralConfig(10_000)).finalReward());

        ScoreBreakdown huge = PhotoScorer.score(
                scene(List.of(entity(ZOMBIE, Optional.empty(), Long.MAX_VALUE)), Long.MAX_VALUE),
                RewardHistory.EMPTY,
                neutralConfig(500)
        );
        assertEquals(500, huge.finalReward());
    }

    @Test
    void biomeTypeCapIsIndependentFromEntityTypeCap() {
        RewardHistory biomeCapped = new RewardHistory(Map.of(), Map.of(), Map.of(PLAINS, 10));
        ScoreBreakdown score = PhotoScorer.score(
                scene(List.of(entity(ZOMBIE, Optional.empty(), 100)), 50),
                biomeCapped,
                neutralConfig(10_000)
        );

        assertEquals(100, score.finalReward());
        assertTrue(score.notes().getFirst().startsWith("biome_type_cap:"));
    }

    private static EntityObservation entity(Identifier type, Optional<UUID> id, long value) {
        return new EntityObservation(type, id, value, 1.0, 1.0);
    }

    private static SceneSnapshot scene(List<EntityObservation> entities, long biomeValue) {
        return new SceneSnapshot(
                1, Identifier.of("minecraft", "overworld"), 1, entities,
                new BiomeObservation(PLAINS, OptionalLong.of(0), biomeValue), false, false
        );
    }

    private static ScoringConfig neutralConfig(long maximumReward) {
        return new ScoringConfig(1, .5, .25, 1, 1, 0, 1, 1, 10, 10, 0, maximumReward);
    }
}
