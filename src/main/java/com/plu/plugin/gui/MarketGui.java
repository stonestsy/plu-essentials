package com.plu.plugin.gui;

import com.plu.plugin.Main;
import com.plu.plugin.managers.MarketManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/** 玩家市场 GUI（54 格，45 个商品位 + 底部导航）。 */
public class MarketGui implements InventoryHolder {

    public static final int PAGE_SIZE = 45;
    private static final int SLOT_PREV = 45;
    private static final int SLOT_MY = 49;
    private static final int SLOT_NEXT = 53;

    private final Inventory inventory;
    private final List<MarketManager.Entry> pageEntries;
    private final int page;
    private final int totalPages;

    @SuppressWarnings("this-escape") // Bukkit.createInventory 需要在构造期传入 holder 自身
    public MarketGui(List<MarketManager.Entry> all, int page) {
        this.totalPages = Math.max(1, (int) Math.ceil(all.size() / (double) PAGE_SIZE));
        this.page = Math.max(0, Math.min(page, totalPages - 1));
        this.pageEntries = new ArrayList<>();
        int from = this.page * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, all.size());
        if (from < to) pageEntries.addAll(all.subList(from, to));

        this.inventory = Bukkit.createInventory(this, 54, "§8玩家市场 §7(" + (this.page + 1) + "/" + totalPages + ")");
        render();
    }

    private void render() {
        for (int i = 0; i < pageEntries.size() && i < PAGE_SIZE; i++) {
            inventory.setItem(i, display(pageEntries.get(i)));
        }
        for (int i = PAGE_SIZE; i < 54; i++) {
            inventory.setItem(i, filler());
        }
        if (page > 0) inventory.setItem(SLOT_PREV, nav(Material.ARROW, "§a上一页"));
        inventory.setItem(SLOT_MY, nav(Material.BOOK, "§b我的上架"));
        if (page < totalPages - 1) inventory.setItem(SLOT_NEXT, nav(Material.ARROW, "§a下一页"));
    }

    private ItemStack display(MarketManager.Entry entry) {
        ItemStack display = entry.item.clone();
        ItemMeta meta = display.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add("");
            lore.add("§7卖家: §f" + entry.sellerName);
            lore.add("§7价格: §e" + String.format("%.2f", entry.price));
            lore.add("§7编号: §8#" + entry.id);
            lore.add("§a点击购买");
            meta.setLore(lore);
            display.setItemMeta(meta);
        }
        return display;
    }

    private ItemStack filler() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.setDisplayName(" "); item.setItemMeta(meta); }
        return item;
    }

    private ItemStack nav(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.setDisplayName(name); item.setItemMeta(meta); }
        return item;
    }

    @Override
    public Inventory getInventory() { return inventory; }

    public List<MarketManager.Entry> getPageEntries() { return pageEntries; }
    public int getPage() { return page; }
    public int getTotalPages() { return totalPages; }

    /** 打开指定页（必须在主线程调用）。 */
    public static void open(Main plugin, Player player, int page) {
        new MarketGui(plugin.getMarketManager().getAll(), page).show(player);
    }

    public void show(Player player) {
        player.openInventory(inventory);
    }
}
