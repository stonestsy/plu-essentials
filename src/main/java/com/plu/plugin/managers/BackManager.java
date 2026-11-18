package com.plu.plugin.managers;

import com.plu.plugin.Main;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** BackManager stores the last death location of each player. */
public class BackManager {

    private final Main plugin;
    private final Map<UUID, Location> deathLocations = new HashMap<>();

    public BackManager(Main plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("back.enabled", true);
    }

    public void setDeathLocation(UUID uuid, Location loc) {
        deathLocations.put(uuid, loc.clone());
    }

    public Location getDeathLocation(UUID uuid) {
        return deathLocations.get(uuid);
    }

    public void clear(UUID uuid) {
        deathLocations.remove(uuid);
    }
}
