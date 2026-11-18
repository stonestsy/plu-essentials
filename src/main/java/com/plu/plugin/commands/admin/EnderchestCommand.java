package com.plu.plugin.commands.admin;

import com.plu.plugin.Main;
import com.plu.plugin.managers.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EnderchestCommand implements CommandExecutor {

    private final Main plugin;

    public EnderchestCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player viewer)) {
            Msg.send(sender, "&cOnly players can use this command.");
            return true;
        }
        if (args.length == 0) {
            // view own ender chest
            viewer.openInventory(viewer.getEnderChest());
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            Msg.send(viewer, plugin.getPrefix() + "&c该玩家不在线。");
            return true;
        }
        viewer.openInventory(target.getEnderChest());
        Msg.send(viewer, plugin.getPrefix() + "&a正在查看 &e" + target.getName() + " &a的末影箱。");
        return true;
    }
}
