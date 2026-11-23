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

/** v1.74：本月卡补齐两项 / 小窗过期抬头复制这一档 / 小窗行右键查备忘录 / 圆点复制整天的待办 */
public class FeatureTest58 {

    static int pass = 0, fail = 0;
    static Object ov, st, mini, main;
    static Class<?> mbC, ovC;
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
        ovC = Class.forName("com.toolbox.pc.OverviewPanel");
        mbC = Class.forName("com.toolbox.pc.MiniBoard");
        ov = pageM.invoke(main, ovC);
        st = pageM.invoke(main, Class.forName("com.toolbox.pc.StatsPanel"));
        Method sel = mc.getDeclaredMethod("selectNav", String.class);
        sel.setAccessible(true);
        SwingUtilities.invokeAndWait(() -> {
            try {
                sel.invoke(main, "overview");
            } catch (Exception ignored) {
            }
        });
        Thread.sleep(1500);

        // ---------- 1) 统计页本月卡右键补齐 ----------
        JPanel monthCard = (JPanel) getField(st, "todoMonthCard");
        JPopupMenu mm = monthCard.getComponentPopupMenu();
        check("本月卡有右键菜单", mm != null);
        List<String> mitems = menuTexts(mm);
        check("本月卡 3 项（" + mitems.size() + "）：" + mitems, mitems.size() == 3);
        check("有「看本月逐日明细」", mitems.contains("看本月逐日明细"));
        check("有「看这个月完成了哪几条」", mitems.contains("看这个月完成了哪几条"));
        check("有「复制这个月完成的条目」", mitems.contains("复制这个月完成的条目"));

        Method cri = st.getClass().getDeclaredMethod("copyRangeItems", String.class,
                LocalDate.class, LocalDate.class);
        cri.setAccessible(true);
        LocalDate first = LocalDate.now().withDayOfMonth(1);
        int cnt = (Integer) cri.invoke(st, "本月", first, LocalDate.now());
        check("本月复制回来 " + Seed.monthDone() + " 条（" + cnt + "）", cnt == Seed.monthDone());
        String clipNow = clip();
        check("剪贴板有 " + md(TODAY) + " 的两条[" + clipNow.replace("\n", " | ") + "]",
                clipNow.contains(md(TODAY) + "   今天已做完") && clipNow.contains(md(TODAY) + "   今天第二件"));
        check("剪贴板有 " + md(MONDAY) + " 那条", clipNow.contains(md(MONDAY) + "   周一做的"));
        boolean w2InMonth = MONDAY.minusDays(12).getMonthValue() == TODAY.getMonthValue();
        check("上上周的 " + md(MONDAY.minusDays(12)) + (w2InMonth ? " 在本月应含" : " 在上月不含")
                        + "[" + clipNow.replace("\n", " | ") + "]",
                w2InMonth == clipNow.contains(md(MONDAY.minusDays(12))));

        // ---------- 2) 圆点右键第三项：复制整天的待办 ----------
        JPanel dots = (JPanel) getField(ov, "weekDots");
        Method dp = ovC.getDeclaredMethod("dotsPopup", java.awt.event.MouseEvent.class);
        dp.setAccessible(true);
        java.awt.event.MouseEvent me = new java.awt.event.MouseEvent(dots,
                java.awt.event.MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(), 0,
                7, 9, 1, true, java.awt.event.MouseEvent.BUTTON3);
        try {
            dp.invoke(ov, me);
        } catch (InvocationTargetException ignored) {
        }
        JPopupMenu dm = (JPopupMenu) getField(ov, "dotsMenu");
        check("圆点菜单弹出来了", dm != null);
        List<String> ditems = menuTexts(dm);
        check("圆点菜单 3 项（" + ditems.size() + "）：" + ditems, ditems.size() == 3);

        /* 周一那天到底几条，按数据算，别写死（"四天前"和"周一"可能正好同一天） */
        Method ct = ovC.getDeclaredMethod("copyTodosOfDay", LocalDate.class, String.class);
        ct.setAccessible(true);
        int realN = (Integer) ct.invoke(ov, MONDAY, md(MONDAY));

        check("第三项写「复制 " + md(MONDAY) + " 那天的全部 " + realN + " 条」[" + ditems.get(2) + "]",
                ditems.get(2).equals("复制 " + md(MONDAY) + " 那天的全部 " + realN + " 条"));

        final JPopupMenu dmF = dm;
        SwingUtilities.invokeAndWait(() -> clickByText(dmF, "那天的全部"));
        Thread.sleep(300);
        String clipDay = clip();
        String[] dayLines = clipDay.split("\n");
        check("剪贴板条数等于那天条数（" + dayLines.length + "/" + realN + "）[" + clipDay + "]",
                dayLines.length == realN);
        boolean allDated = true;
        for (String s : dayLines) {
            if (!s.startsWith(md(MONDAY))) allDated = false;
        }
        check("每行都带日期 " + md(MONDAY) + "[" + clipDay.replace("\n", " | ") + "]", allDated);
        check("完成的那条带 ✓ 标记[" + clipDay.replace("\n", " | ") + "]", clipDay.contains("✓"));

        int c0 = (Integer) ct.invoke(ov, TODAY.plusDays(1), md(TODAY.plusDays(1)));
        check("明天只有「明天的事」一条（" + c0 + "）", c0 == 1);
        int c2 = (Integer) ct.invoke(ov, TODAY, md(TODAY));
        int expectToday = 4 + (MONDAY.equals(TODAY) ? 1 : 0);   // 周一时「周一做的」也算今天
        check("今天 " + expectToday + " 条（含无日期那条，跟清单一致）（" + c2 + "）", c2 == expectToday);
        check("今天的复制里含未完成的「今天的事」[" + clip().replace("\n", " | ") + "]",
                clip().contains(md(TODAY) + "   今天的事"));

        // ---------- 3) 小窗过期抬头：复制这一档 ----------
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
        mini = getField(mbC, null, "inst");
        check("小窗已创建", mini != null);
        setBool(mini, "overdueView", true);
        setBool(mini, "overdueShowAll", true);
        setInt(mini, "boardDay", 0);
        setInt(mini, "overdueAgeFilter", 0);
        invokeRefresh();

        JPanel head = headPanel();
        List<String> hitems = menuTexts(popupOf(head));
        check("抬头菜单 5 项（" + hitems.size() + "）：" + hitems, hitems.size() == 5);
        check("有「复制这 5 条原文」[" + hitems + "]", hitems.contains("复制这 5 条原文"));

        // 切到超 7 天那一档，文案里的数字跟着变成 2
        clickByText(popupOf(headPanel()), "只看超过 7 天的");
        invokeRefresh();
        List<String> h7 = menuTexts(popupOf(headPanel()));
        check("超 7 天档写「复制这 2 条原文」[" + h7 + "]", h7.contains("复制这 2 条原文"));

        Method cos = mbC.getDeclaredMethod("copyOverdueShown");
        cos.setAccessible(true);
        int n7 = (Integer) cos.invoke(mini);
        check("复制这一档返回 2 条（" + n7 + "）", n7 == 2);
        String c7 = clip();
        check("剪贴板两行、按日期排序[" + c7.replace("\n", " | ") + "]",
                c7.equals("十天前\n九天前没动"));

        // 回到全部档再复制，5 条都在
        clickByText(popupOf(headPanel()), "全部过期条目");
        invokeRefresh();
        int nAll = (Integer) cos.invoke(mini);
        check("全部档复制 5 条（" + nAll + "）", nAll == 5);
        String cAll = clip();
        check("五条原文都在[" + cAll.replace("\n", " | ") + "]",
                cAll.contains("十天前") && cAll.contains("昨天没做") && cAll.contains("四天前"));

        // ---------- 4) 小窗行右键：在备忘录中查找 ----------
        JPanel listBox = (JPanel) getField(mini, "listBox");
        Component row = listBox.getComponent(1);
        java.awt.event.MouseEvent rev = new java.awt.event.MouseEvent(row,
                java.awt.event.MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(), 0,
                20, 8, 1, true, java.awt.event.MouseEvent.BUTTON3);
        Method ptd = mbC.getDeclaredMethod("popupTodo", java.awt.event.MouseEvent.class,
                Component.class, String.class);
        ptd.setAccessible(true);
        try {
            ptd.invoke(mini, rev, row, "十天前");
        } catch (InvocationTargetException ignored) {
            // 行不在屏幕上，弹窗会抛异常，菜单对象这时已经建好
        }
        JPopupMenu rowMenu = (JPopupMenu) getField(mini, "lastTodoMenu");
        check("行右键菜单弹出来了", rowMenu != null);
        List<String> ritems = menuTexts(rowMenu);
        check("行菜单 6 项（" + ritems.size() + "）：" + ritems, ritems.size() == 6);
        check("有「在备忘录中查找」", ritems.contains("在备忘录中查找"));
        check("原有的「设为今天」还在", ritems.contains("设为今天"));
        check("原有的「删除这条」还在", ritems.contains("删除这条"));

        final JPopupMenu rmF = rowMenu;
        SwingUtilities.invokeAndWait(() -> clickByText(rmF, "在备忘录中查找"));
        Thread.sleep(600);
        Object memo = pageM.invoke(main, Class.forName("com.toolbox.pc.MemoPanel"));
        check("已经切到备忘录页", memo != null);
        JTextField sf = (JTextField) getField(memo, "searchField");
        check("备忘录搜索框填的是这条待办[" + (sf == null ? "null" : sf.getText()) + "]",
                sf != null && "十天前".equals(sf.getText()));
        check("当前导航键是 memo[" + getField(main, "currentKey") + "]",
                "memo".equals(getField(main, "currentKey")));

        // ---------- 出图 ----------
        String out = "C:\\Users\\chen\\Desktop\\ui-253-month-copy.png";
        shot(monthCard, out, 360, 100);
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

    static Object getField(Object o, String n) throws Exception {
        Field f = o.getClass().getDeclaredField(n);
        f.setAccessible(true);
        return f.get(o);
    }

    static Object getField(Class<?> c, Object o, String n) throws Exception {
        Field f = c.getDeclaredField(n);
        f.setAccessible(true);
        return f.get(o);
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
