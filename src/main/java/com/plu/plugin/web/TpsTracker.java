package com.plu.plugin.web;

import com.plu.plugin.Main;
import org.bukkit.Bukkit;

import java.util.ArrayDeque;
import java.util.Deque;

/** Tracks server TPS by sampling tick completion times. */
public class TpsTracker implements Runnable {

    private final Main plugin;
    private final Deque<Long> timestamps = new ArrayDeque<>();
    private int taskId = -1;
    private double tps = 20.0;

    public TpsTracker(Main plugin) {
        this.plugin = plugin;
    }

    public void start() {
        // sample every 40 ticks (2 seconds)
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this, 40L, 40L);
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        timestamps.addLast(now);
        // keep roughly 2 minutes of samples (60 samples * 2s)
        while (timestamps.size() > 60) timestamps.removeFirst();
        if (timestamps.size() >= 2) {
            long first = timestamps.getFirst();
            long elapsed = now - first;
            int intervals = timestamps.size() - 1;
            // each interval is 40 ticks; TPS = (intervals * 40) / (elapsed seconds)
            if (elapsed > 0) {
                double calc = (intervals * 40.0) / (elapsed / 1000.0);
                tps = Math.min(20.0, calc);
            }
        }
    }

    public double getTps() {
        return Math.round(tps * 100.0) / 100.0;
    }
}
