package com.plu.plugin.commands.admin;

import com.plu.plugin.Main;
import com.plu.plugin.managers.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class DelwarpCommand implements CommandExecutor {

    private final Main plugin;

    public DelwarpCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            Msg.send(sender, plugin.getPrefix() + "&cUsage: /delwarp <name>");
            return true;
        }
        if (plugin.getWarpManager().deleteWarp(args[0])) {
            Msg.send(sender, plugin.getPrefix() + "&a传送点 &e" + args[0] + " &a已删除。");
        } else {
            Msg.send(sender, plugin.getPrefix() + "&c传送点 &e" + args[0] + " &c不存在。");
        }
        return true;
    }
}
