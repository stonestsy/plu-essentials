package com.plu.plugin.commands.user;

import com.plu.plugin.Main;
import com.plu.plugin.gui.MarketGui;
import com.plu.plugin.managers.MarketManager;
import com.plu.plugin.managers.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class MarketCommand implements CommandExecutor {

    private final Main plugin;
    public MarketCommand(Main plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Msg.send(sender, plugin.getPrefix() + "&c仅玩家可使用市场。");
            return true;
        }
        if (!plugin.getEconomyManager().available()) {
            Msg.send(player, plugin.getPrefix() + "&c经济系统不可用（需要 Vault 与经济插件），市场暂不可用。");
            return true;
        }
        if (args.length == 0) {
            MarketGui.open(plugin, player, 0);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "sell" -> sell(player, args);
            case "my" -> listMine(player);
            case "remove" -> remove(player, args);
            case "help" -> help(player);
            default -> {
                // 允许 /market <页码>
                try {
                    int page = Integer.parseInt(args[0]);
                    MarketGui.open(plugin, player, Math.max(0, page - 1));
                } catch (NumberFormatException e) {
                    help(player);
                }
            }
        }
        return true;
    }

    private void sell(Player player, String[] args) {
        if (args.length < 2) {
            Msg.send(player, plugin.getPrefix() + "&c用法: /market sell <价格>（手上物品将被上架）");
            return;
        }
        double price;
        try { price = Double.parseDouble(args[1]); }
        catch (Exception e) { Msg.send(player, plugin.getPrefix() + "&c价格格式错误。"); return; }
        if (price <= 0) { Msg.send(player, plugin.getPrefix() + "&c价格必须大于 0。"); return; }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()) {
            Msg.send(player, plugin.getPrefix() + "&c请手持要上架的物品。");
            return;
        }

        int max = plugin.getConfig().getInt("market.max-items-per-player", 10);
        if (plugin.getMarketManager().getBySeller(player.getUniqueId()).size() >= max) {
            Msg.send(player, plugin.getPrefix() + "&c每个玩家最多上架 " + max + " 件物品。");
            return;
        }

        ItemStack listed = hand.clone();
        player.getInventory().setItemInMainHand(null);
        MarketManager.Entry entry = plugin.getMarketManager().add(player.getUniqueId(), player.getName(), listed, price);

        Msg.send(player, plugin.getPrefix() + "&a上架成功: &f" + listed.getType().name()
                + " x" + listed.getAmount() + " &7编号 &e#" + entry.id
                + " &7价格 &a" + plugin.getEconomyManager().format(price));
    }

    private void listMine(Player player) {
        var mine = plugin.getMarketManager().getBySeller(player.getUniqueId());
        if (mine.isEmpty()) {
            Msg.send(player, plugin.getPrefix() + "&7你还没有上架任何物品。");
            return;
        }
        Msg.send(player, plugin.getPrefix() + "&e你的上架 (" + mine.size() + "):");
        for (MarketManager.Entry e : mine) {
            Msg.send(player, "  &7#" + e.id + " &f" + e.item.getType().name() + " x" + e.item.getAmount()
                    + " &7- &a" + plugin.getEconomyManager().format(e.price));
        }
    }

    private void remove(Player player, String[] args) {
        if (args.length < 2) {
            Msg.send(player, plugin.getPrefix() + "&c用法: /market remove <编号>");
            return;
        }
        int id;
        try { id = Integer.parseInt(args[1]); }
        catch (Exception e) { Msg.send(player, plugin.getPrefix() + "&c编号格式错误。"); return; }
        MarketManager.Entry entry = plugin.getMarketManager().get(id);
        if (entry == null) {
            Msg.send(player, plugin.getPrefix() + "&c找不到该上架记录。");
            return;
        }
        if (!entry.seller.equals(player.getUniqueId()) && !player.hasPermission("plu.market.admin")) {
            Msg.send(player, plugin.getPrefix() + "&c这不是你上架的物品。");
            return;
        }
        if (player.getInventory().firstEmpty() == -1) {
            Msg.send(player, plugin.getPrefix() + "&c背包已满，无法取回物品。");
            return;
        }
        player.getInventory().addItem(entry.item.clone());
        plugin.getMarketManager().remove(id);
        Msg.send(player, plugin.getPrefix() + "&a已下架 #" + id + "，物品已退回背包。");
    }

    private void help(Player player) {
        Msg.send(player, plugin.getPrefix() + "&6玩家市场");
        Msg.send(player, "  &e/market &7- 打开市场界面");
        Msg.send(player, "  &e/market sell <价格> &7- 上架手持物品");
        Msg.send(player, "  &e/market my &7- 查看我的上架");
        Msg.send(player, "  &e/market remove <编号> &7- 下架并取回");
    }
}
