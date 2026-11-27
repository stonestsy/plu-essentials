package com.plu.plugin.commands.admin;

import com.plu.plugin.Main;
import com.plu.plugin.managers.Msg;
import com.plu.plugin.managers.NpcManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

/** /npc —— 内置 / Citizens NPC 管理。 */
public class NpcCommand implements CommandExecutor {

    private final Main plugin;

    public NpcCommand(Main plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!plugin.getNpcManager().isEnabled()) {
            Msg.send(sender, plugin.getPrefix() + "&cNPC 功能已关闭。");
            return true;
        }
        if (!sender.hasPermission("plu.npc.admin")) {
            Msg.send(sender, plugin.getPrefix() + "&c你没有权限。");
            return true;
        }
        if (args.length == 0) {
            help(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list" -> {
                if (plugin.getNpcManager().all().isEmpty()) {
                    Msg.send(sender, plugin.getPrefix() + "&7当前没有 NPC。");
                    return true;
                }
                Msg.send(sender, plugin.getPrefix() + "&7NPC 列表:");
                for (NpcManager.Npc n : plugin.getNpcManager().all().values()) {
                    Msg.send(sender, " &8- &e" + n.id + " &f" + n.name + " &7[" + n.type + "] &7"
                            + n.world + " " + Math.round(n.x) + "," + Math.round(n.y) + "," + Math.round(n.z)
                            + " &8(" + n.actionType + ": " + n.actionValue + ")");
                }
            }
            case "create" -> {
                if (!(sender instanceof Player player)) {
                    Msg.send(sender, plugin.getPrefix() + "&c请在游戏内创建 NPC。");
                    return true;
                }
                if (args.length < 2) {
                    Msg.send(sender, plugin.getPrefix() + "&7用法: /npc create <名称> [villager|armor_stand|zombie|skeleton|blaze|player]");
                    return true;
                }
                String name = args[1];
                String type = args.length >= 3 ? args[2] : "villager";
                if (type.equalsIgnoreCase("player") && !plugin.getNpcManager().citizensInstalled()) {
                    Msg.send(sender, plugin.getPrefix() + "&e未检测到 Citizens，将使用村民代替玩家皮肤 NPC。");
                }
                NpcManager.Npc npc = plugin.getNpcManager().create(name, type, player.getLocation());
                if (npc == null) {
                    Msg.send(sender, plugin.getPrefix() + "&c创建失败。");
                    return true;
                }
                Msg.send(sender, plugin.getPrefix() + "&aNPC 已创建，ID: &e" + npc.id
                        + " &7（用 /npc action " + npc.id + " <command|console|menu|message> <内容> 设置交互）");
            }
            case "remove", "delete" -> {
                if (args.length < 2) { Msg.send(sender, plugin.getPrefix() + "&7用法: /npc remove <id>"); return true; }
                if (plugin.getNpcManager().remove(args[1])) {
                    Msg.send(sender, plugin.getPrefix() + "&a已删除 NPC " + args[1] + "。");
                } else {
                    Msg.send(sender, plugin.getPrefix() + "&cNPC 不存在。");
                }
            }
            case "tp" -> {
                if (!(sender instanceof Player player)) return true;
                if (args.length < 2) { Msg.send(sender, plugin.getPrefix() + "&7用法: /npc tp <id>"); return true; }
                NpcManager.Npc npc = plugin.getNpcManager().get(args[1]);
                if (npc == null) { Msg.send(sender, plugin.getPrefix() + "&cNPC 不存在。"); return true; }
                var world = Bukkit.getWorld(npc.world);
                if (world == null) { Msg.send(sender, plugin.getPrefix() + "&c世界未加载: " + npc.world); return true; }
                player.teleport(new org.bukkit.Location(world, npc.x, npc.y, npc.z, npc.yaw, npc.pitch));
                Msg.send(sender, plugin.getPrefix() + "&a已传送。");
            }
            case "action" -> {
                if (args.length < 4) {
                    Msg.send(sender, plugin.getPrefix() + "&7用法: /npc action <id> <command|console|menu|message> <内容...>");
                    return true;
                }
                NpcManager.Npc npc = plugin.getNpcManager().get(args[1]);
                if (npc == null) { Msg.send(sender, plugin.getPrefix() + "&cNPC 不存在。"); return true; }
                String type = args[2].toLowerCase();
                if (!type.equals("command") && !type.equals("console") && !type.equals("menu") && !type.equals("message")) {
                    Msg.send(sender, plugin.getPrefix() + "&c动作类型只能是 command / console / menu / message。");
                    return true;
                }
                npc.actionType = type;
                npc.actionValue = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                plugin.getNpcManager().update(npc);
                Msg.send(sender, plugin.getPrefix() + "&a已设置 NPC " + npc.id + " 的动作: &e" + type + " &7" + npc.actionValue);
            }
            case "rename" -> {
                if (args.length < 3) { Msg.send(sender, plugin.getPrefix() + "&7用法: /npc rename <id> <名称...>"); return true; }
                NpcManager.Npc npc = plugin.getNpcManager().get(args[1]);
                if (npc == null) { Msg.send(sender, plugin.getPrefix() + "&cNPC 不存在。"); return true; }
                npc.name = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                plugin.getNpcManager().spawn(npc);
                Msg.send(sender, plugin.getPrefix() + "&a已重命名。");
            }
            case "skin" -> {
                if (args.length < 3) { Msg.send(sender, plugin.getPrefix() + "&7用法: /npc skin <id> <正版玩家名>（仅 Citizens）"); return true; }
                NpcManager.Npc npc = plugin.getNpcManager().get(args[1]);
                if (npc == null) { Msg.send(sender, plugin.getPrefix() + "&cNPC 不存在。"); return true; }
                if (!plugin.getNpcManager().citizensInstalled()) {
                    Msg.send(sender, plugin.getPrefix() + "&c皮肤功能需要安装 Citizens。");
                    return true;
                }
                npc.skin = args[2];
                npc.type = "player";
                plugin.getNpcManager().spawn(npc);
                Msg.send(sender, plugin.getPrefix() + "&a已应用皮肤（Citizens）。");
            }
            default -> help(sender);
        }
        return true;
    }

    private void help(CommandSender sender) {
        Msg.send(sender, plugin.getPrefix() + "&6NPC 命令:");
        Msg.send(sender, " &e/npc create <名称> [类型] &7- 在当前位置创建");
        Msg.send(sender, " &e/npc remove <id> &7- 删除 NPC");
        Msg.send(sender, " &e/npc list &7- 列出所有 NPC");
        Msg.send(sender, " &e/npc tp <id> &7- 传送到 NPC");
        Msg.send(sender, " &e/npc action <id> <command|console|menu|message> <内容> &7- 设置点击动作");
        Msg.send(sender, " &e/npc rename <id> <名称> &7- 重命名");
        Msg.send(sender, " &e/npc skin <id> <玩家名> &7- 设置皮肤（需 Citizens）");
    }
}
