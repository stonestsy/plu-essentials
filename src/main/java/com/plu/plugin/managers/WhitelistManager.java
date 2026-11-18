package com.plu.plugin.managers;

import com.plu.plugin.Main;
import com.plu.plugin.storage.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/** Whitelist system. */
public class WhitelistManager {

    private final Main plugin;
    private final DatabaseManager db;
    private final File file;
    private FileConfiguration data;
    private final Set<UUID> whitelist = new HashSet<>();
    private boolean enabled = false;

    public WhitelistManager(Main plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db = db;
        this.file = new File(plugin.getDataFolder(), "whitelist.yml");
        this.enabled = plugin.getConfig().getBoolean("whitelist.enabled", false);
    }

    public void load() {
        whitelist.clear();
        if (db.isEnabled()) {
            try {
                String tbl = db.getPrefix() + "whitelist";
                for (Object[] row : db.query("SELECT uuid FROM " + tbl)) {
                    whitelist.add(UUID.fromString((String) row[0]));
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load whitelist from MySQL: " + e.getMessage());
            }
        } else {
            if (!file.exists()) return;
            data = YamlConfiguration.loadConfiguration(file);
            enabled = data.getBoolean("enabled", false);
            for (String u : data.getStringList("players")) {
                try { whitelist.add(UUID.fromString(u)); } catch (Exception ignored) {}
            }
        }
    }

    private void save() {
        if (db.isEnabled()) return;
        if (data == null) data = new YamlConfiguration();
        data.set("enabled", enabled);
        data.set("players", new ArrayList<>(whitelist.stream().map(UUID::toString).toList()));
        try { data.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public boolean isEnabled() { return enabled; }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        plugin.getConfig().set("whitelist.enabled", enabled);
        plugin.saveConfig();
        if (!db.isEnabled()) save();
    }

    public boolean add(UUID uuid, String name) {
        if (whitelist.contains(uuid)) return false;
        whitelist.add(uuid);
        if (db.isEnabled()) {
            try {
                db.update("INSERT IGNORE INTO " + db.getPrefix() + "whitelist (uuid,name) VALUES (?,?)", uuid.toString(), name);
            } catch (Exception ignored) {}
        } else {
            save();
        }
        return true;
    }

    public boolean remove(UUID uuid) {
        if (!whitelist.remove(uuid)) return false;
        if (db.isEnabled()) {
            try { db.update("DELETE FROM " + db.getPrefix() + "whitelist WHERE uuid=?", uuid.toString()); }
            catch (Exception ignored) {}
        } else {
            save();
        }
        return true;
    }

    public boolean isWhitelisted(UUID uuid) { return whitelist.contains(uuid); }

    public List<String> getNames() {
        List<String> names = new ArrayList<>();
        for (UUID uuid : whitelist) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            names.add(op.getName() != null ? op.getName() : uuid.toString().substring(0, 8));
        }
        return names;
    }

    public int getCount() { return whitelist.size(); }

    public List<Map<String, String>> getAll() {
        List<Map<String, String>> list = new ArrayList<>();
        for (UUID uuid : whitelist) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            Map<String, String> m = new LinkedHashMap<>();
            m.put("uuid", uuid.toString());
            m.put("name", op.getName() != null ? op.getName() : uuid.toString().substring(0, 8));
            list.add(m);
        }
        return list;
    }
}
