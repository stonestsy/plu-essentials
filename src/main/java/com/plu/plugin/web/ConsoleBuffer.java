package com.plu.plugin.web;

import com.plu.plugin.Main;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/** Captures the last N lines of server log output for the web console. */
public class ConsoleBuffer {

    private final Main plugin;
    private final Deque<String> lines = new ArrayDeque<>();
    private Handler handler;
    private int maxLines;

    public ConsoleBuffer(Main plugin) {
        this.plugin = plugin;
        this.maxLines = plugin.getConfig().getInt("web.console-buffer", 500);
    }

    public void attach() {
        maxLines = plugin.getConfig().getInt("web.console-buffer", 500);
        handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                String line = String.format("[%s] %s", record.getLevel().getName(), record.getMessage());
                push(line);
            }
            @Override
            public void flush() {}
            @Override
            public void close() {}
        };
        plugin.getLogger().getParent().addHandler(handler);
    }

    public void detach() {
        if (handler != null) {
            plugin.getLogger().getParent().removeHandler(handler);
            handler = null;
        }
    }

    private synchronized void push(String line) {
        lines.addLast(line);
        while (lines.size() > maxLines) lines.removeFirst();
    }

    public synchronized String[] getLines(int count) {
        int n = Math.min(count, lines.size());
        String[] out = new String[n];
        Object[] arr = lines.toArray();
        int start = lines.size() - n;
        for (int i = 0; i < n; i++) {
            out[i] = (String) arr[start + i];
        }
        return out;
    }
}
