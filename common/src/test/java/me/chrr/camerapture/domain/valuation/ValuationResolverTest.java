package me.chrr.camerapture.domain.valuation;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalLong;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValuationResolverTest {
    private static final Identifier DRAGON = Identifier.of("examplemod", "ancient_dragon");
    private static final Identifier BOSSES = Identifier.of("camerapture", "bosses");

    @Test
    void exactRuleWinsOverTagNamespaceRuntimeAndAutomaticValues() {
        List<ValuationRule> rules = List.of(
                rule(ValuationRule.SelectorKind.NAMESPACE, "examplemod", 200),
                rule(ValuationRule.SelectorKind.TAG, BOSSES.toString(), 600),
                rule(ValuationRule.SelectorKind.EXACT, DRAGON.toString(), 1_200)
        );

        long value = ValuationResolver.resolve(
                DRAGON, Set.of(BOSSES), rules, OptionalLong.of(80), OptionalLong.of(40), 10
        );

        assertEquals(1_200, value);
    }

    @Test
    void runtimeThenAutomaticThenGlobalProvideFallbackOrder() {
        assertEquals(80, ValuationResolver.resolve(DRAGON, Set.of(), List.of(), OptionalLong.of(80), OptionalLong.of(40), 10));
        assertEquals(40, ValuationResolver.resolve(DRAGON, Set.of(), List.of(), OptionalLong.empty(), OptionalLong.of(40), 10));
        assertEquals(10, ValuationResolver.resolve(DRAGON, Set.of(), List.of(), OptionalLong.empty(), OptionalLong.empty(), 10));
    }

    @Test
    void additiveRuleSaturatesInsteadOfOverflowing() {
        ValuationRule add = new ValuationRule(
                ValuationRule.SelectorKind.EXACT, DRAGON.toString(), ValuationRule.Mode.ADD,
                10, 1, true, 0
        );
        assertEquals(Long.MAX_VALUE, ValuationResolver.resolve(
                DRAGON, Set.of(), List.of(add), OptionalLong.of(Long.MAX_VALUE), OptionalLong.empty(), 0
        ));
    }

    @Test
    void higherPriorityWinsWithinSameSelector() {
        ValuationRule low = new ValuationRule(ValuationRule.SelectorKind.EXACT, DRAGON.toString(),
                ValuationRule.Mode.OVERRIDE, 10, 1, true, 1);
        ValuationRule high = new ValuationRule(ValuationRule.SelectorKind.EXACT, DRAGON.toString(),
                ValuationRule.Mode.OVERRIDE, 20, 1, true, 2);
        assertEquals(20, ValuationResolver.resolve(
                DRAGON, Set.of(), List.of(high, low), OptionalLong.empty(), OptionalLong.empty(), 1
        ));
    }

    @Test
    void dataPackCodecParsesDocumentedOverrideShape() {
        ValuationRule decoded = ValuationRule.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {
                  "target": "examplemod:ancient_dragon",
                  "mode": "override",
                  "base_value": 1200,
                  "multiplier": 4.0,
                  "enabled": true
                }
                """)).getOrThrow();

        assertEquals(ValuationRule.SelectorKind.EXACT, decoded.selector());
        assertEquals(1_200, decoded.baseValue());
        assertEquals(4.0, decoded.multiplier());
    }

    @Test
    void dataPackCodecReturnsFieldErrorForUnknownMode() {
        assertTrue(ValuationRule.CODEC.parse(
                JsonOps.INSTANCE,
                JsonParser.parseString("{\"target\":\"minecraft:pig\",\"mode\":\"mystery\"}")
        ).error().isPresent());
    }

    private static ValuationRule rule(ValuationRule.SelectorKind selector, String target, long value) {
        return new ValuationRule(selector, target, ValuationRule.Mode.OVERRIDE, value, 1, true, 0);
    }
}
