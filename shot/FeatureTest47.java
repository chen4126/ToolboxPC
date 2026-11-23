import com.toolbox.pc.*;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** v1.65 验证：节奏行“空 N 天”、分组抬头占比、小窗节奏行右键 */
public class FeatureTest47 {

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

        /* A. 节奏行“空 N 天” */
        JLabel gap = fieldLabel(OverviewPanel.class, ov, "gapLbl");
        Method idleM = OverviewPanel.class.getDeclaredMethod("weekIdleDays");
        idleM.setAccessible(true);
        int ti = Seed.today().getDayOfWeek().getValue() - 1;   // 今天索引，周一=0
        List<Integer> idle = (List<Integer>) idleM.invoke(ov);
        if (ti <= 1) {
            /* 周一/周二：今天之前要么没有日子、要么周一已完成，没有空档可报 */
            check("周一二没有空档", idle.isEmpty(), "idle=" + idle);
            check("周一二空档提示不显示", gap == null || !gap.isVisible(),
                    gap == null ? "null" : "vis=" + gap.isVisible());
        } else {
            check("本周空档是周一之后到今天之前（" + (ti - 1) + " 天）",
                    idle.size() == ti - 1 && idle.get(0) == 1, "idle=" + idle);
            check("节奏行写 空 " + idle.size() + " 天",
                    gap != null && ("空 " + idle.size() + " 天").equals(gap.getText()),
                    gap == null ? "null" : gap.getText());
            check("空档提示可见", gap != null && gap.isVisible(), gap == null ? "null" : "vis=" + gap.isVisible());
            check("悬浮提示点名 " + Seed.wk(Seed.monday().plusDays(1)),
                    gap != null && gap.getToolTipText() != null
                            && gap.getToolTipText().contains(Seed.wk(Seed.monday().plusDays(1))),
                    gap == null ? "null" : gap.getToolTipText());
            check("是手型", gap != null && gap.getCursor().getType() == Cursor.HAND_CURSOR,
                    gap == null ? "null" : String.valueOf(gap.getCursor().getType()));

            click(gap);
            Thread.sleep(200);
            Field vdF = OverviewPanel.class.getDeclaredField("viewDate");
            vdF.setAccessible(true);
            Object vd = vdF.get(ov);
            check("点一下跳到 " + Seed.wk(Seed.monday().plusDays(1)) + " " + Seed.md(Seed.monday().plusDays(1)),
                    String.valueOf(vd).equals(Seed.monday().plusDays(1).toString()), "viewDate=" + vd);
            check("进入某天视图后空档提示收起", gap == null || !gap.isVisible(),
                    gap == null ? "null" : "vis=" + gap.isVisible());
            vdF.set(ov, null);
            refreshOv();
            check("回到今天后空档提示又出现", gap != null && gap.isVisible(),
                    "vis=" + (gap != null && gap.isVisible()));
        }

        /* B. 分组抬头带占比 */
        Method dlM = OverviewPanel.class.getDeclaredMethod("doneListPanel",
                String.class, String.class, List.class, JDialog.class);
        dlM.setAccessible(true);
        List monthRows = monthRows();
        int monthN = Seed.monthDone();
        check("本月清单 " + monthN + " 条", monthRows.size() == monthN, "rows=" + monthRows.size());
        JPanel list = (JPanel) dlM.invoke(ov, "本月完成 · 共 " + monthN + " 条",
                "本周 " + Seed.weekDone() + " 条 · 上周 " + Seed.lastWeekDone() + " 条", monthRows, null);
        list.setSize(520, 352);
        layoutAll(list);
        java.util.List<JPanel> heads = headers(list);
        int dayGroups = distinctDays(monthRows);
        check(dayGroups + " 组抬头", heads.size() == dayGroups, "heads=" + heads.size());
        for (int i = 0; i < heads.size(); i++) {
            System.out.println("  head" + i + ": " + labelOf(heads.get(i), 0) + " | " + labelOf(heads.get(i), 1)
                    + " | n=" + heads.get(i).getComponentCount());
        }
        int firstPct = Math.round(Seed.todayDone() * 100f / monthN);
        check("首组写 占 " + firstPct + "%",
                heads.size() > 0 && (Seed.todayDone() + " 条 · 占 " + firstPct + "% · 收起")
                        .equals(labelOf(heads.get(0), 1)),
                heads.isEmpty() ? "none" : labelOf(heads.get(0), 1));
        int secondPct = Math.round(100f / monthN);
        check("第二组写 占 " + secondPct + "%",
                heads.size() > 1 && ("1 条 · 占 " + secondPct + "% · 收起").equals(labelOf(heads.get(1), 1)),
                heads.size() > 1 ? labelOf(heads.get(1), 1) : "none");
        click(heads.get(0));
        Thread.sleep(150);
        layoutAll(list);
        java.util.List<JPanel> h2 = headers(list);
        check("收起后仍只写状态不写占比", h2.size() > 0 && "已收起 · 展开".equals(labelOf(h2.get(0), 1)),
                h2.isEmpty() ? "none" : labelOf(h2.get(0), 1));

        /* C. 小窗节奏行右键 */
        MiniBoard.show();
        Thread.sleep(600);
        Class<?> mb = Class.forName("com.toolbox.pc.MiniBoard");
        Field instF = mb.getDeclaredField("inst");
        instF.setAccessible(true);
        MiniBoard board = (MiniBoard) instF.get(null);
        check("小窗建起来了", board != null, "inst=" + board);
        if (board != null) {
            Method pm = MiniBoard.class.getDeclaredMethod("buildPaceMenu");
            pm.setAccessible(true);
            JPopupMenu menu = (JPopupMenu) pm.invoke(board);
            check("今天有 2 条完成 → 菜单 3 项", menu.getComponentCount() == 3, "items=" + menu.getComponentCount());
            check("第一项看本周", "看本周完成清单".equals(menu.getComponent(0).toString().length() > 0
                    ? ((JMenuItem) menu.getComponent(0)).getText() : ""),
                    ((JMenuItem) menu.getComponent(0)).getText());
            check("第二项带今天条数",
                    ((JMenuItem) menu.getComponent(1)).getText().contains("今天做完的 " + Seed.todayDone() + " 条"),
                    ((JMenuItem) menu.getComponent(1)).getText());
            check("第三项是撤回今天",
                    ((JMenuItem) menu.getComponent(2)).getText()
                            .equals("撤回今天全部 " + Seed.todayDone() + " 条"),
                    ((JMenuItem) menu.getComponent(2)).getText());

            Method undoM = OverviewPanel.class.getDeclaredMethod("undoAllToday");
            undoM.setAccessible(true);
            int n = (Integer) undoM.invoke(ov);
            check("撤回今天 " + Seed.todayDone() + " 条", n == Seed.todayDone(), "n=" + n);
            Method dtM = OverviewPanel.class.getDeclaredMethod("doneToday");
            dtM.setAccessible(true);
            int left = (Integer) dtM.invoke(ov);
            check("今天完成归零", left == 0, "doneToday=" + left);
            JPopupMenu menu2 = (JPopupMenu) pm.invoke(board);
            check("撤回后菜单只剩看本周", menu2.getComponentCount() == 1, "items=" + menu2.getComponentCount());
            check("小窗节奏行跟着更新", fieldLabel(MiniBoard.class, board, "paceLbl").getText().contains("本周"),
                    fieldLabel(MiniBoard.class, board, "paceLbl").getText());

            int done = 0, arch = 0;
            for (String line : Store.readLines("todo.txt")) {
                String[] p = line.split("\t", -1);
                if (p.length < 3) continue;
                if (!"1".equals(p[0].trim())) continue;
                if (p[1].trim().endsWith("（已归档）")) arch++;
                else if (p[2].trim().equals(Seed.today().toString())) done++;
            }
            check("文件里今天已无完成项", done == 0, "done=" + done);
            check("归档的没被误撤回", arch == 2, "arch=" + arch);
        }
        Store.writeLines("todo.txt", java.util.Arrays.asList(DATA));
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
