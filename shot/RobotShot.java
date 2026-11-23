import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

/** 真实屏幕截图：确认窗口在真机上的渲染，而不是离屏重绘 */
public class RobotShot {
    public static void main(String[] a) throws Exception {
        Robot r = new Robot();
        Rectangle sc = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        Thread.sleep(800);
        BufferedImage img = r.createScreenCapture(sc);
        ImageIO.write(img, "png", new File("C:\\Users\\chen\\Desktop\\ui-237-screen.png"));
        System.out.println("screen=" + sc.width + "x" + sc.height);
        Runtime.getRuntime().halt(0);
    }
}
