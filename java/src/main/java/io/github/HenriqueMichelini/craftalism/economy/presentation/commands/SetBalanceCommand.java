package io.github.HenriqueMichelini.craftalism.economy.presentation.commands;

import io.github.HenriqueMichelini.craftalism.economy.application.service.SetBalanceCommandApplicationService;
import io.github.HenriqueMichelini.craftalism.economy.domain.service.logs.messages.SetBalanceMessages;
import io.github.HenriqueMichelini.craftalism.economy.presentation.validation.PlayerNameCheck;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import io.github.HenriqueMichelini.craftalism.economy.application.dto.SetBalanceExecutionResult;

public class SetBalanceCommand implements CommandExecutor {
    private static final String PERMISSION = "craftalism.setbalance";

    private final PlayerNameCheck playerNameCheck;
    private final SetBalanceMessages messages;
    private final SetBalanceCommandApplicationService service;
    private final JavaPlugin plugin;

    public SetBalanceCommand(
            PlayerNameCheck playerNameCheck,
            SetBalanceMessages messages,
            SetBalanceCommandApplicationService service,
            JavaPlugin plugin
    ) {
        this.playerNameCheck = playerNameCheck;
        this.messages = messages;
        this.service = service;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (!sender.hasPermission(PERMISSION)) {
            messages.sendSetBalanceNoPermission(sender);
            return true;
        }

        if (args.length != 2) {
            messages.sendSetBalanceUsage(sender);
            return true;
        }

        String senderName = getSenderName(sender);
        String targetName = args[0];

        if (!playerNameCheck.isValid(targetName)) {
            messages.sendSetBalanceInvalidName(sender);
            return true;
        }

        Long amount;
        try {
            amount = Long.parseLong(args[1]);
        } catch (NumberFormatException e) {
            messages.sendSetBalanceInvalidAmount(sender);
            return true;
        }

        if (amount < 0) {
            messages.sendSetBalanceInvalidAmount(sender);
            return true;
        }

        service.execute(targetName, amount)
                .thenAccept(result ->
                        Bukkit.getScheduler().runTask(plugin, () ->
                                handleResult(sender, senderName, targetName, amount, result)
                        )
                )
                .exceptionally(ex -> {
                    plugin.getLogger().severe("Unexpected error in setbalance command: " + ex.getMessage());
                    Bukkit.getScheduler().runTask(plugin, () ->
                            messages.sendSetBalanceException(sender)
                    );
                    return null;
                });

        return true;
    }

    private void handleResult(
            CommandSender sender,
            String senderName,
            String targetName,
            Long amount,
            SetBalanceExecutionResult result
    ) {
        switch (result.status()) {
            case SUCCESS -> {
                if (result.uuid().isPresent()) {
                    Player target = Bukkit.getPlayer(result.uuid().get());
                    if (target != null && target.isOnline()) {
                        messages.sendSetBalanceSuccessReceiver(target, String.valueOf(amount), senderName);
                    }
                }
                messages.sendSetBalanceSuccessSender(sender, targetName, String.valueOf(amount));
            }
            case INVALID_AMOUNT -> messages.sendSetBalanceInvalidAmount(sender);
            case PLAYER_NOT_FOUND -> messages.sendSetBalancePlayerNotFound(sender);
            case UPDATE_FAILED -> messages.sendSetBalanceUpdateFailed(sender);
            default -> messages.sendSetBalanceException(sender);
        }
    }

    private String getSenderName(CommandSender sender) {
        return (sender instanceof Player player) ? player.getName() : "Console";
    }
}