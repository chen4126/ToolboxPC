import com.toolbox.pc.*;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** v2.4：导入记忆能看、能改、能删 */
public class FeatureTest77 {

    static int pass = 0, fail = 0;
    static File tmp;

    public static void main(String[] a) throws Exception {
        tmp = new File(System.getProperty("java.io.tmpdir"), "toolbox-ft77");
        wipe(tmp);
        tmp.mkdirs();
        System.setProperty("toolbox.data.dir", tmp.getAbsolutePath());

        File real = new File(System.getProperty("user.home"), "ToolboxPC");
        String before = snapshot(real);

        Store.writeLines("settings.txt", new ArrayList<String>(Arrays.asList("sound=1")));

        // ---------- 1) 攒记忆 ----------
        LedgerPanel.remember("超市 买日用品 87.6", "购物");
        LedgerPanel.remember("美团外卖 32.5", "餐饮");
        List<String[]> m = LedgerPanel.loadMap();
        check("攒下 2 条记忆（" + m.size() + "）", m.size() == 2);
        check("关键词是压过的前 6 字[" + m.get(0)[0] + "]", m.get(0)[0].length() <= 6);
        check("长关键词排前面[" + m.get(0)[0] + "]", m.get(0)[0].length() >= m.get(1)[0].length());

        // ---------- 2) 改分类 ----------
        String kw = m.get(0)[0];
        check("setCat 返回 true", LedgerPanel.setCat(kw, "日用"));
        boolean found = false;
        for (String[] e : LedgerPanel.loadMap()) {
            if (e[0].equals(kw) && e[1].equals("日用")) found = true;
        }
        check("改完读到的是新分类[" + kw + " → 日用]", found);
        check("改完条数没变", LedgerPanel.loadMap().size() == 2);

        // ---------- 3) 新关键词也能直接加 ----------
        LedgerPanel.setCat("奶茶", "餐饮");
        check("setCat 遇到新关键词会新增（3 条）", LedgerPanel.loadMap().size() == 3);

        // ---------- 4) 删一条 ----------
        check("forget 返回 true", LedgerPanel.forget("奶茶"));
        check("删完剩 2 条", LedgerPanel.loadMap().size() == 2);
        check("再删不存在的关键词返回 false", !LedgerPanel.forget("不存在的东西"));
        check("删完「奶茶」真的没了", noKey("奶茶"));

        // ---------- 5) 分类合集 ----------
        List<String> cats = LedgerPanel.allCats();
        check("分类合集里有支出分类「餐饮」", cats.contains("餐饮"));
        check("分类合集里有收入分类「工资」", cats.contains("工资"));
        check("支出分类排在收入前面",
                cats.indexOf("餐饮") < cats.indexOf("工资"));

        // ---------- 6) 设置页里的「导入记忆」区 ----------
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
        final SettingsPanel sp = Main.page(SettingsPanel.class);
        check("能拿到设置页", sp != null);
        if (sp == null) {
            System.out.println("SOME FAIL " + (fail + 1) + "/" + (pass + fail + 1));
            Runtime.getRuntime().halt(1);
        }
        final List<JComboBox<?>> boxes = new ArrayList<JComboBox<?>>();
        final List<JButton> btns = new ArrayList<JButton>();
        SwingUtilities.invokeAndWait(() -> {
            collect(sp, boxes, btns);
        });
        String btnTexts = "";
        for (JButton b : btns) btnTexts += b.getText() + "|";
        check("设置页有「改成这个分类」[" + btnTexts + "]", btnTexts.contains("改成这个分类"));
        check("设置页有「删除这条」", btnTexts.contains("删除这条"));
        check("设置页有下拉框（记忆选择 + 分类）", boxes.size() >= 2);

        JComboBox<?> mapBox = null;
        for (JComboBox<?> b : boxes) {
            if (b.getItemCount() > 0 && String.valueOf(b.getItemAt(0)).contains("→")) mapBox = b;
        }
        check("记忆下拉里是「关键词 → 分类」", mapBox != null);
        if (mapBox != null) {
            check("下拉条数 = 记忆条数（" + mapBox.getItemCount() + "）", mapBox.getItemCount() == 2);
            check("下拉里能看到改过的分类[" + mapBox.getItemAt(0) + "]",
                    String.valueOf(mapBox.getItemAt(0)).contains("日用"));
        }

        // ---------- 7) 真实目录没被动 ----------
        check("用户真实目录未被改动", before.equals(snapshot(real)));

        System.out.println(fail == 0 ? ("ALL PASS (" + pass + ")") : ("SOME FAIL " + fail + "/" + (pass + fail)));
        Runtime.getRuntime().halt(fail == 0 ? 0 : 1);
    }

    static boolean noKey(String k) {
        for (String[] e : LedgerPanel.loadMap()) if (e[0].equals(k)) return false;
        return true;
    }

    static void collect(java.awt.Container c, List<JComboBox<?>> boxes, List<JButton> btns) {
        for (java.awt.Component x : c.getComponents()) {
            if (x instanceof JComboBox) boxes.add((JComboBox<?>) x);
            if (x instanceof JButton) btns.add((JButton) x);
            if (x instanceof java.awt.Container) collect((java.awt.Container) x, boxes, btns);
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
