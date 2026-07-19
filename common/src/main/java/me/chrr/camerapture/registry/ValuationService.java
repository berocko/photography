package me.chrr.camerapture.registry;

import me.chrr.camerapture.domain.registry.RegistryScanSnapshot;
import me.chrr.camerapture.domain.valuation.LoadedValuationRules;
import me.chrr.camerapture.domain.valuation.ValuationBreakdown;
import net.minecraft.util.Identifier;

import java.util.Optional;
import java.util.OptionalLong;

/** Stable read-only gameplay boundary for server-authoritative valuation. */
public interface ValuationService {
    OptionalLong entityValue(Identifier entityId);

    OptionalLong biomeValue(Identifier biomeId);

    Optional<ValuationBreakdown> entityBreakdown(Identifier entityId);

    Optional<ValuationBreakdown> biomeBreakdown(Identifier biomeId);

    RegistryScanSnapshot snapshot();

    LoadedValuationRules loadedRules();
}
