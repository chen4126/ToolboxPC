import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;

/** v1.43 验证：通知卡片点开跳页、多选复制到某天、小窗过期红字右键一键挪到今天 */
public class FeatureTest23 {

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

            OverviewPanel ov = find(frame.getContentPane(), OverviewPanel.class);
            Method refresh = OverviewPanel.class.getDeclaredMethod("refresh");
            refresh.setAccessible(true);
            refresh.invoke(ov);
            Thread.sleep(600);

            /* 1. 多选复制：选 2 条，复制到 11-13，原条目不动 */
            Method openM = OverviewPanel.class.getDeclaredMethod("openTodos");
            openM.setAccessible(true);
            @SuppressWarnings({"unchecked","rawtypes"})
            List open = (List) openM.invoke(ov);
            Field msF = OverviewPanel.class.getDeclaredField("multiSelect");
            msF.setAccessible(true);
            Field pkF = OverviewPanel.class.getDeclaredField("picked");
            pkF.setAccessible(true);
            @SuppressWarnings({"unchecked","rawtypes"})
            java.util.Set picked = (java.util.Set) pkF.get(ov);
            msF.setBoolean(ov, true);
            picked.clear();
            picked.add(open.get(0));
            picked.add(open.get(1));
            int before = com.toolbox.pc.Store.readLines("todo.txt").size();
            Method copyM = OverviewPanel.class.getDeclaredMethod("batchCopy", LocalDate.class);
            copyM.setAccessible(true);
            int copied = (Integer) copyM.invoke(ov, LocalDate.of(2026, 11, 13));
            int after = com.toolbox.pc.Store.readLines("todo.txt").size();
            check("复制返回 2 条", copied == 2, "copied=" + copied);
            check("文件多了 2 行（原条目没删）", after == before + 2, before + " → " + after);
            int onDay = 0;
            for (String line : com.toolbox.pc.Store.readLines("todo.txt")) {
                if (line.endsWith("2026-11-13")) onDay++;
            }
            check("11-13 那天有 2 条副本", onDay == 2, "条数=" + onDay);
            check("复制后退出多选", !msF.getBoolean(ov) && picked.isEmpty(), "multiSelect/picked");

            /* 2. 多选条上出现了「复制到…」 */
            Method ubM = OverviewPanel.class.getDeclaredMethod("updateBulkBar");
            ubM.setAccessible(true);
            msF.setBoolean(ov, true);
            picked.addAll(open);
            ubM.invoke(ov);
            Field bf = OverviewPanel.class.getDeclaredField("bulkBar");
            bf.setAccessible(true);
            JPanel bulkBar = (JPanel) bf.get(ov);
            boolean hasCopy = false;
            for (Component c : bulkBar.getComponents()) {
                if (c instanceof JLabel && "复制到…".equals(((JLabel) c).getText())) hasCopy = true;
            }
            check("多选条有「复制到…」", hasCopy, "bulkBar");
            picked.clear();
            msF.setBoolean(ov, false);

            /* 3. 通知卡片：带跳转的点击动作能执行，且卡片自己关掉 */
            final int[] fired = {0};
            Method toastShow = Toast.class.getDeclaredMethod("show", String.class, String.class, Runnable.class);
            toastShow.setAccessible(true);
            toastShow.invoke(null, "已批量删除", "删掉 2 条待办（测试）", (Runnable) () -> fired[0]++);
            Thread.sleep(400);
            Field stackF = Toast.class.getDeclaredField("stack");
            stackF.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<JWindow> stack = (List<JWindow>) stackF.get(null);
            int n1 = stack.size();
            JWindow w = stack.get(stack.size() - 1);
            Component card = w.getContentPane();
            MouseEvent click = new MouseEvent(card, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
                    0, 40, 30, 1, false, MouseEvent.BUTTON1);
            for (java.awt.event.MouseListener ml : card.getMouseListeners()) ml.mouseClicked(click);
            Thread.sleep(500);
            check("点卡片执行了跳转动作", fired[0] == 1, "fired=" + fired[0]);
            check("点完卡片自己消失", stack.size() == n1 - 1, n1 + " → " + stack.size());

            /* 4. 小窗过期红字右键菜单三项 */
            Field instF = MiniBoard.class.getDeclaredField("inst");
            instF.setAccessible(true);
            Object board = instF.get(null);
            if (board == null) {
                SwingUtilities.invokeAndWait(() -> MiniBoard.show());
                Thread.sleep(800);
                board = instF.get(null);
            }
            Method mp = MiniBoard.class.getDeclaredMethod("overduePopup", MouseEvent.class);
            mp.setAccessible(true);
            Field olF = MiniBoard.class.getDeclaredField("overdueLbl");
            olF.setAccessible(true);
            JLabel lbl = (JLabel) olF.get(board);
            MouseEvent pop = new MouseEvent(lbl, MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(),
                    0, 20, 10, 1, true, MouseEvent.BUTTON3);
            final Object bf2 = board;
            SwingUtilities.invokeAndWait(() -> {
                try {
                    mp.invoke(bf2, pop);
                } catch (Exception e) {
                }
            });
            Thread.sleep(300);
            Field omF = MiniBoard.class.getDeclaredField("lastOverdueMenu");
            omF.setAccessible(true);
            JPopupMenu menu = (JPopupMenu) omF.get(board);
            int items = 0;
            String first = "";
            if (menu != null) {
                for (Component c : menu.getComponents()) {
                    if (c instanceof JMenuItem) {
                        if (items == 0) first = ((JMenuItem) c).getText();
                        items++;
                    }
                }
            }
            check("右键出现 3 项", items == 3, "items=" + items);
            check("第一项是挪到今天", first.contains("全挪到今天"), first);

            /* 5. 一键挪到今天：3 条过期变今天，可撤销 */
            Method mm = MiniBoard.class.getDeclaredMethod("moveAllOverdue");
            mm.setAccessible(true);
            int moved = (Integer) mm.invoke(board);
            check("挪了 3 条过期待办", moved == 3, "moved=" + moved);
            check("小窗回到今日视图", !(Boolean) getField(board, "overdueView"), "overdueView");
            int movedAgain = (Integer) mm.invoke(board);
            check("再点一次没有可挪的", movedAgain == 0, "again=" + movedAgain);

            Method undoM = OverviewPanel.class.getDeclaredMethod("undoMoveOverdue");
            undoM.setAccessible(true);
            int back = (Integer) undoM.invoke(ov);
            check("主窗可撤销 3 条", back == 3, "undo=" + back);

            System.out.println(ok ? "ALL PASS" : "SOME FAIL");
        } catch (Throwable t) {
            t.printStackTrace();
            ok = false;
        }
        Runtime.getRuntime().halt(ok ? 0 : 1);
    }

    static Object getField(Object target, String name) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    static void check(String name, boolean pass, String detail) {
        if (!pass) ok = false;
        System.out.println((pass ? "[PASS] " : "[FAIL] ") + name + " → " + detail);
    }

    static <T extends Component> T find(Component c, Class<T> type) {
        if (type.isInstance(c)) return type.cast(c);
        if (c instanceof Container) {
            for (Component k : ((Container) c).getComponents()) {
                T t = find(k, type);
                if (t != null) return t;
            }
        }
        return null;
    }
}
