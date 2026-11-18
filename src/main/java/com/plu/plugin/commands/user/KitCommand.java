package com.plu.plugin.commands.user;

import com.plu.plugin.Main;
import com.plu.plugin.managers.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class KitCommand implements CommandExecutor {

    private final Main plugin;

    public KitCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Msg.send(sender, "&cOnly players can use this command.");
            return true;
        }
        if (args.length == 0) {
            List<String> kits = plugin.getKitManager().getKitNames();
            if (kits.isEmpty()) {
                Msg.send(player, plugin.getPrefix() + "&cNo kits available.");
            } else {
                Msg.send(player, plugin.getPrefix() + "&eAvailable kits: &a" + String.join("&8, &a", kits));
            }
            return true;
        }
        String kit = args[0].toLowerCase();
        if (!plugin.getKitManager().kitExists(kit)) {
            Msg.send(player, plugin.getPrefix() + "&cKit &e" + kit + " &cdoes not exist.");
            return true;
        }
        if (!plugin.getKitManager().canUse(player, kit)) {
            long remaining = plugin.getKitManager().getRemainingCooldown(player, kit);
            Msg.send(player, plugin.getPrefix() + "&cYou must wait &e" + remaining + "s &cto use this kit again.");
            return true;
        }
        plugin.getKitManager().giveKit(player, kit);
        Msg.send(player, plugin.getPrefix() + "&aReceived kit &e" + kit + "&a.");
        return true;
    }
}
