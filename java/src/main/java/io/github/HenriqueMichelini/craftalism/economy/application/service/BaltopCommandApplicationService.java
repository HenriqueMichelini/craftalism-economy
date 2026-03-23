package io.github.HenriqueMichelini.craftalism.economy.application.service;

import io.github.HenriqueMichelini.craftalism.economy.infra.api.dto.BalanceResponseDTO;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.service.BalanceApiService;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.service.PlayerApiService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class BaltopCommandApplicationService {
    private final BalanceApiService balanceApi;
    private final PlayerApiService playerApi;

    public BaltopCommandApplicationService(BalanceApiService balanceApi, PlayerApiService playerApi) {
        this.balanceApi = balanceApi;
        this.playerApi = playerApi;
    }

    public CompletableFuture<List<BaltopEntry>> getTop10() {
        return getTopPlayers(10);
    }

    public CompletableFuture<List<BaltopEntry>> getTopPlayers(int limit) {
        return balanceApi.getTopBalances(limit)
                .thenCompose(this::enrichWithPlayerData);
    }

    private CompletableFuture<List<BaltopEntry>> enrichWithPlayerData(List<BalanceResponseDTO> balances) {
        List<CompletableFuture<BaltopEntry>> futures = new ArrayList<>();

        for (BalanceResponseDTO balance : balances) {
            CompletableFuture<BaltopEntry> entryFuture = playerApi.getPlayerByUuid(balance.uuid())
                    .thenApply(player -> new BaltopEntry(
                            player.name(),
                            balance.amount(),
                            balance.uuid()
                    ))
                    .exceptionally(ex -> new BaltopEntry(
                            "Unknown",
                            balance.amount(),
                            balance.uuid()
                    ));

            futures.add(entryFuture);
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .toList()
                );
    }

    public static class BaltopEntry {
        private final String playerName;
        private final long balance;
        private final java.util.UUID uuid;

        public BaltopEntry(String playerName, long balance, java.util.UUID uuid) {
            this.playerName = playerName;
            this.balance = balance;
            this.uuid = uuid;
        }

        public String getPlayerName() {
            return playerName;
        }

        public long getBalance() {
            return balance;
        }

        public java.util.UUID getUuid() {
            return uuid;
        }
    }
}