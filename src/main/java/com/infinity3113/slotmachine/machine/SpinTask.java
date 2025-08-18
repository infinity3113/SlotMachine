package com.infinity3113.slotmachine.machine;

import com.infinity3113.slotmachine.SlotMachinePlugin;
import com.infinity3113.slotmachine.util.MessageUtil;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class SpinTask extends BukkitRunnable {

    private final SlotMachinePlugin plugin;
    private final SlotMachine machine;
    private final Player player;
    private final List<Material> rouletteItems;
    private final ItemFrame[] frames;
    private final Sound spinSound;

    private int ticks = 0;
    private final int[] stopTicks = new int[3];
    private final Material[] finalResult = new Material[3];
    private boolean[] stopped = {false, false, false};

    public SpinTask(SlotMachinePlugin plugin, SlotMachine machine, Player player) {
        this.plugin = plugin;
        this.machine = machine;
        this.player = player;
        this.frames = machine.getItemFrames();
        this.rouletteItems = new ArrayList<>();
        plugin.getConfig().getStringList("roulette_items").forEach(s -> {
            try {
                rouletteItems.add(Material.valueOf(s.toUpperCase()));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Item invalido en roulette_items: " + s);
            }
        });

        this.stopTicks[0] = 60 + ThreadLocalRandom.current().nextInt(20);
        this.stopTicks[1] = this.stopTicks[0] + 20 + ThreadLocalRandom.current().nextInt(20);
        this.stopTicks[2] = this.stopTicks[1] + 20 + ThreadLocalRandom.current().nextInt(20);

        Sound sound;
        try {
            sound = Sound.valueOf(plugin.getConfig().getString("sounds.spin", "BLOCK_NOTE_BLOCK_PLING"));
        } catch (Exception e) {
            sound = Sound.BLOCK_NOTE_BLOCK_PLING;
        }
        this.spinSound = sound;
    }

    @Override
    public void run() {
        if (rouletteItems.isEmpty()) {
            MessageUtil.sendMessage(player, "&cError: No hay items configurados para la ruleta.");
            stop();
            return;
        }

        ticks++;
        player.playSound(player.getLocation(), spinSound, 0.5f, 1.5f);

        for (int i = 0; i < 3; i++) {
            if (!stopped[i]) {
                frames[i].setItem(new ItemStack(getRandomItem()));
            }
        }

        for (int i = 0; i < 3; i++) {
            if (!stopped[i] && ticks >= stopTicks[i]) {
                stopped[i] = true;
                finalResult[i] = getRandomItem();
                frames[i].setItem(new ItemStack(finalResult[i]));
            }
        }

        if (stopped[0] && stopped[1] && stopped[2]) {
            checkWin();
            stop();
        }

        if (ticks > 20 * 15) {
            MessageUtil.sendMessage(player, "&cLa maquina se ha atascado. Intentalo de nuevo.");
            stop();
        }
    }

    private void checkWin() {
        // Primero, comprobar si se ganó el bote
        if (plugin.getConfig().getBoolean("jackpot.enabled")) {
            String[] jackpotCombo = plugin.getConfig().getString("jackpot.combination", "DIAMOND,DIAMOND,DIAMOND").split(",");
            if (isCombination(jackpotCombo)) {
                double jackpotAmount = machine.getCurrentJackpot();
                Economy economy = plugin.getEconomyManager().getEconomy();
                economy.depositPlayer(player, jackpotAmount);

                DecimalFormat df = new DecimalFormat("#,##0.00");
                String jackpotWinMsg = plugin.getConfig().getString("jackpot.win_message").replace("{amount}", df.format(jackpotAmount));
                MessageUtil.sendMessage(player, jackpotWinMsg);
                playSound("jackpot_win");

                // Reiniciar el bote
                double startingAmount = plugin.getConfig().getDouble("jackpot.starting_amount", 500.0);
                machine.setCurrentJackpot(startingAmount);
                return; // Ganó el bote, no se dan otros premios
            }
        }

        // Si no ganó el bote, comprobar premios fijos
        ConfigurationSection prizesSection = plugin.getConfig().getConfigurationSection("prizes");
        if (prizesSection != null) {
            for (String key : prizesSection.getKeys(false)) {
                String[] combination = prizesSection.getString(key + ".combination", "").split(",");
                if (isCombination(combination)) {
                    double reward = prizesSection.getDouble(key + ".reward");
                    Economy economy = plugin.getEconomyManager().getEconomy();
                    economy.depositPlayer(player, reward);

                    DecimalFormat df = new DecimalFormat("#,##0.00");
                    String winMessage = plugin.getConfig().getString("messages.win_message").replace("{reward}", df.format(reward));
                    MessageUtil.sendMessage(player, winMessage);
                    playSound("win");
                    return;
                }
            }
        }

        // Si no ganó nada
        MessageUtil.sendMessage(player, plugin.getConfig().getString("messages.lose_message"));
        playSound("lose");
    }

    private boolean isCombination(String[] combination) {
        if (combination.length != 3) return false;
        
        Material[] currentResult = {finalResult[0], finalResult[1], finalResult[2]};
        boolean[] resultUsed = {false, false, false};

        for (int i = 0; i < 3; i++) {
            boolean matchFound = false;
            for (int j = 0; j < 3; j++) {
                if (!resultUsed[j]) {
                    if (combination[i].equalsIgnoreCase("ANY") || currentResult[j].name().equalsIgnoreCase(combination[i])) {
                        resultUsed[j] = true;
                        matchFound = true;
                        break;
                    }
                }
            }
            if (!matchFound) return false;
        }
        return true;
    }

    private void stop() {
        machine.setSpinning(false);
        this.cancel();
    }

    private Material getRandomItem() {
        return rouletteItems.get(ThreadLocalRandom.current().nextInt(rouletteItems.size()));
    }
    
    private void playSound(String soundKey) {
        try {
            Sound sound = Sound.valueOf(plugin.getConfig().getString("sounds." + soundKey).toUpperCase());
            player.playSound(player.getLocation(), sound, 1f, 1f);
        } catch (Exception e) {
            plugin.getLogger().warning("Sonido '" + soundKey + "' invalido en config.yml");
        }
    }
}