package com.plu.plugin.managers;

import com.plu.plugin.Main;
import com.plu.plugin.storage.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/** Ban system supporting both MySQL and YAML storage. */
public class BanManager {

    private final Main plugin;
    private final DatabaseManager db;
    private final File file;
    private FileConfiguration data;
    // in-memory cache: uuid -> [name, reason, banner, created, expires]
    private final Map<UUID, Object[]> bans = new HashMap<>();

    public BanManager(Main plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db = db;
        this.file = new File(plugin.getDataFolder(), "bans.yml");
    }

    public void load() {
        bans.clear();
        if (db.isEnabled()) {
            try {
                String tbl = db.getPrefix() + "bans";
                for (Object[] row : db.query("SELECT uuid,name,reason,banner,created,expires FROM " + tbl)) {
                    UUID uuid = UUID.fromString((String) row[0]);
                    bans.put(uuid, new Object[]{row[1], row[2], row[3], row[4], row[5]});
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load bans from MySQL: " + e.getMessage());
            }
        } else {
            if (!file.exists()) return;
            data = YamlConfiguration.loadConfiguration(file);
            for (String key : data.getKeys(false)) {
                UUID uuid = UUID.fromString(key);
                ConfigurationSection s = data.getConfigurationSection(key);
                bans.put(uuid, new Object[]{
                        s.getString("name"), s.getString("reason"),
                        s.getString("banner"), s.getLong("created"), s.getLong("expires")
                });
            }
        }
    }

    private void save() {
        if (db.isEnabled()) return; // MySQL persists immediately
        if (data == null) data = new YamlConfiguration();
        for (Map.Entry<UUID, Object[]> e : bans.entrySet()) {
            String key = e.getKey().toString();
            Object[] v = e.getValue();
            data.set(key + ".name", v[0]);
            data.set(key + ".reason", v[1]);
            data.set(key + ".banner", v[2]);
            data.set(key + ".created", v[3]);
            data.set(key + ".expires", v[4]);
        }
        try { data.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public boolean ban(UUID uuid, String name, String reason, String banner, long expires) {
        long created = System.currentTimeMillis();
        bans.put(uuid, new Object[]{name, reason, banner, created, expires});
        if (db.isEnabled()) {
            try {
                String tbl = db.getPrefix() + "bans";
                db.update("INSERT INTO " + tbl + " (uuid,name,reason,banner,created,expires) VALUES (?,?,?,?,?,?) "
                        + "ON DUPLICATE KEY UPDATE name=VALUES(name),reason=VALUES(reason),banner=VALUES(banner),created=VALUES(created),expires=VALUES(expires)",
                        uuid.toString(), name, reason, banner, created, expires);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to save ban to MySQL: " + e.getMessage());
            }
        } else {
            save();
        }
        // kick if online
        org.bukkit.entity.Player p = Bukkit.getPlayer(uuid);
        if (p != null) {
            p.kickPlayer(buildBanMessage(reason, banner, expires));
        }
        return true;
    }

    public boolean unban(UUID uuid) {
        if (!bans.containsKey(uuid)) return false;
        bans.remove(uuid);
        if (db.isEnabled()) {
            try {
                db.update("DELETE FROM " + db.getPrefix() + "bans WHERE uuid=?", uuid.toString());
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to delete ban from MySQL: " + e.getMessage());
            }
        } else {
            if (data != null) data.set(uuid.toString(), null);
            save();
        }
        return true;
    }

    public boolean isBanned(UUID uuid) {
        Object[] ban = bans.get(uuid);
        if (ban == null) return false;
        long expires = (Long) ban[4];
        if (expires > 0 && System.currentTimeMillis() > expires) {
            // expired -> remove
            unban(uuid);
            return false;
        }
        return true;
    }

    public Object[] getBan(UUID uuid) {
        return bans.get(uuid);
    }

    public Map<UUID, Object[]> getAllBans() {
        return Collections.unmodifiableMap(bans);
    }

    public String buildBanMessage(String reason, String banner, long expires) {
        StringBuilder sb = new StringBuilder();
        sb.append("§c你已被封禁！\n");
        sb.append("§7原因: §f").append(reason == null ? "未说明" : reason).append("\n");
        sb.append("§7执行者: §f").append(banner == null ? "控制台" : banner).append("\n");
        if (expires > 0) {
            long remain = expires - System.currentTimeMillis();
            sb.append("§7剩余时间: §f").append(formatDuration(remain)).append("\n");
        } else {
            sb.append("§7时长: §c永久\n");
        }
        sb.append("§7如有疑问请联系管理员。");
        return sb.toString();
    }

    public static String formatDuration(long ms) {
        if (ms <= 0) return "已过期";
        long s = ms / 1000;
        long days = s / 86400; s %= 86400;
        long hours = s / 3600; s %= 3600;
        long mins = s / 60; s %= 60;
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("天");
        if (hours > 0) sb.append(hours).append("时");
        if (mins > 0) sb.append(mins).append("分");
        if (sb.length() == 0) sb.append(s).append("秒");
        return sb.toString();
    }
}
