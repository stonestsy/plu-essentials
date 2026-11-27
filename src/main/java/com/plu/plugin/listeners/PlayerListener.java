package com.plu.plugin.listeners;

import com.plu.plugin.Main;
import com.plu.plugin.commands.admin.InvseeCommand;
import com.plu.plugin.managers.Msg;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final Main plugin;

    public PlayerListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();

        // 中文 ID 兼容：默认不拦截非 ASCII 名字（是否允许由服务端/客户端决定）
        if (!plugin.getConfig().getBoolean("compat.allow-non-ascii-names", true)
                && !isAscii(player.getName())) {
            event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
            event.setKickMessage("§c本服务器仅允许英文数字用户名。");
            return;
        }

        // IP ban check
        String ip = event.getAddress() != null ? event.getAddress().getHostAddress() : "";
        if (!ip.isEmpty() && plugin.getIpBanManager().isBanned(ip)) {
            Object[] ban = plugin.getIpBanManager().getBan(ip);
            String reason = ban != null ? (String) ban[0] : "违反服务器规则";
            event.setResult(PlayerLoginEvent.Result.KICK_BANNED);
            event.setKickMessage("§c你的 IP 已被封禁！\n§7原因: §f" + reason);
            return;
        }

        // UUID ban check
        if (plugin.getBanManager().isBanned(player.getUniqueId())) {
            Object[] ban = plugin.getBanManager().getBan(player.getUniqueId());
            String reason = ban != null ? (String) ban[1] : "违反服务器规则";
            String banner = ban != null ? (String) ban[2] : "管理员";
            long expires = ban != null ? (Long) ban[4] : 0;
            event.setResult(PlayerLoginEvent.Result.KICK_BANNED);
            event.setKickMessage(plugin.getBanManager().buildBanMessage(reason, banner, expires));
            return;
        }

        // Whitelist check
        if (plugin.getWhitelistManager().isEnabled()
                && !plugin.getWhitelistManager().isWhitelisted(player.getUniqueId())) {
            event.setResult(PlayerLoginEvent.Result.KICK_WHITELIST);
            event.setKickMessage("§c服务器已启用白名单，您不在白名单中。");
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getNicknameManager().rememberName(player.getUniqueId(), player.getName());
        plugin.getNicknameManager().apply(player);
        plugin.getVanishManager().onJoin(player);
        plugin.getPlayerStatsManager().onJoin(player.getUniqueId(), player.getName());
        plugin.getTabManager().update(player);
        plugin.getEconomyManager().ensureAccount(player);
        plugin.getGroupManager().applyTo(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getTpaManager().clearFor(player.getUniqueId());
        plugin.getVanishManager().remove(player.getUniqueId());
        plugin.getPlayerStatsManager().onQuit(player.getUniqueId());
        plugin.getGroupManager().clear(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        plugin.getChatLogManager().log(event.getPlayer().getName(),
                event.getPlayer().getUniqueId(),
                event.getPlayer().getWorld().getName(),
                event.getMessage());
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player p) {
            InvseeCommand.removeSession(p.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (plugin.getGodManager().isGod(player.getUniqueId())) {
            event.setCancelled(true);
            player.setFireTicks(0);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (plugin.getBackManager().isEnabled()) {
            plugin.getBackManager().setDeathLocation(player.getUniqueId(), player.getLocation());
            String msg = plugin.getConfig().getString("back.death-message",
                    "&cYou died! Use &e/back &cto return to your death point.");
            Msg.send(player, plugin.getPrefix() + msg);
        }
        // God mode doesn't prevent death if it was bypassed; clean up just in case
        if (plugin.getGodManager().isGod(player.getUniqueId())) {
            plugin.getGodManager().remove(player.getUniqueId());
        }
    }

    private boolean isAscii(String name) {
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c < 0x20 || c > 0x7e) return false;
        }
        return true;
    }
}
