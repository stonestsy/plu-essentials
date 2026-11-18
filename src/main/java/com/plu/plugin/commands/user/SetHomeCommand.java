package com.plu.plugin.commands.user;

import com.plu.plugin.Main;
import com.plu.plugin.managers.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetHomeCommand implements CommandExecutor {

    private final Main plugin;

    public SetHomeCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Msg.send(sender, "&cOnly players can use this command.");
            return true;
        }
        String name = args.length > 0 ? args[0].toLowerCase() : "home";

        int limit = plugin.getHomeManager().getHomeLimit(player.getUniqueId());
        int current = plugin.getHomeManager().getHomes(player.getUniqueId()).size();
        boolean exists = plugin.getHomeManager().getHome(player.getUniqueId(), name) != null;

        if (!exists && current >= limit) {
            Msg.send(player, plugin.getPrefix() + "&cYou have reached your home limit (&e" + limit + "&c).");
            return true;
        }

        plugin.getHomeManager().setHome(player.getUniqueId(), name, player.getLocation());
        Msg.send(player, plugin.getPrefix() + "&aHome &e" + name + " &aset at your location.");
        return true;
    }
}
