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

            assertEquals(1, mock.constructed().size(), "HttpClient must be shared across APIs");

            HttpClientService httpClient = mock.constructed().getFirst();

            assertSame(httpClient, getHttpClientFromPlayer(p));
            assertSame(httpClient, getHttpClientFromBalance(b));
            assertSame(httpClient, getHttpClientFromTransaction(t));
        }
    }

    private HttpClientService getHttpClientFromPlayer(PlayerApiService api) {
        try {
            var field = PlayerApiService.class.getDeclaredField("http");
            field.setAccessible(true);
            return (HttpClientService) field.get(api);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private HttpClientService getHttpClientFromBalance(BalanceApiService api) {
        try {
            var field = BalanceApiService.class.getDeclaredField("http");
            field.setAccessible(true);
            return (HttpClientService) field.get(api);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private HttpClientService getHttpClientFromTransaction(TransactionApiService api) {
        try {
            var field = TransactionApiService.class.getDeclaredField("http");
            field.setAccessible(true);
            return (HttpClientService) field.get(api);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
