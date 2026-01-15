package io.github.HenriqueMichelini.craftalism.economy.presentation.commands;

import io.github.HenriqueMichelini.craftalism.economy.application.service.BaltopCommandApplicationService;
import io.github.HenriqueMichelini.craftalism.economy.domain.service.currency.CurrencyFormatter;
import io.github.HenriqueMichelini.craftalism.economy.domain.service.logs.messages.BaltopMessages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BaltopCommand implements CommandExecutor {
    private static final String PERMISSION = "craftalism.baltop";

    private final BaltopMessages messages;
    private final BaltopCommandApplicationService service;
    private final CurrencyFormatter formatter;

    public BaltopCommand(BaltopMessages messages, BaltopCommandApplicationService service, CurrencyFormatter formatter) {
        this.messages = messages;
        this.service = service;
        this.formatter = formatter;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            messages.sendBaltopPlayerOnly();
            return true;
        }

        if (!player.hasPermission(PERMISSION)) {
            messages.sendBaltopNoPermission(player);
            return true;
        }

        if (args.length > 0) {
            messages.sendBaltopUsage(player);
            return true;
        }

        messages.sendBaltopLoading(player);

        service.getTop10().thenAccept(entries -> {
            displayBaltop(player, entries);
        }).exceptionally(ex -> {
            messages.sendBaltopError(player);
            return null;
        });

        return true;
    }

    private void displayBaltop(Player player, List<BaltopCommandApplicationService.BaltopEntry> entries) {
        messages.sendBaltopHeader(player, String.valueOf(entries.size()));

        int position = 1;
        for (BaltopCommandApplicationService.BaltopEntry entry : entries) {
            System.out.println("API raw = " + entry.getBalance());
            String formattedBalance = formatter.formatCurrency(entry.getBalance());
            messages.sendBaltopEntry(player, String.valueOf(position), entry.getPlayerName(), formattedBalance);
            position++;
        }
    }
}