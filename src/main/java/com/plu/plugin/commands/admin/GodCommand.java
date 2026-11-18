package com.plu.plugin.commands.admin;

import com.plu.plugin.Main;
import com.plu.plugin.managers.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GodCommand implements CommandExecutor {

    private final Main plugin;

    public GodCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player target = resolveTarget(sender, args);
        if (target == null) {
            Msg.send(sender, plugin.getPrefix() + "&cPlayer not found or console must specify a player.");
            return true;
        }
        boolean god = plugin.getGodManager().toggle(target.getUniqueId());
        Msg.send(sender, plugin.getPrefix() + "&aGod mode &e" + (god ? "enabled" : "disabled") + " &afor &e" + target.getName() + "&a.");
        if (!target.equals(sender)) {
            Msg.send(target, plugin.getPrefix() + "&aGod mode &e" + (god ? "enabled" : "disabled") + "&a.");
        }
        return true;
    }

    private Player resolveTarget(CommandSender sender, String[] args) {
        if (args.length > 0) return Bukkit.getPlayer(args[0]);
        return sender instanceof Player ? (Player) sender : null;
    }
}
