package me.chrr.camerapture.registry;

import com.mojang.serialization.JsonOps;
import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.domain.config.GameplayConfig;
import me.chrr.camerapture.domain.observation.BiomeObservationStats;
import me.chrr.camerapture.domain.observation.FixedBloomFilter;
import me.chrr.camerapture.domain.registry.BiomeRegistryEntry;
import me.chrr.camerapture.domain.registry.EntityRegistryEntry;
import me.chrr.camerapture.domain.registry.RegistryFingerprint;
import me.chrr.camerapture.domain.registry.RegistryScanSnapshot;
import me.chrr.camerapture.domain.valuation.LoadedValuationRules;
import me.chrr.camerapture.domain.valuation.ValuationCatalogBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Owns the server-thread scan, cache, observation, and atomic catalog lifecycle. */
public final class ServerValuationManager {
    private final MinecraftServer server;
    private final GameplayConfig config;
    private final List<String> mods;
    private final RegistryScanService scanner = new RegistryScanService();
    private final AtomicValuationService service = new AtomicValuationService();
    private final ValuationCacheSavedData savedData;
    private final BiomeObservationStats observations;

    private LoadedValuationRules rules = LoadedValuationRules.empty();
    private RegistryScanService.ScanResult scan;
    private long tickCounter;
    private long lastSuccessfulReload;
    private long lastRebuild;

    public ServerValuationManager(MinecraftServer server, GameplayConfig config, Collection<String> mods) {
        this.server = Objects.requireNonNull(server, "server");
        this.config = Objects.requireNonNull(config, "config");
        this.mods = mods.stream().sorted().toList();
        this.savedData = server.getOverworld().getPersistentStateManager()
                .getOrCreate(ValuationCacheSavedData.TYPE, ValuationCacheSavedData.ID);
        ValuationCachePayload cache = savedData.payload();
        this.scan = scanner.scan(server, config);
        Set<Identifier> currentBiomes = scan.biomes().keySet();
        FixedBloomFilter filter = cache.observationFilterBits() == config.biomeObservation().filterBits()
                ? new FixedBloomFilter(cache.observationFilterBits(), cache.observationFilter())
                : new FixedBloomFilter(config.biomeObservation().filterBits());
        this.observations = new BiomeObservationStats(
                filter, cache.biomeObservationCounts(),
                cache.biomeObservationCounts().values().stream().mapToLong(Long::longValue).sum(), currentBiomes
        );
        this.lastSuccessfulReload = cache.lastSuccessfulReloadEpochMillis();
        this.lastRebuild = cache.lastRebuildEpochMillis();
    }

    public ValuationService service() {
        return service;
    }

    public boolean isFor(MinecraftServer candidate) {
        return server == candidate;
    }

    public GameplayConfig config() {
        return config;
    }

    public ValuationRuleResourceParser.ValidationContext validationContext() {
        Set<Identifier> entityTags = scan.entities().values().stream()
                .flatMap(entry -> entry.tags().stream()).collect(Collectors.toUnmodifiableSet());
        Set<Identifier> biomeTags = scan.biomes().values().stream()
                .flatMap(entry -> entry.tags().stream()).collect(Collectors.toUnmodifiableSet());
        return new ValuationRuleResourceParser.ValidationContext(
                scan.entities().keySet(), scan.biomes().keySet(), entityTags, biomeTags
        );
    }

    public void applyRules(LoadedValuationRules newRules) {
        Objects.requireNonNull(newRules, "newRules");
        publishCatalog(scan, newRules, "reload");
    }

    public void rebuild(String reason) {
        RegistryScanService.ScanResult candidate = scanner.scan(server, config);
        observations.retainBiomes(candidate.biomes().keySet());
        publishCatalog(candidate, rules, reason);
    }

    public void tick() {
        if (!config.biomeObservation().enabled() || ++tickCounter % config.biomeObservation().intervalTicks() != 0) {
            return;
        }
        boolean changed = false;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            Identifier dimension = player.getServerWorld().getRegistryKey().getValue();
            int chunkX = player.getChunkPos().x;
            int chunkZ = player.getChunkPos().z;
            Identifier biome = player.getServerWorld().getBiome(player.getBlockPos()).getKey()
                    .map(key -> key.getValue()).orElse(null);
            if (biome != null) {
                changed |= observations.observe(dimension, chunkX, chunkZ, biome);
            }
        }
        if (changed) {
            publishCatalog(scan, rules, "observations");
        }
    }

    public void resetObservations() {
        observations.reset();
        publishCatalog(scan, rules, "observations_reset");
    }

    public int observationFilterBits() {
        return observations.filterBits();
    }

    private void publishCatalog(
            RegistryScanService.ScanResult candidateScan,
            LoadedValuationRules candidateRules,
            String reason
    ) {
        long now = System.currentTimeMillis();
        RegistryFingerprint fingerprint = fingerprint(candidateScan, candidateRules.digest());
        String priorFingerprint = savedData.payload().registryFingerprint();
        boolean fingerprintChanged = !priorFingerprint.equals(fingerprint.sha256());
        RegistryScanService.ScanResult effectiveScan = candidateScan;
        String cacheStatus;
        if (!fingerprintChanged && !priorFingerprint.isBlank() && !reason.equals("admin_command")) {
            effectiveScan = reuseCachedAutomaticValues(candidateScan, savedData.payload());
            cacheStatus = "fingerprint_match";
        } else if (fingerprintChanged && !priorFingerprint.isBlank()
                && !config.registryScan().rebuildOnFingerprintChange()) {
            effectiveScan = reuseCachedAutomaticValues(candidateScan, savedData.payload());
            cacheStatus = "fingerprint_changed_rebuild_disabled";
        } else {
            cacheStatus = "rebuilt:" + reason;
        }
        ValuationCatalogBuilder.Result built = ValuationCatalogBuilder.build(
                effectiveScan.entities(), effectiveScan.biomes(), candidateRules, observations, config, cacheStatus, now
        );
        RegistryScanSnapshot snapshot = new RegistryScanSnapshot(
                effectiveScan.entities(), built.resolvedBiomes(), fingerprint, candidateRules.digest(), now, effectiveScan.durationMillis()
        );
        long candidateReloadTime = Math.max(lastSuccessfulReload, candidateRules.loadedAtEpochMillis());
        long candidateRebuildTime = cacheStatus.startsWith("rebuilt:") ? now : lastRebuild;
        persist(snapshot, candidateRules, candidateReloadTime, candidateRebuildTime);
        service.replace(snapshot, built.catalog(), candidateRules);
        this.scan = effectiveScan;
        this.rules = candidateRules;
        this.lastSuccessfulReload = candidateReloadTime;
        this.lastRebuild = candidateRebuildTime;
        Camerapture.LOGGER.info(
                "valuation catalog {}: {} entities, {} biomes, {} rules, {} ms, fingerprint {}",
                reason, snapshot.entities().size(), snapshot.biomes().size(), candidateRules.rules().size(),
                snapshot.scanDurationMillis(), snapshot.fingerprint().sha256()
        );
    }

    private static RegistryScanService.ScanResult reuseCachedAutomaticValues(
            RegistryScanService.ScanResult current,
            ValuationCachePayload cache
    ) {
        Map<Identifier, EntityRegistryEntry> entities = current.entities().entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> {
                    Long cached = cache.entityAutomaticValues().get(entry.getKey());
                    EntityRegistryEntry value = entry.getValue();
                    if (cached == null) return value;
                    return new EntityRegistryEntry(
                            value.id(), value.tags(), value.spawnGroup(), value.hasDefaultAttributes(),
                            value.maxHealth(), value.armor(), value.attackDamage(), value.specialScore(),
                            java.util.OptionalLong.of(cached), value.automaticValueStatus(),
                            value.automaticValueReason() + "; compatible cached automatic value reused"
                    );
                }));
        Map<Identifier, BiomeRegistryEntry> biomes = current.biomes().entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> {
                    long cached = cache.biomeAutomaticValues().getOrDefault(
                            entry.getKey(), entry.getValue().automaticBaseValue());
                    BiomeRegistryEntry value = entry.getValue();
                    return new BiomeRegistryEntry(value.id(), value.tags(), cached, 0L, 0.0, 1.0, cached);
                }));
        return new RegistryScanService.ScanResult(entities, biomes, current.durationMillis());
    }

    private RegistryFingerprint fingerprint(RegistryScanService.ScanResult candidateScan, String ruleDigest) {
        String configJson = GameplayConfig.CODEC.encodeStart(JsonOps.INSTANCE, config)
                .getOrThrow().toString();
        return RegistryFingerprint.compute(new RegistryFingerprint.Input(
                net.minecraft.SharedConstants.getGameVersion().getName(),
                ValuationCachePayload.CURRENT_ALGORITHM_VERSION,
                mods,
                server.getDataPackManager().getEnabledIds(),
                RegistryFingerprint.digestStrings(List.of(configJson)),
                ruleDigest,
                candidateScan.entities().keySet().stream().map(Identifier::toString).toList(),
                candidateScan.biomes().keySet().stream().map(Identifier::toString).toList()
        ));
    }

    private void persist(
            RegistryScanSnapshot snapshot,
            LoadedValuationRules candidateRules,
            long candidateReloadTime,
            long candidateRebuildTime
    ) {
        Map<Identifier, Long> entityAutomatic = snapshot.entities().values().stream()
                .filter(entry -> entry.automaticValue().isPresent())
                .collect(Collectors.toUnmodifiableMap(EntityRegistryEntry::id, entry -> entry.automaticValue().getAsLong()));
        Map<Identifier, Long> biomeAutomatic = snapshot.biomes().values().stream()
                .collect(Collectors.toUnmodifiableMap(BiomeRegistryEntry::id, BiomeRegistryEntry::automaticBaseValue));
        savedData.replace(new ValuationCachePayload(
                ValuationCachePayload.CURRENT_SCHEMA_VERSION,
                ValuationCachePayload.CURRENT_ALGORITHM_VERSION,
                snapshot.fingerprint().sha256(),
                candidateRules.digest(),
                entityAutomatic,
                biomeAutomatic,
                observations.counts(),
                observations.filterBits(),
                observations.filterBytes(),
                candidateRebuildTime,
                candidateReloadTime
        ));
    }
}
