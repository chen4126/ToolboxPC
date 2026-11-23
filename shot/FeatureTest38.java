import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** v1.58 验证：清单弹窗加"复制这几条"、键盘流、概览"本月3"也弹清单 */
public class FeatureTest38 {

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

            /* A. 复制文本格式：MM-dd 正文，一行一条 */
            Method cdM = OverviewPanel.class.getDeclaredMethod("copyDoneText", List.class);
            cdM.setAccessible(true);
            List week = rowsOf(ov, "week");
            String txt = (String) cdM.invoke(ov, week);
            check("复制文本按『MM-dd 正文』两行", txt.equals("11-11 今天已做完\n11-09 周一做的"), txt.replace("\n", " | "));
            check("上周那批复制成一行",
                    String.valueOf(cdM.invoke(ov, rowsOf(ov, "last"))).equals("11-04 上周做的"),
                    String.valueOf(cdM.invoke(ov, rowsOf(ov, "last"))));
            check("本月那批三行", String.valueOf(cdM.invoke(ov, rowsOf(ov, "month"))).split("\n").length == 3,
                    String.valueOf(cdM.invoke(ov, rowsOf(ov, "month"))));

            /* B. 清单面板底部有『复制这几条』按钮，点了真进剪贴板 */
            Method dlM = OverviewPanel.class.getDeclaredMethod("doneListPanel",
                    String.class, List.class, JDialog.class);
            dlM.setAccessible(true);
            JPanel panel = (JPanel) dlM.invoke(ov, "本周完成 · 共 2 条", week, null);
            panel.setSize(520, 178);
            layoutAll(panel);
            JButton copyBtn = null;
            for (Component c : all(panel)) {
                if (c instanceof JButton && ((JButton) c).getText().contains("复制")) copyBtn = (JButton) c;
            }
            check("清单面板有『复制这几条』按钮", copyBtn != null, copyBtn == null ? "none" : copyBtn.getText());
            if (copyBtn != null) {
                final JButton cb = copyBtn;
                final java.util.concurrent.atomic.AtomicReference<String> got =
                        new java.util.concurrent.atomic.AtomicReference<String>("");
                SwingUtilities.invokeAndWait(() -> {
                    cb.doClick();
                    try {
                        got.set((String) Toolkit.getDefaultToolkit().getSystemClipboard()
                                .getData(DataFlavor.stringFlavor));
                    } catch (Exception e) {
                        got.set("ERR " + e.getMessage());
                    }
                });
                Thread.sleep(1500);
                check("点复制后剪贴板内容正确", got.get().equals("11-11 今天已做完\n11-09 周一做的"),
                        got.get().replace("\n", " | "));
                check("复制后按钮文案回退到『复制这几条』", cb.getText().equals("复制这几条"), cb.getText());
            }

            /* C. 键盘流：默认选中第一行，↓ 到第二行，↑ 回第一行 */
            JPanel kb = (JPanel) dlM.invoke(ov, "本周完成 · 共 2 条", week, null);
            kb.setSize(520, 178);
            layoutAll(kb);
            java.util.List<JPanel> rows = doneRows(kb);
            check("清单里识别出 2 行", rows.size() == 2, "rows=" + rows.size());
            check("默认选中第一行（浅蓝底）", isSel(rows, 0), colorOf(rows.get(0)));

            kb.getActionMap().get("doneDown").actionPerformed(
                    new java.awt.event.ActionEvent(kb, 0, "down"));
            check("按 ↓ 选中第二行", isSel(rows, 1), colorOf(rows.get(0)) + " " + colorOf(rows.get(1)));
            kb.getActionMap().get("doneUp").actionPerformed(
                    new java.awt.event.ActionEvent(kb, 0, "up"));
            check("按 ↑ 回到第一行", isSel(rows, 0), colorOf(rows.get(0)) + " " + colorOf(rows.get(1)));
            kb.getActionMap().get("doneUp").actionPerformed(
                    new java.awt.event.ActionEvent(kb, 0, "up"));
            check("已在第一行时再按 ↑ 不越界", isSel(rows, 0), colorOf(rows.get(0)));

            /* D. 绑定键位存在：UP/DOWN/ENTER/ESCAPE 四个 action */
            String keys = kb.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).allKeys().length + "";
            boolean hasUp = false, hasDown = false, hasEnter = false, hasEsc = false;
            for (KeyStroke ks : kb.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).allKeys()) {
                String s = String.valueOf(ks);
                if (s.contains("UP") || s.contains("up")) hasUp = true;
                if (s.contains("DOWN") || s.contains("down")) hasDown = true;
                if (s.contains("ENTER") || s.contains("enter")) hasEnter = true;
                if (s.contains("ESCAPE") || s.contains("escape")) hasEsc = true;
            }
            check("四个键都绑上了（↑↓ 回车 Esc）", hasUp && hasDown && hasEnter && hasEsc,
                    "keys=" + keys + " up=" + hasUp + " down=" + hasDown + " enter=" + hasEnter + " esc=" + hasEsc);

            /* E. 回车跳行：选中第二行（11-09）回车后主窗视图落在 11-09 */
            kb.getActionMap().get("doneDown").actionPerformed(new java.awt.event.ActionEvent(kb, 0, "down"));
            kb.getActionMap().get("doneOpen").actionPerformed(new java.awt.event.ActionEvent(kb, 0, "open"));
            Thread.sleep(600);
            Field vdF = OverviewPanel.class.getDeclaredField("viewDate");
            vdF.setAccessible(true);
            check("回车跳到选中那行（11-09）", String.valueOf(vdF.get(ov)).contains("11-09"), String.valueOf(vdF.get(ov)));

            /* F. 概览三段都弹清单：点『·本月3』→ 弹本月清单（不跳统计页） */
            vdF.set(ov, null);
            refresh.invoke(ov);
            Thread.sleep(300);
            Field wmF = OverviewPanel.class.getDeclaredField("wMonth");
            wmF.setAccessible(true);
            final JLabel wm = (JLabel) wmF.get(ov);
            SwingUtilities.invokeAndWait(() -> {
                java.awt.event.MouseEvent e = new java.awt.event.MouseEvent(wm,
                        java.awt.event.MouseEvent.MOUSE_CLICKED, 0,
                        java.awt.event.MouseEvent.BUTTON1_DOWN_MASK, 1, 1, 1, false,
                        java.awt.event.MouseEvent.BUTTON1);
                wm.dispatchEvent(e);
            });
            Thread.sleep(800);
            int dl = countWindows("本月完成");
            check("点『·本月3』弹出本月完成清单", dl >= 1, "dialogs=" + dl);
            Field ckF = mc.getDeclaredField("currentKey");
            ckF.setAccessible(true);
            check("点『·本月3』后仍停在概览（没跳统计页）",
                    "overview".equals(String.valueOf(ckF.get(main))), String.valueOf(ckF.get(main)));
            closeWindows("本月完成");

            /* G. 三段 tooltip 都指向"列出这几条" */
            Field wwF = OverviewPanel.class.getDeclaredField("wWeek");
            wwF.setAccessible(true);
            Field wlF = OverviewPanel.class.getDeclaredField("wLast");
            wlF.setAccessible(true);
            String t1 = ((JLabel) wwF.get(ov)).getToolTipText();
            String t2 = ((JLabel) wlF.get(ov)).getToolTipText();
            String t3 = wm.getToolTipText();
            check("三段提示都是『列出这几条』",
                    t1.contains("列出这几条") && t2.contains("列出这几条") && t3.contains("列出这几条"),
                    t1 + " / " + t2 + " / " + t3);

            System.out.println(ok ? "ALL PASS" : "SOME FAIL");
        } catch (Throwable t) {
            t.printStackTrace();
            ok = false;
        }
        Runtime.getRuntime().halt(ok ? 0 : 1);
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

    static String colorOf(JPanel r) {
        return String.format("#%06X", r.getBackground().getRGB() & 0xFFFFFF);
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

    static java.util.List<Component> all(Container p) {
        java.util.List<Component> out = new java.util.ArrayList<Component>();
        for (Component c : p.getComponents()) {
            out.add(c);
            if (c instanceof Container) out.addAll(all((Container) c));
        }
        return out;
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
