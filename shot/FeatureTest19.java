import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.List;

/** v1.39 三项验证：归档彻底删/撤销分开统计、多选全选反选、小窗摘要点击切视图 */
public class FeatureTest19 {

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
            Field fbF = OverviewPanel.class.getDeclaredField("futureBar");
            fbF.setAccessible(true);
            Field ladF = OverviewPanel.class.getDeclaredField("lastArchivedDeleted");
            ladF.setAccessible(true);
            Method rl = OverviewPanel.class.getDeclaredMethod("refreshTodoList");
            rl.setAccessible(true);

            /* ---------- 1. 多选条里有全选 / 反选 ---------- */
            msF.setBoolean(ov, true);
            LinkedHashSet<Object> picked = (LinkedHashSet<Object>) pkF.get(ov);
            picked.clear();
            run(rl, ov);
            Thread.sleep(500);
            JPanel bulk = (JPanel) bbF.get(ov);
            System.out.println("多选条=" + texts(bulk));
            check("多选条含全选/反选", texts(bulk).contains("全选") && texts(bulk).contains("反选"));

            /* ---------- 2. 全选 / 反选 ---------- */
            clickChip(bulk, "全选");
            Thread.sleep(500);
            int afterAll = ((LinkedHashSet<Object>) pkF.get(ov)).size();
            System.out.println("全选后=" + afterAll + " 条 · " + texts((JPanel) bbF.get(ov)));
            check("全选选中 4 条未完成", afterAll == 4
                    && texts((JPanel) bbF.get(ov)).contains("已选 4 条"));

            clickChip((JPanel) bbF.get(ov), "反选");
            Thread.sleep(500);
            int afterInv = ((LinkedHashSet<Object>) pkF.get(ov)).size();
            System.out.println("反选后=" + afterInv + " 条");
            check("反选清空选择", afterInv == 0);

            /* ---------- 3. 归档：彻底删 vs 可撤销 ---------- */
            msF.setBoolean(ov, false);
            picked.clear();
            run(rl, ov);
            Thread.sleep(400);
            Method archM = OverviewPanel.class.getDeclaredMethod("archivedTodos");
            archM.setAccessible(true);
            System.out.println("归档=" + ((List<?>) archM.invoke(ov)).size());
            check("两条旧条目已自动归档", ((List<?>) archM.invoke(ov)).size() == 2);

            Method delAll = OverviewPanel.class.getDeclaredMethod("deleteAllArchived", boolean.class);
            delAll.setAccessible(true);
            int nDel = (Integer) delAll.invoke(ov, false);
            Thread.sleep(400);
            System.out.println("清空=" + nDel + " 标签=" + texts((JPanel) fbF.get(ov)));
            check("清空 2 条并给出可撤销入口", nDel == 2
                    && texts((JPanel) fbF.get(ov)).contains("撤销清空归档 2 条"));

            /* 撤销一条后再彻底删另一条，撤销数要跟着减 */
            Method undo = OverviewPanel.class.getDeclaredMethod("undoDeleteArchived");
            undo.setAccessible(true);
            undo.invoke(ov);
            Thread.sleep(300);
            List<Object> arch2 = (List<Object>) archM.invoke(ov);
            check("撤销后归档回到 2 条", arch2.size() == 2);

            Method delOne = OverviewPanel.class.getDeclaredMethod("deleteArchived", Todo0());
            delOne.setAccessible(true);
            Object target = arch2.get(0);
            delOne.invoke(ov, target);
            Thread.sleep(400);
            String file = readAll();
            System.out.println("彻底删后归档=" + ((List<?>) archM.invoke(ov)).size()
                    + " 可撤销=" + ((List<Object>) ladF.get(ov)).size() + "\n" + file);
            check("单条彻底删后归档减 1", ((List<?>) archM.invoke(ov)).size() == 1
                    && file.split("\n").length == 6
                    && ((List<Object>) ladF.get(ov)).isEmpty());

            /* ---------- 4. 小窗摘要点击切视图 ---------- */
            SwingUtilities.invokeAndWait(() -> MiniBoard.show());
            Thread.sleep(1500);
            Field instF = MiniBoard.class.getDeclaredField("inst");
            instF.setAccessible(true);
            Object board = instF.get(null);
            Field sumF = MiniBoard.class.getDeclaredField("sumLbl");
            sumF.setAccessible(true);
            Field ovF = MiniBoard.class.getDeclaredField("overdueView");
            ovF.setAccessible(true);
            JLabel sum = (JLabel) sumF.get(board);
            System.out.println("摘要=" + sum.getText() + " 提示=" + sum.getToolTipText()
                    + " 过期视图=" + ovF.getBoolean(board));
            check("摘要提示可点切换", sum.getToolTipText() != null
                    && sum.getToolTipText().contains("点一下只看过期"));

            clickLabel(sum);
            Thread.sleep(700);
            System.out.println("点一次=" + ovF.getBoolean(board) + " 提示=" + sum.getToolTipText());
            check("点一下切到过期视图", ovF.getBoolean(board));

            clickLabel(sum);
            Thread.sleep(700);
            System.out.println("点两次=" + ovF.getBoolean(board) + " 提示=" + sum.getToolTipText());
            check("再点一下回到今天", !ovF.getBoolean(board));

            System.out.println(ok ? "ALL PASS" : "HAS FAIL");
            System.exit(ok ? 0 : 1);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(2);
        }
    }

    @SuppressWarnings("unchecked")
    private static Class<Object> Todo0() {
        try {
            return (Class<Object>) Class.forName("com.toolbox.pc.OverviewPanel$Todo");
        } catch (Exception e) {
            throw new RuntimeException(e);
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
