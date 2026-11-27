package com.plu.plugin.listeners;

import com.plu.plugin.Main;
import com.plu.plugin.managers.Msg;
import com.plu.plugin.managers.SignManager;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

/** 右键命令告示牌执行命令。 */
public class CommandSignListener implements Listener {

    private final Main plugin;

    public CommandSignListener(Main plugin) { this.plugin = plugin; }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != null
                && event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        Block block = event.getClickedBlock();
        if (block == null || !(block.getState() instanceof Sign)) return;

        SignManager.SignEntry entry = plugin.getSignManager().get(block.getLocation());
        if (entry == null) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        if (!entry.permission.isEmpty() && !player.hasPermission(entry.permission)) {
            Msg.send(player, plugin.getPrefix() + "&c你没有使用该告示牌的权限。");
            return;
        }
        String command = entry.command
                .replace("{player}", player.getName())
                .replace("{nick}", plugin.getNicknameManager().display(player.getUniqueId()));
        if (command.startsWith("console:")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.substring(8).trim());
        } else {
            player.performCommand(command.startsWith("/") ? command.substring(1) : command);
        }
    }
}
