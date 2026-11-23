import com.toolbox.pc.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** v1.55 验收：统计页待办完成卡、归档搜日期、小窗过期行右侧灰字 */
public class Shot55 {

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

        /* 1. 统计页：8 张数字卡一行 4 张，含待办本周/本月完成 */
        SwingUtilities.invokeAndWait(() -> {
            try {
                sel.invoke(main, "stats");
            } catch (Exception e) {
            }
        });
        Thread.sleep(1500);
        StatsPanel sp = find(frame.getContentPane(), StatsPanel.class);
        SwingUtilities.invokeAndWait(() -> {
            frame.getContentPane().doLayout();
            frame.getContentPane().validate();
        });
        layoutAll(frame.getContentPane());
        Thread.sleep(300);
        shot(frame.getContentPane(),
                frame.getContentPane().getWidth(), frame.getContentPane().getHeight(),
                "C:\\Users\\chen\\Desktop\\ui-131-stats-todo-cards.png");

        /* 2. 归档弹窗：搜 "10-2"，只剩 10-02 那条 */
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
        Thread.sleep(500);

        Field aqF = OverviewPanel.class.getDeclaredField("archQuery");
        aqF.setAccessible(true);
        aqF.set(ov, "10-2");
        Method apM = OverviewPanel.class.getDeclaredMethod("archivePanel", JDialog.class, int[].class);
        apM.setAccessible(true);
        JPanel panel = (JPanel) apM.invoke(ov, null, new int[]{-1});
        panel.setSize(680, 400);
        layoutAll(panel);
        shot(panel, 680, 400, "C:\\Users\\chen\\Desktop\\ui-132-arch-search-date.png");
        aqF.set(ov, "");

        /* 3. 小窗过期视图：标题在左 + 右侧淡灰"MM-dd · N天" */
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
        Thread.sleep(800);

        Field winF = MiniBoard.class.getDeclaredField("win");
        winF.setAccessible(true);
        Object win = winF.get(board);
        Container root = (win instanceof JFrame) ? ((JFrame) win).getContentPane()
                : (Container) ((java.awt.Window) win).getComponents()[0];
        root.doLayout();
        layoutAll(root);
        shot(root, root.getWidth(), root.getHeight(),
                "C:\\Users\\chen\\Desktop\\ui-133-miniboard-overdue.png");
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
