import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** v1.61 验证：节奏行七天圆点、小窗本周完成行、清单 Ctrl 多选退回 */
public class FeatureTest41 {

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

            /* A. 七天圆点数据：本周只有 11-09、11-11 有完成 */
            Method wdM = OverviewPanel.class.getDeclaredMethod("weekDoneDays");
            wdM.setAccessible(true);
            boolean[] f = (boolean[]) wdM.invoke(ov);
            check("本周 7 天长度", f.length == 7, "len=" + f.length);
            check("周一(11-09)有完成", f[0], "f0=" + f[0]);
            check("周三(11-11 今天)有完成", f[2], "f2=" + f[2]);
            check("周二没完成", !f[1], "f1=" + f[1]);
            check("周四周五没完成", !f[3] && !f[4], "f3=" + f[3] + " f4=" + f[4]);
            check("周日没完成", !f[6], "f6=" + f[6]);

            /* B. 圆点组件挂在节奏行里，宽 60 */
            Field dotsF = OverviewPanel.class.getDeclaredField("weekDots");
            dotsF.setAccessible(true);
            JPanel dots = (JPanel) dotsF.get(ov);
            check("圆点组件存在且宽 60", dots != null && dots.getPreferredSize().width == 60,
                    dots == null ? "null" : dots.getPreferredSize().width + "x" + dots.getPreferredSize().height);
            check("圆点和节奏文字同一行（父是 X 轴 BoxLayout）",
                    dots != null && dots.getParent() != null
                            && dots.getParent().getLayout() instanceof BoxLayout
                            && ((BoxLayout) dots.getParent().getLayout()).getAxis() == BoxLayout.X_AXIS,
                    dots == null ? "null" : dots.getParent().getLayout().getClass().getName());

            /* C. 小窗第四行：本周完成 2 条 · 比上周多 1 条 */
            MiniBoard.show();
            Thread.sleep(600);
            Field instF = MiniBoard.class.getDeclaredField("inst");
            instF.setAccessible(true);
            Object board = instF.get(null);
            check("小窗已创建", board != null, "null?");
            Method bhM = MiniBoard.class.getDeclaredMethod("boardHeight");
            bhM.setAccessible(true);
            Field rowsF = MiniBoard.class.getDeclaredField("paceLbl");
            rowsF.setAccessible(true);
            JLabel pace = (JLabel) rowsF.get(board);
            check("小窗有本周完成那一行", pace != null, "null?");
            check("小窗文案：本周 2 条 · 比上周多 1 条",
                    pace != null && "本周完成 2 条 · 比上周多 1 条".equals(pace.getText()),
                    pace == null ? "null" : pace.getText());
            check("小窗高度加了 16（232 + 4 行 × 20）",
                    ((Integer) bhM.invoke(null)) == 312, "h=" + bhM.invoke(null));

            /* D. 清单底部"退回选中的"按钮：Ctrl 点一行后可用，点了真退回 */
            Method dlM = OverviewPanel.class.getDeclaredMethod("doneListPanel",
                    String.class, String.class, List.class, JDialog.class);
            dlM.setAccessible(true);
            Method subM = OverviewPanel.class.getDeclaredMethod("doneSubTitle");
            subM.setAccessible(true);
            String sub = (String) subM.invoke(ov);
            List month = rowsOf(ov, "month");
            final JPanel p1 = (JPanel) dlM.invoke(ov, "本月完成 · 共 3 条", sub, month, null);
            p1.setSize(520, 244);
            layoutAll(p1);
            final JButton pickBtn = findButton(p1, "退回选中的");
            check("底部有『退回选中的』按钮", pickBtn != null, pickBtn == null ? "null" : pickBtn.getText());
            check("没选中时按钮置灰", pickBtn != null && !pickBtn.isEnabled(),
                    pickBtn == null ? "null" : "enabled=" + pickBtn.isEnabled());
            final java.util.List<JPanel> rows = doneRows(p1);
            check("清单 3 行", rows.size() == 3, "rows=" + rows.size());
            if (rows.size() >= 2 && pickBtn != null) {
                final JPanel target = rows.get(1);       // 11-09 周一做的
                SwingUtilities.invokeAndWait(() -> {
                    MouseEvent me = new MouseEvent(target, MouseEvent.MOUSE_CLICKED,
                            System.currentTimeMillis(), InputEvent.CTRL_DOWN_MASK,
                            4, 4, 1, false, MouseEvent.BUTTON1);
                    target.dispatchEvent(me);
                });
                check("Ctrl 点后按钮可用", pickBtn.isEnabled(), "enabled=" + pickBtn.isEnabled());
                check("按钮文案带上条数", "退回选中的 1 条".equals(pickBtn.getText()), pickBtn.getText());
                check("勾选行换成更深的蓝", target.getBackground().getRGB() == new Color(0xBFDBFE).getRGB(),
                        Integer.toHexString(target.getBackground().getRGB()));
                SwingUtilities.invokeAndWait(pickBtn::doClick);
                Thread.sleep(500);
                String file = String.join("\n", Store.readLines("todo.txt"));
                check("批量退回后文件里变 0", file.contains("0\t周一做的\t2026-11-09"),
                        file.replace("\n", " | "));
            }

            /* E. Store 批量退回：两条一起 */
            int n = Store.markTodosUndone(java.util.Arrays.asList("今天已做完", "上周做的"));
            check("批量退回返回 2", n == 2, "n=" + n);
            String file2 = String.join("\n", Store.readLines("todo.txt"));
            check("今天已做完变 0", file2.contains("0\t今天已做完\t2026-11-11"), "ok");
            check("上周做的变 0", file2.contains("0\t上周做的\t2026-11-04"), "ok");
            check("已归档的不会被误改", file2.contains("1\t二十天前（已归档）\t2026-10-22"), "ok");
            check("传空列表返回 0", Store.markTodosUndone(new java.util.ArrayList<String>()) == 0, "ok");

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
