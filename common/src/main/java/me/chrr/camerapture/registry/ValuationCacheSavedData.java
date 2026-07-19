package me.chrr.camerapture.registry;

import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.PersistentState;

import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Vanilla world-level SavedData mounted in the primary overworld. */
public final class ValuationCacheSavedData extends PersistentState {
    private static final Logger LOGGER = LogManager.getLogger("Camerapture/ValuationCache");
    public static final String ID = "camerapture_valuation_cache";
    public static final Type<ValuationCacheSavedData> TYPE = new Type<>(
            () -> new ValuationCacheSavedData(ValuationCachePayload.empty(1 << 20)),
            ValuationCacheSavedData::load,
            DataFixTypes.LEVEL
    );

    private ValuationCachePayload payload;

    public ValuationCacheSavedData(ValuationCachePayload payload) {
        this.payload = Objects.requireNonNull(payload, "payload");
    }

    public ValuationCachePayload payload() {
        return payload;
    }

    public void replace(ValuationCachePayload payload) {
        this.payload = Objects.requireNonNull(payload, "payload");
        markDirty();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        return nbt.copyFrom(payload.toNbt());
    }

    private static ValuationCacheSavedData load(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        try {
            ValuationCachePayload.DecodeResult decoded = ValuationCachePayload.fromNbt(nbt);
            decoded.warnings().forEach(warning -> LOGGER.warn("valuation cache: {}", warning));
            return new ValuationCacheSavedData(decoded.payload());
        } catch (RuntimeException exception) {
            LOGGER.error("valuation cache header is incompatible or corrupt; rebuilding safely", exception);
            return new ValuationCacheSavedData(ValuationCachePayload.empty(1 << 20));
        }
    }
}
