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

    private LogManager logManager;
    private PluginLogger pluginLogger;

    private CurrencyFormatter currencyFormatter;
    private CurrencyParser currencyParser;

    private BalanceApiService balanceApiService;
    private PlayerApiService playerApiService;
    private TransactionApiService transactionApiService;

    private PlayerApplicationService playerApplicationService;
    private PayCommandApplicationService payCommandApplicationService;
    private BalanceApplicationService balanceApplicationService;
    private BalanceCommandApplicationService balanceCommandApplicationService;
    private BaltopCommandApplicationService baltopCommandApplicationService;
    private SetBalanceCommandApplicationService setBalanceCommandApplicationService;

    public BootContainer(CraftalismEconomy plugin, JavaPlugin javaPlugin) {
        this.plugin = plugin;
        this.javaPlugin = javaPlugin;
    }

    public void initialize() {
        // 1. Load configuration
        ConfigLoader configLoader = new ConfigLoader(plugin);

        // 2. Logging
        this.logManager = new LogManager(plugin);
        this.pluginLogger = new PluginLogger(plugin, logManager);

        // 3. Formatters
        FormatterFactory formatterFactory = new FormatterFactory(
                configLoader,
                plugin,
                pluginLogger
        );

        this.currencyFormatter = formatterFactory.getFormatter();
        this.currencyParser = formatterFactory.getParser();

        // 4. API services
        ApiServiceFactory apiFactory = new ApiServiceFactory(configLoader);

        this.playerApiService = apiFactory.getPlayerApi();
        this.balanceApiService = apiFactory.getBalanceApi();
        this.transactionApiService = apiFactory.getTransactionApi();

        // 5. Application Services
        ApplicationServiceFactory appFactory = new ApplicationServiceFactory(javaPlugin, apiFactory);

        this.playerApplicationService = appFactory.getPlayerApplication();
        this.payCommandApplicationService = appFactory.getPayCommandApplication();
        this.balanceApplicationService = appFactory.getBalanceApplication();
        this.balanceCommandApplicationService = appFactory.getBalanceCommandApplication();
        this.baltopCommandApplicationService = appFactory.getBaltopCommandApplication();
        this.setBalanceCommandApplicationService = appFactory.setBalanceCommandApplication();

        // 6. Command registration
        new CommandRegistrar(
                plugin,
                appFactory,
                formatterFactory
        ).registerAll();

        new EventRegistrar(
                plugin,
                playerApplicationService,
                balanceApplicationService
        ).registerAll();
    }

    public void shutdown() {
        // flush caches, send pending balances, etc
    }

    public CurrencyFormatter getCurrencyFormatter() { return currencyFormatter; }
    public CurrencyParser getCurrencyParser() { return currencyParser; }
    public PluginLogger getPluginLogger() { return pluginLogger; }
}