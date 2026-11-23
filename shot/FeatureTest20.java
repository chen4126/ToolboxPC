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

/** v1.40 三项验证：多选按范围筛、归档恢复并改期、小窗 7 天柱图 */
public class FeatureTest20 {

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
            Field bbF = OverviewPanel.class.getDeclaredField("bulkBar");
            bbF.setAccessible(true);
            Field rbF = OverviewPanel.class.getDeclaredField("rangeBar");
            rbF.setAccessible(true);
            Field rgF = OverviewPanel.class.getDeclaredField("bulkRange");
            rgF.setAccessible(true);
            Field titleF = OverviewPanel.class.getDeclaredField("todoTitle");
            titleF.setAccessible(true);
            Field boxF = OverviewPanel.class.getDeclaredField("todoBox");
            boxF.setAccessible(true);
            Method rl = OverviewPanel.class.getDeclaredMethod("refreshTodoList");
            rl.setAccessible(true);

            /* ---------- 1. 范围标签存在 ---------- */
            msF.setBoolean(ov, true);
            rgF.setInt(ov, 0);
            ((LinkedHashSet<Object>) pkF.get(ov)).clear();
            run(rl, ov);
            Thread.sleep(500);
            JPanel bulk = (JPanel) bbF.get(ov);
            JPanel rangeBar = (JPanel) rbF.get(ov);
            System.out.println("范围条=" + texts(rangeBar) + " 操作条=" + texts(bulk));
            check("范围条含四个标签", texts(rangeBar).contains("全部") && texts(rangeBar).contains("今天")
                    && texts(rangeBar).contains("未来7天") && texts(rangeBar).contains("过期"));

            /* ---------- 2. 各范围条数 ---------- */
            int[] want = {7, 1, 2, 3};
            String[] names = {"全部", "今天", "未来7天", "过期"};
            for (int i = 0; i < 4; i++) {
                clickChip((JPanel) rbF.get(ov), names[i]);
                Thread.sleep(400);
                JLabel title = (JLabel) titleF.get(ov);
                JPanel box = (JPanel) boxF.get(ov);
                System.out.println("范围[" + names[i] + "] 标题=" + title.getText()
                        + " 行数=" + box.getComponentCount() + " range=" + rgF.getInt(ov));
                check("范围 " + names[i] + " = " + want[i] + " 条",
                        box.getComponentCount() == want[i] && rgF.getInt(ov) == i);
            }

            /* ---------- 3. 范围内全选 + 批量改期 ---------- */
            clickChip((JPanel) rbF.get(ov), "过期");
            Thread.sleep(300);
            clickChip((JPanel) bbF.get(ov), "全选");
            Thread.sleep(400);
            int selCnt = ((LinkedHashSet<Object>) pkF.get(ov)).size();
            System.out.println("过期范围全选=" + selCnt + " 条");
            check("过期内全选 3 条", selCnt == 3);

            Method bm = OverviewPanel.class.getDeclaredMethod("batchMove", java.time.LocalDate.class);
            bm.setAccessible(true);
            int moved = (Integer) bm.invoke(ov, java.time.LocalDate.now().plusDays(1));
            String f1 = readAll();
            System.out.println("批量改期=" + moved + "\n" + f1);
            check("3 条过期一起改到明天", moved == 3
                    && f1.contains("九天前没动\t2026-11-12") && f1.contains("四天前\t2026-11-12")
                    && f1.contains("昨天没做\t2026-11-12"));

            /* ---------- 4. 归档恢复并改期 ---------- */
            msF.setBoolean(ov, false);
            ((LinkedHashSet<Object>) pkF.get(ov)).clear();
            run(rl, ov);
            Thread.sleep(400);
            Method archM = OverviewPanel.class.getDeclaredMethod("archivedTodos");
            archM.setAccessible(true);
            List<Object> arch = (List<Object>) archM.invoke(ov);
            System.out.println("归档=" + arch.size());
            check("有 1 条归档", arch.size() == 1);

            Method recTo = OverviewPanel.class.getDeclaredMethod("recoverArchivedTo",
                    Class.forName("com.toolbox.pc.OverviewPanel$Todo"), java.time.LocalDate.class);
            recTo.setAccessible(true);
            recTo.invoke(ov, arch.get(0), java.time.LocalDate.now().plusDays(5));
            Thread.sleep(400);
            String f2 = readAll();
            System.out.println("恢复并改期后归档=" + ((List<?>) archM.invoke(ov)).size() + "\n" + f2);
            check("恢复并改期落到 11-16 且去掉归档后缀", ((List<?>) archM.invoke(ov)).isEmpty()
                    && f2.contains("四十天前\t2026-11-16") && !f2.contains("（已归档）"));

            /* ---------- 5. 小窗 7 天柱图 ---------- */
            SwingUtilities.invokeAndWait(() -> MiniBoard.show());
            Thread.sleep(1500);
            Field instF = MiniBoard.class.getDeclaredField("inst");
            instF.setAccessible(true);
            Object board = instF.get(null);
            Field chartF = MiniBoard.class.getDeclaredField("chart");
            chartF.setAccessible(true);
            Field sumF = MiniBoard.class.getDeclaredField("sumLbl");
            sumF.setAccessible(true);
            JPanel chart = (JPanel) chartF.get(board);
            JLabel sum = (JLabel) sumF.get(board);
            JFrame win = null;
            for (Window w : Window.getWindows()) {
                if (w instanceof JFrame && "小工具箱 · 今日".equals(((JFrame) w).getTitle())) win = (JFrame) w;
            }
            int h0 = win.getHeight();
            System.out.println("柱图默认可见=" + chart.isVisible() + " 高度=" + h0
                    + " 摘要=" + sum.getText() + " 提示=" + sum.getToolTipText());
            check("柱图默认收起", !chart.isVisible());

            clickLabel(sum);
            Thread.sleep(800);
            int h1 = win.getHeight();
            System.out.println("点一次 柱图可见=" + chart.isVisible() + " 高度=" + h1
                    + " 提示=" + sum.getToolTipText());
            check("点一下展开柱图并加高", chart.isVisible() && h1 > h0);

            clickLabel(sum);
            Thread.sleep(800);
            System.out.println("点两次 柱图可见=" + chart.isVisible() + " 高度=" + win.getHeight());
            check("再点一下收起柱图", !chart.isVisible());

            System.out.println(ok ? "ALL PASS" : "HAS FAIL");
            System.exit(ok ? 0 : 1);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(2);
        }
    }

    private static String readAll() {
        StringBuilder sb = new StringBuilder();
        for (String l : Store.readLines("todo.txt")) sb.append(l).append('\n');
        return sb.toString().trim();
    }

    private static void clickChip(JPanel bar, String text) {
        for (Component c : bar.getComponents()) {
            if (c instanceof JLabel && text.equals(((JLabel) c).getText())) {
                clickLabel((JLabel) c);
                return;
            }
        }
        System.out.println("找不到 chip: " + text);
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
