package com.plu.plugin.commands.admin;

import com.plu.plugin.Main;
import com.plu.plugin.managers.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ChatlogCommand implements CommandExecutor {
    private final Main plugin;
    public ChatlogCommand(Main plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        int count = 20;
        String keyword = null;
        if (args.length > 0) {
            try { count = Integer.parseInt(args[0]); }
            catch (Exception e) { keyword = args[0]; }
        }
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        java.util.List<Object[]> logs = keyword != null
                ? plugin.getChatLogManager().search(keyword, count)
                : plugin.getChatLogManager().getRecent(count);
        if (logs.isEmpty()) {
            Msg.send(sender, plugin.getPrefix() + "&c没有聊天记录。");
            return true;
        }
        Msg.send(sender, plugin.getPrefix() + "&e最近 " + logs.size() + " 条聊天记录:");
        for (Object[] e : logs) {
            String time = sdf.format(new Date((Long) e[0]));
            Msg.send(sender, "  &7[" + time + "] &e" + e[1] + "&7: &f" + e[4]);
        }
        return true;
    }
}
