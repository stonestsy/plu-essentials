package com.plu.plugin.web;

import com.plu.plugin.Main;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/** Simple token-based auth. Tokens are stored in memory with expiry. */
public class Auth {

    private final Main plugin;
    private final Map<String, Long> tokens = new HashMap<>();
    private final SecureRandom random = new SecureRandom();

    public Auth(Main plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        tokens.clear();
    }

    public boolean checkPassword(String password) {
        String expected = plugin.getConfig().getString("web.password", "admin123");
        return expected.equals(password);
    }

    public String issueToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        long expiry = System.currentTimeMillis() + getExpiryMillis();
        tokens.put(token, expiry);
        return token;
    }

    public boolean isValid(String token) {
        if (token == null) return false;
        Long expiry = tokens.get(token);
        if (expiry == null) return false;
        if (System.currentTimeMillis() > expiry) {
            tokens.remove(token);
            return false;
        }
        // refresh expiry on use
        tokens.put(token, System.currentTimeMillis() + getExpiryMillis());
        return true;
    }

    public void revoke(String token) {
        tokens.remove(token);
    }

    private long getExpiryMillis() {
        int minutes = plugin.getConfig().getInt("web.token-expiry", 60);
        return minutes * 60_000L;
    }
}
