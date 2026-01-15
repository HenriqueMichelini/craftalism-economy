package io.github.HenriqueMichelini.craftalism.economy.infra.config;

import io.github.HenriqueMichelini.craftalism.economy.CraftalismEconomy;

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
            if (available.getLanguage().equals(parsed.getLanguage()) &&
                    available.getCountry().equals(parsed.getCountry())) {
                valid = true;
                break;
            }
        }

        if (!valid) {
            plugin.getLogger().warning("Invalid locale '" + raw + "', defaulting to en-US");
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
        long value = plugin.getConfig().getLong("default-balance", 100_000_000L);
        if (value < 0) {
            plugin.getLogger().warning("Invalid negative default balance, using 100000000");
            return 100_000_000L;
        }
        return value;
    }

    public String baseUrl() {
        // Keep reading from config; the ConnectionConfig wrapper can be implemented as needed.
        return plugin.getConfig().getString("api-base-url", "http://localhost:8080");
    }
}