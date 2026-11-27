package com.plu.plugin.managers;

import com.plu.plugin.Main;
import com.plu.plugin.storage.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 中文昵称系统。昵称只影响显示（聊天 / TAB），
 * 真实登录名保持不变，因此兼容原版客户端。
 * 同时记录玩家名，便于中文 ID 的全链路解析。
 */
public class NicknameManager {

    private final Main plugin;
    private final DatabaseManager db;
    private final File file;

    /** uuid -> 昵称（原始文本，可能含 & 颜色代码） */
    private final Map<UUID, String> nicknames = new ConcurrentHashMap<>();
    /** uuid -> 最近一次已知玩家名 */
    private final Map<UUID, String> names = new ConcurrentHashMap<>();

    public NicknameManager(Main plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db = db;
        this.file = new File(plugin.getDataFolder(), "nicknames.yml");
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("nickname.enabled", true);
    }

    public int maxLength() {
        return Math.max(1, Math.min(32, plugin.getConfig().getInt("nickname.max-length", 16)));
    }

    public void load() {
        nicknames.clear();
        names.clear();

        if (db.isEnabled()) {
            try {
                for (Object[] r : db.query("SELECT uuid,name,nick FROM " + db.getPrefix() + "nicknames")) {
                    try {
                        UUID uuid = UUID.fromString(String.valueOf(r[0]));
                        names.put(uuid, r[1] == null ? "" : String.valueOf(r[1]));
                        if (r[2] != null && !String.valueOf(r[2]).isEmpty()) {
                            nicknames.put(uuid, String.valueOf(r[2]));
                        }
                    } catch (IllegalArgumentException ignored) {}
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load nicknames from MySQL: " + e.getMessage());
            }
            return;
        }

        if (!file.exists()) return;
        FileConfiguration data = YamlConfiguration.loadConfiguration(file);
        for (String key : data.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                ConfigurationSection section = data.getConfigurationSection(key);
                if (section == null) continue;
                names.put(uuid, section.getString("name", ""));
                String nick = section.getString("nick", "");
                if (nick != null && !nick.isEmpty()) nicknames.put(uuid, nick);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public void save() {
        if (db.isEnabled()) return;
        YamlConfiguration data = new YamlConfiguration();
        for (Map.Entry<UUID, String> e : nicknames.entrySet()) {
            String key = e.getKey().toString();
            data.set(key + ".name", names.getOrDefault(e.getKey(), ""));
            data.set(key + ".nick", e.getValue());
        }
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save nicknames.yml: " + e.getMessage());
        }
    }

    public String nickname(UUID uuid) {
        if (uuid == null || !isEnabled()) return null;
        return nicknames.get(uuid);
    }

    /** 显示名：有昵称用昵称，否则用真实玩家名。 */
    public String display(UUID uuid) {
        String nick = nickname(uuid);
        if (nick != null && !nick.isEmpty()) return nick;
        String name = names.get(uuid);
        if (name != null && !name.isEmpty()) return name;
        Player online = Bukkit.getPlayer(uuid);
        return online == null ? "" : online.getName();
    }

    /** 设置昵称；nick 为空表示清除。 */
    public boolean set(UUID uuid, String playerName, String nick) {
        if (uuid == null) return false;
        names.put(uuid, playerName == null ? "" : playerName);

        String clean = nick == null ? "" : nick.trim();
        if (clean.isEmpty()) {
            nicknames.remove(uuid);
            persist(uuid, playerName, "");
            return true;
        }
        if (clean.codePointCount(0, clean.length()) > maxLength()) return false;
        // 昵称内不允许出现换行，避免破坏聊天/TAB 布局
        clean = clean.replace("\n", "").replace("\r", "");
        nicknames.put(uuid, clean);
        persist(uuid, playerName, clean);
        return true;
    }

    public void remove(UUID uuid) {
        nicknames.remove(uuid);
        persist(uuid, names.get(uuid), "");
    }

    /** 登录/改昵称后刷新显示。 */
    public void apply(Player player) {
        if (player == null) return;
        names.put(player.getUniqueId(), player.getName());
        String nick = nickname(player.getUniqueId());
        if (nick == null || nick.isEmpty()) {
            player.setDisplayName(player.getName());
            try {
                player.setPlayerListName(player.getName());
            } catch (IllegalArgumentException ignored) {}
            return;
        }
        String colored = Msg.color(nick);
        player.setDisplayName(colored);
        try {
            player.setPlayerListName(colored.length() > 16 ? colored.substring(0, 16) : colored);
        } catch (IllegalArgumentException ignored) {}
    }

    /** 按名字（含中文）查找 UUID，先查本插件记录，再回退 Bukkit。 */
    public UUID uuidByName(String name) {
        if (name == null || name.isEmpty()) return null;
        for (Map.Entry<UUID, String> e : names.entrySet()) {
            if (name.equalsIgnoreCase(e.getValue())) return e.getKey();
        }
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online.getUniqueId();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (name.equalsIgnoreCase(p.getName())) return p.getUniqueId();
        }
        try {
            @SuppressWarnings("deprecation")
            var op = Bukkit.getOfflinePlayer(name);
            return op == null ? null : op.getUniqueId();
        } catch (Exception ignored) {
            return null;
        }
    }

    public Map<UUID, String> all() {
        return new LinkedHashMap<>(nicknames);
    }

    /** 记录真实玩家名（用于中文 ID 解析）。 */
    public void rememberName(UUID uuid, String name) {
        if (uuid == null || name == null) return;
        names.put(uuid, name);
    }

    public String knownName(UUID uuid) {
        return names.get(uuid);
    }

    private void persist(UUID uuid, String name, String nick) {
        if (db.isEnabled()) {
            try {
                if (nick == null || nick.isEmpty()) {
                    db.update("DELETE FROM " + db.getPrefix() + "nicknames WHERE uuid=?", uuid.toString());
                } else {
                    db.update("INSERT INTO " + db.getPrefix()
                                    + "nicknames (uuid,name,nick,updated) VALUES (?,?,?,?) "
                                    + "ON DUPLICATE KEY UPDATE name=VALUES(name),nick=VALUES(nick),updated=VALUES(updated)",
                            uuid.toString(), name == null ? "" : name, nick, System.currentTimeMillis());
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to save nickname: " + e.getMessage());
            }
            return;
        }
        save();
    }
}
