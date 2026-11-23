import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** v1.51 验证：本周/上周完成数、小窗过期行带天数、归档条数统计 */
public class FeatureTest31 {

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

            /* 1. 本周 / 上周完成数 */
            Method w0 = OverviewPanel.class.getDeclaredMethod("doneInWeek", int.class);
            w0.setAccessible(true);
            int thisWeek = (Integer) w0.invoke(ov, 0);
            int lastWeek = (Integer) w0.invoke(ov, 1);
            check("本周完成 2 条", thisWeek == 2, "this=" + thisWeek);
            check("上周完成 1 条", lastWeek == 1, "last=" + lastWeek);

            Field tsF = OverviewPanel.class.getDeclaredField("todoStat");
            tsF.setAccessible(true);
            JLabel stat = (JLabel) tsF.get(ov);
            check("统计行有本周完成", stat.getText().contains("本周完成 2"), stat.getText());
            check("统计行带上周对比", stat.getText().contains("上周 1"), stat.getText());
            check("统计行保留待完成数", stat.getText().contains("项待完成"), stat.getText());

            /* 2. 小窗过期视图里每条带天数 */
            Field instF = MiniBoard.class.getDeclaredField("inst");
            instF.setAccessible(true);
            Object board = instF.get(null);
            if (board == null) {
                SwingUtilities.invokeAndWait(() -> MiniBoard.show());
                Thread.sleep(900);
                board = instF.get(null);
            }
            Field ovF = MiniBoard.class.getDeclaredField("overdueView");
            ovF.setAccessible(true);
            ovF.setBoolean(board, true);
            Method br = MiniBoard.class.getDeclaredMethod("refresh");
            br.setAccessible(true);
            br.invoke(board);
            Thread.sleep(800);
            Field lbF = MiniBoard.class.getDeclaredField("listBox");
            lbF.setAccessible(true);
            JPanel listBox = (JPanel) lbF.get(board);
            String texts = "";
            boolean dayPrefixed = false;
            for (Component c : listBox.getComponents()) {
                if (c instanceof JLabel) {
                    String t = ((JLabel) c).getText();
                    texts += t + " | ";
                    if (t != null && t.matches("\\d+天 .*九天前没动")) dayPrefixed = true;
                }
            }
            check("过期行带天数前缀", dayPrefixed, texts);

            /* 3. 归档弹窗条数统计 */
            Method apM = OverviewPanel.class.getDeclaredMethod("archivePanel", JDialog.class, int[].class);
            apM.setAccessible(true);
            final int[] acts = new int[]{-1};
            JPanel panel = (JPanel) apM.invoke(ov, null, acts);
            panel.setSize(680, 400);
            layout(panel);
            String statText = findText(panel, "共 2 条");
            check("归档底部显示共 2 条", statText != null, statText == null ? "none" : statText);

            JTextField search = null;
            for (Component c : all(panel)) if (c instanceof JTextField) search = (JTextField) c;
            if (search != null) {
                search.setText("四十天前");
                Thread.sleep(400);
                String filtered = findText(panel, "筛出");
                check("搜索后显示筛出条数", filtered != null && filtered.contains("筛出 1 条"),
                        filtered == null ? "none" : filtered);
            }

            System.out.println(ok ? "ALL PASS" : "SOME FAIL");
        } catch (Throwable t) {
            t.printStackTrace();
            ok = false;
        }
        Runtime.getRuntime().halt(ok ? 0 : 1);
    }

    static String findText(Component root, String needle) {
        for (Component c : all(root)) {
            if (c instanceof JLabel) {
                String t = ((JLabel) c).getText();
                if (t != null && t.contains(needle)) return t;
            }
        }
        return null;
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

    static void layout(Component c) {
        c.doLayout();
        if (c instanceof Container) for (Component k : ((Container) c).getComponents()) layout(k);
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
