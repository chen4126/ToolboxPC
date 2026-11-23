package com.toolbox.pc;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** 统计：汇总番茄钟/倒计时/记账/备忘录的数据记录 */
public class StatsPanel extends JPanel {

    private JLabel focusTotalLabel, focusCountLabel, todayCountLabel;
    private JLabel cdCountLabel, balanceLabel, memoCountLabel;
    private JLabel todoWeekLabel, todoMonthLabel;
    private JPanel todoWeekCard, todoMonthCard;
    private JLabel todoWeekBadge, todoMonthBadge;
    private JLabel backupValueLabel, backupSubLabel;
    private JPanel backupCard;

    /**
     * 本周 7 天每天完成几条的一行式清单（从"待办本周完成"卡片右键进）。
     * 只把周一到今天算进"没完成的天数"，周四周五还没到不背这个锅。
     */
    void showWeekDaily() {
        JPanel box = buildWeekDailyPanel();
        if (box == null) return;
        JOptionPane.showMessageDialog(this, box, "本周逐日明细", JOptionPane.PLAIN_MESSAGE);
    }

    /** 一组完成条目：一条一行（MM-dd + 文字），供点某周 / 某天展开 */
    JPanel buildDoneItemsPanel(String title, java.util.List<String[]> rows) {
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setOpaque(false);
        box.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 14));
        JLabel head = new JLabel(title);
        head.setFont(Ui.font(11, true));
        head.setForeground(new Color(0x1D4ED8));
        head.setAlignmentX(Component.LEFT_ALIGNMENT);
        head.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        box.add(head);
        if (rows == null || rows.isEmpty()) {
            JLabel empty = new JLabel("这段时间还没有完成的条目");
            empty.setFont(Ui.font(11, false));
            empty.setForeground(Theme.muted());
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            empty.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
            box.add(empty);
            return box;
        }
        for (String[] t : rows) {
            String d = t[0] == null ? "" : t[0];
            if (d.length() >= 10) d = d.substring(5);          // 2026-11-09 → 11-09
            JLabel l = new JLabel(d + "   " + (t[1] == null ? "" : t[1]));
            l.setFont(Ui.font(11, false));
            l.setAlignmentX(Component.LEFT_ALIGNMENT);
            l.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
            l.setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 0));
            box.add(l);
        }
        return box;
    }

    /** 点明细里的某一行（某周某天）时，列出那段时间完成的每一条 */
    void showDoneInRange(String title, java.time.LocalDate from, java.time.LocalDate to) {
        OverviewPanel ov = Main.page(OverviewPanel.class);
        if (ov == null) {
            Main.notifyMsg("打不开", "概览页还没准备好，等两秒再点一次");
            return;
        }
        java.util.List<String[]> rows = ov.doneInRange(from, to);
        JPanel box = buildDoneItemsPanel("共完成 " + rows.size() + " 条 · " + title, rows);
        JOptionPane.showMessageDialog(this, box, title, JOptionPane.PLAIN_MESSAGE);
    }

    /** 近三周对比小表：上上周 / 上周 / 本周各自的完成量、有完成的天、最勤一天、环比 */
    JPanel buildThreeWeekPanel() {
        OverviewPanel ov = Main.page(OverviewPanel.class);
        if (ov == null) {
            Main.notifyMsg("打不开", "概览页还没准备好，等两秒再点一次");
            return null;
        }
        java.time.LocalDate mon = java.time.LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        String[] names = {"上上周", "上周", "本周"};
        JPanel box = new JPanel(new java.awt.GridLayout(4, 5, 6, 5));
        box.setOpaque(false);
        box.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 14));

        String[] head = {"周次", "完成", "天数", "最勤", "环比"};
        for (String h : head) {
            JLabel l = new JLabel(h);
            l.setFont(Ui.font(10, true));
            l.setForeground(Theme.muted());
            box.add(l);
        }
        int prevSize = -1;
        for (int i = 0; i < 3; i++) {
            int back = 2 - i;
            java.time.LocalDate from = mon.minusWeeks(back);
            java.time.LocalDate to = from.plusDays(6);
            java.util.List<String[]> rows = ov.doneInRange(from, to);
            java.util.Map<String, Integer> byDay = new java.util.TreeMap<String, Integer>();
            for (String[] r : rows) byDay.put(r[0], byDay.containsKey(r[0]) ? byDay.get(r[0]) + 1 : 1);
            int best = 0;
            String bestDay = "";
            for (java.util.Map.Entry<String, Integer> en : byDay.entrySet()) {
                if (en.getValue() > best) {
                    best = en.getValue();
                    bestDay = en.getKey().substring(5) + "（" + en.getValue() + "）";
                }
            }
            boolean isThisWeek = back == 0;
            /* 每行都能右键：这一周拆到天 / 看具体条目 / 复制这一周完成的条目 */
            final java.time.LocalDate fRef = from, tRef = to;
            final String wn = names[i];
            JPopupMenu rowMenu = new JPopupMenu();
            JMenuItem dailyItem = new JMenuItem("看这周逐日明细");
            dailyItem.addActionListener(e -> showRangeDaily(wn, fRef, tRef));
            JMenuItem listItem = new JMenuItem("看这周完成了哪几条");
            listItem.addActionListener(e -> showDoneInRange(wn, fRef, tRef));
            JMenuItem copyItem = new JMenuItem("复制这周完成的条目");
            copyItem.addActionListener(e -> copyRangeItems(wn, fRef, tRef));
            rowMenu.add(dailyItem);
            rowMenu.add(listItem);
            rowMenu.add(copyItem);

            JLabel nameL = new JLabel(names[i]);
            nameL.setFont(Ui.font(11, isThisWeek));
            if (isThisWeek) nameL.setForeground(Ui.PRIMARY);
            nameL.setComponentPopupMenu(rowMenu);
            box.add(nameL);

            JLabel cntL = new JLabel(rows.size() + " 条");
            cntL.setFont(Ui.font(11, isThisWeek));
            cntL.setToolTipText(from.toString().substring(5) + " ~ " + to.toString().substring(5));
            if (isThisWeek) cntL.setForeground(Ui.PRIMARY);
            cntL.setComponentPopupMenu(rowMenu);
            box.add(cntL);

            JLabel dayL = new JLabel(byDay.size() + " 天");
            dayL.setFont(Ui.font(11, false));
            if (isThisWeek) dayL.setForeground(Ui.PRIMARY);
            dayL.setComponentPopupMenu(rowMenu);
            box.add(dayL);

            JLabel bestL = new JLabel(best > 0 ? bestDay : "—");
            bestL.setFont(Ui.font(10, false));
            bestL.setForeground(best > 0 ? Theme.muted() : Theme.muted());
            bestL.setComponentPopupMenu(rowMenu);
            box.add(bestL);

            String deltaTxt = prevSize < 0 ? "—"
                    : (rows.size() == prevSize ? "持平"
                    : (rows.size() > prevSize ? "+" + (rows.size() - prevSize) : String.valueOf(rows.size() - prevSize)));
            JLabel deltaL = new JLabel(deltaTxt);
            deltaL.setFont(Ui.font(10, isThisWeek));
            deltaL.setForeground(prevSize < 0 ? Theme.muted()
                    : (rows.size() > prevSize ? new Color(0x16A34A) : (rows.size() < prevSize ? new Color(0xDC2626) : Theme.muted())));
            deltaL.setComponentPopupMenu(rowMenu);
            box.add(deltaL);
            prevSize = rows.size();
        }
        return box;
    }

    void showThreeWeekCompare() {
        JPanel box = buildThreeWeekPanel();
        if (box == null) return;
        JOptionPane.showMessageDialog(this, box, "近三周对比", JOptionPane.PLAIN_MESSAGE);
    }

    /** 把某一段区间拆成 7 天逐日列表（供三周对比每行右键用） */
    JPanel buildRangeDailyPanel(String title, java.time.LocalDate from, java.time.LocalDate to) {
        OverviewPanel ov = Main.page(OverviewPanel.class);
        if (ov == null) {
            Main.notifyMsg("打不开", "概览页还没准备好，等两秒再点一次");
            return null;
        }
        java.util.List<String[]> rows = ov.doneInRange(from, to);
        java.util.Map<String, Integer> byDay = new java.util.TreeMap<String, Integer>();
        for (String[] r : rows) {
            int c = byDay.containsKey(r[0]) ? byDay.get(r[0]) : 0;
            byDay.put(r[0], c + 1);
        }
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setOpaque(false);
        box.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 12));

        JLabel head = new JLabel("共完成 " + rows.size() + " 条 · " + title);
        head.setFont(Ui.font(11, true));
        head.setForeground(new Color(0x1D4ED8));
        head.setAlignmentX(Component.LEFT_ALIGNMENT);
        head.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        box.add(head);

        String[] wk = {"一", "二", "三", "四", "五", "六", "日"};
        java.time.LocalDate today = java.time.LocalDate.now();
        for (int k = 0; k < 7; k++) {
            java.time.LocalDate d = from.plusDays(k);
            String key = d.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            int c = byDay.containsKey(key) ? byDay.get(key) : 0;
            String tail = d.isAfter(today) ? "还没到" : (c > 0 ? c + " 条" : "0 条");
            JLabel l = new JLabel(d.format(java.time.format.DateTimeFormatter.ofPattern("MM-dd"))
                    + " 周" + wk[d.getDayOfWeek().getValue() - 1] + "   " + tail);
            l.setFont(Ui.font(11, c > 0));
            l.setForeground(c > 0 ? Theme.text() : Theme.muted());
            l.setAlignmentX(Component.LEFT_ALIGNMENT);
            l.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
            l.setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 0));
            if (c > 0) l.setToolTipText("点一下看这天完成的 " + c + " 条");
            box.add(l);
        }
        return box;
    }

    void showRangeDaily(String title, java.time.LocalDate from, java.time.LocalDate to) {
        JPanel box = buildRangeDailyPanel(title, from, to);
        if (box == null) return;
        JOptionPane.showMessageDialog(this, box, title + " 逐日明细", JOptionPane.PLAIN_MESSAGE);
    }

    /** 把某一段区间完成的条目抄进剪贴板 */
    int copyRangeItems(String title, java.time.LocalDate from, java.time.LocalDate to) {
        OverviewPanel ov = Main.page(OverviewPanel.class);
        if (ov == null) return 0;
        java.util.List<String[]> rows = ov.doneInRange(from, to);
        if (rows.isEmpty()) {
            Main.notifyMsg("这段时间没有完成的条目", title + " 里一条都没打勾");
            return 0;
        }
        StringBuilder sb = new StringBuilder();
        for (String[] r : rows) {
            if (sb.length() > 0) sb.append("\n");
            String d = r[0].length() >= 10 ? r[0].substring(5) : r[0];
            sb.append(d).append("   ").append(r[1]);
        }
        java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new java.awt.datatransfer.StringSelection(sb.toString()), null);
        Main.notifyMsg("已复制 " + rows.size() + " 条", title + " 完成的条目已放进剪贴板");
        return rows.size();
    }

    /** 只造面板、不弹窗，方便离屏截图和断言 */
    JPanel buildWeekDailyPanel() {
        OverviewPanel ov = Main.page(OverviewPanel.class);
        if (ov == null) {
            Main.notifyMsg("打不开", "概览页还没准备好，等两秒再点一次");
            return null;
        }
        int[] c = ov.weekDoneCounts();
        String[] wk = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        java.time.LocalDate mon = java.time.LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        int todayIdx = java.time.LocalDate.now().getDayOfWeek().getValue() - 1;
        int total = 0, idle = 0, best = -1, bestIdx = -1;
        for (int i = 0; i < 7; i++) {
            total += c[i];
            if (i <= todayIdx) {
                if (c[i] == 0) idle++;
                if (c[i] > best) {
                    best = c[i];
                    bestIdx = i;
                }
            }
        }

        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setOpaque(false);
        box.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 12));

        JLabel sum = new JLabel("本周共完成 " + total + " 条"
                + (idle > 0 ? " · 有 " + idle + " 天一条没完成" : " · 到今天天天都有完成"));
        sum.setFont(Ui.font(11, true));
        sum.setForeground(new Color(0x1D4ED8));
        sum.setAlignmentX(Component.LEFT_ALIGNMENT);
        sum.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        box.add(sum);
        if (bestIdx >= 0 && best > 0) {
            JLabel bestL = new JLabel("最勤的一天：" + wk[bestIdx] + " " + best + " 条");
            bestL.setFont(Ui.font(10, false));
            bestL.setForeground(Theme.muted());
            bestL.setAlignmentX(Component.LEFT_ALIGNMENT);
            bestL.setBorder(BorderFactory.createEmptyBorder(2, 0, 6, 0));
            bestL.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
            box.add(bestL);
        }
        for (int i = 0; i < 7; i++) {
            java.time.LocalDate d = mon.plusDays(i);
            boolean isToday = i == todayIdx;
            boolean future = i > todayIdx;
            JPanel row = new JPanel(new BorderLayout(10, 0));
            row.setOpaque(false);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
            JLabel left = new JLabel(wk[i] + "  " + d.toString().substring(5) + (isToday ? "（今天）" : ""));
            left.setFont(Ui.font(11, isToday));
            JLabel right = new JLabel(future ? "—  还没到" : (c[i] == 0 ? "0 条  没完成" : c[i] + " 条"));
            right.setFont(Ui.font(11, isToday));
            right.setHorizontalAlignment(SwingConstants.RIGHT);
            if (isToday) {
                left.setForeground(Ui.PRIMARY);
                right.setForeground(Ui.PRIMARY);
            } else if (future || c[i] == 0) {
                left.setForeground(Theme.muted());
                right.setForeground(Theme.muted());
            }
            row.add(left, BorderLayout.WEST);
            row.add(right, BorderLayout.EAST);
            final int idx = i;
            final java.time.LocalDate dayD = d;
            final String dayLabel = wk[idx] + " " + dayD.toString().substring(5);
            if (!future && c[idx] > 0) {
                row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                row.setToolTipText("点一下看这天完成的 " + c[idx] + " 条");
                row.addMouseListener(new java.awt.event.MouseAdapter() {
                    public void mouseClicked(java.awt.event.MouseEvent e) {
                        if (!javax.swing.SwingUtilities.isLeftMouseButton(e)) return;
                        showDoneInRange(dayLabel, dayD, dayD);
                    }
                });
            }
            box.add(row);
        }
        return box;
    }

    void showMonthDaily() {
        JPanel box = buildMonthDailyPanel();
        if (box == null) return;
        JOptionPane.showMessageDialog(this, box, "本月逐日明细", JOptionPane.PLAIN_MESSAGE);
    }

    /**
     * 本月明细按自然周分行：每段写完成几条 + 段内最勤的那天。
     * 本月 1 号不一定是周一，所以第一段只有到本周日的那几天，最后一段截到月末。
     */
    JPanel buildMonthDailyPanel() {
        OverviewPanel ov = Main.page(OverviewPanel.class);
        if (ov == null) {
            Main.notifyMsg("打不开", "概览页还没准备好，等两秒再点一次");
            return null;
        }
        int[] c = ov.monthDoneCounts();
        java.time.LocalDate now = java.time.LocalDate.now();
        java.time.LocalDate first = now.withDayOfMonth(1);
        int days = now.lengthOfMonth();
        int todayD = now.getDayOfMonth();

        java.util.List<int[]> segs = new java.util.ArrayList<int[]>();
        int d = 1;
        while (d <= days) {
            java.time.LocalDate cur = first.withDayOfMonth(d);
            int end = d + (7 - cur.getDayOfWeek().getValue());   // 走到本周日
            if (end > days) end = days;
            segs.add(new int[]{d, end});
            d = end + 1;
        }

        int total = 0, idle = 0;
        for (int i = 0; i < todayD; i++) {
            total += c[i];
            if (c[i] == 0) idle++;
        }
        int bestSeg = -1, bestSum = -1, bestDay = -1, bestDayCnt = 0;
        int[] segSum = new int[segs.size()];
        for (int si = 0; si < segs.size(); si++) {
            int[] s = segs.get(si);
            int sum = 0, topDay = -1, topCnt = 0;
            for (int k = s[0]; k <= Math.min(s[1], todayD); k++) {
                sum += c[k - 1];
                if (c[k - 1] > topCnt) {
                    topCnt = c[k - 1];
                    topDay = k;
                }
            }
            segSum[si] = sum;
            if (s[0] <= todayD && sum > bestSum && sum > 0) {
                bestSum = sum;
                bestSeg = si;
                bestDay = topDay;
                bestDayCnt = topCnt;
            }
        }

        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setOpaque(false);
        box.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 12));

        JLabel sum = new JLabel("本月共完成 " + total + " 条"
                + (idle > 0 ? " · 到目前有 " + idle + " 天没完成" : " · 天天都有完成"));
        sum.setFont(Ui.font(11, true));
        sum.setForeground(new Color(0x1D4ED8));
        sum.setAlignmentX(Component.LEFT_ALIGNMENT);
        sum.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        box.add(sum);

        if (bestSeg >= 0 && bestSum > 0) {
            int[] s = segs.get(bestSeg);
            String tail = bestDay > 0 && bestDayCnt > 0
                    ? " · 最勤 " + first.withDayOfMonth(bestDay).toString().substring(5)
                    + "（" + bestDayCnt + " 条）" : "";
            JLabel bestL = new JLabel("最勤的一周：第 " + (bestSeg + 1) + " 周 " + bestSum + " 条" + tail);
            bestL.setFont(Ui.font(10, false));
            bestL.setForeground(Theme.muted());
            bestL.setAlignmentX(Component.LEFT_ALIGNMENT);
            bestL.setBorder(BorderFactory.createEmptyBorder(2, 0, 6, 0));
            bestL.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
            box.add(bestL);
        }

        for (int si = 0; si < segs.size(); si++) {
            int[] s = segs.get(si);
            boolean future = s[0] > todayD;
            java.time.LocalDate from = first.withDayOfMonth(s[0]);
            java.time.LocalDate to = first.withDayOfMonth(s[1]);
            String range = s[0] == s[1] ? from.toString().substring(5)
                    : from.toString().substring(5) + " ~ " + to.toString().substring(5);
            JPanel row = new JPanel(new BorderLayout(10, 0));
            row.setOpaque(false);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
            JLabel left = new JLabel("第 " + (si + 1) + " 周  " + range);
            left.setFont(Ui.font(11, si == bestSeg));
            JLabel right;
            if (future) {
                right = new JLabel("—  还没到");
            } else if (segSum[si] == 0) {
                right = new JLabel("0 条");
            } else {
                String topTxt = "";
                for (int k = s[0]; k <= Math.min(s[1], todayD); k++) {
                    if (c[k - 1] > 0 && c[k - 1] == getMax(c, s[0], Math.min(s[1], todayD))) {
                        topTxt = "   最勤 " + first.withDayOfMonth(k).toString().substring(5)
                                + "（" + c[k - 1] + "）";
                        break;
                    }
                }
                right = new JLabel(segSum[si] + " 条" + topTxt);
            }
            right.setFont(Ui.font(11, si == bestSeg));
            right.setHorizontalAlignment(SwingConstants.RIGHT);
            if (si == bestSeg && !future) {
                left.setForeground(Ui.PRIMARY);
                right.setForeground(Ui.PRIMARY);
            } else if (future || segSum[si] == 0) {
                left.setForeground(Theme.muted());
                right.setForeground(Theme.muted());
            }
            row.add(left, BorderLayout.WEST);
            row.add(right, BorderLayout.EAST);
            final java.time.LocalDate segFrom = from;
            final java.time.LocalDate segTo = to;
            final int segIdx = si;
            final String segLabel = "第 " + (si + 1) + " 周（"
                    + from.toString().substring(5) + " ~ " + to.toString().substring(5) + "）";
            if (!future && segSum[si] > 0) {
                row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                row.setToolTipText("点一下看这一周完成的 " + segSum[si] + " 条");
                row.addMouseListener(new java.awt.event.MouseAdapter() {
                    public void mouseClicked(java.awt.event.MouseEvent e) {
                        if (!javax.swing.SwingUtilities.isLeftMouseButton(e)) return;
                        showDoneInRange(segLabel, segFrom, segTo);
                    }
                });
            }
            box.add(row);
        }
        return box;
    }

    private static int getMax(int[] c, int fromDay, int toDay) {
        int m = 0;
        for (int k = fromDay; k <= toDay; k++) if (c[k - 1] > m) m = c[k - 1];
        return m;
    }

    /** 从别处跳过来时把对应那张待办卡描一圈主色，1.8 秒后自己复原 */
    public static void highlightTodo(String which) {
        StatsPanel sp = Main.page(StatsPanel.class);
        if (sp == null) return;
        boolean month = "month".equals(which);
        final JPanel card = month ? sp.todoMonthCard : sp.todoWeekCard;
        if (card == null) return;
        try {
            sp.refresh();                       // 跳过来先把数字算到最新
        } catch (Exception ignored) {
        }
        final javax.swing.border.Border old = card.getBorder();
        Color c = month ? new Color(0x1D4ED8) : new Color(0x2563EB);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(c, 2),
                BorderFactory.createEmptyBorder(8, 13, 8, 11)));
        card.repaint();
        javax.swing.Timer t = new javax.swing.Timer(1800, e -> {
            card.setBorder(old);
            card.repaint();
        });
        t.setRepeats(false);
        t.start();
    }
    private DefaultTableModel pomodoroModel, countdownModel;
    private static final SimpleDateFormat DAY = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);

    public StatsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        setBackground(Theme.bg());

        JPanel top = new JPanel(new GridLayout(2, 4, 10, 10));
        top.setOpaque(false);
        focusTotalLabel = new JLabel();
        focusCountLabel = new JLabel();
        todayCountLabel = new JLabel();
        cdCountLabel = new JLabel();
        balanceLabel = new JLabel();
        memoCountLabel = new JLabel();
        todoWeekLabel = new JLabel();
        todoMonthLabel = new JLabel();
        JPanel totalCard = numCard("番茄专注总时长", focusTotalLabel, new Color(0x3F51B5));
        JPanel countCard = numCard("完成番茄总数", focusCountLabel, new Color(0x00ACC1));
        JPanel todayCard = numCard("今日番茄", todayCountLabel, new Color(0x00897B));
        linkPomodoro(totalCard, "sum", "点一下：看这些专注按任务汇总成什么");
        linkPomodoro(countCard, "week", "点一下：看本周每一轮番茄是几点做的");
        linkPomodoro(todayCard, "today", "点一下：看今天每一轮番茄是几点做的");
        top.add(totalCard);
        top.add(countCard);
        top.add(todayCard);
        top.add(numCard("倒计时完成次数", cdCountLabel, new Color(0x7B1FA2)));
        todoWeekBadge = new JLabel(" ");
        todoWeekBadge.setFont(Ui.font(9, false));
        todoWeekBadge.setForeground(Theme.muted());
        todoMonthBadge = new JLabel(" ");
        todoMonthBadge.setFont(Ui.font(9, false));
        todoMonthBadge.setForeground(Theme.muted());
        todoWeekCard = numCard("待办本周完成", todoWeekLabel, new Color(0x2563EB), todoWeekBadge);
        todoWeekCard.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        todoWeekCard.setToolTipText("本周完成的待办清单 · 点一下看具体是哪几条");
        todoWeekCard.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (!javax.swing.SwingUtilities.isLeftMouseButton(e)) return;
                OverviewPanel ov = Main.page(OverviewPanel.class);
                if (ov != null) ov.openDoneList("week");
            }
        });
        top.add(todoWeekCard);
        /* 右键：一行式看本周每天完成几条（左键还是进完整清单） */
        JPopupMenu weekMenu = new JPopupMenu();
        JMenuItem weekDaily = new JMenuItem("看本周逐日明细");
        weekDaily.setToolTipText("一行一天：周一到周日各完成几条，谁在拖后腿一眼看出来");
        weekDaily.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                showWeekDaily();
            }
        });
        weekMenu.add(weekDaily);
        JMenuItem weekList = new JMenuItem("看具体是哪几条");
        weekList.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                OverviewPanel ov = Main.page(OverviewPanel.class);
                if (ov != null) ov.openDoneList("week");
            }
        });
        weekMenu.add(weekList);
        JMenuItem weekCompare = new JMenuItem("看近三周对比");
        weekCompare.setToolTipText("上上周 / 上周 / 本周：完成几条、有几天、最勤哪天、比前一周多还是少");
        weekCompare.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                showThreeWeekCompare();
            }
        });
        weekMenu.add(weekCompare);
        todoWeekCard.setComponentPopupMenu(weekMenu);
        todoMonthCard = numCard("待办本月完成", todoMonthLabel, new Color(0x1D4ED8), todoMonthBadge);
        todoMonthCard.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        todoMonthCard.setToolTipText("本月完成的待办清单 · 点一下看具体是哪几条");
        todoMonthCard.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (!javax.swing.SwingUtilities.isLeftMouseButton(e)) return;
                OverviewPanel ov = Main.page(OverviewPanel.class);
                if (ov != null) ov.openDoneList("month");
            }
        });
        top.add(todoMonthCard);
        JPopupMenu monthMenu = new JPopupMenu();
        JMenuItem monthDaily = new JMenuItem("看本月逐日明细");
        monthDaily.setToolTipText("按周分行：每周完成几条、哪天最勤");
        monthDaily.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                showMonthDaily();
            }
        });
        monthMenu.add(monthDaily);
        JMenuItem monthList = new JMenuItem("看这个月完成了哪几条");
        monthList.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                OverviewPanel ov = Main.page(OverviewPanel.class);
                if (ov != null) ov.openDoneList("month");
            }
        });
        monthMenu.add(monthList);
        JMenuItem monthCopy = new JMenuItem("复制这个月完成的条目");
        monthCopy.setToolTipText("本月完成的条目连同日期一起放进剪贴板");
        monthCopy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                java.time.LocalDate from = java.time.LocalDate.now().withDayOfMonth(1);
                copyRangeItems("本月", from, java.time.LocalDate.now());
            }
        });
        monthMenu.add(monthCopy);
        todoMonthCard.setComponentPopupMenu(monthMenu);
        top.add(numCard("记账结余", balanceLabel, new Color(0x43A047)));
        top.add(numCard("备忘录条数", memoCountLabel, new Color(0xFB8C00)));

        /* 数据备份：一眼看现在有几份快照、数据占多大，点开就是备份管理 */
        backupValueLabel = new JLabel("0 份");
        backupSubLabel = new JLabel();
        backupSubLabel.setFont(Ui.font(11, false));
        backupSubLabel.setForeground(Theme.muted());
        JPanel backupCard = numCard("数据备份", backupValueLabel, new Color(0x546E7A), backupSubLabel);
        this.backupCard = backupCard;
        backupCard.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backupCard.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (!javax.swing.SwingUtilities.isLeftMouseButton(e)) return;
                SettingsPanel.openBackupDialogFromAnywhere();
            }
        });
        top.add(backupCard);
        JPanel northWrap = new JPanel(new BorderLayout(0, 8));
        northWrap.setOpaque(false);
        JPanel rangeRow = new JPanel(new BorderLayout(12, 0));
        rangeRow.setOpaque(false);
        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        chips.setOpaque(false);
        range7 = rangeChip("近 7 天", 7);
        range30 = rangeChip("近 30 天", 30);
        ButtonGroup bg = new ButtonGroup();
        bg.add(range7);
        bg.add(range30);
        range7.setSelected(true);
        chips.add(range7);
        chips.add(range30);
        weekInfo = new JLabel(" ");
        weekInfo.setFont(Ui.font(11, false));
        weekInfo.setForeground(Theme.muted());
        weekInfo.setHorizontalAlignment(SwingConstants.RIGHT);
        rangeRow.add(chips, BorderLayout.WEST);
        rangeRow.add(weekInfo, BorderLayout.EAST);
        northWrap.add(rangeRow, BorderLayout.NORTH);
        northWrap.add(top, BorderLayout.CENTER);

        /* 一条专注小结：本月最专注的一天 + 最长连续记录，点一下看明细 */
        focusSummary = new JLabel(" ");
        focusSummary.setFont(Ui.font(11, false));
        focusSummary.setForeground(Theme.muted());
        focusSummary.setBorder(BorderFactory.createEmptyBorder(3, 4, 0, 4));
        focusSummary.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        focusSummary.setToolTipText("点一下看本月最专注那天和最长连续段的明细");
        focusSummary.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (!javax.swing.SwingUtilities.isLeftMouseButton(e)) return;
                showFocusDetail();
            }
        });
        northWrap.add(focusSummary, BorderLayout.SOUTH);
        add(northWrap, BorderLayout.NORTH);
        JPanel tables = new JPanel(new GridLayout(1, 3, 10, 0));
        tables.setOpaque(false);
        pomodoroModel = new DefaultTableModel(new Object[]{"时间", "时长", "类型"}, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        countdownModel = new DefaultTableModel(new Object[]{"时间", "时长"}, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        taskRank = new TaskRank();
        tables.add(recordTable("番茄钟记录", pomodoroModel, 100, 70, 55));
        tables.add(recordTable("倒计时记录", countdownModel, 120, 90));
        tables.add(taskRank);

        chart = new BarChart();
        donut = new DonutChart();
        expChart = new BarChart();
        expChart.setTint(new Color(0xF97316), "本周", "上周");
        expChart.setLegend(false);
        donut.setOnPick(n -> {
            spendFilter = n;
            refresh();
        });
        JPanel chartsRow = new JPanel(new GridLayout(1, 3, 10, 0));
        chartsRow.setOpaque(false);
        chartsRow.add(chart);
        chartsRow.add(expChart);
        chartsRow.add(donut);

        JPanel centerWrap = new ScrollHost();
        centerWrap.setOpaque(false);
        centerWrap.setLayout(new BorderLayout(0, 10));
        // 右侧留出滚动条的宽度：FlatLaf 的滚动条是浮在内容上的，不留空会压住最右一张卡片的边
        centerWrap.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 14));
        trend = new MonthTrend();
        JPanel chartStack = new JPanel(new BorderLayout(0, 10));
        chartStack.setOpaque(false);
        chartStack.add(chartsRow, BorderLayout.NORTH);
        chartStack.add(trend, BorderLayout.CENTER);
        chartStack.setPreferredSize(new Dimension(100, 216));
        centerWrap.add(chartStack, BorderLayout.NORTH);
        tables.setPreferredSize(new Dimension(100, 176));
        centerWrap.add(tables, BorderLayout.CENTER);

        JScrollPane pageScroll = new JScrollPane(centerWrap);
        pageScroll.setBorder(BorderFactory.createEmptyBorder());
        pageScroll.setOpaque(false);
        pageScroll.getViewport().setOpaque(false);
        pageScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        pageScroll.getVerticalScrollBar().setUnitIncrement(18);
        add(pageScroll, BorderLayout.CENTER);

        styleRange();
        refresh();
    }

    private BarChart chart;
    private BarChart expChart;
    private DonutChart donut;
    private MonthTrend trend;
    /** 环形图点选后的支出分类筛选，null 表示不筛选 */
    private String spendFilter;
    private TaskRank taskRank;
    private JLabel weekInfo;
    private JLabel focusSummary;
    /** 小结条算出来的结果，点击明细时用 */
    private String sumBestDay = "";
    private int sumBestMin = 0, sumBestCnt = 0, sumStreak = 0;
    private String sumStreakFrom = "", sumStreakTo = "";
    private int range = 7;
    private JToggleButton range7, range30;

    private static int focusOn(java.util.List<String> plog, String day) {
        int sum = 0;
        for (String line : plog) {
            String[] p = line.split("\t", -1);
            if (p.length < 4 || !"专注".equals(p[3])) continue;
            if (p[1] == null || !p[1].startsWith(day)) continue;
            try {
                sum += Integer.parseInt(p[2].trim());
            } catch (Exception ignored) {
            }
        }
        return sum;
    }

    /** 某天（MM-dd）的支出合计，四舍五入到元 */
    private static int spendOn(java.util.List<String> ledger, String mmdd) {
        double sum = 0;
        for (String line : ledger) {
            String[] p = line.split("\t", -1);
            if (p.length < 4 || !"0".equals(p[0])) continue;
            if (p[3] == null || !p[3].startsWith(mmdd)) continue;
            try {
                sum += Double.parseDouble(p[1].trim());
            } catch (Exception ignored) {
            }
        }
        return (int) Math.round(sum);
    }

    /** 某天（MM-dd）指定分类的支出合计，四舍五入到元 */
    private static int spendOnCat(java.util.List<String> ledger, String mmdd, String cat) {
        double sum = 0;
        for (String line : ledger) {
            String[] p = line.split("\t", -1);
            if (p.length < 4 || !"0".equals(p[0])) continue;
            if (p[3] == null || !p[3].startsWith(mmdd)) continue;
            String c = p.length > 4 && !p[4].trim().isEmpty() ? p[4].trim() : "未分类";
            if (!c.equals(cat)) continue;
            try {
                sum += Double.parseDouble(p[1].trim());
            } catch (Exception ignored) {
            }
        }
        return (int) Math.round(sum);
    }

    /** 筛选状态下，右上角补一行“该类本月合计（N 笔）” */
    private String filterLine(java.util.List<String> ledger) {
        if (spendFilter == null) return "";
        String prefix = new SimpleDateFormat("MM-", Locale.CHINA).format(new Date());
        double sum = 0;
        int cnt = 0;
        for (String line : ledger) {
            String[] p = line.split("\t", -1);
            if (p.length < 4 || !"0".equals(p[0].trim())) continue;
            if (!p[3].trim().startsWith(prefix)) continue;
            String c = p.length > 4 && !p[4].trim().isEmpty() ? p[4].trim() : "未分类";
            if (!c.equals(spendFilter)) continue;
            sum += safeAmt(p[1]);
            cnt++;
        }
        String color = Theme.isDark() ? "#FBBF24" : "#C2410C";
        return "<br><span style='color:" + color + "'>已筛选 " + spendFilter + "</span> · 本月 <b>¥"
                + String.format(Locale.CHINA, "%.2f", sum) + "</b>（" + cnt + " 笔）";
    }

    private static double safeAmt(String s) {
        try {
            return Double.parseDouble(s.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private String deltaStr(double nowV, double prevV, String base) {
        if (prevV <= 0) return base + " —";
        int pct = (int) Math.round((nowV - prevV) * 100 / prevV);
        if (pct == 0) return "与" + base + "持平";
        return (pct > 0 ? "+" : "") + pct + "%";
    }

    private String colorDelta(double nowV, double prevV, String base) {
        String d = deltaStr(nowV, prevV, base);
        if (prevV <= 0 || ("与" + base + "持平").equals(d)) {
            return "<span style='color:#94A3B8'>" + d + "</span>";
        }
        boolean up = d.startsWith("+");
        return "<span style='color:" + (up ? "#22C55E" : "#EF4444") + "'>" + d + "</span>";
    }

    private JToggleButton rangeChip(String text, final int days) {
        JToggleButton b = new JToggleButton(text);
        b.setFont(Ui.font(12, true));
        b.setFocusPainted(false);
        b.setPreferredSize(new Dimension(88, 28));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                range = days;
                styleRange();
                refresh();
            }
        });
        return b;
    }

    private void styleRange() {
        for (JToggleButton b : new JToggleButton[]{range7, range30}) {
            if (b == null) continue;
            boolean on = b.isSelected();
            b.setBackground(on ? Ui.PRIMARY
                    : (Theme.isDark() ? new Color(0x2A2D38) : new Color(0xEEF2FF)));
            b.setForeground(on ? Color.WHITE
                    : (Theme.isDark() ? new Color(0xAFC0FF) : Ui.PRIMARY));
        }
    }

    private static Color catColor(String name) {
        if ("餐饮".equals(name)) return new Color(0xF97316);
        if ("交通".equals(name)) return new Color(0x3B82F6);
        if ("购物".equals(name)) return new Color(0x8B5CF6);
        if ("居住".equals(name)) return new Color(0x14B8A6);
        if ("娱乐".equals(name)) return new Color(0xEC4899);
        if ("医疗".equals(name)) return new Color(0xEF4444);
        if ("学习".equals(name)) return new Color(0x6366F1);
        if ("人情".equals(name)) return new Color(0xF59E0B);
        if ("其他".equals(name)) return new Color(0x94A3B8);
        return new Color(0xCBD5E1);
    }

    /** 本月支出分类环形图 */
    private static class DonutChart extends JPanel {
        private String title = "本月支出分类";
        private String[] names = new String[0];
        private double[] values = new double[0];
        private double total = 0;
        private String selected;
        private java.util.function.Consumer<String> onPick;

        DonutChart() {
            setOpaque(true);
            setBackground(Theme.bg());
            setPreferredSize(new Dimension(0, 112));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Theme.border(), 1),
                    BorderFactory.createEmptyBorder(10, 14, 8, 14)));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("点某一类只看该类支出，再点一次恢复");
            addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (values.length == 0 || total <= 0) return;
                    Insets in = getInsets();
                    int h = getHeight() - in.top - in.bottom;
                    int areaH = h - 22;
                    int d = Math.min(areaH - 6, 96);
                    if (d < 40) return;
                    int r = d / 2;
                    int cx = in.left + r + 4;
                    int cy = in.top + 22 + areaH / 2;
                    double dx = e.getX() - cx, dy = e.getY() - cy;
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    if (dist < d * 0.34 || dist > r + 6) return;
                    double ang = (Math.toDegrees(Math.atan2(dy, dx)) + 90 + 720) % 360;
                    double acc = 0;
                    for (int i = 0; i < values.length; i++) {
                        double span = 360.0 * values[i] / total;
                        if (ang >= acc && ang < acc + span) {
                            String n = names[i];
                            selected = n.equals(selected) ? null : n;
                            if (onPick != null) onPick.accept(selected);
                            repaint();
                            return;
                        }
                        acc += span;
                    }
                }
            });
        }

        void setOnPick(java.util.function.Consumer<String> c) {
            this.onPick = c;
        }

        void setSelected(String name) {
            this.selected = name;
            repaint();
        }

        String selected() {
            return selected;
        }

        void setData(String title, java.util.Map<String, Double> data) {
            this.title = title;
            java.util.List<java.util.Map.Entry<String, Double>> es =
                    new java.util.ArrayList<java.util.Map.Entry<String, Double>>(data.entrySet());
            es.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
            names = new String[es.size()];
            values = new double[es.size()];
            total = 0;
            for (int i = 0; i < es.size(); i++) {
                names[i] = es.get(i).getKey();
                values[i] = es.get(i).getValue();
                total += values[i];
            }
            repaint();
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Insets in = getInsets();
            int w = getWidth() - in.left - in.right;
            int h = getHeight() - in.top - in.bottom;

            g2.setFont(Ui.font(12, true));
            g2.setColor(Theme.text());
            g2.drawString(title, in.left, in.top + 13);

            int top = in.top + 22;
            int areaH = h - 22;
            int d = Math.min(areaH - 6, 96);
            if (d < 40) {
                g2.dispose();
                return;
            }
            int r = d / 2;
            int cx = in.left + r + 4;
            int cy = top + areaH / 2;

            if (total <= 0) {
                g2.setColor(Theme.isDark() ? new Color(0x33384A) : new Color(0xE2E8F2));
                g2.setStroke(new BasicStroke(14));
                g2.drawOval(cx - r + 7, cy - r + 7, d - 14, d - 14);
                // 单行“暂无支出记录”有 66px，比内孔（约 65px）还宽，会压到圆环上像被截断，
                // 拆成两行小字放进去
                g2.setFont(Ui.font(10, false));
                g2.setColor(Theme.muted());
                String l1 = "暂无";
                String l2 = "支出记录";
                int w1 = g2.getFontMetrics().stringWidth(l1);
                int w2 = g2.getFontMetrics().stringWidth(l2);
                g2.drawString(l1, cx - w1 / 2, cy - 1);
                g2.drawString(l2, cx - w2 / 2, cy + 12);
                g2.dispose();
                return;
            }

            double start = 90;
            for (int i = 0; i < values.length; i++) {
                double sweep = -360.0 * values[i] / total;
                Color base = catColor(names[i]);
                if (selected != null && !names[i].equals(selected)) {
                    base = new Color(base.getRed(), base.getGreen(), base.getBlue(), 46);
                }
                g2.setColor(base);
                g2.fillArc(cx - r, cy - r, d, d,
                        (int) Math.round(start), (int) Math.round(sweep));
                start += sweep;
            }

            int hole = (int) (d * 0.68);
            g2.setColor(Theme.bg());
            g2.fillOval(cx - hole / 2, cy - hole / 2, hole, hole);

            g2.setFont(new Font("SansSerif", Font.BOLD, 11));
            g2.setColor(Theme.text());
            /* 筛选某一类时，环内改成该类的金额与占比 */
            double centerVal = total;
            String centerName = null;
            if (selected != null) {
                for (int i = 0; i < names.length; i++) {
                    if (names[i].equals(selected)) {
                        centerVal = values[i];
                        centerName = selected;
                        break;
                    }
                }
            }
            String amt = String.format(Locale.CHINA, "¥%.0f", centerVal);
            int aw = g2.getFontMetrics().stringWidth(amt);
            if (aw > hole - 8) {
                g2.setFont(new Font("SansSerif", Font.BOLD, 9));
                aw = g2.getFontMetrics().stringWidth(amt);
            }
            g2.drawString(amt, cx - aw / 2, cy + 3);
            g2.setFont(Ui.font(7, false));
            g2.setColor(Theme.muted());
            String sub = centerName != null
                    ? centerName + " " + Math.round(centerVal / total * 100) + "%"
                    : title.replace("支出分类", "").trim();
            if (sub.isEmpty()) sub = "支出";
            int sw = g2.getFontMetrics().stringWidth(sub);
            g2.drawString(sub, cx - sw / 2, cy + 14);

            int lx = cx + r + 18;
            if (w - (lx - in.left) < 96) {
                g2.dispose();
                return;
            }
            int rows = Math.min(values.length, Math.max(1, (areaH - 2) / 13));
            int ly = top + 1;
            int right = in.left + w;
            for (int i = 0; i < rows; i++) {
                boolean on = selected != null && names[i].equals(selected);
                g2.setColor(catColor(names[i]));
                g2.fillRoundRect(lx, ly + 2, 8, 8, 3, 3);
                g2.setFont(Ui.font(8, on));
                g2.setColor(Theme.text());
                g2.drawString(names[i], lx + 11, ly + 10);
                g2.setColor(Theme.muted());
                String amt2 = String.format(Locale.CHINA, "¥%.0f", values[i]);
                int aw2 = g2.getFontMetrics().stringWidth(amt2);
                g2.drawString(amt2, right - 24 - aw2, ly + 10);
                String pct = String.format(Locale.CHINA, "%.0f%%", values[i] / total * 100);
                int pw = g2.getFontMetrics().stringWidth(pct);
                g2.drawString(pct, right - pw, ly + 10);
                ly += 13;
            }
            if (values.length > rows) {
                g2.setFont(Ui.font(8, false));
                g2.setColor(Theme.muted());
                g2.drawString("另有 " + (values.length - rows) + " 类", lx, ly + 10);
            }
            g2.dispose();
        }
    }

    /** 让内容宽度跟随滚动视口，避免整体居中 */
    private static class ScrollHost extends JPanel implements Scrollable {
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 18;
        }

        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 18;
        }

        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    /** 近 6 个月支出趋势折线 */
    private static class MonthTrend extends JPanel {
        private String[] labels = new String[0];
        private double[] values = new double[0];
        private String title = "近 6 个月支出趋势（元）";

        MonthTrend() {
            setOpaque(false);
        }

        void setData(String[] labels, double[] values) {
            setData(labels, values, "近 6 个月支出趋势（元）");
        }

        /** 传入自定义标题：筛选某一分类时折线只画该类，标题也跟着变 */
        void setData(String[] labels, double[] values, String title) {
            this.labels = labels;
            this.values = values;
            this.title = title;
            repaint();
        }

        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g2 = (Graphics2D) g0.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            Insets in = getInsets();

            g2.setFont(Ui.font(12, true));
            g2.setColor(Theme.text());
            g2.drawString(title, in.left + 14, in.top + 18);

            double total = 0;
            for (double v : values) total += v;
            g2.setFont(Ui.font(11, false));
            g2.setColor(Theme.muted());
            String sumTxt = "合计 " + String.format(Locale.CHINA, "¥%.0f", total);
            int sw = g2.getFontMetrics().stringWidth(sumTxt);
            g2.drawString(sumTxt, w - in.right - 14 - sw, in.top + 18);

            if (labels.length == 0) {
                g2.dispose();
                return;
            }

            int left = in.left + 46, right = w - in.right - 18;
            int top = in.top + 36, bottom = h - in.bottom - 22;
            int plotH = bottom - top;
            double max = 1;
            for (double v : values) max = Math.max(max, v);

            g2.setFont(Ui.font(9, false));
            String lastLab = null;
            for (int i = 2; i >= 0; i--) {
                int y = top + plotH - (int) (plotH * i / 2.0);
                g2.setColor(Theme.isDark() ? new Color(0x2A2E3A) : new Color(0xEDF1F7));
                g2.drawLine(left, y, right, y);
                g2.setColor(Theme.muted());
                String lab = String.format(Locale.CHINA, "%.0f", max * i / 2);
                // max 只有 1 的时候 0.5 和 1 都会被格式化成 "1"，同一个刻度不重复标
                if (lab.equals(lastLab)) continue;
                lastLab = lab;
                int lw = g2.getFontMetrics().stringWidth(lab);
                g2.drawString(lab, left - 8 - lw, y + 4);
            }

            if (total <= 0) {
                g2.setFont(Ui.font(11, false));
                g2.setColor(Theme.muted());
                String empty = "近 6 个月还没有支出记录";
                int ew = g2.getFontMetrics().stringWidth(empty);
                g2.drawString(empty, left + (right - left - ew) / 2, top + plotH / 2);
                g2.dispose();
                return;
            }

            int n = labels.length;
            int[] xs = new int[n];
            int[] ys = new int[n];
            for (int i = 0; i < n; i++) {
                xs[i] = n == 1 ? (left + right) / 2 : left + (int) ((right - left) * i / (double) (n - 1));
                ys[i] = top + plotH - (int) (plotH * values[i] / max);
            }

            // 面积填充
            int[] px = new int[n + 2];
            int[] py = new int[n + 2];
            for (int i = 0; i < n; i++) {
                px[i] = xs[i];
                py[i] = ys[i];
            }
            px[n] = xs[n - 1];
            py[n] = bottom;
            px[n + 1] = xs[0];
            py[n + 1] = bottom;
            g2.setColor(new Color(249, 115, 22, 26));
            g2.fillPolygon(px, py, n + 2);

            // 折线
            g2.setStroke(new BasicStroke(2f));
            g2.setColor(new Color(0xF97316));
            for (int i = 0; i < n - 1; i++) g2.drawLine(xs[i], ys[i], xs[i + 1], ys[i + 1]);

            // 点与数值
            g2.setFont(Ui.font(9, true));
            for (int i = 0; i < n; i++) {
                g2.setColor(Theme.isDark() ? Color.WHITE : Color.WHITE);
                g2.fillOval(xs[i] - 4, ys[i] - 4, 8, 8);
                g2.setColor(new Color(0xF97316));
                g2.drawOval(xs[i] - 4, ys[i] - 4, 8, 8);
                String v = String.format(Locale.CHINA, "%.0f", values[i]);
                int vw = g2.getFontMetrics().stringWidth(v);
                g2.setColor(Theme.text());
                g2.drawString(v, xs[i] - vw / 2, ys[i] - 8);
            }

            // 月份轴
            g2.setFont(Ui.font(10, false));
            g2.setColor(Theme.muted());
            for (int i = 0; i < n; i++) {
                int lw = g2.getFontMetrics().stringWidth(labels[i]);
                int x = Math.max(in.left + 2, Math.min(w - in.right - 2 - lw, xs[i] - lw / 2));
                g2.drawString(labels[i], x, h - in.bottom - 6);
            }
            g2.dispose();
        }
    }

    /** 近 N 天柱状图（专注分钟 / 支出金额），可选上周对比柱 */
    private static class BarChart extends JPanel {
        private String title = "近 7 天专注时长（分钟）";
        private String[] labels = new String[0];
        private int[] values = new int[0];
        private int[] prev = null;
        private Color tint = null;
        private String legendNow = "本周";
        private String legendOld = "上周";
        private boolean showLegend = true;

        BarChart() {
            setOpaque(true);
            setBackground(Theme.bg());
            setPreferredSize(new Dimension(0, 112));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Theme.border(), 1),
                    BorderFactory.createEmptyBorder(10, 14, 8, 14)));
        }

        void setTint(Color c, String now, String old) {
            this.tint = c;
            if (now != null) legendNow = now;
            if (old != null) legendOld = old;
        }

        void setLegend(boolean on) {
            this.showLegend = on;
        }

        void setData(String title, String[] labels, int[] values, int[] prev) {
            this.title = title;
            this.labels = labels;
            this.values = values;
            this.prev = prev;
            repaint();
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Insets in = getInsets();
            int w = getWidth() - in.left - in.right;
            int h = getHeight() - in.top - in.bottom;

            g2.setFont(Ui.font(12, true));
            g2.setColor(Theme.text());
            g2.drawString(title, in.left, in.top + 13);

            int top = in.top + 22;
            int labelH = 18;
            int plotH = h - 22 - labelH;
            int n = values.length;
            if (plotH < 20 || n <= 0) {
                g2.dispose();
                return;
            }

            int max = 1;
            for (int v : values) max = Math.max(max, v);
            boolean dual = prev != null && prev.length == n;
            if (dual) {
                for (int v : prev) max = Math.max(max, v);
            }

            int slot = Math.max(1, w / n);
            if (dual) {
                if (showLegend) {
                    int lx = in.left + w - 78;
                    g2.setFont(Ui.font(9, false));
                    g2.setColor(tint == null ? Ui.PRIMARY : tint);
                    g2.fillRoundRect(lx, in.top + 6, 8, 8, 2, 2);
                    g2.setColor(Theme.muted());
                    g2.drawString(legendNow, lx + 12, in.top + 14);
                    int lx2 = lx + 44;
                    g2.setColor(Theme.isDark() ? new Color(0x4A5163) : new Color(0xC7D2E5));
                    g2.fillRoundRect(lx2, in.top + 6, 8, 8, 2, 2);
                    g2.setColor(Theme.muted());
                    g2.drawString(legendOld, lx2 + 12, in.top + 14);
                }

                int pairW = Math.min(64, Math.max(10, slot - 8));
                int bw = Math.max(3, (pairW - 3) / 2);
                int plotBottom = top + plotH - labelH;
                for (int i = 0; i < n; i++) {
                    int gx = in.left + i * slot + (slot - pairW) / 2;
                    int hNow = values[i] <= 0 ? 3 : Math.max(6, (int) ((plotH - 26) * values[i] / (double) max));
                    int hOld = prev[i] <= 0 ? 3 : Math.max(6, (int) ((plotH - 26) * prev[i] / (double) max));
                    int xa = gx;
                    int xb = gx + bw + 3;

                    if (values[i] <= 0) {
                        g2.setColor(Theme.isDark() ? new Color(0x3A3F4B) : new Color(0xE2E8F2));
                    } else if (tint == null) {
                        GradientPaint gp = new GradientPaint(xa, plotBottom - hNow, Ui.PRIMARY,
                                xa, plotBottom, Theme.isDark() ? new Color(0x7C8BFF) : new Color(0x7C4DFF));
                        g2.setPaint(gp);
                    } else {
                        GradientPaint gp = new GradientPaint(xa, plotBottom - hNow, tint,
                                xa, plotBottom, tint.brighter());
                        g2.setPaint(gp);
                    }
                    g2.fillRoundRect(xa, plotBottom - hNow, bw, hNow, 6, 6);

                    if (prev[i] <= 0) {
                        g2.setColor(Theme.isDark() ? new Color(0x3A3F4B) : new Color(0xE2E8F2));
                    } else {
                        g2.setColor(Theme.isDark() ? new Color(0x4A5163) : new Color(0xC7D2E5));
                    }
                    g2.fillRoundRect(xb, plotBottom - hOld, bw, hOld, 6, 6);

                    if (values[i] > 0) {
                        g2.setFont(Ui.font(10, true));
                        g2.setColor(Theme.text());
                        String v = String.valueOf(values[i]);
                        int tw = g2.getFontMetrics().stringWidth(v);
                        g2.drawString(v, gx + (pairW - tw) / 2, plotBottom - Math.max(hNow, 4) - 4);
                    }

                    String lb = labels[i] == null ? "" : labels[i];
                    if (!lb.isEmpty()) {
                        g2.setFont(Ui.font(10, false));
                        g2.setColor(Theme.muted());
                        int lw = g2.getFontMetrics().stringWidth(lb);
                        g2.drawString(lb, gx + (pairW - lw) / 2, top + plotH - 4);
                    }
                }
                g2.dispose();
                return;
            }
            int barW = Math.max(4, Math.min(n <= 7 ? 44 : 18, slot - 3));
            for (int i = 0; i < n; i++) {
                int x = in.left + i * slot + (slot - barW) / 2;
                int barH = values[i] <= 0 ? 4 : Math.max(6, (int) ((plotH - 16) * values[i] / (double) max));
                int y = top + plotH - labelH - barH;

                if (values[i] <= 0) {
                    g2.setColor(Theme.isDark() ? new Color(0x3A3F4B) : new Color(0xE2E8F2));
                    g2.fillRoundRect(x, top + plotH - labelH - 4, barW, 4, 4, 4);
                } else {
                    GradientPaint gp = tint == null
                            ? new GradientPaint(x, y, Ui.PRIMARY, x, y + barH,
                                    Theme.isDark() ? new Color(0x7C8BFF) : new Color(0x7C4DFF))
                            : new GradientPaint(x, y, tint, x, y + barH, tint.brighter());
                    g2.setPaint(gp);
                    g2.fillRoundRect(x, y, barW, barH, 8, 8);

                    if (n <= 7) {
                        g2.setFont(Ui.font(11, true));
                        g2.setColor(Theme.text());
                        String v = String.valueOf(values[i]);
                        int tw = g2.getFontMetrics().stringWidth(v);
                        g2.drawString(v, x + (barW - tw) / 2, y - 5);
                    }
                }

                String lb = labels[i] == null ? "" : labels[i];
                if (!lb.isEmpty()) {
                    g2.setFont(Ui.font(10, false));
                    g2.setColor(Theme.muted());
                    int lw = g2.getFontMetrics().stringWidth(lb);
                    g2.drawString(lb, x + (barW - lw) / 2, top + plotH - 4);
                }
            }
            g2.dispose();
        }
    }

    /** 任务专注排行：按任务名汇总区间内的专注分钟 */
    private static class TaskRank extends JPanel {
        private String[] names = new String[0];
        private int[] mins = new int[0];

        TaskRank() {
            setOpaque(true);
            setBackground(Theme.bg());
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Theme.border(), 1),
                    BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        }

        void setData(java.util.Map<String, Integer> data) {
            java.util.List<java.util.Map.Entry<String, Integer>> es =
                    new java.util.ArrayList<java.util.Map.Entry<String, Integer>>(data.entrySet());
            es.sort((a, b) -> b.getValue() - a.getValue());
            int n = Math.min(5, es.size());
            names = new String[n];
            mins = new int[n];
            for (int i = 0; i < n; i++) {
                names[i] = es.get(i).getKey();
                mins[i] = es.get(i).getValue();
            }
            repaint();
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Insets in = getInsets();
            int w = getWidth() - in.left - in.right;

            g2.setFont(Ui.font(12, true));
            g2.setColor(Theme.text());
            g2.drawString("任务专注排行", in.left, in.top + 13);

            int top = in.top + 32;
            if (names.length == 0) {
                g2.setFont(Ui.font(11, false));
                g2.setColor(Theme.muted());
                g2.drawString("还没有带任务名的记录", in.left, top + 14);
                g2.dispose();
                return;
            }

            int max = 1;
            for (int m : mins) max = Math.max(max, m);
            int rowH = 34;
            for (int i = 0; i < names.length; i++) {
                int y = top + i * rowH;

                g2.setFont(Ui.font(11, false));
                g2.setColor(Theme.text());
                java.awt.FontMetrics fm = g2.getFontMetrics();
                String shown = names[i];
                while (shown.length() > 1 && fm.stringWidth(shown + "…") > w - 58) {
                    shown = shown.substring(0, shown.length() - 1);
                }
                if (!shown.equals(names[i])) shown = shown + "…";
                g2.drawString(shown, in.left + 2, y + 12);

                g2.setFont(Ui.font(10, true));
                g2.setColor(Ui.PRIMARY);
                String m = mins[i] + " 分";
                int mw = g2.getFontMetrics().stringWidth(m);
                g2.drawString(m, in.left + w - mw, y + 12);

                g2.setColor(Theme.isDark() ? new Color(0x2B3350) : new Color(0xEEF2FF));
                g2.fillRoundRect(in.left + 2, y + 18, Math.max(4, w - 8), 5, 5, 5);
                g2.setColor(Ui.PRIMARY);
                g2.fillRoundRect(in.left + 2, y + 18,
                        Math.max(6, (int) ((w - 8) * (mins[i] / (double) max))), 5, 5, 5);
            }
            g2.dispose();
        }
    }

    private JPanel numCard(String title, JLabel value, Color color) {
        return numCard(title, value, color, null);
    }

    /** 番茄数字卡点一下：切到番茄钟页并把「番茄记录」停到对应视图（today / week / sum） */
    private void linkPomodoro(JPanel card, final String scope, String tip) {
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.setToolTipText(tip);
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (!javax.swing.SwingUtilities.isLeftMouseButton(e)) return;
                PomodoroPanel pp = Main.page(PomodoroPanel.class);
                if (pp == null) return;
                Main.go("pomodoro");
                pp.openHistory(scope);
            }
        });
    }

    private JPanel numCard(String title, JLabel value, Color color, JLabel badge) {
        JPanel p = new JPanel(new BorderLayout(12, 0));
        p.setOpaque(true);
        p.setBackground(Theme.bg());
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.border(), 1),
                BorderFactory.createEmptyBorder(9, 14, 9, 12)));

        JPanel bar = new JPanel();
        bar.setBackground(color);
        bar.setPreferredSize(new Dimension(4, 0));
        p.add(bar, BorderLayout.WEST);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        value.setHorizontalAlignment(SwingConstants.LEFT);
        value.setFont(new Font("SansSerif", Font.BOLD, 24));
        value.setForeground(color);
        value.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel t = new JLabel(title);
        t.setFont(Ui.font(11, false));
        t.setForeground(Theme.muted());
        t.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel titleRow = new JPanel();
        titleRow.setOpaque(false);
        titleRow.setLayout(new BoxLayout(titleRow, BoxLayout.X_AXIS));
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleRow.add(t);
        titleRow.add(Box.createHorizontalGlue());

        text.add(value);
        text.add(Box.createVerticalStrut(2));
        text.add(titleRow);
        /* 副标题（badge）单独一行：跟标题挤一行时，卡片一窄后半截就被裁掉 */
        if (badge != null) {
            badge.setAlignmentX(Component.LEFT_ALIGNMENT);
            text.add(Box.createVerticalStrut(1));
            text.add(badge);
        }
        p.add(text, BorderLayout.CENTER);
        return p;
    }

    private JPanel recordTable(String title, DefaultTableModel model, int... widths) {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setOpaque(true);
        p.setBackground(Theme.bg());
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.border(), 1),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        JLabel t = new JLabel(title);
        t.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 14));
        t.setForeground(Theme.text());
        p.add(t, BorderLayout.NORTH);
        JTable table = new JTable(model);
        table.setRowHeight(32);
        table.setFillsViewportHeight(true);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFont(Ui.font(12, false));
        table.getTableHeader().setFont(Ui.font(12, true));
        table.getTableHeader().setReorderingAllowed(false);
        for (int i = 0; i < widths.length && i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(Theme.border()));
        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    /** 数据备份卡：份数 + 最近一次 + 数据目录占用，点开进备份管理 */
    private void refreshBackupCard() {
        if (backupValueLabel == null) return;
        List<String> bs = Backup.list();
        backupValueLabel.setText(bs.size() + " 份");
        String size = dataSizeText();
        if (bs.isEmpty()) {
            backupSubLabel.setText("还没有快照");
        } else {
            backupSubLabel.setText("最近 " + shortDay(bs.get(0)));
        }
        if (backupCard != null) {
            backupCard.setToolTipText("<html>现在有 " + bs.size() + " 份快照，备份范围：" + Backup.pickText()
                    + "<br>数据目录 " + Store.dir().getName() + " · " + size
                    + "<br>点一下：打开备份管理，能立即备份 / 回滚到某一天</html>");
        }
    }

    /** 备份目录名太长（2026-11-23-171521），卡片上只放月-日 */
    static String shortDay(String name) {
        if (name == null) return "";
        String d = name.startsWith("pre-restore-") ? name.substring("pre-restore-".length()) : name;
        if (d.length() >= 10 && d.charAt(4) == '-' && d.charAt(7) == '-') return d.substring(5, 10);
        return d;
    }

    /** 数据目录里 *.txt 一共多大 */
    public static String dataSizeText() {
        long bytes = 0;
        File[] fs = Store.dir().listFiles();
        if (fs != null) {
            for (File f : fs) {
                if (f.isFile() && f.getName().endsWith(".txt")) bytes += f.length();
            }
        }
        if (bytes < 1024) return bytes + " B";
        return String.format(java.util.Locale.CHINA, "%.1f KB", bytes / 1024.0);
    }

    /** 切到统计页时刷新 */
    public void refresh() {
        styleRange();
        refreshBackupCard();
        String today = DAY.format(new Date());

        int focusTotalMin = 0, focusCount = 0, todayFocus = 0;
        for (String line : Store.readLines("pomodoro_log.txt")) {
            String[] p = line.split("\t", -1);
            if (p.length < 4 || !p[3].equals("专注")) continue;
            int min = 0;
            try {
                min = Integer.parseInt(p[2].trim());
            } catch (Exception ignored) {
            }
            focusTotalMin += min;
            focusCount++;
            if (p[1] != null && p[1].startsWith(today)) todayFocus++;
        }
        focusTotalLabel.setText(fmtMin(focusTotalMin));
        focusCountLabel.setText(focusCount + " 个");
        todayCountLabel.setText(todayFocus + " 个");

        // 近 N 天柱状图
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.DAY_OF_YEAR, -(range - 1));
        SimpleDateFormat shortFmt = new SimpleDateFormat("MM-dd", Locale.CHINA);
        SimpleDateFormat dayOnly = new SimpleDateFormat("d", Locale.CHINA);
        String[] labels = new String[range];
        int[] values = new int[range];
        int[] prevs = new int[range];
        java.util.List<String> plog = Store.readLines("pomodoro_log.txt");
        for (int i = 0; i < range; i++) {
            String d = DAY.format(cal.getTime());
            if (i == range - 1) labels[i] = "今天";
            else if (range <= 7) labels[i] = shortFmt.format(cal.getTime());
            else labels[i] = (i % 5 == 0) ? dayOnly.format(cal.getTime()) : "";
            values[i] = focusOn(plog, d);
            if (range <= 7) {
                java.util.Calendar pre = (java.util.Calendar) cal.clone();
                pre.add(java.util.Calendar.DAY_OF_YEAR, -7);
                prevs[i] = focusOn(plog, DAY.format(pre.getTime()));
            }
            cal.add(java.util.Calendar.DAY_OF_YEAR, 1);
        }
        chart.setData(range <= 7 ? "近 7 天专注时长（分钟）" : "近 30 天专注时长（分钟）",
                labels, values, range <= 7 ? prevs : null);

        // 近 N 天支出柱（含上周同日对比）
        java.util.List<String> ledger = Store.readLines("ledger.txt");
        int[] spend = new int[range];
        int[] spendPrev = new int[range];
        java.util.Calendar cal2 = java.util.Calendar.getInstance();
        cal2.add(java.util.Calendar.DAY_OF_YEAR, -(range - 1));
        String[] expLabels = new String[range];
        for (int i = 0; i < range; i++) {
            String d = DAY.format(cal2.getTime());
            expLabels[i] = (i == range - 1) ? "今" : (range <= 7 ? dayOnly.format(cal2.getTime()) : "");
            spend[i] = spendFilter == null ? spendOn(ledger, d.substring(5))
                    : spendOnCat(ledger, d.substring(5), spendFilter);
            if (range <= 7) {
                java.util.Calendar pre = (java.util.Calendar) cal2.clone();
                pre.add(java.util.Calendar.DAY_OF_YEAR, -7);
                String pmd = DAY.format(pre.getTime()).substring(5);
                spendPrev[i] = spendFilter == null ? spendOn(ledger, pmd)
                        : spendOnCat(ledger, pmd, spendFilter);
            }
            cal2.add(java.util.Calendar.DAY_OF_YEAR, 1);
        }
        String expTitle = (spendFilter == null ? "" : spendFilter + " · ")
                + (range <= 7 ? "近 7 天支出（元）" : "近 30 天支出（元）");
        expChart.setData(expTitle, expLabels, spend, range <= 7 ? spendPrev : null);

        int cdCount = 0, cdSec = 0;
        for (String line : Store.readLines("countdown_log.txt")) {
            String[] p = line.split("\t", -1);
            if (p.length < 3) continue;
            try {
                cdSec += Integer.parseInt(p[2].trim());
            } catch (Exception ignored) {
            }
            cdCount++;
        }
        cdCountLabel.setText(cdCount + " 次");

        double in = 0, out = 0;
        for (String line : Store.readLines("ledger.txt")) {
            String[] p = line.split("\t", -1);
            int type = p.length > 0 && p[0].equals("1") ? 1 : 0;
            double amt = 0;
            try {
                amt = Double.parseDouble(p[1].trim());
            } catch (Exception ignored) {
            }
            if (type == 1) in += amt;
            else out += amt;
        }
        balanceLabel.setText(String.format(Locale.CHINA, "¥%.2f", in - out));

        // 近 N 天支出分类（环形图）
        java.util.Set<String> days = new java.util.HashSet<String>();
        java.util.Calendar dc = java.util.Calendar.getInstance();
        dc.add(java.util.Calendar.DAY_OF_YEAR, -(range - 1));
        SimpleDateFormat mdFmt = new SimpleDateFormat("MM-dd", Locale.CHINA);
        for (int i = 0; i < range; i++) {
            days.add(mdFmt.format(dc.getTime()));
            dc.add(java.util.Calendar.DAY_OF_YEAR, 1);
        }
        java.util.Map<String, Double> cats = new java.util.LinkedHashMap<String, Double>();
        for (String line : Store.readLines("ledger.txt")) {
            String[] p = line.split("\t", -1);
            if (p.length < 4) continue;
            if ("1".equals(p[0].trim())) continue;
            String t = p[3].trim();
            if (t.length() < 5 || !days.contains(t.substring(0, 5))) continue;
            double amt = 0;
            try {
                amt = Double.parseDouble(p[1].trim());
            } catch (Exception ignored) {
            }
            String cat = p.length > 4 && !p[4].trim().isEmpty() ? p[4].trim() : "未分类";
            Double v = cats.get(cat);
            cats.put(cat, (v == null ? 0 : v) + amt);
        }
        donut.setData(range <= 7 ? "近 7 天支出分类" : "近 30 天支出分类", cats);
        if (spendFilter != null && !cats.containsKey(spendFilter)) spendFilter = null;
        donut.setSelected(spendFilter);

        /* 近 6 个月支出趋势：ledger 只存 MM-dd，同月号跨年无法区分，按最近一年计 */
        String[] mLabel = new String[6];
        double[] mVal = new double[6];
        int[] mNum = new int[6];
        java.util.Calendar mc = java.util.Calendar.getInstance();
        mc.add(java.util.Calendar.MONTH, -5);
        for (int i = 0; i < 6; i++) {
            mNum[i] = mc.get(java.util.Calendar.MONTH) + 1;
            mLabel[i] = mNum[i] + "月";
            mc.add(java.util.Calendar.MONTH, 1);
        }
        for (String line : Store.readLines("ledger.txt")) {
            String[] p = line.split("\t", -1);
            if (p.length < 4 || "1".equals(p[0].trim())) continue;
            /* 统计页点了某一类时，折线只画这一类的支出 */
            if (spendFilter != null) {
                String c = p.length > 4 && !p[4].trim().isEmpty() ? p[4].trim() : "未分类";
                if (!c.equals(spendFilter)) continue;
            }
            String t = p[3].trim();
            if (t.length() < 2) continue;
            int mm;
            try {
                mm = Integer.parseInt(t.substring(0, 2));
            } catch (Exception ignored) {
                continue;
            }
            double amt = 0;
            try {
                amt = Double.parseDouble(p[1].trim());
            } catch (Exception ignored) {
            }
            for (int i = 0; i < 6; i++) {
                if (mNum[i] == mm) {
                    mVal[i] += amt;
                    break;
                }
            }
        }
        trend.setData(mLabel, mVal, spendFilter == null
                ? "近 6 个月支出趋势（元）"
                : "近 6 个月 · " + spendFilter + " 支出趋势（元）");

        // 本周至今 vs 上周同期
        java.util.Calendar wk = java.util.Calendar.getInstance();
        int iso = wk.get(java.util.Calendar.DAY_OF_WEEK);
        int back = iso == java.util.Calendar.SUNDAY ? 6 : iso - 2;
        int daysSoFar = back + 1;
        wk.add(java.util.Calendar.DAY_OF_YEAR, -back);
        java.util.Set<String> thisWeek = new java.util.HashSet<String>();
        java.util.Calendar c1 = (java.util.Calendar) wk.clone();
        for (int i = 0; i < daysSoFar; i++) {
            thisWeek.add(mdFmt.format(c1.getTime()));
            c1.add(java.util.Calendar.DAY_OF_YEAR, 1);
        }
        java.util.Calendar c0 = (java.util.Calendar) wk.clone();
        c0.add(java.util.Calendar.DAY_OF_YEAR, -7);
        java.util.Set<String> lastWeek = new java.util.HashSet<String>();
        for (int i = 0; i < daysSoFar; i++) {
            lastWeek.add(mdFmt.format(c0.getTime()));
            c0.add(java.util.Calendar.DAY_OF_YEAR, 1);
        }

        // 本月 1 日至今 vs 上月 1 日至同一天（跨年数据只存 MM-dd，按月份区分）
        java.util.Set<String> thisMonth = new java.util.HashSet<String>();
        java.util.Set<String> lastMonth = new java.util.HashSet<String>();
        java.util.Calendar cm = java.util.Calendar.getInstance();
        int dom = cm.get(java.util.Calendar.DAY_OF_MONTH);
        for (int i = 1; i <= dom; i++) {
            java.util.Calendar ci = (java.util.Calendar) cm.clone();
            ci.set(java.util.Calendar.DAY_OF_MONTH, i);
            thisMonth.add(mdFmt.format(ci.getTime()));
        }
        java.util.Calendar cmPrev = java.util.Calendar.getInstance();
        cmPrev.add(java.util.Calendar.MONTH, -1);
        int prevMax = cmPrev.getActualMaximum(java.util.Calendar.DAY_OF_MONTH);
        int prevDays = Math.min(dom, prevMax);
        for (int i = 1; i <= prevDays; i++) {
            java.util.Calendar ci = (java.util.Calendar) cmPrev.clone();
            ci.set(java.util.Calendar.DAY_OF_MONTH, i);
            lastMonth.add(mdFmt.format(ci.getTime()));
        }

        int wf = 0, pf = 0, mf = 0, mfPrev = 0;
        for (String line : Store.readLines("pomodoro_log.txt")) {
            String[] p = line.split("\t", -1);
            if (p.length < 4 || !p[3].equals("专注")) continue;
            String t = p[1].trim();
            if (t.length() < 10) continue;
            String mmdd = t.substring(5, 10);
            int min = 0;
            try {
                min = Integer.parseInt(p[2].trim());
            } catch (Exception ignored) {
            }
            if (thisWeek.contains(mmdd)) wf += min;
            else if (lastWeek.contains(mmdd)) pf += min;
            if (thisMonth.contains(mmdd)) mf += min;
            else if (lastMonth.contains(mmdd)) mfPrev += min;
        }
        double wo = 0, po = 0, mo = 0, moPrev = 0;
        for (String line : Store.readLines("ledger.txt")) {
            String[] p = line.split("\t", -1);
            if (p.length < 4 || "1".equals(p[0].trim())) continue;
            String t = p[3].trim();
            if (t.length() < 5) continue;
            String mmdd = t.substring(0, 5);
            double amt = 0;
            try {
                amt = Double.parseDouble(p[1].trim());
            } catch (Exception ignored) {
            }
            if (thisWeek.contains(mmdd)) wo += amt;
            else if (lastWeek.contains(mmdd)) po += amt;
            if (thisMonth.contains(mmdd)) mo += amt;
            else if (lastMonth.contains(mmdd)) moPrev += amt;
        }
        weekInfo.setText("<html><span style='color:#8A94A6'>本周专注</span> <b>" + wf + " 分</b> "
                + colorDelta(wf, pf, "上周") + " &nbsp;·&nbsp; <span style='color:#8A94A6'>本周支出</span> <b>"
                + String.format(Locale.CHINA, "¥%.2f", wo) + "</b> " + colorDelta(wo, po, "上周")
                + "<br><span style='color:#8A94A6'>本月专注</span> <b>" + mf + " 分</b> "
                + colorDelta(mf, mfPrev, "上月") + " &nbsp;·&nbsp; <span style='color:#8A94A6'>本月支出</span> <b>"
                + String.format(Locale.CHINA, "¥%.2f", mo) + "</b> " + colorDelta(mo, moPrev, "上月")
                + filterLine(ledger) + "</html>");

        // 任务专注排行（同一区间）
        java.util.Map<String, Integer> taskMin = new java.util.LinkedHashMap<String, Integer>();
        for (String line : Store.readLines("pomodoro_log.txt")) {
            String[] p = line.split("\t", -1);
            if (p.length < 5 || !p[3].equals("专注")) continue;
            String task = p[4].trim();
            if (task.isEmpty()) continue;
            String t = p[1].trim();
            if (t.length() < 16 || !days.contains(t.substring(5, 10))) continue;
            int min = 0;
            try {
                min = Integer.parseInt(p[2].trim());
            } catch (Exception ignored) {
            }
            Integer v = taskMin.get(task);
            taskMin.put(task, (v == null ? 0 : v) + min);
        }
        taskRank.setData(taskMin);

        int memoCount = 0;
        for (String line : Store.readLines("memo.txt")) {
            if (!line.trim().isEmpty()) memoCount++;
        }
        memoCountLabel.setText(memoCount + " 条");

        /* 待办完成：本周 / 本月（不含归档条目和无日期条目） */
        java.time.LocalDate now = java.time.LocalDate.now();
        java.time.LocalDate mon = now.with(java.time.DayOfWeek.MONDAY);
        int todoWeek = 0, todoMonth = 0, todoLastWeek = 0, todo7 = 0, todoToday = 0;
        int[] weekCounts = new int[13];                 // 本周 / 上周 / 上上周……各完成几条
        for (String line : Store.readLines("todo.txt")) {
            String[] p = line.split("\t", -1);
            if (p.length < 3 || !"1".equals(p[0].trim())) continue;
            if (p[1].endsWith("（已归档）")) continue;
            String d = p[2].trim();
            if (d.isEmpty() || "-".equals(d)) continue;
            java.time.LocalDate day;
            try {
                day = java.time.LocalDate.parse(d);
            } catch (Exception e) {
                continue;
            }
            if (day.getYear() == now.getYear() && day.getMonthValue() == now.getMonthValue()) todoMonth++;
            if (!day.isBefore(mon) && !day.isAfter(mon.plusDays(6))) todoWeek++;
            if (!day.isBefore(mon.minusDays(7)) && !day.isAfter(mon.minusDays(1))) todoLastWeek++;
            if (!day.isBefore(now.minusDays(6)) && !day.isAfter(now)) todo7++;
            if (day.equals(now)) todoToday++;
            java.time.LocalDate dm = day.with(java.time.DayOfWeek.MONDAY);
            int weekBack = (int) java.time.temporal.ChronoUnit.WEEKS.between(dm, mon);
            if (weekBack >= 0 && weekBack < weekCounts.length) weekCounts[weekBack]++;
        }
        todoWeekLabel.setText(todoWeek + " 条");
        todoMonthLabel.setText(todoMonth + " 条");

        /* 角标：跟概览同一套口径 —— 今天几条、连续几周上涨（上周空白就不算） */
        if (todoWeekBadge != null) {
            todoWeekBadge.setText(todoToday <= 0 ? "今天还没勾" : "今天 " + todoToday + " 条");
            todoWeekBadge.setForeground(todoToday <= 0 ? Theme.muted() : Ui.PRIMARY);
        }
        int streak = 0, cur = weekCounts[0];
        for (int bk = 1; bk < weekCounts.length; bk++) {
            int prev = weekCounts[bk];
            if (prev <= 0) break;
            if (cur <= prev) break;
            streak++;
            cur = prev;
        }
        if (todoMonthBadge != null) {
            if (streak >= 2) {
                todoMonthBadge.setText("连 " + streak + " 周↑");
                todoMonthBadge.setForeground(Ui.PRIMARY);
                todoMonthBadge.setToolTipText("已经连续 " + streak + " 周比上一周完成得多了");
            } else {
                todoMonthBadge.setText("近 7 天 " + todo7 + " 条");
                todoMonthBadge.setForeground(Theme.muted());
                todoMonthBadge.setToolTipText("");
            }
        }
        /* 右上角灰色小字补一行待办节奏：本周 / 上周 / 近 7 天 */
        String extra = "<br><span style='color:#8A94A6'>待办完成</span> <b>本周 " + todoWeek
                + " 条</b> · 上周 " + todoLastWeek + " 条 · 近 7 天 " + todo7 + " 条";
        String html = weekInfo.getText();
        if (html != null && html.endsWith("</html>")) {
            weekInfo.setText(html.substring(0, html.length() - "</html>".length()) + extra + "</html>");
        }

        pomodoroModel.setRowCount(0);
        List<String> pl = Store.readLines("pomodoro_log.txt");
        for (int i = pl.size() - 1; i >= 0 && pomodoroModel.getRowCount() < 15; i--) {
            String[] p = pl.get(i).split("\t", -1);
            if (p.length >= 4) {
                String when = p[1].trim();
                if (when.length() >= 16) when = when.substring(5); // yyyy-MM-dd HH:mm → MM-dd HH:mm
                pomodoroModel.addRow(new Object[]{when, p[2] + " 分钟", p[3]});
            }
        }

        countdownModel.setRowCount(0);
        List<String> cl = Store.readLines("countdown_log.txt");
        for (int i = cl.size() - 1; i >= 0 && countdownModel.getRowCount() < 15; i--) {
            String[] p = cl.get(i).split("\t", -1);
            if (p.length >= 3) {
                long s = 0;
                try {
                    s = Long.parseLong(p[2].trim());
                } catch (Exception ignored) {
                }
                String when = p[1].trim();
                if (when.length() >= 16) when = when.substring(5);
                countdownModel.addRow(new Object[]{when, fmtClock(s)});
            }
        }

        updateFocusSummary(pl);
    }

    /**
     * 专注小结的纯计算部分：本月哪天最专注、最长连续了几天。
     * 返回 {最专注的日期(yyyy-MM-dd，没有就是空串), 那天分钟, 那天个数, 最长连续天数,
     * 连续段起, 连续段止, 今天几个}，顺序固定，方便断言。
     */
    public static String[] focusSummaryOf(List<String> plog, java.time.LocalDate ref) {
        java.util.Map<String, Integer> dayMin = new java.util.TreeMap<String, Integer>();
        java.util.Map<String, Integer> dayCnt = new java.util.TreeMap<String, Integer>();
        for (String line : plog) {
            String[] p = line.split("\t", -1);
            if (p.length < 4 || !"专注".equals(p[3].trim())) continue;
            String t = p[1].trim();
            if (t.length() < 10) continue;
            String day = t.substring(0, 10);
            int min = 0;
            try {
                min = Integer.parseInt(p[2].trim());
            } catch (Exception ignored) {
            }
            Integer a = dayMin.get(day);
            dayMin.put(day, (a == null ? 0 : a) + min);
            Integer b = dayCnt.get(day);
            dayCnt.put(day, (b == null ? 0 : b) + 1);
        }

        /* 本月最专注：只在本月内比，分钟最多的那天（并列取更早的） */
        String bestDay = "";
        int bestMin = -1, bestCnt = 0;
        String ym = String.format(Locale.CHINA, "%04d-%02d", ref.getYear(), ref.getMonthValue());
        for (java.util.Map.Entry<String, Integer> e : dayMin.entrySet()) {
            if (!e.getKey().startsWith(ym)) continue;
            if (e.getValue() > bestMin) {
                bestMin = e.getValue();
                bestDay = e.getKey();
                bestCnt = dayCnt.get(e.getKey()) == null ? 0 : dayCnt.get(e.getKey());
            }
        }
        if (bestMin < 0) bestMin = 0;

        /* 最长连续：有专注记录的日子连起来最长的一段 */
        java.util.List<java.time.LocalDate> days = new java.util.ArrayList<java.time.LocalDate>();
        for (String d : dayMin.keySet()) {
            try {
                days.add(java.time.LocalDate.parse(d));
            } catch (Exception ignored) {
            }
        }
        java.util.Collections.sort(days);
        int bestLen = 0;
        String from = "", to = "";
        int i = 0;
        while (i < days.size()) {
            int j = i;
            while (j + 1 < days.size() && days.get(j + 1).equals(days.get(j).plusDays(1))) j++;
            int len = j - i + 1;
            if (len > bestLen) {
                bestLen = len;
                from = days.get(i).toString();
                to = days.get(j).toString();
            }
            i = j + 1;
        }

        int todayCnt = dayCnt.get(ref.toString()) == null ? 0 : dayCnt.get(ref.toString());
        return new String[]{bestDay, String.valueOf(bestMin), String.valueOf(bestCnt),
                String.valueOf(bestLen), from, to, String.valueOf(todayCnt)};
    }

    private void updateFocusSummary(List<String> pl) {
        String[] r = focusSummaryOf(pl, java.time.LocalDate.now());
        sumBestDay = r[0];
        sumBestMin = Integer.parseInt(r[1]);
        sumBestCnt = Integer.parseInt(r[2]);
        sumStreak = Integer.parseInt(r[3]);
        sumStreakFrom = r[4];
        sumStreakTo = r[5];
        int todayCnt = Integer.parseInt(r[6]);

        if (sumBestDay.isEmpty() && sumStreak <= 0) {
            focusSummary.setText("还没有专注记录 · 去番茄钟开一轮，这里就会自动小结");
            return;
        }
        StringBuilder sb = new StringBuilder("<html>");
        if (!sumBestDay.isEmpty()) {
            sb.append("<span style='color:#8A94A6'>本月最专注</span> <b>")
                    .append(mmdd(sumBestDay)).append("</b> · ")
                    .append(sumBestMin).append(" 分（").append(sumBestCnt).append(" 个番茄）");
        } else {
            sb.append("<span style='color:#8A94A6'>本月还没专注过</span>");
        }
        if (sumStreak >= 2) {
            sb.append("&nbsp;&nbsp;&nbsp;<span style='color:#8A94A6'>最长连续</span> <b>")
                    .append(sumStreak).append(" 天</b>（")
                    .append(mmdd(sumStreakFrom)).append(" ~ ").append(mmdd(sumStreakTo)).append("）");
        }
        sb.append("&nbsp;&nbsp;&nbsp;<span style='color:#8A94A6'>今天</span> ").append(todayCnt).append(" 个");
        sb.append("</html>");
        focusSummary.setText(sb.toString());
    }

    private static String mmdd(String isoDay) {
        if (isoDay == null || isoDay.length() < 10) return isoDay;
        return isoDay.substring(5);
    }

    /** 点小结条：列出本月最专注那天的每条专注，再列最长连续段每天的分钟数 */
    void showFocusDetail() {
        JPanel box = buildFocusDetailBox();
        if (sumBestDay.isEmpty()) {
            JOptionPane.showMessageDialog(this, box, "专注小结", JOptionPane.PLAIN_MESSAGE);
            return;
        }
        Object[] opts = {"复制这一天", "用这个任务再专注一轮", "关闭"};
        int pick = JOptionPane.showOptionDialog(this, box, "专注小结", JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE, null, opts, opts[2]);
        if (pick == 0) copyBestDay();
        else if (pick == 1) resumeTaskOfBestDay();
    }

    /** 小结明细的内容区（单独拎出来，方便截图与测试核对） */
    public JPanel buildFocusDetailBox() {
        List<String> pl = Store.readLines("pomodoro_log.txt");
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setOpaque(false);
        box.setBorder(BorderFactory.createEmptyBorder(4, 6, 2, 16));

        JLabel h1 = new JLabel(sumBestDay.isEmpty()
                ? "本月还没有专注记录"
                : "本月最专注 · " + mmdd(sumBestDay) + " 共 " + sumBestMin + " 分（" + sumBestCnt + " 个）");
        h1.setFont(Ui.font(12, true));
        h1.setForeground(new Color(0x1D4ED8));
        box.add(h1);
        if (!sumBestDay.isEmpty()) {
            for (String line : pl) {
                String[] p = line.split("\t", -1);
                if (p.length < 4 || !"专注".equals(p[3].trim())) continue;
                String t = p[1].trim();
                if (t.length() < 16 || !t.startsWith(sumBestDay)) continue;
                String task = p.length > 4 ? p[4].trim() : "";
                JLabel l = new JLabel(t.substring(11) + "  " + p[2].trim() + " 分钟"
                        + (task.isEmpty() ? "" : "  · " + task));
                l.setFont(Ui.font(11, false));
                box.add(l);
            }
        }
        if (sumStreak >= 2) {
            box.add(Box.createVerticalStrut(6));
            JLabel h2 = new JLabel("最长连续 " + sumStreak + " 天（" + mmdd(sumStreakFrom) + " ~ " + mmdd(sumStreakTo) + "）");
            h2.setFont(Ui.font(12, true));
            h2.setForeground(new Color(0x0EA5E9));
            box.add(h2);
            java.util.Map<String, Integer> dayMin = new java.util.TreeMap<String, Integer>();
            for (String line : pl) {
                String[] p = line.split("\t", -1);
                if (p.length < 4 || !"专注".equals(p[3].trim())) continue;
                String t = p[1].trim();
                if (t.length() < 10) continue;
                int min = 0;
                try {
                    min = Integer.parseInt(p[2].trim());
                } catch (Exception ignored) {
                }
                String d = t.substring(0, 10);
                Integer v = dayMin.get(d);
                dayMin.put(d, (v == null ? 0 : v) + min);
            }
            for (java.util.Map.Entry<String, Integer> e : dayMin.entrySet()) {
                if (e.getKey().compareTo(sumStreakFrom) < 0 || e.getKey().compareTo(sumStreakTo) > 0) continue;
                JLabel l = new JLabel(mmdd(e.getKey()) + "  " + e.getValue() + " 分钟");
                l.setFont(Ui.font(11, false));
                box.add(l);
            }
        }
        return box;
    }

    /** 本月最专注那天的任务名里出现最多的一个（没有就空串） */
    public String topTaskOf(String day) {
        java.util.Map<String, Integer> cnt = new java.util.LinkedHashMap<String, Integer>();
        for (String line : Store.readLines("pomodoro_log.txt")) {
            String[] p = line.split("\t", -1);
            if (p.length < 5 || !"专注".equals(p[3].trim())) continue;
            String t = p[1].trim();
            if (t.length() < 16 || !t.startsWith(day)) continue;
            String task = p[4].trim();
            if (task.isEmpty()) continue;
            Integer v = cnt.get(task);
            cnt.put(task, (v == null ? 0 : v) + 1);
        }
        String best = "";
        int bestN = 0;
        for (java.util.Map.Entry<String, Integer> e : cnt.entrySet()) {
            if (e.getValue() > bestN) {
                bestN = e.getValue();
                best = e.getKey();
            }
        }
        return best;
    }

    /** 把那天的专注逐条拼成文本放进系统剪贴板 */
    public void copyBestDay() {
        StringBuilder sb = new StringBuilder();
        sb.append("专注记录 ").append(sumBestDay).append("（").append(sumBestMin).append(" 分 · ")
                .append(sumBestCnt).append(" 个）\n");
        for (String line : Store.readLines("pomodoro_log.txt")) {
            String[] p = line.split("\t", -1);
            if (p.length < 4 || !"专注".equals(p[3].trim())) continue;
            String t = p[1].trim();
            if (t.length() < 16 || !t.startsWith(sumBestDay)) continue;
            String task = p.length > 4 ? p[4].trim() : "";
            sb.append(t.substring(11)).append("  ").append(p[2].trim()).append(" 分钟")
                    .append(task.isEmpty() ? "" : "  · " + task).append("\n");
        }
        java.awt.datatransfer.StringSelection sel =
                new java.awt.datatransfer.StringSelection(sb.toString());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, sel);
        try {
            Main.notifyMsg("已复制", mmdd(sumBestDay) + " 的专注记录已放进剪贴板");
        } catch (Exception ignored) {
        }
    }

    /** 用那天最常做的任务名接一轮番茄 */
    public void resumeTaskOfBestDay() {
        String task = topTaskOf(sumBestDay);
        PomodoroPanel pp = Main.page(PomodoroPanel.class);
        if (pp == null) {
            Main.notifyMsg("打不开番茄钟", "番茄钟页还没准备好");
            return;
        }
        Main.go("pomodoro");
        pp.setTaskName(task);
        pp.startFocusNow();
        try {
            Main.notifyMsg("接着专注", task.isEmpty() ? "已经起了一轮，任务名可以自己补"
                    : "任务名已填成「" + task + "」，计时开始");
        } catch (Exception ignored) {
        }
    }

    private String fmtMin(int min) {
        if (min <= 0) return "0分";
        if (min < 60) return min + "分钟";
        return (min / 60) + "小时" + (min % 60) + "分";
    }

    private String fmtClock(long sec) {
        long h = sec / 3600, m = (sec % 3600) / 60, s = sec % 60;
        if (h > 0) return String.format(Locale.CHINA, "%d:%02d:%02d", h, m, s);
        return String.format(Locale.CHINA, "%02d:%02d", m, s);
    }
}
