import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** v1.44 验证：多选「无日期」范围、归档右键复制到今天、小窗右键改期到具体日期 */
public class FeatureTest24 {

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

            /* 1. 范围切到「无日期」，只剩没定日期的那条 */
            Field brF = OverviewPanel.class.getDeclaredField("bulkRange");
            brF.setAccessible(true);
            Field msF = OverviewPanel.class.getDeclaredField("multiSelect");
            msF.setAccessible(true);
            msF.setBoolean(ov, true);
            brF.setInt(ov, 5);
            Method openM = OverviewPanel.class.getDeclaredMethod("openTodos");
            openM.setAccessible(true);
            List open = (List) openM.invoke(ov);
            check("无日期范围只剩 1 条", open.size() == 1, "条数=" + open.size());
            Method nameM = OverviewPanel.class.getDeclaredMethod("bulkRangeName");
            nameM.setAccessible(true);
            String rn = (String) nameM.invoke(ov);
            check("范围名是「只看没定日期的」", "只看没定日期的".equals(rn), rn);

            Method ubM = OverviewPanel.class.getDeclaredMethod("updateBulkBar");
            ubM.setAccessible(true);
            ubM.invoke(ov);
            Field rbF = OverviewPanel.class.getDeclaredField("rangeBar");
            rbF.setAccessible(true);
            JPanel rangeBar = (JPanel) rbF.get(ov);
            boolean hasNoDate = false;
            for (Component c : rangeBar.getComponents()) {
                if (c instanceof JLabel && "无日期".equals(((JLabel) c).getText())) hasNoDate = true;
            }
            check("范围条有「无日期」chip", hasNoDate, "rangeBar");

            /* 2. 归档右键：复制一条到今天 */
            List arch = null;
            Method am = OverviewPanel.class.getDeclaredMethod("archivedTodos");
            am.setAccessible(true);
            arch = (List) am.invoke(ov);
            check("归档有 2 条", arch.size() == 2, "条数=" + arch.size());
            Method cm = OverviewPanel.class.getDeclaredMethod("copyArchivedToToday",
                    Class.forName("com.toolbox.pc.OverviewPanel$Todo"));
            cm.setAccessible(true);
            int before = com.toolbox.pc.Store.readLines("todo.txt").size();
            cm.invoke(ov, arch.get(0));
            int after = com.toolbox.pc.Store.readLines("todo.txt").size();
            check("文件多了一行", after == before + 1, before + " → " + after);
            boolean todayLine = false;
            boolean archivedSuffix = false;
            for (String line : com.toolbox.pc.Store.readLines("todo.txt")) {
                if (line.endsWith("\t2026-11-11") && line.contains("二十天前")) {
                    if (line.contains("（已归档）")) archivedSuffix = true;
                    else todayLine = true;
                }
            }
            check("复制出来的条目日期是今天", todayLine, "todo.txt");
            check("复制时去掉了（已归档）后缀", !archivedSuffix, "后缀");
            int archAfter = ((List) am.invoke(ov)).size();
            check("归档里那条还在", archAfter == 2, "归档条数=" + archAfter);

            /* 3. 小窗待办右键多了「改期到…」 */
            Field instF = MiniBoard.class.getDeclaredField("inst");
            instF.setAccessible(true);
            Object board = instF.get(null);
            if (board == null) {
                SwingUtilities.invokeAndWait(() -> MiniBoard.show());
                Thread.sleep(900);
                board = instF.get(null);
            }
            Method br = MiniBoard.class.getDeclaredMethod("refresh");
            br.setAccessible(true);
            br.invoke(board);
            Thread.sleep(700);

            Method pm = MiniBoard.class.getDeclaredMethod("popupTodo",
                    MouseEvent.class, Component.class, String.class);
            pm.setAccessible(true);
            Field lblF = MiniBoard.class.getDeclaredField("listBox");
            lblF.setAccessible(true);
            Component inv = (Component) lblF.get(board);
            MouseEvent pop = new MouseEvent(inv, MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(),
                    0, 20, 10, 1, true, MouseEvent.BUTTON3);
            final Object bf = board;
            SwingUtilities.invokeAndWait(() -> {
                try {
                    pm.invoke(bf, pop, inv, "今天的事");
                } catch (Exception e) {
                }
            });
            Thread.sleep(300);
            Field tmF = MiniBoard.class.getDeclaredField("lastTodoMenu");
            tmF.setAccessible(true);
            JPopupMenu menu = (JPopupMenu) tmF.get(board);
            int items = 0;
            boolean hasPick = false;
            for (Component c : menu.getComponents()) {
                if (c instanceof JMenuItem) {
                    items++;
                    if ("改期到…".equals(((JMenuItem) c).getText())) hasPick = true;
                }
            }
            check("右键菜单 4 项", items == 4, "items=" + items);
            check("含「改期到…」", hasPick, "menu");

            System.out.println(ok ? "ALL PASS" : "SOME FAIL");
        } catch (Throwable t) {
            t.printStackTrace();
            ok = false;
        }
        Runtime.getRuntime().halt(ok ? 0 : 1);
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
