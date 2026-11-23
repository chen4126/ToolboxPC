package com.toolbox.pc;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** 记账：收支记录（带分类）+ 本月汇总 */
public class LedgerPanel extends JPanel {

    private static final String[] OUT_CATS = {"餐饮", "交通", "购物", "居住", "娱乐", "医疗", "学习", "人情", "其他"};
    private static final String[] IN_CATS = {"工资", "兼职", "生活费", "理财", "其他"};

    private DefaultTableModel model;
    private JLabel statLabel;
    private JLabel monthTitle, monthInLbl, monthOutLbl, monthBalLbl;
    private JPanel monthCatsBox;
    private List<String> lines; // 每行: type\tamount\tnote\ttime\tcategory
    private JComboBox<String> typeBox, catBox;
    /** 当前按分类筛选（null = 不筛选），点右侧分类行切换 */
    private String filterCat;
    /** 表格行号 → lines 下标，筛选后删除也能对上 */
    private final List<Integer> rowToIndex = new ArrayList<Integer>();
    private JLabel filterBar;
    private JTable table;
    private JMenuItem toggleItemRef;   // 右键菜单里的“改成收入/改成支出”项
    private JDialog importDlg;         // 导入弹窗（单例，不叠窗）

    public LedgerPanel() {
        setLayout(new BorderLayout(10, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(Theme.bg());
        lines = new ArrayList<String>(Store.readLines("ledger.txt"));

        /* 输入行 */
        JPanel input = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        input.setOpaque(false);
        typeBox = new JComboBox<String>(new String[]{"支出", "收入"});
        catBox = new JComboBox<String>(OUT_CATS);
        final JTextField amountField = new JTextField(7);
        amountField.setToolTipText("金额");
        final JTextField noteField = new JTextField(9);
        noteField.setToolTipText("备注（可选）");
        JButton addBtn = Ui.primary("记一笔");
        addBtn.setPreferredSize(new Dimension(84, 32));
        final JButton importBtn = Ui.secondary("导入");
        importBtn.setPreferredSize(new Dimension(72, 32));
        importBtn.setToolTipText("把账单文字粘进来，一行一笔，格式不限：自动识别日期、金额、分类");

        input.add(new JLabel("类型:"));
        input.add(typeBox);
        input.add(new JLabel("分类:"));
        input.add(catBox);
        input.add(new JLabel("金额:"));
        input.add(amountField);
        input.add(new JLabel("备注:"));
        input.add(noteField);
        input.add(addBtn);
        input.add(importBtn);
        add(input, BorderLayout.NORTH);

        typeBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                catBox.removeAllItems();
                String[] cs = typeBox.getSelectedIndex() == 0 ? OUT_CATS : IN_CATS;
                for (String c : cs) catBox.addItem(c);
            }
        });

        /* 表格 */
        model = new DefaultTableModel(new String[]{"时间", "类型", "分类", "备注", "金额"}, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        table = new JTable(model);
        table.setRowHeight(32);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFont(Ui.font(12, false));
        table.setSelectionBackground(Ui.PRIMARY);
        table.setSelectionForeground(Color.WHITE);
        table.setToolTipText("双击某行可改金额或备注，右键可删除单条");
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) editRow(table.rowAtPoint(e.getPoint()));
            }

            public void mousePressed(MouseEvent e) {
                selectOnPopup(e);
            }

            public void mouseReleased(MouseEvent e) {
                selectOnPopup(e);
            }

            private void selectOnPopup(MouseEvent e) {
                if (!e.isPopupTrigger()) return;
                int r = table.rowAtPoint(e.getPoint());
                if (r >= 0) table.setRowSelectionInterval(r, r);
            }
        });
        final JTable tbl = table;
        final JPopupMenu rowMenu = new JPopupMenu();
        JMenuItem copyOne = new JMenuItem("复制这条记录");
        copyOne.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                copyRow(tbl.getSelectedRow());
            }
        });
        final JMenuItem toggle = new JMenuItem("改成收入");
        toggleItemRef = toggle;
        toggle.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                toggleType(tbl.getSelectedRow());
            }
        });
        final JMenuItem delOne = new JMenuItem("删除这条记录");
        delOne.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                deleteRow(tbl.getSelectedRow());
            }
        });
        rowMenu.add(copyOne);
        rowMenu.add(toggle);
        rowMenu.addSeparator();
        rowMenu.add(delOne);
        /* 打开前按当前行是收还是支，决定第二项显示“改成收入”还是“改成支出” */
        rowMenu.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                updateToggleLabel();
            }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            }

            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });
        table.setComponentPopupMenu(rowMenu);
        table.getTableHeader().setFont(Ui.font(12, true));
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(0).setPreferredWidth(108);
        table.getColumnModel().getColumn(1).setPreferredWidth(56);
        table.getColumnModel().getColumn(2).setPreferredWidth(70);
        table.getColumnModel().getColumn(4).setPreferredWidth(96);
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(Theme.border()));

        filterBar = new JLabel(" ");
        filterBar.setFont(Ui.font(11, true));
        filterBar.setBorder(BorderFactory.createEmptyBorder(4, 2, 6, 2));
        filterBar.setVisible(false);

        JPanel tableWrap = new JPanel(new BorderLayout(0, 0));
        tableWrap.setOpaque(false);
        tableWrap.add(filterBar, BorderLayout.NORTH);
        tableWrap.add(sp, BorderLayout.CENTER);
        add(tableWrap, BorderLayout.CENTER);

        /* 本月汇总 */
        add(monthPanel(), BorderLayout.EAST);

        /* 底部：全部汇总 + 删除 */
        statLabel = new JLabel(" ");
        statLabel.setFont(Ui.font(12, false));
        statLabel.setForeground(Theme.muted());
        JButton delBtn = new JButton("删除选中");
        JPanel south = new JPanel(new BorderLayout(8, 0));
        south.setOpaque(false);
        south.add(statLabel, BorderLayout.WEST);
        south.add(delBtn, BorderLayout.EAST);
        add(south, BorderLayout.SOUTH);

        addBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String a = amountField.getText().trim();
                if (a.isEmpty()) {
                    JOptionPane.showMessageDialog(LedgerPanel.this, "请输入金额");
                    return;
                }
                double amount;
                try {
                    amount = Double.parseDouble(a);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(LedgerPanel.this, "金额格式不正确");
                    return;
                }
                if (amount <= 0) {
                    JOptionPane.showMessageDialog(LedgerPanel.this, "金额需大于0");
                    return;
                }
                int type = typeBox.getSelectedIndex();
                String note = noteField.getText().trim();
                String cat = (String) catBox.getSelectedItem();
                if (cat == null) cat = "其他";
                String time = new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(new Date());
                lines.add(type + "\t" + amount + "\t" + note + "\t" + time + "\t" + cat);
                Store.writeLines("ledger.txt", lines);
                amountField.setText("");
                noteField.setText("");
                /* 新记录的分类跟当前筛选不一致时自动取消筛选，保证刚记的一笔能看到 */
                if (filterCat != null && !filterCat.equals(cat)) filterCat = null;
                reloadTable();
            }
        });

        importBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                openImport();
            }
        });

        delBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int row = table.getSelectedRow();
                if (row < 0) {
                    JOptionPane.showMessageDialog(LedgerPanel.this, "请先选中一条记录");
                    return;
                }
                if (JOptionPane.showConfirmDialog(LedgerPanel.this, "删除这条记录？", "确认", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    if (row < rowToIndex.size()) {
                        int real = rowToIndex.get(row);
                        if (real >= 0 && real < lines.size()) lines.remove(real);
                        Store.writeLines("ledger.txt", lines);
                        reloadTable();
                    }
                }
            }
        });

        reloadTable();
    }

    private JPanel monthPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setOpaque(true);
        p.setBackground(Theme.bg());
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.border(), 1),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        p.setPreferredSize(new Dimension(258, 0));

        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        monthTitle = new JLabel("本月汇总");
        monthTitle.setFont(Ui.font(13, true));
        monthTitle.setForeground(Theme.text());
        monthTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        head.add(monthTitle);
        head.add(Box.createVerticalStrut(10));

        monthInLbl = new JLabel("¥0.00");
        monthOutLbl = new JLabel("¥0.00");
        monthBalLbl = new JLabel("¥0.00");
        JPanel rows = new JPanel();
        rows.setOpaque(false);
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.setAlignmentX(Component.LEFT_ALIGNMENT);
        rows.add(statRow("收入", monthInLbl, new Color(0x10B981)));
        rows.add(Box.createVerticalStrut(6));
        rows.add(statRow("支出", monthOutLbl, new Color(0xEF4444)));
        rows.add(Box.createVerticalStrut(6));
        rows.add(statRow("结余", monthBalLbl, Ui.PRIMARY));
        rows.add(Box.createVerticalStrut(12));
        rows.add(Ui.hline());
        rows.add(Box.createVerticalStrut(8));

        JLabel catTitle = new JLabel("支出分类");
        catTitle.setFont(Ui.font(12, true));
        catTitle.setForeground(Theme.muted());
        catTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        rows.add(catTitle);
        head.add(rows);

        monthCatsBox = new JPanel();
        monthCatsBox.setOpaque(false);
        monthCatsBox.setLayout(new BoxLayout(monthCatsBox, BoxLayout.Y_AXIS));
        JScrollPane sp = new JScrollPane(monthCatsBox);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getVerticalScrollBar().setUnitIncrement(12);

        p.add(head, BorderLayout.NORTH);
        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    private JPanel statRow(String name, JLabel value, Color color) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel n = new JLabel(name);
        n.setFont(Ui.font(11, false));
        n.setForeground(Theme.muted());
        value.setFont(new Font("SansSerif", Font.BOLD, 14));
        value.setForeground(color);
        row.add(n, BorderLayout.WEST);
        row.add(value, BorderLayout.EAST);
        return row;
    }

    /** 一个分类：名称 + 金额 + 比例条，点一下只看这一类 */
    private JPanel catRow(String name, double amount, double max) {
        JPanel row = new JPanel();
        boolean sel = name.equals(filterCat);
        row.setOpaque(sel);
        if (sel) row.setBackground(Theme.isDark() ? new Color(0x2B3350) : new Color(0xEEF2FF));
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        row.setBorder(BorderFactory.createEmptyBorder(3, sel ? 6 : 0, 3, sel ? 6 : 0));

        JPanel line = new JPanel(new BorderLayout());
        line.setOpaque(false);
        line.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel n = new JLabel(name);
        n.setFont(Ui.font(11, sel));
        n.setForeground(sel ? Ui.PRIMARY : Theme.text());
        JLabel v = new JLabel(String.format(Locale.CHINA, "¥%.2f", amount));
        v.setFont(Ui.font(11, false));
        v.setForeground(Theme.muted());
        line.add(n, BorderLayout.WEST);
        line.add(v, BorderLayout.EAST);

        final double ratio = max <= 0 ? 0 : amount / max;
        JPanel bar = new JPanel() {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.isDark() ? new Color(0x2C3140) : new Color(0xE9EEF7));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                int w = (int) (getWidth() * ratio);
                if (w > 0) {
                    g2.setColor(sel ? Ui.PRIMARY : new Color(0x9BA8E8));
                    g2.fillRoundRect(0, 0, Math.max(6, w), getHeight(), 4, 4);
                }
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(0, 5));
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 5));
        bar.setAlignmentX(Component.LEFT_ALIGNMENT);

        row.add(line);
        row.add(Box.createVerticalStrut(3));
        row.add(bar);

        bindFilter(name, row, line, n, v, bar);
        return row;
    }

    /** 点分类行：未筛选则只看该类，已筛选中再点一次取消 */
    private void bindFilter(final String name, Component... cs) {
        for (Component c : cs) {
            c.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            c.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    filterCat = name.equals(filterCat) ? null : name;
                    reloadTable();
                }
            });
        }
    }

    public void reloadTable() {
        model.setRowCount(0);
        rowToIndex.clear();
        double sum = 0;
        int cnt = 0;
        for (int i = lines.size() - 1; i >= 0; i--) {
            String[] p = lines.get(i).split("\t", -1);
            int type = p.length > 0 && p[0].equals("1") ? 1 : 0;
            double amount = p.length > 1 ? safeDouble(p[1]) : 0;
            String note = p.length > 2 ? p[2] : "";
            String time = p.length > 3 ? p[3] : "";
            String cat = p.length > 4 && !p[4].trim().isEmpty() ? p[4].trim() : "未分类";
            if (filterCat != null && !filterCat.equals(cat)) continue;
            if (type == 0) {
                sum += amount;
                cnt++;
            }
            model.addRow(new Object[]{time, type == 0 ? "支出" : "收入", cat,
                    note.isEmpty() ? "（无备注）" : note,
                    String.format(Locale.CHINA, "%s%.2f", type == 0 ? "-¥" : "+¥", amount)});
            rowToIndex.add(i);
        }
        if (filterCat == null) {
            filterBar.setVisible(false);
        } else {
            filterBar.setForeground(Theme.isDark() ? new Color(0xC7D2FE) : new Color(0x3B5BDB));
            filterBar.setText("已筛选：" + filterCat + " · 共 " + cnt + " 笔 · 支出 ¥"
                    + String.format(Locale.CHINA, "%.2f", sum) + "　（再点该分类可取消）");
            filterBar.setVisible(true);
        }
        refreshStat();
        refreshMonth();
    }

    /** 概览页点“最近记账”某一条时调用：按 ledger.txt 行号定位并选中该行 */
    public void locateLine(int lineIndex) {
        if (filterCat != null) filterCat = null;   // 先取消分类筛选，避免该条被筛掉
        lines = new ArrayList<String>(Store.readLines("ledger.txt"));
        reloadTable();
        if (table == null || lineIndex < 0 || lineIndex >= lines.size()) return;
        int row = -1;
        for (int i = 0; i < rowToIndex.size(); i++) {
            if (rowToIndex.get(i) == lineIndex) {
                row = i;
                break;
            }
        }
        if (row < 0) return;
        table.setRowSelectionInterval(row, row);
        Rectangle rr = table.getCellRect(row, 0, true);
        table.scrollRectToVisible(rr);
    }

    /** 双击表格行：就地改金额或备注，类型/分类/时间保持不变 */
    private void editRow(int row) {
        if (row < 0 || row >= rowToIndex.size()) return;
        int idx = rowToIndex.get(row);
        if (idx < 0 || idx >= lines.size()) return;
        String[] p = lines.get(idx).split("\t", -1);
        String type = p.length > 0 ? p[0].trim() : "0";
        String amount = p.length > 1 ? p[1].trim() : "";
        String note = p.length > 2 ? p[2].trim() : "";
        String time = p.length > 3 ? p[3].trim() : "";
        String cat = p.length > 4 && !p[4].trim().isEmpty() ? p[4].trim() : "未分类";

        JTextField amtF = new JTextField(amount);
        JTextField noteF = new JTextField(note);
        JPanel form = new JPanel(new GridLayout(2, 2, 8, 8));
        form.setOpaque(false);
        JLabel aL = new JLabel("金额");
        JLabel nL = new JLabel("备注");
        aL.setFont(Ui.font(12, false));
        nL.setFont(Ui.font(12, false));
        form.add(aL);
        form.add(amtF);
        form.add(nL);
        form.add(noteF);
        JPanel wrap = new JPanel(new BorderLayout(0, 8));
        wrap.setOpaque(false);
        JLabel info = new JLabel(time + "　" + ("1".equals(type) ? "收入" : "支出") + " · " + cat);
        info.setFont(Ui.font(11, false));
        info.setForeground(Theme.muted());
        wrap.add(info, BorderLayout.NORTH);
        wrap.add(form, BorderLayout.CENTER);
        wrap.setBorder(BorderFactory.createEmptyBorder(4, 4, 2, 4));

        if (JOptionPane.showConfirmDialog(this, wrap, "修改这条记录",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;

        String amtText = amtF.getText();
        String noteText = noteF.getText();
        double amt;
        try {
            amt = Double.parseDouble(amtText.trim());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "金额要填数字，这条记录没有改动。");
            return;
        }
        if (amt <= 0) {
            JOptionPane.showMessageDialog(this, "金额要大于 0，这条记录没有改动。");
            return;
        }
        if (!applyEdit(idx, amtText, noteText)) return;
        int again = rowToIndex.indexOf(idx);
        if (again >= 0) {
            table.setRowSelectionInterval(again, again);
            table.scrollRectToVisible(table.getCellRect(again, 0, true));
        }
    }

    /** 右键删除单条：弹确认框，确认后才写回文件 */
    private void deleteRow(int row) {
        if (row < 0 || row >= rowToIndex.size()) return;
        int idx = rowToIndex.get(row);
        if (idx < 0 || idx >= lines.size()) return;
        String[] p = lines.get(idx).split("\t", -1);
        String type = p.length > 0 && "1".equals(p[0].trim()) ? "收入" : "支出";
        String amount = p.length > 1 ? p[1].trim() : "0";
        String note = p.length > 2 && !p[2].trim().isEmpty() ? p[2].trim() : "（无备注）";
        String time = p.length > 3 ? p[3].trim() : "";
        String cat = p.length > 4 && !p[4].trim().isEmpty() ? p[4].trim() : "未分类";
        int r = JOptionPane.showConfirmDialog(this,
                "确认删除这条记录？\n\n" + time + "　" + type + " · " + cat + " · " + note
                        + "　" + ("收入".equals(type) ? "+¥" : "-¥") + amount + "\n删除后无法恢复。",
                "删除确认", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r != JOptionPane.YES_OPTION) return;
        removeLine(idx);
    }

    /** 右键菜单第二项的文字：当前行是收入就显示“改成支出”，反之显示“改成收入” */
    void updateToggleLabel() {
        if (toggleItemRef == null) return;
        int row = table.getSelectedRow();
        boolean income = false;
        if (row >= 0 && row < rowToIndex.size()) {
            int idx = rowToIndex.get(row);
            if (idx >= 0 && idx < lines.size()) {
                String[] p = lines.get(idx).split("\t", -1);
                income = p.length > 0 && "1".equals(p[0].trim());
            }
        }
        toggleItemRef.setText(income ? "改成支出" : "改成收入");
    }

    /** 复制选中那一条的可读文本到剪贴板 */
    void copyRow(int row) {
        if (row < 0 || row >= rowToIndex.size()) return;
        int idx = rowToIndex.get(row);
        if (idx < 0 || idx >= lines.size()) return;
        String[] p = lines.get(idx).split("\t", -1);
        boolean income = p.length > 0 && "1".equals(p[0].trim());
        String amount = p.length > 1 ? p[1].trim() : "0";
        String note = p.length > 2 && !p[2].trim().isEmpty() ? p[2].trim() : "（无备注）";
        String time = p.length > 3 ? p[3].trim() : "";
        String cat = p.length > 4 && !p[4].trim().isEmpty() ? p[4].trim() : "未分类";
        String text = time + "　" + (income ? "收入" : "支出") + " · " + cat + " · " + note
                + "　" + (income ? "+¥" : "-¥") + amount;
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                    new StringSelection(text), null);
            Main.notifyMsg("已复制", text);
        } catch (Exception ex) {
            Main.notifyMsg("复制失败", "系统剪贴板不可用");
        }
    }

    /** 把某一条在收入/支出之间切换；原分类不适用于新类型时归到“其他” */
    boolean toggleType(int row) {
        if (row < 0 || row >= rowToIndex.size()) return false;
        int idx = rowToIndex.get(row);
        if (idx < 0 || idx >= lines.size()) return false;
        String[] p = lines.get(idx).split("\t", -1);
        if (p.length < 2) return false;
        boolean income = "1".equals(p[0].trim());
        String cat = p.length > 4 ? p[4].trim() : "其他";
        String newCat = cat;
        String[] valid = income ? OUT_CATS : IN_CATS;
        boolean catOk = false;
        for (String c : valid) if (c.equals(cat)) catOk = true;
        if (!catOk) newCat = "其他";
        StringBuilder sb = new StringBuilder();
        sb.append(income ? "0" : "1").append('\t')
                .append(p[1].trim()).append('\t')
                .append(p.length > 2 ? p[2] : "").append('\t')
                .append(p.length > 3 ? p[3] : "").append('\t')
                .append(newCat);
        lines.set(idx, sb.toString());
        Store.writeLines("ledger.txt", lines);
        reloadTable();
        String tail = cat.equals(newCat) ? "" : "，分类「" + cat + "」不适用已改为「其他」";
        String body = (p.length > 2 && !p[2].trim().isEmpty() ? p[2].trim() : "这条记录") + tail;
        Main.notifyMsg(income ? "已改成支出" : "已改成收入", body);
        return true;
    }

    /** 删除某一条并写回文件（确认框之外的实际动作） */
    boolean removeLine(int idx) {
        if (idx < 0 || idx >= lines.size()) return false;
        lines.remove(idx);
        Store.writeLines("ledger.txt", lines);
        reloadTable();
        return true;
    }

    /** 把某条记录改金额/备注写回文件并刷新（类型、分类、时间保持不变） */
    private boolean applyEdit(int idx, String amountText, String noteText) {
        if (idx < 0 || idx >= lines.size()) return false;
        String[] p = lines.get(idx).split("\t", -1);
        String type = p.length > 0 ? p[0].trim() : "0";
        String time = p.length > 3 ? p[3].trim() : "";
        String cat = p.length > 4 && !p[4].trim().isEmpty() ? p[4].trim() : "未分类";
        double amt;
        try {
            amt = Double.parseDouble(amountText.trim());
        } catch (Exception ex) {
            return false;
        }
        if (amt <= 0) return false;
        lines.set(idx, type + "\t" + String.format(Locale.CHINA, "%.2f", amt) + "\t"
                + (noteText == null ? "" : noteText.trim()) + "\t" + time + "\t" + cat);
        Store.writeLines("ledger.txt", lines);
        reloadTable();
        return true;
    }

    private void refreshStat() {
        double in = 0, out = 0;
        double fOut = 0;
        int fCnt = 0;
        for (String l : lines) {
            String[] p = l.split("\t", -1);
            double a = safeDouble(p.length > 1 ? p[1] : "0");
            int type = p.length > 0 && p[0].equals("1") ? 1 : 0;
            if (type == 1) {
                in += a;
            } else {
                out += a;
                if (filterCat != null) {
                    String cat = p.length > 4 && !p[4].trim().isEmpty() ? p[4].trim() : "未分类";
                    if (cat.equals(filterCat)) {
                        fOut += a;
                        fCnt++;
                    }
                }
            }
        }
        if (filterCat != null) {
            int pct = out <= 0 ? 0 : (int) Math.round(fOut * 100 / out);
            statLabel.setText(String.format(Locale.CHINA,
                    "%s 小计 ¥%.2f（%d 笔 · 占全部支出 %d%%）　　全部支出 ¥%.2f",
                    filterCat, fOut, fCnt, pct, out));
            statLabel.setForeground(Theme.isDark() ? new Color(0xC7D2FE) : new Color(0x3B5BDB));
        } else {
            statLabel.setText(String.format(Locale.CHINA, "合计 收入 ¥%.2f    支出 ¥%.2f    结余 ¥%.2f", in, out, in - out));
            statLabel.setForeground(Theme.muted());
        }
    }

    /** 本月汇总：按当前月份前缀聚合，支出按分类排序展示 */
    private void refreshMonth() {
        String prefix = new SimpleDateFormat("MM-", Locale.CHINA).format(new Date());
        String ym = new SimpleDateFormat("yyyy年M月", Locale.CHINA).format(new Date());
        monthTitle.setText("本月汇总 · " + ym);

        double in = 0, out = 0;
        Map<String, Double> cats = new LinkedHashMap<String, Double>();
        for (String l : lines) {
            String[] p = l.split("\t", -1);
            if (p.length < 4) continue;
            String time = p[3].trim();
            if (!time.startsWith(prefix)) continue;
            double a = safeDouble(p.length > 1 ? p[1] : "0");
            int type = "1".equals(p[0].trim()) ? 1 : 0;
            if (type == 1) {
                in += a;
            } else {
                out += a;
                String cat = p.length > 4 && !p[4].trim().isEmpty() ? p[4].trim() : "未分类";
                Double v = cats.get(cat);
                cats.put(cat, (v == null ? 0 : v) + a);
            }
        }
        monthInLbl.setText(String.format(Locale.CHINA, "¥%.2f", in));
        monthOutLbl.setText(String.format(Locale.CHINA, "¥%.2f", out));
        monthBalLbl.setText(String.format(Locale.CHINA, "¥%.2f", in - out));

        monthCatsBox.removeAll();
        if (cats.isEmpty()) {
            JLabel empty = new JLabel("本月还没有支出记录");
            empty.setFont(Ui.font(11, false));
            empty.setForeground(Theme.muted());
            empty.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
            monthCatsBox.add(empty);
        } else {
            double max = 0;
            for (double v : cats.values()) max = Math.max(max, v);
            List<Map.Entry<String, Double>> es = new ArrayList<Map.Entry<String, Double>>(cats.entrySet());
            es.sort((x, y) -> Double.compare(y.getValue(), x.getValue()));
            for (Map.Entry<String, Double> en : es) {
                monthCatsBox.add(catRow(en.getKey(), en.getValue(), max));
            }
        }
        monthCatsBox.revalidate();
        monthCatsBox.repaint();
    }

    /* ==================== 导入：任意格式解析 ==================== */

    private static final java.util.regex.Pattern P_YMD = java.util.regex.Pattern.compile(
            "(\\d{4})\\s*[-/.年]\\s*(\\d{1,2})\\s*[-/.月]\\s*(\\d{1,2})\\s*[日号]?");
    private static final java.util.regex.Pattern P_MD = java.util.regex.Pattern.compile(
            "(\\d{1,2})\\s*[-/月]\\s*(\\d{1,2})\\s*[日号]?");
    private static final java.util.regex.Pattern P_REL = java.util.regex.Pattern.compile(
            "大前天|前天|昨天|昨日|今天|今日");
    private static final java.util.regex.Pattern P_DOM = java.util.regex.Pattern.compile(
            "(\\d{1,2})\\s*[日号]");
    private static final java.util.regex.Pattern P_TIME = java.util.regex.Pattern.compile(
            "(\\d{1,2}):(\\d{2})");
    private static final java.util.regex.Pattern P_YEN = java.util.regex.Pattern.compile(
            "[¥￥]\\s*(\\d+(?:\\.\\d{1,2})?)");
    private static final java.util.regex.Pattern P_SIGN = java.util.regex.Pattern.compile(
            "([-+])\\s*(\\d+(?:\\.\\d{1,2})?)");
    private static final java.util.regex.Pattern P_UNIT = java.util.regex.Pattern.compile(
            "(\\d+(?:\\.\\d{1,2})?)\\s*[元块]");
    private static final java.util.regex.Pattern P_NUM = java.util.regex.Pattern.compile(
            "(?<![\\d.])(\\d+(?:\\.\\d{1,2})?)(?![\\d.])");

    /** 支出分类关键词（顺序即优先级：先命中先得） */
    private static final String[][] OUT_RULES = {
            {"餐饮", "早餐", "午餐", "晚餐", "早饭", "午饭", "晚饭", "夜宵", "宵夜", "外卖", "奶茶", "咖啡",
                    "饮料", "零食", "水果", "食堂", "烧烤", "火锅", "快餐", "米线", "面包", "买菜", "菜市场", "吃饭"},
            {"交通", "打车", "滴滴", "出租车", "地铁", "公交", "高铁", "火车", "机票", "加油", "油费", "停车",
                    "单车", "过路费", "车票", "网约车", "顺风车"},
            {"医疗", "医院", "看病", "药店", "买药", "体检", "挂号", "门诊", "牙科", "诊所", "口罩"},
            {"学习", "课程", "培训", "网课", "学费", "考试", "教材", "打印", "文具", "复习资料", "书店"},
            {"居住", "房租", "水电", "电费", "水费", "燃气", "物业", "宽带", "租房", "中介费"},
            {"娱乐", "电影", "游戏", "会员", "健身", "门票", "演出", "剧本杀", "按摩", "旅游", "KTV"},
            {"购物", "淘宝", "京东", "拼多多", "天猫", "超市", "衣服", "裤子", "鞋", "日用品", "网购", "商城"},
            {"人情", "红包", "礼物", "礼金", "份子", "请客", "随礼", "份子钱"},
    };
    /** 收入分类关键词 */
    private static final String[][] IN_RULES = {
            {"工资", "工资", "薪资", "薪水", "月薪", "发薪", "奖金"},
            {"兼职", "兼职", "外快", "副业"},
            {"生活费", "生活费", "家里给"},
            {"理财", "利息", "基金", "分红", "股票", "收益", "退款", "报销"},
    };

    /** 把一段粘贴文本逐行解析；空行自动跳过 */
    public static List<String[]> parseImport(String text, LocalDate today) {
        List<String[]> out = new ArrayList<String[]>();
        if (text == null) return out;
        String[] ls = text.split("\\r?\\n");
        for (String l : ls) {
            if (l.trim().isEmpty()) continue;
            out.add(parseImportLine(l.trim(), today));
        }
        return out;
    }

    /** 解析一行任意格式的账单文字。
     *  返回 6 项：{类型 0支出/1收入, 金额(两位小数), 分类, 备注, 时间 MM-dd HH:mm, 失败原因}
     *  第 6 项非空 = 这行没解析出来。 */
    public static String[] parseImportLine(String raw, LocalDate today) {
        String[] r = new String[]{null, null, null, null, null, null};
        String src = raw == null ? "" : raw.trim();
        if (src.isEmpty()) {
            r[5] = "空行";
            return r;
        }
        LocalDate ref = today == null ? LocalDate.now() : today;
        StringBuilder mask = new StringBuilder(half(src));
        LocalDate date = null;
        String hhmm = null;

        java.util.regex.Matcher m = P_YMD.matcher(mask);
        if (m.find()) {
            int y = (int) parseNum(m.group(1)), mo = (int) parseNum(m.group(2)), d = (int) parseNum(m.group(3));
            if (mo >= 1 && mo <= 12 && d >= 1 && d <= 31) {
                date = LocalDate.of(y, mo, Math.min(d, LocalDate.of(y, mo, 1).lengthOfMonth()));
            }
            blank(mask, m.start(), m.end());
        }
        if (date == null) {
            java.util.regex.Matcher rel = P_REL.matcher(mask);
            if (rel.find()) {
                String w = rel.group();
                date = w.startsWith("大前天") ? ref.minusDays(3)
                        : w.startsWith("前天") ? ref.minusDays(2)
                        : (w.startsWith("昨天") || w.startsWith("昨日")) ? ref.minusDays(1) : ref;
                blank(mask, rel.start(), rel.end());
            }
        }
        if (date == null) {
            java.util.regex.Matcher md = P_MD.matcher(mask);
            if (md.find()) {
                int mo = (int) parseNum(md.group(1)), d = (int) parseNum(md.group(2));
                if (mo >= 1 && mo <= 12 && d >= 1 && d <= 31) {
                    LocalDate first = LocalDate.of(ref.getYear(), mo, 1);
                    date = first.withDayOfMonth(Math.min(d, first.lengthOfMonth()));
                    if (date.isAfter(ref.plusDays(1))) date = date.minusYears(1);   // 跨年往回补
                }
                blank(mask, md.start(), md.end());
            }
        }
        if (date == null) {
            java.util.regex.Matcher dom = P_DOM.matcher(mask);
            if (dom.find()) {
                int d = (int) parseNum(dom.group(1));
                if (d >= 1 && d <= ref.lengthOfMonth()) date = ref.withDayOfMonth(d);
                blank(mask, dom.start(), dom.end());
            }
        }

        java.util.regex.Matcher tm = P_TIME.matcher(mask);
        if (tm.find()) {
            int h = (int) parseNum(tm.group(1)), mi = (int) parseNum(tm.group(2));
            if (h >= 0 && h <= 23 && mi >= 0 && mi <= 59) {
                hhmm = String.format(Locale.CHINA, "%02d:%02d", h, mi);
            }
            blank(mask, tm.start(), tm.end());
        }

        double amount = 0;
        int sign = 0;
        boolean found = false;
        java.util.regex.Matcher ym = P_YEN.matcher(mask);
        if (ym.find()) {
            amount = parseNum(ym.group(1));
            found = true;
            blank(mask, ym.start(), ym.end());
        }
        if (!found) {
            java.util.regex.Matcher sm = P_SIGN.matcher(mask);
            if (sm.find()) {
                sign = "-".equals(sm.group(1)) ? -1 : 1;
                amount = parseNum(sm.group(2));
                found = true;
                blank(mask, sm.start(), sm.end());
            }
        }
        if (!found) {
            java.util.regex.Matcher um = P_UNIT.matcher(mask);
            if (um.find()) {
                amount = parseNum(um.group(1));
                found = true;
                blank(mask, um.start(), um.end());
            }
        }
        if (!found) {
            java.util.regex.Matcher nm = P_NUM.matcher(mask);
            if (nm.find()) {
                amount = parseNum(nm.group(1));
                found = true;
                blank(mask, nm.start(), nm.end());
            }
        }
        if (!found) {
            r[5] = "没找到金额";
            return r;
        }
        if (amount <= 0) {
            r[5] = "金额需大于 0";
            return r;
        }

        String rest = mask.toString();
        int type;
        if (sign < 0) {
            type = 0;
        } else if (sign > 0) {
            type = 1;
        } else {
            type = containsAny(rest, "收入", "收到", "到账", "入账", "退款", "报销", "工资", "发薪",
                    "分红", "收益", "奖金", "转给我", "返现") ? 1 : 0;
        }
        String cat = guessCat(rest, type);

        String note = rest.replaceAll("[,\\uFF0C\\u3001;\\uFF1B:\\uFF1A|/（）()\\[\\]【】]+", " ")
                .replaceAll("\\s+", " ").trim();
        if (note.length() > 40) note = note.substring(0, 40);

        String hm = hhmm != null ? hhmm : new SimpleDateFormat("HH:mm", Locale.CHINA).format(new Date());
        LocalDate d0 = date != null ? date : ref;
        /* 上次同样写法归过哪一类，就优先用那一类（用户改过的分类比关键词规则更可信） */
        String memoCat = mapCat(note, type);
        if (memoCat != null) cat = memoCat;
        r[0] = String.valueOf(type);
        r[1] = String.format(Locale.CHINA, "%.2f", amount);
        r[2] = cat;
        r[3] = note;
        r[4] = String.format(Locale.CHINA, "%02d-%02d %s", d0.getMonthValue(), d0.getDayOfMonth(), hm);
        r[5] = null;
        return r;
    }

    /** 把解析成功的记录批量写进账本，返回实际写入条数 */
    public int importRows(List<String[]> rows) {
        if (rows == null || rows.isEmpty()) return 0;
        /* 先按文件的最新内容重算，避免长时间开着页面时用旧缓存覆盖磁盘 */
        lines = new ArrayList<String>(Store.readLines("ledger.txt"));
        int n = 0;
        for (String[] p : rows) {
            if (p == null || p.length < 6 || p[5] != null) continue;
            if (p[0] == null || p[1] == null || p[2] == null || p[4] == null) continue;
            lines.add(p[0] + "\t" + p[1] + "\t" + (p[3] == null ? "" : p[3]) + "\t" + p[4] + "\t" + p[2]);
            /* 顺手把"这类东西归哪类"记下来，下次导入同样的写法直接套用 */
            remember(p[3], p[2]);
            n++;
        }
        if (n > 0) {
            Store.writeLines("ledger.txt", lines);
            reloadTable();
        }
        return n;
    }

    /** 全角转半角：ASCII 可见区整体平移，全角空格与￥单独处理 */
    static String half(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 0xFF01 && c <= 0xFF5E) sb.append((char) (c - 0xFEE0));
            else if (c == 0x3000) sb.append(' ');
            else if (c == 0xFFE5) sb.append('¥');
            else sb.append(c);
        }
        return sb.toString();
    }

    private static void blank(StringBuilder b, int from, int to) {
        for (int i = Math.max(0, from); i < Math.min(to, b.length()); i++) b.setCharAt(i, ' ');
    }

    private static double parseNum(String s) {
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return 0;
        }
    }

    private static boolean containsAny(String text, String... words) {
        for (String w : words) if (text.contains(w)) return true;
        return false;
    }

    private static String guessCat(String text, int type) {
        String[][] rules = type == 1 ? IN_RULES : OUT_RULES;
        for (String[] rule : rules) {
            for (int i = 1; i < rule.length; i++) if (text.contains(rule[i])) return rule[0];
        }
        return "其他";
    }

    /* ---------- 分类记忆：同一个商户/写法上次归到哪一类，下次导入自动套用 ---------- */

    /** 记忆表文件名，每行 keyword\t分类 */
    static final String MAP_FILE = "ledgermap.txt";

    /** 把备注压成关键词：去掉数字、标点、空白，取前 6 个字（"美团外卖 32.5" → "美团外卖"） */
    public static String keyOf(String note) {
        if (note == null) return "";
        String k = half(note).replaceAll("[0-9\\s\\p{Punct}（）()【】、，。]+", "").trim();
        if (k.length() > 6) k = k.substring(0, 6);
        return k;
    }

    /** 读记忆表；关键词长的排前面，保证"美团买菜"优先于"美团" */
    public static List<String[]> loadMap() {
        List<String[]> out = new ArrayList<String[]>();
        for (String line : Store.readLines(MAP_FILE)) {
            String[] p = line.split("\t", -1);
            if (p.length < 2) continue;
            String k = p[0].trim(), c = p[1].trim();
            if (k.isEmpty() || c.isEmpty()) continue;
            out.add(new String[]{k, c});
        }
        java.util.Collections.sort(out, new java.util.Comparator<String[]>() {
            public int compare(String[] x, String[] y) {
                return y[0].length() - x[0].length();
            }
        });
        return out;
    }

    /** 用记忆表判断这条备注该归哪一类；没命中返回 null（调用方继续用规则猜） */
    public static String mapCat(String note, int type) {
        String k = keyOf(note);
        if (k.isEmpty()) return null;
        List<String[]> m = loadMap();
        for (String[] e : m) {
            if (k.contains(e[0]) || e[0].contains(k)) {
                String c = e[1];
                /* 记账本的分类是自由文本，这里只挡住把收入记成支出分类的明显冲突 */
                if (type == 1 && !java.util.Arrays.asList(IN_CATS).contains(c)) {
                    if (java.util.Arrays.asList(OUT_CATS).contains(c)) continue;
                }
                return c;
            }
        }
        return null;
    }

    /** 记一条：关键词 → 分类，同一关键词只留最新的一条 */
    public static void remember(String note, String cat) {
        String k = keyOf(note);
        if (k.isEmpty() || cat == null || cat.trim().isEmpty()) return;
        List<String> lines = Store.readLines(MAP_FILE);
        boolean hit = false;
        for (int i = 0; i < lines.size(); i++) {
            String[] p = lines.get(i).split("\t", -1);
            if (p.length >= 1 && p[0].trim().equals(k)) {
                lines.set(i, k + "\t" + cat.trim());
                hit = true;
                break;
            }
        }
        if (!hit) lines.add(k + "\t" + cat.trim());
        Store.writeLines(MAP_FILE, lines);
    }

    /** 删掉一条记忆（设置页里手动清理用） */
    public static boolean forget(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return false;
        List<String> lines = Store.readLines(MAP_FILE);
        boolean hit = false;
        for (int i = lines.size() - 1; i >= 0; i--) {
            String[] p = lines.get(i).split("\t", -1);
            if (p.length >= 1 && p[0].trim().equals(keyword.trim())) {
                lines.remove(i);
                hit = true;
            }
        }
        if (hit) Store.writeLines(MAP_FILE, lines);
        return hit;
    }

    /** 把某条记忆的分类改成新的（设置页里手动纠正用） */
    public static boolean setCat(String keyword, String cat) {
        if (keyword == null || keyword.trim().isEmpty()) return false;
        if (cat == null || cat.trim().isEmpty()) return false;
        String k = keyword.trim();
        List<String> lines = Store.readLines(MAP_FILE);
        boolean hit = false;
        for (int i = 0; i < lines.size(); i++) {
            String[] p = lines.get(i).split("\t", -1);
            if (p.length >= 1 && p[0].trim().equals(k)) {
                lines.set(i, k + "\t" + cat.trim());
                hit = true;
                break;
            }
        }
        if (!hit) lines.add(k + "\t" + cat.trim());
        Store.writeLines(MAP_FILE, lines);
        return true;
    }

    /** 记账本可选的分类合集（支出在前、收入在后），设置页和导入预览的下拉共用 */
    public static java.util.List<String> allCats() {
        java.util.LinkedHashSet<String> s = new java.util.LinkedHashSet<String>();
        s.addAll(java.util.Arrays.asList(OUT_CATS));
        s.addAll(java.util.Arrays.asList(IN_CATS));
        return new ArrayList<String>(s);
    }

    /** 构建导入弹窗（只建不显示，按钮/测试/出图共用） */
    JDialog createImportDialog() {
        final JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this),
                "导入记账 · 格式不限", Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(0, 8));
        root.setBackground(Theme.bg());
        root.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        JPanel tipBox = new JPanel();
        tipBox.setOpaque(false);
        tipBox.setLayout(new BoxLayout(tipBox, BoxLayout.Y_AXIS));
        JLabel tip = new JLabel("一行一笔，日期/金额/备注顺序随便写，认不出的那一行会自动跳过；分类认错了直接在预览表里改，改完下次自动记住");
        tip.setFont(Ui.font(12, true));
        tip.setForeground(Theme.text());
        JLabel sample = new JLabel("例：2026-11-16 午饭 25　　11-15 打车 -18.5　　昨天 工资 +6500　　10号 奶茶 12元");
        sample.setFont(Ui.font(11, false));
        sample.setForeground(Theme.muted());
        tipBox.add(tip);
        tipBox.add(Box.createVerticalStrut(3));
        tipBox.add(sample);

        final JTextArea area = new JTextArea(5, 46);
        area.setFont(Ui.font(12, false));
        area.setLineWrap(false);
        area.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        area.setText("2026-11-16 午饭 25\n11-15 打车 -18.5\n昨天 工资 +6500\n10号 奶茶 12元\n超市 买日用品 87.6\n健身房月卡 ¥299\n这条没有金额，应该被跳过");
        JScrollPane areaSp = new JScrollPane(area);
        areaSp.setBorder(BorderFactory.createLineBorder(Theme.border(), 1));
        areaSp.setPreferredSize(new Dimension(10, 112));

        final DefaultTableModel pm = new DefaultTableModel(new String[]{"识别", "时间", "类型", "分类", "备注", "金额"}, 0) {
            public boolean isCellEditable(int r, int c) {
                return c == 3;   // 分类可以当场改，改完就记住
            }
        };
        final JTable pv = new JTable(pm);
        pv.setRowHeight(26);
        pv.setShowGrid(false);
        pv.setIntercellSpacing(new Dimension(0, 1));
        pv.setFont(Ui.font(12, false));
        pv.getTableHeader().setFont(Ui.font(12, true));
        pv.getTableHeader().setPreferredSize(new Dimension(10, 28));
        pv.getColumnModel().getColumn(0).setPreferredWidth(150);
        pv.getColumnModel().getColumn(1).setPreferredWidth(110);
        pv.getColumnModel().getColumn(2).setPreferredWidth(60);
        pv.getColumnModel().getColumn(3).setPreferredWidth(76);
        pv.getColumnModel().getColumn(5).setPreferredWidth(80);
        /* 分类列给个下拉，选项是支出+收入的分类合集 */
        java.util.LinkedHashSet<String> allCats = new java.util.LinkedHashSet<String>();
        allCats.addAll(java.util.Arrays.asList(OUT_CATS));
        allCats.addAll(java.util.Arrays.asList(IN_CATS));
        final JComboBox<String> catEditor = new JComboBox<String>(allCats.toArray(new String[0]));
        catEditor.setFont(Ui.font(12, false));
        javax.swing.DefaultCellEditor catCellEd = new javax.swing.DefaultCellEditor(catEditor);
        catCellEd.setClickCountToStart(1);
        pv.getColumnModel().getColumn(3).setCellEditor(catCellEd);
        JScrollPane pvSp = new JScrollPane(pv);
        pvSp.setColumnHeaderView(pv.getTableHeader());
        pvSp.setBorder(BorderFactory.createLineBorder(Theme.border(), 1));

        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setOpaque(false);
        JPanel areaWrap = new JPanel(new BorderLayout(0, 6));
        areaWrap.setOpaque(false);
        areaWrap.add(tipBox, BorderLayout.NORTH);
        areaWrap.add(areaSp, BorderLayout.CENTER);
        center.add(areaWrap, BorderLayout.NORTH);
        center.add(pvSp, BorderLayout.CENTER);

        final JLabel count = new JLabel("粘好文字后点「解析」先看识别结果");
        count.setFont(Ui.font(11, false));
        count.setForeground(Theme.muted());

        final JButton okBtn = Ui.primary("确认导入");
        okBtn.setPreferredSize(new Dimension(132, 32));
        okBtn.setEnabled(false);
        JButton parseBtn = Ui.secondary("解析");
        parseBtn.setPreferredSize(new Dimension(76, 32));
        JButton sampleBtn = Ui.ghost("填示例");
        JButton cancelBtn = new JButton("取消");

        final List<String[]> parsed = new ArrayList<String[]>();
        /* 预览表行号 → parsed 下标（跳过的行记 -1），改分类时用它对上原始那一笔 */
        final List<Integer> rowParsed = new ArrayList<Integer>();
        parseBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                parsed.clear();
                rowParsed.clear();
                pm.setRowCount(0);
                List<String[]> rows = parseImport(area.getText(), LocalDate.now());
                int ok = 0, bad = 0, memo = 0;
                for (String[] p : rows) {
                    if (p[5] == null) {
                        ok++;
                        int pi = parsed.size();
                        parsed.add(p);
                        rowParsed.add(pi);
                        int tp = "1".equals(p[0]) ? 1 : 0;
                        boolean fromMemo = mapCat(p[3], tp) != null;
                        if (fromMemo) memo++;
                        pm.addRow(new Object[]{fromMemo ? "可导入 · 记忆" : "可导入", p[4],
                                tp == 1 ? "收入" : "支出", p[2],
                                p[3] == null || p[3].isEmpty() ? "（无备注）" : p[3],
                                (tp == 1 ? "+¥" : "-¥") + p[1]});
                    } else {
                        bad++;
                        rowParsed.add(-1);
                        pm.addRow(new Object[]{"跳过 · " + p[5], "", "", "", "", ""});
                    }
                }
                if (rows.isEmpty()) {
                    count.setText("没粘东西，先贴几行账单文字");
                    okBtn.setText("确认导入");
                    okBtn.setEnabled(false);
                    return;
                }
                count.setText("识别 " + ok + " 笔，跳过 " + bad + " 行"
                        + (memo > 0 ? "（其中 " + memo + " 笔用了记忆分类）" : "")
                        + " · 分类可以改，改完自动记住");
                okBtn.setText("确认导入 " + ok + " 笔");
                okBtn.setEnabled(ok > 0);
            }
        });
        /* 在预览表里改了分类：同步回待导入的那一笔，并记进分类记忆 */
        pm.addTableModelListener(new javax.swing.event.TableModelListener() {
            public void tableChanged(javax.swing.event.TableModelEvent e) {
                if (e.getType() != javax.swing.event.TableModelEvent.UPDATE) return;
                int r = e.getFirstRow(), c = e.getColumn();
                if (c != 3 || r < 0 || r >= rowParsed.size()) return;
                int pi = rowParsed.get(r);
                if (pi < 0 || pi >= parsed.size()) return;
                String[] p = parsed.get(pi);
                Object v = pm.getValueAt(r, 3);
                if (v == null) return;
                String cat = String.valueOf(v).trim();
                if (cat.isEmpty()) return;
                p[2] = cat;
                remember(p[3], cat);
            }
        });
        sampleBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                area.setText("2026-11-16 午饭 25\n11-15 打车 -18.5\n昨天 工资 +6500\n10号 奶茶 12元\n"
                        + "超市 买日用品 87.6\n健身房月卡 ¥299\n这条没有金额，应该被跳过");
            }
        });
        okBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int n = importRows(parsed);
                dlg.dispose();
                Main.notifyMsg("导入完成", "已记入 " + n + " 笔，可在列表里双击改金额或备注");
            }
        });
        cancelBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dlg.dispose();
            }
        });

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setOpaque(false);
        btns.add(sampleBtn);
        btns.add(cancelBtn);
        btns.add(parseBtn);
        btns.add(okBtn);
        JPanel south = new JPanel(new BorderLayout(0, 6));
        south.setOpaque(false);
        south.add(count, BorderLayout.NORTH);
        south.add(btns, BorderLayout.SOUTH);

        root.add(center, BorderLayout.CENTER);
        root.add(south, BorderLayout.SOUTH);
        dlg.setContentPane(root);
        dlg.setSize(840, 600);
        dlg.setLocationRelativeTo(this);
        dlg.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent e) {
                if (importDlg == dlg) importDlg = null;
            }
        });
        return dlg;
    }

    private void openImport() {
        if (importDlg != null && importDlg.isShowing()) {
            importDlg.toFront();
            importDlg.requestFocus();
            return;
        }
        importDlg = createImportDialog();
        importDlg.setVisible(true);
    }

    private double safeDouble(String s) {
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return 0;
        }
    }
}
