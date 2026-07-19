package me.chrr.camerapture.domain.observation;

import me.chrr.camerapture.domain.config.BiomeObservationConfig;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BiomeObservationStatsTest {
    private static final Identifier OVERWORLD = Identifier.of("minecraft", "overworld");
    private static final Identifier NETHER = Identifier.of("minecraft", "the_nether");
    private static final Identifier PLAINS = Identifier.of("minecraft", "plains");

    @Test
    void sameChunkIsCountedOnlyOnce() {
        BiomeObservationStats stats = new BiomeObservationStats(1_024, Set.of(PLAINS));
        assertTrue(stats.observe(OVERWORLD, 3, -4, PLAINS));
        assertFalse(stats.observe(OVERWORLD, 3, -4, PLAINS));
        assertEquals(1L, stats.total());
    }

    @Test
    void sameChunkCoordinatesInDifferentDimensionsAreDistinct() {
        BiomeObservationStats stats = new BiomeObservationStats(1_024, Set.of(PLAINS));
        assertTrue(stats.observe(OVERWORLD, 0, 0, PLAINS));
        assertTrue(stats.observe(NETHER, 0, 0, PLAINS));
        assertEquals(2L, stats.total());
    }

    @Test
    void filterMemoryIsFixed() {
        BiomeObservationStats stats = new BiomeObservationStats(2_048, Set.of(PLAINS));
        for (int index = 0; index < 10_000; index++) {
            stats.observe(OVERWORLD, index, index * 31, PLAINS);
        }
        assertEquals(2_048, stats.filterBits());
        assertEquals(256, stats.filterBytes().length);
    }

    @Test
    void rarityIsNeutralBelowMinimumSamplesAndClampedAboveIt() {
        BiomeObservationConfig config = new BiomeObservationConfig(true, 20, 10, 1.0, 0.75, 2.0, 1_024);
        BiomeObservationPolicy policy = new BiomeObservationPolicy(config);
        assertEquals(1.0, policy.rarityMultiplier(1, 9, 2));
        assertEquals((1.0 + 1.0) / (100.0 + 2.0), policy.smoothedFrequency(1, 100, 2));
        assertEquals(2.0, policy.rarityMultiplier(1, 100, 2));
        assertEquals(0.75, policy.rarityMultiplier(99, 100, 2));
    }
}
