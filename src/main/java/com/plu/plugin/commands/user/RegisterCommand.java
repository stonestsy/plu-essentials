package com.plu.plugin.commands.user;

import com.plu.plugin.Main;
import com.plu.plugin.managers.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RegisterCommand implements CommandExecutor {
    private final Main plugin;
    public RegisterCommand(Main plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Msg.send(sender, plugin.getPrefix() + "&c仅玩家可使用注册命令。");
            return true;
        }
        if (!plugin.getLoginManager().isEnabled()) {
            Msg.send(player, plugin.getPrefix() + "&7登录系统未启用。");
            return true;
        }
        if (plugin.getLoginManager().isRegistered(player.getUniqueId())) {
            Msg.send(player, plugin.getPrefix() + "&c你已经注册过了，请使用 &e/login <密码>");
            return true;
        }
        if (args.length < 1) {
            Msg.send(player, plugin.getPrefix() + "&c用法: /register <密码>（4-32 位）");
            return true;
        }
        String password = String.join(" ", args);
        if (password.length() < 4 || password.length() > 32) {
            Msg.send(player, plugin.getPrefix() + "&c密码长度必须在 4-32 位之间。");
            return true;
        }
        if (!plugin.getLoginManager().register(player.getUniqueId(), player.getName(), password)) {
            Msg.send(player, plugin.getPrefix() + "&c注册失败。");
            return true;
        }
        Msg.send(player, plugin.getPrefix() + "&a注册成功，已自动登录！");
        return true;
    }
}
