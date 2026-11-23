import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** v1.59 验证：清单搜索框、右键退回待办、清单副标题趋势 */
public class FeatureTest39 {

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

            /* A. 搜索命中规则：10-2 只命中 10-02，不命中 10-22 */
            Method hitM = OverviewPanel.class.getDeclaredMethod("doneHit",
                    String.class, String.class, String.class);
            hitM.setAccessible(true);
            check("10-2 命中 10-02", (Boolean) hitM.invoke(ov, "四十天前", "2026-10-02", "10-2"), "ok");
            check("10-2 不命中 10-22", !(Boolean) hitM.invoke(ov, "二十天前", "2026-10-22", "10-2"), "ok");
            check("10-22 命中 10-22", (Boolean) hitM.invoke(ov, "二十天前", "2026-10-22", "10-22"), "ok");
            check("空词全命中", (Boolean) hitM.invoke(ov, "随便", "2026-10-22", ""), "ok");
            check("『周一』命中标题", (Boolean) hitM.invoke(ov, "周一做的", "2026-11-09", "周一"), "ok");
            check("『周一』不命中今天已做完", !(Boolean) hitM.invoke(ov, "今天已做完", "2026-11-11", "周一"), "ok");
            check("单数字 11 按月命中", (Boolean) hitM.invoke(ov, "今天已做完", "2026-11-11", "11"), "ok");
            check("单数字 4 按日命中", (Boolean) hitM.invoke(ov, "上周做的", "2026-11-04", "4"), "ok");
            check("全日期串命中", (Boolean) hitM.invoke(ov, "今天已做完", "2026-11-11", "2026-11-11"), "ok");

            /* B. 副标题：本周 2 · 上周 1 · 本月 3 · 比上周多 1 条 */
            Method subM = OverviewPanel.class.getDeclaredMethod("doneSubTitle");
            subM.setAccessible(true);
            String sub = (String) subM.invoke(ov);
            check("副标题带三段数与趋势",
                    sub.equals("本周 2 条 · 上周 1 条 · 本月 3 条 · 比上周多 1 条"), sub);

            /* C. 清单面板：有搜索框、副标题、能筛到 1 条再清空回来 */
            Method dlM = OverviewPanel.class.getDeclaredMethod("doneListPanel",
                    String.class, String.class, List.class, JDialog.class);
            dlM.setAccessible(true);
            List month = rowsOf(ov, "month");
            JPanel panel = (JPanel) dlM.invoke(ov, "本月完成 · 共 3 条", sub, month, null);
            panel.setSize(520, 240);
            layoutAll(panel);
            check("清单里有副标题那一行", hasText(panel, "比上周多 1 条"), "ok");
            JTextField q = findField(panel);
            check("清单里有搜索框", q != null && q.isEnabled(), q == null ? "none" : q.getToolTipText());
            check("默认 3 行", doneRows(panel).size() == 3, "rows=" + doneRows(panel).size());
            if (q != null) {
                q.setText("周一");
                layoutAll(panel);
                check("搜『周一』筛到 1 行", doneRows(panel).size() == 1, "rows=" + doneRows(panel).size());
                check("标题显示筛出 1 条", hasText(panel, "筛出 1 条"), titleOf(panel));
                check("筛选时仍选中第一行", isSel(doneRows(panel), 0), "ok");
                q.setText("10-2");
                layoutAll(panel);
                check("搜『10-2』本月清单 0 行（不误命中 11 月）", doneRows(panel).size() == 0,
                        "rows=" + doneRows(panel).size());
                check("空结果有空态文案", hasText(panel, "没有匹配的条目"), "ok");
                q.setText("");
                layoutAll(panel);
                check("清空后回到 3 行", doneRows(panel).size() == 3, "rows=" + doneRows(panel).size());
                check("清空后标题回原样", hasText(panel, "本月完成 · 共 3 条"), titleOf(panel));
            }

            /* D. 退回待办：把『今天已做完』改回未完成 */
            Method undoM = OverviewPanel.class.getDeclaredMethod("undoDone", String.class, String.class);
            undoM.setAccessible(true);
            int r = (Integer) undoM.invoke(ov, "今天已做完", "2026-11-11");
            Thread.sleep(500);
            String file = String.join("\n", Store.readLines("todo.txt"));
            check("退回待办返回 1", r == 1, "r=" + r);
            check("todo.txt 里那条已变回 0", file.contains("0\t今天已做完\t2026-11-11"), "ok");
            check("本周完成数随之降到 1",
                    String.valueOf(subM.invoke(ov)).contains("本周 1 条"), String.valueOf(subM.invoke(ov)));
            check("重复退回同一条返回 0", (Integer) undoM.invoke(ov, "今天已做完", "2026-11-11") == 0, "ok");

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

    static boolean isSel(java.util.List<JPanel> rows, int idx) {
        for (int i = 0; i < rows.size(); i++) {
            boolean sel = rows.get(i).getBackground().getRGB() == new Color(0xDBEAFE).getRGB();
            if (i == idx && !sel) return false;
            if (i != idx && sel) return false;
        }
        return true;
    }

    static boolean hasText(Container p, String s) {
        for (Component c : all(p)) {
            if (c instanceof JLabel) {
                String t = ((JLabel) c).getText();
                if (t != null && t.contains(s)) return true;
            }
        }
        return false;
    }

    static String titleOf(Container p) {
        StringBuilder sb = new StringBuilder();
        for (Component c : all(p)) {
            if (c instanceof JLabel && ((JLabel) c).getText() != null) sb.append(((JLabel) c).getText()).append('/');
        }
        return sb.toString();
    }

    static JTextField findField(Container p) {
        for (Component c : all(p)) {
            if (c instanceof JTextField) return (JTextField) c;
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
