package com.plu.plugin.commands.user;

import com.plu.plugin.Main;
import com.plu.plugin.managers.Msg;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BackCommand implements CommandExecutor {

    private final Main plugin;

    public BackCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Msg.send(sender, "&cOnly players can use this command.");
            return true;
        }
        if (!plugin.getBackManager().isEnabled()) {
            Msg.send(player, plugin.getPrefix() + "&c/back is disabled.");
            return true;
        }
        Location loc = plugin.getBackManager().getDeathLocation(player.getUniqueId());
        if (loc == null) {
            Msg.send(player, plugin.getPrefix() + "&cYou have no death location to return to.");
            return true;
        }
        player.teleport(loc);
        plugin.getBackManager().clear(player.getUniqueId());
        Msg.send(player, plugin.getPrefix() + "&aTeleported to your last death location.");
        return true;
    }
}
