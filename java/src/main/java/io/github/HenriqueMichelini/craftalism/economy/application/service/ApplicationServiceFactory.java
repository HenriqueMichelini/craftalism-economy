package io.github.HenriqueMichelini.craftalism.economy.application.service;

import io.github.HenriqueMichelini.craftalism.economy.infra.api.repository.BalanceCacheRepository;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.repository.PlayerCacheRepository;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.service.ApiServiceFactory;
import org.bukkit.plugin.java.JavaPlugin;

public final class ApplicationServiceFactory {

    private final PlayerApplicationService playerApp;
    private final BalanceApplicationService balanceApp;
    private final TransactionApplicationService transactionApp;

    private final PayCommandApplicationService payCmdApp;
    private final BalanceCommandApplicationService balanceCmdApp;
    private final BaltopCommandApplicationService baltopCmdApp;
    private final SetBalanceCommandApplicationService setBalanceCmdApp;

    public ApplicationServiceFactory(JavaPlugin plugin, ApiServiceFactory apis) {

        PlayerCacheRepository playerCache = new PlayerCacheRepository();

        this.playerApp = new PlayerApplicationService(
                apis.getPlayerApi(),
                playerCache
        );

        BalanceCacheRepository balanceCache = new BalanceCacheRepository();

        this.balanceApp = new BalanceApplicationService(
                apis.getBalanceApi(),
                balanceCache
        );

        this.transactionApp = new TransactionApplicationService(apis.getTransactionApi());

        this.payCmdApp = new PayCommandApplicationService(
                playerApp,
                apis.getPlayerApi(),
                apis.getBalanceApi(),
                apis.getTransactionApi(),
                plugin
        );

        this.balanceCmdApp = new BalanceCommandApplicationService(
                playerApp,
                balanceApp
        );

        this.baltopCmdApp = new BaltopCommandApplicationService(
                apis.getBalanceApi(),
                apis.getPlayerApi()
        );

        this.setBalanceCmdApp = new SetBalanceCommandApplicationService(
                apis.getBalanceApi(),
                playerApp
        );
    }

    public PlayerApplicationService getPlayerApplication() { return playerApp; }
    public BalanceApplicationService getBalanceApplication() { return balanceApp; }
    public TransactionApplicationService getTransactionApplication() {return transactionApp; }
    public PayCommandApplicationService getPayCommandApplication() { return payCmdApp; }
    public BalanceCommandApplicationService getBalanceCommandApplication() { return balanceCmdApp; }
    public BaltopCommandApplicationService getBaltopCommandApplication() { return baltopCmdApp; }
    public SetBalanceCommandApplicationService setBalanceCommandApplication() { return setBalanceCmdApp; }

    public void shutdown() {
        // persist caches, shutdown http client, etc.
//        playerCache.clear();
//        balanceCache.clear();
    }
}