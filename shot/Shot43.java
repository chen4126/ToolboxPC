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

/** v1.43 三项验收：多选复制到某天、小窗过期右键菜单、复制结果在 11-13 可见 */
public class Shot43 {

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

        /* 选中 2 条，多选条上就出现「复制到…」 */
        Method openM = OverviewPanel.class.getDeclaredMethod("openTodos");
        openM.setAccessible(true);
        List open = (List) openM.invoke(ov);
        Field msF = OverviewPanel.class.getDeclaredField("multiSelect");
        msF.setAccessible(true);
        Field pkF = OverviewPanel.class.getDeclaredField("picked");
        pkF.setAccessible(true);
        java.util.Set picked = (java.util.Set) pkF.get(ov);
        msF.setBoolean(ov, true);
        picked.clear();
        picked.add(open.get(0));
        picked.add(open.get(1));
        Method listM = OverviewPanel.class.getDeclaredMethod("refreshTodoList");
        listM.setAccessible(true);
        listM.invoke(ov);
        Thread.sleep(700);

        Field bbF = OverviewPanel.class.getDeclaredField("bulkBar");
        bbF.setAccessible(true);
        JPanel bulkBar = (JPanel) bbF.get(ov);
        System.out.println("bulk bar visible=" + bulkBar.isVisible()
                + " chips=" + bulkBar.getComponentCount());
        SwingUtilities.invokeAndWait(() -> frame.getContentPane().doLayout());
        Thread.sleep(500);
        layoutAll(frame.getContentPane());
        shot(frame.getContentPane(),
                frame.getContentPane().getWidth(), frame.getContentPane().getHeight(),
                "C:\\Users\\chen\\Desktop\\ui-81-bulk-copy.png");

        /* 真正复制到 11-13 */
        Method copyM = OverviewPanel.class.getDeclaredMethod("batchCopy", LocalDate.class);
        copyM.setAccessible(true);
        copyM.invoke(ov, LocalDate.of(2026, 11, 13));
        Thread.sleep(600);

        /* 小窗：右键过期红字 */
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

        Field olF = MiniBoard.class.getDeclaredField("overdueLbl");
        olF.setAccessible(true);
        JLabel lbl = (JLabel) olF.get(board);
        MouseEvent pop = new MouseEvent(lbl, MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(),
                0, 20, 10, 1, true, MouseEvent.BUTTON3);
        Method mp = MiniBoard.class.getDeclaredMethod("overduePopup", MouseEvent.class);
        mp.setAccessible(true);
        final Object bf2 = board;
        SwingUtilities.invokeAndWait(() -> {
            try {
                mp.invoke(bf2, pop);
            } catch (Exception e) {
            }
        });
        Thread.sleep(400);
        Field omF = MiniBoard.class.getDeclaredField("lastOverdueMenu");
        omF.setAccessible(true);
        JPopupMenu menu = (JPopupMenu) omF.get(board);
        if (menu != null) {
            menu.setSize(menu.getPreferredSize());
            menu.doLayout();
            shot(menu, menu.getWidth(), menu.getHeight(),
                    "C:\\Users\\chen\\Desktop\\ui-82-overdue-menu.png");
        } else {
            System.out.println("no overdue menu");
        }

        /* 小窗切到 11-13，看复制出来的两条 */
        Field bdF = MiniBoard.class.getDeclaredField("boardDay");
        bdF.setAccessible(true);
        bdF.setInt(board, 2);          // 柱 2 = 11-13
        Field ovF = MiniBoard.class.getDeclaredField("overdueView");
        ovF.setAccessible(true);
        ovF.setBoolean(board, false);
        br.invoke(board);
        Thread.sleep(700);
        for (Window w : Window.getWindows()) {
            if (w instanceof JFrame && "小工具箱 · 今日".equals(((JFrame) w).getTitle())) {
                layoutAll(w);
                shot(w, w.getWidth(), w.getHeight(),
                        "C:\\Users\\chen\\Desktop\\ui-83-copied-day.png");
            }
        }
    }

    static JPanel todoCardOf(JPanel futureBar) {
        Component c = futureBar;
        while (c != null && !(c instanceof JPanel && c.getParent() != null
                && c.getParent().getLayout() instanceof GridLayout)) {
            c = c.getParent();
        }
        return (JPanel) c;
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
