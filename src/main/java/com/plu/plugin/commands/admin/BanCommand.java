package com.plu.plugin.commands.admin;

import com.plu.plugin.Main;
import com.plu.plugin.managers.Msg;
import com.plu.plugin.util.PlayerResolver;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BanCommand implements CommandExecutor {

    private final Main plugin;

    public BanCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            Msg.send(sender, plugin.getPrefix() + "&cUsage: /ban <player> [reason]");
            return true;
        }
        OfflinePlayer target = PlayerResolver.byName(args[0]);
        String reason = args.length > 1 ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)) : "违反服务器规则";
        String banner = sender instanceof Player ? sender.getName() : "Console";
        plugin.getBanManager().ban(target.getUniqueId(), target.getName(), reason, banner, 0);
        Msg.send(sender, plugin.getPrefix() + "&a已封禁 &e" + target.getName() + "&a，原因: &e" + reason);
        return true;
    }
}
