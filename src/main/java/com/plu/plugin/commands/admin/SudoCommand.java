package com.plu.plugin.commands.admin;

import com.plu.plugin.Main;
import com.plu.plugin.managers.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SudoCommand implements CommandExecutor {
    private final Main plugin;
    public SudoCommand(Main plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            Msg.send(sender, plugin.getPrefix() + "&cUsage: /sudo <player> <command>");
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            Msg.send(sender, plugin.getPrefix() + "&c玩家不在线。");
            return true;
        }
        String cmd = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        Bukkit.dispatchCommand(target, cmd);
        Msg.send(sender, plugin.getPrefix() + "&a已强制 &e" + target.getName() + " &a执行: &e" + cmd);
        return true;
    }
}
