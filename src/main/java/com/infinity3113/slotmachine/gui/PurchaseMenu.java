package com.infinity3113.slotmachine.gui;

import com.infinity3113.slotmachine.SlotMachinePlugin;
import com.infinity3113.slotmachine.util.MessageUtil;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
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
    private double discountPercentage = 0;
    private Inventory inventory;

    public PurchaseMenu(SlotMachinePlugin plugin, double pricePerCoin, Player player) {
        this.plugin = plugin;
        this.pricePerCoin = pricePerCoin;
        this.quantity = 1;
        this.discountPercentage = getBestDiscount(player);
        createInventory();
    }

    private double getBestDiscount(Player player) {
        double bestDiscount = 0;
        ConfigurationSection discountsSection = plugin.getConfig().getConfigurationSection("purchase_discounts");
        if (discountsSection == null) return 0;

        for (String key : discountsSection.getKeys(false)) {
            String permission = discountsSection.getString(key + ".permission");
            double percentage = discountsSection.getDouble(key + ".percentage");
            if (permission != null && player.hasPermission(permission)) {
                if (percentage > bestDiscount) {
                    bestDiscount = percentage;
                }
            }
        }
        return bestDiscount;
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
        
        inventory.setItem(10, createGuiItem("remove_ten_item"));
        inventory.setItem(11, createGuiItem("remove_one_item"));
        inventory.setItem(15, createGuiItem("add_one_item"));
        inventory.setItem(16, createGuiItem("add_ten_item"));
        inventory.setItem(22, createGuiItem("set_max_item"));
        
        inventory.setItem(30, createGuiItem("cancel_item"));
        inventory.setItem(32, createGuiItem("confirm_item"));

        updateInfoItem(player);
    }
    
    private void updateInfoItem(Player player){
        Economy economy = plugin.getEconomyManager().getEconomy();
        double balance = economy.getBalance(player);
        double totalCost = getTotalCost();
        double originalCost = quantity * pricePerCoin;
        double discountAmount = originalCost - totalCost;

        DecimalFormat df = new DecimalFormat("#,##0.00");
        ItemStack infoItem = createGuiItem("info_item");
        ItemMeta meta = infoItem.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            for (String line : plugin.getConfig().getStringList("purchase_gui.info_item.lore")) {
                if (line.contains("{discount_line}") && discountPercentage > 0) {
                    String discountFormat = plugin.getConfig().getString("purchase_gui.discount_line_format", "&7Descuento ({percentage}%): &c-${discount_amount}");
                    lore.add(MessageUtil.colorize(discountFormat
                        .replace("{percentage}", String.valueOf((int)discountPercentage))
                        .replace("{discount_amount}", df.format(discountAmount))
                    ));
                } else if (!line.contains("{discount_line}")) {
                    lore.add(MessageUtil.colorize(line
                            .replace("{quantity}", String.valueOf(quantity))
                            .replace("{cost}", df.format(totalCost))
                            .replace("{balance}", df.format(balance))
                    ));
                }
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
    
    public void setMaxQuantity(Player player){
        Economy economy = plugin.getEconomyManager().getEconomy();
        double balance = economy.getBalance(player);
        double effectivePrice = pricePerCoin * (1 - (discountPercentage / 100.0));
        
        if(effectivePrice > 0){
            this.quantity = (int) (balance / effectivePrice);
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
        double total = quantity * pricePerCoin;
        if (discountPercentage > 0) {
            total *= (1 - (discountPercentage / 100.0));
        }
        return total;
    }
}