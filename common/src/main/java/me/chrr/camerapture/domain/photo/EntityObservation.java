package me.chrr.camerapture.domain.photo;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Server observation of one visible entity at capture time. */
public record EntityObservation(
        Identifier entityType,
        Optional<UUID> entityId,
        long baseValue,
        double visibility,
        double composition
) {
    public static final Codec<EntityObservation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("entity_type").forGetter(EntityObservation::entityType),
            Uuids.CODEC.optionalFieldOf("entity_id").forGetter(EntityObservation::entityId),
            Codec.LONG.fieldOf("base_value").forGetter(EntityObservation::baseValue),
            Codec.DOUBLE.fieldOf("visibility").forGetter(EntityObservation::visibility),
            Codec.DOUBLE.fieldOf("composition").forGetter(EntityObservation::composition)
    ).apply(instance, EntityObservation::new));

    public EntityObservation {
        Objects.requireNonNull(entityType, "entityType");
        entityId = Objects.requireNonNull(entityId, "entityId");
        if (baseValue < 0 || !unitInterval(visibility) || !unitInterval(composition)) {
            throw new IllegalArgumentException("entity values must be non-negative and multipliers within [0, 1]");
        }
    }

    private static boolean unitInterval(double value) {
        return Double.isFinite(value) && value >= 0.0 && value <= 1.0;
    }
}
