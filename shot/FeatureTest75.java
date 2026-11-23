import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** v2.3：统计页番茄三张卡点击联动番茄记录 */
public class FeatureTest75 {

    static int pass = 0, fail = 0;

    public static void main(String[] a) throws Exception {
        File tmp = new File(System.getProperty("java.io.tmpdir"), "toolbox-ft75");
        wipe(tmp);
        tmp.mkdirs();
        System.setProperty("toolbox.data.dir", tmp.getAbsolutePath());

        File real = new File(System.getProperty("user.home"), "ToolboxPC");
        String before = snapshot(real);

        Store.writeLines("settings.txt", new ArrayList<String>(Arrays.asList("sound=0")));
        Store.writeLines("pomodoro_log.txt", new ArrayList<String>(Arrays.asList(
                "2026-11-19\t2026-11-19 09:30\t25\t专注\t写方案",
                "2026-11-19\t2026-11-19 10:10\t25\t专注\t写方案")));

        SwingUtilities.invokeAndWait(() -> Main.main(new String[0]));
        Thread.sleep(2200);

        StatsPanel sp = Main.page(StatsPanel.class);
        check("统计页能拿到", sp != null);

        List<Component> links = new ArrayList<Component>();
        collect(sp, links);
        int cards = 0;
        for (Component c : links) {
            if (!(c instanceof JPanel)) continue;
            String tip = ((JPanel) c).getToolTipText();
            if (tip != null && tip.startsWith("点一下：")) {
                cards++;
                check("卡片有手型光标（" + tip + "）",
                        ((JPanel) c).getCursor().getType() == Cursor.HAND_CURSOR);
                check("卡片挂了点击监听", ((JPanel) c).getMouseListeners().length > 0);
            }
        }
        check("统计页有 3 张番茄卡可点（" + cards + "）", cards == 3);

        PomodoroPanel pp = Main.page(PomodoroPanel.class);
        check("番茄钟页能拿到", pp != null);

        // 点「完成番茄总数」的等价动作：切页 + 打开本周视图
        SwingUtilities.invokeAndWait(() -> {
            Main.go("pomodoro");
            pp.openHistory("week");
        });
        Thread.sleep(600);
        JDialog dlg = (JDialog) field(pp, "historyDlg");
        check("番茄记录弹窗打开了", dlg != null && dlg.isShowing());
        JRadioButton week = findByText(dlg == null ? null : dlg.getContentPane(), "本周");
        check("打开时停在「本周」视图", week != null && week.isSelected());
        JRadioButton today = findByText(dlg == null ? null : dlg.getContentPane(), "今天");
        check("「今天」没有被同时选中", today != null && !today.isSelected());

        // 再点一次不会叠窗
        JDialog first = dlg;
        SwingUtilities.invokeAndWait(() -> pp.openHistory("today"));
        Thread.sleep(300);
        check("重复打开是同一个弹窗（不叠窗）", field(pp, "historyDlg") == first);

        if (dlg != null) SwingUtilities.invokeAndWait(dlg::dispose);
        Thread.sleep(300);
        check("关掉后引用清空", field(pp, "historyDlg") == null);

        String after = snapshot(real);
        check("用户真实目录未被改动", before.equals(after));

        System.out.println(fail == 0 ? ("ALL PASS (" + pass + ")") : ("SOME FAIL " + fail + "/" + (pass + fail)));
        Runtime.getRuntime().halt(fail == 0 ? 0 : 1);
    }

    static Object field(Object o, String name) throws Exception {
        Field f = o.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(o);
    }

    static void collect(Container c, List<Component> out) {
        for (Component x : c.getComponents()) {
            out.add(x);
            if (x instanceof Container) collect((Container) x, out);
        }
    }

    static JRadioButton findByText(Container c, String text) {
        if (c == null) return null;
        for (Component x : c.getComponents()) {
            if (x instanceof JRadioButton && text.equals(((JRadioButton) x).getText())) return (JRadioButton) x;
            if (x instanceof Container) {
                JRadioButton r = findByText((Container) x, text);
                if (r != null) return r;
            }
        }
        return null;
    }

    static String snapshot(File d) {
        if (!d.exists()) return "absent";
        StringBuilder sb = new StringBuilder();
        File[] fs = d.listFiles();
        if (fs != null) for (File f : fs) {
            sb.append(f.getName()).append('=').append(f.length()).append('@')
                    .append(f.lastModified() / 1000).append(';');
        }
        return sb.toString();
    }

    static void wipe(File d) {
        if (!d.exists()) return;
        File[] fs = d.listFiles();
        if (fs != null) for (File f : fs) {
            if (f.isDirectory()) wipe(f);
            f.delete();
        }
    }

    static void check(String name, boolean ok) {
        System.out.println((ok ? "[PASS] " : "[FAIL] ") + name);
        if (ok) pass++;
        else fail++;
    }
}
