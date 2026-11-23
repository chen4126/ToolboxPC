import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** v1.29 三项验证：小日历选日期 / 未来 7 天概况 / 标题栏闪动 */
public class FeatureTest9 {

    private static boolean ok = true;

    public static void main(String[] args) throws Exception {
        try {
            Theme.init();
            SwingUtilities.invokeAndWait(() -> Main.main(new String[0]));
            Thread.sleep(2500);

            Class<?> mc = Class.forName("com.toolbox.pc.Main");
            Field ff = mc.getDeclaredField("frame");
            ff.setAccessible(true);
            Field selfF = mc.getDeclaredField("self");
            selfF.setAccessible(true);
            JFrame frame = (JFrame) ff.get(selfF.get(null));

            OverviewPanel ov = page(OverviewPanel.class);
            Method rf = OverviewPanel.class.getDeclaredMethod("refresh");
            rf.setAccessible(true);
            SwingUtilities.invokeAndWait(() -> {
                try {
                    rf.invoke(ov);
                } catch (Exception ignored) {
                }
            });
            Thread.sleep(500);

            /* ---------- 1. 小日历的日期换算 ---------- */
            Class<?> gridCls = Class.forName("com.toolbox.pc.DatePicker$CalGrid");
            Constructor<?> gc = gridCls.getDeclaredConstructor();
            gc.setAccessible(true);
            Object grid = gc.newInstance();
            Method refresh = gridCls.getDeclaredMethod("refresh", YearMonth.class, LocalDate.class);
            refresh.setAccessible(true);
            refresh.invoke(grid, YearMonth.of(2026, 11), LocalDate.of(2026, 11, 10));
            Method dateAt = gridCls.getDeclaredMethod("dateAt", int.class, int.class);
            dateAt.setAccessible(true);
            Method cellW = gridCls.getDeclaredMethod("cellW");
            cellW.setAccessible(true);
            ((JComponent) grid).setSize(280, 234);
            Object d1 = dateAt.invoke(grid, 6 * 40 + 20, 42);    // 第 0 行第 6 列
            Object d2 = dateAt.invoke(grid, 0 * 40 + 20, 42 + 34); // 第 1 行第 0 列
            Object d3 = dateAt.invoke(grid, 0 * 40 + 20, 42);    // 1 号之前的空格
            System.out.println("11 月 1 日格=" + d1 + " 第2行首格=" + d2 + " 空格=" + d3);
            check("日历 1 号落在周日列", LocalDate.of(2026, 11, 1).equals(d1));
            check("日历第 2 行首格是 2 号", LocalDate.of(2026, 11, 2).equals(d2));
            check("月初空位点不到日期", d3 == null);

            /* ---------- 1b. 菜单里有“指定日期…” ---------- */
            JCheckBox cb = firstTodoCheck(ov);
            JMenu defer = findMenu(cb.getComponentPopupMenu(), "改期到…");
            JMenuItem pickItem = findItem(defer, "指定日期…");
            System.out.println("改期子菜单项=" + defer.getItemCount()
                    + " 指定日期项=" + (pickItem == null ? "无" : pickItem.getText()));
            check("改期子菜单有指定日期入口", pickItem != null);

            /* ---------- 1c. 选个具体日期真的写到盘里 ---------- */
            Field todosF = OverviewPanel.class.getDeclaredField("todos");
            todosF.setAccessible(true);
            java.util.List<?> all = (java.util.List<?>) todosF.get(ov);
            final Object todo0 = all.get(0);
            Method setDate = OverviewPanel.class.getDeclaredMethod(
                    "setTodoDate", todo0.getClass(), LocalDate.class);
            setDate.setAccessible(true);
            LocalDate target = LocalDate.of(2026, 12, 25);
            SwingUtilities.invokeAndWait(() -> {
                try {
                    setDate.invoke(ov, todo0, target);
                } catch (Exception ignored) {
                }
            });
            Thread.sleep(300);
            java.util.List<String> lines = com.toolbox.pc.Store.readLines("todo.txt");
            boolean wrote = false;
            for (String l : lines) if (l.contains("今天的事") && l.endsWith("2026-12-25")) wrote = true;
            System.out.println("todo.txt=" + lines);
            check("选具体日期后写进 todo.txt", wrote);

            /* ---------- 2. 未来 7 天概况 ---------- */
            Field fuF = OverviewPanel.class.getDeclaredField("futureLbl");
            fuF.setAccessible(true);
            JLabel futureLbl = (JLabel) fuF.get(ov);
            System.out.println("未来概况=" + futureLbl.getText()
                    + " 可见=" + futureLbl.isVisible());
            check("未来 7 天概况按天统计",
                    futureLbl.getText().contains("11-11 2 条")
                            && futureLbl.getText().contains("11-15 1 条")
                            && futureLbl.getText().contains("共 3 条")
                            && !futureLbl.getText().contains("11-20"));

            /* ---------- 3. 标题栏闪动 ---------- */
            String base = frame.getTitle();
            Main.flashTitle("泡面 已超时 1 分");
            Thread.sleep(900);
            String flashing = frame.getTitle();
            System.out.println("闪动中标题=" + flashing);
            check("超时后标题栏出现提醒", flashing.startsWith("●") && flashing.contains("已超时"));
            Thread.sleep(6500);
            String restored = frame.getTitle();
            System.out.println("恢复后标题=" + restored);
            check("闪动结束后标题恢复", base.equals(restored));

            System.out.println(ok ? "ALL PASS" : "HAS FAIL");
            System.exit(ok ? 0 : 1);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(2);
        }
    }

    private static JCheckBox firstTodoCheck(OverviewPanel ov) throws Exception {
        Field tbF = OverviewPanel.class.getDeclaredField("todoBox");
        tbF.setAccessible(true);
        JPanel box = (JPanel) tbF.get(ov);
        JPanel row = (JPanel) box.getComponent(0);
        return (JCheckBox) row.getComponent(0);
    }

    private static JMenu findMenu(JPopupMenu menu, String text) {
        for (Component c : menu.getComponents()) {
            if (c instanceof JMenu && text.equals(((JMenu) c).getText())) return (JMenu) c;
        }
        return null;
    }

    private static JMenuItem findItem(JMenu menu, String text) {
        if (menu == null) return null;
        for (Component c : menu.getMenuComponents()) {
            if (c instanceof JMenuItem && text.equals(((JMenuItem) c).getText())) return (JMenuItem) c;
        }
        return null;
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
