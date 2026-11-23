import com.toolbox.pc.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** v1.34 验收图：归档后概览 / 小窗只看过期 / 挪完的撤销入口 */
public class Shot34 {

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
        SwingUtilities.invokeAndWait(() -> {
            try {
                sel.invoke(holder[0], "overview");
            } catch (Exception e) {
            }
        });
        Thread.sleep(1200);

        OverviewPanel ov = find(frame.getContentPane(), OverviewPanel.class);
        Method refresh = OverviewPanel.class.getDeclaredMethod("refresh");
        refresh.setAccessible(true);
        SwingUtilities.invokeAndWait(() -> {
            try {
                refresh.invoke(ov);
            } catch (Exception e) {
            }
        });
        Thread.sleep(500);
        shot(frame.getContentPane(), frame.getContentPane().getWidth(),
                frame.getContentPane().getHeight(), "C:\\Users\\chen\\Desktop\\ui-51-archived.png");

        /* 小窗：只看过期 */
        SwingUtilities.invokeAndWait(() -> MiniBoard.toggle());
        Thread.sleep(900);
        Field instF = MiniBoard.class.getDeclaredField("inst");
        instF.setAccessible(true);
        Object mb = instF.get(null);
        Field winF = MiniBoard.class.getDeclaredField("win");
        winF.setAccessible(true);
        JFrame win = (JFrame) winF.get(mb);
        Field ovF = MiniBoard.class.getDeclaredField("overdueView");
        ovF.setAccessible(true);
        Method rf = MiniBoard.class.getDeclaredMethod("refresh");
        rf.setAccessible(true);
        SwingUtilities.invokeAndWait(() -> {
            try {
                ovF.setBoolean(mb, true);
                rf.invoke(mb);
            } catch (Exception e) {
            }
        });
        Thread.sleep(600);
        Container wc = win.getContentPane();
        layoutAll(wc);
        shot(wc, win.getWidth(), win.getHeight(), "C:\\Users\\chen\\Desktop\\ui-52-board-overdue.png");

        SwingUtilities.invokeAndWait(() -> MiniBoard.toggle());
        Thread.sleep(500);

        /* 挪到今天 -> 撤销入口 */
        Method move = OverviewPanel.class.getDeclaredMethod("moveOverdueToToday");
        move.setAccessible(true);
        move.invoke(ov);
        SwingUtilities.invokeAndWait(() -> {
            try {
                refresh.invoke(ov);
            } catch (Exception e) {
            }
        });
        Thread.sleep(600);
        Field fbF = OverviewPanel.class.getDeclaredField("futureBar");
        fbF.setAccessible(true);
        Component bar = (Component) fbF.get(ov);
        Component south = bar.getParent();
        Component card = south.getParent();
        layoutAll(frame.getContentPane());
        shot(card, card.getWidth(), card.getHeight(),
                "C:\\Users\\chen\\Desktop\\ui-53-undo-chip.png");
        System.exit(0);
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
