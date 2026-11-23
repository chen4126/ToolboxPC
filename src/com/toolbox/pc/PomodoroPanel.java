package com.toolbox.pc;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PomodoroPanel extends JPanel {

    private javax.swing.Timer ticker;
    private long remainMs;
    private boolean running = false;
    private int mode = 0;      // 0=专注 1=休息
    private int focusMin = 25;
    private int breakMin = 5;
    private boolean autoNext = true;
    private String today;
    private int todayCount = 0;
    /** 今天的番茄目标数，1-24，只用来算进度环 */
    private int goal = 8;
    private Ring ring;
    private JLabel goalLabel;

    private JLabel timeLabel;
    private JLabel statLabel;
    private JButton startBtn, modeFocusBtn, modeBreakBtn, minusBtn, plusBtn;
    private JLabel minLabel;
    private JPanel presetRow;
    private JTextField taskField;
    private String taskName = "";
    /** 「番茄记录」弹窗只留一个：再点按钮把已有的提到最前，不再叠窗 */
    private JDialog historyDlg;

    public PomodoroPanel() {
        load();
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setBackground(Theme.bg());
        remainMs = focusMin * 60000L;

        JPanel north = new JPanel(new GridLayout(1, 2, 8, 0));
        north.setOpaque(false);
        modeFocusBtn = makeModeButton("专注", 0);
        modeBreakBtn = makeModeButton("休息", 1);
        north.add(modeFocusBtn);
        north.add(modeBreakBtn);
        add(north, BorderLayout.NORTH);

        timeLabel = new JLabel("", SwingConstants.CENTER);
        timeLabel.setFont(new Font("SansSerif", Font.BOLD, 86));
        timeLabel.setForeground(Theme.text());

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        JPanel face = timeFace();
        face.setPreferredSize(new Dimension(460, 220));
        center.add(face);
        add(center, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout(0, 8));
        south.setOpaque(false);

        JPanel topArea = new JPanel(new BorderLayout(0, 6));
        topArea.setOpaque(false);

        JPanel up = new JPanel();
        up.setOpaque(false);
        up.setLayout(new BoxLayout(up, BoxLayout.Y_AXIS));

        JPanel taskRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        taskRow.setOpaque(false);
        taskRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel tl = new JLabel("当前任务:");
        tl.setFont(Ui.font(12, false));
        tl.setForeground(Theme.muted());
        taskField = new JTextField(18);
        taskField.setFont(Ui.font(12, false));
        taskField.setText(taskName);
        taskField.setMaximumSize(new Dimension(260, 30));
        taskField.setToolTipText("这段专注在做什么，会写进番茄记录");
        taskField.putClientProperty("JTextField.placeholderText", "这次专注做什么？可留空");
        taskRow.add(tl);
        taskRow.add(taskField);

        presetRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        presetRow.setOpaque(false);
        presetRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        up.add(taskRow);
        up.add(Box.createVerticalStrut(8));
        up.add(presetRow);
        topArea.add(up, BorderLayout.NORTH);

        JPanel adjust = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 2));
        adjust.setOpaque(false);
        minusBtn = new JButton("-1分钟");
        plusBtn = new JButton("+1分钟");
        minLabel = new JLabel(" ");
        JButton historyBtn = new JButton("番茄记录");
        historyBtn.setToolTipText("看今天和本周的专注记录，勾几条就能加进备忘录");
        historyBtn.setFont(Ui.font(11, false));
        historyBtn.setMargin(new java.awt.Insets(2, 10, 2, 10));
        historyBtn.setFocusPainted(false);
        historyBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        historyBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                openHistory();
            }
        });
        adjust.add(minusBtn);
        adjust.add(minLabel);
        adjust.add(plusBtn);
        adjust.add(historyBtn);
        topArea.add(adjust, BorderLayout.CENTER);
        south.add(topArea, BorderLayout.NORTH);

        JPanel btns = new JPanel(new GridLayout(1, 2, 10, 0));
        btns.setOpaque(false);
        startBtn = new JButton("开始");
        startBtn.setFont(new Font("SansSerif", Font.BOLD, 20));
        startBtn.setBackground(Ui.PRIMARY);
        startBtn.setForeground(Color.WHITE);
        startBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JButton resetBtn = new JButton("重置");
        resetBtn.setFont(new Font("SansSerif", Font.BOLD, 18));
        resetBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btns.add(startBtn);
        btns.add(resetBtn);
        south.add(btns, BorderLayout.CENTER);

        statLabel = new JLabel(" ", SwingConstants.CENTER);
        final JCheckBox autoBox = new JCheckBox("专注结束自动开始休息");
        autoBox.setFont(Ui.font(11, false));
        autoBox.setOpaque(false);
        autoBox.setSelected(autoNext);
        autoBox.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        autoBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                autoNext = autoBox.isSelected();
                save();
            }
        });
        JPanel bottom = new JPanel(new BorderLayout(12, 0));
        bottom.setOpaque(false);

        /* 左边：今日进度环（完成数 / 目标数） */
        JPanel ringWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        ringWrap.setOpaque(false);
        ring = new Ring();
        ringWrap.add(ring);
        bottom.add(ringWrap, BorderLayout.WEST);

        JPanel midRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 18));
        midRow.setOpaque(false);
        midRow.add(statLabel);
        midRow.add(autoBox);
        bottom.add(midRow, BorderLayout.CENTER);

        /* 右边：目标番茄数可调 */
        JPanel goalWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 16));
        goalWrap.setOpaque(false);
        JLabel goalText = new JLabel("每日目标");
        goalText.setFont(Ui.font(11, false));
        goalText.setForeground(Theme.muted());
        JButton goalMinus = new JButton("−");
        JButton goalPlus = new JButton("+");
        goalLabel = new JLabel(String.valueOf(goal));
        goalLabel.setFont(Ui.font(12, true));
        for (JButton b : new JButton[]{goalMinus, goalPlus}) {
            b.setFont(Ui.font(12, true));
            b.setMargin(new java.awt.Insets(2, 8, 2, 8));
            b.setFocusPainted(false);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        goalMinus.setToolTipText("目标减一个");
        goalPlus.setToolTipText("目标加一个");
        goalMinus.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                changeGoal(-1);
            }
        });
        goalPlus.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                changeGoal(1);
            }
        });
        goalWrap.add(goalText);
        goalWrap.add(goalMinus);
        goalWrap.add(goalLabel);
        goalWrap.add(goalPlus);
        bottom.add(goalWrap, BorderLayout.EAST);
        south.add(bottom, BorderLayout.SOUTH);
        add(south, BorderLayout.SOUTH);

        startBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (running) pause();
                else start();
            }
        });
        resetBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                reset();
            }
        });
        minusBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                changeMin(-1);
            }
        });
        plusBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                changeMin(1);
            }
        });
        modeFocusBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                switchMode(0);
            }
        });
        modeBreakBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                switchMode(1);
            }
        });

        ticker = new javax.swing.Timer(250, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (running) {
                    remainMs -= 250;
                    if (remainMs <= 0) {
                        remainMs = 0;
                        done();
                    }
                    updateTime();
                }
            }
        });
        ticker.start();
        renderMode();
        updateTime();
        updateStat();
    }

    private void start() {
        if (remainMs <= 0) remainMs = currentMin() * 60000L;
        if (mode == 0 && taskField != null) {
            String t = taskField.getText().trim();
            if (!t.equals(taskName)) {
                taskName = t;
                save();
            }
        }
        running = true;
        startBtn.setText("暂停");
    }

    private void pause() {
        running = false;
        startBtn.setText("继续");
    }

    private void reset() {
        running = false;
        remainMs = currentMin() * 60000L;
        startBtn.setText("开始");
        updateTime();
    }

    private void done() {
        running = false;
        startBtn.setText("开始");
        boolean wasFocus = mode == 0;
        if (wasFocus) {
            addCount();
            logRecord(focusMin, "专注");
            autoMemo(focusMin);
            Alerts.fire("番茄钟", "专注完成！");
        } else {
            logRecord(breakMin, "休息");
            Alerts.fire("番茄钟", "休息结束！");
        }
        switchMode(wasFocus ? 1 : 0);
        if (wasFocus && autoNext) {
            remainMs = breakMin * 60000L;
            running = true;
            startBtn.setText("暂停");
            updateTime();
        }
    }

    /** 每次完成写一条历史记录：时间戳、日期时间、时长分钟、类型、任务 */
    private void logRecord(int min, String type) {
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(new Date());
        String task = "";
        if ("专注".equals(type)) {
            task = taskField != null ? taskField.getText().trim() : taskName;
            if (task == null) task = "";
        }
        Store.appendLine("pomodoro_log.txt",
                System.currentTimeMillis() + "\t" + now + "\t" + min + "\t" + type + "\t" + task);
    }

    /** 一轮专注结束后顺手在备忘录留一条，标签固定「番茄」；设置里可以关掉 */
    private void autoMemo(int min) {
        if (!Store.flag("pomoMemo", true)) return;
        String task = taskField != null ? taskField.getText().trim() : taskName;
        if (task == null) task = "";
        String text = "专注 " + min + " 分钟" + (task.isEmpty() ? "" : " · " + task);
        String time = new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(new Date());
        Store.appendLine("memo.txt", "0\t" + time + "\t" + text + "\t番茄");
        MemoPanel mp = Main.page(MemoPanel.class);
        if (mp != null) mp.reloadFromDisk();
    }

    /** 本周一（周一算第一天） */
    public static String weekStart() {
        java.time.LocalDate t = java.time.LocalDate.now();
        return t.minusDays(t.getDayOfWeek().getValue() - 1).toString();
    }

    /**
     * 取专注记录。scope = "today" 只看今天，"week" 看本周一到今天。
     * 每个元素是 pomodoro_log.txt 拆出来的字段数组，按时间倒序。
     */
    public List<String[]> historyRows(String scope) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(new Date());
        String from = "today".equals(scope) ? today : weekStart();
        List<String[]> out = new java.util.ArrayList<String[]>();
        for (String line : Store.readLines("pomodoro_log.txt")) {
            String[] p = line.split("\t", -1);
            if (p.length < 4 || !"专注".equals(p[3].trim())) continue;
            String t = p[1].trim();
            if (t.length() < 10) continue;
            String d = t.substring(0, 10);
            if (d.compareTo(from) < 0 || d.compareTo(today) > 0) continue;
            out.add(p);
        }
        java.util.Collections.reverse(out);
        return out;
    }

    /** 一条记录在列表里怎么显示：11-13 09:00  25 分钟 · 写周报 */
    public String historyText(String[] p) {
        String task = p.length > 4 ? p[4].trim() : "";
        String t = p[1].trim();
        return t.substring(5) + "  " + p[2].trim() + " 分钟" + (task.isEmpty() ? "" : " · " + task);
    }

    /** 一条记录写成备忘录行（四列，带「番茄」标签） */
    public String memoLineOf(String[] p) {
        String task = p.length > 4 ? p[4].trim() : "";
        String t = p[1].trim();
        return "0\t" + t.substring(5) + "\t专注 " + p[2].trim() + " 分钟"
                + (task.isEmpty() ? "" : " · " + task) + "\t番茄";
    }

    /** 把几条记录加进备忘录，返回实际加了几条 */
    public int addToMemo(List<String[]> rows) {
        if (rows == null || rows.isEmpty()) return 0;
        List<String> memo = Store.readLines("memo.txt");
        for (String[] p : rows) memo.add(memoLineOf(p));
        Store.writeLines("memo.txt", memo);
        MemoPanel mp = Main.page(MemoPanel.class);
        if (mp != null) mp.reloadFromDisk();
        return rows.size();
    }

    /** 一条记录里的任务名（没有就返回空串） */
    public String taskOf(String[] p) {
        return p != null && p.length > 4 ? p[4].trim() : "";
    }

    /**
     * 本周按任务汇总：每项 {任务名, 次数, 总分钟}，按总分钟从多到少排。
     * 没写任务名的归到「（没写任务名）」。
     */
    public List<String[]> weekSummary() {
        java.util.Map<String, int[]> acc = new java.util.LinkedHashMap<String, int[]>();
        for (String[] p : historyRows("week")) {
            String task = taskOf(p);
            if (task.isEmpty()) task = "（没写任务名）";
            int min = 0;
            try {
                min = Integer.parseInt(p[2].trim());
            } catch (Exception ignored) {
            }
            int[] a = acc.get(task);
            if (a == null) acc.put(task, a = new int[2]);
            a[0]++;
            a[1] += min;
        }
        List<String[]> out = new java.util.ArrayList<String[]>();
        for (java.util.Map.Entry<String, int[]> e : acc.entrySet()) {
            out.add(new String[]{e.getKey(), String.valueOf(e.getValue()[0]), String.valueOf(e.getValue()[1])});
        }
        out.sort(new java.util.Comparator<String[]>() {
            public int compare(String[] x, String[] y) {
                return Integer.compare(Integer.parseInt(y[2]), Integer.parseInt(x[2]));
            }
        });
        return out;
    }

    /** 汇总里一行的显示：写周报   3 次 · 125 分钟 */
    public String summaryText(String[] s) {
        return s[0] + "   " + s[1] + " 次 · " + s[2] + " 分钟";
    }

    /** 把本周汇总写成一条备忘录（四列，带「番茄」标签）；本周没记录返回 null */
    public String summaryMemoLine() {
        List<String[]> sum = weekSummary();
        if (sum.isEmpty()) return null;
        StringBuilder sb = new StringBuilder("本周专注汇总：");
        int total = 0;
        for (int i = 0; i < sum.size(); i++) {
            String[] s = sum.get(i);
            if (i > 0) sb.append(" · ");
            sb.append(s[0]).append(" ").append(s[2]).append(" 分钟（").append(s[1]).append(" 次）");
            total += Integer.parseInt(s[2]);
        }
        sb.append("，合计 ").append(total).append(" 分钟");
        String time = new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(new Date());
        return "0\t" + time + "\t" + sb + "\t番茄";
    }

    /** 点「番茄记录」：今天 / 本周 / 汇总三个视图，选中几条加进备忘录，或挑一条再专注一轮 */
    public void openHistory() {
        openHistory("today");
    }

    /** 同上，但可以指定打开时停在哪个视图：today / week / sum（统计页点卡片跳过来用） */
    public void openHistory(String startScope) {
        if (historyDlg != null && historyDlg.isShowing()) {
            historyDlg.toFront();
            historyDlg.requestFocus();
            return;
        }
        final JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this), "番茄记录",
                java.awt.Dialog.ModalityType.MODELESS);
        historyDlg = dlg;
        dlg.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent e) {
                if (historyDlg == dlg) historyDlg = null;
            }
        });
        dlg.setLayout(new BorderLayout(0, 8));
        dlg.getRootPane().setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        String s0 = startScope == null || startScope.trim().isEmpty() ? "today" : startScope.trim();
        if (!"week".equals(s0) && !"sum".equals(s0)) s0 = "today";
        final String[] scope = {s0};   // today / week / sum
        final javax.swing.DefaultListModel<String> lm = new javax.swing.DefaultListModel<String>();
        final javax.swing.JList<String> list = new javax.swing.JList<String>(lm);
        list.setFont(Ui.font(12, false));
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setFixedCellHeight(26);
        final JScrollPane sp = new JScrollPane(list);
        sp.setPreferredSize(new Dimension(400, 240));
        final JLabel tip = new JLabel(" ");
        tip.setFont(Ui.font(11, false));
        tip.setForeground(Theme.muted());

        /* keep 与列表一一对应：today/week 存原始记录，sum 存 {任务,次数,分钟} */
        final java.util.List<String[]> keep = new java.util.ArrayList<String[]>();

        final Runnable[] fill = new Runnable[1];
        fill[0] = new Runnable() {
            public void run() {
                keep.clear();
                lm.clear();
                if ("sum".equals(scope[0])) {
                    List<String[]> sum = weekSummary();
                    for (String[] s : sum) {
                        keep.add(s);
                        lm.addElement(summaryText(s));
                    }
                    tip.setText(sum.isEmpty() ? "本周还没有完成的番茄，先开一轮吧"
                            : "本周 " + sum.size() + " 个任务 · 「加到备忘录」会把整张汇总记成一条");
                } else {
                    List<String[]> rows = historyRows(scope[0]);
                    for (String[] p : rows) {
                        keep.add(p);
                        lm.addElement(historyText(p));
                    }
                    if (rows.isEmpty()) {
                        tip.setText("today".equals(scope[0]) ? "今天还没有完成的番茄，先开一轮吧"
                                : "本周还没有完成的番茄，先开一轮吧");
                    } else {
                        tip.setText("共 " + rows.size() + " 条，按住 Ctrl 可以多选，再点「加到备忘录」");
                    }
                }
            }
        };

        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        final JRadioButton todayBtn = new JRadioButton("今天", "today".equals(scope[0]));
        final JRadioButton weekBtn = new JRadioButton("本周", "week".equals(scope[0]));
        final JRadioButton sumBtn = new JRadioButton("汇总", "sum".equals(scope[0]));
        javax.swing.ButtonGroup bg = new javax.swing.ButtonGroup();
        bg.add(todayBtn);
        bg.add(weekBtn);
        bg.add(sumBtn);
        for (JRadioButton b : new JRadioButton[]{todayBtn, weekBtn, sumBtn}) {
            b.setFont(Ui.font(12, false));
            b.setOpaque(false);
        }
        ActionListener tabL = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                scope[0] = todayBtn.isSelected() ? "today" : (weekBtn.isSelected() ? "week" : "sum");
                fill[0].run();
            }
        };
        todayBtn.addActionListener(tabL);
        weekBtn.addActionListener(tabL);
        sumBtn.addActionListener(tabL);
        tabs.add(todayBtn);
        tabs.add(weekBtn);
        tabs.add(sumBtn);
        dlg.add(tabs, BorderLayout.NORTH);
        dlg.add(sp, BorderLayout.CENTER);

        JPanel bottomBar = new JPanel(new BorderLayout(8, 0));
        JButton addBtn = Ui.primary("加到备忘录");
        addBtn.setPreferredSize(new Dimension(120, 32));
        JButton againBtn = new JButton("再专注一轮");
        againBtn.setPreferredSize(new Dimension(104, 32));
        againBtn.setToolTipText("用选中这条的任务名，回番茄钟直接再开一轮");
        JButton closeBtn = new JButton("关闭");
        closeBtn.setPreferredSize(new Dimension(76, 32));
        JPanel rightBtn = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightBtn.add(addBtn);
        rightBtn.add(againBtn);
        rightBtn.add(closeBtn);
        bottomBar.add(tip, BorderLayout.CENTER);
        bottomBar.add(rightBtn, BorderLayout.EAST);
        dlg.add(bottomBar, BorderLayout.SOUTH);

        addBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if ("sum".equals(scope[0])) {
                    String line = summaryMemoLine();
                    if (line == null) {
                        tip.setText("本周还没有记录，没什么可记的");
                        return;
                    }
                    List<String> memo = Store.readLines("memo.txt");
                    memo.add(line);
                    Store.writeLines("memo.txt", memo);
                    MemoPanel mp = Main.page(MemoPanel.class);
                    if (mp != null) mp.reloadFromDisk();
                    tip.setText("已把本周汇总记进备忘录（带「番茄」标签）");
                    return;
                }
                int[] idx = list.getSelectedIndices();
                if (idx.length == 0) {
                    tip.setText("先选中一条或多条记录");
                    return;
                }
                java.util.List<String[]> pick = new java.util.ArrayList<String[]>();
                for (int i : idx) {
                    if (i >= 0 && i < keep.size()) pick.add(keep.get(i));
                }
                int n = addToMemo(pick);
                tip.setText("已加进备忘录 " + n + " 条（带「番茄」标签）");
                list.clearSelection();
            }
        });
        againBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int idx = list.getSelectedIndex();
                if (idx < 0 || idx >= keep.size()) {
                    tip.setText("先选中一条记录");
                    return;
                }
                String task = "sum".equals(scope[0]) ? keep.get(idx)[0] : taskOf(keep.get(idx));
                if (task.isEmpty() || "（没写任务名）".equals(task)) {
                    tip.setText("这条没写任务名，挑一条有任务的");
                    return;
                }
                setTaskName(task);
                dlg.dispose();
                Main.go("pomodoro");
                startFocusNow();
            }
        });
        closeBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dlg.dispose();
            }
        });

        fill[0].run();
        dlg.pack();
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    /** 设置页换时长预设时调用：同时改专注和休息两个数 */
    public void applyPreset(int focus, int brk) {
        if (focus < 1 || focus > 120 || brk < 1 || brk > 120) return;
        focusMin = focus;
        breakMin = brk;
        if (!running) {
            remainMs = currentMin() * 60000L;
            updateTime();
        }
        save();
        renderMode();
        updateStat();
    }

    private void switchMode(int m) {
        running = false;
        mode = m;
        remainMs = currentMin() * 60000L;
        startBtn.setText("开始");
        renderMode();
        updateTime();
    }

    private void changeMin(int delta) {
        int cur = currentMin() + delta;
        if (cur < 1 || cur > 120) {
            JOptionPane.showMessageDialog(this, "时长范围 1-120 分钟");
            return;
        }
        if (mode == 0) focusMin = cur;
        else breakMin = cur;
        if (!running) {
            remainMs = cur * 60000L;
            updateTime();
        } else {
            Main.notifyMsg("番茄钟", "正在倒计时，新时长下次开始生效");
        }
        save();
        renderMode();
    }

    private int currentMin() {
        return mode == 0 ? focusMin : breakMin;
    }

    /** 供测试与概览卡读取 */
    public int goal() {
        return goal;
    }

    public int todayDone() {
        return todayCount;
    }

    public void setGoal(int g) {
        if (g < 1 || g > 24) return;
        goal = g;
        save();
        updateStat();
    }

    /** 进度环比例：0-1，用来核对环画多长 */
    public double goalRatio() {
        return goal <= 0 ? 0 : Math.min(1.0, todayCount * 1.0 / goal);
    }

    /** 外部（统计页「接着专注」）把任务名填进来 */
    public void setTaskName(String t) {
        taskName = t == null ? "" : t.trim();
        if (taskField != null) taskField.setText(taskName);
        save();
    }

    public String currentTask() {
        return taskField != null ? taskField.getText().trim() : taskName;
    }

    /** 外部（统计页「接着专注」）：切回专注模式并立刻开始一轮 */
    public void startFocusNow() {
        if (running) return;
        if (mode != 0) switchMode(0);
        remainMs = focusMin * 60000L;
        updateTime();
        start();
    }

    private void renderMode() {
        styleMode(modeFocusBtn, mode == 0);
        styleMode(modeBreakBtn, mode == 1);
        minLabel.setText("专注 " + focusMin + " 分 · 休息 " + breakMin + " 分");
        renderPresets();
    }

    /** 快捷预设时长：专注与休息各一组 */
    private void renderPresets() {
        if (presetRow == null) return;
        presetRow.removeAll();
        int[] vals = mode == 0 ? new int[]{15, 25, 45, 60} : new int[]{5, 10, 15, 20};
        for (int v : vals) {
            final JButton chip = new JButton(v + " 分");
            boolean active = currentMin() == v;
            chip.setFont(Ui.font(12, true));
            chip.setBackground(active ? Ui.PRIMARY
                    : (Theme.isDark() ? new Color(0x2A2D38) : new Color(0xEEF2FF)));
            chip.setForeground(active ? Color.WHITE
                    : (Theme.isDark() ? new Color(0xAFC0FF) : Ui.PRIMARY));
            chip.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
            chip.setFocusPainted(false);
            chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            chip.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setMin(v);
                }
            });
            presetRow.add(chip);
        }
        presetRow.revalidate();
        presetRow.repaint();
    }

    private void setMin(int v) {
        if (v < 1 || v > 120) return;
        if (mode == 0) focusMin = v;
        else breakMin = v;
        if (!running) {
            remainMs = v * 60000L;
            updateTime();
        } else {
            Main.notifyMsg("番茄钟", "正在倒计时，新时长下次开始生效");
        }
        save();
        renderMode();
    }

    /** 计时表盘底衬 */
    private JPanel timeFace() {
        JPanel face = new JPanel(new BorderLayout()) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.isDark() ? new Color(0x1B1E27) : new Color(0xF4F6FB));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
                g2.setColor(Theme.isDark() ? new Color(0x2C3140) : new Color(0xE4EAF4));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 22, 22);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        face.setOpaque(false);
        face.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        face.add(timeLabel, BorderLayout.CENTER);
        return face;
    }

    private JButton makeModeButton(String label, int m) {
        return new JButton(label);
    }

    private void styleMode(JButton b, boolean active) {
        Color idleBg = Theme.isDark() ? new Color(0x2A2D38) : new Color(0xEDF0F5);
        Color idleFg = Theme.isDark() ? new Color(0x9AA3B5) : new Color(0x667085);
        b.setBackground(active ? Ui.PRIMARY : idleBg);
        b.setForeground(active ? Color.WHITE : idleFg);
        b.setFont(new Font("SansSerif", Font.BOLD, 16));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void updateTime() {
        long totalSec = Math.max(0, remainMs) / 1000;
        timeLabel.setText(String.format(Locale.CHINA, "%02d:%02d", totalSec / 60, totalSec % 60));
    }

    private void updateStat() {
        String t = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(new Date());
        if (!t.equals(today)) {
            today = t;
            todayCount = 0;
        }
        statLabel.setText("今日已完成 " + todayCount + " / " + goal + " 个番茄");
        if (goalLabel != null) goalLabel.setText(String.valueOf(goal));
        if (ring != null) {
            ring.setToolTipText("今天完成 " + todayCount + " 个，目标 " + goal + " 个");
            ring.repaint();
        }
    }

    private void changeGoal(int delta) {
        int g = goal + delta;
        if (g < 1 || g > 24) {
            Main.notifyMsg("目标范围 1-24 个", "现在已经是 " + goal + " 个了");
            return;
        }
        goal = g;
        save();
        updateStat();
    }

    /** 今天的番茄进度环：外圈底环 + 按比例画的进度弧 + 中间 "已完成/目标" */
    private class Ring extends JPanel {
        Ring() {
            setOpaque(false);
            setPreferredSize(new Dimension(58, 58));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("点一下跳统计页");
            addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    Main.go("stats");
                }
            });
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int size = Math.min(getWidth(), getHeight()) - 10;
            int x = (getWidth() - size) / 2, y = (getHeight() - size) / 2;
            g2.setStroke(new BasicStroke(7f));
            g2.setColor(Theme.isDark() ? new Color(0x2C3140) : new Color(0xE4EAF4));
            g2.drawOval(x, y, size, size);
            double ratio = goal <= 0 ? 0 : Math.min(1.0, todayCount * 1.0 / goal);
            g2.setColor(todayCount >= goal && goal > 0 ? new Color(0x10B981) : Ui.PRIMARY);
            /* 从 12 点方向顺时针画 */
            g2.drawArc(x, y, size, size, 90, (int) Math.round(-360 * ratio));
            g2.dispose();

            String txt = todayCount + "/" + goal;
            g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(Ui.font(12, true));
            g2.setColor(Theme.text());
            java.awt.font.FontRenderContext frc = g2.getFontRenderContext();
            int w = (int) g2.getFont().getStringBounds(txt, frc).getWidth();
            g2.drawString(txt, (getWidth() - w) / 2, getHeight() / 2 + 5);
            g2.dispose();
        }
    }

    private void addCount() {
        String t = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(new Date());
        if (!t.equals(today)) {
            today = t;
            todayCount = 0;
        }
        todayCount++;
        save();
        updateStat();
        if (todayCount == goal) {
            Main.notifyMsg("今日目标达成", "今天已经完成 " + goal + " 个番茄，休息一下");
        }
    }

    private void load() {
        List<String> l = Store.readLines("pomodoro.txt");
        if (l.size() > 0) {
            try {
                focusMin = Integer.parseInt(l.get(0).trim());
            } catch (Exception ignored) {
            }
        }
        if (l.size() > 1) {
            try {
                breakMin = Integer.parseInt(l.get(1).trim());
            } catch (Exception ignored) {
            }
        }
        if (l.size() > 2) today = l.get(2).trim();
        if (l.size() > 3) {
            try {
                todayCount = Integer.parseInt(l.get(3).trim());
            } catch (Exception ignored) {
            }
        }
        if (l.size() > 4) autoNext = !"0".equals(l.get(4).trim());
        if (l.size() > 5) taskName = l.get(5).trim();
        if (l.size() > 6) {
            try {
                int g = Integer.parseInt(l.get(6).trim());
                if (g >= 1 && g <= 24) goal = g;      // 老文件只有 6 行，保持默认 8
            } catch (Exception ignored) {
            }
        }
        String t = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(new Date());
        if (!t.equals(today)) {
            today = t;
            todayCount = 0;
        }
    }

    private void save() {
        Store.writeLines("pomodoro.txt", new java.util.ArrayList<String>() {{
            add(String.valueOf(focusMin));
            add(String.valueOf(breakMin));
            add(today);
            add(String.valueOf(todayCount));
            add(autoNext ? "1" : "0");
            add(taskName == null ? "" : taskName);
            add(String.valueOf(goal));
        }});
    }
}
