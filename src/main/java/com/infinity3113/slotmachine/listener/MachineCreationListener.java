package com.infinity3113.slotmachine.listener;

import com.infinity3113.slotmachine.SlotMachinePlugin;
import com.infinity3113.slotmachine.machine.SlotMachine;
import com.infinity3113.slotmachine.machine.SlotMachineManager;
import com.infinity3113.slotmachine.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class MachineCreationListener implements Listener {

    private final SlotMachinePlugin plugin;
    private final SlotMachineManager manager;

    public MachineCreationListener(SlotMachinePlugin plugin) {
        this.plugin = plugin;
        this.manager = plugin.getSlotMachineManager();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!manager.isInCreationMode(player)) return;

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        event.setCancelled(true);
        
        SlotMachineManager.CreationProgress progress = manager.getCreationProgress(player);
        if (progress == null || progress.getJukeboxLocation() != null) return;

        if (event.getClickedBlock().getType() == Material.JUKEBOX) {
            progress.setJukeboxLocation(event.getClickedBlock().getLocation());
            MessageUtil.sendMessage(player, plugin.getConfig().getString("messages.creation_click_frame"));
        } else {
            MessageUtil.sendMessage(player, "&cDebes hacer clic derecho en una Caja de Musica.");
        }
    }
    
    @EventHandler
    public void onPlayerInteractEntity(org.bukkit.event.player.PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (!manager.isInCreationMode(player)) return;
        
        if (event.getRightClicked().getType() != EntityType.ITEM_FRAME) return;

        event.setCancelled(true);
        SlotMachineManager.CreationProgress progress = manager.getCreationProgress(player);
        if (progress == null || progress.getJukeboxLocation() == null) return;

        UUID frameUuid = event.getRightClicked().getUniqueId();
        if (!progress.getFrameUuids().contains(frameUuid)) {
            progress.addFrameUuid(frameUuid);
            MessageUtil.sendMessage(player, "&aMarco " + progress.getFrameUuids().size() + "/3 seleccionado.");
        } else {
            MessageUtil.sendMessage(player, "&cYa has seleccionado este marco.");
            return;
        }

        if (progress.isReady()) {
            SlotMachine newMachine = new SlotMachine(plugin, progress.getName(), progress.getJukeboxLocation(), progress.getFrameUuids());
            manager.addMachine(newMachine);
            MessageUtil.sendMessage(player, plugin.getConfig().getString("messages.creation_success").replace("{name}", progress.getName()));
            manager.cancelCreation(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (manager.isInCreationMode(event.getPlayer())) {
            manager.cancelCreation(event.getPlayer());
        }
    }
}