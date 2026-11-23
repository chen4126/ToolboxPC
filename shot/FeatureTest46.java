import com.toolbox.pc.*;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** v1.64 验证：统计页角标、清单分组折叠、右键撤回今天 */
public class FeatureTest46 {

    private static boolean ok = true;
    private static OverviewPanel ov;
    private static JFrame frame;
    private static Object main;

    private static final String[] DATA = Seed.rows();

    public static void main(String[] a) {
        try {
            SwingUtilities.invokeAndWait(() -> Main.main(new String[0]));
            Thread.sleep(2500);
            Class<?> mc = Class.forName("com.toolbox.pc.Main");
            Field selfF = mc.getDeclaredField("self");
            selfF.setAccessible(true);
            main = selfF.get(null);
            Field ff = mc.getDeclaredField("frame");
            ff.setAccessible(true);
            frame = (JFrame) ff.get(main);
            go("overview");
            Thread.sleep(1000);
            ov = find(frame.getContentPane(), OverviewPanel.class);

            /* A. 统计页角标 */
            go("stats");
            Thread.sleep(900);
            StatsPanel sp = find(frame.getContentPane(), StatsPanel.class);
            Method rf = StatsPanel.class.getDeclaredMethod("refresh");
            rf.setAccessible(true);
            rf.invoke(sp);
            Thread.sleep(200);
            JLabel weekBadge = fieldLabel(StatsPanel.class, sp, "todoWeekBadge");
            JLabel monthBadge = fieldLabel(StatsPanel.class, sp, "todoMonthBadge");
            check("本周卡角标 今天 " + Seed.todayDone() + " 条",
                    weekBadge != null && ("今天 " + Seed.todayDone() + " 条").equals(weekBadge.getText()),
                    weekBadge == null ? "null" : weekBadge.getText());
            check("角标用主色", weekBadge != null && Ui.PRIMARY.equals(weekBadge.getForeground()),
                    weekBadge == null ? "null" : String.valueOf(weekBadge.getForeground()));
            check("本月卡角标 连 2 周↑", monthBadge != null && "连 2 周↑".equals(monthBadge.getText()),
                    monthBadge == null ? "null" : monthBadge.getText());
            JLabel wkVal = fieldLabel(StatsPanel.class, sp, "todoWeekLabel");
            JLabel moVal = fieldLabel(StatsPanel.class, sp, "todoMonthLabel");
            check("卡片数字仍是本周 " + Seed.weekDone() + " / 本月 " + Seed.monthDone(),
                    wkVal != null && (Seed.weekDone() + " 条").equals(wkVal.getText())
                            && moVal != null && (Seed.monthDone() + " 条").equals(moVal.getText()),
                    (wkVal == null ? "?" : wkVal.getText()) + " / " + (moVal == null ? "?" : moVal.getText()));

            go("overview");
            Thread.sleep(700);

            /* B. 清单分组折叠 */
            Method dlM = OverviewPanel.class.getDeclaredMethod("doneListPanel",
                    String.class, String.class, List.class, JDialog.class);
            dlM.setAccessible(true);
            List monthRows = monthRows();
            JPanel list = (JPanel) dlM.invoke(ov, "本月完成 · 共 5 条", "本周 3 条 · 上周 2 条", monthRows, null);
            list.setSize(520, 352);
            layoutAll(list);
            java.util.List<JPanel> heads = headers(list);
            int monthN = Seed.monthDone();
            int dayGroups = distinctDays(monthRows);
            check("初始 " + dayGroups + " 组 " + monthN + " 行",
                    heads.size() == dayGroups && rows(list).size() == monthN,
                    "heads=" + heads.size() + " rows=" + rows(list).size());
            check("抬头右侧带收起提示", heads.size() > 0 && eastText(heads.get(0)).contains("收起"),
                    heads.isEmpty() ? "none" : eastText(heads.get(0)));
            click(heads.get(0));
            Thread.sleep(150);
            layoutAll(list);
            java.util.List<JPanel> h2 = headers(list);
            check("点一下第一组收起：条目 " + monthN + " → " + (monthN - Seed.todayDone()),
                    rows(list).size() == monthN - Seed.todayDone(), "rows=" + rows(list).size());
            check("收起后抬头写 已收起", h2.size() > 0 && "已收起 · 展开".equals(labelOf(h2.get(0), 1)),
                    h2.isEmpty() ? "none" : labelOf(h2.get(0), 1));
            check("收起后提示可展开", h2.size() > 0 && eastText(h2.get(0)).contains("展开"),
                    h2.isEmpty() ? "none" : eastText(h2.get(0)));
            check("其它组还在", h2.size() == dayGroups, "heads=" + h2.size());
            click(h2.get(0));
            Thread.sleep(150);
            layoutAll(list);
            check("再点一下展开回 " + monthN + " 行", rows(list).size() == monthN, "rows=" + rows(list).size());

            /* C. 右键撤回今天全部 */
            Method bm = OverviewPanel.class.getDeclaredMethod("buildTodayMenu");
            bm.setAccessible(true);
            JPopupMenu menu = (JPopupMenu) bm.invoke(ov);
            check("右键菜单只有一项", menu.getComponentCount() == 1, "items=" + menu.getComponentCount());
            check("菜单文案带条数", menu.getComponentCount() == 1
                    && ("撤回今天全部 " + Seed.todayDone() + " 条").equals(((JMenuItem) menu.getComponent(0)).getText()),
                    menu.getComponentCount() == 1 ? ((JMenuItem) menu.getComponent(0)).getText() : "none");

            Method ua = OverviewPanel.class.getDeclaredMethod("undoAllToday");
            ua.setAccessible(true);
            int undone = (int) ua.invoke(ov);
            check("撤回 " + Seed.todayDone() + " 条", undone == Seed.todayDone(), "n=" + undone);
            Method dtM = OverviewPanel.class.getDeclaredMethod("doneToday");
            dtM.setAccessible(true);
            check("今天完成归零", (int) dtM.invoke(ov) == 0, "n=" + dtM.invoke(ov));
            JLabel todayDone = fieldLabel(OverviewPanel.class, ov, "todayDone");
            check("标题变 今天还没勾", todayDone != null && "今天还没勾".equals(todayDone.getText()),
                    todayDone == null ? "null" : todayDone.getText());
            int doneLines = 0;
            for (String l : Store.readLines("todo.txt")) {
                if (l.startsWith("1\t") && l.contains(Seed.today().toString())) doneLines++;
            }
            check("文件里今天已无完成项", doneLines == 0, "done=" + doneLines);
            int archivedKeep = 0;
            for (String l : Store.readLines("todo.txt")) {
                if (l.startsWith("1\t") && l.contains("（已归档）")) archivedKeep++;
            }
            check("归档的没被误撤回", archivedKeep == 2, "arch=" + archivedKeep);

            Store.writeLines("todo.txt", java.util.Arrays.asList(DATA));
            Method load = OverviewPanel.class.getDeclaredMethod("loadTodos");
            load.setAccessible(true);
            load.invoke(ov);
            Method ovRefresh = OverviewPanel.class.getDeclaredMethod("refresh");
            ovRefresh.setAccessible(true);
            ovRefresh.invoke(ov);

            System.out.println(ok ? "ALL PASS" : "SOME FAIL");
        } catch (Throwable t) {
            t.printStackTrace();
            ok = false;
        }
        Runtime.getRuntime().halt(ok ? 0 : 1);
    }

    static void go(String key) throws Exception {
        Method sel = Class.forName("com.toolbox.pc.Main").getDeclaredMethod("selectNav", String.class);
        sel.setAccessible(true);
        SwingUtilities.invokeAndWait(() -> {
            try {
                sel.invoke(main, key);
            } catch (Exception ignored) {
            }
        });
    }

    static String eastText(JPanel head) {
        for (Component c : head.getComponents()) {
            if (c instanceof Container && head.getLayout() instanceof BorderLayout) {
                Object con = ((BorderLayout) head.getLayout()).getConstraints(c);
                if (BorderLayout.EAST.equals(con)) {
                    StringBuilder sb = new StringBuilder();
                    collectText((Container) c, sb);
                    return sb.toString();
                }
            }
        }
        return "?";
    }

    static void collectText(Container c, StringBuilder sb) {
        for (Component k : c.getComponents()) {
            if (k instanceof JLabel) sb.append(((JLabel) k).getText());
            if (k instanceof Container) collectText((Container) k, sb);
        }
    }

    static void click(Component c) {
        c.dispatchEvent(new MouseEvent(c, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
                0, 6, 6, 1, false, MouseEvent.BUTTON1));
    }

    static JLabel fieldLabel(Class<?> cls, Object target, String name) throws Exception {
        Field f = cls.getDeclaredField(name);
        f.setAccessible(true);
        return (JLabel) f.get(target);
    }

    static String labelOf(JPanel p, int idx) {
        if (p.getComponentCount() <= idx) return "?";
        return firstLabelText(p.getComponent(idx));
    }

    static String firstLabelText(Component c) {
        if (c instanceof JLabel) return ((JLabel) c).getText();
        if (c instanceof Container) {
            for (Component k : ((Container) c).getComponents()) {
                if (k instanceof JLabel && !((JLabel) k).getText().trim().isEmpty()) return ((JLabel) k).getText();
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
            if (mh != 22 && mh != 28) continue;
            out.add(h);
        }
        return out;
    }

    static java.util.List<JPanel> rows(JPanel p) {
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

    static List<?> monthRows() throws Exception {
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

    /** 本月清单里有几个不同的完成日（组数跟着天数走） */
    static int distinctDays(List<?> monthRows) {
        java.util.Set<String> days = new java.util.HashSet<String>();
        for (Object r : monthRows) {
            String[] row = (String[]) r;
            days.add(row[1]);
        }
        return days.size();
    }

    static java.util.List<Component> all(Container p) {
        java.util.List<Component> out = new java.util.ArrayList<Component>();
        for (Component c : p.getComponents()) {
            out.add(c);
            if (c instanceof Container) out.addAll(all((Container) c));
        }
        return out;
    }

    static void layoutAll(Component c) {
        c.doLayout();
        if (c instanceof Container) for (Component k : ((Container) c).getComponents()) layoutAll(k);
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
