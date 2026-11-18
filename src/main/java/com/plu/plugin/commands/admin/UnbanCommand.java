package com.plu.plugin.commands.admin;

import com.plu.plugin.Main;
import com.plu.plugin.managers.Msg;
import com.plu.plugin.util.PlayerResolver;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class UnbanCommand implements CommandExecutor {

    private final Main plugin;

    public UnbanCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            Msg.send(sender, plugin.getPrefix() + "&cUsage: /unban <player>");
            return true;
        }
        OfflinePlayer target = PlayerResolver.byName(args[0]);
        if (plugin.getBanManager().unban(target.getUniqueId())) {
            Msg.send(sender, plugin.getPrefix() + "&a已解除 &e" + target.getName() + " &a的封禁。");
        } else {
            Msg.send(sender, plugin.getPrefix() + "&c该玩家未被封禁。");
        }
        return true;
    }
}
