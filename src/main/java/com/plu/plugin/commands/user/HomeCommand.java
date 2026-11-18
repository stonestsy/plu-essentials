package com.plu.plugin.commands.user;

import com.plu.plugin.Main;
import com.plu.plugin.managers.Msg;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HomeCommand implements CommandExecutor {

    private final Main plugin;

    public HomeCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Msg.send(sender, "&cOnly players can use this command.");
            return true;
        }
        String name = args.length > 0 ? args[0].toLowerCase() : "home";
        Location loc = plugin.getHomeManager().getHome(player.getUniqueId(), name);
        if (loc == null) {
            Msg.send(player, plugin.getPrefix() + "&cYou don't have a home named &e" + name + "&c.");
            return true;
        }
        player.teleport(loc);
        Msg.send(player, plugin.getPrefix() + "&aTeleported to home &e" + name + "&a.");
        return true;
    }
}
