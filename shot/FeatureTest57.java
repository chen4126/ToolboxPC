import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** v1.73：小窗过期抬头按久度分档 / 三周对比每行右键 / 节奏行圆点右键 */
public class FeatureTest57 {

    static int pass = 0, fail = 0;
    static Object ov, st, mini, main;
    static Class<?> mbC, ovC, stC;
    static final LocalDate TODAY = LocalDate.now();
    static final LocalDate MONDAY = TODAY.minusDays(TODAY.getDayOfWeek().getValue() - 1);
    static final String[] WK = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};

    static String md(LocalDate d) {
        return String.format("%02d-%02d", d.getMonthValue(), d.getDayOfMonth());
    }

    static String dayRow(LocalDate d, int n) {
        return String.format("%02d-%02d %s   %d 条", d.getMonthValue(), d.getDayOfMonth(),
                WK[d.getDayOfWeek().getValue() % 7], n);
    }

    public static void main(String[] a) throws Exception {
        SwingUtilities.invokeAndWait(() -> Main.main(new String[0]));
        Thread.sleep(2500);
        Class<?> mc = Class.forName("com.toolbox.pc.Main");
        Field selfF = mc.getDeclaredField("self");
        selfF.setAccessible(true);
        main = selfF.get(null);
        Method pageM = mc.getDeclaredMethod("page", Class.class);
        ovC = Class.forName("com.toolbox.pc.OverviewPanel");
        stC = Class.forName("com.toolbox.pc.StatsPanel");
        mbC = Class.forName("com.toolbox.pc.MiniBoard");
        ov = pageM.invoke(main, ovC);
        st = pageM.invoke(main, stC);
        Method sel = mc.getDeclaredMethod("selectNav", String.class);
        sel.setAccessible(true);
        SwingUtilities.invokeAndWait(() -> {
            try {
                sel.invoke(main, "overview");
            } catch (Exception ignored) {
            }
        });
        Thread.sleep(1500);

        // ---------- 1) 小窗过期抬头：右键按久度分档 ----------
        Method showM = mbC.getDeclaredMethod("show");
        showM.setAccessible(true);
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    showM.invoke(null);
                } catch (Exception ignored) {
                }
            });
        } catch (Exception ignored) {
        }
        Thread.sleep(800);
        Field instF = mbC.getDeclaredField("inst");
        instF.setAccessible(true);
        mini = instF.get(null);
        check("小窗已创建", mini != null);
        setBool(mini, "overdueView", true);
        setBool(mini, "overdueShowAll", true);
        setInt(mini, "boardDay", 0);
        setInt(mini, "overdueAgeFilter", 0);
        final Method rf = mbC.getDeclaredMethod("refresh");
        rf.setAccessible(true);
        invokeRefresh();

        JPanel head = headPanel();
        JPopupMenu am = popupOf(head);
        check("抬头挂了右键菜单", am != null);
        List<String> items = menuTexts(am);
        check("抬头菜单 5 项（" + items.size() + "）：" + items, items.size() == 5);
        check("有「复制这 5 条原文」", items.contains("复制这 5 条原文"));
        check("有「全部过期条目」且带勾[" + items.get(0) + "]", items.get(0).contains("全部过期条目"));
        check("有「只看超过 7 天的」", items.contains("只看超过 7 天的"));
        check("有「只看 3–7 天的」", items.contains("只看 3–7 天的"));
        check("有「回到今天」", items.contains("回到今天"));
        check("抬头初始是完整版[" + allLabels(head) + "]", allLabels(head).contains("过期 5 条 · 最久 10 天"));

        // 只看超过 7 天的 → 十天前(10 天) + 九天前(9 天)
        clickByText(am, "只看超过 7 天的");
        invokeRefresh();
        String head7 = allLabels(headPanel());
        check("超 7 天抬头[" + head7 + "]", head7.contains("超 7 天") && head7.contains("2 条")
                && head7.contains("最久 10 天"));
        check("超 7 天只列 2 条[" + listLines() + "]", listLines().contains(md(TODAY.minusDays(10))) && listLines().contains(md(TODAY.minusDays(9)))
                && !listLines().contains(md(TODAY.minusDays(6))) && !listLines().contains(md(TODAY.minusDays(1))));

        // 只看 3–7 天的 → 六天前(6 天) + 四天前(4 天)
        JPopupMenu am2 = popupOf(headPanel());
        clickByText(am2, "只看 3–7 天的");
        invokeRefresh();
        String head37 = allLabels(headPanel());
        check("3–7 天抬头[" + head37 + "]", head37.contains("3–7 天") && head37.contains("2 条")
                && head37.contains("最久 6 天"));
        check("3–7 天列的是 " + md(TODAY.minusDays(6)) + " / " + md(TODAY.minusDays(4)) + "[" + listLines() + "]",
                listLines().contains(md(TODAY.minusDays(6))) && listLines().contains(md(TODAY.minusDays(4)))
                        && !listLines().contains(md(TODAY.minusDays(10))));

        // 回到全部
        clickByText(popupOf(headPanel()), "全部过期条目");
        invokeRefresh();
        String headAll = allLabels(headPanel());
        check("回到全部是 5 条[" + headAll + "]", headAll.contains("过期 5 条") && headAll.contains("最久 10 天"));
        check("全部时前四条目[" + listLines() + "]", listLines().contains(md(TODAY.minusDays(10)))
                && listLines().contains(md(TODAY.minusDays(4))) && !listLines().contains(md(TODAY.minusDays(1))));
        JPanel pin2 = (JPanel) getField(mini, "pinBox");
        check("底栏折叠行写「还有 1 条…」[" + allLabels(pin2) + "]", allLabels(pin2).contains("还有 1 条"));

        // 回到今天 → 退出过期视图，档位清零
        clickByText(popupOf(headPanel()), "回到今天");
        invokeRefresh();
        check("回到今天后退出过期视图", !getBool(mini, "overdueView"));
        check("档位清零转普通抬头[" + allLabels(headPanel()) + "]",
                allLabels(headPanel()).contains("今日待办") && getInt(mini, "overdueAgeFilter") == 0);

        // ---------- 2) 三周对比每行右键 ----------
        Method btp = stC.getDeclaredMethod("buildThreeWeekPanel");
        btp.setAccessible(true);
        JPanel tw = (JPanel) btp.invoke(st);
        check("三周面板造出来了", tw != null && tw.getComponentCount() == 20);
        JPopupMenu rm = popupOf((JComponent) tw.getComponent(5));
        check("上上周这行挂了右键菜单", rm != null);
        List<String> ritems = menuTexts(rm);
        check("行菜单 3 项（" + ritems.size() + "）：" + ritems, ritems.size() == 3);
        check("有「看这周逐日明细」", ritems.contains("看这周逐日明细"));
        check("有「看这周完成了哪几条」", ritems.contains("看这周完成了哪几条"));
        check("有「复制这周完成的条目」", ritems.contains("复制这周完成的条目"));
        JPopupMenu rm2 = popupOf((JComponent) tw.getComponent(15));
        check("本周这行也挂了同一个菜单", rm2 != null && menuTexts(rm2).size() == 3);

        // 逐日面板：上上周 monday-14 ~ monday-8，只有上上周三（monday-12）有 1 条
        Method brd = stC.getDeclaredMethod("buildRangeDailyPanel", String.class,
                LocalDate.class, LocalDate.class);
        brd.setAccessible(true);
        JPanel daily = (JPanel) brd.invoke(st, "上上周", MONDAY.minusDays(14), MONDAY.minusDays(8));
        check("逐日面板造出来了", daily != null && daily.getComponentCount() == 8);
        check("标题是「共完成 1 条 · 上上周」[" + cellText(daily, 0) + "]",
                cellText(daily, 0).equals("共完成 1 条 · 上上周"));
        List<String> days = new ArrayList<>();
        for (int i = 1; i < 8; i++) days.add(cellText(daily, i));
        check("第一条是 " + md(MONDAY.minusDays(14)) + " 周一 0 条[" + days.get(0) + "]",
                days.get(0).equals(dayRow(MONDAY.minusDays(14), 0)));
        check(md(MONDAY.minusDays(12)) + " 周三 1 条[" + days.get(2) + "]",
                days.get(2).equals(dayRow(MONDAY.minusDays(12), 1)));
        check(md(MONDAY.minusDays(8)) + " 周日 0 条[" + days.get(6) + "]",
                days.get(6).equals(dayRow(MONDAY.minusDays(8), 0)));

        // 复制上上周的条目
        Method cri = stC.getDeclaredMethod("copyRangeItems", String.class,
                LocalDate.class, LocalDate.class);
        cri.setAccessible(true);
        int n1 = (Integer) cri.invoke(st, "上上周", MONDAY.minusDays(14), MONDAY.minusDays(8));
        check("复制上上周返回 1 条（" + n1 + "）", n1 == 1);
        check("剪贴板里有 " + md(MONDAY.minusDays(12)) + " 上上周做的[" + clip() + "]",
                clip().contains(md(MONDAY.minusDays(12)) + "   上上周做的"));
        int n0 = (Integer) cri.invoke(st, "上上周", MONDAY.minusDays(21), MONDAY.minusDays(15));
        check("空的一周返回 0 条（" + n0 + "）", n0 == 0);

        // ---------- 3) 节奏行圆点右键 ----------
        JPanel dots = (JPanel) getField(ov, "weekDots");
        check("找到七天圆点", dots != null);
        Method dp = ovC.getDeclaredMethod("dotsPopup", java.awt.event.MouseEvent.class);
        dp.setAccessible(true);
        java.awt.event.MouseEvent me = new java.awt.event.MouseEvent(dots,
                java.awt.event.MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(), 0,
                7, 9, 1, true, java.awt.event.MouseEvent.BUTTON3);
        try {
            dp.invoke(ov, me);
        } catch (InvocationTargetException ignored) {
            // 组件没在屏幕上是正常现象，菜单对象这时已经建好
        }
        JPopupMenu dm = (JPopupMenu) getField(ov, "dotsMenu");
        check("圆点右键弹出菜单", dm != null);
        List<String> ditems = menuTexts(dm);
        check("圆点菜单 3 项（" + ditems.size() + "）：" + ditems, ditems.size() == 3);
        check("第一项定位到 " + md(MONDAY) + "[" + ditems.get(0) + "]",
                ditems.get(0).equals("看 " + md(MONDAY) + " 的待办清单"));
        check("第二项写明 " + Seed.mondayDone() + " 条[" + ditems.get(1) + "]",
                ditems.get(1).equals("复制 " + md(MONDAY) + " 完成的 " + Seed.mondayDone() + " 条"));

        final JPopupMenu dmF = dm;
        SwingUtilities.invokeAndWait(() -> clickByText(dmF,
                "复制 " + md(MONDAY) + " 完成的 " + Seed.mondayDone() + " 条"));
        Thread.sleep(300);
        check("剪贴板里有 " + md(MONDAY) + " 周一做的[" + clip() + "]",
                clip().contains(md(MONDAY) + "   周一做的"));

        SwingUtilities.invokeAndWait(() -> clickByText(dmF, "看 " + md(MONDAY) + " 的待办清单"));
        Thread.sleep(600);
        LocalDate vd = (LocalDate) getField(ov, "viewDate");
        check("跳到 " + md(MONDAY) + "[" + vd + "]", vd != null && vd.equals(MONDAY));

        // ---------- 出图 ----------
        String out = "C:\\Users\\chen\\Desktop\\ui-252-week-range.png";
        shot(daily, out, 340, 172);
        System.out.println("SHOT " + new File(out).length());

        System.out.println(fail == 0 ? ("ALL PASS (" + pass + ")") : ("SOME FAIL " + fail + "/" + (pass + fail)));
        Runtime.getRuntime().halt(fail == 0 ? 0 : 1);
    }

    static void invokeRefresh() throws Exception {
        Method rf = mbC.getDeclaredMethod("refresh");
        rf.setAccessible(true);
        SwingUtilities.invokeAndWait(() -> {
            try {
                rf.invoke(mini);
            } catch (Exception ignored) {
            }
        });
        Thread.sleep(350);
    }

    static JPanel headPanel() throws Exception {
        JPanel listBox = (JPanel) getField(mini, "listBox");
        return (JPanel) listBox.getComponent(0);
    }

    static String listLines() throws Exception {
        JPanel listBox = (JPanel) getField(mini, "listBox");
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < listBox.getComponentCount(); i++) {
            sb.append(allLabels(listBox.getComponent(i))).append(" | ");
        }
        return sb.toString();
    }

    static JPopupMenu popupOf(JComponent c) {
        return c == null ? null : c.getComponentPopupMenu();
    }

    static List<String> menuTexts(JPopupMenu m) {
        List<String> out = new ArrayList<>();
        if (m == null) return out;
        for (Component x : m.getComponents()) {
            if (x instanceof JMenuItem) out.add(((JMenuItem) x).getText());
        }
        return out;
    }

    static void clickByText(JPopupMenu m, String part) {
        if (m == null) return;
        for (Component x : m.getComponents()) {
            if (x instanceof JMenuItem && ((JMenuItem) x).getText().contains(part)) {
                ((JMenuItem) x).doClick();
                return;
            }
        }
    }

    static String clip() throws Exception {
        Object data = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                .getData(java.awt.datatransfer.DataFlavor.stringFlavor);
        return data == null ? "" : data.toString();
    }

    static String cellText(JPanel p, int i) {
        Component c = p.getComponent(i);
        return c instanceof JLabel ? ((JLabel) c).getText() : "";
    }

    static String allLabels(Component c) {
        java.util.List<Component> all = new ArrayList<>();
        collect(c, all);
        StringBuilder sb = new StringBuilder();
        for (Component k : all) if (k instanceof JLabel) sb.append(((JLabel) k).getText()).append(' ');
        return sb.toString().trim();
    }

    static void collect(Component c, List<Component> out) {
        out.add(c);
        if (c instanceof Container) for (Component k : ((Container) c).getComponents()) collect(k, out);
    }

    static Object getField(Object o, String n) throws Exception {
        Field f = o.getClass().getDeclaredField(n);
        f.setAccessible(true);
        return f.get(o);
    }

    static int getInt(Object o, String n) throws Exception {
        return (Integer) getField(o, n);
    }

    static boolean getBool(Object o, String n) throws Exception {
        return (Boolean) getField(o, n);
    }

    static void setBool(Object o, String n, boolean v) throws Exception {
        Field f = o.getClass().getDeclaredField(n);
        f.setAccessible(true);
        f.set(o, v);
    }

    static void setInt(Object o, String n, int v) throws Exception {
        Field f = o.getClass().getDeclaredField(n);
        f.setAccessible(true);
        f.set(o, v);
    }

    static void layoutAll(Component c) {
        c.doLayout();
        if (c instanceof Container) for (Component k : ((Container) c).getComponents()) layoutAll(k);
    }

    static void shot(JComponent c, String path, int w, int h) throws Exception {
        c.setDoubleBuffered(false);
        ((JPanel) c).setOpaque(true);
        c.setBackground(new Color(0xF7F8FC));
        if (c.getParent() != null) c.getParent().remove(c);
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
        System.out.println((ok ? "[PASS] " : "[FAIL] ") + name);
        if (ok) pass++;
        else fail++;
    }
}
