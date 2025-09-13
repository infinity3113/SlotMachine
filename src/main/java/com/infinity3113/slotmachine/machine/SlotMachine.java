package com.infinity3113.slotmachine.machine;

import com.infinity3113.slotmachine.SlotMachinePlugin;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class SlotMachine {

    private final SlotMachinePlugin plugin;
    private final String name;
    private final Location jukeboxLocation;
    private final UUID[] frameUuids;
    private final ItemFrame[] itemFrames;

    private boolean isSpinning = false;
    private BukkitTask spinTask;
    
    private double currentJackpot;
    private final String hologramName;

    public SlotMachine(SlotMachinePlugin plugin, ConfigurationSection section) {
        this.plugin = plugin;
        this.name = section.getName();
        this.jukeboxLocation = section.getLocation("jukeboxLocation");
        List<String> uuidStrings = section.getStringList("frameUuids");
        this.frameUuids = uuidStrings.stream().map(UUID::fromString).toArray(UUID[]::new);
        this.itemFrames = new ItemFrame[3];
        this.currentJackpot = section.getDouble("currentJackpot", plugin.getConfig().getDouble("jackpot.starting_amount", 500.0));
        this.hologramName = "slm-" + this.name.toLowerCase().replace(" ", "_");
        
        fetchFrameEntities();
        createOrUpdateHologram();
    }
    
    public SlotMachine(SlotMachinePlugin plugin, String name, Location jukeboxLocation, List<UUID> frameUuids) {
        this.plugin = plugin;
        this.name = name;
        this.jukeboxLocation = jukeboxLocation;
        this.frameUuids = frameUuids.toArray(new UUID[0]);
        this.itemFrames = new ItemFrame[3];
        this.currentJackpot = plugin.getConfig().getDouble("jackpot.starting_amount", 500.0);
        this.hologramName = "slm-" + this.name.toLowerCase().replace(" ", "_");

        fetchFrameEntities();
        createOrUpdateHologram();
    }

    private void fetchFrameEntities() {
        for (int i = 0; i < 3; i++) {
            Entity entity = Bukkit.getEntity(frameUuids[i]);
            if (entity instanceof ItemFrame) {
                itemFrames[i] = (ItemFrame) entity;
            }
        }
    }

    public void createOrUpdateHologram() {
        if (!plugin.getConfig().getBoolean("jackpot.enabled", true)) return;

        Location hologramLoc = jukeboxLocation.clone().add(0.5, 1.8, 0.5); 
        Hologram hologram = DHAPI.getHologram(hologramName);
        
        DecimalFormat df = new DecimalFormat("#,##0.00");
        String amountStr = df.format(this.currentJackpot);

        List<String> lines = plugin.getLangManager().getStringList("jackpot.hologram_lines").stream()
                .map(line -> line.replace("{amount}", amountStr))
                .collect(Collectors.toList());

        if (hologram == null) {
            DHAPI.createHologram(hologramName, hologramLoc, lines);
        } else {
            DHAPI.setHologramLines(hologram, lines);
            if (!hologram.getLocation().equals(hologramLoc)) {
                // CAMBIO: Se usa DHAPI.moveHologram en lugar de hologram.teleport
                DHAPI.moveHologram(hologramName, hologramLoc);
            }
        }
    }

    public void removeHologram() {
        Hologram hologram = DHAPI.getHologram(hologramName);
        if (hologram != null) {
            hologram.delete();
        }
    }

    public boolean isValid() {
        if (jukeboxLocation == null || jukeboxLocation.getBlock().getType() != Material.JUKEBOX) {
            return false;
        }
        for (ItemFrame frame : itemFrames) {
            if (frame == null || frame.isDead()) {
                return false;
            }
        }
        return true;
    }

    public void startSpin(Player player, double coinValue) {
        if (isSpinning || !isValid()) return;

        if (plugin.getConfig().getBoolean("jackpot.enabled")) {
            double contribution = coinValue * (plugin.getConfig().getInt("machine_settings.play_cost", 1));
            this.currentJackpot += contribution;
            createOrUpdateHologram();
            plugin.getSlotMachineManager().saveMachines();
        }

        this.isSpinning = true;
        this.spinTask = new SpinTask(plugin, this, player).runTaskTimer(plugin, 0L, 2L);
        
        try {
            Sound sound = Sound.valueOf(plugin.getConfig().getString("sounds.coin_insert", "ENTITY_ITEM_PICKUP").toUpperCase());
            player.playSound(player.getLocation(), sound, 1f, 1f);
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid 'coin_insert' sound in config.yml");
        }
    }

    public void forceStop() {
        if (spinTask != null && !spinTask.isCancelled()) {
            spinTask.cancel();
        }
        isSpinning = false;
        cleanupFrames();
    }

    public void cleanup() {
        removeHologram();
        cleanupFrames();
    }

    private void cleanupFrames() {
        for (ItemFrame frame : itemFrames) {
            if (frame != null && !frame.isDead()) {
                frame.setItem(new ItemStack(Material.AIR));
            }
        }
    }

    public void save(ConfigurationSection section) {
        section.set("jukeboxLocation", jukeboxLocation);
        section.set("frameUuids", Arrays.asList(frameUuids[0].toString(), frameUuids[1].toString(), frameUuids[2].toString()));
        section.set("currentJackpot", currentJackpot);
    }

    public String getName() { return name; }
    public Location getJukeboxLocation() { return jukeboxLocation; }
    public ItemFrame[] getItemFrames() { return itemFrames; }
    public boolean isSpinning() { return isSpinning; }
    public void setSpinning(boolean spinning) { isSpinning = spinning; }
    public double getCurrentJackpot() { return currentJackpot; }
    public void setCurrentJackpot(double amount) {
        this.currentJackpot = amount;
        createOrUpdateHologram();
        plugin.getSlotMachineManager().saveMachines();
    }
}
