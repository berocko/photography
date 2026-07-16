package me.chrr.camerapture.domain.photo;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/** Persisted explanation of a server-computed reward. */
public record ScoreBreakdown(
        int algorithmVersion,
        double primaryEntityValue,
        double secondaryEntityValue,
        double tertiaryEntityValue,
        double biomeValue,
        double visibilityMultiplier,
        double compositionMultiplier,
        double discoveryMultiplier,
        double instanceMultiplier,
        double typeDecayMultiplier,
        double unclampedReward,
        long finalReward,
        List<String> notes
) {
    public static final int CURRENT_ALGORITHM_VERSION = 1;

    public static final Codec<ScoreBreakdown> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("algorithm_version").forGetter(ScoreBreakdown::algorithmVersion),
            Codec.DOUBLE.fieldOf("primary_entity_value").forGetter(ScoreBreakdown::primaryEntityValue),
            Codec.DOUBLE.fieldOf("secondary_entity_value").forGetter(ScoreBreakdown::secondaryEntityValue),
            Codec.DOUBLE.fieldOf("tertiary_entity_value").forGetter(ScoreBreakdown::tertiaryEntityValue),
            Codec.DOUBLE.fieldOf("biome_value").forGetter(ScoreBreakdown::biomeValue),
            Codec.DOUBLE.fieldOf("visibility_multiplier").forGetter(ScoreBreakdown::visibilityMultiplier),
            Codec.DOUBLE.fieldOf("composition_multiplier").forGetter(ScoreBreakdown::compositionMultiplier),
            Codec.DOUBLE.fieldOf("discovery_multiplier").forGetter(ScoreBreakdown::discoveryMultiplier),
            Codec.DOUBLE.fieldOf("instance_multiplier").forGetter(ScoreBreakdown::instanceMultiplier),
            Codec.DOUBLE.fieldOf("type_decay_multiplier").forGetter(ScoreBreakdown::typeDecayMultiplier),
            Codec.DOUBLE.fieldOf("unclamped_reward").forGetter(ScoreBreakdown::unclampedReward),
            Codec.LONG.fieldOf("final_reward").forGetter(ScoreBreakdown::finalReward),
            Codec.STRING.listOf().fieldOf("notes").forGetter(ScoreBreakdown::notes)
    ).apply(instance, ScoreBreakdown::new));

    public ScoreBreakdown {
        notes = List.copyOf(notes);
        if (algorithmVersion <= 0 || finalReward < 0) {
            throw new IllegalArgumentException("invalid score version or reward");
        }
    }
}
