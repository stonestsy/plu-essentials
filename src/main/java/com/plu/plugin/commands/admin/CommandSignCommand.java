package com.plu.plugin.commands.admin;

import com.plu.plugin.Main;
import com.plu.plugin.managers.Msg;
import com.plu.plugin.managers.SignManager;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

/** /cmdsign —— 命令告示牌管理。 */
public class CommandSignCommand implements CommandExecutor {

    private final Main plugin;

    public CommandSignCommand(Main plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!plugin.getSignManager().isEnabled()) {
            Msg.send(sender, plugin.getPrefix() + "&c命令告示牌功能已关闭。");
            return true;
        }
        if (!sender.hasPermission("plu.cmdsign.admin")) {
            Msg.send(sender, plugin.getPrefix() + "&c你没有权限。");
            return true;
        }
        if (args.length == 0) {
            help(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "add" -> {
                if (!(sender instanceof Player player)) {
                    Msg.send(sender, plugin.getPrefix() + "&c请在游戏内使用。");
                    return true;
                }
                if (args.length < 2) {
                    Msg.send(sender, plugin.getPrefix() + "&7用法: /cmdsign add <命令...>");
                    return true;
                }
                Block block = player.getTargetBlockExact(6);
                if (!isSign(block)) {
                    Msg.send(sender, plugin.getPrefix() + "&c请看着一个告示牌（6 格内）。");
                    return true;
                }
                String cmd = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                if (cmd.startsWith("/")) cmd = cmd.substring(1);
                plugin.getSignManager().add(block.getLocation(), cmd, "", player.getName());
                if (plugin.getConfig().getBoolean("cmdsigns.write-label", true)) {
                    writeLabel(block, cmd);
                }
                Msg.send(sender, plugin.getPrefix() + "&a命令告示牌已创建: &e/" + cmd);
                Msg.send(sender, plugin.getPrefix() + "&7可用 /cmdsign perm <权限节点> 限制使用者。");
            }
            case "remove", "delete" -> {
                if (!(sender instanceof Player player)) return true;
                Block block = player.getTargetBlockExact(6);
                if (!isSign(block)) {
                    Msg.send(sender, plugin.getPrefix() + "&c请看着一个告示牌。");
                    return true;
                }
                if (plugin.getSignManager().remove(block.getLocation())) {
                    Msg.send(sender, plugin.getPrefix() + "&a已移除该命令告示牌。");
                } else {
                    Msg.send(sender, plugin.getPrefix() + "&c这个告示牌不是命令告示牌。");
                }
            }
            case "perm" -> {
                if (!(sender instanceof Player player)) return true;
                if (args.length < 2) {
                    Msg.send(sender, plugin.getPrefix() + "&7用法: /cmdsign perm <权限节点|none>");
                    return true;
                }
                Block block = player.getTargetBlockExact(6);
                if (!isSign(block)) {
                    Msg.send(sender, plugin.getPrefix() + "&c请看着一个告示牌。");
                    return true;
                }
                SignManager.SignEntry entry = plugin.getSignManager().get(block.getLocation());
                if (entry == null) {
                    Msg.send(sender, plugin.getPrefix() + "&c这个告示牌不是命令告示牌。");
                    return true;
                }
                entry.permission = args[1].equalsIgnoreCase("none") ? "" : args[1];
                plugin.getSignManager().update(entry);
                Msg.send(sender, plugin.getPrefix() + "&a已设置权限: &e"
                        + (entry.permission.isEmpty() ? "无（所有人可用）" : entry.permission));
            }
            case "info" -> {
                if (!(sender instanceof Player player)) return true;
                Block block = player.getTargetBlockExact(6);
                if (!isSign(block)) {
                    Msg.send(sender, plugin.getPrefix() + "&c请看着一个告示牌。");
                    return true;
                }
                SignManager.SignEntry entry = plugin.getSignManager().get(block.getLocation());
                if (entry == null) {
                    Msg.send(sender, plugin.getPrefix() + "&c这个告示牌不是命令告示牌。");
                    return true;
                }
                Msg.send(sender, plugin.getPrefix() + "&7命令: &e/" + entry.command);
                Msg.send(sender, plugin.getPrefix() + "&7权限: &e"
                        + (entry.permission.isEmpty() ? "无" : entry.permission));
                Msg.send(sender, plugin.getPrefix() + "&7创建者: &e" + entry.creator);
            }
            case "list" -> {
                if (plugin.getSignManager().all().isEmpty()) {
                    Msg.send(sender, plugin.getPrefix() + "&7当前没有命令告示牌。");
                    return true;
                }
                Msg.send(sender, plugin.getPrefix() + "&7命令告示牌 (" + plugin.getSignManager().all().size() + "):");
                for (SignManager.SignEntry e : plugin.getSignManager().all()) {
                    Msg.send(sender, " &8- &e" + e.world + " " + e.x + "," + e.y + "," + e.z
                            + " &f/" + e.command + (e.permission.isEmpty() ? "" : " &7[" + e.permission + "]"));
                }
            }
            default -> help(sender);
        }
        return true;
    }

    private boolean isSign(Block block) {
        return block != null && block.getState() instanceof Sign;
    }

    private void writeLabel(Block block, String command) {
        if (!(block.getState() instanceof Sign sign)) return;
        sign.getSide(Side.FRONT).setLine(0, Msg.color("&e[命令]"));
        String line = Msg.color("&f/" + command);
        sign.getSide(Side.FRONT).setLine(1, line.length() > 15 ? line.substring(0, 15) : line);
        sign.getSide(Side.FRONT).setLine(2, Msg.color("&7右键执行"));
        sign.update(true);
    }

    private void help(CommandSender sender) {
        Msg.send(sender, plugin.getPrefix() + "&6命令告示牌:");
        Msg.send(sender, " &e/cmdsign add <命令...> &7- 看着告示牌创建");
        Msg.send(sender, " &e/cmdsign remove &7- 移除看着的命令告示牌");
        Msg.send(sender, " &e/cmdsign perm <节点|none> &7- 设置使用权限");
        Msg.send(sender, " &e/cmdsign info &7- 查看告示牌信息");
        Msg.send(sender, " &e/cmdsign list &7- 列出全部命令告示牌");
        Msg.send(sender, " &8支持 &fconsole:&7 前缀以控制台身份执行，&f{player}&7/&f{nick}&7 占位符");
    }
}
