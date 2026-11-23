import com.toolbox.pc.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** v1.54 验收：本月完成数、另有 N 条可点、归档弹窗键盘选中态 */
public class Shot54 {

    public static void main(String[] a) throws Exception {
        try {
            run();
        } catch (Throwable t) {
            t.printStackTrace();
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

        /* 1. 整窗：本周2 · 上周1 · 本月3 + 另有 1 条 11-02（可点） */
        SwingUtilities.invokeAndWait(() -> frame.getContentPane().doLayout());
        layoutAll(frame.getContentPane());
        shot(frame.getContentPane(),
                frame.getContentPane().getWidth(), frame.getContentPane().getHeight(),
                "C:\\Users\\chen\\Desktop\\ui-121-month-and-rest.png");

        /* 2. 归档弹窗：键盘选中第一行（浅蓝底） */
        Method apM = OverviewPanel.class.getDeclaredMethod("archivePanel", JDialog.class, int[].class);
        apM.setAccessible(true);
        JPanel panel = (JPanel) apM.invoke(ov, null, new int[]{-1});
        panel.setSize(680, 400);
        layoutAll(panel);
        Method mvM = OverviewPanel.class.getDeclaredMethod("moveArchSel", int.class);
        mvM.setAccessible(true);
        mvM.invoke(ov, 1);
        layoutAll(panel);
        shot(panel, 680, 400, "C:\\Users\\chen\\Desktop\\ui-122-archive-keyboard.png");

        /* 3. 点"另有 1 条 11-02"后：列表切到 11-02 那天 */
        Field fbF = OverviewPanel.class.getDeclaredField("futureBar");
        fbF.setAccessible(true);
        JPanel futureBar = (JPanel) fbF.get(ov);
        JLabel rest = null;
        for (Component c : futureBar.getComponents()) {
            if (c instanceof JLabel && ((JLabel) c).getText().startsWith("另有 ")) {
                rest = (JLabel) c;
                break;
            }
        }
        final JLabel rf = rest;
        if (rf != null) {
            SwingUtilities.invokeAndWait(() -> {
                MouseEvent e = new MouseEvent(rf, MouseEvent.MOUSE_CLICKED, 0,
                        MouseEvent.BUTTON1_DOWN_MASK, 1, 1, 1, false, MouseEvent.BUTTON1);
                rf.dispatchEvent(e);
            });
            Thread.sleep(700);
        }
        SwingUtilities.invokeAndWait(() -> frame.getContentPane().doLayout());
        layoutAll(frame.getContentPane());
        shot(frame.getContentPane(),
                frame.getContentPane().getWidth(), frame.getContentPane().getHeight(),
                "C:\\Users\\chen\\Desktop\\ui-123-rest-click.png");
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

    static <T extends Component> T find(Component c, Class<T> type) {
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
