package me.chrr.camerapture.domain.currency;

import java.util.OptionalLong;

/** Checked balance arithmetic shared by atomic provider implementations. */
public final class CurrencyMath {
    private CurrencyMath() {
    }

    public static OptionalLong deposit(long balance, long amount) {
        validate(balance, amount);
        if (amount > Long.MAX_VALUE - balance) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(balance + amount);
    }

    public static OptionalLong withdraw(long balance, long amount) {
        validate(balance, amount);
        return amount > balance ? OptionalLong.empty() : OptionalLong.of(balance - amount);
    }

    private static void validate(long balance, long amount) {
        if (balance < 0 || amount < 0) {
            throw new IllegalArgumentException("currency balance and amount must be non-negative");
        }
    }
}
