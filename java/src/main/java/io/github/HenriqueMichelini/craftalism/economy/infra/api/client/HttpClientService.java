package io.github.HenriqueMichelini.craftalism.economy.infra.api.client;

import io.github.HenriqueMichelini.craftalism.economy.infra.api.exceptions.ApiTimeoutException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

public class HttpClientService {

    private static final Logger LOGGER = Logger.getLogger(
        HttpClientService.class.getName()
    );
    private final HttpClient http;
    private final String baseUrl;
    private final OAuth2TokenService tokenService;
    private final int requestTimeoutSeconds;

    public HttpClientService(
        String baseUrl,
        OAuth2TokenService tokenService,
        int connectTimeoutSeconds,
        int requestTimeoutSeconds
    ) {
        this.http = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
            .build();
        this.baseUrl = baseUrl;
        this.tokenService = tokenService;
        this.requestTimeoutSeconds = requestTimeoutSeconds;
    }

    private CompletableFuture<HttpRequest.Builder> authenticatedRequest(
        String path
    ) {
        return tokenService
            .getToken()
            .thenApply(token ->
                HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(requestTimeoutSeconds))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
            );
    }

    public CompletableFuture<HttpResponse<String>> get(String path) {
        return authenticatedRequest(path).thenCompose(builder ->
            send(builder.GET().build(), path)
        );
    }

    public CompletableFuture<HttpResponse<String>> post(
        String path,
        String body
    ) {
        return authenticatedRequest(path).thenCompose(builder ->
            send(
                builder.POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                path
            )
        );
    }

    public CompletableFuture<HttpResponse<String>> put(
        String path,
        String body
    ) {
        return authenticatedRequest(path).thenCompose(builder ->
            send(
                builder.PUT(HttpRequest.BodyPublishers.ofString(body)).build(),
                path
            )
        );
    }

    private CompletableFuture<HttpResponse<String>> send(
        HttpRequest request,
        String path
    ) {
        LOGGER.fine(() -> "[HttpClient] -> " + request.method() + " " + request.uri());
        return withTimeoutHandling(
            path,
            http
                .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .whenComplete((resp, err) -> {
                    if (resp != null) {
                        LOGGER.fine(() ->
                            "[HttpClient] <- " +
                                resp.statusCode() +
                                " (" +
                                safeBodyLength(resp.body()) +
                                " chars)"
                        );
                    } else {
                        LOGGER.warning("[HttpClient] <- ERROR : " + err);
                    }
                })
        );
    }

    private int safeBodyLength(String body) {
        if (body == null) return 0;
        return body.length();
    }

    private <T> CompletableFuture<T> withTimeoutHandling(
        String path,
        CompletableFuture<T> future
    ) {
        return future
            .orTimeout(requestTimeoutSeconds, TimeUnit.SECONDS)
            .exceptionally(ex -> {
                Throwable cause =
                    ex instanceof CompletionException ? ex.getCause() : ex;

                if (cause instanceof TimeoutException) {
                    throw new ApiTimeoutException(
                        "Request timed out: " + path,
                        cause
                    );
                }

                throw new CompletionException(cause);
            });
    }
}
