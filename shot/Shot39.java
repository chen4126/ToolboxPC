import com.toolbox.pc.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.List;

/** v1.39 验收图：多选全选 / 归档清空与彻底删 / 小窗过期视图 */
public class Shot39 {

    public static void main(String[] a) throws Exception {
        Theme.init();
        Ui.installDefaults();
        Object main;
        Class<?> mc = Class.forName("com.toolbox.pc.Main");
        Object[] box = new Object[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                Object m = mc.getDeclaredConstructor().newInstance();
                Method create = mc.getDeclaredMethod("create");
                create.setAccessible(true);
                create.invoke(m);
                box[0] = m;
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        main = box[0];
        Thread.sleep(2300);

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
        run(refresh, ov);
        Thread.sleep(600);

        /* 1. 多选全选 4 条 */
        Field msF = OverviewPanel.class.getDeclaredField("multiSelect");
        msF.setAccessible(true);
        Field pkF = OverviewPanel.class.getDeclaredField("picked");
        pkF.setAccessible(true);
        Method rl = OverviewPanel.class.getDeclaredMethod("refreshTodoList");
        rl.setAccessible(true);
        Field fbF = OverviewPanel.class.getDeclaredField("futureBar");
        fbF.setAccessible(true);
        msF.setBoolean(ov, true);
        LinkedHashSet<Object> picked = (LinkedHashSet<Object>) pkF.get(ov);
        picked.clear();
        Method openM = OverviewPanel.class.getDeclaredMethod("openTodos");
        openM.setAccessible(true);
        picked.addAll((List<Object>) openM.invoke(ov));
        run(rl, ov);
        Thread.sleep(600);
        layoutAll(frame.getContentPane());
        JPanel bar = (JPanel) fbF.get(ov);
        JPanel card = todoCardOf(bar);
        shot(card, card.getWidth(), card.getHeight(),
                "C:\\Users\\chen\\Desktop\\ui-67-bulk-select-all.png");

        msF.setBoolean(ov, false);
        picked.clear();
        run(rl, ov);
        Thread.sleep(500);

        /* 2. 归档清单（含彻底删按钮与说明） */
        Method archM = OverviewPanel.class.getDeclaredMethod("archivedTodos");
        archM.setAccessible(true);
        Method boxM = OverviewPanel.class.getDeclaredMethod("archiveBox", java.util.List.class);
        boxM.setAccessible(true);
        List<?> arch = (List<?>) archM.invoke(ov);
        JPanel ab = (JPanel) boxM.invoke(ov, arch);
        ab.setSize(430, 200);
        layoutAll(ab);
        int h = Math.max(120, ab.getPreferredSize().height + 10);
        shot(ab, 430, h, "C:\\Users\\chen\\Desktop\\ui-68-archive-dialog.png");

        /* 3. 小窗过期视图 */
        SwingUtilities.invokeAndWait(() -> MiniBoard.show());
        Thread.sleep(1500);
        Field ovF = MiniBoard.class.getDeclaredField("overdueView");
        ovF.setAccessible(true);
        Field instF = MiniBoard.class.getDeclaredField("inst");
        instF.setAccessible(true);
        Object board = instF.get(null);
        ovF.setBoolean(board, true);
        Method br = MiniBoard.class.getDeclaredMethod("refresh");
        br.setAccessible(true);
        run(br, board);
        Thread.sleep(700);
        for (Window w : Window.getWindows()) {
            if (w instanceof JFrame && "小工具箱 · 今日".equals(((JFrame) w).getTitle())) {
                layoutAll(w);
                shot(w, w.getWidth(), w.getHeight(),
                        "C:\\Users\\chen\\Desktop\\ui-69-board-overdue.png");
            }
        }
        System.exit(0);
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

    static void run(Method m, Object t) {
        SwingUtilities.invokeLater(() -> {
            try {
                m.invoke(t);
            } catch (Exception ignored) {
            }
        });
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
