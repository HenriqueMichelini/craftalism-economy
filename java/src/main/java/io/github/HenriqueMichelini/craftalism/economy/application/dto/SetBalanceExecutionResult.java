package io.github.HenriqueMichelini.craftalism.economy.application.dto;

import io.github.HenriqueMichelini.craftalism.economy.domain.service.enums.SetBalanceStatus;

import java.util.Optional;
import java.util.UUID;

public record SetBalanceExecutionResult(
        SetBalanceStatus status,
        Optional<Long> amount,
        Optional<UUID> uuid
) {

    public static SetBalanceExecutionResult success(Long amount, UUID uuid) {
        return new SetBalanceExecutionResult(
                SetBalanceStatus.SUCCESS,
                Optional.of(amount),
                Optional.of(uuid)
        );
    }

    public static SetBalanceExecutionResult invalidAmount() {
        return error(SetBalanceStatus.INVALID_AMOUNT);
    }

    public static SetBalanceExecutionResult updateFailed() {
        return error(SetBalanceStatus.UPDATE_FAILED);
    }

    public static SetBalanceExecutionResult playerNotFound() {
        return error(SetBalanceStatus.PLAYER_NOT_FOUND);
    }

    public static SetBalanceExecutionResult exception() {
        return error(SetBalanceStatus.EXCEPTION);
    }

    private static SetBalanceExecutionResult error(SetBalanceStatus status) {
        return new SetBalanceExecutionResult(status, Optional.empty(), Optional.empty());
    }

    public SetBalanceStatus getStatus() {
        return status;
    }

    public boolean isSuccess() {
        return status == SetBalanceStatus.SUCCESS;
    }
}
