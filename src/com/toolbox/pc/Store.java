package com.toolbox.pc;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/** 数据文件存取：统一放在 用户目录/ToolboxPC/ 下，UTF-8 文本，每行一条记录，TAB 分隔字段 */
public class Store {

    private static File dirCache;
    private static String dirCacheKey;

    /** 数据目录：优先 -Dtoolbox.data.dir，其次环境变量 TOOLBOX_DATA，都没配就用 用户目录/ToolboxPC
     *  （后两者是给自动化测试和打包验证用的，正常使用永远落在 用户目录/ToolboxPC） */
    public static File dir() {
        String custom = System.getProperty("toolbox.data.dir");
        if (custom == null || custom.trim().isEmpty()) custom = System.getenv("TOOLBOX_DATA");
        String key = custom == null ? "" : custom.trim();
        if (dirCache != null && key.equals(dirCacheKey)) return dirCache;
        File d = key.isEmpty() ? new File(System.getProperty("user.home"), "ToolboxPC") : new File(key);
        if (!d.exists()) d.mkdirs();
        dirCache = d;
        dirCacheKey = key;
        return d;
    }

    public static List<String> readLines(String name) {
        File f = new File(dir(), name);
        /* settings.txt 万一被删掉或覆盖成空白，用自动副本拉回来，尽量不让个性化设置丢 */
        if ("settings.txt".equals(name) && !f.exists()) {
            File bak = new File(dir(), "settings.bak");
            if (bak.exists()) {
                try {
                    Files.copy(bak.toPath(), f.toPath());
                } catch (Exception ignored) {
                }
            }
        }
        try {
            if (!f.exists()) return new ArrayList<String>();
            return Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return new ArrayList<String>();
        }
    }

    public static void writeLines(String name, List<String> lines) {
        File f = new File(dir(), name);
        try {
            Files.write(f.toPath(), lines, StandardCharsets.UTF_8);
            /* 设置每写一次就同步一份副本，回头被误删/重装覆盖也能一键拉回 */
            if ("settings.txt".equals(name)) {
                Files.write(new File(dir(), "settings.bak").toPath(), lines, StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {
        }
    }

    public static String readText(String name) {
        List<String> l = readLines(name);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < l.size(); i++) {
            if (i > 0) sb.append("\n");
            sb.append(l.get(i));
        }
        return sb.toString();
    }

    public static void writeText(String name, String text) {
        File f = new File(dir(), name);
        try {
            Files.write(f.toPath(), text.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
    }

    /** 追加一行记录（用于番茄钟/倒计时的历史日志） */
    public static void appendLine(String name, String line) {
        List<String> l = readLines(name);
        l.add(line);
        writeLines(name, l);
    }

    /** 读 settings.txt 里的开关键（值 1/0），没有这个键时返回默认值 */
    public static boolean flag(String key, boolean def) {
        for (String line : readLines("settings.txt")) {
            String[] p = line.split("=", 2);
            if (p.length == 2 && p[0].trim().equals(key)) return "1".equals(p[1].trim());
        }
        return def;
    }

    /** 写 settings.txt 里的开关键：有就改、没有就追加，不碰其它键 */
    public static void setFlag(String key, boolean value) {
        List<String> out = new ArrayList<String>(readLines("settings.txt"));
        boolean found = false;
        for (int i = 0; i < out.size(); i++) {
            String[] p = out.get(i).split("=", 2);
            if (p.length == 2 && p[0].trim().equals(key)) {
                out.set(i, key + "=" + (value ? "1" : "0"));
                found = true;
                break;
            }
        }
        if (!found) out.add(key + "=" + (value ? "1" : "0"));
        writeLines("settings.txt", out);
    }

    /** 读 settings.txt 里的字符串键，没有就返回默认值 */
    public static String str(String key, String def) {
        for (String line : readLines("settings.txt")) {
            String[] p = line.split("=", 2);
            if (p.length == 2 && p[0].trim().equals(key)) return p[1].trim();
        }
        return def;
    }

    /** 把 todo.txt 里第一条未完成的、文本匹配的待办标记为已完成 */
    public static boolean markTodoDone(String text) {
        List<String> l = readLines("todo.txt");
        boolean done = false;
        for (int i = 0; i < l.size(); i++) {
            String[] p = l.get(i).split("\t", -1);
            if (p.length < 2) continue;
            if ("1".equals(p[0].trim())) continue;
            if (!p[1].trim().equals(text)) continue;
            l.set(i, "1\t" + p[1] + (p.length > 2 ? "\t" + p[2] : ""));
            done = true;
            break;
        }
        if (done) writeLines("todo.txt", l);
        return done;
    }

    /** 把已完成、文本匹配的待办改回未完成（误点"完成"可以撤回） */
    public static boolean markTodoUndone(String text) {
        List<String> l = readLines("todo.txt");
        boolean ok = false;
        for (int i = 0; i < l.size(); i++) {
            String[] p = l.get(i).split("\t", -1);
            if (p.length < 2) continue;
            if (!"1".equals(p[0].trim())) continue;
            if (!p[1].trim().equals(text)) continue;
            l.set(i, "0\t" + p[1] + (p.length > 2 ? "\t" + p[2] : ""));
            ok = true;
            break;
        }
        if (ok) writeLines("todo.txt", l);
        return ok;
    }

    /** 把一批已完成、文本匹配的待办一次改回未完成，返回改成功的条数 */
    public static int markTodosUndone(java.util.List<String> texts) {
        if (texts == null || texts.isEmpty()) return 0;
        List<String> l = readLines("todo.txt");
        int n = 0;
        for (int i = 0; i < l.size(); i++) {
            String[] p = l.get(i).split("\t", -1);
            if (p.length < 2) continue;
            if (!"1".equals(p[0].trim())) continue;
            if (!texts.contains(p[1].trim())) continue;
            l.set(i, "0\t" + p[1] + (p.length > 2 ? "\t" + p[2] : ""));
            n++;
        }
        if (n > 0) writeLines("todo.txt", l);
        return n;
    }

    /** 把已完成、文本匹配的待办改回未完成，同时把日期改成 newDate */
    public static boolean markTodoUndoneAndMove(String text, String newDate) {
        List<String> l = readLines("todo.txt");
        boolean ok = false;
        for (int i = 0; i < l.size(); i++) {
            String[] p = l.get(i).split("\t", -1);
            if (p.length < 2) continue;
            if (!"1".equals(p[0].trim())) continue;
            if (!p[1].trim().equals(text)) continue;
            l.set(i, "0\t" + p[1] + "\t" + newDate);
            ok = true;
            break;
        }
        if (ok) writeLines("todo.txt", l);
        return ok;
    }

    /** 改掉第一条未完成、文本匹配的待办的日期 */
    public static boolean updateTodoDate(String text, String newDate) {
        List<String> l = readLines("todo.txt");
        boolean ok = false;
        for (int i = 0; i < l.size(); i++) {
            String[] p = l.get(i).split("\t", -1);
            if (p.length < 2) continue;
            if ("1".equals(p[0].trim())) continue;
            if (!p[1].trim().equals(text)) continue;
            l.set(i, p[0] + "\t" + p[1] + "\t" + newDate);
            ok = true;
            break;
        }
        if (ok) writeLines("todo.txt", l);
        return ok;
    }

    /** 删掉第一条未完成、文本匹配的待办 */
    public static boolean deleteTodo(String text) {
        List<String> l = readLines("todo.txt");
        int hit = -1;
        for (int i = 0; i < l.size(); i++) {
            String[] p = l.get(i).split("\t", -1);
            if (p.length < 2) continue;
            if ("1".equals(p[0].trim())) continue;
            if (!p[1].trim().equals(text)) continue;
            hit = i;
            break;
        }
        if (hit < 0) return false;
        l.remove(hit);
        writeLines("todo.txt", l);
        return true;
    }
}
