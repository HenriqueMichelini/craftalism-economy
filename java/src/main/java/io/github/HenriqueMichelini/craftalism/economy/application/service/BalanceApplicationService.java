package io.github.HenriqueMichelini.craftalism.economy.application.service;

import io.github.HenriqueMichelini.craftalism.economy.domain.model.Balance;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.dto.BalanceResponseDTO;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.exceptions.NotFoundException;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.repository.BalanceCacheRepository;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.service.BalanceApiService;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class BalanceApplicationService {

    private final BalanceApiService api;
    private final BalanceCacheRepository cache;

    public BalanceApplicationService(BalanceApiService api, BalanceCacheRepository cache) {
        this.api = api;
        this.cache = cache;
    }

    public CompletableFuture<Optional<Balance>> getBalance(UUID uuid) {
        return api.getBalance(uuid)
                .thenApply(dto -> Optional.of(toBalance(dto)))
                .exceptionally(ex ->
                        isNotFoundException(ex)
                                ? Optional.empty()
                                : throwAsCompletion(ex)
                );
    }

    public CompletableFuture<Balance> getOrCreateBalance(UUID uuid) {
        return api.getBalance(uuid)
                .exceptionallyCompose(ex -> {

                    Throwable cause = ex;
                    while (cause.getCause() != null &&
                            (cause instanceof CompletionException || cause instanceof java.util.concurrent.ExecutionException)) {
                        cause = cause.getCause();
                    }

                    if (cause instanceof NotFoundException) {
                        return api.createBalance(uuid);
                    }

                    return CompletableFuture.failedFuture(ex);
                })
                .thenApply(this::toBalance);
    }

    public CompletableFuture<Balance> loadBalanceOnJoin(UUID uuid) {
        return getOrCreateBalance(uuid)
                .thenApply(balance -> {
                    cache.save(balance);
                    return balance;
                });
    }

    public CompletableFuture<Balance> syncBalance(UUID uuid) {
        return api.getBalance(uuid)
                .thenApply(dto -> {
                    Balance balance = toBalance(dto);
                    cache.save(balance);
                    return balance;
                });
    }

    public CompletableFuture<Balance> getCachedOrFetch(UUID uuid) {
        return cache.find(uuid)
                .map(CompletableFuture::completedFuture)
                .orElseGet(() -> loadBalanceOnJoin(uuid));
    }

    public CompletableFuture<Balance> updateBalance(UUID uuid, Long amount) {
        return api.updateBalance(uuid, amount)
                .thenApply(dto -> {
                    Balance balance = toBalance(dto);
                    cache.save(balance);
                    return balance;
                });
    }

    private Balance toBalance(BalanceResponseDTO dto) {
        return new Balance(dto.uuid(), dto.amount());
    }

    private boolean isNotFoundException(Throwable ex) {
        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
        return cause instanceof NotFoundException;
    }

    private <T> T throwAsCompletion(Throwable ex) {
        throw new CompletionException(ex);
    }
}
