package io.github.HenriqueMichelini.craftalism.economy.infra.config;

import java.io.File;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

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
        connectionFile = new File(
            plugin.getDataFolder(),
            "connection-config.yml"
        );

        if (!connectionFile.exists()) {
            plugin.saveResource("connection-config.yml", false);
        }
    }

    public void loadConnectionConfig() {
        connectionFile = new File(
            plugin.getDataFolder(),
            "connection-config.yml"
        );
        connectionConfig = YamlConfiguration.loadConfiguration(connectionFile);
    }

    public String getUrl() {
        return connectionConfig.getString("url", "");
    }

    public String getAuthServerUrl() {
        return connectionConfig.getString(
            "auth-server-url",
            "http://localhost:9000"
        );
    }

    public String getClientId() {
        return connectionConfig.getString("client-id", "minecraft-server");
    }

    public String getClientSecret() {
        return connectionConfig.getString("client-secret", "");
    }
}
