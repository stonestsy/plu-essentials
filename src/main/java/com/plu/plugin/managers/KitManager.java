package com.plu.plugin.managers;

import com.plu.plugin.Main;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/** KitManager loads kits from config.yml and gives them to players with cooldowns. */
public class KitManager {

    private final Main plugin;
    // player uuid -> kit name -> last used time (ms)
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    public KitManager(Main plugin) {
        this.plugin = plugin;
    }

    public void load() {
        // kits are read directly from config each time; nothing to load in memory
    }

    public List<String> getKitNames() {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("kits");
        if (sec == null) return new ArrayList<>();
        return new ArrayList<>(sec.getKeys(false));
    }

    public boolean kitExists(String name) {
        return plugin.getConfig().contains("kits." + name);
    }

    public int getCooldown(String kit) {
        return plugin.getConfig().getInt("kits." + kit + ".cooldown", 0);
    }

    public boolean canUse(Player player, String kit) {
        int cd = getCooldown(kit);
        if (cd <= 0) return true;
        Map<String, Long> map = cooldowns.get(player.getUniqueId());
        if (map == null) return true;
        Long last = map.get(kit);
        if (last == null) return true;
        return System.currentTimeMillis() - last >= cd * 1000L;
    }

    public long getRemainingCooldown(Player player, String kit) {
        int cd = getCooldown(kit);
        Map<String, Long> map = cooldowns.get(player.getUniqueId());
        if (map == null) return 0;
        Long last = map.get(kit);
        if (last == null) return 0;
        long remaining = (cd * 1000L) - (System.currentTimeMillis() - last);
        return remaining < 0 ? 0 : remaining / 1000;
    }

    public void giveKit(Player player, String kit) {
        List<Map<?, ?>> items = plugin.getConfig().getMapList("kits." + kit + ".items");
        for (Map<?, ?> m : items) {
            String mat = (String) m.get("material");
            int amount = m.get("amount") instanceof Number ? ((Number) m.get("amount")).intValue() : 1;
            Material material = Material.getMaterial(mat);
            if (material == null) {
                plugin.getLogger().warning("Unknown material in kit " + kit + ": " + mat);
                continue;
            }
            ItemStack stack = new ItemStack(material, amount);
            player.getInventory().addItem(stack).forEach((slot, leftover) ->
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        }
        // set cooldown
        cooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>()).put(kit, System.currentTimeMillis());
    }
}
