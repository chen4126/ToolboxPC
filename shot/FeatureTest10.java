import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.Set;

/** v1.30 三项验证：日历圆点 / 未来标签切天 / 闪动拉起窗口 */
public class FeatureTest10 {

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

            /* ---------- 1. 日历圆点的数据源 ---------- */
            Method marked = OverviewPanel.class.getDeclaredMethod("markedDates");
            marked.setAccessible(true);
            @SuppressWarnings("unchecked")
            Set<LocalDate> marks = (Set<LocalDate>) marked.invoke(ov);
            System.out.println("有待办的日子=" + new java.util.TreeSet<LocalDate>(marks));
            check("只标有具体日期的待办", marks.size() == 3
                    && marks.contains(LocalDate.of(2026, 11, 10))
                    && marks.contains(LocalDate.of(2026, 11, 11))
                    && marks.contains(LocalDate.of(2026, 11, 15)));

            /* ---------- 2. 日历把 marks 收进去 ---------- */
            Class<?> dp = Class.forName("com.toolbox.pc.DatePicker");
            Method build = dp.getDeclaredMethod("build", Window.class, LocalDate.class,
                    Set.class, LocalDate[].class);
            build.setAccessible(true);
            LocalDate[] out = {null};
            JDialog dlg = (JDialog) build.invoke(null, null, LocalDate.of(2026, 11, 10), marks, out);
            Class<?> gridCls = Class.forName("com.toolbox.pc.DatePicker$CalGrid");
            Object grid = find2(dlg.getContentPane(), gridCls);            Field marksF = gridCls.getDeclaredField("marks");
            marksF.setAccessible(true);
            Set<?> got = (Set<?>) marksF.get(grid);
            System.out.println("日历收到的标记数=" + (got == null ? 0 : got.size()));
            check("日历拿到标记日期", got != null && got.size() == 3);

            /* ---------- 3. 未来标签切到那天 ---------- */
            Field fbF = OverviewPanel.class.getDeclaredField("futureBar");
            fbF.setAccessible(true);
            JPanel bar = (JPanel) fbF.get(ov);
            StringBuilder chips = new StringBuilder();
            for (Component c : bar.getComponents()) {
                if (c instanceof JLabel) chips.append(((JLabel) c).getText()).append('|');
            }
            System.out.println("未来标签=" + chips + " 可见=" + bar.isVisible());
            check("未来行是可按的标签", bar.isVisible() && chips.toString().contains("11-11 · 2 条")
                    && chips.toString().contains("11-15 · 1 条"));

            Method showDate = OverviewPanel.class.getDeclaredMethod("showDate", LocalDate.class);
            showDate.setAccessible(true);
            SwingUtilities.invokeAndWait(() -> {
                try {
                    showDate.invoke(ov, LocalDate.of(2026, 11, 11));
                } catch (Exception ignored) {
                }
            });
            Thread.sleep(400);
            Field ttF = OverviewPanel.class.getDeclaredField("todoTitle");
            ttF.setAccessible(true);
            JLabel title = (JLabel) ttF.get(ov);
            Field btF = OverviewPanel.class.getDeclaredField("backToday");
            btF.setAccessible(true);
            JLabel back = (JLabel) btF.get(ov);
            Field tbF = OverviewPanel.class.getDeclaredField("todoBox");
            tbF.setAccessible(true);
            JPanel box = (JPanel) tbF.get(ov);
            int rows = 0;
            for (Component c : box.getComponents()) if (c instanceof JPanel) rows++;
            System.out.println("切到 11-11 标题=" + title.getText() + " 行数=" + rows
                    + " 回到今天可见=" + back.isVisible());
            check("切到 11-11 只显示那天", title.getText().contains("11-11") && rows == 3
                    && back.isVisible());

            SwingUtilities.invokeAndWait(() -> {
                try {
                    showDate.invoke(ov, (Object) null);
                } catch (Exception ignored) {
                }
            });
            Thread.sleep(400);
            rows = 0;
            for (Component c : box.getComponents()) if (c instanceof JPanel) rows++;
            System.out.println("回到今天 标题=" + title.getText() + " 行数=" + rows
                    + " 回到今天可见=" + back.isVisible());
            check("回到今天恢复两行", title.getText().equals("今日待办") && rows == 2
                    && !back.isVisible());

            /* ---------- 3b. 在某天视图下添加，落到那天 ---------- */
            SwingUtilities.invokeAndWait(() -> {
                try {
                    showDate.invoke(ov, LocalDate.of(2026, 11, 11));
                } catch (Exception ignored) {
                }
            });
            Thread.sleep(300);
            Field inF = OverviewPanel.class.getDeclaredField("todoInput");
            inF.setAccessible(true);
            JTextField field = (JTextField) inF.get(ov);
            SwingUtilities.invokeAndWait(() -> {
                field.setText("临时加一条");
                field.postActionEvent();
            });
            Thread.sleep(400);
            boolean landed = false;
            for (String l : Store.readLines("todo.txt")) {
                if (l.contains("临时加一条") && l.endsWith("2026-11-11")) landed = true;
            }
            System.out.println("todo.txt=" + Store.readLines("todo.txt"));
            check("某天视图下添加落到那天", landed);

            /* ---------- 4. 最小化时闪动会把窗口拉起来 ---------- */
            String base = frame.getTitle();
            SwingUtilities.invokeAndWait(() -> frame.setState(Frame.ICONIFIED));
            Thread.sleep(300);
            int st = frame.getState();
            System.out.println("最小化后 state=" + st);
            Main.flashTitle("泡面 已超时 1 分");
            Thread.sleep(900);
            System.out.println("拉起后 state=" + frame.getState() + " 标题=" + frame.getTitle()
                    + " 可见=" + frame.isVisible());
            check("闪动时把最小化窗口拉起来", frame.getState() == Frame.NORMAL
                    && frame.isVisible() && frame.getTitle().startsWith("●"));

            Thread.sleep(6500);
            System.out.println("恢复标题=" + frame.getTitle());
            check("闪完恢复原标题", base.equals(frame.getTitle()));

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

    private static <T extends Component> T page(Class<T> type) {
        for (Frame f : Frame.getFrames()) {
            T t = scan(f, type);
            if (t != null) return t;
        }
        return null;
    }

    private static <T extends Component> T find(Component c, Class<T> type) {
        return scan(c, type);
    }

    private static Component find2(Component c, Class<?> type) {
        if (type.isInstance(c)) return c;
        if (c instanceof Container) {
            for (Component k : ((Container) c).getComponents()) {
                Component t = find2(k, type);
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
