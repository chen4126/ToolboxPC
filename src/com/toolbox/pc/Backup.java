package com.toolbox.pc;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 数据自动备份：每天第一次启动留一份当日快照，最多保留 7 份；设置页可手动备份 / 回滚 */
public class Backup {

    /** 备份根目录：数据目录下的 backups，和业务文件分开，备份里不会再套备份 */
    public static File root() {
        File d = new File(Store.dir(), "backups");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    private static boolean isDataFile(File f) {
        if (f == null || !f.isFile()) return false;
        String n = f.getName();
        if (!n.endsWith(".txt")) return false;
        if ("settings.bak".equals(n)) return false;   // 它本身是副本，不用再备份
        if (n.startsWith(".")) return false;
        return true;
    }

    /* ---------- 备份范围：想让哪几个模块进快照，自己勾 ---------- */

    /** key、显示名、该组包含的文件（顺序即界面顺序） */
    public static final String[][] GROUPS = {
            {"ledger", "记账本", "ledger.txt", "ledgermap.txt"},
            {"memo", "备忘录", "memo.txt"},
            {"plan", "日程与待办", "schedule.txt", "todo.txt"},
            {"habit", "习惯与健康", "habit.txt", "health.txt"},
            {"pomo", "番茄与倒计时", "pomodoro.txt", "pomodoro_log.txt", "countdown_log.txt", "countdown_presets.txt"},
            {"other", "计数器与用眼", "counter.txt", "eye.txt"},
    };

    /** settings.txt 里存勾选结果的键，值形如 ledger,memo（空或没有这个键 = 全量） */
    public static final String PICK_KEY = "backupPick";

    /** 某个文件属于哪一组；没登记的归到 other */
    public static String groupOf(String fileName) {
        for (String[] g : GROUPS) {
            for (int i = 2; i < g.length; i++) {
                if (g[i].equals(fileName)) return g[0];
            }
        }
        return "other";
    }

    /** 当前勾了哪几组；没设置过（或设置里全是非法值）就是全量，明确"一个都不勾"时返回空 */
    public static List<String> pick() {
        String v = Store.str(PICK_KEY, "").trim();
        if ("none".equals(v)) return new ArrayList<String>();
        List<String> wanted = new ArrayList<String>();
        if (!v.isEmpty()) {
            for (String s : v.split(",")) {
                String k = s.trim();
                if (!k.isEmpty() && hasGroup(k) && !wanted.contains(k)) wanted.add(k);
            }
        }
        List<String> out = new ArrayList<String>();
        if (!wanted.isEmpty()) {
            for (String[] g : GROUPS) if (wanted.contains(g[0])) out.add(g[0]);
            return out;
        }
        for (String[] g : GROUPS) out.add(g[0]);
        return out;
    }

    private static boolean hasGroup(String key) {
        for (String[] g : GROUPS) if (g[0].equals(key)) return true;
        return false;
    }

    /** 保存勾选结果；全选写成空键（等于默认全量），一个都不勾写 none */
    public static void setPick(List<String> keys) {
        List<String> valid = new ArrayList<String>();
        for (String[] g : GROUPS) if (keys.contains(g[0])) valid.add(g[0]);
        String v;
        if (valid.isEmpty()) v = "none";
        else if (valid.size() >= GROUPS.length) v = "";
        else v = join(valid);
        Store.str(PICK_KEY, v);   // 读一下保证 settings.txt 已存在
        List<String> lines = new ArrayList<String>(Store.readLines("settings.txt"));
        boolean found = false;
        for (int i = 0; i < lines.size(); i++) {
            String[] p = lines.get(i).split("=", 2);
            if (p.length == 2 && p[0].trim().equals(PICK_KEY)) {
                lines.set(i, PICK_KEY + "=" + v);
                found = true;
                break;
            }
        }
        if (!found) lines.add(PICK_KEY + "=" + v);
        Store.writeLines("settings.txt", lines);
    }

    private static String join(List<String> keys) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keys.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(keys.get(i));
        }
        return sb.toString();
    }

    /** 该组现在算不算在备份范围里 */
    public static boolean picked(String key) {
        return pick().contains(key);
    }

    /** 一句话说明备份范围，给界面提示用 */
    public static String pickText() {
        List<String> keys = pick();
        if (keys.size() >= GROUPS.length) return "全部模块";
        StringBuilder sb = new StringBuilder();
        for (String[] g : GROUPS) {
            if (!keys.contains(g[0])) continue;
            if (sb.length() > 0) sb.append("、");
            sb.append(g[1]);
        }
        return sb.length() == 0 ? "什么都没勾（不会备份）" : sb.toString();
    }

    /** 数据目录里这一组现在有几个文件（界面显示用） */
    public static int countIn(String key) {
        int n = 0;
        File[] fs = Store.dir().listFiles();
        if (fs == null) return 0;
        for (File f : fs) {
            if (!isDataFile(f)) continue;
            if (key.equals(groupOf(f.getName()))) n++;
        }
        return n;
    }

    /** 把数据目录里（且在备份范围内的）*.txt 复制进 dst，返回复制的文件数 */
    public static int copyAll(File dst) {
        if (!dst.exists()) dst.mkdirs();
        int n = 0;
        File[] fs = Store.dir().listFiles();
        if (fs == null) return 0;
        for (File f : fs) {
            if (!isDataFile(f)) continue;
            if (!picked(groupOf(f.getName()))) continue;
            try {
                Files.copy(f.toPath(), new File(dst, f.getName()).toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
                n++;
            } catch (Exception ignored) {
            }
        }
        return n;
    }

    /** 今天是否已经留过当日快照 */
    public static boolean hasToday() {
        return new File(root(), LocalDate.now().toString()).exists();
    }

    /** 每天第一份快照（启动时调用）：已经有就不再重复写，数据中心里没文件也不留空目录 */
    public static boolean daily() {
        File d = new File(root(), LocalDate.now().toString());
        if (d.exists()) return false;
        File tmp = new File(root(), "." + LocalDate.now() + ".tmp");
        deleteDir(tmp);
        int n = copyAll(tmp);
        if (n == 0) {
            deleteDir(tmp);
            dropRootIfEmpty();
            return false;
        }
        if (!tmp.renameTo(d)) {
            deleteDir(tmp);
            return false;
        }
        prune();
        return true;
    }

    /** 手动备份一份，名字带时分秒；返回目录名，失败返回 null */
    public static String manual() {
        String name = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss"));
        File d = new File(root(), name);
        int n = copyAll(d);
        if (n == 0) {
            deleteDir(d);
            dropRootIfEmpty();
            return null;
        }
        prune();
        return name;
    }

    /** 数据中心里一个文件都没有时，别留下空的 backups 目录 */
    static void dropRootIfEmpty() {
        File r = root();
        String[] kids = r.list();
        if (kids != null && kids.length == 0) r.delete();
    }

    /** 现有备份，最新的排前面；pre-restore（回滚前自动留的）排在最后，免得占用"最近一份"的位置 */
    public static List<String> list() {
        List<String> out = new ArrayList<String>();
        File[] fs = root().listFiles();
        if (fs == null) return out;
        for (File f : fs) {
            if (f.isDirectory() && !f.getName().startsWith(".")) out.add(f.getName());
        }
        Collections.sort(out, new java.util.Comparator<String>() {
            public int compare(String x, String y) {
                boolean px = x.startsWith("pre-restore-"), py = y.startsWith("pre-restore-");
                if (px != py) return px ? 1 : -1;
                return y.compareTo(x);
            }
        });
        return out;
    }

    /** 一份备份里有多少个文件、多大，用于列表展示 */
    public static String describe(String name) {
        File d = new File(root(), name);
        if (!d.exists()) return "已不存在";
        int n = 0;
        long bytes = 0;
        File[] fs = d.listFiles();
        if (fs != null) {
            for (File f : fs) {
                if (!f.isFile()) continue;
                n++;
                bytes += f.length();
            }
        }
        return n + " 个文件 · " + (bytes < 1024 ? (bytes + " B") : String.format(java.util.Locale.CHINA, "%.1f KB", bytes / 1024.0));
    }

    /** 回滚到某份备份：先把当前状态另存一份，再把备份文件覆盖回去。返回恢复的文件数，备份不存在返回 -1 */
    public static int restore(String name) {
        File d = new File(root(), name);
        if (!d.exists()) return -1;
        /* 回滚本身也可能后悔，先把现状留一份 */
        String preName = "pre-restore-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss"));
        copyAll(new File(root(), preName));
        int n = 0;
        File[] fs = d.listFiles();
        if (fs == null) return 0;
        for (File f : fs) {
            if (!f.isFile()) continue;
            try {
                Files.copy(f.toPath(), new File(Store.dir(), f.getName()).toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
                n++;
            } catch (Exception ignored) {
            }
        }
        /* 恢复 settings.txt 之后，它的自动副本也要跟上 */
        if (new File(Store.dir(), "settings.txt").exists()) {
            try {
                Files.copy(new File(Store.dir(), "settings.txt").toPath(),
                        new File(Store.dir(), "settings.bak").toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception ignored) {
            }
        }
        prune();
        return n;
    }

    /** 只保留最近 10 份，更早的删掉（只删自己建的目录，不动别的文件） */
    public static void prune() {
        List<String> all = list();
        if (all.size() <= 10) return;
        for (int i = 10; i < all.size(); i++) {
            deleteDir(new File(root(), all.get(i)));
        }
    }

    static void deleteDir(File d) {
        if (d == null || !d.exists()) return;
        File[] fs = d.listFiles();
        if (fs != null) {
            for (File f : fs) {
                if (f.isDirectory()) deleteDir(f);
                f.delete();
            }
        }
        d.delete();
    }
}
