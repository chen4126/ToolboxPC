package com.toolbox.pc;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.util.List;

/** 习惯打卡：每天给自己打几个卡，看连续天数与本周节奏 */
public class HabitPanel extends JPanel {

    private final JPanel rows = new JPanel();
    private final JLabel summary = new JLabel(" ");

    public HabitPanel() {
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(Theme.bg());

        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        summary.setFont(Ui.font(13, true));
        summary.setForeground(Theme.text());
        left.add(summary);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        JButton addBtn = Ui.primary("+ 新习惯");
        addBtn.setPreferredSize(new Dimension(104, 32));
        addBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                addHabit();
            }
        });
        JButton undoBtn = Ui.secondary("撤销今天的打卡");
        undoBtn.setPreferredSize(new Dimension(132, 32));
        undoBtn.setToolTipText("万一误点，把今天所有习惯的打卡都撤掉");
        undoBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                for (String h : Habit.habits()) {
                    if (Habit.done(h, LocalDate.now())) Habit.toggle(h, LocalDate.now());
                }
                render();
                Main.notifyMsg("已撤销", "今天的打卡都清掉了");
            }
        });
        right.add(undoBtn);
        right.add(addBtn);
        top.add(left, BorderLayout.WEST);
        top.add(right, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.setOpaque(false);
        JScrollPane sp = new JScrollPane(rows);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getVerticalScrollBar().setUnitIncrement(18);
        add(sp, BorderLayout.CENTER);

        JLabel tip = new JLabel("每天点一下就算完成；连续天数按往前数不断档算，本月统计到今天就为止。数据存在 habit.txt / habit_log.txt");
        tip.setFont(Ui.font(11, false));
        tip.setForeground(Theme.muted());
        tip.setBorder(new EmptyBorder(4, 2, 0, 0));
        add(tip, BorderLayout.SOUTH);

        render();
    }

    /** 重画整个列表 */
    public void render() {
        rows.removeAll();
        List<String> all = Habit.habits();
        summary.setText(Habit.todayText() + (all.isEmpty() ? "" : "　·　今天是 " + LocalDate.now()));

        if (all.isEmpty()) {
            JPanel empty = new JPanel(new GridBagLayout());
            empty.setOpaque(false);
            JLabel t = new JLabel("还没有习惯 —— 点右上「+ 新习惯」，先加一个「早起」或「喝水」试试");
            t.setFont(Ui.font(13, false));
            t.setForeground(Theme.muted());
            empty.add(t);
            rows.add(empty);
            rows.revalidate();
            rows.repaint();
            return;
        }

        for (int i = 0; i < all.size(); i++) {
            rows.add(rowOf(i, all.get(i)));
            rows.add(Box.createVerticalStrut(8));
        }
        rows.revalidate();
        rows.repaint();
    }

    private JPanel rowOf(final int idx, final String name) {
        final LocalDate today = LocalDate.now();
        final boolean doneToday = Habit.done(name, today);

        JPanel row = new JPanel(new BorderLayout(12, 0)) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.isDark() ? new Color(0x1B1F2A) : Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(10, 14, 10, 12));

        /* 左侧：打卡勾选 + 名字 */
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        final JCheckBox box = new JCheckBox();
        box.setSelected(doneToday);
        box.setOpaque(false);
        box.setIconTextGap(8);
        box.setToolTipText(doneToday ? "点一下取消今天的打卡" : "点一下算今天打过了");
        box.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                boolean now = Habit.toggle(name, today);
                box.setSelected(now);
                render();
                if (now) {
                    int m = Habit.milestoneHit(name);
                    if (m > 0) {
                        Habit.markMilestone(name, m);
                        Main.notifyMsg("连续 " + m + " 天！", "「" + name + "」已经连了 " + m + " 天，稳住别断");
                    } else {
                        Main.notifyMsg("打卡完成", name);
                    }
                } else {
                    Main.notifyMsg("已取消", name);
                }
            }
        });
        JLabel nameLbl = new JLabel(name);
        nameLbl.setFont(Ui.font(14, true));
        nameLbl.setForeground(doneToday ? Theme.text() : Theme.muted());
        left.add(box);
        left.add(nameLbl);
        row.add(left, BorderLayout.WEST);

        /* 中间：连续 / 本月 / 本周 */
        JPanel mid = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        mid.setOpaque(false);
        int streak = Habit.streak(name);
        JLabel streakLbl = new JLabel("连续 " + streak + " 天");
        streakLbl.setFont(Ui.font(12, streak > 0));
        streakLbl.setForeground(streak >= 7 ? Ui.DANGER : Theme.text());
        int mDone = Habit.doneThisMonth(name);
        int mDays = Habit.daysSoFarThisMonth();
        JLabel monthLbl = new JLabel("本月 " + mDone + "/" + mDays);
        monthLbl.setFont(Ui.font(12, false));
        monthLbl.setForeground(Theme.muted());
        mid.add(streakLbl);
        mid.add(monthLbl);
        mid.add(new Dots(name));
        row.add(mid, BorderLayout.CENTER);

        /* 右侧：改名 / 删除 */
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.setOpaque(false);
        JButton ren = Ui.secondary("改名");
        ren.setFont(Ui.font(11, false));
        ren.setPreferredSize(new Dimension(56, 26));
        ren.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String t = (String) JOptionPane.showInputDialog(HabitPanel.this,
                        "改成什么名字？（最多 14 个字）", "改习惯名",
                        JOptionPane.PLAIN_MESSAGE, null, null, name);
                if (t == null || t.trim().isEmpty()) return;
                Habit.rename(idx, t.trim());
                render();
            }
        });
        JButton del = Ui.secondary("删除");
        del.setFont(Ui.font(11, false));
        del.setPreferredSize(new Dimension(56, 26));
        del.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int r = JOptionPane.showConfirmDialog(HabitPanel.this,
                        "删掉「" + name + "」？它的打卡记录也一起清掉。", "删除习惯",
                        JOptionPane.OK_CANCEL_OPTION);
                if (r != JOptionPane.OK_OPTION) return;
                Habit.remove(idx);
                render();
                Main.notifyMsg("已删除", name);
            }
        });
        right.add(ren);
        right.add(del);
        row.add(right, BorderLayout.EAST);

        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        row.setPreferredSize(new Dimension(600, 56));
        /* 点整行也能打卡，省得只盯着小方框 */
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        row.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1 && !(e.getSource() instanceof JCheckBox)) {
                    box.doClick();
                }
            }
        });

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrap.add(row, BorderLayout.CENTER);
        wrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        wrap.setPreferredSize(new Dimension(600, 56));
        return wrap;
    }

    /** 本周 7 个圆点：今天之前的实心=打过、空心=没打，今天之后淡色 */
    private static class Dots extends JComponent {
        private final String habit;

        Dots(String habit) {
            this.habit = habit;
            setPreferredSize(new Dimension(96, 20));
            setToolTipText("本周节奏：从左到右是本周一到周日，实心=当天打过");
        }

        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            LocalDate today = LocalDate.now();
            int d = 3;
            int gap = 4;
            int size = 9;
            for (int i = 0; i < 7; i++) {
                LocalDate day = Habit.weekDay(i);
                int x = i * (size + gap) + 6;
                int y = (getHeight() - size) / 2;
                boolean future = day.isAfter(today);
                boolean on = Habit.done(habit, day);
                if (future) {
                    g2.setColor(Theme.isDark() ? new Color(0x333B4D) : new Color(0xE5E7EB));
                    g2.fillOval(x, y, size, size);
                    continue;
                }
                if (on) {
                    g2.setColor(Ui.PRIMARY);
                    g2.fillOval(x, y, size, size);
                } else {
                    g2.setColor(Theme.isDark() ? new Color(0x4B5563) : new Color(0xCBD5E1));
                    g2.setStroke(new BasicStroke(1.2f));
                    g2.drawOval(x, y, size, size);
                }
            }
            g2.dispose();
        }
    }

    private void addHabit() {
        if (Habit.habits().size() >= Habit.MAX_HABITS) {
            Main.notifyMsg("到上限了", "最多 " + Habit.MAX_HABITS + " 个习惯，先删一个再建");
            return;
        }
        String t = (String) JOptionPane.showInputDialog(this,
                "要养成的习惯叫什么？（如 早起 / 喝水 / 运动 20 分钟 / 看书）",
                "新习惯", JOptionPane.PLAIN_MESSAGE, null, null, "");
        if (t == null || t.trim().isEmpty()) return;
        if (!Habit.add(t.trim())) {
            Main.notifyMsg("没加上", "名字重复了，或者超过 14 个字");
            return;
        }
        render();
        Main.notifyMsg("已添加", t.trim());
    }

    public List<String> habitNames() {
        return Habit.habits();
    }
}
