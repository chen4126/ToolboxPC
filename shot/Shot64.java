import com.toolbox.pc.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** v1.64 验收：统计页角标、清单分组折叠 */
public class Shot64 {

    static OverviewPanel ov;
    static JFrame frame;
    static Object main;

    public static void main(String[] a) {
        try {
            run();
        } catch (Throwable t) {
            t.printStackTrace();
        }
        Runtime.getRuntime().halt(0);
    }

    static void run() throws Exception {
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
        Method ovRefresh = OverviewPanel.class.getDeclaredMethod("refresh");
        ovRefresh.setAccessible(true);
        ovRefresh.invoke(ov);
        Thread.sleep(300);
        Container root = frame.getContentPane();
        layoutAll(root);
        shot(root, root.getWidth(), root.getHeight(), "C:\\Users\\chen\\Desktop\\ui-220-overview.png");

        go("stats");
        Thread.sleep(900);
        StatsPanel sp = find(frame.getContentPane(), StatsPanel.class);
        Method rf = StatsPanel.class.getDeclaredMethod("refresh");
        rf.setAccessible(true);
        rf.invoke(sp);
        Thread.sleep(300);
        layoutAll(root);
        shot(root, root.getWidth(), root.getHeight(), "C:\\Users\\chen\\Desktop\\ui-221-stats.png");

        go("overview");
        Thread.sleep(700);

        Method dlM = OverviewPanel.class.getDeclaredMethod("doneListPanel",
                String.class, String.class, List.class, JDialog.class);
        dlM.setAccessible(true);
        List rows = monthRows();
        JPanel list = (JPanel) dlM.invoke(ov, "本月完成 · 共 5 条", "本周 3 条 · 上周 2 条 · 本月 5 条 · 比上周多 1 条", rows, null);
        list.setSize(520, 352);
        layoutAll(list);
        shot(list, 520, 352, "C:\\Users\\chen\\Desktop\\ui-222-groups-open.png");

        java.util.List<JPanel> heads = headers(list);
        if (!heads.isEmpty()) {
            heads.get(0).dispatchEvent(new MouseEvent(heads.get(0), MouseEvent.MOUSE_CLICKED,
                    System.currentTimeMillis(), 0, 6, 6, 1, false, MouseEvent.BUTTON1));
            Thread.sleep(200);
            layoutAll(list);
        }
        shot(list, 520, 352, "C:\\Users\\chen\\Desktop\\ui-223-groups-folded.png");
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

    static java.util.List<JPanel> headers(JPanel p) {
        java.util.List<JPanel> out = new java.util.ArrayList<JPanel>();
        for (Component c : all(p)) {
            if (!(c instanceof JPanel)) continue;
            JPanel h = (JPanel) c;
            if (!(h.getBorder() instanceof javax.swing.border.CompoundBorder)) continue;
            if (h.getMaximumSize() == null) continue;
            int mh = h.getMaximumSize().height;
            if (mh != 22 && mh != 28) continue;
            out.add(h);
        }
        return out;
    }

    static List<?> monthRows() throws Exception {
        java.time.LocalDate now = java.time.LocalDate.now();
        java.time.LocalDate from = now.withDayOfMonth(1);
        java.time.LocalDate to = now.withDayOfMonth(now.lengthOfMonth());
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
            if (d.isBefore(from) || d.isAfter(to)) continue;
            out.add(new String[]{text, d.toString()});
        }
        java.util.Collections.sort(out, (x, y) -> y[1].compareTo(x[1]));
        return out;
    }

    static void shot(Component c, int w, int h, String path) throws Exception {
        BufferedImage img = new BufferedImage(Math.max(1, w), Math.max(1, h), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        c.paint(g);
        g.dispose();
        ImageIO.write(img, "png", new File(path));
        System.out.println("saved " + path + " " + w + "x" + h);
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
