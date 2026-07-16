package me.chrr.camerapture.domain.currency;

import me.chrr.camerapture.domain.photo.PhotoId;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record TransactionContext(
        Identifier reason,
        Optional<PhotoId> photoId,
        Map<String, String> auditMetadata
) {
    public TransactionContext {
        Objects.requireNonNull(reason, "reason");
        photoId = Objects.requireNonNull(photoId, "photoId");
        auditMetadata = Map.copyOf(auditMetadata);
    }
}
