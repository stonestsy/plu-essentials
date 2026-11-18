package com.plu.plugin.managers;

import com.plu.plugin.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** VanishManager hides vanished players from everyone without the see permission. */
public class VanishManager {

    private final Main plugin;
    private final Set<UUID> vanished = new HashSet<>();

    public VanishManager(Main plugin) {
        this.plugin = plugin;
    }

    public boolean isVanished(UUID uuid) {
        return vanished.contains(uuid);
    }

    public boolean toggle(Player player) {
        if (vanished.contains(player.getUniqueId())) {
            vanished.remove(player.getUniqueId());
            showAll(player);
            return false;
        } else {
            vanished.add(player.getUniqueId());
            hideFromAll(player);
            return true;
        }
    }

    private void hideFromAll(Player vanishedPlayer) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.hasPermission("plu.vanish.see")) {
                online.hidePlayer(plugin, vanishedPlayer);
            }
        }
    }

    private void showAll(Player player) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.showPlayer(plugin, player);
        }
    }

    /** Called on join: hide vanished players from the joining player. */
    public void onJoin(Player joiner) {
        for (UUID uid : vanished) {
            Player p = Bukkit.getPlayer(uid);
            if (p != null && !joiner.hasPermission("plu.vanish.see")) {
                joiner.hidePlayer(plugin, p);
            }
        }
    }

    public void remove(UUID uuid) {
        vanished.remove(uuid);
    }
}
