import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** v1.48 验证：多选「改期到…」下拉、归档搜索清空、＋ 右键带日期 */
public class FeatureTest28 {

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

            /* 1. 多选条收敛成 7 个 chip */
            Field msF = OverviewPanel.class.getDeclaredField("multiSelect");
            msF.setAccessible(true);
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
            int chips = 0;
            java.util.List<String> names = new java.util.ArrayList<String>();
            for (Component c : bulkBar.getComponents()) {
                if (c instanceof JLabel) {
                    chips++;
                    names.add(((JLabel) c).getText());
                }
            }
            check("多选条 7 个 chip", chips == 7, "chips=" + chips + " " + names);
            boolean onlyOneDefer = true;
            for (String n : names) {
                if (n.startsWith("改期到") && !"改期到…".equals(n)) onlyOneDefer = false;
            }
            check("改期类只剩一个入口", onlyOneDefer, names.toString());

            /* 2. 点开「改期到…」下拉：3 项带日期 + 自己选 */
            JLabel defer = null;
            for (Component c : bulkBar.getComponents()) {
                if (c instanceof JLabel && "改期到…".equals(((JLabel) c).getText())) defer = (JLabel) c;
            }
            check("找到「改期到…」", defer != null, "defer");
            final JLabel df = defer;
            if (df != null) {
                MouseEvent click = new MouseEvent(df, MouseEvent.MOUSE_CLICKED,
                        System.currentTimeMillis(), 0, 5, 5, 1, false, MouseEvent.BUTTON1);
                SwingUtilities.invokeAndWait(() -> {
                    for (java.awt.event.MouseListener ml : df.getMouseListeners()) ml.mouseClicked(click);
                });
                Thread.sleep(300);
                Field lbmF = OverviewPanel.class.getDeclaredField("lastBulkMenu");
                lbmF.setAccessible(true);
                JPopupMenu menu = (JPopupMenu) lbmF.get(ov);
                int items = 0;
                String texts = "";
                for (Component c : menu.getComponents()) {
                    if (c instanceof JMenuItem) {
                        items++;
                        texts += ((JMenuItem) c).getText() + " | ";
                    }
                }
                check("下拉 4 项", items == 4, "items=" + items);
                check("明天带日期 11-12", texts.contains("11-12"), texts);
                check("本周五带日期 11-13", texts.contains("11-13"), texts);
                check("下周一带日期 11-16", texts.contains("11-16"), texts);
                check("有自己选一天", texts.contains("自己选一天"), texts);
            }

            /* 3. 归档搜索框旁边有「清空」 */
            Method apM = OverviewPanel.class.getDeclaredMethod("archivePanel", JDialog.class, int[].class);
            apM.setAccessible(true);
            JPanel panel = (JPanel) apM.invoke(ov, null, new int[]{-1});
            panel.setSize(panel.getPreferredSize());
            panel.doLayout();
            JLabel clear = null;
            JTextField search = null;
            for (JLabel l : allLabels(panel)) {
                if ("清空".equals(l.getText())) clear = l;
            }
            for (Component c : all(panel)) {
                if (c instanceof JTextField) search = (JTextField) c;
            }
            check("归档有「清空」", clear != null, "clear");
            final JLabel cl = clear;
            final JTextField sf = search;
            if (cl != null && sf != null) {
                sf.setText("四十天前");
                Thread.sleep(300);
                MouseEvent click = new MouseEvent(cl, MouseEvent.MOUSE_CLICKED,
                        System.currentTimeMillis(), 0, 4, 4, 1, false, MouseEvent.BUTTON1);
                SwingUtilities.invokeAndWait(() -> {
                    for (java.awt.event.MouseListener ml : cl.getMouseListeners()) ml.mouseClicked(click);
                });
                Thread.sleep(300);
                check("点了清空后搜索词为空", sf.getText().isEmpty(), "[" + sf.getText() + "]");
            }

            /* 4. 小窗「＋」右键带日期 */
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
                String texts = "";
                for (Component c : menu.getComponents()) {
                    if (c instanceof JMenuItem) texts += ((JMenuItem) c).getText() + " | ";
                }
                check("＋菜单首项带 11-11", texts.contains("11-11"), texts);
                check("＋菜单有 11-12 与 11-13", texts.contains("11-12") && texts.contains("11-13"), texts);
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

    static java.util.List<Component> all(Component root) {
        java.util.List<Component> out = new java.util.ArrayList<Component>();
        walk(root, out);
        return out;
    }

    static void walk(Component c, java.util.List<Component> out) {
        out.add(c);
        if (c instanceof Container) {
            for (Component k : ((Container) c).getComponents()) walk(k, out);
        }
    }

    static java.util.List<JLabel> allLabels(Component root) {
        java.util.List<JLabel> out = new java.util.ArrayList<JLabel>();
        for (Component c : all(root)) if (c instanceof JLabel) out.add((JLabel) c);
        return out;
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
