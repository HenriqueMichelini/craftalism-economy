package io.github.HenriqueMichelini.craftalism.economy.domain.service.currency;

import io.github.HenriqueMichelini.craftalism.economy.domain.service.logs.PluginLogger;
import io.github.HenriqueMichelini.craftalism.economy.infra.config.ConfigLoader;
import org.bukkit.plugin.java.JavaPlugin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FormatterFactoryTest {

    private ConfigLoader cfg;
    private JavaPlugin plugin;
    private PluginLogger logger;

    @BeforeEach
    void setup() {
        cfg = mock(ConfigLoader.class);
        plugin = mock(JavaPlugin.class);
        logger = mock(PluginLogger.class);

        when(cfg.locale()).thenReturn(java.util.Locale.US);
        when(cfg.currencySymbol()).thenReturn("$");
        when(cfg.nullRepresentation()).thenReturn("—");
    }

    @Test
    void shouldCreateFormatter() {
        when(cfg.locale()).thenReturn(Locale.US);
        when(cfg.currencySymbol()).thenReturn("$");
        when(cfg.nullRepresentation()).thenReturn("—");

        FormatterFactory factory = new FormatterFactory(cfg, plugin, logger);
        CurrencyFormatter formatter = factory.getFormatter();

        assertNotNull(formatter);

        assertEquals("$", formatter.getCurrencySymbol());
        assertEquals("—", formatter.getFallbackValue());

        String formatted = formatter.formatCurrency(123456789L);

        assertTrue(formatted.startsWith("$"));
        assertTrue(formatted.contains(","));
        assertTrue(formatted.contains("."));
    }

    @Test
    void shouldCreateParser() {
        FormatterFactory factory = new FormatterFactory(cfg, plugin, logger);
        CurrencyParser parser = factory.getParser();

        assertNotNull(parser);
    }

    @Test
    void shouldReturnSameInstances() {
        FormatterFactory factory = new FormatterFactory(cfg, plugin, logger);

        CurrencyFormatter f1 = factory.getFormatter();
        CurrencyFormatter f2 = factory.getFormatter();

        CurrencyParser p1 = factory.getParser();
        CurrencyParser p2 = factory.getParser();

        assertSame(f1, f2, "FormatterFactory should return the same formatter instance");
        assertSame(p1, p2, "FormatterFactory should return the same parser instance");
    }

    @Test
    void shouldCallConfigLoaderOnce() {
        new FormatterFactory(cfg, plugin, logger);

        verify(cfg, times(1)).locale();
        verify(cfg, times(1)).currencySymbol();
        verify(cfg, times(1)).nullRepresentation();
    }
}
