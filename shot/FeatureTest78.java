import com.toolbox.pc.*;

import javax.swing.*;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** v2.4：统计页「数据备份」卡 */
public class FeatureTest78 {

    static int pass = 0, fail = 0;
    static File tmp;

    public static void main(String[] a) throws Exception {
        tmp = new File(System.getProperty("java.io.tmpdir"), "toolbox-ft78");
        wipe(tmp);
        tmp.mkdirs();
        System.setProperty("toolbox.data.dir", tmp.getAbsolutePath());

        File real = new File(System.getProperty("user.home"), "ToolboxPC");
        String before = snapshot(real);

        Store.writeLines("settings.txt", new ArrayList<String>(Arrays.asList("sound=1")));
        Store.writeLines("ledger.txt", new ArrayList<String>(Arrays.asList("0\t25.00\t午饭\t11-23 12:30\t餐饮")));
        Store.writeLines("memo.txt", new ArrayList<String>(Arrays.asList("备忘一条")));

        // ---------- 1) 占用大小 ----------
        long real2 = 0;
        for (File f : tmp.listFiles()) if (f.isFile() && f.getName().endsWith(".txt")) real2 += f.length();
        String size = StatsPanel.dataSizeText();
        check("占用统计对得上（" + size + " vs " + real2 + " B）",
                real2 < 1024 ? size.equals(real2 + " B") : size.contains("KB"));

        // ---------- 2) 没有快照时 ----------
        SwingUtilities.invokeAndWait(() -> {
            try {
                Main.main(new String[0]);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        });
        Thread.sleep(900);
        SwingUtilities.invokeAndWait(() -> Main.go("stats"));
        Thread.sleep(400);
        final StatsPanel[] ph = new StatsPanel[1];
        SwingUtilities.invokeAndWait(() -> ph[0] = Main.page(StatsPanel.class));
        StatsPanel sp = ph[0];
        check("能拿到统计页", sp != null);
        if (sp == null) {
            System.out.println("SOME FAIL " + (fail + 1) + "/" + (pass + fail + 1));
            Runtime.getRuntime().halt(1);
        }

        Field vf = StatsPanel.class.getDeclaredField("backupValueLabel");
        vf.setAccessible(true);
        Field sf = StatsPanel.class.getDeclaredField("backupSubLabel");
        sf.setAccessible(true);
        Field cf = StatsPanel.class.getDeclaredField("backupCard");
        cf.setAccessible(true);
        final JLabel value = (JLabel) vf.get(sp);
        final JLabel sub = (JLabel) sf.get(sp);
        final JPanel card = (JPanel) cf.get(sp);
        check("备份卡建出来了", value != null && card != null);
        int beforeN = Backup.list().size();
        check("显示份数与实际一致（" + beforeN + " 份）[" + value.getText() + "]",
                value.getText().contains(beforeN + " 份"));
        check("小字里带「最近」或说明[" + sub.getText() + "]",
                sub.getText().contains("最近") || sub.getText().contains("还没有快照"));
        check("提示里带数据占用[" + card.getToolTipText() + "]",
                card.getToolTipText() != null
                        && (card.getToolTipText().contains(" B") || card.getToolTipText().contains("KB")));

        // ---------- 3) 有快照时 ----------
        String m1 = Backup.manual();
        check("手动备份成功", m1 != null);
        Method rf = StatsPanel.class.getDeclaredMethod("refreshBackupCard");
        rf.setAccessible(true);
        SwingUtilities.invokeAndWait(() -> {
            try {
                rf.invoke(sp);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        check("备份后份数 +1（" + (beforeN + 1) + " 份）[" + value.getText() + "]",
                value.getText().contains((beforeN + 1) + " 份"));
        check("小字里有「最近」[" + sub.getText() + "]", sub.getText().contains("最近"));
        check("最近一次只显示月-日，不撑破卡片[" + sub.getText() + "]",
                sub.getText().length() <= 12);
        String shortM1 = m1 != null && m1.length() >= 10 ? m1.substring(5, 10) : "";
        check("最近一次就是刚备份的那份[" + sub.getText() + " / " + m1 + "]",
                sub.getText().contains(shortM1));

        // ---------- 4) 卡片能点 + 有提示 ----------
        check("卡片是手型光标", card.getCursor().getType() == java.awt.Cursor.HAND_CURSOR);
        String tip = card.getToolTipText();
        check("卡片提示不是空的", tip != null && !tip.isEmpty());
        check("提示里写了备份范围[" + tip + "]", tip != null && tip.contains("备份范围"));
        check("提示里写了能回滚", tip != null && tip.contains("回滚"));
        int clicks = 0;
        for (java.awt.event.MouseListener l : card.getMouseListeners()) {
            if (l instanceof java.awt.event.MouseAdapter) clicks++;
        }
        check("卡片挂了点击监听（" + clicks + "）", clicks >= 1);

        // ---------- 5) 备份没了也能自己回到 0 份 ----------
        File root = Backup.root();
        File[] bs = root.listFiles();
        if (bs != null) for (File f : bs) {
            if (f.isDirectory()) deleteDir(f);
        }
        SwingUtilities.invokeAndWait(() -> {
            try {
                rf.invoke(sp);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        check("快照清空后回到 0 份[" + value.getText() + "]", value.getText().contains("0 份"));
        check("又变成「还没有快照」", sub.getText().contains("还没有快照"));

        // ---------- 6) 真实目录没被动 ----------
        check("用户真实目录未被改动", before.equals(snapshot(real)));

        System.out.println(fail == 0 ? ("ALL PASS (" + pass + ")") : ("SOME FAIL " + fail + "/" + (pass + fail)));
        Runtime.getRuntime().halt(fail == 0 ? 0 : 1);
    }

    static void deleteDir(File d) {
        File[] fs = d.listFiles();
        if (fs != null) for (File f : fs) {
            if (f.isDirectory()) deleteDir(f);
            f.delete();
        }
        d.delete();
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
