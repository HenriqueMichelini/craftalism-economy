package io.github.HenriqueMichelini.craftalism.economy.domain.service.currency;

import io.github.HenriqueMichelini.craftalism.economy.domain.service.logs.PluginLogger;
import io.github.HenriqueMichelini.craftalism.economy.domain.service.logs.messages.CurrencyMessages;
import io.github.HenriqueMichelini.craftalism.economy.infra.config.ConfigLoader;
import org.bukkit.plugin.java.JavaPlugin;

public final class FormatterFactory {
    private final CurrencyFormatter formatter;
    private final CurrencyParser parser;

    public FormatterFactory(ConfigLoader cfg, JavaPlugin javaPlugin, PluginLogger pluginLogger) {
        this.formatter = new CurrencyFormatter(
                cfg.locale(),
                cfg.currencySymbol(),
                cfg.nullRepresentation(),
                javaPlugin
        );
        this.parser = new CurrencyParser(new CurrencyMessages(pluginLogger));
    }

    public CurrencyFormatter getFormatter() { return formatter; }
    public CurrencyParser getParser() { return parser; }
}