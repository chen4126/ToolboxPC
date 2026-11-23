import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** v1.72：多选右键一次勾上过期 / 小窗过期抬头带最久天数 / 统计页近三周对比 */
public class FeatureTest56 {

    static int pass = 0, fail = 0;
    static Object ov, st, mini, main;
    static final LocalDate TODAY = LocalDate.now();

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

        // ---------- 1) 多选按钮右键 ----------
        JPopupMenu bm = null;
        Object bulkBtn = findLabel(ov, "多选");
        check("找到「多选」按钮", bulkBtn != null);
        if (bulkBtn instanceof JComponent) bm = ((JComponent) bulkBtn).getComponentPopupMenu();
        check("多选按钮挂了右键菜单", bm != null);
        final JPopupMenu bmF = bm;
        List<String> items = new ArrayList<>();
        if (bm != null) {
            for (Component x : bm.getComponents()) if (x instanceof JMenuItem) items.add(((JMenuItem) x).getText());
        }
        check("菜单 4 项（" + items.size() + "）：" + items, items.size() == 4);
        check("有「全选过期这几条」", items.contains("全选过期这几条"));
        check("有「只勾今天没做的」", items.contains("只勾今天没做的"));
        check("有「清空已选」", items.contains("清空已选"));
        check("有「退出多选」", items.contains("退出多选"));

        // 点「全选过期这几条」→ 勾上 5 条过期
        SwingUtilities.invokeAndWait(() -> clickItem(bmF, "全选过期这几条"));
        Thread.sleep(400);
        check("点了之后进了多选态", getBool(ov, "multiSelect"));
        check("勾上了 5 条过期（" + pickedSize() + "）", pickedSize() == 5);
        check("按钮文案变成「退出多选」[" + labelText(bulkBtn) + "]",
                labelText(bulkBtn).contains("退出多选"));

        // 清空
        SwingUtilities.invokeAndWait(() -> clickItem(bmF, "清空已选"));
        Thread.sleep(400);
        check("清空后一条不剩（" + pickedSize() + "）", pickedSize() == 0);

        // 只勾今天没做的 → 今天只有「今天的事」1 条未完成
        SwingUtilities.invokeAndWait(() -> clickItem(bmF, "只勾今天没做的"));
        Thread.sleep(400);
        check("只勾今天 → 1 条（" + pickedSize() + "）", pickedSize() == 1);
        String only = pickedTexts();
        check("勾的是「今天的事」[" + only + "]", only.contains("今天的事"));

        SwingUtilities.invokeAndWait(() -> clickItem(bmF, "退出多选"));
        Thread.sleep(400);
        check("退出多选后回到普通态", !getBool(ov, "multiSelect"));
        check("退出后勾选清空（" + pickedSize() + "）", pickedSize() == 0);

        // ---------- 2) 小窗过期抬头带最久天数 ----------
        MiniBoard.show();
        Thread.sleep(800);
        Class<?> mbC = Class.forName("com.toolbox.pc.MiniBoard");
        Field instF = mbC.getDeclaredField("inst");
        instF.setAccessible(true);
        mini = instF.get(null);
        check("小窗已创建", mini != null);
        setBool(mini, "overdueView", true);
        setBool(mini, "overdueShowAll", true);
        setInt(mini, "boardDay", 0);
        Method rf = mbC.getDeclaredMethod("refresh");
        rf.setAccessible(true);
        SwingUtilities.invokeAndWait(() -> {
            try {
                rf.invoke(mini);
            } catch (Exception ignored) {
            }
        });
        Thread.sleep(500);

        JPanel listBox = (JPanel) getField(mini, "listBox");
        check("小窗列表有内容（" + listBox.getComponentCount() + "）", listBox.getComponentCount() > 0);
        JPanel head = (JPanel) listBox.getComponent(0);
        String headTxt = firstLabelText(head);
        check("抬头写「过期 5 条 · 最久 10 天」[" + headTxt + "]",
                headTxt.contains("过期 5 条") && headTxt.contains("最久 10 天"));
        // 抬头的行宽够时保持完整文案
        head.setSize(214, 30);
        head.doLayout();
        head.dispatchEvent(new java.awt.event.ComponentEvent(head,
                java.awt.event.ComponentEvent.COMPONENT_RESIZED));
        Thread.sleep(300);
        check("宽度 214 时仍写完整版 [" + firstLabelText(head) + "]",
                firstLabelText(head).contains("最久 10 天"));
        // 没有最久数据时退回只写条数
        Method th = mbC.getDeclaredMethod("todoHead", LocalDate.class, int.class, int.class);
        th.setAccessible(true);
        JPanel headShort = (JPanel) th.invoke(mini, LocalDate.now(), 5, 0);
        check("最久算不出来时退回「过期未做 · 5 条」[" + firstLabelText(headShort) + "]",
                firstLabelText(headShort).contains("过期未做") && firstLabelText(headShort).contains("5 条"));

        String firstLine = listBox.getComponentCount() > 1 ? allLabels(listBox.getComponent(1)) : "";
        check("最久的一条排在最前（" + md(TODAY.minusDays(10)) + "）[" + firstLine + "]", firstLine.contains(md(TODAY.minusDays(10))));
        String secondLine = listBox.getComponentCount() > 2 ? allLabels(listBox.getComponent(2)) : "";
        check("第二条是 " + md(TODAY.minusDays(9)) + " [" + secondLine + "]", secondLine.contains(md(TODAY.minusDays(9))));
        String fifthLine = listBox.getComponentCount() > 5 ? allLabels(listBox.getComponent(5)) : "";
        check("第五条是 " + md(TODAY.minusDays(1)) + "（最晚的一条压在最末）[" + fifthLine + "]", fifthLine.contains(md(TODAY.minusDays(1))));

        // ---------- 3) 统计页近三周对比 ----------
        Field wcF = st.getClass().getDeclaredField("todoWeekCard");
        wcF.setAccessible(true);
        Object wcard = wcF.get(st);
        JPopupMenu wm = wcard instanceof JComponent ? ((JComponent) wcard).getComponentPopupMenu() : null;
        List<String> witems = new ArrayList<>();
        if (wm != null) for (Component x : wm.getComponents()) if (x instanceof JMenuItem) witems.add(((JMenuItem) x).getText());
        check("本周卡右键 3 项（" + witems.size() + "）：" + witems, witems.size() == 3);
        check("有「看近三周对比」", witems.contains("看近三周对比"));

        Method btw = st.getClass().getDeclaredMethod("buildThreeWeekPanel");
        btw.setAccessible(true);
        JPanel box = (JPanel) btw.invoke(st);
        check("三周面板不为空", box != null);
        List<String> cells = new ArrayList<>();
        if (box != null) for (Component x : box.getComponents()) if (x instanceof JLabel) cells.add(((JLabel) x).getText());
        check("面板 20 格（4 行 × 5 列）[" + cells.size() + "]", cells.size() == 20);
        if (cells.size() == 20) {
            check("表头 [" + cells.subList(0, 5) + "]",
                    cells.get(0).equals("周次") && cells.get(1).equals("完成")
                            && cells.get(2).equals("天数") && cells.get(3).equals("最勤")
                            && cells.get(4).equals("环比"));
            LocalDate mon = Seed.monday();
            check("上上周 1 条 / 1 天 / " + md(mon.minusDays(12)) + "（1） / — [" + cells.subList(5, 10) + "]",
                    cells.get(5).equals("上上周") && cells.get(6).equals("1 条")
                            && cells.get(7).equals("1 天") && cells.get(8).equals(md(mon.minusDays(12)) + "（1）")
                            && cells.get(9).equals("—"));
            check("上周 2 条 / 2 天 / " + md(mon.minusDays(6)) + "（1） / +1 [" + cells.subList(10, 15) + "]",
                    cells.get(10).equals("上周") && cells.get(11).equals("2 条")
                            && cells.get(12).equals("2 天") && cells.get(13).equals(md(mon.minusDays(6)) + "（1）")
                            && cells.get(14).equals("+1"));
            check("本周 3 条 / " + Seed.weekDays() + " 天 / " + md(TODAY) + "（" + Seed.todayDone() + "） / +1 ["
                            + cells.subList(15, 20) + "]",
                    cells.get(15).equals("本周") && cells.get(16).equals("3 条")
                            && cells.get(17).equals(Seed.weekDays() + " 天")
                            && cells.get(18).equals(md(TODAY) + "（" + Seed.todayDone() + "）")
                            && cells.get(19).equals("+1"));
        }

        // ---------- 出图 ----------
        if (box != null) {
            shot(box, "C:\\Users\\chen\\Desktop\\ui-251-three-week.png", 340, 120);
            System.out.println("SHOT " + new File("C:\\Users\\chen\\Desktop\\ui-251-three-week.png").length());
        }

        System.out.println(fail == 0 ? ("ALL PASS (" + pass + ")") : ("SOME FAIL " + fail + "/" + (pass + fail)));
        Runtime.getRuntime().halt(fail == 0 ? 0 : 1);
    }

    static void clickItem(JPopupMenu m, String text) {
        if (m == null) return;
        for (Component x : m.getComponents()) {
            if (x instanceof JMenuItem && text.equals(((JMenuItem) x).getText())) {
                ((JMenuItem) x).doClick();
                return;
            }
        }
    }

    static Object findLabel(Object panel, String text) throws Exception {
        java.util.List<Component> all = new ArrayList<>();
        collect((Component) panel, all);
        for (Component c : all) {
            if (c instanceof JLabel && text.equals(((JLabel) c).getText())) return c;
        }
        return null;
    }

    static void collect(Component c, List<Component> out) {
        out.add(c);
        if (c instanceof Container) for (Component k : ((Container) c).getComponents()) collect(k, out);
    }

    static String firstLabelText(JPanel head) {
        for (Component c : head.getComponents()) {
            Component hit = c;
            if (c instanceof JPanel) {
                for (Component k : ((JPanel) c).getComponents()) return labelTextOf(k);
            }
            if (hit instanceof JLabel) return ((JLabel) hit).getText();
        }
        return "";
    }

    static String allLabels(Component c) {
        java.util.List<Component> all = new ArrayList<>();
        collect(c, all);
        StringBuilder sb = new StringBuilder();
        for (Component k : all) if (k instanceof JLabel) sb.append(((JLabel) k).getText()).append(' ');
        return sb.toString().trim();
    }

    static String labelTextOf(Component c) {
        if (c instanceof JLabel) return ((JLabel) c).getText();
        if (c instanceof Container) {
            java.util.List<Component> all = new ArrayList<>();
            collect((Component) c, all);
            for (Component k : all) if (k instanceof JLabel) return ((JLabel) k).getText();
        }
        return "";
    }

    static String labelText(Object l) {
        return l instanceof JLabel ? ((JLabel) l).getText() : "";
    }

    static int pickedSize() throws Exception {
        Field f = ov.getClass().getDeclaredField("picked");
        f.setAccessible(true);
        return ((java.util.Set<?>) f.get(ov)).size();
    }

    static String pickedTexts() throws Exception {
        Field f = ov.getClass().getDeclaredField("picked");
        f.setAccessible(true);
        StringBuilder sb = new StringBuilder();
        for (Object o : (java.util.Set<?>) f.get(ov)) {
            Field tf = o.getClass().getDeclaredField("text");
            tf.setAccessible(true);
            sb.append(tf.get(o)).append(' ');
        }
        return sb.toString().trim();
    }

    static Object getField(Object o, String n) throws Exception {
        Field f = o.getClass().getDeclaredField(n);
        f.setAccessible(true);
        return f.get(o);
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
