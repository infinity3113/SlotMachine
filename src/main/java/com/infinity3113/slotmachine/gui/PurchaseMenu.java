package com.infinity3113.slotmachine.gui;

import com.infinity3113.slotmachine.SlotMachinePlugin;
import com.infinity3113.slotmachine.util.MessageUtil;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class PurchaseMenu {

    private final SlotMachinePlugin plugin;
    private int quantity;
    private final double pricePerCoin;
    private Inventory inventory;

    public PurchaseMenu(SlotMachinePlugin plugin, double pricePerCoin) {
        this.plugin = plugin;
        this.pricePerCoin = pricePerCoin;
        this.quantity = 1;
        createInventory();
    }

    private void createInventory() {
        String title = MessageUtil.colorize(plugin.getConfig().getString("purchase_gui.title", "&1Comprar Fichas"));
        this.inventory = Bukkit.createInventory(null, 45, title);
    }

    public void open(Player player) {
        updateItems(player);
        player.openInventory(this.inventory);
        GuiListener.openMenus.put(player.getUniqueId(), this);
    }

    private void updateItems(Player player) {
        inventory.clear();
        
        // Botones de control
        inventory.setItem(10, createGuiItem("remove_ten_item"));
        inventory.setItem(11, createGuiItem("remove_one_item"));
        inventory.setItem(15, createGuiItem("add_one_item"));
        inventory.setItem(16, createGuiItem("add_ten_item"));
        inventory.setItem(22, createGuiItem("set_max_item")); // Botón de máximo
        
        // Botones de acción
        inventory.setItem(30, createGuiItem("cancel_item"));
        inventory.setItem(32, createGuiItem("confirm_item"));

        // Item de información central
        updateInfoItem(player);
    }
    
    private void updateInfoItem(Player player){
        Economy economy = plugin.getEconomyManager().getEconomy();
        double balance = economy.getBalance(player);
        double totalCost = quantity * pricePerCoin;

        DecimalFormat df = new DecimalFormat("#,##0.00");
        ItemStack infoItem = createGuiItem("info_item");
        ItemMeta meta = infoItem.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            for (String line : plugin.getConfig().getStringList("purchase_gui.info_item.lore")) {
                lore.add(MessageUtil.colorize(line
                        .replace("{quantity}", String.valueOf(quantity))
                        .replace("{cost}", df.format(totalCost))
                        .replace("{balance}", df.format(balance))
                ));
            }
            meta.setLore(lore);
            infoItem.setItemMeta(meta);
        }
        inventory.setItem(13, infoItem);
    }
    
    public void updateQuantity(Player player, int change) {
        int newQuantity = this.quantity + change;
        if (newQuantity < 1) {
            newQuantity = 1;
        }
        this.quantity = newQuantity;
        updateInfoItem(player);
    }
    
    public void setmaxQuantity(Player player){
        Economy economy = plugin.getEconomyManager().getEconomy();
        double balance = economy.getBalance(player);
        if(pricePerCoin > 0){
            this.quantity = (int) (balance / pricePerCoin);
            if(this.quantity < 1) this.quantity = 1;
        }
        updateInfoItem(player);
    }

    private ItemStack createGuiItem(String configKey) {
        String path = "purchase_gui." + configKey;
        Material material = Material.valueOf(plugin.getConfig().getString(path + ".material", "STONE"));
        String name = MessageUtil.colorize(plugin.getConfig().getString(path + ".name", ""));
        
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getTotalCost() {
        return quantity * pricePerCoin;
    }
}