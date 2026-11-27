package com.plu.plugin.pay;

import com.plu.plugin.Main;
import com.plu.plugin.web.Json;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 微信支付 APIv3（JSAPI 公众号支付）客户端。
 * 仅使用 JDK 自带 HttpClient 与 JCA，无额外依赖。
 * 未配置商户凭据时由 RechargeManager 自动降级为模拟支付。
 */
public class WeChatPayClient {

    public static class NotifyPayload {
        public String outTradeNo;
        public String transactionId;
        public String tradeState;
        public String openid;
        public long totalFen;
    }

    private final Main plugin;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private PrivateKey privateKey;
    private String keyError = "";

    public WeChatPayClient(Main plugin) {
        this.plugin = plugin;
    }

    // ---------------- 配置 ----------------

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("wechatpay.enabled", false);
    }

    public String appId() { return cfg("app-id"); }
    public String appSecret() { return cfg("app-secret"); }
    public String mchId() { return cfg("mch-id"); }
    public String serialNo() { return cfg("cert-serial-no"); }
    public String apiV3Key() { return cfg("api-v3-key"); }
    public String notifyUrl() { return cfg("notify-url"); }
    public String description() {
        String d = cfg("description");
        return d.isEmpty() ? "Minecraft 服务器充值" : d;
    }

    public String publicBaseUrl() {
        String base = cfg("public-base-url");
        if (base.isEmpty()) {
            base = "http://127.0.0.1:" + plugin.getConfig().getInt("web.port", 8080);
        }
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return base;
    }

    /** 是否具备真实调用条件（凭据齐全且私钥可解析）。 */
    public boolean isConfigured() {
        if (appId().isEmpty() || appSecret().isEmpty() || mchId().isEmpty() || serialNo().isEmpty()
                || apiV3Key().length() != 32 || notifyUrl().isEmpty()) return false;
        return loadPrivateKey() != null;
    }

    /** 模拟支付：显式开启，或凭据不全时的自动降级。 */
    public boolean isMock() {
        if (plugin.getConfig().getBoolean("wechatpay.mock", true)) return true;
        return !isConfigured();
    }

    public String lastError() {
        return keyError;
    }

    private String cfg(String key) {
        String value = plugin.getConfig().getString("wechatpay." + key, "");
        return value == null ? "" : value.trim();
    }

    // ---------------- 私钥 ----------------

    private PrivateKey loadPrivateKey() {
        if (privateKey != null) return privateKey;
        String path = cfg("private-key-path");
        if (path.isEmpty()) {
            keyError = "wechatpay.private-key-path 未配置";
            return null;
        }
        File file = new File(path);
        if (!file.isAbsolute()) file = new File(plugin.getDataFolder(), path);
        if (!file.isFile()) {
            keyError = "私钥文件不存在: " + file.getAbsolutePath();
            return null;
        }
        try {
            String pem = Files.readString(Path.of(file.getAbsolutePath()), StandardCharsets.UTF_8);
            String body = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] der = Base64.getDecoder().decode(body);
            privateKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
            keyError = "";
            return privateKey;
        } catch (Exception e) {
            keyError = "私钥解析失败（需要 PKCS#8 PEM）: " + e.getMessage();
            plugin.getLogger().warning("WeChat Pay: " + keyError);
            return null;
        }
    }

    // ---------------- OAuth ----------------

    public String buildOAuthUrl(String bindCode) {
        String redirect = URLEncoder.encode(publicBaseUrl() + "/pay/callback", StandardCharsets.UTF_8);
        String state = URLEncoder.encode(bindCode == null ? "" : bindCode, StandardCharsets.UTF_8);
        return "https://open.weixin.qq.com/connect/oauth2/authorize?appid=" + appId()
                + "&redirect_uri=" + redirect
                + "&response_type=code&scope=snsapi_base&state=" + state + "#wechat_redirect";
    }

    /** 用 OAuth code 换取 openid。 */
    public String exchangeOpenId(String code) throws Exception {
        String url = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=" + appId()
                + "&secret=" + appSecret()
                + "&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                + "&grant_type=authorization_code";
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15)).GET().build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        Object parsed = Json.parse(response.body());
        if (parsed instanceof Map<?, ?> map) {
            Object openid = map.get("openid");
            if (openid != null) return String.valueOf(openid);
            throw new IllegalStateException("微信 OAuth 失败: " + map.get("errmsg"));
        }
        throw new IllegalStateException("微信 OAuth 返回无法解析");
    }

    // ---------------- 下单 ----------------

    /** 创建 JSAPI 订单，返回 prepay_id。 */
    public String createJsapiOrder(String outTradeNo, long amountFen, String openid) throws Exception {
        Map<String, Object> amount = new LinkedHashMap<>();
        amount.put("total", amountFen);
        amount.put("currency", "CNY");
        Map<String, Object> payer = new LinkedHashMap<>();
        payer.put("openid", openid);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("appid", appId());
        body.put("mchid", mchId());
        body.put("description", description());
        body.put("out_trade_no", outTradeNo);
        body.put("notify_url", notifyUrl());
        body.put("amount", amount);
        body.put("payer", payer);

        String json = Json.stringify(body);
        String response = postJson("/v3/pay/transactions/jsapi", json);
        Object parsed = Json.parse(response);
        if (parsed instanceof Map<?, ?> map) {
            Object prepay = map.get("prepay_id");
            if (prepay != null) return String.valueOf(prepay);
            throw new IllegalStateException("微信下单失败: " + map.get("message"));
        }
        throw new IllegalStateException("微信下单返回无法解析");
    }

    /** 生成 WeixinJSBridge 调起支付所需参数。 */
    public Map<String, String> buildPayParams(String prepayId) throws Exception {
        String timeStamp = String.valueOf(Instant.now().getEpochSecond());
        String nonceStr = nonce();
        String pkg = "prepay_id=" + prepayId;
        String message = appId() + "\n" + timeStamp + "\n" + nonceStr + "\n" + pkg + "\n";
        String paySign = sign(message);

        Map<String, String> params = new LinkedHashMap<>();
        params.put("appId", appId());
        params.put("timeStamp", timeStamp);
        params.put("nonceStr", nonceStr);
        params.put("package", pkg);
        params.put("signType", "RSA");
        params.put("paySign", paySign);
        return params;
    }

    // ---------------- 回调 ----------------

    /**
     * 解析支付回调。resource 使用 APIv3 密钥 AES-256-GCM 解密。
     * platformCertPath 配置后还会校验微信平台签名。
     */
    public NotifyPayload parseNotify(Map<String, String> headers, String body) throws Exception {
        verifyNotifySignature(headers, body);

        Object parsed = Json.parse(body);
        if (!(parsed instanceof Map<?, ?> root)) throw new IllegalStateException("回调 JSON 无法解析");
        Object resourceObj = root.get("resource");
        if (!(resourceObj instanceof Map<?, ?> resource)) throw new IllegalStateException("回调缺少 resource");

        String nonce = String.valueOf(resource.get("nonce"));
        String associated = resource.get("associated_data") == null ? "" : String.valueOf(resource.get("associated_data"));
        String ciphertext = String.valueOf(resource.get("ciphertext"));

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec key = new SecretKeySpec(apiV3Key().getBytes(StandardCharsets.UTF_8), "AES");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, nonce.getBytes(StandardCharsets.UTF_8)));
        cipher.updateAAD(associated.getBytes(StandardCharsets.UTF_8));
        byte[] plain = cipher.doFinal(Base64.getDecoder().decode(ciphertext));

        Object decrypted = Json.parse(new String(plain, StandardCharsets.UTF_8));
        if (!(decrypted instanceof Map<?, ?> data)) throw new IllegalStateException("回调密文无法解析");

        NotifyPayload payload = new NotifyPayload();
        payload.outTradeNo = String.valueOf(data.get("out_trade_no"));
        payload.transactionId = String.valueOf(data.get("transaction_id"));
        payload.tradeState = String.valueOf(data.get("trade_state"));
        payload.openid = data.get("payer") instanceof Map<?, ?> payer ? String.valueOf(payer.get("openid")) : "";
        payload.totalFen = data.get("amount") instanceof Map<?, ?> amount
                ? asLong(amount.get("total")) : 0;
        return payload;
    }

    private void verifyNotifySignature(Map<String, String> headers, String body) throws Exception {
        String certPath = cfg("platform-cert-path");
        if (certPath.isEmpty()) return; // 未配置平台证书时跳过（README 已注明生产环境应配置）
        String timestamp = headers.get("Wechatpay-Timestamp");
        String nonce = headers.get("Wechatpay-Nonce");
        String signature = headers.get("Wechatpay-Signature");
        if (timestamp == null || nonce == null || signature == null) {
            throw new IllegalStateException("缺少微信支付签名头");
        }
        File file = new File(certPath);
        if (!file.isAbsolute()) file = new File(plugin.getDataFolder(), certPath);
        java.security.cert.CertificateFactory factory = java.security.cert.CertificateFactory.getInstance("X.509");
        try (var in = Files.newInputStream(file.toPath())) {
            var cert = (java.security.cert.X509Certificate) factory.generateCertificate(in);
            String message = timestamp + "\n" + nonce + "\n" + body + "\n";
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(cert.getPublicKey());
            verifier.update(message.getBytes(StandardCharsets.UTF_8));
            if (!verifier.verify(Base64.getDecoder().decode(signature))) {
                throw new IllegalStateException("微信支付回调签名校验失败");
            }
        }
    }

    // ---------------- HTTP + 签名 ----------------

    private String postJson(String path, String body) throws Exception {
        long timestamp = Instant.now().getEpochSecond();
        String nonce = nonce();
        String message = "POST\n" + path + "\n" + timestamp + "\n" + nonce + "\n" + body + "\n";
        String signature = sign(message);
        String authorization = "WECHATPAY2-SHA256-RSA2048 mchid=\"" + mchId()
                + "\",nonce_str=\"" + nonce
                + "\",signature=\"" + signature
                + "\",timestamp=\"" + timestamp
                + "\",serial_no=\"" + serialNo() + "\"";

        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.mch.weixin.qq.com" + path))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Accept", "application/json")
                .header("Authorization", authorization)
                .header("User-Agent", "PluEssentials/3.0.0")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() / 100 != 2) {
            throw new IllegalStateException("微信支付 HTTP " + response.statusCode() + ": " + response.body());
        }
        return response.body();
    }

    private String sign(String message) throws Exception {
        PrivateKey key = loadPrivateKey();
        if (key == null) throw new IllegalStateException(keyError);
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(key);
        signature.update(message.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature.sign());
    }

    private String nonce() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private long asLong(Object value) {
        if (value instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return 0;
        }
    }
}
