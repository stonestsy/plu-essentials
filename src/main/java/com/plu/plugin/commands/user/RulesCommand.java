package com.plu.plugin.commands.user;

import com.plu.plugin.Main;
import com.plu.plugin.gui.BookGui;
import com.plu.plugin.managers.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RulesCommand implements CommandExecutor {
    private final Main plugin;
    public RulesCommand(Main plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            for (String page : plugin.getAnnouncementManager().getRulesPages()) {
                Msg.send(sender, page);
            }
            return true;
        }
        BookGui.open(player,
                plugin.getAnnouncementManager().getTitle("rules", "服务器规则"),
                plugin.getAnnouncementManager().getAuthor("rules", "服务器管理组"),
                plugin.getAnnouncementManager().getRulesPages());
        return true;
    }
}
