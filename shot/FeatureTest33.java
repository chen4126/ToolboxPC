import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** v1.53 验证：分档"另有 N 条 日期"、归档点条目跳那天、小窗红字说最久几天 */
public class FeatureTest33 {

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

            /* A. 分档行：不再有"更早"，改成"另有 N 条 日期" */
            Field fbF = OverviewPanel.class.getDeclaredField("futureBar");
            fbF.setAccessible(true);
            JPanel futureBar = (JPanel) fbF.get(ov);
            String chips = "";
            String restAncient = null, restDeep = null;
            for (Component c : futureBar.getComponents()) {
                if (c instanceof JLabel) {
                    String t = ((JLabel) c).getText();
                    chips += t + " | ";
                    if (t != null && t.startsWith("另有 ") && t.contains("11-02")) restAncient = t;
                    if (t != null && t.startsWith("另有 ") && t.contains("11-07")) restDeep = t;
                }
            }
            check("不再出现「更早」字样", !chips.contains("更早"), chips);
            check("很久没动档：另有 1 条 11-02", restAncient != null
                    && restAncient.contains("另有 1 条"), restAncient == null ? "none" : restAncient);
            check("该处理了档：另有 1 条 11-07", restDeep != null
                    && restDeep.contains("另有 1 条"), restDeep == null ? "none" : restDeep);

            JLabel rest = null;
            for (Component c : futureBar.getComponents()) {
                if (c instanceof JLabel && ((JLabel) c).getText().startsWith("另有 ")) {
                    rest = (JLabel) c;
                    break;
                }
            }
            check("另有条数带 tooltip 说明落在哪天", rest != null && rest.getToolTipText() != null
                    && rest.getToolTipText().contains("落在"), rest == null ? "none" : rest.getToolTipText());

            /* B. 小窗红字：最久 10 天 */
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
            Field olF = MiniBoard.class.getDeclaredField("overdueLbl");
            olF.setAccessible(true);
            JLabel overdueLbl = (JLabel) olF.get(board);
            check("小窗红字说最久几天", overdueLbl.getText().contains("很久没动 2 条")
                            && overdueLbl.getText().contains("最久 10 天"),
                    overdueLbl.getText());
            check("tooltip 仍保留最早日期", overdueLbl.getToolTipText().contains("最早 11-01"),
                    overdueLbl.getToolTipText());

            /* C. 归档条目点一下跳那天 */
            Method faM = OverviewPanel.class.getDeclaredMethod("filteredArchived");
            faM.setAccessible(true);
            List arch = (List) faM.invoke(ov);
            Object target = null;
            Class<?> todoCls = null;
            for (Object o : arch) {
                todoCls = o.getClass();
                Field tf = todoCls.getDeclaredField("text");
                tf.setAccessible(true);
                String txt = String.valueOf(tf.get(o));
                if (txt.contains("二十天前")) target = o;
            }
            check("找到 10-22 那条归档", target != null, "todo");
            Method jm = OverviewPanel.class.getDeclaredMethod("jumpToArchivedDay", todoCls);
            jm.setAccessible(true);
            int jumped = (Integer) jm.invoke(ov, target);
            Thread.sleep(600);
            Field vdF = OverviewPanel.class.getDeclaredField("viewDate");
            vdF.setAccessible(true);
            Field lvF = OverviewPanel.class.getDeclaredField("overdueLevel");
            lvF.setAccessible(true);
            check("跳转生效", jumped == 1, "ret=" + jumped);
            check("视图落到 2026-10-22", vdF.get(ov) != null
                            && vdF.get(ov).toString().equals("2026-10-22"),
                    "viewDate=" + vdF.get(ov));
            check("跳过去时退出了过期筛选", (Integer) lvF.get(ov) == 0, "level=" + lvF.get(ov));

            Method boxM = OverviewPanel.class.getDeclaredMethod("archiveBox", List.class);
            boxM.setAccessible(true);
            JPanel box = (JPanel) boxM.invoke(ov, arch);
            JLabel row = null;
            for (Component c : all(box)) {
                if (c instanceof JLabel && ((JLabel) c).getText().contains("二十天前")) row = (JLabel) c;
            }
            check("归档行是手型光标（提示可点）", row != null
                    && row.getCursor().getType() == java.awt.Cursor.HAND_CURSOR, "cursor");
            check("归档行 tooltip 说明可点跳转", row != null && row.getToolTipText().contains("点一下跳到那天"),
                    row == null ? "none" : row.getToolTipText());

            System.out.println(ok ? "ALL PASS" : "SOME FAIL");
        } catch (Throwable t) {
            t.printStackTrace();
            ok = false;
        }
        Runtime.getRuntime().halt(ok ? 0 : 1);
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
