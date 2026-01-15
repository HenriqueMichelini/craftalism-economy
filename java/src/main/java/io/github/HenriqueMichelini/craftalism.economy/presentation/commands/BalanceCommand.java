package io.github.HenriqueMichelini.craftalism.economy.presentation.commands;

import io.github.HenriqueMichelini.craftalism.economy.application.dto.BalanceExecutionResult;
import io.github.HenriqueMichelini.craftalism.economy.application.service.BalanceCommandApplicationService;
import io.github.HenriqueMichelini.craftalism.economy.domain.service.currency.CurrencyFormatter;
import io.github.HenriqueMichelini.craftalism.economy.domain.service.logs.messages.BalanceMessages;
import io.github.HenriqueMichelini.craftalism.economy.presentation.validation.PlayerNameCheck;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BalanceCommand implements CommandExecutor {
    private static final String PERMISSION_SELF = "craftalism.balance.self";
    private static final String PERMISSION_OTHER = "craftalism.balance.other";

    private final BalanceMessages messages;
    private final PlayerNameCheck playerNameCheck;
    private final BalanceCommandApplicationService balanceService;
    private final CurrencyFormatter formatter;

    public BalanceCommand(
            BalanceMessages messages,
            PlayerNameCheck playerNameCheck,
            BalanceCommandApplicationService balanceService,
            CurrencyFormatter formatter
    ) {
        this.messages = messages;
        this.playerNameCheck = playerNameCheck;
        this.balanceService = balanceService;
        this.formatter = formatter;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {

        if (args.length == 0) {
            return handleSelfBalance(sender);
        }

        return handleOtherBalance(sender, args[0]);
    }

    private boolean handleSelfBalance(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            messages.sendBalancePlayerOnly();
            return true;
        }

        if (!player.hasPermission(PERMISSION_SELF)) {
            messages.sendBalanceNoPermission(player);
            return true;
        }

        balanceService.executeSelf(player.getUniqueId())
                .thenAccept(result -> handleResult(player, result, player.getName()));

        return true;
    }

    private boolean handleOtherBalance(CommandSender sender, String targetName) {
        if (!(sender instanceof Player player)) {
            messages.sendBalancePlayerOnly();
            return true;
        }

        if (!player.hasPermission(PERMISSION_OTHER)) {
            messages.sendBalanceNoPermission(player);
            return true;
        }

        if (!playerNameCheck.isValid(targetName)) {
            messages.sendBalanceInvalidName(player);
            return true;
        }

        balanceService.executeOther(targetName)
                .thenAccept(result -> handleResult(player, result, targetName));

        return true;
    }

    private void handleResult(Player sender, BalanceExecutionResult result, String targetName) {
        switch (result.status()) {
            case SUCCESS_SELF ->
                    messages.sendBalanceSelfSuccess(sender, formatter.formatCurrency(result.amount()));

            case SUCCESS_OTHER ->
                    messages.sendBalanceOtherSuccess(sender, targetName, formatter.formatCurrency(result.amount()));

            case NO_BALANCE ->
                    messages.sendBalanceOtherNoBalance(sender, targetName);

            case NOT_FOUND ->
                    messages.sendBalanceOtherNotFound(sender, targetName);

            default ->
                    messages.sendBalanceError(sender);
        }
    }
}