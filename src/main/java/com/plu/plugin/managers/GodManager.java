package com.plu.plugin.managers;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** GodManager tracks players in god mode. */
public class GodManager {

    private final Set<UUID> god = new HashSet<>();

    public boolean isGod(UUID uuid) {
        return god.contains(uuid);
    }

    public boolean toggle(UUID uuid) {
        if (god.contains(uuid)) {
            god.remove(uuid);
            return false;
        } else {
            god.add(uuid);
            return true;
        }
    }

    public void remove(UUID uuid) {
        god.remove(uuid);
    }
}
