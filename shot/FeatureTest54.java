import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/** v1.70：统计页本月逐日明细（按周分行）+ 过期筛选行右键整批复制到明天 + 小窗过期提示可来回切 */
public class FeatureTest54 {

    static int pass = 0, fail = 0;
    static Object ov;
    static Object main;
    static java.time.LocalDate today = java.time.LocalDate.now();

    static String md(java.time.LocalDate d) {
        return String.format("%02d-%02d", d.getMonthValue(), d.getDayOfMonth());
    }

    /** 本月到今天为止、完全没有完成记录的天数（有完成的天数按种子动态数） */
    static int noDoneDays() {
        java.util.Set<Integer> days = new java.util.HashSet<Integer>();
        for (String r : Seed.rows()) {
            String[] p = r.split("\t", -1);
            if (p.length < 3 || !"1".equals(p[0]) || p[1].endsWith("（已归档）")) continue;
            java.time.LocalDate d = java.time.LocalDate.parse(p[2].trim());
            if (d.getYear() == today.getYear() && d.getMonthValue() == today.getMonthValue()
                    && !d.isAfter(today)) days.add(d.getDayOfMonth());
        }
        return today.getDayOfMonth() - days.size();
    }

    /** 本月按周一划分的周段：[本周第一天, 本周最后一天]，只含与本月相交的部分 */
    static java.util.List<java.time.LocalDate[]> monthWeeks() {
        java.time.LocalDate first = today.withDayOfMonth(1);
        java.time.LocalDate last = today.withDayOfMonth(today.lengthOfMonth());
        java.time.LocalDate ws = first.minusDays(first.getDayOfWeek().getValue() - 1);
        java.util.List<java.time.LocalDate[]> out = new java.util.ArrayList<java.time.LocalDate[]>();
        while (!ws.isAfter(last)) {
            java.time.LocalDate s = ws.isBefore(first) ? first : ws;
            java.time.LocalDate e = ws.plusDays(6).isAfter(last) ? last : ws.plusDays(6);
            out.add(new java.time.LocalDate[]{s, e});
            ws = ws.plusDays(7);
        }
        return out;
    }

    /** 这周里种子贡献了几条完成（非归档、日期在本月） */
    static int weekExpect(java.time.LocalDate s, java.time.LocalDate e) {
        int n = 0;
        for (String r : Seed.rows()) {
            String[] p = r.split("\t", -1);
            if (p.length < 3 || !"1".equals(p[0]) || p[1].endsWith("（已归档）")) continue;
            java.time.LocalDate d = java.time.LocalDate.parse(p[2].trim());
            if (!d.isBefore(s) && !d.isAfter(e)) n++;
        }
        return n;
    }

    /** 今天落在第几周（0 起） */
    static int thisWeekIndex() {
        java.util.List<java.time.LocalDate[]> wks = monthWeeks();
        for (int i = 0; i < wks.size(); i++) {
            if (!today.isBefore(wks.get(i)[0]) && !today.isAfter(wks.get(i)[1])) return i;
        }
        return -1;
    }

    public static void main(String[] a) throws Exception {
        SwingUtilities.invokeAndWait(() -> Main.main(new String[0]));
        Thread.sleep(2500);
        Class<?> mc = Class.forName("com.toolbox.pc.Main");
        Field selfF = mc.getDeclaredField("self");
        selfF.setAccessible(true);
        main = selfF.get(null);
        Method pageM = mc.getDeclaredMethod("page", Class.class);
        ov = pageM.invoke(main, Class.forName("com.toolbox.pc.OverviewPanel"));
        Method sel = mc.getDeclaredMethod("selectNav", String.class);
        sel.setAccessible(true);
        SwingUtilities.invokeAndWait(() -> {
            try {
                sel.invoke(main, "overview");
            } catch (Exception ignored) {
            }
        });
        Thread.sleep(1500);

        String todoFile = Store.dir() + "\\todo.txt";

        // ---------- 1) 本月逐日计数 ----------
        Method mdc = ov.getClass().getDeclaredMethod("monthDoneCounts");
        mdc.setAccessible(true);
        int[] c = (int[]) mdc.invoke(ov);
        check("本月计数长度 = 30（" + today.lengthOfMonth() + "）[" + c.length + "]",
                c.length == today.lengthOfMonth());
        int monthSum = 0;
        for (int v : c) monthSum += v;
        check("本月完成合计 = " + Seed.monthDone() + "（动态按种子数）[" + monthSum + "]",
                monthSum == Seed.monthDone());
        java.time.LocalDate mon = Seed.monday();
        java.time.LocalDate lastTue = mon.minusDays(6), lastWed = mon.minusDays(5), w2 = mon.minusDays(12);
        check(md(today) + " 今天是 " + Seed.todayDone() + " 条 ["
                        + c[today.getDayOfMonth() - 1] + "]",
                c[today.getDayOfMonth() - 1] == Seed.todayDone());
        if (!mon.isEqual(today)) {
            check(md(mon) + " 周一一条 [" + c[mon.getDayOfMonth() - 1] + "]",
                    c[mon.getDayOfMonth() - 1] == 1);
        }
        if (lastTue.getMonthValue() == today.getMonthValue()) {
            check(md(lastTue) + " 上周二一条 [" + c[lastTue.getDayOfMonth() - 1] + "]",
                    c[lastTue.getDayOfMonth() - 1] == 1);
        }
        if (lastWed.getMonthValue() == today.getMonthValue()) {
            check(md(lastWed) + " 上周三一条 [" + c[lastWed.getDayOfMonth() - 1] + "]",
                    c[lastWed.getDayOfMonth() - 1] == 1);
        }
        if (w2.getMonthValue() == today.getMonthValue()) {
            check(md(w2) + " 上上周三一条 [" + c[w2.getDayOfMonth() - 1] + "]",
                    c[w2.getDayOfMonth() - 1] == 1);
        } else {
            check(md(w2) + " 上上周那条在上月，不计入本月", monthSum == Seed.monthDone());
        }
        check("归档的两条不算进本月", monthSum == Seed.monthDone());

        // ---------- 2) 统计页本月卡右键 + 逐日明细面板 ----------
        Object st = pageM.invoke(main, Class.forName("com.toolbox.pc.StatsPanel"));
        Field mcf = st.getClass().getDeclaredField("todoMonthCard");
        mcf.setAccessible(true);
        JPanel monthCard = (JPanel) mcf.get(st);
        JPopupMenu cm = monthCard.getComponentPopupMenu();
        check("统计页本月卡挂了右键菜单", cm != null);
        if (cm != null) {
            List<String> items = new ArrayList<>();
            for (Component x : cm.getComponents()) {
                if (x instanceof JMenuItem) items.add(((JMenuItem) x).getText());
            }
            check("菜单里有「看本月逐日明细」", items.contains("看本月逐日明细"));
            check("菜单里有「复制这个月完成的条目」", items.contains("复制这个月完成的条目"));
            check("菜单里有「看这个月完成了哪几条」", items.contains("看这个月完成了哪几条"));
        }

        Method bmd = st.getClass().getDeclaredMethod("buildMonthDailyPanel");
        bmd.setAccessible(true);
        JPanel box = (JPanel) bmd.invoke(st);
        check("逐日明细面板不为空", box != null);
        List<String> lines = new ArrayList<>();
        if (box != null) {
            for (Component x : box.getComponents()) {
                if (x instanceof JLabel) lines.add(((JLabel) x).getText());
                else if (x instanceof JPanel) {
                    String line = "";
                    for (Component y : ((JPanel) x).getComponents()) {
                        if (y instanceof JLabel) line += "|" + ((JLabel) y).getText();
                    }
                    lines.add(line);
                }
            }
        }
        java.util.List<java.time.LocalDate[]> wks = monthWeeks();
        int wkIdx = thisWeekIndex();
        check("面板行数 = 2 段头 + " + wks.size() + " 周（" + lines.size() + "）",
                lines.size() == 2 + wks.size());
        if (lines.size() >= 2) {
            check("总述行写本月 " + Seed.monthDone() + " 条 [" + lines.get(0) + "]",
                    lines.get(0).contains("本月共完成 " + Seed.monthDone() + " 条"));
            check("总述行写 " + noDoneDays() + " 天没完成 [" + lines.get(0) + "]",
                    lines.get(0).contains(noDoneDays() + " 天没完成"));
            /* 最勤一周 = 种子贡献最多的那一周；今天这周全在今天一天，平时也最多 */
            int bestWk = 0, bestCnt = -1;
            for (int i = 0; i < wks.size(); i++) {
                int n = weekExpect(wks.get(i)[0], wks.get(i)[1]);
                if (n > bestCnt) {
                    bestCnt = n;
                    bestWk = i;
                }
            }
            check("最勤一周 = 第 " + (bestWk + 1) + " 周 " + bestCnt + " 条 [" + lines.get(1) + "]",
                    lines.get(1).contains("第 " + (bestWk + 1) + " 周")
                            && lines.get(1).contains(bestCnt + " 条"));
            check("最勤一天 = " + md(today) + "（" + Seed.todayDone() + " 条）[" + lines.get(1) + "]",
                    lines.get(1).contains(md(today)) && lines.get(1).contains(Seed.todayDone() + " 条"));
        }
        if (lines.size() == 2 + wks.size()) {
            /* 本周所在行：区间文本 + 本周条数 + 最勤是今天 */
            String thisLine = lines.get(2 + wkIdx);
            String range = md(wks.get(wkIdx)[0]) + " ~ " + md(wks.get(wkIdx)[1]);
            int thisCnt = weekExpect(wks.get(wkIdx)[0], wks.get(wkIdx)[1]);
            check("本周行写 " + range + " " + thisCnt + " 条 [" + thisLine + "]",
                    thisLine.contains(range) && thisLine.contains(thisCnt + " 条"));
            check("本周行最勤是今天 [" + thisLine + "]", thisLine.contains(md(today)));
            /* 本周之后的周都写「还没到」；如果今天就是本月最后一天则没有未来周 */
            boolean hasFuture = false;
            for (int i = wkIdx + 1; i < wks.size(); i++) {
                hasFuture = true;
                String ln = lines.get(2 + i);
                check("第 " + (i + 1) + " 周写还没到 [" + ln + "]", ln.contains("还没到"));
            }
            if (!hasFuture) {
                check("今天是本月最后一周，没有未来周", true);
            }
            /* 第一周行从本月 1 号写起 */
            check("第一周行含 " + md(wks.get(0)[0]) + " [" + lines.get(2) + "]",
                    lines.get(2).contains(md(wks.get(0)[0])));
        }

        // ---------- 3) 概览过期筛选行右键：整批复制到明天 ----------
        Field fbF = ov.getClass().getDeclaredField("futureBar");
        fbF.setAccessible(true);
        JPanel futureBar = (JPanel) fbF.get(ov);
        check("futureBar 存在", futureBar != null);
        JPopupMenu fbm = futureBar == null ? null : futureBar.getComponentPopupMenu();
        check("过期筛选行挂了右键菜单", fbm != null);
        List<String> fitems = new ArrayList<>();
        if (fbm != null) {
            for (Component x : fbm.getComponents()) {
                if (x instanceof JMenuItem) fitems.add(((JMenuItem) x).getText());
            }
        }
        check("菜单里有「把过期这几条一起复制到明天」", fitems.contains("把过期这几条一起复制到明天"));
        check("菜单里有「复制过期条目的文字」", fitems.contains("复制过期条目的文字"));
        check("菜单里有「全部挪到今天」", fitems.contains("全部挪到今天"));

        Method cot = ov.getClass().getDeclaredMethod("copyOverdueToTomorrow");
        cot.setAccessible(true);
        int before = Files.readAllLines(Paths.get(todoFile)).size();
        int copied = (int) cot.invoke(ov);
        Thread.sleep(600);
        int after = Files.readAllLines(Paths.get(todoFile)).size();
        check("过期未做共 5 条被复制 [" + copied + "]", copied == 5);
        check("文件条目 " + before + " -> " + after + "（+5）", after == before + 5);
        java.time.LocalDate tomorrow = today.plusDays(1);
        int newTomorrow = 0, oldOverdue = 0;
        for (String l : Files.readAllLines(Paths.get(todoFile))) {
            if (l.startsWith("0\t") && l.endsWith("\t" + tomorrow)) newTomorrow++;
            if (l.startsWith("0\t") && l.contains("\t" + today.minusDays(9))) oldOverdue++;
        }
        check("明天那天共 6 条（原 1 条「明天的事」+ 新复制 5）[" + newTomorrow + "]", newTomorrow == 6);
        check("原条留在 " + md(today.minusDays(9)) + " 没被搬走 [" + oldOverdue + "]", oldOverdue == 1);

        // ---------- 4) 小窗过期提示条可来回切 ----------
        Class<?> mb = Class.forName("com.toolbox.pc.MiniBoard");
        Field instF = mb.getDeclaredField("inst");
        instF.setAccessible(true);
        Method show = mb.getDeclaredMethod("show");
        if (instF.get(null) == null) SwingUtilities.invokeAndWait(() -> {
            try {
                show.invoke(null);
            } catch (Exception ignored) {
            }
        });
        Thread.sleep(1400);
        Object inst = instF.get(null);
        check("小窗已创建", inst != null);
        if (inst != null) {
            Field ovvF = mb.getDeclaredField("overdueView");
            ovvF.setAccessible(true);
            Field dayF = mb.getDeclaredField("boardDay");
            dayF.setAccessible(true);
            Field olF = mb.getDeclaredField("overdueLbl");
            olF.setAccessible(true);
            JLabel ol = (JLabel) olF.get(inst);
            check("过期提示条存在且可见", ol != null && ol.isVisible());
            check("初始不是过期视图", !ovvF.getBoolean(inst));
            if (ol != null) {
                click(ol);
                Thread.sleep(700);
                check("点一下进入只看过期", ovvF.getBoolean(inst));
                check("进入时看的还是今天（boardDay=0）[" + dayF.getInt(inst) + "]", dayF.getInt(inst) == 0);
                check("提示条写明能切回 [" + ol.getToolTipText() + "]",
                        ol.getToolTipText() != null && ol.getToolTipText().contains("回到今天"));
                Field winF = mb.getDeclaredField("win");
                winF.setAccessible(true);
                Object win = winF.get(inst);
                if (win instanceof JFrame) {
                    int hh = ((JFrame) win).getHeight();
                    if (hh < 120) hh = 320;
                    shot((JComponent) ((JFrame) win).getContentPane(),
                            "C:\\Users\\chen\\Desktop\\ui-249-mini-overdue.png", 254, hh);
                    System.out.println("SHOT2 "
                            + new File("C:\\Users\\chen\\Desktop\\ui-249-mini-overdue.png").length());
                }
                click(ol);
                Thread.sleep(700);
                check("再点一下回到今天", !ovvF.getBoolean(inst));
            }
        }

        // ---------- 出图 ----------
        if (box != null) {
            shot(box, "C:\\Users\\chen\\Desktop\\ui-248-month-daily.png", 340, 200);
            System.out.println("SHOT " + new File("C:\\Users\\chen\\Desktop\\ui-248-month-daily.png").length());
        }

        System.out.println(fail == 0 ? ("ALL PASS (" + pass + ")") : ("SOME FAIL " + fail + "/" + (pass + fail)));
        Runtime.getRuntime().halt(fail == 0 ? 0 : 1);
    }

    static void click(JLabel l) {
        try {
            MouseEvent e = new MouseEvent(l, MouseEvent.MOUSE_CLICKED,
                    System.currentTimeMillis(), 0, 6, 6, 1, false, MouseEvent.BUTTON1);
            l.dispatchEvent(e);
        } catch (Exception ex) {
            System.out.println("click fail " + ex);
        }
    }

    static void layoutAll(Component c) {
        c.doLayout();
        if (c instanceof Container) for (Component k : ((Container) c).getComponents()) layoutAll(k);
    }

    static void shot(JComponent c, String path, int w, int h) throws Exception {
        c.setDoubleBuffered(false);
        if (c instanceof JPanel) ((JPanel) c).setOpaque(true);
        c.setBackground(new Color(0xF7F8FC));
        c.setSize(w, h);
        c.doLayout();
        layoutAll(c);
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(w, h,
                java.awt.image.BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        c.paint(g);
        g.dispose();
        javax.imageio.ImageIO.write(img, "png", new File(path));
    }

    static void check(String name, boolean ok) {
        check(name, ok, "");
    }

    static void check(String name, boolean ok, String extra) {
        if (ok) {
            pass++;
            System.out.println("[PASS] " + name + (extra.isEmpty() ? "" : " → " + extra));
        } else {
            fail++;
            System.out.println("[FAIL] " + name + (extra.isEmpty() ? "" : " → " + extra));
        }
    }
}
