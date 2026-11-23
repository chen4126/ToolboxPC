import com.toolbox.pc.CountDownPanel;
import com.toolbox.pc.MemoPanel;
import com.toolbox.pc.OverviewPanel;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** v1.24 三项验证：概览右栏最近记账 / 备忘搜索高亮 / 倒计时超时计时 */
public class FeatureTest4 {

    private static boolean ok = true;

    public static void main(String[] args) throws Exception {
        try {
            com.toolbox.pc.Theme.init();
            SwingUtilities.invokeAndWait(() -> com.toolbox.pc.Main.main(new String[0]));
            Thread.sleep(2500);

            /* ---------- 1. 概览页右栏“最近记账” ---------- */
            OverviewPanel ov = (OverviewPanel) findPanel("com.toolbox.pc.OverviewPanel");
            if (ov == null) throw new RuntimeException("找不到 OverviewPanel");
            Method rf = OverviewPanel.class.getDeclaredMethod("refresh");
            rf.setAccessible(true);
            SwingUtilities.invokeAndWait(() -> {
                try {
                    rf.invoke(ov);
                } catch (Exception ignored) {
                }
            });
            Thread.sleep(500);
            Field rbF = OverviewPanel.class.getDeclaredField("recentBox");
            rbF.setAccessible(true);
            JPanel rb = (JPanel) rbF.get(ov);
            Field footF = OverviewPanel.class.getDeclaredField("recentFoot");
            footF.setAccessible(true);
            JLabel foot = (JLabel) footF.get(ov);
            System.out.println("最近记账行数=" + rb.getComponentCount() + " 底部=" + foot.getText());
            check("概览右栏显示最近记账", rb.getComponentCount() == 5
                    && foot.getText().contains("本月支出 ¥570.00") && foot.getText().contains("7 笔"));

            /* ---------- 2. 备忘搜索高亮 ---------- */
            MemoPanel mp = (MemoPanel) findPanel("com.toolbox.pc.MemoPanel");
            if (mp == null) throw new RuntimeException("找不到 MemoPanel");
            Method hl = MemoPanel.class.getDeclaredMethod("highlight", String.class, Color.class);
            hl.setAccessible(true);
            String out = (String) hl.invoke(mp, "周五前把预算表发给财务", Color.BLACK);
            System.out.println("未搜索=" + out);

            Field qF = MemoPanel.class.getDeclaredField("query");
            qF.setAccessible(true);
            qF.set(mp, "预算");
            String hot = (String) hl.invoke(mp, "周五前把预算表发给财务", Color.BLACK);
            System.out.println("搜索“预算”=" + hot);
            qF.set(mp, "boss");
            String none = (String) hl.invoke(mp, "目标BOSS复盘", Color.BLACK);
            System.out.println("大小写不敏感=" + none);
            check("命中的关键字黄底高亮", hot.contains("background:#FEF3C7")
                    && hot.contains(">预算</span>") && none.contains(">BOSS</span>"));
            check("未搜索时不加 HTML", out.equals("周五前把预算表发给财务"));

            // 渲染器出来的文本也要带高亮
            qF.set(mp, "预算");
            Method reload = MemoPanel.class.getDeclaredMethod("reloadList");
            reload.setAccessible(true);
            SwingUtilities.invokeAndWait(() -> {
                try {
                    reload.invoke(mp);
                } catch (Exception ignored) {
                }
            });
            Field listF = MemoPanel.class.getDeclaredField("list");
            listF.setAccessible(true);
            JList<?> list = (JList<?>) listF.get(mp);
            System.out.println("搜索结果条数=" + list.getModel().getSize());
            JList rawList = list;
            Component cell = rawList.getCellRenderer().getListCellRendererComponent(
                    rawList, rawList.getModel().getElementAt(0), 0, false, false);
            JLabel txt = findHtmlLabel(cell);
            System.out.println("单元格文本=" + (txt == null ? "无" : txt.getText()));
            check("列表单元格带高亮", txt != null && txt.getText().contains("background:#FEF3C7"));
            qF.set(mp, "");

            /* ---------- 3. 倒计时到点后继续计时 ---------- */
            CountDownPanel cd = (CountDownPanel) findPanel("com.toolbox.pc.CountDownPanel");
            if (cd == null) throw new RuntimeException("找不到 CountDownPanel");
            Field hs = CountDownPanel.class.getDeclaredField("hourSp");
            hs.setAccessible(true);
            Field ms = CountDownPanel.class.getDeclaredField("minSp");
            ms.setAccessible(true);
            Field ss = CountDownPanel.class.getDeclaredField("secSp");
            ss.setAccessible(true);
            SwingUtilities.invokeAndWait(() -> {
                try {
                    ((JSpinner) hs.get(cd)).setValue(0);
                    ((JSpinner) ms.get(cd)).setValue(0);
                    ((JSpinner) ss.get(cd)).setValue(1);
                    Method st = CountDownPanel.class.getDeclaredMethod("start");
                    st.setAccessible(true);
                    st.invoke(cd);
                } catch (Exception ignored) {
                }
            });
            Thread.sleep(2600);   // 1 秒到点后再跑 1.6 秒超时

            Field overF = CountDownPanel.class.getDeclaredField("over");
            overF.setAccessible(true);
            Field overMsF = CountDownPanel.class.getDeclaredField("overMs");
            overMsF.setAccessible(true);
            Field tlF = CountDownPanel.class.getDeclaredField("timeLabel");
            tlF.setAccessible(true);
            Field stF = CountDownPanel.class.getDeclaredField("statusLabel");
            stF.setAccessible(true);
            Field sbF = CountDownPanel.class.getDeclaredField("startBtn");
            sbF.setAccessible(true);
            boolean over = (Boolean) overF.get(cd);
            long overMs = (Long) overMsF.get(cd);
            JLabel tl = (JLabel) tlF.get(cd);
            JLabel sl = (JLabel) stF.get(cd);
            JButton sb = (JButton) sbF.get(cd);
            System.out.println("over=" + over + " overMs=" + overMs + " 显示=" + tl.getText()
                    + " 状态=" + sl.getText() + " 按钮=" + sb.getText());
            check("到点后继续超时计时", over && overMs >= 1500
                    && tl.getText().startsWith("+") && sl.getText().contains("已到点")
                    && "停止".equals(sb.getText()));

            // 停止后归零
            SwingUtilities.invokeAndWait(() -> sb.doClick());
            Thread.sleep(400);
            boolean over2 = (Boolean) overF.get(cd);
            JLabel tl2 = (JLabel) tlF.get(cd);
            System.out.println("停止后 over=" + over2 + " 显示=" + tl2.getText());
            check("停止后归零", !over2 && "00:00:00".equals(tl2.getText()));

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

    private static JLabel findHtmlLabel(Component c) {
        if (c instanceof JLabel) {
            String s = ((JLabel) c).getText();
            if (s != null && s.contains("<html")) return (JLabel) c;
        }
        if (c instanceof Container) {
            for (Component k : ((Container) c).getComponents()) {
                JLabel r = findHtmlLabel(k);
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
