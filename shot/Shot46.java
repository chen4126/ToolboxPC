import com.toolbox.pc.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** v1.46 三项验收：无日期入口、归档右键三项、点了之后的多选状态 */
public class Shot46 {

    public static void main(String[] a) throws Exception {
        try {
            run();
        } catch (Throwable t) {
            t.printStackTrace();
            System.out.flush();
        }
        Runtime.getRuntime().halt(0);
    }

    static void run() throws Exception {
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
            } catch (Exception e) {
            }
        });
        Thread.sleep(1400);

        OverviewPanel ov = find(frame.getContentPane(), OverviewPanel.class);
        Method refresh = OverviewPanel.class.getDeclaredMethod("refresh");
        refresh.setAccessible(true);
        refresh.invoke(ov);
        Thread.sleep(600);

        Method ufM = OverviewPanel.class.getDeclaredMethod("updateFuture");
        ufM.setAccessible(true);
        ufM.invoke(ov);
        Thread.sleep(400);
        SwingUtilities.invokeAndWait(() -> frame.getContentPane().doLayout());
        layoutAll(frame.getContentPane());
        shot(frame.getContentPane(),
                frame.getContentPane().getWidth(), frame.getContentPane().getHeight(),
                "C:\\Users\\chen\\Desktop\\ui-90-nodate-chip.png");

        Field fbF = OverviewPanel.class.getDeclaredField("futureBar");
        fbF.setAccessible(true);
        JPanel futureBar = (JPanel) fbF.get(ov);
        JLabel noDate = null;
        for (Component c : futureBar.getComponents()) {
            if (c instanceof JLabel && ((JLabel) c).getText() != null
                    && ((JLabel) c).getText().startsWith("无日期")) {
                noDate = (JLabel) c;
            }
        }
        final JLabel nd = noDate;
        if (nd != null) {
            java.awt.event.MouseEvent click = new java.awt.event.MouseEvent(nd,
                    java.awt.event.MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
                    0, 10, 10, 1, false, java.awt.event.MouseEvent.BUTTON1);
            SwingUtilities.invokeAndWait(() -> {
                for (java.awt.event.MouseListener ml : nd.getMouseListeners()) ml.mouseClicked(click);
            });
            Thread.sleep(700);
            SwingUtilities.invokeAndWait(() -> frame.getContentPane().doLayout());
            layoutAll(frame.getContentPane());
            shot(frame.getContentPane(),
                    frame.getContentPane().getWidth(), frame.getContentPane().getHeight(),
                    "C:\\Users\\chen\\Desktop\\ui-91-after-nodate.png");
        }

        /* 归档右键三项 */
        Method apM = OverviewPanel.class.getDeclaredMethod("archivePanel", JDialog.class, int[].class);
        apM.setAccessible(true);
        JPanel panel = (JPanel) apM.invoke(ov, null, new int[]{-1});
        panel.setSize(panel.getPreferredSize());
        layoutAll(panel);
        JLabel target = null;
        for (JLabel l : allLabels(panel)) {
            if (l.getText() != null && l.getText().contains("二十天前")) target = l;
        }
        if (target != null && target.getComponentPopupMenu() != null) {
            JPopupMenu m = target.getComponentPopupMenu();
            m.setSize(m.getPreferredSize());
            m.doLayout();
            shot(m, m.getWidth(), m.getHeight(),
                    "C:\\Users\\chen\\Desktop\\ui-92-arch-menu3.png");
        }
    }

    static java.util.List<JLabel> allLabels(Component root) {
        java.util.List<JLabel> out = new java.util.ArrayList<JLabel>();
        collect(root, out);
        return out;
    }

    static void collect(Component c, java.util.List<JLabel> out) {
        if (c instanceof JLabel) out.add((JLabel) c);
        if (c instanceof Container) {
            for (Component k : ((Container) c).getComponents()) collect(k, out);
        }
    }

    static void shot(Component c, int w, int h, String path) throws Exception {
        BufferedImage img = new BufferedImage(Math.max(1, w), Math.max(1, h), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        c.paint(g);
        g.dispose();
        ImageIO.write(img, "png", new File(path));
        System.out.println("saved " + path + " " + w + "x" + h);
    }

    static void layoutAll(Component c) {
        c.doLayout();
        if (c instanceof Container) for (Component k : ((Container) c).getComponents()) layoutAll(k);
    }

    static <T> T find(Component c, Class<T> type) {
        if (type.isInstance(c)) return type.cast(c);
        if (c instanceof Container) {
            for (Component k : ((Container) c).getComponents()) {
                T t = find(k, type);
                if (t != null) return t;
            }
        }
        return null;
    }
}
