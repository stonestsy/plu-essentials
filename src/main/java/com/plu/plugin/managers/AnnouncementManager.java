package com.plu.plugin.managers;

import com.plu.plugin.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** 服务器公告与规则（以书面形式展示）。 */
public class AnnouncementManager {

    private final Main plugin;
    private final File file;
    private FileConfiguration data;

    public AnnouncementManager(Main plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "announcements.yml");
    }

    public void load() {
        if (!file.exists()) createDefault();
        data = YamlConfiguration.loadConfiguration(file);
    }

    private void createDefault() {
        data = new YamlConfiguration();
        List<String> news = new ArrayList<>();
        news.add("&6欢迎来到服务器！");
        news.add("&7请遵守服务器规则，祝游戏愉快。");
        news.add("&7输入 &e/rules &7查看详细规则。");
        List<String> rules = new ArrayList<>();
        rules.add("&c&l服务器规则\n&r1. 禁止作弊与使用外挂。");
        rules.add("&r2. 禁止恶意 PVP 与刷屏。");
        rules.add("&r3. 禁止破坏他人建筑与领地。");
        rules.add("&r4. 禁止利用漏洞获取利益。");
        rules.add("&r5. 管理员拥有最终解释权。");
        data.set("news.title", "&6服务器公告");
        data.set("news.author", "服务器管理组");
        data.set("news.pages", news);
        data.set("rules.title", "&c服务器规则");
        data.set("rules.author", "服务器管理组");
        data.set("rules.pages", rules);
        try { data.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public void reload() { load(); }

    public String getTitle(String section, String fallback) {
        ensure();
        return data.getString(section + ".title", fallback);
    }

    public String getAuthor(String section, String fallback) {
        ensure();
        return data.getString(section + ".author", fallback);
    }

    /** 公告内容，每条对应书本的一页。 */
    public List<String> getNewsPages() {
        ensure();
        List<String> pages = data.getStringList("news.pages");
        return pages.isEmpty() ? List.of("&7暂无公告。") : pages;
    }

    /** 规则内容，每条对应书本的一页。 */
    public List<String> getRulesPages() {
        ensure();
        List<String> pages = data.getStringList("rules.pages");
        return pages.isEmpty() ? List.of("&7暂无规则。") : pages;
    }

    public void setNews(List<String> pages) {
        ensure();
        data.set("news.pages", pages);
        save();
    }

    public void setRules(List<String> pages) {
        ensure();
        data.set("rules.pages", pages);
        save();
    }

    private void ensure() {
        if (data == null) load();
    }

    private void save() {
        try { data.save(file); } catch (IOException e) { e.printStackTrace(); }
    }
}
