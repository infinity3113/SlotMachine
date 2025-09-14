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
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
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
        File backupFile = new File(plugin.getDataFolder(), "machines.yml.bak");

        // Si el archivo principal no existe pero el backup sí, restaura el backup.
        // Esto recupera los datos en caso de un crash durante el guardado.
        if (!machinesFile.exists() && backupFile.exists()) {
            plugin.getLogger().warning("Main machines file not found. Restoring from backup...");
            try {
                Files.copy(backupFile.toPath(), machinesFile.toPath());
            } catch (IOException e) {
                plugin.getLogger().severe("Could not restore backup file: " + e.getMessage());
            }
        }
        
        // Cargar la configuración desde el archivo (o crearlo si no existe)
        if (!machinesFile.exists()) {
            try {
                machinesFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create machines.yml: " + e.getMessage());
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
                        plugin.getLogger().warning("Could not load machine '" + key + "' because its components are not valid in the world.");
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("Error loading machine '" + key + "': " + e.getMessage());
                }
            }
        }
        plugin.getLogger().info(machinesByName.size() + " slot machines loaded.");
    }

    public void saveMachines() {
        // Crear una nueva configuración en memoria para evitar modificar la actual mientras se guarda
        FileConfiguration newConfig = new YamlConfiguration();
        ConfigurationSection machinesSection = newConfig.createSection("machines");

        for (SlotMachine machine : machinesByName.values()) {
            ConfigurationSection machineSection = machinesSection.createSection(machine.getName());
            machine.save(machineSection);
        }

        // Implementación del guardado seguro (atomic save)
        File tempFile = new File(plugin.getDataFolder(), "machines.yml.tmp");
        File backupFile = new File(plugin.getDataFolder(), "machines.yml.bak");

        try {
            // 1. Escribir los nuevos datos en un archivo temporal.
            newConfig.save(tempFile);

            // 2. Eliminar el backup antiguo si existe.
            if (backupFile.exists()) {
                backupFile.delete();
            }

            // 3. Renombrar el archivo actual a .bak (creando un backup).
            if (machinesFile.exists()) {
                Files.move(machinesFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            // 4. Renombrar el archivo temporal al nombre final.
            Files.move(tempFile.toPath(), machinesFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        } catch (IOException e) {
            plugin.getLogger().severe("CRITICAL: Could not save machines.yml safely. Changes may be lost.");
            e.printStackTrace();

            // En caso de error, intentar limpiar los archivos temporales
            if (tempFile.exists()) {
                tempFile.delete();
            }
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
            saveMachines(); // Guardar los cambios después de eliminar
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
    
    public void removeAllHolograms() {
        machinesByName.values().forEach(SlotMachine::removeHologram);
    }
    
    public void startCreation(Player player, String name) {
        if (getMachineByName(name) != null) {
            MessageUtil.sendMessage(player, plugin.getLangManager().getString("messages.machine_already_exists"));
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