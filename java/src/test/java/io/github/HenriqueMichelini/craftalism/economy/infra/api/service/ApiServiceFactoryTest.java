package io.github.HenriqueMichelini.craftalism.economy.infra.api.service;

import io.github.HenriqueMichelini.craftalism.economy.infra.api.client.HttpClientService;
import io.github.HenriqueMichelini.craftalism.economy.infra.config.ConfigLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.MockedConstruction;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApiServiceFactoryTest {

    private ApiServiceFactory factory;

    @BeforeEach
    void setUp() {
        ConfigLoader cfg = mock(ConfigLoader.class);
        when(cfg.baseUrl()).thenReturn("http://localhost:8080");
        when(cfg.authServerUrl()).thenReturn("http://localhost:9000");
        when(cfg.tokenPath()).thenReturn("/oauth2/token");
        when(cfg.clientId()).thenReturn("minecraft-server");
        when(cfg.clientSecret()).thenReturn("secret");
        when(cfg.oauthScopes()).thenReturn("api:read api:write");
        when(cfg.httpConnectTimeoutSeconds()).thenReturn(5);
        when(cfg.httpRequestTimeoutSeconds()).thenReturn(10);
        factory = new ApiServiceFactory(cfg);
    }

    @Test
    void playerApi_shouldBeLazyInitialized() {
        PlayerApiService api = factory.getPlayerApi();

        assertNotNull(api);
        assertSame(api, factory.getPlayerApi(), "Should return same instance");
    }

    @Test
    void balanceApi_shouldBeLazyInitialized() {
        BalanceApiService api = factory.getBalanceApi();

        assertNotNull(api);
        assertSame(api, factory.getBalanceApi(), "Should return same instance");
    }

    @Test
    void transactionApi_shouldBeLazyInitialized() {
        TransactionApiService api = factory.getTransactionApi();

        assertNotNull(api);
        assertSame(api, factory.getTransactionApi(), "Should return same instance");
    }

    @Test
    void allApisShouldReuseSameHttpClient() {
        try (MockedConstruction<HttpClientService> mock = mockConstruction(HttpClientService.class)) {

            PlayerApiService p = factory.getPlayerApi();
            BalanceApiService b = factory.getBalanceApi();
            TransactionApiService t = factory.getTransactionApi();

            assertNotNull(p);
            assertNotNull(b);
            assertNotNull(t);
        }
    }
}
