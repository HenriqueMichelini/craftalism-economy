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
    private final OAuth2TokenService tokenService;

    // lightweight lazy-initialized services
    private HttpClientService httpClient;
    private PlayerApiService playerApiService;
    private BalanceApiService balanceApiService;
    private TransactionApiService transactionApiService;

    public ApiServiceFactory(ConfigLoader cfg) {
        this.cfg = cfg;

        this.tokenService = new OAuth2TokenService(
            HttpClient.newHttpClient(),
            resolveTokenUrl(cfg),
            cfg.clientId(),
            cfg.clientSecret(),
            cfg.oauthScopes()
        );

        this.httpClient =
            new HttpClientService(
                cfg.baseUrl(),
                this.tokenService,
                cfg.httpConnectTimeoutSeconds(),
                cfg.httpRequestTimeoutSeconds()
            );
    }

    private synchronized void ensureHttpClient() {
        if (httpClient == null) httpClient = new HttpClientService(
            cfg.baseUrl(),
            tokenService,
            cfg.httpConnectTimeoutSeconds(),
            cfg.httpRequestTimeoutSeconds()
        );
    }

    public PlayerApiService getPlayerApi() {
        ensureHttpClient();
        if (playerApiService == null) playerApiService = new PlayerApiService(
            httpClient,
            gson
        );
        return playerApiService;
    }

    public BalanceApiService getBalanceApi() {
        ensureHttpClient();
        if (balanceApiService == null) balanceApiService =
            new BalanceApiService(httpClient);
        return balanceApiService;
    }

    public TransactionApiService getTransactionApi() {
        ensureHttpClient();
        if (transactionApiService == null) transactionApiService =
            new TransactionApiService(httpClient);
        return transactionApiService;
    }

    private static String resolveTokenUrl(ConfigLoader cfg) {
        String tokenPath = cfg.tokenPath();
        if (tokenPath.startsWith("http://") || tokenPath.startsWith("https://")) {
            return tokenPath;
        }

        String authServerUrl = cfg.authServerUrl();
        if (authServerUrl.endsWith("/") && tokenPath.startsWith("/")) {
            return authServerUrl.substring(0, authServerUrl.length() - 1) + tokenPath;
        }

        if (!authServerUrl.endsWith("/") && !tokenPath.startsWith("/")) {
            return authServerUrl + "/" + tokenPath;
        }

        return authServerUrl + tokenPath;
    }
}
