package com.infinity3113.slotmachine.util;

import com.infinity3113.slotmachine.SlotMachinePlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class MessageUtil {

    public static String colorize(String message) {
        if (message == null) return "";
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static void sendMessage(CommandSender sender, String message) {
        if (message == null || message.isEmpty()) return;
        
        String prefix = SlotMachinePlugin.getInstance().getConfig().getString("messages.prefix", "&e&lSlots &8» ");
        sender.sendMessage(colorize(prefix + message));
    }
}