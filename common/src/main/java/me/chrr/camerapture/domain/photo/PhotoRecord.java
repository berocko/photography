package me.chrr.camerapture.domain.photo;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.chrr.camerapture.domain.CodecSafety;

import java.util.Objects;
import java.util.Optional;

/** Canonical server-owned metadata record; the WebP bytes remain in blob storage. */
public record PhotoRecord(
        int schemaVersion,
        PhotoId id,
        PhotoMetadata metadata,
        SceneSnapshot scene,
        Status status,
        Optional<ScoreBreakdown> score
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public enum Status {
        SAVED,
        SUBMITTED
    }

    private static final Codec<Status> STATUS_CODEC = Codec.STRING.comapFlatMap(
            value -> {
                try {
                    return DataResult.success(Status.valueOf(value.toUpperCase(java.util.Locale.ROOT)));
                } catch (IllegalArgumentException exception) {
                    return DataResult.error(() -> "unknown photo status: " + value);
                }
            },
            value -> value.name().toLowerCase(java.util.Locale.ROOT)
    );

    public static final Codec<PhotoRecord> CODEC = CodecSafety.guard(RecordCodecBuilder.<PhotoRecord>create(instance -> instance.group(
            Codec.INT.fieldOf("schema_version").forGetter(PhotoRecord::schemaVersion),
            PhotoId.CODEC.fieldOf("id").forGetter(PhotoRecord::id),
            PhotoMetadata.CODEC.fieldOf("metadata").forGetter(PhotoRecord::metadata),
            SceneSnapshot.CODEC.fieldOf("scene").forGetter(PhotoRecord::scene),
            STATUS_CODEC.fieldOf("status").forGetter(PhotoRecord::status),
            ScoreBreakdown.CODEC.optionalFieldOf("score").forGetter(PhotoRecord::score)
    ).apply(instance, PhotoRecord::new)).validate(PhotoRecord::validateForCodec), "invalid photo record");

    public PhotoRecord {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(metadata, "metadata");
        Objects.requireNonNull(scene, "scene");
        Objects.requireNonNull(status, "status");
        score = Objects.requireNonNull(score, "score");
        if (schemaVersion <= 0 || (status == Status.SUBMITTED && score.isEmpty())) {
            throw new IllegalArgumentException("invalid photo schema or submitted record without score");
        }
    }

    private static DataResult<PhotoRecord> validateForCodec(PhotoRecord record) {
        if (record.schemaVersion() > CURRENT_SCHEMA_VERSION) {
            return DataResult.error(() -> "unsupported photo schema version " + record.schemaVersion());
        }
        return DataResult.success(record);
    }
}
