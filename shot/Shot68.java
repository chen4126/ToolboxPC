import com.toolbox.pc.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/** v1.67 验收：节奏行三档宽度、环形图空态、抬头 4 项菜单、小窗“已退回可再勾” */
public class Shot68 {

    static OverviewPanel ov;
    static JFrame frame;
    static Object main;

    static final String[] DATA = {
            "0\t今天的事\t2026-11-11", "0\t九天前没动\t2026-11-02", "0\t四天前\t2026-11-07",
            "0\t昨天没做\t2026-11-10", "0\t没定日子的\t-",
            "1\t今天已做完\t2026-11-11", "1\t今天第二件\t2026-11-11",
            "1\t今天第三件\t2026-11-11", "1\t今天第四件\t2026-11-11",
            "1\t周一做的\t2026-11-09", "1\t上周做的\t2026-11-04", "1\t上上周做的\t2026-10-28"};

    public static void main(String[] a) {
        try {
            run();
        } catch (Throwable t) {
            t.printStackTrace();
        }
        Runtime.getRuntime().halt(0);
    }

    static void run() throws Exception {
        Store.writeLines("todo.txt", Arrays.asList(DATA));
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
        Thread.sleep(900);
        ov = find(frame.getContentPane(), OverviewPanel.class);
        Method rf = OverviewPanel.class.getDeclaredMethod("refresh");
        rf.setAccessible(true);
        rf.invoke(ov);
        Thread.sleep(250);
        Container root = frame.getContentPane();
        root.setSize(1124, 721);
        layoutAll(root);
        shot(root, 1124, 721, "C:\\Users\\chen\\Desktop\\ui-232-overview.png");

        Field rowF = OverviewPanel.class.getDeclaredField("paceRow");
        rowF.setAccessible(true);
        JPanel paceRow = (JPanel) rowF.get(ov);
        Method apply = OverviewPanel.class.getDeclaredMethod("applyPaceText");
        apply.setAccessible(true);
        int[] widths = {420, 300, 200};
        BufferedImage strip = new BufferedImage(430, 3 * 26 + 8, BufferedImage.TYPE_INT_RGB);
        Graphics2D sg = strip.createGraphics();
        sg.setColor(Color.WHITE);
        sg.fillRect(0, 0, strip.getWidth(), strip.getHeight());
        sg.setColor(new Color(0xEEF2F8));
        sg.fillRect(0, 0, strip.getWidth(), strip.getHeight());
        sg.setColor(Color.WHITE);
        int yOff = 4;
        for (int w : widths) {
            paceRow.setSize(w, 22);
            paceRow.doLayout();
            apply.invoke(ov);
            BufferedImage part = new BufferedImage(w, 22, BufferedImage.TYPE_INT_RGB);
            Graphics2D pg = part.createGraphics();
            pg.setColor(Color.WHITE);
            pg.fillRect(0, 0, w, 22);
            paceRow.paint(pg);
            pg.dispose();
            sg.drawImage(part, 4, yOff, null);
            sg.setColor(new Color(0xC7D1E2));
            sg.drawRect(3, yOff - 1, w + 1, 23);
            sg.setColor(Color.WHITE);
            yOff += 26;
        }
        sg.dispose();
        ImageIO.write(strip, "png", new File("C:\\Users\\chen\\Desktop\\ui-233-pace-narrow.png"));
        System.out.println("saved pace strip");
        paceRow.setSize(340, 22);
        apply.invoke(ov);

        Class<?> dc = Class.forName("com.toolbox.pc.StatsPanel$DonutChart");
        java.lang.reflect.Constructor<?> ctor = dc.getDeclaredConstructor();
        ctor.setAccessible(true);
        JPanel donut = (JPanel) ctor.newInstance();
        donut.setDoubleBuffered(false);
        donut.setSize(320, 136);
        Method setData = dc.getDeclaredMethod("setData", String.class, java.util.Map.class);
        setData.setAccessible(true);
        setData.invoke(donut, "近 7 天支出分类", new java.util.LinkedHashMap<String, Double>());
        shot(donut, 320, 136, "C:\\Users\\chen\\Desktop\\ui-234-donut-empty.png");

        Method dlM = OverviewPanel.class.getDeclaredMethod("doneListPanel",
                String.class, String.class, List.class, JDialog.class);
        dlM.setAccessible(true);
        List rows = thisWeek();
        JPanel list = (JPanel) dlM.invoke(ov, "本周完成 · 共 5 条", "本周 5 条 · 上周 1 条 · 本月 6 条 · 比上周多 4 条", rows, null);
        list.setSize(520, 352);
        layoutAll(list);
        java.util.List<JPanel> heads = headers(list);
        if (!heads.isEmpty()) {
            JPopupMenu gm = heads.get(0).getComponentPopupMenu();
            gm.setSize(214, 124);
            gm.doLayout();
            shot(gm, gm.getWidth(), gm.getHeight(), "C:\\Users\\chen\\Desktop\\ui-235-head-menu4.png");
        }

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
        if (row != null) {
            final JLabel r2 = row;
            SwingUtilities.invokeAndWait(() -> r2.dispatchEvent(new MouseEvent(r2, MouseEvent.MOUSE_CLICKED,
                    System.currentTimeMillis(), 0, 6, 6, 1, false, MouseEvent.BUTTON1)));
            Thread.sleep(300);
            JLabel tip = (JLabel) listBox.getComponent(0);
            final JLabel t2 = tip;
            SwingUtilities.invokeAndWait(() -> t2.dispatchEvent(new MouseEvent(t2, MouseEvent.MOUSE_CLICKED,
                    System.currentTimeMillis(), 0, 6, 6, 1, false, MouseEvent.BUTTON1)));
            Thread.sleep(300);   // 现在是“已退回 · 点我再勾上”那一态
        }
        Field bodyF = MiniBoard.class.getDeclaredField("body");
        bodyF.setAccessible(true);
        JComponent body = (JComponent) bodyF.get(board);
        Method bh = MiniBoard.class.getDeclaredMethod("boardHeight");
        bh.setAccessible(true);
        int hgt = (Integer) bh.invoke(board);
        body.setSize(254, hgt);
        layoutAll(body);
        shot(body, 254, hgt, "C:\\Users\\chen\\Desktop\\ui-236-mini-undone.png");
        Store.writeLines("todo.txt", Arrays.asList(DATA));
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
            if (!(h.getBorder() instanceof CompoundBorder)) continue;
            if (h.getMaximumSize() == null) continue;
            if (h.getMaximumSize().height != 22 && h.getMaximumSize().height != 28) continue;
            out.add(h);
        }
        return out;
    }

    static void shot(Component c, int w, int h, String path) throws Exception {
        BufferedImage img = new BufferedImage(Math.max(1, w), Math.max(1, h), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Theme.panelBg());
        g.fillRect(0, 0, w, h);
        c.paint(g);
        g.dispose();
        ImageIO.write(img, "png", new File(path));
        System.out.println("saved " + path);
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
