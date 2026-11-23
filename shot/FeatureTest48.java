import com.toolbox.pc.*;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** v1.66 验证：抬头右键菜单、空档逐日明细、小窗打勾可撤回 */
public class FeatureTest48 {

    private static boolean ok = true;
    private static OverviewPanel ov;
    private static JFrame frame;
    private static Object main;

    private static final String[] DATA = Seed.rows();

    public static void main(String[] a) {
        try {
            run();
        } catch (Throwable t) {
            t.printStackTrace();
            ok = false;
        }
        System.out.println(ok ? "ALL PASS" : "SOME FAILED");
        Runtime.getRuntime().halt(ok ? 0 : 1);
    }

    static void run() throws Exception {
        Store.writeLines("todo.txt", java.util.Arrays.asList(DATA));
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
        refreshOv();

        /* A. 空档提示带逐日明细 */
        JLabel gap = fieldLabel(OverviewPanel.class, ov, "gapLbl");
        String tip = gap == null ? "" : gap.getToolTipText();
        int ti = Seed.today().getDayOfWeek().getValue() - 1;
        if (ti <= 1) {
            /* 周一/周二：今天之前要么没有日子、要么周一已完成，没有空档可报 */
            check("周一二没有空档提示", gap == null || !gap.isVisible(),
                    gap == null ? "null" : gap.getText() + " vis=" + gap.isVisible());
        } else {
            check("空档提示仍在", gap != null && gap.isVisible() && ("空 " + (ti - 1) + " 天").equals(gap.getText()),
                    gap == null ? "null" : gap.getText() + " vis=" + gap.isVisible());
            check("提示里有本周合计", tip.contains("本周共 " + Seed.weekDone() + " 条"), tip);
            check("提示里逐日列到今天", tip.contains("周一 " + Seed.mondayDone()) && tip.contains("周二 0")
                    && tip.contains(Seed.wk(Seed.today()) + " " + 2), tip);
        }

        /* B. 分组抬头右键菜单 */
        Method dlM = OverviewPanel.class.getDeclaredMethod("doneListPanel",
                String.class, String.class, List.class, JDialog.class);
        dlM.setAccessible(true);
        List monthRows = monthRows();
        int monthN = Seed.monthDone();
        JPanel list = (JPanel) dlM.invoke(ov, "本月完成 · 共 " + monthN + " 条",
                "本周 " + Seed.weekDone() + " 条 · 上周 " + Seed.lastWeekDone() + " 条", monthRows, null);
        list.setSize(520, 352);
        layoutAll(list);
        java.util.List<JPanel> heads = headers(list);
        int dayGroups = distinctDays(monthRows);
        check(dayGroups + " 组抬头", heads.size() == dayGroups, "heads=" + heads.size());
        JPopupMenu gm = heads.isEmpty() ? null : heads.get(0).getComponentPopupMenu();
        check("抬头挂了右键菜单", gm != null, String.valueOf(gm));
        if (gm != null) {
            check("菜单 4 项", gm.getComponentCount() == 4, "items=" + gm.getComponentCount());
            check("第一项是收起这一天", ((JMenuItem) gm.getComponent(0)).getText().equals("收起这一天"),
                    ((JMenuItem) gm.getComponent(0)).getText());
            check("第二项只展开这一天", ((JMenuItem) gm.getComponent(1)).getText().equals("只展开这一天"),
                    ((JMenuItem) gm.getComponent(1)).getText());
            check("第三项带条数",
                    ((JMenuItem) gm.getComponent(2)).getText()
                            .equals("复制这天全部 " + Seed.todayDone() + " 条"),
                    ((JMenuItem) gm.getComponent(2)).getText());

            final JMenuItem only = (JMenuItem) gm.getComponent(1);
            SwingUtilities.invokeAndWait(() -> only.doClick());
            Thread.sleep(150);
            layoutAll(list);
            check("只展开这一天：条目 " + monthN + " → " + Seed.todayDone(),
                    rows(list).size() == Seed.todayDone(), "rows=" + rows(list).size());
            java.util.List<JPanel> h2 = headers(list);
            int shut = 0;
            for (JPanel h : h2) {
                if (eastText(h).contains("已收起")) shut++;
            }
            check("其它 " + (dayGroups - 1) + " 组都收起了", h2.size() == dayGroups && shut == dayGroups - 1,
                    "heads=" + h2.size() + " shut=" + shut);
            int pct = Math.round(Seed.todayDone() * 100f / monthN);
            check("留下那组仍是 " + Seed.todayDone() + " 条 · 占 " + pct + "%",
                    h2.size() > 0 && eastText(h2.get(0))
                            .equals(Seed.todayDone() + " 条 · 占 " + pct + "% · 收起"),
                    h2.isEmpty() ? "none" : eastText(h2.get(0)));

            JMenuItem copy = (JMenuItem) h2.get(0).getComponentPopupMenu().getComponent(2);
            SwingUtilities.invokeAndWait(() -> copy.doClick());
            Thread.sleep(150);
            java.awt.datatransfer.Transferable tr =
                    java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
            String clip = (String) tr.getTransferData(java.awt.datatransfer.DataFlavor.stringFlavor);
            String[] clipLines = clip.trim().split("\\R");
            boolean allToday = clipLines.length == Seed.todayDone();
            for (String cl : clipLines) {
                if (!cl.startsWith(Seed.md(Seed.today()))) allToday = false;
            }
            check("复制这天全部：" + Seed.todayDone() + " 行都带今天日期", allToday,
                    clip.replace('\n', '|'));
        }

        /* C. 小窗打勾 → 顶部出现撤回 → 点撤回恢复 */
        MiniBoard.show();
        Thread.sleep(700);
        Class<?> mbc = Class.forName("com.toolbox.pc.MiniBoard");
        Field instF = mbc.getDeclaredField("inst");
        instF.setAccessible(true);
        MiniBoard board = (MiniBoard) instF.get(null);
        Field lbF = MiniBoard.class.getDeclaredField("listBox");
        lbF.setAccessible(true);
        JPanel listBox = (JPanel) lbF.get(board);
        JLabel todoRow = findTodoRow(listBox, "今天的事");
        check("小窗里有这条待办", todoRow != null, String.valueOf(todoRow));
        if (todoRow != null) {
            clickOn(todoRow);
            Thread.sleep(400);
            check("文件里已标完成", doneFlag("今天的事") == 1, "done=" + doneFlag("今天的事"));
            check("顶部出现撤回提示", listBox.getComponentCount() > 0
                    && listBox.getComponent(0) instanceof JLabel
                    && ((JLabel) listBox.getComponent(0)).getText().contains("点我撤回"),
                    listBox.getComponentCount() == 0 ? "empty" : String.valueOf(((JLabel) listBox.getComponent(0)).getText()));
            JLabel pace = fieldLabel(MiniBoard.class, board, "paceLbl");
            check("节奏行当场变 4 条", pace.getText().contains("本周完成 4 条"), pace.getText());

            JLabel undo = (JLabel) listBox.getComponent(0);
            clickOn(undo);
            Thread.sleep(400);
            check("点撤回后文件改回未完成", doneFlag("今天的事") == 0, "done=" + doneFlag("今天的事"));
            Component top = listBox.getComponentCount() > 0 ? listBox.getComponent(0) : null;
            check("撤回提示消失", !(top instanceof JLabel
                            && ((JLabel) top).getText().contains("点我撤回")),
                    top == null ? "empty"
                            : (top instanceof JLabel ? ((JLabel) top).getText() : top.getClass().getSimpleName()));
            check("节奏行回到 3 条", pace.getText().contains("本周完成 3 条"), pace.getText());
        }
        Store.writeLines("todo.txt", java.util.Arrays.asList(DATA));
    }

    static JLabel findTodoRow(JPanel listBox, String text) {
        for (Component c : listBox.getComponents()) {
            if (c instanceof JLabel && ((JLabel) c).getText().contains(text)
                    && ((JLabel) c).getText().startsWith("○ ")) return (JLabel) c;
        }
        return null;
    }

    static void clickOn(Component c) throws Exception {
        SwingUtilities.invokeAndWait(() -> c.dispatchEvent(new MouseEvent(c, MouseEvent.MOUSE_CLICKED,
                System.currentTimeMillis(), 0, 6, 6, 1, false, MouseEvent.BUTTON1)));
    }

    static int doneFlag(String text) {
        for (String line : Store.readLines("todo.txt")) {
            String[] p = line.split("\t", -1);
            if (p.length < 2) continue;
            if (p[1].trim().equals(text)) return "1".equals(p[0].trim()) ? 1 : 0;
        }
        return -1;
    }

    static void refreshOv() throws Exception {
        Method rf = OverviewPanel.class.getDeclaredMethod("refresh");
        rf.setAccessible(true);
        rf.invoke(ov);
        Thread.sleep(200);
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

    static JLabel fieldLabel(Class<?> cls, Object target, String name) throws Exception {
        Field f = cls.getDeclaredField(name);
        f.setAccessible(true);
        return (JLabel) f.get(target);
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
