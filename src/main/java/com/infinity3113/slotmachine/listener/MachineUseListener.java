package com.infinity3113.slotmachine.listener;

import com.infinity3113.slotmachine.SlotMachinePlugin;
import com.infinity3113.slotmachine.command.SlotCommand;
import com.infinity3113.slotmachine.machine.SlotMachine;
import com.infinity3113.slotmachine.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot; // <-- IMPORTANTE: Añadir esta línea
import org.bukkit.inventory.ItemStack;

public class MachineUseListener implements Listener {

    private final SlotMachinePlugin plugin;

    public MachineUseListener(SlotMachinePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMachineUse(PlayerInteractEvent event) {
        // --- INICIO DE LA CORRECCIÓN ---
        // Se añade esta condición para ignorar el evento de la mano secundaria
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        // --- FIN DE LA CORRECCIÓN ---

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.JUKEBOX) return;

        Player player = event.getPlayer();
        SlotMachine machine = plugin.getSlotMachineManager().getMachineByJukeboxLocation(event.getClickedBlock().getLocation());

        if (machine == null || plugin.getSlotMachineManager().isInCreationMode(player)) return;

        event.setCancelled(true);

        if (!player.hasPermission("slotmachine.use")) {
            MessageUtil.sendMessage(player, plugin.getLangManager().getString("messages.no_permission"));
            return;
        }

        if (machine.isSpinning()) {
            MessageUtil.sendMessage(player, plugin.getLangManager().getString("messages.machine_in_use"));
            return;
        }

        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        ItemStack requiredCoin = SlotCommand.getCoinItem();

        if (!itemInHand.isSimilar(requiredCoin)) {
             String coinName = requiredCoin.getItemMeta().hasDisplayName() ? requiredCoin.getItemMeta().getDisplayName() : "Ficha";
             MessageUtil.sendMessage(player, plugin.getLangManager().getFormattedString("messages.need_coin_to_play", "coin_name", coinName));
            return;
        }

        int cost = plugin.getConfig().getInt("machine_settings.play_cost", 1);
        if (itemInHand.getAmount() < cost) {
             String coinName = requiredCoin.getItemMeta().hasDisplayName() ? requiredCoin.getItemMeta().getDisplayName() : "Ficha";
             MessageUtil.sendMessage(player, plugin.getLangManager().getFormattedString("messages.not_enough_coins",
                "amount", String.valueOf(cost),
                "coin_name", coinName));
            return;
        }
        
        double coinValue = plugin.getConfig().getDouble("machine_settings.coin_value_for_jackpot", 50.0);

        itemInHand.setAmount(itemInHand.getAmount() - cost);
        machine.startSpin(player, coinValue);
    }
}