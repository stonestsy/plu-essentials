package com.plu.plugin.commands.admin;

import com.plu.plugin.Main;
import com.plu.plugin.managers.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class BroadcastCommand implements CommandExecutor {

    private final Main plugin;

    public BroadcastCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            Msg.send(sender, plugin.getPrefix() + "&cUsage: /broadcast <message>");
            return true;
        }
        String message = String.join(" ", args);
        Msg.broadcast("&6[&cBroadcast&6] &r" + message);
        return true;
    }
}
