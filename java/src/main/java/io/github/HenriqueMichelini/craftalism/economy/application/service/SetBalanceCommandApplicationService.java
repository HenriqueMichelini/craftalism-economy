package io.github.HenriqueMichelini.craftalism.economy.application.service;

import io.github.HenriqueMichelini.craftalism.economy.application.dto.SetBalanceExecutionResult;
import io.github.HenriqueMichelini.craftalism.economy.domain.model.Player;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.exceptions.NotFoundException;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.service.BalanceApiService;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SetBalanceCommandApplicationService {
    private final BalanceApiService balanceApi;
    private final PlayerApplicationService playerApplicationService;

    public SetBalanceCommandApplicationService(
            BalanceApiService balanceApi,
            PlayerApplicationService playerApplicationService
    ) {
        this.balanceApi = balanceApi;
        this.playerApplicationService = playerApplicationService;
    }

    public CompletableFuture<SetBalanceExecutionResult> execute(
            String targetName,
            long amount
    ) {
        if (amount < 0) {
            return CompletableFuture.completedFuture(SetBalanceExecutionResult.invalidAmount());
        }

        return getPlayerUuid(targetName)
                .thenCompose(uuid -> setBalanceForPlayer(uuid, amount))
                .exceptionally(this::handleException);
    }

    private CompletableFuture<UUID> getPlayerUuid(String targetName) {
        return playerApplicationService.getPlayerByName(targetName)
                .thenApply(Player::getUuid);
    }

    private CompletableFuture<SetBalanceExecutionResult> setBalanceForPlayer(UUID uuid, long amount) {
        return balanceApi.updateBalance(uuid, amount)
                .thenApply(v -> SetBalanceExecutionResult.success(amount, uuid))
                .exceptionally(ex -> SetBalanceExecutionResult.updateFailed());
    }

    private SetBalanceExecutionResult handleException(Throwable ex) {
        Throwable cause = ex;
        while (cause.getCause() != null &&
                (cause instanceof java.util.concurrent.CompletionException ||
                        cause instanceof java.util.concurrent.ExecutionException)) {
            cause = cause.getCause();
        }

        if (cause instanceof NotFoundException ||
                cause.getClass().getName().contains("NotFoundException")) {
            return SetBalanceExecutionResult.playerNotFound();
        }

        return SetBalanceExecutionResult.exception();
    }
}