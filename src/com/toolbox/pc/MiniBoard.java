package com.toolbox.pc;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** 桌面小窗：置顶显示今天的待办与日程，可拖动、可关闭 */
public class MiniBoard {

    private static MiniBoard inst;

    private JFrame win;
    private JPanel body;
    private JPanel listBox;
    /** 列表下方的固定条：过期视图里"还有 N 条 / 全部勾完"钉在这，不随滚动跑掉 */
    private JPanel pinBox;
    private JLabel dateLbl;
    private int boardDay = 0;        // 待办区看的是哪天：0=今天，-1=昨天，+1=明天
    private boolean overdueView;     // true = 待办区只列过期未做的
    private boolean overdueShowAll;  // 过期视图里是否已展开全部条目
    private int overdueAgeFilter;    // 过期视图的久度筛选：0 全部 / 1 只超 7 天 / 2 只看 3–7 天
    private java.util.List<String> overdueShownTexts = new java.util.ArrayList<String>(); // 当前这一档的原文，供"复制这几条"用
    private JLabel overdueLbl;       // 顶部"过期 N 条"红字
    private JLabel paceLbl;          // 顶部第四行"本周完成 N 条 · 比上周…"
    private JLabel sumLbl;           // 顶部右侧"今天 N 条 · 过期 M"数字摘要
    private JPanel chart;            // 未来 7 天迷你柱图（点摘要展开）
    private boolean showChart;       // 柱图是否展开
    private java.time.LocalDate overdueDay;   // 最近一条过期的日子
    private JLabel focusLbl;
    private String lastDoneText;      // 刚在小窗里勾掉的那一条，留 6 秒给一次"撤回"
    private boolean lastDoneWasUndone; // 提示条当前状态：false=已勾（可撤回），true=已退回（可再勾上）
    private final java.util.List<String> doneBatch = new java.util.ArrayList<String>(); // 6 秒内连着勾掉的几条，一起撤回
    private long lastDoneAt;
    private javax.swing.Timer ticker;

    private int pressX, pressY, winX, winY;
    private boolean translucent = false;

    public static boolean isVisibleNow() {
        return inst != null && inst.win.isVisible();
    }

    public static void toggle() {
        if (isVisibleNow()) inst.hide();
        else show();
    }

    public static void show() {
        if (inst == null) inst = new MiniBoard();
        inst.win.setSize(254, boardHeight());
        inst.win.setVisible(true);
        inst.win.toFront();
        inst.refresh();
        writeSetting("board", "1");
    }

    /** 启动时决定是否显示：自动显示开关优先，否则沿用上次的开关状态 */
    public static void restoreIfOn() {
        String auto = readSetting("boardAuto");
        if ("1".equals(auto)) {
            show();
            return;
        }
        if ("0".equals(auto)) return;
        if ("1".equals(readSetting("board"))) show();
    }

    private void hide() {
        savePos();
        win.setVisible(false);
        writeSetting("board", "0");
    }

    private static int rows() {
        try {
            int v = Integer.parseInt(readSetting("boardRows").trim());
            if (v >= 1 && v <= 6) return v;
        } catch (Exception ignored) {
        }
        return 4;
    }

    private static int boardHeight() {
        return 232 + rows() * 20;     // 比原来高 16：顶部多了"本周完成"一行
    }

    private MiniBoard() {
        win = new JFrame("小工具箱 · 今日");
        win.setUndecorated(true);
        win.setAlwaysOnTop(true);
        win.setSize(254, boardHeight());

        int x = Integer.parseInt(readSetting("boardX").isEmpty() ? "-1" : readSetting("boardX"));
        int y = Integer.parseInt(readSetting("boardY").isEmpty() ? "-1" : readSetting("boardY"));
        if (x < 0 || y < 0) {
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            x = screen.width - 254 - 26;
            y = screen.height - boardHeight() - 70;
        }
        win.setLocation(x, y);

        try {
            GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            if (gd.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT)) {
                win.setBackground(new Color(0, 0, 0, 0));
                translucent = true;
            } else {
                win.setBackground(Theme.bg());
            }
        } catch (Exception e) {
            win.setBackground(Theme.bg());
        }

        body = new JPanel(new BorderLayout(0, 6)) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.bg());
                if (translucent) {
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                } else {
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }
                g2.setColor(Theme.border());
                if (translucent) {
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
                } else {
                    g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                }
                g2.dispose();
            }
        };
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        /* 标题栏（可拖动） */
        JPanel head = new JPanel(new BorderLayout(6, 0));
        head.setOpaque(false);
        dateLbl = new JLabel("今天");
        dateLbl.setFont(Ui.font(13, true));
        dateLbl.setForeground(Theme.text());
        JButton close = new JButton("×");
        close.setFont(Ui.font(14, true));
        close.setForeground(Theme.muted());
        close.setContentAreaFilled(false);
        close.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 0));
        close.setFocusPainted(false);
        close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        close.setToolTipText("关闭小窗");
        close.addActionListener(e -> hide());

        focusLbl = new JLabel(" ");
        focusLbl.setFont(Ui.font(10, false));
        focusLbl.setForeground(Theme.muted());
        focusLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        overdueLbl = new JLabel(" ");
        overdueLbl.setFont(Ui.font(10, true));
        overdueLbl.setForeground(new Color(0xDC2626));
        overdueLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        overdueLbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        overdueLbl.setVisible(false);
        overdueLbl.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getButton() != java.awt.event.MouseEvent.BUTTON1) return;
                if (!overdueView) {
                    overdueView = true;      // 只展开过期的，不按天跳
                    boardDay = 0;
                } else {
                    overdueView = false;     // 再点一下回到今天，不用去点别的入口
                    boardDay = 0;
                }
                refresh();
            }

            public void mousePressed(java.awt.event.MouseEvent e) {
                overduePopup(e);
            }

            public void mouseReleased(java.awt.event.MouseEvent e) {
                overduePopup(e);
            }
        });
        sumLbl = new JLabel(" ");
        sumLbl.setFont(Ui.font(10, false));
        sumLbl.setForeground(Theme.muted());
        sumLbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        sumLbl.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                showChart = !showChart;   // 点一下展开 / 收起未来 7 天柱图
                refresh();
            }
        });
        JPanel dateRow = new JPanel(new BorderLayout(6, 0));
        dateRow.setOpaque(false);
        dateRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        dateRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        dateRow.add(dateLbl, BorderLayout.WEST);
        dateRow.add(sumLbl, BorderLayout.EAST);

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        dateLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        titles.add(dateRow);
        titles.add(focusLbl);
        titles.add(overdueLbl);
        paceLbl = new JLabel(" ");
        paceLbl.setFont(Ui.font(10, false));
        paceLbl.setForeground(Theme.muted());
        paceLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        paceLbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        paceLbl.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (!javax.swing.SwingUtilities.isLeftMouseButton(e)) return;
                openWeekList();
            }

            public void mousePressed(java.awt.event.MouseEvent e) {
                showPaceMenu(e);
            }

            public void mouseReleased(java.awt.event.MouseEvent e) {
                showPaceMenu(e);
            }
        });
        titles.add(paceLbl);
        head.add(titles, BorderLayout.CENTER);
        head.add(close, BorderLayout.EAST);

        MouseAdapter drag = new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                pressX = e.getXOnScreen();
                pressY = e.getYOnScreen();
                winX = win.getX();
                winY = win.getY();
            }

            public void mouseDragged(MouseEvent e) {
                win.setLocation(winX + e.getXOnScreen() - pressX, winY + e.getYOnScreen() - pressY);
            }

            /** 松手时靠近屏幕边缘就吸附过去，并记住位置 */
            public void mouseReleased(MouseEvent e) {
                java.awt.Rectangle sb = GraphicsEnvironment.getLocalGraphicsEnvironment()
                        .getMaximumWindowBounds();
                int snap = 18;
                int w = win.getWidth(), h = win.getHeight();
                int x = win.getX(), y = win.getY();
                if (Math.abs(x - sb.x) < snap) x = sb.x;
                if (Math.abs(x + w - (sb.x + sb.width)) < snap) x = sb.x + sb.width - w;
                if (Math.abs(y - sb.y) < snap) y = sb.y;
                if (Math.abs(y + h - (sb.y + sb.height)) < snap) y = sb.y + sb.height - h;
                win.setLocation(x, y);
                savePos();
            }

            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) Main.showMain();
            }
        };
        head.addMouseListener(drag);
        head.addMouseMotionListener(drag);
        dateLbl.addMouseListener(drag);
        dateLbl.addMouseMotionListener(drag);
        focusLbl.addMouseListener(drag);
        focusLbl.addMouseMotionListener(drag);

        listBox = new JPanel();
        listBox.setOpaque(false);
        listBox.setLayout(new BoxLayout(listBox, BoxLayout.Y_AXIS));

        chart = new JPanel() {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                java.time.LocalDate today = java.time.LocalDate.now();
                int[] cnt = new int[7];
                int total = 0;
                for (String line : Store.readLines("todo.txt")) {
                    String[] p = line.split("\t", -1);
                    if (p.length < 2) continue;
                    if ("1".equals(p[0].trim())) continue;
                    String d = p.length > 2 ? p[2].trim() : "";
                    java.time.LocalDate dd;
                    if (d.isEmpty() || "-".equals(d)) dd = today;
                    else {
                        try {
                            dd = java.time.LocalDate.parse(d);
                        } catch (Exception e3) {
                            continue;
                        }
                    }
                    long off = java.time.temporal.ChronoUnit.DAYS.between(today, dd);
                    if (off < 0 || off > 6) continue;
                    cnt[(int) off]++;
                    total++;
                }
                int w = getWidth(), h = getHeight();
                int max = 1;
                for (int n : cnt) max = Math.max(max, n);
                int pad = 8, gap = 6, baseY = h - 15, topY = 25;
                int bw = (w - pad * 2 - gap * 6) / 7;
                int hl = (boardDay >= 0 && boardDay <= 6) ? boardDay : 0;   // 当前正在看的那天高亮
                g2.setFont(Ui.font(10, false));
                g2.setColor(Theme.muted());
                g2.drawString("未来 7 天 " + total + " 条", pad, 12);
                for (int i = 0; i < 7; i++) {
                    int x = pad + i * (bw + gap);
                    int bh = cnt[i] == 0 ? 2 : Math.max(4, (int) Math.round((baseY - topY) * (cnt[i] * 1.0 / max)));
                    g2.setColor(i == hl ? Ui.PRIMARY : (Theme.isDark() ? new Color(0x475569) : new Color(0xBFD4F7)));
                    g2.fillRoundRect(x, baseY - bh, bw, bh, 3, 3);
                    if (cnt[i] > 0) {
                        g2.setColor(i == hl ? Ui.PRIMARY : Theme.muted());
                        String s = String.valueOf(cnt[i]);
                        g2.drawString(s, x + (bw - g2.getFontMetrics().stringWidth(s)) / 2, baseY - bh - 2);
                    }
                    g2.setColor(Theme.muted());
                    String lb = i == 0 ? "今" : String.valueOf(today.plusDays(i).getDayOfMonth());
                    g2.drawString(lb, x + (bw - g2.getFontMetrics().stringWidth(lb)) / 2, h - 3);
                }
                g2.dispose();
            }
        };
        chart.setOpaque(false);
        chart.setPreferredSize(new Dimension(226, 60));
        chart.setVisible(false);
        chart.setToolTipText("点某根柱子，待办区就切到那天");
        chart.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        chart.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getButton() != java.awt.event.MouseEvent.BUTTON1) return;   // 右键留给新建
                int idx = barIndex(e.getX());
                if (idx < 0) return;
                boardDay = idx;              // 柱 0 = 今天，柱 1 = 明天…
                overdueView = false;
                refresh();
            }

            public void mousePressed(java.awt.event.MouseEvent e) {
                chartPopup(e);
            }

            public void mouseReleased(java.awt.event.MouseEvent e) {
                chartPopup(e);
            }
        });

        JPanel center = new JPanel(new BorderLayout(0, 4));
        center.setOpaque(false);
        center.add(chart, BorderLayout.NORTH);

        JScrollPane sp = new JScrollPane(listBox);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getVerticalScrollBar().setUnitIncrement(12);
        sp.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent e) {
                int cur = rows();
                int next = e.getWheelRotation() > 0 ? Math.max(1, cur - 1) : Math.min(6, cur + 1);
                if (next != cur) {
                    writeSetting("boardRows", String.valueOf(next));
                    refresh();
                }
                e.consume();
            }
        });

        pinBox = new JPanel(new BorderLayout(8, 0));
        pinBox.setOpaque(false);
        pinBox.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
        pinBox.setVisible(false);

        JLabel foot = new JLabel("点 ○ 完成待办 · 双击标题打开", SwingConstants.CENTER);
        foot.setFont(Ui.font(10, false));
        foot.setForeground(Theme.muted());

        JLabel plus = new JLabel("＋");
        plus.setFont(Ui.font(13, true));
        plus.setForeground(Ui.PRIMARY);
        plus.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 6, 0, 6));
        plus.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        plus.setToolTipText("左键：今天新建；右键：挑今天/明天/后天或自选日期");
        plus.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) plusMenu(e);
            }

            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) plusMenu(e);
            }

            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (javax.swing.SwingUtilities.isLeftMouseButton(e)) addTodayByButton();
            }
        });
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.add(foot, BorderLayout.CENTER);
        footer.add(plus, BorderLayout.EAST);
        /* 底栏窄：脚注按实宽逐级换成短句，不然会被压成省略号/裁字 */
        foot.setToolTipText("点 ○ 完成待办 · 双击标题打开");
        final JLabel footRef = foot;
        footer.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent e) {
                int w = footRef.getWidth();
                if (w <= 0) return;
                java.awt.FontMetrics fm = footRef.getFontMetrics(footRef.getFont());
                String[] tries = {"点 ○ 完成待办 · 双击标题打开", "点 ○ 完成 · 双击标题", "点 ○ 完成", "○ 完成"};
                for (String t : tries) {
                    if (fm.stringWidth(t) <= w) {
                        if (!t.equals(footRef.getText())) footRef.setText(t);
                        return;
                    }
                }
                footRef.setText("○ 完成");
            }
        });

        body.add(head, BorderLayout.NORTH);
        center.add(sp, BorderLayout.CENTER);
        center.add(pinBox, BorderLayout.SOUTH);
        body.add(center, BorderLayout.CENTER);
        body.add(footer, BorderLayout.SOUTH);
        win.setContentPane(body);

        ticker = new javax.swing.Timer(30000, e -> {
            if (win.isVisible()) refresh();
        });
        ticker.start();
    }

    /* ---------- 刷新 ---------- */

    /** 柱子索引：0=今天 … 6=第 7 天；点在柱外或柱子之间的空隙返回 -1 */
    int barIndex(int x) {
        int pad = 8, gap = 6;
        int w = chart.getWidth();
        if (w <= 0 || x < pad) return -1;
        int bw = (w - pad * 2 - gap * 6) / 7;
        if (bw <= 0) return -1;
        int idx = (x - pad) / (bw + gap);
        int inBar = (x - pad) % (bw + gap);
        if (idx < 0 || idx > 6 || inBar > bw) return -1;
        return idx;
    }

    /** 柱图上点右键：在那天新建一条待办，或只看那天 */
    void chartPopup(final java.awt.event.MouseEvent e) {        if (!e.isPopupTrigger()) return;
        final int idx = barIndex(e.getX());
        if (idx < 0) return;
        final java.time.LocalDate day = java.time.LocalDate.now().plusDays(idx);
        final String mm = day.format(java.time.format.DateTimeFormatter.ofPattern("MM-dd"));
        javax.swing.JPopupMenu m = new javax.swing.JPopupMenu();
        javax.swing.JMenuItem add = new javax.swing.JMenuItem("在 " + mm + " 新建一条待办");
        add.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent a) {
                newTodoOn(day, mm);
            }
        });
        javax.swing.JMenuItem only = new javax.swing.JMenuItem("只看 " + mm + " 的待办");
        only.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent a) {
                boardDay = idx;
                overdueView = false;
                refresh();
            }
        });
        m.add(add);
        m.add(only);
        m.show(chart, e.getX(), e.getY());
        lastChartMenu = m;   // 留给截图/断言用
    }

    /** 最近一次右键弹出的柱图菜单 */
    javax.swing.JPopupMenu lastChartMenu;

    /** 最近一次右键弹出的过期红字菜单 */
    javax.swing.JPopupMenu lastOverdueMenu;

    /** 顶部过期红字上点右键：批量挪到今天 / 只看过期 / 打开主窗 */
    void overduePopup(final java.awt.event.MouseEvent e) {
        if (!e.isPopupTrigger()) return;
        javax.swing.JPopupMenu m = new javax.swing.JPopupMenu();
        javax.swing.JMenuItem move = new javax.swing.JMenuItem("把这批过期全挪到今天");
        move.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent a) {
                moveAllOverdue();
            }
        });
        javax.swing.JMenuItem only = new javax.swing.JMenuItem("只看过期条目");
        only.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent a) {
                overdueView = true;
                boardDay = 0;
                refresh();
            }
        });
        javax.swing.JMenuItem copyOver = new javax.swing.JMenuItem("复制这批过期到剪贴板");
        copyOver.setToolTipText("把顶部红字里的过期条目连同日期一次放进剪贴板");
        copyOver.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent a) {
                OverviewPanel ov = Main.page(OverviewPanel.class);
                int n = ov == null ? 0 : ov.copyOverdueTexts();
                if (n > 0) Main.notifyMsg("已复制", n + " 条过期标题已放进剪贴板");
            }
        });
        javax.swing.JMenuItem open = new javax.swing.JMenuItem("打开主窗看详情");
        open.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent a) {
                Main.showMain();
            }
        });
        m.add(move);
        m.add(only);
        m.add(copyOver);
        m.addSeparator();
        m.add(open);
        m.show(overdueLbl, e.getX(), e.getY());
        lastOverdueMenu = m;
    }

    /** 一键把过期条目全挪到今天（在主窗留了撤销快照） */
    int moveAllOverdue() {
        OverviewPanel ov = Main.page(OverviewPanel.class);
        if (ov == null) return 0;
        int n = ov.moveOverdueToToday();
        if (n > 0) {
            Main.notifyJump("已挪到今天", n + " 条过期待办改成了今天（主窗可撤销）", "overview");
            overdueView = false;
            boardDay = 0;
        } else {
            Main.notifyMsg("没有可挪的过期条目", "现在的待办都没有过期");
        }
        refresh();
        return n;
    }

    /** 在那天新建一条待办：先提示这天已有几条，避免重复 */
    void newTodoOn(java.time.LocalDate day, String mm) {
        OverviewPanel ov = Main.page(OverviewPanel.class);
        String key = day.toString();
        String hint = ov == null ? "这天还没有待办" : ov.newTodoContext(day, key);
        String msg = "<html><body style='width:260px'>" + escHtml(hint)
                + "<br><br><b>在 " + mm + " 新建一条待办</b></body></html>";
        String t = (String) javax.swing.JOptionPane.showInputDialog(win, msg,
                "新建待办", javax.swing.JOptionPane.PLAIN_MESSAGE);
        if (t == null || t.trim().isEmpty() || ov == null) return;
        ov.addTodoOn(day, t.trim());
        writeSetting("boardAddDay", day.toString());   // 记住这次新建用的日子，下次右键排最前
        refresh();
    }

    /** 底部「＋」：在今天新建一条待办（走和柱图右键同一套输入框） */
    int addTodayByButton() {
        String mm = new java.text.SimpleDateFormat("MM-dd", java.util.Locale.CHINA)
                .format(new java.util.Date());
        newTodoOn(java.time.LocalDate.now(), mm);
        return 1;
    }

    /** 底部「＋」右键：直接给明天 */
    int addTomorrowByButton() {
        java.time.LocalDate tomorrow = java.time.LocalDate.now().plusDays(1);
        String mm = tomorrow.format(java.time.format.DateTimeFormatter.ofPattern("MM-dd"));
        newTodoOn(tomorrow, mm);
        return 1;
    }

    /** 底部「＋」右键：上次那天 / 今天 / 明天 / 后天 / 自选一天 */
    void plusMenu(java.awt.event.MouseEvent e) {
        JPopupMenu m = new JPopupMenu();
        java.time.LocalDate today = java.time.LocalDate.now();
        String f = "MM-dd";
        java.time.LocalDate memoDay = lastAddDay();
        if (memoDay != null) {
            JMenuItem memo = new JMenuItem("新建到上次那天（" + memoDay.format(
                    java.time.format.DateTimeFormatter.ofPattern(f)) + "）");
            memo.setToolTipText("你上次用「＋」新建时挑的那一天");
            final java.time.LocalDate md = memoDay;
            memo.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent a) {
                    newTodoOn(md, md.format(java.time.format.DateTimeFormatter.ofPattern(f)));
                }
            });
            m.add(memo);
            m.addSeparator();
        }
        JMenuItem d0 = new JMenuItem("新建到今天（" + today.format(java.time.format.DateTimeFormatter.ofPattern(f)) + "）");
        d0.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent a) {
                addTodayByButton();
            }
        });
        JMenuItem d1 = new JMenuItem("新建到明天（" + today.plusDays(1).format(
                java.time.format.DateTimeFormatter.ofPattern(f)) + "）");
        d1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent a) {
                addTomorrowByButton();
            }
        });
        JMenuItem d2 = new JMenuItem("新建到后天（" + today.plusDays(2).format(
                java.time.format.DateTimeFormatter.ofPattern(f)) + "）");
        d2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent a) {
                java.time.LocalDate day = java.time.LocalDate.now().plusDays(2);
                newTodoOn(day, day.format(java.time.format.DateTimeFormatter.ofPattern(f)));
            }
        });
        JMenuItem pick = new JMenuItem("选一天…");
        pick.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent a) {
                DatePicker.PickResult r = DatePicker.pickEx(win, java.time.LocalDate.now(), null);
                if (r == null || r.day == null || r.create) return;
                newTodoOn(r.day, r.day.format(java.time.format.DateTimeFormatter.ofPattern(f)));
            }
        });
        m.add(d0);
        m.add(d1);
        m.add(d2);
        m.addSeparator();
        m.add(pick);
        m.show((Component) e.getSource(), e.getX(), e.getY());
        lastPlusMenu = m;
    }

    /** 最近一次「＋」右键弹出的菜单 */
    javax.swing.JPopupMenu lastPlusMenu;

    static String escHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void refresh() {
        Calendar now = Calendar.getInstance();
        if (!translucent) win.setBackground(Theme.bg());
        /* 离开过期视图就把久度筛选清掉，下次点进来看到的是完整的过期清单 */
        if (!overdueView) {
            overdueAgeFilter = 0;
            overdueShowAll = false;
            overdueShownTexts.clear();
        }
        String[] week = {"一", "二", "三", "四", "五", "六", "日"};
        dateLbl.setText((now.get(Calendar.MONTH) + 1) + "月" + now.get(Calendar.DAY_OF_MONTH) + "日 星期"
                + week[isoDay(now) - 1]);
        dateLbl.setForeground(Theme.text());

        int todayMin = 0, todayCnt = 0;
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.CHINA)
                .format(now.getTime());
        for (String line : Store.readLines("pomodoro_log.txt")) {
            String[] p = line.split("\t", -1);
            if (p.length < 4 || !"专注".equals(p[3])) continue;
            if (p[1] == null || !p[1].startsWith(today)) continue;
            try {
                todayMin += Integer.parseInt(p[2].trim());
            } catch (Exception ignored) {
            }
            todayCnt++;
        }
        if (focusLbl != null) {
            focusLbl.setText(todayMin > 0
                    ? "今日专注 " + todayMin + " 分钟 · " + todayCnt + " 个番茄"
                    : "今日还没有专注记录");
        }

        listBox.removeAll();
        pinBox.removeAll();
        pinBox.setVisible(false);
        if (lastDoneText != null) listBox.add(lastDoneLine());
        int limit = rows();
        final java.time.LocalDate day = java.time.LocalDate.now().plusDays(boardDay);

        /* 顶部红字：1-2 天红 / 3-6 天深红 / 7 天以上更深的"很久没动" */
        int overCnt = 0, deepCnt = 0, ancientCnt = 0;
        java.time.LocalDate latestOver = null, oldestOver = null, oldestDeep = null, oldestAncient = null;
        for (String line : Store.readLines("todo.txt")) {
            String[] p = line.split("\t", -1);
            if (p.length < 2) continue;
            if ("1".equals(p[0].trim())) continue;
            String d = p.length > 2 ? p[2].trim() : "";
            if (d.isEmpty() || "-".equals(d)) continue;
            java.time.LocalDate dd;
            try {
                dd = java.time.LocalDate.parse(d);
            } catch (Exception e2) {
                continue;
            }
            if (!dd.isBefore(java.time.LocalDate.now())) continue;
            overCnt++;
            long age = java.time.temporal.ChronoUnit.DAYS.between(dd, java.time.LocalDate.now());
            if (age >= 7) {
                ancientCnt++;
                if (oldestAncient == null || dd.isBefore(oldestAncient)) oldestAncient = dd;
            } else if (age >= 3) {
                deepCnt++;
                if (oldestDeep == null || dd.isBefore(oldestDeep)) oldestDeep = dd;
            }
            if (latestOver == null || dd.isAfter(latestOver)) latestOver = dd;
            if (oldestOver == null || dd.isBefore(oldestOver)) oldestOver = dd;
        }
        overdueDay = latestOver;
        if (sumLbl != null) {
            int todayOpen = 0;
            for (String line : Store.readLines("todo.txt")) {
                String[] p = line.split("\t", -1);
                if (p.length < 2) continue;
                if ("1".equals(p[0].trim())) continue;
                String d = p.length > 2 ? p[2].trim() : "";
                if (d.isEmpty() || "-".equals(d) || d.equals(today)) todayOpen++;
            }
            sumLbl.setText(overCnt > 0
                    ? "今天 " + todayOpen + " · 过期 " + overCnt
                    : "今天 " + todayOpen + " 条");
            if (overdueView) {
                sumLbl.setToolTipText("今天未完成 " + todayOpen + " 条，过期未做 " + overCnt + " 条 · 点一下"
                        + (showChart ? "收起" : "展开") + "未来 7 天柱图");
                sumLbl.setForeground(Theme.muted());
            } else if (overCnt > 0) {
                sumLbl.setToolTipText("今天未完成 " + todayOpen + " 条，过期未做 " + overCnt + " 条 · 点一下"
                        + (showChart ? "收起" : "展开") + "未来 7 天柱图");
                sumLbl.setForeground(new Color(0xB45309));
            } else {
                sumLbl.setToolTipText("今天未完成 " + todayOpen + " 条 · 点一下"
                        + (showChart ? "收起" : "展开") + "未来 7 天柱图");
                sumLbl.setForeground(Theme.muted());
            }
            if (chart != null) {
                chart.setVisible(showChart);
                chart.repaint();
            }
        }
        if (overdueLbl != null) {
            if (overCnt == 0) {
                overdueLbl.setVisible(false);
                overdueLbl.setText(" ");
            } else {
                if (ancientCnt > 0) {
                    /* 7 天以上：最深的一档，单独点名"很久没动"，天数直接算给你看 */
                    long days = java.time.temporal.ChronoUnit.DAYS.between(oldestAncient, java.time.LocalDate.now());
                    overdueLbl.setText("很久没动 " + ancientCnt + " 条 · 最久 " + days + " 天");
                    overdueLbl.setForeground(new Color(0x7F1D1D));
                    overdueLbl.setToolTipText("拖了 7 天以上，最早 "
                            + oldestAncient.format(java.time.format.DateTimeFormatter.ofPattern("MM-dd"))
                            + " · 点这里翻到最早那天");
                    overdueDay = oldestAncient;
                } else if (deepCnt > 0) {
                    long days = java.time.temporal.ChronoUnit.DAYS.between(oldestDeep, java.time.LocalDate.now());
                    overdueLbl.setText("该处理了 " + deepCnt + " 条 · 最久 " + days + " 天");
                    overdueLbl.setForeground(new Color(0x991B1B));
                    overdueLbl.setToolTipText("拖了 3 天以上，最早 "
                            + oldestDeep.format(java.time.format.DateTimeFormatter.ofPattern("MM-dd"))
                            + " · 点这里翻到那天");
                    overdueDay = oldestDeep;
                } else {
                    overdueLbl.setText("过期 " + overCnt + " 条未做");
                    overdueLbl.setForeground(new Color(0xDC2626));
                    overdueLbl.setToolTipText("点这里翻到最近过期的那天");
                }
                overdueLbl.setVisible(true);
                if (overdueView) overdueLbl.setToolTipText("当前只显示过期未做 · 点一下回到今天");
            }
        }
        updatePace();

        /* 待办（可按天切换；点顶部红字进来就只列过期的） */
        java.util.List<String[]> overItems = new java.util.ArrayList<String[]>();
        for (String line : Store.readLines("todo.txt")) {
            String[] p = line.split("\t", -1);
            if (p.length < 2) continue;
            if ("1".equals(p[0].trim())) continue;
            String d = p.length > 2 ? p[2].trim() : "";
            if (d.isEmpty() || "-".equals(d)) continue;
            java.time.LocalDate dd;
            try {
                dd = java.time.LocalDate.parse(d);
            } catch (Exception e2) {
                continue;
            }
            if (!dd.isBefore(java.time.LocalDate.now())) continue;
            overItems.add(new String[]{d, p[1].trim()});
        }
        java.util.Collections.sort(overItems, (x, y) -> x[0].compareTo(y[0]));

        /* 抬头右键可以只看"拖得最久的"或"刚过期几天"的：先按久度筛一遍，再算最久天数 */
        int overAllCount = overItems.size();
        if (overdueAgeFilter > 0) {
            java.util.List<String[]> keep = new java.util.ArrayList<String[]>();
            for (String[] it : overItems) {
                long age;
                try {
                    age = java.time.temporal.ChronoUnit.DAYS.between(
                            java.time.LocalDate.parse(it[0]), java.time.LocalDate.now());
                } catch (Exception ignore) {
                    continue;
                }
                if (overdueAgeFilter == 1 && age > 7) keep.add(it);
                else if (overdueAgeFilter == 2 && age >= 3 && age <= 7) keep.add(it);
            }
            overItems = keep;
        }

        /* 记下当前这一档的原文，抬头的"复制这几条"要靠它 */
        java.util.List<String> shownTexts = new java.util.ArrayList<String>();
        for (String[] it : overItems) shownTexts.add(it[1]);
        overdueShownTexts = shownTexts;

        int maxAge = -1;
        if (overdueView && !overItems.isEmpty()) {
            try {
                maxAge = (int) java.time.temporal.ChronoUnit.DAYS.between(
                        java.time.LocalDate.parse(overItems.get(0)[0]), java.time.LocalDate.now());
            } catch (Exception ignore) {
                maxAge = -1;
            }
        }
        listBox.add(todoHead(day, overItems.size(), maxAge));
        int open = 0;
        if (overdueView) {
            int showLimit = overdueShowAll ? overItems.size() : 4;
            for (String[] it : overItems) {
                if (open >= showLimit) break;
                long age = 0;
                try {
                    age = java.time.temporal.ChronoUnit.DAYS.between(
                            java.time.LocalDate.parse(it[0]), java.time.LocalDate.now());
                } catch (Exception ignore) {
                    age = 0;
                }
                listBox.add(todoLineTail(it[1], it[1], it[0].substring(5) + " · " + age + "天"));
                open++;
            }
            if (open == 0) {
                String noneTxt = overdueAgeFilter == 1 ? "已经没有拖过 7 天的了"
                        : (overdueAgeFilter == 2 ? "3–7 天这一段都处理完了" : "没有过期的了");
                listBox.add(itemLine(noneTxt, true));
            }
            else if (overItems.size() > 4) {
                final JLabel fold = new JLabel(overdueShowAll
                        ? "收起"
                        : "还有 " + (overItems.size() - 4) + " 条…");
                fold.setFont(Ui.font(11, false));
                fold.setForeground(Ui.PRIMARY);
                fold.setAlignmentX(Component.LEFT_ALIGNMENT);
                fold.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
                fold.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                fold.setToolTipText(overdueShowAll
                        ? "收起来，回到只看最近 4 条"
                        : "还有 " + (overItems.size() - 4) + " 条没显示 · 点一下全部列出来");
                fold.addMouseListener(new java.awt.event.MouseAdapter() {
                    public void mouseClicked(java.awt.event.MouseEvent e) {
                        overdueShowAll = !overdueShowAll;
                        refresh();
                    }
                });
                pinBox.add(fold, BorderLayout.WEST);
            }
            /* 底栏多一个"最早 MM-dd"入口，一点就去主窗看拖得最久那天 */
            if (!overItems.isEmpty()) {
                final String earliest = overItems.get(0)[0];
                JLabel early = new JLabel("最早 " + earliest.substring(5));
                early.setFont(Ui.font(11, false));
                early.setForeground(Theme.muted());
                early.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                early.setToolTipText("拖得最久的那天 · 点一下去主窗看 " + earliest.substring(5));
                early.addMouseListener(new java.awt.event.MouseAdapter() {
                    public void mouseClicked(java.awt.event.MouseEvent e) {
                        if (!javax.swing.SwingUtilities.isLeftMouseButton(e)) return;
                        OverviewPanel ov = Main.page(OverviewPanel.class);
                        if (ov != null) {
                            try {
                                ov.jumpToDay(java.time.LocalDate.parse(earliest));
                            } catch (Exception ignore) {
                            }
                        }
                    }
                });
                pinBox.add(early, BorderLayout.CENTER);
            }
            if (!overItems.isEmpty()) {
                JLabel all = new JLabel("全部勾完 " + overItems.size() + " 条");
                all.setFont(Ui.font(11, true));
                all.setForeground(Ui.PRIMARY);
                all.setAlignmentX(Component.LEFT_ALIGNMENT);
                all.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 0));
                all.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                all.setToolTipText("把这几条过期的一次都标成已完成");
                all.addMouseListener(new java.awt.event.MouseAdapter() {
                    public void mouseClicked(java.awt.event.MouseEvent e) {
                        int n = finishAllOverdue();
                        if (n > 0) {
                            Main.notifyMsg("已全部勾完", n + " 条过期待办标成了已完成");
                            refresh();
                        }
                    }
                });
                pinBox.add(all, BorderLayout.EAST);
            }
            if (pinBox.getComponentCount() > 0) pinBox.setVisible(true);
        } else {
            String dayKey = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.CHINA)
                    .format(java.util.Date.from(
                            day.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));
            for (String line : Store.readLines("todo.txt")) {
                String[] p = line.split("\t", -1);
                if (p.length < 2) continue;
                if ("1".equals(p[0].trim())) continue;
                String d = p.length > 2 ? p[2].trim() : "";
                if (!(d.isEmpty() || "-".equals(d) || d.equals(dayKey))) continue;
                if (open >= 4) break;
                listBox.add(todoLine(p[1].trim(), "○ " + p[1].trim()));
                open++;
            }
            if (open == 0) listBox.add(itemLine(boardDay == 0 ? "今天没有待办" : "这天没有待办", true));
        }

        /* 今日日程 */
        listBox.add(section("今日日程"));
        List<String> times = new ArrayList<String>();
        for (String line : Store.readLines("schedule.txt")) {
            String[] p = line.split("\t", -1);
            if (p.length < 3) continue;
            long ms;
            try {
                ms = Long.parseLong(p[2].trim());
            } catch (Exception e) {
                continue;
            }
            String rule = p.length > 3 ? p[3].trim() : "none";
            String hm = todayHM(ms, rule, now);
            if (hm == null) continue;
            times.add(hm + "  " + (p.length > 1 ? p[1] : ""));
        }
        String cur = String.format(Locale.CHINA, "%02d:%02d",
                now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE));
        final int curMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        /* 5 分钟内马上要开始的排最前，已过的沉到底部，其余按时间顺序 */
        times.sort((a, b) -> {
            int ra = hmRank(a.substring(0, 5), curMin);
            int rb = hmRank(b.substring(0, 5), curMin);
            if (ra != rb) return ra - rb;
            /* 已过的一组里，刚过去的排最前，免得“已提醒”那条被更早的挤掉 */
            if (ra == 2) return b.compareTo(a);
            return a.compareTo(b);
        });
        int shown = 0;
        for (String t : times) {
            if (shown >= limit) break;
            String hm = t.substring(0, 5);
            boolean past = hm.compareTo(cur) < 0;
            int dueMin = curMin;
            try {
                dueMin = Integer.parseInt(hm.substring(0, 2)) * 60 + Integer.parseInt(hm.substring(3, 5));
            } catch (Exception ignored) {
            }
            int diff = dueMin - curMin;
            if (!past && diff >= 0 && diff <= 60) {
                if (diff <= 5) {
                    /* 5 分钟内：整行琥珀色底 + 橙字，比绿色高亮更醒目 */
                    Color fg = Theme.isDark() ? new Color(0xFBBF24) : new Color(0xB45309);
                    Color bg = Theme.isDark() ? new Color(0x3E3320) : new Color(0xFEF3C7);
                    String tail = diff == 0 ? "  · 马上开始" : "  · " + diff + " 分钟后";
                    JLabel hot = itemLine(t + tail, fg, true);
                    hot.setOpaque(true);
                    hot.setBackground(bg);
                    hot.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
                    hot.setMaximumSize(new Dimension(Integer.MAX_VALUE, hot.getPreferredSize().height));
                    hot.setToolTipText("马上要开始了 · 双击查看日程页");
                    listBox.add(hot);
                } else {
                    String tail = diff == 0 ? "  · 即将开始" : "  · " + diff + " 分钟后";
                    listBox.add(itemLine(t + tail, new Color(0x22C55E), true));
                }
            } else {
                /* 刚过点 5 分钟内的标注“已提醒”，免得还以为是待办；更早的继续灰色显示 */
                if (diff < 0 && diff >= -5) {
                    JLabel done = itemLine(t + "  · 已提醒", new Color(0x94A3B8), false);
                    done.setToolTipText("这条刚刚提醒过了");
                    listBox.add(done);
                } else {
                    listBox.add(itemLine(t, past));
                }
            }
            shown++;
        }
        if (shown == 0) listBox.add(itemLine("今天没有日程", true));
        if (times.size() > limit) {
            JLabel more = new JLabel("还有 " + (times.size() - limit) + " 条…", SwingConstants.LEFT);
            more.setFont(Ui.font(10, false));
            more.setForeground(Theme.muted());
            more.setAlignmentX(Component.LEFT_ALIGNMENT);
            listBox.add(more);
        }

        listBox.revalidate();
        listBox.repaint();
        body.revalidate();      // 顶部红字出现/消失会改变 head 高度，得让它重新排

        // 内容少时窗口自动收短，避免下半截空白
        int contentPref = listBox.getPreferredSize().height
                + (pinBox.isVisible() ? pinBox.getPreferredSize().height : 0)
                + (showChart ? 64 : 0);
        int want = Math.min(boardHeight() + (showChart ? 64 : 0), Math.max(160, contentPref + 150));
        if (win.getHeight() != want) {
            int bottom = win.getY() + win.getHeight();
            win.setSize(254, want);
            win.setLocation(win.getX(), bottom - want);
        }
        win.repaint();
    }

    /** 小窗顶部第四行：本周完成几条、比上周多还是少 */
    /** 左键点节奏行：开主窗看本周清单 */
    private void openWeekList() {
        Main.showMain();
        Main.go("overview");
        OverviewPanel ov = Main.page(OverviewPanel.class);
        if (ov != null) ov.openDoneList("week");
    }

    /** 右键节奏行：看本周 / 看今天 / 撤回今天（今天一条没勾就只有第一项） */
    JPopupMenu buildPaceMenu() {
        JPopupMenu m = new JPopupMenu();
        JMenuItem week = new JMenuItem("看本周完成清单");
        week.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                openWeekList();
            }
        });
        m.add(week);
        OverviewPanel ov = Main.page(OverviewPanel.class);
        int today = ov == null ? 0 : ov.doneToday();
        if (today > 0) {
            JMenuItem t = new JMenuItem("看今天做完的 " + today + " 条");
            t.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    Main.showMain();
                    Main.go("overview");
                    OverviewPanel p = Main.page(OverviewPanel.class);
                    if (p != null) p.openDoneList("today");
                }
            });
            m.add(t);
            JMenuItem undo = new JMenuItem("撤回今天全部 " + today + " 条");
            undo.setToolTipText("改回未完成，日期不动，小窗和主窗一起刷新");
            undo.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    OverviewPanel p = Main.page(OverviewPanel.class);
                    if (p == null) return;
                    int n = p.undoAllToday();
                    if (n > 0) {
                        refresh();
                        updatePace();
                        Main.notifyMsg("已撤回今天 " + n + " 条", "这几条已改回未完成，日期没动");
                    }
                }
            });
            m.add(undo);
        }
        return m;
    }

    private void showPaceMenu(java.awt.event.MouseEvent e) {
        if (!(e.isPopupTrigger() || javax.swing.SwingUtilities.isRightMouseButton(e))) return;
        JPopupMenu m = buildPaceMenu();
        if (m.getComponentCount() == 0) return;
        m.show(paceLbl, e.getX(), e.getY());
        updatePace();                 // 菜单里可能刚改过数据，把这一行重算一遍
    }

    private void updatePace() {
        if (paceLbl == null) return;
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate mon = today.with(java.time.DayOfWeek.MONDAY);
        int w = 0, l = 0;
        for (String line : Store.readLines("todo.txt")) {
            String[] p = line.split("\t", -1);
            if (p.length < 3) continue;
            if (!"1".equals(p[0].trim())) continue;
            String t = p[1].trim();
            if (t.endsWith("（已归档）")) continue;
            java.time.LocalDate d;
            try {
                d = java.time.LocalDate.parse(p[2].trim());
            } catch (Exception e) {
                continue;
            }
            java.time.LocalDate lastMon = mon.minusWeeks(1);
            if (!d.isBefore(mon) && !d.isAfter(mon.plusDays(6))) w++;
            else if (!d.isBefore(lastMon) && !d.isAfter(lastMon.plusDays(6))) l++;
        }
        int diff = w - l;
        String trend = diff > 0 ? "比上周多 " + diff + " 条"
                : diff < 0 ? "比上周少 " + (-diff) + " 条" : "与上周持平";
        paceLbl.setText("本周完成 " + w + " 条 · " + trend);
        paceLbl.setToolTipText("本周完成 " + w + " 条，上周 " + l + " 条 · 点一下打开主窗看本周清单");
    }

    private JLabel section(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Ui.font(11, true));
        l.setForeground(Theme.muted());
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(BorderFactory.createEmptyBorder(8, 0, 4, 0));
        return l;
    }

    /** 一条待办但右侧挂一段淡灰小字（过期视图里用来看"哪天的·几天没动"） */
    private Component todoLineTail(final String todoText, String title, String tail) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        row.setBorder(BorderFactory.createEmptyBorder(2, 1, 2, 1));
        JLabel name = new JLabel(title);
        name.setFont(Ui.font(12, false));
        name.setForeground(Theme.text());
        JLabel mark = new JLabel(tail);
        mark.setFont(Ui.font(10, false));
        mark.setForeground(Theme.isDark() ? new Color(0x64748B) : new Color(0x94A3B8));
        row.add(name, BorderLayout.CENTER);
        row.add(mark, BorderLayout.EAST);
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        row.setToolTipText("点一下完成 · 右键可改期或删除");
        row.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) {
                popupTodo(e, row, todoText);
            }

            public void mouseReleased(java.awt.event.MouseEvent e) {
                popupTodo(e, row, todoText);
            }

            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (!javax.swing.SwingUtilities.isLeftMouseButton(e)) return;
                if (Store.markTodoDone(todoText)) {
                    lastDoneWasUndone = false;
                    noteJustDone(todoText);
                    refresh();
                }
            }
        });
        return row;
    }

    /** 一条待办：左键点一下完成，右键弹出改期/删除 */
    private JLabel todoLine(final String todoText, String show) {
        final JLabel line = itemLine(show, false);
        line.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        line.setToolTipText("点一下完成 · 右键可改期或删除");
        line.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) {
                popupTodo(e, line, todoText);
            }

            public void mouseReleased(java.awt.event.MouseEvent e) {
                popupTodo(e, line, todoText);
            }

            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (!javax.swing.SwingUtilities.isLeftMouseButton(e)) return;
                if (Store.markTodoDone(todoText)) {
                    lastDoneWasUndone = false;
                    noteJustDone(todoText);
                    refresh();
                }
            }
        });
        return line;
    }

    /** 记下刚勾掉的一条，6 秒内列表顶部给一次"撤回"的机会（状态由调用方先设好） */
    private void noteJustDone(String text) {
        long now = System.currentTimeMillis();
        if (now - lastDoneAt > 6000L) doneBatch.clear();   // 上一批已经过期，重新起一批
        if (text != null && !doneBatch.contains(text)) doneBatch.add(text);
        lastDoneText = text;
        lastDoneAt = now;
        javax.swing.Timer tm = new javax.swing.Timer(6000, new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (lastDoneText != null && System.currentTimeMillis() - lastDoneAt >= 5900) {
                    lastDoneText = null;
                    refresh();
                }
            }
        });
        tm.setRepeats(false);
        tm.start();
    }

    /** 小窗顶部那条刚勾掉的提示：点一下撤回，撤回后再点一下能勾回去；连着勾多条时合成一条整批退回 */
    private JLabel lastDoneLine() {
        final String t = lastDoneText;
        final java.util.List<String> batch = new java.util.ArrayList<String>(doneBatch);
        final boolean many = batch.size() >= 2;
        String show = (t != null && t.length() > 9) ? t.substring(0, 9) + "…" : (t == null ? "" : t);
        final boolean undone = lastDoneWasUndone;
        String txt;
        if (many) {
            txt = undone ? "已退回 " + batch.size() + " 条 · 点我再一起勾上"
                    : "已勾 " + batch.size() + " 条 · 点我一起撤回";
        } else {
            txt = undone ? "已退回「" + show + "」· 点我再勾上" : "已勾「" + show + "」· 点我撤回";
        }
        JLabel l = itemLine(txt, undone ? new Color(0x6B7280) : new Color(0x16A34A), false);
        l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        String tip;
        if (many) {
            StringBuilder sb = new StringBuilder(undone
                    ? "这批刚退回的 " + batch.size() + " 条，点一下全部重新标成完成："
                    : "这批刚勾掉的 " + batch.size() + " 条，点一下全部改回未完成：");
            for (String s : batch) sb.append("\n· ").append(s);
            tip = sb.append("\n6 秒后自动收起").toString();
        } else {
            tip = undone
                    ? "刚退回的这一条，点一下重新标成完成 · 6 秒后自动收起"
                    : "刚勾掉的这一条，点一下改回未完成 · 6 秒后自动收起";
        }
        l.setToolTipText(tip);
        l.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (!javax.swing.SwingUtilities.isLeftMouseButton(e)) return;
                java.util.List<String> targets = new java.util.ArrayList<String>(batch);
                if (targets.isEmpty() && t != null) targets.add(t);
                int cnt = targets.size();
                if (!undone) {
                    int ok = Store.markTodosUndone(targets);
                    if (ok > 0) {
                        Main.notifyMsg("已撤回", cnt > 1
                                ? "这一批 " + ok + " 条改回未完成，日期没动"
                                : t + " 改回未完成，日期没动");
                        lastDoneWasUndone = true;
                    }
                } else {
                    int ok = 0;
                    for (String s : targets) if (Store.markTodoDone(s)) ok++;
                    if (ok > 0) {
                        Main.notifyMsg("已勾上", cnt > 1
                                ? "这一批 " + ok + " 条又标成完成了"
                                : t + " 又标成完成了");
                        lastDoneWasUndone = false;
                    }
                }
                noteJustDone(t);        // 重新起 6 秒，状态换一边继续给你后悔的机会
                refresh();
            }
        });
        return l;
    }

    private void popupTodo(java.awt.event.MouseEvent e, Component inv, final String todoText) {
        if (!e.isPopupTrigger()) return;
        JPopupMenu pm = new JPopupMenu();
        JMenuItem today = new JMenuItem("设为今天");
        today.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent a) {
                if (Store.updateTodoDate(todoText, keyOfDays(0))) {
                    Main.notifyMsg("已设为今天", todoText);
                    overdueView = false;
                    boardDay = 0;
                    refresh();
                }
            }
        });
        JMenuItem tomorrow = new JMenuItem("改期到明天");
        tomorrow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent a) {
                if (Store.updateTodoDate(todoText, keyOfDays(1))) {
                    Main.notifyMsg("已改到明天", todoText);
                    refresh();
                }
            }
        });
        JMenuItem del = new JMenuItem("删除这条");
        del.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent a) {
                if (Store.deleteTodo(todoText)) {
                    Main.notifyMsg("已删除", todoText);
                    refresh();
                }
            }
        });
        JMenuItem pick = new JMenuItem("改期到…");
        pick.setToolTipText("自己选一天，直接放到那天");
        pick.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent a) {
                DatePicker.PickResult r = DatePicker.pickEx(win, java.time.LocalDate.now(), null);
                if (r == null || r.day == null || r.create) return;
                String key = r.day.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                if (Store.updateTodoDate(todoText, key)) {
                    Main.notifyMsg("已改到 " + r.day.format(java.time.format.DateTimeFormatter.ofPattern("MM-dd")),
                            todoText);
                    overdueView = false;
                    boardDay = 0;
                    refresh();
                }
            }
        });
        JMenuItem copy = new JMenuItem("复制到剪贴板");
        copy.setToolTipText("把这条原文放进系统剪贴板");
        copy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent a) {
                java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                        .setContents(new java.awt.datatransfer.StringSelection(todoText), null);
                Main.notifyMsg("已复制", todoText);
            }
        });
        JMenuItem findInMemo = new JMenuItem("在备忘录中查找");
        findInMemo.setToolTipText("跳到备忘录，用这条文本当关键字搜一遍");
        findInMemo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent a) {
                Main.go("memo");
                MemoPanel mp = Main.page(MemoPanel.class);
                if (mp != null) mp.searchFor(todoText);
                Main.notifyMsg("已跳到备忘录", "关键字：" + todoText);
            }
        });
        pm.add(today);
        pm.add(tomorrow);
        pm.add(pick);
        pm.add(copy);
        pm.add(findInMemo);
        pm.addSeparator();
        pm.add(del);
        pm.show(inv, e.getX(), e.getY());
        lastTodoMenu = pm;
    }

    /** 把当前档位下的过期条目原文一次性放进剪贴板 */
    int copyOverdueShown() {
        if (overdueShownTexts == null || overdueShownTexts.isEmpty()) {
            Main.notifyMsg("这一档里没有条目", "先按「全部过期条目」看看还有哪些");
            return 0;
        }
        StringBuilder sb = new StringBuilder();
        for (String s : overdueShownTexts) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(s);
        }
        java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new java.awt.datatransfer.StringSelection(sb.toString()), null);
        Main.notifyMsg("已复制 " + overdueShownTexts.size() + " 条", "这一档的原文都在剪贴板里");
        return overdueShownTexts.size();
    }

    /** 最近一次右键弹出的待办菜单 */
    javax.swing.JPopupMenu lastTodoMenu;

    private static String keyOfDays(int delta) {
        return new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.CHINA)
                .format(java.util.Date.from(
                        java.time.LocalDate.now().plusDays(delta)
                                .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));
    }

    /** 过期视图里的"全部勾完"：把所有过期未做的一次标成完成 */
    int finishAllOverdue() {
        java.util.List<String> names = new java.util.ArrayList<String>();
        for (String line : Store.readLines("todo.txt")) {
            String[] p = line.split("\t", -1);
            if (p.length < 2) continue;
            if ("1".equals(p[0].trim())) continue;
            String d = p.length > 2 ? p[2].trim() : "";
            if (d.isEmpty() || "-".equals(d)) continue;
            try {
                if (!java.time.LocalDate.parse(d).isBefore(java.time.LocalDate.now())) continue;
            } catch (Exception e) {
                continue;
            }
            names.add(p[1].trim());
        }
        int n = 0;
        for (String s : names) if (Store.markTodoDone(s)) n++;
        return n;
    }

    /** 待办分组标题：左边是日期（或"过期未做"），右边 ‹ › 换天，点标题回今天 */
    private JPanel todoHead(java.time.LocalDate day, int overCount, int maxAge) {
        JPanel row = new JPanel(new BorderLayout(2, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setBorder(BorderFactory.createEmptyBorder(8, 0, 4, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        String[] wk = {"一", "二", "三", "四", "五", "六", "日"};
        final String scope = overdueAgeFilter == 1 ? "超 7 天"
                : (overdueAgeFilter == 2 ? "3–7 天" : "");
        final String fullHead = overdueView
                ? (overdueAgeFilter > 0
                ? scope + " · " + overCount + " 条 · 最久 " + maxAge + " 天"
                : "过期 " + overCount + " 条 · 最久 " + maxAge + " 天")
                : (boardDay == 0 ? "今日待办"
                : day.format(java.time.format.DateTimeFormatter.ofPattern("MM-dd"))
                + " 周" + wk[day.getDayOfWeek().getValue() - 1] + " 待办");
        final String shortHead = overdueView
                ? (overdueAgeFilter > 0 ? scope + " · " + overCount + " 条"
                : "过期未做 · " + overCount + " 条")
                : fullHead;
        JLabel title = new JLabel(maxAge > 0 ? fullHead : shortHead);
        title.setFont(Ui.font(11, true));
        title.setForeground(overdueView ? new Color(0xDC2626) : Theme.muted());
        title.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        title.setToolTipText(overdueView
                ? "点一下回到今天 · 最久的一条排在最上面 · 右键可按久度只看一部分"
                : (boardDay == 0 ? "今天的待办" : "点一下回到今天"));
        title.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (boardDay != 0 || overdueView) {
                    boardDay = 0;
                    overdueView = false;
                    refresh();
                }
            }
        });

        JPanel arrows = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        arrows.setOpaque(false);
        arrows.add(navBtn("‹", "看前一天的待办", -1));
        arrows.add(navBtn("›", "看后一天的待办", 1));

        /* 抬头右键：按"拖了多久"把过期条目分档看，档位变了抬头自己跟着换文案 */
        if (overdueView) {
            JPopupMenu ageMenu = new JPopupMenu();
            int[] vals = {0, 1, 2};
            String[] names = {"全部过期条目", "只看超过 7 天的", "只看 3–7 天的"};
            for (int k = 0; k < 3; k++) {
                final int v = vals[k];
                JMenuItem it = new JMenuItem((overdueAgeFilter == v ? "✓ " : "") + names[k]);
                it.addActionListener(e -> {
                    overdueAgeFilter = v;
                    overdueShowAll = false;
                    refresh();
                });
                ageMenu.add(it);
            }
            ageMenu.addSeparator();
            JMenuItem copyScope = new JMenuItem("复制这 " + overCount + " 条原文");
            copyScope.setToolTipText("把当前这一档的原文逐行放进剪贴板");
            copyScope.addActionListener(e -> copyOverdueShown());
            ageMenu.add(copyScope);
            JMenuItem back = new JMenuItem("回到今天");
            back.addActionListener(e -> {
                boardDay = 0;
                overdueView = false;
                overdueAgeFilter = 0;
                overdueShowAll = false;
                refresh();
            });
            ageMenu.add(back);
            row.setComponentPopupMenu(ageMenu);
            title.setComponentPopupMenu(ageMenu);
        }

        /* 抬头宽度会被右侧 ‹ › 吃掉一部分：放不下"最久 N 天"就退回只写条数 */
        final JPanel rowRef = row;
        final Runnable fixHead = new Runnable() {
            public void run() {
                if (!overdueView) return;
                java.awt.FontMetrics fm = title.getFontMetrics(title.getFont());
                int room = rowRef.getWidth() - arrows.getPreferredSize().width - 6;
                String want = (maxAge <= 0 || fm.stringWidth(fullHead) > room) ? shortHead : fullHead;
                if (!want.equals(title.getText())) title.setText(want);
            }
        };
        row.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent e) {
                fixHead.run();
            }
        });
        SwingUtilities.invokeLater(fixHead);

        row.add(title, BorderLayout.WEST);
        row.add(arrows, BorderLayout.EAST);
        return row;
    }

    private JLabel navBtn(String s, String tip, final int delta) {
        JLabel b = new JLabel(s);
        b.setFont(Ui.font(13, true));
        b.setForeground(Ui.PRIMARY);
        b.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setToolTipText(tip);
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                b.setForeground(Ui.PRIMARY.darker());
            }

            public void mouseExited(java.awt.event.MouseEvent e) {
                b.setForeground(Ui.PRIMARY);
            }

            public void mouseClicked(java.awt.event.MouseEvent e) {
                int next = boardDay + delta;
                if (next < -30) next = -30;
                if (next > 30) next = 30;
                if (next != boardDay || overdueView) {
                    boardDay = next;
                    overdueView = false;     // 换天就退出"只看过期"
                    refresh();
                }
            }
        });
        return b;
    }

    /** 排序权重：0 = 5 分钟内，1 = 今天还没到，2 = 已经过了 */
    private static int hmRank(String hm, int curMin) {
        int due = curMin;
        try {
            due = Integer.parseInt(hm.substring(0, 2)) * 60 + Integer.parseInt(hm.substring(3, 5));
        } catch (Exception e) {
        }
        int diff = due - curMin;
        if (diff < 0) return 2;
        return diff <= 5 ? 0 : 1;
    }

    private JLabel itemLine(String text, boolean dim) {
        JLabel l = new JLabel(text);
        l.setFont(Ui.font(12, false));
        l.setForeground(dim ? Theme.muted() : Theme.text());
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(BorderFactory.createEmptyBorder(2, 1, 2, 1));
        hookOpen(l);
        return l;
    }

    private JLabel itemLine(String text, Color color, boolean bold) {
        JLabel l = new JLabel(text);
        l.setFont(Ui.font(12, bold));
        l.setForeground(color);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(BorderFactory.createEmptyBorder(2, 1, 2, 1));
        hookOpen(l);
        return l;
    }

    /** 双击小窗里的条目直接打开日程页 */
    private void hookOpen(JLabel l) {
        l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        l.setToolTipText("双击查看日程页");
        l.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    Main.showMain();
                    Main.go("schedule");
                }
            }
        });
    }

    /** 今天这条提醒的触发时刻（HH:mm），今天不生效返回 null */
    private static String todayHM(long ms, String rule, Calendar today) {
        Calendar t = Calendar.getInstance();
        t.setTimeInMillis(ms);
        String hm = String.format(Locale.CHINA, "%02d:%02d",
                t.get(Calendar.HOUR_OF_DAY), t.get(Calendar.MINUTE));
        int iso = isoDay(today);
        int dom = today.get(Calendar.DAY_OF_MONTH);

        if ("daily".equals(rule)) return hm;
        if ("workday".equals(rule)) return iso <= 5 ? hm : null;
        if (rule.startsWith("weekly:")) {
            for (String s : rule.substring("weekly:".length()).split(",")) {
                try {
                    if (Integer.parseInt(s.trim()) == iso) return hm;
                } catch (Exception ignored) {
                }
            }
            return null;
        }
        if (rule.startsWith("monthly:")) {
            try {
                return Integer.parseInt(rule.substring("monthly:".length()).trim()) == dom ? hm : null;
            } catch (Exception ignored) {
                return null;
            }
        }
        // 不重复：只有原日期当天才显示
        Calendar orig = Calendar.getInstance();
        orig.setTimeInMillis(ms);
        return sameDay(orig, today) ? hm : null;
    }

    private static boolean sameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    private static int isoDay(Calendar c) {
        int d = c.get(Calendar.DAY_OF_WEEK);
        return d == Calendar.SUNDAY ? 7 : d - 1;
    }

    private static final SimpleDateFormat DAY = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);

    /* ---------- 位置记忆 ---------- */

    /** 上次用「＋」新建时挑的那一天（settings.txt 的 boardAddDay），没有就返回 null */
    static java.time.LocalDate lastAddDay() {
        String v = readSetting("boardAddDay").trim();
        if (v.isEmpty()) return null;
        try {
            return java.time.LocalDate.parse(v);
        } catch (Exception e) {
            return null;
        }
    }

    private void savePos() {
        writeSetting("boardX", String.valueOf(win.getX()));
        writeSetting("boardY", String.valueOf(win.getY()));
    }

    static String readSetting(String key) {
        for (String l : Store.readLines("settings.txt")) {
            if (l.startsWith(key + "=")) return l.substring(key.length() + 1).trim();
        }
        return "";
    }

    static void writeSetting(String key, String val) {
        List<String> lines = Store.readLines("settings.txt");
        boolean found = false;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith(key + "=")) {
                lines.set(i, key + "=" + val);
                found = true;
                break;
            }
        }
        if (!found) lines.add(key + "=" + val);
        Store.writeLines("settings.txt", lines);
    }
}
