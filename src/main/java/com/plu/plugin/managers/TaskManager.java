package com.plu.plugin.managers;

import com.plu.plugin.Main;
import com.plu.plugin.storage.DatabaseManager;
import com.plu.plugin.util.Values;
import org.bukkit.Bukkit;

import java.util.*;

/** Scheduled task manager with simple 5-field cron support. */
public class TaskManager {

    private final Main plugin;
    private final DatabaseManager db;
    private final List<ScheduledTask> tasks = new ArrayList<>();
    private int tickTaskId = -1;
    private int lastMinute = -1;

    public static class ScheduledTask {
        public int id;
        public String name;
        public String command;
        public String cron;
        public boolean enabled;
        public long lastRun;
    }

    public TaskManager(Main plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db = db;
    }

    public void load() {
        tasks.clear();
        if (db.isEnabled()) {
            try {
                String tbl = db.getPrefix() + "tasks";
                for (Object[] row : db.query("SELECT id,name,command,cron,enabled,last_run FROM " + tbl)) {
                    ScheduledTask t = new ScheduledTask();
                    t.id = Values.asInt(row[0], 0);
                    t.name = (String) row[1];
                    t.command = (String) row[2];
                    t.cron = (String) row[3];
                    t.enabled = Values.asBool(row[4], false);
                    t.lastRun = Values.asLong(row[5], 0);
                    tasks.add(t);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load tasks from MySQL: " + e.getMessage());
            }
        } else {
            // YAML not used for tasks; they persist only via MySQL. Print hint.
            plugin.getLogger().info("Scheduled tasks require MySQL to be enabled.");
        }
    }

    public void start() {
        // check every 20 ticks (1 second)
        tickTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::tick, 20L, 20L);
    }

    public void stop() {
        if (tickTaskId != -1) {
            Bukkit.getScheduler().cancelTask(tickTaskId);
            tickTaskId = -1;
        }
    }

    private void tick() {
        Calendar now = Calendar.getInstance();
        int minute = now.get(Calendar.MINUTE);
        if (minute == lastMinute) return;
        lastMinute = minute;

        for (ScheduledTask t : tasks) {
            if (!t.enabled) continue;
            if (matchesCron(t.cron, now)) {
                execute(t);
            }
        }
    }

    private void execute(ScheduledTask t) {
        t.lastRun = System.currentTimeMillis();
        if (db.isEnabled()) {
            try {
                db.update("UPDATE " + db.getPrefix() + "tasks SET last_run=? WHERE id=?", t.lastRun, t.id);
            } catch (Exception ignored) {}
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            String cmd = t.command.startsWith("/") ? t.command.substring(1) : t.command;
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            plugin.getLogger().info("[Task] Executed scheduled task '" + t.name + "': " + cmd);
        });
    }

    public boolean matchesCron(String cron, Calendar cal) {
        String[] parts = cron.trim().split("\\s+");
        if (parts.length != 5) return false;
        int minute = cal.get(Calendar.MINUTE);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int dom = cal.get(Calendar.DAY_OF_MONTH);
        int month = cal.get(Calendar.MONTH) + 1;
        int dow = cal.get(Calendar.DAY_OF_WEEK) - 1; // 0=Sunday

        return matchField(parts[0], minute, 0, 59)
                && matchField(parts[1], hour, 0, 23)
                && matchField(parts[2], dom, 1, 31)
                && matchField(parts[3], month, 1, 12)
                && matchField(parts[4], dow, 0, 6);
    }

    private boolean matchField(String field, int value, int min, int max) {
        for (String part : field.split(",")) {
            if (part.equals("*")) return true;
            if (part.startsWith("*/")) {
                int step = Integer.parseInt(part.substring(2));
                if (value % step == 0) return true;
                continue;
            }
            if (part.contains("-")) {
                String[] r = part.split("-");
                int lo = Integer.parseInt(r[0]);
                int hi = Integer.parseInt(r[1]);
                if (value >= lo && value <= hi) return true;
                continue;
            }
            if (Integer.parseInt(part) == value) return true;
        }
        return false;
    }

    // ---- CRUD ----
    public List<ScheduledTask> getAll() { return tasks; }

    public ScheduledTask create(String name, String command, String cron) {
        ScheduledTask t = new ScheduledTask();
        t.name = name;
        t.command = command;
        t.cron = cron;
        t.enabled = true;
        t.lastRun = 0;
        if (db.isEnabled()) {
            try {
                db.update("INSERT INTO " + db.getPrefix() + "tasks (name,command,cron,enabled,last_run) VALUES (?,?,?,?,?)",
                        name, command, cron, 1, 0L);
                // get auto id
                List<Object[]> rows = db.query("SELECT LAST_INSERT_ID()");
                t.id = Values.asInt(rows.get(0)[0], 0);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to create task in MySQL: " + e.getMessage());
                t.id = tasks.size() + 1;
            }
        } else {
            t.id = tasks.size() + 1;
        }
        tasks.add(t);
        return t;
    }

    public boolean update(int id, String name, String command, String cron, boolean enabled) {
        ScheduledTask t = getById(id);
        if (t == null) return false;
        t.name = name; t.command = command; t.cron = cron; t.enabled = enabled;
        if (db.isEnabled()) {
            try {
                db.update("UPDATE " + db.getPrefix() + "tasks SET name=?,command=?,cron=?,enabled=? WHERE id=?",
                        name, command, cron, enabled ? 1 : 0, id);
            } catch (Exception ignored) {}
        }
        return true;
    }

    public boolean delete(int id) {
        ScheduledTask t = getById(id);
        if (t == null) return false;
        tasks.remove(t);
        if (db.isEnabled()) {
            try { db.update("DELETE FROM " + db.getPrefix() + "tasks WHERE id=?", id); }
            catch (Exception ignored) {}
        }
        return true;
    }

    public boolean runNow(int id) {
        ScheduledTask t = getById(id);
        if (t == null) return false;
        execute(t);
        return true;
    }

    public ScheduledTask getById(int id) {
        for (ScheduledTask t : tasks) if (t.id == id) return t;
        return null;
    }
}
