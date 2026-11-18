package com.plu.plugin.web.hooks;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Hooks into Vault for economy functionality.
 * Uses the Bukkit ServicesManager so it works without a hard dependency.
 */
public class VaultHook {

    private static Economy economy = null;

    public static boolean setup() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    public static boolean isPluginInstalled() {
        return Bukkit.getPluginManager().getPlugin("Vault") != null;
    }

    /**
     * Lazy check: re-attempts setup if the economy provider wasn't registered
     * yet (economy plugins often enable after this plugin).
     */
    public static boolean isAvailable() {
        if (economy != null) return true;
        return setup();
    }

    /** Returns the raw Vault economy provider, or null if unavailable. */
    public static Economy getEconomy() {
        if (economy == null) setup();
        return economy;
    }

    public static double getBalance(OfflinePlayer player) {
        if (economy == null) return 0;
        return economy.getBalance(player);
    }

    public static boolean setBalance(OfflinePlayer player, double amount) {
        if (economy == null) return false;
        double current = economy.getBalance(player);
        if (amount >= current) {
            economy.depositPlayer(player, amount - current);
        } else {
            economy.withdrawPlayer(player, current - amount);
        }
        return true;
    }

    public static boolean add(OfflinePlayer player, double amount) {
        if (economy == null) return false;
        economy.depositPlayer(player, amount);
        return true;
    }

    public static boolean remove(OfflinePlayer player, double amount) {
        if (economy == null) return false;
        economy.withdrawPlayer(player, amount);
        return true;
    }

    public static String getCurrencyName() {
        if (economy == null) return "";
        try {
            return economy.currencyNamePlural();
        } catch (Exception e) {
            return "";
        }
    }
}
