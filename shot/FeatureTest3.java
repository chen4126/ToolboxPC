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

/** v1.23 三项验证：记账小计 / 小窗已提醒 / 趋势折线按分类 */
public class FeatureTest3 {

    private static boolean ok = true;

    public static void main(String[] args) throws Exception {
        try {
            Theme.init();
            SwingUtilities.invokeAndWait(() -> com.toolbox.pc.Main.main(new String[0]));
            Thread.sleep(2500);

            /* ---------- 1. 小窗刚过点的条目标注“已提醒” ---------- */
            long justPast = System.currentTimeMillis() - 2L * 60000L;
            Store.writeLines("schedule.txt", java.util.Arrays.asList(
                    "99\t两分钟前那条\t" + justPast + "\tnone"));

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
            Container box = (Container) listF.get(mb);
            JLabel noted = findContain(box, "已提醒");
            System.out.println("已提醒行: " + (noted == null ? "无" : noted.getText()
                    + " fg=" + Integer.toHexString(noted.getForeground().getRGB())));
            check("小窗刚过点标已提醒", noted != null && noted.getText().contains("已提醒")
                    && noted.getForeground().getRGB() == 0xFF94A3B8);

            /* ---------- 2. 记账页筛选时底部合计变该类小计 ---------- */
            LedgerPanel lp = (LedgerPanel) findPanel("com.toolbox.pc.LedgerPanel");
            if (lp == null) throw new RuntimeException("找不到 LedgerPanel");
            Field catsF = LedgerPanel.class.getDeclaredField("monthCatsBox");
            catsF.setAccessible(true);
            JPanel catBox = (JPanel) catsF.get(lp);
            JLabel shop = findText(catBox, "购物");
            if (shop != null) {
                JLabel s = shop;
                SwingUtilities.invokeAndWait(() -> s.dispatchEvent(new MouseEvent(s,
                        MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 5, 5, 1, false,
                        MouseEvent.BUTTON1)));
                Thread.sleep(500);
            }
            Field statF = LedgerPanel.class.getDeclaredField("statLabel");
            statF.setAccessible(true);
            JLabel stat = (JLabel) statF.get(lp);
            String txt = stat.getText();
            System.out.println("底部合计=" + txt);
            check("底部合计变该类小计", txt.contains("购物 小计 ¥480.00")
                    && txt.contains("4 笔") && txt.contains("全部支出"));

            /* ---------- 3. 统计页趋势折线按分类 ---------- */
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
            Thread.sleep(700);

            Field trendF = StatsPanel.class.getDeclaredField("trend");
            trendF.setAccessible(true);
            JPanel trend = (JPanel) trendF.get(st);
            Field tF = trend.getClass().getDeclaredField("title");
            tF.setAccessible(true);
            Field vF = trend.getClass().getDeclaredField("values");
            vF.setAccessible(true);
            Field lF = trend.getClass().getDeclaredField("labels");
            lF.setAccessible(true);
            String title = (String) tF.get(trend);
            double[] vals = (double[]) vF.get(trend);
            String[] labs = (String[]) lF.get(trend);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < vals.length; i++) sb.append(labs[i]).append("=").append((int) vals[i]).append(" ");
            System.out.println("趋势标题=" + title);
            System.out.println("趋势数据=" + sb);
            check("折线只画该类", title.contains("购物")
                    && Math.abs(vals[vals.length - 1] - 360) < 0.5);

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

    private static JLabel findContain(Container c, String text) {
        for (Component k : c.getComponents()) {
            if (k instanceof JLabel && ((JLabel) k).getText() != null
                    && ((JLabel) k).getText().contains(text)) return (JLabel) k;
            if (k instanceof Container) {
                JLabel r = findContain((Container) k, text);
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
