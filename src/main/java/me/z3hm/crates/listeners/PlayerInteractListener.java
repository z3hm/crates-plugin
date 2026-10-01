package me.z3hm.crates.listeners;

import me.z3hm.crates.CratesPlugin;
import me.z3hm.crates.classes.Crate;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class PlayerInteractListener implements Listener {

    CratesPlugin plugin;

    public PlayerInteractListener(CratesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.hasBlock()) return;
        if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            onRightClick(event);
        }
        else if (event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
            onLeftClick(event);
        }
    }

    private void onLeftClick(PlayerInteractEvent event) {
        FileConfiguration data = this.plugin.getData();

        Block block = event.getClickedBlock();
        if (block == null) return;

        String instanceId = data.getString("locations." + block.getLocation());
        if (instanceId == null) return;

        String crateId = data.getString("instances." + instanceId);
        if (crateId == null) return;

        Crate crate = this.plugin.crates.getCrates().get(crateId);
        if (crate == null) return;

        event.setCancelled(true);

        this.plugin.crates.showCrate(event.getPlayer(), crateId);
    }

    private void onRightClick(PlayerInteractEvent event) {
        FileConfiguration data = this.plugin.getData();

        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        if (!item.hasItemMeta()) {
            return;
        }
        if (!item.getItemMeta().hasLore()) {
            return;
        }
        List<Component> lore = item.getItemMeta().lore();
        if (lore == null) {
            return;
        }
        event.setCancelled(true);


        Block block = event.getClickedBlock();
        if (block == null) return;

        String instanceId = data.getString("locations." + block.getLocation());
        if (instanceId == null) return;

        String crateId = data.getString("instances." + instanceId);
        if (crateId == null) return;

        Crate crate = this.plugin.crates.getCrates().get(crateId);
        if (crate == null) return;

        if (!loreContains(item, crateId) || !loreContains(item, "crate key")) {
            return;
        }

        if (crate.open(event.getPlayer(), block.getLocation().add(0.5,0.5,0.5))) {
            item.setAmount(item.getAmount()-1);
            event.getPlayer().getInventory().setItemInMainHand(item);
        }
    }

    private boolean loreContains(ItemStack item, String str) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) {
            return false;
        }
        List<Component> lore = item.getItemMeta().lore();

        assert lore != null;
        for (Component c : lore) {
            if (c.toString().toLowerCase().contains(str)) return true;
        }
        return false;
    }
}
