package com.plu.plugin.commands.admin;

import com.plu.plugin.Main;
import com.plu.plugin.managers.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetwarpCommand implements CommandExecutor {

    private final Main plugin;

    public SetwarpCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Msg.send(sender, "&cOnly players can use this command.");
            return true;
        }
        if (args.length == 0) {
            Msg.send(player, plugin.getPrefix() + "&cUsage: /setwarp <name>");
            return true;
        }
        plugin.getWarpManager().setWarp(args[0], player.getLocation());
        Msg.send(player, plugin.getPrefix() + "&a传送点 &e" + args[0] + " &a已设置。");
        return true;
    }
}
