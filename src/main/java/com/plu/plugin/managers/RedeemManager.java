package com.plu.plugin.managers;

import com.plu.plugin.Main;
import com.plu.plugin.storage.DatabaseManager;
import com.plu.plugin.util.Values;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/** 兑换码：生成、查询、玩家兑换（一码一人一次 + 总量/时效限制）。 */
public class RedeemManager {

    /** 兑换码定义。 */
    public static class Code {
        public String code;
        public double amount;
        public int maxUses;      // 0 = 不限次数
        public int usedCount;
        public long expires;     // 0 = 永久
        public long created;
        public String creator;
        public boolean enabled;

        public Code(String code, double amount, int maxUses, int usedCount,
                    long expires, long created, String creator, boolean enabled) {
            this.code = code; this.amount = amount; this.maxUses = maxUses;
            this.usedCount = usedCount; this.expires = expires;
            this.created = created; this.creator = creator; this.enabled = enabled;
        }
    }

    /** 兑换结果。 */
    public enum Result {
        SUCCESS, NOT_FOUND, DISABLED, EXPIRED, EXHAUSTED, ALREADY_USED, NO_ECONOMY, FAILED
    }

    private final Main plugin;
    private final DatabaseManager db;
    private final File file;
    private FileConfiguration data;
    private final Map<String, Code> codes = new LinkedHashMap<>();
    /** 已使用的 "兑换码:uuid" 集合，用于防止同一玩家重复兑换。 */
    private final Set<String> usedKeys = new HashSet<>();

    public RedeemManager(Main plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db = db;
        this.file = new File(plugin.getDataFolder(), "redeem.yml");
    }

    public void load() {
        codes.clear();
        usedKeys.clear();
        if (db.isEnabled()) {
            try {
                String prefix = db.getPrefix();
                for (Object[] r : db.query("SELECT code,amount,max_uses,used_count,expires,created,creator,enabled FROM "
                        + prefix + "redeem_codes")) {
                    String code = ((String) r[0]).toUpperCase(Locale.ROOT);
                    codes.put(code, new Code(code, Values.asDouble(r[1], 0),
                            Values.asInt(r[2], 1), Values.asInt(r[3], 0),
                            Values.asLong(r[4], 0), Values.asLong(r[5], 0),
                            (String) r[6], Values.asBool(r[7], true)));
                }
                for (Object[] r : db.query("SELECT code,uuid FROM " + prefix + "redeem_uses")) {
                    usedKeys.add(((String) r[0]).toUpperCase(Locale.ROOT) + ":" + r[1]);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load redeem codes from MySQL: " + e.getMessage());
            }
            return;
        }

        if (!file.exists()) return;
        data = YamlConfiguration.loadConfiguration(file);
        for (Map<?, ?> m : data.getMapList("codes")) {
            String code = String.valueOf(m.get("code")).toUpperCase(Locale.ROOT);
            codes.put(code, new Code(code,
                    Values.asDouble(m.get("amount"), 0),
                    Values.asInt(m.get("max-uses"), 1),
                    Values.asInt(m.get("used-count"), 0),
                    Values.asLong(m.get("expires"), 0),
                    Values.asLong(m.get("created"), 0),
                    String.valueOf(m.get("creator")),
                    Values.asBool(m.get("enabled"), true)));
        }
        usedKeys.addAll(data.getStringList("uses"));
    }

    private void save() {
        if (db.isEnabled()) return;
        if (data == null) data = new YamlConfiguration();
        List<Map<String, Object>> list = new ArrayList<>();
        for (Code c : codes.values()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("code", c.code);
            m.put("amount", c.amount);
            m.put("max-uses", c.maxUses);
            m.put("used-count", c.usedCount);
            m.put("expires", c.expires);
            m.put("created", c.created);
            m.put("creator", c.creator);
            m.put("enabled", c.enabled);
            list.add(m);
        }
        data.set("codes", list);
        data.set("uses", new ArrayList<>(usedKeys));
        try { data.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    /** 创建兑换码；code 传 "#" 时自动生成 8 位随机码。返回 null 表示已存在或参数非法。 */
    public Code create(String code, double amount, int maxUses, long expires, String creator) {
        if (amount <= 0) return null;
        String key = "#".equals(code) ? generateCode() : code.trim().toUpperCase(Locale.ROOT);
        if (key.isEmpty() || codes.containsKey(key)) return null;

        Code c = new Code(key, amount, maxUses, 0, expires, System.currentTimeMillis(), creator, true);
        codes.put(key, c);
        if (db.isEnabled()) {
            try {
                db.update("INSERT INTO " + db.getPrefix()
                        + "redeem_codes (code,amount,max_uses,used_count,expires,created,creator,enabled) "
                        + "VALUES (?,?,?,?,?,?,?,?)",
                        key, amount, maxUses, 0, expires, c.created, creator, 1);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to save redeem code: " + e.getMessage());
            }
        } else {
            save();
        }
        return c;
    }

    private String generateCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        for (int attempt = 0; attempt < 20; attempt++) {
            StringBuilder sb = new StringBuilder(8);
            for (int i = 0; i < 8; i++) {
                sb.append(chars.charAt(ThreadLocalRandom.current().nextInt(chars.length())));
            }
            String candidate = sb.toString();
            if (!codes.containsKey(candidate)) return candidate;
        }
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
    }

    public boolean delete(String code) {
        String key = code.trim().toUpperCase(Locale.ROOT);
        if (codes.remove(key) == null) return false;
        if (db.isEnabled()) {
            try {
                db.update("DELETE FROM " + db.getPrefix() + "redeem_codes WHERE code=?", key);
                db.update("DELETE FROM " + db.getPrefix() + "redeem_uses WHERE code=?", key);
            } catch (Exception ignored) {}
        } else {
            usedKeys.removeIf(k -> k.startsWith(key + ":"));
            save();
        }
        return true;
    }

    /** 切换启用状态，返回切换后的状态；码不存在返回 null。 */
    public Boolean toggle(String code) {
        Code c = codes.get(code.trim().toUpperCase(Locale.ROOT));
        if (c == null) return null;
        c.enabled = !c.enabled;
        if (db.isEnabled()) {
            try {
                db.update("UPDATE " + db.getPrefix() + "redeem_codes SET enabled=? WHERE code=?",
                        c.enabled ? 1 : 0, c.code);
            } catch (Exception ignored) {}
        } else {
            save();
        }
        return c.enabled;
    }

    public Code get(String code) {
        return codes.get(code.trim().toUpperCase(Locale.ROOT));
    }

    public List<Code> getAll() {
        return new ArrayList<>(codes.values());
    }

    public int size() { return codes.size(); }

    /** 玩家兑换。 */
    public Result redeem(Player player, String rawCode) {
        if (!plugin.getEconomyManager().available()) return Result.NO_ECONOMY;

        String key = rawCode.trim().toUpperCase(Locale.ROOT);
        Code c = codes.get(key);
        if (c == null) return Result.NOT_FOUND;
        if (!c.enabled) return Result.DISABLED;

        long now = System.currentTimeMillis();
        if (c.expires > 0 && now > c.expires) return Result.EXPIRED;
        if (c.maxUses > 0 && c.usedCount >= c.maxUses) return Result.EXHAUSTED;

        String useKey = key + ":" + player.getUniqueId();
        if (usedKeys.contains(useKey)) return Result.ALREADY_USED;

        if (!plugin.getEconomyManager().deposit(player.getUniqueId(), c.amount)) return Result.FAILED;

        c.usedCount++;
        usedKeys.add(useKey);

        if (db.isEnabled()) {
            try {
                db.update("UPDATE " + db.getPrefix() + "redeem_codes SET used_count=? WHERE code=?",
                        c.usedCount, key);
                db.update("INSERT IGNORE INTO " + db.getPrefix() + "redeem_uses (code,uuid,used_at) VALUES (?,?,?)",
                        key, player.getUniqueId().toString(), now);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to record redeem use: " + e.getMessage());
            }
        } else {
            save();
        }
        return Result.SUCCESS;
    }

    /** 该玩家是否兑换过指定码。 */
    public boolean hasUsed(String code, UUID uuid) {
        return usedKeys.contains(code.trim().toUpperCase(Locale.ROOT) + ":" + uuid);
    }
}
