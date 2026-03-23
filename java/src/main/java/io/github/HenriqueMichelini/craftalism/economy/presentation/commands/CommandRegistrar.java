package io.github.HenriqueMichelini.craftalism.economy.presentation.commands;

import io.github.HenriqueMichelini.craftalism.economy.CraftalismEconomy;
import io.github.HenriqueMichelini.craftalism.economy.application.service.ApplicationServiceFactory;
import io.github.HenriqueMichelini.craftalism.economy.domain.service.currency.FormatterFactory;
import io.github.HenriqueMichelini.craftalism.economy.domain.service.logs.messages.BalanceMessages;
import io.github.HenriqueMichelini.craftalism.economy.domain.service.logs.messages.BaltopMessages;
import io.github.HenriqueMichelini.craftalism.economy.domain.service.logs.messages.PayMessages;
import io.github.HenriqueMichelini.craftalism.economy.domain.service.logs.messages.SetBalanceMessages;
import io.github.HenriqueMichelini.craftalism.economy.presentation.validation.PlayerNameCheck;
import org.bukkit.command.CommandExecutor;

import java.util.Objects;

public final class CommandRegistrar {
    private final CraftalismEconomy plugin;
    private final ApplicationServiceFactory apps;
    private final FormatterFactory formatters;

    private final PlayerNameCheck playerNameCheck = new PlayerNameCheck();

    public CommandRegistrar(CraftalismEconomy plugin, ApplicationServiceFactory apps, FormatterFactory formatters) {
        this.plugin = plugin;
        this.apps = apps;
        this.formatters = formatsOrThrow(formatters);
    }

    private static FormatterFactory formatsOrThrow(FormatterFactory f) {
        return Objects.requireNonNull(f, "formatters");
    }

    public void registerAll() {
        register("pay", new PayCommand(
                new PayMessages(
                        plugin.getPluginLogger()),
                        apps.getPayCommandApplication(),
                        apps.getTransactionApplication(),
                        playerNameCheck,
                        formatters.getFormatter()
        ));
        register("balance", new BalanceCommand(
                new BalanceMessages(
                        plugin.getPluginLogger()),
                        playerNameCheck,
                        apps.getBalanceCommandApplication(),
                        formatters.getFormatter()
        ));
        register("baltop", new BaltopCommand(
                new BaltopMessages(
                        plugin.getPluginLogger()),
                        apps.getBaltopCommandApplication(),
                        formatters.getFormatter()
        ));
        register("setbalance", new SetBalanceCommand(
                playerNameCheck,
                new SetBalanceMessages(
                        plugin.getPluginLogger()),
                        apps.setBalanceCommandApplication(),
                        plugin
        ));
    }

    private void register(String name, CommandExecutor executor) {
        var cmd = plugin.getCommand(name);
        if (cmd == null) {
            plugin.getLogger().severe("Command not registered in plugin.yml: /" + name);
            return;
        }
        cmd.setExecutor(executor);
        plugin.getLogger().fine("Registered command: /" + name);
    }
}