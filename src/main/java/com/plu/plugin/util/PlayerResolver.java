package com.plu.plugin.util;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家名 → OfflinePlayer 的解析封装。
 * Bukkit#getOfflinePlayer(String) 已弃用（可能阻塞主线程发起网络查询），
 * 这里用内存缓存减少重复查询，并把弃用调用集中在一处。
 */
public final class PlayerResolver {

    /** 名字（小写）→ 已解析玩家，避免每次命令都查一次。 */
    private static final Map<String, OfflinePlayer> CACHE = new ConcurrentHashMap<>();

    private PlayerResolver() {}

    /** 按名字解析玩家：在线玩家直接命中，其余走缓存/离线查询。 */
    @SuppressWarnings("deprecation")
    public static OfflinePlayer byName(String name) {
        if (name == null || name.isEmpty()) return null;

        // 在线玩家优先，无需查询
        var online = Bukkit.getPlayerExact(name);
        if (online != null) return online;

        String key = name.toLowerCase(Locale.ROOT);
        OfflinePlayer cached = CACHE.get(key);
        if (cached != null) return cached;

        OfflinePlayer resolved = Bukkit.getOfflinePlayer(name);
        if (resolved != null) CACHE.put(key, resolved);
        return resolved;
    }

    /** 按名字取 UUID，无法解析时返回 null。 */
    public static UUID uuidByName(String name) {
        OfflinePlayer op = byName(name);
        return op != null ? op.getUniqueId() : null;
    }
}
