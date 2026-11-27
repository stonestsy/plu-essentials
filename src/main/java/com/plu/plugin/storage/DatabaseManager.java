package com.plu.plugin.storage;

import com.plu.plugin.Main;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * MySQL storage manager. Creates a single reusable connection and
 * auto-creates the required tables. Falls back to YAML when disabled.
 */
public class DatabaseManager {

    private final Main plugin;
    private Connection connection;
    private boolean enabled = false;
    private String prefix;

    public DatabaseManager(Main plugin) {
        this.plugin = plugin;
    }

    public boolean connect() {
        if (!plugin.getConfig().getBoolean("database.enabled", false)) {
            plugin.getLogger().info("Database disabled, using YAML storage.");
            return false;
        }
        String host = plugin.getConfig().getString("database.host", "localhost");
        int port = plugin.getConfig().getInt("database.port", 3306);
        String db = plugin.getConfig().getString("database.database", "pluessentials");
        String user = plugin.getConfig().getString("database.username", "root");
        String pass = plugin.getConfig().getString("database.password", "");
        prefix = plugin.getConfig().getString("database.table-prefix", "plu_");

        String url = "jdbc:mysql://" + host + ":" + port + "/" + db
                + "?useSSL=false&autoReconnect=true&useUnicode=true&characterEncoding=utf8&serverTimezone=UTC";
        try {
            connection = DriverManager.getConnection(url, user, pass);
            enabled = true;
            createTables();
            plugin.getLogger().info("Connected to MySQL database: " + db);
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to connect to MySQL: " + e.getMessage());
            enabled = false;
            return false;
        }
    }

    public void disconnect() {
        if (connection != null) {
            try { connection.close(); } catch (SQLException ignored) {}
            connection = null;
        }
        enabled = false;
    }

    public boolean isEnabled() { return enabled; }
    public String getPrefix() { return prefix; }

    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connect();
        }
        return connection;
    }

    private void createTables() throws SQLException {
        String banTable = prefix + "bans";
        String warpTable = prefix + "warps";
        String taskTable = prefix + "tasks";
        String homeTable = prefix + "homes";

        try (Statement st = connection.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS " + banTable + " ("
                    + "uuid VARCHAR(36) PRIMARY KEY, "
                    + "name VARCHAR(64), "
                    + "reason VARCHAR(255), "
                    + "banner VARCHAR(64), "
                    + "created BIGINT, "
                    + "expires BIGINT)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS " + warpTable + " ("
                    + "name VARCHAR(64) PRIMARY KEY, "
                    + "world VARCHAR(64), "
                    + "x DOUBLE, y DOUBLE, z DOUBLE, "
                    + "yaw FLOAT, pitch FLOAT)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS " + taskTable + " ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "name VARCHAR(64), "
                    + "command VARCHAR(255), "
                    + "cron VARCHAR(64), "
                    + "enabled TINYINT(1), "
                    + "last_run BIGINT DEFAULT 0)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS " + homeTable + " ("
                    + "uuid VARCHAR(36), "
                    + "name VARCHAR(64), "
                    + "world VARCHAR(64), "
                    + "x DOUBLE, y DOUBLE, z DOUBLE, "
                    + "yaw FLOAT, pitch FLOAT, "
                    + "PRIMARY KEY (uuid, name))");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS " + prefix + "ipbans ("
                    + "ip VARCHAR(64) PRIMARY KEY, "
                    + "reason VARCHAR(255), "
                    + "banner VARCHAR(64), "
                    + "created BIGINT, "
                    + "expires BIGINT)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS " + prefix + "whitelist ("
                    + "uuid VARCHAR(36) PRIMARY KEY, "
                    + "name VARCHAR(64))");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS " + prefix + "chatlog ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "time BIGINT, "
                    + "name VARCHAR(64), "
                    + "uuid VARCHAR(36), "
                    + "world VARCHAR(64), "
                    + "message TEXT)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS " + prefix + "playerstats ("
                    + "uuid VARCHAR(36) PRIMARY KEY, "
                    + "name VARCHAR(64), "
                    + "first_join BIGINT, "
                    + "last_seen BIGINT, "
                    + "playtime BIGINT)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS " + prefix + "market ("
                    + "id INT PRIMARY KEY, "
                    + "seller VARCHAR(36), "
                    + "seller_name VARCHAR(64), "
                    + "item TEXT, "
                    + "price DOUBLE, "
                    + "created BIGINT)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS " + prefix + "claims ("
                    + "id INT PRIMARY KEY, "
                    + "owner VARCHAR(36), "
                    + "owner_name VARCHAR(64), "
                    + "world VARCHAR(64), "
                    + "min_x INT, min_z INT, max_x INT, max_z INT, "
                    + "created BIGINT)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS " + prefix + "accounts ("
                    + "uuid VARCHAR(36) PRIMARY KEY, "
                    + "name VARCHAR(64), "
                    + "salt VARCHAR(64), "
                    + "hash VARCHAR(128), "
                    + "registered BIGINT)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS " + prefix + "redeem_codes ("
                    + "code VARCHAR(64) PRIMARY KEY, "
                    + "amount DOUBLE, "
                    + "max_uses INT, "
                    + "used_count INT, "
                    + "expires BIGINT, "
                    + "created BIGINT, "
                    + "creator VARCHAR(64), "
                    + "enabled TINYINT(1))");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS " + prefix + "redeem_uses ("
                    + "code VARCHAR(64), "
                    + "uuid VARCHAR(36), "
                    + "used_at BIGINT, "
                    + "PRIMARY KEY (code, uuid))");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS " + prefix + "balances ("
                    + "uuid VARCHAR(36) PRIMARY KEY, "
                    + "name VARCHAR(64), "
                    + "balance DOUBLE, "
                    + "updated BIGINT)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS " + prefix + "groups ("
                    + "name VARCHAR(64) PRIMARY KEY, "
                    + "prefix VARCHAR(64), "
                    + "weight INT, "
                    + "permissions TEXT)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS " + prefix + "player_groups ("
                    + "uuid VARCHAR(36), "
                    + "group_name VARCHAR(64), "
                    + "PRIMARY KEY (uuid, group_name))");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS " + prefix + "pay_orders ("
                    + "no VARCHAR(40) PRIMARY KEY, "
                    + "uuid VARCHAR(36), "
                    + "player VARCHAR(64), "
                    + "package_id VARCHAR(32), "
                    + "amount_fen BIGINT, "
                    + "coins DOUBLE, "
                    + "status VARCHAR(16), "
                    + "created BIGINT, "
                    + "paid_at BIGINT, "
                    + "openid VARCHAR(64), "
                    + "prepay_id VARCHAR(128), "
                    + "transaction_id VARCHAR(64))");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS " + prefix + "nicknames ("
                    + "uuid VARCHAR(36) PRIMARY KEY, "
                    + "name VARCHAR(64), "
                    + "nick VARCHAR(64), "
                    + "updated BIGINT)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS " + prefix + "npcs ("
                    + "id VARCHAR(16) PRIMARY KEY, "
                    + "name VARCHAR(64), "
                    + "type VARCHAR(32), "
                    + "world VARCHAR(64), "
                    + "x DOUBLE, y DOUBLE, z DOUBLE, yaw FLOAT, pitch FLOAT, "
                    + "action_type VARCHAR(16), "
                    + "action_value VARCHAR(255), "
                    + "skin VARCHAR(64), "
                    + "citizens TINYINT(1), "
                    + "citizens_id INT, "
                    + "entity_uuid VARCHAR(36))");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS " + prefix + "signs ("
                    + "world VARCHAR(64), "
                    + "x INT, y INT, z INT, "
                    + "command VARCHAR(255), "
                    + "permission VARCHAR(64), "
                    + "creator VARCHAR(64), "
                    + "created BIGINT, "
                    + "PRIMARY KEY (world, x, y, z))");
        }
    }

    public int update(String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            return ps.executeUpdate();
        }
    }

    public List<Object[]> query(String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                int cols = md.getColumnCount();
                List<Object[]> rows = new ArrayList<>();
                while (rs.next()) {
                    Object[] row = new Object[cols];
                    for (int i = 0; i < cols; i++) row[i] = rs.getObject(i + 1);
                    rows.add(row);
                }
                return rows;
            }
        }
    }
}
