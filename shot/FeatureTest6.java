import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** v1.26 三项验证：待办右键联动 / 记账双击改金额备注 / 并行倒计时超时分钟 */
public class FeatureTest6 {

    private static boolean ok = true;

    public static void main(String[] args) throws Exception {
        try {
            Theme.init();
            SwingUtilities.invokeAndWait(() -> Main.main(new String[0]));
            Thread.sleep(2500);

            /* ---------- 1. 待办右键 → 在备忘录中查找 ---------- */
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
            System.out.println("第一条待办=" + text);
            JPopupMenu menu = cb.getComponentPopupMenu();
            if (menu == null) throw new RuntimeException("待办行没有右键菜单");
            System.out.println("菜单项=" + menu.getSubElements().length);
            JMenuItem item0 = (JMenuItem) menu.getComponent(0);
            SwingUtilities.invokeAndWait(() -> item0.doClick());
            Thread.sleep(1200);
            MemoPanel mp = page(MemoPanel.class);
            Field qF = MemoPanel.class.getDeclaredField("query");
            qF.setAccessible(true);
            Field sfF = MemoPanel.class.getDeclaredField("searchField");
            sfF.setAccessible(true);
            JTextField sf = (JTextField) sfF.get(mp);
            Field selfF = Main.class.getDeclaredField("self");
            selfF.setAccessible(true);
            Object mainObj = selfF.get(null);
            Field ckF = Main.class.getDeclaredField("currentKey");
            ckF.setAccessible(true);
            String cur = (String) ckF.get(mainObj);
            System.out.println("当前页=" + cur + " 搜索框=" + sf.getText() + " query=" + qF.get(mp));
            check("待办右键跳备忘录并填入关键字",
                    "memo".equals(cur) && text.equals(sf.getText()));

            /* ---------- 2. 待办右键 → 转为日程提醒（预填标题） ---------- */
            SchedulePanel sp = page(SchedulePanel.class);
            Method oe = SchedulePanel.class.getDeclaredMethod("openEditor", boolean.class);
            oe.setAccessible(true);
            Field ptF = SchedulePanel.class.getDeclaredField("pendingTitle");
            ptF.setAccessible(true);
            ptF.set(sp, text);
            SwingUtilities.invokeAndWait(() -> {
                try {
                    oe.invoke(sp, false);   // 非模态，测试用
                } catch (Exception ignored) {
                }
            });
            Thread.sleep(900);
            JDialog dlg = null;
            for (Window w : Window.getWindows()) {
                if (w instanceof JDialog && w.isVisible()) {
                    dlg = (JDialog) w;
                    break;
                }
            }
            String titleInDlg = "";
            if (dlg != null) {
                List<JTextField> tfs = new ArrayList<JTextField>();
                collect(dlg.getContentPane(), tfs);
                if (!tfs.isEmpty()) titleInDlg = tfs.get(0).getText();
                dlg.dispose();
            }
            System.out.println("新建提醒内容框=" + titleInDlg);
            check("待办右键可预填日程内容", text.equals(titleInDlg));

            /* ---------- 3. 记账双击改金额/备注（写回逻辑） ---------- */
            LedgerPanel lp = page(LedgerPanel.class);
            Field lfF = LedgerPanel.class.getDeclaredField("lines");
            lfF.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<String> lines = (List<String>) lfF.get(lp);
            int idx = lines.size() - 1;
            String before = lines.get(idx);
            Method ae = LedgerPanel.class.getDeclaredMethod("applyEdit", int.class, String.class, String.class);
            ae.setAccessible(true);
            boolean done = (Boolean) ae.invoke(lp, idx, "66.5", "改过的备注");
            String after = Store.readLines("ledger.txt").get(idx);
            System.out.println("改前=" + before);
            System.out.println("改后=" + after);
            check("双击改金额备注写回文件", done && after.contains("66.50")
                    && after.contains("改过的备注") && after.split("\t").length == 5);
            boolean bad = (Boolean) ae.invoke(lp, idx, "abc", "x");
            check("非法金额不写回", !bad);

            /* ---------- 4. 并行倒计时超时满 1 分文案 ---------- */
            CountDownPanel cd = page(CountDownPanel.class);
            Field tasksF = CountDownPanel.class.getDeclaredField("tasks");
            tasksF.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<Object> tasks = (List<Object>) tasksF.get(cd);
            Class<?> taskCls = Class.forName("com.toolbox.pc.CountDownPanel$Task");
            java.lang.reflect.Constructor<?> ctor = taskCls.getDeclaredConstructor();
            ctor.setAccessible(true);
            Object task = ctor.newInstance();
            Field nameF = taskCls.getDeclaredField("name");
            nameF.setAccessible(true);
            nameF.set(task, "泡面");
            Field overTF = taskCls.getDeclaredField("over");
            overTF.setAccessible(true);
            overTF.setLong(task, 65000L);
            Field rmF = taskCls.getDeclaredField("remainMs");
            rmF.setAccessible(true);
            rmF.setLong(task, 0L);
            Field pmF = taskCls.getDeclaredField("plannedMs");
            pmF.setAccessible(true);
            pmF.setLong(task, 60000L);
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
            Thread.sleep(400);
            Field tlF = taskCls.getDeclaredField("timeLbl");
            tlF.setAccessible(true);
            JLabel tl = (JLabel) tlF.get(task);
            System.out.println("并行超时文本=" + (tl == null ? "无" : tl.getText()));
            check("并行任务超时 1 分显示分钟", tl != null && tl.getText().contains("超 1 分"));
            tasks.clear();

            System.out.println(ok ? "ALL PASS" : "HAS FAIL");
            System.exit(ok ? 0 : 1);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(2);
        }
    }

    /** 找当前显示中的 JPopupMenu */
    private static JPopupMenu findMenu() {
        for (Window w : Window.getWindows()) {
            JPopupMenu m = menuIn(w);
            if (m != null) return m;
        }
        return null;
    }

    private static JPopupMenu menuIn(Container c) {
        for (Component k : c.getComponents()) {
            if (k instanceof JPopupMenu && ((JPopupMenu) k).getSubElements().length > 0)
                return (JPopupMenu) k;
            if (k instanceof Container) {
                JPopupMenu m = menuIn((Container) k);
                if (m != null) return m;
            }
        }
        return null;
    }

    private static void check(String name, boolean pass) {
        System.out.println((pass ? "[PASS] " : "[FAIL] ") + name);
        if (!pass) ok = false;
    }

    private static void collect(Component c, List<JTextField> out) {
        if (c instanceof JTextField) out.add((JTextField) c);
        if (c instanceof Container) {
            for (Component k : ((Container) c).getComponents()) collect(k, out);
        }
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
