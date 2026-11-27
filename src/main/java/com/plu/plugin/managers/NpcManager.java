package com.plu.plugin.managers;

import com.plu.plugin.Main;
import com.plu.plugin.storage.DatabaseManager;
import com.plu.plugin.util.Values;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * NPC 系统：内置实体 NPC（无 AI）+ 可选 Citizens 反射联动。
 * NPC 支持执行命令、打开菜单、发送消息。
 */
public class NpcManager {

    public static class Npc {
        public String id;
        public String name = "NPC";
        public String type = "villager";
        public String world;
        public double x, y, z;
        public float yaw, pitch;
        public String actionType = "command";
        public String actionValue = "";
        public String skin = "";
        public boolean citizens = false;
        public int citizensId = -1;
        public String entityUuid = "";
    }

    private final Main plugin;
    private final DatabaseManager db;
    private final File file;
    private final Map<String, Npc> npcs = new LinkedHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public NpcManager(Main plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db = db;
        this.file = new File(plugin.getDataFolder(), "npcs.yml");
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("npcs.enabled", true);
    }

    public boolean citizensInstalled() {
        return Bukkit.getPluginManager().getPlugin("Citizens") != null;
    }

    // ---------------- 生命周期 ----------------

    public void load() {
        npcs.clear();
        if (db.isEnabled()) {
            try {
                for (Object[] r : db.query("SELECT id,name,type,world,x,y,z,yaw,pitch,action_type,action_value,skin,citizens,citizens_id,entity_uuid FROM "
                        + db.getPrefix() + "npcs")) {
                    Npc n = new Npc();
                    n.id = String.valueOf(r[0]);
                    n.name = r[1] == null ? "NPC" : String.valueOf(r[1]);
                    n.type = r[2] == null ? "villager" : String.valueOf(r[2]);
                    n.world = r[3] == null ? "" : String.valueOf(r[3]);
                    n.x = Values.asDouble(r[4], 0);
                    n.y = Values.asDouble(r[5], 0);
                    n.z = Values.asDouble(r[6], 0);
                    n.yaw = (float) Values.asDouble(r[7], 0);
                    n.pitch = (float) Values.asDouble(r[8], 0);
                    n.actionType = r[9] == null ? "command" : String.valueOf(r[9]);
                    n.actionValue = r[10] == null ? "" : String.valueOf(r[10]);
                    n.skin = r[11] == null ? "" : String.valueOf(r[11]);
                    n.citizens = Values.asBool(r[12], false);
                    n.citizensId = Values.asInt(r[13], -1);
                    n.entityUuid = r[14] == null ? "" : String.valueOf(r[14]);
                    npcs.put(n.id, n);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load NPCs from MySQL: " + e.getMessage());
            }
            return;
        }

        if (!file.exists()) return;
        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = data.getConfigurationSection("npcs");
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) continue;
            Npc n = new Npc();
            n.id = id;
            n.name = s.getString("name", "NPC");
            n.type = s.getString("type", "villager");
            n.world = s.getString("world", "");
            n.x = s.getDouble("x");
            n.y = s.getDouble("y");
            n.z = s.getDouble("z");
            n.yaw = (float) s.getDouble("yaw");
            n.pitch = (float) s.getDouble("pitch");
            n.actionType = s.getString("action-type", "command");
            n.actionValue = s.getString("action-value", "");
            n.skin = s.getString("skin", "");
            n.citizens = s.getBoolean("citizens", false);
            n.citizensId = s.getInt("citizens-id", -1);
            n.entityUuid = s.getString("entity-uuid", "");
            npcs.put(id, n);
        }
    }

    public void save() {
        if (db.isEnabled()) return;
        YamlConfiguration data = new YamlConfiguration();
        for (Npc n : npcs.values()) {
            String base = "npcs." + n.id;
            data.set(base + ".name", n.name);
            data.set(base + ".type", n.type);
            data.set(base + ".world", n.world);
            data.set(base + ".x", n.x);
            data.set(base + ".y", n.y);
            data.set(base + ".z", n.z);
            data.set(base + ".yaw", (double) n.yaw);
            data.set(base + ".pitch", (double) n.pitch);
            data.set(base + ".action-type", n.actionType);
            data.set(base + ".action-value", n.actionValue);
            data.set(base + ".skin", n.skin);
            data.set(base + ".citizens", n.citizens);
            data.set(base + ".citizens-id", n.citizensId);
            data.set(base + ".entity-uuid", n.entityUuid);
        }
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save npcs.yml: " + e.getMessage());
        }
    }

    /** 启动时生成全部 NPC。 */
    public void spawnAll() {
        if (!isEnabled()) return;
        for (Npc n : npcs.values()) spawn(n);
    }

    /** 关闭时清理实体，避免重载后重复。 */
    public void despawnAll() {
        for (Npc n : npcs.values()) despawn(n);
    }

    // ---------------- CRUD ----------------

    public Npc create(String name, String type, Location location) {
        if (!isEnabled()) return null;
        Npc n = new Npc();
        n.id = newId();
        n.name = name;
        n.type = type == null || type.isEmpty() ? "villager" : type.toLowerCase();
        n.world = location.getWorld().getName();
        n.x = location.getX();
        n.y = location.getY();
        n.z = location.getZ();
        n.yaw = location.getYaw();
        n.pitch = location.getPitch();
        npcs.put(n.id, n);
        spawn(n);
        persist(n);
        return n;
    }

    public boolean remove(String id) {
        Npc n = npcs.remove(id);
        if (n == null) return false;
        despawn(n);
        if (db.isEnabled()) {
            try {
                db.update("DELETE FROM " + db.getPrefix() + "npcs WHERE id=?", id);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to delete NPC: " + e.getMessage());
            }
        } else {
            save();
        }
        return true;
    }

    public Npc get(String id) {
        return npcs.get(id);
    }

    public Map<String, Npc> all() {
        return new LinkedHashMap<>(npcs);
    }

    public Npc findByEntity(Entity entity) {
        if (entity == null) return null;
        String uuid = entity.getUniqueId().toString();
        for (Npc n : npcs.values()) {
            if (uuid.equals(n.entityUuid)) return n;
        }
        return null;
    }

    public void update(Npc n) {
        persist(n);
    }

    // ---------------- 生成 ----------------

    public void spawn(Npc n) {
        if (n == null) return;
        despawn(n);

        boolean wantCitizens = "player".equalsIgnoreCase(n.type)
                || plugin.getConfig().getBoolean("npcs.use-citizens", false);
        if (wantCitizens && citizensInstalled() && spawnCitizens(n)) {
            persist(n);
            return;
        }
        spawnBuiltin(n);
        persist(n);
    }

    public void despawn(Npc n) {
        if (n == null) return;
        if (n.citizens && n.citizensId >= 0 && citizensInstalled()) {
            try {
                Class<?> api = Class.forName("net.citizensnpcs.api.CitizensAPI");
                Object registry = api.getMethod("getNPCRegistry").invoke(null);
                Object npc = registry.getClass().getMethod("getById", int.class).invoke(registry, n.citizensId);
                if (npc != null) npc.getClass().getMethod("destroy").invoke(npc);
            } catch (Throwable ignored) {}
            n.citizens = false;
            n.citizensId = -1;
            n.entityUuid = "";
            return;
        }
        if (n.entityUuid != null && !n.entityUuid.isEmpty()) {
            try {
                UUID uuid = UUID.fromString(n.entityUuid);
                Entity entity = Bukkit.getEntity(uuid);
                if (entity == null) {
                    World world = Bukkit.getWorld(n.world);
                    if (world != null) {
                        world.getChunkAt(new Location(world, n.x, n.y, n.z)).load();
                        entity = Bukkit.getEntity(uuid);
                    }
                }
                if (entity != null) entity.remove();
            } catch (Exception ignored) {}
        }
        n.entityUuid = "";
    }

    private void spawnBuiltin(Npc n) {
        World world = Bukkit.getWorld(n.world);
        if (world == null) {
            plugin.getLogger().warning("NPC " + n.id + " world not found: " + n.world);
            return;
        }
        EntityType type = switch (n.type == null ? "villager" : n.type.toLowerCase()) {
            case "armor_stand" -> EntityType.ARMOR_STAND;
            case "zombie" -> EntityType.ZOMBIE;
            case "skeleton" -> EntityType.SKELETON;
            case "blaze" -> EntityType.BLAZE;
            default -> EntityType.VILLAGER;
        };
        Location location = new Location(world, n.x, n.y, n.z, n.yaw, n.pitch);
        try {
            Entity entity = world.spawnEntity(location, type);
            entity.setSilent(true);
            entity.setInvulnerable(true);
            entity.setPersistent(true);
            entity.setCustomName(Msg.color(n.name));
            entity.setCustomNameVisible(true);
            entity.setGravity(false);
            if (entity instanceof Mob mob) {
                mob.setAI(false);
                mob.setRemoveWhenFarAway(false);
            }
            if (entity instanceof LivingEntity living) {
                living.setCollidable(false);
                if (living.getEquipment() != null) living.getEquipment().clear();
            }
            if (entity instanceof ArmorStand stand) {
                stand.setArms(true);
                stand.setBasePlate(false);
                stand.setMarker(false);
            }
            n.entityUuid = entity.getUniqueId().toString();
            n.citizens = false;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to spawn NPC " + n.id + ": " + e.getMessage());
        }
    }

    private boolean spawnCitizens(Npc n) {
        if (!citizensInstalled()) return false;
        World world = Bukkit.getWorld(n.world);
        if (world == null) return false;
        try {
            Class<?> api = Class.forName("net.citizensnpcs.api.CitizensAPI");
            Object registry = api.getMethod("getNPCRegistry").invoke(null);
            EntityType type = "player".equalsIgnoreCase(n.type) ? EntityType.PLAYER : EntityType.VILLAGER;
            Object npc = registry.getClass().getMethod("createNPC", EntityType.class, String.class)
                    .invoke(registry, type, n.name);
            Location location = new Location(world, n.x, n.y, n.z, n.yaw, n.pitch);
            npc.getClass().getMethod("spawn", Location.class).invoke(npc, location);
            n.citizensId = Values.asInt(npc.getClass().getMethod("getId").invoke(npc), -1);
            Object entityObj = npc.getClass().getMethod("getEntity").invoke(npc);
            if (entityObj instanceof Entity entity) n.entityUuid = entity.getUniqueId().toString();
            try {
                npc.getClass().getMethod("setName", String.class).invoke(npc, Msg.color(n.name));
            } catch (Throwable ignored) {}
            if (n.skin != null && !n.skin.isEmpty()) {
                try {
                    Class<?> metaClass = Class.forName("net.citizensnpcs.api.npc.NPC$Metadata");
                    Object key = metaClass.getField("SKIN").get(null);
                    Object data = npc.getClass().getMethod("data").invoke(npc);
                    data.getClass().getMethod("set", metaClass, Object.class).invoke(data, key, n.skin);
                } catch (Throwable ignored) {}
            }
            n.citizens = true;
            return true;
        } catch (Throwable t) {
            plugin.getLogger().warning("Citizens NPC failed (" + n.name + "): " + t);
            return false;
        }
    }

    // ---------------- 交互 ----------------

    public void interact(Player player, Npc n) {
        if (n == null || n.actionValue == null || n.actionValue.isEmpty()) return;
        String value = n.actionValue
                .replace("{player}", player.getName())
                .replace("{nick}", plugin.getNicknameManager().display(player.getUniqueId()));
        switch (n.actionType == null ? "command" : n.actionType.toLowerCase()) {
            case "menu" -> {
                if (!plugin.getMenuManager().open(player, value)) {
                    Msg.send(player, plugin.getPrefix() + "&c菜单不存在: " + value);
                }
            }
            case "message" -> Msg.send(player, Msg.color(value));
            default -> {
                if (value.startsWith("console:")) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), value.substring(8).trim());
                } else {
                    player.performCommand(value.startsWith("/") ? value.substring(1) : value);
                }
            }
        }
    }

    // ---------------- 持久化 ----------------

    private void persist(Npc n) {
        if (n == null) return;
        if (db.isEnabled()) {
            try {
                db.update("INSERT INTO " + db.getPrefix()
                                + "npcs (id,name,type,world,x,y,z,yaw,pitch,action_type,action_value,skin,citizens,citizens_id,entity_uuid) "
                                + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) "
                                + "ON DUPLICATE KEY UPDATE name=VALUES(name),type=VALUES(type),world=VALUES(world),"
                                + "x=VALUES(x),y=VALUES(y),z=VALUES(z),yaw=VALUES(yaw),pitch=VALUES(pitch),"
                                + "action_type=VALUES(action_type),action_value=VALUES(action_value),skin=VALUES(skin),"
                                + "citizens=VALUES(citizens),citizens_id=VALUES(citizens_id),entity_uuid=VALUES(entity_uuid)",
                        n.id, n.name, n.type, n.world, n.x, n.y, n.z, n.yaw, n.pitch,
                        n.actionType, n.actionValue, n.skin, n.citizens, n.citizensId, n.entityUuid);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to save NPC: " + e.getMessage());
            }
            return;
        }
        save();
    }

    private String newId() {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        while (true) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) sb.append(chars.charAt(random.nextInt(chars.length())));
            String id = sb.toString();
            if (!npcs.containsKey(id)) return id;
        }
    }

    /** 供网页使用的简洁视图。 */
    public List<Map<String, Object>> listForWeb() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Npc n : npcs.values()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", n.id);
            m.put("name", n.name);
            m.put("type", n.type);
            m.put("world", n.world);
            m.put("x", n.x);
            m.put("y", n.y);
            m.put("z", n.z);
            m.put("actionType", n.actionType);
            m.put("actionValue", n.actionValue);
            m.put("skin", n.skin);
            m.put("citizens", n.citizens);
            list.add(m);
        }
        return list;
    }
}
