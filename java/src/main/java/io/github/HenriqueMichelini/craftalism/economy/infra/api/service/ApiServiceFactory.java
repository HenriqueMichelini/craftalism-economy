package io.github.HenriqueMichelini.craftalism.economy.infra.api.service;

import com.google.gson.Gson;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.client.HttpClientService;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.client.OAuth2TokenService;
import io.github.HenriqueMichelini.craftalism.economy.infra.config.ConfigLoader;
import io.github.HenriqueMichelini.craftalism.economy.infra.config.GsonFactory;
import java.net.http.HttpClient;

public final class ApiServiceFactory {

    private final ConfigLoader cfg;
    private final Gson gson = GsonFactory.getInstance();

    // lightweight lazy-initialized services
    private HttpClientService httpClient;
    private PlayerApiService playerApiService;
    private BalanceApiService balanceApiService;
    private TransactionApiService transactionApiService;

    public ApiServiceFactory(ConfigLoader cfg) {
        this.cfg = cfg;

        OAuth2TokenService tokenService = new OAuth2TokenService(
            HttpClient.newHttpClient(),
            cfg.authServerUrl(),
            cfg.clientId(),
            cfg.clientSecret()
        );

        this.httpClient = new HttpClientService(cfg.baseUrl(), tokenService);
    }

    private synchronized void ensureHttpClient(
        OAuth2TokenService tokenService
    ) {
        if (httpClient == null) httpClient = new HttpClientService(
            cfg.baseUrl(),
            tokenService
        );
    }

    public PlayerApiService getPlayerApi(OAuth2TokenService tokenService) {
        ensureHttpClient(tokenService);
        if (playerApiService == null) playerApiService = new PlayerApiService(
            httpClient,
            gson
        );
        return playerApiService;
    }

    public BalanceApiService getBalanceApi(OAuth2TokenService tokenService) {
        ensureHttpClient(tokenService);
        if (balanceApiService == null) balanceApiService =
            new BalanceApiService(httpClient);
        return balanceApiService;
    }

    public TransactionApiService getTransactionApi(
        OAuth2TokenService tokenService
    ) {
        ensureHttpClient(tokenService);
        if (transactionApiService == null) transactionApiService =
            new TransactionApiService(httpClient);
        return transactionApiService;
    }
}
