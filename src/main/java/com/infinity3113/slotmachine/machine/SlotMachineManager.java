package com.infinity3113.slotmachine.machine;

import com.infinity3113.slotmachine.SlotMachinePlugin;
import com.infinity3113.slotmachine.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class SlotMachineManager {

    private final SlotMachinePlugin plugin;
    private final Map<String, SlotMachine> machinesByName = new HashMap<>();
    private final Map<UUID, Player> creationMode = new HashMap<>();
    private final Map<UUID, CreationProgress> creationProgress = new HashMap<>();

    private File machinesFile;
    private FileConfiguration machinesConfig;

    public SlotMachineManager(SlotMachinePlugin plugin) {
        this.plugin = plugin;
        setupFiles();
    }

    private void setupFiles() {
        machinesFile = new File(plugin.getDataFolder(), "machines.yml");
        if (!machinesFile.exists()) {
            try {
                machinesFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("No se pudo crear machines.yml: " + e.getMessage());
            }
        }
        machinesConfig = YamlConfiguration.loadConfiguration(machinesFile);
    }

    public void loadMachines() {
        machinesByName.values().forEach(SlotMachine::removeHologram);
        machinesByName.clear();
        ConfigurationSection machinesSection = machinesConfig.getConfigurationSection("machines");
        if (machinesSection == null) return;

        for (String key : machinesSection.getKeys(false)) {
            ConfigurationSection section = machinesSection.getConfigurationSection(key);
            if (section != null) {
                try {
                    SlotMachine machine = new SlotMachine(plugin, section);
                    if (machine.isValid()) {
                        machinesByName.put(machine.getName().toLowerCase(), machine);
                    } else {
                        plugin.getLogger().warning("No se pudo cargar la maquina '" + key + "' porque sus componentes no son validos en el mundo.");
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("Error al cargar la maquina '" + key + "': " + e.getMessage());
                }
            }
        }
        plugin.getLogger().info(machinesByName.size() + " maquinas tragamonedas cargadas.");
    }

    public void saveMachines() {
        machinesConfig.set("machines", null);
        ConfigurationSection machinesSection = machinesConfig.createSection("machines");

        for (SlotMachine machine : machinesByName.values()) {
            ConfigurationSection machineSection = machinesSection.createSection(machine.getName());
            machine.save(machineSection);
        }
        try {
            machinesConfig.save(machinesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("No se pudo guardar machines.yml: " + e.getMessage());
        }
    }

    public void addMachine(SlotMachine machine) {
        machinesByName.put(machine.getName().toLowerCase(), machine);
        saveMachines();
    }

    public void deleteMachine(String name) {
        SlotMachine machine = getMachineByName(name);
        if (machine != null) {
            machine.cleanup();
            machinesByName.remove(name.toLowerCase());
            machinesConfig.set("machines." + machine.getName(), null);
            saveMachines();
        }
    }
    
    public SlotMachine getMachineByJukeboxLocation(Location location) {
        return machinesByName.values().stream()
                .filter(m -> m.getJukeboxLocation().equals(location))
                .findFirst().orElse(null);
    }

    public SlotMachine getMachineByName(String name) {
        return machinesByName.get(name.toLowerCase());
    }

    public void stopAllMachines() {
        for (SlotMachine machine : machinesByName.values()) {
            if (machine.isSpinning()) {
                machine.forceStop();
            }
        }
    }
    
    public void startCreation(Player player, String name) {
        if (getMachineByName(name) != null) {
            MessageUtil.sendMessage(player, "&cYa existe una maquina con ese nombre.");
            return;
        }
        creationMode.put(player.getUniqueId(), player);
        creationProgress.put(player.getUniqueId(), new CreationProgress(name));
    }

    public void cancelCreation(Player player) {
        creationMode.remove(player.getUniqueId());
        creationProgress.remove(player.getUniqueId());
    }

    public boolean isInCreationMode(Player player) {
        return creationMode.containsKey(player.getUniqueId());
    }

    public CreationProgress getCreationProgress(Player player) {
        return creationProgress.get(player.getUniqueId());
    }
    
    public static class CreationProgress {
        private final String name;
        private Location jukeboxLocation;
        private final List<UUID> frameUuids = new ArrayList<>();

        public CreationProgress(String name) { this.name = name; }
        public String getName() { return name; }
        public Location getJukeboxLocation() { return jukeboxLocation; }
        public void setJukeboxLocation(Location jukeboxLocation) { this.jukeboxLocation = jukeboxLocation; }
        public List<UUID> getFrameUuids() { return frameUuids; }
        public void addFrameUuid(UUID uuid) { this.frameUuids.add(uuid); }
        public boolean isReady() { return jukeboxLocation != null && frameUuids.size() == 3; }
    }
}