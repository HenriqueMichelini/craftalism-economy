package io.github.HenriqueMichelini.craftalism.economy.domain.service.logs.messages;

import io.github.HenriqueMichelini.craftalism.economy.domain.service.logs.LogManager;
import io.github.HenriqueMichelini.craftalism.economy.domain.service.logs.PluginLogger;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetBalanceMessages {
    private final PluginLogger pluginLogger;

    public SetBalanceMessages(PluginLogger pluginLogger) {
        this.pluginLogger = pluginLogger;
    }

    public void sendSetBalanceSuccessSender(CommandSender messageReceiver, String targetName, String amount) {
        LogManager.Placeholder targetPlaceholder = new LogManager.Placeholder("target", targetName);
        LogManager.Placeholder amountPlaceholder = new LogManager.Placeholder("amount", amount);

        pluginLogger.send(messageReceiver, "setbalance.success.sender", targetPlaceholder, amountPlaceholder);
    }

    public void sendSetBalanceSuccessReceiver(Player messageReceiver, String amount, String senderName) {
        LogManager.Placeholder amountPlaceholder = new LogManager.Placeholder("amount", amount);
        LogManager.Placeholder senderPlaceholder = new LogManager.Placeholder("sender", senderName);

        pluginLogger.send(messageReceiver, "setbalance.success.receiver", amountPlaceholder, senderPlaceholder);
    }

    public void sendSetBalanceNoPermission(CommandSender messageReceiver) {
        pluginLogger.send(messageReceiver, "setbalance.error.no_permission");
    }

    public void sendSetBalancePlayerOnly(CommandSender messageReceiver) {
        pluginLogger.send(messageReceiver, "setbalance.error.player_only");
    }

    public void sendSetBalanceUsage(CommandSender messageReceiver) {
        pluginLogger.send(messageReceiver, "setbalance.error.usage");
    }

    public void sendSetBalanceInvalidName(CommandSender messageReceiver) {
        pluginLogger.send(messageReceiver, "setbalance.error.invalid_name");
    }

    public void sendSetBalancePlayerNotFound(CommandSender messageReceiver) {
        pluginLogger.send(messageReceiver, "setbalance.error.player_not_found");
    }

    public void sendSetBalanceInvalidAmount(CommandSender messageReceiver) {
        pluginLogger.send(messageReceiver, "setbalance.error.invalid_amount");
    }

    public void sendSetBalanceUpdateFailed(CommandSender messageReceiver) {
        pluginLogger.send(messageReceiver, "setbalance.error.update_failed");
    }

    public void sendSetBalanceException(CommandSender messageReceiver) {
        pluginLogger.send(messageReceiver, "setbalance.error.exception");
    }
}