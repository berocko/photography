package me.chrr.camerapture.registry;

import com.google.gson.JsonParser;
import me.chrr.camerapture.domain.valuation.LoadedValuationRules;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValuationRuleResourceParserTest {
    private final ValuationRuleResourceParser parser = new ValuationRuleResourceParser();
    private final ValuationRuleResourceParser.ValidationContext context = new ValuationRuleResourceParser.ValidationContext(
            Set.of(Identifier.of("minecraft", "zombie")),
            Set.of(Identifier.of("minecraft", "plains")),
            Set.of(Identifier.of("minecraft", "skeletons")),
            Set.of()
    );

    @Test
    void malformedJsonOnlySkipsItsResource() {
        var loaded = parser.parse(Map.of(
                Identifier.of("camerapture", "photo_values/entities/good.json"),
                JsonParser.parseString("{\"target\":\"minecraft:zombie\",\"base_value\":25}"),
                Identifier.of("camerapture", "photo_values/entities/bad.json"),
                JsonParser.parseString("{\"mode\":\"mystery\",\"target\":\"minecraft:zombie\"}")
        ), context, 1L);
        assertEquals(1, loaded.rules().size());
        assertEquals(1, loaded.skipped().size());
    }

    @Test
    void missingExactTargetIsRejected() {
        var loaded = parser.parse(Map.of(
                Identifier.of("camerapture", "photo_values/entities/missing.json"),
                JsonParser.parseString("{\"target\":\"example:missing\"}")
        ), context, 1L);
        assertTrue(loaded.rules().isEmpty());
        assertTrue(loaded.skipped().getFirst().reason().contains("not present"));
    }

    @Test
    void emptyTagIsANoticeNotAFailure() {
        var loaded = parser.parse(Map.of(
                Identifier.of("camerapture", "photo_values/biomes/tag.json"),
                JsonParser.parseString("{\"selector\":\"tag\",\"target\":\"example:empty\"}")
        ), context, 1L);
        assertEquals(1, loaded.rules().size());
        assertEquals(1, loaded.notices().size());
    }

    @Test
    void tagsDirectoryRequiresAndUsesObjectType() {
        var loaded = parser.parse(Map.of(
                Identifier.of("camerapture", "photo_values/tags/skeletons.json"),
                JsonParser.parseString("{\"object_type\":\"entity\",\"selector\":\"tag\",\"target\":\"minecraft:skeletons\"}")
        ), context, 1L);
        assertEquals(LoadedValuationRules.ObjectType.ENTITY, loaded.rules().getFirst().objectType());
    }
}
