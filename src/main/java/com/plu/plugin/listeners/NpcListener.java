package com.plu.plugin.listeners;

import com.plu.plugin.Main;
import com.plu.plugin.managers.NpcManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** NPC 点击交互。 */
public class NpcListener implements Listener {

    private final Main plugin;
    private final Map<UUID, Long> cooldown = new ConcurrentHashMap<>();

    public NpcListener(Main plugin) { this.plugin = plugin; }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        handle(event.getPlayer(), event.getRightClicked());
    }

    @EventHandler
    public void onInteractAt(PlayerInteractAtEntityEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        handle(event.getPlayer(), event.getRightClicked());
    }

    private void handle(Player player, org.bukkit.entity.Entity entity) {
        NpcManager.Npc npc = plugin.getNpcManager().findByEntity(entity);
        if (npc == null) return;
        long now = System.currentTimeMillis();
        Long last = cooldown.get(player.getUniqueId());
        if (last != null && now - last < 300L) return;
        cooldown.put(player.getUniqueId(), now);
        plugin.getNpcManager().interact(player, npc);
    }
}
