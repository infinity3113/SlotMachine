package com.infinity3113.slotmachine.listener;

import com.infinity3113.slotmachine.SlotMachinePlugin;
import com.infinity3113.slotmachine.gui.PurchaseMenu;
import com.infinity3113.slotmachine.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class SignListener implements Listener {

    private final SlotMachinePlugin plugin;

    public SignListener(SlotMachinePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSignCreate(SignChangeEvent event) {
        // Se obtiene el texto de la primera línea del archivo de idioma
        String line1FromLang = plugin.getLangManager().getString("buy_sign.line1");
        
        // Se comparan las líneas sin colores para asegurar que coincidan
        if (!ChatColor.stripColor(event.getLine(0)).equalsIgnoreCase(ChatColor.stripColor(line1FromLang))) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.hasPermission("slotmachine.sign.create")) {
            MessageUtil.sendMessage(player, plugin.getLangManager().getString("messages.no_permission"));
            event.setCancelled(true);
            return;
        }

        try {
            // Se valida el precio introducido por el jugador
            String priceInput = event.getLine(2).replace(",", ".");
            double price = Double.parseDouble(priceInput);

            // --- INICIO DE LA SOLUCIÓN DEFINITIVA ---

            // 1. Se preparan las líneas del cartel en texto plano (sin colores)
            String plainLine1 = ChatColor.stripColor(line1FromLang);
            String plainLine2 = ChatColor.stripColor(plugin.getLangManager().getString("buy_sign.line2"));
            String plainPriceLine = "$" + String.format("%.2f", price); // Se asegura que el precio tenga 2 decimales

            // Se establece el texto plano en el evento de creación del cartel
            event.setLine(0, plainLine1);
            event.setLine(1, plainLine2);
            event.setLine(2, plainPriceLine);
            event.setLine(3, "");

            // 2. Se programa una tarea para que se ejecute 1 tick de servidor después
            Location signLocation = event.getBlock().getLocation();
            Bukkit.getScheduler().runTask(plugin, () -> {
                Block block = signLocation.getBlock();
                // Se verifica que el bloque siga siendo un cartel
                if (block.getState() instanceof Sign) {
                    Sign sign = (Sign) block.getState();
                    
                    // Se aplican los colores al cartel ya existente
                    sign.setLine(0, MessageUtil.colorize(line1FromLang));
                    sign.setLine(1, MessageUtil.colorize(plugin.getLangManager().getString("buy_sign.line2")));
                    sign.setLine(2, MessageUtil.colorize("&a" + plainPriceLine));
                    
                    // Se actualiza el cartel para que los cambios sean visibles
                    sign.update(true);
                }
            });
            // --- FIN DE LA SOLUCIÓN DEFINITIVA ---

            MessageUtil.sendMessage(player, "&a¡Cartel de compra creado!");

        } catch (NumberFormatException e) {
            MessageUtil.sendMessage(player, plugin.getLangManager().getString("messages.invalid_sign_format"));
            event.setLine(0, MessageUtil.colorize("&c[Error]"));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSignUse(PlayerInteractEvent event) {
        // Se ignora el evento de la mano secundaria para evitar doble ejecución
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null || !(clickedBlock.getState() instanceof Sign)) return;

        Sign sign = (Sign) clickedBlock.getState();
        
        // Se comparan las primeras líneas sin colores para identificar el cartel de compra
        String signLine1 = ChatColor.stripColor(sign.getLine(0));
        String langLine1 = ChatColor.stripColor(plugin.getLangManager().getString("buy_sign.line1"));

        if (!signLine1.equalsIgnoreCase(langLine1)) {
            return;
        }
        
        event.setCancelled(true);

        Player player = event.getPlayer();
        if (!player.hasPermission("slotmachine.sign.use")) {
            MessageUtil.sendMessage(player, plugin.getLangManager().getString("messages.no_permission"));
            return;
        }

        try {
            // Se extrae el precio del cartel, eliminando colores y el símbolo "$"
            String priceText = ChatColor.stripColor(sign.getLine(2)).replace("$", "");
            double pricePerCoin = Double.parseDouble(priceText);
            
            // Se abre el menú de compra
            PurchaseMenu menu = new PurchaseMenu(plugin, pricePerCoin, player);
            menu.open(player);
        } catch (Exception e) {
            MessageUtil.sendMessage(player, "&cEl cartel parece estar dañado.");
        }
    }
}