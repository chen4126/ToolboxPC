import com.toolbox.pc.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** v1.56 验收：小窗底栏固定条、统计页待办节奏小字、点本月段后月卡描边 */
public class Shot56 {

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

        /* 1. 小窗过期视图：底栏"还有 1 条…（点开全部）" + "全部勾完 5 条" 固定可见 */
        Field instF = MiniBoard.class.getDeclaredField("inst");
        instF.setAccessible(true);
        Object board = instF.get(null);
        if (board == null) {
            SwingUtilities.invokeAndWait(() -> MiniBoard.show());
            Thread.sleep(900);
            board = instF.get(null);
        }
        Field ovvF = MiniBoard.class.getDeclaredField("overdueView");
        ovvF.setAccessible(true);
        ovvF.set(board, true);
        Method br = MiniBoard.class.getDeclaredMethod("refresh");
        br.setAccessible(true);
        br.invoke(board);
        Thread.sleep(900);
        Field winF = MiniBoard.class.getDeclaredField("win");
        winF.setAccessible(true);
        Object win = winF.get(board);
        Container root = (win instanceof JFrame) ? ((JFrame) win).getContentPane()
                : (Container) ((java.awt.Window) win).getComponents()[0];
        root.doLayout();
        layoutAll(root);
        shot(root, root.getWidth(), root.getHeight(),
                "C:\\Users\\chen\\Desktop\\ui-141-miniboard-pin.png");

        /* 2. 统计页：右上角多出"待办完成 本周 2 条 · 上周 1 条 · 近 7 天 2 条" */
        SwingUtilities.invokeAndWait(() -> {
            try {
                sel.invoke(main, "stats");
            } catch (Exception e) {
            }
        });
        Thread.sleep(1600);
        frame.getContentPane().doLayout();
        layoutAll(frame.getContentPane());
        Thread.sleep(200);
        shot(frame.getContentPane(),
                frame.getContentPane().getWidth(), frame.getContentPane().getHeight(),
                "C:\\Users\\chen\\Desktop\\ui-142-stats-todo-rhythm.png");

        /* 3. 回概览点"本月3"→ 统计页月卡被描主色，抓那一瞬间 */
        SwingUtilities.invokeAndWait(() -> {
            try {
                sel.invoke(main, "overview");
            } catch (Exception e) {
            }
        });
        Thread.sleep(1300);
        OverviewPanel ov = find(frame.getContentPane(), OverviewPanel.class);
        Method refresh = OverviewPanel.class.getDeclaredMethod("refresh");
        refresh.setAccessible(true);
        refresh.invoke(ov);
        Thread.sleep(400);
        Field wmF = OverviewPanel.class.getDeclaredField("wMonth");
        wmF.setAccessible(true);
        final JLabel wm = (JLabel) wmF.get(ov);
        SwingUtilities.invokeAndWait(() -> {
            java.awt.event.MouseEvent e = new java.awt.event.MouseEvent(wm,
                    java.awt.event.MouseEvent.MOUSE_CLICKED, 0,
                    java.awt.event.MouseEvent.BUTTON1_DOWN_MASK, 1, 1, 1, false,
                    java.awt.event.MouseEvent.BUTTON1);
            wm.dispatchEvent(e);
        });
        Thread.sleep(450);
        frame.getContentPane().doLayout();
        layoutAll(frame.getContentPane());
        shot(frame.getContentPane(),
                frame.getContentPane().getWidth(), frame.getContentPane().getHeight(),
                "C:\\Users\\chen\\Desktop\\ui-143-month-card-highlight.png");
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
