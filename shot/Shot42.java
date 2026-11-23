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

/** v1.42 三项验收：通知卡片（只弹一个）、归档搜索高亮、柱图右键菜单 */
public class Shot42 {

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

        /* 1. 通知卡片：同一句话点两次，只出一个 */
        SwingUtilities.invokeAndWait(() -> {
            Main.notifyMsg("已批量删除", "删掉 2 条待办");
            Main.notifyMsg("已批量删除", "删掉 2 条待办");
        });
        Thread.sleep(500);
        Field stackF = Toast.class.getDeclaredField("stack");
        stackF.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<JWindow> stack = (List<JWindow>) stackF.get(null);
        if (!stack.isEmpty()) {
            JWindow w = stack.get(0);
            Component card = w.getContentPane();
            card.setSize(316, 70);
            layoutAll(card);
            shot(card, 316, 70, "C:\\Users\\chen\\Desktop\\ui-78-toast.png");
        } else {
            System.out.println("no toast");
        }

        /* 2. 归档搜索高亮：在搜索框里输入「二十」 */
        OverviewPanel ov = find(frame.getContentPane(), OverviewPanel.class);
        Method apM = OverviewPanel.class.getDeclaredMethod("archivePanel", JDialog.class, int[].class);
        apM.setAccessible(true);
        JPanel panel = (JPanel) apM.invoke(ov, null, new int[]{-1});
        JTextField search = find(panel, JTextField.class);
        final JTextField sf = search;
        SwingUtilities.invokeAndWait(() -> sf.setText("二十"));
        Thread.sleep(600);
        Dimension dp = panel.getPreferredSize();
        panel.setSize(dp.width, dp.height);
        layoutAll(panel);
        shot(panel, dp.width, dp.height, "C:\\Users\\chen\\Desktop\\ui-79-archive-highlight.png");

        /* 3. 小窗柱图右键菜单 */
        Field instF = MiniBoard.class.getDeclaredField("inst");
        instF.setAccessible(true);
        Object board = instF.get(null);
        if (board == null) {
            SwingUtilities.invokeAndWait(() -> MiniBoard.show());
            Thread.sleep(800);
            board = instF.get(null);
        }
        Field scF = MiniBoard.class.getDeclaredField("showChart");
        scF.setAccessible(true);
        Field chartF = MiniBoard.class.getDeclaredField("chart");
        chartF.setAccessible(true);
        Method br = MiniBoard.class.getDeclaredMethod("refresh");
        br.setAccessible(true);
        scF.setBoolean(board, true);
        br.invoke(board);
        Thread.sleep(800);

        JPanel chart = (JPanel) chartF.get(board);
        MouseEvent pop = new MouseEvent(chart, MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(),
                0, 41, 40, 1, true, MouseEvent.BUTTON3);      // 第 2 根柱 = 11-12
        Method cp = MiniBoard.class.getDeclaredMethod("chartPopup", MouseEvent.class);
        cp.setAccessible(true);
        final Object boardF = board;
        SwingUtilities.invokeAndWait(() -> {
            try {
                cp.invoke(boardF, pop);
            } catch (Exception e) {
            }
        });
        Thread.sleep(400);
        Field lmF = MiniBoard.class.getDeclaredField("lastChartMenu");
        lmF.setAccessible(true);
        JPopupMenu menu = (JPopupMenu) lmF.get(board);
        if (menu != null) {
            menu.setSize(menu.getPreferredSize());
            menu.doLayout();
            shot(menu, menu.getWidth(), menu.getHeight(),
                    "C:\\Users\\chen\\Desktop\\ui-80-chart-menu.png");
        } else {
            System.out.println("no menu");
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
