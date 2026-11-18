package com.plu.plugin.commands.admin;

import com.plu.plugin.Main;
import com.plu.plugin.managers.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class IpBanCommand implements CommandExecutor {
    private final Main plugin;
    public IpBanCommand(Main plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            Msg.send(sender, plugin.getPrefix() + "&cUsage: /ipban <ip|player> [reason]");
            return true;
        }
        String ip = args[0];
        // if a player name was given, resolve their IP
        Player p = plugin.getServer().getPlayer(args[0]);
        if (p != null && p.getAddress() != null) {
            ip = p.getAddress().getAddress().getHostAddress();
        }
        String reason = args.length > 1 ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)) : "违反服务器规则";
        String banner = sender instanceof Player ? sender.getName() : "Console";
        plugin.getIpBanManager().ban(ip, reason, banner, 0);
        Msg.send(sender, plugin.getPrefix() + "&a已封禁 IP &e" + ip + "&a，原因: &e" + reason);
        return true;
    }
}
