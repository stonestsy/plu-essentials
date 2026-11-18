package com.plu.plugin.web;

import com.plu.plugin.Main;
import com.plu.plugin.managers.TaskManager;
import com.plu.plugin.util.PlayerResolver;
import com.plu.plugin.web.hooks.LuckPermsHook;
import com.plu.plugin.web.hooks.VaultHook;
import com.sun.net.httpserver.HttpExchange;
import org.bukkit.*;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.util.*;

/** Dispatches /api/* requests to handlers. All mutations run on the main thread. */
public class ApiRouter {

    private final Main plugin;
    private final Auth auth;
    private final ConsoleBuffer consoleBuffer;
    private final TpsTracker tpsTracker;

    public ApiRouter(Main plugin, Auth auth, ConsoleBuffer consoleBuffer, TpsTracker tpsTracker) {
        this.plugin = plugin;
        this.auth = auth;
        this.consoleBuffer = consoleBuffer;
        this.tpsTracker = tpsTracker;
    }

    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String[] parts = path.split("/");
        // parts: ["", "api", ...]

        // CORS preflight
        if (method.equals("OPTIONS")) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Authorization, Content-Type");
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        // Login does not require auth
        if (path.equals("/api/login") && method.equals("POST")) {
            handleLogin(exchange);
            return;
        }

        // Auth check for everything else
        String token = extractToken(exchange);
        if (!auth.isValid(token)) {
            WebServer.sendJson(exchange, 401, "{\"error\":\"Unauthorized\"}");
            return;
        }

        if (path.equals("/api/logout") && method.equals("POST")) {
            auth.revoke(token);
            WebServer.sendJson(exchange, 200, "{\"ok\":true}");
            return;
        }

        // Dispatch
        try {
            switch (path) {
                case "/api/dashboard" -> handleDashboard(exchange);
                case "/api/players" -> handlePlayers(exchange);
                case "/api/console" -> {
                    if (method.equals("GET")) handleConsoleGet(exchange);
                    else handleConsolePost(exchange);
                }
                case "/api/plugins" -> handlePlugins(exchange);
                case "/api/worlds" -> handleWorlds(exchange);
                default -> {
                    if (path.startsWith("/api/players/")) handlePlayerAction(exchange, parts, method);
                    else if (path.startsWith("/api/bans")) handleBans(exchange, parts, method);
                    else if (path.startsWith("/api/warps")) handleWarps(exchange, parts, method);
                    else if (path.startsWith("/api/tasks")) handleTasks(exchange, parts, method);
                    else if (path.startsWith("/api/plugins/")) handlePluginAction(exchange, parts, method);
                    else if (path.startsWith("/api/worlds/")) handleWorldAction(exchange, parts, method);
                    else if (path.startsWith("/api/economy/")) handleEconomy(exchange, parts, method);
                    else if (path.startsWith("/api/permissions/")) handlePermissions(exchange, parts);
                    else if (path.startsWith("/api/homes/")) handleHomes(exchange, parts);
                    else if (path.startsWith("/api/ipbans")) handleIpBans(exchange, parts, method);
                    else if (path.startsWith("/api/whitelist")) handleWhitelist(exchange, parts, method);
                    else if (path.startsWith("/api/chat")) handleChat(exchange);
                    else if (path.startsWith("/api/stats/")) handleStats(exchange, parts);
                    else if (path.startsWith("/api/market")) handleMarket(exchange, parts, method);
                    else if (path.startsWith("/api/claims")) handleClaims(exchange, parts, method);
                    else if (path.startsWith("/api/announcements")) handleAnnouncements(exchange, parts, method);
                    else if (path.startsWith("/api/accounts")) handleAccounts(exchange);
                    else if (path.startsWith("/api/redeem")) handleRedeem(exchange, parts, method);
                    else if (path.equals("/api/broadcast") && method.equals("POST")) handleBroadcast(exchange);
                    else if (path.equals("/api/server/stop") && method.equals("POST")) handleServerStop(exchange);
                    else if (path.equals("/api/server/reload") && method.equals("POST")) handleServerReload(exchange);
                    else WebServer.sendJson(exchange, 404, "{\"error\":\"Not found\"}");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("API error: " + e);
            WebServer.sendJson(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private String extractToken(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        // also allow ?token= query param
        String query = exchange.getRequestURI().getQuery();
        if (query != null) {
            for (String p : query.split("&")) {
                if (p.startsWith("token=")) return p.substring(6);
            }
        }
        return null;
    }

    // ---------- Login ----------
    private void handleLogin(HttpExchange exchange) throws IOException {
        String body = WebServer.readBody(exchange);
        String password = extractJsonField(body, "password");
        if (password != null && auth.checkPassword(password)) {
            String token = auth.issueToken();
            WebServer.sendJson(exchange, 200, Json.stringify(Map.of("token", token)));
        } else {
            WebServer.sendJson(exchange, 401, "{\"error\":\"密码错误\"}");
        }
    }

    private String extractJsonField(String body, String field) {
        // crude JSON field extractor for flat objects
        String key = "\"" + field + "\"";
        int i = body.indexOf(key);
        if (i < 0) return null;
        int colon = body.indexOf(':', i + key.length());
        if (colon < 0) return null;
        int start = colon + 1;
        while (start < body.length() && (body.charAt(start) == ' ' || body.charAt(start) == '"')) start++;
        int end = start;
        if (start < body.length() && body.charAt(start - 1) == '"') {
            end = body.indexOf('"', start);
        } else {
            end = body.indexOf(',', start);
            if (end < 0) end = body.indexOf('}', start);
        }
        if (end < 0) end = body.length();
        return body.substring(start, end).trim();
    }

    // ---------- Dashboard ----------
    private void handleDashboard(HttpExchange exchange) throws IOException {
        Runtime rt = Runtime.getRuntime();
        long total = rt.totalMemory();
        long free = rt.freeMemory();
        long used = total - free;
        long uptime = System.currentTimeMillis() - plugin.getServer().getWorlds().get(0).getFullTime() * 50L;
        // better: use a start time from plugin enable
        uptime = plugin.getPluginTime();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("tps", tpsTracker.getTps());
        data.put("online", Bukkit.getOnlinePlayers().size());
        data.put("maxPlayers", Bukkit.getMaxPlayers());
        data.put("version", Bukkit.getVersion());
        data.put("bukkitVersion", Bukkit.getBukkitVersion());
        data.put("motd", Bukkit.getMotd());
        data.put("memoryUsedMb", used / (1024 * 1024));
        data.put("memoryMaxMb", rt.maxMemory() / (1024 * 1024));
        data.put("memoryPercent", Math.round(used * 100.0 / rt.maxMemory()));
        data.put("uptimeMs", uptime);
        data.put("vaultEnabled", VaultHook.isAvailable());
        data.put("vaultInstalled", VaultHook.isPluginInstalled());
        data.put("luckpermsEnabled", LuckPermsHook.isAvailable());
        data.put("pluginName", plugin.getName());
        data.put("pluginVersion", plugin.getDescription().getVersion());
        WebServer.sendJson(exchange, 200, Json.stringify(data));
    }

    // ---------- Players ----------
    private void handlePlayers(HttpExchange exchange) throws IOException {
        List<Map<String, Object>> players = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", p.getName());
            m.put("uuid", p.getUniqueId().toString());
            m.put("health", Math.round(p.getHealth() * 10.0) / 10.0);
            m.put("food", p.getFoodLevel());
            m.put("level", p.getLevel());
            m.put("gamemode", p.getGameMode().name().toLowerCase());
            m.put("flying", p.isFlying());
            m.put("god", plugin.getGodManager().isGod(p.getUniqueId()));
            m.put("vanished", plugin.getVanishManager().isVanished(p.getUniqueId()));
            m.put("world", p.getWorld().getName());
            m.put("ip", p.getAddress() != null ? p.getAddress().getAddress().getHostAddress() : "");
            m.put("ping", p.getPing());
            m.put("primaryGroup", LuckPermsHook.getPrimaryGroup(p));
            if (VaultHook.isAvailable()) {
                m.put("balance", VaultHook.getBalance(p));
            }
            players.add(m);
        }
        WebServer.sendJson(exchange, 200, Json.stringify(Map.of("players", players)));
    }

    private void handlePlayerAction(HttpExchange exchange, String[] parts, String method) throws IOException {
        // /api/players/{name}/{action}
        if (parts.length < 4) {
            WebServer.sendJson(exchange, 400, "{\"error\":\"Bad request\"}");
            return;
        }
        String name = parts[3];
        String action = parts.length > 4 ? parts[4] : "";
        Player target = Bukkit.getPlayer(name);
        if (target == null) {
            WebServer.sendJson(exchange, 404, "{\"error\":\"Player not online\"}");
            return;
        }

        String body = "POST".equals(method) ? WebServer.readBody(exchange) : "";
        String value = extractJsonField(body, "value");

        boolean[] success = {false};
        String[] message = {"ok"};

        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                switch (action) {
                    case "kick" -> { target.kickPlayer(ChatColor.RED + "Kicked by admin."); success[0] = true; }
                    case "heal" -> { target.setHealth(target.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue()); target.setFoodLevel(20); success[0] = true; }
                    case "feed" -> { target.setFoodLevel(20); target.setSaturation(20); success[0] = true; }
                    case "kill" -> { target.setHealth(0); success[0] = true; }
                    case "god" -> { plugin.getGodManager().toggle(target.getUniqueId()); success[0] = true; }
                    case "fly" -> { boolean f = !target.getAllowFlight(); target.setAllowFlight(f); target.setFlying(f); success[0] = true; }
                    case "clear" -> { target.getInventory().clear(); success[0] = true; }
                    case "gamemode" -> {
                        if (value == null) { message[0] = "value required"; return; }
                        GameMode gm = parseGm(value);
                        if (gm == null) { message[0] = "invalid gamemode"; return; }
                        target.setGameMode(gm); success[0] = true;
                    }
                    case "message" -> {
                        if (value == null) { message[0] = "value required"; return; }
                        target.sendMessage(ChatColor.translateAlternateColorCodes('&', value));
                        success[0] = true;
                    }
                    case "teleport" -> {
                        Location spawn = plugin.getSpawnManager().getSpawn();
                        if (spawn != null) { target.teleport(spawn); success[0] = true; }
                        else message[0] = "spawn not set";
                    }
                    case "inventory" -> {
                        // handled separately below via inventory data
                        success[0] = true;
                    }
                    case "ban" -> {
                        String r = value != null ? value : "违反服务器规则";
                        plugin.getBanManager().ban(target.getUniqueId(), target.getName(), r, "WebAdmin", 0);
                        success[0] = true;
                    }
                    default -> message[0] = "unknown action: " + action;
                }
            } catch (Exception e) {
                message[0] = e.getMessage();
            }
        });

        // For inventory, return the serialized inventory instead
        if ("inventory".equals(action)) {
            try { Thread.sleep(80); } catch (InterruptedException ignored) {}
            WebServer.sendJson(exchange, 200, Json.stringify(Map.of(
                    "player", target.getName(),
                    "inventory", serializeInventory(target))));
            return;
        }

        // Wait briefly for the main-thread task to complete (it's fast)
        try { Thread.sleep(150); } catch (InterruptedException ignored) {}
        WebServer.sendJson(exchange, success[0] ? 200 : 400,
                Json.stringify(Map.of("success", success[0], "message", message[0])));
    }

    private List<Map<String, Object>> serializeInventory(Player player) {
        List<Map<String, Object>> items = new ArrayList<>();
        org.bukkit.inventory.ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            org.bukkit.inventory.ItemStack it = contents[i];
            if (it == null || it.getType() == org.bukkit.Material.AIR) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("slot", i);
            m.put("type", it.getType().name());
            m.put("amount", it.getAmount());
            if (it.hasItemMeta()) {
                org.bukkit.inventory.meta.ItemMeta meta = it.getItemMeta();
                if (meta.hasDisplayName()) m.put("name", meta.getDisplayName());
                if (meta.hasLore()) m.put("lore", meta.getLore());
            }
            int damage = 0;
            if (it.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable dmg) {
                if (it.getType().getMaxDurability() > 0) damage = dmg.getDamage();
            }
            m.put("durability", damage);
            items.add(m);
        }
        return items;
    }

    private GameMode parseGm(String s) {
        return switch (s.toLowerCase()) {
            case "survival", "s", "0" -> GameMode.SURVIVAL;
            case "creative", "c", "1" -> GameMode.CREATIVE;
            case "adventure", "a", "2" -> GameMode.ADVENTURE;
            case "spectator", "sp", "3" -> GameMode.SPECTATOR;
            default -> null;
        };
    }

    // ---------- Console ----------
    private void handleConsoleGet(HttpExchange exchange) throws IOException {
        int lines = 200;
        String q = exchange.getRequestURI().getQuery();
        if (q != null) {
            for (String p : q.split("&")) {
                if (p.startsWith("lines=")) {
                    try { lines = Integer.parseInt(p.substring(6)); } catch (Exception ignored) {}
                }
            }
        }
        WebServer.sendJson(exchange, 200, Json.stringify(Map.of("lines", consoleBuffer.getLines(lines))));
    }

    private void handleConsolePost(HttpExchange exchange) throws IOException {
        String body = WebServer.readBody(exchange);
        String cmd = extractJsonField(body, "command");
        if (cmd == null || cmd.isEmpty()) {
            WebServer.sendJson(exchange, 400, "{\"error\":\"command required\"}");
            return;
        }
        ConsoleCommandSender console = Bukkit.getConsoleSender();
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(console, cmd));
        WebServer.sendJson(exchange, 200, "{\"success\":true}");
    }

    // ---------- Plugins ----------
    private void handlePlugins(HttpExchange exchange) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Plugin p : Bukkit.getPluginManager().getPlugins()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", p.getName());
            m.put("version", p.getDescription().getVersion());
            m.put("enabled", p.isEnabled());
            m.put("authors", p.getDescription().getAuthors());
            m.put("description", p.getDescription().getDescription());
            list.add(m);
        }
        WebServer.sendJson(exchange, 200, Json.stringify(Map.of("plugins", list)));
    }

    private void handlePluginAction(HttpExchange exchange, String[] parts, String method) throws IOException {
        if (parts.length < 5) {
            WebServer.sendJson(exchange, 400, "{\"error\":\"Bad request\"}");
            return;
        }
        String name = parts[3];
        String action = parts[4];
        Plugin p = Bukkit.getPluginManager().getPlugin(name);
        if (p == null) {
            WebServer.sendJson(exchange, 404, "{\"error\":\"Plugin not found\"}");
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            switch (action) {
                case "enable" -> Bukkit.getPluginManager().enablePlugin(p);
                case "disable" -> Bukkit.getPluginManager().disablePlugin(p);
                case "reload" -> {
                    Bukkit.getPluginManager().disablePlugin(p);
                    Bukkit.getPluginManager().enablePlugin(p);
                }
            }
        });
        try { Thread.sleep(150); } catch (InterruptedException ignored) {}
        WebServer.sendJson(exchange, 200, "{\"success\":true}");
    }

    // ---------- Worlds ----------
    private void handleWorlds(HttpExchange exchange) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (World w : Bukkit.getWorlds()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", w.getName());
            m.put("environment", w.getEnvironment().name());
            m.put("players", w.getPlayers().size());
            m.put("time", w.getTime());
            m.put("weather", w.hasStorm() ? (w.isThundering() ? "thunder" : "rain") : "clear");
            m.put("difficulty", w.getDifficulty().name());
            m.put("seed", w.getSeed());
            list.add(m);
        }
        WebServer.sendJson(exchange, 200, Json.stringify(Map.of("worlds", list)));
    }

    private void handleWorldAction(HttpExchange exchange, String[] parts, String method) throws IOException {
        if (parts.length < 5) {
            WebServer.sendJson(exchange, 400, "{\"error\":\"Bad request\"}");
            return;
        }
        String name = parts[3];
        String action = parts[4];
        World w = Bukkit.getWorld(name);
        if (w == null) {
            WebServer.sendJson(exchange, 404, "{\"error\":\"World not found\"}");
            return;
        }
        String body = "POST".equals(method) ? WebServer.readBody(exchange) : "";
        String value = extractJsonField(body, "value");

        Bukkit.getScheduler().runTask(plugin, () -> {
            switch (action) {
                case "time" -> { if (value != null) w.setTime(Long.parseLong(value)); }
                case "day" -> w.setTime(1000);
                case "night" -> w.setTime(13000);
                case "weather" -> {
                    if ("clear".equals(value)) { w.setStorm(false); w.setThundering(false); }
                    else if ("rain".equals(value)) { w.setStorm(true); w.setThundering(false); }
                    else if ("thunder".equals(value)) { w.setStorm(true); w.setThundering(true); }
                }
                case "difficulty" -> { if (value != null) w.setDifficulty(Difficulty.valueOf(value.toUpperCase())); }
            }
        });
        WebServer.sendJson(exchange, 200, "{\"success\":true}");
    }

    // ---------- Economy (Vault) ----------
    private void handleEconomy(HttpExchange exchange, String[] parts, String method) throws IOException {
        if (!VaultHook.isAvailable()) {
            WebServer.sendJson(exchange, 503, "{\"error\":\"Vault not available\"}");
            return;
        }
        if (parts.length < 4) {
            WebServer.sendJson(exchange, 400, "{\"error\":\"Bad request\"}");
            return;
        }
        String name = parts[3];
        OfflinePlayer op = PlayerResolver.byName(name);
        if (method.equals("GET") || parts.length == 4) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("player", name);
            data.put("balance", VaultHook.getBalance(op));
            data.put("currency", VaultHook.getCurrencyName());
            WebServer.sendJson(exchange, 200, Json.stringify(data));
            return;
        }
        // POST /api/economy/{player}/set|add|remove
        String action = parts[4];
        String body = WebServer.readBody(exchange);
        String amountStr = extractJsonField(body, "amount");
        if (amountStr == null) {
            WebServer.sendJson(exchange, 400, "{\"error\":\"amount required\"}");
            return;
        }
        double amount = Double.parseDouble(amountStr);
        boolean ok = switch (action) {
            case "set" -> VaultHook.setBalance(op, amount);
            case "add" -> VaultHook.add(op, amount);
            case "remove" -> VaultHook.remove(op, amount);
            default -> false;
        };
        WebServer.sendJson(exchange, ok ? 200 : 400, Json.stringify(Map.of(
                "success", ok, "balance", VaultHook.getBalance(op))));
    }

    // ---------- Permissions (LuckPerms) ----------
    private void handlePermissions(HttpExchange exchange, String[] parts) throws IOException {
        if (parts.length < 4) {
            WebServer.sendJson(exchange, 400, "{\"error\":\"Bad request\"}");
            return;
        }
        String name = parts[3];
        Player p = Bukkit.getPlayer(name);
        if (p == null) {
            WebServer.sendJson(exchange, 404, "{\"error\":\"Player not online\"}");
            return;
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("player", name);
        data.put("primaryGroup", LuckPermsHook.getPrimaryGroup(p));
        data.put("groups", LuckPermsHook.getGroups(p));
        data.put("available", LuckPermsHook.isAvailable());
        WebServer.sendJson(exchange, 200, Json.stringify(data));
    }

    // ---------- Homes ----------
    private void handleHomes(HttpExchange exchange, String[] parts) throws IOException {
        if (parts.length < 4) {
            WebServer.sendJson(exchange, 400, "{\"error\":\"Bad request\"}");
            return;
        }
        String name = parts[3];
        Player p = Bukkit.getPlayer(name);
        if (p == null) {
            WebServer.sendJson(exchange, 404, "{\"error\":\"Player not online\"}");
            return;
        }
        List<String> homes = plugin.getHomeManager().getHomes(p.getUniqueId());
        WebServer.sendJson(exchange, 200, Json.stringify(Map.of("homes", homes, "limit", plugin.getHomeManager().getHomeLimit(p.getUniqueId()))));
    }

    // ---------- Broadcast / Server ----------
    private void handleBroadcast(HttpExchange exchange) throws IOException {
        String body = WebServer.readBody(exchange);
        String msg = extractJsonField(body, "message");
        if (msg == null || msg.isEmpty()) {
            WebServer.sendJson(exchange, 400, "{\"error\":\"message required\"}");
            return;
        }
        String finalMsg = ChatColor.translateAlternateColorCodes('&', "&6[&cBroadcast&6] &r" + msg);
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.broadcastMessage(finalMsg));
        WebServer.sendJson(exchange, 200, "{\"success\":true}");
    }

    private void handleServerStop(HttpExchange exchange) throws IOException {
        String body = WebServer.readBody(exchange);
        String delay = extractJsonField(body, "delay");
        int secs = 0;
        if (delay != null) { try { secs = Integer.parseInt(delay); } catch (Exception ignored) {} }
        Bukkit.getScheduler().runTaskLater(plugin, () -> Bukkit.shutdown(), secs * 20L);
        WebServer.sendJson(exchange, 200, "{\"success\":true}");
    }

    private void handleServerReload(HttpExchange exchange) throws IOException {
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.reload());
        WebServer.sendJson(exchange, 200, "{\"success\":true}");
    }

    // ---------- Bans ----------
    private void handleBans(HttpExchange exchange, String[] parts, String method) throws IOException {
        // GET /api/bans - list
        if (method.equals("GET")) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (Map.Entry<UUID, Object[]> e : plugin.getBanManager().getAllBans().entrySet()) {
                Object[] v = e.getValue();
                long expires = (Long) v[4];
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("uuid", e.getKey().toString());
                m.put("name", v[0]);
                m.put("reason", v[1]);
                m.put("banner", v[2]);
                m.put("created", v[3]);
                m.put("expires", expires);
                m.put("remaining", expires == 0 ? -1 : Math.max(0, expires - System.currentTimeMillis()));
                m.put("permanent", expires == 0);
                list.add(m);
            }
            WebServer.sendJson(exchange, 200, Json.stringify(Map.of("bans", list)));
            return;
        }
        // POST /api/bans - create
        if (method.equals("POST")) {
            String body = WebServer.readBody(exchange);
            String name = extractJsonField(body, "name");
            String reason = extractJsonField(body, "reason");
            String duration = extractJsonField(body, "duration");
            if (name == null) {
                WebServer.sendJson(exchange, 400, "{\"error\":\"name required\"}");
                return;
            }
            org.bukkit.OfflinePlayer op = PlayerResolver.byName(name);
            if (op == null) {
                WebServer.sendJson(exchange, 400, "{\"error\":\"unknown player\"}");
                return;
            }
            long expires = 0;
            if (duration != null && !duration.isEmpty()) {
                long ms = parseDurationMs(duration);
                if (ms > 0) expires = System.currentTimeMillis() + ms;
            }
            plugin.getBanManager().ban(op.getUniqueId(), name,
                    reason == null ? "违反服务器规则" : reason, "WebAdmin", expires);
            WebServer.sendJson(exchange, 200, "{\"success\":true}");
            return;
        }
        // DELETE /api/bans/{uuid}
        if (method.equals("DELETE") && parts.length >= 4) {
            UUID uuid;
            try { uuid = UUID.fromString(parts[3]); }
            catch (Exception e) { WebServer.sendJson(exchange, 400, "{\"error\":\"invalid uuid\"}"); return; }
            boolean ok = plugin.getBanManager().unban(uuid);
            WebServer.sendJson(exchange, ok ? 200 : 404, Json.stringify(Map.of("success", ok)));
            return;
        }
        WebServer.sendJson(exchange, 400, "{\"error\":\"Bad request\"}");
    }

    private long parseDurationMs(String s) {
        try {
            int len = s.length();
            char unit = s.charAt(len - 1);
            long num = Long.parseLong(s.substring(0, len - 1));
            return switch (unit) {
                case 's' -> num * 1000L;
                case 'm' -> num * 60_000L;
                case 'h' -> num * 3_600_000L;
                case 'd' -> num * 86_400_000L;
                case 'w' -> num * 604_800_000L;
                default -> -1;
            };
        } catch (Exception e) { return -1; }
    }

    // ---------- Warps ----------
    private void handleWarps(HttpExchange exchange, String[] parts, String method) throws IOException {
        if (method.equals("GET")) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (String name : plugin.getWarpManager().getWarpNames()) {
                Location loc = plugin.getWarpManager().getWarp(name);
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", name);
                m.put("world", loc.getWorld().getName());
                m.put("x", Math.round(loc.getX() * 100.0) / 100.0);
                m.put("y", Math.round(loc.getY() * 100.0) / 100.0);
                m.put("z", Math.round(loc.getZ() * 100.0) / 100.0);
                list.add(m);
            }
            WebServer.sendJson(exchange, 200, Json.stringify(Map.of("warps", list, "count", list.size())));
            return;
        }
        if (method.equals("POST")) {
            String body = WebServer.readBody(exchange);
            String name = extractJsonField(body, "name");
            if (name == null) {
                WebServer.sendJson(exchange, 400, "{\"error\":\"name required\"}");
                return;
            }
            // Set warp at a player's location (must be online) or specific coords
            String playerName = extractJsonField(body, "player");
            if (playerName != null) {
                Player p = Bukkit.getPlayer(playerName);
                if (p == null) { WebServer.sendJson(exchange, 404, "{\"error\":\"player not online\"}"); return; }
                plugin.getWarpManager().setWarp(name, p.getLocation());
            } else {
                // need coords + world
                String world = extractJsonField(body, "world");
                double x = Double.parseDouble(extractJsonField(body, "x"));
                double y = Double.parseDouble(extractJsonField(body, "y"));
                double z = Double.parseDouble(extractJsonField(body, "z"));
                plugin.getWarpManager().setWarp(name, new Location(Bukkit.getWorld(world), x, y, z));
            }
            WebServer.sendJson(exchange, 200, "{\"success\":true}");
            return;
        }
        if (method.equals("DELETE") && parts.length >= 4) {
            String name = parts[3];
            boolean ok = plugin.getWarpManager().deleteWarp(name);
            WebServer.sendJson(exchange, ok ? 200 : 404, Json.stringify(Map.of("success", ok)));
            return;
        }
        WebServer.sendJson(exchange, 400, "{\"error\":\"Bad request\"}");
    }

    // ---------- Scheduled Tasks ----------
    private void handleTasks(HttpExchange exchange, String[] parts, String method) throws IOException {
        if (method.equals("GET") && parts.length == 3) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (TaskManager.ScheduledTask t : plugin.getTaskManager().getAll()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", t.id);
                m.put("name", t.name);
                m.put("command", t.command);
                m.put("cron", t.cron);
                m.put("enabled", t.enabled);
                m.put("lastRun", t.lastRun);
                list.add(m);
            }
            WebServer.sendJson(exchange, 200, Json.stringify(Map.of("tasks", list)));
            return;
        }
        if (method.equals("POST") && parts.length == 3) {
            String body = WebServer.readBody(exchange);
            String name = extractJsonField(body, "name");
            String command = extractJsonField(body, "command");
            String cron = extractJsonField(body, "cron");
            if (name == null || command == null || cron == null) {
                WebServer.sendJson(exchange, 400, "{\"error\":\"name, command, cron required\"}");
                return;
            }
            TaskManager.ScheduledTask t = plugin.getTaskManager().create(name, command, cron);
            WebServer.sendJson(exchange, 200, Json.stringify(Map.of("id", t.id)));
            return;
        }
        if (parts.length >= 4) {
            int id;
            try { id = Integer.parseInt(parts[3]); }
            catch (Exception e) { WebServer.sendJson(exchange, 400, "{\"error\":\"invalid id\"}"); return; }

            if (method.equals("PUT")) {
                String body = WebServer.readBody(exchange);
                String name = extractJsonField(body, "name");
                String command = extractJsonField(body, "command");
                String cron = extractJsonField(body, "cron");
                String enabledStr = extractJsonField(body, "enabled");
                boolean enabled = enabledStr == null || enabledStr.equals("true") || enabledStr.equals("1");
                boolean ok = plugin.getTaskManager().update(id, name, command, cron, enabled);
                WebServer.sendJson(exchange, ok ? 200 : 404, Json.stringify(Map.of("success", ok)));
                return;
            }
            if (method.equals("DELETE")) {
                boolean ok = plugin.getTaskManager().delete(id);
                WebServer.sendJson(exchange, ok ? 200 : 404, Json.stringify(Map.of("success", ok)));
                return;
            }
            if (method.equals("POST") && parts.length == 5 && parts[4].equals("run")) {
                boolean ok = plugin.getTaskManager().runNow(id);
                WebServer.sendJson(exchange, ok ? 200 : 404, Json.stringify(Map.of("success", ok)));
                return;
            }
        }
        WebServer.sendJson(exchange, 400, "{\"error\":\"Bad request\"}");
    }

    // ---------- IP Bans ----------
    private void handleIpBans(HttpExchange exchange, String[] parts, String method) throws IOException {
        if (method.equals("GET")) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (var e : plugin.getIpBanManager().getAll().entrySet()) {
                Object[] v = e.getValue();
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("ip", e.getKey());
                m.put("reason", v[0]);
                m.put("banner", v[1]);
                m.put("created", v[2]);
                m.put("expires", v[3]);
                list.add(m);
            }
            WebServer.sendJson(exchange, 200, Json.stringify(Map.of("bans", list)));
            return;
        }
        if (method.equals("POST")) {
            String body = WebServer.readBody(exchange);
            String ip = extractJsonField(body, "ip");
            String reason = extractJsonField(body, "reason");
            if (ip == null) { WebServer.sendJson(exchange, 400, "{\"error\":\"ip required\"}"); return; }
            if (reason == null) reason = "违反服务器规则";
            plugin.getIpBanManager().ban(ip, reason, "Web", 0);
            WebServer.sendJson(exchange, 200, Json.stringify(Map.of("success", true)));
            return;
        }
        if (method.equals("DELETE") && parts.length >= 4) {
            String ip = parts[3];
            boolean ok = plugin.getIpBanManager().unban(ip);
            WebServer.sendJson(exchange, ok ? 200 : 404, Json.stringify(Map.of("success", ok)));
            return;
        }
        WebServer.sendJson(exchange, 400, "{\"error\":\"Bad request\"}");
    }

    // ---------- Whitelist ----------
    private void handleWhitelist(HttpExchange exchange, String[] parts, String method) throws IOException {
        if (method.equals("GET") && parts.length == 3) {
            // GET /api/whitelist
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("enabled", plugin.getWhitelistManager().isEnabled());
            data.put("list", plugin.getWhitelistManager().getAll());
            WebServer.sendJson(exchange, 200, Json.stringify(data));
            return;
        }
        if (method.equals("POST") && parts.length == 3) {
            // toggle on/off
            String body = WebServer.readBody(exchange);
            String en = extractJsonField(body, "enabled");
            boolean enabled = "true".equals(en) || "1".equals(en);
            plugin.getWhitelistManager().setEnabled(enabled);
            WebServer.sendJson(exchange, 200, Json.stringify(Map.of("success", true, "enabled", enabled)));
            return;
        }
        if (method.equals("POST") && parts.length >= 4) {
            // add player: /api/whitelist/{uuidOrName} with name in body
            String name = extractJsonField(WebServer.readBody(exchange), "name");
            UUID uuid = resolvePlayerUuid(parts[3], name);
            if (uuid == null) { WebServer.sendJson(exchange, 400, "{\"error\":\"invalid uuid or unknown player\"}"); return; }
            String resolvedName = name != null ? name : Bukkit.getOfflinePlayer(uuid).getName();
            if (resolvedName == null) resolvedName = uuid.toString().substring(0, 8);
            boolean ok = plugin.getWhitelistManager().add(uuid, resolvedName);
            WebServer.sendJson(exchange, ok ? 200 : 400, Json.stringify(Map.of("success", ok)));
            return;
        }
        if (method.equals("DELETE") && parts.length >= 4) {
            UUID uuid = resolvePlayerUuid(parts[3], null);
            if (uuid == null) { WebServer.sendJson(exchange, 400, "{\"error\":\"invalid uuid or unknown player\"}"); return; }
            boolean ok = plugin.getWhitelistManager().remove(uuid);
            WebServer.sendJson(exchange, ok ? 200 : 404, Json.stringify(Map.of("success", ok)));
            return;
        }
        WebServer.sendJson(exchange, 400, "{\"error\":\"Bad request\"}");
    }

    /** Accepts a UUID or a player name in the path; resolves names via OfflinePlayer. */
    private UUID resolvePlayerUuid(String pathSegment, String bodyName) {
        try { return UUID.fromString(pathSegment); }
        catch (IllegalArgumentException ignored) {}
        String name = bodyName != null && !bodyName.isEmpty() ? bodyName : pathSegment;
        OfflinePlayer op = PlayerResolver.byName(name);
        // 名字查询未命中缓存时会得到离线模式 UUID，仍可用于白名单
        return op != null ? op.getUniqueId() : null;
    }

    // ---------- Chat Log ----------
    private void handleChat(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        int limit = 50;
        String keyword = null;
        if (query != null) {
            for (String p : query.split("&")) {
                if (p.startsWith("limit=")) { try { limit = Integer.parseInt(p.substring(6)); } catch (Exception ignored) {} }
                if (p.startsWith("player=")) keyword = p.substring(7);
            }
        }
        List<Object[]> logs = keyword != null
                ? plugin.getChatLogManager().search(keyword, limit)
                : plugin.getChatLogManager().getRecent(limit);
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object[] e : logs) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("time", e[0]);
            m.put("name", e[1]);
            m.put("uuid", e[2]);
            m.put("world", e[3]);
            m.put("message", e[4]);
            list.add(m);
        }
        WebServer.sendJson(exchange, 200, Json.stringify(Map.of("logs", list)));
    }

    // ---------- Player Stats ----------
    private void handleStats(HttpExchange exchange, String[] parts) throws IOException {
        if (parts.length < 4) { WebServer.sendJson(exchange, 400, "{\"error\":\"player name required\"}"); return; }
        String name = parts[3];
        var op = PlayerResolver.byName(name);
        Object[] stats = plugin.getPlayerStatsManager().get(op.getUniqueId());
        if (stats == null) { WebServer.sendJson(exchange, 404, "{\"error\":\"no stats\"}"); return; }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", stats[3]);
        data.put("firstJoin", stats[0]);
        data.put("lastSeen", stats[1]);
        data.put("playtimeMs", stats[2]);
        data.put("online", op.isOnline());
        WebServer.sendJson(exchange, 200, Json.stringify(data));
    }

    // ---------- Player market ----------
    private void handleMarket(HttpExchange exchange, String[] parts, String method) throws IOException {
        if (method.equals("GET")) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (var e : plugin.getMarketManager().getAll()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", e.id);
                m.put("seller", e.seller.toString());
                m.put("sellerName", e.sellerName);
                m.put("type", e.item != null ? e.item.getType().name() : "AIR");
                m.put("amount", e.item != null ? e.item.getAmount() : 0);
                m.put("displayName", (e.item != null && e.item.hasItemMeta()
                        && e.item.getItemMeta().hasDisplayName())
                        ? e.item.getItemMeta().getDisplayName() : null);
                m.put("price", e.price);
                m.put("created", e.created);
                list.add(m);
            }
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("listings", list);
            data.put("count", list.size());
            data.put("available", plugin.getEconomyManager().available());
            WebServer.sendJson(exchange, 200, Json.stringify(data));
            return;
        }
        if (method.equals("DELETE") && parts.length >= 4) {
            int id;
            try { id = Integer.parseInt(parts[3]); }
            catch (Exception e) { WebServer.sendJson(exchange, 400, "{\"error\":\"invalid id\"}"); return; }
            boolean ok = plugin.getMarketManager().remove(id);
            WebServer.sendJson(exchange, ok ? 200 : 404, Json.stringify(Map.of("success", ok)));
            return;
        }
        WebServer.sendJson(exchange, 400, "{\"error\":\"Bad request\"}");
    }

    // ---------- Claims ----------
    private void handleClaims(HttpExchange exchange, String[] parts, String method) throws IOException {
        if (method.equals("GET")) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (var c : plugin.getClaimManager().getAll()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", c.id);
                m.put("owner", c.owner.toString());
                m.put("ownerName", c.ownerName);
                m.put("world", c.world);
                m.put("minX", c.minX);
                m.put("minZ", c.minZ);
                m.put("maxX", c.maxX);
                m.put("maxZ", c.maxZ);
                m.put("area", c.area());
                m.put("created", c.created);
                list.add(m);
            }
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("claims", list);
            data.put("count", list.size());
            WebServer.sendJson(exchange, 200, Json.stringify(data));
            return;
        }
        if (method.equals("DELETE") && parts.length >= 4) {
            int id;
            try { id = Integer.parseInt(parts[3]); }
            catch (Exception e) { WebServer.sendJson(exchange, 400, "{\"error\":\"invalid id\"}"); return; }
            boolean ok = plugin.getClaimManager().remove(id);
            WebServer.sendJson(exchange, ok ? 200 : 404, Json.stringify(Map.of("success", ok)));
            return;
        }
        WebServer.sendJson(exchange, 400, "{\"error\":\"Bad request\"}");
    }

    // ---------- Announcements / rules ----------
    private void handleAnnouncements(HttpExchange exchange, String[] parts, String method) throws IOException {
        if (method.equals("GET")) {
            Map<String, Object> data = new LinkedHashMap<>();
            Map<String, Object> news = new LinkedHashMap<>();
            news.put("title", plugin.getAnnouncementManager().getTitle("news", "服务器公告"));
            news.put("author", plugin.getAnnouncementManager().getAuthor("news", "服务器管理组"));
            news.put("pages", plugin.getAnnouncementManager().getNewsPages());
            Map<String, Object> rules = new LinkedHashMap<>();
            rules.put("title", plugin.getAnnouncementManager().getTitle("rules", "服务器规则"));
            rules.put("author", plugin.getAnnouncementManager().getAuthor("rules", "服务器管理组"));
            rules.put("pages", plugin.getAnnouncementManager().getRulesPages());
            data.put("news", news);
            data.put("rules", rules);
            WebServer.sendJson(exchange, 200, Json.stringify(data));
            return;
        }
        if (method.equals("POST") && parts.length >= 4) {
            String section = parts[3];
            if (!section.equals("news") && !section.equals("rules")) {
                WebServer.sendJson(exchange, 400, "{\"error\":\"section must be news or rules\"}");
                return;
            }
            String body = WebServer.readBody(exchange);
            List<String> pages = extractJsonStringArray(body, "pages");
            if (pages == null || pages.isEmpty()) {
                WebServer.sendJson(exchange, 400, "{\"error\":\"pages required\"}");
                return;
            }
            if (section.equals("news")) plugin.getAnnouncementManager().setNews(pages);
            else plugin.getAnnouncementManager().setRules(pages);
            WebServer.sendJson(exchange, 200, "{\"success\":true}");
            return;
        }
        WebServer.sendJson(exchange, 400, "{\"error\":\"Bad request\"}");
    }

    // ---------- Accounts ----------
    private void handleAccounts(HttpExchange exchange) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (var op : Bukkit.getOfflinePlayers()) {
            if (op == null || op.getUniqueId() == null) continue;
            String name = op.getName();
            if (name == null) continue;
            if (!plugin.getLoginManager().isRegistered(op.getUniqueId())) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("uuid", op.getUniqueId().toString());
            m.put("name", name);
            m.put("online", op.isOnline());
            m.put("authed", plugin.getLoginManager().isAuthed(op.getUniqueId()));
            list.add(m);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("enabled", plugin.getLoginManager().isEnabled());
        data.put("accounts", list);
        data.put("count", list.size());
        WebServer.sendJson(exchange, 200, Json.stringify(data));
    }

    // ---------- Redeem codes ----------
    private void handleRedeem(HttpExchange exchange, String[] parts, String method) throws IOException {
        if (method.equals("GET")) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (var c : plugin.getRedeemManager().getAll()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("code", c.code);
                m.put("amount", c.amount);
                m.put("maxUses", c.maxUses);
                m.put("usedCount", c.usedCount);
                m.put("expires", c.expires);
                m.put("created", c.created);
                m.put("creator", c.creator);
                m.put("enabled", c.enabled);
                m.put("expired", c.expires > 0 && System.currentTimeMillis() > c.expires);
                list.add(m);
            }
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("codes", list);
            data.put("count", list.size());
            data.put("economyAvailable", plugin.getEconomyManager().available());
            WebServer.sendJson(exchange, 200, Json.stringify(data));
            return;
        }

        if (method.equals("POST") && parts.length == 3) {
            String body = WebServer.readBody(exchange);
            String code = extractJsonField(body, "code");
            String amountStr = extractJsonField(body, "amount");
            String usesStr = extractJsonField(body, "maxUses");
            String duration = extractJsonField(body, "duration");
            if (code == null || code.isEmpty() || amountStr == null) {
                WebServer.sendJson(exchange, 400, "{\"error\":\"code and amount required\"}");
                return;
            }
            double amount;
            try { amount = Double.parseDouble(amountStr); }
            catch (Exception e) { WebServer.sendJson(exchange, 400, "{\"error\":\"invalid amount\"}"); return; }

            int maxUses = 1;
            if (usesStr != null && !usesStr.isEmpty()) {
                try { maxUses = Math.max(0, Integer.parseInt(usesStr)); } catch (Exception ignored) {}
            }
            long expires = 0;
            if (duration != null && !duration.isEmpty()) {
                long ms = parseDurationMs(duration);
                if (ms > 0) expires = System.currentTimeMillis() + ms;
            }
            var created = plugin.getRedeemManager().create(code, amount, maxUses, expires, "WebAdmin");
            if (created == null) {
                WebServer.sendJson(exchange, 400, "{\"error\":\"code already exists or invalid amount\"}");
                return;
            }
            WebServer.sendJson(exchange, 200, Json.stringify(Map.of("success", true, "code", created.code)));
            return;
        }

        if (method.equals("POST") && parts.length == 5 && parts[4].equals("toggle")) {
            Boolean state = plugin.getRedeemManager().toggle(parts[3]);
            if (state == null) { WebServer.sendJson(exchange, 404, "{\"error\":\"code not found\"}"); return; }
            WebServer.sendJson(exchange, 200, Json.stringify(Map.of("success", true, "enabled", state)));
            return;
        }

        if (method.equals("DELETE") && parts.length >= 4) {
            boolean ok = plugin.getRedeemManager().delete(parts[3]);
            WebServer.sendJson(exchange, ok ? 200 : 404, Json.stringify(Map.of("success", ok)));
            return;
        }

        WebServer.sendJson(exchange, 400, "{\"error\":\"Bad request\"}");
    }

    /** 朴素解析 JSON 字符串数组：["a","b","c"]。 */
    private List<String> extractJsonStringArray(String body, String field) {
        List<String> out = new ArrayList<>();
        if (body == null) return out;
        String key = "\"" + field + "\"";
        int i = body.indexOf(key);
        if (i < 0) return out;
        int start = body.indexOf('[', i);
        if (start < 0) return out;
        int end = body.indexOf(']', start);
        if (end < 0) return out;
        String inner = body.substring(start + 1, end).trim();
        if (inner.isEmpty()) return out;

        String[] parts = inner.split("\",\"");
        for (String raw : parts) {
            String s = raw.trim();
            if (s.startsWith("\"")) s = s.substring(1);
            if (s.endsWith("\"")) s = s.substring(0, s.length() - 1);
            s = s.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
            if (!s.isEmpty()) out.add(s);
        }
        return out;
    }
}
