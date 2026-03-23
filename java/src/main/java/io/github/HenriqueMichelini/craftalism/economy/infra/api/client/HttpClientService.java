package io.github.HenriqueMichelini.craftalism.economy.infra.api.client;

import io.github.HenriqueMichelini.craftalism.economy.infra.api.exceptions.ApiTimeoutException;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class HttpClientService {

    private final HttpClient http;
    private final String baseUrl;

    public HttpClientService(String baseUrl) {
        this.http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.baseUrl = baseUrl;
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json");
    }

    public CompletableFuture<HttpResponse<String>> get(String path) {
        return send(request(path).GET().build(), path);
    }

    public CompletableFuture<HttpResponse<String>> post(String path, String body) {
        return send(request(path).POST(HttpRequest.BodyPublishers.ofString(body)).build(), path);
    }

    public CompletableFuture<HttpResponse<String>> put(String path, String body) {
        return send(request(path).PUT(HttpRequest.BodyPublishers.ofString(body)).build(), path);
    }

    private CompletableFuture<HttpResponse<String>> send(HttpRequest request, String path) {
        System.out.println("[HttpClient] -> " + request.uri());
        return withTimeoutHandling(path,
                http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .whenComplete((resp, err) -> {
                            if (resp != null) {
                                System.out.println("[HttpClient] <- " + resp.statusCode()
                                        + " : " + safePreview(resp.body()));
                            } else {
                                System.out.println("[HttpClient] <- ERROR : " + err);
                            }
                        })
        );
    }

    private String safePreview(String body) {
        if (body == null) return "<null>";
        return body.length() > 300 ? body.substring(0, 300) + "..." : body;
    }

    private <T> CompletableFuture<T> withTimeoutHandling(String path, CompletableFuture<T> future) {
        return future
                .orTimeout(10, TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    Throwable cause = ex instanceof CompletionException ? ex.getCause() : ex;

                    if (cause instanceof TimeoutException) {
                        throw new ApiTimeoutException("Request timed out: " + path, cause);
                    }

                    throw new CompletionException(cause);
                });
    }
}
