import com.toolbox.pc.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** v1.50 三项验收：全部过期一起勾完、小窗过期右键复制、勾完后的红字消失 */
public class Shot50 {

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

        /* 1. 只看过期 + 底部「全部过期一起勾完」 */
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
                "C:\\Users\\chen\\Desktop\\ui-102-overdue-finish.png");

        /* 2. 小窗过期右键 4 项 */
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
        JLabel overdueLbl = (JLabel) getField(board, "overdueLbl");
        if (overdueLbl != null && overdueLbl.isShowing()) {
            MouseEvent pop = new MouseEvent(overdueLbl, MouseEvent.MOUSE_RELEASED,
                    System.currentTimeMillis(), 0, 5, 5, 1, true, MouseEvent.BUTTON3);
            Method op = MiniBoard.class.getDeclaredMethod("overduePopup", MouseEvent.class);
            op.setAccessible(true);
            final Object bf = board;
            SwingUtilities.invokeAndWait(() -> {
                try {
                    op.invoke(bf, pop);
                } catch (Exception e) {
                }
            });
            Thread.sleep(300);
            Field loF = MiniBoard.class.getDeclaredField("lastOverdueMenu");
            loF.setAccessible(true);
            JPopupMenu menu = (JPopupMenu) loF.get(board);
            if (menu != null) {
                menu.setSize(menu.getPreferredSize());
                menu.doLayout();
                shot(menu, menu.getWidth(), menu.getHeight(),
                        "C:\\Users\\chen\\Desktop\\ui-103-overdue-menu4.png");
                menu.setVisible(false);
            }
        }

        /* 3. 点「全部过期一起勾完」，再看小窗红字消失 */
        Field fbF = OverviewPanel.class.getDeclaredField("futureBar");
        fbF.setAccessible(true);
        JPanel futureBar = (JPanel) fbF.get(ov);
        JLabel finish = null;
        for (Component c : futureBar.getComponents()) {
            if (c instanceof JLabel && ((JLabel) c).getText() != null
                    && ((JLabel) c).getText().startsWith("全部过期一起勾完")) finish = (JLabel) c;
        }
        if (finish != null) {
            final JLabel f2 = finish;
            SwingUtilities.invokeAndWait(() -> {
                for (java.awt.event.MouseListener ml : f2.getMouseListeners()) ml.mouseClicked(click);
            });
            Thread.sleep(900);
        }
        br.invoke(board);
        Thread.sleep(800);
        layoutAll(win);
        shot(win, win.getWidth(), win.getHeight(),
                "C:\\Users\\chen\\Desktop\\ui-104-after-finish.png");
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
