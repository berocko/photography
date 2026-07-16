package me.chrr.camerapture.domain.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Identifier;

import java.util.Objects;

public record CurrencyConfig(Identifier provider, boolean teamShared) {
    public static final Identifier INTERNAL_PROVIDER = Identifier.of("camerapture", "internal");
    public static final CurrencyConfig DEFAULT = new CurrencyConfig(INTERNAL_PROVIDER, false);

    public static final Codec<CurrencyConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.optionalFieldOf("provider", INTERNAL_PROVIDER).forGetter(CurrencyConfig::provider),
            Codec.BOOL.optionalFieldOf("team_shared", false).forGetter(CurrencyConfig::teamShared)
    ).apply(instance, CurrencyConfig::new));

    public CurrencyConfig {
        Objects.requireNonNull(provider, "provider");
    }
}
