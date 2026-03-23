package io.github.HenriqueMichelini.craftalism.economy.domain.service.logs.messages;

import io.github.HenriqueMichelini.craftalism.economy.domain.service.logs.PluginLogger;
import org.bukkit.entity.Player;

public class SystemMessages {
    private final PluginLogger pluginLogger;

    public SystemMessages(PluginLogger pluginLogger) {
        this.pluginLogger = pluginLogger;
    }

    public void sendNoPermission(Player messageReceiver) {
        pluginLogger.send(messageReceiver, "system.no_permission");
    }

    public void sendPlayerNotFound(Player messageReceiver) {
        pluginLogger.send(messageReceiver, "system.player_not_found");
    }

    public void sendInvalidAmount(Player messageReceiver) {
        pluginLogger.send(messageReceiver, "system.invalid_amount");
    }

    public void sendInternalError(Player messageReceiver) {
        pluginLogger.send(messageReceiver, "system.internal_error");
    }
}