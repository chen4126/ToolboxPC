import com.toolbox.pc.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** 真机核对：设置页导航排序整块可见 + 按住拖动时的高亮（Robot 截真实窗口） */
public class ShotDragFinal {

    static JPanel box;
    static List<JPanel> rows = new ArrayList<JPanel>();

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
        Thread.sleep(1000);

        Method pageM = mc.getDeclaredMethod("page", Class.class);
        pageM.setAccessible(true);
        Object panel = pageM.invoke(main, Class.forName("com.toolbox.pc.SettingsPanel"));
        Field boxF = panel.getClass().getDeclaredField("navOrderBox");
        boxF.setAccessible(true);
        box = (JPanel) boxF.get(panel);

        JScrollPane sp = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, box);
        if (sp != null) {
            Component view = sp.getViewport().getView();
            Point p0 = SwingUtilities.convertPoint(box, 0, 0, view);
            JScrollBar bar = sp.getVerticalScrollBar();
            bar.setValue(Math.max(0, Math.min(bar.getMaximum(), p0.y - 8)));
            Thread.sleep(500);
        }
        Thread.sleep(600);

        for (Component c : box.getComponents()) {
            if (!(c instanceof JPanel)) continue;
            if (((JPanel) c).getClientProperty("navIdx") != null) rows.add((JPanel) c);
        }
        System.out.println("rows=" + rows.size() + " boxY=" + box.getY());
        for (int i = 0; i < rows.size(); i++) {
            System.out.println("row" + i + " y=" + rows.get(i).getY() + " h=" + rows.get(i).getHeight());
        }
        for (Component c : box.getComponents()) {
            if (!(c instanceof JPanel)) continue;
            if (((JPanel) c).getClientProperty("navIdx") == null) {
                System.out.println("pin y=" + c.getY() + " h=" + c.getHeight());
            }
        }

        if (rows.size() >= 3) {
            JPanel first = rows.get(0);
            Point in1 = SwingUtilities.convertPoint(box, new Point(5, 10), first);
            first.dispatchEvent(new MouseEvent(first, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
                    MouseEvent.BUTTON1_DOWN_MASK, in1.x, in1.y, 1, false, MouseEvent.BUTTON1));
            int yMid = rows.get(2).getY() + rows.get(2).getHeight() / 2;
            Point in2 = SwingUtilities.convertPoint(box, new Point(5, yMid), first);
            first.dispatchEvent(new MouseEvent(first, MouseEvent.MOUSE_DRAGGED, System.currentTimeMillis(),
                    MouseEvent.BUTTON1_DOWN_MASK, in2.x, in2.y, 1, false, MouseEvent.BUTTON1));
            Thread.sleep(300);
        }

        Thread.sleep(200);
        Rectangle b = frame.getBounds();
        BufferedImage img = new Robot().createScreenCapture(b);
        ImageIO.write(img, "png", new File("C:\\Users\\chen\\Desktop\\ui-258-drag-order.png"));
        System.out.println("SHOT " + new File("C:\\Users\\chen\\Desktop\\ui-258-drag-order.png").length()
                + " " + img.getWidth() + "x" + img.getHeight());
        Runtime.getRuntime().halt(0);
    }
}
