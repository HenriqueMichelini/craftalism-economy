package io.github.HenriqueMichelini.craftalism.economy.infra.config.bootstrap;

import io.github.HenriqueMichelini.craftalism.economy.CraftalismEconomy;
import io.github.HenriqueMichelini.craftalism.economy.application.service.*;
import io.github.HenriqueMichelini.craftalism.economy.domain.service.currency.CurrencyFormatter;
import io.github.HenriqueMichelini.craftalism.economy.domain.service.currency.CurrencyParser;
import io.github.HenriqueMichelini.craftalism.economy.domain.service.currency.FormatterFactory;
import io.github.HenriqueMichelini.craftalism.economy.domain.service.logs.LogManager;
import io.github.HenriqueMichelini.craftalism.economy.domain.service.logs.PluginLogger;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.service.ApiServiceFactory;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.service.BalanceApiService;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.service.PlayerApiService;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.service.TransactionApiService;
import io.github.HenriqueMichelini.craftalism.economy.infra.config.ConfigLoader;
import io.github.HenriqueMichelini.craftalism.economy.presentation.commands.CommandRegistrar;
import io.github.HenriqueMichelini.craftalism.economy.presentation.listeners.EventRegistrar;
import org.bukkit.plugin.java.JavaPlugin;

public final class BootContainer {

    private final CraftalismEconomy plugin;
    private final JavaPlugin javaPlugin;

    private PluginLogger pluginLogger;

    private CurrencyFormatter currencyFormatter;
    private CurrencyParser currencyParser;

    private BalanceApiService balanceApiService;
    private PlayerApiService playerApiService;
    private TransactionApiService transactionApiService;

    private PlayerApplicationService playerApplicationService;
    private BalanceApplicationService balanceApplicationService;

    public BootContainer(CraftalismEconomy plugin, JavaPlugin javaPlugin) {
        this.plugin = plugin;
        this.javaPlugin = javaPlugin;
    }

    public void initialize() {
        ConfigLoader configLoader = new ConfigLoader(plugin);

        LogManager logManager = new LogManager(plugin);
        this.pluginLogger = new PluginLogger(plugin, logManager);

        FormatterFactory formatterFactory = new FormatterFactory(configLoader, plugin, pluginLogger);
        this.currencyFormatter = formatterFactory.getFormatter();
        this.currencyParser = formatterFactory.getParser();

        ApiServiceFactory apiFactory = new ApiServiceFactory(configLoader);
        this.playerApiService = apiFactory.getPlayerApi();
        this.balanceApiService = apiFactory.getBalanceApi();
        this.transactionApiService = apiFactory.getTransactionApi();

        long defaultBalance = configLoader.defaultBalance();
        ApplicationServiceFactory appFactory = new ApplicationServiceFactory(javaPlugin, apiFactory, defaultBalance);

        this.playerApplicationService = appFactory.getPlayerApplication();
        this.balanceApplicationService = appFactory.getBalanceApplication();

        new CommandRegistrar(plugin, appFactory, formatterFactory).registerAll();
        new EventRegistrar(plugin, playerApplicationService, balanceApplicationService).registerAll();
    }

    public void shutdown() {
        // flush caches, send pending balances, etc
    }

    public CurrencyFormatter getCurrencyFormatter() {
        return currencyFormatter;
    }

    public CurrencyParser getCurrencyParser() {
        return currencyParser;
    }

    public PluginLogger getPluginLogger() {
        return pluginLogger;
    }
}
