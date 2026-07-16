package me.chrr.camerapture.domain.currency;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrencyMathTest {
    @Test
    void depositRejectsOverflowWithoutChangingTheBalance() {
        assertTrue(CurrencyMath.deposit(Long.MAX_VALUE - 2, 3).isEmpty());
        assertEquals(12, CurrencyMath.deposit(5, 7).orElseThrow());
    }

    @Test
    void withdrawRejectsInsufficientFundsAndNegativeAmounts() {
        assertTrue(CurrencyMath.withdraw(5, 6).isEmpty());
        assertEquals(2, CurrencyMath.withdraw(5, 3).orElseThrow());
        assertThrows(IllegalArgumentException.class, () -> CurrencyMath.withdraw(5, -1));
    }
}
