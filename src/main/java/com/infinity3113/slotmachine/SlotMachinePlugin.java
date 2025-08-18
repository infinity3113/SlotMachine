package com.infinity3113.slotmachine;

import com.infinity3113.slotmachine.command.SlotCommand;
import com.infinity3113.slotmachine.economy.EconomyManager;
import com.infinity3113.slotmachine.gui.GuiListener;
import com.infinity3113.slotmachine.listener.MachineCreationListener;
import com.infinity3113.slotmachine.listener.MachineUseListener;
import com.infinity3113.slotmachine.listener.SignListener;
import com.infinity3113.slotmachine.machine.SlotMachineManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class SlotMachinePlugin extends JavaPlugin {

    private static SlotMachinePlugin instance;
    private EconomyManager economyManager;
    private SlotMachineManager slotMachineManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.economyManager = new EconomyManager(this);
        if (!economyManager.setupEconomy()) {
            getLogger().severe("Vault no encontrado o no hay un plugin de economia. Desactivando plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.slotMachineManager = new SlotMachineManager(this);
        this.slotMachineManager.loadMachines();

        Objects.requireNonNull(getCommand("slot")).setExecutor(new SlotCommand(this));
        getServer().getPluginManager().registerEvents(new MachineUseListener(this), this);
        getServer().getPluginManager().registerEvents(new MachineCreationListener(this), this);
        getServer().getPluginManager().registerEvents(new SignListener(this), this);
        getServer().getPluginManager().registerEvents(new GuiListener(this), this); // Listener para el nuevo GUI

        getLogger().info("Plugin SlotMachine v3 activado exitosamente.");
    }

    @Override
    public void onDisable() {
        if (slotMachineManager != null) {
            slotMachineManager.stopAllMachines();
        }
        getLogger().info("Plugin SlotMachine desactivado.");
    }

    public void reloadPluginConfig() {
        reloadConfig();
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
}