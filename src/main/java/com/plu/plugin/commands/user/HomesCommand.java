package com.plu.plugin.commands.user;

import com.plu.plugin.Main;
import com.plu.plugin.managers.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class HomesCommand implements CommandExecutor {

    private final Main plugin;

    public HomesCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Msg.send(sender, "&cOnly players can use this command.");
            return true;
        }
        List<String> homes = plugin.getHomeManager().getHomes(player.getUniqueId());
        if (homes.isEmpty()) {
            Msg.send(player, plugin.getPrefix() + "&cYou have no homes.");
        } else {
            Msg.send(player, plugin.getPrefix() + "&eYour homes (&a" + homes.size() + "&e):");
            Msg.send(player, "&7" + String.join("&8, &7", homes));
        }
        return true;
    }
}
