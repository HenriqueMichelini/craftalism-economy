package io.github.HenriqueMichelini.craftalism.economy.application.dto;

import io.github.HenriqueMichelini.craftalism.economy.domain.service.enums.PayStatus;

import java.util.Optional;
import java.util.UUID;

public record PayExecutionResult(
        PayStatus status,
        Optional<UUID> receiverUuid
) {

    public static PayExecutionResult success(UUID receiverUuid) {
        return new PayExecutionResult(
                PayStatus.SUCCESS,
                Optional.of(receiverUuid)
        );
    }

    public static PayExecutionResult targetNotFound() {
        return error(PayStatus.TARGET_NOT_FOUND);
    }

    public static PayExecutionResult notEnoughFunds() {
        return error(PayStatus.NOT_ENOUGH_FUNDS);
    }

    public static PayExecutionResult invalidAmount() {
        return error(PayStatus.INVALID_AMOUNT);
    }

    public static PayExecutionResult cannotPaySelf() {
        return error(PayStatus.CANNOT_PAY_SELF);
    }

    public static PayExecutionResult exception() {
        return error(PayStatus.ERROR);
    }

    private static PayExecutionResult error(PayStatus status) {
        return new PayExecutionResult(status, Optional.empty());
    }

    public PayStatus getStatus() {
        return status;
    }

    public boolean isSuccess() {
        return status == PayStatus.SUCCESS;
    }
}