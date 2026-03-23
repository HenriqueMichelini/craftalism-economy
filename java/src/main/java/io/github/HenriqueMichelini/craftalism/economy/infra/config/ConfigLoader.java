package io.github.HenriqueMichelini.craftalism.economy.infra.config;

import io.github.HenriqueMichelini.craftalism.economy.CraftalismEconomy;
import io.github.HenriqueMichelini.craftalism.economy.infra.config.ConnectionConfig;
import java.util.Locale;

public final class ConfigLoader {

    private final CraftalismEconomy plugin;

    public ConfigLoader(CraftalismEconomy plugin) {
        this.plugin = plugin;
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
            return envUrl;
        }
        return new ConnectionConfig(plugin).getUrl();
    }

    public String authServerUrl() {
        String env = System.getenv("AUTH_ISSUER_URI");
        if (env != null && !env.isBlank()) return env;
        return new ConnectionConfig(plugin).getAuthServerUrl();
    }

    public String tokenPath() {
        String env = System.getenv("AUTH_TOKEN_PATH");
        if (env != null && !env.isBlank()) return env;
        return new ConnectionConfig(plugin).getTokenPath();
    }

    public String clientId() {
        String env = System.getenv("MINECRAFT_CLIENT_ID");
        if (env != null && !env.isBlank()) return env;
        return new ConnectionConfig(plugin).getClientId();
    }

    public String clientSecret() {
        String env = System.getenv("CRAFTALISM_API_KEY");
        if (env != null && !env.isBlank()) return env;
        return new ConnectionConfig(plugin).getClientSecret();
    }

    public String oauthScopes() {
        String env = System.getenv("MINECRAFT_CLIENT_SCOPES");
        if (env != null && !env.isBlank()) return env;
        return new ConnectionConfig(plugin).getScopes();
    }
}
