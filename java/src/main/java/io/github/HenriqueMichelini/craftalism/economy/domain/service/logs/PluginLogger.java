package io.github.HenriqueMichelini.craftalism.economy.domain.service.logs;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class PluginLogger {
    private final LogManager logManager;
    private final JavaPlugin plugin;

    public PluginLogger(JavaPlugin plugin, LogManager logManager) {
        this.plugin = plugin;
        this.logManager = logManager;
    }

    public void info(String path, LogManager.Placeholder... placeholders) {
        plugin.getLogger().info(logManager.getMessage(path, placeholders));
    }

    public void warn(String path, LogManager.Placeholder... placeholders) {
        plugin.getLogger().warning(logManager.getMessage(path, placeholders));
    }

    public void send(Player player, String path, LogManager.Placeholder... placeholders) {
        String raw = logManager.getMessage(path, placeholders);
        Component message = LegacyComponentSerializer.legacyAmpersand().deserialize(raw);
        player.sendMessage(message);
    }

    public void send(CommandSender sender, String path, LogManager.Placeholder... placeholders) {
        String raw = logManager.getMessage(path, placeholders);
        Component message = LegacyComponentSerializer.legacyAmpersand().deserialize(raw);
        sender.sendMessage(message);
    }
}