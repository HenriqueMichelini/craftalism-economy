package io.github.HenriqueMichelini.craftalism.economy.application.service;

import io.github.HenriqueMichelini.craftalism.economy.application.dto.BalanceExecutionResult;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.exceptions.NotFoundException;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class BalanceCommandApplicationService {

    private final PlayerApplicationService playerService;
    private final BalanceApplicationService balanceService;

    public BalanceCommandApplicationService(
            PlayerApplicationService playerService,
            BalanceApplicationService balanceService) {
        this.playerService = playerService;
        this.balanceService = balanceService;
    }

    public CompletableFuture<BalanceExecutionResult> executeSelf(UUID playerUuid) {
        return balanceService.getOrCreateBalance(playerUuid)
                .thenApply(balance -> BalanceExecutionResult.successSelf(balance.getAmount()))
                .exceptionally(ex -> BalanceExecutionResult.error());
    }

    public CompletableFuture<BalanceExecutionResult> executeOther(String playerName) {
        return playerService.getUuidByName(playerName)
                .thenCompose(this::fetchBalanceForOther)
                .exceptionally(this::handleOtherError);
    }

    private CompletableFuture<BalanceExecutionResult> fetchBalanceForOther(UUID uuid) {
        return balanceService.getBalance(uuid)
                .thenApply(balanceOpt -> balanceOpt
                        .map(balance -> BalanceExecutionResult.successOther(balance.getAmount()))
                        .orElse(BalanceExecutionResult.noBalance())
                );
    }

    private BalanceExecutionResult handleOtherError(Throwable ex) {
        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;

        if (cause instanceof NotFoundException) {
            return BalanceExecutionResult.notFound();
        }

        return BalanceExecutionResult.error();
    }
}