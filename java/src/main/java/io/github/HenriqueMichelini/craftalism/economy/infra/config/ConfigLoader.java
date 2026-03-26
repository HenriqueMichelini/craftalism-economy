package io.github.HenriqueMichelini.craftalism.economy.infra.config;

import io.github.HenriqueMichelini.craftalism.economy.CraftalismEconomy;
import java.util.Locale;

public final class ConfigLoader {

    private final CraftalismEconomy plugin;
    private final ConnectionConfig connectionConfig;

    private static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = 5;
    private static final int DEFAULT_REQUEST_TIMEOUT_SECONDS = 10;

    public ConfigLoader(CraftalismEconomy plugin) {
        this(plugin, new SystemEnvironment());
    }

    ConfigLoader(CraftalismEconomy plugin, EnvironmentI environment) {
        this.plugin = plugin;
        this.connectionConfig = new ConnectionConfig(plugin);
    }

    public Locale locale() {
        final String raw = plugin.getConfig().getString("locale", "en-US");
        final String normalized = raw.replace('_', '-');
        final Locale parsed = Locale.forLanguageTag(normalized);

        boolean valid = false;
        for (Locale available : Locale.getAvailableLocales()) {
            if (
                available.getLanguage().equals(parsed.getLanguage()) &&
                available.getCountry().equals(parsed.getCountry())
            ) {
                valid = true;
                break;
            }
        }

        if (!valid) {
            plugin
                .getLogger()
                .warning("Invalid locale '" + raw + "', defaulting to en-US");
            return Locale.US;
        }

        return parsed;
    }

    public String currencySymbol() {
        return plugin.getConfig().getString("currency-symbol", "$");
    }

    public String nullRepresentation() {
        return plugin.getConfig().getString("null-representation", "—");
    }

    public Long defaultBalance() {
        long value = plugin
            .getConfig()
            .getLong("default-balance", 100_000_000L);
        if (value < 0) {
            plugin
                .getLogger()
                .warning("Invalid negative default balance, using 100000000");
            return 100_000_000L;
        }
        return value;
    }

    public String baseUrl() {
        String envUrl = System.getenv("CRAFTALISM_API_URL");
        if (envUrl != null && !envUrl.isBlank()) {
            return normalizeBaseUrl(envUrl, "CRAFTALISM_API_URL");
        }
        return normalizeBaseUrl(
            connectionConfig.getUrl(),
            "connection-config.yml:url"
        );
    }

    public String authServerUrl() {
        String env = System.getenv("AUTH_ISSUER_URI");
        if (env != null && !env.isBlank()) {
            return normalizeBaseUrl(env, "AUTH_ISSUER_URI");
        }
        return normalizeBaseUrl(
            connectionConfig.getAuthServerUrl(),
            "connection-config.yml:auth-server-url"
        );
    }

    public String tokenPath() {
        String env = System.getenv("AUTH_TOKEN_PATH");
        if (env != null && !env.isBlank()) {
            return normalizeTokenPath(env, "AUTH_TOKEN_PATH");
        }
        return normalizeTokenPath(
            connectionConfig.getTokenPath(),
            "connection-config.yml:token-path"
        );
    }

    public String clientId() {
        String env = System.getenv("MINECRAFT_CLIENT_ID");
        if (env != null && !env.isBlank()) return env;
        return connectionConfig.getClientId();
    }

    public String clientSecret() {
        String env = System.getenv("MINECRAFT_CLIENT_SECRET");
        if (env != null && !env.isBlank()) return env;
        env = System.getenv("CRAFTALISM_API_KEY");
        if (env != null && !env.isBlank()) return env;
        String configured = connectionConfig.getClientSecret();
        if (configured == null || configured.isBlank()) {
            plugin
                .getLogger()
                .warning(
                    "OAuth client secret is empty. Configure MINECRAFT_CLIENT_SECRET for production."
                );
            return "";
        }
        return configured;
    }

    public String oauthScopes() {
        String env = System.getenv("MINECRAFT_CLIENT_SCOPES");
        if (env != null && !env.isBlank()) return env;
        return connectionConfig.getScopes();
    }

    public int httpConnectTimeoutSeconds() {
        return normalizeTimeout(
            connectionConfig.getHttpConnectTimeoutSeconds(),
            DEFAULT_CONNECT_TIMEOUT_SECONDS,
            "connection-config.yml:http-connect-timeout-seconds"
        );
    }

    public int httpRequestTimeoutSeconds() {
        return normalizeTimeout(
            connectionConfig.getHttpRequestTimeoutSeconds(),
            DEFAULT_REQUEST_TIMEOUT_SECONDS,
            "connection-config.yml:http-request-timeout-seconds"
        );
    }

    private int normalizeTimeout(int value, int fallback, String key) {
        if (value <= 0) {
            plugin
                .getLogger()
                .warning(
                    "Invalid timeout for " +
                        key +
                        " (" +
                        value +
                        "), falling back to " +
                        fallback +
                        " seconds."
                );
            return fallback;
        }
        return value;
    }

    private String normalizeBaseUrl(String value, String sourceName) {
        String trimmed = value == null ? "" : value.trim();
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            plugin
                .getLogger()
                .warning(
                    "Invalid URL in " +
                        sourceName +
                        ": '" +
                        value +
                        "'. Falling back to http://localhost:8080"
                );
            return "http://localhost:8080";
        }
        return trimmed.endsWith("/")
            ? trimmed.substring(0, trimmed.length() - 1)
            : trimmed;
    }

    private String normalizeTokenPath(String value, String sourceName) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isBlank()) {
            plugin
                .getLogger()
                .warning(
                    "Blank token path in " +
                        sourceName +
                        ", falling back to /oauth2/token"
                );
            return "/oauth2/token";
        }
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        return trimmed.startsWith("/") ? trimmed : "/" + trimmed;
    }
}
