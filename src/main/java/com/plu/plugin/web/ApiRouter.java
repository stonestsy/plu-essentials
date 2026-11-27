package com.plu.plugin.web;

import com.plu.plugin.Main;
import com.plu.plugin.managers.MenuManager;
import com.plu.plugin.managers.NpcManager;
import com.plu.plugin.managers.RechargeManager;
import com.plu.plugin.managers.SignManager;
import com.plu.plugin.managers.TaskManager;
import com.plu.plugin.util.PlayerResolver;
import com.plu.plugin.util.Values;
import com.sun.net.httpserver.HttpExchange;
import org.bukkit.*;
import org.bukkit.block.Sign;
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
                    else if (path.startsWith("/api/groups")) handleGroups(exchange, parts, method);
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
                    else if (path.startsWith("/api/pay")) handlePay(exchange, parts, method);
                    else if (path.startsWith("/api/nicknames")) handleNicknames(exchange, parts, method);
                    else if (path.startsWith("/api/menus")) handleMenus(exchange, parts, method);
                    else if (path.startsWith("/api/npcs")) handleNpcs(exchange, parts, method);
                    else if (path.startsWith("/api/signs")) handleSigns(exchange, parts, method);
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
        data.put("vaultEnabled", plugin.getEconomyManager().isEnabled());
        data.put("vaultInstalled", true);
        data.put("luckpermsEnabled", true);
        data.put("economyEnabled", plugin.getEconomyManager().isEnabled());
        data.put("groupsEnabled", true);
        data.put("totalMoney", plugin.getEconomyManager().totalMoney());
        data.put("accountCount", plugin.getEconomyManager().accountCount());
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
            m.put("primaryGroup", plugin.getGroupManager().getPrimaryGroup(p.getUniqueId()));
            m.put("balance", plugin.getEconomyManager().balance(p.getUniqueId()));
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

    // ---------- Economy (built-in) ----------
    private void handleEconomy(HttpExchange exchange, String[] parts, String method) throws IOException {
        if (!plugin.getEconomyManager().isEnabled()) {
            WebServer.sendJson(exchange, 503, "{\"error\":\"economy disabled\"}");
            return;
        }
        if (parts.length < 4) {
            WebServer.sendJson(exchange, 400, "{\"error\":\"Bad request\"}");
            return;
        }
        String name = parts[3];
        OfflinePlayer op = PlayerResolver.byName(name);
        if (op == null) {
            WebServer.sendJson(exchange, 404, "{\"error\":\"unknown player\"}");
            return;
        }
        var eco = plugin.getEconomyManager();
        if (method.equals("GET") || parts.length == 4) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("player", name);
            data.put("balance", eco.balance(op.getUniqueId()));
            data.put("currency", eco.currencyName());
            data.put("formatted", eco.format(eco.balance(op.getUniqueId())));
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
        double amount;
        try { amount = Double.parseDouble(amountStr); }
        catch (Exception e) {
            WebServer.sendJson(exchange, 400, "{\"error\":\"invalid amount\"}");
            return;
        }
        boolean ok = switch (action) {
            case "set" -> eco.set(op.getUniqueId(), amount);
            case "add" -> eco.deposit(op.getUniqueId(), amount);
            case "remove" -> eco.set(op.getUniqueId(), Math.max(0, eco.balance(op.getUniqueId()) - amount));
            default -> false;
        };
        WebServer.sendJson(exchange, ok ? 200 : 400, Json.stringify(Map.of(
                "success", ok, "balance", eco.balance(op.getUniqueId()))));
    }

    // ---------- Permissions / groups (built-in) ----------
    private void handlePermissions(HttpExchange exchange, String[] parts) throws IOException {
        if (parts.length < 4) {
            WebServer.sendJson(exchange, 400, "{\"error\":\"Bad request\"}");
            return;
        }
        String name = parts[3];
        OfflinePlayer op = PlayerResolver.byName(name);
        if (op == null) {
            WebServer.sendJson(exchange, 404, "{\"error\":\"unknown player\"}");
            return;
        }
        var gm = plugin.getGroupManager();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("player", name);
        data.put("primaryGroup", gm.getPrimaryGroup(op.getUniqueId()));
        data.put("groups", gm.getGroups(op.getUniqueId()));
        data.put("prefix", gm.getPrefix(op.getUniqueId()));
        data.put("permissionCount", gm.effectivePermissions(op.getUniqueId()).size());
        data.put("permissions", new ArrayList<>(gm.effectivePermissions(op.getUniqueId())));
        data.put("available", true);
        WebServer.sendJson(exchange, 200, Json.stringify(data));
    }

    // ---------- Group management ----------
    private void handleGroups(HttpExchange exchange, String[] parts, String method) throws IOException {
        var gm = plugin.getGroupManager();

        if (method.equals("GET")) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (var g : gm.getAll()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", g.name);
                m.put("prefix", g.prefix == null ? "" : g.prefix);
                m.put("weight", g.weight);
                m.put("permissions", new ArrayList<>(g.permissions));
                m.put("memberCount", gm.memberCount(g.name));
                list.add(m);
            }
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("groups", list);
            data.put("count", list.size());
            WebServer.sendJson(exchange, 200, Json.stringify(data));
            return;
        }

        if (method.equals("POST") && parts.length == 3) {
            String body = WebServer.readBody(exchange);
            String name = extractJsonField(body, "name");
            if (name == null) {
                WebServer.sendJson(exchange, 400, "{\"error\":\"name required\"}");
                return;
            }
            String prefix = extractJsonField(body, "prefix");
            int weight = 0;
            String weightStr = extractJsonField(body, "weight");
            if (weightStr != null) {
                try { weight = Integer.parseInt(weightStr); } catch (Exception ignored) {}
            }
            var g = gm.create(name, prefix == null ? "" : prefix, weight);
            if (g == null) {
                WebServer.sendJson(exchange, 400, "{\"error\":\"invalid or duplicate name\"}");
                return;
            }
            WebServer.sendJson(exchange, 200, Json.stringify(Map.of("success", true, "name", g.name)));
            return;
        }

        if (parts.length < 4) {
            WebServer.sendJson(exchange, 400, "{\"error\":\"Bad request\"}");
            return;
        }
        String group = parts[3];

        if (method.equals("DELETE") && parts.length == 4) {
            boolean ok = gm.delete(group);
            WebServer.sendJson(exchange, ok ? 200 : 400, Json.stringify(Map.of("success", ok)));
            return;
        }

        // POST /api/groups/{name}/permissions
        if (method.equals("POST") && parts.length == 5 && parts[4].equals("permissions")) {
            String body = WebServer.readBody(exchange);
            String permission = extractJsonField(body, "permission");
            String action = extractJsonField(body, "action");
            if (permission == null || permission.trim().isEmpty()) {
                WebServer.sendJson(exchange, 400, "{\"error\":\"permission required\"}");
                return;
            }
            boolean remove = "remove".equalsIgnoreCase(action) || "del".equalsIgnoreCase(action);
            boolean ok = remove ? gm.removePermission(group, permission) : gm.addPermission(group, permission);
            WebServer.sendJson(exchange, ok ? 200 : 400, Json.stringify(Map.of("success", ok)));
            return;
        }

        // POST|DELETE /api/groups/{name}/players/{player}
        if (parts.length >= 6 && parts[4].equals("players")) {
            String playerName = parts[5];
            OfflinePlayer op = PlayerResolver.byName(playerName);
            if (op == null) {
                WebServer.sendJson(exchange, 404, "{\"error\":\"unknown player\"}");
                return;
            }
            boolean ok;
            if (method.equals("POST")) ok = gm.addPlayer(op.getUniqueId(), group);
            else if (method.equals("DELETE")) ok = gm.removePlayer(op.getUniqueId(), group);
            else ok = false;
            WebServer.sendJson(exchange, ok ? 200 : 400, Json.stringify(Map.of("success", ok)));
            return;
        }

        // PUT /api/groups/{name} — 更新前缀/权重
        if (method.equals("PUT") && parts.length == 4) {
            if (gm.get(group) == null) {
                WebServer.sendJson(exchange, 404, "{\"error\":\"group not found\"}");
                return;
            }
            String body = WebServer.readBody(exchange);
            String prefix = extractJsonField(body, "prefix");
            String weightStr = extractJsonField(body, "weight");
            if (prefix != null) gm.setPrefix(group, prefix);
            if (weightStr != null) {
                try { gm.setWeight(group, Integer.parseInt(weightStr)); } catch (Exception ignored) {}
            }
            WebServer.sendJson(exchange, 200, "{\"success\":true}");
            return;
        }

        WebServer.sendJson(exchange, 400, "{\"error\":\"Bad request\"}");
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
    // ---------- Recharge (admin) ----------
    private void handlePay(HttpExchange exchange, String[] parts, String method) throws IOException {
        RechargeManager recharge = plugin.getRechargeManager();

        if (parts.length == 3 && method.equals("GET")) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("enabled", recharge.isEnabled());
            result.put("mock", recharge.isMock());
            List<Map<String, Object>> packages = new ArrayList<>();
            for (RechargeManager.Package p : recharge.packages()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", p.id);
                m.put("name", p.name);
                m.put("priceFen", p.priceFen);
                m.put("price", p.priceFen / 100.0);
                m.put("coins", p.coins);
                m.put("enabled", p.enabled);
                packages.add(m);
            }
            result.put("packages", packages);
            List<Map<String, Object>> orders = new ArrayList<>();
            for (RechargeManager.Order o : recharge.recentOrders(200)) orders.add(recharge.toWeb(o));
            result.put("orders", orders);
            WebServer.sendJson(exchange, 200, Json.stringify(result));
            return;
        }

        if (parts.length >= 4 && parts[3].equals("packages")) {
            if (method.equals("POST") || method.equals("PUT")) {
                Object parsed = Json.parse(WebServer.readBody(exchange));
                if (!(parsed instanceof Map<?, ?> body)) {
                    WebServer.sendJson(exchange, 400, "{\"error\":\"请求格式错误\"}");
                    return;
                }
                RechargeManager.Package p = new RechargeManager.Package();
                p.id = method.equals("PUT") && parts.length >= 5 ? parts[4] : strVal(body, "id");
                p.name = strVal(body, "name");
                p.priceFen = asLongVal(body.get("priceFen"), 0);
                p.coins = asDoubleVal(body.get("coins"), 0);
                p.enabled = body.get("enabled") == null || Boolean.parseBoolean(String.valueOf(body.get("enabled")));
                if (p.id == null || p.id.isBlank() || recharge.upsertPackage(p) == null) {
                    WebServer.sendJson(exchange, 400, "{\"error\":\"礼包参数无效\"}");
                    return;
                }
                WebServer.sendJson(exchange, 200, "{\"ok\":true}");
                return;
            }
            if (method.equals("DELETE") && parts.length >= 5) {
                boolean ok = recharge.deletePackage(parts[4]);
                WebServer.sendJson(exchange, ok ? 200 : 404, ok ? "{\"ok\":true}" : "{\"error\":\"礼包不存在\"}");
                return;
            }
        }

        if (parts.length >= 6 && parts[3].equals("orders") && parts[5].equals("confirm")
                && method.equals("POST")) {
            boolean ok = recharge.markPaid(parts[4], "MANUAL", 0, "admin");
            WebServer.sendJson(exchange, ok ? 200 : 400, ok ? "{\"ok\":true}" : "{\"error\":\"订单不存在或已处理\"}");
            return;
        }
        WebServer.sendJson(exchange, 404, "{\"error\":\"Not found\"}");
    }

    // ---------- Nicknames ----------
    private void handleNicknames(HttpExchange exchange, String[] parts, String method) throws IOException {
        if (method.equals("GET")) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (Map.Entry<UUID, String> e : plugin.getNicknameManager().all().entrySet()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("uuid", e.getKey().toString());
                m.put("name", plugin.getNicknameManager().knownName(e.getKey()));
                m.put("nick", e.getValue());
                list.add(m);
            }
            WebServer.sendJson(exchange, 200, Json.stringify(Map.of("nicknames", list)));
            return;
        }
        if (method.equals("POST")) {
            Object parsed = Json.parse(WebServer.readBody(exchange));
            if (!(parsed instanceof Map<?, ?> body)) {
                WebServer.sendJson(exchange, 400, "{\"error\":\"请求格式错误\"}");
                return;
            }
            String name = strVal(body, "name");
            String nick = strVal(body, "nick");
            UUID uuid = plugin.getNicknameManager().uuidByName(name);
            if (uuid == null) {
                WebServer.sendJson(exchange, 404, "{\"error\":\"找不到玩家\"}");
                return;
            }
            boolean ok = plugin.getNicknameManager().set(uuid, name, nick == null ? "" : nick);
            if (!ok) {
                WebServer.sendJson(exchange, 400, "{\"error\":\"昵称过长\"}");
                return;
            }
            applyNickname(uuid);
            WebServer.sendJson(exchange, 200, "{\"ok\":true}");
            return;
        }
        if (method.equals("DELETE") && parts.length >= 4) {
            try {
                UUID uuid = UUID.fromString(parts[3]);
                plugin.getNicknameManager().remove(uuid);
                applyNickname(uuid);
                WebServer.sendJson(exchange, 200, "{\"ok\":true}");
            } catch (IllegalArgumentException e) {
                WebServer.sendJson(exchange, 400, "{\"error\":\"UUID 无效\"}");
            }
            return;
        }
        WebServer.sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
    }

    private void applyNickname(UUID uuid) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                plugin.getNicknameManager().apply(player);
                plugin.getTabManager().update(player);
            }
        });
    }

    // ---------- Menus ----------
    private void handleMenus(HttpExchange exchange, String[] parts, String method) throws IOException {
        MenuManager manager = plugin.getMenuManager();

        if (method.equals("GET")) {
            if (parts.length >= 4) {
                MenuManager.Menu menu = manager.get(parts[3]);
                if (menu == null) {
                    WebServer.sendJson(exchange, 404, "{\"error\":\"菜单不存在\"}");
                    return;
                }
                WebServer.sendJson(exchange, 200, Json.stringify(menuToMap(menu)));
                return;
            }
            List<Map<String, Object>> list = new ArrayList<>();
            for (MenuManager.Menu menu : manager.all().values()) list.add(menuToMap(menu));
            WebServer.sendJson(exchange, 200, Json.stringify(Map.of("menus", list)));
            return;
        }

        if (method.equals("POST") && parts.length == 3) {
            Object parsed = Json.parse(WebServer.readBody(exchange));
            if (!(parsed instanceof Map<?, ?> body)) {
                WebServer.sendJson(exchange, 400, "{\"error\":\"请求格式错误\"}");
                return;
            }
            String id = strVal(body, "id");
            int size = MenuManager.normalizeSize(Values.asInt(body.get("size"), 27));
            MenuManager.Menu menu = manager.create(id, size);
            if (menu == null) {
                WebServer.sendJson(exchange, 400, "{\"error\":\"菜单已存在或 id 无效\"}");
                return;
            }
            WebServer.sendJson(exchange, 200, "{\"ok\":true}");
            return;
        }

        if (method.equals("PUT") && parts.length >= 4) {
            MenuManager.Menu menu = manager.get(parts[3]);
            if (menu == null) {
                WebServer.sendJson(exchange, 404, "{\"error\":\"菜单不存在\"}");
                return;
            }
            Object parsed = Json.parse(WebServer.readBody(exchange));
            if (!(parsed instanceof Map<?, ?> body)) {
                WebServer.sendJson(exchange, 400, "{\"error\":\"请求格式错误\"}");
                return;
            }
            String title = strVal(body, "title");
            if (title != null) menu.title = title;
            menu.size = MenuManager.normalizeSize(Values.asInt(body.get("size"), menu.size));
            if (body.get("items") instanceof List<?> items) {
                List<MenuManager.MenuItem> parsedItems = new ArrayList<>();
                for (Object raw : items) {
                    if (raw instanceof Map<?, ?> map) parsedItems.add(parseMenuItem(map));
                }
                menu.items = parsedItems;
            }
            manager.put(menu);
            WebServer.sendJson(exchange, 200, "{\"ok\":true}");
            return;
        }

        if (method.equals("DELETE") && parts.length >= 4) {
            boolean ok = manager.delete(parts[3]);
            WebServer.sendJson(exchange, ok ? 200 : 404, ok ? "{\"ok\":true}" : "{\"error\":\"菜单不存在\"}");
            return;
        }
        WebServer.sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
    }

    private Map<String, Object> menuToMap(MenuManager.Menu menu) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", menu.id);
        m.put("title", menu.title);
        m.put("size", menu.size);
        List<Map<String, Object>> items = new ArrayList<>();
        for (MenuManager.MenuItem item : menu.items) {
            Map<String, Object> raw = new LinkedHashMap<>();
            raw.put("slot", item.slot);
            raw.put("material", item.material);
            raw.put("name", item.name);
            raw.put("lore", item.lore);
            raw.put("amount", item.amount);
            raw.put("permission", item.permission);
            raw.put("commands", item.commands);
            raw.put("open", item.open);
            raw.put("close", item.close);
            items.add(raw);
        }
        m.put("items", items);
        return m;
    }

    private MenuManager.MenuItem parseMenuItem(Map<?, ?> raw) {
        MenuManager.MenuItem item = new MenuManager.MenuItem();
        item.slot = Values.asInt(raw.get("slot"), 0);
        item.material = raw.get("material") == null ? "STONE" : String.valueOf(raw.get("material"));
        item.name = raw.get("name") == null ? "" : String.valueOf(raw.get("name"));
        item.amount = Values.asInt(raw.get("amount"), 1);
        item.permission = raw.get("permission") == null ? "" : String.valueOf(raw.get("permission"));
        item.open = raw.get("open") == null ? "" : String.valueOf(raw.get("open"));
        item.close = raw.get("close") != null && Boolean.parseBoolean(String.valueOf(raw.get("close")));
        if (raw.get("lore") instanceof List<?> lore) {
            for (Object o : lore) item.lore.add(String.valueOf(o));
        }
        if (raw.get("commands") instanceof List<?> commands) {
            for (Object o : commands) item.commands.add(String.valueOf(o));
        }
        return item;
    }

    // ---------- NPCs ----------
    private void handleNpcs(HttpExchange exchange, String[] parts, String method) throws IOException {
        NpcManager manager = plugin.getNpcManager();

        if (method.equals("GET")) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("npcs", manager.listForWeb());
            result.put("citizens", manager.citizensInstalled());
            WebServer.sendJson(exchange, 200, Json.stringify(result));
            return;
        }

        try {
            if (method.equals("POST") && parts.length == 3) {
                Object parsed = Json.parse(WebServer.readBody(exchange));
                if (!(parsed instanceof Map<?, ?> body)) {
                    WebServer.sendJson(exchange, 400, "{\"error\":\"请求格式错误\"}");
                    return;
                }
                String name = strVal(body, "name");
                String type = strVal(body, "type");
                World world = Bukkit.getWorld(strVal(body, "world") == null ? "" : strVal(body, "world"));
                if (world == null) world = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
                if (world == null || name == null || name.isBlank()) {
                    WebServer.sendJson(exchange, 400, "{\"error\":\"参数无效\"}");
                    return;
                }
                Location location = new Location(world,
                        asDoubleVal(body.get("x"), world.getSpawnLocation().getX()),
                        asDoubleVal(body.get("y"), world.getSpawnLocation().getY()),
                        asDoubleVal(body.get("z"), world.getSpawnLocation().getZ()),
                        (float) asDoubleVal(body.get("yaw"), 0),
                        (float) asDoubleVal(body.get("pitch"), 0));
                NpcManager.Npc npc = callSync(() -> manager.create(name, type, location));
                if (npc == null) {
                    WebServer.sendJson(exchange, 500, "{\"error\":\"创建失败\"}");
                    return;
                }
                WebServer.sendJson(exchange, 200, Json.stringify(Map.of("ok", true, "id", npc.id)));
                return;
            }

            if (method.equals("POST") && parts.length >= 4) {
                NpcManager.Npc npc = manager.get(parts[3]);
                if (npc == null) {
                    WebServer.sendJson(exchange, 404, "{\"error\":\"NPC 不存在\"}");
                    return;
                }
                Object parsed = Json.parse(WebServer.readBody(exchange));
                if (!(parsed instanceof Map<?, ?> body)) {
                    WebServer.sendJson(exchange, 400, "{\"error\":\"请求格式错误\"}");
                    return;
                }
                String name = strVal(body, "name");
                String actionType = strVal(body, "actionType");
                String actionValue = strVal(body, "actionValue");
                String skin = strVal(body, "skin");
                if (name != null && !name.isBlank()) npc.name = name;
                if (actionType != null) npc.actionType = actionType;
                if (actionValue != null) npc.actionValue = actionValue;
                if (skin != null) npc.skin = skin;
                callSync(() -> {
                    manager.spawn(npc);
                    return true;
                });
                WebServer.sendJson(exchange, 200, "{\"ok\":true}");
                return;
            }

            if (method.equals("DELETE") && parts.length >= 4) {
                boolean ok = callSync(() -> manager.remove(parts[3]));
                WebServer.sendJson(exchange, ok ? 200 : 404, ok ? "{\"ok\":true}" : "{\"error\":\"NPC 不存在\"}");
                return;
            }
        } catch (Exception e) {
            WebServer.sendJson(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            return;
        }
        WebServer.sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
    }

    // ---------- Command signs ----------
    private void handleSigns(HttpExchange exchange, String[] parts, String method) throws IOException {
        SignManager manager = plugin.getSignManager();

        if (method.equals("GET")) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (SignManager.SignEntry e : manager.all()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("world", e.world);
                m.put("x", e.x);
                m.put("y", e.y);
                m.put("z", e.z);
                m.put("command", e.command);
                m.put("permission", e.permission);
                m.put("creator", e.creator);
                m.put("created", e.created);
                list.add(m);
            }
            WebServer.sendJson(exchange, 200, Json.stringify(Map.of("signs", list)));
            return;
        }

        if (method.equals("POST") && parts.length == 3) {
            Object parsed = Json.parse(WebServer.readBody(exchange));
            if (!(parsed instanceof Map<?, ?> body)) {
                WebServer.sendJson(exchange, 400, "{\"error\":\"请求格式错误\"}");
                return;
            }
            World world = Bukkit.getWorld(strVal(body, "world") == null ? "" : strVal(body, "world"));
            if (world == null) {
                WebServer.sendJson(exchange, 400, "{\"error\":\"世界不存在\"}");
                return;
            }
            int x = Values.asInt(body.get("x"), 0);
            int y = Values.asInt(body.get("y"), 0);
            int z = Values.asInt(body.get("z"), 0);
            String command = strVal(body, "command");
            if (command == null || command.isBlank()) {
                WebServer.sendJson(exchange, 400, "{\"error\":\"命令不能为空\"}");
                return;
            }
            try {
                boolean isSign = callSync(() -> world.getBlockAt(x, y, z).getState() instanceof Sign);
                if (!isSign) {
                    WebServer.sendJson(exchange, 400, "{\"error\":\"该坐标不是告示牌\"}");
                    return;
                }
            } catch (Exception e) {
                WebServer.sendJson(exchange, 500, "{\"error\":\"读取方块失败\"}");
                return;
            }
            manager.add(new Location(world, x, y, z), command, strVal(body, "permission"), "web");
            WebServer.sendJson(exchange, 200, "{\"ok\":true}");
            return;
        }

        if (method.equals("DELETE") && parts.length >= 7) {
            World world = Bukkit.getWorld(parts[3]);
            if (world == null) {
                WebServer.sendJson(exchange, 404, "{\"error\":\"世界不存在\"}");
                return;
            }
            Location location = new Location(world,
                    Values.asInt(parts[4], 0), Values.asInt(parts[5], 0), Values.asInt(parts[6], 0));
            boolean ok = manager.remove(location);
            WebServer.sendJson(exchange, ok ? 200 : 404, ok ? "{\"ok\":true}" : "{\"error\":\"不是命令告示牌\"}");
            return;
        }
        WebServer.sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
    }

    private String strVal(Map<?, ?> map, String key) {
        Object value = map.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private long asLongVal(Object value, long def) {
        if (value instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return def;
        }
    }

    private double asDoubleVal(Object value, double def) {
        if (value instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception e) {
            return def;
        }
    }

    private <T> T callSync(java.util.concurrent.Callable<T> task) throws Exception {
        return Bukkit.getScheduler().callSyncMethod(plugin, task).get();
    }

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
