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
import java.util.concurrent.ExecutionException;

public class BalanceApplicationService {

    private final BalanceApiService api;
    private final BalanceCacheRepository cache;
    private final long DEFAULT_VALUE;

    public BalanceApplicationService(
        BalanceApiService api,
        BalanceCacheRepository cache,
        long DEFAULT_VALUE
    ) {
        this.api = api;
        this.cache = cache;
        this.DEFAULT_VALUE = DEFAULT_VALUE;
    }

    public CompletableFuture<Optional<Balance>> getBalance(UUID uuid) {
        return api
            .getBalance(uuid)
            .handle((dto, ex) -> {
                if (ex == null) {
                    return Optional.of(toBalance(dto));
                }

                if (isNotFoundException(ex)) {
                    return Optional.empty();
                }

                throwAsCompletion(ex);
                return Optional.empty();
            });
    }

    public CompletableFuture<Balance> getOrCreateBalance(UUID uuid) {
        return api
            .getBalance(uuid)
            .exceptionallyCompose(ex -> {
                Throwable cause = ex;
                while (
                    cause.getCause() != null &&
                    (cause instanceof CompletionException ||
                        cause instanceof
                            java.util.concurrent.ExecutionException)
                ) {
                    cause = cause.getCause();
                }

                if (cause instanceof NotFoundException) {
                    return api.createBalance(uuid, DEFAULT_VALUE);
                }

                return CompletableFuture.failedFuture(cause);
            })
            .thenApply(this::toBalance);
    }

    public CompletableFuture<Balance> loadBalanceOnJoin(UUID uuid) {
        return getOrCreateBalance(uuid).thenApply(balance -> {
            cache.save(balance);
            return balance;
        });
    }

    public CompletableFuture<Balance> syncBalance(UUID uuid) {
        return api
            .getBalance(uuid)
            .thenApply(dto -> {
                Balance balance = toBalance(dto);
                cache.save(balance);
                return balance;
            });
    }

    public CompletableFuture<Balance> getCachedOrFetch(UUID uuid) {
        return cache
            .find(uuid)
            .map(CompletableFuture::completedFuture)
            .orElseGet(() -> loadBalanceOnJoin(uuid));
    }

    public CompletableFuture<Balance> updateBalance(UUID uuid, Long amount) {
        return api
            .updateBalance(uuid, amount)
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
        return unwrap(ex) instanceof NotFoundException;
    }

    private Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while (
            current instanceof CompletionException ||
            current instanceof ExecutionException
        ) {
            if (current.getCause() == null) {
                break;
            }
            current = current.getCause();
        }
        return current;
    }

    private void throwAsCompletion(Throwable ex) {
        throw new CompletionException(unwrap(ex));
    }
}
