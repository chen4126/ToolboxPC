package com.toolbox.pc;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/** 计数器：支持多组并存，每组独立计数与步长 */
public class CounterPanel extends JPanel {

    private static class Group {
        String name;
        long value;
        long step;

        Group(String name, long value, long step) {
            this.name = name;
            this.value = value;
            this.step = step;
        }
    }

    private final List<Group> groups = new ArrayList<Group>();
    private Group cur;
    private JComboBox<String> groupBox;
    private JLabel nameLabel, countLabel, sumLabel;
    private JButton step1, step5, step10, minusBtn, plusBtn;
    private boolean syncing = false;

    public CounterPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(Theme.bg());
        load();

        /* 顶部：计数组选择 */
        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.setOpaque(false);
        JLabel gl = new JLabel("计数组:");
        gl.setFont(Ui.font(12, false));
        gl.setForeground(Theme.muted());
        left.add(gl);
        groupBox = new JComboBox<String>();
        groupBox.setPreferredSize(new Dimension(168, 32));
        left.add(groupBox);
        JButton newBtn = new JButton("+ 新建组");
        JButton delGroupBtn = new JButton("删除组");
        left.add(newBtn);
        left.add(delGroupBtn);

        sumLabel = new JLabel(" ");
        sumLabel.setFont(Ui.font(11, false));
        sumLabel.setForeground(Theme.muted());
        top.add(left, BorderLayout.WEST);
        top.add(sumLabel, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        /* 中间：组名 + 大数字 */
        nameLabel = new JLabel(" ");
        nameLabel.setFont(Ui.font(15, true));
        nameLabel.setForeground(Theme.muted());
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        nameLabel.setHorizontalAlignment(SwingConstants.CENTER);

        countLabel = new JLabel("0", SwingConstants.CENTER);
        countLabel.setFont(new Font("SansSerif", Font.BOLD, 96));
        countLabel.setForeground(Theme.isDark() ? new Color(0xAFC0FF) : Ui.PRIMARY);
        countLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel mid = new JPanel();
        mid.setOpaque(false);
        mid.setLayout(new BoxLayout(mid, BoxLayout.Y_AXIS));
        mid.add(Box.createVerticalGlue());
        mid.add(nameLabel);
        mid.add(Box.createVerticalStrut(4));
        mid.add(countLabel);
        mid.add(Box.createVerticalGlue());
        add(mid, BorderLayout.CENTER);

        /* 底部：步长 + 按钮 */
        JPanel south = new JPanel(new BorderLayout(10, 10));
        south.setOpaque(false);

        JPanel stepRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        stepRow.setOpaque(false);
        JLabel sl = new JLabel("步长:");
        sl.setFont(Ui.font(12, false));
        sl.setForeground(Theme.muted());
        stepRow.add(sl);
        step1 = makeStepButton("1", 1);
        step5 = makeStepButton("5", 5);
        step10 = makeStepButton("10", 10);
        stepRow.add(step1);
        stepRow.add(step5);
        stepRow.add(step10);
        south.add(stepRow, BorderLayout.NORTH);

        JPanel btnRow = new JPanel(new GridLayout(1, 3, 10, 0));
        btnRow.setOpaque(false);
        minusBtn = new JButton("-1");
        minusBtn.setFont(new Font("SansSerif", Font.BOLD, 26));
        minusBtn.setBackground(new Color(0xE53935));
        minusBtn.setForeground(Color.WHITE);
        plusBtn = new JButton("+1");
        plusBtn.setFont(new Font("SansSerif", Font.BOLD, 26));
        plusBtn.setBackground(new Color(0x43A047));
        plusBtn.setForeground(Color.WHITE);
        JButton reset = new JButton("清零");
        reset.setFont(new Font("SansSerif", Font.BOLD, 18));
        btnRow.add(minusBtn);
        btnRow.add(reset);
        btnRow.add(plusBtn);
        south.add(btnRow, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

        /* 事件 */
        minusBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cur.value -= cur.step;
                refresh();
            }
        });
        plusBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cur.value += cur.step;
                refresh();
            }
        });
        reset.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (JOptionPane.showConfirmDialog(CounterPanel.this,
                        "把「" + cur.name + "」清零？", "确认", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    cur.value = 0;
                    refresh();
                }
            }
        });

        groupBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (syncing) return;
                int i = groupBox.getSelectedIndex();
                if (i >= 0 && i < groups.size()) {
                    cur = groups.get(i);
                    refresh();
                }
            }
        });

        newBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String name = JOptionPane.showInputDialog(CounterPanel.this,
                        "新计数组的名称：", "计数组 " + (groups.size() + 1));
                if (name == null) return;
                name = name.trim();
                if (name.isEmpty()) return;
                for (Group g : groups) {
                    if (g.name.equals(name)) {
                        JOptionPane.showMessageDialog(CounterPanel.this, "已经有同名计数组了");
                        return;
                    }
                }
                Group g = new Group(name, 0, 1);
                groups.add(g);
                cur = g;
                save();
                rebuildCombo();
                refresh();
            }
        });

        delGroupBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (groups.size() <= 1) {
                    JOptionPane.showMessageDialog(CounterPanel.this, "至少要保留一个计数组");
                    return;
                }
                if (JOptionPane.showConfirmDialog(CounterPanel.this,
                        "删除计数组「" + cur.name + "」及其数值？", "确认", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
                    return;
                groups.remove(cur);
                cur = groups.get(0);
                save();
                rebuildCombo();
                refresh();
            }
        });

        rebuildCombo();
        refresh();
    }

    private JButton makeStepButton(String label, final long s) {
        JButton b = new JButton(label);
        b.setPreferredSize(new Dimension(52, 30));
        b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cur.step = s;
                save();
                refresh();
            }
        });
        return b;
    }

    private void load() {
        for (String line : Store.readLines("counter.txt")) {
            if (line.trim().isEmpty()) continue;
            String[] p = line.split("\t", -1);
            if (p.length >= 2) {
                long v = parseLong(p[1], 0);
                long s = p.length >= 3 ? parseLong(p[2], 1) : 1;
                if (s <= 0) s = 1;
                groups.add(new Group(p[0].trim(), v, s));
            } else {
                // 旧格式：单行即一个数字
                groups.add(new Group("计数器", parseLong(line.trim(), 0), 1));
            }
        }
        if (groups.isEmpty()) groups.add(new Group("计数器", 0, 1));
        cur = groups.get(0);
    }

    private void save() {
        List<String> out = new ArrayList<String>();
        for (Group g : groups) out.add(g.name + "\t" + g.value + "\t" + g.step);
        Store.writeLines("counter.txt", out);
    }

    private void rebuildCombo() {
        syncing = true;
        groupBox.removeAllItems();
        for (Group g : groups) groupBox.addItem(g.name);
        groupBox.setSelectedIndex(groups.indexOf(cur));
        syncing = false;
    }

    private void refresh() {
        nameLabel.setText(cur.name);
        countLabel.setText(String.valueOf(cur.value));
        minusBtn.setText("-" + cur.step);
        plusBtn.setText("+" + cur.step);
        sumLabel.setText("共 " + groups.size() + " 组");
        highlight(step1, cur.step == 1);
        highlight(step5, cur.step == 5);
        highlight(step10, cur.step == 10);
        save();
    }

    private void highlight(JButton b, boolean on) {
        if (on) {
            b.setBackground(Ui.PRIMARY);
            b.setForeground(Color.WHITE);
            b.setOpaque(true);
        } else {
            b.setBackground(null);
            b.setForeground(Theme.text());
        }
        b.repaint();
    }

    private long parseLong(String s, long def) {
        try {
            return Long.parseLong(s.trim());
        } catch (Exception e) {
            return def;
        }
    }
}
