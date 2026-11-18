package com.plu.plugin.listeners;

import com.plu.plugin.Main;
import com.plu.plugin.managers.ClaimManager;
import com.plu.plugin.managers.Msg;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/** 领地保护：拦截在他人领地内的破坏、放置与容器操作。 */
public class ClaimListener implements Listener {

    private final Main plugin;
    public ClaimListener(Main plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        check(event.getPlayer(), event.getBlock(), "破坏方块");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        check(event.getPlayer(), event.getBlock(), "放置方块");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null) return;
        if (!isContainer(block)) return;
        check(event.getPlayer(), block, "操作容器");
    }

    private boolean isContainer(Block block) {
        return switch (block.getType()) {
            case CHEST, TRAPPED_CHEST, BARREL, FURNACE, BLAST_FURNACE, SMOKER,
                 HOPPER, DROPPER, DISPENSER, BREWING_STAND, SHULKER_BOX,
                 WHITE_SHULKER_BOX, ORANGE_SHULKER_BOX, MAGENTA_SHULKER_BOX, LIGHT_BLUE_SHULKER_BOX,
                 YELLOW_SHULKER_BOX, LIME_SHULKER_BOX, PINK_SHULKER_BOX, GRAY_SHULKER_BOX,
                 LIGHT_GRAY_SHULKER_BOX, CYAN_SHULKER_BOX, PURPLE_SHULKER_BOX, BLUE_SHULKER_BOX,
                 BROWN_SHULKER_BOX, GREEN_SHULKER_BOX, RED_SHULKER_BOX, BLACK_SHULKER_BOX,
                 ENCHANTING_TABLE, ANVIL, CHIPPED_ANVIL, DAMAGED_ANVIL -> true;
            default -> false;
        };
    }

    private void check(Player player, Block block, String action) {
        if (!plugin.getClaimManager().canBuild(player, block.getLocation())) {
            ClaimManager.Claim claim = plugin.getClaimManager().getAt(block.getLocation());
            String owner = claim != null ? claim.ownerName : "他人";
            Msg.send(player, plugin.getPrefix() + "&c这里属于 &e" + owner + " &c的领地，无法" + action + "。");
        }
    }
}
