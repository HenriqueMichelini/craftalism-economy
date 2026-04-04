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

    public ApplicationServiceFactory(
            JavaPlugin plugin,
            ApiServiceFactory apis,
            long defaultBalance,
            boolean legacyPayFallbackEnabled
    ) {
        PlayerCacheRepository playerCache = new PlayerCacheRepository();
        BalanceCacheRepository balanceCache = new BalanceCacheRepository();

        this.playerApp = new PlayerApplicationService(apis.getPlayerApi(), playerCache);
        this.balanceApp = new BalanceApplicationService(apis.getBalanceApi(), balanceCache, defaultBalance);
        this.transactionApp = new TransactionApplicationService(apis.getTransactionApi());

        this.payCmdApp = new PayCommandApplicationService(
                playerApp,
                apis.getPlayerApi(),
                new PaymentTransferService(
                        apis.getBalanceApi(),
                        legacyPayFallbackEnabled,
                        plugin.getLogger()
                ),
                plugin.getLogger()
        );

        this.balanceCmdApp = new BalanceCommandApplicationService(playerApp, balanceApp);
        this.baltopCmdApp = new BaltopCommandApplicationService(apis.getBalanceApi(), apis.getPlayerApi());
        this.setBalanceCmdApp = new SetBalanceCommandApplicationService(apis.getBalanceApi(), playerApp);
    }

    public PlayerApplicationService getPlayerApplication() {
        return playerApp;
    }

    public BalanceApplicationService getBalanceApplication() {
        return balanceApp;
    }

    public TransactionApplicationService getTransactionApplication() {
        return transactionApp;
    }

    public PayCommandApplicationService getPayCommandApplication() {
        return payCmdApp;
    }

    public BalanceCommandApplicationService getBalanceCommandApplication() {
        return balanceCmdApp;
    }

    public BaltopCommandApplicationService getBaltopCommandApplication() {
        return baltopCmdApp;
    }

    public SetBalanceCommandApplicationService getSetBalanceCommandApplication() {
        return setBalanceCmdApp;
    }

    public void shutdown() {
        playerApp.clearCache();
        balanceApp.clearCache();
    }
}
