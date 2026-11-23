import com.toolbox.pc.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;

/** v1.47 三项验收：多选两个新 chip、＋ 右键菜单、改到下周一后小窗能看到 */
public class Shot47 {

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

        Method openM = OverviewPanel.class.getDeclaredMethod("openTodos");
        openM.setAccessible(true);
        List open = (List) openM.invoke(ov);
        Field msF = OverviewPanel.class.getDeclaredField("multiSelect");
        msF.setAccessible(true);
        Field pkF = OverviewPanel.class.getDeclaredField("picked");
        pkF.setAccessible(true);
        msF.setBoolean(ov, true);
        java.util.Set picked = (java.util.Set) pkF.get(ov);
        picked.clear();
        picked.add(open.get(0));
        picked.add(open.get(1));
        Method listM = OverviewPanel.class.getDeclaredMethod("refreshTodoList");
        listM.setAccessible(true);
        listM.invoke(ov);
        Thread.sleep(700);
        SwingUtilities.invokeAndWait(() -> frame.getContentPane().doLayout());
        layoutAll(frame.getContentPane());
        shot(frame.getContentPane(),
                frame.getContentPane().getWidth(), frame.getContentPane().getHeight(),
                "C:\\Users\\chen\\Desktop\\ui-93-bulk-chips.png");

        /* 把选中的两条挪到下周一 11-16 */
        Method bm = OverviewPanel.class.getDeclaredMethod("batchMove", LocalDate.class);
        bm.setAccessible(true);
        bm.invoke(ov, LocalDate.of(2026, 11, 16));
        Thread.sleep(600);

        /* 小窗＋右键菜单 */
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
            final JLabel pl = plus;
            SwingUtilities.invokeAndWait(() -> {
                try {
                    pm.invoke(bf, pop);
                } catch (Exception e) {
                }
            });
            Thread.sleep(400);
            Field lpF = MiniBoard.class.getDeclaredField("lastPlusMenu");
            lpF.setAccessible(true);
            JPopupMenu menu = (JPopupMenu) lpF.get(board);
            if (menu != null) {
                menu.setSize(menu.getPreferredSize());
                menu.doLayout();
                shot(menu, menu.getWidth(), menu.getHeight(),
                        "C:\\Users\\chen\\Desktop\\ui-94-plus-menu.png");
            }
        }

        /* 小窗切到 11-16（柱 5），看挪过去的条目 */
        Field bdF = MiniBoard.class.getDeclaredField("boardDay");
        bdF.setAccessible(true);
        bdF.setInt(board, 5);
        br.invoke(board);
        Thread.sleep(700);
        layoutAll(win);
        shot(win, win.getWidth(), win.getHeight(),
                "C:\\Users\\chen\\Desktop\\ui-95-moved-monday.png");
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
