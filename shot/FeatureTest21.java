import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.List;

/** v1.41 三项验证：多选自定义日期段、归档搜索+排序、柱图点柱切天 */
public class FeatureTest21 {

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
            Thread.sleep(700);

            Field msF = OverviewPanel.class.getDeclaredField("multiSelect");
            msF.setAccessible(true);
            Field pkF = OverviewPanel.class.getDeclaredField("picked");
            pkF.setAccessible(true);
            Field rbF = OverviewPanel.class.getDeclaredField("rangeBar");
            rbF.setAccessible(true);
            Field bbF = OverviewPanel.class.getDeclaredField("bulkBar");
            bbF.setAccessible(true);
            Field rgF = OverviewPanel.class.getDeclaredField("bulkRange");
            rgF.setAccessible(true);
            Field fromF = OverviewPanel.class.getDeclaredField("bulkFrom");
            fromF.setAccessible(true);
            Field toF = OverviewPanel.class.getDeclaredField("bulkTo");
            toF.setAccessible(true);
            Field boxF = OverviewPanel.class.getDeclaredField("todoBox");
            boxF.setAccessible(true);
            Method rl = OverviewPanel.class.getDeclaredMethod("refreshTodoList");
            rl.setAccessible(true);

            /* ---------- 1. 自定义范围 ---------- */
            msF.setBoolean(ov, true);
            rgF.setInt(ov, 0);
            ((LinkedHashSet<Object>) pkF.get(ov)).clear();
            run(rl, ov);
            Thread.sleep(500);
            String rb = texts((JPanel) rbF.get(ov));
            System.out.println("范围条=" + rb);
            check("范围条含自定义项", rb.contains("自定义"));

            fromF.set(ov, java.time.LocalDate.of(2026, 11, 11));
            toF.set(ov, java.time.LocalDate.of(2026, 11, 15));
            rgF.setInt(ov, 4);
            run(rl, ov);
            Thread.sleep(500);
            JPanel box = (JPanel) boxF.get(ov);
            String tail = texts((JPanel) rbF.get(ov));
            System.out.println("11-11~11-15 行数=" + box.getComponentCount() + " 范围条=" + tail);
            check("自定义 11-11~11-15 = 3 条", box.getComponentCount() == 3 && tail.contains("11-11~11-15"));

            clickChip((JPanel) bbF.get(ov), "全选");
            Thread.sleep(400);
            int selCnt = ((LinkedHashSet<Object>) pkF.get(ov)).size();
            System.out.println("自定义范围内全选=" + selCnt);
            check("自定义范围内全选 3 条", selCnt == 3);

            /* ---------- 2. 归档搜索 + 排序 ---------- */
            msF.setBoolean(ov, false);
            rgF.setInt(ov, 0);
            ((LinkedHashSet<Object>) pkF.get(ov)).clear();
            run(rl, ov);
            Thread.sleep(400);

            Field qF = OverviewPanel.class.getDeclaredField("archQuery");
            qF.setAccessible(true);
            Field sortF = OverviewPanel.class.getDeclaredField("archByDate");
            sortF.setAccessible(true);
            Method faM = OverviewPanel.class.getDeclaredMethod("filteredArchived");
            faM.setAccessible(true);

            qF.set(ov, "");
            sortF.set(ov, false);
            List<Object> byOrder = (List<Object>) faM.invoke(ov);
            System.out.println("按归档顺序=" + titles(byOrder));
            check("归档共 2 条", byOrder.size() == 2);

            sortF.set(ov, true);
            List<Object> byDateL = (List<Object>) faM.invoke(ov);
            System.out.println("按原日期=" + titles(byDateL));
            Field dateF = null;
            check("按原日期排序后 10-02 在前", titles(byDateL).startsWith("四十天前"));

            qF.set(ov, "二十");
            List<Object> searchL = (List<Object>) faM.invoke(ov);
            System.out.println("搜「二十」=" + titles(searchL));
            check("搜「二十」命中 1 条", searchL.size() == 1 && titles(searchL).contains("二十天前"));

            qF.set(ov, "没有这条");
            check("搜不存在的词得 0 条", ((List<?>) faM.invoke(ov)).isEmpty());
            qF.set(ov, "");

            /* ---------- 3. 柱图点柱子切那天 ---------- */
            SwingUtilities.invokeAndWait(() -> MiniBoard.show());
            Thread.sleep(1500);
            Field instF = MiniBoard.class.getDeclaredField("inst");
            instF.setAccessible(true);
            Object board = instF.get(null);
            Field chartF = MiniBoard.class.getDeclaredField("chart");
            chartF.setAccessible(true);
            Field scF = MiniBoard.class.getDeclaredField("showChart");
            scF.setAccessible(true);
            Field dayF = MiniBoard.class.getDeclaredField("boardDay");
            dayF.setAccessible(true);
            scF.setBoolean(board, true);
            Method br = MiniBoard.class.getDeclaredMethod("refresh");
            br.setAccessible(true);
            run(br, board);
            Thread.sleep(800);

            JPanel chart = (JPanel) chartF.get(board);
            int w = chart.getWidth();
            int pad = 8, gap = 6;
            int bw = (w - pad * 2 - gap * 6) / 7;
            int x2 = pad + (bw + gap) * 1 + bw / 2;   // 第 2 根柱（明天）
            System.out.println("chart=" + w + "x" + chart.getHeight() + " bw=" + bw + " clickX=" + x2);
            clickAt(chart, x2, 40);
            Thread.sleep(700);
            System.out.println("点击后 boardDay=" + dayF.getInt(board));
            check("点第 2 根柱切到明天", dayF.getInt(board) == 1);

            clickAt(chart, pad + bw / 2, 40);
            Thread.sleep(700);
            System.out.println("点第 1 根后 boardDay=" + dayF.getInt(board));
            check("点第 1 根柱回到今天", dayF.getInt(board) == 0);

            int gapX = pad + bw + 2;      // 落在两根柱之间
            clickAt(chart, gapX, 40);
            Thread.sleep(500);
            System.out.println("点柱子间隙后 boardDay=" + dayF.getInt(board));
            check("点空隙不切天", dayF.getInt(board) == 0);

            System.out.println(ok ? "ALL PASS" : "HAS FAIL");
            System.exit(ok ? 0 : 1);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(2);
        }
    }

    private static String titles(List<Object> list) {
        StringBuilder sb = new StringBuilder();
        for (Object t : list) {
            try {
                Field f = t.getClass().getDeclaredField("text");
                f.setAccessible(true);
                sb.append(f.get(t)).append('|');
            } catch (Exception ignored) {
            }
        }
        return sb.toString();
    }

    private static void clickChip(JPanel bar, String text) {
        for (Component c : bar.getComponents()) {
            if (c instanceof JLabel) {
                String s = ((JLabel) c).getText();
                if (s != null && s.startsWith(text)) {
                    clickLabel((JLabel) c);
                    return;
                }
            }
        }
        System.out.println("找不到 chip: " + text);
    }

    private static void clickAt(Component c, int x, int y) {
        MouseEvent ev = new MouseEvent(c, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
                0, x, y, 1, false, MouseEvent.BUTTON1);
        for (java.awt.event.MouseListener ml : c.getMouseListeners()) ml.mouseClicked(ev);
    }

    private static void clickLabel(JLabel l) {
        for (java.awt.event.MouseListener ml : l.getMouseListeners()) {
            ml.mouseClicked(new MouseEvent(l, MouseEvent.MOUSE_CLICKED,
                    System.currentTimeMillis(), 0, 5, 5, 1, false));
        }
    }

    private static String texts(JPanel box) {
        StringBuilder sb = new StringBuilder();
        for (Component c : box.getComponents()) {
            if (c instanceof JLabel) sb.append(((JLabel) c).getText()).append('|');
        }
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

    private static void check(String name, boolean pass) {
        System.out.println((pass ? "[PASS] " : "[FAIL] ") + name);
        if (!pass) ok = false;
    }

    private static <T> T page(Component c, Class<T> type) {
        if (type.isInstance(c)) return type.cast(c);
        if (c instanceof Container) {
            for (Component k : ((Container) c).getComponents()) {
                T t = page(k, type);
                if (t != null) return t;
            }
        }
        return null;
    }
}
