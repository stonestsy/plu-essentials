package com.plu.plugin.listeners;

import com.plu.plugin.Main;
import com.plu.plugin.gui.MenuGui;
import com.plu.plugin.managers.MenuManager;
import com.plu.plugin.managers.Msg;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/** 自定义菜单点击处理。 */
public class MenuListener implements Listener {

    private final Main plugin;

    public MenuListener(Main plugin) { this.plugin = plugin; }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof MenuGui gui)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null
                || event.getClickedInventory() != event.getView().getTopInventory()) return;

        MenuManager.Menu menu = plugin.getMenuManager().get(gui.getMenuId());
        if (menu == null) return;
        MenuManager.MenuItem target = null;
        for (MenuManager.MenuItem item : menu.items) {
            if (item.slot == event.getSlot()) { target = item; break; }
        }
        if (target == null) return;
        if (!target.permission.isEmpty() && !player.hasPermission(target.permission)) {
            Msg.send(player, plugin.getPrefix() + "&c你没有使用该菜单项的权限。");
            return;
        }

        for (String raw : target.commands) {
            if (raw == null || raw.isBlank()) continue;
            String command = raw
                    .replace("{player}", player.getName())
                    .replace("{nick}", plugin.getNicknameManager().display(player.getUniqueId()));
            if (command.startsWith("console:")) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.substring(8).trim());
            } else {
                player.performCommand(command.startsWith("/") ? command.substring(1) : command);
            }
        }

        if (!target.open.isEmpty()) {
            plugin.getMenuManager().open(player, target.open);
        }
        if (target.close) player.closeInventory();
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof MenuGui) {
            event.setCancelled(true);
        }
    }
}
