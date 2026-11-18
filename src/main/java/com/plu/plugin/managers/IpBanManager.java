package com.plu.plugin.managers;

import com.plu.plugin.Main;
import com.plu.plugin.storage.DatabaseManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/** IP ban system. */
public class IpBanManager {

    private final Main plugin;
    private final DatabaseManager db;
    private final File file;
    private FileConfiguration data;
    // ip -> [reason, banner, created, expires]
    private final Map<String, Object[]> bans = new HashMap<>();

    public IpBanManager(Main plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db = db;
        this.file = new File(plugin.getDataFolder(), "ipbans.yml");
    }

    public void load() {
        bans.clear();
        if (db.isEnabled()) {
            try {
                String tbl = db.getPrefix() + "ipbans";
                for (Object[] row : db.query("SELECT ip,reason,banner,created,expires FROM " + tbl)) {
                    bans.put((String) row[0], new Object[]{row[1], row[2], row[3], row[4]});
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load ipbans from MySQL: " + e.getMessage());
            }
        } else {
            if (!file.exists()) return;
            data = YamlConfiguration.loadConfiguration(file);
            for (String ip : data.getKeys(false)) {
                var s = data.getConfigurationSection(ip);
                bans.put(ip, new Object[]{s.getString("reason"), s.getString("banner"), s.getLong("created"), s.getLong("expires")});
            }
        }
    }

    public void ban(String ip, String reason, String banner, long expires) {
        bans.put(ip, new Object[]{reason, banner, System.currentTimeMillis(), expires});
        if (db.isEnabled()) {
            try {
                String tbl = db.getPrefix() + "ipbans";
                db.update("INSERT INTO " + tbl + " (ip,reason,banner,created,expires) VALUES (?,?,?,?,?) "
                        + "ON DUPLICATE KEY UPDATE reason=VALUES(reason),banner=VALUES(banner),created=VALUES(created),expires=VALUES(expires)",
                        ip, reason, banner, System.currentTimeMillis(), expires);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to save ipban to MySQL: " + e.getMessage());
            }
        } else {
            if (data == null) data = new YamlConfiguration();
            data.set(ip + ".reason", reason);
            data.set(ip + ".banner", banner);
            data.set(ip + ".created", System.currentTimeMillis());
            data.set(ip + ".expires", expires);
            try { data.save(file); } catch (IOException e) { e.printStackTrace(); }
        }
    }

    public boolean unban(String ip) {
        if (!bans.containsKey(ip)) return false;
        bans.remove(ip);
        if (db.isEnabled()) {
            try { db.update("DELETE FROM " + db.getPrefix() + "ipbans WHERE ip=?", ip); }
            catch (Exception ignored) {}
        } else {
            if (data != null) data.set(ip, null);
            try { if (data != null) data.save(file); } catch (IOException e) { e.printStackTrace(); }
        }
        return true;
    }

    public Object[] getBan(String ip) { return bans.get(ip); }

    public boolean isBanned(String ip) {
        Object[] ban = bans.get(ip);
        if (ban == null) return false;
        long expires = (Long) ban[3];
        if (expires > 0 && System.currentTimeMillis() > expires) {
            unban(ip);
            return false;
        }
        return true;
    }

    public Map<String, Object[]> getAll() { return Collections.unmodifiableMap(bans); }
}
