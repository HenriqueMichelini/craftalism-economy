package io.github.HenriqueMichelini.craftalism.economy.presentation.listeners;

import io.github.HenriqueMichelini.craftalism.economy.application.service.PlayerApplicationService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class OnQuit implements Listener {
    private final PlayerApplicationService playerService;

    public OnQuit(PlayerApplicationService playerService) {
        this.playerService = playerService;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        playerService.evictCachedPlayer(uuid);
    }
}
