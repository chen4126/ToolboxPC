import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** v2.1：记账本「导入」——任意格式账单文字，一行一笔自动识别日期/金额/分类 */
public class FeatureTest71 {

    static int pass = 0, fail = 0;
    static Object main, ledger;
    static final LocalDate REF = LocalDate.of(2026, 11, 16);   // 固定基准，不随真实日期漂移

    public static void main(String[] a) throws Exception {
        SwingUtilities.invokeAndWait(() -> Main.main(new String[0]));
        Thread.sleep(2500);
        Class<?> mc = Class.forName("com.toolbox.pc.Main");
        Field selfF = mc.getDeclaredField("self");
        selfF.setAccessible(true);
        main = selfF.get(null);
        Method pageM = mc.getDeclaredMethod("page", Class.class);
        ledger = pageM.invoke(main, Class.forName("com.toolbox.pc.LedgerPanel"));
        Method sel = mc.getDeclaredMethod("selectNav", String.class);
        sel.setAccessible(true);
        SwingUtilities.invokeAndWait(() -> {
            try {
                sel.invoke(main, "ledger");
            } catch (Exception ignored) {
            }
        });
        Thread.sleep(1200);
        check("记账页已创建", ledger != null);

        // ---------- 1) 输入行里「导入」在「记一笔」右边 ----------
        JButton add = findButton((Component) ledger, "记一笔");
        JButton imp = findButton((Component) ledger, "导入");
        check("找到「记一笔」按钮", add != null);
        check("找到「导入」按钮", imp != null);
        if (add != null && imp != null) {
            Container parent = add.getParent();
            check("两个按钮在同一行", parent != null && imp.getParent() == parent);
            int ai = -1, bi = -1;
            if (parent != null) {
                for (int i = 0; i < parent.getComponentCount(); i++) {
                    if (parent.getComponent(i) == add) ai = i;
                    if (parent.getComponent(i) == imp) bi = i;
                }
            }
            check("「导入」紧跟在「记一笔」后面（" + ai + "→" + bi + "）", ai >= 0 && bi == ai + 1);
        }

        // ---------- 2) 单行解析：各种写法 ----------
        String[] r;

        r = LedgerPanel.parseImportLine("2026-11-16 午饭 25", REF);
        check("年月日+支出", ok(r) && "0".equals(r[0]) && "25.00".equals(r[1]) && "餐饮".equals(r[2]),
                show(r));
        check("时间取到 11-16", ok(r) && r[4].startsWith("11-16"), show(r));
        check("备注留「午饭」", ok(r) && "午饭".equals(r[3]), show(r));

        r = LedgerPanel.parseImportLine("11-15 打车 -18.5", REF);
        check("月日+负号=支出", ok(r) && "0".equals(r[0]) && "18.50".equals(r[1]) && "交通".equals(r[2]),
                show(r));
        check("日期 11-15", ok(r) && r[4].startsWith("11-15"), show(r));

        r = LedgerPanel.parseImportLine("昨天 工资 +6500", REF);
        check("昨天+正号=收入", ok(r) && "1".equals(r[0]) && "6500.00".equals(r[1]) && "工资".equals(r[2]),
                show(r));
        check("日期回退到 11-15", ok(r) && r[4].startsWith("11-15"), show(r));

        r = LedgerPanel.parseImportLine("10号 奶茶 12元", REF);
        check("几号+带「元」", ok(r) && "12.00".equals(r[1]) && "餐饮".equals(r[2]), show(r));
        check("日期 11-10", ok(r) && r[4].startsWith("11-10"), show(r));

        r = LedgerPanel.parseImportLine("超市 买日用品 87.6", REF);
        check("裸数字当金额", ok(r) && "87.60".equals(r[1]), show(r));
        check("「超市」归购物", ok(r) && "购物".equals(r[2]), show(r));
        check("没写日期就用今天", ok(r) && r[4].startsWith("11-16"), show(r));

        r = LedgerPanel.parseImportLine("健身房月卡 ¥299", REF);
        check("¥ 前缀金额", ok(r) && "299.00".equals(r[1]), show(r));
        check("「健身」归娱乐", ok(r) && "娱乐".equals(r[2]), show(r));

        r = LedgerPanel.parseImportLine("2026/11/12 10:30 电影 45", REF);
        check("斜杠日期+时间", ok(r) && r[4].equals("11-12 10:30"), show(r));
        check("「电影」归娱乐", ok(r) && "娱乐".equals(r[2]), show(r));

        r = LedgerPanel.parseImportLine("前天 早餐 8块", REF);
        check("前天回退两天", ok(r) && r[4].startsWith("11-14"), show(r));
        check("「块」也算金额单位", ok(r) && "8.00".equals(r[1]), show(r));

        r = LedgerPanel.parseImportLine("１１－１５　外卖　２３", REF);
        check("全角数字和减号也能认", ok(r) && "23.00".equals(r[1]) && "餐饮".equals(r[2]), show(r));
        check("全角日期 11-15", ok(r) && r[4].startsWith("11-15"), show(r));

        r = LedgerPanel.parseImportLine("退款到账 88", REF);
        check("「退款」自动判成收入", ok(r) && "1".equals(r[0]), show(r));
        check("「退款」归理财", ok(r) && "理财".equals(r[2]), show(r));

        r = LedgerPanel.parseImportLine("工资 6500", REF);
        check("没写符号但有关键词→收入", ok(r) && "1".equals(r[0]) && "工资".equals(r[2]), show(r));

        r = LedgerPanel.parseImportLine("1-05 红包 200", REF);
        check("跨年的月日回到去年", ok(r) && r[4].startsWith("01-05"), show(r));
        check("「红包」归人情", ok(r) && "人情".equals(r[2]), show(r));

        r = LedgerPanel.parseImportLine("这条没有金额", REF);
        check("没金额的行被拒", !ok(r) && r[5].contains("金额"), show(r));

        r = LedgerPanel.parseImportLine("午饭 0", REF);
        check("金额 0 被拒", !ok(r) && r[5].contains("大于"), show(r));

        r = LedgerPanel.parseImportLine("   ", REF);
        check("空行被拒", !ok(r) && "空行".equals(r[5]), show(r));

        r = LedgerPanel.parseImportLine(null, REF);
        check("null 不崩", !ok(r), show(r));

        // 多行整段
        List<String[]> rows = LedgerPanel.parseImport(
                "2026-11-16 午饭 25\n\n11-15 打车 18.5\n没金额的一行\n", REF);
        check("空行跳过 → 3 行（" + rows.size() + "）", rows.size() == 3);
        int okc = 0;
        for (String[] x : rows) if (ok(x)) okc++;
        check("其中 2 行可导入（" + okc + "）", okc == 2);

        // ---------- 3) 写进账本 ----------
        Store.writeLines("ledger.txt", new ArrayList<String>(Arrays.asList(
                "0\t12.50\t老的一条\t11-10 09:00\t餐饮")));
        int n = (Integer) call(ledger, "importRows", new Class<?>[]{List.class}, new Object[]{rows});
        check("导入 2 条成功（" + n + "）", n == 2);
        List<String> lf = Store.readLines("ledger.txt");
        check("账本从 1 条变 3 条（" + lf.size() + "）", lf.size() == 3);
        check("老数据没动", lf.get(0).startsWith("0\t12.50\t老的一条"), lf.get(0));
        check("新条带分类", lf.get(1).endsWith("\t餐饮"), lf.get(1));
        check("新条 5 段", lf.get(1).split("\t", -1).length == 5, lf.get(1));
        int n0 = (Integer) call(ledger, "importRows", new Class<?>[]{List.class}, new Object[]{new ArrayList<String[]>()});
        check("空列表返回 0", n0 == 0);

        // ---------- 4) 弹窗结构 ----------
        JDialog dlg = (JDialog) call(ledger, "createImportDialog", new Class<?>[]{}, new Object[]{});
        check("导入弹窗建出来了", dlg != null);
        check("标题带「格式不限」[" + (dlg == null ? "" : dlg.getTitle()) + "]",
                dlg != null && dlg.getTitle().contains("格式不限"));
        JTextArea area = findComp(dlg, JTextArea.class);
        check("有粘贴文本框", area != null);
        check("文本框预填了示例（" + (area == null ? 0 : area.getText().split("\n").length) + " 行）",
                area != null && area.getText().split("\n").length == 7);
        JTable pv = findComp(dlg, JTable.class);
        check("有预览表", pv != null);
        check("预览表 6 列（" + (pv == null ? 0 : pv.getColumnCount()) + "）",
                pv != null && pv.getColumnCount() == 6);
        check("表头对得上", pv != null && "识别".equals(pv.getColumnName(0))
                && "金额".equals(pv.getColumnName(5)));
        JButton okBtn = findButton(dlg, "确认导入");
        check("有「确认导入」按钮", okBtn != null);
        check("没解析前不能点导入", okBtn != null && !okBtn.isEnabled());
        check("有「解析」按钮", findButton(dlg, "解析") != null);
        check("有「取消」按钮", findButton(dlg, "取消") != null);
        check("有「填示例」按钮", findButton(dlg, "填示例") != null);

        // 点解析 → 表格填满、按钮亮起来
        JButton parseBtn = findButton(dlg, "解析");
        SwingUtilities.invokeAndWait(() -> parseBtn.doClick());
        SwingUtilities.invokeAndWait(() -> { });
        Thread.sleep(200);
        check("示例 7 行全进预览（" + pv.getRowCount() + "）", pv.getRowCount() == 7);
        check("最后一行标记跳过[" + pv.getValueAt(6, 0) + "]",
                String.valueOf(pv.getValueAt(6, 0)).contains("跳过"));
        boolean allOk = true;
        for (int i = 0; i < 6; i++) {
            if (!String.valueOf(pv.getValueAt(i, 0)).startsWith("可导入")) allOk = false;
        }
        check("其余 6 行都是可导入[" + pv.getValueAt(0, 0) + "]", allOk);
        check("确认按钮变为可点且带条数[" + okBtn.getText() + "]",
                okBtn.isEnabled() && okBtn.getText().contains("6"));

        // 出图
        shot(dlg, "C:\\Users\\chen\\Desktop\\ui-292-ledger-import.png");

        System.out.println(fail == 0 ? ("ALL PASS (" + pass + ")") : ("SOME FAIL " + fail + "/" + (pass + fail)));
        Runtime.getRuntime().halt(fail == 0 ? 0 : 1);
    }

    static boolean ok(String[] r) {
        return r != null && r.length == 6 && r[5] == null;
    }

    static String show(String[] r) {
        if (r == null) return "null";
        return "{" + r[0] + "," + r[1] + "," + r[2] + "," + r[3] + "," + r[4] + "," + r[5] + "}";
    }

    static Object call(Object o, String method, Class<?>[] types, Object[] args) throws Exception {
        Method m = o.getClass().getDeclaredMethod(method, types);
        m.setAccessible(true);
        return m.invoke(o, args);
    }

    static JButton findButton(Component root, String text) {
        List<Component> all = new ArrayList<>();
        collect(root, all);
        for (Component c : all) if (c instanceof JButton && text.equals(((JButton) c).getText())) return (JButton) c;
        return null;
    }

    static <T> T findComp(Component root, Class<T> cls) {
        List<Component> all = new ArrayList<>();
        collect(root, all);
        for (Component c : all) if (cls.isInstance(c)) return cls.cast(c);
        return null;
    }

    static void collect(Component c, List<Component> out) {
        out.add(c);
        if (c instanceof Container) for (Component k : ((Container) c).getComponents()) collect(k, out);
    }

    static void layoutAll(Component c) {
        c.doLayout();
        if (c instanceof Container) for (Component k : ((Container) c).getComponents()) layoutAll(k);
    }

    static void shot(JDialog dlg, String path) throws Exception {
        JComponent root = (JComponent) dlg.getContentPane();
        if (dlg.getContentPane() instanceof JPanel) {
            ((JPanel) dlg.getContentPane()).setOpaque(true);
            dlg.getContentPane().setBackground(new Color(0xF7F8FC));
        }
        dlg.pack();
        dlg.setSize(840, 600);
        dlg.doLayout();
        layoutAll(dlg.getContentPane());
        int w = dlg.getContentPane().getWidth(), h = dlg.getContentPane().getHeight();
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(w, h,
                java.awt.image.BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        dlg.getContentPane().printAll(g);
        g.dispose();
        javax.imageio.ImageIO.write(img, "png", new File(path));
        System.out.println("SHOT " + path + " " + new File(path).length() + " " + w + "x" + h);
        dlg.dispose();
    }

    static void check(String name, boolean ok) {
        check(name, ok, "");
    }

    static void check(String name, boolean ok, String extra) {
        System.out.println((ok ? "[PASS] " : "[FAIL] ") + name + (extra.isEmpty() ? "" : " → " + extra));
        if (ok) pass++;
        else fail++;
    }
}
