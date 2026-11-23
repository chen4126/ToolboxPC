import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** v1.47 验证：多选「改期到下周一」「清除选择」、小窗「＋」右键四项菜单 */
public class FeatureTest27 {

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

            /* 进入多选，看 chip 行 */
            Field msF = OverviewPanel.class.getDeclaredField("multiSelect");
            msF.setAccessible(true);
            Field brF = OverviewPanel.class.getDeclaredField("bulkRange");
            brF.setAccessible(true);
            Field pkF = OverviewPanel.class.getDeclaredField("picked");
            pkF.setAccessible(true);
            Method openM = OverviewPanel.class.getDeclaredMethod("openTodos");
            openM.setAccessible(true);
            List open = (List) openM.invoke(ov);
            msF.setBoolean(ov, true);
            ((java.util.Set) pkF.get(ov)).clear();
            ((java.util.Set) pkF.get(ov)).add(open.get(0));
            Method ubM = OverviewPanel.class.getDeclaredMethod("updateBulkBar");
            ubM.setAccessible(true);
            ubM.invoke(ov);
            Thread.sleep(400);

            Field bfF = OverviewPanel.class.getDeclaredField("bulkBar");
            bfF.setAccessible(true);
            JPanel bulkBar = (JPanel) bfF.get(ov);
            java.util.List<JLabel> chips = new java.util.ArrayList<JLabel>();
            for (Component c : bulkBar.getComponents()) {
                if (c instanceof JLabel) chips.add((JLabel) c);
            }
            String[] names = new String[chips.size()];
            for (int i = 0; i < chips.size(); i++) names[i] = chips.get(i).getText();
            boolean hasMon = false;
            boolean hasClear = false;
            for (String n : names) {
                if ("改期到下周一".equals(n)) hasMon = true;
                if ("清除选择".equals(n)) hasClear = true;
            }
            check("多选条有「改期到下周一」", hasMon, "chips=" + chips.size());
            check("多选条有「清除选择」", hasClear, "chips=" + chips.size());

            /* 下周一换算：11-11 周三 → 11-16 周一 */
            java.time.LocalDate now = java.time.LocalDate.now();
            java.time.LocalDate mon = now.with(java.time.DayOfWeek.MONDAY);
            if (!mon.isAfter(now)) mon = mon.plusWeeks(1);
            check("下周一算成 11-16", "2026-11-16".equals(mon.toString()), mon.toString());

            /* 点「清除选择」 */
            JLabel clear = null;
            for (JLabel l : chips) if ("清除选择".equals(l.getText())) clear = l;
            if (clear != null) {
                MouseEvent click = new MouseEvent(clear, MouseEvent.MOUSE_CLICKED,
                        System.currentTimeMillis(), 0, 5, 5, 1, false, MouseEvent.BUTTON1);
                final JLabel cl = clear;
                SwingUtilities.invokeAndWait(() -> {
                    for (java.awt.event.MouseListener ml : cl.getMouseListeners()) ml.mouseClicked(click);
                });
                Thread.sleep(500);
                check("清除后退出多选", !msF.getBoolean(ov), "multiSelect=" + msF.getBoolean(ov));
                check("清除后选中为空", ((java.util.Set) pkF.get(ov)).isEmpty(), "picked");
                check("清除后范围回全部", brF.getInt(ov) == 0, "bulkRange=" + brF.getInt(ov));
            }

            /* 小窗「＋」右键菜单 */
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
            JFrame win = (JFrame) getField(board, "win");
            JLabel plus = null;
            for (JLabel l : allLabels(win.getContentPane())) {
                if ("＋".equals(l.getText())) plus = l;
            }
            check("小窗有「＋」", plus != null, "plus");
            if (plus != null) {
                MouseEvent pop = new MouseEvent(plus, MouseEvent.MOUSE_RELEASED,
                        System.currentTimeMillis(), 0, 5, 5, 1, true, MouseEvent.BUTTON3);
                Method pm = MiniBoard.class.getDeclaredMethod("plusMenu", MouseEvent.class);
                pm.setAccessible(true);
                final Object bf = board;
                SwingUtilities.invokeAndWait(() -> {
                    try {
                        pm.invoke(bf, pop);
                    } catch (Exception e) {
                    }
                });
                Thread.sleep(300);
                Field lpF = MiniBoard.class.getDeclaredField("lastPlusMenu");
                lpF.setAccessible(true);
                JPopupMenu menu = (JPopupMenu) lpF.get(board);
                int items = 0;
                String first = "";
                String last = "";
                for (Component c : menu.getComponents()) {
                    if (c instanceof JMenuItem) {
                        if (items == 0) first = ((JMenuItem) c).getText();
                        last = ((JMenuItem) c).getText();
                        items++;
                    }
                }
                check("「＋」右键 4 项", items == 4, "items=" + items);
                check("首项是今天", "新建到今天".equals(first), first);
                check("末项是选一天", "选一天…".equals(last), last);
            }

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

    static java.util.List<JLabel> allLabels(Component root) {
        java.util.List<JLabel> out = new java.util.ArrayList<JLabel>();
        collect(root, out);
        return out;
    }

    static void collect(Component c, java.util.List<JLabel> out) {
        if (c instanceof JLabel) out.add((JLabel) c);
        if (c instanceof Container) {
            for (Component k : ((Container) c).getComponents()) collect(k, out);
        }
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
