package com.toolbox.pc;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Arrays;
import java.util.List;

/** 工具页：剪贴板速记 + 文件整理助手，两张卡来回切 */
public class ToolboxPanel extends JPanel {

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardHost = new JPanel(cardLayout);
    private JToggleButton clipTab;
    private JToggleButton tidyTab;

    /* 剪贴板 */
    private final DefaultListModel<String> clipModel = new DefaultListModel<String>();
    private final JList<String> clipList = new JList<String>(clipModel);
    private final JTextField clipSearch = new JTextField();
    private final JToggleButton clipOnlyPinned = new JToggleButton("只看常用");
    private final JLabel clipStat = new JLabel(" ");
    private java.util.List<String[]> clipItems;

    /* 文件整理 */
    private final JTextField dirField = new JTextField();
    private final DefaultTableModel tidyModel = new DefaultTableModel(new String[]{"文件", "归档到"}, 0) {
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };
    private final JTable tidyTable = new JTable(tidyModel);
    private final JLabel tidyStat = new JLabel(" ");
    private File currentDir;
    /** 文件整理的分档方式：0 按类型 / 1 按月 / 2 按年 */
    private int tidyMode = FileTidy.MODE_TYPE;
    private final JComboBox<String> tidyModeBox = new JComboBox<String>(FileTidy.MODE_NAMES);

    public ToolboxPanel() {
        setLayout(new BorderLayout(0, 12));
        setOpaque(false);

        cardHost.setOpaque(false);
        cardHost.add(buildClip(), "clip");
        cardHost.add(buildTidy(), "tidy");
        add(cardHost, BorderLayout.CENTER);

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        bar.setOpaque(false);
        clipTab = tab("剪贴板速记");
        tidyTab = tab("文件整理");
        clipTab.setSelected(true);
        clipTab.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showCard("clip");
            }
        });
        tidyTab.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showCard("tidy");
            }
        });
        ButtonGroup g = new ButtonGroup();
        g.add(clipTab);
        g.add(tidyTab);
        bar.add(clipTab);
        bar.add(tidyTab);
        add(bar, BorderLayout.NORTH);

        refreshClips();
    }

    private JToggleButton tab(String text) {
        JToggleButton b = new JToggleButton(text);
        b.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(text.length() > 4 ? 108 : 88, 28));
        return b;
    }

    public void showCard(String key) {
        cardLayout.show(cardHost, key);
        clipTab.setSelected("clip".equals(key));
        tidyTab.setSelected("tidy".equals(key));
        if ("clip".equals(key)) refreshClips();
    }

    public String currentCard() {
        return clipTab.isSelected() ? "clip" : "tidy";
    }

    /* ===================== 剪贴板速记 ===================== */

    private JPanel buildClip() {
        JPanel pan = new JPanel(new BorderLayout(0, 10));
        pan.setOpaque(false);

        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setOpaque(false);
        clipStat.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        top.add(clipStat, BorderLayout.WEST);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setOpaque(false);
        JButton grab = new JButton("记下当前剪贴板");
        grab.setFocusPainted(false);
        grab.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (ClipNotes.grabNow()) {
                    refreshClips();
                    Main.notifyMsg("已记下", "当前剪贴板内容已存成速记");
                } else {
                    Main.notifyMsg("剪贴板是空的", "先复制一段文字再来");
                }
            }
        });
        JButton clear = new JButton("清空全部");
        clear.setFocusPainted(false);
        clear.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int r = JOptionPane.showConfirmDialog(ToolboxPanel.this,
                        "把所有速记都清掉？这个不能恢复。", "清空速记", JOptionPane.OK_CANCEL_OPTION);
                if (r != JOptionPane.OK_OPTION) return;
                ClipNotes.clear();
                refreshClips();
                Main.notifyMsg("已清空", "速记列表清空了");
            }
        });
        btns.add(grab);
        btns.add(clear);
        top.add(btns, BorderLayout.EAST);

        JPanel headWrap = new JPanel(new BorderLayout(0, 8));
        headWrap.setOpaque(false);
        headWrap.add(top, BorderLayout.NORTH);

        JPanel sRow = new JPanel(new BorderLayout(8, 0));
        sRow.setOpaque(false);
        clipSearch.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        clipSearch.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.border(), 1, true),
                new EmptyBorder(5, 9, 5, 9)));
        clipSearch.putClientProperty("JTextField.placeholderText", "搜索关键词：时间或内容里含这个词就留下");
        clipSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                refreshClips();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                refreshClips();
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                refreshClips();
            }
        });
        sRow.add(clipSearch, BorderLayout.CENTER);

        JPanel sRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        sRight.setOpaque(false);
        clipOnlyPinned.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
        clipOnlyPinned.setFocusPainted(false);
        clipOnlyPinned.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                refreshClips();
            }
        });
        JButton pinBtn = new JButton("标为常用");
        pinBtn.setFocusPainted(false);
        pinBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                pinSelectedClip();
            }
        });
        sRight.add(pinBtn);
        sRight.add(clipOnlyPinned);
        sRow.add(sRight, BorderLayout.EAST);
        headWrap.add(sRow, BorderLayout.SOUTH);
        pan.add(headWrap, BorderLayout.NORTH);

        clipList.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        clipList.setFixedCellHeight(30);
        clipList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        clipList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) copySelectedClip();
            }
        });
        JScrollPane sp = new JScrollPane(clipList);
        sp.setBorder(BorderFactory.createLineBorder(Theme.border(), 1, true));
        pan.add(sp, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(8, 0));
        bottom.setOpaque(false);
        JLabel tip = new JLabel("后台一直记：复制了新文字就自动存一条，最多 150 条；★ 常用的不会被上限挤掉，双击一条复制回去");
        tip.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
        tip.setForeground(Theme.muted());
        bottom.add(tip, BorderLayout.WEST);

        JPanel b2 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        b2.setOpaque(false);
        JButton copy = new JButton("复制选中");
        copy.setFocusPainted(false);
        copy.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                copySelectedClip();
            }
        });
        JButton del = new JButton("删除选中");
        del.setFocusPainted(false);
        del.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int idx = clipList.getSelectedIndex();
                if (idx < 0) {
                    Main.notifyMsg("没选", "先点一条速记再删");
                    return;
                }
                if (clipItems != null && idx < clipItems.size()) {
                    String[] it = clipItems.get(idx);
                    ClipNotes.removeBy(it[0], it[1]);
                }
                refreshClips();
            }
        });
        b2.add(copy);
        b2.add(del);
        bottom.add(b2, BorderLayout.EAST);
        pan.add(bottom, BorderLayout.SOUTH);
        return pan;
    }

    private void copySelectedClip() {
        int idx = clipList.getSelectedIndex();
        if (idx < 0 || clipItems == null || idx >= clipItems.size()) {
            Main.notifyMsg("没选", "先点一条速记");
            return;
        }
        String text = clipItems.get(idx)[1];
        ClipNotes.copyBack(text);
        Main.notifyMsg("已复制", text.length() > 28 ? text.substring(0, 28) + "…" : text.replace("\n", " "));
    }

    /** 重新读速记列表（吃搜索词和「只看常用」） */
    public void refreshClips() {
        String kw = clipSearch.getText();
        clipItems = ClipNotes.search(kw);
        if (clipOnlyPinned.isSelected()) {
            List<String[]> keep = new java.util.ArrayList<String[]>();
            for (String[] it : clipItems) {
                if ("1".equals(it[2])) keep.add(it);
            }
            clipItems = keep;
        }
        clipModel.clear();
        for (String[] it : clipItems) {
            String text = it[1].replace("\n", " ⏎ ");
            if (text.length() > 70) text = text.substring(0, 70) + "…";
            String star = "1".equals(it[2]) ? "★ " : "   ";
            clipModel.addElement(star + it[0] + "   " + text);
        }
        int all = ClipNotes.size();
        int pinAll = ClipNotes.pinnedCount();
        String base = "已记 " + all + " 条（自动记录中）· 常用 " + pinAll + " 条";
        boolean filtering = !kw.trim().isEmpty() || clipOnlyPinned.isSelected();
        clipStat.setText(filtering ? "筛出 " + clipModel.size() + " 条 / " + base : base);
    }

    private void pinSelectedClip() {
        int idx = clipList.getSelectedIndex();
        if (idx < 0 || clipItems == null || idx >= clipItems.size()) {
            Main.notifyMsg("没选", "先点一条速记再标");
            return;
        }
        String[] it = clipItems.get(idx);
        boolean now = ClipNotes.togglePin(it[0], it[1]);
        refreshClips();
        Main.notifyMsg(now ? "已标为常用" : "已取消常用",
                now ? "这条不会再被条数上限挤掉了" : "恢复成普通速记");
    }

    public JList<String> clipListComponent() {
        return clipList;
    }

    public JTextField clipSearchField() {
        return clipSearch;
    }

    public JToggleButton clipOnlyPinnedButton() {
        return clipOnlyPinned;
    }

    public void pinSelectedClipNow() {
        pinSelectedClip();
    }

    /* ===================== 文件整理 ===================== */

    private JPanel buildTidy() {
        JPanel pan = new JPanel(new BorderLayout(0, 10));
        pan.setOpaque(false);

        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setOpaque(false);
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        dirField.setEditable(false);
        dirField.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        dirField.setBorder(new EmptyBorder(6, 8, 6, 8));
        row.add(dirField, BorderLayout.CENTER);
        JPanel pick = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pick.setOpaque(false);
        JButton choose = new JButton("选文件夹");
        choose.setFocusPainted(false);
        choose.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new JFileChooser(currentDir == null ? new File("C:\\Users\\chen") : currentDir);
                fc.setDialogTitle("选一个要整理的文件夹（只整理里面散落的文件）");
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                if (fc.showOpenDialog(ToolboxPanel.this) == JFileChooser.APPROVE_OPTION) {
                    currentDir = fc.getSelectedFile();
                    dirField.setText(currentDir.getAbsolutePath());
                    previewTidy();
                }
            }
        });
        JButton preview = new JButton("刷新预览");
        preview.setFocusPainted(false);
        preview.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                previewTidy();
            }
        });
        pick.add(choose);
        pick.add(preview);
        row.add(pick, BorderLayout.EAST);

        /* 分档方式：按类型 / 按月 / 按年 */
        JPanel modeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        modeRow.setOpaque(false);
        JLabel modeLbl = new JLabel("分档方式");
        modeLbl.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
        modeLbl.setForeground(Theme.muted());
        final JComboBox<String> modeBox = tidyModeBox;
        modeBox.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
        modeBox.setPreferredSize(new Dimension(110, 28));
        modeBox.setSelectedIndex(0);
        final JLabel modeHint = new JLabel("按扩展名分成 图片 / 文档 / 表格 等 7 类");
        modeHint.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
        modeHint.setForeground(Theme.muted());
        modeBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                tidyMode = modeBox.getSelectedIndex();
                modeHint.setText(tidyHint(tidyMode));
                previewTidy();
            }
        });
        modeRow.add(modeLbl);
        modeRow.add(modeBox);
        modeRow.add(modeHint);
        top.setLayout(new BorderLayout(0, 8));
        JPanel topNorth = new JPanel(new BorderLayout());
        topNorth.setOpaque(false);
        topNorth.add(row, BorderLayout.CENTER);
        top.add(topNorth, BorderLayout.NORTH);
        top.add(modeRow, BorderLayout.SOUTH);
        pan.add(top, BorderLayout.NORTH);

        tidyTable.setRowHeight(26);
        tidyTable.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        tidyTable.getTableHeader().setFont(new Font("Microsoft YaHei UI", Font.BOLD, 12));
        JScrollPane sp = new JScrollPane(tidyTable);
        sp.setBorder(BorderFactory.createLineBorder(Theme.border(), 1, true));
        pan.add(sp, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(8, 0));
        bottom.setOpaque(false);
        tidyStat.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
        tidyStat.setForeground(Theme.muted());
        bottom.add(tidyStat, BorderLayout.WEST);

        JPanel b2 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        b2.setOpaque(false);
        JButton go = Ui.primary("开始整理");
        go.setPreferredSize(new Dimension(112, 32));
        go.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                runTidy();
            }
        });
        b2.add(go);
        bottom.add(b2, BorderLayout.EAST);
        pan.add(bottom, BorderLayout.SOUTH);
        return pan;
    }

    private void previewTidy() {
        tidyModel.setRowCount(0);
        if (currentDir == null) {
            tidyStat.setText("先点右上「选文件夹」指定一个目录");
            return;
        }
        List<String[]> plan = FileTidy.plan(currentDir, tidyMode);
        for (String[] it : plan) {
            tidyModel.addRow(new Object[]{it[0], it[1]});
        }
        if (plan.isEmpty()) {
            tidyStat.setText("这个文件夹顶层没有可整理的文件（子文件夹和隐藏文件不算）");
        } else {
            StringBuilder b = new StringBuilder("共 " + plan.size() + " 个文件，将按"
                    + (tidyMode == FileTidy.MODE_TYPE ? "类型" : tidyMode == FileTidy.MODE_MONTH ? "月份" : "年份")
                    + "归到子文件夹：");
            for (java.util.Map.Entry<String, Integer> en : FileTidy.groupCounts(currentDir, tidyMode).entrySet()) {
                b.append(en.getKey()).append(" ").append(en.getValue()).append(" 个；");
            }
            tidyStat.setText(b.toString());
        }
    }

    private String tidyHint(int mode) {
        if (mode == FileTidy.MODE_MONTH) return "按文件修改时间归到「2026-11」这类文件夹";
        if (mode == FileTidy.MODE_YEAR) return "按文件修改时间归到「2026」这类文件夹";
        return "按扩展名分成 图片 / 文档 / 表格 等 7 类";
    }

    private void runTidy() {
        if (currentDir == null) {
            Main.notifyMsg("还没选目录", "先点「选文件夹」");
            return;
        }
        List<String[]> plan = FileTidy.plan(currentDir, tidyMode);
        int total = plan.size();
        if (total == 0) {
            Main.notifyMsg("没有要整理的", "这个目录顶层已经没有散落文件了");
            return;
        }
        String way = tidyMode == FileTidy.MODE_TYPE ? "按类型"
                : tidyMode == FileTidy.MODE_MONTH ? "按月（2026-11 这种）" : "按年（2026 这种）";
        String msg = "把 " + total + " 个文件" + way + "挪进「" + currentDir.getName() + "」下的子文件夹？\n"
                + "只会移动，不会删除任何文件；重名会自动加 (1)。";
        int r = JOptionPane.showConfirmDialog(this, msg, "确认整理", JOptionPane.OK_CANCEL_OPTION);
        if (r != JOptionPane.OK_OPTION) return;
        int moved = FileTidy.run(currentDir, tidyMode);
        previewTidy();
        Main.notifyMsg("整理完成", "移动了 " + moved + " 个文件");
    }

    public File currentDir() {
        return currentDir;
    }

    public void setCurrentDir(File f) {
        currentDir = f;
        dirField.setText(f == null ? "" : f.getAbsolutePath());
        previewTidy();
    }

    public DefaultTableModel tidyTableModel() {
        return tidyModel;
    }

    public int tidyMode() {
        return tidyMode;
    }

    /** 换分档方式（顺带刷新预览） */
    public void setTidyMode(int m) {
        if (m < 0 || m >= FileTidy.MODE_NAMES.length) return;
        tidyModeBox.setSelectedIndex(m);   // 会触发监听，tidyMode 与预览一起更新
    }

    static String joinCats() {
        return Arrays.toString(FileTidy.CATS);
    }
}
