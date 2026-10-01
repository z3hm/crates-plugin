package me.z3hm.crates;

import me.z3hm.crates.classes.Crates;
import me.z3hm.crates.commands.CratesCommand;
import me.z3hm.crates.listeners.InventoryClickListener;
import me.z3hm.crates.listeners.PlayerInteractListener;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public final class CratesPlugin extends JavaPlugin {

    public Crates crates;

    private final File dataFile = new File(getDataFolder(), "data.yml");
    private FileConfiguration dataConfig;

    @Override
    public void onEnable() {

        if (!this.dataFile.exists()) {
            getDataFolder().mkdirs();
            try {
                this.dataFile.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        this.dataConfig = YamlConfiguration.loadConfiguration(this.dataFile);

        crates = new Crates(this);

        getCommand("crates").setExecutor(new CratesCommand(this));

        Bukkit.getPluginManager().registerEvents(new PlayerInteractListener(this), this);
        Bukkit.getPluginManager().registerEvents(new InventoryClickListener(this), this);

        saveDefaultConfig();

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        saveData();
    }

    public FileConfiguration getData() {
        return dataConfig;
    }
    public void setData(FileConfiguration data) {
        this.dataConfig = data;
    }
    public void saveData() {
        try {
            this.dataConfig.save(this.dataFile);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
