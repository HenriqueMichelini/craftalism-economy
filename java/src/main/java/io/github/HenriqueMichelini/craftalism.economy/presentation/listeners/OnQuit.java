package io.github.HenriqueMichelini.craftalism.economy.presentation.listeners;

import io.github.HenriqueMichelini.craftalism.economy.infra.api.repository.PlayerCacheRepository;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

public class OnQuit {
    private final PlayerCacheRepository cache;

    public OnQuit(PlayerCacheRepository cache) {
        this.cache = cache;
    }

    @EventHandler
    public void OnPlayerQuit(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        UUID uuid = player.getUniqueId();

        cache.delete(uuid);
    }
}