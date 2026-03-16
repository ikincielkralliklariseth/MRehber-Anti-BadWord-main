package xyz.mrehber.manager;

import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import xyz.mrehber.MRehberPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

@RequiredArgsConstructor
public class LanguageManager {

    private final MRehberPlugin plugin;
    private FileConfiguration messages;

    public void load() {
        File folder = plugin.getDataFolder();
        if (!folder.exists()) folder.mkdirs();

        File messageFile = new File(folder, "messages.yml");
        if (!messageFile.exists()) {
            try (InputStream in = plugin.getResource("messages.yml")) {
                if (in != null) {
                    Files.copy(in, messageFile.toPath());
                }
            } catch (IOException e) {
                plugin.getLogger().severe("messages.yml could not be created: " + e.getMessage());
            }
        }
        this.messages = YamlConfiguration.loadConfiguration(messageFile);
    }

    public String getMessage(String path, String... placeholders) {
        String raw = messages.getString("mesajlar." + path, "&cMessage not found: " + path);
        String prefix = messages.getString("mesajlar.prefix", "");
        String message = ChatColor.translateAlternateColorCodes('&', prefix + raw);

        if (placeholders.length % 2 == 0) {
            for (int i = 0; i < placeholders.length; i += 2) {
                message = message.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
            }
        }
        return message;
    }

    public String getRawMessage(String path, String... placeholders) {
        String raw = messages.getString("mesajlar." + path, "&cMessage not found: " + path);
        String message = ChatColor.translateAlternateColorCodes('&', raw);
        if (placeholders.length % 2 == 0) {
            for (int i = 0; i < placeholders.length; i += 2) {
                message = message.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
            }
        }
        return message;
    }
}
