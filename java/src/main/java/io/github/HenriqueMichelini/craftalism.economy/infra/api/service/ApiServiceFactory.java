package io.github.HenriqueMichelini.craftalism.economy.infra.api.service;

import com.google.gson.Gson;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.client.HttpClientService;
import io.github.HenriqueMichelini.craftalism.economy.infra.config.ConfigLoader;
import io.github.HenriqueMichelini.craftalism.economy.infra.config.GsonFactory;

public final class ApiServiceFactory {
    private final ConfigLoader cfg;
    private final Gson gson = GsonFactory.getInstance();

    // lightweight lazy-initialized services
    private HttpClientService httpClient;
    private PlayerApiService playerApiService;
    private BalanceApiService balanceApiService;
    private TransactionApiService transactionApiService;

    public ApiServiceFactory(ConfigLoader cfg) { this.cfg = cfg; }

    private synchronized void ensureHttpClient() {
        if (httpClient == null) httpClient = new HttpClientService(cfg.baseUrl());
    }

    public PlayerApiService getPlayerApi() {
        ensureHttpClient();
        if (playerApiService == null) playerApiService = new PlayerApiService(httpClient, gson);
        return playerApiService;
    }

    public BalanceApiService getBalanceApi() {
        ensureHttpClient();
        if (balanceApiService == null) balanceApiService = new BalanceApiService(httpClient);
        return balanceApiService;
    }

    public TransactionApiService getTransactionApi() {
        ensureHttpClient();
        if (transactionApiService == null) transactionApiService = new TransactionApiService(httpClient);
        return transactionApiService;
    }
}