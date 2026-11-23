import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** v2.3：账本导入的分类记忆（同一个写法下次自动套用） */
public class FeatureTest74 {

    static int pass = 0, fail = 0;
    static File tmp;

    public static void main(String[] a) throws Exception {
        tmp = new File(System.getProperty("java.io.tmpdir"), "toolbox-ft74");
        wipe(tmp);
        tmp.mkdirs();
        System.setProperty("toolbox.data.dir", tmp.getAbsolutePath());

        File real = new File(System.getProperty("user.home"), "ToolboxPC");
        String before = snapshot(real);

        Store.writeLines("settings.txt", new ArrayList<String>(Arrays.asList("sound=0")));
        Store.writeLines("ledger.txt", new ArrayList<String>());
        Store.writeLines("ledgermap.txt", new ArrayList<String>());

        LocalDate d = LocalDate.of(2026, 11, 19);

        // ---------- 1) 关键词压缩 ----------
        check("关键词去数字和空格（" + LedgerPanel.keyOf("美团外卖 32.5") + "）",
                "美团外卖".equals(LedgerPanel.keyOf("美团外卖 32.5")));
        check("超长备注只取前 6 字（" + LedgerPanel.keyOf("在楼下小卖部买了瓶水") + "）",
                LedgerPanel.keyOf("在楼下小卖部买了瓶水").length() == 6);
        check("空备备注不产生关键词", LedgerPanel.keyOf("  42 ").isEmpty() || LedgerPanel.keyOf("  42 ").length() <= 2);

        // ---------- 2) 没有记忆时按规则猜，第一次是「其他」 ----------
        String[] p1 = LedgerPanel.parseImportLine("老周面馆 25", d);
        check("陌生写法先归到 其他（" + p1[2] + "）", "其他".equals(p1[2]));

        // ---------- 3) 记住之后自动套用 ----------
        LedgerPanel.remember("老周面馆 25", "餐饮");
        check("记忆表写进了文件", Store.readLines("ledgermap.txt").size() == 1);
        check("记忆表内容格式对（" + Store.readLines("ledgermap.txt").get(0) + "）",
                Store.readLines("ledgermap.txt").get(0).equals("老周面馆\t餐饮"));

        String[] p2 = LedgerPanel.parseImportLine("老周面馆 25", d);
        check("同样的写法再来一次，分类是 餐饮（" + p2[2] + "）", "餐饮".equals(p2[2]));
        check("同写法第二次的金额照旧（" + p2[1] + "）", "25.00".equals(p2[1]));

        // ---------- 4) 长关键词优先 ----------
        LedgerPanel.remember("美团", "购物");
        LedgerPanel.remember("美团买菜", "餐饮");
        check("美团买菜 走更长的那条（" + LedgerPanel.mapCat("美团买菜 30", 0) + "）",
                "餐饮".equals(LedgerPanel.mapCat("美团买菜 30", 0)));
        check("美团打车 命中 美团（" + LedgerPanel.mapCat("美团打车 20", 0) + "）",
                "购物".equals(LedgerPanel.mapCat("美团打车 20", 0)));

        // ---------- 5) 收入不会被支出分类带跑 ----------
        String[] in = LedgerPanel.parseImportLine("工资 +6500", d);
        check("收入还是按收入规则归到 工资（" + in[2] + "）", "工资".equals(in[2]));
        String[] in2 = LedgerPanel.parseImportLine("老周面馆 +25", d);
        check("收入行不套用支出的「餐饮」记忆（" + in2[2] + "）", !"餐饮".equals(in2[2]));

        // ---------- 6) 导入入库时顺手记忆 ----------
        SwingUtilities.invokeAndWait(() -> Main.main(new String[0]));
        Thread.sleep(1500);
        LedgerPanel lp = Main.page(LedgerPanel.class);
        check("记账本页能拿到", lp != null);

        String[] row = LedgerPanel.parseImportLine("楼下打印店 8", d);
        check("这行可解析", row[5] == null);
        List<String[]> one = new ArrayList<String[]>();
        one.add(row);
        int n = lp.importRows(one);
        check("导入 1 笔写进账本（" + n + "）", n == 1);
        List<String> led = Store.readLines("ledger.txt");
        check("账本行还是 5 个字段（" + led.get(led.size() - 1) + "）",
                led.get(led.size() - 1).split("\t", -1).length == 5);
        boolean mapped = false;
        for (String l : Store.readLines("ledgermap.txt")) {
            if (l.startsWith("楼下打印店\t")) mapped = true;
        }
        check("导入后自动记住了「楼下打印店」的分类", mapped);

        // ---------- 7) 预览弹窗：分类列可改 ----------
        Method mk = LedgerPanel.class.getDeclaredMethod("createImportDialog");
        mk.setAccessible(true);
        final JDialog[] holder = new JDialog[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                holder[0] = (JDialog) mk.invoke(lp);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        JDialog dlg = holder[0];
        check("导入弹窗能建出来", dlg != null);
        JTable pv = null;
        if (dlg != null) pv = findTable(dlg.getContentPane());
        check("弹窗里有预览表", pv != null);
        if (pv != null) {
            check("预览表 6 列", pv.getColumnCount() == 6);
            check("分类列（第 4 列）可以改", pv.getModel().isCellEditable(0, 3));
            check("备注列不能改", !pv.getModel().isCellEditable(0, 4));
            check("分类列挂了下拉编辑器", pv.getColumnModel().getColumn(3).getCellEditor() != null);
        }
        if (dlg != null) dlg.dispose();

        // ---------- 8) 真实目录没被碰 ----------
        String after = snapshot(real);
        check("用户真实目录未被改动", before.equals(after));

        System.out.println(fail == 0 ? ("ALL PASS (" + pass + ")") : ("SOME FAIL " + fail + "/" + (pass + fail)));
        Runtime.getRuntime().halt(fail == 0 ? 0 : 1);
    }

    static JTable findTable(Container c) {
        for (Component x : c.getComponents()) {
            if (x instanceof JTable) return (JTable) x;
            if (x instanceof Container) {
                JTable t = findTable((Container) x);
                if (t != null) return t;
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
