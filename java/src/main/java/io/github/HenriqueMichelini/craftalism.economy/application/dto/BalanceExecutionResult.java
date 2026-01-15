package io.github.HenriqueMichelini.craftalism.economy.application.dto;

import io.github.HenriqueMichelini.craftalism.economy.domain.service.enums.BalanceStatus;

public record BalanceExecutionResult(
        BalanceStatus status,
        Long amount
) {
    public static BalanceExecutionResult successSelf(Long amount) {
        return new BalanceExecutionResult(BalanceStatus.SUCCESS_SELF, amount);
    }

    public static BalanceExecutionResult successOther(Long amount) {
        return new BalanceExecutionResult(BalanceStatus.SUCCESS_OTHER, amount);
    }

    public static BalanceExecutionResult noBalance() {
        return new BalanceExecutionResult(BalanceStatus.NO_BALANCE, 0L);
    }

    public static BalanceExecutionResult notFound() {
        return new BalanceExecutionResult(BalanceStatus.NOT_FOUND, 0L);
    }

    public static BalanceExecutionResult error() {
        return new BalanceExecutionResult(BalanceStatus.ERROR, 0L);
    }
}

