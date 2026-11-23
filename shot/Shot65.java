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

/** v1.65 验收：节奏行“空 N 天”、分组占比、小窗节奏行右键 */
public class Shot65 {

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
        Method rf = OverviewPanel.class.getDeclaredMethod("refresh");
        rf.setAccessible(true);
        rf.invoke(ov);
        Thread.sleep(300);
        Container root = frame.getContentPane();
        layoutAll(root);
        shot(root, root.getWidth(), root.getHeight(), "C:\\Users\\chen\\Desktop\\ui-224-overview.png");

        Method dlM = OverviewPanel.class.getDeclaredMethod("doneListPanel",
                String.class, String.class, List.class, JDialog.class);
        dlM.setAccessible(true);
        List rows = monthRows();
        JPanel list = (JPanel) dlM.invoke(ov, "本月完成 · 共 5 条", "本周 3 条 · 上周 2 条 · 本月 5 条 · 比上周多 1 条", rows, null);
        list.setSize(520, 352);
        layoutAll(list);
        shot(list, 520, 352, "C:\\Users\\chen\\Desktop\\ui-225-groups-pct.png");

        MiniBoard.show();
        Thread.sleep(700);
        Class<?> mbc = Class.forName("com.toolbox.pc.MiniBoard");
        Field instF = mbc.getDeclaredField("inst");
        instF.setAccessible(true);
        MiniBoard board = (MiniBoard) instF.get(null);
        Field bodyF = MiniBoard.class.getDeclaredField("body");
        bodyF.setAccessible(true);
        JComponent body = (JComponent) bodyF.get(board);
        Method bh = MiniBoard.class.getDeclaredMethod("boardHeight");
        bh.setAccessible(true);
        int hgt = (Integer) bh.invoke(board);
        body.setSize(254, hgt);
        layoutAll(body);
        shot(body, 254, hgt, "C:\\Users\\chen\\Desktop\\ui-226-mini.png");

        Method pm = MiniBoard.class.getDeclaredMethod("buildPaceMenu");
        pm.setAccessible(true);
        JPopupMenu menu = (JPopupMenu) pm.invoke(board);
        menu.setSize(178, menu.getComponentCount() * 26 + 8);
        menu.doLayout();
        shot(menu, menu.getWidth(), menu.getHeight(), "C:\\Users\\chen\\Desktop\\ui-227-mini-menu.png");
        System.out.println("menu items=" + menu.getComponentCount());
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
