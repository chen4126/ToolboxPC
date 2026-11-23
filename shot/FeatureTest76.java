import com.toolbox.pc.*;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** v2.4：备份范围按模块勾选 */
public class FeatureTest76 {

    static int pass = 0, fail = 0;
    static File tmp;

    public static void main(String[] a) throws Exception {
        tmp = new File(System.getProperty("java.io.tmpdir"), "toolbox-ft76");
        wipe(tmp);
        tmp.mkdirs();
        System.setProperty("toolbox.data.dir", tmp.getAbsolutePath());

        File real = new File(System.getProperty("user.home"), "ToolboxPC");
        String before = snapshot(real);

        Store.writeLines("settings.txt", new ArrayList<String>(Arrays.asList("sound=1", "pin=0")));

        // ---------- 1) 默认全量 ----------
        check("没设置过时默认 6 组全备份（" + Backup.pick().size() + "）",
                Backup.pick().size() == Backup.GROUPS.length);
        check("默认提示是「全部模块」[" + Backup.pickText() + "]", "全部模块".equals(Backup.pickText()));

        // ---------- 2) 文件属于哪一组 ----------
        check("ledger.txt → ledger", "ledger".equals(Backup.groupOf("ledger.txt")));
        check("ledgermap.txt → ledger（记忆表跟着账本走）", "ledger".equals(Backup.groupOf("ledgermap.txt")));
        check("memo.txt → memo", "memo".equals(Backup.groupOf("memo.txt")));
        check("schedule.txt / todo.txt → plan",
                "plan".equals(Backup.groupOf("schedule.txt")) && "plan".equals(Backup.groupOf("todo.txt")));
        check("pomodoro_log.txt → pomo", "pomo".equals(Backup.groupOf("pomodoro_log.txt")));
        check("countdown_presets.txt → pomo", "pomo".equals(Backup.groupOf("countdown_presets.txt")));
        check("eye.txt → other", "other".equals(Backup.groupOf("eye.txt")));
        check("没登记的文件归 other", "other".equals(Backup.groupOf("奇怪.txt")));

        // ---------- 3) 只勾两组 ----------
        List<String> two = new ArrayList<String>(Arrays.asList("memo", "ledger"));
        Backup.setPick(two);
        List<String> now = Backup.pick();
        check("只勾两组时 pick 是 2 组（" + now + "）", now.size() == 2);
        check("顺序按界面顺序（记账本在前）", "ledger".equals(now.get(0)) && "memo".equals(now.get(1)));
        check("settings 里存成 ledger,memo[" + Store.str(Backup.PICK_KEY, "-") + "]",
                "ledger,memo".equals(Store.str(Backup.PICK_KEY, "-")));
        check("提示文字含记账本和备忘录[" + Backup.pickText() + "]",
                Backup.pickText().contains("记账本") && Backup.pickText().contains("备忘录"));
        check("提示文字不再提日程", !Backup.pickText().contains("日程"));

        // ---------- 4) 只备份勾选的那几组 ----------
        Store.writeLines("ledger.txt", new ArrayList<String>(Arrays.asList("0\t25.00\t午饭\t11-23 12:30\t餐饮")));
        Store.writeLines("ledgermap.txt", new ArrayList<String>(Arrays.asList("午饭\t餐饮")));
        Store.writeLines("memo.txt", new ArrayList<String>(Arrays.asList("一条备忘")));
        Store.writeLines("todo.txt", new ArrayList<String>(Arrays.asList("0\t买菜\t2026-11-23")));
        Store.writeLines("pomodoro.txt", new ArrayList<String>(Arrays.asList("25\t5")));
        Store.writeLines("counter.txt", new ArrayList<String>(Arrays.asList("计数器 0 1")));
        String m1 = Backup.manual();
        check("手动备份成功", m1 != null);
        File d1 = m1 == null ? null : new File(Backup.root(), m1);
        int n1 = d1 == null ? -1 : d1.listFiles().length;
        check("快照里只有记账本的 2 个 + 备忘录 1 个 = 3 个（" + n1 + "）", n1 == 3);
        check("快照里有 ledger.txt", d1 != null && new File(d1, "ledger.txt").exists());
        check("快照里有 ledgermap.txt（记忆表一起走）", d1 != null && new File(d1, "ledgermap.txt").exists());
        check("快照里有 memo.txt", d1 != null && new File(d1, "memo.txt").exists());
        check("快照里没有 todo.txt（日程组没勾）", d1 != null && !new File(d1, "todo.txt").exists());
        check("快照里没有 counter.txt", d1 != null && !new File(d1, "counter.txt").exists());

        // ---------- 5) 各组现在有几个文件 ----------
        check("记账本组 2 个文件", Backup.countIn("ledger") == 2);
        check("备忘录组 1 个文件", Backup.countIn("memo") == 1);
        check("日程组 1 个文件（但有 2 个候选文件名，只有 todo 存在）", Backup.countIn("plan") == 1);

        // ---------- 6) 全选回到默认 ----------
        List<String> all = new ArrayList<String>();
        for (String[] g : Backup.GROUPS) all.add(g[0]);
        Backup.setPick(all);
        check("全选后 settings 里存空值 = 默认全量[" + Store.str(Backup.PICK_KEY, "-") + "]",
                "".equals(Store.str(Backup.PICK_KEY, "-")));
        check("全选后 pick 是 6 组", Backup.pick().size() == 6);
        check("全选后提示回到「全部模块」", "全部模块".equals(Backup.pickText()));

        // ---------- 7) 一个都不勾 ----------
        Backup.setPick(new ArrayList<String>());
        check("不勾时 pick 为空", Backup.pick().isEmpty());
        check("不勾时提示「什么都没勾」[" + Backup.pickText() + "]", Backup.pickText().contains("什么都没勾"));
        check("不勾时备份不出来东西", Backup.manual() == null);
        check("不勾时 daily 也不留快照", !Backup.daily());

        // ---------- 8) 设置里塞了乱码值 ----------
        List<String> lines = new ArrayList<String>();
        for (String l : Store.readLines("settings.txt")) {
            if (!l.startsWith(Backup.PICK_KEY + "=")) lines.add(l);
        }
        lines.add(Backup.PICK_KEY + "=xx,yy");
        Store.writeLines("settings.txt", lines);
        check("非法值回落成全量", Backup.pick().size() == Backup.GROUPS.length);

        // ---------- 9) 备份弹窗里有勾选 ----------
        Backup.setPick(all);
        SwingUtilities.invokeAndWait(() -> {
            try {
                Main.main(new String[0]);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        });
        Thread.sleep(700);
        SwingUtilities.invokeAndWait(() -> Main.go("settings"));
        Thread.sleep(600);
        SettingsPanel sp = Main.page(SettingsPanel.class);
        check("能拿到设置页", sp != null);
        if (sp == null) {
            System.out.println("SOME FAIL " + (fail + 1) + "/" + (pass + fail + 1));
            Runtime.getRuntime().halt(1);
        }
        java.lang.reflect.Method mk = SettingsPanel.class.getDeclaredMethod("buildBackupDialog");
        mk.setAccessible(true);
        final JDialog[] dh = new JDialog[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                dh[0] = (JDialog) mk.invoke(sp);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        JDialog dlg = dh[0];
        List<JCheckBox> boxes = new ArrayList<JCheckBox>();
        collectChecks(dlg.getContentPane(), boxes);
        check("弹窗里有 6 个模块勾选框（" + boxes.size() + "）", boxes.size() == 6);
        String labels = "";
        for (JCheckBox b : boxes) labels += b.getText() + "|";
        check("勾选框文字对得上[" + labels + "]",
                labels.contains("记账本") && labels.contains("备忘录") && labels.contains("日程"));
        int checked = 0;
        for (JCheckBox b : boxes) if (b.isSelected()) checked++;
        check("全量时 6 个都是勾上的（" + checked + "）", checked == 6);
        SwingUtilities.invokeAndWait(dlg::dispose);

        // ---------- 10) 用户目录没被动 ----------
        check("用户真实目录未被改动", before.equals(snapshot(real)));

        System.out.println(fail == 0 ? ("ALL PASS (" + pass + ")") : ("SOME FAIL " + fail + "/" + (pass + fail)));
        Runtime.getRuntime().halt(fail == 0 ? 0 : 1);
    }

    static void collectChecks(java.awt.Container c, List<JCheckBox> out) {
        for (java.awt.Component x : c.getComponents()) {
            if (x instanceof JCheckBox) out.add((JCheckBox) x);
            if (x instanceof java.awt.Container) collectChecks((java.awt.Container) x, out);
        }
    }

    static String snapshot(File d) {
        StringBuilder sb = new StringBuilder();
        File[] fs = d.listFiles();
        if (fs == null) return "(none)";
        for (File f : fs) {
            if (f.isFile()) sb.append(f.getName()).append('/').append(f.length())
                    .append('/').append(f.lastModified()).append(';');
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
        d.delete();
    }

    static void check(String name, boolean ok) {
        System.out.println((ok ? "[PASS] " : "[FAIL] ") + name);
        if (ok) pass++;
        else fail++;
    }
}
