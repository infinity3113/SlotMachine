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
    private final ItemFrame[] frames;
    private final Sound spinSound;

    private final TreeMap<Integer, Material> weightedItems = new TreeMap<>();
    private int totalWeight = 0;

    private int ticks = 0;
    private final int[] stopTicks = new int[3];
    private final Material[] finalResult = new Material[3];
    private boolean[] stopped = {false, false, false};
    private final int timeoutTicks;

    public SpinTask(SlotMachinePlugin plugin, SlotMachine machine, Player player) {
        this.plugin = plugin;
        this.machine = machine;
        this.player = player;
        this.frames = machine.getItemFrames();
        
        loadWeightedItems();

        int baseTicks = plugin.getConfig().getInt("machine_settings.spin_duration.base_ticks", 60);
        int reelDelay = plugin.getConfig().getInt("machine_settings.spin_duration.reel_delay_ticks", 20);
        int extraRandom = plugin.getConfig().getInt("machine_settings.spin_duration.extra_random_ticks", 20);
        this.timeoutTicks = plugin.getConfig().getInt("machine_settings.spin_duration.timeout_seconds", 15) * 20;

        this.stopTicks[0] = baseTicks + ThreadLocalRandom.current().nextInt(extraRandom);
        this.stopTicks[1] = this.stopTicks[0] + reelDelay + ThreadLocalRandom.current().nextInt(extraRandom);
        this.stopTicks[2] = this.stopTicks[1] + reelDelay + ThreadLocalRandom.current().nextInt(extraRandom);

        Sound sound;
        try {
            sound = Sound.valueOf(plugin.getConfig().getString("sounds.spin", "BLOCK_NOTE_BLOCK_PLING").toUpperCase());
        } catch (Exception e) {
            sound = Sound.BLOCK_NOTE_BLOCK_PLING;
        }
        this.spinSound = sound;
    }

    private void loadWeightedItems() {
        ConfigurationSection itemsSection = plugin.getConfig().getConfigurationSection("roulette_items");
        if(itemsSection == null) return;
        
        for(String key : itemsSection.getKeys(false)) {
            try {
                Material material = Material.valueOf(key.toUpperCase());
                int weight = itemsSection.getInt(key);
                if(weight > 0) {
                    totalWeight += weight;
                    weightedItems.put(totalWeight, material);
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid item in roulette_items: " + key);
            }
        }
    }

    @Override
    public void run() {
        if (weightedItems.isEmpty()) {
            MessageUtil.sendMessage(player, plugin.getLangManager().getString("messages.no_roulette_items"));
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

        if (ticks > timeoutTicks) {
            MessageUtil.sendMessage(player, plugin.getLangManager().getString("messages.machine_stuck"));
            stop();
        }
    }

    private void checkWin() {
        if (plugin.getConfig().getBoolean("jackpot.enabled")) {
            String[] jackpotCombo = plugin.getLangManager().getString("jackpot.combination").split(",");
            if (isCombination(jackpotCombo)) {
                double jackpotAmount = machine.getCurrentJackpot();
                Economy economy = plugin.getEconomyManager().getEconomy();
                economy.depositPlayer(player, jackpotAmount);

                DecimalFormat df = new DecimalFormat("#,##0.00");
                String jackpotWinMsg = plugin.getLangManager().getFormattedString("jackpot.win_message", "amount", df.format(jackpotAmount));
                MessageUtil.sendMessage(player, jackpotWinMsg);
                playSound("jackpot_win");

                double startingAmount = plugin.getConfig().getDouble("jackpot.starting_amount", 500.0);
                machine.setCurrentJackpot(startingAmount);
                return;
            }
        }

        ConfigurationSection prizesSection = plugin.getConfig().getConfigurationSection("prizes");
        if (prizesSection != null) {
            for (String key : prizesSection.getKeys(false)) {
                String[] combination = plugin.getLangManager().getString("prizes." + key + ".combination").split(",");
                if (isCombination(combination)) {
                    double reward = prizesSection.getDouble(key + ".reward");
                    Economy economy = plugin.getEconomyManager().getEconomy();
                    economy.depositPlayer(player, reward);

                    DecimalFormat df = new DecimalFormat("#,##0.00");
                    String winMessage = plugin.getLangManager().getFormattedString("messages.win_message", "reward", df.format(reward));
                    MessageUtil.sendMessage(player, winMessage);
                    playSound("win");
                    return;
                }
            }
        }
        
        MessageUtil.sendMessage(player, plugin.getLangManager().getString("messages.lose_message"));
        playSound("lose");
    }

    private boolean isCombination(String[] combination) {
        if (combination.length != 3) return false;
        
        List<Material> results = new ArrayList<>(Arrays.asList(finalResult));
        
        for(String comboItemName : combination) {
            boolean matchFound = false;
            if (comboItemName.equalsIgnoreCase("ANY")) {
                if(!results.isEmpty()){
                    results.remove(0);
                    matchFound = true;
                }
            } else {
                Material comboMaterial = Material.matchMaterial(comboItemName);
                if (comboMaterial != null && results.contains(comboMaterial)) {
                    results.remove(comboMaterial);
                    matchFound = true;
                }
            }
            if(!matchFound) return false;
        }
        return true;
    }

    private void stop() {
        machine.setSpinning(false);
        this.cancel();
    }

    private Material getRandomItem() {
        if (totalWeight == 0) return Material.BARRIER;
        int r = ThreadLocalRandom.current().nextInt(totalWeight);
        return weightedItems.higherEntry(r).getValue();
    }
    
    private void playSound(String soundKey) {
        try {
            Sound sound = Sound.valueOf(plugin.getConfig().getString("sounds." + soundKey).toUpperCase());
            player.playSound(player.getLocation(), sound, 1f, 1f);
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid sound '" + soundKey + "' in config.yml");
        }
    }
}