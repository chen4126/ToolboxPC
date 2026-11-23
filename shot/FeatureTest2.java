import com.toolbox.pc.LedgerPanel;
import com.toolbox.pc.MiniBoard;
import com.toolbox.pc.StatsPanel;
import com.toolbox.pc.Store;
import com.toolbox.pc.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** v1.22 三项验证：小窗 5 分钟变黄 / 记账按分类筛选 / 统计筛选显示本月合计 */
public class FeatureTest2 {

    private static boolean ok = true;

    public static void main(String[] args) throws Exception {
        try {
            Theme.init();
            SwingUtilities.invokeAndWait(() -> com.toolbox.pc.Main.main(new String[0]));
            Thread.sleep(2500);

            /* ---------- 1. 小窗 5 分钟内整行变黄 ---------- */
            long soon = System.currentTimeMillis() + 3L * 60000L;
            Store.writeLines("schedule.txt", java.util.Arrays.asList("99\t三分钟后开始\t" + soon + "\tnone"));

            Method show = MiniBoard.class.getDeclaredMethod("show");
            show.setAccessible(true);
            SwingUtilities.invokeAndWait(() -> {
                try {
                    show.invoke(null);
                } catch (Exception ignored) {
                }
            });
            Thread.sleep(900);
            Field instF = MiniBoard.class.getDeclaredField("inst");
            instF.setAccessible(true);
            Object mb = instF.get(null);
            Method rf = MiniBoard.class.getDeclaredMethod("refresh");
            rf.setAccessible(true);
            SwingUtilities.invokeAndWait(() -> {
                try {
                    rf.invoke(mb);
                } catch (Exception ignored) {
                }
            });
            Thread.sleep(600);

            Field listF = MiniBoard.class.getDeclaredField("listBox");
            listF.setAccessible(true);
            JComponent box = (JComponent) listF.get(mb);
            JLabel yellow = findYellow(box);
            System.out.println("黄色行: " + (yellow == null ? "无" : yellow.getText()
                    + " bg=" + Integer.toHexString(yellow.getBackground().getRGB())));
            check("5 分钟内整行变黄", yellow != null
                    && yellow.getText().contains("3 分钟后")
                    && yellow.getBackground().getRGB() == 0xFFFEF3C7);

            /* ---------- 2. 记账页按分类筛选 ---------- */
            LedgerPanel lp = (LedgerPanel) findPanel("com.toolbox.pc.LedgerPanel");
            if (lp == null) throw new RuntimeException("找不到 LedgerPanel");
            Field linesF = LedgerPanel.class.getDeclaredField("lines");
            linesF.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<String> lines = (List<String>) linesF.get(lp);

            Field catsF = LedgerPanel.class.getDeclaredField("monthCatsBox");
            catsF.setAccessible(true);
            JPanel catBox = (JPanel) catsF.get(lp);
            System.out.println("分类行数量: " + catBox.getComponentCount());

            JLabel shop = findText(catBox, "购物");
            System.out.println("找到分类标签: " + (shop == null ? "无" : shop.getText()));
            if (shop != null) {
                JLabel s = shop;
                SwingUtilities.invokeAndWait(() -> s.dispatchEvent(new MouseEvent(s,
                        MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 5, 5, 1, false,
                        MouseEvent.BUTTON1)));
                Thread.sleep(500);
            }

            Field fcF = LedgerPanel.class.getDeclaredField("filterCat");
            fcF.setAccessible(true);
            Field modelF = LedgerPanel.class.getDeclaredField("model");
            modelF.setAccessible(true);
            javax.swing.table.DefaultTableModel model =
                    (javax.swing.table.DefaultTableModel) modelF.get(lp);
            Field barF = LedgerPanel.class.getDeclaredField("filterBar");
            barF.setAccessible(true);
            JLabel bar = (JLabel) barF.get(lp);

            String cat = (String) fcF.get(lp);
            System.out.println("筛选后 filterCat=" + cat + " 表格行数=" + model.getRowCount()
                    + " 提示=" + bar.getText() + " 可见=" + bar.isVisible());
            check("记账筛选只留该类", "购物".equals(cat) && model.getRowCount() == 4
                    && bar.isVisible() && bar.getText().contains("已筛选：购物"));

            Field r2iF = LedgerPanel.class.getDeclaredField("rowToIndex");
            r2iF.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<Integer> r2i = (List<Integer>) r2iF.get(lp);
            int idx0 = r2i.get(0);
            String[] p0 = lines.get(idx0).split("\t", -1);
            System.out.println("首行映射下标=" + idx0 + " 分类=" + (p0.length > 4 ? p0[4] : ""));
            check("筛选后行号映射正确", p0.length > 4 && "购物".equals(p0[4].trim()));

            // 再点一次取消
            if (shop != null) {
                JLabel s2 = shop;
                SwingUtilities.invokeAndWait(() -> s2.dispatchEvent(new MouseEvent(s2,
                        MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 5, 5, 1, false,
                        MouseEvent.BUTTON1)));
                Thread.sleep(500);
            }
            String cat2 = (String) fcF.get(lp);
            System.out.println("再点后 filterCat=" + cat2 + " 表格行数=" + model.getRowCount()
                    + " 提示可见=" + bar.isVisible());
            check("再点一次取消筛选", cat2 == null && model.getRowCount() == lines.size()
                    && !bar.isVisible());

            /* ---------- 3. 统计页筛选状态显示本月合计 ---------- */
            StatsPanel st = (StatsPanel) findPanel("com.toolbox.pc.StatsPanel");
            if (st == null) throw new RuntimeException("找不到 StatsPanel");
            Field donutF = StatsPanel.class.getDeclaredField("donut");
            donutF.setAccessible(true);
            final Component dc = (Component) donutF.get(st);
            dc.setSize(290, 112);
            dc.doLayout();
            SwingUtilities.invokeAndWait(() -> dc.dispatchEvent(new MouseEvent(dc,
                    MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 71, 48, 1, false,
                    MouseEvent.BUTTON1)));
            Thread.sleep(600);

            Field wiF = StatsPanel.class.getDeclaredField("weekInfo");
            wiF.setAccessible(true);
            JLabel wi = (JLabel) wiF.get(st);
            String html = wi.getText();
            System.out.println("weekInfo=" + html);
            check("统计页显示该类本月合计",
                    html.contains("已筛选 购物") && html.contains("本月 <b>¥360.00</b>（3 笔）"));

            System.out.println(ok ? "ALL PASS" : "HAS FAIL");
            System.exit(ok ? 0 : 1);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(2);
        }
    }

    private static void check(String name, boolean pass) {
        System.out.println((pass ? "[PASS] " : "[FAIL] ") + name);
        if (!pass) ok = false;
    }

    private static JLabel findYellow(Container c) {
        for (Component k : c.getComponents()) {
            if (k instanceof JLabel) {
                JLabel l = (JLabel) k;
                if (l.isOpaque() && l.getBackground() != null
                        && l.getBackground().getRGB() == 0xFFFEF3C7) return l;
            }
            if (k instanceof Container) {
                JLabel r = findYellow((Container) k);
                if (r != null) return r;
            }
        }
        return null;
    }

    private static JLabel findText(Container c, String text) {
        for (Component k : c.getComponents()) {
            if (k instanceof JLabel && text.equals(((JLabel) k).getText())) return (JLabel) k;
            if (k instanceof Container) {
                JLabel r = findText((Container) k, text);
                if (r != null) return r;
            }
        }
        return null;
    }

    private static JPanel findPanel(String cls) {
        for (Frame f : Frame.getFrames()) {
            JPanel p = scan(f, cls);
            if (p != null) return p;
        }
        return null;
    }

    private static JPanel scan(Container c, String cls) {
        for (Component k : c.getComponents()) {
            if (cls.equals(k.getClass().getName())) return (JPanel) k;
            if (k instanceof Container) {
                JPanel r = scan((Container) k, cls);
                if (r != null) return r;
            }
        }
        return null;
    }
}
