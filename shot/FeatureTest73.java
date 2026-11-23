import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.Container;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** v2.3：数据自动快照 / 回滚 */
public class FeatureTest73 {

    static int pass = 0, fail = 0;
    static File tmp;

    public static void main(String[] a) throws Exception {
        tmp = new File(System.getProperty("java.io.tmpdir"), "toolbox-ft73");
        wipe(tmp);
        tmp.mkdirs();
        System.setProperty("toolbox.data.dir", tmp.getAbsolutePath());

        File real = new File(System.getProperty("user.home"), "ToolboxPC");
        String before = snapshot(real);

        Store.writeLines("settings.txt", new ArrayList<String>(Arrays.asList("sound=1", "pin=0")));
        Store.writeLines("todo.txt", new ArrayList<String>(Arrays.asList("0\t买菜\t2026-11-19")));
        Store.writeLines("ledger.txt", new ArrayList<String>(Arrays.asList("0\t25.00\t午饭\t11-19 12:30\t餐饮")));

        // ---------- 1) 每天一份 ----------
        boolean first = Backup.daily();
        check("首次调用留了一份当日快照", first);
        File dayDir = new File(Backup.root(), java.time.LocalDate.now().toString());
        check("快照目录按日期命名（" + dayDir.getName() + "）", dayDir.exists());
        int files = dayDir.listFiles() == null ? 0 : dayDir.listFiles().length;
        check("快照里有 3 个数据文件（" + files + "）", files == 3);
        check("同一天再调不会重复写", !Backup.daily());
        check("hasToday 为真", Backup.hasToday());
        check("列表里能查到今天", Backup.list().contains(java.time.LocalDate.now().toString()));
        check("描述带文件数与大小（" + Backup.describe(java.time.LocalDate.now().toString()) + "）",
                Backup.describe(java.time.LocalDate.now().toString()).contains("3 个文件"));

        // ---------- 2) 手动备份 ----------
        String manual = Backup.manual();
        check("手动备份返回了名字", manual != null && !manual.isEmpty());
        check("手动备份目录里有 3 个文件",
                new File(Backup.root(), manual).listFiles().length == 3);
        check("手动备份后列表至少 2 份", Backup.list().size() >= 2);

        // ---------- 3) 回滚 ----------
        Store.writeLines("settings.txt", new ArrayList<String>(Arrays.asList("sound=0", "pin=1")));
        Store.writeLines("todo.txt", new ArrayList<String>());
        int n = Backup.restore(java.time.LocalDate.now().toString());
        check("回滚恢复了文件（" + n + " 个）", n >= 3);
        check("settings 回到快照里的 sound=1", Store.flag("sound", false));
        check("todo 也回来了", Store.readLines("todo.txt").size() == 1);
        check("回滚前自动留了一份 pre-restore",
                Backup.list().toString().contains("pre-restore") || Backup.list().size() > 3);
        check("回滚不存在的快照返回 -1", Backup.restore("1999-01-01") == -1);

        // ---------- 4) 只留最近 10 份 ----------
        for (int i = 1; i <= 12; i++) {
            File d = new File(Backup.root(), String.format(java.util.Locale.CHINA, "2026-01-%02d", i));
            d.mkdirs();
            Files.write(new File(d, "x.txt").toPath(), "x".getBytes("UTF-8"));
        }
        Backup.prune();
        check("超过 10 份会被裁掉（现在 " + Backup.list().size() + "）", Backup.list().size() == 10);

        // ---------- 5) 真机：设置页读得到、reloadAll 不出事 ----------
        SwingUtilities.invokeAndWait(() -> Main.main(new String[0]));
        Thread.sleep(2000);
        SettingsPanel sp = Main.page(SettingsPanel.class);
        check("设置页能打开", sp != null);
        String txt = SettingsPanel.backupStatusText();
        check("设置页备份状态文字（" + txt + "）", txt.contains("份") && txt.contains("自动备份"));
        check("「最近一份」显示的是日期快照，不是 pre-restore",
                !Backup.list().isEmpty() && !Backup.list().get(0).startsWith("pre-restore"));
        try {
            Main.reloadAll();
            check("reloadAll 正常执行", true);
        } catch (Throwable t) {
            check("reloadAll 抛异常：" + t, false);
        }

        // ---------- 5.5) 备份管理弹窗结构 ----------
        java.lang.reflect.Method mk = SettingsPanel.class.getDeclaredMethod("buildBackupDialog");
        mk.setAccessible(true);
        final JDialog[] holder = new JDialog[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                holder[0] = (JDialog) mk.invoke(sp);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        JDialog dlg = holder[0];
        check("备份管理弹窗能建出来", dlg != null);
        if (dlg != null) {
            List<String> btns = buttonTexts(dlg.getContentPane());
            check("弹窗里有「立即备份一份」（" + btns + "）", btns.contains("立即备份一份"));
            check("弹窗里有「回滚到选中」", btns.contains("回滚到选中"));
            check("弹窗里有「打开备份目录」", btns.contains("打开备份目录"));
            check("弹窗里有「关闭」", btns.contains("关闭"));
            check("弹窗里有快照列表", findList(dlg.getContentPane()) != null);
            JList<?> l = findList(dlg.getContentPane());
            check("列表里列出了快照", l != null && l.getModel().getSize() == Backup.list().size());
            SwingUtilities.invokeAndWait(dlg::dispose);
        }

        // ---------- 6) 用户真实目录没被碰 ----------
        String after = snapshot(real);
        check("用户真实目录未被改动", before.equals(after));

        System.out.println(fail == 0 ? ("ALL PASS (" + pass + ")") : ("SOME FAIL " + fail + "/" + (pass + fail)));
        Runtime.getRuntime().halt(fail == 0 ? 0 : 1);
    }

    static List<String> buttonTexts(Container c) {
        List<String> out = new ArrayList<String>();
        for (java.awt.Component x : c.getComponents()) {
            if (x instanceof JButton) {
                String t = ((JButton) x).getText();
                if (t != null && !t.isEmpty()) out.add(t);
            }
            if (x instanceof Container) out.addAll(buttonTexts((Container) x));
        }
        return out;
    }

    static JList<?> findList(Container c) {
        for (java.awt.Component x : c.getComponents()) {
            if (x instanceof JList) return (JList<?>) x;
            if (x instanceof Container) {
                JList<?> l = findList((Container) x);
                if (l != null) return l;
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
