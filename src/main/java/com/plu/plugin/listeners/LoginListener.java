package com.plu.plugin.listeners;

import com.plu.plugin.Main;
import com.plu.plugin.managers.Msg;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.*;

/** 未登录玩家的行为限制。 */
public class LoginListener implements Listener {

    private final Main plugin;

    public LoginListener(Main plugin) { this.plugin = plugin; }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getLoginManager().isEnabled()) return;

        if (plugin.getLoginManager().needsRegister(player.getUniqueId())) {
            Msg.send(player, plugin.getPrefix() + "&e请先注册: &f/register <密码>");
            Msg.send(player, plugin.getPrefix() + "&7密码长度 4-32 位，请牢记。");
        } else if (plugin.getLoginManager().needsLogin(player.getUniqueId())) {
            Msg.send(player, plugin.getPrefix() + "&e请登录: &f/login <密码>");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (restrict(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (restrict(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (restrict(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getLoginManager().isRestricted(player.getUniqueId())) return;
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;
        // 只允许原地转头，不允许位移
        if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
            event.setTo(from.clone());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getLoginManager().isRestricted(player.getUniqueId())) return;
        event.setCancelled(true);
        Msg.send(player, plugin.getPrefix() + "&c登录前无法聊天。");
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getLoginManager().isRestricted(player.getUniqueId())) return;
        String cmd = event.getMessage().toLowerCase();
        if (cmd.startsWith("/login") || cmd.startsWith("/register")) return;
        if (player.hasPermission("plu.login.bypass")) return;
        event.setCancelled(true);
        Msg.send(player, plugin.getPrefix() + "&c登录前只能使用 /login 或 /register。");
    }

    /** 未登录玩家受限时返回 true，并给出提示。 */
    private boolean restrict(Player player) {
        if (!plugin.getLoginManager().isRestricted(player.getUniqueId())) return false;
        Msg.send(player, plugin.getPrefix() + "&c请先登录！使用 &e/login <密码> &c或 &e/register <密码>");
        return true;
    }
}
