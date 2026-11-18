package com.plu.plugin.managers;

import com.plu.plugin.Main;
import com.plu.plugin.storage.DatabaseManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** 账号注册与登录（密码 SHA-256 加盐存储）。 */
public class LoginManager {

    private final Main plugin;
    private final DatabaseManager db;
    private final File file;
    private FileConfiguration data;
    // uuid -> [name, salt, hash, registeredMillis]
    private final Map<UUID, String[]> accounts = new HashMap<>();
    private final Set<UUID> authed = new HashSet<>();

    public LoginManager(Main plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db = db;
        this.file = new File(plugin.getDataFolder(), "accounts.yml");
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("login.enabled", true);
    }

    public void load() {
        accounts.clear();
        authed.clear();
        if (db.isEnabled()) {
            try {
                String tbl = db.getPrefix() + "accounts";
                for (Object[] r : db.query("SELECT uuid,name,salt,hash,registered FROM " + tbl)) {
                    accounts.put(UUID.fromString((String) r[0]),
                            new String[]{(String) r[1], (String) r[2], (String) r[3], String.valueOf(r[4])});
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load accounts from MySQL: " + e.getMessage());
            }
        } else {
            if (!file.exists()) return;
            data = YamlConfiguration.loadConfiguration(file);
            for (String key : data.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    accounts.put(uuid, new String[]{
                            data.getString(key + ".name", ""),
                            data.getString(key + ".salt", ""),
                            data.getString(key + ".hash", ""),
                            data.getString(key + ".registered", "0")
                    });
                } catch (Exception ignored) {}
            }
        }
    }

    public boolean isRegistered(UUID uuid) {
        return accounts.containsKey(uuid);
    }

    /** 是否需要先注册。 */
    public boolean needsRegister(UUID uuid) {
        return isEnabled() && !isRegistered(uuid);
    }

    /** 是否需要先登录（已注册但本次会话未认证）。 */
    public boolean needsLogin(UUID uuid) {
        return isEnabled() && isRegistered(uuid) && !authed.contains(uuid);
    }

    /** 是否需要被限制（未注册或未登录）。 */
    public boolean isRestricted(UUID uuid) {
        if (!isEnabled()) return false;
        return !isRegistered(uuid) || !authed.contains(uuid);
    }

    /** 注册，成功返回 true。 */
    public boolean register(UUID uuid, String name, String password) {
        if (!isEnabled()) return false;
        if (isRegistered(uuid)) return false;
        if (password == null || password.length() < 4 || password.length() > 32) return false;

        String salt = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String hash = hash(salt, password);
        String[] rec = new String[]{name, salt, hash, String.valueOf(System.currentTimeMillis())};
        accounts.put(uuid, rec);
        authed.add(uuid);

        if (db.isEnabled()) {
            try {
                db.update("INSERT INTO " + db.getPrefix() + "accounts (uuid,name,salt,hash,registered) VALUES (?,?,?,?,?)",
                        uuid.toString(), name, salt, hash, rec[3]);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to save account: " + e.getMessage());
            }
        } else {
            save();
        }
        return true;
    }

    /** 校验密码，成功则标记为已登录。 */
    public boolean authenticate(UUID uuid, String password) {
        String[] rec = accounts.get(uuid);
        if (rec == null || password == null) return false;
        String expected = rec[2];
        String actual = hash(rec[1], password);
        if (!expected.equalsIgnoreCase(actual)) return false;
        authed.add(uuid);
        return true;
    }

    public void logout(UUID uuid) {
        authed.remove(uuid);
    }

    public void markAuthed(UUID uuid) {
        authed.add(uuid);
    }

    public boolean isAuthed(UUID uuid) {
        return authed.contains(uuid);
    }

    public String getName(UUID uuid) {
        String[] rec = accounts.get(uuid);
        return rec != null ? rec[0] : null;
    }

    /** 修改密码（调用方需保证已认证）。 */
    public boolean changePassword(UUID uuid, String newPassword) {
        String[] rec = accounts.get(uuid);
        if (rec == null || newPassword == null || newPassword.length() < 4 || newPassword.length() > 32) return false;
        String salt = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String hash = hash(salt, newPassword);
        rec[1] = salt;
        rec[2] = hash;
        if (db.isEnabled()) {
            try {
                db.update("UPDATE " + db.getPrefix() + "accounts SET salt=?, hash=? WHERE uuid=?",
                        salt, hash, uuid.toString());
            } catch (Exception ignored) {}
        } else {
            save();
        }
        return true;
    }

    private void save() {
        if (db.isEnabled()) return;
        if (data == null) data = new YamlConfiguration();
        for (Map.Entry<UUID, String[]> e : accounts.entrySet()) {
            String key = e.getKey().toString();
            String[] r = e.getValue();
            data.set(key + ".name", r[0]);
            data.set(key + ".salt", r[1]);
            data.set(key + ".hash", r[2]);
            data.set(key + ".registered", r[3]);
        }
        try { data.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    private static String hash(String salt, String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update((salt + password).getBytes(StandardCharsets.UTF_8));
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
