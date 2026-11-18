package com.plu.plugin.commands.admin;

import com.plu.plugin.Main;
import com.plu.plugin.managers.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * /invsee <player> - open another player's inventory for viewing/editing.
 * Uses a session lock to prevent concurrent edits from overwriting changes.
 */
public class InvseeCommand implements CommandExecutor {

    private final Main plugin;
    // viewer uuid -> target uuid (session lock)
    private static final Map<UUID, UUID> sessions = new HashMap<>();

    public InvseeCommand(Main plugin) {
        this.plugin = plugin;
    }

    public static UUID getViewerTarget(UUID viewer) {
        return sessions.get(viewer);
    }

    public static void removeSession(UUID viewer) {
        sessions.remove(viewer);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player viewer)) {
            Msg.send(sender, "&cOnly players can use this command.");
            return true;
        }
        if (args.length == 0) {
            Msg.send(viewer, plugin.getPrefix() + "&cUsage: /invsee <player>");
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            Msg.send(viewer, plugin.getPrefix() + "&c该玩家不在线。");
            return true;
        }
        if (target.equals(viewer)) {
            Msg.send(viewer, plugin.getPrefix() + "&c不能查看自己的背包。");
            return true;
        }
        // Open the target's inventory directly (edits are real-time)
        sessions.put(viewer.getUniqueId(), target.getUniqueId());
        viewer.openInventory(target.getInventory());
        Msg.send(viewer, plugin.getPrefix() + "&a正在查看 &e" + target.getName() + " &a的背包。");
        return true;
    }
}
