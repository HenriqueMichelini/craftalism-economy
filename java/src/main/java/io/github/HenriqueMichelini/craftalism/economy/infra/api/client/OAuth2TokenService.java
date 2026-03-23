package io.github.HenriqueMichelini.craftalism.economy.infra.api.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class OAuth2TokenService {

    private final HttpClient http;
    private final String tokenUrl;
    private final String clientId;
    private final String clientSecret;

    private final AtomicReference<String> cachedToken = new AtomicReference<>();
    private volatile Instant tokenExpiry = Instant.MIN;

    public OAuth2TokenService(
        HttpClient http,
        String authServerUrl,
        String clientId,
        String clientSecret
    ) {
        this.http = http;
        this.tokenUrl = authServerUrl + "/oauth2/token";
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public CompletableFuture<String> getToken() {
        if (
            cachedToken.get() != null &&
            Instant.now().isBefore(tokenExpiry.minusSeconds(30))
        ) {
            return CompletableFuture.completedFuture(cachedToken.get());
        }
        return fetchNewToken();
    }

    private CompletableFuture<String> fetchNewToken() {
        String credentials = Base64.getEncoder().encodeToString(
            (clientId + ":" + clientSecret).getBytes()
        );

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(tokenUrl))
            .header("Authorization", "Basic " + credentials)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(
                HttpRequest.BodyPublishers.ofString(
                    "grant_type=client_credentials&scope=api:read api:write"
                )
            )
            .build();

        return http
            .sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                if (response.statusCode() != 200) {
                    throw new RuntimeException(
                        "Failed to fetch token: " + response.statusCode()
                    );
                }

                JsonObject json = JsonParser.parseString(
                    response.body()
                ).getAsJsonObject();
                String token = json.get("access_token").getAsString();
                int expiresIn = json.get("expires_in").getAsInt();

                cachedToken.set(token);
                tokenExpiry = Instant.now().plusSeconds(expiresIn);

                return token;
            });
    }
}
