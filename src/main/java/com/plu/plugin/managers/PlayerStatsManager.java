package com.plu.plugin.managers;

import com.plu.plugin.Main;
import com.plu.plugin.storage.DatabaseManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/** Tracks player first-join and total playtime. */
public class PlayerStatsManager {

    private final Main plugin;
    private final DatabaseManager db;
    private final File file;
    private FileConfiguration data;
    // uuid -> {firstJoin, lastSeen, playtimeMs, name}
    private final Map<UUID, Object[]> stats = new HashMap<>();

    public PlayerStatsManager(Main plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db = db;
        this.file = new File(plugin.getDataFolder(), "playerstats.yml");
    }

    public void load() {
        stats.clear();
        if (db.isEnabled()) {
            try {
                String tbl = db.getPrefix() + "playerstats";
                for (Object[] row : db.query("SELECT uuid,name,first_join,last_seen,playtime FROM " + tbl)) {
                    stats.put(UUID.fromString((String) row[0]), new Object[]{row[2], row[3], row[4], row[1]});
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load playerstats from MySQL: " + e.getMessage());
            }
        } else {
            if (!file.exists()) return;
            data = YamlConfiguration.loadConfiguration(file);
            for (String u : data.getKeys(false)) {
                UUID uuid = UUID.fromString(u);
                var s = data.getConfigurationSection(u);
                stats.put(uuid, new Object[]{s.getLong("firstJoin"), s.getLong("lastSeen"), s.getLong("playtime"), s.getString("name")});
            }
        }
    }

    public void onJoin(UUID uuid, String name) {
        Object[] s = stats.get(uuid);
        long now = System.currentTimeMillis();
        if (s == null) {
            s = new Object[]{now, now, 0L, name};
            stats.put(uuid, s);
        } else {
            s[1] = now;
            s[3] = name;
        }
        persist(uuid);
    }

    public void onQuit(UUID uuid) {
        Object[] s = stats.get(uuid);
        if (s == null) return;
        long now = System.currentTimeMillis();
        s[2] = (Long) s[2] + (now - (Long) s[1]);
        s[1] = now;
        persist(uuid);
    }

    private void persist(UUID uuid) {
        Object[] s = stats.get(uuid);
        if (s == null) return;
        if (db.isEnabled()) {
            try {
                String tbl = db.getPrefix() + "playerstats";
                db.update("INSERT INTO " + tbl + " (uuid,name,first_join,last_seen,playtime) VALUES (?,?,?,?,?) "
                        + "ON DUPLICATE KEY UPDATE name=VALUES(name),last_seen=VALUES(last_seen),playtime=VALUES(playtime)",
                        uuid.toString(), s[3], s[0], s[1], s[2]);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to save playerstat to MySQL: " + e.getMessage());
            }
        } else {
            if (data == null) data = new YamlConfiguration();
            String key = uuid.toString();
            data.set(key + ".firstJoin", s[0]);
            data.set(key + ".lastSeen", s[1]);
            data.set(key + ".playtime", s[2]);
            data.set(key + ".name", s[3]);
            try { data.save(file); } catch (IOException e) { e.printStackTrace(); }
        }
    }

    public Object[] get(UUID uuid) { return stats.get(uuid); }

    public Map<UUID, Object[]> getAll() { return Collections.unmodifiableMap(stats); }
}
