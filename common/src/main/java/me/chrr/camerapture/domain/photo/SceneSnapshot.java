package me.chrr.camerapture.domain.photo;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Identifier;
import me.chrr.camerapture.domain.CodecSafety;

import java.util.List;
import java.util.Objects;

/** Versioned, server-authoritative facts used to reproduce a score. */
public record SceneSnapshot(
        int schemaVersion,
        Identifier dimension,
        long capturedAtEpochMillis,
        List<EntityObservation> entities,
        BiomeObservation biome,
        boolean newEntityDiscovery,
        boolean newBiomeDiscovery
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    private static final int MAX_SCORED_ENTITIES = 3;

    public static final Codec<SceneSnapshot> CODEC = CodecSafety.guard(RecordCodecBuilder.<SceneSnapshot>create(instance -> instance.group(
            Codec.INT.fieldOf("schema_version").forGetter(SceneSnapshot::schemaVersion),
            Identifier.CODEC.fieldOf("dimension").forGetter(SceneSnapshot::dimension),
            Codec.LONG.fieldOf("captured_at_epoch_millis").forGetter(SceneSnapshot::capturedAtEpochMillis),
            EntityObservation.CODEC.listOf().fieldOf("entities").forGetter(SceneSnapshot::entities),
            BiomeObservation.CODEC.fieldOf("biome").forGetter(SceneSnapshot::biome),
            Codec.BOOL.fieldOf("new_entity_discovery").forGetter(SceneSnapshot::newEntityDiscovery),
            Codec.BOOL.fieldOf("new_biome_discovery").forGetter(SceneSnapshot::newBiomeDiscovery)
    ).apply(instance, SceneSnapshot::new)).validate(SceneSnapshot::validateForCodec), "invalid scene snapshot");

    public SceneSnapshot {
        Objects.requireNonNull(dimension, "dimension");
        entities = List.copyOf(entities);
        Objects.requireNonNull(biome, "biome");
        if (schemaVersion <= 0 || capturedAtEpochMillis < 0 || entities.size() > MAX_SCORED_ENTITIES) {
            throw new IllegalArgumentException("invalid scene schema, timestamp, or entity count");
        }
    }

    private static DataResult<SceneSnapshot> validateForCodec(SceneSnapshot snapshot) {
        if (snapshot.schemaVersion() > CURRENT_SCHEMA_VERSION) {
            return DataResult.error(() -> "unsupported scene schema version " + snapshot.schemaVersion());
        }
        return DataResult.success(snapshot);
    }
}
