package me.chrr.camerapture.registry;

import me.chrr.camerapture.domain.config.GameplayConfig;
import me.chrr.camerapture.domain.registry.BiomeRegistryEntry;
import me.chrr.camerapture.domain.registry.EntityRegistryEntry;
import me.chrr.camerapture.domain.valuation.EntityAutomaticValuator;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.DefaultAttributeRegistry;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.stream.Collectors;

/** Safe registry scanner. It never constructs or spawns an entity. */
public final class RegistryScanService {
    public ScanResult scan(MinecraftServer server, GameplayConfig config) {
        long startedNanos = System.nanoTime();
        Map<Identifier, EntityRegistryEntry> entities = scanEntities(config);
        Map<Identifier, BiomeRegistryEntry> biomes = scanBiomes(server, config);
        long durationMillis = Math.max(0L, (System.nanoTime() - startedNanos) / 1_000_000L);
        return new ScanResult(entities, biomes, durationMillis);
    }

    private Map<Identifier, EntityRegistryEntry> scanEntities(GameplayConfig config) {
        Map<Identifier, EntityRegistryEntry> entries = new HashMap<>();
        for (EntityType<?> type : Registries.ENTITY_TYPE) {
            Identifier id = Registries.ENTITY_TYPE.getId(type);
            Set<Identifier> tags = Registries.ENTITY_TYPE.getEntry(type).streamTags()
                    .map(tag -> tag.id())
                    .collect(Collectors.toUnmodifiableSet());
            boolean hasDefaults = DefaultAttributeRegistry.hasDefinitionFor(type);
            if (!config.registryScan().includeNonLivingEntities() && !hasDefaults) {
                continue;
            }
            EntityAutomaticValuator.Outcome outcome;
            try {
                if (!config.registryScan().enabled()) {
                    outcome = EntityAutomaticValuator.Outcome.unavailable(
                            me.chrr.camerapture.domain.registry.AutomaticValueStatus.NO_DEFAULT_ATTRIBUTES,
                            "registry automatic scanning is disabled"
                    );
                } else if (!hasDefaults) {
                    outcome = EntityAutomaticValuator.evaluate(
                            EntityAutomaticValuator.AttributeSource.noDefaultAttributes(), 1.0, config.entityValues()
                    );
                } else {
                    DefaultAttributeContainer attributes = getDefaultAttributes(type);
                    outcome = EntityAutomaticValuator.evaluate(new EntityAutomaticValuator.AttributeSource(
                                    true,
                                    true,
                                    value(attributes, EntityAttributes.GENERIC_MAX_HEALTH),
                                    value(attributes, EntityAttributes.GENERIC_ARMOR),
                                    value(attributes, EntityAttributes.GENERIC_ATTACK_DAMAGE)
                            ),
                            type.getSpawnGroup() == SpawnGroup.MONSTER ? config.registryScan().hostileMultiplier() : 1.0,
                            config.entityValues()
                    );
                }
            } catch (RuntimeException exception) {
                outcome = EntityAutomaticValuator.readError(exception);
            }
            entries.put(id, new EntityRegistryEntry(
                    id,
                    tags,
                    type.getSpawnGroup().getName(),
                    hasDefaults,
                    outcome.maxHealth(),
                    outcome.armor(),
                    outcome.attackDamage(),
                    0.0,
                    outcome.automaticValue(),
                    outcome.status(),
                    outcome.reason()
            ));
        }
        return Map.copyOf(entries);
    }

    private Map<Identifier, BiomeRegistryEntry> scanBiomes(MinecraftServer server, GameplayConfig config) {
        Registry<Biome> registry = server.getRegistryManager().get(RegistryKeys.BIOME);
        Map<Identifier, BiomeRegistryEntry> entries = new HashMap<>();
        for (RegistryEntry.Reference<Biome> entry : registry.streamEntries().toList()) {
            Identifier id = entry.registryKey().getValue();
            Set<Identifier> tags = entry.streamTags().map(tag -> tag.id()).collect(Collectors.toUnmodifiableSet());
            long base = config.registryScan().globalBiomeDefault();
            entries.put(id, new BiomeRegistryEntry(id, tags, base, 0L, 0.0, 1.0, base));
        }
        return Map.copyOf(entries);
    }

    @SuppressWarnings("unchecked")
    private static DefaultAttributeContainer getDefaultAttributes(EntityType<?> type) {
        return DefaultAttributeRegistry.get((EntityType<? extends LivingEntity>) type);
    }

    private static OptionalDouble value(
            DefaultAttributeContainer attributes,
            RegistryEntry<net.minecraft.entity.attribute.EntityAttribute> attribute
    ) {
        return attributes.has(attribute) ? OptionalDouble.of(attributes.getValue(attribute)) : OptionalDouble.empty();
    }

    public record ScanResult(
            Map<Identifier, EntityRegistryEntry> entities,
            Map<Identifier, BiomeRegistryEntry> biomes,
            long durationMillis
    ) {
        public ScanResult {
            entities = Map.copyOf(entities);
            biomes = Map.copyOf(biomes);
        }
    }
}
