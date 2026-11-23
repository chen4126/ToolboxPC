import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** v1.60 验证：完成节奏行、搜索词记忆、退回并挪到今天 */
public class FeatureTest40 {

    private static boolean ok = true;

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
            OverviewPanel ov = find(frame.getContentPane(), OverviewPanel.class);
            Method refresh = OverviewPanel.class.getDeclaredMethod("refresh");
            refresh.setAccessible(true);
            refresh.invoke(ov);
            Thread.sleep(400);

            /* A. 完成节奏行：文案、在待办卡里、可点 */
            Field paceF = OverviewPanel.class.getDeclaredField("paceBar");
            paceF.setAccessible(true);
            JLabel pace = (JLabel) paceF.get(ov);
            check("节奏行存在", pace != null, "null?");
            String text = pace == null ? "" : pace.getText();
            check("节奏行文案带三段数与趋势",
                    text.equals("本周 2 条 · 上周 1 条 · 本月 3 条 · 比上周多 1 条"), text);
            check("节奏行是手型且带提示",
                    pace != null && pace.getCursor().getType() == Cursor.HAND_CURSOR
                            && pace.getToolTipText() != null && pace.getToolTipText().contains("完成节奏"),
                    pace == null ? "null" : String.valueOf(pace.getToolTipText()));
            check("节奏行没挤进标题行（在 SOUTH 区）",
                    pace != null && pace.getParent() != null
                            && !(pace.getParent().getParent() instanceof JPanel
                            && ((JPanel) pace.getParent().getParent()).getLayout() instanceof GridBagLayout),
                    pace == null ? "null" : pace.getParent().getClass().getName());

            /* B. 搜索词记忆：面板构造时回填，输入后记住 */
            Method dlM = OverviewPanel.class.getDeclaredMethod("doneListPanel",
                    String.class, String.class, List.class, JDialog.class);
            dlM.setAccessible(true);
            Method subM = OverviewPanel.class.getDeclaredMethod("doneSubTitle");
            subM.setAccessible(true);
            String sub = (String) subM.invoke(ov);
            List month = rowsOf(ov, "month");
            JPanel p1 = (JPanel) dlM.invoke(ov, "本月完成 · 共 3 条", sub, month, null);
            p1.setSize(520, 244);
            layoutAll(p1);
            JTextField q1 = findField(p1);
            check("首次打开搜索框是空的", q1 != null && "".equals(q1.getText()),
                    q1 == null ? "none" : "[" + q1.getText() + "]");
            if (q1 != null) {
                q1.setText("周一");
                layoutAll(p1);
            }
            JPanel p2 = (JPanel) dlM.invoke(ov, "本月完成 · 共 3 条", sub, month, null);
            p2.setSize(520, 244);
            layoutAll(p2);
            JTextField q2 = findField(p2);
            check("再打开时记住了『周一』", q2 != null && "周一".equals(q2.getText()),
                    q2 == null ? "none" : "[" + q2.getText() + "]");
            check("记忆后列表也已筛好（1 行）", q2 != null && doneRows(p2).size() == 1,
                    "rows=" + doneRows(p2).size());
            if (q2 != null) {
                q2.setText("");
                layoutAll(p2);
            }

            /* C. 退回并挪到今天：『周一做的』回到未完成且日期变今天 */
            Method utM = OverviewPanel.class.getDeclaredMethod("undoDoneToToday", String.class);
            utM.setAccessible(true);
            int r = (Integer) utM.invoke(ov, "周一做的");
            Thread.sleep(500);
            String file = String.join("\n", Store.readLines("todo.txt"));
            check("退回并挪到今天返回 1", r == 1, "r=" + r);
            check("todo.txt 里变回未完成且日期是今天",
                    file.contains("0\t周一做的\t2026-11-11"), file.replace("\n", " | "));
            check("节奏行随之更新（本周少 1）",
                    ((JLabel) paceF.get(ov)).getText().startsWith("本周 1 条"),
                    ((JLabel) paceF.get(ov)).getText());

            /* D. Store 直接调用：已完成条目可退回并改期，未完成条目返回 false */
            boolean b1 = Store.markTodoUndoneAndMove("上周做的", "2026-11-11");
            check("Store 把已完成条目退回并改期", b1
                    && String.join("\n", Store.readLines("todo.txt")).contains("0\t上周做的\t2026-11-11"), "b1=" + b1);
            boolean b2 = Store.markTodoUndoneAndMove("今天的事", "2026-11-11");
            check("未完成条目不会被这条误改", !b2, "b2=" + b2);

            System.out.println(ok ? "ALL PASS" : "SOME FAIL");
        } catch (Throwable t) {
            t.printStackTrace();
            ok = false;
        }
        Runtime.getRuntime().halt(ok ? 0 : 1);
    }

    static List<?> rowsOf(OverviewPanel ov, String which) throws Exception {
        java.time.LocalDate now = java.time.LocalDate.now();
        java.time.LocalDate from, to;
        if ("last".equals(which)) {
            java.time.LocalDate mon = now.with(java.time.DayOfWeek.MONDAY).minusWeeks(1);
            from = mon;
            to = mon.plusDays(6);
        } else if ("month".equals(which)) {
            from = now.withDayOfMonth(1);
            to = now.withDayOfMonth(now.lengthOfMonth());
        } else {
            java.time.LocalDate mon = now.with(java.time.DayOfWeek.MONDAY);
            from = mon;
            to = mon.plusDays(6);
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

    static JTextField findField(Container p) {
        for (Component c : all(p)) if (c instanceof JTextField) return (JTextField) c;
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
