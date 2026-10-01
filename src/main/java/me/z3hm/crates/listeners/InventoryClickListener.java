package me.z3hm.crates.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import me.z3hm.crates.CratesPlugin;

public class InventoryClickListener implements Listener {

    CratesPlugin plugin;

    public InventoryClickListener(CratesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked().getOpenInventory().title().toString().contains("Crate")) event.setCancelled(true);;
    }
}
