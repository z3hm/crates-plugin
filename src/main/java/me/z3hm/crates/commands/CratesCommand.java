package me.z3hm.crates.commands;

import me.z3hm.crates.CratesPlugin;
import me.z3hm.crates.classes.Crate;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Set;

public class CratesCommand implements CommandExecutor {

    private CratesPlugin plugin;

    public CratesCommand(CratesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {

        if (args.length == 0) return true;

        if (!sender.hasPermission("crates." + args[0].toLowerCase()) && !sender.isOp()) {
            sender.sendMessage("§cYou don't have permission for that!");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            this.plugin.crates.loadCrates();
            this.plugin.reloadConfig();

            sender.sendMessage("§aCrates configuration reloaded successfully!");
            return true;
        }
        else if (args[0].equalsIgnoreCase("list")) {

            StringBuilder message = new StringBuilder("§fAvailable crates: ");
            Set<String> stringSet = this.plugin.crates.getCrates().keySet();
            if (args.length > 1 && args[1].equalsIgnoreCase("instances")) {
                message = new StringBuilder("§fAvailable instances: ");
                ConfigurationSection instances = this.plugin.getData().getConfigurationSection("instances");
                if (instances == null) return true;
                stringSet = instances.getKeys(false);
            }

            boolean addComma = false;
            for (String string : stringSet) {
                if (addComma) message.append(", ");
                addComma = true;
                message.append(string);
            }
            message.append(".");
            sender.sendMessage(message.toString());
            return true;
        }
        else if (args[0].equalsIgnoreCase("create")) {
            if (args.length == 1) {
                sender.sendMessage("§cMissing argument! Expected:");
                sender.sendMessage("§c/" + command.getName() + " create <id>");
                return true;
            }
            String crateId = args[1];
            Crate crate = new Crate(this.plugin.crates, new HashMap<>());
            this.plugin.crates.saveCrate(crate, crateId);

            sender.sendMessage("§aSuccessfully §2created §athe crate §2" + crateId +"§a!");

            return true;
        }
        else if (args[0].equalsIgnoreCase("set")) {
            if (args.length < 4) {
                sender.sendMessage("§cMissing argument(s)! Expected:");
                sender.sendMessage("§c/" + command.getName() + " set <crateId> <itemId> <weight>");
                return true;
            }
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cOnly a player may execute this command!");
                return true;
            }
            Player player = (Player) sender;

            String crateId = args[1];
            String itemId = args[2];
            int weight;
            try {
                weight = Integer.parseInt(args[3]);
            }
            catch (Exception ignored) {
                sender.sendMessage("§cWeight should be a number!");
                sender.sendMessage("§c/" + command.getName() + " set <crateId> <itemId> <weight>");
                return true;
            }
            FileConfiguration data = this.plugin.getData();
            ConfigurationSection section = data.getConfigurationSection("crates." + crateId);
            if (section == null) {
                sender.sendMessage("§cThe specified crate does not exist!");
                sender.sendMessage("§cCreate one with:");
                sender.sendMessage("§c/" + command.getName() + " create <id>");
                return true;
            }
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item.getType().equals(Material.AIR)) {
                sender.sendMessage("§cYou should be holding an item!");
                return true;
            }

            section.set(itemId + ".item", item);
            section.set(itemId + ".weight", weight);

            sender.sendMessage("§aSuccessfully §2set §athe item §2" + itemId + "§a in crate §2" + crateId +"§a!");

            return true;
        }
        else if (args[0].equalsIgnoreCase("unset")) {
            if (args.length < 3) {
                sender.sendMessage("§cMissing argument(s)! Expected:");
                sender.sendMessage("§c/" + command.getName() + " set <crateId> <itemId>");
                return true;
            }
            String crateId = args[1];
            String itemId = args[2];

            FileConfiguration data = this.plugin.getData();
            ConfigurationSection section = data.getConfigurationSection("crates." + crateId);

            if (section == null) {
                sender.sendMessage("§cThe specified crate does not exist!");
                sender.sendMessage("§cCreate one with:");
                sender.sendMessage("§c/" + command.getName() + " create <id>");
                return true;
            }
            if (section.getConfigurationSection(itemId) == null) {
                sender.sendMessage("§cThe specified item does not exist!");
                return true;
            }
            section.set(itemId, null);
            sender.sendMessage("§aSuccessfully §cunset §athe item §2" + itemId + "§a from crate §2" + crateId +"§a!");
            return true;
        }
        else if (args[0].equalsIgnoreCase("instance")) {
            if (args.length < 3) {
                sender.sendMessage("§cMissing argument(s)! Expected:");
                sender.sendMessage("§c/" + command.getName() + " instance <crateId> <instanceId>");
                return true;
            }

            if (!(sender instanceof Player)) {
                sender.sendMessage("§cOnly a player may execute this command!");
                return true;
            }
            Player player = (Player) sender;

            Block block = player.getTargetBlockExact(4, FluidCollisionMode.NEVER);

            if (block == null || block.getType().isAir()) {
                sender.sendMessage("§cYou should be targeting a block!");
                return true;
            }
            String crateId = args[1];
            String instanceId = args[2];

            FileConfiguration data = this.plugin.getData();

            if (data.getConfigurationSection("crates." + crateId) == null) {
                sender.sendMessage("§cThe specified crate does not exist!");
                sender.sendMessage("§cCreate one with:");
                sender.sendMessage("§c/" + command.getName() + " create <id>");
                return true;
            }

            data.set("instances." + instanceId, crateId);
            data.set("locations." + block.getLocation(), instanceId);

            sender.sendMessage("§aCrate instance §2" + instanceId + "§a of type §2" + crateId + "§a created successfully!");
            return true;
        }
        else if (args[0].equalsIgnoreCase("delinstance")) {
            if (args.length < 2) {
                sender.sendMessage("§cMissing argument(s)! Expected:");
                sender.sendMessage("§c/" + command.getName() + " instance <instanceId>");
                return true;
            }
            String instanceId = args[1];

            FileConfiguration data = this.plugin.getData();

            ConfigurationSection locations = data.getConfigurationSection("locations");

            if (locations == null) {
                sender.sendMessage("§cThe specified instance does not exist!");
                return true;
            }

            String locationString = null;
            for (String key : locations.getKeys(false)) {
                String str = locations.getString(key);
                if (str == null) continue;
                if (str.equalsIgnoreCase(instanceId)) {
                    locationString = key;
                    break;
                }
            }

            if (locationString != null) {
                data.set("locations." + locationString, null);
            }

            data.set("instances." + instanceId, null);

            sender.sendMessage("§aCrate instance §2" + instanceId + "§a deleted successfully!");
            return true;
        }
        else if (args[0].equalsIgnoreCase("give")) {
            if (args.length < 3) {
                sender.sendMessage("§cMissing argument(s)! Expected:");
                sender.sendMessage("§c/" + command.getName() + " give <playerName> <crateId> [amount]");
                return true;
            }

            Player player = Bukkit.getPlayer(args[1]);
            if (player == null) {
                sender.sendMessage("§cThis player is offline or does not exist!");
                return true;
            }
            String crateId = args[2];
            if (!this.plugin.crates.getCrates().containsKey(crateId)) {
                sender.sendMessage("§cThe specified crate does not exist!");
                sender.sendMessage("§cCreate one with:");
                sender.sendMessage("§c/" + command.getName() + " create <id>");
                return true;
            }
            int amount = 1;
            if (args.length >= 4) {
                try {
                    amount = Integer.parseInt(args[3]);
                } catch (NumberFormatException ignored) {
                    sender.sendMessage("§cThe specified amount is not a number! Expected:");
                    sender.sendMessage("§c/" + command.getName() + " give <playerName> <crateId> [amount]");
                    return true;
                }
            }

            this.plugin.crates.giveKey(player, crateId, amount);

            return true;
        }
        else if (args[0].equalsIgnoreCase("color")) {
            if (args.length < 3) {
                sender.sendMessage("§cMissing argument(s)! Expected:");
                sender.sendMessage("§c/" + command.getName() + " color <instanceId> <color>");
                return true;
            }
            String instanceId = args[1];
            FileConfiguration data = this.plugin.getData();
            if (data.getString("instances." + instanceId, null) == null) {
                sender.sendMessage("§cThe specified instance does not exist!");
                sender.sendMessage("§cCreate one with:");
                sender.sendMessage("§c/" + command.getName() + " instance <instanceId> <instanceId>");
                return true;
            }
            String colorString = args[2];
            int colorHex;
            try {
                colorHex = Integer.parseInt(colorString, 16);
            }
            catch (NumberFormatException ignore) {
                sender.sendMessage("§cThe color should be in hexadecimal! Expected:");
                sender.sendMessage("§c/" + command.getName() + " color <instanceId> <color>");
                return true;
            }

            Color color = Color.fromRGB(colorHex);

            data.set("color." + instanceId, color);

            sender.sendMessage("§aColor successfully set for instance §2" + instanceId + "§a!");

            return true;
        }
        else if (args[0].equalsIgnoreCase("delete")) {
            if (args.length < 2) {
                sender.sendMessage("§cMissing argument(s)! Expected:");
                sender.sendMessage("§c/" + command.getName() + " delete <crateId>");
                return true;
            }
            String crateId = args[1];

            if (!this.plugin.crates.getCrates().containsKey(crateId)) {
                sender.sendMessage("§cThe specified crate does not exist!");
                return true;
            }

            FileConfiguration data = this.plugin.getData();

            data.set("crates." + crateId, null);

            ConfigurationSection instances = data.getConfigurationSection("instances");

            if (instances == null) return true;

            for (String instance : instances.getKeys(false)) {
                String str = instances.getString(instance);
                if (str == null) continue;
                if (str.equalsIgnoreCase(crateId)) {
                    instances.set(instance, null);
                }
            }

            sender.sendMessage("§aSuccessfully deleted §2" + crateId + "§a and all its instances!");
            return true;
        }
        else if (args[0].equalsIgnoreCase("setsize")) {
            if (args.length < 3) {
                sender.sendMessage("§cMissing argument(s)! Expected:");
                sender.sendMessage("§c/" + command.getName() + " setsize <crateId> <amount of lines>");
                return true;
            }

            String crateId = args[1];
            if (!this.plugin.crates.getCrates().containsKey(crateId)) {
                sender.sendMessage("§cThe specified crate does not exist!");
                sender.sendMessage("§cCreate one with:");
                sender.sendMessage("§c/" + command.getName() + " create <id>");
                return true;
            }

            int lines;
            try {
                lines = Integer.parseInt(args[2]);
            }
            catch (NumberFormatException ignore) {
                sender.sendMessage("§cThe specified amount is not a number! Expected:");
                sender.sendMessage("§c/" + command.getName() + " give <playerName> <crateId> [amount]");
                return true;
            }

            FileConfiguration data = this.plugin.getData();
            data.set("crates." + crateId + ".guiLines", lines);
            this.plugin.setData(data);

            return true;
        }

        return true;
    }
}
