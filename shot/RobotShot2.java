import com.toolbox.pc.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** 真实屏幕截图：统计页 + 概览，确认真机 DPI 下有没有文字截断 */
public class RobotShot2 {

    public static void main(String[] a) throws Exception {
        SwingUtilities.invokeAndWait(() -> Main.main(new String[0]));
        Thread.sleep(3000);
        Class<?> mc = Class.forName("com.toolbox.pc.Main");
        Field selfF = mc.getDeclaredField("self");
        selfF.setAccessible(true);
        Object main = selfF.get(null);
        Field ff = mc.getDeclaredField("frame");
        ff.setAccessible(true);
        JFrame frame = (JFrame) ff.get(main);
        frame.setLocation(40, 40);
        frame.setSize(1180, 760);

        Method sel = mc.getDeclaredMethod("selectNav", String.class);
        sel.setAccessible(true);

        SwingUtilities.invokeAndWait(() -> {
            try {
                sel.invoke(main, "stats");
            } catch (Exception ignored) {
            }
        });
        Thread.sleep(2500);
        grab("C:\\Users\\chen\\Desktop\\ui-238-real-stats.png", frame);

        SwingUtilities.invokeAndWait(() -> {
            try {
                sel.invoke(main, "overview");
            } catch (Exception ignored) {
            }
        });
        Thread.sleep(1200);
        grab("C:\\Users\\chen\\Desktop\\ui-239-real-overview.png", frame);
        Runtime.getRuntime().halt(0);
    }

    static void grab(String path, JFrame frame) throws Exception {
        Robot r = new Robot();
        Point p = frame.getLocationOnScreen();
        int w = frame.getWidth(), h = frame.getHeight();
        BufferedImage img = r.createScreenCapture(new Rectangle(p.x, p.y, w, h));
        ImageIO.write(img, "png", new File(path));
        System.out.println("saved " + path + " " + w + "x" + h);
    }
}
