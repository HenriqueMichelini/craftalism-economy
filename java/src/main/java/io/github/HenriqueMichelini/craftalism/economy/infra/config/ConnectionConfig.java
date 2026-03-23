package io.github.HenriqueMichelini.craftalism.economy.infra.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class ConnectionConfig {

    private final JavaPlugin plugin;
    private FileConfiguration connectionConfig;
    private File connectionFile;

    public ConnectionConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        createConnectionFileIfNotExists();
        loadConnectionConfig();
    }

    private void createConnectionFileIfNotExists() {
        connectionFile = new File(plugin.getDataFolder(), "connection-config.yml");

        if (!connectionFile.exists()) {
            plugin.saveResource("connection-config.yml", false);
        }
    }

    public void loadConnectionConfig() {
        connectionFile = new File(plugin.getDataFolder(), "connection-config.yml");
        connectionConfig = YamlConfiguration.loadConfiguration(connectionFile);
    }

    public String getUrl() {
        return connectionConfig.getString("url", "");
    }

}