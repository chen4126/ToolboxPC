import com.toolbox.pc.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** v1.49 三项验收：只看过期开关、＋ 菜单上次那天、归档复制全部 */
public class Shot49 {

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

        /* 点「只看过期」 */
        Field obF = OverviewPanel.class.getDeclaredField("onlyOverdueBtn");
        obF.setAccessible(true);
        final JLabel ovBtn = (JLabel) obF.get(ov);
        MouseEvent click = new MouseEvent(ovBtn, MouseEvent.MOUSE_CLICKED,
                System.currentTimeMillis(), 0, 4, 4, 1, false, MouseEvent.BUTTON1);
        SwingUtilities.invokeAndWait(() -> {
            for (java.awt.event.MouseListener ml : ovBtn.getMouseListeners()) ml.mouseClicked(click);
        });
        Thread.sleep(700);
        SwingUtilities.invokeAndWait(() -> frame.getContentPane().doLayout());
        layoutAll(frame.getContentPane());
        shot(frame.getContentPane(),
                frame.getContentPane().getWidth(), frame.getContentPane().getHeight(),
                "C:\\Users\\chen\\Desktop\\ui-99-only-overdue.png");

        /* ＋ 菜单（先写一条记忆） */
        Method ws = MiniBoard.class.getDeclaredMethod("writeSetting", String.class, String.class);
        ws.setAccessible(true);
        ws.invoke(null, "boardAddDay", "2026-11-20");
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
        Thread.sleep(700);
        JFrame win = (JFrame) getField(board, "win");
        JLabel plus = null;
        for (JLabel l : allLabels(win.getContentPane())) {
            if ("＋".equals(l.getText())) plus = l;
        }
        if (plus != null) {
            MouseEvent pop = new MouseEvent(plus, MouseEvent.MOUSE_RELEASED,
                    System.currentTimeMillis(), 0, 5, 5, 1, true, MouseEvent.BUTTON3);
            Method pm = MiniBoard.class.getDeclaredMethod("plusMenu", MouseEvent.class);
            pm.setAccessible(true);
            final Object bf = board;
            SwingUtilities.invokeAndWait(() -> {
                try {
                    pm.invoke(bf, pop);
                } catch (Exception e) {
                }
            });
            Thread.sleep(300);
            Field lpF = MiniBoard.class.getDeclaredField("lastPlusMenu");
            lpF.setAccessible(true);
            JPopupMenu menu = (JPopupMenu) lpF.get(board);
            if (menu != null) {
                menu.setSize(menu.getPreferredSize());
                menu.doLayout();
                shot(menu, menu.getWidth(), menu.getHeight(),
                        "C:\\Users\\chen\\Desktop\\ui-100-plus-memo.png");
            }
        }

        /* 归档弹窗：复制全部按钮 */
        Method apM = OverviewPanel.class.getDeclaredMethod("archivePanel", JDialog.class, int[].class);
        apM.setAccessible(true);
        JPanel panel = (JPanel) apM.invoke(ov, null, new int[]{-1});
        panel.setSize(680, 400);
        layoutAll(panel);
        panel.doLayout();
        layoutAll(panel);
        shot(panel, 680, 400, "C:\\Users\\chen\\Desktop\\ui-101-arch-copyall.png");
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

    static Object getField(Object target, String name) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    static java.util.List<Component> all(Component root) {
        java.util.List<Component> out = new java.util.ArrayList<Component>();
        walk(root, out);
        return out;
    }

    static void walk(Component c, java.util.List<Component> out) {
        out.add(c);
        if (c instanceof Container) for (Component k : ((Container) c).getComponents()) walk(k, out);
    }

    static java.util.List<JLabel> allLabels(Component root) {
        java.util.List<JLabel> out = new java.util.ArrayList<JLabel>();
        for (Component c : all(root)) if (c instanceof JLabel) out.add((JLabel) c);
        return out;
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
