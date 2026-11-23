package com.toolbox.pc;

import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 久坐 / 喝水提醒：按活跃度累计连续坐着的时间和距上次喝水的时间，到点用提醒窗口喊一声。
 * 连续 5 分钟没有任何输入视为人不在，自动暂停计时；数据落 ToolboxPC/health.txt，
 * 一行一天：日期 \t 久坐提醒次数 \t 喝水提醒次数。
 */
public class HealthCare {

    static final String FILE = "health.txt";
    static final long IDLE_MS = 5L * 60 * 1000;
    static final long MAX_STEP = 15L * 1000;
    static final int KEEP_DAYS = 60;

    /* ---------- 配置 ---------- */
    public static boolean sitOn = false;
    public static boolean waterOn = false;
    public static int sitMin = 60;
    public static int waterMin = 45;

    /* ---------- 运行态 ---------- */
    public static String day = "";
    public static long sitMs = 0;
    public static long waterMs = 0;
    public static int sitCount = 0;
    public static int waterCount = 0;
    public static long lastTick = 0;
    public static long lastActive = 0;
    static Point lastMouse = null;
    static javax.swing.Timer ticker;
    static AWTEventListener listener;

    public static void start() {
        reload();
        loadToday();
        long now = System.currentTimeMillis();
        lastTick = now;
        lastActive = now;
        lastMouse = mouseNow();
        installListener();
        if (ticker != null) ticker.stop();
        ticker = new javax.swing.Timer(5000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                tick(System.currentTimeMillis(), activeNow());
            }
        });
        ticker.setRepeats(true);
        ticker.start();
    }

    public static void reload() {
        sitOn = readBool("sitOn", false);
        waterOn = readBool("waterOn", false);
        sitMin = readInt("sitGap", 60);
        waterMin = readInt("waterGap", 45);
        if (sitMin < 15) sitMin = 15;
        if (sitMin > 180) sitMin = 180;
        if (waterMin < 10) waterMin = 10;
        if (waterMin > 180) waterMin = 180;
    }

    static void installListener() {
        if (listener != null) return;
        listener = new AWTEventListener() {
            public void eventDispatched(AWTEvent e) {
                if (e instanceof KeyEvent) {
                    int id = ((KeyEvent) e).getID();
                    if (id == KeyEvent.KEY_PRESSED || id == KeyEvent.KEY_RELEASED) {
                        lastActive = System.currentTimeMillis();
                    }
                } else if (e instanceof MouseEvent) {
                    int id = ((MouseEvent) e).getID();
                    if (id == MouseEvent.MOUSE_PRESSED || id == MouseEvent.MOUSE_CLICKED
                            || id == MouseEvent.MOUSE_WHEEL || id == MouseEvent.MOUSE_MOVED
                            || id == MouseEvent.MOUSE_DRAGGED) {
                        lastActive = System.currentTimeMillis();
                    }
                }
            }
        };
        try {
            Toolkit.getDefaultToolkit().addAWTEventListener(listener,
                    AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK
                            | AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.MOUSE_WHEEL_EVENT_MASK);
        } catch (Exception ignored) {
        }
    }

    static Point mouseNow() {
        try {
            return MouseInfo.getPointerInfo() == null ? null : MouseInfo.getPointerInfo().getLocation();
        } catch (Exception e) {
            return null;
        }
    }

    static boolean activeNow() {
        Point p = mouseNow();
        boolean moved = p != null && lastMouse != null && (p.x != lastMouse.x || p.y != lastMouse.y);
        lastMouse = p;
        return moved || (System.currentTimeMillis() - lastActive <= 4000);
    }

    /* ---------- 计时核心（now / active 可注入，测试用） ---------- */

    public static void tick(long now, boolean active) {
        String today = LocalDate.now().toString();
        if (!day.isEmpty() && !today.equals(day)) {
            day = today;
            sitMs = 0;
            waterMs = 0;
            sitCount = 0;
            waterCount = 0;
        }
        if (day.isEmpty()) day = today;

        if (active) lastActive = now;
        if (lastTick == 0) {
            lastTick = now;
            return;
        }
        long away = now - lastActive;
        long step = Math.max(0, now - lastTick);
        if (step > MAX_STEP) step = MAX_STEP;
        lastTick = now;

        if (away > IDLE_MS) {
            /* 人不在：计时清零，回来重新算，不刷次数 */
            sitMs = 0;
            waterMs = 0;
            return;
        }
        if (!active) return;

        if (sitOn) {
            sitMs += step;
            if (sitMs >= sitMin * 60000L) {
                sitMs = 0;
                sitCount++;
                saveToday();
                Alerts.fire("该起来动一动了", "已经连续坐了 " + sitMin + " 分钟，站起来走两步、伸个懒腰吧");
            }
        }
        if (waterOn) {
            waterMs += step;
            if (waterMs >= waterMin * 60000L) {
                waterMs = 0;
                waterCount++;
                saveToday();
                Alerts.fire("喝口水吧", "距离上次喝水 " + waterMin + " 分钟了，喝一杯水再接着干");
            }
        }
    }

    /** 手动重置两个计时（相当于"我刚起来过了 / 我刚喝过水"） */
    public static void resetAll() {
        sitMs = 0;
        waterMs = 0;
        lastTick = System.currentTimeMillis();
        lastActive = lastTick;
    }

    /* ---------- 今日数据 ---------- */

    public static void loadToday() {
        day = LocalDate.now().toString();
        sitCount = 0;
        waterCount = 0;
        for (String line : Store.readLines(FILE)) {
            String[] p = line.split("\t");
            if (p.length >= 3 && p[0].trim().equals(day)) {
                sitCount = parseInt(p[1]);
                waterCount = parseInt(p[2]);
                return;
            }
        }
    }

    public static void saveToday() {
        List<String> out = new ArrayList<String>();
        boolean found = false;
        for (String line : Store.readLines(FILE)) {
            String[] p = line.split("\t");
            if (p.length >= 3 && p[0].trim().equals(day)) {
                out.add(day + "\t" + sitCount + "\t" + waterCount);
                found = true;
            } else {
                out.add(line);
            }
        }
        if (!found) out.add(day + "\t" + sitCount + "\t" + waterCount);
        while (out.size() > KEEP_DAYS) out.remove(0);
        Store.writeLines(FILE, out);
    }

    /** 设置页上的今日数据行 */
    public static String todayText() {
        return "今日久坐提醒 " + sitCount + " 次 · 喝水提醒 " + waterCount + " 次";
    }

    /** 本轮进度行 */
    public static String roundText() {
        StringBuilder b = new StringBuilder("本轮");
        if (sitOn) {
            b.append("已连续坐 ").append(fmt(sitMs)).append(" / 每 ").append(sitMin).append(" 分钟提醒");
        } else {
            b.append("久坐提醒未开");
        }
        if (waterOn) {
            b.append("｜距上次喝水 ").append(fmt(waterMs)).append(" / 每 ").append(waterMin).append(" 分钟");
        }
        return b.toString();
    }

    static String fmt(long ms) {
        long m = ms / 60000L;
        if (m <= 0) return "0 分钟";
        return m + " 分钟";
    }

    /* ---------- 设置读写 ---------- */

    static boolean readBool(String key, boolean def) {
        for (String line : Store.readLines("settings.txt")) {
            String[] p = line.split("=", 2);
            if (p.length == 2 && p[0].trim().equals(key)) return p[1].trim().equals("1");
        }
        return def;
    }

    static int readInt(String key, int def) {
        for (String line : Store.readLines("settings.txt")) {
            String[] p = line.split("=", 2);
            if (p.length == 2 && p[0].trim().equals(key)) {
                try {
                    return Integer.parseInt(p[1].trim());
                } catch (NumberFormatException e) {
                    return def;
                }
            }
        }
        return def;
    }

    static void saveSetting(String key, String value) {
        List<String> out = new ArrayList<String>(Store.readLines("settings.txt"));
        boolean found = false;
        for (int i = 0; i < out.size(); i++) {
            String[] p = out.get(i).split("=", 2);
            if (p.length == 2 && p[0].trim().equals(key)) {
                out.set(i, key + "=" + value);
                found = true;
                break;
            }
        }
        if (!found) out.add(key + "=" + value);
        Store.writeLines("settings.txt", out);
    }

    static int parseInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
