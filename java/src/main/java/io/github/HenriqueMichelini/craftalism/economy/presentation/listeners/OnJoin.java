package io.github.HenriqueMichelini.craftalism.economy.presentation.listeners;

import io.github.HenriqueMichelini.craftalism.economy.application.service.BalanceApplicationService;
import io.github.HenriqueMichelini.craftalism.economy.application.service.PlayerApplicationService;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class OnJoin implements Listener {

    private final PlayerApplicationService playerService;
    private final BalanceApplicationService balanceService;

    public OnJoin(
        PlayerApplicationService playerService,
        BalanceApplicationService balanceService
    ) {
        this.playerService = playerService;
        this.balanceService = balanceService;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        UUID uuid = player.getUniqueId();
        String name = player.getName();

        playerService
            .loadPlayerOnJoin(uuid, name)
            .thenCompose(p -> balanceService.loadBalanceOnJoin(uuid)) // 🔥 FIX: chain
            .exceptionally(ex -> {
                System.err.println(
                    "Erro ao inicializar player " + uuid + ": " + ex
                );
                return null;
            });
    }
}
