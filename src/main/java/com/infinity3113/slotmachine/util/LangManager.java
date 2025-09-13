package com.infinity3113.slotmachine.util;

import com.infinity3113.slotmachine.SlotMachinePlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class LangManager {

    private final SlotMachinePlugin plugin;
    private FileConfiguration langConfig;

    public LangManager(SlotMachinePlugin plugin) {
        this.plugin = plugin;
        loadLanguageFile();
    }

    private void loadLanguageFile() {
        String lang = plugin.getConfig().getString("language", "en");
        File langFile = new File(plugin.getDataFolder(), "lang/" + lang + ".yml");

        if (!langFile.exists()) {
            plugin.getLogger().warning("Language file '" + lang + ".yml' not found. Creating defaults...");
            File langDir = new File(plugin.getDataFolder(), "lang");
            if (!langDir.exists()) {
                langDir.mkdirs();
            }
            plugin.saveResource("lang/en.yml", false);
            plugin.saveResource("lang/es.yml", false);
            
            if (!langFile.exists()) {
                 plugin.getLogger().severe("Could not create language file. Using internal en.yml.");
                 try (Reader defConfigStream = new InputStreamReader(plugin.getResource("lang/en.yml"), StandardCharsets.UTF_8)) {
                    this.langConfig = YamlConfiguration.loadConfiguration(defConfigStream);
                } catch (Exception e) {
                     e.printStackTrace();
                }
                return;
            }
        }
        this.langConfig = YamlConfiguration.loadConfiguration(langFile);
    }

    public String getString(String path) {
        String message = langConfig.getString(path, "&cMissing message: " + path);
        return MessageUtil.colorize(message);
    }

    public List<String> getStringList(String path) {
        return langConfig.getStringList(path).stream()
                .map(MessageUtil::colorize)
                .collect(Collectors.toList());
    }

    public String getFormattedString(String path, String... replacements) {
        String message = getString(path);
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace("{" + replacements[i] + "}", replacements[i + 1]);
            }
        }
        return message;
    }
}