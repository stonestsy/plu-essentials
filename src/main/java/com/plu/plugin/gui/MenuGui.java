package com.plu.plugin.gui;

import com.plu.plugin.managers.Msg;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** 自定义菜单的 GUI 容器。 */
public class MenuGui implements InventoryHolder {

    private final String menuId;
    private final Inventory inventory;

    @SuppressWarnings("this-escape")
    public MenuGui(String menuId, String title, int size) {
        this.menuId = menuId;
        String safeTitle = Msg.color(title == null ? "" : title);
        if (safeTitle.codePointCount(0, safeTitle.length()) > 32) {
            safeTitle = safeTitle.substring(0, safeTitle.offsetByCodePoints(0, 32));
        }
        this.inventory = Bukkit.createInventory(this, size, safeTitle);
    }

    public String getMenuId() {
        return menuId;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
