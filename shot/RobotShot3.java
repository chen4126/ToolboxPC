import com.toolbox.pc.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;

/** 默认窗口尺寸下截图概览，核对快捷入口卡片里的数值/说明是否被裁 */
public class RobotShot3 {

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
        System.out.println("window=" + frame.getWidth() + "x" + frame.getHeight());
        grab("C:\\Users\\chen\\Desktop\\ui-240-quick-default.png", frame);

        // 再压窄一档，看窄窗口下说明行还在不在
        SwingUtilities.invokeAndWait(() -> frame.setSize(1000, 700));
        Thread.sleep(1200);
        grab("C:\\Users\\chen\\Desktop\\ui-241-quick-narrow.png", frame);

        // 每个卡片里三个标签实际占位
        Object page = mc.getDeclaredMethod("page", Class.class).invoke(main, Class.forName("com.toolbox.pc.OverviewPanel"));
        Field vf = page.getClass().getDeclaredField("values");
        vf.setAccessible(true);
        JLabel[] vals = (JLabel[]) vf.get(page);
        Field sf = page.getClass().getDeclaredField("subs");
        sf.setAccessible(true);
        JLabel[] subs = (JLabel[]) sf.get(page);
        for (int i = 0; i < 6; i++) {
            JLabel v = vals[i], s = subs[i];
            System.out.println(i + " value=[" + v.getText() + "] size=" + v.getWidth() + "x" + v.getHeight()
                    + " needW=" + v.getPreferredSize().width + " needH=" + v.getPreferredSize().height
                    + " | sub=[" + s.getText() + "] size=" + s.getWidth() + "x" + s.getHeight()
                    + " needW=" + s.getPreferredSize().width);
        }
        Runtime.getRuntime().halt(0);
    }

    static void grab(String path, JFrame frame) throws Exception {
        Robot r = new Robot();
        Point p = frame.getLocationOnScreen();
        BufferedImage img = r.createScreenCapture(new Rectangle(p.x, p.y, frame.getWidth(), frame.getHeight()));
        ImageIO.write(img, "png", new File(path));
        System.out.println("saved " + path);
    }
}
