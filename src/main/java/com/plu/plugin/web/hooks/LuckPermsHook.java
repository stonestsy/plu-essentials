package com.plu.plugin.web.hooks;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Hooks into LuckPerms via reflection (no hard dependency).
 * Provides access to player groups and permissions.
 */
public class LuckPermsHook {

    private static Object luckPermsApi = null;
    private static Class<?> userManagerClass = null;
    private static Class<?> userClass = null;

    public static boolean setup() {
        try {
            Class<?> apiClass = Class.forName("net.luckperms.api.LuckPerms");
            Object provider = Bukkit.getServicesManager().getRegistration(apiClass);
            if (provider == null) return false;
            luckPermsApi = ((org.bukkit.plugin.RegisteredServiceProvider<?>) provider).getProvider();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isAvailable() {
        return luckPermsApi != null;
    }

    /** Returns the primary group of a player, or "default" if unavailable. */
    public static String getPrimaryGroup(Player player) {
        if (luckPermsApi == null) return "default";
        try {
            Method getPlayerAdapter = luckPermsApi.getClass().getMethod("getPlayerAdapter", Class.class);
            Object adapter = getPlayerAdapter.invoke(luckPermsApi, Player.class);
            Method getPrimaryGroup = adapter.getClass().getMethod("getPrimaryGroup", Player.class);
            return (String) getPrimaryGroup.invoke(adapter, player);
        } catch (Exception e) {
            return "default";
        }
    }

    /** Returns a list of groups the player belongs to. */
    public static List<String> getGroups(Player player) {
        List<String> groups = new ArrayList<>();
        if (luckPermsApi == null) return groups;
        try {
            Method getUserManager = luckPermsApi.getClass().getMethod("getUserManager");
            Object userManager = getUserManager.invoke(luckPermsApi);
            Method getUser = userManager.getClass().getMethod("getUser", UUID.class);
            Object user = getUser.invoke(userManager, player.getUniqueId());
            if (user == null) return groups;
            Method getDistinctNodes = user.getClass().getMethod("getDistinctNodes");
            java.util.Collection<?> nodes = (java.util.Collection<?>) getDistinctNodes.invoke(user);
            Class<?> nodeClass = Class.forName("net.luckperms.api.node.Node");
            Method getKey = nodeClass.getMethod("getKey");
            Method isInherited = nodeClass.getMethod("isInherited");
            for (Object node : nodes) {
                String key = (String) getKey.invoke(node);
                boolean inherited = (Boolean) isInherited.invoke(node);
                if (key.startsWith("group.") && inherited) {
                    groups.add(key.substring(6));
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return groups;
    }
}
