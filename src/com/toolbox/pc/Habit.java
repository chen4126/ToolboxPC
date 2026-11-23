package com.toolbox.pc;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 习惯打卡：维护几个每天要做的小习惯，记录每天是否完成。
 * habits.txt 一行一个习惯名；habit_log.txt 一行一次打卡：yyyy-MM-dd \t 习惯名。
 */
public class Habit {

    static final String FILE = "habit.txt";
    static final String LOG = "habit_log.txt";
    public static final int MAX_HABITS = 8;
    public static final int KEEP_DAYS = 400;

    /* ---------- 习惯清单 ---------- */

    public static List<String> habits() {
        List<String> out = new ArrayList<String>();
        for (String line : Store.readLines(FILE)) {
            String t = line.trim();
            if (!t.isEmpty() && !out.contains(t)) out.add(t);
        }
        return out;
    }

    public static boolean add(String name) {
        String t = name == null ? "" : name.trim();
        if (t.isEmpty() || t.length() > 14) return false;
        List<String> all = habits();
        if (all.size() >= MAX_HABITS) return false;
        if (all.contains(t)) return false;
        all.add(t);
        Store.writeLines(FILE, all);
        return true;
    }

    public static void rename(int idx, String newName) {
        List<String> all = habits();
        if (idx < 0 || idx >= all.size()) return;
        String t = newName == null ? "" : newName.trim();
        if (t.isEmpty() || t.length() > 14 || all.contains(t)) return;
        String old = all.get(idx);
        all.set(idx, t);
        Store.writeLines(FILE, all);
        /* 记录里跟着改名 */
        List<String> logs = Store.readLines(LOG);
        List<String> out = new ArrayList<String>();
        for (String l : logs) {
            String[] p = l.split("\t");
            if (p.length >= 2 && p[1].equals(old)) out.add(p[0] + "\t" + t);
            else out.add(l);
        }
        Store.writeLines(LOG, out);
    }

    public static void remove(int idx) {
        List<String> all = habits();
        if (idx < 0 || idx >= all.size()) return;
        String name = all.get(idx);
        all.remove(idx);
        Store.writeLines(FILE, all);
        List<String> out = new ArrayList<String>();
        for (String l : Store.readLines(LOG)) {
            String[] p = l.split("\t");
            if (p.length >= 2 && p[1].equals(name)) continue;
            out.add(l);
        }
        Store.writeLines(LOG, out);
    }

    /* ---------- 打卡 ---------- */

    static List<String[]> logs() {
        List<String[]> out = new ArrayList<String[]>();
        for (String line : Store.readLines(LOG)) {
            String[] p = line.split("\t");
            if (p.length >= 2 && !p[0].trim().isEmpty()) out.add(new String[]{p[0].trim(), p[1].trim()});
        }
        return out;
    }

    public static boolean done(String habit, LocalDate day) {
        String ds = day.toString();
        for (String[] l : logs()) {
            if (l[0].equals(ds) && l[1].equals(habit)) return true;
        }
        return false;
    }

    /** 打卡 / 取消打卡，返回当天是否已是完成状态 */
    public static boolean toggle(String habit, LocalDate day) {
        if (done(habit, day)) {
            List<String> out = new ArrayList<String>();
            for (String l : Store.readLines(LOG)) {
                String[] p = l.split("\t");
                if (p.length >= 2 && p[0].trim().equals(day.toString()) && p[1].trim().equals(habit)) continue;
                out.add(l);
            }
            Store.writeLines(LOG, out);
            return false;
        }
        List<String> out = new ArrayList<String>(Store.readLines(LOG));
        out.add(day + "\t" + habit);
        while (out.size() > KEEP_DAYS) out.remove(0);
        Store.writeLines(LOG, out);
        return true;
    }

    /** 连续天数：今天没打就从昨天往前数，断了就停 */
    public static int streak(String habit) {
        LocalDate d = LocalDate.now();
        if (!done(habit, d)) d = d.minusDays(1);
        int n = 0;
        while (done(habit, d)) {
            n++;
            d = d.minusDays(1);
        }
        return n;
    }

    /** 本月已完成几天 */
    public static int doneThisMonth(String habit) {
        LocalDate today = LocalDate.now();
        LocalDate first = today.withDayOfMonth(1);
        int n = 0;
        for (LocalDate d = first; !d.isAfter(today); d = d.plusDays(1)) {
            if (done(habit, d)) n++;
        }
        return n;
    }

    /** 本月到目前为止过了几天（当月 1 号到今天） */
    public static int daysSoFarThisMonth() {
        return LocalDate.now().getDayOfMonth();
    }

    /** 本周某一天（周一为 0） */
    public static LocalDate weekDay(int idx) {
        LocalDate today = LocalDate.now();
        LocalDate monday = today.minusDays(today.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue());
        return monday.plusDays(idx);
    }

    /** 今日概览：今天该打几个、已打几个 */
    public static String todayText() {
        List<String> all = habits();
        int doneN = 0;
        for (String h : all) {
            if (done(h, LocalDate.now())) doneN++;
        }
        return "今天 " + doneN + " / " + all.size() + " 已完成";
    }

    /* ---------- 连续里程碑 ---------- */

    static final String MILE = "habit_mile.txt";
    /** 这几个天数各庆祝一次，之后不再重复弹 */
    public static final int[] MARKS = {7, 30, 100};

    static boolean celebrated(String habit, int days) {
        for (String line : Store.readLines(MILE)) {
            String[] p = line.split("\t");
            if (p.length >= 2 && p[0].trim().equals(String.valueOf(days)) && p[1].trim().equals(habit)) return true;
        }
        return false;
    }

    /** 刚打完卡时问一下：是否正好踩到 7/30/100 天且还没庆祝过，返回天数或 0 */
    public static int milestoneHit(String habit) {
        int st = streak(habit);
        for (int m : MARKS) {
            if (st == m && !celebrated(habit, m)) return m;
        }
        return 0;
    }

    public static void markMilestone(String habit, int days) {
        if (celebrated(habit, days)) return;
        List<String> out = new ArrayList<String>(Store.readLines(MILE));
        out.add(days + "\t" + habit);
        Store.writeLines(MILE, out);
    }
}
