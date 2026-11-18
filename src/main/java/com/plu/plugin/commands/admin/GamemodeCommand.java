package com.plu.plugin.commands.admin;

import com.plu.plugin.Main;
import com.plu.plugin.managers.Msg;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GamemodeCommand implements CommandExecutor {

    private final Main plugin;

    public GamemodeCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            Msg.send(sender, plugin.getPrefix() + "&cUsage: /gamemode <survival|creative|adventure|spectator> [player]");
            return true;
        }
        GameMode mode = parseMode(args[0]);
        if (mode == null) {
            Msg.send(sender, plugin.getPrefix() + "&cInvalid gamemode: &e" + args[0]);
            return true;
        }
        Player target = args.length > 1 ? Bukkit.getPlayer(args[1]) : (sender instanceof Player ? (Player) sender : null);
        if (target == null) {
            Msg.send(sender, plugin.getPrefix() + "&cPlayer not found.");
            return true;
        }
        target.setGameMode(mode);
        Msg.send(sender, plugin.getPrefix() + "&aGamemode of &e" + target.getName() + " &aset to &e" + mode.name().toLowerCase() + "&a.");
        if (!target.equals(sender)) {
            Msg.send(target, plugin.getPrefix() + "&aYour gamemode was set to &e" + mode.name().toLowerCase() + "&a.");
        }
        return true;
    }

    private GameMode parseMode(String s) {
        switch (s.toLowerCase()) {
            case "survival": case "s": case "0": return GameMode.SURVIVAL;
            case "creative": case "c": case "1": return GameMode.CREATIVE;
            case "adventure": case "a": case "2": return GameMode.ADVENTURE;
            case "spectator": case "sp": case "3": return GameMode.SPECTATOR;
            default: return null;
        }
    }
}
