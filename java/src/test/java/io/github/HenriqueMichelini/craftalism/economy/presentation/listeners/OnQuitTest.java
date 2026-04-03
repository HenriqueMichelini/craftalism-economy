package io.github.HenriqueMichelini.craftalism.economy.presentation.listeners;

import io.github.HenriqueMichelini.craftalism.economy.application.service.PlayerApplicationService;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OnQuitTest {

    @Test
    void shouldEvictPlayerCacheOnQuit() {
        PlayerApplicationService playerService = mock(PlayerApplicationService.class);
        PlayerQuitEvent event = mock(PlayerQuitEvent.class);
        Player player = mock(Player.class);
        UUID playerUuid = UUID.randomUUID();

        when(event.getPlayer()).thenReturn(player);
        when(player.getUniqueId()).thenReturn(playerUuid);

        OnQuit listener = new OnQuit(playerService);
        listener.onPlayerQuit(event);

        verify(playerService).evictCachedPlayer(playerUuid);
    }
}
