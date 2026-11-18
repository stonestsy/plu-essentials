package com.plu.plugin.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.UUID;

/** Simple utility for colored messages shared across the plugin. */
public final class Msg {

    private Msg() {}

    public static String color(String s) {
        if (s == null) return "";
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static void send(CommandSender sender, String message) {
        sender.sendMessage(color(message));
    }

    public static void sendPrefixed(CommandSender sender, String prefix, String message) {
        sender.sendMessage(color(prefix + message));
    }

    public static void broadcast(String message) {
        Bukkit.broadcastMessage(color(message));
    }

    public static String name(UUID uuid) {
        return Bukkit.getOfflinePlayer(uuid).getName();
    }
}
