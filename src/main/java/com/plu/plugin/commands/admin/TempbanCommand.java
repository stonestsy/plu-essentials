package com.plu.plugin.commands.admin;

import com.plu.plugin.Main;
import com.plu.plugin.managers.BanManager;
import com.plu.plugin.managers.Msg;
import com.plu.plugin.util.PlayerResolver;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class TempbanCommand implements CommandExecutor {

    private final Main plugin;

    public TempbanCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            Msg.send(sender, plugin.getPrefix() + "&cUsage: /tempban <player> <duration> [reason]");
            return true;
        }
        OfflinePlayer target = PlayerResolver.byName(args[0]);
        long duration = parseDuration(args[1]);
        if (duration <= 0) {
            Msg.send(sender, plugin.getPrefix() + "&c无效的时长格式，例如: 1d, 2h, 30m, 1w");
            return true;
        }
        String reason = args.length > 2 ? String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length)) : "违反服务器规则";
        String banner = sender instanceof org.bukkit.entity.Player ? sender.getName() : "Console";
        long expires = System.currentTimeMillis() + duration;
        plugin.getBanManager().ban(target.getUniqueId(), target.getName(), reason, banner, expires);
        Msg.send(sender, plugin.getPrefix() + "&a已临时封禁 &e" + target.getName()
                + "&a，时长: &e" + BanManager.formatDuration(duration) + "&a，原因: &e" + reason);
        return true;
    }

    private long parseDuration(String s) {
        try {
            int len = s.length();
            char unit = s.charAt(len - 1);
            long num = Long.parseLong(s.substring(0, len - 1));
            switch (unit) {
                case 's': return num * 1000L;
                case 'm': return num * 60_000L;
                case 'h': return num * 3_600_000L;
                case 'd': return num * 86_400_000L;
                case 'w': return num * 604_800_000L;
                default: return -1;
            }
        } catch (Exception e) {
            return -1;
        }
    }
}
