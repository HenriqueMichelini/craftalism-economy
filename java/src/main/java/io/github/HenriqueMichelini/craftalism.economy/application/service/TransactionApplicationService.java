package io.github.HenriqueMichelini.craftalism.economy.application.service;

import io.github.HenriqueMichelini.craftalism.economy.domain.model.Transaction;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.dto.TransactionResponseDTO;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.exceptions.RateLimitException;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.service.TransactionApiService;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.exceptions.BadRequestException;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.exceptions.NotFoundException;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class TransactionApplicationService {

    private final TransactionApiService api;

    public TransactionApplicationService(TransactionApiService api) {
        this.api = api;
    }

    public CompletableFuture<Transaction> registerTransaction(UUID from, UUID to, long amount) {
        return api.register(from, to, amount)
                .thenApply(this::toTransaction);
    }

    public CompletableFuture<Optional<Transaction>> tryRegisterTransaction(UUID from, UUID to, long amount) {
        return api.register(from, to, amount)
                .thenApply(dto -> Optional.of(toTransaction(dto)))
                .exceptionally(ex -> {
                    if (isNotFoundException(ex) || isBadRequestException(ex)) {
                        return Optional.empty();
                    }
                    return throwAsCompletion(ex);
                });
    }

    public CompletableFuture<Transaction> registerTransactionWithRetry(UUID from, UUID to, long amount) {
        return api.register(from, to, amount)
                .exceptionallyCompose(ex -> {
                    Throwable cause = unwrapException(ex);

                    if (cause instanceof RateLimitException) {
                        // Could implement retry logic here if needed
                        return CompletableFuture.failedFuture(ex);
                    }

                    return CompletableFuture.failedFuture(ex);
                })
                .thenApply(this::toTransaction);
    }

    private Transaction toTransaction(TransactionResponseDTO dto) {
        return new Transaction(dto.id(), dto.fromPlayerUuid(), dto.toPlayerUuid(), dto.amount(), dto.createdAt());
    }

    private Throwable unwrapException(Throwable ex) {
        Throwable cause = ex;
        while (cause.getCause() != null &&
                (cause instanceof CompletionException || cause instanceof java.util.concurrent.ExecutionException)) {
            cause = cause.getCause();
        }
        return cause;
    }

    private boolean isNotFoundException(Throwable ex) {
        return unwrapException(ex) instanceof NotFoundException;
    }

    private boolean isBadRequestException(Throwable ex) {
        return unwrapException(ex) instanceof BadRequestException;
    }

    private <T> T throwAsCompletion(Throwable ex) {
        throw new CompletionException(ex);
    }
}