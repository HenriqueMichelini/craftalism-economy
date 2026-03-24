package io.github.HenriqueMichelini.craftalism.economy.infra.api.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OAuth2TokenServiceTest {

    @Test
    void getToken_retriesWithClientSecretPostWhenBasicAuthFails() {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> unauthorizedResponse = mock(HttpResponse.class);
        HttpResponse<String> tokenResponse = mock(HttpResponse.class);

        when(unauthorizedResponse.statusCode()).thenReturn(401);
        when(unauthorizedResponse.body()).thenReturn("{\"error\":\"invalid_client\"}");

        when(tokenResponse.statusCode()).thenReturn(200);
        when(tokenResponse.body()).thenReturn(
            "{\"access_token\":\"token-123\",\"expires_in\":300}"
        );

        when(httpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(CompletableFuture.completedFuture(unauthorizedResponse))
            .thenReturn(CompletableFuture.completedFuture(tokenResponse));

        OAuth2TokenService service = new OAuth2TokenService(
            httpClient,
            "https://auth.example.test/oauth2/token",
            "minecraft-server",
            "super-secret",
            "api:read api:write"
        );

        String token = service.getToken().join();

        assertEquals("token-123", token);

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(
            HttpRequest.class
        );
        verify(httpClient, times(2)).sendAsync(
            requestCaptor.capture(),
            any(HttpResponse.BodyHandler.class)
        );

        HttpRequest firstRequest = requestCaptor.getAllValues().get(0);
        HttpRequest secondRequest = requestCaptor.getAllValues().get(1);

        assertTrue(firstRequest.headers().firstValue("Authorization").isPresent());
        assertFalse(secondRequest.headers().firstValue("Authorization").isPresent());
    }
}
