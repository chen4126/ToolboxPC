package com.toolbox.pc;

import javax.swing.*;
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
 * 护眼监控：按鼠标/键盘活跃度累计连续用眼时间，到点弹出休息提示，记录每天用眼数据。
 * 数据落在 ToolboxPC/eye.txt，一行一天：日期 \t 用眼毫秒 \t 休息次数 \t 最长连续毫秒。
 * 纯 Java 实现（MouseInfo + AWT 全局事件），不依赖系统 API。
 */
public class EyeCare {

    static final String FILE = "eye.txt";
    /** 连续这么久没有任何输入，视为人不在，暂停计时 */
    static final long IDLE_MS = 5L * 60 * 1000;
    /** 单次 tick 最多按这么久算，防止休眠跳变把时长灌爆 */
    static final long MAX_STEP = 15L * 1000;
    /** 提醒窗口自己关掉后，多久再提醒一次 */
    static final int AUTO_SNOOZE_MIN = 10;
    static final int KEEP_DAYS = 60;

    /* ---------- 配置（来自 settings.txt） ---------- */
    static boolean on = true;
    static int gapMin = 40;
    static int restSec = 20;

    /* ---------- 运行态 ---------- */
    static String day = "";
    static long eyeMs = 0;       // 本轮连续用眼
    static long todayMs = 0;     // 今日累计
    static int restCount = 0;    // 今日休息次数
    static long peakMs = 0;      // 今日最长连续
    static long lastTick = 0;
    static long lastActive = 0;
    static long snoozeUntil = 0;
    static boolean resting = false;
    static boolean reminding = false;
    static long lastSave = 0;

    static Point lastMouse = null;
    static javax.swing.Timer ticker;
    static javax.swing.Timer restTicker;
    static JWindow restWin;
    static JLabel countLabel = null;
    static AWTEventListener listener;

    /* ---------- 生命周期 ---------- */

    public static void start() {
        reload();
        if (ticker != null) ticker.stop();
        lastTick = System.currentTimeMillis();
        lastActive = lastTick;
        lastMouse = mouseNow();
        installListener();
        ticker = new javax.swing.Timer(5000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                tick(System.currentTimeMillis(), activeNow());
            }
        });
        ticker.setRepeats(true);
        ticker.start();
    }

    /** 重新读设置（设置页改完调用） */
    static void reload() {
        on = readBool("eyeOn", true);
        gapMin = readInt("eyeGap", 40);
        restSec = readInt("eyeRest", 20);
        if (gapMin < 5) gapMin = 5;
        if (gapMin > 180) gapMin = 180;
        if (restSec < 10) restSec = 10;
        if (restSec > 180) restSec = 180;
    }

    static void installListener() {
        if (listener != null) return;
        listener = new AWTEventListener() {
            public void eventDispatched(AWTEvent e) {
                if (e instanceof KeyEvent) {
                    int id = ((KeyEvent) e).getID();
                    if (id == KeyEvent.KEY_PRESSED || id == KeyEvent.KEY_RELEASED) lastActive = System.currentTimeMillis();
                } else if (e instanceof MouseEvent) {
                    int id = ((MouseEvent) e).getID();
                    if (id == MouseEvent.MOUSE_PRESSED || id == MouseEvent.MOUSE_CLICKED
                            || id == MouseEvent.MOUSE_WHEEL || id == MouseEvent.MOUSE_MOVED
                            || id == MouseEvent.MOUSE_DRAGGED) lastActive = System.currentTimeMillis();
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

    /** 鼠标挪了 或 4 秒内有过按键/点击，就算人还在电脑前 */
    static boolean activeNow() {
        Point p = mouseNow();
        boolean moved = p != null && lastMouse != null && (p.x != lastMouse.x || p.y != lastMouse.y);
        lastMouse = p;
        return moved || (System.currentTimeMillis() - lastActive <= 4000);
    }

    /* ---------- 计时核心（now 可注入，测试用） ---------- */

    static void tick(long now, boolean active) {
        if (!day.isEmpty() && !LocalDate.now().toString().equals(day)) {
            rollDay(LocalDate.now().toString());
        }
        if (active) lastActive = now;
        if (lastTick == 0) lastTick = now;

        long gapToActive = now - lastActive;
        if (gapToActive > IDLE_MS) {
            /* 人离开了：本轮连续用眼结算掉，回来重新计数；今日累计保留 */
            eyeMs = 0;
            lastTick = now;
            saveIfDue(now);
            return;
        }

        long d = now - lastTick;
        if (d < 0) d = 0;
        if (d > MAX_STEP) d = MAX_STEP;
        lastTick = now;

        if (resting) {
            saveIfDue(now);
            return;
        }

        eyeMs += d;
        todayMs += d;
        if (eyeMs > peakMs) peakMs = eyeMs;
        saveIfDue(now);

        if (on && !reminding && now >= snoozeUntil && eyeMs >= gapMin * 60000L) {
            reminding = true;
            if (GraphicsEnvironment.isHeadless()) {
                /* 无界面（测试）只管状态 */
                return;
            }
            showRestWindow();
        }
    }

    /** 到点该不该弹了（供协议判断，不触发弹窗） */
    static boolean due() {
        return on && !resting && !reminding && eyeMs >= gapMin * 60000L;
    }

    /** 开始一次休息：暂停计时，弹窗切倒计时 */
    static void beginRest() {
        resting = true;
        reminding = false;
    }

    /** 休息结束：记一次、连续清零、下次从 0 开始算 */
    static void finishRest() {
        if (resting) restCount++;
        resting = false;
        reminding = false;
        eyeMs = 0;
        snoozeUntil = 0;
        lastTick = System.currentTimeMillis();
        save();
        if (ticker != null) ticker.restart();
    }

    static void snooze(int minutes) {
        reminding = false;
        snoozeUntil = System.currentTimeMillis() + minutes * 60000L;
        lastTick = System.currentTimeMillis();
    }

    /** 用户在设置页手动重置本次计时 */
    static void resetRound() {
        eyeMs = 0;
        reminding = false;
        snoozeUntil = 0;
        save();
    }

    /* ---------- 每日数据 ---------- */

    static void rollDay(String newDay) {
        save();
        day = newDay;
        eyeMs = 0;
        todayMs = 0;
        restCount = 0;
        peakMs = 0;
        snoozeUntil = 0;
        reminding = false;
        loadDay();
    }

    static void loadDay() {
        day = LocalDate.now().toString();
        eyeMs = 0;
        todayMs = 0;
        restCount = 0;
        peakMs = 0;
        List<String> lines = Store.readLines(FILE);
        for (String line : lines) {
            String[] p = line.split("\t");
            if (p.length < 4) continue;
            if (!p[0].trim().equals(day)) continue;
            todayMs = parseLong(p[1]);
            restCount = (int) parseLong(p[2]);
            peakMs = parseLong(p[3]);
            break;
        }
    }

    static void saveIfDue(long now) {
        if (now - lastSave >= 60000L) save();
    }

    static void save() {
        lastSave = System.currentTimeMillis();
        if (day == null || day.isEmpty()) day = LocalDate.now().toString();
        List<String> lines = Store.readLines(FILE);
        List<String> out = new ArrayList<String>();
        boolean replaced = false;
        for (String line : lines) {
            String[] p = line.split("\t");
            if (p.length >= 4 && p[0].trim().equals(day)) {
                out.add(day + "\t" + todayMs + "\t" + restCount + "\t" + peakMs);
                replaced = true;
            } else {
                out.add(line);
            }
        }
        if (!replaced) out.add(day + "\t" + todayMs + "\t" + restCount + "\t" + peakMs);
        while (out.size() > KEEP_DAYS) out.remove(0);
        Store.writeLines(FILE, out);
    }

    static long parseLong(String s) {
        try {
            return Long.parseLong(s.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    /* ---------- 展示文案 ---------- */

    /** 毫秒转「X 分钟 / H 小时 M 分」 */
    static String span(long ms) {
        long min = ms / 60000L;
        if (min < 1) return "0 分钟";
        if (min < 60) return min + " 分钟";
        return (min / 60) + " 小时 " + (min % 60) + " 分";
    }

    /** 设置页那行数据 */
    public static String todayText() {
        String s = "今日用眼 " + span(todayMs) + " · 休息 " + restCount + " 次 · 最长连续 " + span(peakMs);
        if (!on) s = s + "（提醒已关闭，仍在统计）";
        return s;
    }

    /** 本轮进度，设置页/小窗可读 */
    public static String roundText() {
        return "本轮连续 " + span(eyeMs) + " / 每 " + gapMin + " 分钟提醒一次";
    }

    static boolean isOn() {
        return on;
    }

    static int gap() {
        return gapMin;
    }

    static int rest() {
        return restSec;
    }

    static String readStr(String key, String def) {
        List<String> l = Store.readLines("settings.txt");
        for (String line : l) {
            String[] p = line.split("=", 2);
            if (p.length == 2 && p[0].trim().equals(key)) return p[1].trim();
        }
        return def;
    }

    static boolean readBool(String key, boolean def) {
        String v = readStr(key, null);
        if (v == null) return def;
        return v.equals("1") || v.equalsIgnoreCase("true");
    }

    static int readInt(String key, int def) {
        try {
            return Integer.parseInt(readStr(key, String.valueOf(def)));
        } catch (Exception e) {
            return def;
        }
    }

    /* ---------- 休息提示窗口 ---------- */

    static void showRestWindow() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                closeRestWindow();
                final JWindow w = new JWindow();
                restWin = w;
                JPanel card = new JPanel();
                card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
                card.setBackground(new Color(0xF3F7FF));
                card.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(0xBFD3F5), 1, true),
                        BorderFactory.createEmptyBorder(20, 26, 18, 26)));

                JLabel title = new JLabel("该让眼睛歇一下了", SwingConstants.CENTER);
                title.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 20));
                title.setForeground(new Color(0x1D4ED8));
                title.setAlignmentX(Component.CENTER_ALIGNMENT);

                JLabel sub = new JLabel("已连续用眼 " + span(eyeMs) + " · 今天第 " + (restCount + 1) + " 次休息");
                sub.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
                sub.setForeground(new Color(0x5B6B85));
                sub.setAlignmentX(Component.CENTER_ALIGNMENT);

                JLabel body = new JLabel("<html><div style='width:330px;line-height:1.7'>"
                        + "抬头看向 6 米外（约 20 英尺）的远处 20 秒，让睫状肌松开；<br>"
                        + "顺手眨几次眼、耸耸肩、转两下脖子；<br>"
                        + "屏幕亮度和环境光调得接近一点，别在暗房里盯亮屏。"
                        + "</div></html>");
                body.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
                body.setForeground(new Color(0x2B3446));
                body.setAlignmentX(Component.CENTER_ALIGNMENT);

                countLabel = new JLabel(" ", SwingConstants.CENTER);
                countLabel.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 46));
                countLabel.setForeground(new Color(0x1D4ED8));
                countLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

                JPanel btns = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
                btns.setOpaque(false);
                btns.setAlignmentX(Component.CENTER_ALIGNMENT);

                JButton restBtn = new JButton("开始休息 " + restSec + " 秒");
                restBtn.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 14));
                restBtn.setBackground(new Color(0x1D4ED8));
                restBtn.setForeground(Color.WHITE);
                restBtn.setFocusPainted(false);
                restBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

                JButton laterBtn = new JButton("稍后 " + AUTO_SNOOZE_MIN + " 分钟");
                laterBtn.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
                laterBtn.setFocusPainted(false);
                laterBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

                restBtn.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        beginRest();
                        startRestCountdown(w, restSec, countLabel);
                    }
                });
                laterBtn.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        snooze(AUTO_SNOOZE_MIN);
                        closeRestWindow();
                    }
                });
                btns.add(restBtn);
                btns.add(laterBtn);

                card.add(title);
                card.add(Box.createVerticalStrut(4));
                card.add(sub);
                card.add(Box.createVerticalStrut(12));
                card.add(body);
                card.add(Box.createVerticalStrut(6));
                card.add(countLabel);
                card.add(Box.createVerticalStrut(6));
                card.add(btns);

                w.setContentPane(card);
                w.pack();
                Dimension sc = Toolkit.getDefaultToolkit().getScreenSize();
                w.setLocation((sc.width - w.getWidth()) / 2, Math.max(80, sc.height / 4));
                w.setAlwaysOnTop(true);
                w.setVisible(true);
            }
        });
    }

    static void startRestCountdown(final JWindow w, final int total, final JLabel lbl) {
        if (restTicker != null) restTicker.stop();
        final long startAt = System.currentTimeMillis();
        restTicker = new javax.swing.Timer(1000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                long leftMs = total * 1000L - (System.currentTimeMillis() - startAt);
                int left = (int) Math.max(0, (leftMs + 999) / 1000);
                if (lbl != null) {
                    lbl.setText(left > 0 ? (left + " 秒") : "休息结束");
                }
                if (leftMs <= 0) {
                    if (restTicker != null) restTicker.stop();
                    finishRest();
                    closeRestWindow();
                    Main.notifyMsg("休息结束", "眼睛放松了 " + total + " 秒，接着来吧");
                }
            }
        });
        restTicker.start();
    }

    static void closeRestWindow() {
        if (restTicker != null) {
            restTicker.stop();
            restTicker = null;
        }
        if (restWin != null) {
            restWin.setVisible(false);
            restWin.dispose();
            restWin = null;
        }
    }

    /** 从托盘/窗口回来时，避免把离开期间算成用眼 */
    static void markActive() {
        lastActive = System.currentTimeMillis();
        lastTick = lastActive;
        lastMouse = mouseNow();
    }
}
