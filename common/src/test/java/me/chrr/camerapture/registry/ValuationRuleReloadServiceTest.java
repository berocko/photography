package me.chrr.camerapture.registry;

import me.chrr.camerapture.domain.valuation.LoadedValuationRules;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ValuationRuleReloadServiceTest {
    @Test
    void globalFailureRetainsLastSuccessfulRules() {
        AtomicInteger publishes = new AtomicInteger();
        ValuationRuleReloadService service = new ValuationRuleReloadService(
                ValuationRuleResourceParser.ValidationContext::empty,
                rules -> publishes.incrementAndGet()
        );
        LoadedValuationRules valid = LoadedValuationRules.of(List.of(), List.of(), List.of("valid"), 10L);

        service.apply(ValuationRuleReloadService.Prepared.success(valid), null, null);
        service.apply(ValuationRuleReloadService.Prepared.failure(new IllegalStateException("broken")), null, null);

        assertEquals(1, publishes.get());
        assertEquals(valid, service.lastSuccessful());
    }
}
