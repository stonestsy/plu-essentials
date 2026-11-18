package com.plu.plugin.managers;

import com.plu.plugin.Main;
import com.plu.plugin.util.PlayerResolver;
import com.plu.plugin.web.hooks.VaultHook;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import net.milkbowl.vault.economy.Economy;

import java.util.*;

/** 经济系统封装：余额查询、存取、转账、排行。 */
public class EconomyManager {

    private final Main plugin;
    private volatile List<Map.Entry<String, Double>> topCache = Collections.emptyList();
    private volatile long topCacheTime = 0;

    public EconomyManager(Main plugin) {
        this.plugin = plugin;
    }

    private Economy eco() {
        return VaultHook.getEconomy();
    }

    public boolean available() {
        return VaultHook.isAvailable();
    }

    public double balance(UUID uuid) {
        Economy e = eco();
        if (e == null) return 0;
        return e.getBalance(Bukkit.getOfflinePlayer(uuid));
    }

    public double balance(String name) {
        Economy e = eco();
        if (e == null) return 0;
        OfflinePlayer op = PlayerResolver.byName(name);
        return op != null ? e.getBalance(op) : 0;
    }

    public boolean has(UUID uuid, double amount) {
        return balance(uuid) >= amount;
    }

    /** 扣款，余额不足返回 false。 */
    public boolean withdraw(UUID uuid, double amount) {
        Economy e = eco();
        if (e == null || amount <= 0) return false;
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        if (e.getBalance(op) < amount) return false;
        e.withdrawPlayer(op, amount);
        return true;
    }

    /** 入账。 */
    public boolean deposit(UUID uuid, double amount) {
        Economy e = eco();
        if (e == null || amount <= 0) return false;
        e.depositPlayer(Bukkit.getOfflinePlayer(uuid), amount);
        return true;
    }

    /** 设置余额（通过存/取差值实现）。 */
    public boolean set(UUID uuid, double amount) {
        Economy e = eco();
        if (e == null || amount < 0) return false;
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        double current = e.getBalance(op);
        if (amount >= current) e.depositPlayer(op, amount - current);
        else e.withdrawPlayer(op, current - amount);
        return true;
    }

    /** 转账，失败不扣款。 */
    public boolean transfer(UUID from, UUID to, double amount) {
        if (from.equals(to) || amount <= 0) return false;
        if (!withdraw(from, amount)) return false;
        deposit(to, amount);
        return true;
    }

    public String format(double amount) {
        Economy e = eco();
        if (e == null) return String.format("%.2f", amount);
        try {
            return e.format(amount);
        } catch (Exception ex) {
            return String.format("%.2f", amount);
        }
    }

    public String currencyName() {
        Economy e = eco();
        if (e == null) return "";
        try {
            return e.currencyNamePlural();
        } catch (Exception ex) {
            return "";
        }
    }

    /** 余额排行（5 分钟缓存，避免频繁遍历离线玩家）。 */
    public List<Map.Entry<String, Double>> top(int limit) {
        if (System.currentTimeMillis() - topCacheTime < 300_000 && topCache.size() >= limit) {
            return topCache.subList(0, Math.min(limit, topCache.size()));
        }
        Economy e = eco();
        if (e == null) return Collections.emptyList();
        List<Map.Entry<String, Double>> list = new ArrayList<>();
        for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
            if (op == null || op.getName() == null) continue;
            double b = e.getBalance(op);
            if (b <= 0) continue;
            list.add(new AbstractMap.SimpleEntry<>(op.getName(), b));
        }
        list.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        topCache = list;
        topCacheTime = System.currentTimeMillis();
        return list.subList(0, Math.min(limit, list.size()));
    }

    /** 玩家在线时提示余额（供其他模块调用）。 */
    public void notify(Player player, String message) {
        Msg.sendPrefixed(player, plugin.getPrefix(), message);
    }
}
