import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;

/** v1.35 三项验证：归档入口可翻回 / 小窗全部勾完 / 过期日期点一下只留那天 */
public class FeatureTest15 {

    private static boolean ok = true;

    public static void main(String[] args) throws Exception {
        try {
            Theme.init();
            SwingUtilities.invokeAndWait(() -> Main.main(new String[0]));
            Thread.sleep(2500);

            Class<?> mc = Class.forName("com.toolbox.pc.Main");
            Field selfF = mc.getDeclaredField("self");
            selfF.setAccessible(true);
            Field ff = mc.getDeclaredField("frame");
            ff.setAccessible(true);
            JFrame frame = (JFrame) ff.get(selfF.get(null));

            OverviewPanel ov = page(OverviewPanel.class);
            Method refresh = OverviewPanel.class.getDeclaredMethod("refresh");
            refresh.setAccessible(true);
            SwingUtilities.invokeAndWait(() -> {
                try {
                    refresh.invoke(ov);
                } catch (Exception ignored) {
                }
            });
            Thread.sleep(500);

            Field fbF = OverviewPanel.class.getDeclaredField("futureBar");
            fbF.setAccessible(true);
            JPanel bar = (JPanel) fbF.get(ov);

            /* ---------- 1. 归档入口 ---------- */
            String s1 = labels(bar);
            System.out.println("标签=" + s1);
            check("过期区按天列出 11-02 两条", s1.contains("很久没动|11-02 · 2 条|"));
            check("有归档入口且数量正确", s1.contains("已归档 1 条"));

            /* ---------- 2. 点过期日期只留那天 ---------- */
            JLabel chip = null;
            for (Component c : bar.getComponents()) {
                if (c instanceof JLabel && ((JLabel) c).getText().startsWith("11-02 · 2 条")) chip = (JLabel) c;
            }
            check("找到 11-02 的日期 chip", chip != null);
            click(chip);
            Thread.sleep(400);

            Field ofF = OverviewPanel.class.getDeclaredField("overdueFilter");
            ofF.setAccessible(true);
            Field boxF = OverviewPanel.class.getDeclaredField("todoBox");
            boxF.setAccessible(true);
            JPanel box = (JPanel) boxF.get(ov);
            Field tf = OverviewPanel.class.getDeclaredField("todoTitle");
            tf.setAccessible(true);
            JLabel title = (JLabel) tf.get(ov);
            String s2 = labels(box) + " 标题=" + title.getText();
            System.out.println("点完=" + s2);
            check("筛选生效只留这天的过期", LocalDate.of(2026, 11, 2).equals(ofF.get(ov))
                    && s2.contains("九天前没动") && s2.contains("九天前也拖了") && !s2.contains("今天的事"));
            check("标题写明是哪天的过期", title.getText().equals("11-02 过期 · 2 条"));

            /* 再点一次取消 */
            JLabel chip2 = null;
            for (Component c : bar.getComponents()) {
                if (c instanceof JLabel && ((JLabel) c).getText().startsWith("11-02 · 2 条")) chip2 = (JLabel) c;
            }
            click(chip2);
            Thread.sleep(400);
            check("再点一次取消筛选", ofF.get(ov) == null && title.getText().equals("今日待办"));

            /* ---------- 3. 小窗全部勾完 ---------- */
            SwingUtilities.invokeAndWait(() -> MiniBoard.toggle());
            Thread.sleep(800);
            Field instF = MiniBoard.class.getDeclaredField("inst");
            instF.setAccessible(true);
            Object mb = instF.get(null);
            Method fin = MiniBoard.class.getDeclaredMethod("finishAllOverdue");
            fin.setAccessible(true);
            int n = (Integer) fin.invoke(mb);
            System.out.println("一次勾完=" + n);
            check("三条过期一次勾完", n == 3);

            Method mr = MiniBoard.class.getDeclaredMethod("refresh");
            mr.setAccessible(true);
            SwingUtilities.invokeAndWait(() -> {
                try {
                    mr.invoke(mb);
                } catch (Exception ignored) {
                }
            });
            Thread.sleep(300);
            Field odF = MiniBoard.class.getDeclaredField("overdueLbl");
            odF.setAccessible(true);
            JLabel od = (JLabel) odF.get(mb);
            check("勾完小窗红字自动消失", !od.isVisible());

            /* ---------- 4. 归档翻回来 ---------- */
            Method arch = OverviewPanel.class.getDeclaredMethod("archivedTodos");
            arch.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.List<Object> list = (java.util.List<Object>) arch.invoke(ov);
            check("归档清单里有一条", list.size() == 1);
            Method rec = OverviewPanel.class.getDeclaredMethod("recoverArchived", list.get(0).getClass());
            rec.setAccessible(true);
            rec.invoke(ov, list.get(0));
            Thread.sleep(400);

            Field todosF = OverviewPanel.class.getDeclaredField("todos");
            todosF.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.List<Object> todos = (java.util.List<Object>) todosF.get(ov);
            Field dateF = todos.get(0).getClass().getDeclaredField("date");
            dateF.setAccessible(true);
            Field doneF = todos.get(0).getClass().getDeclaredField("done");
            doneF.setAccessible(true);
            Field textF = todos.get(0).getClass().getDeclaredField("text");
            textF.setAccessible(true);
            boolean recovered = false;
            for (Object t : todos) {
                if (String.valueOf(textF.get(t)).equals("四十天前")) {
                    recovered = !doneF.getBoolean(t) && "2026-11-11".equals(String.valueOf(dateF.get(t)));
                }
            }
            System.out.println("翻回=" + recovered);
            check("归档条目翻回后回到今天且未完成", recovered);

            SwingUtilities.invokeAndWait(() -> {
                try {
                    refresh.invoke(ov);
                } catch (Exception ignored) {
                }
            });
            Thread.sleep(400);
            String s3 = labels(box);
            System.out.println("翻回后今日=" + s3);
            check("翻回的条目出现在今日待办", s3.contains("四十天前") && s3.contains("今天的事"));
            check("归档区清空后入口消失", !labels(bar).contains("已归档"));

            System.out.println(ok ? "ALL PASS" : "HAS FAIL");
            System.exit(ok ? 0 : 1);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(2);
        }
    }

    private static String labels(JPanel box) {
        StringBuilder sb = new StringBuilder();
        for (Component c : box.getComponents()) {
            if (c instanceof JLabel) sb.append(((JLabel) c).getText()).append('|');
            else if (c instanceof JPanel) {
                for (Component k : ((JPanel) c).getComponents()) {
                    if (k instanceof JLabel) sb.append(((JLabel) k).getText()).append('|');
                    else if (k instanceof JCheckBox) sb.append(((JCheckBox) k).getText()).append('|');
                }
            }
        }
        return sb.toString();
    }

    private static void click(JLabel l) {
        if (l == null) return;
        MouseEvent ev = new MouseEvent(l, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
                0, 1, 1, 1, false);
        for (java.awt.event.MouseListener ml : l.getMouseListeners()) ml.mouseClicked(ev);
    }

    private static void check(String name, boolean pass) {
        System.out.println((pass ? "[PASS] " : "[FAIL] ") + name);
        if (!pass) ok = false;
    }

    private static <T extends Component> T page(Class<T> type) {
        for (Frame f : Frame.getFrames()) {
            T t = scan(f, type);
            if (t != null) return t;
        }
        return null;
    }

    private static <T extends Component> T scan(Component c, Class<T> type) {
        if (type.isInstance(c)) return type.cast(c);
        if (c instanceof Container) {
            for (Component k : ((Container) c).getComponents()) {
                T t = scan(k, type);
                if (t != null) return t;
            }
        }
        return null;
    }
}
