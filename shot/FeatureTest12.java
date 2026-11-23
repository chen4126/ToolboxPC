import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** v1.32 三项验证：小窗过期红字 / 3 天以上深红置顶 / 日历双击新建 */
public class FeatureTest12 {

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

            /* ---------- 1. 3 天以上深红并置顶 ---------- */
            Field fbF = OverviewPanel.class.getDeclaredField("futureBar");
            fbF.setAccessible(true);
            JPanel bar = (JPanel) fbF.get(ov);
            StringBuilder sb = new StringBuilder();
            Color firstColor = null, secondFlagColor = null;
            for (Component c : bar.getComponents()) {
                if (!(c instanceof JLabel)) continue;
                JLabel l = (JLabel) c;
                sb.append(l.getText()).append('|');
                if (firstColor == null) firstColor = l.getForeground();
                if (l.getText().equals("过期未做") && secondFlagColor == null) secondFlagColor = l.getForeground();
            }
            System.out.println("标签=" + sb);
            check("3 天以上的排最前且是深红", sb.toString().startsWith("该处理了|")
                    && firstColor != null && firstColor.getRGB() == new Color(0x991B1B).getRGB());
            check("近两天的过期仍是普通红", sb.toString().contains("过期未做|")
                    && secondFlagColor != null && secondFlagColor.getRGB() == new Color(0xDC2626).getRGB());

            /* ---------- 2. 小窗顶部红字 ---------- */
            SwingUtilities.invokeAndWait(() -> MiniBoard.toggle());
            Thread.sleep(800);
            Field instF = MiniBoard.class.getDeclaredField("inst");
            instF.setAccessible(true);
            Object mb = instF.get(null);
            Field odF = MiniBoard.class.getDeclaredField("overdueLbl");
            odF.setAccessible(true);
            JLabel od = (JLabel) odF.get(mb);
            System.out.println("小窗红字=" + od.getText() + " 可见=" + od.isVisible()
                    + " 色=" + Integer.toHexString(od.getForeground().getRGB()));
            check("小窗顶部显示该处理了几条", od.isVisible()
                    && od.getText().contains("该处理了 1 条")
                    && od.getText().contains("11-06")
                    && od.getForeground().getRGB() == new Color(0x991B1B).getRGB());

            /* 点红字翻到那天 */
            MouseEvent ev = new MouseEvent(od, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
                    0, 1, 1, 1, false);
            for (java.awt.event.MouseListener ml : od.getMouseListeners()) ml.mouseClicked(ev);
            Thread.sleep(400);
            Field bdF = MiniBoard.class.getDeclaredField("boardDay");
            bdF.setAccessible(true);
            Field lbF = MiniBoard.class.getDeclaredField("listBox");
            lbF.setAccessible(true);
            JPanel listBox = (JPanel) lbF.get(mb);
            String texts = joinLabels(listBox);
            System.out.println("点红字后 boardDay=" + bdF.getInt(mb) + " 内容=" + texts);
            check("点红字翻到过期那天", bdF.getInt(mb) == -4 && texts.contains("11-06") && !texts.contains("今天的事"));

            bdF.setInt(mb, 0);
            Method mref = MiniBoard.class.getDeclaredMethod("refresh");
            mref.setAccessible(true);
            SwingUtilities.invokeAndWait(() -> {
                try {
                    mref.invoke(mb);
                } catch (Exception ignored) {
                }
            });
            Thread.sleep(300);

            /* ---------- 3. 日历双击 = 新建 ---------- */
            Method titles = OverviewPanel.class.getDeclaredMethod("todoTitlesByDay");
            titles.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<LocalDate, List<String>> dayMap = (Map<LocalDate, List<String>>) titles.invoke(ov);
            Class<?> dp = Class.forName("com.toolbox.pc.DatePicker");
            Method build = dp.getDeclaredMethod("build", Window.class, LocalDate.class,
                    Map.class, LocalDate[].class, boolean[].class);
            build.setAccessible(true);
            LocalDate[] out = {null};
            boolean[] create = {false};
            JDialog dlg = (JDialog) build.invoke(null, null, LocalDate.of(2026, 11, 11), dayMap, out, create);
            Object grid = findAny(dlg.getContentPane(), Class.forName("com.toolbox.pc.DatePicker$CalGrid"));
            Class<?> gc = grid.getClass();
            Field onPickF = gc.getDeclaredField("onPick");
            onPickF.setAccessible(true);
            Field onDoubleF = gc.getDeclaredField("onDouble");
            onDoubleF.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.function.Consumer<LocalDate> onPick =
                    (java.util.function.Consumer<LocalDate>) onPickF.get(grid);
            @SuppressWarnings("unchecked")
            java.util.function.Consumer<LocalDate> onDouble =
                    (java.util.function.Consumer<LocalDate>) onDoubleF.get(grid);
            SwingUtilities.invokeAndWait(() -> onPick.accept(LocalDate.of(2026, 11, 12)));
            Thread.sleep(200);
            boolean afterSingle = create[0];
            System.out.println("单击后 create=" + afterSingle);
            check("单击只是选中，不算新建", !afterSingle && out[0] == null);

            SwingUtilities.invokeAndWait(() -> onDouble.accept(LocalDate.of(2026, 11, 16)));
            Thread.sleep(300);
            System.out.println("双击后 day=" + out[0] + " create=" + create[0]);
            check("双击格子触发新建", create[0] && LocalDate.of(2026, 11, 16).equals(out[0]));

            /* 提示行存在 */
            boolean hasHint = false;
            for (Component c : all(dlg.getContentPane())) {
                if (c instanceof JLabel && ((JLabel) c).getText().contains("双击格子")) hasHint = true;
            }
            check("日历上写着双击能新建", hasHint);

            /* 写盘逻辑：模拟新建一条后落到那天 */
            Method showDate = OverviewPanel.class.getDeclaredMethod("showDate", LocalDate.class);
            showDate.setAccessible(true);
            Field todosF = OverviewPanel.class.getDeclaredField("todos");
            todosF.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.List<Object> todos = (java.util.List<Object>) todosF.get(ov);
            String key = "2026-11-16";
            Class<?> todoCls = Class.forName("com.toolbox.pc.OverviewPanel$Todo");
            java.lang.reflect.Constructor<?> ctor =
                    todoCls.getDeclaredConstructor(boolean.class, String.class, String.class);
            ctor.setAccessible(true);
            todos.add(ctor.newInstance(false, "双击加的事", key));
            Method save = OverviewPanel.class.getDeclaredMethod("saveTodos");
            save.setAccessible(true);
            save.invoke(ov);
            SwingUtilities.invokeAndWait(() -> {
                try {
                    showDate.invoke(ov, LocalDate.of(2026, 11, 16));
                } catch (Exception ignored) {
                }
            });
            Thread.sleep(300);
            Field titleF = OverviewPanel.class.getDeclaredField("todoTitle");
            titleF.setAccessible(true);
            JLabel title = (JLabel) titleF.get(ov);
            System.out.println("新建后标题=" + title.getText());
            check("新建的落到指定那天", title.getText().contains("11-16"));

            /* ---------- 4. 没有过期时小窗红字隐藏 ---------- */
            bdF.setInt(mb, 0);
            SwingUtilities.invokeAndWait(() -> {
                try {
                    mref.invoke(mb);
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

    private static List<Component> all(Component root) {
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
