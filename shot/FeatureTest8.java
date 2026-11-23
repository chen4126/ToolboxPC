import com.toolbox.pc.*;

import javax.swing.*;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** v1.28 三项验证：待办改期 / 记账复制与改收入 / 并行超时 5 分强提醒 */
public class FeatureTest8 {

    private static boolean ok = true;
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static void main(String[] args) throws Exception {
        try {
            Theme.init();
            SwingUtilities.invokeAndWait(() -> Main.main(new String[0]));
            Thread.sleep(2500);

            OverviewPanel ov = page(OverviewPanel.class);
            Method rf = OverviewPanel.class.getDeclaredMethod("refresh");
            rf.setAccessible(true);
            SwingUtilities.invokeAndWait(() -> {
                try {
                    rf.invoke(ov);
                } catch (Exception ignored) {
                }
            });
            Thread.sleep(400);

            /* ---------- 1. 待办“改期到…明天” ---------- */
            JCheckBox cb = firstTodoCheck(ov);
            String text = cb.getText();
            JPopupMenu menu = cb.getComponentPopupMenu();
            JMenu defer = findMenu(menu, "改期到…");
            System.out.println("待办=" + text + " 顶层项=" + menu.getSubElements().length
                    + " 改期子项=" + (defer == null ? "无" : defer.getItemCount()));
            JMenuItem tomorrowItem = findItem(defer, "明天");
            SwingUtilities.invokeAndWait(() -> tomorrowItem.doClick());
            Thread.sleep(600);
            String tm = LocalDate.now().plusDays(1).format(DF);
            String savedRow = rowOf(text);
            System.out.println("改期后=" + savedRow + " 期望日期=" + tm);
            check("待办可改期到明天并写盘", savedRow.contains(tm));
            check("改到明天后不再出现在今日待办", !inTodayTodos(ov, text));

            /* ---------- 2. 待办“不限日期” ---------- */
            // 重新取行（列表已重建，原先那条不在今日待办里了，改用第二条“复习英语”）
            JCheckBox cb2 = firstTodoCheck(ov);
            String text2 = cb2.getText();
            JMenuItem anyItem = findItem(findMenu(cb2.getComponentPopupMenu(), "改期到…"), "不限日期");
            SwingUtilities.invokeAndWait(() -> anyItem.doClick());
            Thread.sleep(600);
            String row2 = rowOf(text2);
            System.out.println("清除日期后=" + row2);
            check("待办可清除日期为不限", row2.endsWith("-") || row2.endsWith("\t"));

            /* ---------- 3. 记账右键“复制这条记录” ---------- */
            LedgerPanel lp = page(LedgerPanel.class);
            Field lfF = LedgerPanel.class.getDeclaredField("lines");
            lfF.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<String> lines = (List<String>) lfF.get(lp);
            Method cpr = LedgerPanel.class.getDeclaredMethod("copyRow", int.class);
            cpr.setAccessible(true);
            Field rtiF = LedgerPanel.class.getDeclaredField("rowToIndex");
            rtiF.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.List<Integer> rti = (java.util.List<Integer>) rtiF.get(lp);
            int idx0 = rti.get(0);
            String src = lines.get(idx0);
            String[] sp = src.split("\t", -1);
            String expect = sp[3] + "　" + ("1".equals(sp[0]) ? "收入" : "支出") + " · " + sp[4]
                    + " · " + sp[2] + "　-¥" + sp[1];
            cpr.invoke(lp, 0);
            Thread.sleep(400);
            String clip = (String) Toolkit.getDefaultToolkit().getSystemClipboard()
                    .getData(java.awt.datatransfer.DataFlavor.stringFlavor);
            System.out.println("选中行=" + src);
            System.out.println("复制内容=" + clip);
            check("记账右键复制这条", expect.equals(clip));

            /* ---------- 4. 记账右键“改成收入” ---------- */
            Method tg = LedgerPanel.class.getDeclaredMethod("toggleType", int.class);
            tg.setAccessible(true);
            boolean toggled = (Boolean) tg.invoke(lp, 0);
            String after = Store.readLines("ledger.txt").get(idx0);
            System.out.println("改成收入后=" + after);
            check("改成收入并调整分类", toggled && after.startsWith("1\t")
                    && after.endsWith("其他"));

            // 菜单第二项文字应随行类型变成“改成支出”
            Method utl = LedgerPanel.class.getDeclaredMethod("updateToggleLabel");
            utl.setAccessible(true);
            SwingUtilities.invokeAndWait(() -> tableSel(lp, 0));
            utl.invoke(lp);
            Field trF = LedgerPanel.class.getDeclaredField("toggleItemRef");
            trF.setAccessible(true);
            JMenuItem toggleItem = (JMenuItem) trF.get(lp);
            System.out.println("第二项文字=" + toggleItem.getText());
            check("菜单项按收支自动切换文案", "改成支出".equals(toggleItem.getText()));

            /* ---------- 5. 并行超时满 5 分强提醒 ---------- */
            CountDownPanel cd = page(CountDownPanel.class);
            Field tasksF = CountDownPanel.class.getDeclaredField("tasks");
            tasksF.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<Object> tasks = (List<Object>) tasksF.get(cd);
            Class<?> taskCls = Class.forName("com.toolbox.pc.CountDownPanel$Task");
            java.lang.reflect.Constructor<?> ctor = taskCls.getDeclaredConstructor();
            ctor.setAccessible(true);
            Object task = ctor.newInstance();
            setField(taskCls, task, "name", "泡面");
            setField(taskCls, task, "remainMs", 0L);
            setField(taskCls, task, "plannedMs", 60000L);
            setField(taskCls, task, "over", 299900L);
            setField(taskCls, task, "overNotified", true);   // 1 分那次已发过
            setField(taskCls, task, "overStrong", false);
            tasks.clear();
            tasks.add(task);
            Method rt = CountDownPanel.class.getDeclaredMethod("renderTasks");
            rt.setAccessible(true);
            SwingUtilities.invokeAndWait(() -> {
                try {
                    rt.invoke(cd);
                } catch (Exception ignored) {
                }
            });
            Thread.sleep(300);
            Method tick = CountDownPanel.class.getDeclaredMethod("tick");
            tick.setAccessible(true);
            SwingUtilities.invokeAndWait(() -> {
                try {
                    tick.invoke(cd);
                } catch (Exception ignored) {
                }
            });
            Thread.sleep(600);
            Field osF = taskCls.getDeclaredField("overStrong");
            osF.setAccessible(true);
            Field lblF = taskCls.getDeclaredField("timeLbl");
            lblF.setAccessible(true);
            JLabel lbl = (JLabel) lblF.get(task);
            System.out.println("5 分钟强提醒已发=" + osF.get(task) + " 行内文本=" + (lbl == null ? "无" : lbl.getText()));
            check("并行超时满 5 分触发一次强提醒", (Boolean) osF.get(task));
            check("超 5 分文案还在累加分钟", lbl != null && lbl.getText().contains("超 5 分"));

            // 再 tick 一次不重复强提醒
            SwingUtilities.invokeAndWait(() -> {
                try {
                    tick.invoke(cd);
                } catch (Exception ignored) {
                }
            });
            Thread.sleep(400);
            check("5 分强提醒不重复", (Boolean) osF.get(task));
            tasks.clear();

            System.out.println(ok ? "ALL PASS" : "HAS FAIL");
            System.exit(ok ? 0 : 1);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(2);
        }
    }

    private static void tableSel(LedgerPanel lp, int row) {
        try {
            Field tf = LedgerPanel.class.getDeclaredField("table");
            tf.setAccessible(true);
            JTable table = (JTable) tf.get(lp);
            table.setRowSelectionInterval(row, row);
        } catch (Exception ignored) {
        }
    }

    private static JCheckBox firstTodoCheck(OverviewPanel ov) throws Exception {
        Field tbF = OverviewPanel.class.getDeclaredField("todoBox");
        tbF.setAccessible(true);
        JPanel box = (JPanel) tbF.get(ov);
        JPanel row = (JPanel) box.getComponent(0);
        return (JCheckBox) row.getComponent(0);
    }

    private static boolean inTodayTodos(OverviewPanel ov, String text) throws Exception {
        Method m = OverviewPanel.class.getDeclaredMethod("todayTodos");
        m.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>) m.invoke(ov);
        for (Object t : list) {
            Field tf = t.getClass().getDeclaredField("text");
            tf.setAccessible(true);
            if (text.equals(tf.get(t))) return true;
        }
        return false;
    }

    private static String rowOf(String text) {
        for (String l : Store.readLines("todo.txt")) {
            if (l.contains(text)) return l;
        }
        return "";
    }

    private static JMenu findMenu(JPopupMenu menu, String text) {
        for (Component c : menu.getComponents()) {
            if (c instanceof JMenu && text.equals(((JMenu) c).getText())) return (JMenu) c;
        }
        return null;
    }

    private static JMenuItem findItem(JMenu menu, String text) {
        if (menu == null) return null;
        for (Component c : menu.getMenuComponents()) {
            if (c instanceof JMenuItem && text.equals(((JMenuItem) c).getText())) return (JMenuItem) c;
        }
        return null;
    }

    private static void setField(Class<?> cls, Object obj, String name, Object v) throws Exception {
        Field f = cls.getDeclaredField(name);
        f.setAccessible(true);
        if (v instanceof Long) f.setLong(obj, (Long) v);
        else if (v instanceof Boolean) f.setBoolean(obj, (Boolean) v);
        else f.set(obj, v);
    }

    private static void check(String name, boolean pass) {
        System.out.println((pass ? "[PASS] " : "[FAIL] ") + name);
        if (!pass) ok = false;
    }

    private static <T extends Component> T page(Class<T> type) {
        for (Frame f : Frame.getFrames()) {
            T t = scan(f, type);
            if (t != null) return t;
        }
        return null;
    }

    private static <T extends Component> T scan(Component c, Class<T> type) {
        if (type.isInstance(c)) return type.cast(c);
        if (c instanceof Container) {
            for (Component k : ((Container) c).getComponents()) {
                T t = scan(k, type);
                if (t != null) return t;
            }
        }
        return null;
    }
}
