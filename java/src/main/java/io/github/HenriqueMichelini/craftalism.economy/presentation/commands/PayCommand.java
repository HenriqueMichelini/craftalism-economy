package io.github.HenriqueMichelini.craftalism.economy.presentation.commands;

import io.github.HenriqueMichelini.craftalism.economy.application.service.PayCommandApplicationService;
import io.github.HenriqueMichelini.craftalism.economy.application.service.TransactionApplicationService;
import io.github.HenriqueMichelini.craftalism.economy.domain.service.currency.CurrencyFormatter;
import io.github.HenriqueMichelini.craftalism.economy.domain.service.logs.messages.PayMessages;
import io.github.HenriqueMichelini.craftalism.economy.presentation.validation.PlayerNameCheck;
import java.math.BigDecimal;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PayCommand implements CommandExecutor {

    private static final String PERMISSION = "craftalism.pay";

    private final PayMessages messages;
    private final PayCommandApplicationService payService;
    private final PlayerNameCheck playerNameCheck;
    private final CurrencyFormatter formatter;

    public PayCommand(
        PayMessages messages,
        PayCommandApplicationService payService,
        TransactionApplicationService transactionService,
        PlayerNameCheck playerNameCheck,
        CurrencyFormatter formatter
    ) {
        this.messages = messages;
        this.payService = payService;
        this.playerNameCheck = playerNameCheck;
        this.formatter = formatter;
    }

    @Override
    public boolean onCommand(
        @NotNull CommandSender sender,
        @NotNull Command command,
        @NotNull String label,
        @NotNull String[] args
    ) {
        if (!(sender instanceof Player player)) {
            messages.sendPayPlayerOnly();
            return true;
        }

        if (!player.hasPermission(PERMISSION)) {
            messages.sendPayNoPermission(player);
            return true;
        }

        if (args.length < 2) {
            messages.sendPayUsage(player);
            return true;
        }

        String targetName = args[0];

        if (!playerNameCheck.isValid(targetName)) {
            messages.sendPayInvalidName(player);
            return true;
        }

        String amountStr = args[1];

        if (amountStr.isEmpty()) {
            messages.sendPayAmountEmpty(player);
            return true;
        }

        if (!amountStr.matches("\\d+(\\.\\d{1,2})?")) {
            messages.sendPayInvalidAmount(player);
            return true;
        }

        long amount;
        try {
            BigDecimal displayAmount = new BigDecimal(amountStr);
            amount = formatter.fromDisplayValue(displayAmount);
        } catch (NumberFormatException e) {
            messages.sendPayInvalidAmount(player);
            return true;
        }

        if (targetName.equalsIgnoreCase(player.getName())) {
            messages.sendPaySelfPayment(player);
            return true;
        }

        payService
            .execute(player.getUniqueId(), player.getName(), targetName, amount)
            .thenAccept(result -> {
                switch (result.getStatus()) {
                    case SUCCESS -> {
                        messages.sendPaySuccessSender(
                            player,
                            formatter.formatCurrency(amount),
                            targetName
                        );

                        Player targetPlayer = Bukkit.getPlayer(targetName);
                        if (targetPlayer != null && targetPlayer.isOnline()) {
                            messages.sendPaySuccessReceiver(
                                targetPlayer,
                                formatter.formatCurrency(amount),
                                player.getName()
                            );
                        }
                    }
                    case TARGET_NOT_FOUND -> messages.sendPayPlayerNotFound(
                        player
                    );
                    case NOT_ENOUGH_FUNDS -> messages.sendPayInsufficientFunds(
                        player
                    );
                    case INVALID_AMOUNT -> messages.sendPayInvalidAmount(
                        player
                    );
                    case CANNOT_PAY_SELF -> messages.sendPaySelfPayment(player);
                    default -> messages.sendPayException(player);
                }
            });

        return true;
    }
}
