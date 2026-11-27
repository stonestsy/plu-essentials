package com.plu.plugin.commands.admin;

import com.plu.plugin.Main;
import com.plu.plugin.managers.GroupManager;
import com.plu.plugin.managers.Msg;
import com.plu.plugin.util.PlayerResolver;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.UUID;

/** 内置权限组管理命令。 */
public class GroupCommand implements CommandExecutor {

    private final Main plugin;

    public GroupCommand(Main plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            help(sender);
            return true;
        }
        String sub = args[0].toLowerCase();
        GroupManager gm = plugin.getGroupManager();

        switch (sub) {
            case "list" -> list(sender, gm);
            case "info" -> info(sender, gm, args);
            case "create" -> create(sender, gm, args);
            case "delete" -> delete(sender, gm, args);
            case "prefix" -> prefix(sender, gm, args);
            case "weight" -> weight(sender, gm, args);
            case "perm" -> perm(sender, gm, args);
            case "add" -> addPlayer(sender, gm, args);
            case "remove" -> removePlayer(sender, gm, args);
            case "check" -> check(sender, gm, args);
            default -> {
                Msg.send(sender, plugin.getPrefix() + "&c未知子命令，输入 &e/group &c查看帮助。");
            }
        }
        return true;
    }

    private boolean requireAdmin(CommandSender sender) {
        if (!sender.hasPermission("plu.group.admin")) {
            Msg.send(sender, plugin.getPrefix() + "&c你没有管理权限组的权限。");
            return false;
        }
        return true;
    }

    private void list(CommandSender sender, GroupManager gm) {
        Msg.send(sender, plugin.getPrefix() + "&e权限组 (" + gm.groupCount() + "):");
        for (GroupManager.Group g : gm.getAll()) {
            Msg.send(sender, "  &f" + g.name + " &7- 权重 &e" + g.weight
                    + " &7| 前缀 " + (g.prefix == null || g.prefix.isEmpty() ? "&8无" : "&f" + g.prefix)
                    + " &7| 权限节点 &e" + g.permissions.size());
        }
    }

    private void info(CommandSender sender, GroupManager gm, String[] args) {
        if (args.length < 2) { Msg.send(sender, plugin.getPrefix() + "&c用法: /group info <组名>"); return; }
        GroupManager.Group g = gm.get(args[1]);
        if (g == null) { Msg.send(sender, plugin.getPrefix() + "&c分组不存在。"); return; }
        Msg.send(sender, plugin.getPrefix() + "&e分组: &f" + g.name);
        Msg.send(sender, "  &7权重: &f" + g.weight);
        Msg.send(sender, "  &7前缀: &f" + (g.prefix == null || g.prefix.isEmpty() ? "无" : g.prefix));
        if (g.permissions.isEmpty()) {
            Msg.send(sender, "  &7权限节点: &8无");
        } else {
            Msg.send(sender, "  &7权限节点 (&e" + g.permissions.size() + "&7):");
            for (String perm : g.permissions) {
                Msg.send(sender, "    &f- " + perm);
            }
        }
    }

    private void create(CommandSender sender, GroupManager gm, String[] args) {
        if (!requireAdmin(sender)) return;
        if (args.length < 2) {
            Msg.send(sender, plugin.getPrefix() + "&c用法: /group create <组名> [前缀] [权重]");
            return;
        }
        String prefix = args.length > 2 ? args[2] : "";
        int weight = 0;
        if (args.length > 3) {
            try { weight = Integer.parseInt(args[3]); }
            catch (Exception e) { Msg.send(sender, plugin.getPrefix() + "&c权重必须是整数。"); return; }
        }
        GroupManager.Group g = gm.create(args[1], prefix, weight);
        if (g == null) {
            Msg.send(sender, plugin.getPrefix() + "&c创建失败：名称非法（仅限 a-z 0-9 _ -，最长 32）或已存在。");
            return;
        }
        Msg.send(sender, plugin.getPrefix() + "&a已创建分组 &e" + g.name);
    }

    private void delete(CommandSender sender, GroupManager gm, String[] args) {
        if (!requireAdmin(sender)) return;
        if (args.length < 2) { Msg.send(sender, plugin.getPrefix() + "&c用法: /group delete <组名>"); return; }
        if (!gm.delete(args[1])) {
            Msg.send(sender, plugin.getPrefix() + "&c删除失败：分组不存在，或试图删除 default。");
            return;
        }
        Msg.send(sender, plugin.getPrefix() + "&a已删除分组 &e" + args[1].toLowerCase());
    }

    private void prefix(CommandSender sender, GroupManager gm, String[] args) {
        if (!requireAdmin(sender)) return;
        if (args.length < 2) {
            Msg.send(sender, plugin.getPrefix() + "&c用法: /group prefix <组名> [前缀，留空清除]");
            return;
        }
        String prefix = args.length > 2
                ? String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length))
                : "";
        if (!gm.setPrefix(args[1], prefix)) {
            Msg.send(sender, plugin.getPrefix() + "&c分组不存在。");
            return;
        }
        Msg.send(sender, plugin.getPrefix() + "&a已设置 &e" + args[1].toLowerCase() + " &a的前缀。");
    }

    private void weight(CommandSender sender, GroupManager gm, String[] args) {
        if (!requireAdmin(sender)) return;
        if (args.length < 3) {
            Msg.send(sender, plugin.getPrefix() + "&c用法: /group weight <组名> <权重>");
            return;
        }
        int weight;
        try { weight = Integer.parseInt(args[2]); }
        catch (Exception e) { Msg.send(sender, plugin.getPrefix() + "&c权重必须是整数。"); return; }
        if (!gm.setWeight(args[1], weight)) {
            Msg.send(sender, plugin.getPrefix() + "&c分组不存在。");
            return;
        }
        Msg.send(sender, plugin.getPrefix() + "&a已将 &e" + args[1].toLowerCase() + " &a的权重设为 &e" + weight);
    }

    private void perm(CommandSender sender, GroupManager gm, String[] args) {
        if (!requireAdmin(sender)) return;
        if (args.length < 4) {
            Msg.send(sender, plugin.getPrefix() + "&c用法: /group perm <组名> <add|remove> <权限节点>");
            return;
        }
        String group = args[1];
        String action = args[2].toLowerCase();
        String perm = args[3];
        boolean ok;
        switch (action) {
            case "add" -> ok = gm.addPermission(group, perm);
            case "remove", "del" -> ok = gm.removePermission(group, perm);
            default -> {
                Msg.send(sender, plugin.getPrefix() + "&c动作只能是 add 或 remove。");
                return;
            }
        }
        if (!ok) {
            Msg.send(sender, plugin.getPrefix() + "&c操作失败：分组不存在，或节点无变化。");
            return;
        }
        Msg.send(sender, plugin.getPrefix() + (action.equals("add") ? "&a已为 " : "&c已从 ")
                + "&e" + group.toLowerCase() + " &7" + (action.equals("add") ? "添加" : "移除")
                + " &f" + perm);
    }

    private void addPlayer(CommandSender sender, GroupManager gm, String[] args) {
        if (!requireAdmin(sender)) return;
        if (args.length < 3) {
            Msg.send(sender, plugin.getPrefix() + "&c用法: /group add <玩家> <组名>");
            return;
        }
        OfflinePlayer op = PlayerResolver.byName(args[1]);
        if (op == null) { Msg.send(sender, plugin.getPrefix() + "&c找不到该玩家。"); return; }
        if (!gm.addPlayer(op.getUniqueId(), args[2])) {
            Msg.send(sender, plugin.getPrefix() + "&c添加失败：分组不存在，或玩家已在该组。");
            return;
        }
        Msg.send(sender, plugin.getPrefix() + "&a已将 &e" + args[1] + " &a加入分组 &e" + args[2].toLowerCase());
    }

    private void removePlayer(CommandSender sender, GroupManager gm, String[] args) {
        if (!requireAdmin(sender)) return;
        if (args.length < 3) {
            Msg.send(sender, plugin.getPrefix() + "&c用法: /group remove <玩家> <组名>");
            return;
        }
        OfflinePlayer op = PlayerResolver.byName(args[1]);
        if (op == null) { Msg.send(sender, plugin.getPrefix() + "&c找不到该玩家。"); return; }
        if (!gm.removePlayer(op.getUniqueId(), args[2])) {
            Msg.send(sender, plugin.getPrefix() + "&c移除失败：分组不存在，或玩家不在该组。");
            return;
        }
        Msg.send(sender, plugin.getPrefix() + "&a已将 &e" + args[1] + " &a从分组 &e" + args[2].toLowerCase() + " &a移除");
    }

    private void check(CommandSender sender, GroupManager gm, String[] args) {
        if (args.length < 2) {
            if (!(sender instanceof org.bukkit.entity.Player p)) {
                Msg.send(sender, plugin.getPrefix() + "&c用法: /group check <玩家>");
                return;
            }
            printPlayer(sender, gm, p.getUniqueId(), p.getName());
            return;
        }
        OfflinePlayer op = PlayerResolver.byName(args[1]);
        if (op == null) { Msg.send(sender, plugin.getPrefix() + "&c找不到该玩家。"); return; }
        printPlayer(sender, gm, op.getUniqueId(), args[1]);
    }

    private void printPlayer(CommandSender sender, GroupManager gm, UUID uuid, String name) {
        List<String> list = gm.getGroups(uuid);
        Msg.send(sender, plugin.getPrefix() + "&e" + name + " &7的分组: &f" + String.join("&7, &f", list));
        Msg.send(sender, "  &7主分组: &f" + gm.getPrimaryGroup(uuid)
                + " &7| 前缀: " + (gm.getPrefix(uuid).isEmpty() ? "&8无" : "&f" + gm.getPrefix(uuid)));
        int count = gm.effectivePermissions(uuid).size();
        Msg.send(sender, "  &7生效权限节点: &e" + count);
        if (Bukkit.getPlayer(uuid) == null) {
            Msg.send(sender, "  &8（玩家离线，权限将在下次登录时应用）");
        }
    }

    private void help(CommandSender sender) {
        Msg.send(sender, plugin.getPrefix() + "&6内置权限组");
        Msg.send(sender, "  &e/group list &7- 查看所有分组");
        Msg.send(sender, "  &e/group info <组名> &7- 查看分组详情与权限");
        Msg.send(sender, "  &e/group check [玩家] &7- 查看玩家分组与生效权限数");
        if (sender.hasPermission("plu.group.admin")) {
            Msg.send(sender, "  &e/group create <组名> [前缀] [权重] &7- 创建");
            Msg.send(sender, "  &e/group delete <组名> &7- 删除");
            Msg.send(sender, "  &e/group prefix <组名> [前缀] &7- 设置前缀");
            Msg.send(sender, "  &e/group weight <组名> <权重> &7- 设置权重（决定主分组）");
            Msg.send(sender, "  &e/group perm <组名> <add|remove> <权限节点> &7- 增删权限");
            Msg.send(sender, "  &e/group add <玩家> <组名> &7- 玩家入组");
            Msg.send(sender, "  &e/group remove <玩家> <组名> &7- 玩家离组");
        }
    }
}
