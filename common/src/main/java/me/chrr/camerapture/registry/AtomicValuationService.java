package me.chrr.camerapture.registry;

import me.chrr.camerapture.domain.registry.RegistryScanSnapshot;
import me.chrr.camerapture.domain.valuation.LoadedValuationRules;
import me.chrr.camerapture.domain.valuation.ValuationBreakdown;
import me.chrr.camerapture.domain.valuation.ValuationCatalog;
import net.minecraft.util.Identifier;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicReference;

/** Publishes snapshot/catalog pairs in one atomic operation. */
public final class AtomicValuationService implements ValuationService {
    private final AtomicReference<State> state = new AtomicReference<>(new State(
            RegistryScanSnapshot.empty(), ValuationCatalog.empty(), LoadedValuationRules.empty()
    ));

    public void replace(RegistryScanSnapshot snapshot, ValuationCatalog catalog, LoadedValuationRules rules) {
        state.set(new State(snapshot, catalog, rules));
    }

    @Override
    public OptionalLong entityValue(Identifier entityId) {
        return state.get().catalog().entityValue(entityId);
    }

    @Override
    public OptionalLong biomeValue(Identifier biomeId) {
        return state.get().catalog().biomeValue(biomeId);
    }

    @Override
    public Optional<ValuationBreakdown> entityBreakdown(Identifier entityId) {
        return state.get().catalog().entityBreakdown(entityId);
    }

    @Override
    public Optional<ValuationBreakdown> biomeBreakdown(Identifier biomeId) {
        return state.get().catalog().biomeBreakdown(Objects.requireNonNull(biomeId, "biomeId"));
    }

    @Override
    public RegistryScanSnapshot snapshot() {
        return state.get().snapshot();
    }

    @Override
    public LoadedValuationRules loadedRules() {
        return state.get().rules();
    }

    private record State(
            RegistryScanSnapshot snapshot,
            ValuationCatalog catalog,
            LoadedValuationRules rules
    ) {
        private State {
            Objects.requireNonNull(snapshot, "snapshot");
            Objects.requireNonNull(catalog, "catalog");
            Objects.requireNonNull(rules, "rules");
        }
    }
}
