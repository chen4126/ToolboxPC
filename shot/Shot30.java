import com.toolbox.pc.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.Set;

/** v1.30 验收图：未来标签 / 切到某天 / 日历圆点 */
public class Shot30 {

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

        SwingUtilities.invokeAndWait(() -> {
            try {
                sel.invoke(holder[0], "overview");
            } catch (Exception e) {
            }
        });
        Thread.sleep(1200);
        shot(frame, w, h, "C:\\Users\\chen\\Desktop\\ui-39-future-chips.png");

        /* 切到 11-11 */
        OverviewPanel ov = find(frame.getContentPane(), OverviewPanel.class);
        Method showDate = OverviewPanel.class.getDeclaredMethod("showDate", LocalDate.class);
        showDate.setAccessible(true);
        SwingUtilities.invokeAndWait(() -> {
            try {
                showDate.invoke(ov, LocalDate.of(2026, 11, 11));
            } catch (Exception e) {
            }
        });
        Thread.sleep(700);
        shot(frame, w, h, "C:\\Users\\chen\\Desktop\\ui-40-view-day.png");

        /* 日历圆点 */
        Method marked = OverviewPanel.class.getDeclaredMethod("markedDates");
        marked.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<LocalDate> marks = (Set<LocalDate>) marked.invoke(ov);
        Class<?> dp = Class.forName("com.toolbox.pc.DatePicker");
        Method build = dp.getDeclaredMethod("build", Window.class, LocalDate.class,
                Set.class, LocalDate[].class);
        build.setAccessible(true);
        LocalDate[] out = {null};
        JDialog dlg = (JDialog) build.invoke(null, null, LocalDate.of(2026, 11, 11), marks, out);
        Container root = dlg.getContentPane();
        root.setSize(root.getPreferredSize());
        layoutAll(root);
        int w2 = root.getWidth(), h2 = root.getHeight();
        BufferedImage img = new BufferedImage(w2, h2, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w2, h2);
        root.paint(g);
        g.dispose();
        ImageIO.write(img, "png", new File("C:\\Users\\chen\\Desktop\\ui-41-calendar-dots.png"));
        System.out.println("saved ui-41 " + w2 + "x" + h2);

        System.exit(0);
    }

    static void shot(JFrame frame, int w, int h, String path) throws Exception {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        frame.getContentPane().paint(g);
        g.dispose();
        ImageIO.write(img, "png", new File(path));
        System.out.println("saved " + path);
    }

    static void layoutAll(Component c) {
        c.doLayout();
        if (c instanceof Container) for (Component k : ((Container) c).getComponents()) layoutAll(k);
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
