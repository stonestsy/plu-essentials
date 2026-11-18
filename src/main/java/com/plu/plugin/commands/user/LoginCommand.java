package com.plu.plugin.commands.user;

import com.plu.plugin.Main;
import com.plu.plugin.managers.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LoginCommand implements CommandExecutor {
    private final Main plugin;
    public LoginCommand(Main plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Msg.send(sender, plugin.getPrefix() + "&c仅玩家可使用登录命令。");
            return true;
        }
        if (!plugin.getLoginManager().isEnabled()) {
            Msg.send(player, plugin.getPrefix() + "&7登录系统未启用。");
            return true;
        }
        if (plugin.getLoginManager().isAuthed(player.getUniqueId())) {
            Msg.send(player, plugin.getPrefix() + "&7你已经登录了。");
            return true;
        }
        if (!plugin.getLoginManager().isRegistered(player.getUniqueId())) {
            Msg.send(player, plugin.getPrefix() + "&c你还没有注册，请使用 &e/register <密码>");
            return true;
        }
        if (args.length < 1) {
            Msg.send(player, plugin.getPrefix() + "&c用法: /login <密码>");
            return true;
        }
        String password = String.join(" ", args);
        if (!plugin.getLoginManager().authenticate(player.getUniqueId(), password)) {
            Msg.send(player, plugin.getPrefix() + "&c密码错误。");
            return true;
        }
        Msg.send(player, plugin.getPrefix() + "&a登录成功，欢迎回来！");
        return true;
    }
}
