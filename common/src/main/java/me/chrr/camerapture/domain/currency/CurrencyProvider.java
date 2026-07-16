package me.chrr.camerapture.domain.currency;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Loader-neutral, server-only currency boundary. Implementations must make each
 * deposit or withdrawal atomic and reject negative amounts and overflow.
 */
public interface CurrencyProvider {
    Identifier id();

    long getBalance(ServerPlayerEntity player);

    TransactionResult deposit(ServerPlayerEntity player, long amount, TransactionContext context);

    TransactionResult withdraw(ServerPlayerEntity player, long amount, TransactionContext context);

    Text format(long amount);

    boolean isAvailable();
}
