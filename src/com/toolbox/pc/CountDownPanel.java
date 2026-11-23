package com.toolbox.pc;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/** 倒计时：主计时器 + 预设库 + 多个任务并行运行 */
public class CountDownPanel extends JPanel {

    private static class Task {
        String name;
        long remainMs;
        long plannedMs;
        long over = -1;   // -1 表示还没到点，>=0 表示已到点后的超时毫秒
        boolean overNotified;   // 超时满 1 分的托盘提醒是否已发过
        boolean overStrong;     // 超时满 5 分的强提醒是否已发过
        JLabel timeLbl;
    }

    private JLabel timeLabel;
    private JSpinner hourSp, minSp, secSp;
    private JButton startBtn;
    private javax.swing.Timer ticker;

    private long remainMs = 0;
    private long plannedMs = 0;
    private boolean running = false;
    private boolean over = false;
    private long overMs = 0;
    private String mainName = "倒计时";
    private JLabel statusLabel;

    private final List<Task> tasks = new ArrayList<Task>();
    private final List<String[]> presets = new ArrayList<String[]>();
    private JPanel runningBox;
    private JLabel runningTitle;
    private JPanel presetBox;
    private JPanel topHolder, runningHolder, presetHolder;

    public CountDownPanel() {
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        setBackground(Theme.bg());
        loadPresets();

        buildTop();
        buildRunning();
        buildPresets();

        JPanel root = new JPanel();
        root.setOpaque(false);
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        topHolder.setAlignmentX(Component.LEFT_ALIGNMENT);
        runningHolder.setAlignmentX(Component.LEFT_ALIGNMENT);
        presetHolder.setAlignmentX(Component.LEFT_ALIGNMENT);
        topHolder.setMaximumSize(new Dimension(Integer.MAX_VALUE, 212));
        runningHolder.setMaximumSize(new Dimension(Integer.MAX_VALUE, 132));
        presetHolder.setMaximumSize(new Dimension(Integer.MAX_VALUE, 118));
        root.add(topHolder);
        root.add(Box.createVerticalStrut(10));
        root.add(runningHolder);
        root.add(Box.createVerticalStrut(8));
        root.add(presetHolder);
        root.add(Box.createVerticalGlue());

        JScrollPane outer = new JScrollPane(root);
        outer.setBorder(BorderFactory.createEmptyBorder());
        outer.setOpaque(false);
        outer.getViewport().setOpaque(false);
        outer.getVerticalScrollBar().setUnitIncrement(14);
        add(outer, BorderLayout.CENTER);

        ticker = new javax.swing.Timer(250, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                tick();
            }
        });
        ticker.start();
        updateTime();
        renderTasks();
        renderPresets();
    }

    /* ---------- 主计时器 ---------- */

    private void buildTop() {
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        JPanel faceWrap = new JPanel(new GridBagLayout());
        faceWrap.setOpaque(false);
        faceWrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        faceWrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 166));
        JPanel face = timeFace();
        face.setPreferredSize(new Dimension(430, 158));
        faceWrap.add(face);
        top.add(faceWrap);

        JPanel setRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 2));
        setRow.setOpaque(false);
        setRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        setRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        hourSp = makeSpinner(0, 23, 0);
        minSp = makeSpinner(0, 59, 5);
        secSp = makeSpinner(0, 59, 0);
        setRow.add(label("时"));
        setRow.add(hourSp);
        setRow.add(label("分"));
        setRow.add(minSp);
        setRow.add(label("秒"));
        setRow.add(secSp);
        top.add(setRow);

        JPanel btnRow = new JPanel(new GridLayout(1, 2, 10, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        startBtn = new JButton("开始");
        startBtn.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 16));
        startBtn.setBackground(Ui.PRIMARY);
        startBtn.setForeground(Color.WHITE);
        startBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JButton resetBtn = new JButton("重置");
        resetBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnRow.add(startBtn);
        btnRow.add(resetBtn);
        top.add(Box.createVerticalStrut(8));
        top.add(btnRow);

        topHolder = top;

        startBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (over) {
                    reset();
                    return;
                }
                if (running) pause();
                else start();
            }
        });
        resetBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                reset();
            }
        });
    }

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
        face.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        timeLabel = new JLabel("00:00:00", SwingConstants.CENTER);
        timeLabel.setFont(new Font("SansSerif", Font.BOLD, 72));
        timeLabel.setForeground(Theme.text());
        face.add(timeLabel, BorderLayout.CENTER);
        statusLabel = new JLabel("设置时长后点「开始」", SwingConstants.CENTER);
        statusLabel.setFont(Ui.font(11, false));
        statusLabel.setForeground(Theme.muted());
        face.add(statusLabel, BorderLayout.SOUTH);
        return face;
    }

    private JLabel label(String s) {
        JLabel l = new JLabel(s);
        l.setFont(Ui.font(12, false));
        l.setForeground(Theme.muted());
        return l;
    }

    private JSpinner makeSpinner(int min, int max, int val) {
        JSpinner s = new JSpinner(new SpinnerNumberModel(val, min, max, 1));
        JComponent editor = s.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            ((JSpinner.DefaultEditor) editor).getTextField().setColumns(2);
        }
        s.setPreferredSize(new Dimension(58, 30));
        return s;
    }

    private long totalMs() {
        return ((Number) hourSp.getValue()).longValue() * 3600000L
                + ((Number) minSp.getValue()).longValue() * 60000L
                + ((Number) secSp.getValue()).longValue() * 1000L;
    }

    private void start() {
        if (remainMs <= 0) {
            remainMs = totalMs();
            plannedMs = remainMs;
            mainName = "倒计时";
            if (remainMs <= 0) {
                JOptionPane.showMessageDialog(this, "请先设置倒计时时长");
                return;
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
        over = false;
        overMs = 0;
        remainMs = 0;
        plannedMs = 0;
        startBtn.setText("开始");
        updateTime();
    }

    /* ---------- 并行任务 ---------- */

    private void buildRunning() {
        JPanel wrap = new JPanel(new BorderLayout(0, 6));
        wrap.setOpaque(false);

        runningTitle = new JLabel("并行倒计时");
        runningTitle.setFont(Ui.font(12, true));
        runningTitle.setForeground(Theme.muted());
        wrap.add(runningTitle, BorderLayout.NORTH);

        runningBox = new JPanel();
        runningBox.setOpaque(false);
        runningBox.setLayout(new BoxLayout(runningBox, BoxLayout.Y_AXIS));
        JScrollPane sp = new JScrollPane(runningBox);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getVerticalScrollBar().setUnitIncrement(12);
        wrap.add(sp, BorderLayout.CENTER);
        runningHolder = wrap;
    }

    private void renderTasks() {
        if (runningBox == null) return;
        runningBox.removeAll();
        if (tasks.isEmpty()) {
            JLabel empty = new JLabel("暂无并行倒计时");
            empty.setFont(Ui.font(11, false));
            empty.setForeground(Theme.muted());
            empty.setBorder(BorderFactory.createEmptyBorder(6, 2, 6, 2));
            runningBox.add(empty);
        } else {
            for (final Task t : tasks) {
                JPanel row = new JPanel(new BorderLayout(10, 0));
                row.setOpaque(false);
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
                row.setAlignmentX(Component.LEFT_ALIGNMENT);
                row.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

                JLabel nameL = new JLabel(t.name);
                nameL.setFont(Ui.font(12, false));
                nameL.setForeground(Theme.text());

                t.timeLbl = new JLabel(t.over >= 0
                        ? "+" + fmt(t.over) + (t.over / 60000 >= 1 ? " · 超 " + t.over / 60000 + " 分" : "")
                        : fmt(t.remainMs));
                t.timeLbl.setFont(new Font("SansSerif", Font.BOLD, 15));   // 逻辑字体，中文才不会变方块
                t.timeLbl.setForeground(t.over >= 0 ? new Color(0xDC2626) : Ui.PRIMARY);

                JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
                right.setOpaque(false);
                JButton stop = new JButton("停止");
                stop.setFont(Ui.font(11, false));
                stop.setFocusPainted(false);
                stop.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                stop.setPreferredSize(new Dimension(62, 26));
                stop.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        tasks.remove(t);
                        renderTasks();
                    }
                });
                right.add(t.timeLbl);
                right.add(stop);

                row.add(nameL, BorderLayout.CENTER);
                row.add(right, BorderLayout.EAST);
                runningBox.add(row);
            }
        }
        runningTitle.setText(tasks.isEmpty() ? "并行倒计时" : "并行倒计时 · " + tasks.size() + " 个");
        runningBox.revalidate();
        runningBox.repaint();
    }

    /* ---------- 预设库 ---------- */

    private void buildPresets() {
        JPanel south = new JPanel();
        south.setOpaque(false);
        south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));

        JLabel t1 = new JLabel("常用");
        t1.setFont(Ui.font(11, true));
        t1.setForeground(Theme.muted());
        t1.setAlignmentX(Component.LEFT_ALIGNMENT);
        south.add(t1);

        JPanel quick = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        quick.setOpaque(false);
        quick.setAlignmentX(Component.LEFT_ALIGNMENT);
        long[] seconds = {60, 180, 300, 600, 900, 1800, 3600};
        String[] names = {"1分钟", "3分钟", "5分钟", "10分钟", "15分钟", "30分钟", "1小时"};
        for (int i = 0; i < seconds.length; i++) {
            final long sec = seconds[i];
            final String nm = names[i];
            quick.add(chip(nm, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    startPreset(nm, sec * 1000L);
                }
            }));
        }
        south.add(quick);

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        JLabel t2 = new JLabel("我的预设");
        t2.setFont(Ui.font(11, true));
        t2.setForeground(Theme.muted());
        titleRow.add(t2, BorderLayout.WEST);
        JButton save = new JButton("+ 存为预设");
        save.setFont(Ui.font(11, false));
        save.setFocusPainted(false);
        save.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        save.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveCurrentAsPreset();
            }
        });
        titleRow.add(save, BorderLayout.EAST);
        south.add(titleRow);

        presetBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        presetBox.setOpaque(false);
        presetBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        south.add(presetBox);
        presetHolder = south;
    }

    private JButton chip(String text, ActionListener l) {
        JButton b = new JButton(text);
        b.setFont(Ui.font(11, true));
        b.setBackground(Theme.isDark() ? new Color(0x2A2D38) : new Color(0xEEF2FF));
        b.setForeground(Theme.isDark() ? new Color(0xAFC0FF) : Ui.PRIMARY);
        b.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(l);
        return b;
    }

    private void renderPresets() {
        if (presetBox == null) return;
        presetBox.removeAll();
        if (presets.isEmpty()) {
            JLabel empty = new JLabel("还没有自定义预设，设好时长后点「+ 存为预设」");
            empty.setFont(Ui.font(11, false));
            empty.setForeground(Theme.muted());
            presetBox.add(empty);
        } else {
            for (int i = 0; i < presets.size(); i++) {
                final int idx = i;
                String[] pr = presets.get(i);
                String name = pr[0];
                long sec = parse(pr[1]);
                JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
                item.setOpaque(false);
                final JButton c = chip(name + " · " + shortDur(sec), new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        startPreset(name, sec * 1000L);
                    }
                });
                JButton x = new JButton("×");
                x.setFont(Ui.font(11, true));
                x.setForeground(Theme.muted());
                x.setContentAreaFilled(false);
                x.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
                x.setFocusPainted(false);
                x.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                x.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        presets.remove(idx);
                        savePresets();
                        renderPresets();
                    }
                });
                item.add(c);
                item.add(x);
                presetBox.add(item);
            }
        }
        presetBox.revalidate();
        presetBox.repaint();
    }

    private void saveCurrentAsPreset() {
        long ms = remainMs > 0 ? remainMs : totalMs();
        if (ms <= 0) {
            JOptionPane.showMessageDialog(this, "请先设置倒计时时长");
            return;
        }
        long sec = ms / 1000;
        String def = "倒计时 " + shortDur(sec);
        String name = JOptionPane.showInputDialog(this, "给这个预设起个名字：", def);
        if (name == null) return;
        name = name.trim();
        if (name.isEmpty()) name = def;
        if (name.contains("\t")) name = name.replace("\t", " ");
        presets.add(new String[]{name, String.valueOf(sec)});
        savePresets();
        renderPresets();
    }

    private void loadPresets() {
        presets.clear();
        for (String line : Store.readLines("countdown_presets.txt")) {
            String[] p = line.split("\t", -1);
            if (p.length >= 2 && !p[0].trim().isEmpty()) {
                presets.add(new String[]{p[0].trim(), p[1].trim()});
            }
        }
    }

    private void savePresets() {
        List<String> out = new ArrayList<String>();
        for (String[] p : presets) out.add(p[0] + "\t" + p[1]);
        Store.writeLines("countdown_presets.txt", out);
    }

    /* ---------- 运行 ---------- */

    /** 主计时器空闲时占用它，否则作为并行任务再开一个 */
    private void startPreset(String name, long ms) {
        if (ms <= 0) ms = 60 * 1000L;
        if (!running && remainMs <= 0 && tasks.isEmpty()) {
            mainName = name;
            remainMs = ms;
            plannedMs = ms;
            running = true;
            startBtn.setText("暂停");
            updateTime();
        } else {
            Task t = new Task();
            t.name = name;
            t.remainMs = ms;
            t.plannedMs = ms;
            tasks.add(t);
            renderTasks();
        }
    }

    private void tick() {
        if (running) {
            if (over) {
                overMs += 250;   // 到点后像秒表一样继续往上加
            } else {
                remainMs -= 250;
                if (remainMs <= 0) {
                    remainMs = 0;
                    finishMain();
                }
            }
            updateTime();
        }
        boolean changed = false;
        for (Iterator<Task> it = tasks.iterator(); it.hasNext(); ) {
            Task t = it.next();
            if (t.over < 0) {
                t.remainMs -= 250;
                if (t.remainMs <= 0) {
                    t.remainMs = 0;
                    t.over = 0;
                    log(t.plannedMs);
                    Alerts.fire("倒计时", t.name + " 到点！");
                    changed = true;
                } else if (t.timeLbl != null) {
                    t.timeLbl.setText(fmt(t.remainMs));
                }
            } else {
                t.over += 250;
                long om = t.over / 60000;
                if (!t.overNotified && t.over >= 60000L) {
                    t.overNotified = true;
                    Alerts.pre("倒计时 · 已超时", t.name + " 已超时 " + om + " 分");
                    Main.flashTitle(t.name + " 已超时 " + om + " 分");
                }
                if (!t.overStrong && t.over >= 300000L) {
                    t.overStrong = true;
                    Alerts.fire("倒计时 · 超时 5 分", t.name + " 已超时 " + (t.over / 60000) + " 分，还在计时");
                    Main.flashTitle(t.name + " 已超时 " + (t.over / 60000) + " 分");
                }
                if (t.timeLbl != null) {
                    t.timeLbl.setText("+" + fmt(t.over) + (om >= 1 ? " · 超 " + om + " 分" : ""));
                    t.timeLbl.setForeground(new Color(0xDC2626));
                }
            }
        }
        if (changed) renderTasks();
    }

    private void finishMain() {
        /* 到点不再停表，转为超时计时，直到手动重置 */
        over = true;
        overMs = 0;
        startBtn.setText("停止");
        if (plannedMs > 0) log(plannedMs);
        plannedMs = 0;
        Alerts.fire("倒计时", mainName + " 到点！");
    }

    private void log(long ms) {
        if (ms <= 0) return;
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(new Date());
        Store.appendLine("countdown_log.txt",
                System.currentTimeMillis() + "\t" + now + "\t" + (ms / 1000));
    }

    private void updateTime() {
        if (over) {
            timeLabel.setText("+" + fmtClock(overMs));
            timeLabel.setForeground(new Color(0xDC2626));
            long overMin = overMs / 60000;
            String extra = overMin >= 1 ? "（已超时 " + overMin + " 分）" : "";
            statusLabel.setText("已到点 · 超时 " + fmtClock(overMs) + extra + "　点「停止」清零");
            statusLabel.setForeground(new Color(0xDC2626));
        } else {
            timeLabel.setText(fmtClock(Math.max(0, remainMs)));
            timeLabel.setForeground(Theme.text());
            if (running) {
                statusLabel.setText("倒计时中 · " + (mainName.isEmpty() ? "倒计时" : mainName));
                statusLabel.setForeground(Theme.muted());
            } else if (remainMs > 0) {
                statusLabel.setText("已暂停 · 剩余 " + fmtClock(remainMs));
                statusLabel.setForeground(Theme.muted());
            } else {
                statusLabel.setText("设置时长后点「开始」");
                statusLabel.setForeground(Theme.muted());
            }
        }
    }

    private String fmtClock(long ms) {
        long s = Math.max(0, ms) / 1000;
        return String.format(Locale.CHINA, "%02d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60);
    }

    private String fmt(long ms) {
        long s = Math.max(0, ms) / 1000;
        if (s >= 3600) return String.format(Locale.CHINA, "%d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60);
        return String.format(Locale.CHINA, "%02d:%02d", s / 60, s % 60);
    }

    private String shortDur(long sec) {
        if (sec < 60) return sec + " 秒";
        if (sec < 3600) {
            long m = sec / 60;
            return sec % 60 == 0 ? m + " 分钟" : m + " 分 " + (sec % 60) + " 秒";
        }
        long h = sec / 3600;
        long m = (sec % 3600) / 60;
        return m == 0 ? h + " 小时" : h + " 小时 " + m + " 分";
    }

    private long parse(String s) {
        try {
            return Long.parseLong(s.trim());
        } catch (Exception e) {
            return 0;
        }
    }
}
