package com.plu.plugin.commands.user;

import com.plu.plugin.Main;
import com.plu.plugin.managers.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MoneyCommand implements CommandExecutor {
    private final Main plugin;
    public MoneyCommand(Main plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!plugin.getEconomyManager().available()) {
            Msg.send(sender, plugin.getPrefix() + "&c经济系统已关闭（config.yml 的 economy.enabled）。");
            return true;
        }
        if (args.length > 0) {
            if (!sender.hasPermission("plu.money.others")) {
                Msg.send(sender, plugin.getPrefix() + "&c你没有查看他人余额的权限。");
                return true;
            }
            double bal = plugin.getEconomyManager().balance(args[0]);
            Msg.send(sender, plugin.getPrefix() + "&e" + args[0] + " &7的余额: &a" + plugin.getEconomyManager().format(bal));
            return true;
        }
        if (!(sender instanceof Player player)) {
            Msg.send(sender, plugin.getPrefix() + "&c请在游戏内使用，或指定玩家名。");
            return true;
        }
        double bal = plugin.getEconomyManager().balance(player.getUniqueId());
        Msg.send(sender, plugin.getPrefix() + "&7你的余额: &a" + plugin.getEconomyManager().format(bal));
        return true;
    }
}
