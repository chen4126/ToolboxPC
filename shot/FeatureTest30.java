import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** v1.50 验证：全部过期一起勾完、小窗过期右键复制、归档复制带日期 */
public class FeatureTest30 {

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

            /* A. 先做小窗过期右键（此刻还有 3 条过期） */
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
            JLabel overdueLbl = (JLabel) getField(board, "overdueLbl");
            check("小窗红字可见", overdueLbl != null && overdueLbl.isShowing(),
                    overdueLbl == null ? "null" : ("showing=" + overdueLbl.isShowing() + " text=" + overdueLbl.getText()));
            if (overdueLbl != null) {
                MouseEvent pop = new MouseEvent(overdueLbl, MouseEvent.MOUSE_RELEASED,
                        System.currentTimeMillis(), 0, 5, 5, 1, true, MouseEvent.BUTTON3);
                Method op = MiniBoard.class.getDeclaredMethod("overduePopup", MouseEvent.class);
                op.setAccessible(true);
                final Object bf = board;
                SwingUtilities.invokeAndWait(() -> {
                    try {
                        op.invoke(bf, pop);
                    } catch (Exception e) {
                        if (e.getCause() != null) e.getCause().printStackTrace();
                    }
                });
                Thread.sleep(300);
                Field loF = MiniBoard.class.getDeclaredField("lastOverdueMenu");
                loF.setAccessible(true);
                JPopupMenu menu = (JPopupMenu) loF.get(board);
                check("过期右键菜单已弹出", menu != null, "menu");
                if (menu != null) {
                    int items = 0;
                    String texts = "";
                    for (Component c : menu.getComponents()) {
                        if (c instanceof JMenuItem) {
                            items++;
                            texts += ((JMenuItem) c).getText() + " | ";
                        }
                    }
                    check("过期右键 4 项", items == 4, "items=" + items + " " + texts);
                    check("有「复制这批过期到剪贴板」", texts.contains("复制这批过期"), texts);
                }
            }

            Method coM = OverviewPanel.class.getDeclaredMethod("copyOverdueTexts");
            coM.setAccessible(true);
            int on = (Integer) coM.invoke(ov);
            String clip2 = "";
            try {
                clip2 = (String) java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                        .getData(java.awt.datatransfer.DataFlavor.stringFlavor);
            } catch (Exception e) {
                clip2 = "ERR";
            }
            check("复制过期 3 条", on == 3, "n=" + on);
            check("过期复制带日期且三行",
                    clip2.contains("11-02  九天前没动") && clip2.split("\n").length == 3,
                    clip2.replace("\n", "\\n"));

            /* B. 归档复制带原日期 */
            Method caM = OverviewPanel.class.getDeclaredMethod("copyAllArchived");
            caM.setAccessible(true);
            int n = (Integer) caM.invoke(ov);
            String clip = "";
            try {
                clip = (String) java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                        .getData(java.awt.datatransfer.DataFlavor.stringFlavor);
            } catch (Exception e) {
                clip = "ERR";
            }
            check("复制 2 条归档", n == 2, "n=" + n);
            check("归档复制带原日期", clip.contains("10-22  二十天前") && clip.contains("10-02  四十天前"),
                    clip.replace("\n", "\\n"));
            check("归档复制没留后缀", !clip.contains("（已归档）"), clip.replace("\n", "\\n"));

            /* C. 切「只看过期」并点「全部过期一起勾完」 */
            Field obF = OverviewPanel.class.getDeclaredField("onlyOverdueBtn");
            obF.setAccessible(true);
            final JLabel ovBtn = (JLabel) obF.get(ov);
            MouseEvent click = new MouseEvent(ovBtn, MouseEvent.MOUSE_CLICKED,
                    System.currentTimeMillis(), 0, 4, 4, 1, false, MouseEvent.BUTTON1);
            SwingUtilities.invokeAndWait(() -> {
                for (java.awt.event.MouseListener ml : ovBtn.getMouseListeners()) ml.mouseClicked(click);
            });
            Thread.sleep(700);

            Field fbF = OverviewPanel.class.getDeclaredField("futureBar");
            fbF.setAccessible(true);
            JPanel futureBar = (JPanel) fbF.get(ov);
            String chips = "";
            JLabel finish = null;
            for (Component c : futureBar.getComponents()) {
                if (c instanceof JLabel) {
                    String t = ((JLabel) c).getText();
                    chips += t + " | ";
                    if (t != null && t.startsWith("全部过期一起勾完")) finish = (JLabel) c;
                }
            }
            check("底部有「全部过期一起勾完 3 条」", finish != null, chips);
            if (finish != null) {
                final JLabel f2 = finish;
                SwingUtilities.invokeAndWait(() -> {
                    for (java.awt.event.MouseListener ml : f2.getMouseListeners()) ml.mouseClicked(click);
                });
                Thread.sleep(800);
                Method foM = OverviewPanel.class.getDeclaredMethod("filteredOverdueTodos");
                foM.setAccessible(true);
                List left = (List) foM.invoke(ov);
                check("勾完后过期剩 0 条", left.isEmpty(), "left=" + left.size());
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
