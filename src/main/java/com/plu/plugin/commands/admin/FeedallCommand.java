package com.plu.plugin.commands.admin;

import com.plu.plugin.Main;
import com.plu.plugin.managers.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FeedallCommand implements CommandExecutor {
    private final Main plugin;
    public FeedallCommand(Main plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        int count = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setFoodLevel(20);
            p.setSaturation(20);
            count++;
        }
        Msg.send(sender, plugin.getPrefix() + "&a已喂食 &e" + count + " &a名玩家。");
        return true;
    }
}
