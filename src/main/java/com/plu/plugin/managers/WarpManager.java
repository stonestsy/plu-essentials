package com.plu.plugin.managers;

import com.plu.plugin.Main;
import com.plu.plugin.storage.DatabaseManager;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/** Warp system with MySQL/YAML storage. */
public class WarpManager {

    private final Main plugin;
    private final DatabaseManager db;
    private final File file;
    private FileConfiguration data;
    private final Map<String, Location> warps = new HashMap<>();

    public WarpManager(Main plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db = db;
        this.file = new File(plugin.getDataFolder(), "warps.yml");
    }

    public void load() {
        warps.clear();
        if (db.isEnabled()) {
            try {
                String tbl = db.getPrefix() + "warps";
                for (Object[] row : db.query("SELECT name,world,x,y,z,yaw,pitch FROM " + tbl)) {
                    String name = (String) row[0];
                    String world = (String) row[1];
                    warps.put(name.toLowerCase(), new Location(
                            plugin.getServer().getWorld(world),
                            ((Number) row[2]).doubleValue(),
                            ((Number) row[3]).doubleValue(),
                            ((Number) row[4]).doubleValue(),
                            ((Number) row[5]).floatValue(),
                            ((Number) row[6]).floatValue()));
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load warps from MySQL: " + e.getMessage());
            }
        } else {
            if (!file.exists()) return;
            data = YamlConfiguration.loadConfiguration(file);
            for (String name : data.getKeys(false)) {
                ConfigurationSection s = data.getConfigurationSection(name);
                String world = s.getString("world");
                warps.put(name.toLowerCase(), new Location(
                        plugin.getServer().getWorld(world),
                        s.getDouble("x"), s.getDouble("y"), s.getDouble("z"),
                        (float) s.getDouble("yaw"), (float) s.getDouble("pitch")));
            }
        }
    }

    private void save() {
        if (db.isEnabled()) return;
        if (data == null) data = new YamlConfiguration();
        for (String key : data.getKeys(false)) data.set(key, null);
        for (Map.Entry<String, Location> e : warps.entrySet()) {
            Location loc = e.getValue();
            data.set(e.getKey() + ".world", loc.getWorld().getName());
            data.set(e.getKey() + ".x", loc.getX());
            data.set(e.getKey() + ".y", loc.getY());
            data.set(e.getKey() + ".z", loc.getZ());
            data.set(e.getKey() + ".yaw", (double) loc.getYaw());
            data.set(e.getKey() + ".pitch", (double) loc.getPitch());
        }
        try { data.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public boolean setWarp(String name, Location loc) {
        warps.put(name.toLowerCase(), loc.clone());
        if (db.isEnabled()) {
            try {
                String tbl = db.getPrefix() + "warps";
                db.update("INSERT INTO " + tbl + " (name,world,x,y,z,yaw,pitch) VALUES (?,?,?,?,?,?,?) "
                        + "ON DUPLICATE KEY UPDATE world=VALUES(world),x=VALUES(x),y=VALUES(y),z=VALUES(z),yaw=VALUES(yaw),pitch=VALUES(pitch)",
                        name.toLowerCase(), loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(),
                        loc.getYaw(), loc.getPitch());
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to save warp to MySQL: " + e.getMessage());
            }
        } else {
            save();
        }
        return true;
    }

    public boolean deleteWarp(String name) {
        String key = name.toLowerCase();
        if (!warps.containsKey(key)) return false;
        warps.remove(key);
        if (db.isEnabled()) {
            try {
                db.update("DELETE FROM " + db.getPrefix() + "warps WHERE name=?", key);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to delete warp from MySQL: " + e.getMessage());
            }
        } else {
            if (data != null) data.set(key, null);
            save();
        }
        return true;
    }

    public Location getWarp(String name) {
        return warps.get(name.toLowerCase());
    }

    public List<String> getWarpNames() {
        return new ArrayList<>(warps.keySet());
    }

    public int getCount() { return warps.size(); }
}
