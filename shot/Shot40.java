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

/** v1.40 验收图：多选按范围筛 / 归档恢复并改期 / 小窗 7 天柱图 */
public class Shot40 {

    public static void main(String[] a) throws Exception {
        Theme.init();
        Ui.installDefaults();
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
        Object main = box[0];
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

        Field msF = OverviewPanel.class.getDeclaredField("multiSelect");
        msF.setAccessible(true);
        Field pkF = OverviewPanel.class.getDeclaredField("picked");
        pkF.setAccessible(true);
        Field rgF = OverviewPanel.class.getDeclaredField("bulkRange");
        rgF.setAccessible(true);
        Field fbF = OverviewPanel.class.getDeclaredField("futureBar");
        fbF.setAccessible(true);
        Field rbF = OverviewPanel.class.getDeclaredField("rangeBar");
        rbF.setAccessible(true);
        Method rl = OverviewPanel.class.getDeclaredMethod("refreshTodoList");
        rl.setAccessible(true);

        /* 1. 多选 + 只看过期，选中 3 条 */
        msF.setBoolean(ov, true);
        rgF.setInt(ov, 3);
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
                "C:\\Users\\chen\\Desktop\\ui-70-bulk-range.png");

        msF.setBoolean(ov, false);
        rgF.setInt(ov, 0);
        picked.clear();
        run(rl, ov);
        Thread.sleep(500);

        /* 2. 归档清单：彻底删 / 恢复并改期 / 恢复 */
        Method archM = OverviewPanel.class.getDeclaredMethod("archivedTodos");
        archM.setAccessible(true);
        Method boxM = OverviewPanel.class.getDeclaredMethod("archiveBox", java.util.List.class);
        boxM.setAccessible(true);
        List<?> arch = (List<?>) archM.invoke(ov);
        System.out.println("归档条数=" + arch.size());
        JPanel ab = (JPanel) boxM.invoke(ov, arch);
        ab.setSize(520, 200);
        layoutAll(ab);
        int h = 96;
        ab.setSize(520, h);
        layoutAll(ab);
        shot(ab, 520, h, "C:\\Users\\chen\\Desktop\\ui-71-archive-recover-day.png");

        /* 3. 小窗展开 7 天柱图 */
        SwingUtilities.invokeAndWait(() -> MiniBoard.show());
        Thread.sleep(1500);
        Field instF = MiniBoard.class.getDeclaredField("inst");
        instF.setAccessible(true);
        Object board = instF.get(null);
        Field scF = MiniBoard.class.getDeclaredField("showChart");
        scF.setAccessible(true);
        scF.setBoolean(board, true);
        Method br = MiniBoard.class.getDeclaredMethod("refresh");
        br.setAccessible(true);
        run(br, board);
        Thread.sleep(800);
        for (Window w : Window.getWindows()) {
            if (w instanceof JFrame && "小工具箱 · 今日".equals(((JFrame) w).getTitle())) {
                layoutAll(w);
                shot(w, w.getWidth(), w.getHeight(),
                        "C:\\Users\\chen\\Desktop\\ui-72-board-chart.png");
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
