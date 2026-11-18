package com.plu.plugin.gui;

import com.plu.plugin.managers.Msg;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.List;

/** 以成书界面展示多页文本。 */
public final class BookGui {

    private BookGui() {}

    /** 每页最大字符数，超出会被截断以避免写入失败。 */
    private static final int MAX_PAGE_CHARS = 250;

    public static void open(Player player, String title, String author, List<String> pages) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        if (meta == null) return;

        String safeTitle = Msg.color(title);
        meta.setTitle(safeTitle.length() > 32 ? safeTitle.substring(0, 32) : safeTitle);
        meta.setAuthor(Msg.color(author));

        List<String> colored = new ArrayList<>();
        for (String page : pages) {
            String text = Msg.color(page);
            if (text.length() > MAX_PAGE_CHARS) text = text.substring(0, MAX_PAGE_CHARS);
            colored.add(text);
        }
        if (colored.size() > 100) colored = colored.subList(0, 100);
        meta.setPages(colored);
        book.setItemMeta(meta);

        player.openBook(book);
    }
}
