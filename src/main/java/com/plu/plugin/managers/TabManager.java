package com.plu.plugin.managers;

import com.plu.plugin.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/** 自定义 TAB 列表（页眉/页脚），支持占位符。 */
public class TabManager {

    private final Main plugin;
    private BukkitTask task;

    public TabManager(Main plugin) { this.plugin = plugin; }

    public void start() {
        stop();
        if (!plugin.getConfig().getBoolean("tab.enabled", true)) return;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::updateAll, 20L, 100L);
    }

    public void stop() {
        if (task != null) { task.cancel(); task = null; }
    }

    public void updateAll() {
        for (Player player : Bukkit.getOnlinePlayers()) update(player);
    }

    public void update(Player player) {
        String header = plugin.getConfig().getString("tab.header", "&b&lPluEssentials");
        String footer = plugin.getConfig().getString("tab.footer",
                "&7在线: &a{online}&7/&a{max}  &7世界: &a{world}  &7余额: &a{balance}");

        header = apply(header, player);
        footer = apply(footer, player);

        try {
            player.setPlayerListHeaderFooter(Msg.color(header), Msg.color(footer));
        } catch (Exception ignored) {
            // 某些服务端实现可能不支持，静默忽略
        }
    }

    private String apply(String text, Player player) {
        String result = text;
        result = result.replace("{player}", player.getName());
        result = result.replace("{online}", String.valueOf(Bukkit.getOnlinePlayers().size()));
        result = result.replace("{max}", String.valueOf(Bukkit.getMaxPlayers()));
        result = result.replace("{world}", player.getWorld().getName());
        result = result.replace("{ping}", String.valueOf(player.getPing()));

        if (result.contains("{balance}")) {
            String balance = "N/A";
            if (plugin.getEconomyManager() != null && plugin.getEconomyManager().available()) {
                balance = plugin.getEconomyManager().format(
                        plugin.getEconomyManager().balance(player.getUniqueId()));
            }
            result = result.replace("{balance}", balance);
        }

        // 服务器时间（现实时间）
        if (result.contains("{time}")) {
            result = result.replace("{time}", new java.text.SimpleDateFormat("HH:mm").format(new java.util.Date()));
        }
        return result;
    }
}
