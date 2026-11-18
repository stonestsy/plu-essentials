package com.plu.plugin.commands.admin;

import com.plu.plugin.Main;
import com.plu.plugin.managers.BanManager;
import com.plu.plugin.managers.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Map;

public class IpBanlistCommand implements CommandExecutor {
    private final Main plugin;
    public IpBanlistCommand(Main plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Map<String, Object[]> bans = plugin.getIpBanManager().getAll();
        if (bans.isEmpty()) {
            Msg.send(sender, plugin.getPrefix() + "&a当前没有 IP 封禁记录。");
            return true;
        }
        Msg.send(sender, plugin.getPrefix() + "&eIP 封禁列表 (&a" + bans.size() + "&e):");
        for (Map.Entry<String, Object[]> e : bans.entrySet()) {
            Object[] v = e.getValue();
            long expires = (Long) v[3];
            String dur = expires == 0 ? "永久" : BanManager.formatDuration(expires - System.currentTimeMillis());
            Msg.send(sender, "  &f" + e.getKey() + " &7- 原因: &e" + v[0] + " &7| 剩余: &e" + dur);
        }
        return true;
    }
}
