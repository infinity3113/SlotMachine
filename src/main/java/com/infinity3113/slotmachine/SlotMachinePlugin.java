package com.infinity3113.slotmachine;

import com.infinity3113.slotmachine.command.SlotCommand;
import com.infinity3113.slotmachine.economy.EconomyManager;
import com.infinity3113.slotmachine.gui.GuiListener;
import com.infinity3113.slotmachine.listener.MachineCreationListener;
import com.infinity3113.slotmachine.listener.MachineUseListener;
import com.infinity3113.slotmachine.listener.SignListener;
import com.infinity3113.slotmachine.machine.SlotMachineManager;
import com.infinity3113.slotmachine.util.ConfigUpdater;
import com.infinity3113.slotmachine.util.LangManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class SlotMachinePlugin extends JavaPlugin {

    private static SlotMachinePlugin instance;
    private EconomyManager economyManager;
    private SlotMachineManager slotMachineManager;
    private LangManager langManager;

    @Override
    public void onEnable() {
        instance = this;
        // Llama al actualizador para el config.yml
        ConfigUpdater.updateConfig(this, "config.yml");
        // reloadConfig() carga la configuración actualizada en memoria
        reloadConfig();

        this.langManager = new LangManager(this);

        this.economyManager = new EconomyManager(this);
        if (!economyManager.setupEconomy()) {
            getLogger().severe("Vault not found or no economy plugin hooked. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (Bukkit.getPluginManager().getPlugin("DecentHolograms") == null) {
            getLogger().severe("DecentHolograms not found! This plugin is required. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.slotMachineManager = new SlotMachineManager(this);
        this.slotMachineManager.loadMachines();

        Objects.requireNonNull(getCommand("slot")).setExecutor(new SlotCommand(this));
        getServer().getPluginManager().registerEvents(new MachineUseListener(this), this);
        getServer().getPluginManager().registerEvents(new MachineCreationListener(this), this);
        getServer().getPluginManager().registerEvents(new SignListener(this), this);
        getServer().getPluginManager().registerEvents(new GuiListener(this), this);

        getLogger().info("SlotMachine plugin enabled successfully.");
    }

    @Override
    public void onDisable() {
        if (slotMachineManager != null) {
            slotMachineManager.stopAllMachines();
            slotMachineManager.removeAllHolograms();
        }
        getLogger().info("SlotMachine plugin disabled.");
    }

    public void reloadPluginConfig() {
        reloadConfig();
        ConfigUpdater.updateConfig(this, "config.yml");
        this.langManager = new LangManager(this); // Esto recargará los archivos de idioma actualizados
        slotMachineManager.loadMachines();
    }

    public static SlotMachinePlugin getInstance() {
        return instance;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public SlotMachineManager getSlotMachineManager() {
        return slotMachineManager;
    }
    
    public LangManager getLangManager() {
        return langManager;
    }
}