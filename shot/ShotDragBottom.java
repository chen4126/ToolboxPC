import com.toolbox.pc.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** 真机核对：设置页滚到最底部，看"设置（固定在左下最底部…）"这行有没有被压扁 */
public class ShotDragBottom {

    public static void main(String[] a) throws Exception {
        SwingUtilities.invokeAndWait(() -> Main.main(new String[0]));
        Thread.sleep(2600);
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
                sel.invoke(main, "settings");
            } catch (Exception ignored) {
            }
        });
        Thread.sleep(900);

        Method pageM = mc.getDeclaredMethod("page", Class.class);
        pageM.setAccessible(true);
        Object panel = pageM.invoke(main, Class.forName("com.toolbox.pc.SettingsPanel"));
        Field boxF = panel.getClass().getDeclaredField("navOrderBox");
        boxF.setAccessible(true);
        final JPanel box = (JPanel) boxF.get(panel);

        JScrollPane sp = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, box);
        if (sp != null) {
            Component view = sp.getViewport().getView();
            Rectangle r = SwingUtilities.convertRectangle(box.getParent(),
                    new Rectangle(0, box.getHeight() - 120, box.getWidth(), 120), view);
            ((JComponent) view).scrollRectToVisible(r);
            Thread.sleep(400);
        }
        Thread.sleep(600);

        System.out.println("boxH=" + box.getHeight());
        for (Component c : box.getComponents()) {
            if (!(c instanceof JPanel)) continue;
            System.out.println("y=" + c.getY() + " h=" + c.getHeight()
                    + " navIdx=" + (((JPanel) c).getClientProperty("navIdx")));
        }

        Rectangle b = frame.getBounds();
        BufferedImage img = new Robot().createScreenCapture(b);
        ImageIO.write(img, "png", new File("C:\\Users\\chen\\Desktop\\ui-260-drag-bottom.png"));
        System.out.println("SHOT " + new File("C:\\Users\\chen\\Desktop\\ui-260-drag-bottom.png").length());
        Runtime.getRuntime().halt(0);
    }
}
