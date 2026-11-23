import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;

/** v1.42 验证：通知去重（不再弹两个）、归档搜索高亮、柱图右键新建 */
public class FeatureTest22 {

    private static boolean ok = true;

    public static void main(String[] args) throws Exception {
        try {
            SwingUtilities.invokeAndWait(() -> Main.main(new String[0]));
            Thread.sleep(2500);

            Class<?> mc = Class.forName("com.toolbox.pc.Main");
            Field selfF = mc.getDeclaredField("self");
            selfF.setAccessible(true);
            Object main = selfF.get(null);
            Field ff = mc.getDeclaredField("frame");
            ff.setAccessible(true);
            JFrame frame = (JFrame) ff.get(main);
            Method sel = mc.getDeclaredMethod("selectNav", String.class);
            sel.setAccessible(true);
            SwingUtilities.invokeAndWait(() -> {
                try {
                    sel.invoke(main, "overview");
                } catch (Exception e) {
                }
            });
            Thread.sleep(1400);

            OverviewPanel ov = page(frame.getContentPane(), OverviewPanel.class);
            Method refresh = OverviewPanel.class.getDeclaredMethod("refresh");
            refresh.setAccessible(true);
            run(refresh, ov);
            Thread.sleep(600);

            /* 1. 同一句话连发两次，只出一个通知卡片 */
            Method count = Toast.class.getDeclaredMethod("count");
            count.setAccessible(true);
            SwingUtilities.invokeAndWait(() -> {
                Main.notifyMsg("已批量删除", "删掉 2 条待办");
                Main.notifyMsg("已批量删除", "删掉 2 条待办");
            });
            Thread.sleep(400);
            int n1 = (Integer) count.invoke(null);
            check("同一句话 2 次只弹 1 个卡片", n1 == 1, "卡片数=" + n1);

            /* 2. 换一句话会再出一个 */
            SwingUtilities.invokeAndWait(() -> Main.notifyMsg("已批量删除", "删掉 3 条待办"));
            Thread.sleep(400);
            int n2 = (Integer) count.invoke(null);
            check("换个文案会新弹一个", n2 == 2, "卡片数=" + n2);

            /* 3. 归档搜索高亮 */
            Field aq = OverviewPanel.class.getDeclaredField("archQuery");
            aq.setAccessible(true);
            aq.set(ov, "二十");
            Method archLine = OverviewPanel.class.getDeclaredMethod("archLine", String.class, boolean.class);
            archLine.setAccessible(true);
            String hi = (String) archLine.invoke(ov, "10-22　二十天前（已归档）", false);
            check("命中段标黄", hi != null && hi.contains("background:#FDE68A"), String.valueOf(hi));
            check("命中词只标一次", hi != null && hi.split("FDE68A").length == 2, "片段数");
            String none = (String) archLine.invoke(ov, "10-02　四十天前（已归档）", false);
            check("没命中不返回 HTML", none == null, String.valueOf(none));

            /* 4. 柱图索引：柱子内 / 间隙 / 柱外 */
            Field mbF = MiniBoard.class.getDeclaredField("inst");
            mbF.setAccessible(true);
            MiniBoard mb = (MiniBoard) mbF.get(null);
            if (mb == null) {
                SwingUtilities.invokeAndWait(() -> MiniBoard.show());
                Thread.sleep(600);
                mb = (MiniBoard) mbF.get(null);
            }
            Field chartF = MiniBoard.class.getDeclaredField("chart");
            chartF.setAccessible(true);
            javax.swing.JPanel chart = (javax.swing.JPanel) chartF.get(mb);
            SwingUtilities.invokeAndWait(() -> chart.setSize(226, 60));
            Method barIndex = MiniBoard.class.getDeclaredMethod("barIndex", int.class);
            barIndex.setAccessible(true);
            int i0 = (Integer) barIndex.invoke(mb, 12);
            int i1 = (Integer) barIndex.invoke(mb, 41);
            int iGap = (Integer) barIndex.invoke(mb, 35);   // 柱与柱之间的空隙
            int iOut = (Integer) barIndex.invoke(mb, 2);    // 左边距里
            check("第 1 根柱 = 今天", i0 == 0, "idx=" + i0);
            check("第 2 根柱 = 明天", i1 == 1, "idx=" + i1);
            check("点空隙不算", iGap == -1, "idx=" + iGap);
            check("点左边距不算", iOut == -1, "idx=" + iOut);

            /* 5. 指定那天新建一条，落盘到 11-13 */
            Method addOn = OverviewPanel.class.getDeclaredMethod("addTodoOn", LocalDate.class, String.class);
            addOn.setAccessible(true);
            LocalDate day = LocalDate.of(2026, 11, 13);
            int added = (Integer) addOn.invoke(ov, day, "柱图右键新增");
            check("新建返回 1 条", added == 1, "added=" + added);
            Thread.sleep(300);
            boolean onDisk = false;
            for (String line : com.toolbox.pc.Store.readLines("todo.txt")) {
                if (line.contains("柱图右键新增") && line.contains("2026-11-13")) onDisk = true;
            }
            check("新条目落盘到 11-13", onDisk, "todo.txt");
            int added2 = (Integer) addOn.invoke(ov, day, "   ");
            check("空文本不新增", added2 == 0, "added2=" + added2);

            System.out.println(ok ? "ALL PASS" : "SOME FAIL");
        } catch (Throwable t) {
            t.printStackTrace();
            ok = false;
        }
        Runtime.getRuntime().halt(ok ? 0 : 1);
    }

    static void check(String name, boolean pass, String detail) {
        if (!pass) ok = false;
        System.out.println((pass ? "[PASS] " : "[FAIL] ") + name + " → " + detail);
    }

    static void run(Method m, Object target) throws Exception {
        m.invoke(target);
    }

    static <T extends Component> T page(Component root, Class<T> type) {
        if (type.isInstance(root)) return type.cast(root);
        if (root instanceof Container) {
            for (Component c : ((Container) root).getComponents()) {
                T t = page(c, type);
                if (t != null) return t;
            }
        }
        return null;
    }
}
