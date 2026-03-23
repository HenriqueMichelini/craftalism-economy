package io.github.HenriqueMichelini.craftalism.economy.presentation.listeners;

import io.github.HenriqueMichelini.craftalism.economy.application.service.BalanceApplicationService;
import io.github.HenriqueMichelini.craftalism.economy.application.service.PlayerApplicationService;
import org.bukkit.plugin.java.JavaPlugin;

public class EventRegistrar {
    private final JavaPlugin plugin;
    private final PlayerApplicationService playerApplicationService;
    private final BalanceApplicationService balanceApplicationService;

    public EventRegistrar(
            JavaPlugin plugin,
            PlayerApplicationService playerApplicationService, BalanceApplicationService balanceApplicationService

    ) {
        this.plugin = plugin;
        this.playerApplicationService = playerApplicationService;
        this.balanceApplicationService = balanceApplicationService;
    }

    public void registerAll() {
        plugin.getServer().getPluginManager().registerEvents(
                new OnJoin(playerApplicationService, balanceApplicationService),
                plugin
        );
    }
}