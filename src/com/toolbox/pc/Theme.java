package com.toolbox.pc;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

/** 主题管理：浅色/深色/跟随系统，统一取色入口 */
public class Theme {

    private static boolean dark;
    private static boolean auto;

    public static void init() {
        auto = readBool("autoDark", false);
        dark = resolveDark();
        if (dark) FlatDarkLaf.setup();
        else FlatLightLaf.setup();
    }

    /** 运行时切换主题并刷新整个窗口 */
    public static void apply(JFrame frame) {
        auto = readBool("autoDark", false);
        dark = resolveDark();
        if (dark) FlatDarkLaf.setup();
        else FlatLightLaf.setup();
        Ui.installDefaults();
        SwingUtilities.updateComponentTreeUI(frame);
        frame.repaint();
    }

    private static boolean resolveDark() {
        if (readBool("autoDark", false)) return systemDark();
        return readBool("dark", false);
    }

    public static boolean isAuto() {
        return auto;
    }

    /** 读取 Windows 应用主题（AppsUseLightTheme：0 深色 / 1 浅色） */
    public static boolean systemDark() {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"reg", "query",
                    "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                    "/v", "AppsUseLightTheme"});
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), "GBK"));
            String line;
            while ((line = r.readLine()) != null) {
                if (line.contains("AppsUseLightTheme")) {
                    String t = line.trim().toLowerCase();
                    if (t.endsWith("0x0")) return true;
                    if (t.endsWith("0x1")) return false;
                }
            }
            p.waitFor();
        } catch (Exception ignored) {
        }
        return false;
    }

    /** 跟随系统时，系统主题变了就重刷界面 */
    public static void watchSystem(JFrame frame) {
        final boolean[] last = {dark};
        javax.swing.Timer t = new javax.swing.Timer(8000, e -> {
            if (!readBool("autoDark", false)) {
                last[0] = dark;
                return;
            }
            boolean now = systemDark();
            if (now != last[0]) {
                last[0] = now;
                dark = now;
                if (dark) FlatDarkLaf.setup();
                else FlatLightLaf.setup();
                Ui.installDefaults();
                SwingUtilities.updateComponentTreeUI(frame);
                Main.rebuildNav();
                frame.repaint();
            }
        });
        t.start();
    }

    public static boolean isDark() {
        return dark;
    }

    /** 卡片/导航背景 */
    public static Color bg() {
        return dark ? new Color(0x23252F) : Color.WHITE;
    }

    /** 窗口底色 */
    public static Color panelBg() {
        return dark ? new Color(0x14161C) : new Color(0xF5F7FB);
    }

    /** 主文字 */
    public static Color text() {
        return dark ? new Color(0xE6E8F0) : new Color(0x1E293B);
    }

    /** 次要文字 */
    public static Color muted() {
        return dark ? new Color(0x9AA3B5) : new Color(0x64748B);
    }

    /** 卡片边框 */
    public static Color border() {
        return dark ? new Color(0x3A3F4B) : new Color(0xE8EDF5);
    }

    public static boolean readBool(String key, boolean def) {
        List<String> l = Store.readLines("settings.txt");
        for (String line : l) {
            String[] p = line.split("=", 2);
            if (p.length == 2 && p[0].trim().equals(key)) {
                return p[1].trim().equals("1");
            }
        }
        return def;
    }
}
