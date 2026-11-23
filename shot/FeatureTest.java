import com.toolbox.pc.Alerts;
import com.toolbox.pc.MiniBoard;
import com.toolbox.pc.SchedulePanel;
import com.toolbox.pc.StatsPanel;
import com.toolbox.pc.Store;
import com.toolbox.pc.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** v1.21 三项功能的自动化验证：提前 5 分钟预告 / 环形图分类筛选 / 小窗待办打勾 */
public class FeatureTest {

    private static boolean ok = true;

    public static void main(String[] args) throws Exception {
        try {
            Theme.init();
            SwingUtilities.invokeAndWait(() -> com.toolbox.pc.Main.main(new String[0]));
            Thread.sleep(2500);

            /* ---------- 1. 提前 5 分钟预告 ---------- */
            SchedulePanel sp = (SchedulePanel) findPanel("com.toolbox.pc.SchedulePanel");
            if (sp == null) throw new RuntimeException("找不到 SchedulePanel");

            Field itemsF = SchedulePanel.class.getDeclaredField("items");
            itemsF.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<Object> items = (List<Object>) itemsF.get(sp);
            items.clear();

            Class<?> ic = Class.forName("com.toolbox.pc.SchedulePanel$Item");
            java.lang.reflect.Constructor<?> ctor = ic.getDeclaredConstructor();
            ctor.setAccessible(true);
            Object it = ctor.newInstance();
            setField(ic, it, "id", 99);
            setField(ic, it, "title", "提前预告测试");
            setField(ic, it, "time", System.currentTimeMillis() + 4L * 60000L);
            setField(ic, it, "rule", "none");
            items.add(it);

            Method check = SchedulePanel.class.getDeclaredMethod("check");
            check.setAccessible(true);
            check.invoke(sp);

            Field lpF = Alerts.class.getDeclaredField("lastPre");
            lpF.setAccessible(true);
            String pre1 = (String) lpF.get(null);
            System.out.println("预告文案: " + pre1);

            Field preF = SchedulePanel.class.getDeclaredField("preAired");
            preF.setAccessible(true);
            java.util.Set<Long> preAired = castSet(preF.get(sp));
            System.out.println("preAired 数量: " + preAired.size());

            check.invoke(sp);
            System.out.println("二次调用后 preAired 数量: " + preAired.size());
            check("提前预告只发一次", preAired.size() == 1
                    && pre1.contains("提前预告测试") && pre1.contains("分钟后开始"));

            setField(ic, it, "time", System.currentTimeMillis() - 1000L);
            check.invoke(sp);
            System.out.println("到点后 preAired 数量: " + preAired.size() + " items=" + items.size());
            check("到点后清空预告标记", preAired.isEmpty());

            /* ---------- 2. 环形图分类筛选 ---------- */
            StatsPanel st = (StatsPanel) findPanel("com.toolbox.pc.StatsPanel");
            if (st == null) throw new RuntimeException("找不到 StatsPanel");
            Field donutF = StatsPanel.class.getDeclaredField("donut");
            donutF.setAccessible(true);
            final Component dc = (Component) donutF.get(st);
            dc.setSize(290, 112);
            dc.doLayout();

            Field sfF = StatsPanel.class.getDeclaredField("spendFilter");
            sfF.setAccessible(true);
            Field expF = StatsPanel.class.getDeclaredField("expChart");
            expF.setAccessible(true);
            Object exp = expF.get(st);
            Field ttF = exp.getClass().getDeclaredField("title");
            ttF.setAccessible(true);
            Field valsF = exp.getClass().getDeclaredField("values");
            valsF.setAccessible(true);

            // 圆心 (51,68)、环中半径 28，45° 方向落在金额最大的那一类
            SwingUtilities.invokeAndWait(() -> {
                MouseEvent e = new MouseEvent(dc, MouseEvent.MOUSE_CLICKED,
                        System.currentTimeMillis(), 0, 71, 48, 1, false, MouseEvent.BUTTON1);
                dc.dispatchEvent(e);
            });
            Thread.sleep(500);

            String filter = (String) sfF.get(st);
            String title = (String) ttF.get(exp);
            int[] vals = (int[]) valsF.get(exp);
            System.out.println("点击后筛选=" + filter + " 标题=" + title + " 柱值和=" + sum(vals));
            check("筛选生效且柱图只含该类", "购物".equals(filter)
                    && title.startsWith("购物 · ") && sum(vals) == 120);

            SwingUtilities.invokeAndWait(() -> {
                MouseEvent e = new MouseEvent(dc, MouseEvent.MOUSE_CLICKED,
                        System.currentTimeMillis(), 0, 71, 48, 1, false, MouseEvent.BUTTON1);
                dc.dispatchEvent(e);
            });
            Thread.sleep(500);
            String f2 = (String) sfF.get(st);
            int[] vals2 = (int[]) valsF.get(exp);
            System.out.println("再次点击后筛选=" + f2 + " 柱值和=" + sum(vals2));
            check("再点一次取消筛选", f2 == null && sum(vals2) == 330);

            /* ---------- 3. 小窗待办打勾 ---------- */
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
            Field listF = MiniBoard.class.getDeclaredField("listBox");
            listF.setAccessible(true);
            JComponent box = (JComponent) listF.get(mb);

            final JLabel target = findTodo(box);
            System.out.println("待办标签: " + (target == null ? "无" : target.getText()));
            if (target != null) {
                SwingUtilities.invokeAndWait(() -> {
                    MouseEvent e = new MouseEvent(target, MouseEvent.MOUSE_CLICKED,
                            System.currentTimeMillis(), 0, 5, 5, 1, false, MouseEvent.BUTTON1);
                    target.dispatchEvent(e);
                });
                Thread.sleep(700);
                String after = Store.readText("todo.txt");
                System.out.println("todo.txt: " + after.replace("\n", " | "));
                check("小窗打勾写入已完成", after.contains("1\t写周报"));
                JLabel again = findTodo(box);
                System.out.println("刷新后首个待办: " + (again == null ? "无" : again.getText()));
                check("打勾后该条从小窗消失",
                        again == null || !again.getText().contains("写周报"));
            } else {
                check("找到小窗待办行", false);
            }

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

    private static int sum(int[] a) {
        int s = 0;
        for (int v : a) s += v;
        return s;
    }

    private static JLabel findTodo(Container c) {
        for (Component k : c.getComponents()) {
            if (k instanceof JLabel) {
                String s = ((JLabel) k).getText();
                if (s != null && s.startsWith("○ ")) return (JLabel) k;
            }
            if (k instanceof Container) {
                JLabel r = findTodo((Container) k);
                if (r != null) return r;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static java.util.Set<Long> castSet(Object o) {
        return (java.util.Set<Long>) o;
    }

    private static void setField(Class<?> c, Object obj, String name, Object v) throws Exception {
        Field f = c.getDeclaredField(name);
        f.setAccessible(true);
        f.set(obj, v);
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
