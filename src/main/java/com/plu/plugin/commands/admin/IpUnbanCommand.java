package com.plu.plugin.commands.admin;

import com.plu.plugin.Main;
import com.plu.plugin.managers.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class IpUnbanCommand implements CommandExecutor {
    private final Main plugin;
    public IpUnbanCommand(Main plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            Msg.send(sender, plugin.getPrefix() + "&cUsage: /ipunban <ip>");
            return true;
        }
        if (plugin.getIpBanManager().unban(args[0])) {
            Msg.send(sender, plugin.getPrefix() + "&a已解封 IP &e" + args[0]);
        } else {
            Msg.send(sender, plugin.getPrefix() + "&c该 IP 未被封禁。");
        }
        return true;
    }
}
