package me.chrr.camerapture.domain.photo;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhotoCodecTest {
    @Test
    void roundTripsVersionedPhotoRecord() {
        UUID owner = UUID.randomUUID();
        PhotoId id = new PhotoId(UUID.randomUUID());
        SceneSnapshot scene = new SceneSnapshot(
                1,
                Identifier.of("minecraft", "overworld"),
                1234,
                List.of(new EntityObservation(
                        Identifier.of("minecraft", "fox"), Optional.empty(), 30, .8, .9
                )),
                new BiomeObservation(Identifier.of("minecraft", "taiga"), OptionalLong.of(42), 20),
                true,
                false
        );
        PhotoRecord expected = new PhotoRecord(
                1,
                id,
                new PhotoMetadata(owner, "Explorer", 1234, scene.dimension(), "webp", 320, 180),
                scene,
                PhotoRecord.Status.SAVED,
                Optional.empty()
        );

        JsonElement encoded = PhotoRecord.CODEC.encodeStart(JsonOps.INSTANCE, expected).getOrThrow();
        PhotoRecord decoded = PhotoRecord.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();

        assertEquals(42, encoded.getAsJsonObject().getAsJsonObject("scene")
                .getAsJsonObject("biome").get("chunk").getAsLong());
        assertEquals(expected, decoded);
    }

    @Test
    void rejectsUnsupportedFutureSchema() {
        JsonElement encoded = SceneSnapshot.CODEC.encodeStart(JsonOps.INSTANCE, new SceneSnapshot(
                2,
                Identifier.of("minecraft", "overworld"),
                1,
                List.of(),
                new BiomeObservation(Identifier.of("minecraft", "plains"), OptionalLong.empty(), 1),
                false,
                false
        )).resultOrPartial().orElse(null);

        assertTrue(encoded == null, "future schema must fail codec validation");
    }
}
