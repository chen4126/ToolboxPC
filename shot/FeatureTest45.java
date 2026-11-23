import com.toolbox.pc.*;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** v1.63 验证：今天做完 N 条、连 N 周↑徽标、清单按天分组 */
public class FeatureTest45 {

    private static boolean ok = true;
    private static OverviewPanel ov;

    private static final String[] DATA = {
            "0\t今天的事\t2026-11-11", "0\t九天前没动\t2026-11-02", "0\t四天前\t2026-11-07",
            "0\t昨天没做\t2026-11-10", "0\t六天前\t2026-11-05", "0\t十天前\t2026-11-01",
            "0\t明天的事\t2026-11-12", "0\t十五号\t2026-11-15", "0\t二十五号\t2026-11-25",
            "0\t没定日子的\t-",
            "1\t今天已做完\t2026-11-11", "1\t今天第二件\t2026-11-11",
            "1\t周一做的\t2026-11-09", "1\t上周做的\t2026-11-04", "1\t上周第二条\t2026-11-03",
            "1\t上上周做的\t2026-10-28",
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
            Method refresh = OverviewPanel.class.getDeclaredMethod("refresh");
            refresh.setAccessible(true);
            refresh.invoke(ov);
            Thread.sleep(300);

            /* A. 今天做完 N 条 */
            Method dtM = OverviewPanel.class.getDeclaredMethod("doneToday");
            dtM.setAccessible(true);
            int dt = (int) dtM.invoke(ov);
            check("今天完成 2 条", dt == 2, "n=" + dt);
            Field tdF = OverviewPanel.class.getDeclaredField("todayDone");
            tdF.setAccessible(true);
            JLabel todayDone = (JLabel) tdF.get(ov);
            check("标题旁文案", todayDone != null && "今天做完 2 条".equals(todayDone.getText()),
                    todayDone == null ? "null" : todayDone.getText());
            check("有完成时用蓝色", todayDone != null && Ui.PRIMARY.equals(todayDone.getForeground()),
                    todayDone == null ? "null" : String.valueOf(todayDone.getForeground()));
            check("看今天时显示", todayDone != null && todayDone.isVisible(), "visible=" + (todayDone != null && todayDone.isVisible()));
            check("可点看今天清单", todayDone != null && todayDone.getMouseListeners().length > 0,
                    todayDone == null ? "null" : "listeners=" + todayDone.getMouseListeners().length);

            /* B. 连续上涨周数 */
            Method stM = OverviewPanel.class.getDeclaredMethod("streakUpWeeks");
            stM.setAccessible(true);
            int streak = (int) stM.invoke(ov);
            check("本周 3 > 上周 2 > 上上周 1，连续 2 周", streak == 2, "streak=" + streak);
            Field skF = OverviewPanel.class.getDeclaredField("streakLbl");
            skF.setAccessible(true);
            JLabel sk = (JLabel) skF.get(ov);
            check("徽标文案", sk != null && "连 2 周↑".equals(sk.getText()), sk == null ? "null" : sk.getText());
            check("徽标显示出来", sk != null && sk.isVisible(), "visible=" + (sk != null && sk.isVisible()));

            /* 数据里上周为空时不该算上涨：临时把上周两条去掉再算 */
            java.util.List<String> trimmed = new java.util.ArrayList<String>();
            for (String line : Store.readLines("todo.txt")) if (!line.contains("2026-11-04") && !line.contains("2026-11-03")) trimmed.add(line);
            Store.writeLines("todo.txt", trimmed);
            Method load = OverviewPanel.class.getDeclaredMethod("loadTodos");
            load.setAccessible(true);
            load.invoke(ov);
            int streak2 = (int) stM.invoke(ov);
            check("上周空着就不算连续（不虚高）", streak2 <= 1, "streak=" + streak2);
            Store.writeLines("todo.txt", java.util.Arrays.asList(DATA));
            load.invoke(ov);
            refresh.invoke(ov);
            Thread.sleep(200);

            /* C. 清单按天分组 */
            Method dlM = OverviewPanel.class.getDeclaredMethod("doneListPanel",
                    String.class, String.class, List.class, JDialog.class);
            dlM.setAccessible(true);
            List month = rowsOf("month");
            JPanel list = (JPanel) dlM.invoke(ov, "本月完成 · 共 5 条", "本周 3 条 · 上周 2 条", month, null);
            list.setSize(520, 300);
            layoutAll(list);
            java.util.List<JPanel> heads = headers(list);
            check("四个日期分组", heads.size() == 4, "heads=" + heads.size());
            check("第一组 11-11", heads.size() > 0 && "11-11".equals(labelOf(heads.get(0), 0))
                    && "2 条".equals(labelOf(heads.get(0), 1)), heads.isEmpty() ? "none" : labelOf(heads.get(0), 0) + "/" + labelOf(heads.get(0), 1));
            check("第二组 11-09 一条", heads.size() > 1 && "11-09".equals(labelOf(heads.get(1), 0))
                    && "1 条".equals(labelOf(heads.get(1), 1)), heads.size() > 1 ? labelOf(heads.get(1), 0) + "/" + labelOf(heads.get(1), 1) : "none");
            java.util.List<JPanel> rows = doneRows(list);
            check("五行条目都在", rows.size() == 5, "rows=" + rows.size());
            check("行内日期列已清空（日期只在分组头上）", rows.size() > 0 && "".equals(labelOf(rows.get(0), 0)),
                    rows.isEmpty() ? "none" : "'" + labelOf(rows.get(0), 0) + "'");

            System.out.println(ok ? "ALL PASS" : "SOME FAIL");
        } catch (Throwable t) {
            t.printStackTrace();
            ok = false;
        }
        Runtime.getRuntime().halt(ok ? 0 : 1);
    }

    static String labelOf(JPanel p, int idx) {
        if (p.getComponentCount() <= idx) return "?";
        return firstLabelText(p.getComponent(idx));
    }

    static String firstLabelText(Component c) {
        if (c instanceof JLabel) return ((JLabel) c).getText();       // 空串也照实返回（行首日期列就是空串）
        if (c instanceof Container) {
            for (Component k : ((Container) c).getComponents()) {
                if (k instanceof JLabel && !((JLabel) k).getText().isEmpty()) return ((JLabel) k).getText();
            }
            for (Component k : ((Container) c).getComponents()) {
                if (k instanceof Container) {
                    String s = firstLabelText(k);
                    if (s != null && !s.isEmpty() && !"?".equals(s)) return s;
                }
            }
        }
        return "?";
    }

    static java.util.List<JPanel> headers(JPanel p) {
        java.util.List<JPanel> out = new java.util.ArrayList<JPanel>();
        for (Component c : all(p)) {
            if (!(c instanceof JPanel)) continue;
            JPanel h = (JPanel) c;
            if (!(h.getBorder() instanceof CompoundBorder)) continue;
            if (h.getMaximumSize() == null) continue;
            int mh = h.getMaximumSize().height;
            if (mh != 22 && mh != 28) continue;      // 首组 22，其余 28
            out.add(h);
        }
        return out;
    }

    static List<?> rowsOf(String which) throws Exception {
        java.time.LocalDate now = java.time.LocalDate.now();
        java.time.LocalDate from = now.withDayOfMonth(1);
        java.time.LocalDate to = now.withDayOfMonth(now.lengthOfMonth());
        Field tF = OverviewPanel.class.getDeclaredField("todos");
        tF.setAccessible(true);
        List<?> todos = (List<?>) tF.get(ov);
        java.util.List<String[]> out = new java.util.ArrayList<String[]>();
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
            out.add(new String[]{text, d.toString()});
        }
        java.util.Collections.sort(out, (x, y) -> y[1].compareTo(x[1]));
        return out;
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
