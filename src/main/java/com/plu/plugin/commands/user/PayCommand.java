package com.plu.plugin.commands.user;

import com.plu.plugin.Main;
import com.plu.plugin.managers.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PayCommand implements CommandExecutor {
    private final Main plugin;
    public PayCommand(Main plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Msg.send(sender, plugin.getPrefix() + "&c仅玩家可使用该命令。");
            return true;
        }
        if (!plugin.getEconomyManager().available()) {
            Msg.send(sender, plugin.getPrefix() + "&c经济系统不可用（需要 Vault 与经济插件）。");
            return true;
        }
        if (args.length < 2) {
            Msg.send(sender, plugin.getPrefix() + "&c用法: /pay <玩家> <金额>");
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            Msg.send(sender, plugin.getPrefix() + "&c玩家不在线。");
            return true;
        }
        double amount;
        try { amount = Double.parseDouble(args[1]); }
        catch (Exception e) { Msg.send(sender, plugin.getPrefix() + "&c金额格式错误。"); return true; }
        if (amount <= 0) {
            Msg.send(sender, plugin.getPrefix() + "&c金额必须大于 0。");
            return true;
        }
        if (target.equals(player)) {
            Msg.send(sender, plugin.getPrefix() + "&c不能给自己转账。");
            return true;
        }
        if (!plugin.getEconomyManager().transfer(player.getUniqueId(), target.getUniqueId(), amount)) {
            Msg.send(sender, plugin.getPrefix() + "&c余额不足。当前余额: &a"
                    + plugin.getEconomyManager().format(plugin.getEconomyManager().balance(player.getUniqueId())));
            return true;
        }
        Msg.send(sender, plugin.getPrefix() + "&a已向 &e" + target.getName() + " &a转账 &e" + plugin.getEconomyManager().format(amount));
        Msg.send(target, plugin.getPrefix() + "&a收到 &e" + player.getName() + " &a的转账 &e" + plugin.getEconomyManager().format(amount));
        return true;
    }
}
