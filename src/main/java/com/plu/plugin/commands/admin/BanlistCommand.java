package com.plu.plugin.commands.admin;

import com.plu.plugin.Main;
import com.plu.plugin.managers.BanManager;
import com.plu.plugin.managers.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

public class BanlistCommand implements CommandExecutor {

    private final Main plugin;

    public BanlistCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Map<UUID, Object[]> bans = plugin.getBanManager().getAllBans();
        if (bans.isEmpty()) {
            Msg.send(sender, plugin.getPrefix() + "&a当前没有封禁记录。");
            return true;
        }
        Msg.send(sender, plugin.getPrefix() + "&e封禁列表 (&a" + bans.size() + "&e):");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        for (Map.Entry<UUID, Object[]> e : bans.entrySet()) {
            Object[] v = e.getValue();
            String name = (String) v[0];
            String reason = (String) v[1];
            String banner = (String) v[2];
            long expires = (Long) v[4];
            String dur = expires == 0 ? "永久" : BanManager.formatDuration(expires - System.currentTimeMillis());
            Msg.send(sender, "  &f" + name + " &7- 原因: &e" + reason + " &7| 执行者: &e" + banner + " &7| 剩余: &e" + dur);
        }
        return true;
    }
}
