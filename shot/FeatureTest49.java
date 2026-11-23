import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/** v1.67 验证：节奏行防截断、环形图空态不压环、抬头“整组退回”、小窗勾/退/再勾 */
public class FeatureTest49 {

    static boolean ok = true;
    static OverviewPanel ov;
    static JFrame frame;
    static Object main;

    static String[] DATA() {
        java.time.LocalDate t = Seed.today();
        java.time.LocalDate mon = Seed.monday();
        return new String[]{
                "0\t今天的事\t" + t, "0\t九天前没动\t" + t.minusDays(9), "0\t四天前\t" + t.minusDays(4),
                "0\t昨天没做\t" + t.minusDays(1), "0\t没定日子的\t-",
                "1\t今天已做完\t" + t, "1\t今天第二件\t" + t,
                "1\t今天第三件\t" + t, "1\t今天第四件\t" + t,
                "1\t周一做的\t" + mon, "1\t上周做的\t" + mon.minusDays(5), "1\t上上周做的\t" + mon.minusDays(12)};
    }

    public static void main(String[] a) {
        try {
            run();
        } catch (Throwable t) {
            t.printStackTrace();
            ok = false;
        }
        System.out.println(ok ? "ALL PASS" : "SOME FAILED");
        Runtime.getRuntime().halt(ok ? 0 : 1);
    }

    static void run() throws Exception {
        Store.writeLines("todo.txt", Arrays.asList(DATA()));
        SwingUtilities.invokeAndWait(() -> Main.main(new String[0]));
        Thread.sleep(2500);
        Class<?> mc = Class.forName("com.toolbox.pc.Main");
        Field selfF = mc.getDeclaredField("self");
        selfF.setAccessible(true);
        main = selfF.get(null);
        Field ff = mc.getDeclaredField("frame");
        ff.setAccessible(true);
        frame = (JFrame) ff.get(main);
        go("overview");
        Thread.sleep(1000);
        ov = find(frame.getContentPane(), OverviewPanel.class);
        Method rf = OverviewPanel.class.getDeclaredMethod("refresh");
        rf.setAccessible(true);
        rf.invoke(ov);
        Thread.sleep(200);

        rateRowFits();

        donutFits();

        groupBack();

        miniToggle();

        Store.writeLines("todo.txt", Arrays.asList(DATA()));
    }

    /* A. 节奏行：任何宽度下文字都不超出可用宽度 */
    static void rateRowFits() throws Exception {
        Field rowF = OverviewPanel.class.getDeclaredField("paceRow");
        rowF.setAccessible(true);
        JPanel paceRow = (JPanel) rowF.get(ov);
        Field barF = OverviewPanel.class.getDeclaredField("paceBar");
        barF.setAccessible(true);
        JLabel paceBar = (JLabel) barF.get(ov);
        Method apply = OverviewPanel.class.getDeclaredMethod("applyPaceText");
        apply.setAccessible(true);
        String[] cases = new String[3];
        boolean allFit = true;
        /* 量宽度时把 paceRow 从父容器摘出来：否则父容器任何一次 validate 都会把宽度
           改回真实值，断言读到的是"真实宽度下的文案"，w=200 那一档会偶发假 FAIL */
        Container parent = paceRow.getParent();
        int idx = -1;
        if (parent != null) {
            idx = java.util.Arrays.asList(parent.getComponents()).indexOf(paceRow);
            parent.remove(paceRow);
        }
        try {
        for (int w : new int[]{420, 300, 200, 150}) {
            paceRow.setSize(w, 22);
            paceRow.doLayout();
            apply.invoke(ov);
            String txt = paceBar.getText();
            int need = paceBar.getFontMetrics(paceBar.getFont()).stringWidth(txt);
            int keep = 76
                    + (labelVisible(OverviewPanel.class, ov, "streakLbl")
                    ? labelPref(OverviewPanel.class, ov, "streakLbl") : 0)
                    + (labelVisible(OverviewPanel.class, ov, "gapLbl")
                    ? labelPref(OverviewPanel.class, ov, "gapLbl") : 0);
            int room = w - keep - 8;
            boolean fit = need <= Math.max(room, 0);
            if (!fit) allFit = false;
            System.out.println("  w=" + w + " room=" + room + " need=" + need + " text=" + txt);
            if (w == 420) cases[0] = txt;
            if (w == 200) cases[1] = txt;
            if (w == 150) cases[2] = txt;
        }
        check("节奏行 420 用完整文案",
                cases[0].contains("本周 5 条 · 上周 1 条 · 本月 " + monthCount() + " 条"), cases[0]);
        check("节奏行 200 退到短文案", !cases[1].contains("条 · 上周"), cases[1]);
        check("节奏行 150 用最短", cases[2].equals("5 / 1 / " + monthCount()) || cases[2].length() <= 12, cases[2]);
        check("各宽度都不裁字", allFit, "");
        paceRow.setSize(360, 22);
        apply.invoke(ov);
        } finally {
            if (parent != null) {
                if (idx >= 0) parent.add(paceRow, idx); else parent.add(paceRow);
                parent.revalidate();
                parent.repaint();
            }
        }
    }

    /** 本套数据里本月完成的条数：本周 5 + 上周 1 +（上上周那条若还在本月）1 */
    static int monthCount() {
        java.time.LocalDate t = Seed.today();
        int n = 0;
        for (String r : DATA()) {
            String[] p = r.split("\t", -1);
            if (p.length < 3 || !"1".equals(p[0])) continue;
            java.time.LocalDate d = java.time.LocalDate.parse(p[2].trim());
            if (d.getYear() == t.getYear() && d.getMonthValue() == t.getMonthValue()) n++;
        }
        return n;
    }

    /** 今天完成的条数：周一时「周一做的」也落在今天，4 → 5 */
    static int todayCount49() {
        return 4 + (Seed.today().getDayOfWeek().getValue() == 1 ? 1 : 0);
    }

    static boolean labelVisible(Class<?> c, Object o, String n) throws Exception {
        Field f = c.getDeclaredField(n);
        f.setAccessible(true);
        JLabel l = (JLabel) f.get(o);
        return l != null && l.isVisible();
    }

    static int labelPref(Class<?> c, Object o, String n) throws Exception {
        Field f = c.getDeclaredField(n);
        f.setAccessible(true);
        JLabel l = (JLabel) f.get(o);
        return l == null ? 0 : l.getPreferredSize().width;
    }

    /* B. 环形图空态：两行小字都落在内孔里，不压到圆环 */
    static void donutFits() throws Exception {
        Class<?> dc = Class.forName("com.toolbox.pc.StatsPanel$DonutChart");
        java.lang.reflect.Constructor<?> ctor = dc.getDeclaredConstructor();
        ctor.setAccessible(true);
        JPanel d = (JPanel) ctor.newInstance();
        d.setDoubleBuffered(false);          // 离屏组件没有 Window，双缓冲会取不到 buffer
        d.setSize(300, 120);
        Method setData = dc.getDeclaredMethod("setData", String.class, java.util.Map.class);
        setData.setAccessible(true);
        setData.invoke(d, "近 7 天支出分类", new java.util.LinkedHashMap<String, Double>());
        BufferedImage img = new BufferedImage(300, 120, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 300, 120);
        d.paint(g);
        g.dispose();
        Color muted = Theme.muted();
        int minX = 999, maxX = -1;
        for (int y = 0; y < 120; y++) {
            for (int x = 0; x < 300; x++) {
                int rgb = img.getRGB(x, y) & 0xFFFFFF;
                if (near(rgb, muted.getRGB() & 0xFFFFFF)) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                }
            }
        }
        int hole = (int) (96 * 0.68);
        int span = maxX - minX;
        check("空态文字有两行墨迹", maxX > minX, minX + "~" + maxX);
        check("空态文字宽度在内孔以内", span <= hole, "span=" + span + " hole=" + hole);
    }

    static boolean near(int a, int b) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return Math.abs(ar - br) <= 6 && Math.abs(ag - bg) <= 6 && Math.abs(ab - bb) <= 6;
    }

    /* C. 抬头右键：4 项 + 整组退回（4 条要二次确认） */
    static void groupBack() throws Exception {
        Method dlM = OverviewPanel.class.getDeclaredMethod("doneListPanel",
                String.class, String.class, List.class, JDialog.class);
        dlM.setAccessible(true);
        List rows = thisWeek();
        JPanel list = (JPanel) dlM.invoke(ov, "本周完成 · 共 5 条", "本周 5 条", rows, null);
        list.setSize(520, 352);
        layoutAll(list);
        java.util.List<JPanel> heads = headers(list);
        boolean isMon = Seed.today().getDayOfWeek().getValue() == 1;
        int groups = isMon ? 1 : 2;
        int todayN = todayCount49();
        check("本周 " + groups + " 组抬头", heads.size() == groups, "heads=" + heads.size());
        JPopupMenu gm = heads.get(0).getComponentPopupMenu();
        check("菜单 4 项", gm != null && gm.getComponentCount() == 4,
                gm == null ? "null" : String.valueOf(gm.getComponentCount()));
        JMenuItem back = (JMenuItem) gm.getComponent(3);
        check("第 4 项是整组退回且带条数", back.getText().equals("把这一天全部退回 " + todayN + " 条"), back.getText());

        SwingUtilities.invokeAndWait(() -> back.doClick());
        Thread.sleep(120);
        check(todayN + " 条先要确认", back.getText().equals("再点一下确认退回 " + todayN + " 条"), back.getText());
        check("确认前文件没动", doneFlag("今天已做完") == 1, "done=" + doneFlag("今天已做完"));

        SwingUtilities.invokeAndWait(() -> back.doClick());
        Thread.sleep(250);
        check("确认后 " + todayN + " 条全退回", doneFlag("今天已做完") == 0 && doneFlag("今天第二件") == 0
                && doneFlag("今天第三件") == 0 && doneFlag("今天第四件") == 0,
                "四件= " + doneFlag("今天已做完") + "/" + doneFlag("今天第二件") + "/"
                        + doneFlag("今天第三件") + "/" + doneFlag("今天第四件"));
        if (isMon) {
            /* 周一：「周一做的」就在今天这一组里，会一起被退回 */
            check("周一时周一做的同组一起退回", doneFlag("周一做的") == 0, "done=" + doneFlag("周一做的"));
        } else {
            check("同组另一天没被动", doneFlag("周一做的") == 1, "done=" + doneFlag("周一做的"));
        }
        Store.writeLines("todo.txt", Arrays.asList(DATA()));
    }

    /* D. 小窗：勾 → 撤回 → 再勾上 */
    static void miniToggle() throws Exception {
        Method rf = OverviewPanel.class.getDeclaredMethod("refresh");
        rf.setAccessible(true);
        rf.invoke(ov);
        MiniBoard.show();
        Thread.sleep(700);
        Class<?> mbc = Class.forName("com.toolbox.pc.MiniBoard");
        Field instF = mbc.getDeclaredField("inst");
        instF.setAccessible(true);
        MiniBoard board = (MiniBoard) instF.get(null);
        Field lbF = MiniBoard.class.getDeclaredField("listBox");
        lbF.setAccessible(true);
        JPanel listBox = (JPanel) lbF.get(board);
        JLabel row = null;
        for (Component c : listBox.getComponents()) {
            if (c instanceof JLabel && ((JLabel) c).getText().startsWith("○ 今天的事")) row = (JLabel) c;
        }
        check("小窗里有这条", row != null, String.valueOf(row));
        if (row == null) return;
        clickOn(row);
        Thread.sleep(350);
        JLabel tip = (JLabel) listBox.getComponent(0);
        check("勾掉后提示已勾", tip.getText().contains("已勾") && tip.getText().contains("点我撤回"), tip.getText());
        clickOn(tip);
        Thread.sleep(350);
        tip = (JLabel) listBox.getComponent(0);
        check("撤回后文件未完成", doneFlag("今天的事") == 0, "done=" + doneFlag("今天的事"));
        check("提示变成可再勾上", tip.getText().contains("已退回") && tip.getText().contains("点我再勾上"), tip.getText());
        clickOn(tip);
        Thread.sleep(350);
        tip = (JLabel) listBox.getComponent(0);
        check("再点一次又标完成", doneFlag("今天的事") == 1, "done=" + doneFlag("今天的事"));
        check("提示回到已勾", tip.getText().contains("已勾") && tip.getText().contains("点我撤回"), tip.getText());
        Store.writeLines("todo.txt", Arrays.asList(DATA()));
        JLabel pace = fieldLabel(MiniBoard.class, board, "paceLbl");
        check("节奏行同步", pace.getText().contains("本周完成 6 条"), pace.getText());
    }

    static int doneFlag(String text) {
        for (String line : Store.readLines("todo.txt")) {
            String[] p = line.split("\t", -1);
            if (p.length < 2) continue;
            if (p[1].trim().equals(text)) return "1".equals(p[0].trim()) ? 1 : 0;
        }
        return -1;
    }

    static void clickOn(Component c) throws Exception {
        SwingUtilities.invokeAndWait(() -> c.dispatchEvent(new java.awt.event.MouseEvent(c,
                java.awt.event.MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 6, 6, 1, false,
                java.awt.event.MouseEvent.BUTTON1)));
    }

    static void go(String key) throws Exception {
        Method sel = Class.forName("com.toolbox.pc.Main").getDeclaredMethod("selectNav", String.class);
        sel.setAccessible(true);
        SwingUtilities.invokeAndWait(() -> {
            try {
                sel.invoke(main, key);
            } catch (Exception ignored) {
            }
        });
    }

    static JLabel fieldLabel(Class<?> cls, Object target, String name) throws Exception {
        Field f = cls.getDeclaredField(name);
        f.setAccessible(true);
        return (JLabel) f.get(target);
    }

    static List<?> thisWeek() throws Exception {
        java.time.LocalDate mon = java.time.LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        Field tF = OverviewPanel.class.getDeclaredField("todos");
        tF.setAccessible(true);
        List<?> todos = (List<?>) tF.get(ov);
        java.util.List<String[]> out = new java.util.ArrayList<String[]>();
        for (Object t : todos) {
            Class<?> tc = t.getClass();
            Field doneF = tc.getDeclaredField("done");
            doneF.setAccessible(true);
            Field textF = tc.getDeclaredField("text");
            textF.setAccessible(true);
            Field dateF = tc.getDeclaredField("date");
            dateF.setAccessible(true);
            if (!doneF.getBoolean(t)) continue;
            String text = String.valueOf(textF.get(t));
            if (text.endsWith("（已归档）")) continue;
            java.time.LocalDate d;
            try {
                d = java.time.LocalDate.parse(String.valueOf(dateF.get(t)).trim());
            } catch (Exception e) {
                continue;
            }
            if (d.isBefore(mon) || d.isAfter(mon.plusDays(6))) continue;
            out.add(new String[]{text, d.toString()});
        }
        java.util.Collections.sort(out, (x, y) -> y[1].compareTo(x[1]));
        return out;
    }

    static java.util.List<JPanel> headers(JPanel p) {
        java.util.List<JPanel> out = new java.util.ArrayList<JPanel>();
        for (Component c : all(p)) {
            if (!(c instanceof JPanel)) continue;
            JPanel h = (JPanel) c;
            if (!(h.getBorder() instanceof javax.swing.border.CompoundBorder)) continue;
            if (h.getMaximumSize() == null) continue;
            if (h.getMaximumSize().height != 22 && h.getMaximumSize().height != 28) continue;
            out.add(h);
        }
        return out;
    }

    static java.util.List<Component> all(Container p) {
        java.util.List<Component> out = new java.util.ArrayList<Component>();
        for (Component c : p.getComponents()) {
            out.add(c);
            if (c instanceof Container) out.addAll(all((Container) c));
        }
        return out;
    }

    static void layoutAll(Component c) {
        c.doLayout();
        if (c instanceof Container) for (Component k : ((Container) c).getComponents()) layoutAll(k);
    }

    static void check(String name, boolean pass, String detail) {
        if (!pass) ok = false;
        System.out.println((pass ? "[PASS] " : "[FAIL] ") + name + " → " + detail);
    }

    static <T extends Component> T find(Component c, Class<T> type) {
        if (type.isInstance(c)) return type.cast(c);
        if (c instanceof Container) {
            for (Component k : ((Container) c).getComponents()) {
                T t = find(k, type);
                if (t != null) return t;
            }
        }
        return null;
    }
}
