package io.github.HenriqueMichelini.craftalism.economy.infra.api.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.client.HttpClientService;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.dto.BalanceCreateRequestDTO;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.dto.BalanceResponseDTO;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.dto.BalanceSetRequestDTO;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.dto.BalanceUpdateRequestDTO;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.dto.TransferRequestDTO;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.exceptions.*;
import io.github.HenriqueMichelini.craftalism.economy.infra.config.GsonFactory;
import java.lang.reflect.Type;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class BalanceApiService {

    private final HttpClientService http;
    private final Gson gson;

    public BalanceApiService(HttpClientService http) {
        this(http, GsonFactory.getInstance());
    }

    public BalanceApiService(HttpClientService http, Gson gson) {
        this.http = http;
        this.gson = gson;
    }

    public CompletableFuture<BalanceResponseDTO> getBalance(UUID uuid) {
        return http
            .get("/api/balances/" + uuid)
            .thenCompose(resp -> {
                int status = resp.statusCode();
                String body = resp.body();

                if (status == 200) {
                    try {
                        return CompletableFuture.completedFuture(
                            parseJson(body)
                        );
                    } catch (ApiException e) {
                        return CompletableFuture.failedFuture(e);
                    }
                }

                return CompletableFuture.failedFuture(
                    mapTransferStatusToException(status, body)
                );
            });
    }

    public CompletableFuture<BalanceResponseDTO> createBalance(
        UUID uuid,
        long initialAmount
    ) {
        BalanceCreateRequestDTO dto = new BalanceCreateRequestDTO(
            uuid,
            initialAmount
        );
        return http
            .post("/api/balances", gson.toJson(dto))
            .thenCompose(resp -> {
                int status = resp.statusCode();
                String body = resp.body();

                if (status == 201 || status == 200) {
                    try {
                        return CompletableFuture.completedFuture(
                            parseJson(body)
                        );
                    } catch (ApiException e) {
                        return CompletableFuture.failedFuture(e);
                    }
                }

                return CompletableFuture.failedFuture(
                    mapStatusToException(status, body)
                );
            });
    }

    public CompletableFuture<BalanceResponseDTO> getOrCreateBalance(
        UUID uuid,
        long initialAmount
    ) {
        return getBalance(uuid).exceptionallyCompose(ex -> {
            if (ex instanceof NotFoundException) {
                return createBalance(uuid, initialAmount);
            }
            return CompletableFuture.failedFuture(ex);
        });
    }

    public CompletableFuture<BalanceResponseDTO> updateBalance(
        UUID uuid,
        long amount
    ) {
        BalanceSetRequestDTO dto = new BalanceSetRequestDTO(amount);
        return http
            .put("/api/balances/" + uuid + "/set", gson.toJson(dto))
            .thenCompose(resp -> {
                int status = resp.statusCode();
                String respBody = resp.body();

                if (status == 200) {
                    try {
                        return CompletableFuture.completedFuture(
                            parseJson(respBody)
                        );
                    } catch (ApiException e) {
                        return CompletableFuture.failedFuture(e);
                    }
                }

                return CompletableFuture.failedFuture(
                    mapStatusToException(status, respBody)
                );
            });
    }

    public CompletableFuture<Void> deposit(UUID uuid, long amount) {
        BalanceUpdateRequestDTO dto = new BalanceUpdateRequestDTO(amount);
        return http
            .post(
                "/api/balances/" + uuid + "/deposit?amount=" + amount,
                gson.toJson(dto)
            )
            .thenCompose(resp -> {
                int status = resp.statusCode();
                String body = resp.body();

                if (status == 200 || status == 204) {
                    return CompletableFuture.completedFuture(null);
                }

                return CompletableFuture.failedFuture(
                    mapStatusToException(status, body)
                );
            });
    }

    public CompletableFuture<Void> withdraw(UUID uuid, long amount) {
        BalanceUpdateRequestDTO dto = new BalanceUpdateRequestDTO(amount);
        return http
            .post(
                "/api/balances/" + uuid + "/withdraw?amount=" + amount,
                gson.toJson(dto)
            )
            .thenCompose(resp -> {
                int status = resp.statusCode();
                String body = resp.body();

                if (status == 200 || status == 204) {
                    return CompletableFuture.completedFuture(null);
                }

                return CompletableFuture.failedFuture(
                    mapStatusToException(status, body)
                );
            });
    }

    public CompletableFuture<Void> transfer(UUID from, UUID to, long amount) {
        TransferRequestDTO dto = new TransferRequestDTO(from, to, amount);
        return http
            .post("/api/balances/transfer", gson.toJson(dto))
            .thenCompose(resp -> {
                int status = resp.statusCode();
                String body = resp.body();

                if (status == 200 || status == 201 || status == 204) {
                    return CompletableFuture.completedFuture(null);
                }

                return CompletableFuture.failedFuture(
                    mapTransferStatusToException(status, body)
                );
            });
    }

    public CompletableFuture<List<BalanceResponseDTO>> getTopBalances(
        int limit
    ) {
        return http
            .get("/api/balances/top?limit=" + limit)
            .thenCompose(resp -> {
                int status = resp.statusCode();
                String body = resp.body();

                if (status == 200) {
                    try {
                        Type listType = new TypeToken<
                            List<BalanceResponseDTO>
                        >() {}.getType();
                        return CompletableFuture.completedFuture(
                            parseJson(body, listType)
                        );
                    } catch (ApiException e) {
                        return CompletableFuture.failedFuture(e);
                    }
                }

                return CompletableFuture.failedFuture(
                    mapStatusToException(status, body)
                );
            });
    }

    private <T> T parseJson(String body) {
        try {
            T parsed = gson.fromJson(body, (Class<T>) BalanceResponseDTO.class);
            if (parsed == null) {
                throw new ApiException(
                    "Parsed JSON was null for type: " +
                        BalanceResponseDTO.class.getSimpleName() +
                        ", body: " +
                        safePreview(body)
                );
            }
            return parsed;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(
                "Failed to parse JSON for " +
                    BalanceResponseDTO.class.getSimpleName() +
                    ": " +
                    e.getMessage(),
                e
            );
        }
    }

    private <T> T parseJson(String body, Type typeOfT) {
        try {
            T parsed = gson.fromJson(body, typeOfT);
            if (parsed == null) {
                throw new ApiException(
                    "Parsed JSON was null for type: " +
                        typeOfT.getTypeName() +
                        ", body: " +
                        safePreview(body)
                );
            }
            return parsed;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(
                "Failed to parse JSON for " +
                    typeOfT.getTypeName() +
                    ": " +
                    e.getMessage(),
                e
            );
        }
    }

    private ApiException mapTransferStatusToException(int status, String body) {
        String normalizedBody = body == null ? "" : body.toLowerCase();

        if (status == 400) {
            return new TransferApiException(
                inferTransferFailureReason(normalizedBody),
                "Transfer validation failed (status=400). Body: " + safePreview(body)
            );
        }

        if (status == 404 || status == 405 || status == 501) {
            return new TransferEndpointUnavailableException(
                "Transfer endpoint unavailable (status=" +
                status +
                "). Body: " +
                safePreview(body)
            );
        }

        return mapStatusToException(status, body);
    }

    private TransferFailureReason inferTransferFailureReason(String body) {
        if (body.contains("insufficient") || body.contains("not_enough")) {
            return TransferFailureReason.INSUFFICIENT_FUNDS;
        }

        if (body.contains("sender") && body.contains("not") && body.contains("found")) {
            return TransferFailureReason.SENDER_NOT_FOUND;
        }

        if (body.contains("receiver") && body.contains("not") && body.contains("found")) {
            return TransferFailureReason.RECEIVER_NOT_FOUND;
        }

        if (body.contains("duplicate") || body.contains("idempot")) {
            return TransferFailureReason.DUPLICATE_REQUEST;
        }

        if (body.contains("invalid") || body.contains("validation") || body.contains("bad request")) {
            return TransferFailureReason.INVALID_REQUEST;
        }

        return TransferFailureReason.GENERIC_FAILURE;
    }

    private ApiException mapStatusToException(int status, String body) {
        if (status == 404) {
            return new NotFoundException(
                "Resource not found (status=404). Body: " + safePreview(body)
            );
        }
        if (status == 429) {
            return new RateLimitException(
                "Rate limit exceeded (status=429). Body: " + safePreview(body)
            );
        }
        if (status >= 500) {
            return new ApiServerException(
                "Server error (status=" +
                    status +
                    "). Body: " +
                    safePreview(body)
            );
        }
        return new ApiException(
            "Unexpected status: " + status + ". Body: " + safePreview(body)
        );
    }

    private String safePreview(String body) {
        if (body == null) return "";
        final int max = 500;
        if (body.length() <= max) return body;
        return body.substring(0, max) + "...(truncated)";
    }
}
