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

/** v1.48 三项验收：多选条收敛 7 chip、改期下拉、归档搜索清空 */
public class Shot48 {

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
        picked.add(open.get(2));
        Method listM = OverviewPanel.class.getDeclaredMethod("refreshTodoList");
        listM.setAccessible(true);
        listM.invoke(ov);
        Thread.sleep(700);
        SwingUtilities.invokeAndWait(() -> frame.getContentPane().doLayout());
        layoutAll(frame.getContentPane());
        shot(frame.getContentPane(),
                frame.getContentPane().getWidth(), frame.getContentPane().getHeight(),
                "C:\\Users\\chen\\Desktop\\ui-96-bulk-chips7.png");

        /* 改期下拉 */
        Field bfF = OverviewPanel.class.getDeclaredField("bulkBar");
        bfF.setAccessible(true);
        JPanel bulkBar = (JPanel) bfF.get(ov);
        JLabel defer = null;
        for (Component c : bulkBar.getComponents()) {
            if (c instanceof JLabel && "改期到…".equals(((JLabel) c).getText())) defer = (JLabel) c;
        }
        if (defer != null) {
            final JLabel df = defer;
            Method dm = OverviewPanel.class.getDeclaredMethod("bulkMoveMenu", Component.class);
            dm.setAccessible(true);
            final Object of = ov;
            SwingUtilities.invokeAndWait(() -> {
                try {
                    dm.invoke(of, df);
                } catch (Exception e) {
                }
            });
            Thread.sleep(300);
            Field lbmF = OverviewPanel.class.getDeclaredField("lastBulkMenu");
            lbmF.setAccessible(true);
            JPopupMenu menu = (JPopupMenu) lbmF.get(ov);
            if (menu != null) {
                menu.setSize(menu.getPreferredSize());
                menu.doLayout();
                shot(menu, menu.getWidth(), menu.getHeight(),
                        "C:\\Users\\chen\\Desktop\\ui-97-bulk-defer-menu.png");
            }
        }

        /* 归档弹窗：搜索框带「清空」 */
        Method apM = OverviewPanel.class.getDeclaredMethod("archivePanel", JDialog.class, int[].class);
        apM.setAccessible(true);
        JPanel panel = (JPanel) apM.invoke(ov, null, new int[]{-1});
        panel.setSize(680, 400);
        layoutAll(panel);
        panel.doLayout();
        layoutAll(panel);
        shot(panel, 680, 400, "C:\\Users\\chen\\Desktop\\ui-98-arch-clear.png");
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
