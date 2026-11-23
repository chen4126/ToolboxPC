import com.toolbox.pc.*;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import javax.imageio.ImageIO;

/** 统计页 30 天区间截图 */
public class TestStats30 {
    public static void main(String[] args) throws Exception {
        Theme.init();
        Ui.installDefaults();
        final JFrame f = new JFrame("stats30");
        SwingUtilities.invokeAndWait(() -> {
            try {
                StatsPanel p = new StatsPanel();
                Field r30 = StatsPanel.class.getDeclaredField("range30");
                r30.setAccessible(true);
                ((JToggleButton) r30.get(p)).setSelected(true);
                Field rf = StatsPanel.class.getDeclaredField("range");
                rf.setAccessible(true);
                rf.set(p, 30);
                java.lang.reflect.Method refresh = StatsPanel.class.getDeclaredMethod("refresh");
                refresh.setAccessible(true);
                refresh.invoke(p);
                f.setContentPane(p);
                f.setSize(900, 640);
                f.setLocation(120, 120);
                f.setAlwaysOnTop(true);
                f.setVisible(true);
                f.toFront();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        Thread.sleep(900);
        Rectangle b = f.getBounds();
        Robot robot = new Robot();
        BufferedImage img = robot.createScreenCapture(b);
        ImageIO.write(img, "png", new File("C:\\Users\\chen\\Desktop\\ui-stats30.png"));
        System.out.println("saved ui-stats30.png " + img.getWidth() + "x" + img.getHeight());
        System.exit(0);
    }
}
