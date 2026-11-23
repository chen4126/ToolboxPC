package com.toolbox.pc;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** 日程提醒：支持不重复 / 每天 / 每周(多选星期) / 每月(指定几号) / 工作日 */
public class SchedulePanel extends JPanel {

    private static final String[] WEEK_CN = {"一", "二", "三", "四", "五", "六", "日"};
    private static final SimpleDateFormat DAYKEY = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);

    private static class Item {
        int id;
        String title;
        long time;
        String rule = "none";
    }

    private List<Item> items = new ArrayList<Item>();
    private int seq = 1;
    private DefaultTableModel model;
    private javax.swing.Timer checker;
    private JLabel countLabel;
    private JPanel daysBar;
    private JPanel daysChips;
    private int view = 0; // 0 全部 / 1 本周 / 2 本月
    private final JToggleButton[] viewBtns = new JToggleButton[3];
    private final List<Integer> rowIds = new ArrayList<>();
    private final List<String> rowKeys = new ArrayList<>();
    private final java.util.Set<String> folded = new java.util.HashSet<String>();
    private int viewTotal = 0;
    private JButton foldBtn;
    private final java.util.Set<String> viewKeys = new java.util.HashSet<String>();
    /** 已经发过“还有 5 分钟”预告的提醒时间点，避免同一轮重复气泡 */
    private final java.util.Set<Long> preAired = new java.util.HashSet<Long>();
    private JLabel emptyLabel;
    private JLabel summaryLabel;
    /** 跨页联动时预填到“新建提醒”内容框的文字，用完即清 */
    private String pendingTitle;

    public SchedulePanel() {
        setLayout(new BorderLayout(8, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        setBackground(Theme.bg());
        load();

        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        JButton addBtn = Ui.primary("+ 新建提醒");
        addBtn.setPreferredSize(new Dimension(112, 34));
        JButton delBtn = Ui.secondary("删除选中");
        JButton boardBtn = Ui.secondary("桌面小窗");
        boardBtn.setPreferredSize(new Dimension(96, 34));
        boardBtn.setToolTipText("把今天的待办与日程缩小成置顶小窗，放在桌面角落");
        left.add(addBtn);
        left.add(delBtn);
        left.add(boardBtn);
        boardBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                MiniBoard.toggle();
            }
        });
        countLabel = new JLabel(" ");
        countLabel.setFont(Ui.font(12, false));
        countLabel.setForeground(Theme.muted());

        /* 视图切换：全部 / 本周 / 本月 */
        String[] vn = {"全部", "本周", "本月"};
        ButtonGroup vg = new ButtonGroup();
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            JToggleButton b = new JToggleButton(vn[i]);
            b.setFont(Ui.font(11, true));
            b.setFocusPainted(false);
            b.setPreferredSize(new Dimension(58, 28));
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.setSelected(idx == view);
            b.addActionListener(e -> {
                view = idx;
                styleView();
                refreshTable();
            });
            vg.add(b);
            viewBtns[i] = b;
            left.add(b);
        }
        viewBtns[0].setToolTipText("显示所有提醒（原始设定时间）");
        viewBtns[1].setToolTipText("只看本周一到周日，重复规则已展开");
        viewBtns[2].setToolTipText("只看本月，重复规则已展开");

        foldBtn = Ui.secondary("全部收起");
        foldBtn.setPreferredSize(new Dimension(88, 28));
        foldBtn.setFont(Ui.font(11, false));
        foldBtn.setToolTipText("一次性收起或展开所有日期分组");
        foldBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                boolean anyFolded = false;
                for (String k : viewKeys) {
                    if (folded.contains(k)) {
                        anyFolded = true;
                        break;
                    }
                }
                if (anyFolded) folded.clear();
                else folded.addAll(viewKeys);
                refreshTable();
            }
        });
        left.add(foldBtn);

        styleView();
        bar.add(left, BorderLayout.WEST);
        bar.add(countLabel, BorderLayout.EAST);

        /* 顶部再挂一条倒数日：距离考试 / 截止 / 生日还有多少天 */
        JPanel head = new JPanel(new BorderLayout(0, 6));
        head.setOpaque(false);
        head.add(bar, BorderLayout.NORTH);
        daysBar = new JPanel(new BorderLayout(8, 0));
        daysBar.setOpaque(false);
        daysChips = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        daysChips.setOpaque(false);
        daysBar.add(daysChips, BorderLayout.CENTER);
        JButton addDay = Ui.secondary("＋ 添加");
        addDay.setFont(Ui.font(11, false));
        addDay.setPreferredSize(new Dimension(84, 28));
        addDay.setToolTipText("加一个要倒数的日子，比如考试、截止、生日");
        addDay.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                openAddDays();
            }
        });
        daysBar.add(addDay, BorderLayout.EAST);
        head.add(daysBar, BorderLayout.CENTER);
        renderDaysLeft();
        add(head, BorderLayout.NORTH);

        model = new DefaultTableModel(new String[]{"时间", "内容", "重复"}, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        final JTable table = new JTable(model);
        table.setRowHeight(36);

        javax.swing.table.DefaultTableCellRenderer cellRenderer =
                new javax.swing.table.DefaultTableCellRenderer() {
                    public Component getTableCellRendererComponent(JTable t, Object v, boolean selS,
                            boolean hasFocus, int row, int col) {
                        JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, selS, hasFocus, row, col);
                        boolean grp = row >= 0 && row < rowIds.size() && rowIds.get(row) == -1;
                        l.setOpaque(true);
                        if (grp) {
                            l.setBackground(Theme.isDark() ? new Color(0x2B3350) : new Color(0xEEF2FF));
                            l.setForeground(Theme.isDark() ? new Color(0xAFC0FF) : Ui.PRIMARY);
                            l.setFont(Ui.font(11, true));
                        } else {
                            l.setBackground(Theme.isDark() ? Theme.bg() : Color.WHITE);
                            l.setForeground(Theme.text());
                            l.setFont(Ui.font(12, false));
                        }
                        l.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 6));
                        return l;
                    }
                };
        table.setDefaultRenderer(Object.class, cellRenderer);
        MouseAdapter groupMouse = new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (view == 0 || e.getClickCount() != 1) return;
                int row = table.rowAtPoint(e.getPoint());
                if (row < 0 || row >= rowIds.size() || rowIds.get(row) != -1) return;
                String k = rowKeys.get(row);
                if (k == null) return;
                if (!folded.remove(k)) folded.add(k);
                refreshTable();
            }

            public void mouseMoved(MouseEvent e) {
                if (view == 0) return;
                int row = table.rowAtPoint(e.getPoint());
                boolean grp = row >= 0 && row < rowIds.size() && rowIds.get(row) == -1;
                table.setCursor(Cursor.getPredefinedCursor(
                        grp ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            }
        };
        table.addMouseListener(groupMouse);
        table.addMouseMotionListener(groupMouse);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFont(Ui.font(12, false));
        table.getTableHeader().setFont(Ui.font(12, true));
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(0).setPreferredWidth(170);
        table.getColumnModel().getColumn(0).setMaxWidth(220);
        table.getColumnModel().getColumn(1).setPreferredWidth(360);
        table.getColumnModel().getColumn(2).setPreferredWidth(190);
        table.getColumnModel().getColumn(2).setMaxWidth(260);
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(Theme.border()));

        emptyLabel = new JLabel("还没有提醒 · 点左上角「+ 新建提醒」开始", SwingConstants.CENTER);        emptyLabel.setFont(Ui.font(13, false));
        emptyLabel.setForeground(Theme.muted());
        JPanel tableWrap = new JPanel();
        tableWrap.setOpaque(false);
        tableWrap.setLayout(new OverlayLayout(tableWrap));
        emptyLabel.setAlignmentX(0.5f);
        emptyLabel.setAlignmentY(0.5f);
        sp.setAlignmentX(0.5f);
        sp.setAlignmentY(0.5f);
        tableWrap.add(emptyLabel);
        tableWrap.add(sp);
        add(tableWrap, BorderLayout.CENTER);

        summaryLabel = new JLabel(" ");
        summaryLabel.setFont(Ui.font(11, false));
        summaryLabel.setForeground(Theme.muted());
        summaryLabel.setBorder(BorderFactory.createEmptyBorder(4, 2, 0, 0));
        add(summaryLabel, BorderLayout.SOUTH);

        addBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                openEditor();
            }
        });

        delBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int row = table.getSelectedRow();
                if (row < 0) {
                    JOptionPane.showMessageDialog(SchedulePanel.this, "请先选中一条提醒");
                    return;
                }
                if (row < rowIds.size() && rowIds.get(row) == -1) {
                    JOptionPane.showMessageDialog(SchedulePanel.this, "这是日期分组行，请选中下面具体的某一条");
                    return;
                }
                if (JOptionPane.showConfirmDialog(SchedulePanel.this,
                        view == 0 ? "删除这条提醒？" : "删除这条安排所属的提醒？（重复规则会一并删除）", "确认",
                        JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    if (row >= 0 && row < rowIds.size()) {
                        int id = rowIds.get(row);
                        for (int i = 0; i < items.size(); i++) {
                            if (items.get(i).id == id) {
                                items.remove(i);
                                break;
                            }
                        }
                    }
                    save();
                    refreshTable();
                }
            }
        });

        checker = new javax.swing.Timer(1000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                check();
            }
        });
        checker.start();
        refreshTable();
    }

    /* ---------- 新建提醒对话框 ---------- */

    private void openEditor() {
        openEditor(true);
    }

    /** 跨页联动：用给定文字预填“新建提醒”的内容框 */
    public void createFrom(String title) {
        pendingTitle = title == null ? "" : title.trim();
        openEditor(true);
    }

    /** modal=false 时用于界面验收截图 */
    private void openEditor(boolean modal) {
        Window owner = SwingUtilities.getWindowAncestor(this);
        final JDialog dlg = new JDialog(owner, "新建提醒",
                modal ? Dialog.ModalityType.APPLICATION_MODAL : Dialog.ModalityType.MODELESS);

        final JTextField titleF = new JTextField();
        titleF.setFont(Ui.font(13, false));
        if (pendingTitle != null) {
            titleF.setText(pendingTitle);
            pendingTitle = null;
        }
        final JTextField timeF = new JTextField(new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
                .format(new Date(System.currentTimeMillis() + 3600000L)));
        timeF.setFont(Ui.font(13, false));

        final JComboBox<String> repBox = new JComboBox<String>(
                new String[]{"不重复", "每天", "每周", "每月", "工作日"});
        repBox.setFont(Ui.font(13, false));

        final JToggleButton[] weekChips = new JToggleButton[7];
        JPanel week = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        week.setOpaque(false);
        Calendar now = Calendar.getInstance();
        int curWeek = isoDay(now);
        for (int i = 0; i < 7; i++) {
            final JToggleButton b = new JToggleButton(WEEK_CN[i]);
            b.setFont(Ui.font(12, true));
            b.setFocusPainted(false);
            b.setPreferredSize(new Dimension(34, 30));
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.setSelected(i == curWeek - 1);
            styleWeekChip(b);
            b.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    styleWeekChip(b);
                }
            });
            weekChips[i] = b;
            week.add(b);
        }

        final JSpinner monthSpin = new JSpinner(new SpinnerNumberModel(15, 1, 31, 1));
        monthSpin.setFont(Ui.font(13, false));
        JPanel month = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        month.setOpaque(false);
        JLabel mL = new JLabel("每月");
        mL.setFont(Ui.font(12, false));
        mL.setForeground(Theme.muted());
        JLabel mL2 = new JLabel("号");
        mL2.setFont(Ui.font(12, false));
        mL2.setForeground(Theme.muted());
        month.add(mL);
        month.add(monthSpin);
        month.add(mL2);

        week.setVisible(false);
        month.setVisible(false);

        repBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int idx = repBox.getSelectedIndex();
                week.setVisible(idx == 2);
                month.setVisible(idx == 3);
                dlg.pack();
            }
        });

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.add(fieldRow("内容", titleF));
        form.add(Box.createVerticalStrut(10));
        form.add(fieldRow("时间", timeF));
        form.add(Box.createVerticalStrut(4));
        JLabel tip = new JLabel("格式 yyyy-MM-dd HH:mm，首次触发时间需晚于现在");
        tip.setFont(Ui.font(10, false));
        tip.setForeground(Theme.muted());
        tip.setAlignmentX(Component.LEFT_ALIGNMENT);
        tip.setBorder(BorderFactory.createEmptyBorder(0, 66, 0, 0));
        form.add(tip);
        form.add(Box.createVerticalStrut(12));
        form.add(fieldRow("重复", repBox));
        form.add(Box.createVerticalStrut(6));
        week.setAlignmentX(Component.LEFT_ALIGNMENT);
        month.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(week);
        form.add(month);
        form.add(Box.createVerticalStrut(18));

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setOpaque(false);
        JButton cancel = Ui.secondary("取消");
        final JButton ok = Ui.primary("保存");
        ok.setPreferredSize(new Dimension(84, 34));
        btns.add(cancel);
        btns.add(ok);
        btns.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(btns);

        dlg.setContentPane(new JPanel(new BorderLayout()) {{
            setBackground(Theme.bg());
            setBorder(BorderFactory.createEmptyBorder(18, 18, 16, 18));
            add(form, BorderLayout.CENTER);
        }});
        dlg.pack();
        dlg.setLocationRelativeTo(this);
        try {
            dlg.setIconImage(AppIcon.image(64));
        } catch (Exception ignored) {
        }

        cancel.addActionListener(e -> dlg.dispose());
        ok.addActionListener(e -> {
            String title = titleF.getText().trim();
            if (title.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "请输入提醒内容");
                return;
            }
            long t;
            try {
                t = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
                        .parse(timeF.getText().trim()).getTime();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "时间格式应为 yyyy-MM-dd HH:mm");
                return;
            }
            if (t <= System.currentTimeMillis()) {
                JOptionPane.showMessageDialog(dlg, "首次提醒时间已过，请重新选择");
                return;
            }
            String rule;
            int idx = repBox.getSelectedIndex();
            if (idx == 1) {
                rule = "daily";
            } else if (idx == 2) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < 7; i++) {
                    if (weekChips[i].isSelected()) {
                        if (sb.length() > 0) sb.append(',');
                        sb.append(i + 1);
                    }
                }
                if (sb.length() == 0) {
                    JOptionPane.showMessageDialog(dlg, "请至少选择一个星期");
                    return;
                }
                rule = "weekly:" + sb;
            } else if (idx == 3) {
                rule = "monthly:" + monthSpin.getValue();
            } else if (idx == 4) {
                rule = "workday";
            } else {
                rule = "none";
            }

            Item it = new Item();
            it.id = seq++;
            it.title = title;
            it.time = t;
            it.rule = rule;
            items.add(it);
            save();
            refreshTable();
            dlg.dispose();
        });
        dlg.setVisible(true);
    }

    private JPanel fieldRow(String label, JComponent comp) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel l = new JLabel(label);
        l.setFont(Ui.font(12, true));
        l.setForeground(Theme.text());
        l.setPreferredSize(new Dimension(58, 28));
        row.add(l, BorderLayout.WEST);
        comp.setPreferredSize(new Dimension(300, 32));
        row.add(comp, BorderLayout.CENTER);
        return row;
    }

    private void styleWeekChip(JToggleButton b) {
        if (b.isSelected()) {
            b.setBackground(Ui.PRIMARY);
            b.setForeground(Color.WHITE);
        } else {
            b.setBackground(Theme.isDark() ? new Color(0x2A2D38) : new Color(0xEEF2FF));
            b.setForeground(Theme.isDark() ? new Color(0xAFC0FF) : Ui.PRIMARY);
        }
        b.setFocusPainted(false);
    }

    /* ---------- 触发与重复计算 ---------- */

    private void check() {
        if (!Main.isEnabled("schedule")) return;
        long now = System.currentTimeMillis();
        boolean changed = false;
        for (Iterator<Item> it = items.iterator(); it.hasNext(); ) {
            Item x = it.next();
            /* 提前 5 分钟：只发一次托盘气泡 */
            long lead = x.time - now;
            if (lead > 0 && lead <= 5 * 60000L && preAired.add(x.time)) {
                int min = (int) Math.ceil(lead / 60000.0);
                Alerts.pre("日程提醒", x.title + " · " + min + " 分钟后开始");
            }
            if (x.time > 0 && now >= x.time) {
                preAired.remove(x.time);
                Alerts.fire("日程提醒", x.title);
                long nx = nextTime(x);
                if (nx > 0) x.time = nx;
                else it.remove();
                changed = true;
            }
        }
        if (changed) {
            save();
            refreshTable();
        }
        /* 清掉已经不在列表里的时间点，避免改时间后残留 */
        java.util.Set<Long> alive = new java.util.HashSet<Long>();
        for (Item x : items) {
            if (x.time > 0) alive.add(x.time);
        }
        preAired.retainAll(alive);
    }

    private long nextTime(Item it) {
        return nextOccurrence(it.time, it.rule);
    }

    /** 按重复规则计算下一次触发时间；0 表示不再重复 */
    public static long nextOccurrence(long time, String rule) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(time);
        String r = rule == null ? "none" : rule;

        if ("daily".equals(r)) {
            c.add(Calendar.DAY_OF_MONTH, 1);
            return c.getTimeInMillis();
        }
        if ("workday".equals(r)) {
            for (int i = 0; i < 10; i++) {
                c.add(Calendar.DAY_OF_MONTH, 1);
                int dow = c.get(Calendar.DAY_OF_WEEK);
                if (dow != Calendar.SATURDAY && dow != Calendar.SUNDAY) return c.getTimeInMillis();
            }
            return 0;
        }
        if (r.startsWith("weekly:")) {
            Set<Integer> days = new HashSet<Integer>();
            for (String s : r.substring("weekly:".length()).split(",")) {
                try {
                    days.add(Integer.parseInt(s.trim()));
                } catch (Exception ignored) {
                }
            }
            for (int i = 0; i < 10; i++) {
                c.add(Calendar.DAY_OF_MONTH, 1);
                if (days.contains(isoDay(c))) return c.getTimeInMillis();
            }
            return 0;
        }
        if (r.startsWith("monthly:")) {
            int day = 15;
            try {
                day = Integer.parseInt(r.substring("monthly:".length()).trim());
            } catch (Exception ignored) {
            }
            c.add(Calendar.MONTH, 1);
            int max = c.getActualMaximum(Calendar.DAY_OF_MONTH);
            c.set(Calendar.DAY_OF_MONTH, Math.min(day, max));
            return c.getTimeInMillis();
        }
        return 0;
    }

    private static int isoDay(Calendar c) {
        int d = c.get(Calendar.DAY_OF_WEEK);
        return d == Calendar.SUNDAY ? 7 : d - 1;
    }

    private static String ruleText(String rule) {
        if (rule == null || "none".equals(rule)) return "不重复";
        if ("daily".equals(rule)) return "每天";
        if ("workday".equals(rule)) return "工作日";
        if (rule.startsWith("weekly:")) {
            StringBuilder sb = new StringBuilder("每周 ");
            for (String s : rule.substring("weekly:".length()).split(",")) {
                try {
                    int d = Integer.parseInt(s.trim());
                    if (d >= 1 && d <= 7) sb.append(WEEK_CN[d - 1]).append(" ");
                } catch (Exception ignored) {
                }
            }
            return sb.toString().trim();
        }
        if (rule.startsWith("monthly:")) {
            return "每月 " + rule.substring("monthly:".length()).trim() + " 号";
        }
        return "不重复";
    }

    private void refreshTable() {
        model.setRowCount(0);
        rowIds.clear();
        rowKeys.clear();
        viewKeys.clear();
        if (view == 0) {
            for (int i = items.size() - 1; i >= 0; i--) {
                Item it = items.get(i);
                if (it.time <= 0) continue;
                model.addRow(new Object[]{fmt(it.time), it.title, ruleText(it.rule)});
                rowIds.add(it.id);
                rowKeys.add(null);
            }
        } else {
            long[] win = viewWindow();
            List<Object[]> rows = new ArrayList<>();
            for (Item it : items) {
                if (it.time <= 0) continue;
                long t = it.time;
                int guard = 0;
                while (t > 0 && t < win[0] && guard++ < 4000) t = nextOccurrence(t, it.rule);
                while (t > 0 && t <= win[1] && guard++ < 8000) {
                    String rep = wd(t);
                    if (!"不重复".equals(ruleText(it.rule))) rep = rep + " · " + ruleText(it.rule);
                    rows.add(new Object[]{t, fmtDay(t), it.title, rep, it.id});
                    long nx = nextOccurrence(t, it.rule);
                    if (nx <= t) break;
                    t = nx;
                }
            }
            rows.sort((x, y) -> Long.compare((Long) x[0], (Long) y[0]));
            viewTotal = rows.size();
            java.util.Map<String, Integer> dayCount = new java.util.HashMap<String, Integer>();
            for (Object[] r : rows) {
                String k = DAYKEY.format(new Date((Long) r[0]));
                Integer c0 = dayCount.get(k);
                dayCount.put(k, c0 == null ? 1 : c0 + 1);
            }
            String curKey = null;
            for (Object[] r : rows) {
                String k = DAYKEY.format(new Date((Long) r[0]));
                boolean f = folded.contains(k);
                viewKeys.add(k);
                if (!k.equals(curKey)) {
                    curKey = k;
                    Integer c0 = dayCount.get(k);
                    model.addRow(new Object[]{"", (f ? "▽ " : "▼ ") + groupTitle((Long) r[0], c0 == null ? 1 : c0), ""});
                    rowIds.add(-1);
                    rowKeys.add(k);
                }
                if (f) continue;
                model.addRow(new Object[]{r[1], r[2], r[3]});
                rowIds.add((Integer) r[4]);
                rowKeys.add(k);
            }
        }
        refreshCount();
        if (foldBtn != null) {
            boolean anyFolded = false;
            for (String k : viewKeys) {
                if (folded.contains(k)) {
                    anyFolded = true;
                    break;
                }
            }
            foldBtn.setText(anyFolded ? "全部展开" : "全部收起");
            foldBtn.setVisible(view != 0);
        }
        if (emptyLabel != null) {
            emptyLabel.setVisible(model.getRowCount() == 0);
            emptyLabel.setText(view == 1 ? "本周还没有安排" : (view == 2 ? "本月还没有安排" : "还没有提醒 · 点左上角「+ 新建提醒」开始"));
        }
    }

    /** 当前视图的时间窗口 [起, 止] */
    private long[] viewWindow() {
        Calendar c = Calendar.getInstance();
        if (view == 2) {
            c.set(Calendar.DAY_OF_MONTH, 1);
        } else {
            c.add(Calendar.DAY_OF_MONTH, -(isoDay(c) - 1));
        }
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        long from = c.getTimeInMillis();
        long to = view == 2
                ? from + c.getActualMaximum(Calendar.DAY_OF_MONTH) * 86400000L - 1
                : from + 7L * 86400000L - 1;
        return new long[]{from, to};
    }

    /** 分组标题：今天 / 明天 / MM-dd 周X，附当天条数 */
    private static String groupTitle(long t, int count) {
        String key = DAYKEY.format(new Date(t));
        String todayKey = DAYKEY.format(new Date());
        String tomorrowKey = DAYKEY.format(new Date(System.currentTimeMillis() + 86400000L));
        String day;
        if (key.equals(todayKey)) {
            day = "今天";
        } else if (key.equals(tomorrowKey)) {
            day = "明天";
        } else {
            day = new SimpleDateFormat("MM-dd", Locale.CHINA).format(new Date(t));
        }
        return day + " · " + wd(t) + " · " + count + " 条";
    }

    private static String wd(long t) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(t);
        return "周" + WEEK_CN[isoDay(c) - 1];
    }

    private static String fmtDay(long t) {
        return new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(new Date(t));
    }

    private void styleView() {
        for (JToggleButton b : viewBtns) {
            if (b == null) continue;
            boolean on = b.isSelected();
            b.setBackground(on ? Ui.PRIMARY
                    : (Theme.isDark() ? new Color(0x2A2D38) : new Color(0xEEF2FF)));
            b.setForeground(on ? Color.WHITE
                    : (Theme.isDark() ? new Color(0xAFC0FF) : Ui.PRIMARY));
        }
        if (foldBtn != null) foldBtn.setVisible(view != 0);
    }

    private void refreshCount() {
        int n = 0;
        for (Item it : items) if (it.time > 0) n++;
        if (view == 0) countLabel.setText("共 " + n + " 条提醒");
        else countLabel.setText((view == 1 ? "本周 " : "本月 ") + viewTotal + " 条安排");
        if (summaryLabel != null) summaryLabel.setText(summaryText());
    }

    /** 本周（按天分布）与本月各有多少条提醒，重复规则已展开计入 */
    private String summaryText() {
        Calendar now = Calendar.getInstance();
        int iso = isoDay(now);
        Calendar mon = (Calendar) now.clone();
        mon.add(Calendar.DAY_OF_MONTH, -(iso - 1));
        mon.set(Calendar.HOUR_OF_DAY, 0);
        mon.set(Calendar.MINUTE, 0);
        mon.set(Calendar.SECOND, 0);
        mon.set(Calendar.MILLISECOND, 0);
        long from = mon.getTimeInMillis();
        long to = from + 7L * 86400000L - 1;

        String[] wname = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        int[] perDay = new int[7];
        for (Item it : items) {
            if (it.time <= 0) continue;
            long t = it.time;
            int guard = 0;
            while (t > 0 && t < from && guard++ < 3000) t = nextOccurrence(t, it.rule);
            while (t > 0 && t <= to && guard++ < 6000) {
                int idx = (int) ((t - from) / 86400000L);
                if (idx >= 0 && idx < 7) perDay[idx]++;
                t = nextOccurrence(t, it.rule);
            }
        }
        int weekN = 0;
        StringBuilder dist = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            weekN += perDay[i];
            if (perDay[i] > 0) {
                if (dist.length() > 0) dist.append(" · ");
                dist.append(wname[i]).append(" ").append(perDay[i]);
            }
        }

        Calendar first = Calendar.getInstance();
        first.set(Calendar.DAY_OF_MONTH, 1);
        first.set(Calendar.HOUR_OF_DAY, 0);
        first.set(Calendar.MINUTE, 0);
        first.set(Calendar.SECOND, 0);
        first.set(Calendar.MILLISECOND, 0);
        long mFrom = first.getTimeInMillis();
        long mTo = mFrom + first.getActualMaximum(Calendar.DAY_OF_MONTH) * 86400000L - 1;
        int monthN = 0;
        for (Item it : items) {
            if (it.time > 0) monthN += countIn(mFrom, mTo, it);
        }

        String s = "本周 " + weekN + " 条";
        if (dist.length() > 0) s += "（" + dist + "）";
        s += " · 本月 " + monthN + " 条";
        if (weekN == 0 && monthN == 0) s += " · 还没有安排";
        return s;
    }

    private int countIn(long from, long to, Item it) {
        long t = it.time;
        int guard = 0;
        while (t > 0 && t < from && guard++ < 3000) t = nextOccurrence(t, it.rule);
        int n = 0;
        while (t > 0 && t <= to && guard++ < 6000) {
            n++;
            t = nextOccurrence(t, it.rule);
        }
        return n;
    }

    private String fmt(long ms) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(new Date(ms));
    }

    /* ---------- 倒数日 ---------- */

    /** 重画顶部那排倒数日小标签 */
    void renderDaysLeft() {
        if (daysChips == null) return;
        daysChips.removeAll();
        JLabel head = new JLabel("倒数日");
        head.setFont(Ui.font(12, true));
        head.setForeground(Theme.muted());
        daysChips.add(head);

        List<String[]> items = DaysLeft.items();
        if (items.isEmpty()) {
            JLabel empty = new JLabel("还没加 · 点右边「＋ 添加」，考试 / 截止 / 生日都行");
            empty.setFont(Ui.font(12, false));
            empty.setForeground(Theme.muted());
            daysChips.add(empty);
        } else {
            int show = Math.min(items.size(), 3);
            for (int i = 0; i < show; i++) {
                final int idx = i;
                String[] it = items.get(i);
                final int left = DaysLeft.daysFromToday(it[1]);
                JLabel chip = new JLabel(DaysLeft.text(it[0], it[1]));
                chip.setFont(Ui.font(12, false));
                chip.setOpaque(true);
                chip.setBackground(Ui.navHoverBg());
                if (left < 0) chip.setForeground(Theme.muted());
                else if (left <= 3) chip.setForeground(Ui.DANGER);
                else chip.setForeground(Theme.text());
                chip.setBorder(BorderFactory.createEmptyBorder(3, 9, 3, 9));
                chip.setToolTipText(it[1] + "　右键可删除");
                chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                chip.addMouseListener(new MouseAdapter() {
                    public void mousePressed(MouseEvent e) {
                        if (!SwingUtilities.isRightMouseButton(e)) return;
                        int r = JOptionPane.showConfirmDialog(SchedulePanel.this,
                                "删掉「" + it[0] + "」这个倒数日？", "删除倒数日",
                                JOptionPane.OK_CANCEL_OPTION);
                        if (r == JOptionPane.OK_OPTION) {
                            DaysLeft.remove(idx);
                            renderDaysLeft();
                        }
                    }
                });
                daysChips.add(chip);
            }
            if (items.size() > show) {
                JLabel more = new JLabel("+" + (items.size() - show) + " 个");
                more.setFont(Ui.font(12, false));
                more.setForeground(Theme.muted());
                daysChips.add(more);
            }
        }

        daysChips.revalidate();
        daysChips.repaint();
    }

    /** 加一条倒数日：先问名字，再选日期 */
    void openAddDays() {
        String t = (String) JOptionPane.showInputDialog(this,
                "这个日子叫什么？（如 四级考试 / 交材料 / 生日）",
                "添加倒数日", JOptionPane.PLAIN_MESSAGE, null, null, "");
        if (t == null || t.trim().isEmpty()) return;
        LocalDate picked = DatePicker.pick(this, LocalDate.now().plusDays(7));
        if (picked == null) return;
        if (!DaysLeft.add(t.trim(), picked.toString())) {
            Main.notifyMsg("没加上", "同样的名字和日期已经有了");
            return;
        }
        renderDaysLeft();
        Main.notifyMsg("已添加", DaysLeft.text(t.trim(), picked.toString()));
    }

    private void load() {
        for (String line : Store.readLines("schedule.txt")) {
            String[] p = line.split("\t", -1);
            try {
                Item it = new Item();
                it.id = Integer.parseInt(p[0]);
                it.title = p.length > 1 ? p[1] : "";
                it.time = Long.parseLong(p.length > 2 ? p[2] : "0");
                if (p.length > 3 && !p[3].trim().isEmpty()) {
                    String r = p[3].trim();
                    if (r.matches("\\d+")) {
                        int old = Integer.parseInt(r);
                        it.rule = old == 1 ? "daily" : old == 7 ? "weekly:" + weekOf(it.time) : "none";
                    } else {
                        it.rule = r;
                    }
                }
                if (it.time > 0) items.add(it);
                if (it.id >= seq) seq = it.id + 1;
            } catch (Exception ignored) {
            }
        }
    }

    private int weekOf(long ms) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ms);
        return isoDay(c);
    }

    private void save() {
        List<String> out = new ArrayList<String>();
        for (Item it : items) {
            if (it.time <= 0) continue;
            out.add(it.id + "\t" + it.title + "\t" + it.time + "\t" + it.rule);
        }
        Store.writeLines("schedule.txt", out);
    }
}
