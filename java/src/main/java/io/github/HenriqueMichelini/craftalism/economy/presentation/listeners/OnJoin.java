package io.github.HenriqueMichelini.craftalism.economy.presentation.listeners;

import io.github.HenriqueMichelini.craftalism.economy.application.service.BalanceApplicationService;
import io.github.HenriqueMichelini.craftalism.economy.application.service.PlayerApplicationService;
import java.util.UUID;
import java.util.concurrent.CompletionException;
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
            .thenCompose(p -> balanceService.loadBalanceOnJoin(uuid))
            .exceptionally(ex -> {
                Throwable cause = unwrap(ex);
                System.err.println(
                    "Erro ao inicializar player " +
                    uuid +
                    ": " +
                    cause.getClass().getSimpleName() +
                    ": " +
                    cause.getMessage()
                );
                return null;
            });
    }

    private Throwable unwrap(Throwable ex) {
        Throwable current = ex;
        while (
            current.getCause() != null &&
            current instanceof CompletionException
        ) {
            current = current.getCause();
        }
        return current;
    }
}
