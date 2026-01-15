package io.github.HenriqueMichelini.craftalism.economy.infra.config;

import io.github.HenriqueMichelini.craftalism.economy.CraftalismEconomy;
import org.bukkit.configuration.file.FileConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfigLoaderTest {
    private FileConfiguration config;
    private Logger logger;
    private ConfigLoader loader;

    @BeforeEach
    void setUp() {
        CraftalismEconomy plugin = mock(CraftalismEconomy.class);
        config = mock(FileConfiguration.class);
        logger = mock(Logger.class);

        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getLogger()).thenReturn(logger);

        loader = new ConfigLoader(plugin);
    }

    @Test
    void locale_validLocale_returnsCorrectLocale() {
        when(config.getString("locale", "en-US")).thenReturn("pt-BR");

        Locale result = loader.locale();

        assertEquals(Locale.forLanguageTag("pt-BR"), result);
    }

    @Test
    void locale_invalidLocale_fallsBackToUS() {
        when(config.getString("locale", "en-US")).thenReturn("INVALID_LOCALE");

        Locale result = loader.locale();

        assertEquals(Locale.US, result);
        verify(logger).warning("Invalid locale 'INVALID_LOCALE', defaulting to en-US");
    }

    @Test
    void currencySymbol_readsValueOrDefault() {
        when(config.getString("currency-symbol", "$")).thenReturn("€");

        assertEquals("€", loader.currencySymbol());
    }

    @Test
    void nullRepresentation_readsValueOrDefault() {
        when(config.getString("null-representation", "—")).thenReturn("N/A");

        assertEquals("N/A", loader.nullRepresentation());
    }

    @Test
    void defaultBalance_withPositiveValue_returnsConfigured() {
        when(config.getLong("default-balance", 100_000_000L))
                .thenReturn(500_000L);

        assertEquals(500_000L, loader.defaultBalance());
    }

    @Test
    void defaultBalance_withNegativeValue_returnsFallback() {
        when(config.getLong("default-balance", 100_000_000L))
                .thenReturn(-50L);

        long result = loader.defaultBalance();

        assertEquals(100_000_000L, result);
        verify(logger).warning("Invalid negative default balance, using 100000000");
    }

    @Test
    void baseUrl_readsValueOrDefault() {
        when(config.getString("api-base-url", "http://localhost:8080"))
                .thenReturn("https://external-api.test");

        assertEquals("https://external-api.test", loader.baseUrl());
    }

    @Test
    void baseUrl_usesDefaultWhenMissing() {
        when(config.getString("api-base-url", "http://localhost:8080"))
                .thenReturn("http://localhost:8080");

        assertEquals("http://localhost:8080", loader.baseUrl());
    }
}
