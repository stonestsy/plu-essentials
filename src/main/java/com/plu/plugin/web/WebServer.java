package com.plu.plugin.web;

import com.plu.plugin.Main;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * Embedded HTTP server for the admin web panel.
 * Uses only JDK built-in classes (no external dependencies).
 */
public class WebServer {

    private final Main plugin;
    private final Auth auth;
    private final ApiRouter router;
    private HttpServer server;
    private final ConsoleBuffer consoleBuffer;
    private final TpsTracker tpsTracker;

    public WebServer(Main plugin) {
        this.plugin = plugin;
        this.auth = new Auth(plugin);
        this.consoleBuffer = new ConsoleBuffer(plugin);
        this.tpsTracker = new TpsTracker(plugin);
        this.router = new ApiRouter(plugin, auth, consoleBuffer, tpsTracker);
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("web.enabled", true)) {
            plugin.getLogger().info("Web panel is disabled in config.");
            return;
        }
        String host = plugin.getConfig().getString("web.host", "0.0.0.0");
        int port = plugin.getConfig().getInt("web.port", 8080);
        try {
            server = HttpServer.create(new InetSocketAddress(host, port), 0);
            server.createContext("/", new RootHandler());
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
            tpsTracker.start();
            consoleBuffer.attach();
            plugin.getLogger().info("Web admin panel running at http://" + host + ":" + port);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to start web server: " + e.getMessage());
        }
    }

    public void stop() {
        if (tpsTracker != null) tpsTracker.stop();
        if (consoleBuffer != null) consoleBuffer.detach();
        if (server != null) {
            server.stop(0);
            plugin.getLogger().info("Web admin panel stopped.");
        }
    }

    public void reload() {
        auth.reload();
    }

    /** Serves the SPA frontend and dispatches /api/* to the router. */
    private class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            try {
                if (path.startsWith("/api/")) {
                    router.handle(exchange);
                } else {
                    serveFrontend(exchange, path);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Web request error for " + path + ": " + e);
                sendJson(exchange, 500, "{\"error\":\"Internal server error\"}");
            }
        }
    }

    private void serveFrontend(HttpExchange exchange, String path) throws IOException {
        String externalDir = plugin.getConfig().getString("web.external-web-dir", "");
        if (externalDir != null && !externalDir.isEmpty()) {
            File f = new File(externalDir, path.equals("/") ? "index.html" : path);
            if (f.exists() && f.isFile()) {
                byte[] data = java.nio.file.Files.readAllBytes(f.toPath());
                exchange.getResponseHeaders().set("Content-Type", contentType(f.getName()));
                exchange.sendResponseHeaders(200, data.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(data); }
                return;
            }
            // SPA fallback to index.html
            File idx = new File(externalDir, "index.html");
            if (idx.exists()) {
                byte[] data = java.nio.file.Files.readAllBytes(idx.toPath());
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                exchange.sendResponseHeaders(200, data.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(data); }
                return;
            }
        }
        // Embedded resources
        String resource = path.equals("/") ? "/web/index.html" : "/web" + path;
        byte[] data = readResource(resource);
        if (data == null) {
            // SPA fallback
            data = readResource("/web/index.html");
        }
        if (data == null) {
            sendJson(exchange, 404, "{\"error\":\"Not found\"}");
            return;
        }
        exchange.getResponseHeaders().set("Content-Type", contentType(resource));
        exchange.sendResponseHeaders(200, data.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(data); }
    }

    private String contentType(String name) {
        String lower = name.toLowerCase();
        if (lower.endsWith(".html")) return "text/html; charset=utf-8";
        if (lower.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (lower.endsWith(".css")) return "text/css; charset=utf-8";
        if (lower.endsWith(".json")) return "application/json; charset=utf-8";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".ico")) return "image/x-icon";
        if (lower.endsWith(".woff2")) return "font/woff2";
        return "application/octet-stream";
    }

    private byte[] readResource(String path) {
        try (var in = getClass().getResourceAsStream(path)) {
            if (in == null) return null;
            return in.readAllBytes();
        } catch (IOException e) {
            return null;
        }
    }

    public static void sendJson(HttpExchange exchange, int code, String json) throws IOException {
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(code, data.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(data);
        }
    }

    public static String readBody(HttpExchange exchange) throws IOException {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }
}
