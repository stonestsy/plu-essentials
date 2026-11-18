package com.plu.plugin.commands.admin;

import com.plu.plugin.Main;
import com.plu.plugin.managers.BanManager;
import com.plu.plugin.managers.Msg;
import com.plu.plugin.util.PlayerResolver;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.text.SimpleDateFormat;
import java.util.Date;

public class SeenCommand implements CommandExecutor {
    private final Main plugin;
    public SeenCommand(Main plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            Msg.send(sender, plugin.getPrefix() + "&cUsage: /seen <player>");
            return true;
        }
        OfflinePlayer op = PlayerResolver.byName(args[0]);
        Object[] stats = plugin.getPlayerStatsManager().get(op.getUniqueId());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        if (stats == null) {
            Msg.send(sender, plugin.getPrefix() + "&c没有找到该玩家的记录。");
            return true;
        }
        Msg.send(sender, plugin.getPrefix() + "&e玩家信息: &f" + args[0]);
        Msg.send(sender, "  &7首次登录: &f" + sdf.format(new Date((Long) stats[0])));
        Msg.send(sender, "  &7最后在线: &f" + sdf.format(new Date((Long) stats[1])));
        Msg.send(sender, "  &7总游戏时长: &f" + BanManager.formatDuration((Long) stats[2]));
        Msg.send(sender, "  &7当前状态: " + (op.isOnline() ? "&a在线" : "&c离线"));
        return true;
    }
}
