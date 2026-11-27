package com.plu.plugin.commands.user;

import com.plu.plugin.Main;
import com.plu.plugin.managers.MenuManager;
import com.plu.plugin.managers.Msg;
import com.plu.plugin.util.Values;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /menu —— 打开自定义菜单。 */
public class MenuCommand implements CommandExecutor {

    private final Main plugin;

    public MenuCommand(Main plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!plugin.getMenuManager().isEnabled()) {
            Msg.send(sender, plugin.getPrefix() + "&c自定义菜单已关闭。");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("list")) {
            Msg.send(sender, plugin.getPrefix() + "&7菜单: &f" + String.join(", ", plugin.getMenuManager().all().keySet()));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("plu.menu.admin")) {
                Msg.send(sender, plugin.getPrefix() + "&c你没有权限。");
                return true;
            }
            plugin.getMenuManager().load();
            Msg.send(sender, plugin.getPrefix() + "&a菜单已重载。");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("create")) {
            if (!sender.hasPermission("plu.menu.admin")) {
                Msg.send(sender, plugin.getPrefix() + "&c你没有权限。");
                return true;
            }
            if (args.length < 2) {
                Msg.send(sender, plugin.getPrefix() + "&7用法: /menu create <id> [size]");
                return true;
            }
            int size = args.length >= 3 ? Values.asInt(args[2], 27) : 27;
            if (plugin.getMenuManager().create(args[1], MenuManager.normalizeSize(size)) == null) {
                Msg.send(sender, plugin.getPrefix() + "&c菜单已存在或 id 非法。");
                return true;
            }
            Msg.send(sender, plugin.getPrefix() + "&a菜单 &e" + args[1] + " &a已创建，请在 menus.yml 中编辑内容。");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("delete")) {
            if (!sender.hasPermission("plu.menu.admin")) {
                Msg.send(sender, plugin.getPrefix() + "&c你没有权限。");
                return true;
            }
            if (args.length < 2 || !plugin.getMenuManager().delete(args[1])) {
                Msg.send(sender, plugin.getPrefix() + "&c菜单不存在。");
                return true;
            }
            Msg.send(sender, plugin.getPrefix() + "&a菜单已删除。");
            return true;
        }

        if (!(sender instanceof Player player)) {
            Msg.send(sender, plugin.getPrefix() + "&c该命令只能由玩家使用。");
            return true;
        }

        String id = args.length > 0 ? args[0] : "main";
        if (!plugin.getMenuManager().open(player, id)) {
            Msg.send(sender, plugin.getPrefix() + "&c菜单不存在: " + id);
        }
        return true;
    }
}
