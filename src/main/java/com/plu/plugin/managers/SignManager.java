package com.plu.plugin.managers;

import com.plu.plugin.Main;
import com.plu.plugin.storage.DatabaseManager;
import com.plu.plugin.util.Values;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 命令告示牌：右键告示牌执行预设命令。 */
public class SignManager {

    public static class SignEntry {
        public String world;
        public int x, y, z;
        public String command = "";
        public String permission = "";
        public String creator = "";
        public long created;
    }

    private final Main plugin;
    private final DatabaseManager db;
    private final File file;
    private final Map<String, SignEntry> signs = new LinkedHashMap<>();

    public SignManager(Main plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db = db;
        this.file = new File(plugin.getDataFolder(), "signs.yml");
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("cmdsigns.enabled", true);
    }

    public static String key(Location loc) {
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    public void load() {
        signs.clear();
        if (db.isEnabled()) {
            try {
                for (Object[] r : db.query("SELECT world,x,y,z,command,permission,creator,created FROM "
                        + db.getPrefix() + "signs")) {
                    SignEntry e = new SignEntry();
                    e.world = String.valueOf(r[0]);
                    e.x = Values.asInt(r[1], 0);
                    e.y = Values.asInt(r[2], 0);
                    e.z = Values.asInt(r[3], 0);
                    e.command = r[4] == null ? "" : String.valueOf(r[4]);
                    e.permission = r[5] == null ? "" : String.valueOf(r[5]);
                    e.creator = r[6] == null ? "" : String.valueOf(r[6]);
                    e.created = Values.asLong(r[7], 0);
                    signs.put(e.world + ":" + e.x + ":" + e.y + ":" + e.z, e);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load signs from MySQL: " + e.getMessage());
            }
            return;
        }

        if (!file.exists()) return;
        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = data.getConfigurationSection("signs");
        if (root == null) return;
        for (String key : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(key);
            if (s == null) continue;
            SignEntry e = new SignEntry();
            e.world = s.getString("world", "");
            e.x = s.getInt("x");
            e.y = s.getInt("y");
            e.z = s.getInt("z");
            e.command = s.getString("command", "");
            e.permission = s.getString("permission", "");
            e.creator = s.getString("creator", "");
            e.created = s.getLong("created");
            signs.put(e.world + ":" + e.x + ":" + e.y + ":" + e.z, e);
        }
    }

    public void save() {
        if (db.isEnabled()) return;
        YamlConfiguration data = new YamlConfiguration();
        for (Map.Entry<String, SignEntry> entry : signs.entrySet()) {
            String base = "signs." + entry.getKey().replace(":", "_");
            SignEntry e = entry.getValue();
            data.set(base + ".world", e.world);
            data.set(base + ".x", e.x);
            data.set(base + ".y", e.y);
            data.set(base + ".z", e.z);
            data.set(base + ".command", e.command);
            data.set(base + ".permission", e.permission);
            data.set(base + ".creator", e.creator);
            data.set(base + ".created", e.created);
        }
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save signs.yml: " + e.getMessage());
        }
    }

    public SignEntry get(Location loc) {
        return signs.get(key(loc));
    }

    public void add(Location loc, String command, String permission, String creator) {
        SignEntry e = new SignEntry();
        e.world = loc.getWorld().getName();
        e.x = loc.getBlockX();
        e.y = loc.getBlockY();
        e.z = loc.getBlockZ();
        e.command = command;
        e.permission = permission == null ? "" : permission;
        e.creator = creator == null ? "" : creator;
        e.created = System.currentTimeMillis();
        signs.put(key(loc), e);
        persist(e, false);
    }

    /** 更新已有告示牌配置（权限等）并持久化。 */
    public void update(SignEntry entry) {
        if (entry == null) return;
        signs.put(entry.world + ":" + entry.x + ":" + entry.y + ":" + entry.z, entry);
        persist(entry, true);
    }

    public boolean remove(Location loc) {        String key = key(loc);
        SignEntry removed = signs.remove(key);
        if (removed == null) return false;
        if (db.isEnabled()) {
            try {
                db.update("DELETE FROM " + db.getPrefix() + "signs WHERE world=? AND x=? AND y=? AND z=?",
                        removed.world, removed.x, removed.y, removed.z);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to delete sign: " + e.getMessage());
            }
        } else {
            save();
        }
        return true;
    }

    public List<SignEntry> all() {
        return new ArrayList<>(signs.values());
    }

    public Location toLocation(SignEntry e) {
        World world = Bukkit.getWorld(e.world);
        if (world == null) return null;
        return new Location(world, e.x, e.y, e.z);
    }

    private void persist(SignEntry e, boolean update) {
        if (db.isEnabled()) {
            try {
                db.update("INSERT INTO " + db.getPrefix()
                                + "signs (world,x,y,z,command,permission,creator,created) VALUES (?,?,?,?,?,?,?,?) "
                                + "ON DUPLICATE KEY UPDATE command=VALUES(command),permission=VALUES(permission),"
                                + "creator=VALUES(creator),created=VALUES(created)",
                        e.world, e.x, e.y, e.z, e.command, e.permission, e.creator, e.created);
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to save sign: " + ex.getMessage());
            }
            return;
        }
        save();
    }
}
