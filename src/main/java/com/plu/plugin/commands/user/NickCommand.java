package com.plu.plugin.commands.user;

import com.plu.plugin.Main;
import com.plu.plugin.managers.Msg;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/** /nick —— 中文昵称（显示名）。 */
public class NickCommand implements CommandExecutor {

    private final Main plugin;

    public NickCommand(Main plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!plugin.getNicknameManager().isEnabled()) {
            Msg.send(sender, plugin.getPrefix() + "&c昵称功能已关闭。");
            return true;
        }

        // /nick
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                Msg.send(sender, plugin.getPrefix() + "&7用法: /nick <昵称> | /nick reset | /nick <玩家> <昵称>");
                return true;
            }
            String nick = plugin.getNicknameManager().nickname(player.getUniqueId());
            Msg.send(sender, plugin.getPrefix() + "&7你的昵称: &f"
                    + (nick == null || nick.isEmpty() ? "&7（未设置，显示真实名）" : Msg.color(nick)));
            return true;
        }

        // /nick reset [player]
        if (args[0].equalsIgnoreCase("reset")) {
            if (args.length >= 2) {
                if (!sender.hasPermission("plu.nick.others")) {
                    Msg.send(sender, plugin.getPrefix() + "&c你没有修改他人昵称的权限。");
                    return true;
                }
                UUID uuid = plugin.getNicknameManager().uuidByName(args[1]);
                if (uuid == null) {
                    Msg.send(sender, plugin.getPrefix() + "&c找不到玩家: " + args[1]);
                    return true;
                }
                plugin.getNicknameManager().remove(uuid);
                applyOnline(uuid);
                Msg.send(sender, plugin.getPrefix() + "&a已清除 &e" + args[1] + " &a的昵称。");
                return true;
            }
            if (!(sender instanceof Player player)) {
                Msg.send(sender, plugin.getPrefix() + "&c该命令只能由玩家使用。");
                return true;
            }
            plugin.getNicknameManager().remove(player.getUniqueId());
            plugin.getNicknameManager().apply(player);
            Msg.send(sender, plugin.getPrefix() + "&a已清除你的昵称。");
            return true;
        }

        // /nick <player> <nick>
        if (args.length >= 2) {
            if (!sender.hasPermission("plu.nick.others")) {
                Msg.send(sender, plugin.getPrefix() + "&c你没有修改他人昵称的权限。");
                return true;
            }
            UUID uuid = plugin.getNicknameManager().uuidByName(args[0]);
            if (uuid == null) {
                Msg.send(sender, plugin.getPrefix() + "&c找不到玩家: " + args[0]);
                return true;
            }
            String nick = sanitize(sender, String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)));
            if (nick.isEmpty()) {
                Msg.send(sender, plugin.getPrefix() + "&c昵称不能为空。");
                return true;
            }
            if (!plugin.getNicknameManager().set(uuid, args[0], nick)) {
                Msg.send(sender, plugin.getPrefix() + "&c昵称过长，最多 "
                        + plugin.getNicknameManager().maxLength() + " 个字符。");
                return true;
            }
            applyOnline(uuid);
            Msg.send(sender, plugin.getPrefix() + "&a已设置 &e" + args[0] + " &a的昵称为 &f" + Msg.color(nick));
            return true;
        }

        // /nick <nick>
        if (!(sender instanceof Player player)) {
            Msg.send(sender, plugin.getPrefix() + "&c控制台请使用 /nick <玩家> <昵称>。");
            return true;
        }
        String nick = sanitize(sender, String.join(" ", args));
        if (!plugin.getNicknameManager().set(player.getUniqueId(), player.getName(), nick)) {
            Msg.send(sender, plugin.getPrefix() + "&c昵称过长，最多 "
                    + plugin.getNicknameManager().maxLength() + " 个字符。");
            return true;
        }
        plugin.getNicknameManager().apply(player);
        plugin.getTabManager().update(player);
        Msg.send(sender, plugin.getPrefix() + "&a昵称已更新为 &f" + Msg.color(nick));
        return true;
    }

    /** 无 plu.nick.color 权限时移除颜色代码。 */
    private String sanitize(CommandSender sender, String raw) {
        String text = raw == null ? "" : raw.trim();
        if (sender.hasPermission("plu.nick.color")) return text;
        return text.replaceAll("(?i)&[0-9a-fk-or]", "").replace("§", "");
    }

    private void applyOnline(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online == null) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            plugin.getNicknameManager().rememberName(uuid, op.getName());
            return;
        }
        plugin.getNicknameManager().apply(online);
        plugin.getTabManager().update(online);
    }
}
