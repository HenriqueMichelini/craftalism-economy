package io.github.HenriqueMichelini.craftalism.economy.domain.service.logs.messages;

import io.github.HenriqueMichelini.craftalism.economy.domain.service.logs.LogManager;
import io.github.HenriqueMichelini.craftalism.economy.domain.service.logs.PluginLogger;
import org.bukkit.entity.Player;

public class BaltopMessages {
    private final PluginLogger pluginLogger;

    public BaltopMessages(PluginLogger pluginLogger) {
        this.pluginLogger = pluginLogger;
    }

    public void sendBaltopHeader(Player messageReceiver, String count) {
        LogManager.Placeholder countPlaceholder = new LogManager.Placeholder("count", count);

        pluginLogger.send(messageReceiver, "baltop.header", countPlaceholder);
    }

    public void sendBaltopEntry(Player messageReceiver, String rank, String player, String balance) {
        LogManager.Placeholder rankPlaceholder = new LogManager.Placeholder("rank", rank);
        LogManager.Placeholder playerPlaceholder = new LogManager.Placeholder("player", player);
        LogManager.Placeholder balancePlaceholder = new LogManager.Placeholder("balance", balance);

        pluginLogger.send(messageReceiver, "baltop.entry", rankPlaceholder, playerPlaceholder, balancePlaceholder);
    }

    public void sendBaltopNoData(Player messageReceiver) {
        pluginLogger.send(messageReceiver, "baltop.no_data");
    }

    public void sendBaltopLoading(Player messageReceiver) {
        pluginLogger.send(messageReceiver, "baltop.loading");
    }

    public void sendBaltopPlayerOnly() {
        pluginLogger.info("baltop.player_only");
    }

    public void sendBaltopError(Player messageReceiver) {
        pluginLogger.send(messageReceiver, "baltop.error");
    }

    public void sendBaltopUsage(Player messageReceiver) {
        pluginLogger.send(messageReceiver, "baltop.usage");
    }

    public void sendBaltopNoPermission(Player messageReceiver) {
        pluginLogger.send(messageReceiver, "baltop.no_permission");
    }
}