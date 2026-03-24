package io.github.HenriqueMichelini.craftalism.economy.infra.api.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class OAuth2TokenService {

    private final HttpClient http;
    private final String tokenUrl;
    private final String clientId;
    private final String clientSecret;
    private final String scopes;

    private final AtomicReference<String> cachedToken = new AtomicReference<>();
    private volatile Instant tokenExpiry = Instant.MIN;
    private volatile CompletableFuture<String> inFlightTokenRequest;

    public OAuth2TokenService(
        HttpClient http,
        String tokenUrl,
        String clientId,
        String clientSecret,
        String scopes
    ) {
        this.http = http;
        this.tokenUrl = tokenUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.scopes = scopes;
    }

    public CompletableFuture<String> getToken() {
        if (
            cachedToken.get() != null &&
            Instant.now().isBefore(tokenExpiry.minusSeconds(30))
        ) {
            return CompletableFuture.completedFuture(cachedToken.get());
        }
        synchronized (this) {
            if (
                cachedToken.get() != null &&
                Instant.now().isBefore(tokenExpiry.minusSeconds(30))
            ) {
                return CompletableFuture.completedFuture(cachedToken.get());
            }
            if (
                inFlightTokenRequest != null && !inFlightTokenRequest.isDone()
            ) {
                return inFlightTokenRequest;
            }
            inFlightTokenRequest = fetchNewToken().whenComplete(
                (token, error) -> {
                    synchronized (this) {
                        inFlightTokenRequest = null;
                    }
                }
            );
            return inFlightTokenRequest;
        }
    }

    private CompletableFuture<String> fetchNewToken() {
        return requestToken(false)
            .thenCompose(response -> {
                if (!shouldRetryWithClientSecretPost(response.statusCode())) {
                    return CompletableFuture.completedFuture(response);
                }
                return requestToken(true);
            })
            .thenApply(this::parseTokenResponse);
    }

    private CompletableFuture<HttpResponse<String>> requestToken(
        boolean includeClientCredentialsInBody
    ) {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(tokenUrl))
            .header("Content-Type", "application/x-www-form-urlencoded");

        if (!includeClientCredentialsInBody) {
            String credentials = Base64.getEncoder().encodeToString(
                (clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8)
            );
            requestBuilder.header("Authorization", "Basic " + credentials);
        }

        return http.sendAsync(
            requestBuilder
                .POST(
                    HttpRequest.BodyPublishers.ofString(
                        buildRequestBody(includeClientCredentialsInBody)
                    )
                )
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
    }

    private boolean shouldRetryWithClientSecretPost(int statusCode) {
        return statusCode == 400 || statusCode == 401 || statusCode == 403;
    }

    private String parseTokenResponse(HttpResponse<String> response) {
        if (response.statusCode() != 200) {
            throw new RuntimeException(
                "Failed to fetch token: " +
                    response.statusCode() +
                    " - " +
                    response.body()
            );
        }
        JsonObject json = JsonParser.parseString(
            response.body()
        ).getAsJsonObject();
        if (!json.has("access_token")) {
            throw new RuntimeException(
                "Token response did not contain access_token"
            );
        }
        String token = json.get("access_token").getAsString();
        long expiresIn = json.has("expires_in")
            ? json.get("expires_in").getAsLong()
            : 300L;
        cachedToken.set(token);
        tokenExpiry = Instant.now().plusSeconds(expiresIn);
        return token;
    }

    private String buildRequestBody(boolean includeClientCredentialsInBody) {
        StringBuilder body = new StringBuilder("grant_type=client_credentials");
        if (includeClientCredentialsInBody) {
            body
                .append("&client_id=")
                .append(URLEncoder.encode(clientId, StandardCharsets.UTF_8))
                .append("&client_secret=")
                .append(
                    URLEncoder.encode(clientSecret, StandardCharsets.UTF_8)
                );
        }
        if (scopes != null && !scopes.isBlank()) {
            body
                .append("&scope=")
                .append(URLEncoder.encode(scopes, StandardCharsets.UTF_8));
        }
        return body.toString();
    }
}
