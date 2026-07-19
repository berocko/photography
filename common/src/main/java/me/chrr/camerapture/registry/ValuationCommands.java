package me.chrr.camerapture.registry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.domain.registry.BiomeRegistryEntry;
import me.chrr.camerapture.domain.registry.EntityRegistryEntry;
import me.chrr.camerapture.domain.registry.RegistryScanSnapshot;
import me.chrr.camerapture.domain.valuation.ValuationBreakdown;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/** Permission-gated server-only valuation diagnostics. */
public final class ValuationCommands {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ValuationCommands() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("camerapture")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("valuation")
                        .requires(source -> Camerapture.CONFIG_MANAGER.getConfig().server.expedition.valuation.debugCommands)
                        .then(CommandManager.literal("status").executes(ValuationCommands::status))
                        .then(CommandManager.literal("rebuild").executes(ValuationCommands::rebuild))
                        .then(CommandManager.literal("entity")
                                .then(CommandManager.argument("id", IdentifierArgumentType.identifier())
                                        .suggests((context, builder) -> ValuationRuntime.active()
                                                .map(manager -> CommandSource.suggestIdentifiers(
                                                        manager.service().snapshot().entities().keySet(), builder))
                                                .orElseGet(builder::buildFuture))
                                        .executes(ValuationCommands::entity)))
                        .then(CommandManager.literal("biome")
                                .then(CommandManager.argument("id", IdentifierArgumentType.identifier())
                                        .suggests((context, builder) -> ValuationRuntime.active()
                                                .map(manager -> CommandSource.suggestIdentifiers(
                                                        manager.service().snapshot().biomes().keySet(), builder))
                                                .orElseGet(builder::buildFuture))
                                        .executes(ValuationCommands::biome)))
                        .then(CommandManager.literal("dump").executes(ValuationCommands::dump))
                        .then(CommandManager.literal("observations")
                                .then(CommandManager.literal("reset").executes(ValuationCommands::resetObservations)))));
    }

    private static int status(CommandContext<ServerCommandSource> context) {
        return manager(context).map(manager -> {
            RegistryScanSnapshot snapshot = manager.service().snapshot();
            context.getSource().sendFeedback(() -> Text.literal(
                    "Valuation: entities=" + snapshot.entities().size()
                            + ", biomes=" + snapshot.biomes().size()
                            + ", rules=" + manager.service().loadedRules().rules().size()
                            + ", skipped=" + manager.service().loadedRules().skipped().size()
                            + ", cache_schema=" + ValuationCachePayload.CURRENT_SCHEMA_VERSION
                            + ", filter_bits=" + manager.observationFilterBits()
                            + ", fingerprint=" + snapshot.fingerprint().sha256()), false);
            return 1;
        }).orElse(0);
    }

    private static int rebuild(CommandContext<ServerCommandSource> context) {
        return manager(context).map(manager -> {
            manager.rebuild("admin_command");
            context.getSource().sendFeedback(() -> Text.literal("Valuation registry and catalog rebuilt."), true);
            return 1;
        }).orElse(0);
    }

    private static int entity(CommandContext<ServerCommandSource> context) {
        Identifier id = IdentifierArgumentType.getIdentifier(context, "id");
        return manager(context).map(manager -> {
            EntityRegistryEntry entry = manager.service().snapshot().entities().get(id);
            ValuationBreakdown value = manager.service().entityBreakdown(id).orElse(null);
            if (entry == null || value == null) {
                context.getSource().sendError(Text.literal("Unknown entity ID: " + id));
                return 0;
            }
            context.getSource().sendFeedback(() -> Text.literal(
                    id + " attributes[health=" + entry.maxHealth() + ", armor=" + entry.armor()
                            + ", attack=" + entry.attackDamage() + "] automatic=" + optional(value.automaticValue())
                            + " status=" + entry.automaticValueStatus() + " runtime=" + optional(value.runtimeObservationValue())
                            + " matched=" + value.matchedRuleResource().orElse("none")
                            + " selector=" + value.matchedRule().map(rule -> rule.selector().name()).orElse("fallback")
                            + " final=" + value.finalValue() + " cache=" + value.cacheStatus()), false);
            return 1;
        }).orElse(0);
    }

    private static int biome(CommandContext<ServerCommandSource> context) {
        Identifier id = IdentifierArgumentType.getIdentifier(context, "id");
        return manager(context).map(manager -> {
            BiomeRegistryEntry entry = manager.service().snapshot().biomes().get(id);
            ValuationBreakdown value = manager.service().biomeBreakdown(id).orElse(null);
            if (entry == null || value == null) {
                context.getSource().sendError(Text.literal("Unknown biome ID: " + id));
                return 0;
            }
            context.getSource().sendFeedback(() -> Text.literal(
                    id + " base=" + entry.automaticBaseValue() + " observed=" + entry.observedCount()
                            + " frequency=" + entry.smoothedObservedFrequency()
                            + " rarity_multiplier=" + entry.runtimeRarityMultiplier()
                            + " runtime=" + optional(value.runtimeObservationValue())
                            + " matched=" + value.matchedRuleResource().orElse("none")
                            + " selector=" + value.matchedRule().map(rule -> rule.selector().name()).orElse("fallback")
                            + " final=" + value.finalValue() + " cache=" + value.cacheStatus()), false);
            return 1;
        }).orElse(0);
    }

    private static int resetObservations(CommandContext<ServerCommandSource> context) {
        return manager(context).map(manager -> {
            manager.resetObservations();
            context.getSource().sendFeedback(() -> Text.literal("Biome observations reset."), true);
            return 1;
        }).orElse(0);
    }

    private static int dump(CommandContext<ServerCommandSource> context) {
        return manager(context).map(manager -> {
            RegistryScanSnapshot snapshot = manager.service().snapshot();
            Path directory = context.getSource().getServer().getSavePath(WorldSavePath.ROOT)
                    .resolve("camerapture").resolve("debug");
            Path output = directory.resolve("valuation-" + System.currentTimeMillis() + ".json");
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    Files.createDirectories(directory);
                    Files.writeString(output, GSON.toJson(toJson(snapshot, manager)));
                    context.getSource().getServer().execute(() -> context.getSource().sendFeedback(
                            () -> Text.literal("Valuation dump written to " + output.getFileName()), false));
                } catch (Exception exception) {
                    Camerapture.LOGGER.error("failed to write valuation dump", exception);
                    context.getSource().getServer().execute(() -> context.getSource().sendError(
                            Text.literal("Failed to write valuation dump; see server log.")));
                }
            });
            context.getSource().sendFeedback(() -> Text.literal("Valuation dump scheduled."), false);
            return 1;
        }).orElse(0);
    }

    private static JsonObject toJson(RegistryScanSnapshot snapshot, ServerValuationManager manager) {
        JsonObject root = new JsonObject();
        root.addProperty("fingerprint", snapshot.fingerprint().sha256());
        root.addProperty("rule_digest", snapshot.ruleDigest());
        root.addProperty("cache_schema", ValuationCachePayload.CURRENT_SCHEMA_VERSION);
        root.addProperty("observation_filter_bits", manager.observationFilterBits());
        JsonArray entities = new JsonArray();
        snapshot.entities().values().stream().sorted(Comparator.comparing(entry -> entry.id().toString()))
                .forEach(entry -> {
                    JsonObject json = new JsonObject();
                    json.addProperty("id", entry.id().toString());
                    json.addProperty("status", entry.automaticValueStatus().name());
                    json.addProperty("automatic_value", entry.automaticValue().orElse(-1L));
                    json.addProperty("max_health", entry.maxHealth());
                    json.addProperty("armor", entry.armor());
                    json.addProperty("attack_damage", entry.attackDamage());
                    json.addProperty("final_value", manager.service().entityValue(entry.id()).orElse(-1L));
                    entities.add(json);
                });
        root.add("entities", entities);
        JsonArray biomes = new JsonArray();
        snapshot.biomes().values().stream().sorted(Comparator.comparing(entry -> entry.id().toString()))
                .forEach(entry -> {
                    JsonObject json = new JsonObject();
                    json.addProperty("id", entry.id().toString());
                    json.addProperty("automatic_base_value", entry.automaticBaseValue());
                    json.addProperty("observed_count", entry.observedCount());
                    json.addProperty("smoothed_frequency", entry.smoothedObservedFrequency());
                    json.addProperty("runtime_rarity_multiplier", entry.runtimeRarityMultiplier());
                    json.addProperty("final_value", entry.finalResolvedValue());
                    biomes.add(json);
                });
        root.add("biomes", biomes);
        return root;
    }

    private static java.util.Optional<ServerValuationManager> manager(CommandContext<ServerCommandSource> context) {
        java.util.Optional<ServerValuationManager> active = ValuationRuntime.activeFor(context.getSource().getServer());
        if (active.isEmpty()) {
            context.getSource().sendError(Text.literal("Valuation service is not active."));
        }
        return active;
    }

    private static String optional(java.util.OptionalLong value) {
        return value.isPresent() ? Long.toString(value.getAsLong()) : "none";
    }
}
