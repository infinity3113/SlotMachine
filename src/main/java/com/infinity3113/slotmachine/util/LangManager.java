package com.infinity3113.slotmachine.util;

import com.infinity3113.slotmachine.SlotMachinePlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
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
        // Asegurarse de que los archivos de idioma por defecto existan si no están
        plugin.saveResource("lang/en.yml", false);
        plugin.saveResource("lang/es.yml", false);

        String lang = plugin.getConfig().getString("language", "en");
        String langFileName = "lang/" + lang + ".yml";

        // Usar el actualizador para el archivo de idioma seleccionado
        ConfigUpdater.updateConfig(plugin, langFileName);
        
        File langFile = new File(plugin.getDataFolder(), langFileName);
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