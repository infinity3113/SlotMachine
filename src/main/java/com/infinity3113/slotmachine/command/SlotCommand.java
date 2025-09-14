package com.infinity3113.slotmachine.command;

import com.infinity3113.slotmachine.SlotMachinePlugin;
import com.infinity3113.slotmachine.machine.SlotMachineManager;
import com.infinity3113.slotmachine.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginDescriptionFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SlotCommand implements CommandExecutor, TabCompleter {

    private final SlotMachinePlugin plugin;
    private final SlotMachineManager machineManager;

    public SlotCommand(SlotMachinePlugin plugin) {
        this.plugin = plugin;
        this.machineManager = plugin.getSlotMachineManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("slotmachine.admin")) {
            MessageUtil.sendMessage(sender, plugin.getLangManager().getString("messages.no_permission"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create":
                handleCreate(sender, args);
                break;
            case "delete":
                handleDelete(sender, args);
                break;
            case "getcoin":
                handleGetCoin(sender, args);
                break;
            case "reload":
                plugin.reloadPluginConfig();
                MessageUtil.sendMessage(sender, plugin.getLangManager().getString("messages.reload_success"));
                break;
            case "help":
                sendHelp(sender);
                break;
            case "ver":
                handleVer(sender);
                break;
            default:
                sendHelp(sender);
                break;
        }
        return true;
    }

    private void handleVer(CommandSender sender) {
        PluginDescriptionFile pdf = plugin.getDescription();
        MessageUtil.sendMessage(sender, "&e--- Informacion de SlotMachine ---");
        MessageUtil.sendMessage(sender, "&6Nombre: &f" + pdf.getName());
        MessageUtil.sendMessage(sender, "&6Autor: &f" + String.join(", ", pdf.getAuthors()));
        MessageUtil.sendMessage(sender, "&6Version: &f" + pdf.getVersion());
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtil.sendMessage(sender, plugin.getLangManager().getString("messages.player_only"));
            return;
        }
        if (args.length < 2) {
            MessageUtil.sendMessage(sender, "&cUso: /slot create <nombre>");
            return;
        }
        Player player = (Player) sender;
        String machineName = args[1];
        
        machineManager.startCreation(player, machineName);
        MessageUtil.sendMessage(player, plugin.getLangManager().getString("messages.creation_start"));
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendMessage(sender, "&cUso: /slot delete <nombre>");
            return;
        }
        String machineName = args[1];
        if (machineManager.getMachineByName(machineName) == null) {
            MessageUtil.sendMessage(sender, plugin.getLangManager().getFormattedString("messages.machine_not_found", "name", machineName));
            return;
        }
        
        machineManager.deleteMachine(machineName);
        MessageUtil.sendMessage(sender, plugin.getLangManager().getFormattedString("messages.delete_success", "name", machineName));
    }

    private void handleGetCoin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtil.sendMessage(sender, plugin.getLangManager().getString("messages.player_only"));
            return;
        }
        Player player = (Player) sender;
        int amount = 1;
        if (args.length > 1) {
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                MessageUtil.sendMessage(player, "&cLa cantidad debe ser un numero.");
                return;
            }
        }
        
        ItemStack coin = getCoinItem();
        coin.setAmount(amount);
        player.getInventory().addItem(coin);

        String coinName = coin.getItemMeta().hasDisplayName() ? coin.getItemMeta().getDisplayName() : "Ficha";
        MessageUtil.sendMessage(player, plugin.getLangManager().getFormattedString("messages.coin_received", 
                "amount", String.valueOf(amount), "coin_name", coinName));
    }

    public static ItemStack getCoinItem() {
        SlotMachinePlugin plugin = SlotMachinePlugin.getInstance();
        String materialName = plugin.getConfig().getString("coin.material", "GOLD_NUGGET");
        Material material = Material.matchMaterial(materialName);
        if (material == null) material = Material.GOLD_NUGGET;
        
        ItemStack coin = new ItemStack(material);
        ItemMeta meta = coin.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(plugin.getLangManager().getString("coin.name"));
            List<String> lore = plugin.getLangManager().getStringList("coin.lore");
            meta.setLore(lore);
            coin.setItemMeta(meta);
        }
        return coin;
    }

    private void sendHelp(CommandSender sender) {
        List<String> helpLines = plugin.getLangManager().getStringList("messages.help_message");
        for (String line : helpLines) {
            sender.sendMessage(MessageUtil.colorize(line));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("create", "delete", "getcoin", "reload", "help", "ver");
        }
        return new ArrayList<>();
    }
}