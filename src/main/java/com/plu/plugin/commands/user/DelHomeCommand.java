package com.plu.plugin.commands.user;

import com.plu.plugin.Main;
import com.plu.plugin.managers.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DelHomeCommand implements CommandExecutor {

    private final Main plugin;

    public DelHomeCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Msg.send(sender, "&cOnly players can use this command.");
            return true;
        }
        if (args.length == 0) {
            Msg.send(player, plugin.getPrefix() + "&cUsage: /delhome <name>");
            return true;
        }
        String name = args[0].toLowerCase();
        if (plugin.getHomeManager().deleteHome(player.getUniqueId(), name)) {
            Msg.send(player, plugin.getPrefix() + "&aHome &e" + name + " &adeleted.");
        } else {
            Msg.send(player, plugin.getPrefix() + "&cHome &e" + name + " &cdoes not exist.");
        }
        return true;
    }
}
