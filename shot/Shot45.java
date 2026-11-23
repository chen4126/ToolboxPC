import com.toolbox.pc.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** v1.45 三项验收：归档右键两项、小窗右键五项、小窗底部「＋」新建 */
public class Shot45 {

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

        /* 1. 归档右键菜单 */
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
                    "C:\\Users\\chen\\Desktop\\ui-87-arch-menu2.png");
        }

        /* 2. 小窗整窗（底部有 ＋） */
        Field instF = MiniBoard.class.getDeclaredField("inst");
        instF.setAccessible(true);
        Object board = instF.get(null);
        if (board == null) {
            SwingUtilities.invokeAndWait(() -> MiniBoard.show());
            Thread.sleep(900);
            board = instF.get(null);
        }
        Method br = MiniBoard.class.getDeclaredMethod("refresh");
        br.setAccessible(true);
        br.invoke(board);
        Thread.sleep(800);
        JFrame win = (JFrame) getField(board, "win");
        layoutAll(win);
        shot(win, win.getWidth(), win.getHeight(),
                "C:\\Users\\chen\\Desktop\\ui-88-board-footer.png");

        /* 3. 小窗待办右键五项 */
        Method pm = MiniBoard.class.getDeclaredMethod("popupTodo",
                MouseEvent.class, Component.class, String.class);
        pm.setAccessible(true);
        Field lbF = MiniBoard.class.getDeclaredField("listBox");
        lbF.setAccessible(true);
        Component inv = (Component) lbF.get(board);
        MouseEvent pop = new MouseEvent(inv, MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(),
                0, 20, 10, 1, true, MouseEvent.BUTTON3);
        final Object bf = board;
        SwingUtilities.invokeAndWait(() -> {
            try {
                pm.invoke(bf, pop, inv, "今天的事");
            } catch (Exception e) {
            }
        });
        Thread.sleep(400);
        Field tmF = MiniBoard.class.getDeclaredField("lastTodoMenu");
        tmF.setAccessible(true);
        JPopupMenu menu = (JPopupMenu) tmF.get(board);
        if (menu != null) {
            menu.setSize(menu.getPreferredSize());
            menu.doLayout();
            shot(menu, menu.getWidth(), menu.getHeight(),
                    "C:\\Users\\chen\\Desktop\\ui-89-board-menu5.png");
        }
    }

    static Object getField(Object target, String name) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
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
