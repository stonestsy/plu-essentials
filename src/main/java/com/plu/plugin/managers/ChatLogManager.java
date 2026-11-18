package com.plu.plugin.managers;

import com.plu.plugin.Main;
import com.plu.plugin.storage.DatabaseManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/** Chat message audit log. */
public class ChatLogManager {

    private final Main plugin;
    private final DatabaseManager db;
    private final File file;
    private FileConfiguration data;
    private final List<Object[]> cache = new ArrayList<>(); // [time, name, uuid, world, message]
    private int seq = 0;

    public ChatLogManager(Main plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db = db;
        this.file = new File(plugin.getDataFolder(), "chatlog.yml");
    }

    public void load() {
        cache.clear();
        if (db.isEnabled()) {
            try {
                String tbl = db.getPrefix() + "chatlog";
                List<Object[]> rows = db.query("SELECT time,name,uuid,world,message FROM " + tbl + " ORDER BY time DESC LIMIT 500");
                for (Object[] row : rows) cache.add(row);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load chatlog from MySQL: " + e.getMessage());
            }
        } else {
            if (!file.exists()) return;
            data = YamlConfiguration.loadConfiguration(file);
            for (String key : data.getKeys(false)) {
                var s = data.getConfigurationSection(key);
                cache.add(new Object[]{s.getLong("time"), s.getString("name"), s.getString("uuid"), s.getString("world"), s.getString("message")});
            }
        }
    }

    public void log(String name, UUID uuid, String world, String message) {
        long time = System.currentTimeMillis();
        Object[] entry = new Object[]{time, name, uuid.toString(), world, message};
        cache.add(0, entry);
        while (cache.size() > 500) cache.remove(cache.size() - 1);

        if (db.isEnabled()) {
            try {
                db.update("INSERT INTO " + db.getPrefix() + "chatlog (time,name,uuid,world,message) VALUES (?,?,?,?,?)",
                        time, name, uuid.toString(), world, message);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to save chatlog to MySQL: " + e.getMessage());
            }
        } else {
            // YAML: keep last 200
            if (data == null) data = new YamlConfiguration();
            String key = String.valueOf(seq++);
            data.set(key + ".time", time);
            data.set(key + ".name", name);
            data.set(key + ".uuid", uuid.toString());
            data.set(key + ".world", world);
            data.set(key + ".message", message);
            try { data.save(file); } catch (IOException e) { e.printStackTrace(); }
        }
    }

    public List<Object[]> getRecent(int count) {
        return cache.subList(0, Math.min(count, cache.size()));
    }

    public List<Object[]> search(String keyword, int limit) {
        List<Object[]> result = new ArrayList<>();
        String kw = keyword.toLowerCase();
        for (Object[] e : cache) {
            String msg = (String) e[4];
            String name = (String) e[1];
            if (msg.toLowerCase().contains(kw) || name.toLowerCase().contains(kw)) {
                result.add(e);
                if (result.size() >= limit) break;
            }
        }
        return result;
    }
}
