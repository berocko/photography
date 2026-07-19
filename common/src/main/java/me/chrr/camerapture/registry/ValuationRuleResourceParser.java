package me.chrr.camerapture.registry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import me.chrr.camerapture.domain.valuation.LoadedValuationRules;
import me.chrr.camerapture.domain.valuation.ValuationRule;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Pure per-resource parser and registry-aware validator. */
public final class ValuationRuleResourceParser {
    public LoadedValuationRules parse(
            Map<Identifier, JsonElement> resources,
            ValidationContext context,
            long loadedAtEpochMillis
    ) {
        List<LoadedValuationRules.LoadedRule> loaded = new ArrayList<>();
        List<LoadedValuationRules.RuleDiagnostic> skipped = new ArrayList<>();
        List<String> notices = new ArrayList<>();

        resources.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            Identifier resourceId = entry.getKey();
            try {
                if (entry.getValue().isJsonObject()
                        && entry.getValue().getAsJsonObject().has("__camerapture_read_error")) {
                    throw new IllegalArgumentException("resource read/JSON error: "
                            + entry.getValue().getAsJsonObject().get("__camerapture_read_error").getAsString());
                }
                LoadedValuationRules.ObjectType objectType = objectType(resourceId, entry.getValue());
                ValuationRule rule = ValuationRule.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                        .getOrThrow(message -> new IllegalArgumentException("Codec: " + message));
                validate(rule, objectType, context, resourceId, notices);
                loaded.add(new LoadedValuationRules.LoadedRule(resourceId, objectType, rule));
            } catch (RuntimeException exception) {
                skipped.add(new LoadedValuationRules.RuleDiagnostic(resourceId, message(exception)));
            }
        });

        loaded.sort(Comparator.comparing(rule -> rule.resourceId().toString()));
        return LoadedValuationRules.of(loaded, skipped, notices, loadedAtEpochMillis);
    }

    private static LoadedValuationRules.ObjectType objectType(Identifier resourceId, JsonElement json) {
        String path = resourceId.getPath();
        if (path.startsWith("photo_values/entities/")) {
            return LoadedValuationRules.ObjectType.ENTITY;
        }
        if (path.startsWith("photo_values/biomes/")) {
            return LoadedValuationRules.ObjectType.BIOME;
        }
        if (path.startsWith("photo_values/tags/") && json.isJsonObject()) {
            JsonObject object = json.getAsJsonObject();
            if (!object.has("object_type") || !object.get("object_type").isJsonPrimitive()) {
                throw new IllegalArgumentException("tags resource requires object_type entity or biome");
            }
            return switch (object.get("object_type").getAsString()) {
                case "entity" -> LoadedValuationRules.ObjectType.ENTITY;
                case "biome" -> LoadedValuationRules.ObjectType.BIOME;
                default -> throw new IllegalArgumentException("unknown object_type");
            };
        }
        throw new IllegalArgumentException("resource is outside a supported valuation directory");
    }

    private static void validate(
            ValuationRule rule,
            LoadedValuationRules.ObjectType type,
            ValidationContext context,
            Identifier resourceId,
            List<String> notices
    ) {
        switch (rule.selector()) {
            case EXACT -> {
                Identifier target = Identifier.tryParse(rule.target());
                if (target == null) {
                    throw new IllegalArgumentException("exact target is not a valid identifier");
                }
                Set<Identifier> ids = type == LoadedValuationRules.ObjectType.ENTITY
                        ? context.entityIds() : context.biomeIds();
                if (!ids.isEmpty() && !ids.contains(target)) {
                    throw new IllegalArgumentException("exact target is not present in the registry: " + target);
                }
            }
            case TAG -> {
                Identifier tag = Identifier.tryParse(rule.target());
                if (tag == null) {
                    throw new IllegalArgumentException("tag target is not a valid identifier");
                }
                Set<Identifier> tags = type == LoadedValuationRules.ObjectType.ENTITY
                        ? context.entityTags() : context.biomeTags();
                if (!tags.contains(tag)) {
                    notices.add(resourceId + ": tag currently has no entries: " + tag);
                }
            }
            case NAMESPACE -> {
                if (!Identifier.isNamespaceValid(rule.target())) {
                    throw new IllegalArgumentException("invalid namespace target: " + rule.target());
                }
            }
        }
    }

    private static String message(RuntimeException exception) {
        return exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
    }

    public record ValidationContext(
            Set<Identifier> entityIds,
            Set<Identifier> biomeIds,
            Set<Identifier> entityTags,
            Set<Identifier> biomeTags
    ) {
        public ValidationContext {
            entityIds = Set.copyOf(entityIds);
            biomeIds = Set.copyOf(biomeIds);
            entityTags = Set.copyOf(entityTags);
            biomeTags = Set.copyOf(biomeTags);
        }

        public static ValidationContext empty() {
            return new ValidationContext(Set.of(), Set.of(), Set.of(), Set.of());
        }
    }
}
