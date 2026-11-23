package com.toolbox.pc;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * 右下角自绘通知卡片：替代系统托盘气泡。
 * 系统托盘的 displayMessage 由 JVM 发，通知名会显示成 "OpenJDK Platform binary"，
 * 而且同一次操作偶尔会连着弹两条；这里统一成自己的卡片，同一句话 2 秒内只弹一次。
 */
public class Toast {

    private static final int W = 316;
    private static final int H = 70;
    private static final int GAP = 10;
    private static final int MARGIN = 24;
    private static final int LIFE_MS = 3400;

    private static final List<JWindow> stack = new ArrayList<JWindow>();
    private static String lastKey = "";
    private static long lastAt = 0L;

    /** 弹一条通知；同一标题+正文在 2 秒内重复触发时只弹一次 */
    public static void show(String title, String msg) {
        show(title, msg, null);
    }

    /** 带点击动作的通知：点卡片就执行 onClick（比如跳到相关页面） */
    public static void show(String title, String msg, Runnable onClick) {
        if (title == null) title = "小工具箱";
        if (msg == null) msg = "";
        final String key = title + "|" + msg;
        long now = System.currentTimeMillis();
        if (key.equals(lastKey) && now - lastAt < 2000L) return;
        lastKey = key;
        lastAt = now;
        final String t = title, m = msg;
        final Runnable act = onClick;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                build(t, m, act);
            }
        });
    }

    private static void build(final String title, final String msg, final Runnable onClick) {
        final JWindow w = new JWindow();
        w.setAlwaysOnTop(true);

        final JPanel card = new JPanel() {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.isDark() ? new Color(0x1E293B) : new Color(0xF8FAFC));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.setColor(Theme.isDark() ? new Color(0x334155) : new Color(0xE2E8F0));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                java.awt.geom.RoundRectangle2D.Float cl =
                        new java.awt.geom.RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.clip(cl);
                g2.setColor(Ui.PRIMARY);
                g2.fillRect(0, 0, 5, getHeight());
                g2.setClip(null);
                g2.setFont(Ui.font(12, true));
                g2.setColor(Theme.text());
                g2.drawString(clip(title, g2.getFontMetrics(), getWidth() - 46), 18, 26);
                g2.setFont(Ui.font(11, false));
                g2.setColor(Theme.muted());
                g2.drawString(clip(msg, g2.getFontMetrics(), getWidth() - 38), 18, 48);
                g2.setFont(Ui.font(10, false));
                g2.setColor(Theme.muted());
                g2.drawString("×", getWidth() - 20, 22);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BorderLayout());
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                close(w);
                if (onClick != null) SwingUtilities.invokeLater(onClick);
            }
        });
        if (onClick != null) card.setToolTipText("点一下进去看");

        w.setContentPane(card);
        w.setSize(W, H);
        try {
            w.setBackground(new Color(0, 0, 0, 0));
        } catch (Exception ignored) {
        }
        stack.add(w);
        place();
        w.setVisible(true);

        javax.swing.Timer life = new javax.swing.Timer(LIFE_MS, e -> close(w));
        life.setRepeats(false);
        life.start();
    }

    private static void close(JWindow w) {
        if (!w.isVisible()) return;
        stack.remove(w);
        w.dispose();
        place();
    }

    /** 从屏幕右下往上堆，最新的在最下面 */
    private static void place() {
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int n = stack.size();
        for (int i = 0; i < n; i++) {
            JWindow w = stack.get(i);
            int fromBottom = (n - 1 - i);
            int x = screen.width - W - MARGIN;
            int y = screen.height - MARGIN - H - fromBottom * (H + GAP);
            w.setLocation(x, y);
        }
    }

    private static String clip(String s, FontMetrics fm, int max) {
        if (s == null) return "";
        if (fm.stringWidth(s) <= max) return s;
        String out = s;
        while (out.length() > 1 && fm.stringWidth(out + "…") > max) {
            out = out.substring(0, out.length() - 1);
        }
        return out + "…";
    }

    static int count() {
        return stack.size();
    }
}
