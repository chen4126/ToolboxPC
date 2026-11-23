package com.toolbox.pc;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** 备忘录：支持置顶与关键字搜索 */
public class MemoPanel extends JPanel {

    /** 一条备忘 */
    private static class Memo {
        boolean pinned;
        String time;
        String text;
        /** 标签，逗号分隔，空串表示没打标签；老数据三列读进来就是空串 */
        String tags;

        Memo(boolean pinned, String time, String text) {
            this(pinned, time, text, "");
        }

        Memo(boolean pinned, String time, String text, String tags) {
            this.pinned = pinned;
            this.time = time;
            this.text = text;
            this.tags = tags == null ? "" : tags;
        }
    }

    private final List<Memo> memos = new ArrayList<Memo>();
    private DefaultListModel<Memo> model;
    private JList<Memo> list;
    private JLabel countLabel;
    private String query = "";
    private JTextField searchField;
    /** 标签筛选：null 表示"全部标签" */
    private String tagFilter = null;
    private JComboBox<String> tagBox;
    private JTextField tagField;
    private JButton clearTagBtn;
    private boolean rebuildingTags = false;

    public MemoPanel() {
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(Theme.bg());
        load();

        /* 顶部：搜索 + 新增 */
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        JPanel searchRow = new JPanel(new BorderLayout(8, 0));
        searchRow.setOpaque(false);
        searchRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        searchRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        searchField = new JTextField();
        searchField.setToolTipText("输入关键字，实时筛选备忘");
        searchField.putClientProperty("JTextField.placeholderText", "搜索备忘内容…");
        countLabel = new JLabel(" ");
        countLabel.setFont(Ui.font(11, false));
        countLabel.setForeground(Theme.muted());
        searchRow.add(searchField, BorderLayout.CENTER);
        countLabel.setPreferredSize(new Dimension(88, 30));
        countLabel.setMaximumSize(new Dimension(88, 30));
        countLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        countLabel.setVerticalAlignment(SwingConstants.CENTER);

        /* 搜索框右边：标签筛选下拉 + 条数 */
        JPanel searchRight = new JPanel();
        searchRight.setOpaque(false);
        searchRight.setLayout(new BoxLayout(searchRight, BoxLayout.X_AXIS));
        tagBox = new JComboBox<String>(new String[]{"全部标签"});
        tagBox.setPreferredSize(new Dimension(126, 30));
        tagBox.setMaximumSize(new Dimension(126, 30));
        tagBox.setMinimumSize(new Dimension(110, 30));
        tagBox.setFont(Ui.font(11, false));
        tagBox.setToolTipText("按标签筛选备忘，和左边的搜索可以叠加");
        tagBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (rebuildingTags) return;          // 下拉在重填选项时别回头再筛一遍
                Object it = tagBox.getSelectedItem();
                String s = it == null ? "" : String.valueOf(it).trim();
                tagFilter = (s.isEmpty() || "全部标签".equals(s)) ? null : s;
                reloadList();
            }
        });
        searchRight.add(Box.createHorizontalStrut(6));
        searchRight.add(tagBox);
        searchRight.add(Box.createHorizontalStrut(4));
        clearTagBtn = new JButton("×");
        clearTagBtn.setFont(Ui.font(12, true));
        clearTagBtn.setMargin(new java.awt.Insets(1, 7, 1, 7));
        clearTagBtn.setFocusPainted(false);
        clearTagBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clearTagBtn.setToolTipText("清掉标签筛选，回到全部");
        clearTagBtn.setPreferredSize(new Dimension(28, 30));
        clearTagBtn.setMaximumSize(new Dimension(28, 30));
        clearTagBtn.setEnabled(false);
        clearTagBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                tagFilter = null;
                rebuildingTags = true;
                try {
                    if (tagBox.getItemCount() > 0) tagBox.setSelectedIndex(0);
                } finally {
                    rebuildingTags = false;
                }
                reloadList();
            }
        });
        searchRight.add(clearTagBtn);
        searchRight.add(Box.createHorizontalStrut(6));
        searchRight.add(countLabel);
        searchRow.add(searchRight, BorderLayout.EAST);

        JPanel addRow = new JPanel(new BorderLayout(8, 0));
        addRow.setOpaque(false);
        addRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        addRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        final JTextField input = new JTextField();
        input.putClientProperty("JTextField.placeholderText", "记一条新备忘，回车保存…");
        JButton addBtn = Ui.primary("保存");
        addBtn.setPreferredSize(new Dimension(84, 34));
        addRow.add(input, BorderLayout.CENTER);
        addRow.add(addBtn, BorderLayout.EAST);

        top.add(searchRow);
        top.add(Box.createVerticalStrut(8));
        top.add(addRow);

        /* 标签行：新增备忘时顺手打标签 */
        JPanel tagRow = new JPanel(new BorderLayout(8, 0));
        tagRow.setOpaque(false);
        tagRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        tagRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel tgLabel = new JLabel("标签");
        tgLabel.setFont(Ui.font(11, false));
        tgLabel.setForeground(Theme.muted());
        tgLabel.setPreferredSize(new Dimension(34, 0));
        tagField = new JTextField();
        tagField.setFont(Ui.font(12, false));
        tagField.putClientProperty("JTextField.placeholderText",
                "可选，给这条打标签，多个用逗号分隔（如 工作,学习）");
        tagField.setToolTipText("标签用来分类筛选；已有标签会出现在右上角下拉里");
        tagRow.add(tgLabel, BorderLayout.WEST);
        tagRow.add(tagField, BorderLayout.CENTER);
        top.add(Box.createVerticalStrut(6));
        top.add(tagRow);
        add(top, BorderLayout.NORTH);

        /* 列表 */
        model = new DefaultListModel<Memo>();
        list = new JList<Memo>(model);
        list.setCellRenderer(new MemoRenderer());
        list.setFixedCellHeight(52);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setBackground(Theme.bg());
        list.setBorder(BorderFactory.createEmptyBorder());
        list.setFixedCellWidth(-1);
        JScrollPane sp = new JScrollPane(list);
        sp.setBorder(BorderFactory.createLineBorder(Theme.border()));
        add(sp, BorderLayout.CENTER);

        /* 底部按钮 */
        JPanel bottom = new JPanel(new BorderLayout(8, 0));
        bottom.setOpaque(false);
        final JButton pinBtn = new JButton("置顶");
        final JButton tagBtn = new JButton("标签");
        final JButton editBtn = new JButton("编辑");
        final JButton delBtn = new JButton("删除");
        JPanel btns = new JPanel(new GridLayout(1, 4, 6, 0));
        btns.setOpaque(false);
        for (JButton b : new JButton[]{pinBtn, tagBtn, editBtn, delBtn}) {
            b.setPreferredSize(new Dimension(70, 32));
            btns.add(b);
        }
        JLabel hint = new JLabel("双击可编辑 · ★ 置顶始终最前 · 标签可多选筛选");
        hint.setFont(Ui.font(11, false));
        hint.setForeground(Theme.muted());
        bottom.add(hint, BorderLayout.WEST);
        bottom.add(btns, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        /* 事件 */
        ActionListener addL = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String text = input.getText().trim();
                if (text.isEmpty()) {
                    JOptionPane.showMessageDialog(MemoPanel.this, "内容不能为空");
                    return;
                }
                String time = new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(new Date());
                String tags = normalizeTags(tagField == null ? "" : tagField.getText());
                memos.add(new Memo(false, time, text, tags));
                save();
                input.setText("");
                if (tagField != null) tagField.setText("");
                reloadList();
            }
        };
        addBtn.addActionListener(addL);
        input.addActionListener(addL);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                query = searchField.getText();
                reloadList();
            }

            public void removeUpdate(DocumentEvent e) {
                query = searchField.getText();
                reloadList();
            }

            public void changedUpdate(DocumentEvent e) {
                query = searchField.getText();
                reloadList();
            }
        });

        pinBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Memo m = list.getSelectedValue();
                if (m == null) {
                    JOptionPane.showMessageDialog(MemoPanel.this, "请先选中一条备忘");
                    return;
                }
                m.pinned = !m.pinned;
                save();
                reloadList();
                int idx = model.indexOf(m);
                if (idx >= 0) list.setSelectedIndex(idx);
            }
        });

        editBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                editSelected(list.getSelectedValue());
            }
        });

        tagBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Memo m = list.getSelectedValue();
                if (m == null) {
                    JOptionPane.showMessageDialog(MemoPanel.this, "请先选中一条备忘");
                    return;
                }
                String in = JOptionPane.showInputDialog(MemoPanel.this,
                        "给这条打标签（多个用逗号分隔，留空表示清掉）：", m.tags);
                if (in == null) return;
                m.tags = normalizeTags(in);
                save();
                reloadList();
                int idx = model.indexOf(m);
                if (idx >= 0) list.setSelectedIndex(idx);
            }
        });

        list.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && list.getSelectedValue() != null) {
                    editSelected(list.getSelectedValue());
                }
            }
        });

        delBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Memo m = list.getSelectedValue();
                if (m == null) {
                    JOptionPane.showMessageDialog(MemoPanel.this, "请先选中一条备忘");
                    return;
                }
                if (JOptionPane.showConfirmDialog(MemoPanel.this, "删除这条备忘？", "确认", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    memos.remove(m);
                    save();
                    reloadList();
                }
            }
        });

        reloadList();
    }

    private void editSelected(Memo m) {
        if (m == null) return;
        String input2 = JOptionPane.showInputDialog(this, "编辑内容：", m.text);
        if (input2 == null) return;
        input2 = input2.trim();
        if (input2.isEmpty()) {
            memos.remove(m);
        } else {
            m.text = input2;
            m.time = new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(new Date());
        }
        save();
        reloadList();
    }

    /* ---------- 存取 ---------- */

    private void load() {
        memos.clear();
        for (String line : Store.readLines("memo.txt")) {
            if (line.trim().isEmpty()) continue;
            String[] p = line.split("\t", -1);
            if (p.length >= 3 && ("1".equals(p[0]) || "0".equals(p[0]))) {
                /* 新版四列：pin / 时间 / 正文 / 标签；老版三列没有标签 */
                if (p.length == 4) {
                    memos.add(new Memo("1".equals(p[0]), p[1], p[2], normalizeTags(p[3])));
                } else {
                    memos.add(new Memo("1".equals(p[0]), p[1], join(p, 2)));
                }
            } else if (p.length >= 1) {
                String text = p.length > 1 ? join(p, 1) : "";
                if (!text.isEmpty()) memos.add(new Memo(false, p[0], text));
            }
        }
    }

    private void save() {
        List<String> out = new ArrayList<String>();
        for (Memo m : memos) {
            out.add((m.pinned ? "1" : "0") + "\t" + m.time + "\t" + m.text + "\t" + m.tags);
        }
        Store.writeLines("memo.txt", out);
    }

    /** 标签统一成 "a,b,c"：去空格、去重、去掉开头的 #、最多 6 个 */
    public static String normalizeTags(String raw) {
        if (raw == null) return "";
        String[] parts = raw.split("[,，;；\\s]+");
        List<String> out = new ArrayList<String>();
        for (String s : parts) {
            String t = s.trim();
            while (t.startsWith("#")) t = t.substring(1).trim();
            if (t.isEmpty()) continue;
            if (!out.contains(t)) out.add(t);
            if (out.size() >= 6) break;
        }
        StringBuilder sb = new StringBuilder();
        for (String t : out) {
            if (sb.length() > 0) sb.append(',');
            sb.append(t);
        }
        return sb.toString();
    }

    /** 这条备忘有没有某个标签（精确匹配，大小写不敏感） */
    public static boolean hasTag(String tags, String tag) {
        if (tags == null || tags.isEmpty() || tag == null || tag.isEmpty()) return false;
        for (String s : tags.split(",")) {
            if (s.trim().equalsIgnoreCase(tag.trim())) return true;
        }
        return false;
    }

    private static String join(String[] p, int from) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < p.length; i++) {
            if (i > from) sb.append("\t");
            sb.append(p[i]);
        }
        return sb.toString();
    }

    /* ---------- 列表 ---------- */

    /** 把命中的关键字包成黄底加粗，没有命中时原样显示（都要先做 HTML 转义） */
    private String highlight(String s, Color base) {
        String q = query == null ? "" : query.trim();
        if (q.isEmpty()) return esc(s);
        String lower = s.toLowerCase(Locale.CHINA);
        String ql = q.toLowerCase(Locale.CHINA);
        if (!lower.contains(ql)) return esc(s);
        String hex = String.format("#%02X%02X%02X", base.getRed(), base.getGreen(), base.getBlue());
        StringBuilder sb = new StringBuilder("<html><body style='margin:0;color:" + hex + "'>");
        int from = 0;
        while (true) {
            int i = lower.indexOf(ql, from);
            if (i < 0) {
                sb.append(esc(s.substring(from)));
                break;
            }
            sb.append(esc(s.substring(from, i)));
            sb.append("<span style='background:#FEF3C7;color:#B45309'>")
                    .append(esc(s.substring(i, Math.min(s.length(), i + q.length()))))
                    .append("</span>");
            from = i + q.length();
            if (from >= s.length()) break;
        }
        sb.append("</body></html>");
        return sb.toString();
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** 跨页联动：把关键字填进搜索框（概览页待办右键“在备忘录中查找”） */
    public void searchFor(String keyword) {
        if (searchField == null) return;
        searchField.setText(keyword == null ? "" : keyword.trim());
        searchField.requestFocusInWindow();
    }

    private void reloadList() {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.CHINA);
        List<Memo> pinned = new ArrayList<Memo>();
        List<Memo> normal = new ArrayList<Memo>();
        for (int i = memos.size() - 1; i >= 0; i--) {
            Memo m = memos.get(i);
            if (!q.isEmpty()) {
                if (!m.text.toLowerCase(Locale.CHINA).contains(q) && !m.time.toLowerCase(Locale.CHINA).contains(q)) continue;
            }
            if (tagFilter != null && !hasTag(m.tags, tagFilter)) continue;
            if (m.pinned) pinned.add(0, m);
            else normal.add(0, m);
        }
        model.clear();
        for (Memo m : pinned) model.addElement(m);
        for (Memo m : normal) model.addElement(m);

        int total = pinned.size() + normal.size();
        if (q.isEmpty() && tagFilter == null) countLabel.setText("共 " + total + " 条");
        else if (tagFilter != null && q.isEmpty()) countLabel.setText("标签 " + tagFilter + " · " + total + " 条");
        else countLabel.setText("找到 " + total + " 条");
        if (clearTagBtn != null) clearTagBtn.setEnabled(tagFilter != null);
        rebuildTagBox();
    }

    /** 所有用到的标签，按字母序排 */
    public List<String> tagChoices() {
        List<String> out = new ArrayList<String>();
        for (Memo m : memos) {
            if (m.tags == null || m.tags.isEmpty()) continue;
            for (String s : m.tags.split(",")) {
                String t = s.trim();
                if (!t.isEmpty() && !out.contains(t)) out.add(t);
            }
        }
        java.util.Collections.sort(out, String.CASE_INSENSITIVE_ORDER);
        return out;
    }

    /** 下拉里的选项跟着数据走：全部标签 + 每个标签（带条数） */
    private void rebuildTagBox() {
        if (tagBox == null) return;
        String keep = tagFilter;
        rebuildingTags = true;
        try {
            tagBox.removeAllItems();
            tagBox.addItem("全部标签");
            for (String t : tagChoices()) {
                int n = 0;
                for (Memo m : memos) if (hasTag(m.tags, t)) n++;
                tagBox.addItem(t + " (" + n + ")");
            }
            if (keep == null) {
                tagBox.setSelectedIndex(0);
            } else {
                boolean hit = false;
                for (int i = 1; i < tagBox.getItemCount(); i++) {
                    String it = tagBox.getItemAt(i);
                    if (it != null && it.startsWith(keep + " (")) {
                        tagBox.setSelectedIndex(i);
                        hit = true;
                        break;
                    }
                }
                if (!hit) {
                    /* 这个标签的最后一条被删掉了，退回全部 */
                    tagFilter = null;
                    tagBox.setSelectedIndex(0);
                }
            }
        } finally {
            rebuildingTags = false;
        }
    }

    /** 当前筛出来的条数，供测试核对 */
    public int filteredCount() {
        return model.getSize();
    }

    /** 外部改了 memo.txt（比如番茄钟自动记一条）后，把内容重新读一遍 */
    public void reloadFromDisk() {
        load();
        reloadList();
    }

    /**
     * 标签改名：把所有带 from 的备忘改成 to。to 为空等于删掉这个标签；
     * to 已经有别的条目在用，就等于合并。返回被改动的条数。
     */
    public int renameTag(String from, String to) {
        String f = from == null ? "" : from.trim();
        if (f.isEmpty()) return 0;
        String t = normalizeTags(to == null ? "" : to);
        int changed = 0;
        String[] targets = t.isEmpty() ? new String[0] : t.split(",");
        for (Memo m : memos) {
            if (!hasTag(m.tags, f)) continue;
            List<String> keep = new ArrayList<String>();
            boolean placed = false;
            for (String s : m.tags.split(",")) {
                String x = s.trim();
                if (x.isEmpty()) continue;
                if (x.equalsIgnoreCase(f)) {
                    /* 原地换成新名字，位置不变；一次只放一次，多标签展开 */
                    if (!placed) {
                        for (String y : targets) {
                            String yy = y.trim();
                            if (!yy.isEmpty() && containsIgnoreCase(keep, yy) == false) keep.add(yy);
                        }
                        placed = true;
                    }
                    continue;
                }
                if (!containsIgnoreCase(keep, x)) keep.add(x);
            }
            /* 原来只有 from 一个标签、而 targets 为空时上面没放任何东西，keep 就是空的 */
            StringBuilder sb = new StringBuilder();
            for (String x : keep) {
                if (sb.length() > 0) sb.append(',');
                sb.append(x);
            }
            m.tags = sb.toString();
            changed++;
        }
        if (changed > 0) {
            save();
            if (tagFilter != null && tagFilter.equalsIgnoreCase(f)) tagFilter = t.isEmpty() ? null : t;
            reloadList();
        }
        return changed;
    }

    /** 标签列表里是否已经有这个（大小写不敏感） */
    public static boolean containsIgnoreCase(List<String> list, String tag) {
        for (String s : list) {
            if (s.equalsIgnoreCase(tag)) return true;
        }
        return false;
    }

    /** 标签条数统计：标签 → 有多少条备忘在用 */
    public java.util.Map<String, Integer> tagCounts() {
        java.util.TreeMap<String, Integer> out =
                new java.util.TreeMap<String, Integer>(String.CASE_INSENSITIVE_ORDER);
        for (Memo m : memos) {
            if (m.tags == null || m.tags.isEmpty()) continue;
            for (String s : m.tags.split(",")) {
                String t = s.trim();
                if (t.isEmpty()) continue;
                Integer v = out.get(t);
                out.put(t, (v == null ? 0 : v) + 1);
            }
        }
        return out;
    }

    /** 跨页/测试用：直接按标签筛，null 或空串表示全部 */
    public void setTagFilter(String tag) {
        tagFilter = (tag == null || tag.trim().isEmpty()) ? null : tag.trim();
        reloadList();
    }

    public String tagFilterNow() {
        return tagFilter;
    }

    /** 测试用：拿到当前筛出来的第 i 条标签 */
    public String tagAt(int i) {
        if (i < 0 || i >= model.getSize()) return null;
        return model.get(i).tags;
    }

    public int totalCount() {
        return memos.size();
    }

    private class MemoRenderer extends JPanel implements ListCellRenderer<Memo> {
        private final JLabel star = new JLabel();
        private final JLabel text = new JLabel();
        private final JLabel tag = new JLabel();
        private final JLabel time = new JLabel();

        MemoRenderer() {
            setLayout(new BorderLayout(8, 0));
            setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 12));
            star.setPreferredSize(new Dimension(18, 0));
            star.setHorizontalAlignment(SwingConstants.CENTER);
            text.setFont(Ui.font(13, false));
            tag.setFont(Ui.font(10, true));
            time.setFont(Ui.font(10, false));
            add(star, BorderLayout.WEST);
            add(text, BorderLayout.CENTER);
            /* 右边一列：先是标签，再是时间 */
            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            right.setOpaque(false);
            right.add(tag);
            right.add(time);
            add(right, BorderLayout.EAST);
        }

        public Component getListCellRendererComponent(JList<? extends Memo> l, Memo value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            star.setText(value.pinned ? "★" : "☆");
            star.setFont(Ui.font(13, true));
            Color base = isSelected
                    ? (Theme.isDark() ? new Color(0xAFC0FF) : Ui.PRIMARY)
                    : Theme.text();
            text.setText(highlight(value.text, base));
            time.setText(highlight(value.time, isSelected
                    ? (Theme.isDark() ? new Color(0xAFC0FF) : Ui.PRIMARY)
                    : Theme.muted()));
            tag.setText(value.tags == null || value.tags.isEmpty() ? "" : "#" + value.tags.replace(",", " #"));
            tag.setForeground(isSelected
                    ? (Theme.isDark() ? new Color(0xC7B6FF) : new Color(0x6D28D9))
                    : new Color(0x7C3AED));

            if (isSelected) {
                setBackground(Theme.isDark() ? new Color(0x2B3350) : new Color(0xEEF2FF));
                text.setForeground(Theme.isDark() ? new Color(0xAFC0FF) : Ui.PRIMARY);
            } else {
                setBackground(index % 2 == 0 ? Theme.bg()
                        : (Theme.isDark() ? new Color(0x1B1D25) : new Color(0xF7F9FC)));
                text.setForeground(Theme.text());
            }
            star.setForeground(value.pinned ? new Color(0xF59E0B) : Theme.muted());
            time.setForeground(Theme.muted());
            return this;
        }
    }
}
