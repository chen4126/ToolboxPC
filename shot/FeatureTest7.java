import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** v1.27 三项验证：待办复制/设为今天 / 记账右键删单条 / 并行超时托盘提醒 */
public class FeatureTest7 {

    private static boolean ok = true;

    public static void main(String[] args) throws Exception {
        try {
            Theme.init();
            SwingUtilities.invokeAndWait(() -> Main.main(new String[0]));
            Thread.sleep(2500);

            /* ---------- 1a. 待办右键“复制到剪贴板” ---------- */
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
            Field tbF = OverviewPanel.class.getDeclaredField("todoBox");
            tbF.setAccessible(true);
            JPanel todoBox = (JPanel) tbF.get(ov);
            JPanel row0 = (JPanel) todoBox.getComponent(0);
            JCheckBox cb = (JCheckBox) row0.getComponent(0);
            String text = cb.getText();
            JPopupMenu menu = cb.getComponentPopupMenu();
            System.out.println("待办=" + text + " 菜单项=" + menu.getSubElements().length);
            JMenuItem copyItem = findItem(menu, "复制到剪贴板");
            SwingUtilities.invokeAndWait(() -> copyItem.doClick());
            Thread.sleep(500);
            java.awt.datatransfer.Clipboard clip =
                    Toolkit.getDefaultToolkit().getSystemClipboard();
            String clipText = (String) clip.getData(java.awt.datatransfer.DataFlavor.stringFlavor);
            System.out.println("剪贴板=" + clipText);
            check("待办右键复制到剪贴板", text.equals(clipText));

            /* ---------- 1b. 待办右键“设为今天” ---------- */
            // 先把一条待办的日期改成 “-”（不限日期），再点“设为今天”
            Field todosF = OverviewPanel.class.getDeclaredField("todos");
            todosF.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<Object> todos = (List<Object>) todosF.get(ov);
            Object target = null;
            for (Object t : todos) {
                Field tf = t.getClass().getDeclaredField("text");
                tf.setAccessible(true);
                if (text.equals(tf.get(t))) target = t;
            }
            if (target != null) {
                Field df = target.getClass().getDeclaredField("date");
                df.setAccessible(true);
                df.set(target, "-");
                Method sv = OverviewPanel.class.getDeclaredMethod("saveTodos");
                sv.setAccessible(true);
                sv.invoke(ov);
            }
            SwingUtilities.invokeAndWait(() -> {
                try {
                    rf.invoke(ov);
                } catch (Exception ignored) {
                }
            });
            Thread.sleep(400);
            row0 = (JPanel) todoBox.getComponent(0);
            cb = (JCheckBox) row0.getComponent(0);
            JMenuItem todayItem = findItem(cb.getComponentPopupMenu(), "设为今天");
            SwingUtilities.invokeAndWait(() -> todayItem.doClick());
            Thread.sleep(500);
            String today = new java.text.SimpleDateFormat("yyyy-MM-dd",
                    java.util.Locale.CHINA).format(new java.util.Date());
            String saved = "";
            for (String l : Store.readLines("todo.txt")) {
                if (l.contains(text)) saved = l;
            }
            System.out.println("设为今天后这一行=" + saved + " 期望日期=" + today);
            check("待办右键设为今天并写盘", saved.contains(today));

            /* ---------- 2. 记账右键删除单条 ---------- */
            LedgerPanel lp = page(LedgerPanel.class);
            Field lfF = LedgerPanel.class.getDeclaredField("lines");
            lfF.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<String> lines = (List<String>) lfF.get(lp);
            int before = lines.size();
            int beforeFile = Store.readLines("ledger.txt").size();
            Method rl = LedgerPanel.class.getDeclaredMethod("removeLine", int.class);
            rl.setAccessible(true);
            boolean removed = (Boolean) rl.invoke(lp, 0);
            int afterFile = Store.readLines("ledger.txt").size();
            System.out.println("删除前=" + before + "/文件" + beforeFile
                    + " 删除后=" + lines.size() + "/文件" + afterFile);
            check("删单条后内存与文件同步", removed && lines.size() == before - 1
                    && afterFile == beforeFile - 1 && before > 0);

            // 右键菜单存在且能取到“删除这条记录”
            Field tf2 = LedgerPanel.class.getDeclaredField("table");
            tf2.setAccessible(true);
            JTable table = (JTable) tf2.get(lp);
            JPopupMenu rowMenu = table.getComponentPopupMenu();
            boolean hasDel = rowMenu != null && findItem(rowMenu, "删除这条记录") != null;
            check("记账表格有右键删除菜单", hasDel);

            /* ---------- 3. 并行超时满 1 分的托盘提醒 ---------- */
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
            setField(taskCls, task, "over", 59900L);
            setField(taskCls, task, "overNotified", false);
            tasks.clear();
            tasks.add(task);

            Field lastPreF = Alerts.class.getDeclaredField("lastPre");
            lastPreF.setAccessible(true);
            lastPreF.set(null, "");
            Method tick = CountDownPanel.class.getDeclaredMethod("tick");
            tick.setAccessible(true);
            SwingUtilities.invokeAndWait(() -> {
                try {
                    tick.invoke(cd);
                } catch (Exception ignored) {
                }
            });
            Thread.sleep(500);
            Field onF = taskCls.getDeclaredField("overNotified");
            onF.setAccessible(true);
            String pre = (String) lastPreF.get(null);
            System.out.println("超时提醒=" + pre);
            check("并行超时满 1 分弹一次托盘提醒",
                    (Boolean) onF.get(task)
                            && "倒计时 · 已超时｜泡面 已超时 1 分".equals(pre));

            // 再 tick 一次不应重复提醒
            lastPreF.set(null, "");
            SwingUtilities.invokeAndWait(() -> {
                try {
                    tick.invoke(cd);
                } catch (Exception ignored) {
                }
            });
            Thread.sleep(400);
            String pre2 = (String) lastPreF.get(null);
            System.out.println("第二次 tick 的提醒=" + ("".equals(pre2) ? "（无）" : pre2));
            check("同一任务不重复提醒", "".equals(pre2));
            tasks.clear();

            System.out.println(ok ? "ALL PASS" : "HAS FAIL");
            System.exit(ok ? 0 : 1);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(2);
        }
    }

    private static JMenuItem findItem(JPopupMenu menu, String text) {
        for (Component c : menu.getComponents()) {
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
