package me.chrr.camerapture.domain.observation;

import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** World-level bounded biome counts plus approximate chunk deduplication. */
public final class BiomeObservationStats {
    private final FixedBloomFilter filter;
    private final Map<Identifier, Long> counts;
    private Set<Identifier> allowedBiomes;
    private long total;

    public BiomeObservationStats(int filterBits, Set<Identifier> allowedBiomes) {
        this(new FixedBloomFilter(filterBits), Map.of(), 0L, allowedBiomes);
    }

    public BiomeObservationStats(
            FixedBloomFilter filter,
            Map<Identifier, Long> counts,
            long total,
            Set<Identifier> allowedBiomes
    ) {
        this.filter = Objects.requireNonNull(filter, "filter");
        this.allowedBiomes = Set.copyOf(allowedBiomes);
        this.counts = new HashMap<>();
        counts.forEach((id, count) -> {
            if (id != null && count != null && count >= 0 && this.allowedBiomes.contains(id)) {
                this.counts.put(id, count);
            }
        });
        long sum = this.counts.values().stream().mapToLong(Long::longValue).sum();
        this.total = total == sum ? total : sum;
    }

    /** Records at most one sample for a dimension/chunk key. */
    public synchronized boolean observe(Identifier dimension, int chunkX, int chunkZ, Identifier biome) {
        Objects.requireNonNull(biome, "biome");
        if (!allowedBiomes.contains(biome) || !filter.add(dimension, chunkX, chunkZ)) {
            return false;
        }
        counts.merge(biome, 1L, BiomeObservationStats::saturatedAdd);
        total = saturatedAdd(total, 1L);
        return true;
    }

    public synchronized void retainBiomes(Set<Identifier> currentBiomes) {
        this.allowedBiomes = Set.copyOf(currentBiomes);
        counts.keySet().retainAll(this.allowedBiomes);
        total = counts.values().stream().mapToLong(Long::longValue).sum();
    }

    public synchronized long count(Identifier biome) {
        return counts.getOrDefault(biome, 0L);
    }

    public synchronized long total() {
        return total;
    }

    public synchronized int observedBiomeKinds() {
        return (int) counts.values().stream().filter(value -> value > 0).count();
    }

    public synchronized Map<Identifier, Long> counts() {
        return Map.copyOf(counts);
    }

    public synchronized byte[] filterBytes() {
        return filter.copyBytes();
    }

    public int filterBits() {
        return filter.bitCount();
    }

    public synchronized void reset() {
        counts.clear();
        total = 0L;
        filter.clear();
    }

    private static long saturatedAdd(long left, long right) {
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }
}
