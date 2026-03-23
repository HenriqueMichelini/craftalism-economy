package io.github.HenriqueMichelini.craftalism.economy.domain.service.logs.messages;

import io.github.HenriqueMichelini.craftalism.economy.domain.service.logs.LogManager;
import io.github.HenriqueMichelini.craftalism.economy.domain.service.logs.PluginLogger;
import org.bukkit.entity.Player;

public class BalanceMessages {
    private final PluginLogger pluginLogger;

    public BalanceMessages(PluginLogger pluginLogger) {
        this.pluginLogger = pluginLogger;
    }

    public void sendBalanceSelfSuccess(Player messageReceiver, String balance) {
        LogManager.Placeholder balancePlaceholder = new LogManager.Placeholder("balance", balance);

        pluginLogger.send(messageReceiver, "balance.self.success", balancePlaceholder);
    }

    public void sendBalanceOtherSuccess(Player messageReceiver, String targetName, String balance) {
        LogManager.Placeholder targetNamePlaceholder = new LogManager.Placeholder("target", targetName);
        LogManager.Placeholder balancePlaceholder = new LogManager.Placeholder("balance", balance);

        pluginLogger.send(messageReceiver, "balance.other.success", targetNamePlaceholder, balancePlaceholder);
    }

    public void sendBalanceInvalidName(Player messageReceiver) {
        pluginLogger.send(messageReceiver, "balance.other.invalid_name");
    }

    public void sendBalanceOtherNotFound(Player messageReceiver, String targetName) {
        LogManager.Placeholder targetNamePlaceholder = new LogManager.Placeholder("target", targetName);

        pluginLogger.send(messageReceiver, "balance.other.not_found", targetNamePlaceholder);
    }

    public void sendBalanceOtherNoBalance(Player messageReceiver, String targetName) {
        LogManager.Placeholder targetNamePlaceholder = new LogManager.Placeholder("target", targetName);

        pluginLogger.send(messageReceiver, "balance.other.no_balance", targetNamePlaceholder);
    }

    public void sendBalanceUsage(Player messageReceiver) {
        pluginLogger.send(messageReceiver, "balance.usage");
    }

    public void sendBalanceError(Player messageReceiver) {
        pluginLogger.send(messageReceiver, "balance.error");
    }

    public void sendBalancePlayerOnly() {
        pluginLogger.info("balance.player_only");
    }

    public void sendBalanceNoPermission(Player messageReceiver) {
        pluginLogger.send(messageReceiver, "balance.no_permission");
    }
}