package com.plu.plugin.listeners;

import com.plu.plugin.Main;
import com.plu.plugin.gui.MarketGui;
import com.plu.plugin.managers.MarketManager;
import com.plu.plugin.managers.Msg;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/** 市场 GUI 点击处理。 */
public class MarketListener implements Listener {

    private final Main plugin;

    public MarketListener(Main plugin) { this.plugin = plugin; }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof MarketGui gui)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) return;

        if (slot < MarketGui.PAGE_SIZE) {
            if (slot >= gui.getPageEntries().size()) return;
            buy(player, gui, gui.getPageEntries().get(slot));
            return;
        }
        if (slot == 45) {
            if (gui.getPage() > 0) MarketGui.open(plugin, player, gui.getPage() - 1);
            return;
        }
        if (slot == 49) {
            player.closeInventory();
            showMyListings(player);
            return;
        }
        if (slot == 53) {
            if (gui.getPage() < gui.getTotalPages() - 1) MarketGui.open(plugin, player, gui.getPage() + 1);
        }
    }

    private void buy(Player buyer, MarketGui gui, MarketManager.Entry entry) {
        if (!plugin.getEconomyManager().available()) {
            Msg.send(buyer, plugin.getPrefix() + "&c经济系统不可用，无法购买。");
            return;
        }
        if (entry.seller.equals(buyer.getUniqueId())) {
            Msg.send(buyer, plugin.getPrefix() + "&c不能购买自己上架的物品。");
            return;
        }
        if (buyer.getInventory().firstEmpty() == -1) {
            Msg.send(buyer, plugin.getPrefix() + "&c背包已满，请先腾出空间。");
            return;
        }
        if (!plugin.getEconomyManager().has(buyer.getUniqueId(), entry.price)) {
            Msg.send(buyer, plugin.getPrefix() + "&c余额不足，需要 &e"
                    + plugin.getEconomyManager().format(entry.price));
            return;
        }
        if (!plugin.getEconomyManager().withdraw(buyer.getUniqueId(), entry.price)) {
            Msg.send(buyer, plugin.getPrefix() + "&c扣款失败。");
            return;
        }

        double taxPercent = plugin.getConfig().getDouble("market.tax-percent", 0);
        double tax = Math.max(0, entry.price * taxPercent / 100.0);
        double sellerIncome = entry.price - tax;
        plugin.getEconomyManager().deposit(entry.seller, sellerIncome);

        ItemStack item = entry.item.clone();
        buyer.getInventory().addItem(item);
        plugin.getMarketManager().remove(entry.id);

        Msg.send(buyer, plugin.getPrefix() + "&a购买成功: &f" + itemLabel(item)
                + " &7花费 &a" + plugin.getEconomyManager().format(entry.price));
        Player seller = Bukkit.getPlayer(entry.seller);
        if (seller != null) {
            Msg.send(seller, plugin.getPrefix() + "&a你上架的 &f" + itemLabel(item)
                    + " &a已被 &e" + buyer.getName() + " &a买下，收入 &a"
                    + plugin.getEconomyManager().format(sellerIncome)
                    + (tax > 0 ? " &7(税 " + plugin.getEconomyManager().format(tax) + ")" : ""));
        }

        // 刷新界面
        MarketGui.open(plugin, buyer, gui.getPage());
    }

    private void showMyListings(Player player) {
        var mine = plugin.getMarketManager().getBySeller(player.getUniqueId());
        if (mine.isEmpty()) {
            Msg.send(player, plugin.getPrefix() + "&7你还没有上架任何物品。用法: &e/market sell <价格>");
            return;
        }
        Msg.send(player, plugin.getPrefix() + "&e你的上架 (" + mine.size() + "):");
        for (MarketManager.Entry e : mine) {
            Msg.send(player, "  &7#" + e.id + " &f" + itemLabel(e.item)
                    + " &7- &a" + plugin.getEconomyManager().format(e.price));
        }
        Msg.send(player, plugin.getPrefix() + "&7下架请使用: &e/market remove <编号>");
    }

    private String itemLabel(ItemStack item) {
        if (item == null) return "未知物品";
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) return item.getItemMeta().getDisplayName();
        return item.getType().name() + " x" + item.getAmount();
    }
}
