package com.toolbox.pc;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 文件整理助手：把某个文件夹顶层散落的文件按类型挪进子文件夹。
 * 只移动、不删除；遇到重名自动加 (1)；子文件夹和隐藏文件一律不碰。
 */
public class FileTidy {

    public static final String[] CATS = {"图片", "文档", "表格", "压缩包", "音视频", "代码", "其他"};

    /* 三种分档方式：按类型（老做法）、按月、按年 */
    public static final int MODE_TYPE = 0, MODE_MONTH = 1, MODE_YEAR = 2;
    public static final String[] MODE_NAMES = {"按类型", "按月", "按年"};

    static final String[] IMG = {"jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "tif", "tiff", "svg", "ico"};
    static final String[] DOC = {"doc", "docx", "pdf", "txt", "md", "rtf", "wps", "odt", "epub"};
    static final String[] SHEET = {"xls", "xlsx", "csv", "et", "ods"};
    static final String[] ZIP = {"zip", "rar", "7z", "tar", "gz", "bz2", "xz"};
    static final String[] MEDIA = {"mp4", "mkv", "avi", "mov", "wmv", "flv", "mp3", "wav", "flac", "aac", "m4a", "ogg"};
    static final String[] CODE = {"java", "py", "js", "ts", "html", "css", "json", "xml", "sql", "c", "cpp", "h", "go", "rs", "sh", "bat", "ps1"};

    /** 按扩展名给分类名 */
    public static String catOf(String fileName) {
        String ext = extOf(fileName);
        if (ext.isEmpty()) return "其他";
        if (in(ext, IMG)) return "图片";
        if (in(ext, DOC)) return "文档";
        if (in(ext, SHEET)) return "表格";
        if (in(ext, ZIP)) return "压缩包";
        if (in(ext, MEDIA)) return "音视频";
        if (in(ext, CODE)) return "代码";
        return "其他";
    }

    /** 修改时间分档：按月给 2026-11，按年给 2026 */
    public static String dateBucket(long lastModified, int mode) {
        java.text.SimpleDateFormat f = new java.text.SimpleDateFormat(
                mode == MODE_YEAR ? "yyyy" : "yyyy-MM", java.util.Locale.CHINA);
        return f.format(new java.util.Date(lastModified));
    }

    /** 这个文件该进哪个子目录（含空目录兜底：时间读不出来就归「未标注」） */
    public static String bucketOf(String fileName, long lastModified, int mode) {
        if (mode == MODE_MONTH || mode == MODE_YEAR) {
            if (lastModified <= 0) return "未标注";
            return dateBucket(lastModified, mode);
        }
        return catOf(fileName);
    }

    /** 列出可整理的文件，每项 {文件名, 目标子目录}；mode 决定按类型还是按日期分 */
    public static List<String[]> plan(File dir, int mode) {
        List<String[]> out = new ArrayList<String[]>();
        if (dir == null || !dir.isDirectory()) return out;
        File[] fs = dir.listFiles();
        if (fs == null) return out;
        Arrays.sort(fs);
        for (File f : fs) {
            if (f.isDirectory() || f.isHidden()) continue;
            out.add(new String[]{f.getName(), bucketOf(f.getName(), f.lastModified(), mode)});
        }
        return out;
    }

    /** 兼容老调用：按类型 */
    public static List<String[]> plan(File dir) {
        return plan(dir, MODE_TYPE);
    }

    /** 目标子目录 → 文件数，按目录名排好序 */
    public static java.util.Map<String, Integer> groupCounts(File dir, int mode) {
        java.util.TreeMap<String, Integer> m = new java.util.TreeMap<String, Integer>();
        for (String[] it : plan(dir, mode)) {
            Integer old = m.get(it[1]);
            m.put(it[1], old == null ? 1 : old + 1);
        }
        return m;
    }

    static String extOf(String name) {
        int i = name.lastIndexOf('.');
        if (i < 0 || i == name.length() - 1) return "";
        return name.substring(i + 1).toLowerCase();
    }

    static boolean in(String s, String[] arr) {
        return Arrays.asList(arr).contains(s);
    }

    /** 分类汇总：分类 → 数量（按类型口径，老接口保留） */
    public static int[] counts(File dir) {
        int[] c = new int[CATS.length];
        for (String[] it : plan(dir)) {
            int idx = Arrays.asList(CATS).indexOf(it[1]);
            if (idx < 0) idx = CATS.length - 1;
            c[idx]++;
        }
        return c;
    }

    /** 真正动手：移动到 目录/分类/ 下，返回成功移动的个数 */
    public static int run(File dir, int mode) {
        if (dir == null || !dir.isDirectory()) return 0;
        int moved = 0;
        for (String[] it : plan(dir, mode)) {
            File src = new File(dir, it[0]);
            File target = new File(dir, it[1]);
            if (!target.exists() && !target.mkdirs()) continue;
            File dst = new File(target, it[0]);
            if (dst.exists()) dst = unique(target, it[0]);
            try {
                if (src.renameTo(dst)) moved++;
            } catch (Exception ignored) {
            }
        }
        return moved;
    }

    /** 兼容老调用：按类型 */
    public static int run(File dir) {
        return run(dir, MODE_TYPE);
    }

    /** 重名时生成 "名字(1).后缀" */
    public static File unique(File dir, String name) {
        String base = name, ext = "";
        int i = name.lastIndexOf('.');
        if (i > 0) {
            base = name.substring(0, i);
            ext = name.substring(i);
        }
        for (int n = 1; n < 1000; n++) {
            File f = new File(dir, base + "(" + n + ")" + ext);
            if (!f.exists()) return f;
        }
        return new File(dir, base + "(999)" + ext);
    }
}
