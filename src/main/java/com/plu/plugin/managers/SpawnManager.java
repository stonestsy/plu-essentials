package com.plu.plugin.managers;

import com.plu.plugin.Main;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class SpawnManager {

    private final Main plugin;
    private final File file;
    private FileConfiguration data;
    private Location spawn;

    public SpawnManager(Main plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "spawn.yml");
    }

    public void load() {
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            return;
        }
        data = YamlConfiguration.loadConfiguration(file);
        String world = data.getString("world");
        if (world == null) return;
        double x = data.getDouble("x");
        double y = data.getDouble("y");
        double z = data.getDouble("z");
        float yaw = (float) data.getDouble("yaw");
        float pitch = (float) data.getDouble("pitch");
        this.spawn = new Location(plugin.getServer().getWorld(world), x, y, z, yaw, pitch);
    }

    public void save() {
        if (spawn == null) return;
        if (data == null) data = new YamlConfiguration();
        data.set("world", spawn.getWorld().getName());
        data.set("x", spawn.getX());
        data.set("y", spawn.getY());
        data.set("z", spawn.getZ());
        data.set("yaw", (double) spawn.getYaw());
        data.set("pitch", (double) spawn.getPitch());
        try {
            data.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setSpawn(Location loc) {
        this.spawn = loc.clone();
        save();
    }

    public Location getSpawn() {
        if (spawn != null) return spawn;
        // fallback
        if (plugin.getConfig().getBoolean("spawn.use-world-spawn", true)) {
            return plugin.getServer().getWorlds().get(0).getSpawnLocation();
        }
        return null;
    }

    public boolean isCustomSpawn() {
        return spawn != null;
    }
}
