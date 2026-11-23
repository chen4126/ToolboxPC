import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** v1.37 三项验证：归档批量恢复/清空、按档位一键勾完、筛选状态跨页与重启保留 */
public class FeatureTest17 {

    private static boolean ok = true;

    public static void main(String[] args) throws Exception {
        try {
            Class<?> mc = Class.forName("com.toolbox.pc.Main");
            SwingUtilities.invokeAndWait(() -> Main.main(new String[0]));
            Thread.sleep(2500);

            Field selfF = mc.getDeclaredField("self");
            selfF.setAccessible(true);
            Field ff = mc.getDeclaredField("frame");
            ff.setAccessible(true);
            JFrame frame = (JFrame) ff.get(selfF.get(null));
            Object main = selfF.get(null);

            OverviewPanel ov = page(OverviewPanel.class);
            Method refresh = OverviewPanel.class.getDeclaredMethod("refresh");
            refresh.setAccessible(true);
            run(refresh, ov);
            Thread.sleep(600);

            Field fbF = OverviewPanel.class.getDeclaredField("futureBar");
            fbF.setAccessible(true);
            JPanel bar = (JPanel) fbF.get(ov);
            Field boxF = OverviewPanel.class.getDeclaredField("todoBox");
            boxF.setAccessible(true);
            JPanel box = (JPanel) boxF.get(ov);
            Field tf = OverviewPanel.class.getDeclaredField("todoTitle");
            tf.setAccessible(true);
            JLabel title = (JLabel) tf.get(ov);

            Method archivedM = OverviewPanel.class.getDeclaredMethod("archivedTodos");
            archivedM.setAccessible(true);
            Method recAll = OverviewPanel.class.getDeclaredMethod("recoverAllArchived");
            recAll.setAccessible(true);
            Method delArch = OverviewPanel.class.getDeclaredMethod("deleteArchived", todoClass());
            delArch.setAccessible(true);
            Method finish = OverviewPanel.class.getDeclaredMethod("finishFiltered");
            finish.setAccessible(true);
            Field lfF = OverviewPanel.class.getDeclaredField("overdueLevel");
            lfF.setAccessible(true);
            Field ofF = OverviewPanel.class.getDeclaredField("overdueFilter");
            ofF.setAccessible(true);

            /* ---------- 1. 40 天那条已归档 ---------- */
            int n0 = ((java.util.List<?>) archivedM.invoke(ov)).size();
            System.out.println("归档数=" + n0 + " 标签=" + labels(bar));
            check("40 天那条已被归档", n0 == 1 && labels(bar).contains("已归档 1 条"));

            /* ---------- 2. 归档批量恢复 ---------- */
            int back = (Integer) recAll.invoke(ov);
            System.out.println("全部恢复=" + back + " 标签=" + labels(bar) + " 最近=" + readLast());
            check("全部恢复把 1 条翻回今天", back == 1 && readLast().contains("0\t四十天前\t2026-11-11"));
            check("恢复后归档入口消失", ((java.util.List<?>) archivedM.invoke(ov)).isEmpty()
                    && !labels(bar).contains("已归档"));

            /* ---------- 3. 归档单条删除 + 全部清空 ---------- */
            Object t40 = findTodo(ov, "四十天前");
            check("找不到四十天前这条", t40 != null);
            boolean d1 = (Boolean) delArch.invoke(ov, t40);
            check("归档单条删除成功", d1 && !readAll().contains("四十天前"));

            /* 造一条归档再试全部清空 */
            Field todosF = OverviewPanel.class.getDeclaredField("todos");
            todosF.setAccessible(true);
            java.util.List<Object> todos = (java.util.List<Object>) todosF.get(ov);
            java.lang.reflect.Constructor<?> ctor = todoClass()
                    .getDeclaredConstructor(boolean.class, String.class, String.class);
            ctor.setAccessible(true);
            Object fake = ctor.newInstance(true, "临时归档（已归档）", "2026-09-01");
            todos.add(fake);
            Method saveM = OverviewPanel.class.getDeclaredMethod("saveTodos");
            saveM.setAccessible(true);
            saveM.invoke(ov);
            Method delAllM = OverviewPanel.class.getDeclaredMethod("deleteAllArchived", boolean.class);
            delAllM.setAccessible(true);
            int nDel = (Integer) delAllM.invoke(ov, false);
            System.out.println("全部清空=" + nDel + " 文件=" + readAll());
            check("全部清空删掉 1 条归档", nDel == 1 && !readAll().contains("临时归档"));

            /* ---------- 4. 只看很久没动这一档，一键勾完 ---------- */
            JLabel lv = findLabel(bar, "很久没动");
            click(lv);
            Thread.sleep(400);
            System.out.println("档位标题=" + title.getText() + " 列表=" + labels(box) + " 标签=" + labels(bar));
            check("只看很久没动是 2 条", title.getText().equals("很久没动 · 2 条")
                    && labels(box).contains("九天前没动") && labels(box).contains("十六天前"));
            check("出现这一档全部勾完入口", labels(bar).contains("这一档全部勾完 2 条"));

            int done = (Integer) finish.invoke(ov);
            System.out.println("勾完=" + done + " 标题=" + title.getText() + " 文件=" + readAll());
            check("一键勾完 2 条并清掉筛选", done == 2 && lfF.getInt(ov) == 0
                    && title.getText().equals("今日待办"));
            check("两条确实写进 todo.txt 为完成", readAll().contains("1\t九天前没动\t2026-11-02")
                    && readAll().contains("1\t十六天前\t2026-10-26"));

            /* ---------- 5. 筛选状态跨页保留 ---------- */
            JLabel lv2 = findLabel(bar, "该处理了");
            click(lv2);
            Thread.sleep(400);
            check("点该处理了生效", title.getText().equals("该处理了 · 1 条"));
            Method sel = mc.getDeclaredMethod("selectNav", String.class);
            sel.setAccessible(true);
            SwingUtilities.invokeAndWait(() -> {
                try {
                    sel.invoke(main, "stats");
                    Thread.sleep(600);
                    sel.invoke(main, "overview");
                } catch (Exception e) {
                }
            });
            Thread.sleep(1200);
            System.out.println("切页回来标题=" + title.getText());
            check("切页回来筛选还在", title.getText().equals("该处理了 · 1 条") && lfF.getInt(ov) == 2);
            check("已写入 todoView", readSetting().contains("todoView=level:2"));

            /* ---------- 6. 重启后仍保留 ---------- */
            final OverviewPanel[] fresh = new OverviewPanel[1];
            SwingUtilities.invokeAndWait(() -> {
                try {
                    fresh[0] = new OverviewPanel();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            Thread.sleep(600);
            Field ntf = OverviewPanel.class.getDeclaredField("todoTitle");
            ntf.setAccessible(true);
            String t2 = ((JLabel) ntf.get(fresh[0])).getText();
            System.out.println("新建面板标题=" + t2);
            check("重启后恢复上次的档位筛选", t2.equals("该处理了 · 1 条"));

            /* 复位到 today */
            Method sd = OverviewPanel.class.getDeclaredMethod("showDate", java.time.LocalDate.class);
            sd.setAccessible(true);
            sd.invoke(fresh[0], new Object[]{null});

            System.out.println(ok ? "ALL PASS" : "HAS FAIL");
            System.exit(ok ? 0 : 1);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(2);
        }
    }

    static Class<?> todoClass() throws Exception {
        return Class.forName("com.toolbox.pc.OverviewPanel$Todo");
    }

    private static Object findTodo(Object ov, String text) throws Exception {
        Field tf = OverviewPanel.class.getDeclaredField("todos");
        tf.setAccessible(true);
        java.util.List<?> list = (java.util.List<?>) tf.get(ov);
        for (Object t : list) {
            Field f = t.getClass().getDeclaredField("text");
            f.setAccessible(true);
            if (String.valueOf(f.get(t)).equals(text)) return t;
        }
        return null;
    }

    private static String readAll() {
        StringBuilder sb = new StringBuilder();
        for (String l : Store.readLines("todo.txt")) sb.append(l).append('\n');
        return sb.toString();
    }

    private static String readLast() {
        return readAll();
    }

    private static String readSetting() {
        String s = "";
        try {
            for (String l : Store.readLines("settings.txt")) s += l + "\n";
        } catch (Exception e) {
        }
        return s;
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
