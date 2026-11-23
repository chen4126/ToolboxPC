package com.toolbox.pc;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 剪贴板速记：每秒看一眼系统剪贴板，文本一变就记一条，回头能翻能复制。
 * 数据落在 ToolboxPC/clip.txt，一行一条：MM-dd HH:mm \t 文本（换行写成 \n 两个字符）。
 * 标记为「常用」的行，时间前面加一个 ★，这些条目不会被条数上限挤掉。
 */
public class ClipNotes {

    static final String FILE = "clip.txt";
    public static final int MAX = 150;
    static final int MIN_LEN = 2;
    public static final String STAR = "★";

    static String last = "";
    static javax.swing.Timer poller;
    static String latestSkip = "";

    public static void start() {
        last = readClipboard();
        if (poller != null) poller.stop();
        poller = new javax.swing.Timer(1000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                poll();
            }
        });
        poller.setRepeats(true);
        poller.start();
    }

    /** 每秒看一眼，变了就记 */
    static void poll() {
        String now = readClipboard();
        if (now == null) return;
        if (now.equals(last)) return;
        last = now;
        String t = now.trim();
        if (t.isEmpty() || t.length() < MIN_LEN) return;
        if (t.equals(latestSkip)) return;      // 自己刚复制出去的，不再记一遍
        add(t);
    }

    static String readClipboard() {
        try {
            Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
            if (cb.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                Object o = cb.getData(DataFlavor.stringFlavor);
                return o == null ? null : o.toString();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /** 手动把当前剪贴板存一条 */
    public static boolean grabNow() {
        String now = readClipboard();
        if (now == null || now.trim().isEmpty()) return false;
        latestSkip = now.trim();
        last = now;
        add(now.trim());
        return true;
    }

    public static void add(String text) {
        String line = esc(text.trim());
        if (line.isEmpty()) return;
        List<String> out = new ArrayList<String>(Store.readLines(FILE));
        /* 刚刚记过的同一个内容不重复塞 */
        if (!out.isEmpty()) {
            String lastLine = out.get(out.size() - 1);
            String[] p = lastLine.split("\t", 2);
            if (p.length == 2 && p[1].equals(line)) return;
        }
        String time = new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(new Date());
        out.add(time + "\t" + line);
        trimToMax(out);
        Store.writeLines(FILE, out);
    }

    /** 超上限时先扔最旧的「没标常用」那条，常用的一条都不动；全标了才扔最旧的 */
    static void trimToMax(List<String> out) {
        while (out.size() > MAX) {
            int victim = -1;
            for (int i = 0; i < out.size(); i++) {
                String[] p = out.get(i).split("\t", 2);
                if (p.length < 2) {
                    victim = i;
                    break;
                }
                if (!p[0].startsWith(STAR)) {
                    victim = i;
                    break;
                }
            }
            if (victim < 0) victim = 0;
            out.remove(victim);
        }
    }

    /** 新的排前面，每项 {时间, 文本, 是否常用("1"/"0")} */
    public static List<String[]> items() {
        List<String[]> out = new ArrayList<String[]>();
        List<String> lines = Store.readLines(FILE);
        for (int i = lines.size() - 1; i >= 0; i--) {
            String[] p = lines.get(i).split("\t", 2);
            if (p.length < 2) continue;
            boolean pin = p[0].startsWith(STAR);
            out.add(new String[]{pin ? p[0].substring(1) : p[0], unesc(p[1]), pin ? "1" : "0"});
        }
        return out;
    }

    /** 关键词搜索：时间或正文里含这个词就行，空词等于全都要 */
    public static List<String[]> search(String kw) {
        List<String[]> all = items();
        String k = kw == null ? "" : kw.trim().toLowerCase();
        if (k.isEmpty()) return all;
        List<String[]> out = new ArrayList<String[]>();
        for (String[] it : all) {
            if (it[0].toLowerCase().contains(k) || it[1].toLowerCase().contains(k)) out.add(it);
        }
        return out;
    }

    /** 常用条目单独看 */
    public static List<String[]> pinned() {
        List<String[]> out = new ArrayList<String[]>();
        for (String[] it : items()) {
            if ("1".equals(it[2])) out.add(it);
        }
        return out;
    }

    public static int pinnedCount() {
        return pinned().size();
    }

    public static void remove(int idx) {
        List<String[]> all = items();
        if (idx < 0 || idx >= all.size()) return;
        String[] target = all.get(idx);
        removeBy(target[0], target[1]);
    }

    /** 按「时间 + 正文」删，搜索结果里删也不会错行 */
    public static boolean removeBy(String time, String text) {
        List<String> src = Store.readLines(FILE);
        List<String> out = new ArrayList<String>();
        boolean removed = false;
        for (String line : src) {
            String[] p = line.split("\t", 2);
            if (!removed && p.length == 2 && sameTime(p[0], time) && p[1].equals(esc(text))) {
                removed = true;
                continue;
            }
            out.add(line);
        }
        if (removed) Store.writeLines(FILE, out);
        return removed;
    }

    /** 点一下星标：常用 ↔ 普通。返回改完之后是不是常用 */
    public static boolean togglePin(String time, String text) {
        List<String> src = Store.readLines(FILE);
        boolean now = false;
        boolean hit = false;
        for (int i = 0; i < src.size(); i++) {
            String[] p = src.get(i).split("\t", 2);
            if (!hit && p.length == 2 && sameTime(p[0], time) && p[1].equals(esc(text))) {
                boolean pin = p[0].startsWith(STAR);
                now = !pin;
                src.set(i, (now ? STAR : "") + time + "\t" + p[1]);
                hit = true;
            }
        }
        if (hit) Store.writeLines(FILE, src);
        return now;
    }

    static boolean sameTime(String field, String time) {
        String t = field.startsWith(STAR) ? field.substring(STAR.length()) : field;
        return t.equals(time);
    }

    public static void clear() {
        Store.writeLines(FILE, new ArrayList<String>());
    }

    /** 清空，但保留标了常用的那些 */
    public static int clearUnpinned() {
        List<String> src = Store.readLines(FILE);
        List<String> keep = new ArrayList<String>();
        for (String line : src) {
            String[] p = line.split("\t", 2);
            if (p.length == 2 && p[0].startsWith(STAR)) keep.add(line);
        }
        Store.writeLines(FILE, keep);
        return src.size() - keep.size();
    }

    public static int size() {
        return Store.readLines(FILE).size();
    }

    /** 复制回剪贴板，同时记下"这条是我自己放出去的"，避免又被记一遍 */
    public static void copyBack(String text) {
        latestSkip = text.trim();
        last = text;
        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new java.awt.datatransfer.StringSelection(text), null);
    }

    public static String esc(String s) {
        return s.replace("\r\n", "\n").replace("\n", "\\n");
    }

    public static String unesc(String s) {
        return s.replace("\\n", "\n");
    }
}
