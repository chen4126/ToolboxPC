package com.toolbox.pc;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.datatransfer.StringSelection;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

/** 首页概览：今日数据一览 + 点击直达对应工具 */
public class OverviewPanel extends JPanel {

    private static final String[] KEYS = {"pomodoro", "countdown", "schedule", "ledger", "counter", "memo", "habit"};
    private static final String[] TITLES = {"今日专注", "今日倒计时", "日程提醒", "记账结余", "计数器", "备忘条目", "今日打卡"};
    private static final Color[] COLORS = {
            new Color(0x06B6D4), new Color(0x14B8A6), new Color(0x8B5CF6),
            new Color(0x10B981), new Color(0xEF4444), new Color(0xF59E0B),
            new Color(0xF97316)};

    private final JLabel dateLabel = new JLabel();
    private final JLabel[] values = new JLabel[7];
    private final JLabel[] subs = new JLabel[7];
    /** 每张快捷卡的说明行两套文案：完整版、缩写版，按卡内实际宽度择优 */
    private final String[] subFull = new String[7];
    private final String[] subShort = new String[7];
    private JLabel[] enters = new JLabel[7];
    private final Runnable[] fixers = new Runnable[7];

    private JPanel todoBox;
    private JLabel todoStat;
    private JLabel weekDone;
    private JLabel wWeek, wLast, wMonth;
    private JPanel weekDoneBox;
    private JPanel statBar;
    private JLabel todoTitle;                  // “今日待办”或“11-11 周三”
    private JLabel backToday;                  // 看别的日子时的“回到今天”
    private JLabel onlyOverdueBtn;             // 待办卡右上“只看过期”开关
    private JPanel futureBar;                  // 未来 7 天的小标签
    private JLabel paceBar;                    // 待办卡底部的完成节奏
    private JPanel paceRow;                    // 节奏行容器（paceBar + 连涨 + 空档 + 圆点）
    private String paceFull = " ";              // 节奏文案三级：完整 / 只留数字 / 极短
    private String paceShort = " ";
    private String paceMini = " ";
    private String paceTiny = " ";              // 极窄时的兜底：只写“本周/上周”
    private JLabel streakLbl;                  // 节奏行里的“连 N 周↑”小徽标
    private JLabel gapLbl;                     // 节奏行里的“空 N 天”：本周到今还没完成的日子
    private JLabel todayDone;                  // 标题旁的“今天做完 N 条”
    private JPanel weekDots;                   // 节奏行右侧的七天圆点
    private JPopupMenu dotsMenu;                // 圆点右键菜单（留给断言取用）
    private String doneQuery = "";              // 完成清单上次的搜索词
    private JTextField todoInput;              // 待办输入框
    private LocalDate viewDate;                // null = 今天
    private LocalDate overdueFilter;           // 非 null = 只看这天的过期条目
    private int overdueLevel;                  // 1=过期未做 2=该处理了 3=很久没动，0=不筛
    private final java.util.List<Todo> lastMoved = new java.util.ArrayList<Todo>();      // 挪到今天的备份
    private final java.util.List<String> lastMovedDates = new java.util.ArrayList<String>();
    private final java.util.List<Todo> lastArchivedDeleted = new java.util.ArrayList<Todo>();   // 清空归档前的备份
    private boolean multiSelect;                                                       // 待办多选模式
    private final java.util.LinkedHashSet<Todo> picked = new java.util.LinkedHashSet<Todo>();
    private JPanel bulkBar;                                                            // 多选底部的操作条
    private JPanel rangeBar;                                                           // 多选范围标签行
    private boolean archByDate;                                                        // 归档清单按原日期排序
    private JDialog archDialog;                                                        // 归档弹窗，点条目跳主窗时要关掉
    private int archSel = -1;                                                          // 归档弹窗里键盘选中的行（-1 = 未选）
    private final java.util.List<JLabel> archRowNames =
            new java.util.ArrayList<JLabel>();                                          // 归档每行的名字 label，用来描选中态
    private String archQuery = "";                                                     // 归档搜索词
    private int bulkRange;                                                             // 多选范围：0=全部 1=今天 2=未来7天 3=过期 4=自定义
    private java.time.LocalDate bulkFrom, bulkTo;                                      // 自定义起止日
    private JPanel recentBox;
    private JLabel recentStat;
    private JLabel recentFoot;
    private WeekTrend trend;

    /** 本周专注趋势小图（周一~周日） */
    private static class WeekTrend extends JPanel {
        private final int[] mins = new int[7];
        private int total = 0;

        WeekTrend() {
            setOpaque(false);
            setPreferredSize(new Dimension(0, 92));
        }

        void setData(int[] values, int total) {
            System.arraycopy(values, 0, mins, 0, 7);
            this.total = total;
            repaint();
        }

        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Theme.isDark() ? new Color(0x1E2029) : new Color(0xF7F9FC));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
            g2.setColor(Theme.border());
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);

            int padL = 18;
            g2.setFont(Ui.font(12, true));
            g2.setColor(Theme.text());
            g2.drawString("本周专注趋势", padL, 20);

            g2.setFont(Ui.font(11, false));
            g2.setColor(Theme.muted());
            String sum = "共 " + total + " 分钟";
            int sw = g2.getFontMetrics().stringWidth(sum);
            g2.drawString(sum, getWidth() - 18 - sw, 20);

            int n = 7;
            int areaW = getWidth() - padL * 2;
            int slot = Math.max(1, areaW / n);
            int barW = Math.min(34, Math.max(8, slot - 14));
            int top = 28;
            int base = 70;
            int plotH = base - top;
            int max = 1;
            for (int v : mins) max = Math.max(max, v);

            String[] week = {"一", "二", "三", "四", "五", "六", "日"};
            for (int i = 0; i < n; i++) {
                int x = padL + i * slot + (slot - barW) / 2;
                int h = mins[i] <= 0 ? 3 : Math.max(6, (int) (plotH * mins[i] / (double) max));
                if (mins[i] <= 0) {
                    g2.setColor(Theme.isDark() ? new Color(0x3A3F4B) : new Color(0xE2E8F2));
                    g2.fillRoundRect(x, base - 3, barW, 3, 3, 3);
                } else {
                    g2.setColor(new Color(0x06B6D4));
                    g2.fillRoundRect(x, base - h, barW, h, 7, 7);
                }
                g2.setFont(Ui.font(10, false));
                g2.setColor(Theme.muted());
                String lb = week[i];
                int lw = g2.getFontMetrics().stringWidth(lb);
                g2.drawString(lb, x + (barW - lw) / 2, base + 14);
            }
            g2.dispose();
        }
    }
    private final java.util.List<Todo> todos = new java.util.ArrayList<Todo>();
    private static final SimpleDateFormat DAY = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);

    /** 一条待办 */
    private static class Todo {
        boolean done;
        String text;
        String date;

        Todo(boolean done, String text, String date) {
            this.done = done;
            this.text = text;
            this.date = date;
        }
    }

    public OverviewPanel() {
        setOpaque(false);
        setLayout(new BorderLayout(0, 12));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        dateLabel.setFont(Ui.font(13, false));
        dateLabel.setForeground(Theme.muted());
        head.add(dateLabel, BorderLayout.WEST);
        add(head, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(2, 4, 12, 12));
        grid.setOpaque(false);
        for (int i = 0; i < KEYS.length; i++) grid.add(metricCard(i));
        /* 7 张卡塞进 2×4：最后一格留一个透明占位，免得 GridLayout 把 7 张摊成 4+3 的怪样子 */
        JPanel filler = new JPanel();
        filler.setOpaque(false);
        grid.add(filler);

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        trend = new WeekTrend();
        body.add(trend, BorderLayout.NORTH);
        body.add(grid, BorderLayout.CENTER);
        loadTodos();
        loadViewState();     // 上次看的是哪天/哪一档，切页和重启都还在
        /* 底部左右分栏：左边今日待办，右边最近记账 */
        JPanel bottom = new JPanel(new GridLayout(1, 2, 14, 0));
        bottom.setOpaque(false);
        bottom.setPreferredSize(new Dimension(0, 220));   // 比原来高 24：让出底部"完成节奏"那行（含七天圆点）
        bottom.add(todoCard());
        bottom.add(recentCard());
        body.add(bottom, BorderLayout.SOUTH);
        add(body, BorderLayout.CENTER);

        refresh();
    }

    /* ---------- 最近记账 ---------- */

    private JPanel recentCard() {
        JPanel card = new JPanel(new BorderLayout(0, 8)) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.isDark() ? new Color(0x1E2029) : new Color(0xF7F9FC));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(Theme.border());
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JLabel title = new JLabel("最近记账");
        title.setFont(Ui.font(13, true));
        title.setForeground(Theme.text());
        JLabel more = new JLabel("全部 →");
        more.setFont(Ui.font(11, false));
        more.setForeground(Ui.PRIMARY);
        more.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        more.setToolTipText("打开记账本");
        more.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                Main.go("ledger");
            }
        });
        recentStat = new JLabel(" ");
        recentStat.setFont(Ui.font(11, false));
        recentStat.setForeground(Theme.muted());
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(recentStat);
        right.add(more);
        head.add(title, BorderLayout.WEST);
        head.add(right, BorderLayout.EAST);

        recentBox = new JPanel();
        recentBox.setOpaque(false);
        recentBox.setLayout(new BoxLayout(recentBox, BoxLayout.Y_AXIS));
        JScrollPane sp = new JScrollPane(recentBox);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getVerticalScrollBar().setUnitIncrement(12);

        JPanel foot = new JPanel(new BorderLayout());
        foot.setOpaque(false);
        recentFoot = new JLabel(" ");
        recentFoot.setFont(Ui.font(11, false));
        recentFoot.setForeground(Theme.muted());
        foot.add(recentFoot, BorderLayout.WEST);

        card.add(head, BorderLayout.NORTH);
        card.add(sp, BorderLayout.CENTER);
        card.add(foot, BorderLayout.SOUTH);
        return card;
    }

    private void refreshRecent() {
        if (recentBox == null) return;
        recentBox.removeAll();
        java.util.List<String> lines = Store.readLines("ledger.txt");
        int shown = 0;
        for (int i = lines.size() - 1; i >= 0 && shown < 5; i--) {
            String[] p = lines.get(i).split("\t", -1);
            if (p.length < 2) continue;
            boolean income = p.length > 0 && "1".equals(p[0].trim());
            double amt = 0;
            try {
                amt = Double.parseDouble(p[1].trim());
            } catch (Exception ignored) {
            }
            String note = p.length > 2 ? p[2].trim() : "";
            String time = p.length > 3 ? p[3].trim() : "";
            String cat = p.length > 4 && !p[4].trim().isEmpty() ? p[4].trim() : "未分类";
            recentBox.add(recentRow(income, cat, note, time, amt, i));
            shown++;
        }
        if (shown == 0) {
            JLabel empty = new JLabel("还没有记账 · 到记账本记第一笔");
            empty.setFont(Ui.font(11, false));
            empty.setForeground(Theme.muted());
            empty.setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));
            recentBox.add(empty);
        }

        /* 底部统计：本月收支 */
        String prefix = new SimpleDateFormat("MM-", Locale.CHINA).format(new Date());
        double mIn = 0, mOut = 0;
        int mCnt = 0;
        for (String line : lines) {
            String[] p = line.split("\t", -1);
            if (p.length < 4 || !p[3].trim().startsWith(prefix)) continue;
            double amt = 0;
            try {
                amt = Double.parseDouble(p[1].trim());
            } catch (Exception ignored) {
            }
            if ("1".equals(p[0].trim())) mIn += amt;
            else {
                mOut += amt;
                mCnt++;
            }
        }
        recentFoot.setText("本月支出 " + String.format(Locale.CHINA, "¥%.2f", mOut)
                + " · " + mCnt + " 笔" + (mIn > 0 ? "　收入 ¥" + String.format(Locale.CHINA, "%.2f", mIn) : ""));
        recentStat.setText(lines.isEmpty() ? " " : "共 " + lines.size() + " 笔");

        recentBox.revalidate();
        recentBox.repaint();
    }

    private JPanel recentRow(final boolean income, final String cat, final String note,
                             final String time, final double amt, final int lineIndex) {
        final boolean[] hover = new boolean[1];
        final JPanel row = new JPanel(new BorderLayout(8, 0)) {
            protected void paintComponent(Graphics g) {
                if (hover[0]) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(Theme.isDark() ? new Color(0x262A36) : new Color(0xEEF2FF));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setBorder(BorderFactory.createEmptyBorder(1, 4, 1, 4));
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        row.setToolTipText("在记账本中定位这一条");
        row.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                LedgerPanel lp = Main.page(LedgerPanel.class);
                if (lp != null) lp.locateLine(lineIndex);
                Main.go("ledger");
            }

            public void mouseEntered(MouseEvent e) {
                hover[0] = true;
                row.repaint();
            }

            public void mouseExited(MouseEvent e) {
                hover[0] = false;
                row.repaint();
            }
        });

        JLabel t = new JLabel(time.length() > 11 ? time.substring(0, 11) : time);
        t.setFont(Ui.font(10, false));
        t.setForeground(Theme.muted());
        t.setPreferredSize(new Dimension(78, 0));

        JLabel name = new JLabel(note.isEmpty() ? cat : cat + " · " + note);
        name.setFont(Ui.font(12, false));
        name.setForeground(income ? new Color(0x10B981) : Theme.text());

        JLabel val = new JLabel(String.format(Locale.CHINA, "%s¥%.2f", income ? "+" : "-", amt));
        val.setFont(new Font("SansSerif", Font.BOLD, 12));
        val.setForeground(income ? new Color(0x10B981) : new Color(0xEF4444));

        row.add(t, BorderLayout.WEST);
        row.add(name, BorderLayout.CENTER);
        row.add(val, BorderLayout.EAST);
        return row;
    }

    /* ---------- 今日待办 ---------- */

    private JPanel todoCard() {
        JPanel card = new JPanel(new BorderLayout(0, 8)) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.isDark() ? new Color(0x1E2029) : new Color(0xF7F9FC));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(Theme.border());
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));
        card.setPreferredSize(new Dimension(0, 214));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        todoTitle = new JLabel("今日待办");
        todoTitle.setFont(Ui.font(13, true));
        todoTitle.setForeground(Theme.text());
        backToday = new JLabel("回到今天");
        backToday.setFont(Ui.font(10, false));
        backToday.setForeground(Ui.PRIMARY);
        backToday.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 0));
        backToday.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backToday.setToolTipText("回到今天的待办");
        backToday.setVisible(false);
        backToday.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (multiSelect) {
                    multiSelect = false;
                    picked.clear();
                    refreshTodoList();
                    return;
                }
                if (overdueFilter != null || overdueLevel > 0) {
                    overdueFilter = null;
                    overdueLevel = 0;
                    saveViewState();
                    refreshTodoList();
                } else {
                    showDate(null);
                }
            }
        });
        JPanel leftBox = new JPanel();
        leftBox.setOpaque(false);
        leftBox.setLayout(new BoxLayout(leftBox, BoxLayout.X_AXIS));
        leftBox.add(todoTitle);
        todayDone = new JLabel(" ");
        todayDone.setFont(Ui.font(10, false));
        todayDone.setForeground(Theme.muted());
        todayDone.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        todayDone.setToolTipText("今天勾掉的条数 · 看今天时才显示，一点就列出今天完成的这几条；右键可一键撤回");
        todayDone.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        todayDone.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (!javax.swing.SwingUtilities.isLeftMouseButton(e)) return;
                if (doneToday() <= 0) return;
                openDoneList("today");
            }

            public void mousePressed(java.awt.event.MouseEvent e) {
                showTodayMenu(e);
            }

            public void mouseReleased(java.awt.event.MouseEvent e) {
                showTodayMenu(e);
            }
        });
        leftBox.add(todayDone);
        leftBox.add(Box.createHorizontalStrut(10));
        leftBox.add(backToday);
        leftBox.add(Box.createHorizontalStrut(10));
        todoStat = new JLabel(" ");
        todoStat.setFont(Ui.font(11, false));
        todoStat.setForeground(Theme.muted());
        leftBox.add(todoStat);
        leftBox.add(Box.createHorizontalGlue());
        weekDone = new JLabel(" ");
        weekDone.setFont(Ui.font(11, false));
        weekDone.setForeground(Ui.PRIMARY);
        weekDone.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        weekDone.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        weekDone.setToolTipText("这一周完成了多少条 · 点一下去统计页看趋势");
        weekDone.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                saveViewState();
                Main.go("stats");
            }
        });
        /* 完成数拆成三段，各自可点：本周/上周对到统计页的本周卡，本月对到月度卡 */
        wWeek = doneSeg("本周", 0);
        wLast = doneSeg("·上周", 1);
        wMonth = doneSeg("·本月", 2);
        weekDoneBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        weekDoneBox.setOpaque(false);
        weekDoneBox.add(wWeek);
        weekDoneBox.add(wLast);
        weekDoneBox.add(wMonth);
        statBar = null;
        final JLabel bulkBtn = new JLabel("多选");
        bulkBtn.setFont(Ui.font(10, false));
        bulkBtn.setForeground(Ui.PRIMARY);
        bulkBtn.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
        bulkBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        bulkBtn.setToolTipText("选中几条待办，一起改期或一起删掉");
        bulkBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                multiSelect = !multiSelect;
                picked.clear();
                bulkBtn.setText(multiSelect ? "退出多选" : "多选");
                refreshTodoList();
            }
        });
        /* 右键：一次勾上所有过期 / 只勾今天没做的 / 清空已选 / 退出多选 */
        JPopupMenu bulkMenu = new JPopupMenu();
        JMenuItem pickOver = new JMenuItem("全选过期这几条");
        pickOver.setToolTipText("把所有过期未做的一次勾上，底部可以一起改期或一起删掉");
        pickOver.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                java.util.List<Todo> over = allOverdueTodos();
                if (over.isEmpty()) {
                    Main.notifyMsg("没有过期的", "当前一条过期未做都没有");
                    return;
                }
                if (!multiSelect) {
                    multiSelect = true;
                    bulkBtn.setText("退出多选");
                }
                picked.addAll(over);
                refreshTodoList();
                Main.notifyMsg("已勾选过期 " + over.size() + " 条", "底部可以一起改期、一起删掉");
            }
        });
        bulkMenu.add(pickOver);
        JMenuItem pickToday = new JMenuItem("只勾今天没做的");
        pickToday.setToolTipText("把今天还没打勾的条目选中，适合收工前统一处理");
        pickToday.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                LocalDate today = LocalDate.now();
                java.util.List<Todo> todays = new java.util.ArrayList<Todo>();
                for (Todo t : todos) {
                    if (t.done) continue;
                    String raw = t.date == null ? "" : t.date.trim();
                    if (raw.isEmpty() || "-".equals(raw)) continue;
                    LocalDate d = parseDate(raw);
                    if (d != null && d.isEqual(today)) todays.add(t);
                }
                if (todays.isEmpty()) {
                    Main.notifyMsg("今天都做完了", "今天没有还没打勾的条目");
                    return;
                }
                if (!multiSelect) {
                    multiSelect = true;
                    bulkBtn.setText("退出多选");
                }
                picked.addAll(todays);
                refreshTodoList();
                Main.notifyMsg("已勾选今天 " + todays.size() + " 条", "底部可以一起改期、一起删掉");
            }
        });
        bulkMenu.add(pickToday);
        JMenuItem clearPick = new JMenuItem("清空已选");
        clearPick.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                picked.clear();
                refreshTodoList();
            }
        });
        bulkMenu.add(clearPick);
        bulkMenu.addSeparator();
        JMenuItem exitBulk = new JMenuItem("退出多选");
        exitBulk.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                multiSelect = false;
                picked.clear();
                bulkBtn.setText("多选");
                refreshTodoList();
            }
        });
        bulkMenu.add(exitBulk);
        bulkBtn.setComponentPopupMenu(bulkMenu);
        JPanel rightBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightBox.setOpaque(false);
        final JLabel onlyOverdue = new JLabel("只看过期");
        onlyOverdue.setFont(Ui.font(10, false));
        onlyOverdue.setForeground(Ui.PRIMARY);
        onlyOverdue.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        onlyOverdue.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        onlyOverdue.setToolTipText("只列没做完又已经过期的，再点一次回到今天");
        onlyOverdue.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                boolean wasAll = overdueFilter == null && overdueLevel == 9;
                overdueFilter = null;
                overdueLevel = wasAll ? 0 : 9;
                multiSelect = false;
                picked.clear();
                saveViewState();
                refreshTodoList();
                onlyOverdue.setText(wasAll ? "只看过期" : "取消过期");
            }
        });
        rightBox.add(onlyOverdue);
        rightBox.add(bulkBtn);
        rightBox.add(weekDoneBox);
        head.add(leftBox, BorderLayout.WEST);
        head.add(rightBox, BorderLayout.EAST);
        onlyOverdueBtn = onlyOverdue;
        onlyOverdue.setComponentPopupMenu(overdueBarMenu());    // 右键也能整批复制到明天 / 挪到今天

        todoBox = new JPanel();
        todoBox.setOpaque(false);
        todoBox.setLayout(new BoxLayout(todoBox, BoxLayout.Y_AXIS));
        JScrollPane sp = new JScrollPane(todoBox);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getVerticalScrollBar().setUnitIncrement(12);
        /* 窗口压到 1000x700 这种小尺寸时，底部几行横条不能把列表挤没：至少留 3 行高 */
        sp.setMinimumSize(new Dimension(0, 108));

        final JTextField input = new JTextField();
        todoInput = input;
        input.setToolTipText("添加一条今天要做的事，回车即可");
        JButton add = Ui.primary("添加");
        add.setPreferredSize(new Dimension(72, 30));
        ActionListener addL = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String t = input.getText().trim();
                if (t.isEmpty()) return;
                LocalDate day = viewDate == null ? LocalDate.now() : viewDate;
                String key = DAY.format(java.util.Date.from(
                        day.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));
                todos.add(new Todo(false, t, key));
                saveTodos();
                input.setText("");
                refreshTodoList();
            }
        };
        input.addActionListener(addL);
        add.addActionListener(addL);

        JPanel addRow = new JPanel(new BorderLayout(8, 0));
        addRow.setOpaque(false);
        addRow.add(input, BorderLayout.CENTER);
        addRow.add(add, BorderLayout.EAST);

        card.add(head, BorderLayout.NORTH);
        card.add(sp, BorderLayout.CENTER);
        JPanel south = new JPanel();
        south.setOpaque(false);
        south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));
        futureBar = WrapLayout.row();
        futureBar.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        futureBar.setVisible(false);
        futureBar.setComponentPopupMenu(overdueBarMenu());
        futureBar.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent e) {
                futureBar.revalidate();
            }
        });
        addRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        rangeBar = WrapLayout.row();
        rangeBar.setBorder(BorderFactory.createEmptyBorder(0, 0, 3, 0));
        rangeBar.setVisible(false);
        rangeBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        bulkBar = WrapLayout.row();
        bulkBar.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        bulkBar.setVisible(false);
        bulkBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        bulkBar.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent e) {
                bulkBar.revalidate();
            }
        });
        paceBar = new JLabel(" ");
        paceBar.setFont(Ui.font(10, false));
        paceBar.setForeground(Theme.muted());
        paceBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        paceBar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        paceBar.setToolTipText("完成节奏 · 点一下列出本周完成的那几条");
        paceBar.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (!javax.swing.SwingUtilities.isLeftMouseButton(e)) return;
                openDoneList("week");
            }
        });

        /* 本周七天的圆点：有完成填蓝，今天描一圈 */
        weekDots = new JPanel() {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int[] cnt = weekDoneCounts();
                int d = 8, gap = 2;
                int totalW = 7 * (d + gap) - gap;
                g2.setColor(Theme.isDark() ? new Color(0x242733) : new Color(0xF1F4FA));
                g2.fillRoundRect(0, 2, totalW + 6, 15, 8, 8);
                int todayIdx = LocalDate.now().getDayOfWeek().getValue() - 1;   // 周一 = 0
                for (int i = 0; i < 7; i++) {
                    int x = 3 + i * (d + gap);
                    if (cnt[i] >= 2) g2.setColor(new Color(0x3730A3));          // 这天不止一条，颜色更深
                    else if (cnt[i] == 1) g2.setColor(Ui.PRIMARY);
                    else g2.setColor(Theme.isDark() ? new Color(0x4A5160) : new Color(0xCFD9E8));
                    g2.fillOval(x, 5, d, d);
                    if (i == todayIdx) {                        // 今天描一圈深蓝，一眼定位
                        g2.setColor(Theme.isDark() ? new Color(0x93C5FD) : new Color(0x1D4ED8));
                        g2.drawOval(x - 1, 4, d + 2, d + 2);
                    }
                }
                g2.dispose();
            }

            /** 悬停在某颗点上时说明这天完成了几条 */
            public String getToolTipText(java.awt.event.MouseEvent e) {
                int d = 8, gap = 2;
                int[] cnt = weekDoneCounts();
                int todayIdx = LocalDate.now().getDayOfWeek().getValue() - 1;
                String[] names = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
                for (int i = 0; i < 7; i++) {
                    int x = 3 + i * (d + gap);
                    if (e.getX() >= x && e.getX() <= x + d) {
                        String day = names[i] + (i == todayIdx ? "（今天）" : "");
                        return day + " · 完成 " + cnt[i] + " 条"
                                + (cnt[i] >= 2 ? " · 这天产出最多" : "");
                    }
                }
                return "周一到周日 · 蓝点 = 这天完成过，越深越多 · 点一下列出本周清单";
            }
        };
        weekDots.setOpaque(false);
        weekDots.setPreferredSize(new Dimension(74, 18));
        weekDots.setMinimumSize(new Dimension(74, 18));
        weekDots.setMaximumSize(new Dimension(74, 18));
        weekDots.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        weekDots.setToolTipText("周一到周日：蓝点 = 这天完成过，圈 = 今天 · 点一下列出本周清单");
        weekDots.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (!javax.swing.SwingUtilities.isLeftMouseButton(e)) return;
                openDoneList("week");
            }

            public void mousePressed(java.awt.event.MouseEvent e) {
                dotsPopup(e);
            }

            public void mouseReleased(java.awt.event.MouseEvent e) {
                dotsPopup(e);
            }
        });

        paceRow = new JPanel();
        paceRow.setOpaque(false);
        paceRow.setLayout(new BoxLayout(paceRow, BoxLayout.X_AXIS));
        paceRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        paceRow.setBorder(BorderFactory.createEmptyBorder(0, 2, 4, 0));
        paceRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        paceRow.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent e) {
                applyPaceText();
            }
        });
        paceRow.add(paceBar);
        streakLbl = new JLabel(" ");
        streakLbl.setFont(Ui.font(9, true));
        streakLbl.setForeground(new Color(0x1D4ED8));
        streakLbl.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 0));
        streakLbl.setToolTipText("连续几周都比上周做得多");
        paceRow.add(streakLbl);
        gapLbl = new JLabel(" ");
        gapLbl.setFont(Ui.font(9, false));
        gapLbl.setForeground(Theme.muted());
        gapLbl.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        gapLbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        gapLbl.setToolTipText("本周一到今天，一条都没完成的日子");
        gapLbl.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (!javax.swing.SwingUtilities.isLeftMouseButton(e)) return;
                java.util.List<Integer> idle = weekIdleDays();
                if (idle.isEmpty()) return;
                jumpToDay(LocalDate.now().with(java.time.DayOfWeek.MONDAY).plusDays(idle.get(0)));
            }
        });
        paceRow.add(gapLbl);
        paceRow.add(Box.createHorizontalGlue());
        paceRow.add(weekDots);

        south.add(rangeBar);
        south.add(bulkBar);
        south.add(futureBar);
        south.add(paceRow);
        south.add(addRow);
        card.add(south, BorderLayout.SOUTH);
        return card;
    }

    /** 本周（周一…周日）每天是否有完成，7 个布尔 */
    boolean[] weekDoneDays() {
        int[] c = weekDoneCounts();
        boolean[] f = new boolean[7];
        for (int i = 0; i < 7; i++) f[i] = c[i] > 0;
        return f;
    }

    /** 本周每天完成了多少条，长度 7（周一 = 0） */
    int[] weekDoneCounts() {
        int[] n = new int[7];
        LocalDate mon = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        for (Todo t : todos) {
            if (!t.done) continue;
            if (t.text != null && t.text.endsWith("（已归档）")) continue;
            String raw = t.date == null ? "" : t.date.trim();
            if (raw.isEmpty() || "-".equals(raw)) continue;
            LocalDate d = parseDate(raw);
            if (d == null) continue;
            long k = java.time.temporal.ChronoUnit.DAYS.between(mon, d);
            if (k >= 0 && k < 7) n[(int) k]++;
        }
        return n;
    }

    /** 本月每天完成了多少条，长度 = 本月天数（下标 0 = 1 号） */
    int[] monthDoneCounts() {
        java.time.LocalDate now = java.time.LocalDate.now();
        int[] n = new int[now.lengthOfMonth()];
        for (Todo t : todos) {
            if (!t.done) continue;
            if (t.text != null && t.text.endsWith("（已归档）")) continue;
            String raw = t.date == null ? "" : t.date.trim();
            if (raw.isEmpty() || "-".equals(raw)) continue;
            LocalDate d = parseDate(raw);
            if (d == null) continue;
            if (d.getYear() == now.getYear() && d.getMonthValue() == now.getMonthValue()) {
                n[d.getDayOfMonth() - 1]++;
            }
        }
        return n;
    }

    /** 某一段时间里完成的条目（含首尾），按日期升序：{yyyy-MM-dd, 文本}，供统计页点某周/某天展开 */
    java.util.List<String[]> doneInRange(LocalDate from, LocalDate to) {
        java.util.List<String[]> out = new java.util.ArrayList<String[]>();
        for (Todo t : todos) {
            if (!t.done) continue;
            if (t.text != null && t.text.endsWith("（已归档）")) continue;
            String raw = t.date == null ? "" : t.date.trim();
            if (raw.isEmpty() || "-".equals(raw)) continue;
            LocalDate d = parseDate(raw);
            if (d == null) continue;
            if (d.isBefore(from) || d.isAfter(to)) continue;
            out.add(new String[]{d.toString(), t.text == null ? "" : t.text});
        }
        java.util.Collections.sort(out, new java.util.Comparator<String[]>() {
            public int compare(String[] x, String[] y) {
                return x[0].compareTo(y[0]);
            }
        });
        return out;
    }

    private void loadTodos() {
        todos.clear();
        for (String line : Store.readLines("todo.txt")) {
            String[] p = line.split("\t", -1);
            if (p.length < 2) continue;
            String text = p[1].trim();
            if (text.isEmpty()) continue;
            todos.add(new Todo("1".equals(p[0].trim()), text, p.length > 2 ? p[2].trim() : ""));
        }
    }

    /** 把 "yyyy-MM-dd" 解析成日期；空、"-" 或非法值返回今天 */
    private static LocalDate parseDate(String s) {
        if (s == null) return java.time.LocalDate.now();
        String v = s.trim();
        if (v.isEmpty() || "-".equals(v)) return java.time.LocalDate.now();
        try {
            return java.time.LocalDate.parse(v);
        } catch (Exception e) {
            return java.time.LocalDate.now();
        }
    }

    /**
     * 把一条待办改到某天（null 表示今天）。改到非今天的日期后它会从今日待办里消失，
     * 所以提示里带上具体日期，避免用户以为是丢了。
     */
    private void setTodoDate(Todo item, java.time.LocalDate d) {
        java.time.LocalDate day = d == null ? java.time.LocalDate.now() : d;
        item.date = DAY.format(java.util.Date.from(
                day.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));
        saveTodos();
        refreshTodoList();
        String[] wk = {"一", "二", "三", "四", "五", "六", "日"};
        String label = day.equals(java.time.LocalDate.now()) ? "今天"
                : day.toString().substring(5) + " 周" + wk[day.getDayOfWeek().getValue() - 1];
        boolean stillToday = day.equals(java.time.LocalDate.now());
        Main.notifyMsg(stillToday ? "已设为今天" : "已改期到 " + label,
                stillToday ? item.text : item.text + "（到那天会出现在今日待办）");
    }

    private void saveTodos() {
        java.util.List<String> out = new java.util.ArrayList<String>();
        for (Todo t : todos) out.add((t.done ? "1" : "0") + "\t" + t.text + "\t" + (t.date == null ? "" : t.date));
        Store.writeLines("todo.txt", out);
    }

    private java.util.List<Todo> todayTodos() {
        return todosFor(null);
    }

    /** 某天（null=今天）的待办；"不限日期"的每天都会出现 */
    /** 所有未完成的待办（不按天筛），多选模式下用 */
    private java.util.List<Todo> openTodos() {
        java.util.List<Todo> list = new java.util.ArrayList<Todo>();
        for (Todo t : todos) {
            if (t.done) continue;
            if (!inBulkRange(t)) continue;
            list.add(t);
        }
        return list;
    }

    /** 多选范围判定（按待办自己的日期） */
    private boolean inBulkRange(Todo t) {
        if (bulkRange == 0) return true;
        String raw = t.date == null ? "" : t.date.trim();
        java.time.LocalDate today = java.time.LocalDate.now();
        if (bulkRange == 5) return raw.isEmpty() || "-".equals(raw);   // 只看没定日期的
        if (raw.isEmpty() || "-".equals(raw)) return bulkRange == 1;
        java.time.LocalDate d = parseDate(raw);
        if (bulkRange == 1) return d.equals(today);
        if (bulkRange == 2) return d.isAfter(today) && !d.isAfter(today.plusDays(7));   // 明天起 7 天
        if (bulkRange == 3) return d.isBefore(today);      // 只看过期
        if (bulkFrom == null || bulkTo == null) return true;
        return !d.isBefore(bulkFrom) && !d.isAfter(bulkTo);
    }

    private String bulkRangeName() {
        switch (bulkRange) {
            case 1: return "只看今天";
            case 2: return "只看未来 7 天";
            case 3: return "只看过期";
            case 5: return "只看没定日期的";
            case 4: return bulkFrom == null || bulkTo == null ? "自定义范围"
                    : "只看 " + bulkFrom.format(DateTimeFormatter.ofPattern("MM-dd"))
                    + " ~ " + bulkTo.format(DateTimeFormatter.ofPattern("MM-dd"));
            default: return "全部";
        }
    }

    private java.util.List<Todo> todosFor(LocalDate day) {
        LocalDate target = day == null ? LocalDate.now() : day;
        String key = DAY.format(java.util.Date.from(
                target.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));
        java.util.List<Todo> list = new java.util.ArrayList<Todo>();
        for (Todo t : todos) {
            String d = t.date == null ? "" : t.date.trim();
            if (d.isEmpty() || "-".equals(d) || d.equals(key)) list.add(t);
        }
        return list;
    }

    /** 切到某天的待办列表；null 表示回到今天。未来那行的小标签点一下就调这里 */
    void showDate(LocalDate day) {
        overdueFilter = null;
        overdueLevel = 0;
        viewDate = day;
        saveViewState();
        refreshTodoList();
    }

    /** 已经有待办的日子，日历上给它们画小圆点 */
    java.util.Set<LocalDate> markedDates() {
        java.util.Set<LocalDate> set = new java.util.HashSet<LocalDate>();
        for (Todo t : todos) {
            String d = t.date == null ? "" : t.date.trim();
            if (d.isEmpty() || "-".equals(d)) continue;
            set.add(parseDate(d));
        }
        return set;
    }

    /** 日期 → 那天要做的事（日历里点某天就列出来） */
    java.util.Map<LocalDate, java.util.List<String>> todoTitlesByDay() {
        java.util.Map<LocalDate, java.util.List<String>> map =
                new java.util.LinkedHashMap<LocalDate, java.util.List<String>>();
        for (Todo t : todos) {
            String d = t.date == null ? "" : t.date.trim();
            if (d.isEmpty() || "-".equals(d)) continue;
            LocalDate day = parseDate(d);
            java.util.List<String> list = map.get(day);
            if (list == null) {
                list = new java.util.ArrayList<String>();
                map.put(day, list);
            }
            list.add(t.done ? t.text + "（已完成）" : t.text);
        }
        return map;
    }

    private void refreshTodoList() {
        if (todoBox == null) return;
        todoBox.removeAll();
        java.util.List<Todo> list;
        String[] wk = {"一", "二", "三", "四", "五", "六", "日"};
        if (multiSelect) {
            /* 多选模式：把今天、过期、未来的未完成条目一次全铺开，方便一起处理 */
            list = openTodos();
            if (todoTitle != null) {
                todoTitle.setText("全部未完成 · " + list.size() + " 条");
            }
            backToday.setVisible(false);       // 退出入口在右上角"退出多选"
        } else if (overdueFilter != null || overdueLevel > 0) {
            /* 点了某个过期日期 / 某一档：只列对应的过期条目 */
            list = filteredOverdueTodos();
            if (todoTitle != null) {
                todoTitle.setText(overdueFilter != null
                        ? overdueFilter.format(DateTimeFormatter.ofPattern("MM-dd")) + " 过期 · " + list.size() + " 条"
                        : levelName(overdueLevel) + " · " + list.size() + " 条");
            }
            backToday.setText("清除");
            backToday.setToolTipText("回到今天的待办");
            backToday.setVisible(true);
        } else {
            list = todosFor(viewDate);
            if (todoTitle != null) {
                todoTitle.setText(viewDate == null ? "今日待办"
                        : viewDate.format(DateTimeFormatter.ofPattern("MM-dd"))
                        + " 周" + wk[viewDate.getDayOfWeek().getValue() - 1] + " 待办");
                backToday.setText("回到今天");
                backToday.setVisible(viewDate != null);
            }
        }
        if (todoInput != null) {
            todoInput.setToolTipText(viewDate == null ? "添加一条今天要做的事，回车即可"
                    : "添加一条 " + viewDate.format(DateTimeFormatter.ofPattern("MM-dd"))
                    + " 要做的事，回车即可");
        }
        if (list.isEmpty()) {
            JLabel empty = new JLabel(multiSelect ? "没有未完成的待办"
                    : (overdueFilter != null ? "这天过期的都做完了"
                    : (viewDate == null ? "今天还没有待办 · 在下面输入一条" : "这天没有待办")));
            empty.setFont(Ui.font(11, false));
            empty.setForeground(Theme.muted());
            empty.setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));
            todoBox.add(empty);
        } else {
            for (Todo t : list) todoBox.add(todoRow(t));
        }
        if (onlyOverdueBtn != null) {
            onlyOverdueBtn.setText(overdueFilter == null && overdueLevel == 9 ? "取消过期" : "只看过期");
        }
        updateTodoStat();
        updateBulkBar();
        updateFuture();
        attachWeekDone();
        todoBox.revalidate();
        todoBox.repaint();
        if (todoTitle != null && todoTitle.getParent() != null) todoTitle.getParent().revalidate();
    }

    /** 当前筛选（某天或某一档）命中的过期未做条目 */
    java.util.List<Todo> filteredOverdueTodos() {
        java.util.List<Todo> out = new java.util.ArrayList<Todo>();
        if (overdueFilter == null && overdueLevel <= 0) return out;
        LocalDate today = LocalDate.now();
        for (Todo t : todos) {
            if (t.done) continue;
            String raw = t.date == null ? "" : t.date.trim();
            if (raw.isEmpty() || "-".equals(raw)) continue;
            LocalDate d = parseDate(raw);
            if (!d.isBefore(today)) continue;
            if (overdueFilter != null) {
                if (d.equals(overdueFilter)) out.add(t);
            } else if (overdueLevel == 9) {
                out.add(t);                       // 9 = 不分档，全部过期
            } else {
                long age = java.time.temporal.ChronoUnit.DAYS.between(d, today);
                if (levelOf(age) == overdueLevel) out.add(t);
            }
        }
        return out;
    }

    /** 把当前筛选出来的过期条目一次全部勾完 */
    int finishFiltered() {
        java.util.List<Todo> hit = filteredOverdueTodos();
        int n = 0;
        for (Todo t : hit) {
            if (!t.done) {
                t.done = true;
                n++;
            }
        }
        if (n > 0) saveTodos();
        overdueFilter = null;
        overdueLevel = 0;
        saveViewState();
        refreshTodoList();
        if (n > 0) {
            Main.notifyMsg("已勾完", n + " 条过期待办标记为完成");
        }
        return n;
    }

    /* ---------- 上次看的视图（切页与重启都保留） ---------- */

    private void saveViewState() {
        String v = "today";
        if (overdueFilter != null) v = "filter:" + overdueFilter;
        else if (overdueLevel > 0) v = "level:" + overdueLevel;
        else if (viewDate != null) v = "day:" + viewDate;
        MiniBoard.writeSetting("todoView", v);
    }

    private void loadViewState() {
        String v = MiniBoard.readSetting("todoView").trim();
        if (v.isEmpty() || "today".equals(v)) return;
        try {
            if (v.startsWith("filter:")) overdueFilter = LocalDate.parse(v.substring(7));
            else if (v.startsWith("level:")) {
                int lv = Integer.parseInt(v.substring(6));
                if (lv >= 1 && lv <= 3) overdueLevel = lv;
                else if (lv == 9) overdueLevel = 9;
            } else if (v.startsWith("day:")) viewDate = LocalDate.parse(v.substring(4));
        } catch (Exception e) {
            overdueFilter = null;
            overdueLevel = 0;
            viewDate = null;
        }
    }

    /** 新建待办时，先告诉用户这天已经有几条，免得重复加 */
    String newTodoContext(LocalDate day, String key) {
        java.util.List<String> names = new java.util.ArrayList<String>();
        for (Todo t : todos) {
            String d = t.date == null ? "" : t.date.trim();
            if (d.equals(key)) names.add(t.text + (t.done ? "（已完成）" : ""));
        }
        if (names.isEmpty()) return "这天还没有待办，直接输入一条就行";
        StringBuilder sb = new StringBuilder("这天已有 " + names.size() + " 条：");
        int shown = 0;
        for (String s : names) {
            if (shown >= 3) {
                sb.append(" 等");
                break;
            }
            if (shown > 0) sb.append("、");
            sb.append(s);
            shown++;
        }
        return sb.toString();
    }

    /** 过期没做完的待办，按天分组（只算已经过去的日子，未完成） */
    java.util.Map<LocalDate, Integer> overdueByDay() {
        LocalDate today = LocalDate.now();
        java.util.Map<LocalDate, Integer> byDay = new java.util.TreeMap<LocalDate, Integer>();
        for (Todo t : todos) {
            if (t.done) continue;
            String raw = t.date == null ? "" : t.date.trim();
            if (raw.isEmpty() || "-".equals(raw)) continue;
            LocalDate d = parseDate(raw);
            if (d.isBefore(today)) {
                Integer n = byDay.get(d);
                byDay.put(d, n == null ? 1 : n + 1);
            }
        }
        return byDay;
    }

    /** 未来 7 天 / 过期未做，一天一个小标签，点一下切到那天的列表 */
    private void updateFuture() {
        if (futureBar == null) return;
        futureBar.removeAll();
        if (multiSelect) {
            /* 多选时整片让给列表，别再挤未来/归档标签行 */
            futureBar.setVisible(false);
            futureBar.revalidate();
            futureBar.repaint();
            return;
        }

        /* 过期未做：按天数分三档，越久越靠前、颜色越深 */
        java.util.Map<LocalDate, Integer> overdue = overdueByDay();
        int overTotal = 0;
        for (Integer n : overdue.values()) overTotal += n;
        if (overTotal > 0) {
            java.util.List<LocalDate> days = new java.util.ArrayList<LocalDate>(overdue.keySet());
            java.util.List<LocalDate> ancient = new java.util.ArrayList<LocalDate>();   // 7 天以上
            java.util.List<LocalDate> deep = new java.util.ArrayList<LocalDate>();      // 3-6 天
            java.util.List<LocalDate> near = new java.util.ArrayList<LocalDate>();      // 1-2 天
            for (LocalDate day : days) {
                long age = java.time.temporal.ChronoUnit.DAYS.between(day, LocalDate.now());
                if (age >= 7) ancient.add(day);
                else if (age >= 3) deep.add(day);
                else near.add(day);
            }
            // 各档内部按"越早越靠前"排，读起来就是先处理最久的
            java.util.Collections.sort(ancient);
            java.util.Collections.sort(deep);
            java.util.Collections.sort(near);

            if (!ancient.isEmpty()) {
                int n = 0;
                for (LocalDate day : ancient) n += overdue.get(day);
                JLabel flag = levelFlag("很久没动", DEEPER, 3);
                flag.setToolTipText(n + " 条拖了 7 天以上还没动，点一下只看这一档");
                futureBar.add(flag);
                futureBar.add(overdueChip(ancient.get(0), overdue.get(ancient.get(0)), DEEPER,
                        "拖了 7 天以上，还没动"));
                if (ancient.size() > 1) {
                    futureBar.add(restLabel(ancient.subList(1, ancient.size()), overdue, DEEPER, "很久没动"));
                }
                futureBar.add(sepLabel());
            }
            if (!deep.isEmpty()) {
                int n = 0;
                for (LocalDate day : deep) n += overdue.get(day);
                JLabel flag = levelFlag("该处理了", DEEP, 2);
                flag.setToolTipText(n + " 条拖了 3 天以上，点一下只看这一档");
                futureBar.add(flag);
                futureBar.add(overdueChip(deep.get(0), overdue.get(deep.get(0)), DEEP, "拖了 3 天以上，该处理了"));
                if (deep.size() > 1) {
                    futureBar.add(restLabel(deep.subList(1, deep.size()), overdue, DEEP, "该处理了"));
                }
                futureBar.add(sepLabel());
            }
            if (!near.isEmpty()) {
                int n = 0;
                for (LocalDate day : near) n += overdue.get(day);
                futureBar.add(levelFlag("过期未做", OVERDUE, 1));
                int shown = 0;
                for (LocalDate day : near) {
                    if (shown >= 2) break;
                    futureBar.add(overdueChip(day, overdue.get(day), OVERDUE, "这天有没做完的事，点开补做"));
                    shown++;
                }
                if (near.size() > 2) {
                    futureBar.add(restLabel(near.subList(2, near.size()), overdue, OVERDUE, "过期未做"));
                }
                futureBar.add(sepLabel());
            }
        }

        /* 未来 7 天 */
        LocalDate today = LocalDate.now();
        java.util.Map<LocalDate, Integer> byDay = new java.util.TreeMap<LocalDate, Integer>();
        for (Todo t : todos) {
            String raw = t.date == null ? "" : t.date.trim();
            if (raw.isEmpty() || "-".equals(raw)) continue;
            LocalDate d = parseDate(raw);
            if (d.isAfter(today) && d.isBefore(today.plusDays(8))) {
                Integer n = byDay.get(d);
                byDay.put(d, n == null ? 1 : n + 1);
            }
        }
        if (byDay.isEmpty()) {
            if (overTotal == 0) {
                if (lastMoved.isEmpty() && archivedTodos().isEmpty()) {
                    futureBar.setVisible(false);
                    futureBar.revalidate();
                    return;
                }
                if (!lastMoved.isEmpty()) {
                    JLabel done = new JLabel("已挪到今天 " + lastMoved.size() + " 条");
                    done.setFont(Ui.font(10, false));
                    done.setForeground(Theme.muted());
                    futureBar.add(done);
                    addUndoChip();
                }
                addFinishFilteredChip();
                addArchiveChip();
                addUndoArchiveChip();
                addNoDateChip();
                finishFutureBar();
                return;
            }
            addMoveAllChip(overTotal);
            addFinishFilteredChip();
            addUndoChip();
            addArchiveChip();
            addUndoArchiveChip();
            addNoDateChip();
            finishFutureBar();
            return;
        }
        JLabel head = new JLabel("未来 7 天");
        head.setFont(Ui.font(10, false));
        head.setForeground(Theme.muted());
        head.setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 4));
        futureBar.add(head);
        int total = 0;
        int shownDay = 0;
        for (java.util.Map.Entry<LocalDate, Integer> e : byDay.entrySet()) {
            total += e.getValue();
            if (shownDay >= 3) continue;
            final LocalDate day = e.getKey();
            futureBar.add(dayChip(day, e.getValue(), Ui.PRIMARY, "查看这天的待办"));
            shownDay++;
        }
        if (byDay.size() > 3) {
            int rest = 0;
            int i = 0;
            for (java.util.Map.Entry<LocalDate, Integer> e : byDay.entrySet()) {
                if (i++ >= 3) rest += e.getValue();
            }
            JLabel moreDay = new JLabel("还有 " + rest + " 条在后几天");
            moreDay.setFont(Ui.font(10, false));
            moreDay.setForeground(Theme.muted());
            moreDay.setBorder(BorderFactory.createEmptyBorder(1, 6, 1, 6));
            futureBar.add(moreDay);
        }
        JLabel tail = new JLabel("（共 " + total + " 条）");
        tail.setFont(Ui.font(10, false));
        tail.setForeground(Theme.muted());
        futureBar.add(tail);
        addMoveAllChip(overTotal);
        addFinishFilteredChip();
        addUndoChip();
        addArchiveChip();
        addUndoArchiveChip();
        addNoDateChip();
        finishFutureBar();
    }

    /** 待办卡上的「无日期 N 条」入口：点一下直接切进无日期范围 */
    private void addNoDateChip() {
        int n = 0;
        for (Todo t : todos) {
            if (t.done) continue;
            String raw = t.date == null ? "" : t.date.trim();
            if (raw.isEmpty() || "-".equals(raw)) n++;
        }
        if (n == 0) return;
        JLabel chip = new JLabel("无日期 " + n + " 条");
        chip.setFont(Ui.font(10, true));
        chip.setForeground(Ui.PRIMARY);
        chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        chip.setBorder(BorderFactory.createEmptyBorder(1, 6, 1, 6));
        chip.setToolTipText("点一下切成多选，只看没定日期的那 " + n + " 条");
        chip.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (!javax.swing.SwingUtilities.isLeftMouseButton(e)) return;
                multiSelect = true;
                bulkRange = 5;
                picked.clear();
                refreshTodoList();
            }
        });
        futureBar.add(chip);
    }

    /** 刚清空过归档，给一次撤销机会 */
    private void addUndoArchiveChip() {
        if (lastArchivedDeleted.isEmpty()) return;
        final int n = lastArchivedDeleted.size();
        JLabel chip = new JLabel("撤销清空归档 " + n + " 条");
        chip.setFont(Ui.font(10, true));
        chip.setForeground(new Color(0x64748B));
        chip.setBorder(BorderFactory.createEmptyBorder(1, 8, 1, 4));
        chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        chip.setToolTipText("把刚清空的 " + n + " 条归档条目加回来");
        chip.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int back = undoDeleteArchived();
                if (back > 0) {
                    updateFuture();
                    Main.notifyMsg("已撤销清空", back + " 条归档条目加回来了");
                }
            }
        });
        futureBar.add(chip);
    }

    /** 筛选状态下给一个"全部勾完"的入口，省得一条条点 */
    private void addFinishFilteredChip() {
        java.util.List<Todo> hit = filteredOverdueTodos();
        if (hit.isEmpty()) return;
        final int n = hit.size();
        String text = overdueFilter != null
                ? "这天全部勾完 " + n + " 条"
                : (overdueLevel == 9 ? "全部过期一起勾完 " + n + " 条"
                : "这一档全部勾完 " + n + " 条");
        JLabel chip = new JLabel(text);
        chip.setFont(Ui.font(10, true));
        chip.setForeground(Ui.PRIMARY);
        chip.setBorder(BorderFactory.createEmptyBorder(1, 8, 1, 4));
        chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        chip.setToolTipText("把上面列出的 " + n + " 条一次全部标记为完成");
        chip.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                finishFiltered();
            }
        });
        futureBar.add(chip);
    }

    /** 已经归档的（超过 30 天自动收起来的） */
    java.util.List<Todo> archivedTodos() {
        java.util.List<Todo> out = new java.util.ArrayList<Todo>();
        for (Todo t : todos) {
            if (t.done && t.text != null && t.text.endsWith("（已归档）")) out.add(t);
        }
        return out;
    }

    /** 底部那行"已归档 N 条"，点开能看清单、能翻回来 */
    private void addArchiveChip() {
        int n = archivedTodos().size();
        if (n == 0) return;
        int undo = lastArchivedDeleted.size();
        JLabel chip = new JLabel(undo > 0 ? "已归档 " + n + " 条 · 可撤销 " + undo + " 条"
                : "已归档 " + n + " 条");
        chip.setFont(Ui.font(10, false));
        chip.setForeground(Theme.muted());
        chip.setBorder(BorderFactory.createEmptyBorder(1, 8, 1, 4));
        chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        chip.setToolTipText("看看归档了哪些，可以翻回来；彻底删掉的不会出现在撤销里");
        chip.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                showArchiveDialog();
            }
        });
        futureBar.add(chip);
    }

    private void showArchiveDialog() {
        java.util.List<Todo> all = archivedTodos();
        String title = "已归档 " + all.size() + " 条";
        if (!lastArchivedDeleted.isEmpty()) {
            title = title + " · 可撤销 " + lastArchivedDeleted.size() + " 条";
        }
        final JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), title,
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        final int[] act = {-1};
        JPanel outer = archivePanel(dialog, act);
        dialog.setContentPane(outer);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);

        if (act[0] == 0) {
            int n = recoverAllArchived();
            if (n > 0) Main.notifyMsg("已全部翻回", n + " 条归档条目回到今天待办");
        } else if (act[0] == 1) {
            int n = deleteAllArchived();
            if (n > 0) Main.notifyMsg("已清空归档", "删掉 " + n + " 条 · 可在待办卡底部撤销一次");
        }
        refreshTodoList();
    }

    /** 归档弹窗的内容面板：搜索框 + 排序切换 + 清单 + 底部三个按钮（dialog 可为 null，供离屏截图） */
    JPanel archivePanel(final JDialog dialog, final int[] act) {
        archDialog = dialog;
        JPanel outer = new JPanel(new BorderLayout(0, 6));
        outer.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        outer.setOpaque(true);
        outer.setBackground(Theme.panelBg());

        final JLabel archStat = new JLabel(" ");
        archStat.setFont(Ui.font(10, false));
        archStat.setForeground(Theme.muted());

        JPanel bar = new JPanel(new BorderLayout(6, 0));
        bar.setOpaque(false);
        JTextField search = new JTextField(archQuery == null ? "" : archQuery);
        search.setFont(Ui.font(11, false));
        search.setToolTipText("输入关键词或日期即时筛选，比如 四十天前 / 10-22 / 10-2");
        JLabel hint = new JLabel("搜：");
        hint.setFont(Ui.font(10, false));
        hint.setForeground(Theme.muted());
        bar.add(hint, BorderLayout.WEST);
        JPanel searchWrap = new JPanel(new BorderLayout(4, 0));
        searchWrap.setOpaque(false);
        searchWrap.add(search, BorderLayout.CENTER);
        JLabel clearSearch = new JLabel("清空");
        clearSearch.setFont(Ui.font(10, true));
        clearSearch.setForeground(Ui.PRIMARY);
        clearSearch.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clearSearch.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        clearSearch.setToolTipText("把搜索词清掉，回到全部归档");
        clearSearch.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (!javax.swing.SwingUtilities.isLeftMouseButton(e)) return;
                search.setText("");
            }
        });
        searchWrap.add(clearSearch, BorderLayout.EAST);
        bar.add(searchWrap, BorderLayout.CENTER);

        JPanel sort = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        sort.setOpaque(false);
        final JLabel byDate = new JLabel("按原日期");
        final JLabel byOrder = new JLabel("按归档顺序");
        for (JLabel l : new JLabel[]{byDate, byOrder}) {
            l.setFont(Ui.font(10, true));
            l.setForeground(Ui.PRIMARY);
            l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            l.setBorder(BorderFactory.createEmptyBorder(1, 6, 1, 6));
            sort.add(l);
        }
        bar.add(sort, BorderLayout.EAST);
        outer.add(bar, BorderLayout.NORTH);

        final JPanel holder = new JPanel(new BorderLayout());
        holder.setOpaque(false);
        final JScrollPane sp = new JScrollPane(holder);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setPreferredSize(new Dimension(600, Math.min(300, Math.max(90, 62 + filteredArchived().size() * 30))));
        outer.add(sp, BorderLayout.CENTER);

        final Runnable[] rebuild = new Runnable[1];
        rebuild[0] = new Runnable() {
            public void run() {
                archQuery = search.getText() == null ? "" : search.getText().trim();
                byDate.setFont(Ui.font(10, archByDate));
                byOrder.setFont(Ui.font(10, !archByDate));
                byDate.setForeground(archByDate ? Ui.PRIMARY : Theme.muted());
                byOrder.setForeground(archByDate ? Theme.muted() : Ui.PRIMARY);
                holder.removeAll();
                java.util.List<Todo> shown = filteredArchived();
                if (shown.isEmpty()) {
                    JLabel empty = new JLabel(archQuery.isEmpty() ? "归档里没有条目" : "没有匹配「" + archQuery + "」的归档");
                    empty.setFont(Ui.font(11, false));
                    empty.setForeground(Theme.muted());
                    holder.add(empty, BorderLayout.NORTH);
                } else {
                    holder.add(archiveBox(shown), BorderLayout.NORTH);
                }
                holder.revalidate();
                holder.repaint();
                /* 清单换了（搜索 / 切换排序）就把键盘选中挪回第一行 */
                if (!shown.isEmpty() && archSel >= 0) archSel = Math.min(archSel, shown.size() - 1);
                else if (shown.isEmpty()) archSel = -1;
                paintArchSel();
                int all = archivedTodos().size();
                String span = "";
                java.util.List<Todo> allList = archivedTodos();
                LocalDate oldest = null, newest = null;
                for (Todo t : allList) {
                    LocalDate d = parseDate(t.date);
                    if (d == null) continue;
                    if (oldest == null || d.isBefore(oldest)) oldest = d;
                    if (newest == null || d.isAfter(newest)) newest = d;
                }
                if (oldest != null) {
                    java.time.format.DateTimeFormatter f2 =
                            java.time.format.DateTimeFormatter.ofPattern("MM-dd");
                    span = " · 最早 " + oldest.format(f2)
                            + (newest != null && !newest.equals(oldest) ? " · 最近 " + newest.format(f2) : "");
                }
                archStat.setText("共 " + all + " 条" + span
                        + (shown.size() == all ? "" : " · 筛出 " + shown.size() + " 条"));
                sp.setPreferredSize(new Dimension(600,
                        Math.min(300, Math.max(90, 62 + Math.max(1, shown.size()) * 30))));
                if (dialog != null) dialog.pack();
            }
        };
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                rebuild[0].run();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                rebuild[0].run();
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                rebuild[0].run();
            }
        });
        byDate.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                archByDate = true;
                rebuild[0].run();
            }
        });
        byOrder.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                archByDate = false;
                rebuild[0].run();
            }
        });

        JPanel foot = new JPanel(new BorderLayout(8, 0));
        foot.setOpaque(false);
        foot.add(archStat, BorderLayout.WEST);
        JPanel footBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        footBtns.setOpaque(false);
        JButton copyAll = new JButton("复制全部");
        JButton allBack = new JButton("全部恢复");
        JButton allDel = new JButton("全部清空");
        JButton close = new JButton("关闭");
        copyAll.setToolTipText("把上面列着的归档条目一次放进剪贴板，多行排列");
        for (JButton b : new JButton[]{copyAll, allBack, allDel, close}) {
            b.setFont(Ui.font(11, false));
            footBtns.add(b);
        }
        foot.add(footBtns, BorderLayout.EAST);
        copyAll.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int n = copyAllArchived();
                if (n > 0) Main.notifyMsg("已复制", n + " 条归档条目已放进剪贴板");
            }
        });
        allBack.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (act != null) act[0] = 0;
                if (dialog != null) dialog.dispose();
            }
        });
        allDel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (act != null) act[0] = 1;
                if (dialog != null) dialog.dispose();
            }
        });
        close.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (dialog != null) dialog.dispose();
            }
        });
        outer.add(foot, BorderLayout.SOUTH);

        rebuild[0].run();
        bindArchKeys(outer, search, dialog);
        return outer;
    }

    /* ---------- 归档弹窗的键盘流：↑↓ 选行、回车跳那天、Esc 收起来 ---------- */

    /** 上下移动归档弹窗的选中行；第一次按就落在首行/末行 */
    void moveArchSel(int delta) {
        int n = filteredArchived().size();
        if (n <= 0) {
            archSel = -1;
            paintArchSel();
            return;
        }
        if (archSel < 0) archSel = delta >= 0 ? 0 : n - 1;
        else archSel = Math.max(0, Math.min(n - 1, archSel + delta));
        paintArchSel();
    }

    /** 把选中行描出来：手型不变，只加一层浅蓝底，HTML 高亮也不受影响 */
    void paintArchSel() {
        for (int i = 0; i < archRowNames.size(); i++) {
            JLabel l = archRowNames.get(i);
            boolean on = i == archSel;
            l.setOpaque(on);
            l.setBackground(on ? (Theme.isDark() ? new Color(0x1E3A5F) : new Color(0xDBEAFE)) : Theme.panelBg());
            l.repaint();
        }
    }

    /** 回车打开选中那条：跳到它原来那一天；返回是否真的跳了 */
    int openArchSel() {
        java.util.List<Todo> shown = filteredArchived();
        if (archSel < 0 || archSel >= shown.size()) return 0;
        jumpToArchivedDay(shown.get(archSel));
        return 1;
    }

    private void bindArchKeys(JPanel outer, final JTextField search, final JDialog dialog) {
        javax.swing.Action up = new javax.swing.AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                moveArchSel(-1);
            }
        };
        javax.swing.Action down = new javax.swing.AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                moveArchSel(1);
            }
        };
        javax.swing.Action open = new javax.swing.AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (!search.getText().trim().isEmpty() && archSel < 0) return;
                openArchSel();
            }
        };
        javax.swing.Action esc = new javax.swing.AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (!search.getText().isEmpty()) {
                    search.setText("");
                    return;
                }
                if (dialog != null) dialog.dispose();
            }
        };
        javax.swing.InputMap im = outer.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        javax.swing.ActionMap am = outer.getActionMap();
        im.put(javax.swing.KeyStroke.getKeyStroke("UP"), "archUp");
        im.put(javax.swing.KeyStroke.getKeyStroke("DOWN"), "archDown");
        im.put(javax.swing.KeyStroke.getKeyStroke("ENTER"), "archOpen");
        im.put(javax.swing.KeyStroke.getKeyStroke("ESCAPE"), "archEsc");
        am.put("archUp", up);
        am.put("archDown", down);
        am.put("archOpen", open);
        am.put("archEsc", esc);
    }

    /** 归档搜索命中：文字与原始日期都参与；打 `10-2`、`10-02`、`10-22` 都只命中那一天 */
    boolean archHit(String text, String date, String query) {
        String q = query == null ? "" : query.trim();
        if (q.isEmpty()) return true;
        /* 形如 10-2 / 10-02：按"月-日"整段比对，避免 10-2 顺手把 10-22 也算上 */
        if (q.matches("\\d{1,2}-\\d{1,2}")) {
            int[] md = monthDay(date);
            if (md == null) return false;
            String[] b = q.split("-");
            return md[0] == Integer.parseInt(b[0]) && md[1] == Integer.parseInt(b[1]);
        }
        /* 只打一个数：月或日任一对上就算 */
        if (q.matches("\\d{1,2}")) {
            int[] md = monthDay(date);
            if (md == null) return false;
            int v = Integer.parseInt(q);
            return md[0] == v || md[1] == v;
        }
        String hay = (text == null ? "" : text) + " " + (date == null ? "" : date);
        return hay.toLowerCase().contains(q.toLowerCase());
    }

    /** 从 `2026-10-02` 里取 {10, 2}；取不到返回 null */
    private int[] monthDay(String date) {
        if (date == null) return null;
        String s = date.trim();
        String mmdd = s.length() >= 10 ? s.substring(5) : s;
        String[] a = mmdd.split("-");
        if (a.length < 2) return null;
        try {
            return new int[]{Integer.parseInt(a[0]), Integer.parseInt(a[1])};
        } catch (Exception e) {
            return null;
        }
    }

    /** 归档清单按搜索词过滤，并按愿望排序（原日期 or 归档顺序） */
    java.util.List<Todo> filteredArchived() {
        java.util.List<Todo> list = archivedTodos();
        java.util.List<Todo> out = new java.util.ArrayList<Todo>();
        for (Todo t : list) {
            if (!archHit(t.text, t.date, archQuery)) continue;
            out.add(t);
        }
        if (archByDate) {
            java.util.Collections.sort(out, new java.util.Comparator<Todo>() {
                public int compare(Todo a, Todo b) {
                    String x = a.date == null ? "" : a.date;
                    String y = b.date == null ? "" : b.date;
                    return x.compareTo(y);
                }
            });
        }
        return out;
    }

    /** 归档行文字：搜到了词就把命中那段标黄；返回 null 表示不用 HTML（普通黑字） */
    String archLine(String plain, boolean dim) {
        String q = archQuery == null ? "" : archQuery.trim();
        if (q.isEmpty()) return null;
        int idx = plain.toLowerCase().indexOf(q.toLowerCase());
        int end = idx + q.length();
        if (idx < 0 && q.matches("\\d{1,2}(-\\d{1,2})?")) {
            /* 按日期搜（10-2 命中 10-02）：字符串对不上，就标黄开头那段日期 */
            int sp = plain.indexOf('　');
            if (sp > 0 && archHit("", plain.substring(0, sp), q)) {
                idx = 0;
                end = sp;
            }
        }
        if (idx < 0) return null;
        String base = dim ? (Theme.isDark() ? "#64748B" : "#94A3B8")
                : (Theme.isDark() ? "#E2E8F0" : "#334155");
        return "<html><body style='color:" + base + "'>" + escHtml(plain.substring(0, idx))
                + "<span style='background:#FDE68A;color:#111827'>"
                + escHtml(plain.substring(idx, end)) + "</span>"
                + escHtml(plain.substring(end)) + "</body></html>";
    }

    String escHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** 归档条目处理完变灰（有高亮时要把 HTML 里的颜色一起改成灰） */
    void dimArch(JLabel name, String plain) {
        String hl = archLine(plain, true);
        if (hl != null) name.setText(hl);
        name.setForeground(Theme.muted());
    }

    /** 小窗柱图右键：直接在指定那天新建一条待办；返回新增条数（0 = 取消或空文本） */
    int addTodoOn(java.time.LocalDate day, String text) {
        if (day == null || text == null || text.trim().isEmpty()) return 0;
        String key = DAY.format(java.util.Date.from(
                day.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));
        todos.add(new Todo(false, text.trim(), key));
        saveTodos();
        refreshTodoList();
        updateTodoStat();
        Main.notifyMsg("已在 " + day.format(DateTimeFormatter.ofPattern("MM-dd")) + " 加了一条",
                text.trim());
        return 1;
    }

    /** 归档条目复制一条到今天：归档里那条保持不动，只多出一条新待办 */
    int copyArchivedToToday(Todo t) {
        String txt = t.text == null ? "" : t.text;
        if (txt.endsWith("（已归档）")) txt = txt.substring(0, txt.length() - "（已归档）".length());
        todos.add(new Todo(false, txt, DAY.format(new Date())));
        saveTodos();
        refreshTodoList();
        Main.notifyJump("已复制到今天", txt, "overview");
        return 1;
    }

    /** 归档清单面板：每条两个按钮，可以单条恢复或彻底删掉 */
    JPanel archiveBox(java.util.List<Todo> list) {
        archRowNames.clear();
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setOpaque(false);
        JLabel tip = new JLabel("超过 " + archiveDays() + " 天没动的条目已自动收起：点「恢复」回到今天，"
                + "点「恢复并改期」直接放到某一天");
        tip.setFont(Ui.font(11, false));
        tip.setForeground(Theme.muted());
        tip.setAlignmentX(Component.LEFT_ALIGNMENT);
        tip.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        box.add(tip);
        JLabel tip2 = new JLabel("「彻底删」永久抹掉不走撤销；「全部清空」可在待办卡底部撤销一次"
                + (lastArchivedDeleted.isEmpty() ? "" : "（现在可撤销 " + lastArchivedDeleted.size() + " 条）"));
        tip2.setFont(Ui.font(10, false));
        tip2.setForeground(Theme.muted());
        tip2.setAlignmentX(Component.LEFT_ALIGNMENT);
        tip2.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        box.add(tip2);
        JLabel tip3 = new JLabel("键盘：↑↓ 选行 · 回车跳到那天 · Esc 收起来（Esc 先清搜索词）");
        tip3.setFont(Ui.font(10, false));
        tip3.setForeground(Theme.muted());
        tip3.setAlignmentX(Component.LEFT_ALIGNMENT);
        tip3.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        box.add(tip3);
        for (final Todo t : list) {
            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setOpaque(false);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.setMaximumSize(new Dimension(560, 28));
            String when = t.date == null ? "" : t.date.trim();
            if (when.length() >= 10) when = when.substring(5);
            String plain = when + "　" + t.text;
            String hl = archLine(plain, false);
            final JLabel name = new JLabel(hl == null ? plain : hl);
            name.setFont(Ui.font(11, false));
            if (hl == null) name.setForeground(Theme.text());
            name.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            name.setToolTipText("点一下跳到那天的待办 · 右键可复制 / 恢复成原日期 / 复制到剪贴板");
            name.setBorder(BorderFactory.createEmptyBorder(1, 4, 1, 4));
            archRowNames.add(name);
            name.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (!javax.swing.SwingUtilities.isLeftMouseButton(e)) return;
                    jumpToArchivedDay(t);
                }
            });
            javax.swing.JPopupMenu archMenu = new javax.swing.JPopupMenu();
            javax.swing.JMenuItem copyToday = new javax.swing.JMenuItem("复制一条到今天");
            copyToday.setToolTipText("老账重新立成今天的新待办，归档里那一条不动");
            copyToday.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    copyArchivedToToday(t);
                }
            });
            archMenu.add(copyToday);
            javax.swing.JMenuItem backOrigin = new javax.swing.JMenuItem("恢复成原日期");
            backOrigin.setToolTipText("翻回来后仍停在原来那天，而不是一律落到今天");
            backOrigin.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    recoverArchivedKeepDate(t);
                }
            });
            archMenu.add(backOrigin);
            javax.swing.JMenuItem archCopy = new javax.swing.JMenuItem("复制到剪贴板");
            archCopy.setToolTipText("把这条老账的原文放进系统剪贴板");
            archCopy.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    String txt = t.text == null ? "" : t.text;
                    if (txt.endsWith("（已归档）")) txt = txt.substring(0, txt.length() - "（已归档）".length());
                    java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                            .setContents(new java.awt.datatransfer.StringSelection(txt), null);
                    Main.notifyMsg("已复制", txt);
                }
            });
            archMenu.add(archCopy);
            name.setComponentPopupMenu(archMenu);
            JPanel acts = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
            acts.setOpaque(false);
            final JButton back = new JButton("恢复");
            back.setFont(Ui.font(10, false));
            final JButton pick = new JButton("恢复并改期");
            pick.setFont(Ui.font(10, false));
            pick.setToolTipText("翻回来并直接放到指定日期，而不是一律落到今天");
            final JButton del = new JButton("彻底删");
            del.setFont(Ui.font(10, false));
            del.setToolTipText("直接从归档里抹掉，不进撤销快照");
            back.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    recoverArchived(t);
                    back.setText("已恢复");
                    back.setEnabled(false);
                    pick.setEnabled(false);
                    del.setEnabled(false);
                    dimArch(name, plain);
                }
            });
            pick.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    DatePicker.PickResult r = DatePicker.pickEx(OverviewPanel.this,
                            java.time.LocalDate.now(), todoTitlesByDay());
                    if (r == null || r.day == null) return;
                    recoverArchivedTo(t, r.day);
                    pick.setText("已放到 " + r.day.format(DateTimeFormatter.ofPattern("MM-dd")));
                    pick.setEnabled(false);
                    back.setEnabled(false);
                    del.setEnabled(false);
                    dimArch(name, plain);
                }
            });
            del.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    deleteArchived(t);
                    back.setEnabled(false);
                    pick.setEnabled(false);
                    del.setText("已彻底删");
                    del.setEnabled(false);
                    dimArch(name, plain);
                }
            });
            acts.add(del);
            acts.add(pick);
            acts.add(back);
            row.add(name, BorderLayout.CENTER);
            row.add(acts, BorderLayout.EAST);
            box.add(row);
        }
        return box;
    }

    /** 归档条目恢复到原日期：日期保持归档时那天，不落到今天 */
    int recoverArchivedKeepDate(Todo t) {
        if (t == null) return 0;
        t.done = false;
        if (t.text != null && t.text.endsWith("（已归档）")) {
            t.text = t.text.substring(0, t.text.length() - "（已归档）".length());
        }
        String when = t.date == null ? "" : t.date.trim();
        String mm = when.length() >= 10 ? when.substring(5) : when;
        saveTodos();
        refreshTodoList();
        Main.notifyJump("已恢复成原日期", t.text + " 回到 " + mm + "（那天已过去，会算作过期）", "overview");
        return 1;
    }

    /** 点归档条目：跳到主窗那天的待办列表（弹窗顺手关掉，别挡着） */
    int jumpToArchivedDay(Todo t) {
        if (t == null) return 0;
        LocalDate d = parseDate(t.date);
        if (d == null) {
            Main.notifyMsg("这条没有日期", "归档条目没记日期，跳不过去");
            return 0;
        }
        if (archDialog != null) {
            archDialog.dispose();
            archDialog = null;
        }
        archSel = -1;
        archRowNames.clear();
        viewDate = d;
        overdueFilter = null;
        overdueLevel = 0;
        multiSelect = false;
        picked.clear();
        saveViewState();
        Main.go("overview");
        refreshTodoList();
        Main.notifyMsg("已跳到 " + d.format(DateTimeFormatter.ofPattern("MM-dd")),
                "这天目前没有未完成的待办，就是老账本身");
        return 1;
    }

    /** 干脆跳到某一天（小窗"最早 11-01"、完成清单里点某行都走这里） */
    void jumpToDay(LocalDate d) {
        if (d == null) return;
        viewDate = d;
        overdueFilter = null;
        overdueLevel = 0;
        multiSelect = false;
        picked.clear();
        saveViewState();
        Main.go("overview");
        refreshTodoList();
        updateTodoStat();
    }

    /** 圆点右键：定位到这一天的清单，或把那天完成的条目录进剪贴板 */
    private void dotsPopup(java.awt.event.MouseEvent e) {
        if (!e.isPopupTrigger()) return;
        int idx = (e.getX() - 3) / 10;      // 点直径 8、间距 2，起点 x=3
        if (idx < 0 || idx > 6) return;
        LocalDate monday = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        final LocalDate d = monday.plusDays(idx);
        String mm = d.format(java.time.format.DateTimeFormatter.ofPattern("MM-dd"));
        int doneCnt = weekDoneCounts()[idx];
        final int allCnt = todosOn(d);
        JPopupMenu pm = new JPopupMenu();
        JMenuItem open = new JMenuItem("看 " + mm + " 的待办清单");
        open.setToolTipText("翻到这一天，按天的待办列出来");
        open.addActionListener(a -> {
            jumpToDay(d);
            if (allCnt == 0) Main.notifyMsg(mm + " 没有待办", "这天一条待办都没有，可以直接新建");
        });
        JMenuItem copy = new JMenuItem("复制 " + mm + " 完成的 " + doneCnt + " 条");
        copy.setToolTipText(doneCnt == 0 ? "这天没有打勾的条目" : "把这天完成的条目连同日期放进剪贴板");
        copy.addActionListener(a -> copyDoneOfDay(d, mm));
        JMenuItem copyAll = new JMenuItem("复制 " + mm + " 那天的全部 " + allCnt + " 条");
        copyAll.setToolTipText(allCnt == 0 ? "这天没有待办" : "已完成的前面标 ✓，一起放进剪贴板");
        copyAll.addActionListener(a -> copyTodosOfDay(d, mm));
        pm.add(open);
        pm.add(copy);
        pm.add(copyAll);
        dotsMenu = pm;
        pm.show(weekDots, e.getX(), e.getY());
    }

    /** 这一天有几条待办（不限完成状态，不含归档） */
    private int todosOn(LocalDate d) {
        int n = 0;
        for (Todo t : todos) {
            if (t.text != null && t.text.endsWith("（已归档）")) continue;
            if (d.equals(parseDate(t.date))) n++;
        }
        return n;
    }

    /** 把某一天完成的条目录进剪贴板 */
    int copyDoneOfDay(LocalDate d, String mm) {
        java.util.List<String[]> rows = doneInRange(d, d);
        if (rows.isEmpty()) {
            Main.notifyMsg("这天没有完成的条目", mm + " 一条都没打勾");
            return 0;
        }
        StringBuilder sb = new StringBuilder();
        for (String[] r : rows) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(mm).append("   ").append(r[1]);
        }
        java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new java.awt.datatransfer.StringSelection(sb.toString()), null);
        Main.notifyMsg("已复制 " + rows.size() + " 条", mm + " 完成的条目已放进剪贴板");
        return rows.size();
    }

    /** 这一天全部的待办原文（含已完成的，跳过归档） */
    java.util.List<String> todoTextsOn(LocalDate d) {
        java.util.List<String> out = new java.util.ArrayList<String>();
        for (Todo t : todos) {
            if (t.text == null || t.text.endsWith("（已归档）")) continue;
            if (d.equals(parseDate(t.date))) out.add((t.done ? "✓ " : "") + t.text);
        }
        return out;
    }

    /** 把某一天全部待办原文放进剪贴板，已完成的前面标 ✓ */
    int copyTodosOfDay(LocalDate d, String mm) {
        java.util.List<String> txt = todoTextsOn(d);
        if (txt.isEmpty()) {
            Main.notifyMsg("这天没有待办", mm + " 一条都没有，可以直接新建");
            return 0;
        }
        StringBuilder sb = new StringBuilder();
        for (String s : txt) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(mm).append("   ").append(s);
        }
        java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new java.awt.datatransfer.StringSelection(sb.toString()), null);
        Main.notifyMsg("已复制 " + txt.size() + " 条", mm + " 当天的待办已放进剪贴板");
        return txt.size();
    }

    /** 打开"本周/上周/本月完成"清单：which = week / last / month */
    void openDoneList(String which) {
        LocalDate now = LocalDate.now();
        LocalDate from, to;
        String title;
        if ("last".equals(which)) {
            LocalDate mon = now.with(java.time.DayOfWeek.MONDAY).minusWeeks(1);
            from = mon;
            to = mon.plusDays(6);
            title = "上周完成";
        } else if ("today".equals(which)) {
            from = now;
            to = now;
            title = "今天做完";
        } else if ("month".equals(which)) {
            from = now.withDayOfMonth(1);
            to = now.withDayOfMonth(now.lengthOfMonth());
            title = "本月完成";
        } else {
            LocalDate mon = now.with(java.time.DayOfWeek.MONDAY);
            from = mon;
            to = mon.plusDays(6);
            title = "本周完成";
        }
        java.util.List<String[]> rows = new java.util.ArrayList<String[]>();
        for (Todo t : todos) {
            if (!t.done) continue;
            if (t.text != null && t.text.endsWith("（已归档）")) continue;
            LocalDate d = parseDate(t.date);
            if (d == null) continue;
            if (d.isBefore(from) || d.isAfter(to)) continue;
            rows.add(new String[]{t.text == null ? "" : t.text, d.toString()});
        }
        java.util.Collections.sort(rows, (x, y) -> y[1].compareTo(x[1]));   // 近的在前
        showDoneList(title + " · 共 " + rows.size() + " 条", doneSubTitle(), rows);
    }

    /** 清单窗口的副标题：本周/上周/本月完成数，外加一句与上周的差 */
    String doneSubTitle() {
        int w = doneInWeek(0), l = doneInWeek(1), m = doneInMonth();
        int diff = w - l;
        String trend = diff > 0 ? "比上周多 " + diff + " 条"
                : diff < 0 ? "比上周少 " + (-diff) + " 条"
                : "与上周持平";
        return "本周 " + w + " 条 · 上周 " + l + " 条 · 本月 " + m + " 条 · " + trend;
    }

    /** 完成清单弹窗（非模态，行可点跳那天） */
    void showDoneList(String title, String sub, java.util.List<String[]> rows) {
        java.awt.Window owner = javax.swing.SwingUtilities.getWindowAncestor(this);
        final JDialog dlg = new JDialog(owner, title);
        dlg.setModal(false);
        JPanel panel = doneListPanel(title, sub, rows, dlg);
        dlg.setContentPane(panel);
        dlg.pack();
        int h = Math.min(460, 176 + rows.size() * 28
                + Math.max(0, doneGroupCount(rows) - 1) * 12);   // 按天分组后每组抬头也要地方
        dlg.setSize(520, h);
        dlg.setLocationRelativeTo(owner);
        dlg.setVisible(true);
    }

    /** 清单本体（dialog 可为 null，供离屏截图） */
    JPanel doneListPanel(String title, String sub, java.util.List<String[]> rows, final JDialog dlg) {
        final JPanel outer = new JPanel(new BorderLayout(0, 6));
        outer.setOpaque(true);
        outer.setBackground(Theme.panelBg());
        outer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        final JLabel head = new JLabel(title);
        head.setFont(Ui.font(13, true));
        head.setForeground(Theme.text());
        head.setAlignmentX(Component.LEFT_ALIGNMENT);

        final JLabel subLbl = new JLabel(sub == null ? "" : sub);
        subLbl.setFont(Ui.font(10, false));
        subLbl.setForeground(Theme.muted());
        subLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        final JTextField q = new JTextField(doneQuery);
        q.setFont(Ui.font(11, false));
        q.setPreferredSize(new Dimension(230, 24));
        q.setToolTipText("输入关键词或日期即时筛选，比如 周一 / 10-22 / 10-2");
        JLabel qTag = new JLabel("搜：");
        qTag.setFont(Ui.font(11, false));
        qTag.setForeground(Theme.muted());
        JButton qClear = new JButton("清空");
        qClear.setFont(Ui.font(11, false));

        JPanel north = new JPanel();
        north.setOpaque(false);
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(head);
        north.add(subLbl);
        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 3));
        searchRow.setOpaque(false);
        searchRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        searchRow.add(qTag);
        searchRow.add(q);
        searchRow.add(qClear);
        north.add(searchRow);
        outer.add(north, BorderLayout.NORTH);

        final JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));

        final java.util.List<JPanel> rowPanels = new java.util.ArrayList<JPanel>();
        final java.util.List<LocalDate> rowDates = new java.util.ArrayList<LocalDate>();
        final int[] sel = {0};
        final java.util.Set<JPanel> pickedRows = new java.util.LinkedHashSet<JPanel>();
        final JButton[] undoPick = new JButton[1];
        final boolean[] armed = {false};
        final javax.swing.Timer disarm = new javax.swing.Timer(3000, new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent ev) {
                armed[0] = false;
                updatePickBtn(undoPick[0], pickedRows, false);
            }
        });
        disarm.setRepeats(false);

        final Runnable[] rebuildRef = new Runnable[1];
        final java.util.Set<String> collapsed = new java.util.HashSet<String>();
        final Runnable rebuild = new Runnable() {
            public void run() {
                String query = q.getText() == null ? "" : q.getText();
                doneQuery = query;
                list.removeAll();
                rowPanels.clear();
                rowDates.clear();
                int n = 0;
                java.util.Map<String, Integer> dayCount = new java.util.LinkedHashMap<String, Integer>();
                final java.util.List<String> dayOrder = new java.util.ArrayList<String>();
                final java.util.Map<String, java.util.List<String[]>> dayItems =
                        new java.util.LinkedHashMap<String, java.util.List<String[]>>();
                for (String[] r : rows) {
                    if (!doneHit(r[0], r[1], query)) continue;
                    LocalDate dd = parseDate(r[1]);
                    String key = dd == null ? r[1] : dd.format(DateTimeFormatter.ofPattern("MM-dd"));
                    Integer c0 = dayCount.get(key);
                    dayCount.put(key, c0 == null ? 1 : c0 + 1);
                    if (!dayOrder.contains(key)) dayOrder.add(key);
                    java.util.List<String[]> bi = dayItems.get(key);
                    if (bi == null) {
                        bi = new java.util.ArrayList<String[]>();
                        dayItems.put(key, bi);
                    }
                    bi.add(r);
                    n++;
                }
                final int totalRows = n;      // 本次清单的总条数，后面算“占 % ”要用，不能再被行计数改掉
                String prevDayKey = null;
                for (String[] r : rows) {
                    if (!doneHit(r[0], r[1], query)) continue;
                    final LocalDate d = parseDate(r[1]);
                    final String tText = r[0];
                    final String tDay = d == null ? r[1] : d.format(DateTimeFormatter.ofPattern("MM-dd"));
                    if (!tDay.equals(prevDayKey)) {              // 换了一天，先插一个分组头
                        boolean firstGroup = prevDayKey == null;
                        prevDayKey = tDay;
                        Integer c1 = dayCount.get(tDay);
                        final String keyForToggle = tDay;
                        final int cntForToggle = c1 == null ? 0 : c1;
                        final Runnable toggle = new Runnable() {
                            public void run() {
                                if (collapsed.contains(keyForToggle)) collapsed.remove(keyForToggle);
                                else collapsed.add(keyForToggle);
                                if (rebuildRef[0] != null) rebuildRef[0].run();
                            }
                        };
                        list.add(doneDayHeader(tDay, cntForToggle, firstGroup,
                                collapsed.contains(tDay), totalRows, toggle,
                                groupMenu(tDay, dayItems.get(tDay), dayOrder, collapsed, rebuildRef, dlg)));
                    }
                    if (collapsed.contains(tDay)) continue;      // 收起的那天，只留抬头
                    JPanel row = new JPanel(new BorderLayout(8, 0));
                    row.setOpaque(true);
                    row.setBackground(Theme.panelBg());
                    row.setAlignmentX(Component.LEFT_ALIGNMENT);
                    row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
                    row.setBorder(BorderFactory.createEmptyBorder(3, 2, 3, 2));
                    JLabel day = new JLabel("");
                    day.setFont(Ui.font(10, false));
                    day.setForeground(Theme.muted());
                    day.setPreferredSize(new Dimension(20, 18));   // 只留一点缩进，日期已在上方分组头
                    JLabel name = new JLabel(tText);
                    name.setFont(Ui.font(12, false));
                    name.setForeground(Theme.text());
                    row.add(day, BorderLayout.WEST);
                    row.add(name, BorderLayout.CENTER);
                    row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    row.setToolTipText(d == null ? "右键可退回待办" : "点一下跳到这天 · 右键可退回待办");

                    final JPopupMenu menu = new JPopupMenu();
                    if (d != null) {
                        JMenuItem jump = new JMenuItem("跳到 " + tDay);
                        jump.addActionListener(new java.awt.event.ActionListener() {
                            public void actionPerformed(java.awt.event.ActionEvent e) {
                                if (dlg != null) dlg.dispose();
                                jumpToDay(d);
                            }
                        });
                        menu.add(jump);
                    }
                    JMenuItem undo = new JMenuItem("退回待办");
                    undo.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent e) {
                            undoDone(tText, r[1]);
                            if (dlg != null) dlg.dispose();
                        }
                    });
                    menu.add(undo);
                    JMenuItem undoToday = new JMenuItem("退回并挪到今天");
                    undoToday.setToolTipText("改回未完成，同时把日期改成今天");
                    undoToday.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent e) {
                            undoDoneToToday(tText);
                            if (dlg != null) dlg.dispose();
                        }
                    });
                    menu.add(undoToday);
                    JMenuItem copyOne = new JMenuItem("复制这一条");
                    copyOne.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent e) {
                            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                                    new java.awt.datatransfer.StringSelection(tDay + " " + tText), null);
                        }
                    });
                    menu.add(copyOne);
                    row.addMouseListener(new java.awt.event.MouseAdapter() {
                        public void mouseClicked(java.awt.event.MouseEvent e) {
                            if (!javax.swing.SwingUtilities.isLeftMouseButton(e)) return;
                            if (e.isControlDown()) {          // Ctrl 点：勾上一起退回，不跳页
                                if (pickedRows.contains(row)) pickedRows.remove(row);
                                else pickedRows.add(row);
                                armed[0] = false;
                                disarm.stop();
                                paintDoneSel(rowPanels, sel[0], pickedRows);
                                updatePickBtn(undoPick[0], pickedRows, false);
                                return;
                            }
                            if (d == null) return;
                            if (dlg != null) dlg.dispose();
                            jumpToDay(d);
                        }

                        public void mousePressed(java.awt.event.MouseEvent e) {
                            popup(e);
                        }

                        public void mouseReleased(java.awt.event.MouseEvent e) {
                            popup(e);
                        }

                        private void popup(java.awt.event.MouseEvent e) {
                            if (e.isPopupTrigger()) menu.show(e.getComponent(), e.getX(), e.getY());
                        }
                    });
                    rowPanels.add(row);
                    rowDates.add(d);
                    list.add(row);
                    n++;
                }
                if (n == 0) {
                    JLabel empty = new JLabel(rows.isEmpty() ? "这段时间还没有完成的待办" : "没有匹配的条目");
                    empty.setFont(Ui.font(11, false));
                    empty.setForeground(Theme.muted());
                    empty.setAlignmentX(Component.LEFT_ALIGNMENT);
                    list.add(empty);
                }
                head.setText(title + (query.trim().isEmpty() ? "" : " · 筛出 " + n + " 条"));
                sel[0] = rowPanels.isEmpty() ? -1 : 0;
                pickedRows.clear();
                armed[0] = false;
                disarm.stop();
                paintDoneSel(rowPanels, sel[0], pickedRows);
                updatePickBtn(undoPick[0], pickedRows, false);
                list.revalidate();
                list.repaint();
                outer.revalidate();
                outer.repaint();
            }
        };

        JScrollPane sp = new JScrollPane(list);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getVerticalScrollBar().setUnitIncrement(14);
        outer.add(sp, BorderLayout.CENTER);

        JButton undoPickBtn = new JButton("退回选中的");
        undoPickBtn.setFont(Ui.font(11, false));
        undoPickBtn.setEnabled(false);
        undoPickBtn.setToolTipText("按住 Ctrl 点几行选中，再一起退回待办");
        undoPick[0] = undoPickBtn;
        undoPickBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                java.util.List<String> texts = new java.util.ArrayList<String>();
                for (JPanel r : pickedRows) {
                    if (r.getComponentCount() < 2) continue;
                    if (!(r.getComponent(1) instanceof JLabel)) continue;
                    texts.add(((JLabel) r.getComponent(1)).getText());
                }
                if (texts.isEmpty()) return;
                if (needUndoConfirm(texts.size()) && !armed[0]) {    // 超过 3 条先要一次确认
                    armed[0] = true;
                    updatePickBtn(undoPick[0], pickedRows, true);
                    disarm.restart();
                    return;
                }
                armed[0] = false;
                disarm.stop();
                int n = Store.markTodosUndone(texts);
                if (n <= 0) {
                    Main.notifyMsg("没找到这些条目", "可能已经改过或已归档，刷新后再看");
                    return;
                }
                loadTodos();
                refreshTodoList();
                Main.notifyMsg("已退回 " + n + " 条", "这些待办又回到了未完成");
                if (dlg != null) dlg.dispose();
            }
        });

        JPanel foot = new JPanel(new BorderLayout(6, 0));
        foot.setOpaque(false);
        JPanel footLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        footLeft.setOpaque(false);
        footLeft.add(undoPickBtn);
        JPanel footRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        footRight.setOpaque(false);
        final JButton copy = new JButton("复制这几条");
        copy.setFont(Ui.font(11, false));
        copy.setToolTipText("把整份清单按「MM-dd 正文」多行放进剪贴板");
        copy.setEnabled(!rows.isEmpty());
        copy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                String t = copyDoneText(rows);
                java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                        .setContents(new java.awt.datatransfer.StringSelection(t), null);
                copy.setText("已复制 " + rows.size() + " 条");
                javax.swing.Timer back = new javax.swing.Timer(1500, new java.awt.event.ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent ev) {
                        copy.setText("复制这几条");
                    }
                });
                back.setRepeats(false);
                back.start();
            }
        });
        footRight.add(copy);
        JButton close = new JButton("关闭");
        close.setFont(Ui.font(11, false));
        close.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (dlg != null) dlg.dispose();
            }
        });
        footRight.add(close);
        foot.add(footLeft, BorderLayout.WEST);
        foot.add(footRight, BorderLayout.EAST);
        outer.add(foot, BorderLayout.SOUTH);

        q.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                rebuild.run();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                rebuild.run();
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                rebuild.run();
            }
        });
        qClear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                q.setText("");
                q.requestFocusInWindow();
            }
        });

        bindDoneKeys(outer, rowPanels, rowDates, sel, dlg);
        rebuildRef[0] = rebuild;               // 分组头的点击要能重跑这一遍
        rebuild.run();
        return outer;
    }

    /** 清单搜索命中：空 query 全命中；月-日整段比对；单个数字按月或日；其余文字子串 */
    boolean doneHit(String text, String date, String query) {
        String q = query == null ? "" : query.trim().toLowerCase();
        if (q.isEmpty()) return true;
        String d = date == null ? "" : date.trim();
        String md = d.length() >= 10 ? d.substring(5) : d;
        if (q.matches("\\d{1,2}-\\d{1,2}")) {
            int[] want = splitMonthDay(q);
            int[] have = splitMonthDay(md);
            return want != null && have != null && want[0] == have[0] && want[1] == have[1];
        }
        if (q.matches("\\d{1,2}")) {
            int[] have = splitMonthDay(md);
            if (have == null) return false;
            int n = Integer.parseInt(q);
            return n == have[0] || n == have[1];
        }
        String t = text == null ? "" : text.toLowerCase();
        return t.contains(q) || md.contains(q) || d.contains(q);
    }

    /** "10-2" / "10-02" → {10, 2}，解析失败返回 null */
    private int[] splitMonthDay(String md) {
        if (md == null) return null;
        String s = md.trim();
        if (s.startsWith("-")) s = s.substring(1);
        String[] p = s.split("-");
        if (p.length < 2) return null;
        try {
            return new int[]{Integer.parseInt(p[0].trim()), Integer.parseInt(p[1].trim())};
        } catch (Exception e) {
            return null;
        }
    }

    /** 把一条已完成的待办退回未完成，并重刷界面 */
    int undoDone(String text, String date) {
        if (!Store.markTodoUndone(text == null ? "" : text.trim())) {
            Main.notifyMsg("没找到这条", "可能已经改过或已归档，刷新后再看");
            return 0;
        }
        loadTodos();
        refreshTodoList();
        Main.notifyMsg("已退回待办", text + " 又回到了未完成");
        return 1;
    }

    /** 退回未完成，同时把日期挪到今天 */
    int undoDoneToToday(String text) {
        String today = DAY.format(new Date());
        if (!Store.markTodoUndoneAndMove(text == null ? "" : text.trim(), today)) {
            Main.notifyMsg("没找到这条", "可能已经改过或已归档，刷新后再看");
            return 0;
        }
        loadTodos();
        refreshTodoList();
        Main.notifyMsg("已退回并挪到今天", text + " 现在算今天的待办");
        return 1;
    }

    /** 清单里键盘选中行的底色 */
    void paintDoneSel(java.util.List<JPanel> rowPanels, int sel) {
        paintDoneSel(rowPanels, sel, new java.util.LinkedHashSet<JPanel>());
    }

    /** 选中行浅蓝，Ctrl 勾上的行稍深一档 */
    void paintDoneSel(java.util.List<JPanel> rowPanels, int sel, java.util.Set<JPanel> picked) {
        for (int i = 0; i < rowPanels.size(); i++) {
            JPanel r = rowPanels.get(i);
            Color c = picked.contains(r) ? new Color(0xBFDBFE)
                    : (i == sel ? new Color(0xDBEAFE) : Theme.panelBg());
            r.setBackground(c);
            r.repaint();
        }
    }

    /** 清单里一共几个日期分组（弹窗算高度用） */
    int doneGroupCount(java.util.List<String[]> rows) {
        java.util.Set<String> k = new java.util.HashSet<String>();
        for (String[] r : rows) {
            LocalDate d = parseDate(r[1]);
            k.add(d == null ? r[1] : d.format(DateTimeFormatter.ofPattern("MM-dd")));
        }
        return k.size();
    }

    /** 清单里按天分组的抬头：日期 + 这天几条，上面一道浅线（第一组不画线） */
    private JPanel doneDayHeader(String dayKey, int cnt, boolean firstGroup,
                                 boolean collapsed, int total, final Runnable onClick,
                                 JPopupMenu groupMenu) {
        JPanel h = new JPanel(new BorderLayout(6, 0));
        h.setOpaque(false);
        h.setAlignmentX(Component.LEFT_ALIGNMENT);
        h.setMaximumSize(new Dimension(Integer.MAX_VALUE, firstGroup ? 22 : 28));
        h.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(firstGroup ? 0 : 1, 0, 0, 0, new Color(0xC7D1E2)),
                BorderFactory.createEmptyBorder(firstGroup ? 4 : 10, 2, 2, 2)));
        JLabel d = new JLabel(dayKey);
        d.setFont(Ui.font(10, true));
        d.setForeground(Ui.PRIMARY);
        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.X_AXIS));
        JLabel bar = new JLabel();                 // 日期前一小段蓝竖条，扫一眼就知道换天了
        bar.setOpaque(true);
        bar.setBackground(Ui.PRIMARY);
        bar.setPreferredSize(new Dimension(3, 12));
        bar.setMinimumSize(new Dimension(3, 12));
        bar.setMaximumSize(new Dimension(3, 12));
        left.add(bar);
        left.add(Box.createHorizontalStrut(5));
        left.add(d);
        JPanel east = new JPanel();
        east.setOpaque(false);
        east.setLayout(new BoxLayout(east, BoxLayout.X_AXIS));
        JLabel c = new JLabel(collapsed
                ? "已收起 · 展开"
                : cnt + " 条 · 占 " + (total <= 0 ? 0 : (int) Math.round(cnt * 100.0 / total)) + "% · 收起");
        c.setFont(Ui.font(9, false));
        c.setForeground(Theme.muted());
        east.add(c);
        h.add(left, BorderLayout.WEST);
        h.add(east, BorderLayout.EAST);
        h.setToolTipText(dayKey + " · 完成 " + cnt + " 条"
                + (onClick == null ? "" : collapsed ? " · 点一下展开这一天" : " · 点一下收起这一天"));
        if (onClick != null) {
            h.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            h.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (!javax.swing.SwingUtilities.isLeftMouseButton(e)) return;
                    onClick.run();
                }
            });
        }
        if (groupMenu != null) h.setComponentPopupMenu(groupMenu);
        return h;
    }

    /** 分组抬头的右键菜单：切换收起、只留这天展开、复制这天全部、把这天全部退回 */
    private JPopupMenu groupMenu(final String day, java.util.List<String[]> items,
                                 final java.util.List<String> allDays,
                                 final java.util.Set<String> collapsed, final Runnable[] rebuildRef,
                                 final JDialog dlg) {
        JPopupMenu m = new JPopupMenu();
        final boolean shut = collapsed.contains(day);
        JMenuItem toggle = new JMenuItem(shut ? "展开这一天" : "收起这一天");
        toggle.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (shut) collapsed.remove(day);
                else collapsed.add(day);
                if (rebuildRef[0] != null) rebuildRef[0].run();
            }
        });
        m.add(toggle);
        JMenuItem only = new JMenuItem("只展开这一天");
        only.setToolTipText("其它几天都收起来，只留这一天看明细");
        only.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                collapsed.remove(day);
                for (String k : allDays) {
                    if (!k.equals(day)) collapsed.add(k);
                }
                if (rebuildRef[0] != null) rebuildRef[0].run();
            }
        });
        m.add(only);
        JMenuItem copy = new JMenuItem("复制这天全部 " + (items == null ? 0 : items.size()) + " 条");
        copy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                StringBuilder sb = new StringBuilder();
                if (items != null) {
                    for (String[] r : items) {
                        if (sb.length() > 0) sb.append('\n');
                        sb.append(day).append(' ').append(r[0]);
                    }
                }
                java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                        new java.awt.datatransfer.StringSelection(sb.toString()), null);
            }
        });
        m.add(copy);
        final int n = items == null ? 0 : items.size();
        final JMenuItem back = new JMenuItem("把这一天全部退回 " + n + " 条");
        back.setToolTipText(n > 3 ? "一次退 " + n + " 条有点多，会再要你确认一下" : "这一天改回未完成，日期不动");
        back.setEnabled(n > 0);
        final boolean[] armedBack = {false};
        final javax.swing.Timer disarmBack = new javax.swing.Timer(3000, new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                armedBack[0] = false;
                back.setText("把这一天全部退回 " + n + " 条");
                back.setForeground(null);
            }
        });
        disarmBack.setRepeats(false);
        back.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (items == null || items.isEmpty()) return;
                if (needUndoConfirm(n) && !armedBack[0]) {          // 超过 3 条，先问一句
                    armedBack[0] = true;
                    back.setText("再点一下确认退回 " + n + " 条");
                    back.setForeground(new Color(0xB45309));
                    disarmBack.restart();
                    return;
                }
                java.util.List<String> texts = new java.util.ArrayList<String>();
                for (String[] r : items) texts.add(r[0]);
                int done = Store.markTodosUndone(texts);
                Main.notifyMsg("已退回", done + " 条改回未完成，日期没动");
                refresh();
                if (dlg != null) dlg.dispose();                 // 清单里的行已经作废，直接关掉
                else if (rebuildRef[0] != null) rebuildRef[0].run();
            }
        });
        m.add(back);
        return m;
    }

    /** 底部"退回选中的 N 条"按钮：没选中就置灰 */
    void updatePickBtn(JButton b, java.util.Set<JPanel> picked) {
        updatePickBtn(b, picked, false);
    }

    /** armed = 已经在问"确认退回 N 条？"那一步 */
    void updatePickBtn(JButton b, java.util.Set<JPanel> picked, boolean armed) {
        if (b == null) return;
        int n = picked == null ? 0 : picked.size();
        if (armed && n > 0) {
            b.setText("确认退回 " + n + " 条？");
            b.setForeground(new Color(0xB45309));      // 警示橙：这一步是真的要退了
            b.setEnabled(true);
            b.setToolTipText("一次退 " + n + " 条有点多 · 再点一下才真的退回");
            return;
        }
        b.setForeground(null);
        b.setText(n == 0 ? "退回选中的" : "退回选中的 " + n + " 条");
        b.setEnabled(n > 0);
        b.setToolTipText(n == 0 ? "按住 Ctrl 点几行选中，再一起退回待办"
                : (n > 3 ? "一次超过 3 条，会再要你确认一下" : "把这 " + n + " 条一起退回未完成"));
    }

    /** 选中的条数超过 3 条才要二次确认，防手滑 */
    boolean needUndoConfirm(int n) {
        return n > 3;
    }

    /** ↑↓ 选行 · 回车跳那天 · Esc 收起（清单被搜索筛成空时也照样能收起来） */
    void bindDoneKeys(JPanel p, final java.util.List<JPanel> rowPanels,
                      final java.util.List<LocalDate> rowDates, final int[] sel, final JDialog dlg) {
        p.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("UP"), "doneUp");
        p.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("DOWN"), "doneDown");
        p.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ENTER"), "doneOpen");
        p.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "doneEsc");
        p.getActionMap().put("doneUp", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (rowPanels.isEmpty()) return;
                sel[0] = Math.max(0, Math.max(0, sel[0]) - 1);
                paintDoneSel(rowPanels, sel[0]);
            }
        });
        p.getActionMap().put("doneDown", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (rowPanels.isEmpty()) return;
                sel[0] = Math.min(rowPanels.size() - 1, Math.max(0, sel[0]) + 1);
                paintDoneSel(rowPanels, sel[0]);
            }
        });
        p.getActionMap().put("doneOpen", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (rowPanels.isEmpty() || sel[0] < 0 || sel[0] >= rowDates.size()) return;
                LocalDate d = rowDates.get(sel[0]);
                if (d == null) return;
                if (dlg != null) dlg.dispose();
                jumpToDay(d);
            }
        });
        p.getActionMap().put("doneEsc", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (dlg != null) dlg.dispose();
            }
        });
    }

    /** 清单复制文本：MM-dd 正文，一行一条 */
    String copyDoneText(java.util.List<String[]> rows) {
        StringBuilder sb = new StringBuilder();
        for (String[] r : rows) {
            String md = r[1] != null && r[1].length() >= 10 ? r[1].substring(5, 10) : r[1];
            if (sb.length() > 0) sb.append("\n");
            sb.append(md).append(' ').append(r[0]);
        }
        return sb.toString();
    }

    /** 归档里的一次性全部翻回今天 */
    /** 把当前筛出来的归档条目一次全部放进剪贴板（多行，带原日期），返回条数 */
    int copyAllArchived() {
        java.util.List<Todo> list = filteredArchived();
        if (list.isEmpty()) return 0;
        StringBuilder sb = new StringBuilder();
        for (Todo t : list) {
            if (sb.length() > 0) sb.append("\n");
            String s = t.text == null ? "" : t.text;
            s = s.replace("（已归档）", "").trim();
            String d = t.date == null ? "" : t.date.trim();
            LocalDate day = null;
            if (!d.isEmpty() && !"-".equals(d)) {
                try {
                    day = LocalDate.parse(d);
                } catch (Exception ignore) {
                    day = null;
                }
            }
            if (day != null) sb.append(day.format(DateTimeFormatter.ofPattern("MM-dd"))).append("  ");
            sb.append(s);
        }
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(sb.toString()), null);
        } catch (Exception e) {
            return 0;
        }
        return list.size();
    }

    /** 全部过期未做（不分档），供小窗复制用 */
    java.util.List<Todo> allOverdueTodos() {
        java.util.List<Todo> out = new java.util.ArrayList<Todo>();
        LocalDate today = LocalDate.now();
        for (Todo t : todos) {
            if (t.done) continue;
            String raw = t.date == null ? "" : t.date.trim();
            if (raw.isEmpty() || "-".equals(raw)) continue;
            LocalDate d = parseDate(raw);
            if (d != null && d.isBefore(today)) out.add(t);
        }
        return out;
    }

    /** 过期未做的都另存一条到明天，原条留在原日期不动；返回复制的条数 */
    int copyOverdueToTomorrow() {
        java.util.List<Todo> list = allOverdueTodos();
        if (list.isEmpty()) return 0;
        java.time.LocalDate tomorrow = LocalDate.now().plusDays(1);
        String key = DAY.format(java.util.Date.from(
                tomorrow.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));
        int n = 0;
        for (Todo t : list) {
            todos.add(new Todo(false, t.text, key));
            n++;
        }
        saveTodos();
        refreshTodoList();
        return n;
    }

    /** 过期/未来筛选行的右键菜单：整批复制到明天、复制文本、全部挪到今天 */
    private JPopupMenu overdueBarMenu() {
        JPopupMenu m = new JPopupMenu();
        JMenuItem copyTo = new JMenuItem("把过期这几条一起复制到明天");
        copyTo.setToolTipText("原条留在原日期不动，另外各存一条放到明天");
        copyTo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int n = copyOverdueToTomorrow();
                if (n > 0) {
                    java.time.LocalDate t = LocalDate.now().plusDays(1);
                    Main.notifyMsg("已复制到明天", n + " 条过期待办另存到 "
                            + t.toString().substring(5) + "，原条没动");
                } else {
                    Main.notifyMsg("没有可复制的过期条目", "现在的待办都没有过期");
                }
            }
        });
        JMenuItem copyTxt = new JMenuItem("复制过期条目的文字");
        copyTxt.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int n = copyOverdueTexts();
                if (n > 0) Main.notifyMsg("已复制", n + " 条过期条目（带原日期）");
                else Main.notifyMsg("没有可复制的过期条目", "现在的待办都没有过期");
            }
        });
        JMenuItem move = new JMenuItem("全部挪到今天");
        move.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int n = moveOverdueToToday();
                if (n > 0) Main.notifyMsg("已挪到今天", n + " 条过期待办改成了今天");
                else Main.notifyMsg("没有可挪的过期条目", "现在的待办都没有过期");
            }
        });
        m.add(copyTo);
        m.add(copyTxt);
        m.addSeparator();
        m.add(move);
        return m;
    }

    /** 把这批过期标题连同原日期放进剪贴板，返回条数 */
    int copyOverdueTexts() {
        java.util.List<Todo> list = allOverdueTodos();
        if (list.isEmpty()) return 0;
        StringBuilder sb = new StringBuilder();
        for (Todo t : list) {
            if (sb.length() > 0) sb.append("\n");
            String d = t.date == null ? "" : t.date.trim();
            LocalDate day = null;
            try {
                day = LocalDate.parse(d);
            } catch (Exception ignore) {
                day = null;
            }
            if (day != null) sb.append(day.format(DateTimeFormatter.ofPattern("MM-dd"))).append("  ");
            sb.append(t.text == null ? "" : t.text.trim());
        }
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(sb.toString()), null);
        } catch (Exception e) {
            return 0;
        }
        return list.size();
    }

    int recoverAllArchived() {
        java.util.List<Todo> list = archivedTodos();
        if (list.isEmpty()) return 0;
        String today = DAY.format(new Date());
        for (Todo t : list) {
            t.done = false;
            if (t.text != null && t.text.endsWith("（已归档）")) {
                t.text = t.text.substring(0, t.text.length() - "（已归档）".length());
            }
            t.date = today;
        }
        saveTodos();
        refreshTodoList();
        return list.size();
    }

    /** 归档里的一次性全部删除（有确认框） */
    int deleteAllArchived() {
        return deleteAllArchived(true);
    }

    int deleteAllArchived(boolean confirm) {
        java.util.List<Todo> list = archivedTodos();
        if (list.isEmpty()) return 0;
        if (confirm) {
            int c = JOptionPane.showConfirmDialog(this,
                    "把这 " + list.size() + " 条归档条目彻底删掉？删了就找不回来",
                    "清空归档", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (c != JOptionPane.YES_OPTION) return 0;
        }
        todos.removeAll(list);
        lastArchivedDeleted.clear();
        lastArchivedDeleted.addAll(list);      // 留个快照，删错了能翻回来
        saveTodos();
        refreshTodoList();
        return list.size();
    }

    /** 清空归档留的撤销入口：把刚删掉的条目加回去 */
    int undoDeleteArchived() {
        if (lastArchivedDeleted.isEmpty()) return 0;
        int n = lastArchivedDeleted.size();
        todos.addAll(lastArchivedDeleted);
        lastArchivedDeleted.clear();
        saveTodos();
        refreshTodoList();
        return n;
    }

    /** 归档里删掉单条（彻底删除，不进可撤销快照） */
    boolean deleteArchived(Todo t) {
        if (t == null) return false;
        boolean removed = todos.remove(t);
        if (removed) {
            lastArchivedDeleted.remove(t);   // 已彻底删掉的，不再参与"撤销清空"
            saveTodos();
            refreshTodoList();
        }
        return removed;
    }

    /** 把归档条目翻回来：回到今天待办，去掉"已归档"后缀 */
    void recoverArchived(Todo t) {
        recoverArchivedTo(t, java.time.LocalDate.now());
    }

    /** 翻回归档并直接落到指定日期（归档里"恢复并改期到…"用） */
    void recoverArchivedTo(Todo t, java.time.LocalDate day) {
        if (t == null || day == null) return;
        t.done = false;
        if (t.text != null && t.text.endsWith("（已归档）")) {
            t.text = t.text.substring(0, t.text.length() - "（已归档）".length());
        }
        t.date = DAY.format(java.util.Date.from(
                day.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));
        saveTodos();
        refreshTodoList();
        Main.notifyMsg("已从归档翻回", t.text + " 已放到 "
                + day.format(DateTimeFormatter.ofPattern("MM-dd")));
    }

    /** 刚挪过就留一个撤销入口，点一下把日期放回去 */
    private void addUndoChip() {
        if (lastMoved.isEmpty()) return;
        final int n = lastMoved.size();
        JLabel chip = new JLabel("撤销挪动 " + n + " 条");
        chip.setFont(Ui.font(10, true));
        chip.setForeground(new Color(0x64748B));
        chip.setBorder(BorderFactory.createEmptyBorder(1, 8, 1, 4));
        chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        chip.setToolTipText("把刚才挪到今天的 " + n + " 条放回原来的日期");
        chip.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int back = undoMoveOverdue();
                if (back > 0) {
                    updateFuture();
                    Main.notifyMsg("已撤销", back + " 条待办回到了原来的日期");
                }
            }
        });
        futureBar.add(chip);
    }

    /** 过期段末尾的"全部挪到今天"，一键把逾期的都拖回今天 */
    private void addMoveAllChip(int overTotal) {
        if (overTotal <= 0) return;
        JLabel chip = new JLabel("全部挪到今天");
        chip.setFont(Ui.font(10, true));
        chip.setForeground(Ui.PRIMARY);
        chip.setBorder(BorderFactory.createEmptyBorder(1, 8, 1, 4));
        chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        chip.setToolTipText("把 " + overTotal + " 条过期未做的都改成今天");
        chip.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                chip.setForeground(Ui.PRIMARY.darker());
            }

            public void mouseExited(java.awt.event.MouseEvent e) {
                chip.setForeground(Ui.PRIMARY);
            }

            public void mouseClicked(java.awt.event.MouseEvent e) {
                int n = overdueByDayTotal();
                if (n == 0) return;
                int r = JOptionPane.showConfirmDialog(OverviewPanel.this,
                        "把 " + n + " 条过期未做的待办都改成今天？",
                        "全部挪到今天", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (r != JOptionPane.YES_OPTION) return;
                int moved = moveOverdueToToday();
                if (moved > 0) Main.notifyMsg("已挪到今天", moved + " 条过期待办改成了今天");
            }
        });
        futureBar.add(chip);
    }

    private int overdueByDayTotal() {
        int n = 0;
        for (Integer v : overdueByDay().values()) n += v;
        return n;
    }

    /** 把所有过期未做的挪到今天，返回挪了几条（同时记住原日期以便撤销） */
    int moveOverdueToToday() {
        LocalDate today = LocalDate.now();
        String key = DAY.format(java.util.Date.from(
                today.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));
        java.util.List<Todo> movedNow = new java.util.ArrayList<Todo>();
        java.util.List<String> datesNow = new java.util.ArrayList<String>();
        for (Todo t : todos) {
            if (t.done) continue;
            String raw = t.date == null ? "" : t.date.trim();
            if (raw.isEmpty() || "-".equals(raw)) continue;
            if (parseDate(raw).isBefore(today)) {
                movedNow.add(t);
                datesNow.add(t.date);
                t.date = key;
            }
        }
        if (!movedNow.isEmpty()) {
            /* 只有真的挪了才覆盖撤销快照：连续点两次不会把上一次的撤销弄丢 */
            lastMoved.clear();
            lastMovedDates.clear();
            lastMoved.addAll(movedNow);
            lastMovedDates.addAll(datesNow);
            saveTodos();
            refreshTodoList();
        }
        return movedNow.size();
    }

    /** 撤销上一次"全部挪到今天"，把日期放回去 */
    int undoMoveOverdue() {
        int n = 0;
        for (int i = 0; i < lastMoved.size() && i < lastMovedDates.size(); i++) {
            lastMoved.get(i).date = lastMovedDates.get(i);
            n++;
        }
        lastMoved.clear();
        lastMovedDates.clear();
        if (n > 0) {
            saveTodos();
            refreshTodoList();
        }
        return n;
    }

    /** 归档阈值：默认 30 天，可在设置里改成 15/60/90 */
    static int archiveDays() {
        try {
            int v = Integer.parseInt(MiniBoard.readSetting("archiveDays").trim());
            if (v >= 1 && v <= 365) return v;
        } catch (Exception ignored) {
        }
        return 30;
    }

    /** 超过阈值没动的过期条目自动归档（标完成 + 后缀），不再提醒 */
    int archiveStale() {
        LocalDate today = LocalDate.now();
        int limit = archiveDays();
        int n = 0;
        for (Todo t : todos) {
            if (t.done) continue;
            String raw = t.date == null ? "" : t.date.trim();
            if (raw.isEmpty() || "-".equals(raw)) continue;
            LocalDate d;
            try {
                d = java.time.LocalDate.parse(raw);
            } catch (Exception e) {
                continue;
            }
            if (java.time.temporal.ChronoUnit.DAYS.between(d, today) > limit) {
                t.done = true;
                if (!t.text.endsWith("（已归档）")) t.text = t.text + "（已归档）";
                n++;
            }
        }
        if (n > 0) saveTodos();
        return n;
    }

    /** 1=1-2 天，2=3-6 天，3=7 天以上 */
    private static int levelOf(long age) {
        if (age >= 7) return 3;
        if (age >= 3) return 2;
        return 1;
    }

    private static String levelName(int level) {
        if (level == 9) return "全部过期未做";
        if (level == 3) return "很久没动";
        if (level == 2) return "该处理了";
        return "过期未做";
    }

    /** 过期区档位名，点一下只看这一档的所有条目 */
    private JLabel levelFlag(String text, final Color color, final int level) {
        final boolean on = overdueLevel == level;
        JLabel l = new JLabel(on ? text + " · 收起" : text);
        l.setFont(Ui.font(10, true));
        l.setForeground(color);
        l.setOpaque(on);
        l.setBackground(on ? new Color(0xFEECEC) : new Color(0x00000000));
        l.setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 4));
        l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        l.setToolTipText(on ? "再点一下取消筛选" : "点一下只看这一档");
        l.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                overdueLevel = (overdueLevel == level) ? 0 : level;
                overdueFilter = null;
                saveViewState();
                refreshTodoList();
            }
        });
        return l;
    }

    private static final Color OVERDUE = new Color(0xDC2626);    private static final Color DEEP = new Color(0x991B1B);
    private static final Color DEEPER = new Color(0x7F1D1D);

    private JLabel flagLabel(String text, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(Ui.font(10, true));
        l.setForeground(color);
        l.setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 4));
        return l;
    }

    private JLabel tagLabel(String text, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(Ui.font(10, true));
        l.setForeground(color);
        l.setBorder(BorderFactory.createEmptyBorder(1, 6, 1, 6));
        return l;
    }

    /** 某一档里除首个日期之外还剩下的条目：写成"另有 N 条 11-02"，点一下只看那天 */
    private JLabel restLabel(java.util.List<LocalDate> days, java.util.Map<LocalDate, Integer> src,
                            Color color, String levelName) {
        int rest = 0;
        for (LocalDate day : days) rest += src.get(day);
        final LocalDate first = days.get(0);
        JLabel l = tagLabel("另有 " + rest + " 条 "
                + first.format(DateTimeFormatter.ofPattern("MM-dd")), color);
        StringBuilder all = new StringBuilder();
        for (LocalDate day : days) {
            if (all.length() > 0) all.append("、");
            all.append(day.format(DateTimeFormatter.ofPattern("MM-dd")));
        }
        l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        l.setToolTipText(levelName + "这档另有 " + rest + " 条，落在 " + all
                + " · 点一下只看 " + first.format(DateTimeFormatter.ofPattern("MM-dd")));
        l.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (!javax.swing.SwingUtilities.isLeftMouseButton(e)) return;
                overdueFilter = first;
                overdueLevel = 0;
                viewDate = null;
                multiSelect = false;
                picked.clear();
                saveViewState();
                refreshTodoList();
            }
        });
        return l;
    }

    private JLabel sepLabel() {
        JLabel l = new JLabel("｜");
        l.setFont(Ui.font(10, false));
        l.setForeground(Theme.muted());
        l.setBorder(BorderFactory.createEmptyBorder(1, 2, 1, 2));
        return l;
    }

    /** 过期筛选行收尾：把末尾悬着的"｜"去掉，再统一做可见性刷新 */
    private void finishFutureBar() {
        int n = futureBar.getComponentCount();
        while (n > 0) {
            java.awt.Component c = futureBar.getComponent(n - 1);
            if (c instanceof JLabel && "｜".equals(((JLabel) c).getText())) {
                futureBar.remove(n - 1);
                n--;
            } else {
                break;
            }
        }
        futureBar.setVisible(futureBar.getComponentCount() > 0);
        futureBar.revalidate();
        futureBar.repaint();
    }

    /** 一个小标签：MM-dd · N 条，点一下切到那天的列表 */
    /** 过期区专用的日期 chip：点一下只列这天的过期条目，再点一下取消 */
    private JLabel overdueChip(final LocalDate day, int count, final Color color, String tip) {
        final boolean on = overdueFilter != null && overdueFilter.equals(day);
        JLabel chip = new JLabel(day.format(DateTimeFormatter.ofPattern("MM-dd"))
                + " · " + count + " 条" + (on ? " · 收起" : ""));
        chip.setFont(Ui.font(10, true));
        chip.setForeground(color);
        chip.setOpaque(on);
        chip.setBackground(on ? new Color(0xFEECEC) : new Color(0x00000000));
        chip.setBorder(BorderFactory.createEmptyBorder(1, 6, 1, 6));
        chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        chip.setToolTipText(on ? "再点一下取消筛选" : tip);
        chip.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent ev) {
                chip.setForeground(color.darker());
            }

            public void mouseExited(java.awt.event.MouseEvent ev) {
                chip.setForeground(color);
            }

            public void mouseClicked(java.awt.event.MouseEvent ev) {
                overdueFilter = (overdueFilter != null && overdueFilter.equals(day)) ? null : day;
                overdueLevel = 0;
                if (overdueFilter != null) viewDate = null;
                saveViewState();
                refreshTodoList();
            }
        });
        return chip;
    }

    private JLabel dayChip(final LocalDate day, int count, final Color color, String tip) {
        JLabel chip = new JLabel(day.format(DateTimeFormatter.ofPattern("MM-dd"))
                + " · " + count + " 条");
        chip.setFont(Ui.font(10, true));
        chip.setForeground(color);
        chip.setBorder(BorderFactory.createEmptyBorder(1, 6, 1, 6));
        chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        chip.setToolTipText(tip);
        chip.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent ev) {
                chip.setForeground(color.darker());
            }

            public void mouseExited(java.awt.event.MouseEvent ev) {
                chip.setForeground(color);
            }

            public void mouseClicked(java.awt.event.MouseEvent ev) {
                showDate(day);
            }
        });
        return chip;
    }

    private void updateTodoStat() {
        int thisWeek = doneInWeek(0);
        int lastWeek = doneInWeek(1);
        int thisMonth = doneInMonth();
        int todayCnt = doneToday();
        if (todayDone != null) {
            todayDone.setText(todayCnt <= 0 ? "今天还没勾" : "今天做完 " + todayCnt + " 条");
            todayDone.setForeground(todayCnt <= 0 ? Theme.muted() : Ui.PRIMARY);
            todayDone.setVisible(viewDate == null && overdueFilter == null && overdueLevel == 0 && !multiSelect);
            todayDone.setToolTipText(todayCnt <= 0
                    ? "今天勾掉的条数 · 勾完一条这里就会涨"
                    : "今天勾掉 " + todayCnt + " 条 · 点一下列出今天完成的这几条");
        }
        if (paceBar != null) {
            int diff = thisWeek - lastWeek;
            String trend = diff > 0 ? "比上周多 " + diff + " 条"
                    : diff < 0 ? "比上周少 " + (-diff) + " 条"
                    : "与上周持平";
            int streak = streakUpWeeks();
            String base = "本周 " + thisWeek + " 条 · 上周 " + lastWeek + " 条 · 本月 " + thisMonth + " 条";
            // 连续上涨时“连 N 周↑”已经把趋势说完了，这里不再重复写“比上周多 N 条”，
            // 把这一行剩下的宽度让给后面的“空 N 天”
            paceFull = streak >= 2 ? base : base + " · " + trend;
            // 待办卡窄的时候（窗口压小 / 同时有“连 N 周↑”和“空 N 天”）整行放不下，
            // 按三级降级，保证不把文字裁掉
            paceShort = "本周 " + thisWeek + " · 上周 " + lastWeek + " · 本月 " + thisMonth;
            paceMini = thisWeek + " / " + lastWeek + " / " + thisMonth;
            paceTiny = thisWeek + "/" + lastWeek;
            applyPaceText();
            if (streakLbl != null) {
                if (streak >= 2) {
                    streakLbl.setText("连 " + streak + " 周↑");
                    streakLbl.setVisible(true);
                    streakLbl.setToolTipText("已经连续 " + streak + " 周比上一周做得多了 · 上周 " + lastWeek
                            + " 条，这周 " + thisWeek + " 条");
                } else {
                    streakLbl.setText(" ");
                    streakLbl.setVisible(false);
                }
            }
            paceBar.setToolTipText("完成节奏 · 本周 " + thisWeek + " 条，上周 " + lastWeek + " 条，本月 "
                    + thisMonth + " 条 · 点一下列出本周完成的那几条");
            if (gapLbl != null) {
                java.util.List<Integer> idle = weekIdleDays();
                boolean plain = viewDate == null && overdueFilter == null && overdueLevel == 0 && !multiSelect;
                if (idle.isEmpty() || !plain) {
                    gapLbl.setText(" ");
                    gapLbl.setVisible(false);
                    gapLbl.setCursor(Cursor.getDefaultCursor());
                } else {
                    String[] names = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < idle.size(); i++) {
                        if (i > 0) sb.append("、");
                        sb.append(names[idle.get(i)]);
                    }
                    int[] cnts = weekDoneCounts();
                    int todayIdx = LocalDate.now().getDayOfWeek().getValue() - 1;
                    StringBuilder week = new StringBuilder();
                    int sum = 0;
                    for (int i = 0; i <= todayIdx && i < 7; i++) {
                        if (i > 0) week.append(" · ");
                        week.append(names[i]).append(" ").append(cnts[i]);
                        sum += cnts[i];
                    }
                    gapLbl.setText("空 " + idle.size() + " 天");
                    gapLbl.setToolTipText("周一到今天，" + sb + " 一条都没完成（本周共 " + sum + " 条："
                            + week + "）· 点一下跳到" + names[idle.get(0)]);
                    gapLbl.setVisible(true);
                    gapLbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    gapLbl.setVisible(true);
                    gapLbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                }
            }
            if (weekDots != null) weekDots.repaint();
            applyPaceText();      // “空 N 天”刚刚显/隐，占用宽度变了，重新选一次文案
        }
        if (multiSelect) {
            todoStat.setText(bulkRangeName() + " · 勾选后一起改期 / 删除");
            return;
        }
        java.util.List<Todo> list = todosFor(viewDate);
        int open = 0;
        for (Todo t : list) if (!t.done) open++;
        String base = list.isEmpty() ? "" : "共 " + list.size() + " 项";
        if (weekDone != null) {
            weekDone.setText("本周" + thisWeek + "·上周" + lastWeek + "·本月" + thisMonth);
            weekDone.setToolTipText("这一周完成了 " + thisWeek + " 条"
                    + (lastWeek > 0 ? "，上周 " + lastWeek + " 条" : "")
                    + "，这个月到此刻 " + thisMonth + " 条 · 点一下去统计页看趋势");
        }
        if (wWeek != null) {
            wWeek.setText("本周" + thisWeek);
            wWeek.setToolTipText("本周完成 " + thisWeek + " 条 · 点一下列出这几条");
            wLast.setText("·上周" + lastWeek);
            wLast.setToolTipText("上周完成 " + lastWeek + " 条 · 点一下列出这几条");
            wMonth.setText("·本月" + thisMonth);
            wMonth.setToolTipText("这个月到此刻完成 " + thisMonth + " 条 · 点一下列出这几条");
        }
        todoStat.setText(base.isEmpty() ? " " : base);
    }

    /** 按节奏行当前宽度挑一条放得下的文案，宁可字少也不让文字被裁 */
    private void applyPaceText() {
        if (paceBar == null) return;
        int room = 9999;
        if (paceRow != null && paceRow.getWidth() > 0) {
            int keep = 76;                                        // 右侧七天圆点固定 74 + 空隙
            if (streakLbl != null && streakLbl.isVisible()) keep += streakLbl.getPreferredSize().width;
            if (gapLbl != null && gapLbl.isVisible()) keep += gapLbl.getPreferredSize().width;
            room = paceRow.getWidth() - keep - 8;
        }
        java.awt.FontMetrics fm = paceBar.getFontMetrics(paceBar.getFont());
        String t = paceFull;
        if (fm.stringWidth(t) > room) t = paceShort;
        if (fm.stringWidth(t) > room) t = paceMini;
        if (fm.stringWidth(t) > room) t = paceTiny;
        if (!t.equals(paceBar.getText())) paceBar.setText(t);
    }

    /** 分档行已不再承载"本周完成"，这里只保证标题行右侧那组完成数一直显示 */
    private void attachWeekDone() {
        if (weekDone == null) return;
        weekDone.setVisible(true);
        if (weekDoneBox != null) weekDoneBox.setVisible(true);
    }

    /** 标题行右侧的一段完成数：kind 0=本周 1=上周 2=本月，点一下跳到统计页对应那张卡 */
    private JLabel doneSeg(String prefix, final int kind) {
        JLabel l = new JLabel(" ");
        l.setFont(Ui.font(11, false));
        l.setForeground(Ui.PRIMARY);
        l.setBorder(BorderFactory.createEmptyBorder(0, prefix.startsWith("·") ? 4 : 0, 0, 0));
        l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        l.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (!javax.swing.SwingUtilities.isLeftMouseButton(e)) return;
                openDoneList(kind == 1 ? "last" : kind == 2 ? "month" : "week");
            }
        });
        return l;
    }

    /** 本周（back=0）或上周（back=1）完成了多少条，不含归档条目 */
    /** 右键"今天做完 N 条"弹出的菜单：就一项，撤回今天勾掉的全部 */
    JPopupMenu buildTodayMenu() {
        int n = doneToday();
        JPopupMenu m = new JPopupMenu();
        if (n <= 0) return m;
        JMenuItem item = new JMenuItem("撤回今天全部 " + n + " 条");
        item.setToolTipText("把今天勾掉的 " + n + " 条改回未完成，日期不变");
        item.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                int done = undoAllToday();
                if (done > 0) Main.notifyMsg("已撤回今天 " + done + " 条", "这几条已改回未完成，日期没动");
            }
        });
        m.add(item);
        return m;
    }

    private void showTodayMenu(java.awt.event.MouseEvent e) {
        if (!(e.isPopupTrigger() || javax.swing.SwingUtilities.isRightMouseButton(e))) return;
        if (doneToday() <= 0) return;
        buildTodayMenu().show(todayDone, e.getX(), e.getY());
    }

    /** 把今天勾掉的待办全部改回未完成（日期保持不变），返回改了几条 */
    int undoAllToday() {
        LocalDate today = LocalDate.now();
        java.util.List<String> texts = new java.util.ArrayList<String>();
        for (Todo t : todos) {
            if (!t.done) continue;
            if (t.text == null) continue;
            if (t.text.endsWith("（已归档）")) continue;
            LocalDate d = parseDate(t.date == null ? "" : t.date.trim());
            if (d != null && d.equals(today)) texts.add(t.text);
        }
        if (texts.isEmpty()) return 0;
        int n = Store.markTodosUndone(texts);
        loadTodos();
        refreshTodoList();
        updateTodoStat();
        return n;
    }

    /** 本周从周一到今天，一条都没完成的日子（0 = 周一），给“空 N 天”用 */
    java.util.List<Integer> weekIdleDays() {
        int[] c = weekDoneCounts();
        int todayIdx = LocalDate.now().getDayOfWeek().getValue() - 1;
        java.util.List<Integer> out = new java.util.ArrayList<Integer>();
        for (int i = 0; i <= todayIdx && i < 7; i++) {
            if (c[i] <= 0) out.add(i);
        }
        return out;
    }

    /** 今天勾掉了多少条（不含归档） */
    int doneToday() {
        LocalDate today = LocalDate.now();
        int n = 0;
        for (Todo t : todos) {
            if (!t.done) continue;
            if (t.text != null && t.text.endsWith("（已归档）")) continue;
            LocalDate d = parseDate(t.date == null ? "" : t.date.trim());
            if (d != null && d.equals(today)) n++;
        }
        return n;
    }

    /**
     * 连续几周都比上一周完成得多（本周 vs 上周算第 1 周）。
     * 只往回看 12 周，遇到持平或下滑就停。
     */
    int streakUpWeeks() {
        int streak = 0;
        int cur = doneInWeek(0);
        for (int back = 1; back <= 12; back++) {
            int prev = doneInWeek(back);
            if (prev <= 0) break;               // 那周一条记录都没有，没法比，别硬算成"上涨"
            if (cur <= prev) break;
            streak++;
            cur = prev;
        }
        return streak;
    }

    int doneInWeek(int back) {
        LocalDate mon = LocalDate.now().with(java.time.DayOfWeek.MONDAY).minusWeeks(back);
        int n = 0;
        for (Todo t : todos) {
            if (!t.done) continue;
            if (t.text != null && t.text.endsWith("（已归档）")) continue;
            String raw = t.date == null ? "" : t.date.trim();
            if (raw.isEmpty() || "-".equals(raw)) continue;
            LocalDate d = parseDate(raw);
            if (d == null) continue;
            if (!d.isBefore(mon) && !d.isAfter(mon.plusDays(6))) n++;
        }
        return n;
    }

    /** 本月到此刻完成了多少条，不含归档条目与无日期条目 */
    int doneInMonth() {
        LocalDate now = LocalDate.now();
        int n = 0;
        for (Todo t : todos) {
            if (!t.done) continue;
            if (t.text != null && t.text.endsWith("（已归档）")) continue;
            String raw = t.date == null ? "" : t.date.trim();
            if (raw.isEmpty() || "-".equals(raw)) continue;
            LocalDate d = parseDate(raw);
            if (d == null) continue;
            if (d.getYear() == now.getYear() && d.getMonthValue() == now.getMonthValue()) n++;
        }
        return n;
    }

    /* ---------- 待办多选：一起改期、一起删掉 ---------- */

    /** 底部操作条：已选几条 + 改期 + 删除 + 退出 */
    private void updateBulkBar() {
        if (bulkBar == null) return;
        bulkBar.removeAll();
        if (!multiSelect) {
            bulkBar.setVisible(false);
            bulkBar.revalidate();
            bulkBar.repaint();
            if (rangeBar != null) {
                rangeBar.setVisible(false);
                rangeBar.revalidate();
                rangeBar.repaint();
            }
            return;
        }
        JLabel cnt = new JLabel(picked.isEmpty() ? "点上面的待办选中它" : "已选 " + picked.size() + " 条");
        cnt.setFont(Ui.font(10, true));
        cnt.setForeground(picked.isEmpty() ? Theme.muted() : Ui.PRIMARY);
        cnt.setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 10));
        bulkBar.add(cnt);
        if (rangeBar != null) {
            rangeBar.removeAll();
            JLabel scope = new JLabel("范围");
            scope.setFont(Ui.font(10, false));
            scope.setForeground(Theme.muted());
            scope.setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 6));
            rangeBar.add(scope);
            rangeBar.add(rangeChip("全部", 0));
            rangeBar.add(rangeChip("今天", 1));
            rangeBar.add(rangeChip("未来7天", 2));
            rangeBar.add(rangeChip("过期", 3));
            rangeBar.add(rangeChip("无日期", 5));
            rangeBar.add(rangeChip(bulkRange == 4 && bulkFrom != null && bulkTo != null
                    ? bulkFrom.format(DateTimeFormatter.ofPattern("MM-dd")) + "~"
                    + bulkTo.format(DateTimeFormatter.ofPattern("MM-dd"))
                    : "自定义", 4));
            rangeBar.setVisible(true);
            rangeBar.revalidate();
            rangeBar.repaint();
        }
        final java.util.List<Todo> open = openTodos();
        bulkBar.add(bulkChip("全选", new Runnable() {
            public void run() {
                picked.addAll(open);
                refreshTodoList();
            }
        }));
        bulkBar.add(bulkChip("反选", new Runnable() {
            public void run() {
                for (Todo t : open) {
                    if (!picked.remove(t)) picked.add(t);
                }
                refreshTodoList();
            }
        }));
        bulkBar.add(bulkMenuChip("改期到…"));
        bulkBar.add(bulkChip("删除选中", new Runnable() {
            public void run() {
                batchDelete();
            }
        }));
        bulkBar.add(bulkChip("复制到…", new Runnable() {
            public void run() {
                DatePicker.PickResult r = DatePicker.pickEx(OverviewPanel.this,
                        java.time.LocalDate.now(), todoTitlesByDay());
                if (r == null || r.day == null || r.create) return;
                batchCopy(r.day);
            }
        }));
        bulkBar.add(bulkChip("清除选择", new Runnable() {
            public void run() {
                picked.clear();
                multiSelect = false;
                bulkRange = 0;
                refreshTodoList();
            }
        }));
        bulkBar.setVisible(true);
        bulkBar.revalidate();
        bulkBar.repaint();
    }

    /** 范围标签：当前范围高亮蓝底，点一下切换并只对范围内条目生效 */
    private JLabel rangeChip(String text, final int kind) {
        final boolean on = bulkRange == kind;
        JLabel chip = new JLabel(text);
        chip.setFont(Ui.font(10, on));
        chip.setForeground(on ? Color.WHITE : Theme.muted());
        chip.setOpaque(on);
        chip.setBackground(Ui.PRIMARY);
        chip.setBorder(BorderFactory.createEmptyBorder(1, 6, 1, 6));
        chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        chip.setToolTipText(kind == 4 ? "自己选起止两天，只看这段里的待办"
                : (on ? "当前范围 " + text + " · 全选只作用于范围内" : "只看 " + text));
        chip.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (bulkRange == kind && kind != 4) return;
                if (kind == 4) {
                    if (!pickBulkRange()) return;
                } else {
                    bulkRange = kind;
                }
                picked.clear();
                refreshTodoList();
            }
        });
        return chip;
    }

    /** 自定义范围：连选两次日期（起 / 止），顺序反了就自动换过来 */
    boolean pickBulkRange() {
        DatePicker.PickResult r1 = DatePicker.pickEx(OverviewPanel.this,
                java.time.LocalDate.now(), todoTitlesByDay());
        if (r1 == null || r1.day == null) return false;
        java.time.LocalDate start = r1.day;
        DatePicker.PickResult r2 = DatePicker.pickEx(OverviewPanel.this, start, todoTitlesByDay());
        if (r2 == null || r2.day == null) return false;
        java.time.LocalDate end = r2.day;
        if (end.isBefore(start)) {
            java.time.LocalDate tmp = start;
            start = end;
            end = tmp;
        }
        bulkFrom = start;
        bulkTo = end;
        bulkRange = 4;
        Main.notifyMsg("已筛出这段日期", "只看 " + start.format(DateTimeFormatter.ofPattern("MM-dd"))
                + " ~ " + end.format(DateTimeFormatter.ofPattern("MM-dd")) + " 的待办");
        return true;
    }

    /** 多选条上的「改期到…」：点开一个下拉，里面挑明天 / 本周五 / 下周一 / 自己选 */
    private JLabel bulkMenuChip(String text) {
        JLabel chip = new JLabel(text);
        chip.setFont(Ui.font(10, true));
        chip.setForeground(Ui.PRIMARY);
        chip.setBorder(BorderFactory.createEmptyBorder(1, 8, 1, 4));
        chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        chip.setToolTipText("点开挑一天：明天 / 本周五 / 下周一 / 自己选");
        chip.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (!javax.swing.SwingUtilities.isLeftMouseButton(e)) return;
                bulkMoveMenu((Component) e.getSource());
            }
        });
        return chip;
    }

    /** 「改期到…」下拉里的选项 */
    void bulkMoveMenu(Component inv) {
        javax.swing.JPopupMenu m = new javax.swing.JPopupMenu();
        java.time.LocalDate now = java.time.LocalDate.now();
        javax.swing.JMenuItem tomorrow = new javax.swing.JMenuItem("改期到明天（"
                + now.plusDays(1).format(DateTimeFormatter.ofPattern("MM-dd")) + "）");
        tomorrow.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                batchMove(java.time.LocalDate.now().plusDays(1));
            }
        });
        java.time.LocalDate fri = now.with(java.time.DayOfWeek.FRIDAY);
        if (fri.isBefore(now)) fri = fri.plusWeeks(1);
        final java.time.LocalDate friDay = fri;
        javax.swing.JMenuItem friday = new javax.swing.JMenuItem("改期到本周五（"
                + friDay.format(DateTimeFormatter.ofPattern("MM-dd")) + "）");
        friday.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                batchMove(friDay);
            }
        });
        java.time.LocalDate mon = now.with(java.time.DayOfWeek.MONDAY);
        if (!mon.isAfter(now)) mon = mon.plusWeeks(1);
        final java.time.LocalDate monDay = mon;
        javax.swing.JMenuItem monday = new javax.swing.JMenuItem("改期到下周一（"
                + monDay.format(DateTimeFormatter.ofPattern("MM-dd")) + "）");
        monday.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                batchMove(monDay);
            }
        });
        javax.swing.JMenuItem other = new javax.swing.JMenuItem("自己选一天…");
        other.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                DatePicker.PickResult r = DatePicker.pickEx(OverviewPanel.this,
                        java.time.LocalDate.now(), todoTitlesByDay());
                if (r == null || r.day == null || r.create) return;
                batchMove(r.day);
            }
        });
        m.add(tomorrow);
        m.add(friday);
        m.add(monday);
        m.addSeparator();
        m.add(other);
        m.show(inv, 0, inv.getHeight());
        lastBulkMenu = m;
    }

    /** 最近一次多选「改期到…」下拉，供截图与断言 */
    javax.swing.JPopupMenu lastBulkMenu;

    private JLabel bulkChip(String text, Runnable action) {
        JLabel chip = new JLabel(text);
        chip.setFont(Ui.font(10, true));
        chip.setForeground(Ui.PRIMARY);
        chip.setBorder(BorderFactory.createEmptyBorder(1, 8, 1, 4));
        chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        chip.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                action.run();
            }
        });
        return chip;
    }

    /** 把选中的待办一起改到某天 */
    int batchMove(java.time.LocalDate day) {
        if (picked.isEmpty()) {
            Main.notifyMsg("还没选待办", "先点几条待办，再一起改期");
            return 0;
        }
        int n = 0;
        String key = DAY.format(java.util.Date.from(
                day.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));
        for (Todo t : new java.util.ArrayList<Todo>(picked)) {
            t.date = key;
            n++;
        }
        picked.clear();
        multiSelect = false;
        saveTodos();
        refreshTodoList();
        Main.notifyJump("已批量改期", n + " 条待办改到 "
                + day.format(DateTimeFormatter.ofPattern("MM-dd")), "overview");
        return n;
    }

    /** 把选中的待办复制一份到某天（原条目保持不动，用来做“这件事再来一次”） */
    int batchCopy(java.time.LocalDate day) {
        if (picked.isEmpty()) {
            Main.notifyJump("还没选待办", "先点几条待办，再复制这一批", "overview");
            return 0;
        }
        String key = DAY.format(java.util.Date.from(
                day.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));
        int n = 0;
        for (Todo t : new java.util.ArrayList<Todo>(picked)) {
            todos.add(new Todo(false, t.text, key));
            n++;
        }
        picked.clear();
        multiSelect = false;
        saveTodos();
        refreshTodoList();
        Main.notifyJump("已复制这批待办", n + " 条复制到 "
                + day.format(DateTimeFormatter.ofPattern("MM-dd")) + "（原条目还在）", "overview");
        return n;
    }

    /** 把选中的待办一起删掉 */
    int batchDelete() {
        if (picked.isEmpty()) {
            Main.notifyMsg("还没选待办", "先点几条待办，再一起删除");
            return 0;
        }
        int n = picked.size();
        todos.removeAll(picked);
        picked.clear();
        multiSelect = false;
        saveTodos();
        refreshTodoList();
        Main.notifyJump("已批量删除", "删掉 " + n + " 条待办", "overview");
        return n;
    }

    private JPanel todoRow(final Todo item) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, multiSelect ? 24 : 30));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 0));

        final JCheckBox cb = new JCheckBox(item.text);
        cb.setFont(Ui.font(12, false));
        cb.setOpaque(false);
        cb.setSelected(item.done);
        cb.setForeground(item.done ? Theme.muted() : Theme.text());
        cb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cb.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (multiSelect) return;
                item.done = cb.isSelected();
                cb.setForeground(item.done ? Theme.muted() : Theme.text());
                saveTodos();
                updateTodoStat();
            }
        });
        if (multiSelect) {
            /* 多选模式：点这一行就是选中，不再打勾 */
            cb.setEnabled(false);
            cb.setToolTipText("多选模式：点一下选中这条");
            final boolean on = picked.contains(item);
            row.setOpaque(on);
            row.setBackground(new Color(0xDCEBFF));
            java.awt.event.MouseAdapter sel = new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (!javax.swing.SwingUtilities.isLeftMouseButton(e)) return;
                    if (!picked.remove(item)) picked.add(item);
                    refreshTodoList();
                }
            };
            cb.addMouseListener(sel);
            row.addMouseListener(sel);
            if (on) {
                cb.setForeground(Theme.text());
            }
        }

        /* 右键联动：在备忘录中查找 / 转为日程提醒 / 复制 / 设为今天 */
        final JPopupMenu menu = new JPopupMenu();
        JMenuItem toMemo = new JMenuItem("在备忘录中查找");
        toMemo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                MemoPanel mp = Main.page(MemoPanel.class);
                if (mp != null) mp.searchFor(item.text);
                Main.go("memo");
            }
        });
        JMenuItem toSched = new JMenuItem("转为日程提醒");
        toSched.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Main.go("schedule");
                SchedulePanel sp = Main.page(SchedulePanel.class);
                if (sp != null) sp.createFrom(item.text);
            }
        });
        JMenuItem copy = new JMenuItem("复制到剪贴板");
        copy.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                            new StringSelection(item.text), null);
                    Main.notifyMsg("已复制", item.text);
                } catch (Exception ex) {
                    Main.notifyMsg("复制失败", "系统剪贴板不可用");
                }
            }
        });
        JMenuItem copyTomorrow = new JMenuItem("复制一条到明天");
        copyTomorrow.setToolTipText("原条目留在原地，另存一条新的放到明天");
        copyTomorrow.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                java.time.LocalDate d = java.time.LocalDate.now().plusDays(1);
                String key = DAY.format(java.util.Date.from(
                        d.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));
                todos.add(new Todo(false, item.text, key));
                saveTodos();
                refreshTodoList();
                Main.notifyMsg("已复制到明天",
                        item.text + " · " + d.toString().substring(5) + " 会出现在今日待办");
            }
        });
        JMenuItem today = new JMenuItem("设为今天");
        today.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setTodoDate(item, java.time.LocalDate.now());
            }
        });
        /* 改期到指定日期：明天 / 本周五 / 下周一 / 不限日期 */
        JMenu defer = new JMenu("改期到…");
        JMenuItem toTomorrow = new JMenuItem("明天");
        toTomorrow.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setTodoDate(item, java.time.LocalDate.now().plusDays(1));
            }
        });
        JMenuItem toFriday = new JMenuItem("本周五");
        toFriday.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                java.time.LocalDate d = java.time.LocalDate.now()
                        .with(java.time.DayOfWeek.FRIDAY);
                if (d.isBefore(java.time.LocalDate.now())) d = d.plusWeeks(1);
                setTodoDate(item, d);
            }
        });
        JMenuItem toNextMonday = new JMenuItem("下周一");
        toNextMonday.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                java.time.LocalDate now = java.time.LocalDate.now();
                java.time.LocalDate d = now.with(java.time.DayOfWeek.MONDAY);
                if (!d.isAfter(now)) d = d.plusWeeks(1);
                setTodoDate(item, d);
            }
        });
        JMenuItem toAny = new JMenuItem("不限日期");
        toAny.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                item.date = "-";
                saveTodos();
                refreshTodoList();
                Main.notifyMsg("已清除日期", item.text);
            }
        });
        JMenuItem toPick = new JMenuItem("指定日期…");
        toPick.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                LocalDate init = parseDate(item.date);
                DatePicker.PickResult r = DatePicker.pickEx(OverviewPanel.this, init, todoTitlesByDay());
                if (r == null || r.day == null) return;
                if (r.create) {
                    /* 双击格子 = 在那天新建一条，而不是把当前这条挪过去 */
                    String mmdd = r.day.format(DateTimeFormatter.ofPattern("MM-dd"));
                    String key = DAY.format(java.util.Date.from(
                            r.day.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));
                    JPanel box = new JPanel();
                    box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
                    box.setOpaque(false);
                    JLabel ctx = new JLabel(newTodoContext(r.day, key));
                    ctx.setFont(Ui.font(11, false));
                    ctx.setForeground(Theme.muted());
                    ctx.setAlignmentX(Component.LEFT_ALIGNMENT);
                    ctx.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
                    JTextField field = new JTextField();
                    field.setAlignmentX(Component.LEFT_ALIGNMENT);
                    field.setMaximumSize(new Dimension(300, 30));
                    field.setPreferredSize(new Dimension(300, 30));
                    box.add(ctx);
                    box.add(field);
                    int ret = JOptionPane.showConfirmDialog(OverviewPanel.this, box,
                            "在 " + mmdd + " 新建待办", JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.PLAIN_MESSAGE);
                    if (ret != JOptionPane.OK_OPTION) return;
                    String t = field.getText().trim();
                    if (t.isEmpty()) return;
                    todos.add(new Todo(false, t, key));
                    saveTodos();
                    showDate(r.day);
                    Main.notifyMsg("已在 " + mmdd + " 加了一条", t);
                } else {
                    setTodoDate(item, r.day);
                }
            }
        });
        defer.add(toTomorrow);
        defer.add(toFriday);
        defer.add(toNextMonday);
        defer.addSeparator();
        defer.add(toPick);
        defer.add(toAny);

        menu.add(toMemo);
        menu.add(toSched);
        menu.addSeparator();
        menu.add(copy);
        menu.add(copyTomorrow);
        menu.add(today);
        menu.add(defer);
        cb.setComponentPopupMenu(menu);
        row.setComponentPopupMenu(menu);
        cb.setToolTipText("右键可转到备忘录 / 日程、复制、改期");

        JButton del = new JButton("×");
        del.setFont(Ui.font(12, true));
        del.setForeground(Theme.muted());
        del.setContentAreaFilled(false);
        del.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 2));
        del.setFocusPainted(false);
        del.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        del.setToolTipText("删除这条待办");
        del.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                todos.remove(item);
                saveTodos();
                refreshTodoList();
            }
        });

        row.add(cb, BorderLayout.CENTER);
        row.add(del, BorderLayout.EAST);
        return row;
    }

    private JPanel metricCard(final int i) {
        final Color accent = COLORS[i];
        JPanel card = new JPanel(new BorderLayout()) {
            private boolean hover;

            {
                setOpaque(false);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) {
                        hover = true;
                        repaint();
                    }

                    public void mouseExited(MouseEvent e) {
                        hover = false;
                        repaint();
                    }

                    public void mouseClicked(MouseEvent e) {
                        Main.go(KEYS[i]);
                        if (i == 0) {
                            /* 今日专注卡：人到番茄钟后顺手把「番茄记录」弹窗也开出来 */
                            SwingUtilities.invokeLater(() -> {
                                PomodoroPanel pp = Main.page(PomodoroPanel.class);
                                if (pp != null) pp.openHistory();
                            });
                        }
                    }
                });
            }

            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.isDark() ? new Color(0x1E2029) : new Color(0xF7F9FC));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setStroke(new BasicStroke(hover ? 1.4f : 1f));
                g2.setColor(hover ? accent : Theme.border());
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
            }
        };
        card.setBorder(BorderFactory.createEmptyBorder(10, 20, 9, 18));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 0));
        top.setOpaque(false);
        top.add(new JLabel(IconFactory.nav(KEYS[i], accent)));
        JLabel title = new JLabel(TITLES[i]);
        title.setFont(Ui.font(12, false));
        title.setForeground(Theme.muted());
        top.add(title);

        JLabel value = new JLabel("—");
        value.setFont(Ui.font(23, true));
        value.setForeground(Theme.text());

        JLabel sub = new JLabel(" ");
        sub.setFont(Ui.font(11, false));
        sub.setForeground(Theme.muted());

        JLabel enter = new JLabel("点击进入 →");
        enter.setFont(Ui.font(10, false));
        enter.setForeground(Theme.muted());

        /* 说明文字和"点击进入"并排放一行：卡高固定，两行会把说明挤成 0 高（真机上整行不显示） */
        JPanel south = new JPanel(new BorderLayout(8, 0));
        south.setOpaque(false);
        south.add(sub, BorderLayout.CENTER);
        south.add(enter, BorderLayout.EAST);

        card.add(top, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        card.add(south, BorderLayout.SOUTH);

        values[i] = value;
        subs[i] = sub;
        enters[i] = enter;
        final JPanel cardRef = card;
        final JPanel topRef = top;
        final JPanel southRef = south;
        fixers[i] = () -> fixCard(cardRef, topRef, southRef, i);
        card.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent e) {
                SwingUtilities.invokeLater(OverviewPanel.this::applyAllFixers);
            }
        });
        return card;
    }

    /**
     * 快捷卡自适应。卡宽卡高都是被 grid 摊出来的，窗口一矮，大号数字（BorderLayout 中间块）
     * 会被压成 0 高直接看不见；说明行也会被"点击进入"挤掉。这里按实际可用空间逐级降级：
     * 数字 23 → 20 → 18 → 16 px；说明 完整版 → 缩写版 → 10px → 收起"点击进入" → 整行留空。
     */
    private void fixCard(JPanel card, JPanel top, JPanel south, int i) {
        int h = card.getHeight();
        if (h <= 0) return;
        JLabel value = values[i], sub = subs[i], enter = enters[i];
        if (value == null || sub == null || subFull[i] == null) return;

        /* 说明行是"这张卡现在是多少"的解释，比装饰性的"点击进入"更要紧：
           空间不够时先收"点击进入"，再一路降数字字号，最后才动说明行 */
        sub.setVisible(true);
        south.setVisible(true);

        int topH = top.getPreferredSize().height;
        int southH = south.getPreferredSize().height;
        int availH = h - 19 - topH - southH;        // 19 = 上下内边距
        sizeValue(value, availH);

        if (value.getPreferredSize().height > availH && enter != null && enter.isVisible()) {
            enter.setVisible(false);
            southH = south.getPreferredSize().height;
            availH = h - 19 - topH - southH;
            sizeValue(value, availH);
        }
        if (value.getPreferredSize().height > availH) {   // 真的挤不下：说明行收起，数字优先
            south.setVisible(false);
            availH = h - 19 - topH;
            sizeValue(value, availH);
            return;
        }
        int availW = sub.getWidth();
        if (availW <= 0) return;
        sub.setFont(Ui.font(11, false));
        String full = subFull[i];
        String brief = subShort[i] == null ? full : subShort[i];
        if (fits(sub, full, availW)) {
            sub.setText(full);
            return;
        }
        if (fits(sub, brief, availW)) {
            sub.setText(brief);
            return;
        }
        if (enter != null && enter.isVisible()) {
            enter.setVisible(false);
            SwingUtilities.invokeLater(this::applyAllFixers);
            return;
        }
        sub.setFont(Ui.font(10, false));
        sub.setText(fits(sub, brief, availW) ? brief : "");
    }

    /** 数字从 23px 一路试到 14px，取第一档放得下的；字号没变就不 setFont（避免无谓重排） */
    private void sizeValue(JLabel value, int availH) {
        int want = 14;
        for (int fs : new int[]{23, 20, 18, 16, 14}) {
            if (value.getFontMetrics(Ui.font(fs, true)).getHeight() <= availH) {
                want = fs;
                break;
            }
        }
        if (value.getFont() == null || value.getFont().getSize() != want) value.setFont(Ui.font(want, true));
    }

    private void applyAllFixers() {
        for (int i = 0; i < fixers.length; i++) if (fixers[i] != null) fixers[i].run();
    }

    private boolean fits(JLabel l, String text, int avail) {
        return l.getFontMetrics(l.getFont()).stringWidth(text) <= avail;
    }

    /** 切到概览页时刷新 */
    public void refresh() {
        int archived = archiveStale();     // 超过 30 天没动的先归档，不再提醒
        if (archived > 0) Main.notifyMsg("已归档 " + archived + " 条",
                "超过 30 天没动的过期待办已收起");
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(new Date());
        dateLabel.setText(new SimpleDateFormat("yyyy年M月d日  EEEE", Locale.CHINA).format(new Date())
                + "    ·    数据实时汇总，点击卡片直达工具");

        int todayMin = 0, todayCount = 0;
        for (String line : Store.readLines("pomodoro_log.txt")) {
            String[] p = line.split("\t", -1);
            if (p.length < 4 || !"专注".equals(p[3]) || !p[1].startsWith(today)) continue;
            try {
                todayMin += Integer.parseInt(p[2].trim());
            } catch (Exception ignored) {
            }
            todayCount++;
        }
        int pomoGoal = 8;
        java.util.List<String> pl = Store.readLines("pomodoro.txt");
        if (pl.size() > 6) {
            try {
                int g = Integer.parseInt(pl.get(6).trim());
                if (g >= 1 && g <= 24) pomoGoal = g;
            } catch (Exception ignored) {
            }
        }
        int pct = pomoGoal <= 0 ? 0 : (int) Math.round(Math.min(1.0, todayCount * 1.0 / pomoGoal) * 100);
        values[0].setText(todayMin + " 分钟");
        subFull[0] = todayCount >= pomoGoal
                ? "完成 " + todayCount + "/" + pomoGoal + " 个 · 已达标"
                : "完成 " + todayCount + "/" + pomoGoal + " 个 · " + pct + "%";
        subShort[0] = todayCount + "/" + pomoGoal + " 个";

        int cdCount = 0, cdSec = 0;
        for (String line : Store.readLines("countdown_log.txt")) {
            String[] p = line.split("\t", -1);
            if (p.length < 3 || !p[1].startsWith(today)) continue;
            cdCount++;
            try {
                cdSec += Integer.parseInt(p[2].trim());
            } catch (Exception ignored) {
            }
        }
        values[1].setText(cdCount + " 次");
        subFull[1] = cdSec > 0 ? "共 " + fmtClock(cdSec) : "今天还没用过";
        subShort[1] = subFull[1];

        int schCount = 0;
        long next = Long.MAX_VALUE;
        for (String line : Store.readLines("schedule.txt")) {
            String[] p = line.split("\t", -1);
            if (p.length < 3) continue;
            schCount++;
            try {
                long t = Long.parseLong(p[2].trim());
                if (t > 0 && t < next) next = t;
            } catch (Exception ignored) {
            }
        }
        values[2].setText(schCount + " 条");
        if (next == Long.MAX_VALUE) {
            subFull[2] = "暂无待办";
            subShort[2] = "暂无待办";
        } else {
            subFull[2] = "最近 " + new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(new Date(next));
            subShort[2] = "最近 " + new SimpleDateFormat("MM-dd", Locale.CHINA).format(new Date(next));
        }

        double in = 0, out = 0;
        for (String line : Store.readLines("ledger.txt")) {
            String[] p = line.split("\t", -1);
            int type = p.length > 0 && "1".equals(p[0]) ? 1 : 0;
            double amt = 0;
            try {
                amt = Double.parseDouble(p[1].trim());
            } catch (Exception ignored) {
            }
            if (type == 1) in += amt;
            else out += amt;
        }
        values[3].setText(String.format(Locale.CHINA, "¥%.2f", in - out));
        subFull[3] = "收入 " + Math.round(in) + " / 支出 " + Math.round(out);
        subShort[3] = "收 " + Math.round(in) + " · 支 " + Math.round(out);

        long cnt = 0;
        java.util.List<String> cl = Store.readLines("counter.txt");
        if (!cl.isEmpty()) {
            String[] cp = cl.get(0).split("\t", -1);
            try {
                cnt = Long.parseLong(cp.length >= 2 ? cp[1].trim() : cp[0].trim());
            } catch (Exception ignored) {
            }
        }
        values[4].setText(String.valueOf(cnt));
        subFull[4] = "当前计数值";
        subShort[4] = "计数值";

        int memoCount = 0;
        for (String line : Store.readLines("memo.txt")) {
            if (!line.trim().isEmpty()) memoCount++;
        }
        values[5].setText(memoCount + " 条");
        subFull[5] = "已保存的备忘";
        subShort[5] = "备忘";

        /* 第 7 张：今日打卡（habit.txt / habit_log.txt） */
        java.util.List<String> habits = Habit.habits();
        int habitDone = 0, habitBest = 0;
        String habitBestName = "";
        for (String h : habits) {
            if (Habit.done(h, java.time.LocalDate.now())) habitDone++;
            int st = Habit.streak(h);
            if (st > habitBest) {
                habitBest = st;
                habitBestName = h;
            }
        }
        values[6].setText(habitDone + " / " + habits.size());
        if (habits.isEmpty()) {
            subFull[6] = "还没建习惯，点进去加一个";
            subShort[6] = "还没建习惯";
        } else if (habitDone == habits.size()) {
            subFull[6] = "今天全打完了 · 最长 " + habitBest + " 天";
            subShort[6] = "今天全打完";
        } else {
            subFull[6] = (habitBest > 0 ? "最长 " + habitBest + " 天（" + habitBestName + "）"
                    : "今天还差 " + (habits.size() - habitDone) + " 个");
            subShort[6] = habitBest > 0 ? "最长 " + habitBest + " 天" : "还差 " + (habits.size() - habitDone) + " 个";
        }

        applyAllFixers();
        /* 数值和说明都可能很长，卡宽固定，鼠标停在上面能看到完整文字 */
        for (int i = 0; i < KEYS.length; i++) {
            values[i].setToolTipText(values[i].getText());
            subs[i].setToolTipText((subFull[i] == null ? "" : subFull[i])
                    + (subShort[i] != null && !subShort[i].equals(subFull[i]) ? "（卡窄时缩写：" + subShort[i] + "）" : ""));
        }

        // 本周专注趋势（周一~周日）
        java.util.Calendar wk = java.util.Calendar.getInstance();
        int iso = wk.get(java.util.Calendar.DAY_OF_WEEK);
        int back = iso == java.util.Calendar.SUNDAY ? 6 : iso - 2;
        String[] keys = new String[7];
        java.util.Calendar c = (java.util.Calendar) wk.clone();
        c.add(java.util.Calendar.DAY_OF_YEAR, -back);
        SimpleDateFormat md = new SimpleDateFormat("MM-dd", Locale.CHINA);
        for (int i = 0; i < 7; i++) {
            keys[i] = md.format(c.getTime());
            c.add(java.util.Calendar.DAY_OF_YEAR, 1);
        }
        int[] wm = new int[7];
        int wtotal = 0;
        for (String line : Store.readLines("pomodoro_log.txt")) {
            String[] p = line.split("\t", -1);
            if (p.length < 4 || !"专注".equals(p[3])) continue;
            String t = p[1].trim();
            if (t.length() < 10) continue;
            String mmdd = t.substring(5, 10);
            int min = 0;
            try {
                min = Integer.parseInt(p[2].trim());
            } catch (Exception ignored) {
            }
            for (int i = 0; i < 7; i++) {
                if (keys[i].equals(mmdd)) {
                    wm[i] += min;
                    wtotal += min;
                    break;
                }
            }
        }
        if (trend != null) trend.setData(wm, wtotal);

        refreshTodoList();
        refreshRecent();
    }

    private String fmtClock(long sec) {
        if (sec < 60) return sec + " 秒";
        return (sec / 60) + " 分 " + (sec % 60) + " 秒";
    }
}
