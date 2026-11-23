import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** v1.49 验证：待办卡「只看过期」开关、＋ 菜单"上次那天"、归档"复制全部" */
public class FeatureTest29 {

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

            /* 1. 「只看过期」开关 */
            Field obF = OverviewPanel.class.getDeclaredField("onlyOverdueBtn");
            obF.setAccessible(true);
            JLabel ovBtn = (JLabel) obF.get(ov);
            check("待办卡有「只看过期」", ovBtn != null && "只看过期".equals(ovBtn.getText()),
                    ovBtn == null ? "null" : ovBtn.getText());
            Field olF = OverviewPanel.class.getDeclaredField("overdueLevel");
            olF.setAccessible(true);
            final JLabel vb = ovBtn;
            MouseEvent click = new MouseEvent(vb, MouseEvent.MOUSE_CLICKED,
                    System.currentTimeMillis(), 0, 4, 4, 1, false, MouseEvent.BUTTON1);
            SwingUtilities.invokeAndWait(() -> {
                for (java.awt.event.MouseListener ml : vb.getMouseListeners()) ml.mouseClicked(click);
            });
            Thread.sleep(500);
            check("点了进入全部过期档", olF.getInt(ov) == 9, "overdueLevel=" + olF.getInt(ov));
            check("按钮文案变「取消过期」", "取消过期".equals(vb.getText()), vb.getText());
            Method foM = OverviewPanel.class.getDeclaredMethod("filteredOverdueTodos");
            foM.setAccessible(true);
            List hit = (List) foM.invoke(ov);
            check("过期条数 3 条", hit.size() == 3, "hit=" + hit.size());
            SwingUtilities.invokeAndWait(() -> {
                for (java.awt.event.MouseListener ml : vb.getMouseListeners()) ml.mouseClicked(click);
            });
            Thread.sleep(500);
            check("再点回到今天", olF.getInt(ov) == 0, "overdueLevel=" + olF.getInt(ov));
            check("按钮文案回「只看过期」", "只看过期".equals(vb.getText()), vb.getText());

            /* 2. ＋ 右键：没有记忆时 4 项；新建后多一项"上次那天" */
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
            String before = menuTexts(board, plus);
            check("没记忆时 4 项", count(menuTextsItems(board)) == 4, before);

            Method ws = MiniBoard.class.getDeclaredMethod("writeSetting", String.class, String.class);
            ws.setAccessible(true);
            ws.invoke(null, "boardAddDay", "2026-11-20");
            Thread.sleep(100);
            String after = menuTexts(board, plus);
            check("有记忆时 5 项", count(menuTextsItems(board)) == 5, after);
            check("首项是上次那天 11-20", after.contains("上次那天（11-20）"), after);

            /* 3. 归档「复制全部」 */
            Method apM = OverviewPanel.class.getDeclaredMethod("archivePanel", JDialog.class, int[].class);
            apM.setAccessible(true);
            JPanel panel = (JPanel) apM.invoke(ov, null, new int[]{-1});
            panel.setSize(680, 400);
            layout(panel);
            boolean hasCopyAll = false;
            for (Component c : all(panel)) {
                if (c instanceof JButton && "复制全部".equals(((JButton) c).getText())) hasCopyAll = true;
            }
            check("归档有「复制全部」", hasCopyAll, "copyAll");

            Method caM = OverviewPanel.class.getDeclaredMethod("copyAllArchived");
            caM.setAccessible(true);
            int n = (Integer) caM.invoke(ov);
            check("复制了 2 条", n == 2, "n=" + n);
            String clip = "";
            try {
                clip = (String) java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                        .getData(java.awt.datatransfer.DataFlavor.stringFlavor);
            } catch (Exception e) {
                clip = "ERR " + e;
            }
            check("剪贴板里两行且去掉了（已归档）",
                    clip.contains("二十天前") && clip.contains("四十天前")
                            && !clip.contains("（已归档）") && clip.indexOf('\n') > 0,
                    clip.replace("\n", "\\n"));

            System.out.println(ok ? "ALL PASS" : "SOME FAIL");
        } catch (Throwable t) {
            t.printStackTrace();
            ok = false;
        }
        Runtime.getRuntime().halt(ok ? 0 : 1);
    }

    static String menuTextsItems(Object board) throws Exception {
        Field lpF = MiniBoard.class.getDeclaredField("lastPlusMenu");
        lpF.setAccessible(true);
        JPopupMenu menu = (JPopupMenu) lpF.get(board);
        String s = "";
        for (Component c : menu.getComponents()) if (c instanceof JMenuItem) s += ((JMenuItem) c).getText() + " | ";
        return s;
    }

    static int count(String s) {
        if (s.isEmpty()) return 0;
        int n = 0;
        for (String p : s.split(" \\| ")) if (!p.trim().isEmpty()) n++;
        return n;
    }

    static String menuTexts(Object board, JLabel plus) throws Exception {
        MouseEvent pop = new MouseEvent(plus, MouseEvent.MOUSE_RELEASED,
                System.currentTimeMillis(), 0, 5, 5, 1, true, MouseEvent.BUTTON3);
        Method pm = MiniBoard.class.getDeclaredMethod("plusMenu", MouseEvent.class);
        pm.setAccessible(true);
        SwingUtilities.invokeAndWait(() -> {
            try {
                pm.invoke(board, pop);
            } catch (Exception e) {
            }
        });
        Thread.sleep(300);
        return menuTextsItems(board);
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
        if (c instanceof Container) for (Component k : ((Container) c).getComponents()) walk(k, out);
    }

    static java.util.List<JLabel> allLabels(Component root) {
        java.util.List<JLabel> out = new java.util.ArrayList<JLabel>();
        for (Component c : all(root)) if (c instanceof JLabel) out.add((JLabel) c);
        return out;
    }

    static void layout(Component c) {
        c.doLayout();
        if (c instanceof Container) for (Component k : ((Container) c).getComponents()) layout(k);
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
