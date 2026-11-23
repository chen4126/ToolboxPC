import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** v1.71：只看过期按钮右键整批复制 + 明细里点某周/某天展开完成清单 */
public class FeatureTest55 {

    static int pass = 0, fail = 0;
    static Object ov;
    static Object main;
    static final LocalDate TODAY = LocalDate.now();
    static final LocalDate MONDAY = TODAY.minusDays(TODAY.getDayOfWeek().getValue() - 1);

    static String md(LocalDate d) {
        return String.format("%02d-%02d", d.getMonthValue(), d.getDayOfMonth());
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

        // ---------- 1) 「只看过期」按钮挂上整批菜单 ----------
        Field bob = ov.getClass().getDeclaredField("onlyOverdueBtn");
        bob.setAccessible(true);
        Object btn = bob.get(ov);
        check("只看过期按钮存在", btn != null);
        JPopupMenu bm = null;
        if (btn instanceof JComponent) bm = ((JComponent) btn).getComponentPopupMenu();
        check("只看过期按钮挂了右键菜单", bm != null);
        List<String> bitems = new ArrayList<>();
        if (bm != null) {
            for (Component x : bm.getComponents()) {
                if (x instanceof JMenuItem) bitems.add(((JMenuItem) x).getText());
            }
        }
        check("菜单里有「把过期这几条一起复制到明天」", bitems.contains("把过期这几条一起复制到明天"));
        check("菜单里有「全部挪到今天」", bitems.contains("全部挪到今天"));
        check("菜单是 3 项（" + bitems.size() + "）", bitems.size() == 3);

        // ---------- 2) doneInRange 取某段时间完成的条目 ----------
        Method dir = ov.getClass().getDeclaredMethod("doneInRange", LocalDate.class, LocalDate.class);
        dir.setAccessible(true);
        List<?> week = (List<?>) dir.invoke(ov, MONDAY, MONDAY.plusDays(6));
        check("本周区间共 3 条（" + week.size() + "）", week.size() == 3);
        if (MONDAY.equals(TODAY)) {
            /* 周一：本周 3 条全在今天 */
            check("三条都是今天 [" + fmt(week, 0) + " / " + fmt(week, 1) + " / " + fmt(week, 2) + "]",
                    fmt(week, 0).contains(TODAY.toString()) && fmt(week, 1).contains(TODAY.toString())
                            && fmt(week, 2).contains(TODAY.toString()));
        } else {
            check("第一条是 " + md(MONDAY) + " [" + fmt(week, 0) + "]", fmt(week, 0).contains(MONDAY.toString()));
            check("第二条是 " + md(TODAY) + " [" + fmt(week, 1) + "]", fmt(week, 1).contains(TODAY.toString()));
            check("第三条也是 " + md(TODAY) + " [" + fmt(week, 2) + "]", fmt(week, 2).contains(TODAY.toString()));
            check("条目带文字 [" + fmt(week, 0) + "]", fmt(week, 0).contains("周一做的"));
        }

        List<?> todayL = (List<?>) dir.invoke(ov, TODAY, TODAY);
        check("只看今天共 " + Seed.todayDone() + " 条（" + todayL.size() + "）",
                todayL.size() == Seed.todayDone());

        List<?> octL = (List<?>) dir.invoke(ov, MONDAY.minusDays(14), MONDAY.minusDays(8));
        check("上上周只算 1 条（归档两条被排除）[" + octL.size() + "]", octL.size() == 1);
        check("上上周那条是 " + md(MONDAY.minusDays(12)) + " [" + fmt(octL, 0) + "]",
                fmt(octL, 0).contains(MONDAY.minusDays(12).toString()));

        List<?> futureL = (List<?>) dir.invoke(ov, MONDAY.plusDays(7), MONDAY.plusDays(13));
        check("未来那一周 0 条", futureL.isEmpty());

        // ---------- 3) 本周明细：某一天的行可点开 ----------
        Object st = pageM.invoke(main, Class.forName("com.toolbox.pc.StatsPanel"));
        Method bwd = st.getClass().getDeclaredMethod("buildWeekDailyPanel");
        bwd.setAccessible(true);
        JPanel wbox = (JPanel) bwd.invoke(st);
        check("本周明细面板不为空", wbox != null);
        int ti = TODAY.getDayOfWeek().getValue() - 1;      // 周一=0
        JPanel wedRow = rowAt(wbox, 2 + ti);          // 今天那行
        check("本周今天那行是面板第 " + (3 + ti) + " 个组件", wedRow != null);
        if (wedRow != null) {
            check("今天那行可点（有监听）", wedRow.getMouseListeners().length > 0);
            check("今天那行手型光标", wedRow.getCursor().getType() == Cursor.HAND_CURSOR);
            check("今天那行 tooltip 写这天 [" + wedRow.getToolTipText() + "]",
                    wedRow.getToolTipText() != null && wedRow.getToolTipText().contains("这天"));
        }
        JPanel monRow = rowAt(wbox, 2);
        if (monRow != null) {
            check("周一那行也可点（有完成）", monRow.getMouseListeners().length > 0);
        }
        JPanel thuRow = rowAt(wbox, 3 + ti);         // 明天：还没到，不该可点
        if (thuRow != null) {
            check("明天那行不可点（还没到）", thuRow.getMouseListeners().length == 0);
        }

        // ---------- 4) 本月明细：本周那行可点开，0 条的过去周不可点 ----------
        Method bmd = st.getClass().getDeclaredMethod("buildMonthDailyPanel");
        bmd.setAccessible(true);
        JPanel mbox = (JPanel) bmd.invoke(st);
        java.util.List<LocalDate[]> wks = monthWeeks();
        int wkIdx = -1;
        for (int i = 0; i < wks.size(); i++) {
            if (!TODAY.isBefore(wks.get(i)[0]) && !TODAY.isAfter(wks.get(i)[1])) wkIdx = i;
        }
        JPanel segThis = wkIdx < 0 ? null : rowAt(mbox, 2 + wkIdx);
        check("本月本周（第 " + (wkIdx + 1) + " 周）那行存在", segThis != null);
        if (segThis != null) {
            check("本周那行可点", segThis.getMouseListeners().length > 0);
            check("本周 tooltip 写这一周 [" + segThis.getToolTipText() + "]",
                    segThis.getToolTipText() != null && segThis.getToolTipText().contains("这一周"));
        }
        int zeroWk = -1;
        for (int i = 0; i < wkIdx; i++) {
            if (weekExpect(wks.get(i)[0], wks.get(i)[1]) == 0) {
                zeroWk = i;
                break;
            }
        }
        if (zeroWk >= 0) {
            JPanel seg0 = rowAt(mbox, 2 + zeroWk);
            check("0 条的第 " + (zeroWk + 1) + " 周不可点",
                    seg0 != null && seg0.getMouseListeners().length == 0);
        } else {
            check("本月本周之前没有 0 条的周", true);
        }

        // ---------- 5) 点开后的清单面板 ----------
        Method bdi = st.getClass().getDeclaredMethod("buildDoneItemsPanel", String.class, java.util.List.class);
        bdi.setAccessible(true);
        JPanel items = (JPanel) bdi.invoke(st, "共完成 3 条 · 第 3 周（11-09 ~ 11-15）", week);
        check("清单面板不为空", items != null);
        List<String> lines = new ArrayList<>();
        if (items != null) {
            for (Component x : items.getComponents()) if (x instanceof JLabel) lines.add(((JLabel) x).getText());
        }
        check("标题带条数（" + (lines.isEmpty() ? "" : lines.get(0)) + "）",
                !lines.isEmpty() && lines.get(0).contains("共完成 3 条"));
        check("标题带周范围", !lines.isEmpty() && lines.get(0).contains("11-09 ~ 11-15"));
        check("逐条 4 行（1 标题 + 3 条）[" + lines.size() + "]", lines.size() == 4);
        if (lines.size() == 4) {
            if (MONDAY.equals(TODAY)) {
                /* 周一：三条都是今天的，文字各一 */
                check("三条都落在今天", lines.get(1).startsWith(md(TODAY))
                        && lines.get(2).startsWith(md(TODAY)) && lines.get(3).startsWith(md(TODAY)));
                check("三条文字齐全", (lines.get(1) + lines.get(2) + lines.get(3)).contains("周一做的")
                        && (lines.get(1) + lines.get(2) + lines.get(3)).contains("今天已做完")
                        && (lines.get(1) + lines.get(2) + lines.get(3)).contains("今天第二件"));
            } else {
                check("第一条 " + md(MONDAY) + " 周一做的 [" + lines.get(1) + "]",
                        lines.get(1).startsWith(md(MONDAY)) && lines.get(1).contains("周一做的"));
                check("第二条 " + md(TODAY) + " 含今天已做完 [" + lines.get(2) + "]",
                        lines.get(2).startsWith(md(TODAY)) && lines.get(2).contains("今天已做完"));
                check("第三条 " + md(TODAY) + " 含今天第二件 [" + lines.get(3) + "]",
                        lines.get(3).startsWith(md(TODAY)) && lines.get(3).contains("今天第二件"));
            }
        }
        JPanel emptyBox = (JPanel) bdi.invoke(st, "共完成 0 条 · 未来那一周", futureL);
        List<String> emptyLines = new ArrayList<>();
        if (emptyBox != null) {
            for (Component x : emptyBox.getComponents()) if (x instanceof JLabel) emptyLines.add(((JLabel) x).getText());
        }
        check("空区间写还没有完成 [" + (emptyLines.isEmpty() ? "" : emptyLines.get(1)) + "]",
                emptyLines.size() == 2 && emptyLines.get(1).contains("还没有完成"));

        // ---------- 出图 ----------
        if (items != null) {
            shot(items, "C:\\Users\\chen\\Desktop\\ui-250-done-items.png", 320, 130);
            System.out.println("SHOT " + new File("C:\\Users\\chen\\Desktop\\ui-250-done-items.png").length());
        }

        System.out.println(fail == 0 ? ("ALL PASS (" + pass + ")") : ("SOME FAIL " + fail + "/" + (pass + fail)));
        Runtime.getRuntime().halt(fail == 0 ? 0 : 1);
    }

    /** 本月按周一划分的周段（与 FT54 同口径） */
    static java.util.List<LocalDate[]> monthWeeks() {
        LocalDate first = TODAY.withDayOfMonth(1);
        LocalDate last = TODAY.withDayOfMonth(TODAY.lengthOfMonth());
        LocalDate ws = first.minusDays(first.getDayOfWeek().getValue() - 1);
        java.util.List<LocalDate[]> out = new java.util.ArrayList<LocalDate[]>();
        while (!ws.isAfter(last)) {
            LocalDate s = ws.isBefore(first) ? first : ws;
            LocalDate e = ws.plusDays(6).isAfter(last) ? last : ws.plusDays(6);
            out.add(new LocalDate[]{s, e});
            ws = ws.plusDays(7);
        }
        return out;
    }

    /** 这周里种子贡献了几条完成 */
    static int weekExpect(LocalDate s, LocalDate e) {
        int n = 0;
        for (String r : Seed.rows()) {
            String[] p = r.split("\t", -1);
            if (p.length < 3 || !"1".equals(p[0]) || p[1].endsWith("（已归档）")) continue;
            LocalDate d = LocalDate.parse(p[2].trim());
            if (!d.isBefore(s) && !d.isAfter(e)) n++;
        }
        return n;
    }

    /** 面板里第 i 个组件如果是行（JPanel）就返回它 */
    static JPanel rowAt(JPanel box, int idx) {
        if (box == null || idx >= box.getComponentCount()) return null;
        Component c = box.getComponent(idx);
        return c instanceof JPanel ? (JPanel) c : null;
    }

    static String fmt(List<?> list, int i) {
        if (list == null || i >= list.size()) return "";
        Object o = list.get(i);
        if (o instanceof String[]) {
            String[] s = (String[]) o;
            return s.length >= 2 ? (s[0] + " " + s[1]) : String.valueOf(s);
        }
        return String.valueOf(o);
    }

    static void layoutAll(Component c) {
        c.doLayout();
        if (c instanceof Container) for (Component k : ((Container) c).getComponents()) layoutAll(k);
    }

    static void shot(JComponent c, String path, int w, int h) throws Exception {
        c.setDoubleBuffered(false);
        if (c instanceof JPanel) {
            ((JPanel) c).setOpaque(true);
            c.setBackground(new Color(0xF7F8FC));
        }
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
