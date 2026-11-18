package com.plu.plugin.commands.user;

import com.plu.plugin.Main;
import com.plu.plugin.managers.Msg;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpawnCommand implements CommandExecutor {

    private final Main plugin;

    public SpawnCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Msg.send(sender, "&cOnly players can use this command.");
            return true;
        }
        Location spawn = plugin.getSpawnManager().getSpawn();
        if (spawn == null) {
            Msg.send(player, plugin.getPrefix() + "&cSpawn is not set.");
            return true;
        }
        player.teleport(spawn);
        Msg.send(player, plugin.getPrefix() + "&aTeleported to spawn.");
        return true;
    }
}
