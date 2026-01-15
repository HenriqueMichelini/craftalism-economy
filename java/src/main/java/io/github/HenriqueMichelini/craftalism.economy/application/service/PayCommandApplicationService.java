package io.github.HenriqueMichelini.craftalism.economy.application.service;

import io.github.HenriqueMichelini.craftalism.economy.application.dto.PayExecutionResult;
import io.github.HenriqueMichelini.craftalism.economy.domain.model.Player;
import io.github.HenriqueMichelini.craftalism.economy.domain.service.enums.PayStatus;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.dto.PlayerResponseDTO;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.exceptions.NotFoundException;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.service.BalanceApiService;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.service.PlayerApiService;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.service.TransactionApiService;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PayCommandApplicationService {
    private final PlayerApplicationService playerService;
    private final PlayerApiService playerApi;
    private final BalanceApiService balanceApi;
    private final TransactionApiService transactionApi;
    private final JavaPlugin plugin;

    public PayCommandApplicationService(
            PlayerApplicationService playerService,
            PlayerApiService playerApi,
            BalanceApiService balanceApi,
            TransactionApiService transactionApi,
            JavaPlugin plugin
    ) {
        this.playerService = playerService;
        this.playerApi = playerApi;
        this.balanceApi = balanceApi;
        this.transactionApi = transactionApi;
        this.plugin = plugin;
    }

    public CompletableFuture<PayExecutionResult> execute(
            UUID payerUuid,
            String payerName,
            String receiverName,
            long amount
    ) {
        return playerService.getCachedOrFetch(payerUuid, payerName)
                .thenCompose(payer -> processPayment(payer, receiverName, amount))
                .exceptionally(this::handleTopLevelException);
    }

    private CompletableFuture<PayExecutionResult> processPayment(
            Player payer,
            String receiverName,
            long amount
    ) {
        return playerApi.getPlayerByName(receiverName)
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

    private PayStatus validatePayment(Player payer, PlayerResponseDTO receiver, long amount) {
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
        return balanceApi.getBalance(payerUuid)
                .thenCompose(balance -> checkBalanceAndTransfer(payerUuid, receiverUuid, amount, balance.amount()))
                .exceptionally(ex -> handleTransferException(ex, "balance check"));
    }

    private CompletableFuture<PayExecutionResult> checkBalanceAndTransfer(
            UUID payerUuid,
            UUID receiverUuid,
            long amount,
            long currentBalance
    ) {
        if (currentBalance < amount) {
            return CompletableFuture.completedFuture(PayExecutionResult.notEnoughFunds());
        }

        return performTransfer(payerUuid, receiverUuid, amount);
    }

    private CompletableFuture<PayExecutionResult> performTransfer(UUID payerUuid, UUID receiverUuid, long amount) {
        return withdrawFromPayer(payerUuid, amount)
                .thenCompose(v -> depositToReceiver(payerUuid, receiverUuid, amount))
                .thenCompose(v -> logTransaction(payerUuid, receiverUuid, amount))
                .thenApply(v -> PayExecutionResult.success(receiverUuid))
                .exceptionally(ex -> handleTransferException(ex, "transfer"));
    }

    private CompletableFuture<Void> withdrawFromPayer(UUID payerUuid, long amount) {
        return balanceApi.withdraw(payerUuid, amount);
    }

    private CompletableFuture<Void> depositToReceiver(UUID payerUuid, UUID receiverUuid, long amount) {
        return balanceApi.deposit(receiverUuid, amount)
                .exceptionallyCompose(depositEx ->
                        handleDepositFailure(payerUuid, receiverUuid, amount, depositEx)
                );
    }

    private CompletableFuture<Void> handleDepositFailure(
            UUID payerUuid,
            UUID receiverUuid,
            long amount,
            Throwable depositEx
    ) {
        logError("Deposit failed for " + receiverUuid + ", rolling back withdrawal", depositEx);

        return rollbackWithdrawal(payerUuid, amount)
                .thenCompose(v -> {
                    // Rollback succeeded, now fail with the original deposit error
                    return CompletableFuture.<Void>failedFuture(
                            new TransferException("Deposit failed and rollback succeeded", depositEx)
                    );
                })
                .exceptionallyCompose(rollbackEx ->
                        handleRollbackFailure(payerUuid, amount, depositEx, rollbackEx)
                );
    }

    private CompletableFuture<Void> rollbackWithdrawal(UUID payerUuid, long amount) {
        return balanceApi.deposit(payerUuid, amount)
                .thenApply(v -> {
                    logInfo("Successfully rolled back withdrawal for " + payerUuid);
                    return v;
                });
    }

    private CompletableFuture<Void> handleRollbackFailure(
            UUID payerUuid,
            long amount,
            Throwable depositEx,
            Throwable rollbackEx
    ) {
        Throwable unwrapped = unwrapException(rollbackEx);

        if (unwrapped instanceof TransferException) {
            return CompletableFuture.failedFuture(unwrapped);
        }

        logCritical(
                "CRITICAL: Rollback failed for " + payerUuid +
                        ". User has lost " + amount + " coins!",
                rollbackEx
        );

        return CompletableFuture.failedFuture(
                new CriticalTransferException(
                        "Both deposit and rollback failed",
                        depositEx,
                        rollbackEx
                )
        );
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

    private CompletableFuture<Void> logTransaction(UUID payerUuid, UUID receiverUuid, long amount) {
        return transactionApi.register(payerUuid, receiverUuid, amount)
                .thenApply(transaction -> (Void) null)  // Convert to Void
                .exceptionally(ex -> {
                    logWarning("Transaction logging failed (payment completed): " + ex.getMessage());
                    return null; // Payment succeeded, logging is best-effort
                });
    }

    private PayExecutionResult handleTransferException(Throwable ex, String phase) {
        Throwable cause = unwrapException(ex);

        if (cause instanceof NotFoundException) {
            logError("Player not found during " + phase, cause);
            return PayExecutionResult.targetNotFound();
        }

        if (cause instanceof TransferException) {
            logError("Transfer failed during " + phase + " (rollback succeeded)", cause);
            return PayExecutionResult.exception();
        }

        if (cause instanceof CriticalTransferException critical) {
            logCritical("CRITICAL TRANSFER FAILURE during " + phase, critical);
            // TODO: Alert admins, create manual intervention ticket
            return PayExecutionResult.exception();
        }

        logError("Unexpected error during " + phase, cause);
        return PayExecutionResult.exception();
    }

    private PayExecutionResult handleReceiverLookupException(Throwable ex) {
        Throwable cause = unwrapException(ex);

        if (cause instanceof NotFoundException) {
            logInfo("Receiver not found: " + cause.getMessage());
            return PayExecutionResult.targetNotFound();
        }

        logError("Error looking up receiver", cause);
        return PayExecutionResult.exception();
    }

    private PayExecutionResult handleTopLevelException(Throwable ex) {
        Throwable cause = unwrapException(ex);

        if (cause instanceof NotFoundException) {
            logInfo("Payer not found: " + cause.getMessage());
            return PayExecutionResult.targetNotFound();
        }

        logError("Top-level error during payment", cause);
        return PayExecutionResult.exception();
    }

    /**
     * Unwraps CompletionException and ExecutionException to get the root cause.
     */
    private Throwable unwrapException(Throwable ex) {
        Throwable cause = ex;
        while ((cause instanceof java.util.concurrent.CompletionException ||
                cause instanceof java.util.concurrent.ExecutionException) &&
                cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

    private void logInfo(String message) {
        if (plugin != null) {
            plugin.getLogger().info(message);
        }
    }

    private void logWarning(String message) {
        if (plugin != null) {
            plugin.getLogger().warning(message);
        }
    }

    private void logError(String message, Throwable ex) {
        if (plugin != null) {
            plugin.getLogger().severe(message + ": " + ex.getMessage());
            if (ex.getCause() != null) {
                plugin.getLogger().severe("Caused by: " + ex.getCause().getMessage());
            }
        }
    }

    private void logCritical(String message, Throwable ex) {
        if (plugin != null) {
            plugin.getLogger().severe("***** CRITICAL ERROR *****");
            plugin.getLogger().severe(message);
            plugin.getLogger().severe("Exception: " + ex.getMessage());
            if (ex.getCause() != null) {
                plugin.getLogger().severe("Caused by: " + ex.getCause().getMessage());
            }
            plugin.getLogger().severe("***** END CRITICAL ERROR *****");
        }
    }

    private static class TransferException extends RuntimeException {
        public TransferException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static class CriticalTransferException extends RuntimeException {
        private final Throwable rollbackException;

        public CriticalTransferException(String message, Throwable transferCause, Throwable rollbackCause) {
            super(message, transferCause);
            this.rollbackException = rollbackCause;
        }

        public Throwable getRollbackException() {
            return rollbackException;
        }
    }
}