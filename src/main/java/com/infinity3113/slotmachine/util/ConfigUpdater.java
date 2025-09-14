package com.infinity3113.slotmachine.util;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ConfigUpdater {

    public static void updateConfig(JavaPlugin plugin, String fileName) {
        File configFile = new File(plugin.getDataFolder(), fileName);
        if (!configFile.exists()) {
            plugin.saveResource(fileName, false);
            return;
        }

        FileConfiguration userConfig = YamlConfiguration.loadConfiguration(configFile);
        
        try (InputStream defaultConfigStream = plugin.getResource(fileName)) {
            if (defaultConfigStream != null) {
                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultConfigStream, StandardCharsets.UTF_8));
                
                for (String key : defaultConfig.getKeys(true)) {
                    if (!userConfig.contains(key)) {
                        userConfig.set(key, defaultConfig.get(key));
                    }
                }
                userConfig.save(configFile);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Could not update configuration file: " + fileName);
            e.printStackTrace();
        }
    }
}