package com.infinity3113.slotmachine.listener;

import com.infinity3113.slotmachine.SlotMachinePlugin;
import com.infinity3113.slotmachine.gui.PurchaseMenu;
import com.infinity3113.slotmachine.util.MessageUtil;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class SignListener implements Listener {

    private final SlotMachinePlugin plugin;

    public SignListener(SlotMachinePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSignCreate(SignChangeEvent event) {
        if (!event.getLine(0).equalsIgnoreCase(plugin.getLangManager().getString("buy_sign.line1"))) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.hasPermission("slotmachine.sign.create")) {
            MessageUtil.sendMessage(player, plugin.getLangManager().getString("messages.no_permission"));
            event.setCancelled(true);
            return;
        }

        try {
            // Se reemplaza la coma por un punto para una validación consistente
            Double.parseDouble(event.getLine(2).replace(",", "."));
        } catch (Exception e) {
            MessageUtil.sendMessage(player, plugin.getLangManager().getString("messages.invalid_sign_format"));
            event.setLine(0, MessageUtil.colorize("&c[Error]"));
            return;
        }

        event.setLine(0, plugin.getLangManager().getString("buy_sign.line1"));
        event.setLine(1, plugin.getLangManager().getString("buy_sign.line2"));
        event.setLine(2, MessageUtil.colorize("&a$" + event.getLine(2)));
        event.setLine(3, "");
        MessageUtil.sendMessage(player, "&a¡Cartel de compra creado!");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSignUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null || !(clickedBlock.getState() instanceof Sign)) return;

        Sign sign = (Sign) clickedBlock.getState();
        if (!sign.getLine(0).equalsIgnoreCase(plugin.getLangManager().getString("buy_sign.line1"))) {
            return;
        }
        
        event.setCancelled(true);

        Player player = event.getPlayer();
        if (!player.hasPermission("slotmachine.sign.use")) {
            MessageUtil.sendMessage(player, plugin.getLangManager().getString("messages.no_permission"));
            return;
        }

        try {
            // --- INICIO DE LA CORRECCIÓN ---
            // Se limpia el texto, se reemplaza la coma por un punto y luego se convierte a número.
            String priceText = ChatColor.stripColor(sign.getLine(2)).replace("$", "").replace(",", ".");
            double pricePerCoin = Double.parseDouble(priceText);
            // --- FIN DE LA CORRECCIÓN ---
            
            PurchaseMenu menu = new PurchaseMenu(plugin, pricePerCoin, player);
            menu.open(player);
        } catch (Exception e) {
            MessageUtil.sendMessage(player, "&cEl cartel parece estar dañado.");
        }
    }
}