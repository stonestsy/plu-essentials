package com.plu.plugin.managers;

import com.plu.plugin.Main;
import com.plu.plugin.gui.MenuGui;
import com.plu.plugin.util.Values;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 自定义菜单：YAML 定义，GUI 展示，点击执行命令/打开子菜单。 */
public class MenuManager {

    public static class MenuItem {
        public int slot;
        public String material = "STONE";
        public String name = "";
        public List<String> lore = new ArrayList<>();
        public int amount = 1;
        public String permission = "";
        public List<String> commands = new ArrayList<>();
        public String open = "";
        public boolean close = false;
    }

    public static class Menu {
        public String id;
        public String title = "&8菜单";
        public int size = 27;
        public List<MenuItem> items = new ArrayList<>();
    }

    private final Main plugin;
    private final File file;
    private final Map<String, Menu> menus = new LinkedHashMap<>();

    public MenuManager(Main plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "menus.yml");
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("menus.enabled", true);
    }

    public void load() {
        menus.clear();
        if (!file.exists()) {
            createDefault();
            save();
            return;
        }
        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = data.getConfigurationSection("menus");
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) continue;
            Menu menu = new Menu();
            menu.id = id;
            menu.title = section.getString("title", "&8" + id);
            menu.size = Values.asInt(section.get("size"), 27);
            for (Map<?, ?> raw : section.getMapList("items")) {
                MenuItem item = new MenuItem();
                item.slot = Values.asInt(raw.get("slot"), 0);
                item.material = raw.get("material") == null ? "STONE" : String.valueOf(raw.get("material"));
                item.name = raw.get("name") == null ? "" : String.valueOf(raw.get("name"));
                item.amount = Values.asInt(raw.get("amount"), 1);
                item.permission = raw.get("permission") == null ? "" : String.valueOf(raw.get("permission"));
                item.open = raw.get("open") == null ? "" : String.valueOf(raw.get("open"));
                item.close = Values.asBool(raw.get("close"), false);
                item.lore = stringList(raw.get("lore"));
                item.commands = stringList(raw.get("commands"));
                menu.items.add(item);
            }
            menus.put(id, menu);
        }
        if (!menus.containsKey("main")) {
            createDefault();
            save();
        }
    }

    private void createDefault() {
        Menu main = new Menu();
        main.id = "main";
        main.title = "&8&l服务器菜单";
        main.size = 27;

        main.items.add(item(11, "COMPASS", "&a传送", List.of("&7点击传送到出生点"), "spawn", ""));
        main.items.add(item(13, "GOLD_INGOT", "&6我的余额", List.of("&7查看金币余额"), "money", ""));
        main.items.add(item(15, "BOOK", "&e服务器规则", List.of("&7阅读服务器规则"), "rules", ""));
        menus.put("main", main);
    }

    private MenuItem item(int slot, String material, String name, List<String> lore, String command, String open) {
        MenuItem item = new MenuItem();
        item.slot = slot;
        item.material = material;
        item.name = name;
        item.lore = new ArrayList<>(lore);
        if (command != null && !command.isEmpty()) item.commands.add(command);
        item.open = open == null ? "" : open;
        return item;
    }

    private List<String> stringList(Object raw) {
        List<String> list = new ArrayList<>();
        if (raw instanceof List<?> l) {
            for (Object o : l) if (o != null) list.add(String.valueOf(o));
        }
        return list;
    }

    public void save() {
        YamlConfiguration data = new YamlConfiguration();
        for (Menu menu : menus.values()) {
            String base = "menus." + menu.id;
            data.set(base + ".title", menu.title);
            data.set(base + ".size", menu.size);
            List<Map<String, Object>> items = new ArrayList<>();
            for (MenuItem item : menu.items) {
                Map<String, Object> raw = new LinkedHashMap<>();
                raw.put("slot", item.slot);
                raw.put("material", item.material);
                raw.put("name", item.name);
                raw.put("amount", item.amount);
                if (!item.lore.isEmpty()) raw.put("lore", item.lore);
                if (!item.permission.isEmpty()) raw.put("permission", item.permission);
                if (!item.commands.isEmpty()) raw.put("commands", item.commands);
                if (!item.open.isEmpty()) raw.put("open", item.open);
                if (item.close) raw.put("close", true);
                items.add(raw);
            }
            data.set(base + ".items", items);
        }
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save menus.yml: " + e.getMessage());
        }
    }

    public Menu get(String id) {
        return id == null ? null : menus.get(id);
    }

    public Map<String, Menu> all() {
        return new LinkedHashMap<>(menus);
    }

    public Menu create(String id, int size) {
        if (id == null || id.isEmpty() || menus.containsKey(id)) return null;
        Menu menu = new Menu();
        menu.id = id;
        menu.title = "&8" + id;
        menu.size = size;
        menus.put(id, menu);
        save();
        return menu;
    }

    public boolean delete(String id) {
        if (menus.remove(id) == null) return false;
        save();
        return true;
    }

    public void put(Menu menu) {
        if (menu == null || menu.id == null || menu.id.isEmpty()) return;
        menus.put(menu.id, menu);
        save();
    }

    /** 打开菜单；不存在时给出提示。 */
    public boolean open(Player player, String id) {
        Menu menu = get(id);
        if (menu == null) return false;
        int size = normalizeSize(menu.size);
        MenuGui gui = new MenuGui(menu.id, menu.title, size);
        for (MenuItem item : menu.items) {
            if (item.slot < 0 || item.slot >= size) continue;
            if (!item.permission.isEmpty() && !player.hasPermission(item.permission)) continue;
            Material material = Material.matchMaterial(item.material == null ? "STONE" : item.material);
            if (material == null) material = Material.STONE;
            int amount = Math.max(1, Math.min(64, item.amount));
            ItemStack stack = new ItemStack(material, amount);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                if (item.name != null && !item.name.isEmpty()) meta.setDisplayName(Msg.color(item.name));
                if (!item.lore.isEmpty()) meta.setLore(item.lore.stream().map(Msg::color).toList());
                stack.setItemMeta(meta);
            }
            gui.getInventory().setItem(item.slot, stack);
        }
        player.openInventory(gui.getInventory());
        return true;
    }

    public static int normalizeSize(int size) {
        int s = Math.max(9, Math.min(54, size));
        return (s / 9) * 9;
    }
}
