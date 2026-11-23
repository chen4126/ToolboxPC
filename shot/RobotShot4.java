import com.toolbox.pc.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** 真机：概览（普通数据）+ 小窗底栏 */
public class RobotShot4 {

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
        SwingUtilities.invokeAndWait(() -> {
            frame.setLocation(20, 20);
            frame.setAlwaysOnTop(true);
            frame.toFront();
        });
        Method sel = mc.getDeclaredMethod("selectNav", String.class);
        sel.setAccessible(true);
        SwingUtilities.invokeAndWait(() -> {
            try {
                sel.invoke(main, "overview");
            } catch (Exception ignored) {
            }
        });
        Thread.sleep(1500);
        grab("C:\\Users\\chen\\Desktop\\ui-242-overview-final.png", frame);
        SwingUtilities.invokeAndWait(() -> frame.setAlwaysOnTop(false));

        Class<?> mb = Class.forName("com.toolbox.pc.MiniBoard");
        Field instF = mb.getDeclaredField("inst");
        instF.setAccessible(true);
        Object inst = instF.get(null);
        if (inst != null) {
            Field wf = mb.getDeclaredField("win");
            wf.setAccessible(true);
            JFrame win = (JFrame) wf.get(inst);
            SwingUtilities.invokeAndWait(() -> {
                win.setAlwaysOnTop(true);
                win.toFront();
            });
            Thread.sleep(900);
            grab("C:\\Users\\chen\\Desktop\\ui-243-mini-final.png", win);
        } else {
            System.out.println("mini not open");
        }
        Runtime.getRuntime().halt(0);
    }

    static void grab(String path, JFrame frame) throws Exception {
        Robot r = new Robot();
        Point p = frame.getLocationOnScreen();
        BufferedImage img = r.createScreenCapture(new Rectangle(p.x, p.y, frame.getWidth(), frame.getHeight()));
        ImageIO.write(img, "png", new File(path));
        System.out.println("saved " + path + " " + frame.getWidth() + "x" + frame.getHeight());
    }
}
