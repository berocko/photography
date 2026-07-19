package me.chrr.camerapture.fabric;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.registry.ValuationRuntime;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceReloader;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/** Fabric identity wrapper around the common transactional reload listener. */
public final class FabricValuationReloadListener implements IdentifiableResourceReloadListener {
    @Override
    public Identifier getFabricId() {
        return Camerapture.id("valuation_rules");
    }

    @Override
    public CompletableFuture<Void> reload(
            ResourceReloader.Synchronizer synchronizer,
            ResourceManager manager,
            Profiler prepareProfiler,
            Profiler applyProfiler,
            Executor prepareExecutor,
            Executor applyExecutor
    ) {
        return ValuationRuntime.reloadListener().reload(
                synchronizer, manager, prepareProfiler, applyProfiler, prepareExecutor, applyExecutor
        );
    }
}
