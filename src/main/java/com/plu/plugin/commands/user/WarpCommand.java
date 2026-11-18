package com.plu.plugin.commands.user;

import com.plu.plugin.Main;
import com.plu.plugin.managers.Msg;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class WarpCommand implements CommandExecutor {

    private final Main plugin;

    public WarpCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Msg.send(sender, "&cOnly players can use this command.");
            return true;
        }
        if (args.length == 0) {
            List<String> warps = plugin.getWarpManager().getWarpNames();
            if (warps.isEmpty()) {
                Msg.send(player, plugin.getPrefix() + "&c没有可用的传送点。");
            } else {
                Msg.send(player, plugin.getPrefix() + "&e可用传送点: &a" + String.join("&8, &a", warps));
            }
            return true;
        }
        Location loc = plugin.getWarpManager().getWarp(args[0]);
        if (loc == null) {
            Msg.send(player, plugin.getPrefix() + "&c传送点 &e" + args[0] + " &c不存在。");
            return true;
        }
        player.teleport(loc);
        Msg.send(player, plugin.getPrefix() + "&a已传送到 &e" + args[0] + "&a。");
        return true;
    }
}
