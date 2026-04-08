package io.github.HenriqueMichelini.craftalism.economy.application.service;

import io.github.HenriqueMichelini.craftalism.economy.application.dto.PayExecutionResult;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.exceptions.*;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.service.BalanceApiService;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class PaymentTransferService {

    private final BalanceApiService balanceApi;
    private final boolean legacyFallbackEnabled;
    private final Logger logger;

    public PaymentTransferService(
        BalanceApiService balanceApi,
        boolean legacyFallbackEnabled,
        Logger logger
    ) {
        this.balanceApi = balanceApi;
        this.legacyFallbackEnabled = legacyFallbackEnabled;
        this.logger = logger;
    }

    public CompletableFuture<PayExecutionResult> execute(
        UUID payerUuid,
        UUID receiverUuid,
        long amount
    ) {
        logInfo(
            "Starting transfer: sender=" +
            payerUuid +
            ", receiver=" +
            receiverUuid +
            ", amount=" +
            amount
        );
        return balanceApi
            .transfer(payerUuid, receiverUuid, amount)
            .thenApply(v -> PayExecutionResult.success(receiverUuid))
            .exceptionally(ex -> mapTransferException(ex, payerUuid, receiverUuid, amount));
    }

    private PayExecutionResult mapTransferException(
        Throwable ex,
        UUID payerUuid,
        UUID receiverUuid,
        long amount
    ) {
        Throwable cause = AsyncExceptionResolver.unwrap(ex);

        if (cause instanceof TransferEndpointUnavailableException) {
            if (!legacyFallbackEnabled) {
                logInfo("Transfer endpoint unavailable and legacy fallback disabled");
                return PayExecutionResult.transferEndpointUnavailable();
            }

            logInfo("Transfer endpoint unavailable. Falling back to legacy withdraw/deposit flow");
            try {
                return runLegacyTransfer(payerUuid, receiverUuid, amount).join();
            } catch (Exception fallbackEx) {
                Throwable fallbackCause = AsyncExceptionResolver.unwrap(fallbackEx);
                return mapNonEndpointFailure(fallbackCause);
            }
        }

        return mapNonEndpointFailure(cause);
    }

    private PayExecutionResult mapNonEndpointFailure(Throwable cause) {
        if (cause instanceof TransferApiException transferApiException) {
            return mapTransferReason(transferApiException.getReason());
        }

        if (cause instanceof NotFoundException) {
            return PayExecutionResult.targetNotFound();
        }

        if (
            cause instanceof RateLimitException ||
            cause instanceof ApiTimeoutException ||
            cause instanceof ApiServerException
        ) {
            return PayExecutionResult.transferTemporarilyUnavailable();
        }

        logError("Unexpected transfer failure", cause);
        return PayExecutionResult.exception();
    }

    private PayExecutionResult mapTransferReason(TransferFailureReason reason) {
        return switch (reason) {
            case INSUFFICIENT_FUNDS -> PayExecutionResult.notEnoughFunds();
            case SENDER_NOT_FOUND, RECEIVER_NOT_FOUND ->
                PayExecutionResult.targetNotFound();
            case INVALID_REQUEST -> PayExecutionResult.transferInvalidRequest();
            case DUPLICATE_REQUEST -> PayExecutionResult.transferDuplicate();
            default -> PayExecutionResult.exception();
        };
    }

    private CompletableFuture<PayExecutionResult> runLegacyTransfer(
        UUID payerUuid,
        UUID receiverUuid,
        long amount
    ) {
        return balanceApi
            .withdraw(payerUuid, amount)
            .thenCompose(v ->
                balanceApi
                    .deposit(receiverUuid, amount)
                    .thenApply(x -> PayExecutionResult.success(receiverUuid))
            )
            .exceptionallyCompose(ex -> compensateLegacyTransfer(payerUuid, amount, ex));
    }

    private CompletableFuture<PayExecutionResult> compensateLegacyTransfer(
        UUID payerUuid,
        long amount,
        Throwable ex
    ) {
        Throwable cause = AsyncExceptionResolver.unwrap(ex);

        if (cause instanceof NotFoundException) {
            return CompletableFuture.completedFuture(PayExecutionResult.targetNotFound());
        }

        if (cause instanceof TransferApiException transferApiException) {
            return CompletableFuture.completedFuture(mapTransferReason(transferApiException.getReason()));
        }

        if (
            cause instanceof RateLimitException ||
            cause instanceof ApiTimeoutException ||
            cause instanceof ApiServerException
        ) {
            return CompletableFuture.completedFuture(PayExecutionResult.transferTemporarilyUnavailable());
        }

        return balanceApi
            .deposit(payerUuid, amount)
            .handle((ignored, rollbackError) -> {
                if (rollbackError != null) {
                    logError("Legacy rollback failed", AsyncExceptionResolver.unwrap(rollbackError));
                }
                logError("Legacy transfer failed", cause);
                return PayExecutionResult.exception();
            });
    }

    private void logInfo(String message) {
        if (logger != null) {
            logger.info(message);
        }
    }

    private void logError(String message, Throwable ex) {
        if (logger != null) {
            logger.severe(
                message +
                ": " +
                ex.getClass().getSimpleName() +
                ": " +
                ex.getMessage()
            );
        }
    }
}
