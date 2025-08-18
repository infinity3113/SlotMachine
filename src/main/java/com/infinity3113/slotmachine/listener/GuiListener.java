package com.infinity3113.slotmachine.gui;

import com.infinity3113.slotmachine.SlotMachinePlugin;
import com.infinity3113.slotmachine.command.SlotCommand;
import com.infinity3113.slotmachine.util.MessageUtil;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GuiListener implements Listener {

    private final SlotMachinePlugin plugin;
    public static final Map<UUID, PurchaseMenu> openMenus = new HashMap<>();

    public GuiListener(SlotMachinePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (!openMenus.containsKey(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true); // Prevenir que el jugador tome los items
        PurchaseMenu menu = openMenus.get(player.getUniqueId());
        Inventory clickedInventory = event.getClickedInventory();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedInventory == null || clickedInventory.equals(player.getInventory()) || clickedItem == null) {
            return;
        }
        
        player.playSound(player.getLocation(), Sound.valueOf(plugin.getConfig().getString("sounds.gui_click", "UI_BUTTON_CLICK").toUpperCase()), 0.7f, 1f);

        int slot = event.getSlot();
        switch (slot) {
            case 10: // Remove 10
                menu.updateQuantity(player, -10);
                break;
            case 11: // Remove 1
                menu.updateQuantity(player, -1);
                break;
            case 15: // Add 1
                menu.updateQuantity(player, 1);
                break;
            case 16: // Add 10
                menu.updateQuantity(player, 10);
                break;
            case 22: // Set Max
                 menu.setmaxQuantity(player);
                break;
            case 30: // Cancel
                player.closeInventory();
                openMenus.remove(player.getUniqueId());
                break;
            case 32: // Confirm
                handlePurchase(player, menu);
                break;
        }
    }

    private void handlePurchase(Player player, PurchaseMenu menu) {
        int quantity = menu.getQuantity();
        double totalCost = menu.getTotalCost();

        if (quantity <= 0) {
            return;
        }

        Economy economy = plugin.getEconomyManager().getEconomy();
        if (economy.getBalance(player) < totalCost) {
            MessageUtil.sendMessage(player, plugin.getConfig().getString("messages.not_enough_money"));
            return;
        }

        economy.withdrawPlayer(player, totalCost);
        ItemStack coins = SlotCommand.getCoinItem();
        coins.setAmount(quantity);
        player.getInventory().addItem(coins);

        String coinName = coins.getItemMeta().hasDisplayName() ? coins.getItemMeta().getDisplayName() : "Ficha";
        MessageUtil.sendMessage(player, plugin.getConfig().getString("messages.purchase_success")
                .replace("{quantity}", String.valueOf(quantity))
                .replace("{coin_name}", coinName)
                .replace("{cost}", String.format("%,.2f", totalCost)));
        
        player.closeInventory();
        openMenus.remove(player.getUniqueId());
    }
}