package io.github.HenriqueMichelini.craftalism.economy;

import io.github.HenriqueMichelini.craftalism.economy.domain.service.currency.CurrencyFormatter;
import io.github.HenriqueMichelini.craftalism.economy.domain.service.currency.CurrencyParser;
import io.github.HenriqueMichelini.craftalism.economy.domain.service.logs.PluginLogger;
import io.github.HenriqueMichelini.craftalism.economy.infra.config.bootstrap.BootContainer;
import org.bukkit.plugin.java.JavaPlugin;

public final class CraftalismEconomy extends JavaPlugin {

    private BootContainer container;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();

        this.container = new BootContainer(this, this);
        try {
            this.container.initialize();
            getLogger().info("Plugin enabled successfully");
        } catch (RuntimeException exception) {
            getLogger().severe("Failed to enable CraftalismEconomy: " + exception.getMessage());
            throw exception;
        }
    }

    @Override
    public void onDisable() {
        if (container != null) {
            container.shutdown();
        }
    }

    public CurrencyFormatter getCurrencyFormatter() {
        return container.getCurrencyFormatter();
    }

    public CurrencyParser getCurrencyParser() {
        return container.getCurrencyParser();
    }

    public PluginLogger getPluginLogger() {
        return container.getPluginLogger();
    }
}
