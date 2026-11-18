package com.plu.plugin.commands.admin;

import com.plu.plugin.Main;
import com.plu.plugin.managers.Msg;
import com.plu.plugin.util.PlayerResolver;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class WhitelistCommand implements CommandExecutor {
    private final Main plugin;
    public WhitelistCommand(Main plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            Msg.send(sender, plugin.getPrefix() + "&cUsage: /whitelist <on|off|add|remove|list> [player]");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "on" -> {
                plugin.getWhitelistManager().setEnabled(true);
                Msg.send(sender, plugin.getPrefix() + "&a白名单已启用。");
                // kick non-whitelisted players
                for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
                    if (!plugin.getWhitelistManager().isWhitelisted(p.getUniqueId())) {
                        p.kickPlayer("§c服务器已启用白名单，您不在白名单中。");
                    }
                }
            }
            case "off" -> {
                plugin.getWhitelistManager().setEnabled(false);
                Msg.send(sender, plugin.getPrefix() + "&a白名单已关闭。");
            }
            case "add" -> {
                if (args.length < 2) { Msg.send(sender, plugin.getPrefix() + "&c请指定玩家名"); return true; }
                OfflinePlayer op = PlayerResolver.byName(args[1]);
                if (plugin.getWhitelistManager().add(op.getUniqueId(), op.getName())) {
                    Msg.send(sender, plugin.getPrefix() + "&a已将 &e" + args[1] + " &a加入白名单。");
                } else {
                    Msg.send(sender, plugin.getPrefix() + "&e" + args[1] + " &c已在白名单中。");
                }
            }
            case "remove" -> {
                if (args.length < 2) { Msg.send(sender, plugin.getPrefix() + "&c请指定玩家名"); return true; }
                OfflinePlayer op = PlayerResolver.byName(args[1]);
                if (plugin.getWhitelistManager().remove(op.getUniqueId())) {
                    Msg.send(sender, plugin.getPrefix() + "&a已将 &e" + args[1] + " &a移出白名单。");
                } else {
                    Msg.send(sender, plugin.getPrefix() + "&e" + args[1] + " &c不在白名单中。");
                }
            }
            case "list" -> {
                Msg.send(sender, plugin.getPrefix() + "&e白名单 (&a" + plugin.getWhitelistManager().getCount() + "&e):");
                Msg.send(sender, "  &a" + String.join("&8, &a", plugin.getWhitelistManager().getNames()));
            }
            default -> Msg.send(sender, plugin.getPrefix() + "&c未知操作。");
        }
        return true;
    }
}
