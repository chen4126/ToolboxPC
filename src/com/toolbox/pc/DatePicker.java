package com.toolbox.pc;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** 一个小而干净的月历选择器：点日期选中，双击直接确认 */
public class DatePicker {

    private static final DateTimeFormatter HEAD =
            DateTimeFormatter.ofPattern("yyyy 年 M 月", Locale.CHINA);
    private static final String[] WEEK = {"一", "二", "三", "四", "五", "六", "日"};

    /** 返回值：选中的日期，以及这次是不是"双击新建" */
    public static final class PickResult {
        public final LocalDate day;
        public final boolean create;

        PickResult(LocalDate day, boolean create) {
            this.day = day;
            this.create = create;
        }
    }

    /** 弹出月历，返回选中的日期；取消返回 null */
    public static LocalDate pick(Component parent, LocalDate init) {
        PickResult r = pickEx(parent, init, null);
        return r == null ? null : r.day;
    }

    /** dayMap 是"某天已有的待办"，既用来画小圆点，也用来看那天的具体内容 */
    public static LocalDate pick(Component parent, LocalDate init,
                                 java.util.Map<LocalDate, java.util.List<String>> dayMap) {
        PickResult r = pickEx(parent, init, dayMap);
        return r == null ? null : r.day;
    }

    /** 同 pick，但会告诉调用方这次是"选日期"还是"双击新建" */
    public static PickResult pickEx(Component parent, LocalDate init,
                                    java.util.Map<LocalDate, java.util.List<String>> dayMap) {
        LocalDate[] out = {null};
        boolean[] create = {false};
        JDialog dlg = build(SwingUtilities.getWindowAncestor(parent), init, dayMap, out, create);
        dlg.setLocationRelativeTo(parent);
        dlg.setVisible(true);
        if (out[0] == null) return null;
        return new PickResult(out[0], create[0]);
    }

    /** 只负责把月历搭出来，显示与返回值交给调用方（便于离屏渲染验收） */
    static JDialog build(Window owner, LocalDate init,
                         java.util.Map<LocalDate, java.util.List<String>> dayMap,
                         final LocalDate[] out, final boolean[] create) {
        final JDialog dlg = new JDialog(owner,
                "选择日期", Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setUndecorated(false);

        final YearMonth[] view = {YearMonth.from(init == null ? LocalDate.now() : init)};
        final LocalDate[] sel = {init == null ? LocalDate.now() : init};

        JPanel root = new JPanel(new BorderLayout(0, 8));
        root.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        root.setBackground(Theme.isDark() ? new Color(0x1E2029) : Color.WHITE);

        /* 顶部：上月 / 月份标题 / 下月 */
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JButton prev = arrow("‹"), next = arrow("›");
        final JLabel title = new JLabel("", SwingConstants.CENTER);
        title.setFont(Ui.font(14, true));
        title.setForeground(Theme.text());
        head.add(prev, BorderLayout.WEST);
        head.add(title, BorderLayout.CENTER);
        head.add(next, BorderLayout.EAST);
        root.add(head, BorderLayout.NORTH);

        /* 中间：星期表头 + 6 行日期，全部自绘 */
        final CalGrid grid = new CalGrid();
        grid.dayMap = dayMap;
        grid.setPreferredSize(new Dimension(7 * 40, 30 + 6 * 34));
        root.add(grid, BorderLayout.CENTER);

        /* 底部：快捷项 + 按钮 */
        JPanel bottom = new JPanel(new BorderLayout(6, 0));
        bottom.setOpaque(false);
        JPanel quick = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        quick.setOpaque(false);
        JButton today = small("今天");
        JButton tomorrow = small("明天");
        JButton monday = small("下周一");
        quick.add(today);
        quick.add(tomorrow);
        quick.add(monday);

        JPanel okRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        okRow.setOpaque(false);
        JButton cancel = new JButton("取消");
        JButton ok = Ui.primary("确定");
        ok.setPreferredSize(new Dimension(76, 30));
        cancel.setPreferredSize(new Dimension(64, 30));
        okRow.add(cancel);
        okRow.add(ok);
        bottom.add(quick, BorderLayout.WEST);
        bottom.add(okRow, BorderLayout.EAST);

        /* 选中那天的待办预览：看内容再决定要不要改到这里 */
        final JLabel preview = new JLabel(" ");
        preview.setFont(Ui.font(10, false));
        preview.setForeground(Theme.muted());
        preview.setBorder(BorderFactory.createEmptyBorder(2, 2, 6, 2));
        preview.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel hint = new JLabel("双击格子 = 在那天新建一条待办，单击只是选中");
        hint.setFont(Ui.font(10, false));
        hint.setForeground(Theme.isDark() ? new Color(0x6B7280) : new Color(0x94A3B8));
        hint.setBorder(BorderFactory.createEmptyBorder(0, 2, 4, 2));
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel south = new JPanel();
        south.setOpaque(false);
        south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));
        bottom.setAlignmentX(Component.LEFT_ALIGNMENT);
        south.add(preview);
        south.add(hint);
        south.add(bottom);
        root.add(south, BorderLayout.SOUTH);

        final Runnable sync = new Runnable() {
            public void run() {
                title.setText(view[0].format(HEAD));
                grid.refresh(view[0], sel[0]);
                updatePreview(preview, sel[0], dayMap);
            }
        };

        prev.addActionListener(e -> {
            view[0] = view[0].minusMonths(1);
            sync.run();
        });
        next.addActionListener(e -> {
            view[0] = view[0].plusMonths(1);
            sync.run();
        });
        today.addActionListener(e -> {
            sel[0] = LocalDate.now();
            view[0] = YearMonth.from(sel[0]);
            sync.run();
        });
        tomorrow.addActionListener(e -> {
            sel[0] = LocalDate.now().plusDays(1);
            view[0] = YearMonth.from(sel[0]);
            sync.run();
        });
        monday.addActionListener(e -> {
            LocalDate n = LocalDate.now();
            LocalDate d = n.with(java.time.DayOfWeek.MONDAY);
            if (!d.isAfter(n)) d = d.plusWeeks(1);
            sel[0] = d;
            view[0] = YearMonth.from(d);
            sync.run();
        });
        grid.onPick = d -> {
            sel[0] = d;
            view[0] = YearMonth.from(d);
            sync.run();
        };
        grid.onDouble = d -> {
            out[0] = d;
            create[0] = true;
            dlg.dispose();
        };
        cancel.addActionListener(e -> dlg.dispose());
        ok.addActionListener(e -> {
            out[0] = sel[0];
            dlg.dispose();
        });

        sync.run();
        dlg.setContentPane(root);
        dlg.pack();
        dlg.setResizable(false);
        return dlg;
    }

    /** 预览：这天已有几条待办、分别是什么 */
    private static void updatePreview(JLabel preview, LocalDate day,
                                      java.util.Map<LocalDate, java.util.List<String>> dayMap) {
        String key = day.format(DateTimeFormatter.ofPattern("MM-dd"));
        java.util.List<String> items = dayMap == null ? null : dayMap.get(day);
        if (items == null || items.isEmpty()) {
            preview.setText(key + " · 这天没有待办");
            preview.setForeground(Theme.muted());
            return;
        }
        StringBuilder sb = new StringBuilder(key + " · " + items.size() + " 条：");
        int shown = 0;
        for (String s : items) {
            if (shown >= 3) {
                sb.append(" 等");
                break;
            }
            if (shown > 0) sb.append("、");
            sb.append(s);
            shown++;
        }
        preview.setText(sb.toString());
        preview.setForeground(Ui.PRIMARY);
    }

    private static JButton arrow(String s) {
        JButton b = new JButton(s);
        b.setFont(Ui.font(15, true));
        b.setForeground(Theme.muted());
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setBorder(BorderFactory.createEmptyBorder(2, 12, 2, 12));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private static JButton small(String s) {
        JButton b = new JButton(s);
        b.setFont(Ui.font(11, false));
        b.setFocusPainted(false);
        b.setBackground(new Color(0xEEF3FF));
        b.setForeground(Ui.PRIMARY);
        b.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        b.setPreferredSize(new Dimension(54, 26));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    /** 自绘的日期网格：7 列，周一开头 */
    private static class CalGrid extends JPanel {
        YearMonth month;
        LocalDate selected;
        java.util.Map<LocalDate, java.util.List<String>> dayMap;
        java.util.function.Consumer<LocalDate> onPick;
        java.util.function.Consumer<LocalDate> onDouble;

        CalGrid() {
            setOpaque(false);
            addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    LocalDate d = dateAt(e.getX(), e.getY());
                    if (d == null) return;
                    onPick.accept(d);
                    if (e.getClickCount() == 2) onDouble.accept(d);
                }
            });
        }

        void refresh(YearMonth m, LocalDate sel) {
            month = m;
            selected = sel;
            repaint();
        }

        private int cellW() {
            return Math.max(38, getWidth() / 7);
        }

        /** 第 index 个格子（从当月的 1 号算起有偏移）对应的日期 */
        private LocalDate dateAt(int x, int y) {
            if (month == null) return null;
            int top = 32;
            int row = (y - top) / 34;
            int col = x / cellW();
            if (row < 0 || row > 5 || col < 0 || col > 6) return null;
            int offset = month.atDay(1).getDayOfWeek().getValue() - 1;
            int day = row * 7 + col - offset + 1;
            if (day < 1 || day > month.lengthOfMonth()) return null;
            return month.atDay(day);
        }

        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int cw = cellW();
            g2.setFont(Ui.font(11, false));
            g2.setColor(Theme.muted());
            for (int i = 0; i < 7; i++) {
                String w = WEEK[i];
                int tw = g2.getFontMetrics().stringWidth(w);
                g2.drawString(w, i * cw + (cw - tw) / 2, 18);
            }
            if (month == null) {
                g2.dispose();
                return;
            }
            int offset = month.atDay(1).getDayOfWeek().getValue() - 1;
            LocalDate now = LocalDate.now();
            for (int d = 1; d <= month.lengthOfMonth(); d++) {
                int idx = offset + d - 1;
                int row = idx / 7, col = idx % 7;
                int cx = col * cw + cw / 2;
                int cy = 32 + row * 34 + 14;
                LocalDate date = month.atDay(d);
                boolean isSel = date.equals(selected);
                boolean isToday = date.equals(now);
                boolean weekend = col >= 5;
                if (isSel) {
                    g2.setColor(Ui.PRIMARY);
                    g2.fillRoundRect(cx - 15, cy - 13, 30, 26, 10, 10);
                } else if (isToday) {
                    g2.setColor(Ui.PRIMARY);
                    g2.setStroke(new BasicStroke(1.2f));
                    g2.drawRoundRect(cx - 15, cy - 13, 30, 26, 10, 10);
                }
                g2.setFont(Ui.font(12, isSel || isToday));
                g2.setColor(isSel ? Color.WHITE
                        : isToday ? Ui.PRIMARY
                        : weekend ? Theme.muted() : Theme.text());
                String s = String.valueOf(d);
                int tw = g2.getFontMetrics().stringWidth(s);
                g2.drawString(s, cx - tw / 2, cy + 5);
                /* 这天已经有待办：格子上沿一个小圆点 */
                if (dayMap != null && dayMap.containsKey(date)) {
                    g2.setColor(isSel ? Color.WHITE : new Color(0xF59E0B));
                    g2.fillOval(cx - 2, cy - 12, 4, 4);
                }
            }
            g2.dispose();
        }
    }
}
