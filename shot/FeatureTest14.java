import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;

/** v1.34 三项验证：小窗只展开过期 / 挪动可撤销 / 30 天自动归档 */
public class FeatureTest14 {

    private static boolean ok = true;

    public static void main(String[] args) throws Exception {
        try {
            Theme.init();
            SwingUtilities.invokeAndWait(() -> Main.main(new String[0]));
            Thread.sleep(2500);

            Class<?> mc = Class.forName("com.toolbox.pc.Main");
            Field selfF = mc.getDeclaredField("self");
            selfF.setAccessible(true);
            Field ff = mc.getDeclaredField("frame");
            ff.setAccessible(true);
            JFrame frame = (JFrame) ff.get(selfF.get(null));

            OverviewPanel ov = page(OverviewPanel.class);
            Method refresh = OverviewPanel.class.getDeclaredMethod("refresh");
            refresh.setAccessible(true);
            SwingUtilities.invokeAndWait(() -> {
                try {
                    refresh.invoke(ov);
                } catch (Exception ignored) {
                }
            });
            Thread.sleep(500);

            Field fbF = OverviewPanel.class.getDeclaredField("futureBar");
            fbF.setAccessible(true);
            JPanel bar = (JPanel) fbF.get(ov);

            /* ---------- 1. 30 天自动归档 ---------- */
            Method arch = OverviewPanel.class.getDeclaredMethod("archiveStale");
            arch.setAccessible(true);
            Field todosF = OverviewPanel.class.getDeclaredField("todos");
            todosF.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.List<Object> todos = (java.util.List<Object>) todosF.get(ov);
            Field doneF = todos.get(0).getClass().getDeclaredField("done");
            doneF.setAccessible(true);
            Field textF = todos.get(0).getClass().getDeclaredField("text");
            textF.setAccessible(true);
            Field dateF = todos.get(0).getClass().getDeclaredField("date");
            dateF.setAccessible(true);
            boolean archived = false;
            for (Object t : todos) {
                if (String.valueOf(textF.get(t)).contains("四十天前")) {
                    archived = doneF.getBoolean(t) && String.valueOf(textF.get(t)).contains("（已归档）");
                }
            }
            System.out.println("四十天前已归档=" + archived);
            check("超过 30 天的过期条目被归档并标完成", archived);
            int again = (Integer) arch.invoke(ov);
            check("归档是幂等的，第二次不再动", again == 0);

            StringBuilder sb = new StringBuilder();
            for (Component c : bar.getComponents()) {
                if (c instanceof JLabel) sb.append(((JLabel) c).getText()).append('|');
            }
            System.out.println("归档后标签=" + sb);
            check("归档的那条不再进过期三档", sb.toString().contains("很久没动|11-01 · 1 条|")
                    && sb.toString().contains("过期未做|11-09 · 1 条|")
                    && !sb.toString().contains("10-01"));

            /* ---------- 2. 小窗红字 = 只展开过期 ---------- */
            SwingUtilities.invokeAndWait(() -> MiniBoard.toggle());
            Thread.sleep(800);
            Field instF = MiniBoard.class.getDeclaredField("inst");
            instF.setAccessible(true);
            Object mb = instF.get(null);
            Field odF = MiniBoard.class.getDeclaredField("overdueLbl");
            odF.setAccessible(true);
            JLabel od = (JLabel) odF.get(mb);
            System.out.println("小窗红字=" + od.getText());
            MouseEvent ev = new MouseEvent(od, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
                    0, 1, 1, 1, false);
            for (java.awt.event.MouseListener ml : od.getMouseListeners()) ml.mouseClicked(ev);
            Thread.sleep(400);

            Field bdF = MiniBoard.class.getDeclaredField("boardDay");
            bdF.setAccessible(true);
            Field ovF = MiniBoard.class.getDeclaredField("overdueView");
            ovF.setAccessible(true);
            Field lbF = MiniBoard.class.getDeclaredField("listBox");
            lbF.setAccessible(true);
            JPanel listBox = (JPanel) lbF.get(mb);
            String texts = join(listBox);
            System.out.println("点红字后 overdueView=" + ovF.getBoolean(mb) + " boardDay=" + bdF.getInt(mb)
                    + " 内容=" + texts);
            check("点红字进入只看过期的视图", ovF.getBoolean(mb) && bdF.getInt(mb) == 0);
            check("标题显示过期条数", texts.contains("过期未做 · 2 条"));
            check("只列过期条目，不含今天的", texts.contains("九天前没动") && texts.contains("昨天没做")
                    && !texts.contains("今天的事") && !texts.contains("四十天前"));

            /* 点标题退出过期视图 */
            JLabel title = null;
            for (Component c : listBox.getComponents()) {
                if (c instanceof JPanel) {
                    for (Component k : ((JPanel) c).getComponents()) {
                        if (k instanceof JLabel && ((JLabel) k).getText().contains("过期未做 · 2 条")) title = (JLabel) k;
                    }
                }
            }
            check("找到过期视图的标题", title != null);
            if (title != null) {
                MouseEvent ev2 = new MouseEvent(title, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
                        0, 1, 1, 1, false);
                for (java.awt.event.MouseListener ml : title.getMouseListeners()) ml.mouseClicked(ev2);
                Thread.sleep(400);
            }
            String after = join(listBox);
            System.out.println("点标题后 overdueView=" + ovF.getBoolean(mb) + " 内容=" + after);
            check("点标题回到今日待办", !ovF.getBoolean(mb) && after.contains("今日待办")
                    && after.contains("今天的事"));

            /* ---------- 3. 挪到今天 + 撤销 ---------- */
            Method move = OverviewPanel.class.getDeclaredMethod("moveOverdueToToday");
            move.setAccessible(true);
            Method undo = OverviewPanel.class.getDeclaredMethod("undoMoveOverdue");
            undo.setAccessible(true);
            int moved = (Integer) move.invoke(ov);
            System.out.println("挪动=" + moved);
            check("把两条未归档的过期都挪过来", moved == 2);

            SwingUtilities.invokeAndWait(() -> {
                try {
                    refresh.invoke(ov);
                } catch (Exception ignored) {
                }
            });
            Thread.sleep(400);
            String s2 = "";
            for (Component c : bar.getComponents()) {
                if (c instanceof JLabel) s2 += ((JLabel) c).getText() + "|";
            }
            System.out.println("挪完标签=" + s2);
            check("挪完给出撤销入口", s2.contains("已挪到今天 2 条") && s2.contains("撤销挪动 2 条"));

            int back = (Integer) undo.invoke(ov);
            System.out.println("撤销=" + back);
            check("撤销一次全部回退", back == 2);
            StringBuilder dates = new StringBuilder();
            for (Object t : todos) dates.append(String.valueOf(dateF.get(t))).append('/');
            System.out.println("撤销后日期=" + dates);
            check("日期回到原样", dates.toString().contains("2026-11-01/") && dates.toString().contains("2026-11-09/"));

            SwingUtilities.invokeAndWait(() -> {
                try {
                    refresh.invoke(ov);
                } catch (Exception ignored) {
                }
            });
            Thread.sleep(400);
            String s3 = "";
            for (Component c : bar.getComponents()) {
                if (c instanceof JLabel) s3 += ((JLabel) c).getText() + "|";
            }
            System.out.println("撤销后标签=" + s3);
            check("撤销后又恢复过期提醒和挪动入口", s3.contains("很久没动") && s3.contains("全部挪到今天")
                    && !s3.contains("撤销挪动"));

            System.out.println(ok ? "ALL PASS" : "HAS FAIL");
            System.exit(ok ? 0 : 1);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(2);
        }
    }

    private static String join(JPanel box) {
        StringBuilder sb = new StringBuilder();
        for (Component c : box.getComponents()) {
            if (c instanceof JLabel) sb.append(((JLabel) c).getText()).append('|');
            else if (c instanceof JPanel) {
                for (Component k : ((JPanel) c).getComponents()) {
                    if (k instanceof JLabel) sb.append(((JLabel) k).getText()).append('|');
                }
            }
        }
        return sb.toString();
    }

    private static void check(String name, boolean pass) {
        System.out.println((pass ? "[PASS] " : "[FAIL] ") + name);
        if (!pass) ok = false;
    }

    private static <T extends Component> T page(Class<T> type) {
        for (Frame f : Frame.getFrames()) {
            T t = scan(f, type);
            if (t != null) return t;
        }
        return null;
    }

    private static <T extends Component> T scan(Component c, Class<T> type) {
        if (type.isInstance(c)) return type.cast(c);
        if (c instanceof Container) {
            for (Component k : ((Container) c).getComponents()) {
                T t = scan(k, type);
                if (t != null) return t;
            }
        }
        return null;
    }
}
