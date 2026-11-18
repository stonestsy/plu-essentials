package com.plu.plugin.commands.user;

import com.plu.plugin.Main;
import com.plu.plugin.managers.ClaimManager;
import com.plu.plugin.managers.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class ClaimCommand implements CommandExecutor {

    private final Main plugin;
    public ClaimCommand(Main plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Msg.send(sender, plugin.getPrefix() + "&c仅玩家可使用领地功能。");
            return true;
        }
        if (args.length == 0) {
            info(player);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "create", "new" -> create(player, args);
            case "list", "mine" -> list(player);
            case "remove", "delete", "del" -> remove(player, args);
            case "info" -> info(player);
            default -> help(player);
        }
        return true;
    }

    private void create(Player player, String[] args) {
        int max = plugin.getClaimManager().maxPerPlayer();
        if (plugin.getClaimManager().getByOwner(player.getUniqueId()).size() >= max) {
            Msg.send(player, plugin.getPrefix() + "&c你已达到领地数量上限 (" + max + ")。");
            return;
        }
        int radius = 5;
        if (args.length > 1) {
            try { radius = Integer.parseInt(args[1]); }
            catch (Exception e) { Msg.send(player, plugin.getPrefix() + "&c半径必须是数字。"); return; }
        }
        int maxRadius = plugin.getClaimManager().maxRadius();
        if (radius < 1 || radius > maxRadius) {
            Msg.send(player, plugin.getPrefix() + "&c半径必须在 1-" + maxRadius + " 之间。");
            return;
        }
        ClaimManager.Claim claim = plugin.getClaimManager().create(player, radius);
        if (claim == null) {
            Msg.send(player, plugin.getPrefix() + "&c创建失败：该区域与其他领地重叠。");
            return;
        }
        Msg.send(player, plugin.getPrefix() + "&a领地创建成功 &7#" + claim.id
                + " &7范围 &e" + claim.sizeX() + "x" + claim.sizeZ() + " &7("
                + claim.minX + "," + claim.minZ + ") ~ (" + claim.maxX + "," + claim.maxZ + ")");
    }

    private void list(Player player) {
        List<ClaimManager.Claim> mine = plugin.getClaimManager().getByOwner(player.getUniqueId());
        if (mine.isEmpty()) {
            Msg.send(player, plugin.getPrefix() + "&7你还没有领地。使用 &e/claim create [半径] &7创建。");
            return;
        }
        Msg.send(player, plugin.getPrefix() + "&e你的领地 (" + mine.size() + "):");
        for (ClaimManager.Claim c : mine) {
            Msg.send(player, "  &7#" + c.id + " &f" + c.world + " &7(" + c.minX + "," + c.minZ + ") ~ ("
                    + c.maxX + "," + c.maxZ + ") &7面积 &e" + c.area());
        }
    }

    private void remove(Player player, String[] args) {
        if (args.length < 2) {
            Msg.send(player, plugin.getPrefix() + "&c用法: /claim remove <编号>");
            return;
        }
        int id;
        try { id = Integer.parseInt(args[1]); }
        catch (Exception e) { Msg.send(player, plugin.getPrefix() + "&c编号格式错误。"); return; }

        ClaimManager.Claim target = null;
        for (ClaimManager.Claim c : plugin.getClaimManager().getByOwner(player.getUniqueId())) {
            if (c.id == id) { target = c; break; }
        }
        if (target == null && !player.hasPermission("plu.claim.admin")) {
            Msg.send(player, plugin.getPrefix() + "&c找不到属于你的该编号领地。");
            return;
        }
        if (!plugin.getClaimManager().remove(id)) {
            Msg.send(player, plugin.getPrefix() + "&c删除失败。");
            return;
        }
        Msg.send(player, plugin.getPrefix() + "&a已删除领地 #" + id);
    }

    private void info(Player player) {
        ClaimManager.Claim claim = plugin.getClaimManager().getAt(player.getLocation());
        if (claim == null) {
            Msg.send(player, plugin.getPrefix() + "&7当前位置不属于任何领地。使用 &e/claim create [半径] &7圈地。");
            return;
        }
        boolean owner = claim.owner.equals(player.getUniqueId());
        Msg.send(player, plugin.getPrefix() + "&e领地 #" + claim.id
                + " &7归属: " + (owner ? "&a你自己" : "&f" + claim.ownerName)
                + " &7范围 &f(" + claim.minX + "," + claim.minZ + ") ~ (" + claim.maxX + "," + claim.maxZ + ")");
    }

    private void help(Player player) {
        Msg.send(player, plugin.getPrefix() + "&6领地系统");
        Msg.send(player, "  &e/claim create [半径] &7- 在以你为中心的位置创建领地");
        Msg.send(player, "  &e/claim list &7- 查看你的领地");
        Msg.send(player, "  &e/claim remove <编号> &7- 删除领地");
        Msg.send(player, "  &e/claim info &7- 查看所在位置领地");
    }
}
