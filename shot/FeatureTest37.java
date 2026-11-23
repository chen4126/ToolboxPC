import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** v1.57 验证：完成清单弹窗、概览本周/上周点开清单、小窗底栏"最早那天" */
public class FeatureTest37 {

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
            Thread.sleep(400);

            /* A. 完成清单：本周 2 条 / 上周 1 条 / 本月 3 条，行里带日期和标题 */
            Method dlM = OverviewPanel.class.getDeclaredMethod("doneListPanel",
                    String.class, List.class, JDialog.class);
            dlM.setAccessible(true);
            JPanel weekP = (JPanel) dlM.invoke(ov, "本周完成 · 共 2 条", weekRows(ov), null);
            weekP.setSize(520, 178);
            layoutAll(weekP);
            check("本周清单两行都在", countRows(weekP) == 2, "rows=" + countRows(weekP));
            check("本周清单里有『今天已做完』", weekPContains(weekP, "今天已做完"), tx(weekP));
            check("本周清单里有『周一做的』", weekPContains(weekP, "周一做的"), "ok");

            JPanel lastP = (JPanel) dlM.invoke(ov, "上周完成 · 共 1 条", lastRows(ov), null);
            lastP.setSize(520, 148);
            layoutAll(lastP);
            check("上周清单只有 1 条且是『上周做的』",
                    countRows(lastP) == 1 && lastPContains(lastP, "上周做的"),
                    "rows=" + countRows(lastP) + " " + tx(lastP));

            JPanel monthP = (JPanel) dlM.invoke(ov, "本月完成 · 共 3 条", monthRows(ov), null);
            monthP.setSize(520, 206);
            layoutAll(monthP);
            check("本月清单 3 条", countRows(monthP) == 3, "rows=" + countRows(monthP));

            /* B. 点概览"本周2"→ 弹出本周清单（不跳页） */
            Field wwF = OverviewPanel.class.getDeclaredField("wWeek");
            wwF.setAccessible(true);
            final JLabel ww = (JLabel) wwF.get(ov);
            SwingUtilities.invokeAndWait(() -> {
                java.awt.event.MouseEvent e = new java.awt.event.MouseEvent(ww,
                        java.awt.event.MouseEvent.MOUSE_CLICKED, 0,
                        java.awt.event.MouseEvent.BUTTON1_DOWN_MASK, 1, 1, 1, false,
                        java.awt.event.MouseEvent.BUTTON1);
                ww.dispatchEvent(e);
            });
            Thread.sleep(700);
            int dl = countWindows("本周完成");
            check("点『本周2』弹出了本周完成清单窗口", dl >= 1, "dialogs=" + dl);
            closeWindows("本周完成");

            /* C. 点"·上周1"→ 弹出上周清单 */
            Field wlF = OverviewPanel.class.getDeclaredField("wLast");
            wlF.setAccessible(true);
            final JLabel wl = (JLabel) wlF.get(ov);
            SwingUtilities.invokeAndWait(() -> {
                java.awt.event.MouseEvent e = new java.awt.event.MouseEvent(wl,
                        java.awt.event.MouseEvent.MOUSE_CLICKED, 0,
                        java.awt.event.MouseEvent.BUTTON1_DOWN_MASK, 1, 1, 1, false,
                        java.awt.event.MouseEvent.BUTTON1);
                wl.dispatchEvent(e);
            });
            Thread.sleep(700);
            int dl2 = countWindows("上周完成");
            check("点『·上周1』弹出了上周完成清单窗口", dl2 >= 1, "dialogs=" + dl2);
            closeWindows("上周完成");

            /* D. 统计页两张待办卡可点（手型 + 提示） */
            Object stats = Main.page(StatsPanel.class);
            Field twcF = StatsPanel.class.getDeclaredField("todoWeekCard");
            twcF.setAccessible(true);
            Field tmcF = StatsPanel.class.getDeclaredField("todoMonthCard");
            tmcF.setAccessible(true);
            JPanel twc = (JPanel) twcF.get(stats);
            JPanel tmc = (JPanel) tmcF.get(stats);
            check("统计页本周卡可点（手型 + tooltip）",
                    twc.getCursor().getType() == Cursor.HAND_CURSOR && twc.getToolTipText() != null,
                    twc.getToolTipText());
            check("统计页本月卡可点（手型 + tooltip）",
                    tmc.getCursor().getType() == Cursor.HAND_CURSOR && tmc.getToolTipText() != null,
                    tmc.getToolTipText());

            /* E. jumpToDay：点清单里某行会跳到那天 */
            Method jdM = OverviewPanel.class.getDeclaredMethod("jumpToDay", java.time.LocalDate.class);
            jdM.setAccessible(true);
            jdM.invoke(ov, java.time.LocalDate.of(2026, 11, 2));
            Thread.sleep(500);
            Field vdF = OverviewPanel.class.getDeclaredField("viewDate");
            vdF.setAccessible(true);
            check("jumpToDay 把视图切到 11-02", String.valueOf(vdF.get(ov)).contains("11-02"),
                    String.valueOf(vdF.get(ov)));

            /* F. 小窗底栏：还有 1 条 / 最早 11-01 / 全部勾完 5 条，三段都放得下且不重叠 */
            Field instF = MiniBoard.class.getDeclaredField("inst");
            instF.setAccessible(true);
            Object board = instF.get(null);
            if (board == null) {
                SwingUtilities.invokeAndWait(() -> MiniBoard.show());
                Thread.sleep(900);
                board = instF.get(null);
            }
            vdF.set(ov, null);
            Method rr = OverviewPanel.class.getDeclaredMethod("refresh");
            rr.setAccessible(true);
            rr.invoke(ov);
            Field ovvF = MiniBoard.class.getDeclaredField("overdueView");
            ovvF.setAccessible(true);
            ovvF.set(board, true);
            Method br = MiniBoard.class.getDeclaredMethod("refresh");
            br.setAccessible(true);
            br.invoke(board);
            Thread.sleep(700);
            Field pinF = MiniBoard.class.getDeclaredField("pinBox");
            pinF.setAccessible(true);
            JPanel pinBox = (JPanel) pinF.get(board);
            Rectangle pr = boundsIn(pinBox, pinBox);
            java.util.Map<String, Rectangle> rects = new java.util.HashMap<String, Rectangle>();
            for (Component c : pinBox.getComponents()) {
                if (c instanceof JLabel) rects.put(((JLabel) c).getText(), boundsIn(c, pinBox));
            }
            String texts = "";
            for (String k : rects.keySet()) texts += "[" + k + "]";
            check("底栏三段都在", texts.contains("还有 1 条") && texts.contains("最早 11-01")
                    && texts.contains("全部勾完 5 条"), texts);
            boolean overlap = false;
            Rectangle a = null, b = null, c2 = null;
            for (java.util.Map.Entry<String, Rectangle> en : rects.entrySet()) {
                if (en.getKey().startsWith("还有 ")) a = en.getValue();
                if (en.getKey().startsWith("最早 ")) b = en.getValue();
                if (en.getKey().startsWith("全部勾完")) c2 = en.getValue();
            }
            if (a != null && b != null && a.intersects(b)) overlap = true;
            if (b != null && c2 != null && b.intersects(c2)) overlap = true;
            if (a != null && c2 != null && a.intersects(c2)) overlap = true;
            boolean inside = true;
            for (Rectangle r : rects.values()) {
                if (r.x + r.width > pr.width + 2) inside = false;
            }
            check("三段互不重叠", !overlap, "overlap=" + overlap);
            check("三段都没超出底栏宽度", inside, "pinW=" + pr.width + " " + rects.values());

            /* G. 点"最早 11-01"→ 主窗切到 11-01 那天 */
            JLabel early = null;
            for (Component c : pinBox.getComponents()) {
                if (c instanceof JLabel && ((JLabel) c).getText().startsWith("最早 ")) early = (JLabel) c;
            }
            final JLabel ef = early;
            if (ef != null) {
                SwingUtilities.invokeAndWait(() -> {
                    java.awt.event.MouseEvent e = new java.awt.event.MouseEvent(ef,
                            java.awt.event.MouseEvent.MOUSE_CLICKED, 0,
                            java.awt.event.MouseEvent.BUTTON1_DOWN_MASK, 1, 1, 1, false,
                            java.awt.event.MouseEvent.BUTTON1);
                    ef.dispatchEvent(e);
                });
                Thread.sleep(600);
            }
            check("点『最早 11-01』后主窗视图落在 11-01",
                    String.valueOf(vdF.get(ov)).contains("11-01"), String.valueOf(vdF.get(ov)));

            System.out.println(ok ? "ALL PASS" : "SOME FAIL");
        } catch (Throwable t) {
            t.printStackTrace();
            ok = false;
        }
        Runtime.getRuntime().halt(ok ? 0 : 1);
    }

    @SuppressWarnings("unchecked")
    static List<String[]> weekRows(OverviewPanel ov) throws Exception {
        return (List<String[]>) rowsOf(ov, "week");
    }

    static List<?> lastRows(OverviewPanel ov) throws Exception {
        return rowsOf(ov, "last");
    }

    static List<?> monthRows(OverviewPanel ov) throws Exception {
        return rowsOf(ov, "month");
    }

    /** 反射走 openDoneList 的数据源：拷一份 todos 用真实统计口径算 */
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
        Class<?> tc = null;
        for (Object t : todos) {
            tc = t.getClass();
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

    static int countRows(JPanel p) {
        int n = 0;
        for (Component c : all(p)) {
            if (!(c instanceof JPanel)) continue;
            JPanel row = (JPanel) c;
            if (row.getMaximumSize() == null || row.getMaximumSize().height != 24) continue;
            if (row.getComponentCount() != 2) continue;
            if (row.getComponent(0) instanceof JLabel && row.getComponent(1) instanceof JLabel) n++;
        }
        return n;
    }

    static boolean weekPContains(JPanel p, String s) {
        return contains(p, s);
    }

    static boolean lastPContains(JPanel p, String s) {
        return contains(p, s);
    }

    static boolean contains(Container p, String s) {
        for (Component c : all(p)) {
            if (c instanceof JLabel) {
                String t = ((JLabel) c).getText();
                if (t != null && t.contains(s)) return true;
            }
        }
        return false;
    }

    static String tx(Container p) {
        StringBuilder sb = new StringBuilder();
        for (Component c : all(p)) {
            if (c instanceof JLabel && ((JLabel) c).getText() != null) sb.append(((JLabel) c).getText()).append('/');
        }
        return sb.toString();
    }

    static java.util.List<Component> all(Container p) {
        java.util.List<Component> out = new java.util.ArrayList<Component>();
        for (Component c : p.getComponents()) {
            out.add(c);
            if (c instanceof Container) out.addAll(all((Container) c));
        }
        return out;
    }

    static Rectangle boundsIn(Component c, Container root) {
        return SwingUtilities.convertRectangle(c.getParent(), c.getBounds(), root);
    }

    static int countWindows(String titlePart) {
        int n = 0;
        for (Window w : Window.getWindows()) {
            if (w instanceof JDialog && w.isVisible()) {
                String t = ((JDialog) w).getTitle();
                if (t != null && t.contains(titlePart)) n++;
            }
        }
        return n;
    }

    static void closeWindows(String titlePart) {
        for (Window w : Window.getWindows()) {
            if (w instanceof JDialog && w.isVisible()) {
                String t = ((JDialog) w).getTitle();
                if (t != null && t.contains(titlePart)) w.dispose();
            }
        }
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
