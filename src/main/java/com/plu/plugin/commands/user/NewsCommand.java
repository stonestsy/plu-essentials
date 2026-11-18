package com.plu.plugin.commands.user;

import com.plu.plugin.Main;
import com.plu.plugin.gui.BookGui;
import com.plu.plugin.managers.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class NewsCommand implements CommandExecutor {
    private final Main plugin;
    public NewsCommand(Main plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            for (String page : plugin.getAnnouncementManager().getNewsPages()) {
                Msg.send(sender, page);
            }
            return true;
        }
        BookGui.open(player,
                plugin.getAnnouncementManager().getTitle("news", "服务器公告"),
                plugin.getAnnouncementManager().getAuthor("news", "服务器管理组"),
                plugin.getAnnouncementManager().getNewsPages());
        return true;
    }
}
