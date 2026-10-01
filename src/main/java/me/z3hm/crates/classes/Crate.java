package me.z3hm.crates.classes;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Crate {

    private Crates crates;
    private HashMap<ItemStack, Integer> rewards;

    public Crate(Crates crates, HashMap<ItemStack, Integer> rewards) {
        this.rewards = rewards;
        this.crates = crates;
    }

    public HashMap<ItemStack, Integer> getRewards() {
        return rewards;
    }
    public void setRewards(HashMap<ItemStack, Integer> rewards) {
        this.rewards = rewards;
    }

    public boolean open(Player player, Location location) {
        if (this.crates.clickedTickAgo.contains(player)) return false;
        this.crates.clickedTickAgo.add(player);
        Bukkit.getScheduler().runTaskLater(this.crates.plugin, () -> this.crates.clickedTickAgo.remove(player), 1L);

        if (this.crates.opening.contains(player)) {
            player.sendMessage(this.crates.plugin.getConfig().getString("messages.alreadyOpening", "§cYou are already opening a crate! Wait for it to open first!"));
            return false;
        }

        if (this.rewards.isEmpty()) return false;

        if (this.crates.plugin.getConfig().getBoolean("mechanics.waitForCrateToOpen", true)) this.crates.opening.add(player);

        int totalChance = 0;
        for (int chance : this.rewards.values()) {
            totalChance += chance;
        }

        int pickedInt = (int) (totalChance*Math.random());

        List<ItemStack> items = new ArrayList<>(this.rewards.keySet());
        int current = 0;
        ItemStack pickedItem = null;
        for (ItemStack item : items) {
            pickedItem = item;
            if (current >= pickedInt) break;
            current += this.rewards.get(item);
        }

        crates.playSoundThenGiveItem(player, pickedItem, location);

        return true;
    }

}
