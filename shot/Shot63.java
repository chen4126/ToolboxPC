import com.toolbox.pc.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** v1.63 验收：待办卡标题行、节奏行徽标、清单分组 */
public class Shot63 {

    static OverviewPanel ov;

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
        Object main = selfF.get(null);
        Field ff = mc.getDeclaredField("frame");
        ff.setAccessible(true);
        JFrame frame = (JFrame) ff.get(main);
        Method sel = mc.getDeclaredMethod("selectNav", String.class);
        sel.setAccessible(true);
        SwingUtilities.invokeAndWait(() -> {
            try {
                sel.invoke(main, "overview");
            } catch (Exception e) {
            }
        });
        Thread.sleep(1400);
        ov = find(frame.getContentPane(), OverviewPanel.class);
        Method refresh = OverviewPanel.class.getDeclaredMethod("refresh");
        refresh.setAccessible(true);
        refresh.invoke(ov);
        Thread.sleep(400);

        Container root = frame.getContentPane();
        root.doLayout();
        layoutAll(root);
        shot(root, root.getWidth(), root.getHeight(), "C:\\Users\\chen\\Desktop\\ui-211-full.png");

        BufferedImage full = render(root);
        Field ttF = OverviewPanel.class.getDeclaredField("todoTitle");
        ttF.setAccessible(true);
        JLabel todoTitle = (JLabel) ttF.get(ov);
        Rectangle tr = SwingUtilities.convertRectangle(todoTitle, todoTitle.getBounds(), root);
        Rectangle headRect = new Rectangle(Math.max(0, tr.x - 14), Math.max(0, tr.y - 6), 380, 24);
        crop(full, headRect, "C:\\Users\\chen\\Desktop\\ui-212-head.png", 380, 24);

        Field paceF = OverviewPanel.class.getDeclaredField("weekDots");
        paceF.setAccessible(true);
        JPanel dots = (JPanel) paceF.get(ov);
        Rectangle paceRect = SwingUtilities.convertRectangle(dots.getParent().getParent(),
                dots.getParent().getBounds(), root);
        crop(full, paceRect, "C:\\Users\\chen\\Desktop\\ui-213-pace.png", paceRect.width, paceRect.height);

        /* 清单：按天分组 */
        Method dlM = OverviewPanel.class.getDeclaredMethod("doneListPanel",
                String.class, String.class, List.class, JDialog.class);
        dlM.setAccessible(true);
        List month = rowsOf();
        JPanel list = (JPanel) dlM.invoke(ov, "本月完成 · 共 5 条", "本周 3 条 · 上周 2 条 · 本月 5 条 · 比上周多 1 条", month, null);
        Method gcM = OverviewPanel.class.getDeclaredMethod("doneGroupCount", List.class);
        gcM.setAccessible(true);
        int groups = (int) gcM.invoke(ov, month);
        int hh = Math.min(460, 176 + month.size() * 28 + Math.max(0, groups - 1) * 12);
        list.setSize(520, hh);
        layoutAll(list);
        shot(list, 520, hh, "C:\\Users\\chen\\Desktop\\ui-214-groups.png");
    }

    static void crop(BufferedImage full, Rectangle r, String path, int w, int h) throws Exception {
        int cw = Math.min(w, full.getWidth() - r.x);
        int ch = Math.min(h, full.getHeight() - r.y);
        ImageIO.write(full.getSubimage(r.x, r.y, Math.max(1, cw), Math.max(1, ch)), "png", new File(path));
        System.out.println("saved " + path + " " + cw + "x" + ch);
    }

    static List<?> rowsOf() throws Exception {
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

    static BufferedImage render(Component c) {
        BufferedImage img = new BufferedImage(Math.max(1, c.getWidth()), Math.max(1, c.getHeight()),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, c.getWidth(), c.getHeight());
        c.paint(g);
        g.dispose();
        return img;
    }

    static void shot(Component c, int w, int h, String path) throws Exception {
        BufferedImage img = new BufferedImage(Math.max(1, w), Math.max(1, h), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        c.paint(g);
        g.dispose();
        ImageIO.write(img, "png", new File(path));
        System.out.println("saved " + path + " " + w + "x" + h);
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
