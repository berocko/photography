package me.chrr.camerapture.domain.photo;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Identifier;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

/** Server observation of the biome and optional source chunk. */
public record BiomeObservation(Identifier biome, OptionalLong chunkKey, long baseValue) {
    private static final MapCodec<OptionalLong> OPTIONAL_LONG_FIELD = Codec.LONG.optionalFieldOf("chunk")
            .xmap(value -> value.map(OptionalLong::of).orElseGet(OptionalLong::empty),
                    value -> value.isPresent() ? Optional.of(value.getAsLong()) : Optional.empty());

    public static final Codec<BiomeObservation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("biome").forGetter(BiomeObservation::biome),
            OPTIONAL_LONG_FIELD.forGetter(BiomeObservation::chunkKey),
            Codec.LONG.fieldOf("base_value").forGetter(BiomeObservation::baseValue)
    ).apply(instance, BiomeObservation::new));

    public BiomeObservation {
        Objects.requireNonNull(biome, "biome");
        chunkKey = Objects.requireNonNull(chunkKey, "chunkKey");
        if (baseValue < 0) {
            throw new IllegalArgumentException("baseValue must be non-negative");
        }
    }
}
