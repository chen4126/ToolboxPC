import com.toolbox.pc.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** 离屏渲染出图：桌面被置顶的抖音窗口压住，Robot 截不到干净的主窗，改用同一 JVM 的整窗绘制 */
public class Shot69 {

    public static void main(String[] a) throws Exception {
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
            } catch (Exception ignored) {
            }
        });
        Thread.sleep(1000);

        paint(frame, 1140, 760, "C:\\Users\\chen\\Desktop\\ui-242-overview-final.png");
        paint(frame, 1000, 700, "C:\\Users\\chen\\Desktop\\ui-244-overview-narrow-final.png");

        SwingUtilities.invokeAndWait(() -> {
            try {
                sel.invoke(main, "stats");
            } catch (Exception ignored) {
            }
        });
        Thread.sleep(1200);
        paint(frame, 1140, 760, "C:\\Users\\chen\\Desktop\\ui-247-stats-final.png");

        Class<?> mb = Class.forName("com.toolbox.pc.MiniBoard");
        Field instF = mb.getDeclaredField("inst");
        instF.setAccessible(true);
        Object inst = instF.get(null);
        Method show = mb.getDeclaredMethod("show");
        if (inst == null) {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    show.invoke(null);
                } catch (Exception ignored) {
                }
            });
            Thread.sleep(1200);
            inst = instF.get(null);
        }
        if (inst != null) {
            Field wf = mb.getDeclaredField("win");
            wf.setAccessible(true);
            JFrame win = (JFrame) wf.get(inst);
            paint(win, win.getWidth(), win.getHeight(), "C:\\Users\\chen\\Desktop\\ui-243-mini-final.png");
        }
        Runtime.getRuntime().halt(0);
    }

    static void paint(JFrame frame, int w, int h, String path) throws Exception {
        final int fw = w, fh = h;
        SwingUtilities.invokeAndWait(() -> frame.setSize(fw, fh));
        Thread.sleep(700);
        JRootPane root = frame.getRootPane();
        SwingUtilities.invokeAndWait(() -> {
            root.setSize(fw, fh);
            layoutAll(root);
        });
        Thread.sleep(300);
        BufferedImage img = new BufferedImage(fw, fh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, fw, fh);
        SwingUtilities.invokeAndWait(() -> root.paint(g));
        g.dispose();
        ImageIO.write(img, "png", new File(path));
        System.out.println("saved " + path + " " + fw + "x" + fh);
    }

    static void layoutAll(Container c) {
        c.doLayout();
        for (Component k : c.getComponents()) {
            if (k instanceof Container) layoutAll((Container) k);
        }
    }
}
