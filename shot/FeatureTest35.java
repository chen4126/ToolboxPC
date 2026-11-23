import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** v1.55 验证：归档搜日期、统计页待办完成卡、小窗过期行右侧灰字 */
public class FeatureTest35 {

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
            Thread.sleep(500);

            /* A. 归档搜日期：10-2 / 10-22 / 40 天前 各能筛出对应那一条 */
            Method hitM = OverviewPanel.class.getDeclaredMethod("archHit", String.class, String.class, String.class);
            hitM.setAccessible(true);
            Object r1 = hitM.invoke(ov, "四十天前（已归档）", "2026-10-02", "10-2");
            check("搜 10-2 命中 10-02 那条", (Boolean) r1, "hit=" + r1);
            Object r2 = hitM.invoke(ov, "四十天前（已归档）", "2026-10-02", "10-22");
            check("搜 10-22 不命中 10-02 那条", !((Boolean) r2), "hit=" + r2);
            Object r3 = hitM.invoke(ov, "二十天前（已归档）", "2026-10-22", "10-2");
            check("搜 10-2 不该把 10-22 也算上", !((Boolean) r3), "hit=" + r3);
            Object r4 = hitM.invoke(ov, "二十天前（已归档）", "2026-10-22", "10-22");
            check("搜 10-22 命中 10-22 那条", (Boolean) r4, "hit=" + r4);

            Field aqF = OverviewPanel.class.getDeclaredField("archQuery");
            aqF.setAccessible(true);
            Method faM = OverviewPanel.class.getDeclaredMethod("filteredArchived");
            faM.setAccessible(true);
            aqF.set(ov, "10-22");
            List one = (List) faM.invoke(ov);
            check("搜 10-22 只剩 1 条", one.size() == 1, "size=" + one.size());
            aqF.set(ov, "10-02");
            List two = (List) faM.invoke(ov);
            check("搜完整 10-02 也能命中", two.size() == 1, "size=" + two.size());
            aqF.set(ov, "四十天前");
            List three = (List) faM.invoke(ov);
            check("原来按文字搜仍好使", three.size() == 1, "size=" + three.size());
            aqF.set(ov, "");
            faM.invoke(ov);

            /* B. 统计页：待办本周完成 2 条、本月完成 3 条 */
            Method pageM = mc.getDeclaredMethod("page", Class.class);
            pageM.setAccessible(true);
            Object stats = pageM.invoke(main, StatsPanel.class);
            Field twF = StatsPanel.class.getDeclaredField("todoWeekLabel");
            twF.setAccessible(true);
            Field tmF = StatsPanel.class.getDeclaredField("todoMonthLabel");
            tmF.setAccessible(true);
            JLabel tw = (JLabel) twF.get(stats);
            JLabel tm = (JLabel) tmF.get(stats);
            Method sr = StatsPanel.class.getDeclaredMethod("refresh");
            sr.setAccessible(true);
            sr.invoke(stats);
            Thread.sleep(300);
            check("统计页待办本周完成 = 2 条", tw.getText().equals("2 条"), tw.getText());
            check("统计页待办本月完成 = 3 条", tm.getText().equals("3 条"), tm.getText());

            /* C. 小窗过期行：标题在左、右侧淡灰"MM-dd · N天" */
            Field instF = MiniBoard.class.getDeclaredField("inst");
            instF.setAccessible(true);
            Object board = instF.get(null);
            if (board == null) {
                SwingUtilities.invokeAndWait(() -> MiniBoard.show());
                Thread.sleep(900);
                board = instF.get(null);
            }
            Field ovvF = MiniBoard.class.getDeclaredField("overdueView");
            ovvF.setAccessible(true);
            ovvF.set(board, true);
            Method br = MiniBoard.class.getDeclaredMethod("refresh");
            br.setAccessible(true);
            br.invoke(board);
            Thread.sleep(700);
            Field lbF = MiniBoard.class.getDeclaredField("listBox");
            lbF.setAccessible(true);
            JPanel listBox = (JPanel) lbF.get(board);
            String leftTitle = null, rightTail = null;
            for (Component c : listBox.getComponents()) {
                if (!(c instanceof JPanel)) continue;
                JPanel row = (JPanel) c;
                String l = null, r = null;
                for (Component k : row.getComponents()) {
                    String pos = null;
                    Object cons = row.getLayout() instanceof java.awt.BorderLayout
                            ? ((java.awt.BorderLayout) row.getLayout()).getConstraints(k) : null;
                    if (cons != null) pos = cons.toString();
                    if (k instanceof JLabel) {
                        if ("Center".equals(pos)) l = ((JLabel) k).getText();
                        if ("East".equals(pos)) r = ((JLabel) k).getText();
                    }
                }
                if (l != null && l.contains("九天前没动")) {
                    leftTitle = l;
                    rightTail = r;
                    break;
                }
            }
            check("过期行左边是标题本身", leftTitle != null && leftTitle.equals("九天前没动"),
                    String.valueOf(leftTitle));
            check("过期行右边挂 MM-dd · N天", rightTail != null && rightTail.contains("11-02")
                    && rightTail.contains("9天"), String.valueOf(rightTail));
            JLabel tail = null;
            for (Component c : listBox.getComponents()) {
                if (!(c instanceof JPanel)) continue;
                for (Component k : ((JPanel) c).getComponents()) {
                    if (k instanceof JLabel && ((JLabel) k).getText() != null
                            && ((JLabel) k).getText().contains("9天")) tail = (JLabel) k;
                }
            }
            check("右侧小字是淡灰色", tail != null && tail.getForeground() != null
                    && tail.getForeground().getRGB() != java.awt.Color.BLACK.getRGB(),
                    tail == null ? "none" : String.format("#%06X", tail.getForeground().getRGB() & 0xFFFFFF));

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
