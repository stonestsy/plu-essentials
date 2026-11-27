package com.plu.plugin.commands.user;

import com.plu.plugin.Main;
import com.plu.plugin.managers.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;

public class BaltopCommand implements CommandExecutor {
    private final Main plugin;
    public BaltopCommand(Main plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!plugin.getEconomyManager().available()) {
            Msg.send(sender, plugin.getPrefix() + "&c经济系统已关闭（config.yml 的 economy.enabled）。");
            return true;
        }
        int limit = 10;
        if (args.length > 0) {
            try { limit = Math.max(1, Math.min(50, Integer.parseInt(args[0]))); }
            catch (Exception ignored) {}
        }
        List<Map.Entry<String, Double>> top = plugin.getEconomyManager().top(limit);
        if (top.isEmpty()) {
            Msg.send(sender, plugin.getPrefix() + "&c暂无排行数据。");
            return true;
        }
        Msg.send(sender, plugin.getPrefix() + "&6=== 财富排行榜 ===");
        int i = 1;
        for (Map.Entry<String, Double> e : top) {
            String medal = switch (i) { case 1 -> "&6①"; case 2 -> "&e②"; case 3 -> "&a③"; default -> "&7" + i + "."; };
            Msg.send(sender, "  " + medal + " &f" + e.getKey() + " &7- &a" + plugin.getEconomyManager().format(e.getValue()));
            i++;
        }
        return true;
    }
}
