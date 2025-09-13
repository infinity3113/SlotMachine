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
import org.bukkit.event.player.PlayerQuitEvent;
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

        event.setCancelled(true);
        PurchaseMenu menu = openMenus.get(player.getUniqueId());
        Inventory clickedInventory = event.getClickedInventory();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedInventory == null || !clickedInventory.equals(menu.getInventory())) {
            return;
        }
        
        player.playSound(player.getLocation(), Sound.valueOf(plugin.getConfig().getString("sounds.gui_click", "UI_BUTTON_CLICK").toUpperCase()), 0.7f, 1f);

        int slot = event.getSlot();
        switch (slot) {
            case 10:
                menu.updateQuantity(player, -10);
                break;
            case 11:
                menu.updateQuantity(player, -1);
                break;
            case 15:
                menu.updateQuantity(player, 1);
                break;
            case 16:
                menu.updateQuantity(player, 10);
                break;
            case 22:
                 menu.setMaxQuantity(player);
                break;
            case 30:
                player.closeInventory();
                openMenus.remove(player.getUniqueId());
                break;
            case 32:
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
            MessageUtil.sendMessage(player, plugin.getLangManager().getString("messages.not_enough_money"));
            return;
        }

        economy.withdrawPlayer(player, totalCost);
        ItemStack coins = SlotCommand.getCoinItem();
        coins.setAmount(quantity);
        player.getInventory().addItem(coins);

        String coinName = coins.getItemMeta().hasDisplayName() ? coins.getItemMeta().getDisplayName() : "Ficha";
        MessageUtil.sendMessage(player, plugin.getLangManager().getFormattedString("messages.purchase_success",
                "quantity", String.valueOf(quantity),
                "coin_name", coinName,
                "cost", String.format("%,.2f", totalCost)));
        
        player.closeInventory();
        openMenus.remove(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        openMenus.remove(event.getPlayer().getUniqueId());
    }
}