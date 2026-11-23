import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** 出设置页整面板图，核对「导航排序」里设置固定行文案不被裁 */
public class ShotSettings {
    public static void main(String[] a) throws Exception {
        SwingUtilities.invokeAndWait(() -> com.toolbox.pc.Main.main(new String[0]));
        Thread.sleep(2500);
        Class<?> mc = Class.forName("com.toolbox.pc.Main");
        Field selfF = mc.getDeclaredField("self");
        selfF.setAccessible(true);
        Object main = selfF.get(null);
        Method page = mc.getDeclaredMethod("page", Class.class);
        page.setAccessible(true);
        Object st = page.invoke(main, Class.forName("com.toolbox.pc.SettingsPanel"));
        JPanel panel = (JPanel) st;
        panel.setDoubleBuffered(false);
        if (panel.getParent() != null) panel.getParent().remove(panel);
        panel.setSize(660, 900);
        layoutAll(panel);
        BufferedImage img = new BufferedImage(660, 900, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 660, 900);
        panel.paint(g);
        g.dispose();
        File out = new File("C:\\Users\\chen\\Desktop\\ui-255-settings-order.png");
        javax.imageio.ImageIO.write(img, "png", out);
        System.out.println("SHOT " + out.length());
        Runtime.getRuntime().halt(0);
    }

    static void layoutAll(Component c) {
        c.doLayout();
        if (c instanceof Container) for (Component k : ((Container) c).getComponents()) layoutAll(k);
    }
}
