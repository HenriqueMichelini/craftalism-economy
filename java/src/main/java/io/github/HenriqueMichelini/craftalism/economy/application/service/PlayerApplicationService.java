package io.github.HenriqueMichelini.craftalism.economy.application.service;

import io.github.HenriqueMichelini.craftalism.economy.domain.model.Player;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.dto.PlayerResponseDTO;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.exceptions.ApiServerException;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.exceptions.NotFoundException;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.repository.PlayerCacheRepository;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.service.PlayerApiService;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

public class PlayerApplicationService {
    private final PlayerApiService api;
    private final PlayerCacheRepository cache;

    public PlayerApplicationService(PlayerApiService api, PlayerCacheRepository cache) {
        this.api = api;
        this.cache = cache;
    }

    public CompletableFuture<Player> loadPlayerOnJoin(UUID uuid, String name) {
        return api.getOrCreatePlayer(uuid, name)
                .thenApply(dto -> {
                    Player player = new Player(
                            dto.uuid(),
                            dto.name(),
                            dto.createdAt()
                    );
                    cache.save(player);
                    return player;
                });
    }

    public CompletableFuture<Player> syncPlayer(UUID uuid) {
        return api.getPlayerByUuid(uuid)
                .thenApply(dto -> {
                    Player updated = new Player(
                            dto.uuid(),
                            dto.name(),
                            dto.createdAt()
                    );

                    cache.save(updated);
                    return updated;
                });
    }

    public CompletableFuture<Player> getPlayerByName(String name) {
        return api.getPlayerByName(name)
                .thenApply(dto -> {
                    Player player = new Player(dto.uuid(), dto.name(), dto.createdAt());
                    cache.save(player);
                    return player;
                });
    }

    public CompletableFuture<PlayerResponseDTO> getPlayerByUuid(UUID uuid) {
        return api.getPlayerByUuid(uuid);
    }

    public CompletableFuture<String> getNameByUuid(UUID uuid) {
        return api.getPlayerByUuid(uuid).thenApply(PlayerResponseDTO::name);
    }

    public CompletableFuture<UUID> getUuidByName(String name) {
        return api.getPlayerByName(name)
                .thenApply(PlayerResponseDTO::uuid);
    }

    public CompletableFuture<PlayerResponseDTO> getOrCreatePlayer(UUID uuid, String name) {
        return api.getPlayerByUuid(uuid)
                .handle((player, ex) -> {
                    if (ex == null) {
                        return CompletableFuture.completedFuture(player);
                    }

                    Throwable cause = unwrap(ex);
                    if (cause instanceof NotFoundException || cause instanceof ApiServerException) {
                        return api.createPlayer(uuid, name);
                    }

                    return CompletableFuture.<PlayerResponseDTO>failedFuture(cause);
                })
                .thenCompose(future -> future);
    }

    public CompletableFuture<Player> getCachedOrFetch(UUID uuid, String name) {
        Optional<Player> cached = cache.find(uuid);
        if (cached.isPresent()) {
            return CompletableFuture.completedFuture(cached.get());
        }

        return api.getOrCreatePlayer(uuid, name)
                .thenApply(dto -> {
                    Player player = new Player(dto.uuid(), dto.name(), dto.createdAt());
                    cache.save(player);
                    return player;
                });
    }

    private Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while (current instanceof CompletionException || current instanceof ExecutionException) {
            if (current.getCause() == null) {
                break;
            }
            current = current.getCause();
        }
        return current;
    }
}
