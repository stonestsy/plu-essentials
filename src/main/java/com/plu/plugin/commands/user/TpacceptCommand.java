package com.plu.plugin.commands.user;

import com.plu.plugin.Main;
import com.plu.plugin.managers.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class TpacceptCommand implements CommandExecutor {

    private final Main plugin;

    public TpacceptCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player target)) {
            Msg.send(sender, "&cOnly players can use this command.");
            return true;
        }
        UUID requesterId = plugin.getTpaManager().getRequester(target);
        if (requesterId == null) {
            Msg.send(target, plugin.getPrefix() + "&cYou have no pending teleport requests.");
            return true;
        }
        Player requester = Bukkit.getPlayer(requesterId);
        if (requester == null) {
            Msg.send(target, plugin.getPrefix() + "&cThe requester is no longer online.");
            plugin.getTpaManager().clearFor(requesterId);
            return true;
        }
        requester.teleport(target.getLocation());
        Msg.send(requester, plugin.getPrefix() + "&aTeleported to &e" + target.getName() + "&a.");
        Msg.send(target, plugin.getPrefix() + "&aTeleported &e" + requester.getName() + " &ato you.");
        plugin.getTpaManager().cancel(requester);
        return true;
    }
}
