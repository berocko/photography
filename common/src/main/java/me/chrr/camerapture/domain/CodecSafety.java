package me.chrr.camerapture.domain;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

/** Converts record-constructor validation exceptions into ordinary Codec errors. */
public final class CodecSafety {
    private CodecSafety() {
    }

    public static <A> Codec<A> guard(Codec<A> delegate, String description) {
        return new Codec<>() {
            @Override
            public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
                try {
                    return delegate.decode(ops, input);
                } catch (IllegalArgumentException exception) {
                    return DataResult.error(() -> description + ": " + exception.getMessage());
                }
            }

            @Override
            public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
                try {
                    return delegate.encode(input, ops, prefix);
                } catch (IllegalArgumentException exception) {
                    return DataResult.error(() -> description + ": " + exception.getMessage());
                }
            }

            @Override
            public String toString() {
                return "Guarded[" + delegate + "]";
            }
        };
    }
}
