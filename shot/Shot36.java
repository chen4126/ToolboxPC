import com.toolbox.pc.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** v1.36 验收图：归档天数设置 / 只看某一档 / 小窗待办右键菜单 */
public class Shot36 {

    private static Object main;

    public static void main(String[] a) throws Exception {
        Theme.init();
        Ui.installDefaults();
        SwingUtilities.invokeAndWait(() -> {
            try {
                Class<?> c = Class.forName("com.toolbox.pc.Main");
                Object m = c.getDeclaredConstructor().newInstance();
                Method create = c.getDeclaredMethod("create");
                create.setAccessible(true);
                create.invoke(m);
                main = m;
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        Thread.sleep(2300);

        Class<?> mc = Class.forName("com.toolbox.pc.Main");
        Field ff = mc.getDeclaredField("frame");
        ff.setAccessible(true);
        JFrame frame = (JFrame) ff.get(main);
        Method sel = mc.getDeclaredMethod("selectNav", String.class);
        sel.setAccessible(true);

        /* 1. 设置页：归档天数下拉 */
        SwingUtilities.invokeAndWait(() -> {
            try {
                sel.invoke(main, "settings");
            } catch (Exception e) {
            }
        });
        Thread.sleep(1500);
        shot(frame.getContentPane(), frame.getContentPane().getWidth(),
                frame.getContentPane().getHeight(), "C:\\Users\\chen\\Desktop\\ui-58-settings-archive.png");

        /* 2. 概览：只看"很久没动"这一档 */
        SwingUtilities.invokeAndWait(() -> {
            try {
                sel.invoke(main, "overview");
            } catch (Exception e) {
            }
        });
        Thread.sleep(1200);
        OverviewPanel ov = find(frame.getContentPane(), OverviewPanel.class);
        Method refresh = OverviewPanel.class.getDeclaredMethod("refresh");
        refresh.setAccessible(true);
        SwingUtilities.invokeAndWait(() -> {
            try {
                refresh.invoke(ov);
            } catch (Exception e) {
            }
        });
        Thread.sleep(500);

        Field fbF = OverviewPanel.class.getDeclaredField("futureBar");
        fbF.setAccessible(true);
        JPanel bar = (JPanel) fbF.get(ov);
        JLabel lv = null;
        for (Component c : bar.getComponents()) {
            if (c instanceof JLabel && ((JLabel) c).getText().equals("很久没动")) lv = (JLabel) c;
        }
        if (lv != null) {
            java.awt.event.MouseEvent ev = new java.awt.event.MouseEvent(lv,
                    java.awt.event.MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 1, 1, 1, false);
            for (java.awt.event.MouseListener ml : lv.getMouseListeners()) ml.mouseClicked(ev);
        }
        Thread.sleep(500);
        layoutAll(frame.getContentPane());
        Component card = ((Component) bar).getParent().getParent();
        shot(card, card.getWidth(), card.getHeight(), "C:\\Users\\chen\\Desktop\\ui-59-level-filter.png");

        /* 复位筛选 */
        JLabel off = null;
        for (Component c : bar.getComponents()) {
            if (c instanceof JLabel && ((JLabel) c).getText().equals("很久没动 · 收起")) off = (JLabel) c;
        }
        if (off != null) {
            java.awt.event.MouseEvent ev = new java.awt.event.MouseEvent(off,
                    java.awt.event.MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 1, 1, 1, false);
            for (java.awt.event.MouseListener ml : off.getMouseListeners()) ml.mouseClicked(ev);
        }
        Thread.sleep(400);

        /* 3. 小窗待办行的右键菜单 */
        JPopupMenu pm = new JPopupMenu();
        pm.add(new JMenuItem("设为今天"));
        pm.add(new JMenuItem("改期到明天"));
        pm.addSeparator();
        pm.add(new JMenuItem("删除这条"));
        pm.setSize(pm.getPreferredSize());
        pm.doLayout();
        for (Component k : pm.getComponents()) {
            k.setSize(k.getPreferredSize());
            if (k instanceof JComponent) ((JComponent) k).doLayout();
        }
        shot(pm, pm.getWidth(), pm.getHeight(), "C:\\Users\\chen\\Desktop\\ui-60-todo-menu.png");
        System.exit(0);
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
