package io.github.HenriqueMichelini.craftalism.economy.domain.service.logs;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class LogManager {

    private final JavaPlugin plugin;
    private FileConfiguration messagesConfig;
    private File messagesFile;

    private final Map<String, String> templateCache = new HashMap<>();
    private String prefix = "";

    public LogManager(JavaPlugin plugin) {
        this.plugin = plugin;
        createLogsFileIfNotExists();
        loadLogs();
    }

    private void createLogsFileIfNotExists() {
        messagesFile = new File(plugin.getDataFolder(), "logs.yml");

        if (!messagesFile.exists()) {
            plugin.saveResource("logs.yml", false);
        }
    }

    public void loadLogs() {
        messagesFile = new File(plugin.getDataFolder(), "logs.yml");
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        prefix = messagesConfig.getString("prefix", "");
        cacheAllTemplates();
    }

    private void cacheAllTemplates() {
        ConfigurationSection root = messagesConfig.getRoot();
        if (root == null) {
            plugin.getLogger().warning("logs.yml está vazio ou inválido!");
            return;
        }
        cacheSection(root, "");
    }

    private void cacheSection(ConfigurationSection section, String parentPath) {
        for (String key : section.getKeys(false)) {
            String fullPath = parentPath.isEmpty() ? key : parentPath + "." + key;
            Object value = section.get(key);

            if (value instanceof ConfigurationSection subSection) {
                cacheSection(subSection, fullPath);
            } else if (value instanceof String template) {
                templateCache.put(fullPath, template);
            }
        }
    }

    public String getMessage(String path, Placeholder... placeholders) {
        String template = templateCache.getOrDefault(path, "Message not found: " + path);

        if (placeholders == null || placeholders.length == 0) {
            return prefix + template;
        }

        StringBuilder result = new StringBuilder(template);
        for (Placeholder p : placeholders) {
            String placeholder = "{" + p.key() + "}";
            int index = result.indexOf(placeholder);
            while (index != -1) {
                result.replace(index, index + placeholder.length(), p.value());
                index = result.indexOf(placeholder, index + p.value().length());
            }
        }

        return prefix + result;
    }

    public record Placeholder(String key, String value) {}
}
