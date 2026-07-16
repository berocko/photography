package me.chrr.camerapture.domain.photo;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.Objects;
import java.util.UUID;

/** Immutable capture facts. Reward-relevant values are supplied by the server. */
public record PhotoMetadata(
        UUID ownerId,
        String ownerName,
        long capturedAtEpochMillis,
        Identifier dimension,
        String imageFormat,
        int width,
        int height
) {
    public static final Codec<PhotoMetadata> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Uuids.CODEC.fieldOf("owner_id").forGetter(PhotoMetadata::ownerId),
            Codec.STRING.fieldOf("owner_name").forGetter(PhotoMetadata::ownerName),
            Codec.LONG.fieldOf("captured_at_epoch_millis").forGetter(PhotoMetadata::capturedAtEpochMillis),
            Identifier.CODEC.fieldOf("dimension").forGetter(PhotoMetadata::dimension),
            Codec.STRING.fieldOf("image_format").forGetter(PhotoMetadata::imageFormat),
            Codec.INT.fieldOf("width").forGetter(PhotoMetadata::width),
            Codec.INT.fieldOf("height").forGetter(PhotoMetadata::height)
    ).apply(instance, PhotoMetadata::new));

    public PhotoMetadata {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(ownerName, "ownerName");
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(imageFormat, "imageFormat");
        if (capturedAtEpochMillis < 0 || width <= 0 || height <= 0) {
            throw new IllegalArgumentException("capture timestamp and dimensions must be positive");
        }
    }
}
