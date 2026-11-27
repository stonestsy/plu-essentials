package com.plu.plugin.managers;

import com.plu.plugin.Main;
import com.plu.plugin.storage.DatabaseManager;
import com.plu.plugin.util.PlayerResolver;
import com.plu.plugin.util.Values;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内置经济系统：余额存储、转账、排行、格式化。
 * 不依赖 Vault 或任何外部经济插件，数据落在 MySQL 的 balances 表或 balances.yml。
 */
public class EconomyManager {

    private final Main plugin;
    private final DatabaseManager db;
    private final File file;
    private FileConfiguration data;

    /** uuid -> 余额 */
    private final Map<UUID, Double> balances = new ConcurrentHashMap<>();

    private volatile List<Map.Entry<String, Double>> topCache = Collections.emptyList();
    private volatile long topCacheTime = 0L;
    private volatile boolean dirty = false;

    public EconomyManager(Main plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db = db;
        this.file = new File(plugin.getDataFolder(), "balances.yml");
    }

    // ---------------- 配置 ----------------

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("economy.enabled", true);
    }

    /** 兼容旧调用：内置经济始终可用（除非被显式关闭）。 */
    public boolean available() {
        return isEnabled();
    }

    public String currencyName() {
        return plugin.getConfig().getString("economy.currency-name", "金币");
    }

    public String currencySymbol() {
        return plugin.getConfig().getString("economy.currency-symbol", "");
    }

    public int decimals() {
        return Math.max(0, Math.min(4, plugin.getConfig().getInt("economy.decimals", 2)));
    }

    public double startingBalance() {
        return Math.max(0, plugin.getConfig().getDouble("economy.starting-balance", 0));
    }

    // ---------------- 生命周期 ----------------

    public void load() {
        flush(); // 重载前先落盘，避免丢失未保存的改动
        balances.clear();
        dirty = false;
        invalidateTop();

        if (db.isEnabled()) {
            try {
                for (Object[] r : db.query("SELECT uuid,balance FROM " + db.getPrefix() + "balances")) {
                    try {
                        balances.put(UUID.fromString((String) r[0]), Values.asDouble(r[1], 0));
                    } catch (IllegalArgumentException ignored) {}
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load balances from MySQL: " + e.getMessage());
            }
            return;
        }

        if (!file.exists()) return;
        data = YamlConfiguration.loadConfiguration(file);
        for (String key : data.getKeys(false)) {
            try {
                balances.put(UUID.fromString(key), data.getDouble(key, 0));
            } catch (IllegalArgumentException ignored) {}
        }
    }

    /** 把内存改动写入磁盘（YAML 模式由定时任务与关服时调用）。 */
    public void flush() {
        if (db.isEnabled() || !dirty || data == null) return;
        try {
            data.save(file);
            dirty = false;
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save balances.yml: " + e.getMessage());
        }
    }

    /** 玩家首次进入时按配置发放初始余额并建档。 */
    public void ensureAccount(Player player) {
        UUID uuid = player.getUniqueId();
        if (balances.containsKey(uuid)) {
            // 仅刷新名字，便于排行显示
            return;
        }
        double start = startingBalance();
        balances.put(uuid, start);
        persist(uuid, start, player.getName());
        invalidateTop();
    }

    // ---------------- 查询 ----------------

    public double balance(UUID uuid) {
        if (uuid == null) return 0;
        return balances.getOrDefault(uuid, 0d);
    }

    public double balance(String name) {
        UUID uuid = PlayerResolver.uuidByName(name);
        return uuid == null ? 0 : balance(uuid);
    }

    public boolean has(UUID uuid, double amount) {
        return balance(uuid) >= amount;
    }

    // ---------------- 变更 ----------------

    /** 入账，返回是否成功。 */
    public synchronized boolean deposit(UUID uuid, double amount) {
        if (uuid == null || amount <= 0) return false;
        setSilently(uuid, balance(uuid) + amount);
        return true;
    }

    /** 扣款；余额不足返回 false 且不产生任何变更。 */
    public synchronized boolean withdraw(UUID uuid, double amount) {
        if (uuid == null || amount <= 0) return false;
        double current = balance(uuid);
        if (current < amount) return false;
        setSilently(uuid, current - amount);
        return true;
    }

    /** 直接设置余额（管理用），负数视为非法。 */
    public synchronized boolean set(UUID uuid, double amount) {
        if (uuid == null || amount < 0) return false;
        setSilently(uuid, sanitize(amount));
        return true;
    }

    /** 转账：先扣后加，扣款失败则整体失败。 */
    public synchronized boolean transfer(UUID from, UUID to, double amount) {
        if (from == null || to == null || from.equals(to) || amount <= 0) return false;
        if (!withdraw(from, amount)) return false;
        deposit(to, amount);
        return true;
    }

    private void setSilently(UUID uuid, double amount) {
        double value = sanitize(amount);
        balances.put(uuid, value);
        persist(uuid, value, resolveName(uuid));
        invalidateTop();
    }

    private double sanitize(double amount) {
        if (Double.isNaN(amount) || Double.isInfinite(amount)) return 0;
        double scale = Math.pow(10, decimals());
        return Math.round(amount * scale) / scale;
    }

    private String resolveName(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) return online.getName();
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        return op.getName() == null ? "" : op.getName();
    }

    private void persist(UUID uuid, double balance, String name) {
        if (db.isEnabled()) {
            try {
                db.update("INSERT INTO " + db.getPrefix()
                                + "balances (uuid,name,balance,updated) VALUES (?,?,?,?) "
                                + "ON DUPLICATE KEY UPDATE name=VALUES(name),balance=VALUES(balance),updated=VALUES(updated)",
                        uuid.toString(), name == null ? "" : name, balance, System.currentTimeMillis());
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to save balance: " + e.getMessage());
            }
            return;
        }
        if (data == null) data = new YamlConfiguration();
        data.set(uuid.toString(), balance);
        dirty = true;
    }

    // ---------------- 展示 ----------------

    public String format(double amount) {
        String number = String.format(Locale.ROOT, "%,." + decimals() + "f", amount);
        String symbol = currencySymbol();
        String name = currencyName();
        String prefix = symbol.isEmpty() ? "" : symbol;
        String suffix = name.isEmpty() ? "" : name;
        if (!prefix.isEmpty()) return prefix + number + (suffix.isEmpty() ? "" : " " + suffix);
        return suffix.isEmpty() ? number : number + " " + suffix;
    }

    private void invalidateTop() {
        topCacheTime = 0L;
        topCache = Collections.emptyList();
    }

    /** 财富排行（只统计余额 > 0），缓存 5 分钟。 */
    public List<Map.Entry<String, Double>> top(int limit) {
        int want = Math.max(1, Math.min(50, limit));
        if (System.currentTimeMillis() - topCacheTime < 300_000L && topCache.size() >= want) {
            return topCache.subList(0, Math.min(want, topCache.size()));
        }
        List<Map.Entry<String, Double>> list = new ArrayList<>();
        for (Map.Entry<UUID, Double> e : balances.entrySet()) {
            double value = e.getValue() == null ? 0 : e.getValue();
            if (value <= 0) continue;
            String name = resolveName(e.getKey());
            if (name.isEmpty()) continue;
            list.add(new AbstractMap.SimpleEntry<>(name, value));
        }
        list.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        topCache = list;
        topCacheTime = System.currentTimeMillis();
        return list.subList(0, Math.min(want, list.size()));
    }

    /** 某玩家是否已有账户（用于后台判断）。 */
    public boolean hasAccount(UUID uuid) {
        return balances.containsKey(uuid);
    }

    /** 账户总数。 */
    public int accountCount() {
        return balances.size();
    }

    /** 全服货币总量，可用于经济监控。 */
    public double totalMoney() {
        double sum = 0;
        for (double v : balances.values()) sum += v;
        return sum;
    }
}
