package com.plu.plugin.commands.admin;

import com.plu.plugin.Main;
import com.plu.plugin.managers.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpeedCommand implements CommandExecutor {
    private final Main plugin;
    public SpeedCommand(Main plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            Msg.send(sender, plugin.getPrefix() + "&cUsage: /speed <1-10> [player]");
            return true;
        }
        float speed;
        try { speed = Float.parseFloat(args[0]); }
        catch (Exception e) { Msg.send(sender, plugin.getPrefix() + "&c速度必须是数字"); return true; }
        speed = Math.max(0, Math.min(10, speed)) / 10f;
        Player target = args.length > 1 ? Bukkit.getPlayer(args[1]) : (sender instanceof Player ? (Player) sender : null);
        if (target == null) { Msg.send(sender, plugin.getPrefix() + "&c请指定玩家"); return true; }
        if (target.isFlying()) target.setFlySpeed(speed);
        else target.setWalkSpeed(speed);
        Msg.send(sender, plugin.getPrefix() + "&a速度设为 &e" + (speed * 10) + " &a(模式: " + (target.isFlying() ? "飞行" : "行走") + ")");
        return true;
    }
}
