import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.List;

/** v1.38 三项验证：待办多选批量改期/删除、清空归档可撤销、小窗顶部条数摘要 */
public class FeatureTest18 {

    private static boolean ok = true;

    public static void main(String[] args) throws Exception {
        try {
            Class<?> mc = Class.forName("com.toolbox.pc.Main");
            SwingUtilities.invokeAndWait(() -> Main.main(new String[0]));
            Thread.sleep(2500);

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
            Thread.sleep(1300);

            OverviewPanel ov = page(OverviewPanel.class);
            Method refresh = OverviewPanel.class.getDeclaredMethod("refresh");
            refresh.setAccessible(true);
            run(refresh, ov);
            Thread.sleep(600);

            Field msF = OverviewPanel.class.getDeclaredField("multiSelect");
            msF.setAccessible(true);
            Field pkF = OverviewPanel.class.getDeclaredField("picked");
            pkF.setAccessible(true);
            Field bbF = OverviewPanel.class.getDeclaredField("bulkBar");
            bbF.setAccessible(true);
            Field todosF = OverviewPanel.class.getDeclaredField("todos");
            todosF.setAccessible(true);
            Field ladF = OverviewPanel.class.getDeclaredField("lastArchivedDeleted");
            ladF.setAccessible(true);
            JPanel bulkBar = (JPanel) bbF.get(ov);

            /* ---------- 1. 多选模式与底部操作条 ---------- */
            msF.setBoolean(ov, true);
            LinkedHashSet<Object> picked = (LinkedHashSet<Object>) pkF.get(ov);
            picked.clear();
            run(refreshList(ov), ov);
            Thread.sleep(500);
            System.out.println("多选条=" + texts(bulkBar) + " 可见=" + bulkBar.isVisible());
            check("多选模式显示操作条", bulkBar.isVisible() && texts(bulkBar).contains("点上面的待办选中它")
                    && texts(bulkBar).contains("改期到明天") && texts(bulkBar).contains("删除选中"));
            Field titleF = OverviewPanel.class.getDeclaredField("todoTitle");
            titleF.setAccessible(true);
            Field boxF = OverviewPanel.class.getDeclaredField("todoBox");
            boxF.setAccessible(true);
            JLabel title = (JLabel) titleF.get(ov);
            JPanel box = (JPanel) boxF.get(ov);
            System.out.println("标题=" + title.getText() + " 行数=" + box.getComponentCount());
            check("多选模式列出全部未完成 4 条", title.getText().contains("全部未完成 · 4 条")
                    && box.getComponentCount() == 4);

            /* ---------- 2. 选中两条 ---------- */
            Object t2 = findTodo(ov, "九天前没动");
            Object t3 = findTodo(ov, "四天前");
            check("找到两条待办", t2 != null && t3 != null);
            picked.add(t2);
            picked.add(t3);
            run(refreshList(ov), ov);
            Thread.sleep(400);
            System.out.println("选中后=" + texts(bulkBar));
            check("底部显示已选 2 条", texts(bulkBar).contains("已选 2 条"));

            /* ---------- 3. 批量改期到明天 ---------- */
            Method bm = OverviewPanel.class.getDeclaredMethod("batchMove", java.time.LocalDate.class);
            bm.setAccessible(true);
            int moved = (Integer) bm.invoke(ov, java.time.LocalDate.now().plusDays(1));
            String f1 = readAll();
            System.out.println("批量改期=" + moved + "\n" + f1);
            check("批量改期 2 条", moved == 2
                    && f1.contains("九天前没动\t2026-11-12") && f1.contains("四天前\t2026-11-12"));
            check("改期后退出多选", !msF.getBoolean(ov) && !bulkBar.isVisible());

            /* ---------- 4. 批量删除 ---------- */
            msF.setBoolean(ov, true);
            picked.clear();
            picked.add(findTodo(ov, "九天前没动"));
            picked.add(findTodo(ov, "四天前"));
            Method bd = OverviewPanel.class.getDeclaredMethod("batchDelete");
            bd.setAccessible(true);
            int deleted = (Integer) bd.invoke(ov);
            String f2 = readAll();
            System.out.println("批量删除=" + deleted + "\n" + f2);
            check("批量删除 2 条", deleted == 2
                    && !f2.contains("九天前没动") && !f2.contains("四天前"));

            /* ---------- 5. 清空归档可撤销 ---------- */
            Method delAll = OverviewPanel.class.getDeclaredMethod("deleteAllArchived", boolean.class);
            delAll.setAccessible(true);
            Field fbF = OverviewPanel.class.getDeclaredField("futureBar");
            fbF.setAccessible(true);
            int nDel = (Integer) delAll.invoke(ov, false);
            List<Object> snap = (List<Object>) ladF.get(ov);
            System.out.println("清空=" + nDel + " 快照=" + snap.size()
                    + " 标签=" + texts((JPanel) fbF.get(ov)));
            check("清空归档留下快照", nDel == 1 && snap.size() == 1);
            check("底部出现撤销清空入口", texts((JPanel) fbF.get(ov)).contains("撤销清空归档 1 条"));

            Method undo = OverviewPanel.class.getDeclaredMethod("undoDeleteArchived");
            undo.setAccessible(true);
            int back = (Integer) undo.invoke(ov);
            Method archM = OverviewPanel.class.getDeclaredMethod("archivedTodos");
            archM.setAccessible(true);
            System.out.println("撤销=" + back + " 归档=" + ((List<?>) archM.invoke(ov)).size()
                    + " 文件=" + readAll());
            check("撤销清空后归档回来", back == 1 && ((List<?>) archM.invoke(ov)).size() == 1
                    && readAll().contains("四十天前（已归档）"));

            /* ---------- 6. 小窗顶部条数摘要 ---------- */
            SwingUtilities.invokeAndWait(() -> MiniBoard.show());
            Thread.sleep(1500);
            Field sumF = MiniBoard.class.getDeclaredField("sumLbl");
            sumF.setAccessible(true);
            Object board = null;
            for (Frame f : Frame.getFrames()) {
                if (f.getClass().getName().contains("MiniBoard")) board = f;
            }
            System.out.println("小窗=" + (board != null));
            JLabel sum = null;
            for (Frame f : Frame.getFrames()) {
                JLabel l = findLabelDeep(f, sumF);
                if (l != null) sum = l;
            }
            System.out.println("摘要=" + (sum == null ? "null" : sum.getText()));
            check("小窗顶部有今天/过期摘要", sum != null
                    && sum.getText().startsWith("今天 ") && sum.getText().contains("过期"));

            System.out.println(ok ? "ALL PASS" : "HAS FAIL");
            System.exit(ok ? 0 : 1);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(2);
        }
    }

    private static JLabel findLabelDeep(Container c, Field f) {
        try {
            for (Component k : c.getComponents()) {
                if (k instanceof JLabel && f.getName().equals("sumLbl")) {
                    JLabel l = (JLabel) k;
                    if (l.getText() != null && l.getText().startsWith("今天")) return l;
                }
                if (k instanceof Container) {
                    JLabel r = findLabelDeep((Container) k, f);
                    if (r != null) return r;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static Method refreshList(OverviewPanel ov) throws Exception {
        Method m = OverviewPanel.class.getDeclaredMethod("refreshTodoList");
        m.setAccessible(true);
        return m;
    }

    private static Object findTodo(Object ov, String text) throws Exception {
        Field tf = OverviewPanel.class.getDeclaredField("todos");
        tf.setAccessible(true);
        for (Object t : (List<?>) tf.get(ov)) {
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

    private static String texts(JPanel box) {
        StringBuilder sb = new StringBuilder();
        for (Component c : box.getComponents()) {
            if (c instanceof JLabel) sb.append(((JLabel) c).getText()).append('|');
            else if (c instanceof AbstractButton) sb.append(((AbstractButton) c).getText()).append('|');
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
