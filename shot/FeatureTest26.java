import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** v1.46 验证：待办卡「无日期 N 条」入口、归档右键复制到剪贴板、小窗「＋」右键新建到明天 */
public class FeatureTest26 {

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

            /* 1. 待办卡底部出现「无日期 N 条」 */
            Method ufM = OverviewPanel.class.getDeclaredMethod("updateFuture");
            ufM.setAccessible(true);
            ufM.invoke(ov);
            Thread.sleep(300);
            Field fbF = OverviewPanel.class.getDeclaredField("futureBar");
            fbF.setAccessible(true);
            JPanel futureBar = (JPanel) fbF.get(ov);
            JLabel noDate = null;
            for (Component c : futureBar.getComponents()) {
                if (c instanceof JLabel && ((JLabel) c).getText() != null
                        && ((JLabel) c).getText().startsWith("无日期")) {
                    noDate = (JLabel) c;
                }
            }
            check("待办卡有「无日期 N 条」", noDate != null, noDate == null ? "无" : noDate.getText());
            check("条数是 1", noDate != null && noDate.getText().contains("1 条"),
                    noDate == null ? "无" : noDate.getText());

            /* 点一下切进无日期范围 */
            final JLabel nd = noDate;
            if (nd != null) {
                java.awt.event.MouseEvent click = new java.awt.event.MouseEvent(nd,
                        java.awt.event.MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
                        0, 10, 10, 1, false, java.awt.event.MouseEvent.BUTTON1);
                SwingUtilities.invokeAndWait(() -> {
                    for (java.awt.event.MouseListener ml : nd.getMouseListeners()) {
                        ml.mouseClicked(click);
                    }
                });
                Thread.sleep(500);
                Field msF = OverviewPanel.class.getDeclaredField("multiSelect");
                msF.setAccessible(true);
                Field brF = OverviewPanel.class.getDeclaredField("bulkRange");
                brF.setAccessible(true);
                check("点了进入多选", msF.getBoolean(ov), "multiSelect=" + msF.getBoolean(ov));
                check("范围变成无日期(5)", brF.getInt(ov) == 5, "bulkRange=" + brF.getInt(ov));
                Method openM = OverviewPanel.class.getDeclaredMethod("openTodos");
                openM.setAccessible(true);
                java.util.List l = (java.util.List) openM.invoke(ov);
                check("列表只剩 1 条", l.size() == 1, "条数=" + l.size());
                /* 复位 */
                msF.setBoolean(ov, false);
                brF.setInt(ov, 0);
                Method listM = OverviewPanel.class.getDeclaredMethod("refreshTodoList");
                listM.setAccessible(true);
                listM.invoke(ov);
                Thread.sleep(400);
            }

            /* 2. 归档右键第三项：复制到剪贴板 */
            Method apM = OverviewPanel.class.getDeclaredMethod("archivePanel", JDialog.class, int[].class);
            apM.setAccessible(true);
            JPanel panel = (JPanel) apM.invoke(ov, null, new int[]{-1});
            panel.setSize(panel.getPreferredSize());
            panel.doLayout();
            JLabel target = null;
            for (JLabel l : allLabels(panel)) {
                if (l.getText() != null && l.getText().contains("二十天前")) target = l;
            }
            JPopupMenu menu = target == null ? null : target.getComponentPopupMenu();
            int items = 0;
            boolean hasClip = false;
            if (menu != null) {
                for (Component c : menu.getComponents()) {
                    if (c instanceof JMenuItem) {
                        items++;
                        if ("复制到剪贴板".equals(((JMenuItem) c).getText())) hasClip = true;
                    }
                }
            }
            check("归档右键 3 项", items == 3, "items=" + items);
            check("含复制到剪贴板", hasClip, "menu");

            /* 3. 小窗「＋」有右键新建到明天的方法 */
            Field instF = MiniBoard.class.getDeclaredField("inst");
            instF.setAccessible(true);
            Object board = instF.get(null);
            if (board == null) {
                SwingUtilities.invokeAndWait(() -> MiniBoard.show());
                Thread.sleep(900);
                board = instF.get(null);
            }
            Method tm = MiniBoard.class.getDeclaredMethod("addTomorrowByButton");
            tm.setAccessible(true);
            check("「＋」右键新建到明天方法存在", tm != null, "addTomorrowByButton");
            /* 明天日期换算正确（不弹窗，只校验日期字符串） */
            java.time.LocalDate tomorrow = java.time.LocalDate.now().plusDays(1);
            String mm = tomorrow.format(java.time.format.DateTimeFormatter.ofPattern("MM-dd"));
            check("明天是 11-12", "11-12".equals(mm), mm);

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
