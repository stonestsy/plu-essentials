package com.plu.plugin.web;

import com.plu.plugin.Main;
import com.plu.plugin.managers.RechargeManager;
import com.plu.plugin.pay.WeChatPayClient;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 玩家充值页与微信支付回调。这些路径不需要管理员 Token。
 * /pay 页面、/pay/oauth、/pay/callback、/api/pay/public/*、/api/pay/notify
 */
public class PayPublicHandler {

    private final Main plugin;

    public PayPublicHandler(Main plugin) { this.plugin = plugin; }

    public boolean isApiPath(String path) {
        return path.startsWith("/api/pay/public/") || path.equals("/api/pay/notify");
    }

    public boolean isPagePath(String path) {
        return path.equals("/pay") || path.equals("/pay/") || path.startsWith("/pay/");
    }

    // ---------------- 页面 ----------------

    public void handlePage(HttpExchange exchange, String path) throws IOException {
        String method = exchange.getRequestMethod();
        if (method.equals("OPTIONS")) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (path.startsWith("/pay/oauth")) {
            String bind = queryParam(exchange, "bind");
            RechargeManager recharge = plugin.getRechargeManager();
            if (bind == null || recharge.resolveBindCode(bind, false) == null) {
                redirect(exchange, "/pay?error=bind");
                return;
            }
            if (recharge.isMock()) {
                redirect(exchange, "/pay?bind=" + enc(bind));
                return;
            }
            redirect(exchange, recharge.client().buildOAuthUrl(bind));
            return;
        }

        if (path.startsWith("/pay/callback")) {
            String code = queryParam(exchange, "code");
            String bind = queryParam(exchange, "state");
            try {
                String openid = plugin.getRechargeManager().client().exchangeOpenId(code);
                String session = plugin.getRechargeManager().createSession(openid);
                redirect(exchange, "/pay?session=" + enc(session) + "&bind=" + enc(bind));
            } catch (Exception e) {
                plugin.getLogger().warning("WeChat OAuth callback failed: " + e.getMessage());
                redirect(exchange, "/pay?error=oauth");
            }
            return;
        }

        if (path.equals("/pay") || path.equals("/pay/")) {
            byte[] data;
            try (var in = getClass().getResourceAsStream("/pay/index.html")) {
                data = in == null ? null : in.readAllBytes();
            }
            if (data == null) {
                WebServer.sendJson(exchange, 404, "{\"error\":\"pay page missing\"}");
                return;
            }
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, data.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(data); }
            return;
        }

        WebServer.sendJson(exchange, 404, "{\"error\":\"Not found\"}");
    }

    // ---------------- API ----------------

    public void handleApi(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        if (method.equals("OPTIONS")) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        RechargeManager recharge = plugin.getRechargeManager();
        try {
            if (path.equals("/api/pay/public/packages") && method.equals("GET")) {
                sendPackages(exchange, recharge);
                return;
            }
            if (path.equals("/api/pay/public/create") && method.equals("POST")) {
                createOrder(exchange, recharge);
                return;
            }
            if (path.equals("/api/pay/public/status") && method.equals("GET")) {
                String no = queryParam(exchange, "order");
                RechargeManager.Order order = recharge.order(no);
                if (order == null) {
                    WebServer.sendJson(exchange, 404, "{\"error\":\"订单不存在\"}");
                    return;
                }
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("orderNo", order.no);
                result.put("status", order.status);
                result.put("coins", order.coins);
                result.put("paid", "PAID".equals(order.status));
                WebServer.sendJson(exchange, 200, Json.stringify(result));
                return;
            }
            if (path.equals("/api/pay/public/mock-confirm") && method.equals("POST")) {
                if (!recharge.isMock()) {
                    WebServer.sendJson(exchange, 403, "{\"error\":\"当前不是模拟支付模式\"}");
                    return;
                }
                Object body = Json.parse(WebServer.readBody(exchange));
                String no = body instanceof Map<?, ?> map ? str(map, "order") : null;
                RechargeManager.Order order = recharge.order(no);
                if (order == null) {
                    WebServer.sendJson(exchange, 404, "{\"error\":\"订单不存在\"}");
                    return;
                }
                boolean ok = recharge.markPaid(order.no, "MOCK", order.amountFen, "mock");
                WebServer.sendJson(exchange, ok ? 200 : 400, ok ? "{\"ok\":true}" : "{\"error\":\"发货失败\"}");
                return;
            }
            if (path.equals("/api/pay/notify") && method.equals("POST")) {
                handleNotify(exchange, recharge);
                return;
            }
            WebServer.sendJson(exchange, 404, "{\"error\":\"Not found\"}");
        } catch (IllegalArgumentException | IllegalStateException e) {
            WebServer.sendJson(exchange, 400, "{\"error\":\"" + safe(e.getMessage()) + "\"}");
        } catch (Exception e) {
            plugin.getLogger().warning("Pay API error: " + e);
            WebServer.sendJson(exchange, 500, "{\"error\":\"" + safe(e.getMessage()) + "\"}");
        }
    }

    private void sendPackages(HttpExchange exchange, RechargeManager recharge) throws IOException {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("enabled", recharge.isEnabled());
        result.put("mock", recharge.isMock());
        result.put("currency", plugin.getEconomyManager().currencyName());
        List<Map<String, Object>> list = new ArrayList<>();
        for (RechargeManager.Package p : recharge.packages()) {
            if (!p.enabled) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.id);
            m.put("name", p.name);
            m.put("priceFen", p.priceFen);
            m.put("price", p.priceFen / 100.0);
            m.put("coins", p.coins);
            list.add(m);
        }
        result.put("packages", list);
        WebServer.sendJson(exchange, 200, Json.stringify(result));
    }

    private void createOrder(HttpExchange exchange, RechargeManager recharge) throws Exception {
        if (!recharge.isEnabled()) {
            WebServer.sendJson(exchange, 503, "{\"error\":\"充值系统未开启\"}");
            return;
        }
        Object body = Json.parse(WebServer.readBody(exchange));
        if (!(body instanceof Map<?, ?> map)) {
            WebServer.sendJson(exchange, 400, "{\"error\":\"请求格式错误\"}");
            return;
        }
        String bind = str(map, "bind");
        String packageId = str(map, "packageId");
        String session = str(map, "session");

        UUID uuid = recharge.resolveBindCode(bind, false);
        if (uuid == null) {
            WebServer.sendJson(exchange, 400, "{\"error\":\"绑定码无效或已过期，请在游戏内重新 /charge\"}");
            return;
        }
        RechargeManager.Package pkg = recharge.packageById(packageId);
        if (pkg == null || !pkg.enabled) {
            WebServer.sendJson(exchange, 400, "{\"error\":\"礼包不存在\"}");
            return;
        }

        String openid = null;
        if (!recharge.isMock()) {
            openid = recharge.sessionOpenId(session);
            if (openid == null) {
                WebServer.sendJson(exchange, 401, "{\"error\":\"需要微信授权，请重新从充值页进入\"}");
                return;
            }
        }

        String playerName = recharge.bindCodeName(bind);
        RechargeManager.Order order = recharge.createOrder(uuid, playerName, pkg, openid);
        recharge.resolveBindCode(bind, true);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.put("mock", recharge.isMock());
        result.put("orderNo", order.no);
        result.put("coins", order.coins);
        result.put("amount", order.amountFen / 100.0);
        if (!recharge.isMock()) {
            result.put("payParams", recharge.payParams(order));
        }
        WebServer.sendJson(exchange, 200, Json.stringify(result));
    }

    private void handleNotify(HttpExchange exchange, RechargeManager recharge) throws IOException {
        String body = WebServer.readBody(exchange);
        Map<String, String> headers = new HashMap<>();
        for (String name : List.of("Wechatpay-Timestamp", "Wechatpay-Nonce",
                "Wechatpay-Signature", "Wechatpay-Serial")) {
            String value = exchange.getRequestHeaders().getFirst(name);
            if (value != null) headers.put(name, value);
        }
        try {
            WeChatPayClient.NotifyPayload payload = recharge.client().parseNotify(headers, body);
            boolean ok = false;
            if ("SUCCESS".equalsIgnoreCase(payload.tradeState)) {
                ok = recharge.markPaid(payload.outTradeNo, payload.transactionId, payload.totalFen, "notify");
            }
            if (ok) {
                WebServer.sendJson(exchange, 200, "{\"code\":\"SUCCESS\",\"message\":\"成功\"}");
            } else {
                WebServer.sendJson(exchange, 200, "{\"code\":\"FAIL\",\"message\":\"订单处理失败\"}");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("WeChat notify error: " + e.getMessage());
            WebServer.sendJson(exchange, 400, "{\"code\":\"FAIL\",\"message\":\"" + safe(e.getMessage()) + "\"}");
        }
    }

    // ---------------- 工具 ----------------

    private String queryParam(HttpExchange exchange, String name) {
        String query = exchange.getRequestURI().getQuery();
        if (query == null) return null;
        for (String pair : query.split("&")) {
            int i = pair.indexOf('=');
            if (i > 0 && pair.substring(0, i).equals(name)) {
                return java.net.URLDecoder.decode(pair.substring(i + 1), StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private void redirect(HttpExchange exchange, String location) throws IOException {
        exchange.getResponseHeaders().set("Location", location);
        exchange.sendResponseHeaders(302, -1);
    }

    private String str(Map<?, ?> map, String key) {
        Object value = map.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private String enc(String value) {
        return java.net.URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String safe(String message) {
        if (message == null) return "error";
        return message.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
    }
}
