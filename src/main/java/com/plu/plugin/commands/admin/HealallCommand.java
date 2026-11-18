package com.plu.plugin.commands.admin;

import com.plu.plugin.Main;
import com.plu.plugin.managers.Msg;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HealallCommand implements CommandExecutor {
    private final Main plugin;
    public HealallCommand(Main plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        int count = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            double max = p.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null
                    ? p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() : 20.0;
            p.setHealth(max);
            p.setFoodLevel(20);
            p.setSaturation(20);
            p.setFireTicks(0);
            count++;
        }
        Msg.send(sender, plugin.getPrefix() + "&a已治疗 &e" + count + " &a名玩家。");
        return true;
    }
}
