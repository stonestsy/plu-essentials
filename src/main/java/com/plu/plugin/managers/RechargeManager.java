package com.plu.plugin.managers;

import com.plu.plugin.Main;
import com.plu.plugin.pay.WeChatPayClient;
import com.plu.plugin.storage.DatabaseManager;
import com.plu.plugin.util.Values;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 充值系统：充值礼包、绑定码、微信 JSAPI 订单、回调发货。
 * 未配置微信商户凭据时自动进入模拟支付模式。
 */
public class RechargeManager {

    public static class Package {
        public String id;
        public String name;
        public long priceFen;
        public double coins;
        public boolean enabled = true;
    }

    public static class Order {
        public String no;
        public String uuid;
        public String player;
        public String packageId;
        public long amountFen;
        public double coins;
        public String status = "PENDING";
        public long created;
        public long paidAt;
        public String openid = "";
        public String prepayId = "";
        public String transactionId = "";
    }

    private static class BindCode {
        UUID uuid;
        String name;
        long expires;
    }

    private static class Session {
        String openid;
        long expires;
    }

    private final Main plugin;
    private final DatabaseManager db;
    private final WeChatPayClient client;
    private final File packageFile;
    private final File orderFile;

    private final Map<String, Package> packages = new LinkedHashMap<>();
    private final Map<String, Order> orders = new ConcurrentHashMap<>();
    private final Map<String, BindCode> bindCodes = new ConcurrentHashMap<>();
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    private final SecureRandom random = new SecureRandom();
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    public RechargeManager(Main plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db = db;
        this.client = new WeChatPayClient(plugin);
        this.packageFile = new File(plugin.getDataFolder(), "pay-packages.yml");
        this.orderFile = new File(plugin.getDataFolder(), "pay-orders.yml");
    }

    public WeChatPayClient client() {
        return client;
    }

    public boolean isEnabled() {
        return client.isEnabled();
    }

    public boolean isMock() {
        return client.isMock();
    }

    // ---------------- 加载/保存 ----------------

    public void load() {
        loadPackages();
        loadOrders();
    }

    private void loadPackages() {
        packages.clear();
        if (!packageFile.exists()) {
            createDefaultPackages();
            savePackages();
            return;
        }
        YamlConfiguration data = YamlConfiguration.loadConfiguration(packageFile);
        for (Map<?, ?> raw : data.getMapList("packages")) {
            Package p = new Package();
            p.id = raw.get("id") == null ? "" : String.valueOf(raw.get("id"));
            p.name = raw.get("name") == null ? p.id : String.valueOf(raw.get("name"));
            p.priceFen = Values.asLong(raw.get("price-fen"), 0);
            p.coins = Values.asDouble(raw.get("coins"), 0);
            p.enabled = Values.asBool(raw.get("enabled"), true);
            if (!p.id.isEmpty()) packages.put(p.id, p);
        }
        if (packages.isEmpty()) {
            createDefaultPackages();
            savePackages();
        }
    }

    private void createDefaultPackages() {
        packages.put("p100", pkg("p100", "100 金币", 100, 100));
        packages.put("p600", pkg("p600", "600 金币", 500, 600));
        packages.put("p1300", pkg("p1300", "1300 金币", 1000, 1300));
    }

    private Package pkg(String id, String name, long fen, double coins) {
        Package p = new Package();
        p.id = id;
        p.name = name;
        p.priceFen = fen;
        p.coins = coins;
        return p;
    }

    public void savePackages() {
        YamlConfiguration data = new YamlConfiguration();
        List<Map<String, Object>> list = new ArrayList<>();
        for (Package p : packages.values()) {
            Map<String, Object> raw = new LinkedHashMap<>();
            raw.put("id", p.id);
            raw.put("name", p.name);
            raw.put("price-fen", p.priceFen);
            raw.put("coins", p.coins);
            raw.put("enabled", p.enabled);
            list.add(raw);
        }
        data.set("packages", list);
        try {
            data.save(packageFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save pay-packages.yml: " + e.getMessage());
        }
    }

    private void loadOrders() {
        orders.clear();
        if (db.isEnabled()) {
            try {
                for (Object[] r : db.query("SELECT no,uuid,player,package_id,amount_fen,coins,status,created,paid_at,openid,prepay_id,transaction_id FROM "
                        + db.getPrefix() + "pay_orders")) {
                    Order o = new Order();
                    o.no = String.valueOf(r[0]);
                    o.uuid = r[1] == null ? "" : String.valueOf(r[1]);
                    o.player = r[2] == null ? "" : String.valueOf(r[2]);
                    o.packageId = r[3] == null ? "" : String.valueOf(r[3]);
                    o.amountFen = Values.asLong(r[4], 0);
                    o.coins = Values.asDouble(r[5], 0);
                    o.status = r[6] == null ? "PENDING" : String.valueOf(r[6]);
                    o.created = Values.asLong(r[7], 0);
                    o.paidAt = Values.asLong(r[8], 0);
                    o.openid = r[9] == null ? "" : String.valueOf(r[9]);
                    o.prepayId = r[10] == null ? "" : String.valueOf(r[10]);
                    o.transactionId = r[11] == null ? "" : String.valueOf(r[11]);
                    orders.put(o.no, o);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load pay orders from MySQL: " + e.getMessage());
            }
            return;
        }

        if (!orderFile.exists()) return;
        YamlConfiguration data = YamlConfiguration.loadConfiguration(orderFile);
        ConfigurationSection root = data.getConfigurationSection("orders");
        if (root == null) return;
        for (String no : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(no);
            if (s == null) continue;
            Order o = new Order();
            o.no = no;
            o.uuid = s.getString("uuid", "");
            o.player = s.getString("player", "");
            o.packageId = s.getString("package-id", "");
            o.amountFen = s.getLong("amount-fen");
            o.coins = s.getDouble("coins");
            o.status = s.getString("status", "PENDING");
            o.created = s.getLong("created");
            o.paidAt = s.getLong("paid-at");
            o.openid = s.getString("openid", "");
            o.prepayId = s.getString("prepay-id", "");
            o.transactionId = s.getString("transaction-id", "");
            orders.put(no, o);
        }
    }

    private void saveOrders() {
        if (db.isEnabled()) return;
        YamlConfiguration data = new YamlConfiguration();
        for (Order o : orders.values()) {
            String base = "orders." + o.no;
            data.set(base + ".uuid", o.uuid);
            data.set(base + ".player", o.player);
            data.set(base + ".package-id", o.packageId);
            data.set(base + ".amount-fen", o.amountFen);
            data.set(base + ".coins", o.coins);
            data.set(base + ".status", o.status);
            data.set(base + ".created", o.created);
            data.set(base + ".paid-at", o.paidAt);
            data.set(base + ".openid", o.openid);
            data.set(base + ".prepay-id", o.prepayId);
            data.set(base + ".transaction-id", o.transactionId);
        }
        try {
            data.save(orderFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save pay-orders.yml: " + e.getMessage());
        }
    }

    private void persistOrder(Order o) {
        if (db.isEnabled()) {
            try {
                db.update("INSERT INTO " + db.getPrefix()
                                + "pay_orders (no,uuid,player,package_id,amount_fen,coins,status,created,paid_at,openid,prepay_id,transaction_id) "
                                + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?) "
                                + "ON DUPLICATE KEY UPDATE status=VALUES(status),paid_at=VALUES(paid_at),"
                                + "openid=VALUES(openid),prepay_id=VALUES(prepay_id),transaction_id=VALUES(transaction_id)",
                        o.no, o.uuid, o.player, o.packageId, o.amountFen, o.coins, o.status,
                        o.created, o.paidAt, o.openid, o.prepayId, o.transactionId);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to save pay order: " + e.getMessage());
            }
            return;
        }
        saveOrders();
    }

    // ---------------- 礼包 ----------------

    public List<Package> packages() {
        return new ArrayList<>(packages.values());
    }

    public Package packageById(String id) {
        return id == null ? null : packages.get(id);
    }

    public Package upsertPackage(Package p) {
        if (p == null || p.id == null || p.id.isBlank()) return null;
        if (p.priceFen <= 0 || p.coins <= 0) return null;
        packages.put(p.id, p);
        savePackages();
        return p;
    }

    public boolean deletePackage(String id) {
        if (id == null || packages.remove(id) == null) return false;
        savePackages();
        return true;
    }

    // ---------------- 绑定码 / 会话 ----------------

    public String createBindCode(Player player) {
        bindCodes.entrySet().removeIf(e -> e.getValue().uuid.equals(player.getUniqueId()));
        String code;
        do {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 6; i++) sb.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
            code = sb.toString();
        } while (bindCodes.containsKey(code));

        BindCode bc = new BindCode();
        bc.uuid = player.getUniqueId();
        bc.name = player.getName();
        bc.expires = System.currentTimeMillis() + 10 * 60_000L;
        bindCodes.put(code, bc);
        return code;
    }

    /** 校验绑定码；consume=true 时一次性消费。 */
    public UUID resolveBindCode(String code, boolean consume) {
        if (code == null) return null;
        String key = code.trim().toUpperCase();
        BindCode bc = bindCodes.get(key);
        if (bc == null) return null;
        if (System.currentTimeMillis() > bc.expires) {
            bindCodes.remove(key);
            return null;
        }
        if (consume) bindCodes.remove(key);
        return bc.uuid;
    }

    public String bindCodeName(String code) {
        BindCode bc = bindCodes.get(code == null ? "" : code.trim().toUpperCase());
        return bc == null ? null : bc.name;
    }

    public String createSession(String openid) {
        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Session session = new Session();
        session.openid = openid;
        session.expires = System.currentTimeMillis() + 10 * 60_000L;
        sessions.put(token, session);
        return token;
    }

    public String sessionOpenId(String token) {
        if (token == null) return null;
        Session session = sessions.get(token);
        if (session == null) return null;
        if (System.currentTimeMillis() > session.expires) {
            sessions.remove(token);
            return null;
        }
        return session.openid;
    }

    // ---------------- 订单 ----------------

    public Order createOrder(UUID uuid, String playerName, Package pkg, String openid) throws Exception {
        if (uuid == null || pkg == null || !pkg.enabled) throw new IllegalStateException("礼包不可用");
        long min = plugin.getConfig().getLong("recharge.min-fen", 1);
        long max = plugin.getConfig().getLong("recharge.max-fen", 1000000);
        if (pkg.priceFen < min || pkg.priceFen > max) throw new IllegalStateException("金额超出允许范围");

        Order order = new Order();
        order.no = newOrderNo();
        order.uuid = uuid.toString();
        order.player = playerName;
        order.packageId = pkg.id;
        order.amountFen = pkg.priceFen;
        order.coins = pkg.coins;
        order.created = System.currentTimeMillis();
        order.openid = openid == null ? "" : openid;

        if (!isMock()) {
            order.prepayId = client.createJsapiOrder(order.no, order.amountFen, openid);
        }
        orders.put(order.no, order);
        persistOrder(order);
        return order;
    }

    public Map<String, String> payParams(Order order) throws Exception {
        if (order == null || order.prepayId == null || order.prepayId.isEmpty()) {
            throw new IllegalStateException("订单未生成预支付号");
        }
        return client.buildPayParams(order.prepayId);
    }

    public Order order(String no) {
        return no == null ? null : orders.get(no);
    }

    /** 回调/模拟/人工确认统一入口；幂等。 */
    public synchronized boolean markPaid(String no, String transactionId, long amountFen, String source) {
        Order order = orders.get(no);
        if (order == null) return false;
        if ("PAID".equals(order.status)) return true;
        if (!"PENDING".equals(order.status)) return false;
        if (amountFen > 0 && amountFen != order.amountFen) {
            plugin.getLogger().warning("充值订单金额不匹配: " + no + " 期望 " + order.amountFen + " 实收 " + amountFen);
            return false;
        }
        order.status = "PAID";
        order.paidAt = System.currentTimeMillis();
        order.transactionId = transactionId == null ? "" : transactionId;
        persistOrder(order);

        UUID uuid;
        try {
            uuid = UUID.fromString(order.uuid);
        } catch (IllegalArgumentException e) {
            return false;
        }
        plugin.getEconomyManager().deposit(uuid, order.coins);

        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            String msg = plugin.getPrefix() + "&a充值成功！&e" + plugin.getEconomyManager().format(order.coins)
                    + " &a已到账 &7(订单 " + order.no + ")";
            Bukkit.getScheduler().runTask(plugin, () -> Msg.send(online, msg));
        }
        plugin.getLogger().info("Recharge paid: " + no + " " + order.player + " +" + order.coins + " (" + source + ")");
        return true;
    }

    public List<Order> recentOrders(int limit) {
        List<Order> list = new ArrayList<>(orders.values());
        list.sort((a, b) -> Long.compare(b.created, a.created));
        if (list.size() > limit) return list.subList(0, limit);
        return list;
    }

    public Map<String, Object> toWeb(Order o) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("no", o.no);
        m.put("player", o.player);
        m.put("uuid", o.uuid);
        m.put("packageId", o.packageId);
        m.put("amountFen", o.amountFen);
        m.put("amount", o.amountFen / 100.0);
        m.put("coins", o.coins);
        m.put("status", o.status);
        m.put("created", o.created);
        m.put("paidAt", o.paidAt);
        m.put("transactionId", o.transactionId);
        return m;
    }

    private String newOrderNo() {
        String time = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                .withZone(ZoneId.of("Asia/Shanghai")).format(Instant.now());
        int suffix = random.nextInt(900000) + 100000;
        return "PLU" + time + suffix;
    }
}
