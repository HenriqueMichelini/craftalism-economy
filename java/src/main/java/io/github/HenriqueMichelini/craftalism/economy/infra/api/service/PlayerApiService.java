package io.github.HenriqueMichelini.craftalism.economy.infra.api.service;

import com.google.gson.Gson;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.client.HttpClientService;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.dto.PlayerRequestDTO;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.dto.PlayerResponseDTO;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.exceptions.*;
import io.github.HenriqueMichelini.craftalism.economy.infra.config.GsonFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PlayerApiService {

    private final HttpClientService http;
    private final Gson gson;

    public PlayerApiService(HttpClientService http) {
        this(http, GsonFactory.getInstance());
    }

    public PlayerApiService(HttpClientService http, Gson gson) {
        this.http = http;
        this.gson = gson;
    }

    private <T> T parseJson(String body, Class<T> type) {
        try {
            T parsed = gson.fromJson(body, type);
            if (parsed == null) {
                throw new ApiException("Parsed JSON was null for type " + type.getSimpleName() +
                        ". Body: " + body);
            }
            return parsed;
        } catch (Exception e) {
            throw new ApiException("Failed to parse JSON for " + type.getSimpleName() +
                    ": " + e.getMessage(), e);
        }
    }

    private String safePreview(String body) {
        if (body == null) return "<null>";
        return body.length() > 300 ? body.substring(0, 300) + "..." : body;
    }

    private ApiException mapStatusToException(int status, String body) {
        String preview = safePreview(body);

        if (status == 400) {
            return new BadRequestException("Bad request (400). Body: " + preview);
        }

        if (status == 404) {
            return new NotFoundException("Player not found (404). Body: " + preview);
        }

        if (status == 429) {
            return new RateLimitException("Rate limit exceeded (429). Body: " + preview);
        }

        if (status == 408) {
            return new ApiTimeoutException("Timeout (408). Body: " + preview);
        }

        if (status >= 500) {
            return new ApiServerException("Server error (" + status + "). Body: " + preview);
        }

        return new ApiException(
                "Unexpected status " + status + ". Body: " + preview
        );
    }

    private <T> CompletableFuture<T> unwrapOrThrow(int status, String body) {
        if (status >= 200 && status < 300) {
            return CompletableFuture.completedFuture(parseJson(body, (Class<T>) PlayerResponseDTO.class));
        }
        return CompletableFuture.failedFuture(mapStatusToException(status, body));
    }

    public CompletableFuture<PlayerResponseDTO> getPlayerByUuid(UUID uuid) {
        return http.get("/api/players/" + uuid)
                .thenCompose(resp -> unwrapOrThrow(
                        resp.statusCode(),
                        resp.body()
                ));
    }

    public CompletableFuture<PlayerResponseDTO> getPlayerByName(String name) {
        String encoded = URLEncoder.encode(name, StandardCharsets.UTF_8);
        return http.get("/api/players/name/" + encoded)
                .thenCompose(resp -> unwrapOrThrow(
                        resp.statusCode(),
                        resp.body()
                ));
    }

    public CompletableFuture<PlayerResponseDTO> createPlayer(UUID uuid, String name) {
        PlayerRequestDTO dto = new PlayerRequestDTO(uuid, name);
        String json = gson.toJson(dto);

        return http.post("/api/players", json)
                .thenCompose(resp -> unwrapOrThrow(
                        resp.statusCode(),
                        resp.body()
                ));
    }

    public CompletableFuture<PlayerResponseDTO> getOrCreatePlayer(UUID uuid, String name) {
        return getPlayerByUuid(uuid)
                .exceptionallyCompose(ex -> {
                    // Unwrap CompletionException to get the real cause
                    Throwable cause = ex;
                    while (cause.getCause() != null &&
                            (cause instanceof java.util.concurrent.CompletionException ||
                                    cause instanceof java.util.concurrent.ExecutionException)) {
                        cause = cause.getCause();
                    }

                    // Check if the unwrapped cause is NotFoundException
                    if (cause instanceof NotFoundException) {
                        return createPlayer(uuid, name);
                    }
                    return CompletableFuture.failedFuture(ex);
                });
    }
}
