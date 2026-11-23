import com.toolbox.pc.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** v1.60 验收：概览整窗 + 待办卡局部（底部完成节奏行） */
public class Shot60 {

    public static void main(String[] a) {
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
        Thread.sleep(500);

        frame.getContentPane().doLayout();
        layoutAll(frame.getContentPane());
        Container root = frame.getContentPane();
        shot(root, root.getWidth(), root.getHeight(),
                "C:\\Users\\chen\\Desktop\\ui-181-overview-pace.png");

        Field paceF = OverviewPanel.class.getDeclaredField("paceBar");
        paceF.setAccessible(true);
        JLabel pace = (JLabel) paceF.get(ov);
        Container card = (Container) pace.getParent().getParent();
        Rectangle r = SwingUtilities.convertRectangle(card.getParent(), card.getBounds(), root);
        BufferedImage full = render(root);
        int w = Math.min(r.width, full.getWidth() - r.x);
        int h = Math.min(r.height, full.getHeight() - r.y);
        BufferedImage crop = full.getSubimage(r.x, r.y, Math.max(1, w), Math.max(1, h));
        ImageIO.write(crop, "png", new File("C:\\Users\\chen\\Desktop\\ui-182-todo-card-pace.png"));
        System.out.println("saved ui-182 " + w + "x" + h + " @ " + r.x + "," + r.y);
    }

    static BufferedImage render(Component c) {
        BufferedImage img = new BufferedImage(c.getWidth(), c.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, c.getWidth(), c.getHeight());
        c.paint(g);
        g.dispose();
        return img;
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
