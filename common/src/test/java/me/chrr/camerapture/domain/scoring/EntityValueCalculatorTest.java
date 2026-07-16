package me.chrr.camerapture.domain.scoring;

import me.chrr.camerapture.domain.config.EntityValueConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EntityValueCalculatorTest {
    @Test
    void compressesExtremeAttributesWithLog1p() {
        assertEquals(Math.log1p(1_000_000.0), EntityValueCalculator.compress(1_000_000.0), 1.0e-12);
        assertEquals(0.0, EntityValueCalculator.compress(-5.0));
        assertEquals(0.0, EntityValueCalculator.compress(Double.NaN));
    }

    @Test
    void clampsCalculatedValuesToConfiguredBounds() {
        EntityValueConfig config = new EntityValueConfig(100, 100, 100, 100, 10, 500);

        assertEquals(10, EntityValueCalculator.calculate(0, 0, 0, 0, 1, 1, 1, 1, config));
        assertEquals(500, EntityValueCalculator.calculate(
                Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE,
                Double.MAX_VALUE, 2, 2, 2, config
        ));
    }
}
