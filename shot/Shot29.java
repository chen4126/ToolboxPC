import com.toolbox.pc.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;

/** v1.29 验收图：概览未来待办行 / 日期选择器 / 待办右键（含指定日期） */
public class Shot29 {

    public static void main(String[] a) throws Exception {
        Theme.init();
        Ui.installDefaults();
        final Object[] holder = new Object[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                Class<?> c = Class.forName("com.toolbox.pc.Main");
                Object m = c.getDeclaredConstructor().newInstance();
                Method create = c.getDeclaredMethod("create");
                create.setAccessible(true);
                create.invoke(m);
                holder[0] = m;
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        Thread.sleep(2300);

        Class<?> mc = Class.forName("com.toolbox.pc.Main");
        Field ff = mc.getDeclaredField("frame");
        ff.setAccessible(true);
        final JFrame frame = (JFrame) ff.get(holder[0]);
        Method sel = mc.getDeclaredMethod("selectNav", String.class);
        sel.setAccessible(true);
        int w = frame.getContentPane().getWidth();
        int h = frame.getContentPane().getHeight();

        /* ---------- 1. 概览：未来 7 天一行 ---------- */
        SwingUtilities.invokeAndWait(() -> {
            try {
                sel.invoke(holder[0], "overview");
            } catch (Exception e) {
            }
        });
        Thread.sleep(1200);
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        frame.getContentPane().paint(g);
        g.dispose();
        ImageIO.write(img, "png", new File("C:\\Users\\chen\\Desktop\\ui-36-todo-future.png"));
        System.out.println("saved ui-36 " + w + "x" + h);

        /* ---------- 2. 日期选择器（离屏） ---------- */
        Class<?> dp = Class.forName("com.toolbox.pc.DatePicker");
        Method build = dp.getDeclaredMethod("build", java.awt.Window.class, LocalDate.class, LocalDate[].class);
        build.setAccessible(true);
        LocalDate[] out = {null};
        JDialog dlg = (JDialog) build.invoke(null, null, LocalDate.of(2026, 11, 10), out);
        Container root = dlg.getContentPane();
        root.setSize(root.getPreferredSize());
        layoutAll(root);
        int w2 = root.getWidth(), h2 = root.getHeight();
        BufferedImage img2 = new BufferedImage(w2, h2, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img2.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, w2, h2);
        root.paint(g2);
        g2.dispose();
        ImageIO.write(img2, "png", new File("C:\\Users\\chen\\Desktop\\ui-37-date-picker.png"));
        System.out.println("saved ui-37 " + w2 + "x" + h2);

        /* ---------- 3. 待办右键菜单（6 项 + 子菜单 6 项） ---------- */
        OverviewPanel ov = find(frame.getContentPane(), OverviewPanel.class);
        Field tbF = OverviewPanel.class.getDeclaredField("todoBox");
        tbF.setAccessible(true);
        JPanel box = (JPanel) tbF.get(ov);
        JPanel row0 = (JPanel) box.getComponent(0);
        JCheckBox cb = (JCheckBox) row0.getComponent(0);
        JPopupMenu menu = cb.getComponentPopupMenu();
        menu.setSize(menu.getPreferredSize());
        menu.doLayout();
        for (Component c : menu.getComponents()) if (c instanceof JComponent) ((JComponent) c).doLayout();
        BufferedImage img3 = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g3 = img3.createGraphics();
        g3.setColor(Color.WHITE);
        g3.fillRect(0, 0, w, h);
        frame.getContentPane().paint(g3);
        Point pt = SwingUtilities.convertPoint(cb, 0, 0, frame.getContentPane());
        int mx = pt.x + 30, my = pt.y - menu.getHeight() - 8;
        if (my < 60) my = pt.y + 18;
        g3.translate(mx, my);
        menu.paint(g3);
        g3.translate(-mx, -my);
        JMenu defer = null;
        for (Component c : menu.getComponents()) if (c instanceof JMenu) defer = (JMenu) c;
        if (defer != null) {
            JPopupMenu sub = defer.getPopupMenu();
            sub.setSize(sub.getPreferredSize());
            sub.doLayout();
            for (Component c : sub.getComponents()) if (c instanceof JComponent) ((JComponent) c).doLayout();
            int sx = mx + menu.getWidth() - 3;
            int sy = my + defer.getY() + 10;
            g3.translate(sx, sy);
            sub.paint(g3);
            g3.translate(-sx, -sy);
            System.out.println("submenu " + sub.getComponentCount() + " items at " + sx + "," + sy);
        }
        g3.dispose();
        ImageIO.write(img3, "png", new File("C:\\Users\\chen\\Desktop\\ui-38-todo-pick.png"));
        System.out.println("saved ui-38");
        System.exit(0);
    }

    static void layoutAll(Component c) {
        c.doLayout();
        if (c instanceof Container) {
            for (Component k : ((Container) c).getComponents()) layoutAll(k);
        }
    }

    static <T> T find(Component c, Class<T> type) {
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
