package com.plu.plugin.commands.user;

import com.plu.plugin.Main;
import com.plu.plugin.managers.Msg;
import com.plu.plugin.managers.RedeemManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class RedeemCommand implements CommandExecutor {

    private final Main plugin;
    public RedeemCommand(Main plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            usage(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        boolean admin = sender.hasPermission("plu.redeem.admin");

        if (admin && sub.equals("create")) { create(sender, args); return true; }
        if (admin && sub.equals("list")) { list(sender); return true; }
        if (admin && sub.equals("delete")) { delete(sender, args); return true; }
        if (admin && sub.equals("toggle")) { toggle(sender, args); return true; }

        // 其余情况按兑换码处理
        redeem(sender, args[0]);
        return true;
    }

    private void redeem(CommandSender sender, String code) {
        if (!(sender instanceof Player player)) {
            Msg.send(sender, plugin.getPrefix() + "&c仅玩家可以兑换。");
            return;
        }
        if (!plugin.getEconomyManager().available()) {
            Msg.send(player, plugin.getPrefix() + "&c经济系统已关闭（config.yml 的 economy.enabled）。");
            return;
        }
        RedeemManager.Result result = plugin.getRedeemManager().redeem(player, code);
        switch (result) {
            case SUCCESS -> {
                RedeemManager.Code c = plugin.getRedeemManager().get(code);
                double amount = c != null ? c.amount : 0;
                Msg.send(player, plugin.getPrefix() + "&a兑换成功！获得 &e"
                        + plugin.getEconomyManager().format(amount));
            }
            case NOT_FOUND -> Msg.send(player, plugin.getPrefix() + "&c兑换码不存在。");
            case DISABLED -> Msg.send(player, plugin.getPrefix() + "&c该兑换码已被停用。");
            case EXPIRED -> Msg.send(player, plugin.getPrefix() + "&c该兑换码已过期。");
            case EXHAUSTED -> Msg.send(player, plugin.getPrefix() + "&c该兑换码已被用完。");
            case ALREADY_USED -> Msg.send(player, plugin.getPrefix() + "&c你已经兑换过这个码了。");
            case NO_ECONOMY -> Msg.send(player, plugin.getPrefix() + "&c经济系统不可用。");
            default -> Msg.send(player, plugin.getPrefix() + "&c兑换失败，请稍后再试。");
        }
    }

    private void create(CommandSender sender, String[] args) {
        if (args.length < 3) {
            Msg.send(sender, plugin.getPrefix() + "&c用法: /redeem create <兑换码|#> <金额> [次数] [时长]");
            Msg.send(sender, plugin.getPrefix() + "&7次数 0=不限（默认 1）；时长如 7d、1h，0 或留空=永久；# 为随机生成");
            return;
        }
        String code = args[1];
        double amount;
        try { amount = Double.parseDouble(args[2]); }
        catch (Exception e) { Msg.send(sender, plugin.getPrefix() + "&c金额必须是数字。"); return; }
        if (amount <= 0) { Msg.send(sender, plugin.getPrefix() + "&c金额必须大于 0。"); return; }

        int maxUses = 1;
        if (args.length > 3) {
            try { maxUses = Integer.parseInt(args[3]); }
            catch (Exception e) { Msg.send(sender, plugin.getPrefix() + "&c次数必须是整数。"); return; }
            if (maxUses < 0) maxUses = 0;
        }

        long expires = 0;
        if (args.length > 4) {
            long ms = parseDuration(args[4]);
            if (ms < 0) { Msg.send(sender, plugin.getPrefix() + "&c时长格式错误，例如: 7d, 12h, 0=永久"); return; }
            expires = ms == 0 ? 0 : System.currentTimeMillis() + ms;
        }

        String creator = sender instanceof Player ? sender.getName() : "Console";
        RedeemManager.Code created = plugin.getRedeemManager().create(code, amount, maxUses, expires, creator);
        if (created == null) {
            Msg.send(sender, plugin.getPrefix() + "&c创建失败：兑换码已存在或参数非法。");
            return;
        }
        Msg.send(sender, plugin.getPrefix() + "&a兑换码已创建: &e" + created.code
                + " &7金额 &a" + plugin.getEconomyManager().format(amount)
                + " &7次数 " + (maxUses == 0 ? "不限" : maxUses)
                + " &7有效期 " + (expires == 0 ? "永久" : args[4]));
    }

    private void list(CommandSender sender) {
        List<RedeemManager.Code> all = plugin.getRedeemManager().getAll();
        if (all.isEmpty()) {
            Msg.send(sender, plugin.getPrefix() + "&7当前没有兑换码。");
            return;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Msg.send(sender, plugin.getPrefix() + "&e兑换码列表 (" + all.size() + "):");
        for (RedeemManager.Code c : all) {
            String expiry = c.expires == 0 ? "永久"
                    : (System.currentTimeMillis() > c.expires ? "&c已过期" : sdf.format(new Date(c.expires)));
            String uses = (c.maxUses == 0 ? "不限" : c.usedCount + "/" + c.maxUses);
            Msg.send(sender, "  &f" + c.code + (c.enabled ? "" : " &7[停用]")
                    + " &7- 金额 &a" + plugin.getEconomyManager().format(c.amount)
                    + " &7| 使用 &e" + uses + " &7| 到期 &e" + expiry);
        }
    }

    private void delete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            Msg.send(sender, plugin.getPrefix() + "&c用法: /redeem delete <兑换码>");
            return;
        }
        if (plugin.getRedeemManager().delete(args[1])) {
            Msg.send(sender, plugin.getPrefix() + "&a已删除兑换码 &e" + args[1].toUpperCase());
        } else {
            Msg.send(sender, plugin.getPrefix() + "&c兑换码不存在。");
        }
    }

    private void toggle(CommandSender sender, String[] args) {
        if (args.length < 2) {
            Msg.send(sender, plugin.getPrefix() + "&c用法: /redeem toggle <兑换码>");
            return;
        }
        Boolean state = plugin.getRedeemManager().toggle(args[1]);
        if (state == null) {
            Msg.send(sender, plugin.getPrefix() + "&c兑换码不存在。");
            return;
        }
        Msg.send(sender, plugin.getPrefix() + (state ? "&a已启用 " : "&c已停用 ")
                + "&e" + args[1].toUpperCase());
    }

    private void usage(CommandSender sender) {
        Msg.send(sender, plugin.getPrefix() + "&6兑换码");
        Msg.send(sender, "  &e/redeem <兑换码> &7- 兑换货币");
        if (sender.hasPermission("plu.redeem.admin")) {
            Msg.send(sender, "  &e/redeem create <兑换码|#> <金额> [次数] [时长] &7- 创建");
            Msg.send(sender, "  &e/redeem list &7- 查看全部");
            Msg.send(sender, "  &e/redeem toggle <兑换码> &7- 启用/停用");
            Msg.send(sender, "  &e/redeem delete <兑换码> &7- 删除");
        }
    }

    /** 解析 7d / 12h / 30m / 0；返回 -1 表示格式错误。 */
    private long parseDuration(String s) {
        if (s == null || s.isEmpty()) return -1;
        if (s.equals("0")) return 0;
        try {
            char unit = s.charAt(s.length() - 1);
            long num = Long.parseLong(s.substring(0, s.length() - 1));
            if (num <= 0) return -1;
            return switch (unit) {
                case 's' -> num * 1000L;
                case 'm' -> num * 60_000L;
                case 'h' -> num * 3_600_000L;
                case 'd' -> num * 86_400_000L;
                case 'w' -> num * 604_800_000L;
                default -> -1;
            };
        } catch (Exception e) {
            return -1;
        }
    }
}
