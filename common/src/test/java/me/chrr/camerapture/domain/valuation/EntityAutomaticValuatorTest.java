package me.chrr.camerapture.domain.valuation;

import me.chrr.camerapture.domain.config.EntityValueConfig;
import me.chrr.camerapture.domain.registry.AutomaticValueStatus;
import org.junit.jupiter.api.Test;

import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityAutomaticValuatorTest {
    @Test
    void nonLivingEntityHasNoAutomaticValue() {
        var outcome = EntityAutomaticValuator.evaluate(
                EntityAutomaticValuator.AttributeSource.notLiving(), 1.0, EntityValueConfig.DEFAULT
        );
        assertTrue(outcome.automaticValue().isEmpty());
        assertEquals(AutomaticValueStatus.NOT_LIVING, outcome.status());
    }

    @Test
    void missingDefaultAttributesSafelyFallsBack() {
        var outcome = EntityAutomaticValuator.evaluate(
                EntityAutomaticValuator.AttributeSource.noDefaultAttributes(), 1.0, EntityValueConfig.DEFAULT
        );
        assertTrue(outcome.automaticValue().isEmpty());
        assertEquals(AutomaticValueStatus.NO_DEFAULT_ATTRIBUTES, outcome.status());
    }

    @Test
    void missingIndividualAttributeIsZeroAndStructured() {
        var outcome = EntityAutomaticValuator.evaluate(new EntityAutomaticValuator.AttributeSource(
                true, true, OptionalDouble.of(20), OptionalDouble.empty(), OptionalDouble.of(4)
        ), 1.0, EntityValueConfig.DEFAULT);
        assertTrue(outcome.automaticValue().isPresent());
        assertEquals(AutomaticValueStatus.MISSING_ATTRIBUTE, outcome.status());
        assertEquals(0.0, outcome.armor());
    }
}
