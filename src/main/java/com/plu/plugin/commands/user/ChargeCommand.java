package com.plu.plugin.commands.user;

import com.plu.plugin.Main;
import com.plu.plugin.managers.Msg;
import com.plu.plugin.managers.RechargeManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /charge —— 获取充值绑定码与充值页地址。 */
public class ChargeCommand implements CommandExecutor {

    private final Main plugin;

    public ChargeCommand(Main plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Msg.send(sender, plugin.getPrefix() + "&c该命令只能由玩家使用。");
            return true;
        }
        if (!plugin.getRechargeManager().isEnabled()) {
            Msg.send(sender, plugin.getPrefix() + "&c充值系统未开启。");
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("list")) {
            Msg.send(sender, plugin.getPrefix() + "&6充值礼包:");
            for (RechargeManager.Package p : plugin.getRechargeManager().packages()) {
                if (!p.enabled) continue;
                Msg.send(sender, " &e" + p.id + " &f" + p.name + " &7- &a"
                        + String.format(java.util.Locale.ROOT, "%.2f", p.priceFen / 100.0) + " 元");
            }
            return true;
        }

        String code = plugin.getRechargeManager().createBindCode(player);
        String url = plugin.getRechargeManager().client().publicBaseUrl() + "/pay";
        Msg.send(sender, plugin.getPrefix() + "&a你的充值绑定码: &e&l" + code + " &7(10 分钟内有效)");
        Msg.send(sender, plugin.getPrefix() + "&7请在微信中打开: &f" + url);
        Msg.send(sender, plugin.getPrefix() + "&7输入绑定码后选择礼包并支付即可。");
        if (plugin.getRechargeManager().isMock()) {
            Msg.send(sender, plugin.getPrefix() + "&e当前为模拟支付模式（未配置微信商户凭据）。");
        }
        return true;
    }
}
