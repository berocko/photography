package me.chrr.camerapture.domain.observation;

import net.minecraft.util.Identifier;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/** Fixed-memory Bloom filter; false positives only omit observations. */
public final class FixedBloomFilter {
    private static final int HASH_COUNT = 3;

    private final int bitCount;
    private final byte[] bits;

    public FixedBloomFilter(int bitCount) {
        this(bitCount, new byte[checkedByteCount(bitCount)]);
    }

    public FixedBloomFilter(int bitCount, byte[] bits) {
        int byteCount = checkedByteCount(bitCount);
        if (bits.length != byteCount) {
            throw new IllegalArgumentException("Bloom filter byte length does not match bit count");
        }
        this.bitCount = bitCount;
        this.bits = Arrays.copyOf(bits, bits.length);
    }

    /** Returns true only when at least one bit was newly set. */
    public boolean add(Identifier dimension, int chunkX, int chunkZ) {
        long[] hashes = hashes(dimension, chunkX, chunkZ);
        boolean changed = false;
        for (int index = 0; index < HASH_COUNT; index++) {
            int bit = index(hashes[0] + index * hashes[1]);
            int byteIndex = bit >>> 3;
            int mask = 1 << (bit & 7);
            if ((bits[byteIndex] & mask) == 0) {
                bits[byteIndex] = (byte) (bits[byteIndex] | mask);
                changed = true;
            }
        }
        return changed;
    }

    public boolean mightContain(Identifier dimension, int chunkX, int chunkZ) {
        long[] hashes = hashes(dimension, chunkX, chunkZ);
        for (int index = 0; index < HASH_COUNT; index++) {
            int bit = index(hashes[0] + index * hashes[1]);
            if ((bits[bit >>> 3] & (1 << (bit & 7))) == 0) {
                return false;
            }
        }
        return true;
    }

    public int bitCount() {
        return bitCount;
    }

    public int byteCount() {
        return bits.length;
    }

    public byte[] copyBytes() {
        return Arrays.copyOf(bits, bits.length);
    }

    public void clear() {
        Arrays.fill(bits, (byte) 0);
    }

    private int index(long hash) {
        return (int) Long.remainderUnsigned(mix64(hash), bitCount);
    }

    private static long[] hashes(Identifier dimension, int chunkX, int chunkZ) {
        Objects.requireNonNull(dimension, "dimension");
        byte[] id = dimension.toString().getBytes(StandardCharsets.UTF_8);
        long first = 0xcbf29ce484222325L;
        for (byte value : id) {
            first ^= value & 0xffL;
            first *= 0x100000001b3L;
        }
        first ^= Integer.toUnsignedLong(chunkX) * 0x9e3779b97f4a7c15L;
        first ^= Integer.toUnsignedLong(chunkZ) * 0xc2b2ae3d27d4eb4fL;
        long second = mix64(first ^ 0x94d049bb133111ebL) | 1L;
        return new long[]{mix64(first), second};
    }

    private static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }

    private static int checkedByteCount(int bitCount) {
        if (bitCount < 8 || (bitCount & 7) != 0) {
            throw new IllegalArgumentException("Bloom filter bit count must be positive and byte aligned");
        }
        return bitCount / 8;
    }
}
