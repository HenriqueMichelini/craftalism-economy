package io.github.HenriqueMichelini.craftalism.economy.domain.service.logs.messages;

import io.github.HenriqueMichelini.craftalism.economy.domain.service.logs.PluginLogger;
import org.bukkit.entity.Player;

public class CurrencyMessages {
    private final PluginLogger pluginLogger;

    public CurrencyMessages(PluginLogger pluginLogger) {
        this.pluginLogger = pluginLogger;
    }

    public void sendAmountEmpty(Player messageReceiver) {
        pluginLogger.send(messageReceiver, "currency.error.empty");
    }

    public void sendInvalidFormat(Player messageReceiver) {
        pluginLogger.send(messageReceiver, "currency.error.invalid_format");
    }

    public void sendInvalidAmount(Player messageReceiver) {
        pluginLogger.send(messageReceiver, "currency.error.invalid_amount");
    }

    public void sendNonPositive(Player messageReceiver) {
        pluginLogger.send(messageReceiver, "currency.error.non_positive");
    }

    public void sendTooLarge(Player messageReceiver) {
        pluginLogger.send(messageReceiver, "currency.error.too_large");
    }
}