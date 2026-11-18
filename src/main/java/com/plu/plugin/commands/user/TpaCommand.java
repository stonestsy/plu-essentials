package com.plu.plugin.commands.user;

import com.plu.plugin.Main;
import com.plu.plugin.managers.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TpaCommand implements CommandExecutor {

    private final Main plugin;

    public TpaCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Msg.send(sender, "&cOnly players can use this command.");
            return true;
        }
        if (args.length == 0) {
            Msg.send(player, plugin.getPrefix() + "&cUsage: /tpa <player>");
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            Msg.send(player, plugin.getPrefix() + "&cPlayer not found.");
            return true;
        }
        if (target.equals(player)) {
            Msg.send(player, plugin.getPrefix() + "&cYou can't request teleport to yourself.");
            return true;
        }
        plugin.getTpaManager().request(player, target);
        Msg.send(player, plugin.getPrefix() + "&aTeleport request sent to &e" + target.getName() + "&a.");
        Msg.send(target, plugin.getPrefix() + "&e" + player.getName() + " &awants to teleport to you. Use &e/tpaccept &aor &e/tpdeny&a.");
        return true;
    }
}
