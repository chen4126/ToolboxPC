import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** v1.36 三项验证：归档天数可调 / 只看某一档 / 小窗待办右键改期删除 */
public class FeatureTest16 {

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
            run(refresh, ov);
            Thread.sleep(500);

            Field fbF = OverviewPanel.class.getDeclaredField("futureBar");
            fbF.setAccessible(true);
            JPanel bar = (JPanel) fbF.get(ov);
            Field boxF = OverviewPanel.class.getDeclaredField("todoBox");
            boxF.setAccessible(true);
            JPanel box = (JPanel) boxF.get(ov);
            Field tf = OverviewPanel.class.getDeclaredField("todoTitle");
            tf.setAccessible(true);
            JLabel title = (JLabel) tf.get(ov);

            /* ---------- 1. 默认 30 天 ---------- */
            Method ad = OverviewPanel.class.getDeclaredMethod("archiveDays");
            ad.setAccessible(true);
            System.out.println("默认阈值=" + ad.invoke(null));
            check("默认归档阈值是 30 天", (Integer) ad.invoke(null) == 30);
            String s1 = labels(bar);
            System.out.println("标签=" + s1);
            check("16 天那条默认还在过期里（折叠成更早）", s1.contains("很久没动|10-26 · 1 条|更早 1 条|")
                    && s1.contains("该处理了|11-07 · 1 条|") && !s1.contains("已归档"));

            /* ---------- 2. 改成 15 天后它被归档 ---------- */
            writeSetting("archiveDays", "15");
            run(refresh, ov);
            Thread.sleep(500);
            check("阈值改成 15 天后立即归档", (Integer) ad.invoke(null) == 15
                    && labels(bar).contains("已归档 1 条"));
            System.out.println("15 天后标签=" + labels(bar));

            /* ---------- 3. 只看某一档 ---------- */
            JLabel lv = findLabel(bar, "很久没动");
            check("找到档位名标签", lv != null);
            click(lv);
            Thread.sleep(400);
            Field lfF = OverviewPanel.class.getDeclaredField("overdueLevel");
            lfF.setAccessible(true);
            String s2 = labels(box);
            System.out.println("点档位后=" + s2 + " 标题=" + title.getText());
            check("只看很久没动这一档", lfF.getInt(ov) == 3 && title.getText().equals("很久没动 · 1 条")
                    && s2.contains("九天前没动") && !s2.contains("四天前") && !s2.contains("昨天没做")
                    && !s2.contains("今天的事"));

            /* 点另一档，切换 */
            JLabel lv2 = findLabel(bar, "过期未做");
            click(lv2);
            Thread.sleep(400);
            String s3 = labels(box);
            System.out.println("切到过期未做=" + s3 + " 标题=" + title.getText());
            check("切到过期未做这一档", lfF.getInt(ov) == 1 && title.getText().equals("过期未做 · 1 条")
                    && s3.contains("昨天没做") && !s3.contains("九天前没动"));

            /* 再点一次取消 */
            JLabel lv3 = findLabel(bar, "过期未做 · 收起");
            click(lv3);
            Thread.sleep(400);
            check("再点一次取消档位筛选", lfF.getInt(ov) == 0 && title.getText().equals("今日待办"));

            /* ---------- 4. 小窗待办右键改期 / 删除 ---------- */
            check("改期到明天写盘成功", Store.updateTodoDate("九天前没动", "2026-11-12"));
            String file1 = read();
            System.out.println("改期后=" + file1);
            check("日期确实写成明天", file1.contains("九天前没动\t2026-11-12"));

            check("删除单条成功", Store.deleteTodo("四天前"));
            String file2 = read();
            System.out.println("删除后=" + file2);
            check("条目已从 todo.txt 移除", !file2.contains("四天前") && file2.contains("九天前没动"));
            check("再删同一条返回 false", !Store.deleteTodo("四天前"));

            /* ---------- 复位阈值 ---------- */
            writeSetting("archiveDays", "30");
            check("阈值回到 30", (Integer) ad.invoke(null) == 30);

            System.out.println(ok ? "ALL PASS" : "HAS FAIL");
            System.exit(ok ? 0 : 1);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(2);
        }
    }

    private static void writeSetting(String key, String val) throws Exception {
        Method m = MiniBoard.class.getDeclaredMethod("writeSetting", String.class, String.class);
        m.setAccessible(true);
        m.invoke(null, key, val);
    }

    private static String read() {
        StringBuilder sb = new StringBuilder();
        for (String l : Store.readLines("todo.txt")) sb.append(l).append('\n');
        return sb.toString();
    }

    private static void run(Method m, Object target) {
        SwingUtilities.invokeLater(() -> {
            try {
                m.invoke(target);
            } catch (Exception ignored) {
            }
        });
    }

    private static JLabel findLabel(JPanel bar, String text) {
        for (Component c : bar.getComponents()) {
            if (c instanceof JLabel && ((JLabel) c).getText().equals(text)) return (JLabel) c;
        }
        return null;
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
