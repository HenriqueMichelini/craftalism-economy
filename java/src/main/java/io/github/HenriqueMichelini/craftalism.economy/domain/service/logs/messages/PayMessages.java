package io.github.HenriqueMichelini.craftalism.economy.domain.service.logs.messages;

import io.github.HenriqueMichelini.craftalism.economy.domain.service.logs.LogManager;
import io.github.HenriqueMichelini.craftalism.economy.domain.service.logs.PluginLogger;
import org.bukkit.entity.Player;

public class PayMessages {
    private final PluginLogger pluginLogger;

    public PayMessages(PluginLogger pluginLogger) {
        this.pluginLogger = pluginLogger;
    }

    public void sendPaySuccessSender(Player messageReceiver, String amount, String target) {
        LogManager.Placeholder amountPlaceholder = new LogManager.Placeholder("amount", amount);
        LogManager.Placeholder targetPlaceholder = new LogManager.Placeholder("target", target);

        pluginLogger.send(messageReceiver, "pay.success.sender", amountPlaceholder, targetPlaceholder);
    }

    public void sendPaySuccessReceiver(Player messageReceiver, String amount, String sender) {
        LogManager.Placeholder amountPlaceholder = new LogManager.Placeholder("amount", amount);
        LogManager.Placeholder senderPlaceholder = new LogManager.Placeholder("sender", sender);

        pluginLogger.send(messageReceiver, "pay.success.receiver", amountPlaceholder, senderPlaceholder);
    }

    public void sendPayPlayerOnly() {
        pluginLogger.info("pay.error.player_only");
    }

    public void sendPayUsage(Player messageReceiver) {
        pluginLogger.send(messageReceiver, "pay.error.usage");
    }

    public void sendPayPlayerEmpty(Player messageReceiver) {
        pluginLogger.send(messageReceiver, "pay.error.player_empty");
    }

    public void sendPayAmountEmpty(Player messageReceiver) {
        pluginLogger.send(messageReceiver, "pay.error.amount_empty");
    }

    public void sendPayPlayerNotFound(Player messageReceiver) {
        pluginLogger.send(messageReceiver, "pay.error.player_not_found");
    }

    public void sendPayNoAccount(Player messageReceiver, String target) {
        LogManager.Placeholder targetPlaceholder = new LogManager.Placeholder("target", target);

        pluginLogger.send(messageReceiver, "pay.error.no_account", targetPlaceholder);
    }

    public void sendPayInvalidName(Player messageReceiver) {
        pluginLogger.send(messageReceiver, "pay.error.invalid_name");
    }

    public void sendPaySelfPayment(Player messageReceiver) {
        pluginLogger.send(messageReceiver, "pay.error.self_payment");
    }

    public void sendPayInvalidAmount(Player messageReceiver) {
        pluginLogger.send(messageReceiver, "pay.error.invalid_amount");
    }

    public void sendPayInsufficientFunds(Player messageReceiver) {
        pluginLogger.send(messageReceiver, "pay.error.insufficient_funds");
    }

    public void sendPayException(Player messageReceiver) {
        pluginLogger.send(messageReceiver, "pay.error.exception");
    }

    public void sendPayNoPermission(Player messageReceiver) {
        pluginLogger.send(messageReceiver, "pay.error.no_permission");
    }
}