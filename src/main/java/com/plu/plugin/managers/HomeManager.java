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

public class HomeManager {

    private final Main plugin;
    private final DatabaseManager db;
    private final File file;
    private FileConfiguration data;
    // cache: uuid -> name -> location
    private final Map<UUID, Map<String, Location>> cache = new HashMap<>();

    public HomeManager(Main plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db = db;
        this.file = new File(plugin.getDataFolder(), "homes.yml");
    }

    public void load() {
        cache.clear();
        if (db.isEnabled()) {
            try {
                String tbl = db.getPrefix() + "homes";
                for (Object[] row : db.query("SELECT uuid,name,world,x,y,z,yaw,pitch FROM " + tbl)) {
                    UUID uuid = UUID.fromString((String) row[0]);
                    String name = (String) row[1];
                    String world = (String) row[2];
                    cache.computeIfAbsent(uuid, k -> new HashMap<>()).put(name.toLowerCase(), new Location(
                            plugin.getServer().getWorld(world),
                            ((Number) row[3]).doubleValue(),
                            ((Number) row[4]).doubleValue(),
                            ((Number) row[5]).doubleValue(),
                            ((Number) row[6]).floatValue(),
                            ((Number) row[7]).floatValue()));
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load homes from MySQL: " + e.getMessage());
            }
        } else {
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                try { file.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
            }
            data = YamlConfiguration.loadConfiguration(file);
        }
    }

    public void save() {
        if (db.isEnabled()) return;
        if (data == null) return;
        try { data.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public void setHome(UUID uuid, String name, Location loc) {
        String key = name.toLowerCase();
        if (db.isEnabled()) {
            try {
                String tbl = db.getPrefix() + "homes";
                db.update("INSERT INTO " + tbl + " (uuid,name,world,x,y,z,yaw,pitch) VALUES (?,?,?,?,?,?,?,?) "
                        + "ON DUPLICATE KEY UPDATE world=VALUES(world),x=VALUES(x),y=VALUES(y),z=VALUES(z),yaw=VALUES(yaw),pitch=VALUES(pitch)",
                        uuid.toString(), key, loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(),
                        loc.getYaw(), loc.getPitch());
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to save home to MySQL: " + e.getMessage());
            }
        } else {
            String path = uuid + "." + key;
            data.set(path + ".world", loc.getWorld().getName());
            data.set(path + ".x", loc.getX());
            data.set(path + ".y", loc.getY());
            data.set(path + ".z", loc.getZ());
            data.set(path + ".yaw", (double) loc.getYaw());
            data.set(path + ".pitch", (double) loc.getPitch());
            save();
        }
        cache.computeIfAbsent(uuid, k -> new HashMap<>()).put(key, loc);
    }

    public Location getHome(UUID uuid, String name) {
        String key = name.toLowerCase();
        Map<String, Location> m = cache.get(uuid);
        if (m != null && m.containsKey(key)) return m.get(key);
        if (db.isEnabled()) return null;
        // YAML fallback
        String path = uuid + "." + key;
        if (!data.contains(path)) return null;
        String world = data.getString(path + ".world");
        if (world == null) return null;
        Location loc = new Location(plugin.getServer().getWorld(world),
                data.getDouble(path + ".x"), data.getDouble(path + ".y"), data.getDouble(path + ".z"),
                (float) data.getDouble(path + ".yaw"), (float) data.getDouble(path + ".pitch"));
        cache.computeIfAbsent(uuid, k -> new HashMap<>()).put(key, loc);
        return loc;
    }

    public boolean deleteHome(UUID uuid, String name) {
        String key = name.toLowerCase();
        Map<String, Location> m = cache.get(uuid);
        if (m != null) m.remove(key);
        if (db.isEnabled()) {
            try {
                int rows = db.update("DELETE FROM " + db.getPrefix() + "homes WHERE uuid=? AND name=?", uuid.toString(), key);
                return rows > 0;
            } catch (Exception e) { return false; }
        } else {
            String path = uuid + "." + key;
            if (!data.contains(path)) return false;
            data.set(path, null);
            save();
            return true;
        }
    }

    public List<String> getHomes(UUID uuid) {
        Map<String, Location> m = cache.get(uuid);
        if (m != null) return new ArrayList<>(m.keySet());
        if (db.isEnabled()) return new ArrayList<>();
        ConfigurationSection sec = data.getConfigurationSection(uuid.toString());
        if (sec == null) return new ArrayList<>();
        List<String> names = new ArrayList<>(sec.getKeys(false));
        return names;
    }

    public int getHomeLimit(UUID uuid) {
        ConfigurationSection limits = plugin.getConfig().getConfigurationSection("home.limits");
        if (limits != null) {
            for (String key : limits.getKeys(false)) {
                if (plugin.getServer().getPlayer(uuid) != null
                        && plugin.getServer().getPlayer(uuid).hasPermission("plu.home.limit." + key)) {
                    return limits.getInt(key);
                }
            }
        }
        return plugin.getConfig().getInt("home.default-limit", 5);
    }
}
