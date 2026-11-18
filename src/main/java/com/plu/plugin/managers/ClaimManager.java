package com.plu.plugin.managers;

import com.plu.plugin.Main;
import com.plu.plugin.storage.DatabaseManager;
import com.plu.plugin.util.Values;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

/** 领地圈地（二维矩形区域保护）。 */
public class ClaimManager {

    public static class Claim {
        public int id;
        public UUID owner;
        public String ownerName;
        public String world;
        public int minX, minZ, maxX, maxZ;
        public long created;

        public Claim(int id, UUID owner, String ownerName, String world,
                     int minX, int minZ, int maxX, int maxZ, long created) {
            this.id = id; this.owner = owner; this.ownerName = ownerName; this.world = world;
            this.minX = minX; this.minZ = minZ; this.maxX = maxX; this.maxZ = maxZ;
            this.created = created;
        }

        public boolean contains(String world, int x, int z) {
            return this.world.equals(world) && x >= minX && x <= maxX && z >= minZ && z <= maxZ;
        }

        public int sizeX() { return maxX - minX + 1; }
        public int sizeZ() { return maxZ - minZ + 1; }
        public int area() { return sizeX() * sizeZ(); }
    }

    private final Main plugin;
    private final DatabaseManager db;
    private final File file;
    private FileConfiguration data;
    private final List<Claim> claims = new ArrayList<>();
    private int nextId = 1;

    public ClaimManager(Main plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db = db;
        this.file = new File(plugin.getDataFolder(), "claims.yml");
    }

    public void load() {
        claims.clear();
        nextId = 1;
        if (db.isEnabled()) {
            try {
                String tbl = db.getPrefix() + "claims";
                for (Object[] r : db.query("SELECT id,owner,owner_name,world,min_x,min_z,max_x,max_z,created FROM " + tbl)) {
                    int id = Values.asInt(r[0], 0);
                    claims.add(new Claim(id, UUID.fromString((String) r[1]), (String) r[2], (String) r[3],
                            Values.asInt(r[4], 0), Values.asInt(r[5], 0),
                            Values.asInt(r[6], 0), Values.asInt(r[7], 0),
                            Values.asLong(r[8], 0)));
                    nextId = Math.max(nextId, id + 1);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load claims from MySQL: " + e.getMessage());
            }
        } else {
            if (!file.exists()) return;
            data = YamlConfiguration.loadConfiguration(file);
            nextId = data.getInt("next-id", 1);
            for (Map<?, ?> m : data.getMapList("claims")) {
                claims.add(new Claim(Values.asInt(m.get("id"), 0),
                        UUID.fromString((String) m.get("owner")),
                        (String) m.get("owner-name"),
                        (String) m.get("world"),
                        Values.asInt(m.get("min-x"), 0),
                        Values.asInt(m.get("min-z"), 0),
                        Values.asInt(m.get("max-x"), 0),
                        Values.asInt(m.get("max-z"), 0),
                        Values.asLong(m.get("created"), 0)));
            }
        }
    }

    private void save() {
        if (db.isEnabled()) return;
        if (data == null) data = new YamlConfiguration();
        List<Map<String, Object>> list = new ArrayList<>();
        for (Claim c : claims) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.id);
            m.put("owner", c.owner.toString());
            m.put("owner-name", c.ownerName);
            m.put("world", c.world);
            m.put("min-x", c.minX);
            m.put("min-z", c.minZ);
            m.put("max-x", c.maxX);
            m.put("max-z", c.maxZ);
            m.put("created", c.created);
            list.add(m);
        }
        data.set("next-id", nextId);
        data.set("claims", list);
        try { data.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    /** 创建以玩家为中心、边长 2*radius+1 的领地；与其他领地重叠时失败返回 null。 */
    public Claim create(Player owner, int radius) {
        Location loc = owner.getLocation();
        int cx = loc.getBlockX(), cz = loc.getBlockZ();
        String world = loc.getWorld().getName();
        int minX = cx - radius, maxX = cx + radius;
        int minZ = cz - radius, maxZ = cz + radius;

        for (Claim c : claims) {
            if (!c.world.equals(world)) continue;
            if (minX <= c.maxX && maxX >= c.minX && minZ <= c.maxZ && maxZ >= c.minZ) return null;
        }

        int id = nextId++;
        Claim claim = new Claim(id, owner.getUniqueId(), owner.getName(), world, minX, minZ, maxX, maxZ,
                System.currentTimeMillis());
        claims.add(claim);
        if (db.isEnabled()) {
            try {
                db.update("INSERT INTO " + db.getPrefix() + "claims (id,owner,owner_name,world,min_x,min_z,max_x,max_z,created) VALUES (?,?,?,?,?,?,?,?,?)",
                        id, claim.owner.toString(), claim.ownerName, world, minX, minZ, maxX, maxZ, claim.created);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to save claim: " + e.getMessage());
            }
        } else {
            save();
        }
        return claim;
    }

    public boolean remove(int id) {
        Claim target = null;
        for (Claim c : claims) if (c.id == id) { target = c; break; }
        if (target == null) return false;
        claims.remove(target);
        if (db.isEnabled()) {
            try { db.update("DELETE FROM " + db.getPrefix() + "claims WHERE id=?", id); }
            catch (Exception ignored) {}
        } else {
            save();
        }
        return true;
    }

    public Claim getAt(String world, int x, int z) {
        for (Claim c : claims) if (c.contains(world, x, z)) return c;
        return null;
    }

    public Claim getAt(Location loc) {
        return getAt(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockZ());
    }

    public List<Claim> getByOwner(UUID owner) {
        List<Claim> list = new ArrayList<>();
        for (Claim c : claims) if (c.owner.equals(owner)) list.add(c);
        return list;
    }

    public List<Claim> getAll() { return Collections.unmodifiableList(claims); }

    /** 判断玩家能否在指定位置建造：自己的领地、无领地、或有 bypass 权限时允许。 */
    public boolean canBuild(Player player, Location loc) {
        if (player.hasPermission("plu.claim.bypass")) return true;
        Claim claim = getAt(loc);
        return claim == null || claim.owner.equals(player.getUniqueId());
    }

    public int maxPerPlayer() {
        return plugin.getConfig().getInt("claims.max-per-player", 2);
    }

    public int maxRadius() {
        return plugin.getConfig().getInt("claims.max-radius", 16);
    }
}
