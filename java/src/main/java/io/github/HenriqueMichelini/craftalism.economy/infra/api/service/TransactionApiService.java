package io.github.HenriqueMichelini.craftalism.economy.infra.api.service;

import com.google.gson.Gson;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.client.HttpClientService;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.dto.TransactionRequestDTO;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.dto.TransactionResponseDTO;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.exceptions.*;
import io.github.HenriqueMichelini.craftalism.economy.infra.config.GsonFactory;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class TransactionApiService {

    private final HttpClientService http;
    private final Gson gson;

    public TransactionApiService(HttpClientService http) {
        this(http, GsonFactory.getInstance());
    }

    public TransactionApiService(HttpClientService http, Gson gson) {
        this.http = http;
        this.gson = gson;
    }

    private <T> T parseJson(String body, Class<T> type) {
        try {
            T parsed = gson.fromJson(body, type);
            if (parsed == null) {
                throw new ApiException("Parsed JSON was null for " + type.getSimpleName() +
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
            return new NotFoundException("Transaction/Player not found (404). Body: " + preview);
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

        return new ApiException("Unexpected status " + status + ". Body: " + preview);
    }

    private <T> CompletableFuture<T> unwrapOrThrow(int status, String body) {
        if (status >= 200 && status < 300) {
            return CompletableFuture.completedFuture(parseJson(body, (Class<T>) TransactionResponseDTO.class));
        }
        return CompletableFuture.failedFuture(mapStatusToException(status, body));
    }

    public CompletableFuture<TransactionResponseDTO> register(UUID from, UUID to, long amount) {
        TransactionRequestDTO dto = new TransactionRequestDTO(from, to, amount);
        String json = gson.toJson(dto);

        return http.post("/api/transactions", json)
                .thenCompose(resp -> unwrapOrThrow(
                        resp.statusCode(),
                        resp.body()
                ));
    }
}
