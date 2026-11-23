package com.toolbox.pc;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 倒数日：记几个"距离某天还有多少天"的日子（考试、截止、生日）。
 * 数据落在 ToolboxPC/days.txt，一行一条：标题 \t 日期(yyyy-MM-dd)，最多保存 20 条。
 */
public class DaysLeft {

    static final String FILE = "days.txt";
    static final int MAX = 20;

    /** 读出全部条目，每项 {标题, 日期} */
    public static List<String[]> items() {
        List<String[]> out = new ArrayList<String[]>();
        for (String line : Store.readLines(FILE)) {
            String[] p = line.split("\t");
            if (p.length >= 2 && !p[0].trim().isEmpty() && isDate(p[1].trim())) {
                out.add(new String[]{p[0].trim(), p[1].trim()});
            }
        }
        return out;
    }

    /** 新增一条；标题空或日期不合法就返回 false */
    public static boolean add(String title, String date) {
        String t = title == null ? "" : title.trim();
        String d = date == null ? "" : date.trim();
        if (t.isEmpty() || !isDate(d)) return false;
        List<String[]> all = items();
        if (all.size() >= MAX) return false;
        for (String[] it : all) {
            if (it[0].equals(t) && it[1].equals(d)) return false;   // 重复不加
        }
        List<String> lines = new ArrayList<String>();
        for (String[] it : all) lines.add(it[0] + "\t" + it[1]);
        lines.add(t + "\t" + d);
        Store.writeLines(FILE, lines);
        return true;
    }

    public static void remove(int idx) {
        List<String[]> all = items();
        if (idx < 0 || idx >= all.size()) return;
        List<String> lines = new ArrayList<String>();
        for (int i = 0; i < all.size(); i++) {
            if (i == idx) continue;
            lines.add(all.get(i)[0] + "\t" + all.get(i)[1]);
        }
        Store.writeLines(FILE, lines);
    }

    /** 距离目标日还有几天：正数未到、0 是今天、负数是已过 */
    public static int daysFromToday(String date) {
        try {
            LocalDate d = LocalDate.parse(date.trim());
            return (int) ChronoUnit.DAYS.between(LocalDate.now(), d);
        } catch (Exception e) {
            return 0;
        }
    }

    /** 一条的显示文案：四级 · 还有 38 天 / 四级 · 就是今天 / 四级 · 已过 12 天 */
    public static String text(String title, String date) {
        int n = daysFromToday(date);
        if (n > 0) return title + " · 还有 " + n + " 天";
        if (n == 0) return title + " · 就是今天";
        return title + " · 已过 " + (-n) + " 天";
    }

    static boolean isDate(String s) {
        try {
            LocalDate.parse(s);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
