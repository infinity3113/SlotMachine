package com.infinity3113.slotmachine.machine;

import com.infinity3113.slotmachine.SlotMachinePlugin;
import com.infinity3113.slotmachine.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.text.DecimalFormat;
import java.util.List;
import java.util.UUID;

public class SlotMachine {

    private final SlotMachinePlugin plugin;
    private final String name;
    private final Location jukeboxLocation;
    private final UUID[] frameUuids;
    private final ItemFrame[] itemFrames;

    private boolean isSpinning = false;
    private BukkitTask spinTask;

    // Nuevos campos para el bote
    private double currentJackpot;
    private UUID hologramUuid;
    private ArmorStand hologram;

    public SlotMachine(SlotMachinePlugin plugin, ConfigurationSection section) {
        this.plugin = plugin;
        this.name = section.getName();
        this.jukeboxLocation = section.getLocation("jukeboxLocation");
        List<String> uuidStrings = section.getStringList("frameUuids");
        this.frameUuids = uuidStrings.stream().map(UUID::fromString).toArray(UUID[]::new);
        this.itemFrames = new ItemFrame[3];
        this.currentJackpot = section.getDouble("currentJackpot", plugin.getConfig().getDouble("jackpot.starting_amount", 500.0));
        String uuidStr = section.getString("hologramUuid");
        if (uuidStr != null) {
            this.hologramUuid = UUID.fromString(uuidStr);
        }
        fetchFrameEntities();
        fetchHologram();
    }
    
    public SlotMachine(SlotMachinePlugin plugin, String name, Location jukeboxLocation, List<UUID> frameUuids) {
        this.plugin = plugin;
        this.name = name;
        this.jukeboxLocation = jukeboxLocation;
        this.frameUuids = frameUuids.toArray(new UUID[0]);
        this.itemFrames = new ItemFrame[3];
        this.currentJackpot = plugin.getConfig().getDouble("jackpot.starting_amount", 500.0);
        fetchFrameEntities();
        createHologram();
    }

    private void fetchFrameEntities() {
        for (int i = 0; i < 3; i++) {
            Entity entity = Bukkit.getEntity(frameUuids[i]);
            if (entity instanceof ItemFrame) {
                itemFrames[i] = (ItemFrame) entity;
            }
        }
    }

    private void fetchHologram() {
        if (hologramUuid != null) {
            Entity entity = Bukkit.getEntity(hologramUuid);
            if (entity instanceof ArmorStand) {
                this.hologram = (ArmorStand) entity;
                updateHologram(); // Asegurarse de que el texto esté actualizado al cargar
            }
        }
    }

    private void createHologram() {
        if (!plugin.getConfig().getBoolean("jackpot.enabled", true)) return;
        // ***** CAMBIO REALIZADO AQUÍ *****
        // Se sube el holograma 1 bloque (de 0.5 a 1.5 en el eje Y)
        Location hologramLoc = jukeboxLocation.clone().add(0.5, 1.5, 0.5); 
        this.hologram = (ArmorStand) jukeboxLocation.getWorld().spawnEntity(hologramLoc, EntityType.ARMOR_STAND);
        this.hologram.setVisible(false);
        this.hologram.setGravity(false);
        this.hologram.setCustomNameVisible(true);
        this.hologram.setMarker(true); // No se puede interactuar con él
        this.hologramUuid = this.hologram.getUniqueId();
        updateHologram();
    }

    public void updateHologram() {
        if (hologram == null || hologram.isDead() || !plugin.getConfig().getBoolean("jackpot.enabled", true)) return;
        DecimalFormat df = new DecimalFormat("#,##0.00");
        String text = plugin.getConfig().getString("jackpot.hologram_text", "&e&lBote: &a${amount}");
        text = text.replace("{amount}", df.format(this.currentJackpot));
        hologram.setCustomName(MessageUtil.colorize(text));
    }

    public void removeHologram() {
        if (hologram != null && !hologram.isDead()) {
            hologram.remove();
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

        // Contribución al bote
        if (plugin.getConfig().getBoolean("jackpot.enabled")) {
            double contributionPercent = plugin.getConfig().getDouble("jackpot.contribution_percentage", 50);
            double contribution = coinValue * (contributionPercent / 100.0);
            this.currentJackpot += contribution;
            updateHologram();
            plugin.getSlotMachineManager().saveMachines(); // Guardar el nuevo bote
        }

        this.isSpinning = true;
        this.spinTask = new SpinTask(plugin, this, player).runTaskTimer(plugin, 0L, 2L);
        
        try {
            Sound sound = Sound.valueOf(plugin.getConfig().getString("sounds.coin_insert", "ENTITY_ITEM_PICKUP"));
            player.playSound(player.getLocation(), sound, 1f, 1f);
        } catch (Exception e) {
            plugin.getLogger().warning("Sonido 'coin_insert' invalido en config.yml");
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
        section.set("frameUuids", List.of(frameUuids[0].toString(), frameUuids[1].toString(), frameUuids[2].toString()));
        if (hologramUuid != null) {
            section.set("hologramUuid", hologramUuid.toString());
        }
        section.set("currentJackpot", currentJackpot);
    }

    // Getters y Setters
    public String getName() { return name; }
    public Location getJukeboxLocation() { return jukeboxLocation; }
    public ItemFrame[] getItemFrames() { return itemFrames; }
    public boolean isSpinning() { return isSpinning; }
    public void setSpinning(boolean spinning) { isSpinning = spinning; }
    public double getCurrentJackpot() { return currentJackpot; }
    public void setCurrentJackpot(double amount) {
        this.currentJackpot = amount;
        updateHologram();
        plugin.getSlotMachineManager().saveMachines();
    }
}