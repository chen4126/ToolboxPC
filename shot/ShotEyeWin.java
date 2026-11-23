import javax.swing.SwingUtilities;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** 制造一次「到 40 分钟」的提醒，截真机全屏看休息弹窗长什么样 */
public class ShotEyeWin {

    public static void main(String[] a) throws Exception {
        SwingUtilities.invokeAndWait(() -> com.toolbox.pc.Main.main(new String[0]));
        Thread.sleep(3000);

        Class<?> ec = Class.forName("com.toolbox.pc.EyeCare");
        Field fEye = f(ec, "eyeMs");
        Field fLastActive = f(ec, "lastActive");
        Field fLastTick = f(ec, "lastTick");
        Field fRemind = f(ec, "reminding");
        Field fOn = f(ec, "on");
        Method tick = ec.getDeclaredMethod("tick", long.class, boolean.class);
        tick.setAccessible(true);
        Method close = ec.getDeclaredMethod("closeRestWindow");
        close.setAccessible(true);

        fOn.setBoolean(null, true);
        fRemind.setBoolean(null, false);
        long now = System.currentTimeMillis();
        fEye.setLong(null, 40L * 60000);
        fLastActive.setLong(null, now);
        fLastTick.setLong(null, now);
        tick.invoke(null, now + 1000, true);

        Thread.sleep(1500);
        try {
            Robot robot = new Robot();
            Rectangle screen = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            BufferedImage img = robot.createScreenCapture(screen);
            File out = new File("C:\\Users\\chen\\Desktop\\ui-257-eye-popup.png");
            javax.imageio.ImageIO.write(img, "png", out);
            System.out.println("SHOT " + out.length() + " " + img.getWidth() + "x" + img.getHeight());
        } catch (Exception e) {
            System.out.println("SHOT-FAIL " + e);
        }
        close.invoke(null);
        Runtime.getRuntime().halt(0);
    }

    static Field f(Class<?> c, String n) throws Exception {
        Field x = c.getDeclaredField(n);
        x.setAccessible(true);
        return x;
    }
}
