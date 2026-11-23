import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** v1.31 三项验证：过期未做红标 / 小窗按天切换 / 日历列出当天待办 */
public class FeatureTest11 {

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
            Method refresh = OverviewPanel.class.getDeclaredMethod("refresh");
            refresh.setAccessible(true);
            SwingUtilities.invokeAndWait(() -> {
                try {
                    refresh.invoke(ov);
                } catch (Exception ignored) {
                }
            });
            Thread.sleep(600);

            /* ---------- 1. 过期未做 ---------- */
            Method overdue = OverviewPanel.class.getDeclaredMethod("overdueByDay");
            overdue.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<LocalDate, Integer> over = (Map<LocalDate, Integer>) overdue.invoke(ov);
            System.out.println("过期按天=" + over);
            check("过期只算没做完的", over.size() == 2 && over.get(LocalDate.of(2026, 11, 9)) == 1
                    && over.get(LocalDate.of(2026, 11, 8)) == 1);

            Field fbF = OverviewPanel.class.getDeclaredField("futureBar");
            fbF.setAccessible(true);
            JPanel bar = (JPanel) fbF.get(ov);
            StringBuilder texts = new StringBuilder();
            Color firstChipColor = null;
            for (Component c : bar.getComponents()) {
                if (c instanceof JLabel) {
                    JLabel l = (JLabel) c;
                    texts.append(l.getText()).append('|');
                    if (firstChipColor == null && l.getText().contains("11-09")) firstChipColor = l.getForeground();
                }
            }
            System.out.println("标签=" + texts + " 可见=" + bar.isVisible());
            check("过期未做排在最前且是红色", bar.isVisible() && texts.toString().startsWith("过期未做|")
                    && texts.toString().contains("11-09 · 1 条")
                    && firstChipColor != null && firstChipColor.getRGB() == new Color(0xDC2626).getRGB());

            /* 点过期标签切到那天 */
            Method showDate = OverviewPanel.class.getDeclaredMethod("showDate", LocalDate.class);
            showDate.setAccessible(true);
            SwingUtilities.invokeAndWait(() -> {
                try {
                    showDate.invoke(ov, LocalDate.of(2026, 11, 9));
                } catch (Exception ignored) {
                }
            });
            Thread.sleep(400);
            Field ttF = OverviewPanel.class.getDeclaredField("todoTitle");
            ttF.setAccessible(true);
            JLabel title = (JLabel) ttF.get(ov);
            Field tbF = OverviewPanel.class.getDeclaredField("todoBox");
            tbF.setAccessible(true);
            JPanel box = (JPanel) tbF.get(ov);
            int rows = 0;
            for (Component c : box.getComponents()) if (c instanceof JPanel) rows++;
            System.out.println("过期那天 标题=" + title.getText() + " 行数=" + rows);
            check("点过期标签切到那天", title.getText().contains("11-09") && rows == 2);
            SwingUtilities.invokeAndWait(() -> {
                try {
                    showDate.invoke(ov, (Object) null);
                } catch (Exception ignored) {
                }
            });
            Thread.sleep(300);

            /* ---------- 2. 日历预览当天待办 ---------- */
            Method titles = OverviewPanel.class.getDeclaredMethod("todoTitlesByDay");
            titles.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<LocalDate, List<String>> dayMap = (Map<LocalDate, List<String>>) titles.invoke(ov);
            System.out.println("日期→待办=" + dayMap);
            Class<?> dp = Class.forName("com.toolbox.pc.DatePicker");
            Method build = dp.getDeclaredMethod("build", Window.class, LocalDate.class,
                    Map.class, LocalDate[].class);
            build.setAccessible(true);
            LocalDate[] out = {null};
            JDialog dlg = (JDialog) build.invoke(null, null, LocalDate.of(2026, 11, 11), dayMap, out);
            // 预览标签通过组件查找（类里没有静态引用）
            JLabel preview = null;
            for (Component c : allLabels(dlg.getContentPane())) {
                if (c instanceof JLabel && ((JLabel) c).getText().contains("11-11")) preview = (JLabel) c;
            }
            System.out.println("预览=" + (preview == null ? "无" : preview.getText()));
            check("日历列出当天的待办", preview != null
                    && preview.getText().contains("2 条")
                    && preview.getText().contains("明天的甲")
                    && preview.getText().contains("明天的乙"));

            /* 选一个没有待办的日子 */
            Class<?> gridCls = Class.forName("com.toolbox.pc.DatePicker$CalGrid");
            Object grid = findAny(dlg.getContentPane(), gridCls);
            java.util.function.Consumer<LocalDate> onPick = d -> {
            };
            Field onPickF = gridCls.getDeclaredField("onPick");
            onPickF.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.function.Consumer<LocalDate> picker =
                    (java.util.function.Consumer<LocalDate>) onPickF.get(grid);
            SwingUtilities.invokeAndWait(() -> picker.accept(LocalDate.of(2026, 11, 5)));
            Thread.sleep(300);
            JLabel preview2 = null;
            for (Component c : allLabels(dlg.getContentPane())) {
                if (c instanceof JLabel && ((JLabel) c).getText().contains("11-05")) preview2 = (JLabel) c;
            }
            System.out.println("空日预览=" + (preview2 == null ? "无" : preview2.getText()));
            check("没安排的日子给出提示", preview2 != null && preview2.getText().contains("没有待办"));

            /* ---------- 3. 小窗按天切换待办 ---------- */
            SwingUtilities.invokeAndWait(() -> MiniBoard.toggle());
            Thread.sleep(800);
            Field instF = MiniBoard.class.getDeclaredField("inst");
            instF.setAccessible(true);
            Object mb = instF.get(null);
            Field lbF = MiniBoard.class.getDeclaredField("listBox");
            lbF.setAccessible(true);
            JPanel listBox = (JPanel) lbF.get(mb);
            String day0 = joinLabels(listBox);
            System.out.println("小窗今天=" + day0);
            check("小窗今天只显示今天的事", day0.contains("今日待办") && day0.contains("今天的事")
                    && !day0.contains("明天的甲"));

            Field bdF = MiniBoard.class.getDeclaredField("boardDay");
            bdF.setAccessible(true);
            bdF.setInt(mb, 1);
            Method mbRefresh = MiniBoard.class.getDeclaredMethod("refresh");
            mbRefresh.setAccessible(true);
            SwingUtilities.invokeAndWait(() -> {
                try {
                    mbRefresh.invoke(mb);
                } catch (Exception ignored) {
                }
            });
            Thread.sleep(500);
            String day1 = joinLabels(listBox);
            System.out.println("小窗明天=" + day1);
            check("小窗切到明天显示那天的", day1.contains("11-11") && day1.contains("明天的甲")
                    && day1.contains("明天的乙") && !day1.contains("今天的事"));

            bdF.setInt(mb, 0);
            SwingUtilities.invokeAndWait(() -> {
                try {
                    mbRefresh.invoke(mb);
                } catch (Exception ignored) {
                }
            });
            Thread.sleep(300);

            System.out.println(ok ? "ALL PASS" : "HAS FAIL");
            System.exit(ok ? 0 : 1);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(2);
        }
    }

    private static List<Component> allLabels(Component root) {
        java.util.List<Component> out = new java.util.ArrayList<Component>();
        collect(root, out);
        return out;
    }

    private static void collect(Component c, List<Component> out) {
        out.add(c);
        if (c instanceof Container) for (Component k : ((Container) c).getComponents()) collect(k, out);
    }

    private static String joinLabels(JPanel box) {
        StringBuilder sb = new StringBuilder();
        for (Component c : box.getComponents()) {
            if (c instanceof JLabel) sb.append(((JLabel) c).getText()).append('|');
            else if (c instanceof JPanel) {
                for (Component k : ((JPanel) c).getComponents()) {
                    if (k instanceof JLabel) sb.append(((JLabel) k).getText()).append('|');
                }
            }
        }
        return sb.toString();
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

    private static Component findAny(Component c, Class<?> type) {
        if (type.isInstance(c)) return c;
        if (c instanceof Container) {
            for (Component k : ((Container) c).getComponents()) {
                Component t = findAny(k, type);
                if (t != null) return t;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Component> T scan(Component c, Class<T> type) {
        if (type.isInstance(c)) return (T) c;
        if (c instanceof Container) {
            for (Component k : ((Container) c).getComponents()) {
                T t = scan(k, type);
                if (t != null) return t;
            }
        }
        return null;
    }
}
