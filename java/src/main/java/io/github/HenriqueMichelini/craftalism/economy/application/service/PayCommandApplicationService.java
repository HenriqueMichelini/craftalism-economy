package io.github.HenriqueMichelini.craftalism.economy.application.service;

import io.github.HenriqueMichelini.craftalism.economy.application.dto.PayExecutionResult;
import io.github.HenriqueMichelini.craftalism.economy.domain.model.Player;
import io.github.HenriqueMichelini.craftalism.economy.domain.service.enums.PayStatus;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.dto.PlayerResponseDTO;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.exceptions.NotFoundException;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.service.BalanceApiService;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.service.PlayerApiService;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class PayCommandApplicationService {

    private final PlayerApplicationService playerService;
    private final PlayerApiService playerApi;
    private final BalanceApiService balanceApi;
    private final Logger logger;

    public PayCommandApplicationService(
        PlayerApplicationService playerService,
        PlayerApiService playerApi,
        BalanceApiService balanceApi,
        Logger logger
    ) {
        this.playerService = playerService;
        this.playerApi = playerApi;
        this.balanceApi = balanceApi;
        this.logger = logger;
    }

    public CompletableFuture<PayExecutionResult> execute(
        UUID payerUuid,
        String payerName,
        String receiverName,
        long amount
    ) {
        return playerService
            .getCachedOrFetch(payerUuid, payerName)
            .thenCompose(payer -> processPayment(payer, receiverName, amount))
            .exceptionally(this::handleTopLevelException);
    }

    private CompletableFuture<PayExecutionResult> processPayment(
        Player payer,
        String receiverName,
        long amount
    ) {
        return playerApi
            .getPlayerByName(receiverName)
            .thenCompose(receiver -> validateAndExecutePayment(payer, receiver, amount))
            .exceptionally(this::handleReceiverLookupException);
    }

    private CompletableFuture<PayExecutionResult> validateAndExecutePayment(
        Player payer,
        PlayerResponseDTO receiver,
        long amount
    ) {
        PayStatus validationResult = validatePayment(payer, receiver, amount);
        if (validationResult != PayStatus.SUCCESS) {
            return CompletableFuture.completedFuture(
                mapStatusToResult(validationResult)
            );
        }

        return executeTransfer(payer.getUuid(), receiver.uuid(), amount);
    }

    private PayStatus validatePayment(
        Player payer,
        PlayerResponseDTO receiver,
        long amount
    ) {
        if (payer.getUuid().equals(receiver.uuid())) {
            return PayStatus.CANNOT_PAY_SELF;
        }

        if (amount <= 0) {
            return PayStatus.INVALID_AMOUNT;
        }

        return PayStatus.SUCCESS;
    }

    private CompletableFuture<PayExecutionResult> executeTransfer(
        UUID payerUuid,
        UUID receiverUuid,
        long amount
    ) {
        return balanceApi
            .getBalance(payerUuid)
            .thenCompose(balance ->
                checkBalanceAndTransfer(
                    payerUuid,
                    receiverUuid,
                    amount,
                    balance.amount()
                )
            )
            .exceptionally(ex -> handleTransferException(ex, "balance check"));
    }

    private CompletableFuture<PayExecutionResult> checkBalanceAndTransfer(
        UUID payerUuid,
        UUID receiverUuid,
        long amount,
        long currentBalance
    ) {
        if (currentBalance < amount) {
            return CompletableFuture.completedFuture(
                PayExecutionResult.notEnoughFunds()
            );
        }

        return balanceApi
            .transfer(payerUuid, receiverUuid, amount)
            .thenApply(v -> PayExecutionResult.success(receiverUuid))
            .exceptionally(ex -> handleTransferException(ex, "transfer"));
    }

    private PayExecutionResult mapStatusToResult(PayStatus status) {
        return switch (status) {
            case CANNOT_PAY_SELF -> PayExecutionResult.cannotPaySelf();
            case INVALID_AMOUNT -> PayExecutionResult.invalidAmount();
            case NOT_ENOUGH_FUNDS -> PayExecutionResult.notEnoughFunds();
            case TARGET_NOT_FOUND -> PayExecutionResult.targetNotFound();
            default -> PayExecutionResult.exception();
        };
    }

    private PayExecutionResult handleTransferException(Throwable ex, String phase) {
        Throwable cause = AsyncExceptionResolver.unwrap(ex);

        if (cause instanceof NotFoundException) {
            logError("Player not found during " + phase, cause);
            return PayExecutionResult.targetNotFound();
        }

        logError("Unexpected error during " + phase, cause);
        return PayExecutionResult.exception();
    }

    private PayExecutionResult handleReceiverLookupException(Throwable ex) {
        Throwable cause = AsyncExceptionResolver.unwrap(ex);

        if (cause instanceof NotFoundException) {
            logInfo("Receiver not found: " + cause.getMessage());
            return PayExecutionResult.targetNotFound();
        }

        logError("Error looking up receiver", cause);
        return PayExecutionResult.exception();
    }

    private PayExecutionResult handleTopLevelException(Throwable ex) {
        Throwable cause = AsyncExceptionResolver.unwrap(ex);

        if (cause instanceof NotFoundException) {
            logInfo("Payer not found: " + cause.getMessage());
            return PayExecutionResult.targetNotFound();
        }

        logError("Top-level error during payment", cause);
        return PayExecutionResult.exception();
    }

    private void logInfo(String message) {
        if (logger != null) {
            logger.info(message);
        }
    }

    private void logError(String message, Throwable ex) {
        if (logger != null) {
            logger.severe(message + ": " + ex.getMessage());
            if (ex.getCause() != null) {
                logger.severe("Caused by: " + ex.getCause().getMessage());
            }
        }
    }
}
