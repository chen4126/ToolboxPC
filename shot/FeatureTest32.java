import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** v1.52 验证：本周完成可点、归档统计带最早/最近、小窗过期可展开 */
public class FeatureTest32 {

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

            /* A. 小窗过期视图：5 条过期，先显示 4 条 + 可点展开 */
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
            Thread.sleep(700);

            check("折叠时列 4 条过期", overdueLines(board) == 4, "lines=" + overdueLines(board));
            JLabel fold = findFold(board);
            check("折叠时提示还有 1 条", fold != null && fold.getText().contains("还有 1 条"),
                    fold == null ? "none" : fold.getText());
            check("折叠提示可点（有手型光标）", fold != null
                    && fold.getCursor().getType() == java.awt.Cursor.HAND_CURSOR, "cursor");

            if (fold != null) {
                MouseEvent click = new MouseEvent(fold, MouseEvent.MOUSE_CLICKED,
                        System.currentTimeMillis(), 0, 4, 4, 1, false, MouseEvent.BUTTON1);
                SwingUtilities.invokeAndWait(() -> {
                    for (java.awt.event.MouseListener ml : fold.getMouseListeners()) ml.mouseClicked(click);
                });
                Thread.sleep(700);
            }
            Field allF = MiniBoard.class.getDeclaredField("overdueShowAll");
            allF.setAccessible(true);
            check("展开后 overdueShowAll=true", allF.getBoolean(board), "flag");
            check("展开后列 5 条过期", overdueLines(board) == 5, "lines=" + overdueLines(board));
            JLabel fold2 = findFold(board);
            check("展开后变「收起」", fold2 != null && fold2.getText().contains("收起"),
                    fold2 == null ? "none" : fold2.getText());

            /* B. 归档弹窗统计带最早 / 最近 */
            Method apM = OverviewPanel.class.getDeclaredMethod("archivePanel", JDialog.class, int[].class);
            apM.setAccessible(true);
            JPanel panel = (JPanel) apM.invoke(ov, null, new int[]{-1});
            panel.setSize(680, 400);
            layout(panel);
            String statText = findText(panel, "共 2 条");
            check("归档统计带最早最近", statText != null
                            && statText.contains("最早 10-02") && statText.contains("最近 10-22"),
                    statText == null ? "none" : statText);
            JTextField search = null;
            for (Component c : all(panel)) if (c instanceof JTextField) search = (JTextField) c;
            if (search != null) {
                search.setText("四十天前");
                Thread.sleep(400);
                String filtered = findText(panel, "筛出");
                check("筛出时仍保留最早最近", filtered != null && filtered.contains("筛出 1 条")
                                && filtered.contains("最早 10-02"),
                        filtered == null ? "none" : filtered);
            }

            /* C. 本周完成可点，跳到统计页 */
            Field wdF = OverviewPanel.class.getDeclaredField("weekDone");
            wdF.setAccessible(true);
            final JLabel weekDone = (JLabel) wdF.get(ov);
            check("本周完成文案", weekDone.getText().contains("本周 2 条")
                            && weekDone.getText().contains("上周 1"),
                    weekDone.getText());
            MouseEvent click2 = new MouseEvent(weekDone, MouseEvent.MOUSE_CLICKED,
                    System.currentTimeMillis(), 0, 4, 4, 1, false, MouseEvent.BUTTON1);
            SwingUtilities.invokeAndWait(() -> {
                for (java.awt.event.MouseListener ml : weekDone.getMouseListeners()) ml.mouseClicked(click2);
            });
            Thread.sleep(1200);
            Field ckF = mc.getDeclaredField("currentKey");
            ckF.setAccessible(true);
            check("点一下跳到统计页", "stats".equals(ckF.get(main)), "key=" + ckF.get(main));

            System.out.println(ok ? "ALL PASS" : "SOME FAIL");
        } catch (Throwable t) {
            t.printStackTrace();
            ok = false;
        }
        Runtime.getRuntime().halt(ok ? 0 : 1);
    }

    static int overdueLines(Object board) throws Exception {
        Field lbF = MiniBoard.class.getDeclaredField("listBox");
        lbF.setAccessible(true);
        JPanel listBox = (JPanel) lbF.get(board);
        int n = 0;
        for (Component c : listBox.getComponents()) {
            if (c instanceof JLabel) {
                String t = ((JLabel) c).getText();
                if (t != null && t.matches("\\d+天 .*")) n++;
            }
        }
        return n;
    }

    static JLabel findFold(Object board) throws Exception {
        Field lbF = MiniBoard.class.getDeclaredField("listBox");
        lbF.setAccessible(true);
        JPanel listBox = (JPanel) lbF.get(board);
        for (Component c : listBox.getComponents()) {
            if (c instanceof JLabel) {
                String t = ((JLabel) c).getText();
                if (t != null && (t.contains("还有 ") || t.contains("收起"))) return (JLabel) c;
            }
        }
        return null;
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
