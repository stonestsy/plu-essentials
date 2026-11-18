package com.plu.plugin.managers;

import com.plu.plugin.Main;
import com.plu.plugin.storage.DatabaseManager;
import com.plu.plugin.util.Values;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;

/** 玩家市场：物品上架、购买、下架。 */
public class MarketManager {

    /** 一条上架记录。 */
    public static class Entry {
        public int id;
        public UUID seller;
        public String sellerName;
        public ItemStack item;
        public double price;
        public long created;

        public Entry(int id, UUID seller, String sellerName, ItemStack item, double price, long created) {
            this.id = id; this.seller = seller; this.sellerName = sellerName;
            this.item = item; this.price = price; this.created = created;
        }
    }

    private final Main plugin;
    private final DatabaseManager db;
    private final File file;
    private FileConfiguration data;
    private final List<Entry> entries = new ArrayList<>();
    private int nextId = 1;

    public MarketManager(Main plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db = db;
        this.file = new File(plugin.getDataFolder(), "market.yml");
    }

    public void load() {
        entries.clear();
        nextId = 1;
        if (db.isEnabled()) {
            try {
                String tbl = db.getPrefix() + "market";
                for (Object[] row : db.query("SELECT id,seller,seller_name,item,price,created FROM " + tbl + " ORDER BY id ASC")) {
                    int id = Values.asInt(row[0], 0);
                    ItemStack item = decode((String) row[3]);
                    if (item == null) continue;
                    entries.add(new Entry(id, UUID.fromString((String) row[1]), (String) row[2], item,
                            Values.asDouble(row[4], 0), Values.asLong(row[5], 0)));
                    nextId = Math.max(nextId, id + 1);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load market from MySQL: " + e.getMessage());
            }
        } else {
            if (!file.exists()) return;
            data = YamlConfiguration.loadConfiguration(file);
            nextId = data.getInt("next-id", 1);
            for (Map<?, ?> m : data.getMapList("entries")) {
                ItemStack item = decode((String) m.get("item"));
                if (item == null) continue;
                entries.add(new Entry(Values.asInt(m.get("id"), 0),
                        UUID.fromString((String) m.get("seller")),
                        (String) m.get("seller-name"),
                        item,
                        Values.asDouble(m.get("price"), 0),
                        Values.asLong(m.get("created"), 0)));
            }
        }
    }

    private void save() {
        if (db.isEnabled()) return;
        if (data == null) data = new YamlConfiguration();
        List<Map<String, Object>> list = new ArrayList<>();
        for (Entry e : entries) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", e.id);
            m.put("seller", e.seller.toString());
            m.put("seller-name", e.sellerName);
            m.put("item", encode(e.item));
            m.put("price", e.price);
            m.put("created", e.created);
            list.add(m);
        }
        data.set("next-id", nextId);
        data.set("entries", list);
        try { data.save(file); } catch (IOException ex) { ex.printStackTrace(); }
    }

    /** 上架，返回新条目。 */
    public Entry add(UUID seller, String sellerName, ItemStack item, double price) {
        int id = nextId++;
        Entry e = new Entry(id, seller, sellerName, item.clone(), price, System.currentTimeMillis());
        entries.add(e);
        if (db.isEnabled()) {
            try {
                db.update("INSERT INTO " + db.getPrefix() + "market (id,seller,seller_name,item,price,created) VALUES (?,?,?,?,?,?)",
                        id, seller.toString(), sellerName, encode(item), price, e.created);
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to save market entry: " + ex.getMessage());
            }
        } else {
            save();
        }
        return e;
    }

    /** 删除条目。 */
    public boolean remove(int id) {
        Entry target = get(id);
        if (target == null) return false;
        entries.remove(target);
        if (db.isEnabled()) {
            try { db.update("DELETE FROM " + db.getPrefix() + "market WHERE id=?", id); }
            catch (Exception ignored) {}
        } else {
            save();
        }
        return true;
    }

    public Entry get(int id) {
        for (Entry e : entries) if (e.id == id) return e;
        return null;
    }

    public List<Entry> getAll() {
        return Collections.unmodifiableList(entries);
    }

    public List<Entry> getBySeller(UUID seller) {
        List<Entry> list = new ArrayList<>();
        for (Entry e : entries) if (e.seller.equals(seller)) list.add(e);
        return list;
    }

    public int size() { return entries.size(); }

    // ---- ItemStack 序列化（Base64 + Bukkit 对象流） ----

    public static String encode(ItemStack item) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (BukkitObjectOutputStream data = new BukkitObjectOutputStream(out)) {
                data.writeObject(item);
            }
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            return null;
        }
    }

    public static ItemStack decode(String s) {
        if (s == null) return null;
        try {
            byte[] bytes = Base64.getDecoder().decode(s);
            try (BukkitObjectInputStream in = new BukkitObjectInputStream(new ByteArrayInputStream(bytes))) {
                return (ItemStack) in.readObject();
            }
        } catch (Exception e) {
            return null;
        }
    }
}
