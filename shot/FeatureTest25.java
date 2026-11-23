import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** v1.45 验证：归档右键「恢复成原日期」、小窗右键「复制到剪贴板」、小窗底部「＋」新建 */
public class FeatureTest25 {

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

            /* 1. 归档右键菜单两项 */
            Method apM = OverviewPanel.class.getDeclaredMethod("archivePanel", JDialog.class, int[].class);
            apM.setAccessible(true);
            JPanel panel = (JPanel) apM.invoke(ov, null, new int[]{-1});
            panel.setSize(panel.getPreferredSize());
            panel.doLayout();
            JLabel target = null;
            for (Component c : allLabels(panel)) {
                if (c instanceof JLabel && ((JLabel) c).getText() != null
                        && ((JLabel) c).getText().contains("二十天前")) {
                    target = (JLabel) c;
                    break;
                }
            }
            JPopupMenu menu = target == null ? null : target.getComponentPopupMenu();
            int items = 0;
            boolean hasOrigin = false;
            if (menu != null) {
                for (Component c : menu.getComponents()) {
                    if (c instanceof JMenuItem) {
                        items++;
                        if ("恢复成原日期".equals(((JMenuItem) c).getText())) hasOrigin = true;
                    }
                }
            }
            check("归档右键 2 项", items == 2, "items=" + items);
            check("含「恢复成原日期」", hasOrigin, "menu");

            /* 2. 恢复成原日期：日期保持 10-22，文本去后缀，done 变 false */
            Method am = OverviewPanel.class.getDeclaredMethod("archivedTodos");
            am.setAccessible(true);
            List arch = (List) am.invoke(ov);
            @SuppressWarnings({"unchecked", "rawtypes"})
            Object t0 = arch.get(0);
            Method km = OverviewPanel.class.getDeclaredMethod("recoverArchivedKeepDate",
                    Class.forName("com.toolbox.pc.OverviewPanel$Todo"));
            km.setAccessible(true);
            km.invoke(ov, t0);
            Field doneF = t0.getClass().getDeclaredField("done");
            doneF.setAccessible(true);
            Field dateF = t0.getClass().getDeclaredField("date");
            dateF.setAccessible(true);
            Field textF = t0.getClass().getDeclaredField("text");
            textF.setAccessible(true);
            check("恢复后 unfinish", !doneF.getBoolean(t0), "done=" + doneF.getBoolean(t0));
            check("日期仍是 2026-10-22", "2026-10-22".equals(dateF.get(t0)), String.valueOf(dateF.get(t0)));
            check("文本去掉（已归档）", !String.valueOf(textF.get(t0)).contains("（已归档）"),
                    String.valueOf(textF.get(t0)));
            check("归档清单少一条", ((List) am.invoke(ov)).size() == 1, "归档条数="
                    + ((List) am.invoke(ov)).size());

            /* 3. 小窗待办右键五项，含复制到剪贴板 */
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
            Field lbF = MiniBoard.class.getDeclaredField("listBox");
            lbF.setAccessible(true);
            Component inv = (Component) lbF.get(board);
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
            JPopupMenu tmenu = (JPopupMenu) tmF.get(board);
            int tItems = 0;
            String copyText = "";
            for (Component c : tmenu.getComponents()) {
                if (c instanceof JMenuItem) {
                    tItems++;
                    if (((JMenuItem) c).getText().contains("剪贴板")) copyText = ((JMenuItem) c).getText();
                }
            }
            check("小窗右键 5 项", tItems == 5, "items=" + tItems);
            check("含复制到剪贴板", "复制到剪贴板".equals(copyText), copyText);

            /* 4. 底部有「＋」新建入口 */
            JFrame win = (JFrame) getField(board, "win");
            boolean hasPlus = false;
            for (JLabel l : allLabels(win.getContentPane())) {
                if ("＋".equals(l.getText())) hasPlus = true;
            }
            check("小窗底部有「＋」", hasPlus, "footer");
            Method addM = MiniBoard.class.getDeclaredMethod("addTodayByButton");
            addM.setAccessible(true);
            check("新建入口方法存在", addM != null, "addTodayByButton");

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
