package com.plu.plugin.managers;

import com.plu.plugin.Main;
import com.plu.plugin.storage.DatabaseManager;
import com.plu.plugin.util.Values;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * 内置权限组系统：分组、组权限、玩家分组，并把组权限注入 Bukkit 权限体系。
 * 不依赖 LuckPerms 或任何外部权限插件。
 */
public class GroupManager {

    public static class Group {
        public final String name;
        public String prefix;
        public int weight;
        public final Set<String> permissions = new LinkedHashSet<>();

        public Group(String name) { this.name = name; }
    }

    public static final String DEFAULT_GROUP = "default";

    private final Main plugin;
    private final DatabaseManager db;
    private final File file;
    private FileConfiguration data;

    private final Map<String, Group> groups = new LinkedHashMap<>();
    private final Map<UUID, Set<String>> playerGroups = new HashMap<>();
    private final Map<UUID, PermissionAttachment> attachments = new HashMap<>();

    public GroupManager(Main plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db = db;
        this.file = new File(plugin.getDataFolder(), "groups.yml");
    }

    // ---------------- 生命周期 ----------------

    public void load() {
        groups.clear();
        playerGroups.clear();

        if (db.isEnabled()) {
            try {
                for (Object[] r : db.query("SELECT name,prefix,weight,permissions FROM " + db.getPrefix() + "groups")) {
                    Group g = new Group((String) r[0]);
                    g.prefix = r[1] == null ? "" : (String) r[1];
                    g.weight = Values.asInt(r[2], 0);
                    g.permissions.addAll(splitPermissions((String) r[3]));
                    groups.put(g.name.toLowerCase(Locale.ROOT), g);
                }
                for (Object[] r : db.query("SELECT uuid,group_name FROM " + db.getPrefix() + "player_groups")) {
                    try {
                        UUID uuid = UUID.fromString((String) r[0]);
                        playerGroups.computeIfAbsent(uuid, k -> new LinkedHashSet<>())
                                .add(String.valueOf(r[1]).toLowerCase(Locale.ROOT));
                    } catch (IllegalArgumentException ignored) {}
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load groups from MySQL: " + e.getMessage());
            }
        } else if (file.exists()) {
            data = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection gs = data.getConfigurationSection("groups");
            if (gs != null) {
                for (String key : gs.getKeys(false)) {
                    Group g = new Group(key.toLowerCase(Locale.ROOT));
                    g.prefix = gs.getString(key + ".prefix", "");
                    g.weight = gs.getInt(key + ".weight", 0);
                    g.permissions.addAll(gs.getStringList(key + ".permissions"));
                    groups.put(g.name, g);
                }
            }
            ConfigurationSection ps = data.getConfigurationSection("players");
            if (ps != null) {
                for (String key : ps.getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(key);
                        Set<String> set = new LinkedHashSet<>();
                        for (String n : ps.getStringList(key)) set.add(n.toLowerCase(Locale.ROOT));
                        if (!set.isEmpty()) playerGroups.put(uuid, set);
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        }

        // 保证默认组存在
        if (!groups.containsKey(DEFAULT_GROUP)) {
            Group def = new Group(DEFAULT_GROUP);
            def.prefix = "";
            def.weight = 0;
            groups.put(DEFAULT_GROUP, def);
            persistGroup(def);
        }
    }

    public void flush() {
        if (db.isEnabled() || data == null) return;
        try { data.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    // ---------------- 分组操作 ----------------

    public Collection<Group> getAll() {
        List<Group> list = new ArrayList<>(groups.values());
        list.sort((a, b) -> Integer.compare(b.weight, a.weight));
        return list;
    }

    public Group get(String name) {
        return name == null ? null : groups.get(name.toLowerCase(Locale.ROOT));
    }

    public boolean exists(String name) {
        return get(name) != null;
    }

    /** 创建分组；名称非法或已存在返回 null。 */
    public Group create(String name, String prefix, int weight) {
        if (name == null) return null;
        String key = name.trim().toLowerCase(Locale.ROOT);
        if (key.isEmpty() || !key.matches("[a-z0-9_\\-]{1,32}")) return null;
        if (groups.containsKey(key)) return null;

        Group g = new Group(key);
        g.prefix = prefix == null ? "" : prefix;
        g.weight = weight;
        groups.put(key, g);
        persistGroup(g);
        return g;
    }

    public boolean delete(String name) {
        String key = name == null ? null : name.trim().toLowerCase(Locale.ROOT);
        if (key == null || key.equals(DEFAULT_GROUP) || !groups.containsKey(key)) return false;
        groups.remove(key);

        // 解除所有玩家的该分组
        List<UUID> affected = new ArrayList<>();
        for (Map.Entry<UUID, Set<String>> e : playerGroups.entrySet()) {
            if (e.getValue().remove(key)) affected.add(e.getKey());
        }
        if (db.isEnabled()) {
            try {
                db.update("DELETE FROM " + db.getPrefix() + "groups WHERE name=?", key);
                db.update("DELETE FROM " + db.getPrefix() + "player_groups WHERE group_name=?", key);
            } catch (Exception ignored) {}
        } else {
            saveYaml();
        }
        for (UUID uuid : affected) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) applyTo(p);
        }
        return true;
    }

    public boolean setPrefix(String name, String prefix) {
        Group g = get(name);
        if (g == null) return false;
        g.prefix = prefix == null ? "" : prefix;
        persistGroup(g);
        return true;
    }

    public boolean setWeight(String name, int weight) {
        Group g = get(name);
        if (g == null) return false;
        g.weight = weight;
        persistGroup(g);
        return true;
    }

    // ---------------- 权限节点 ----------------

    public boolean addPermission(String groupName, String permission) {
        Group g = get(groupName);
        if (g == null || permission == null) return false;
        String perm = permission.trim();
        if (perm.isEmpty()) return false;
        if (!g.permissions.add(perm)) return false;
        persistGroup(g);
        refreshMembers(g.name);
        return true;
    }

    public boolean removePermission(String groupName, String permission) {
        Group g = get(groupName);
        if (g == null || permission == null) return false;
        if (!g.permissions.remove(permission.trim())) return false;
        persistGroup(g);
        refreshMembers(g.name);
        return true;
    }

    // ---------------- 玩家分组 ----------------

    public boolean addPlayer(UUID uuid, String groupName) {
        Group g = get(groupName);
        if (uuid == null || g == null) return false;
        Set<String> set = playerGroups.computeIfAbsent(uuid, k -> new LinkedHashSet<>());
        if (!set.add(g.name)) return false;
        persistPlayerGroups(uuid);
        Player p = Bukkit.getPlayer(uuid);
        if (p != null) applyTo(p);
        return true;
    }

    public boolean removePlayer(UUID uuid, String groupName) {
        Group g = get(groupName);
        if (uuid == null || g == null) return false;
        Set<String> set = playerGroups.get(uuid);
        if (set == null || !set.remove(g.name)) return false;
        if (set.isEmpty()) playerGroups.remove(uuid);
        persistPlayerGroups(uuid);
        Player p = Bukkit.getPlayer(uuid);
        if (p != null) applyTo(p);
        return true;
    }

    /** 玩家所属分组（按权重降序）。 */
    public List<String> getGroups(UUID uuid) {
        Set<String> set = playerGroups.get(uuid);
        if (set == null || set.isEmpty()) return new ArrayList<>(List.of(DEFAULT_GROUP));
        List<Group> list = new ArrayList<>();
        for (String name : set) {
            Group g = groups.get(name);
            if (g != null) list.add(g);
        }
        if (list.isEmpty()) return new ArrayList<>(List.of(DEFAULT_GROUP));
        list.sort((a, b) -> Integer.compare(b.weight, a.weight));
        List<String> names = new ArrayList<>();
        for (Group g : list) names.add(g.name);
        return names;
    }

    /** 主分组 = 权重最高的分组，无分组时为 default。 */
    public String getPrimaryGroup(UUID uuid) {
        List<String> list = getGroups(uuid);
        return list.isEmpty() ? DEFAULT_GROUP : list.get(0);
    }

    public String getPrefix(UUID uuid) {
        Group g = get(getPrimaryGroup(uuid));
        return g == null || g.prefix == null ? "" : g.prefix;
    }

    /** 玩家通过分组获得的全部权限（含 "-" 前缀的否定节点）。 */
    public Set<String> effectivePermissions(UUID uuid) {
        Set<String> result = new LinkedHashSet<>();
        Set<String> set = playerGroups.get(uuid);
        List<String> names = new ArrayList<>();
        if (set != null) names.addAll(set);
        names.add(DEFAULT_GROUP);
        for (String name : names) {
            Group g = groups.get(name);
            if (g != null) result.addAll(g.permissions);
        }
        return result;
    }

    /** 把分组权限注入玩家（覆盖上一次注入）。 */
    public void applyTo(Player player) {
        clear(player);
        Set<String> perms = effectivePermissions(player.getUniqueId());
        if (perms.isEmpty()) return;
        try {
            PermissionAttachment attachment = player.addAttachment(plugin);
            for (String raw : perms) {
                String perm = raw.trim();
                if (perm.isEmpty()) continue;
                if (perm.startsWith("-")) {
                    attachment.setPermission(perm.substring(1), false);
                } else {
                    attachment.setPermission(perm, true);
                }
            }
            attachments.put(player.getUniqueId(), attachment);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to apply permissions for " + player.getName() + ": " + e.getMessage());
        }
    }

    public void clear(Player player) {
        PermissionAttachment old = attachments.remove(player.getUniqueId());
        if (old == null) return;
        try {
            player.removeAttachment(old);
        } catch (Exception ignored) {}
    }

    private void refreshMembers(String groupName) {
        for (Map.Entry<UUID, Set<String>> e : playerGroups.entrySet()) {
            if (!e.getValue().contains(groupName)) continue;
            Player p = Bukkit.getPlayer(e.getKey());
            if (p != null) applyTo(p);
        }
    }

    // ---------------- 持久化 ----------------

    private void persistGroup(Group g) {
        if (db.isEnabled()) {
            try {
                db.update("INSERT INTO " + db.getPrefix()
                                + "groups (name,prefix,weight,permissions) VALUES (?,?,?,?) "
                                + "ON DUPLICATE KEY UPDATE prefix=VALUES(prefix),weight=VALUES(weight),permissions=VALUES(permissions)",
                        g.name, g.prefix, g.weight, String.join(",", g.permissions));
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to save group: " + e.getMessage());
            }
            return;
        }
        saveYaml();
    }

    private void persistPlayerGroups(UUID uuid) {
        if (db.isEnabled()) {
            try {
                db.update("DELETE FROM " + db.getPrefix() + "player_groups WHERE uuid=?", uuid.toString());
                Set<String> set = playerGroups.get(uuid);
                if (set != null) {
                    for (String name : set) {
                        db.update("INSERT INTO " + db.getPrefix() + "player_groups (uuid,group_name) VALUES (?,?)",
                                uuid.toString(), name);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to save player groups: " + e.getMessage());
            }
            return;
        }
        saveYaml();
    }

    private void saveYaml() {
        if (db.isEnabled()) return;
        if (data == null) data = new YamlConfiguration();
        data.set("groups", null);
        data.set("players", null);
        for (Group g : groups.values()) {
            String base = "groups." + g.name + ".";
            data.set(base + "prefix", g.prefix);
            data.set(base + "weight", g.weight);
            data.set(base + "permissions", new ArrayList<>(g.permissions));
        }
        for (Map.Entry<UUID, Set<String>> e : playerGroups.entrySet()) {
            data.set("players." + e.getKey(), new ArrayList<>(e.getValue()));
        }
        try { data.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    private List<String> splitPermissions(String csv) {
        if (csv == null || csv.trim().isEmpty()) return List.of();
        List<String> out = new ArrayList<>();
        for (String s : csv.split(",")) {
            String v = s.trim();
            if (!v.isEmpty()) out.add(v);
        }
        return out;
    }

    public int groupCount() {
        return groups.size();
    }

    /** 某分组的成员数量（用于后台展示）。 */
    public int memberCount(String groupName) {
        String key = groupName == null ? "" : groupName.toLowerCase(Locale.ROOT);
        int count = 0;
        for (Set<String> set : playerGroups.values()) {
            if (set.contains(key)) count++;
        }
        // default 组包含所有未显式分组的玩家，这里只统计显式成员
        return count;
    }
}
