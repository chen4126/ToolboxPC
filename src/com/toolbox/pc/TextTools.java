package com.toolbox.pc;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;

/** 文本小工具的逻辑部分：时间戳、JSON、Base64、URL、大小写。纯静态，方便断言。 */
public class TextTools {

    public static final String[] TOOLS = {"时间戳 / 日期", "JSON 格式化", "Base64", "URL 编解码", "大小写转换"};

    /* ---------- 时间戳 ---------- */

    /** 时间戳（秒或毫秒）转 "yyyy-MM-dd HH:mm:ss"；不像时间戳就返回提示 */
    public static String tsToDate(String raw) {
        String s = raw == null ? "" : raw.trim();
        if (s.isEmpty()) return "请输入 10 位秒级或 13 位毫秒级时间戳";
        if (!s.matches("-?\\d{1,14}")) return "这不是纯数字时间戳";
        long n = Long.parseLong(s);
        long ms = s.length() <= 10 ? n * 1000L : n;
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
        return f.format(new Date(ms));
    }

    /** "yyyy-MM-dd" 或 "yyyy-MM-dd HH:mm[:ss]" 转秒级 + 毫秒级时间戳 */
    public static String dateToTs(String raw) {
        String s = raw == null ? "" : raw.trim();
        if (s.isEmpty()) return "请输入日期，如 2026-11-12 14:30";
        Date d = parseDate(s);
        if (d == null) return "日期格式看不懂，试 2026-11-12 或 2026-11-12 14:30";
        long ms = d.getTime();
        return "秒: " + (ms / 1000) + "\n毫秒: " + ms;
    }

    static Date parseDate(String s) {
        String[] pats = {"yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd"};
        String[] vals = {s, s, s};
        // 允许用 "/" 或 "." 分隔
        s = s.replace('/', '-').replace('.', '-');
        vals[0] = s;
        vals[1] = s;
        vals[2] = s;
        for (int i = 0; i < pats.length; i++) {
            try {
                SimpleDateFormat f = new SimpleDateFormat(pats[i], Locale.CHINA);
                f.setLenient(false);
                return f.parse(vals[i]);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    /* ---------- JSON ---------- */

    public static String jsonPretty(String raw) {
        String s = raw == null ? "" : raw.trim();
        if (s.isEmpty()) return "请输入 JSON";
        StringBuilder out = new StringBuilder();
        boolean inStr = false, esc = false;
        int depth = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (inStr) {
                out.append(c);
                if (esc) esc = false;
                else if (c == '\\') esc = true;
                else if (c == '"') inStr = false;
                continue;
            }
            if (c == '"') {
                inStr = true;
                out.append(c);
                continue;
            }
            if (c == '{' || c == '[') {
                if (isNextCloser(s, i)) {
                    out.append(c);
                    continue;
                }
                depth++;
                out.append(c).append('\n').append(indent(depth));
                continue;
            }
            if (c == '}' || c == ']') {
                if (!isPrevOpener(s, i)) {
                    depth--;
                    if (depth < 0) depth = 0;
                    out.append('\n').append(indent(depth));
                }
                out.append(c);
                continue;
            }
            if (c == ',') {
                out.append(',').append('\n').append(indent(depth));
                continue;
            }
            if (c == ':') {
                out.append(": ");
                continue;
            }
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t') continue;
            out.append(c);
        }
        return out.toString();
    }

    private static boolean isNextCloser(String s, int i) {
        for (int j = i + 1; j < s.length(); j++) {
            char c = s.charAt(j);
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t') continue;
            return c == '}' || c == ']';
        }
        return false;
    }

    private static boolean isPrevOpener(String s, int i) {
        for (int j = i - 1; j >= 0; j--) {
            char c = s.charAt(j);
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t') continue;
            return c == '{' || c == '[';
        }
        return false;
    }

    private static String indent(int n) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < n; i++) b.append("  ");
        return b.toString();
    }

    public static String jsonMinify(String raw) {
        String s = raw == null ? "" : raw.trim();
        if (s.isEmpty()) return "请输入 JSON";
        StringBuilder out = new StringBuilder();
        boolean inStr = false, esc = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (inStr) {
                out.append(c);
                if (esc) esc = false;
                else if (c == '\\') esc = true;
                else if (c == '"') inStr = false;
                continue;
            }
            if (c == '"') {
                inStr = true;
                out.append(c);
                continue;
            }
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t') continue;
            out.append(c);
        }
        return out.toString();
    }

    /* ---------- Base64 ---------- */

    public static String b64Enc(String raw) {
        if (raw == null || raw.isEmpty()) return "请输入原文";
        try {
            return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return "编码失败: " + e.getMessage();
        }
    }

    public static String b64Dec(String raw) {
        String s = raw == null ? "" : raw.trim();
        if (s.isEmpty()) return "请输入 Base64 串";
        try {
            return new String(Base64.getDecoder().decode(s), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "不是合法的 Base64，解不开";
        }
    }

    /* ---------- URL ---------- */

    public static String urlEnc(String raw) {
        if (raw == null || raw.isEmpty()) return "请输入原文";
        try {
            return URLEncoder.encode(raw, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "编码失败: " + e.getMessage();
        }
    }

    public static String urlDec(String raw) {
        String s = raw == null ? "" : raw.trim();
        if (s.isEmpty()) return "请输入编码后的串";
        try {
            return URLDecoder.decode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "不是合法的 URL 编码串";
        }
    }

    /* ---------- 大小写 ---------- */

    public static String upper(String raw) {
        return raw == null ? "" : raw.toUpperCase(Locale.ROOT);
    }

    public static String lower(String raw) {
        return raw == null ? "" : raw.toLowerCase(Locale.ROOT);
    }

    /** "user name" / "user_name" / "user-name" → userName */
    public static String camel(String raw) {
        String s = normalize(raw);
        if (s.isEmpty()) return "";
        StringBuilder b = new StringBuilder();
        boolean up = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == ' ') {
                up = true;
                continue;
            }
            b.append(up ? Character.toUpperCase(c) : c);
            up = false;
        }
        return b.toString();
    }

    /** "userName" / "user name" → user_name */
    public static String snake(String raw) {
        if (raw == null) return "";
        String s = raw.trim().replaceAll("[\\s\\-]+", " ");
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == ' ') {
                b.append('_');
                continue;
            }
            if (Character.isUpperCase(c) && i > 0 && b.length() > 0
                    && b.charAt(b.length() - 1) != '_') {
                b.append('_');
            }
            b.append(Character.toLowerCase(c));
        }
        return b.toString();
    }

    private static String normalize(String raw) {
        if (raw == null) return "";
        String s = raw.trim().toLowerCase(Locale.ROOT);
        s = s.replace('_', ' ').replace('-', ' ');
        return s.trim();
    }
}
