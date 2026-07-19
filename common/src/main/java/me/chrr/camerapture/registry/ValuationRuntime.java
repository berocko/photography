package me.chrr.camerapture.registry;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.domain.config.GameplayConfig;
import me.chrr.camerapture.domain.valuation.LoadedValuationRules;
import net.minecraft.server.MinecraftServer;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/** Static loader-neutral lifecycle bridge. */
public final class ValuationRuntime {
    private static final AtomicReference<ServerValuationManager> ACTIVE = new AtomicReference<>();
    private static final AtomicReference<LoadedValuationRules> PENDING_RULES = new AtomicReference<>(LoadedValuationRules.empty());
    private static final ValuationRuleReloadService RELOAD_LISTENER = new ValuationRuleReloadService(
            () -> active().map(ServerValuationManager::validationContext)
                    .orElse(ValuationRuleResourceParser.ValidationContext.empty()),
            ValuationRuntime::applyRules,
            () -> Camerapture.CONFIG_MANAGER.getConfig().server.expedition.valuation.logUnknownEntities,
            () -> Camerapture.CONFIG_MANAGER.getConfig().server.expedition.valuation.logEmptyTags
    );

    private ValuationRuntime() {
    }

    public static ValuationRuleReloadService reloadListener() {
        return RELOAD_LISTENER;
    }

    public static void start(MinecraftServer server, Collection<String> mods) {
        GameplayConfig config;
        try {
            config = Camerapture.CONFIG_MANAGER.getConfig().server.gameplayConfig();
        } catch (RuntimeException exception) {
            Camerapture.LOGGER.error("invalid expedition server config; using safe defaults", exception);
            config = GameplayConfig.DEFAULT;
        }
        ServerValuationManager manager = new ServerValuationManager(server, config, mods);
        ACTIVE.set(manager);
        LoadedValuationRules rules;
        try {
            rules = RELOAD_LISTENER.loadNow(server.getResourceManager());
            PENDING_RULES.set(rules);
        } catch (RuntimeException exception) {
            Camerapture.LOGGER.error("initial valuation rule load failed; using previous valid rules", exception);
            rules = PENDING_RULES.get();
        }
        RELOAD_LISTENER.publishNow(rules);
    }

    public static void stop(MinecraftServer server) {
        ACTIVE.updateAndGet(manager -> manager != null && manager.isFor(server) ? null : manager);
    }

    public static void tick(MinecraftServer server) {
        activeFor(server).ifPresent(ServerValuationManager::tick);
    }

    public static Optional<ServerValuationManager> active() {
        return Optional.ofNullable(ACTIVE.get());
    }

    public static Optional<ServerValuationManager> activeFor(MinecraftServer server) {
        ServerValuationManager manager = ACTIVE.get();
        return manager == null || !manager.isFor(server) ? Optional.empty() : Optional.of(manager);
    }

    private static void applyRules(LoadedValuationRules rules) {
        Optional<ServerValuationManager> manager = active();
        if (manager.isPresent()) {
            manager.get().applyRules(rules);
        }
        PENDING_RULES.set(rules);
    }
}
