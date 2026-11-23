import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** v1.62 验证：圆点带当天条数、小窗节奏行可点、清单超 3 条二次确认 */
public class FeatureTest43 {

    private static boolean ok = true;
    private static OverviewPanel ov;

    private static final String[] DATA = {
            "0\t今天的事\t2026-11-11", "0\t九天前没动\t2026-11-02", "0\t四天前\t2026-11-07",
            "0\t昨天没做\t2026-11-10", "0\t六天前\t2026-11-05", "0\t十天前\t2026-11-01",
            "0\t明天的事\t2026-11-12", "0\t十五号\t2026-11-15", "0\t二十五号\t2026-11-25",
            "0\t没定日子的\t-",
            "1\t今天已做完\t2026-11-11", "1\t今天第二件\t2026-11-11",
            "1\t周一做的\t2026-11-09", "1\t上周做的\t2026-11-04",
            "1\t二十天前（已归档）\t2026-10-22", "1\t四十天前（已归档）\t2026-10-02"};

    public static void main(String[] a) {
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
            ov = find(frame.getContentPane(), OverviewPanel.class);
            reload();

            /* A. 每天完成条数：周三 2 条、周一 1 条、周二 0 条 */
            Method ccM = OverviewPanel.class.getDeclaredMethod("weekDoneCounts");
            ccM.setAccessible(true);
            int[] cnt = (int[]) ccM.invoke(ov);
            check("周三(今天)完成 2 条", cnt[2] == 2, "cnt[2]=" + cnt[2]);
            check("周一完成 1 条", cnt[0] == 1, "cnt[0]=" + cnt[0]);
            check("周二 0 条", cnt[1] == 0, "cnt[1]=" + cnt[1]);
            check("周日 0 条", cnt[6] == 0, "cnt[6]=" + cnt[6]);

            /* B. 圆点悬浮：周三点上说明完成 2 条 */
            Field dotsF = OverviewPanel.class.getDeclaredField("weekDots");
            dotsF.setAccessible(true);
            JPanel dots = (JPanel) dotsF.get(ov);
            final int d = 8, gap = 2;
            int xToday = 3 + 2 * (d + gap) + 4;              // 周三那颗的中心
            final MouseEvent me = new MouseEvent(dots, MouseEvent.MOUSE_MOVED,
                    System.currentTimeMillis(), 0, xToday, 9, 0, false);
            final String tip = dots.getToolTipText(me);
            check("周三点悬浮带条数", tip != null && tip.contains("周三（今天）") && tip.contains("完成 2 条"), tip);
            check("多条时提示产出最多", tip != null && tip.contains("这天产出最多"), tip);
            final MouseEvent meMonday = new MouseEvent(dots, MouseEvent.MOUSE_MOVED,
                    System.currentTimeMillis(), 0, 3 + 4, 9, 0, false);
            String tipMon = dots.getToolTipText(meMonday);
            check("周一悬浮是 1 条", tipMon != null && tipMon.contains("周一") && tipMon.contains("完成 1 条"), tipMon);

            /* C. 小窗节奏行可点 */
            MiniBoard.show();
            Thread.sleep(600);
            Field instF = MiniBoard.class.getDeclaredField("inst");
            instF.setAccessible(true);
            Object board = instF.get(null);
            Field paceF = MiniBoard.class.getDeclaredField("paceLbl");
            paceF.setAccessible(true);
            JLabel pace = (JLabel) paceF.get(board);
            check("小窗节奏行有鼠标监听", pace != null && pace.getMouseListeners().length > 0,
                    pace == null ? "null" : "listeners=" + pace.getMouseListeners().length);
            check("小窗节奏行是手型", pace != null && pace.getCursor().getType() == Cursor.HAND_CURSOR, "cursor");
            check("小窗文案与数据一致", pace != null && "本周完成 3 条 · 比上周多 2 条".equals(pace.getText()),
                    pace == null ? "null" : pace.getText());

            /* D. 阈值函数 */
            Method ncM = OverviewPanel.class.getDeclaredMethod("needUndoConfirm", int.class);
            ncM.setAccessible(true);
            check("退回 3 条不用确认", !(Boolean) ncM.invoke(ov, 3), "3");
            check("退回 4 条要确认", (Boolean) ncM.invoke(ov, 4), "4");
            check("退回 0 条不用确认", !(Boolean) ncM.invoke(ov, 0), "0");

            /* E. 面板 A：选 3 条直接退，不弹确认 */
            JPanel pA = newPanel("本月完成 · 共 4 条");
            pickFirst(pA, 3);
            JButton btnA = findButton(pA, "退回选中的");
            check("选 3 条按钮文案", btnA != null && "退回选中的 3 条".equals(btnA.getText()),
                    btnA == null ? "null" : btnA.getText());
            SwingUtilities.invokeAndWait(btnA::doClick);
            Thread.sleep(400);
            check("3 条不出现确认文案", "退回选中的 3 条".equals(btnA.getText()), btnA.getText());
            String f1 = String.join("\n", Store.readLines("todo.txt"));
            check("3 条直接退成功", countZero(f1, "今天已做完", "今天第二件", "周一做的") == 3,
                    "zero=" + countZero(f1, "今天已做完", "今天第二件", "周一做的"));

            /* F. 面板 B：选 4 条先确认，再点才退 */
            Store.writeLines("todo.txt", java.util.Arrays.asList(DATA));   // 复位数据，四条完成都在
            reload();
            JPanel pB = newPanel("本月完成 · 共 4 条");
            pickFirst(pB, 4);
            JButton btnB = findButton(pB, "退回选中的");
            check("选 4 条按钮文案", btnB != null && "退回选中的 4 条".equals(btnB.getText()),
                    btnB == null ? "null" : btnB.getText());
            SwingUtilities.invokeAndWait(btnB::doClick);
            Thread.sleep(200);
            check("第一次点击变成确认文案", "确认退回 4 条？".equals(btnB.getText()), btnB.getText());
            String f2 = String.join("\n", Store.readLines("todo.txt"));
            check("确认前文件没动", countZero(f2, "今天已做完", "今天第二件", "周一做的", "上周做的") == 0,
                    "zero=" + countZero(f2, "今天已做完", "今天第二件", "周一做的", "上周做的"));
            SwingUtilities.invokeAndWait(btnB::doClick);
            Thread.sleep(400);
            String f3 = String.join("\n", Store.readLines("todo.txt"));
            check("第二次点击才真的退回 4 条",
                    countZero(f3, "今天已做完", "今天第二件", "周一做的", "上周做的") == 4,
                    "zero=" + countZero(f3, "今天已做完", "今天第二件", "周一做的", "上周做的"));

            System.out.println(ok ? "ALL PASS" : "SOME FAIL");
        } catch (Throwable t) {
            t.printStackTrace();
            ok = false;
        }
        Runtime.getRuntime().halt(ok ? 0 : 1);
    }

    static JPanel newPanel(String title) throws Exception {
        Method subM = OverviewPanel.class.getDeclaredMethod("doneSubTitle");
        subM.setAccessible(true);
        String sub = (String) subM.invoke(ov);
        Method dlM = OverviewPanel.class.getDeclaredMethod("doneListPanel",
                String.class, String.class, List.class, JDialog.class);
        dlM.setAccessible(true);
        List month = rowsOf("month");
        JPanel p = (JPanel) dlM.invoke(ov, title, sub, month, null);
        p.setSize(520, 270);
        layoutAll(p);
        return p;
    }

    static void pickFirst(JPanel p, int n) throws Exception {
        java.util.List<JPanel> rows = doneRows(p);
        for (int i = 0; i < n && i < rows.size(); i++) {
            final JPanel target = rows.get(i);
            SwingUtilities.invokeAndWait(() -> {
                MouseEvent me = new MouseEvent(target, MouseEvent.MOUSE_CLICKED,
                        System.currentTimeMillis(), InputEvent.CTRL_DOWN_MASK,
                        4, 4, 1, false, MouseEvent.BUTTON1);
                target.dispatchEvent(me);
            });
        }
        layoutAll(p);
    }

    static int countZero(String file, String... texts) {
        int n = 0;
        for (String t : texts) if (file.contains("0\t" + t + "\t")) n++;
        return n;
    }

    static void reload() throws Exception {
        Method load = OverviewPanel.class.getDeclaredMethod("loadTodos");
        load.setAccessible(true);
        load.invoke(ov);
        Method refresh = OverviewPanel.class.getDeclaredMethod("refresh");
        refresh.setAccessible(true);
        refresh.invoke(ov);
        Thread.sleep(300);
    }

    static List<?> rowsOf(String which) throws Exception {
        java.time.LocalDate now = java.time.LocalDate.now();
        java.time.LocalDate from, to;
        if ("last".equals(which)) {
            java.time.LocalDate mon = now.with(java.time.DayOfWeek.MONDAY).minusWeeks(1);
            from = mon;
            to = mon.plusDays(6);
        } else {
            from = now.withDayOfMonth(1);
            to = now.withDayOfMonth(now.lengthOfMonth());
        }
        Field tF = OverviewPanel.class.getDeclaredField("todos");
        tF.setAccessible(true);
        List<?> todos = (List<?>) tF.get(ov);
        java.util.List<String[]> rows = new java.util.ArrayList<String[]>();
        for (Object t : todos) {
            Class<?> tc = t.getClass();
            Field doneF = tc.getDeclaredField("done");
            doneF.setAccessible(true);
            Field textF = tc.getDeclaredField("text");
            textF.setAccessible(true);
            Field dateF = tc.getDeclaredField("date");
            dateF.setAccessible(true);
            if (!doneF.getBoolean(t)) continue;
            String text = String.valueOf(textF.get(t));
            if (text.endsWith("（已归档）")) continue;
            java.time.LocalDate d;
            try {
                d = java.time.LocalDate.parse(String.valueOf(dateF.get(t)).trim());
            } catch (Exception e) {
                continue;
            }
            if (d.isBefore(from) || d.isAfter(to)) continue;
            rows.add(new String[]{text, d.toString()});
        }
        java.util.Collections.sort(rows, (x, y) -> y[1].compareTo(x[1]));
        return rows;
    }

    static java.util.List<JPanel> doneRows(JPanel p) {
        java.util.List<JPanel> out = new java.util.ArrayList<JPanel>();
        for (Component c : all(p)) {
            if (!(c instanceof JPanel)) continue;
            JPanel row = (JPanel) c;
            if (row.getMaximumSize() == null || row.getMaximumSize().height != 24) continue;
            if (row.getComponentCount() != 2) continue;
            if (row.getComponent(0) instanceof JLabel && row.getComponent(1) instanceof JLabel) out.add(row);
        }
        return out;
    }

    static JButton findButton(Container p, String prefix) {
        for (Component c : all(p)) {
            if (c instanceof JButton && ((JButton) c).getText() != null
                    && ((JButton) c).getText().startsWith(prefix)) return (JButton) c;
        }
        return null;
    }

    static java.util.List<Component> all(Container p) {
        java.util.List<Component> out = new java.util.ArrayList<Component>();
        for (Component c : p.getComponents()) {
            out.add(c);
            if (c instanceof Container) out.addAll(all((Container) c));
        }
        return out;
    }

    static void check(String name, boolean pass, String detail) {
        if (!pass) ok = false;
        System.out.println((pass ? "[PASS] " : "[FAIL] ") + name + " → " + detail);
    }

    static void layoutAll(Component c) {
        c.doLayout();
        if (c instanceof Container) for (Component k : ((Container) c).getComponents()) layoutAll(k);
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
