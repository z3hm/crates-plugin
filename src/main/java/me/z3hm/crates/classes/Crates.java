package me.z3hm.crates.classes;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BukkitConverters;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import me.z3hm.crates.CratesPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class Crates {

    public final CratesPlugin plugin;
    private HashMap<String, Crate> crates = new HashMap<>();

    public HashSet<Player> clickedTickAgo = new HashSet<>();
    public HashSet<Player> opening = new HashSet<>();

    public Crates(CratesPlugin plugin) {
        this.plugin = plugin;
        loadCrates();
    }

    public HashMap<String, Crate> getCrates() {
        loadCrates();
        return crates;
    }

    public void showCrate(Player player, String crateId) {
        Component title = Component.text(Character.toUpperCase(crateId.charAt(0)) + crateId.substring(1) + " Crate");

        int guiLines = this.plugin.getData().getInt("crates." + crateId + ".guiLines", this.plugin.getConfig().getInt("gui.crateGuiSize", 5));

        Inventory inv = Bukkit.createInventory(null, guiLines*9, title);

        ItemStack glass_pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta gpMeta = glass_pane.getItemMeta();
        gpMeta.itemName(Component.text(""));
        glass_pane.setItemMeta(gpMeta);
        for (int line = 0; line < guiLines; line++) {
            if (line == 0 || line == guiLines-1) {
                for (int s = 0; s < 9; s++) {
                    inv.setItem(line*9+s, glass_pane.clone());
                }
            }
            else {
                inv.setItem(line*9, glass_pane.clone());
                inv.setItem(line*9+8, glass_pane.clone());
            }
        }

        FileConfiguration data = this.plugin.getData();

        ConfigurationSection section = data.getConfigurationSection("crates." + crateId);

        if (section == null) return;

        ArrayList<String> itemStrings = new ArrayList<>(section.getKeys(false));
        itemStrings.remove("dummy");
        
        int offset = 0;
        for (int i = 0; i < Math.min(itemStrings.size(), guiLines*9); i++) {
            if (i + offset >= guiLines*9) break;
            while (inv.getItem(i + offset) != null) {
                if (i + offset >= guiLines*9) break;
                offset += 1;
            }
            ItemStack item = section.getItemStack(itemStrings.get(i) + ".item", null);
            if (item == null) continue;
            int slot = section.getInt(itemStrings.get(i) + ".slot", i) + offset;
            inv.setItem(slot, item);
        }

        player.openInventory(inv);
    }

    public void loadCrates() {
        FileConfiguration data = this.plugin.getData();
        ConfigurationSection section = data.getConfigurationSection("crates");
        this.crates = new HashMap<>();
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            Crate crate = loadCrate(key);
            this.crates.put(key, loadCrate(key));
        }
    }

    public Crate loadCrate(String crateId) {
        FileConfiguration data = this.plugin.getData();
        HashMap<ItemStack, Integer> rewards = new HashMap<>();

        ConfigurationSection section = data.getConfigurationSection("crates." + crateId);

        if (section == null) return null;

        for (String key : section.getKeys(false)) {
            if (key.equalsIgnoreCase("dummy")) continue;
            ItemStack item = section.getItemStack(key + ".item");
            int chance = section.getInt(key + ".weight");
            rewards.put(item, chance);
        }

        return new Crate(this, rewards);
    }

    public void saveCrate(Crate crate, String crateId) {
        FileConfiguration data = this.plugin.getData();

        ArrayList<ItemStack> items = new ArrayList<>(crate.getRewards().keySet());
        if (items.isEmpty()) {
            data.set("crates." + crateId + ".dummy", 0);
            return;
        }
        for (int i = 0; i < items.toArray().length; i++) {
            data.set("crates." + crateId + ".item" + i + ".item", items.get(i));
            data.set("crates." + crateId + ".item" + i + ".weight", crate.getRewards().get(items.get(i)));
        }
    }

    public boolean giveKey(Player player, String crateId, int amount) {
        Crate crate = this.crates.get(crateId);
        if (crate == null) {
            return false;
        }
        ItemStack key = new ItemStack(Material.TRIPWIRE_HOOK, amount);
        ItemMeta meta = key.getItemMeta();
        String capitalizedCrateId = Character.toUpperCase(crateId.charAt(0)) + crateId.substring(1);
        meta.itemName(Component.text(capitalizedCrateId + " Key").color(NamedTextColor.YELLOW));

        meta.lore(Arrays.asList(
                Component.text("Click a crate to open it!").color(NamedTextColor.GRAY),
                Component.text("Crate: " + crateId).color(NamedTextColor.GRAY)
        ));

        key.setItemMeta(meta);
        giveSafe(player, key);
        return true;
    }


    public void playSoundThenGiveItem(Player player, ItemStack item, Location location) {
        playSoundRepeat(player, item, location, 0);
    }

    private void playSoundRepeat(Player player, ItemStack item, Location location, int i) {

        if (i >= this.plugin.getConfig().getInt("sound.loopTimes", 5)) {
            giveSafe(player, item);
            this.opening.remove(player);
            sendFireworkExplosion(player, location);
            return;
        }
        if (this.plugin.getConfig().get("sound.sound") == null) {
            this.plugin.getConfig().set("sound.sound", Registry.SOUNDS.getKey(Sound.BLOCK_NOTE_BLOCK_PLING));
        }
        String keyString = this.plugin.getConfig().getString("sound.sound", Registry.SOUNDS.getKey(Sound.BLOCK_NOTE_BLOCK_PLING).asString());
        NamespacedKey key = NamespacedKey.fromString(keyString);
        Sound sound = Sound.BLOCK_NOTE_BLOCK_PLING;
        if (key != null) {
            sound = Registry.SOUNDS.get(key);
        }

        player.playSound(location, sound, (float)(this.plugin.getConfig().getInt("sound.volume", 25))/100, i%3);
        Bukkit.getScheduler().runTaskLater(this.plugin,
                () -> playSoundRepeat(player, item, location, i+1),
                this.plugin.getConfig().getInt("sound.interval", 3)
        );
    }

    private void giveSafe(Player player, ItemStack item) {
        if (player.getInventory().firstEmpty() == -1) {
            player.getWorld().dropItem(player.getLocation(), item);
            return;
        }
        player.getInventory().addItem(item);
    }

    public void sendFireworkExplosion(Player player, Location loc) {
        ProtocolManager manager = ProtocolLibrary.getProtocolManager();
        int entityId = new Random().nextInt(Integer.MAX_VALUE - 1) + 1;
        UUID entityUuid = UUID.randomUUID();

        // 1. Spawn the fake firework entity
        PacketContainer spawn = manager.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
        spawn.getIntegers().write(0, entityId);
        spawn.getUUIDs().write(0, entityUuid);
        spawn.getEntityTypeModifier().write(0, EntityType.FIREWORK_ROCKET);
        spawn.getDoubles()
                .write(0, loc.getX())
                .write(1, loc.getY())
                .write(2, loc.getZ());

        // 2. Give it firework metadata so the colors/shape render correctly
        ItemStack firework = new ItemStack(Material.FIREWORK_ROCKET);
        FireworkMeta meta = (FireworkMeta) firework.getItemMeta();
        String instanceId = this.plugin.getData().getString("locations." + loc.add(-0.5,-0.5,-0.5), "dummy");
        meta.addEffect(FireworkEffect.builder()
                .withColor(this.plugin.getData().getColor("color." + instanceId, Color.WHITE))
                .with(FireworkEffect.Type.BALL_LARGE)
                .withTrail()
                .build());
        firework.setItemMeta(meta);

        WrappedDataValue fireworkItemValue = new WrappedDataValue(
                8, // metadata index for the firework item — verify against current NMS for 1.26.1.2
                WrappedDataWatcher.Registry.getItemStackSerializer(false),
                BukkitConverters.getItemStackConverter().getGeneric(firework)
        );

        byte entityFlags = 0x20;
        WrappedDataValue invisibleFlag = new WrappedDataValue(
                0,
                WrappedDataWatcher.Registry.get(Byte.class),
                entityFlags
        );

        PacketContainer metadata = manager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
        metadata.getIntegers().write(0, entityId);
        metadata.getDataValueCollectionModifier().write(0, List.of(invisibleFlag, fireworkItemValue));

        // 3. Tell the client it exploded (status 17 = firework explode)
        PacketContainer status = manager.createPacket(PacketType.Play.Server.ENTITY_STATUS);
        status.getIntegers().write(0, entityId);
        status.getBytes().write(0, (byte) 17);

        // 4. Remove the fake entity
        PacketContainer destroy = manager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
        destroy.getIntLists().write(0, Collections.singletonList(entityId));

        try {
            manager.sendServerPacket(player, spawn);
            manager.sendServerPacket(player, metadata);
            manager.sendServerPacket(player, status);

            // small delay before destroying so the explosion has time to render
            Bukkit.getScheduler().runTaskLater(plugin, () ->
                    manager.sendServerPacket(player, destroy), 5L);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
