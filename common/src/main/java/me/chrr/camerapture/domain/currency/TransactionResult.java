package me.chrr.camerapture.domain.currency;

import java.util.Objects;
import java.util.Optional;

public record TransactionResult(boolean success, long balanceAfter, Optional<String> errorCode) {
    public TransactionResult {
        errorCode = Objects.requireNonNull(errorCode, "errorCode");
        if (balanceAfter < 0 || (success && errorCode.isPresent()) || (!success && errorCode.isEmpty())) {
            throw new IllegalArgumentException("inconsistent transaction result");
        }
    }

    public static TransactionResult success(long balanceAfter) {
        return new TransactionResult(true, balanceAfter, Optional.empty());
    }

    public static TransactionResult failure(long balanceAfter, String errorCode) {
        return new TransactionResult(false, balanceAfter, Optional.of(errorCode));
    }
}
