package me.chrr.camerapture.domain.photo;

import com.mojang.serialization.Codec;
import net.minecraft.util.Uuids;

import java.util.Objects;
import java.util.UUID;

/** Stable identity shared by the image blob and all server-owned metadata. */
public record PhotoId(UUID value) {
    public static final Codec<PhotoId> CODEC = Uuids.CODEC.xmap(PhotoId::new, PhotoId::value);

    public PhotoId {
        Objects.requireNonNull(value, "value");
    }
}
