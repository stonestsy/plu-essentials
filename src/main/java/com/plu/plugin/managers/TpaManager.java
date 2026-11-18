package com.plu.plugin.managers;

import com.plu.plugin.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** TpaManager handles teleport requests between players with a timeout. */
public class TpaManager {

    private final Main plugin;
    // requester -> target
    private final Map<UUID, UUID> requests = new HashMap<>();
    // requester -> task id
    private final Map<UUID, Integer> tasks = new HashMap<>();

    public TpaManager(Main plugin) {
        this.plugin = plugin;
    }

    /** Create a teleport request from requester to target. */
    public void request(Player requester, Player target) {
        // Cancel any existing request from requester
        cancel(requester);

        requests.put(requester.getUniqueId(), target.getUniqueId());

        int timeout = plugin.getConfig().getInt("tpa.timeout", 60);
        int taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            if (requests.containsKey(requester.getUniqueId())) {
                requests.remove(requester.getUniqueId());
                tasks.remove(requester.getUniqueId());
                Msg.send(requester, plugin.getPrefix() + "&cYour teleport request to " + target.getName() + " has timed out.");
            }
        }, timeout * 20L);
        tasks.put(requester.getUniqueId(), taskId);
    }

    public UUID getRequester(Player target) {
        for (Map.Entry<UUID, UUID> e : requests.entrySet()) {
            if (e.getValue().equals(target.getUniqueId())) return e.getKey();
        }
        return null;
    }

    public UUID getTarget(Player requester) {
        return requests.get(requester.getUniqueId());
    }

    public boolean hasRequestFrom(Player target) {
        return getRequester(target) != null;
    }

    public void cancel(Player requester) {
        Integer task = tasks.remove(requester.getUniqueId());
        if (task != null) {
            Bukkit.getScheduler().cancelTask(task);
        }
        requests.remove(requester.getUniqueId());
    }

    public void clearFor(UUID uuid) {
        // Remove as requester
        Integer task = tasks.remove(uuid);
        if (task != null) Bukkit.getScheduler().cancelTask(task);
        requests.remove(uuid);
        // Remove as target
        requests.entrySet().removeIf(e -> e.getValue().equals(uuid));
    }
}
