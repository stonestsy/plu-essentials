package com.plu.plugin.commands.admin;

import com.plu.plugin.Main;
import com.plu.plugin.managers.Msg;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HealCommand implements CommandExecutor {

    private final Main plugin;

    public HealCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player target = resolveTarget(sender, args);
        if (target == null) {
            Msg.send(sender, plugin.getPrefix() + "&cPlayer not found or console must specify a player.");
            return true;
        }
        double max = target.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null
                ? target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() : 20.0;
        target.setHealth(max);
        target.setFoodLevel(20);
        target.setSaturation(20);
        target.setFireTicks(0);
        Msg.send(sender, plugin.getPrefix() + "&aHealed &e" + target.getName() + "&a.");
        if (!target.equals(sender)) {
            Msg.send(target, plugin.getPrefix() + "&aYou were healed.");
        }
        return true;
    }

    private Player resolveTarget(CommandSender sender, String[] args) {
        if (args.length > 0) return Bukkit.getPlayer(args[0]);
        return sender instanceof Player ? (Player) sender : null;
    }
}
