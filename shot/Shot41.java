import com.toolbox.pc.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.List;

/** v1.41 验收图：多选自定义日期段 / 归档搜索与排序 / 柱图点柱切天 */
public class Shot41 {

    public static void main(String[] a) throws Exception {
        try {
            run(a);
        } catch (Throwable t) {
            t.printStackTrace();
            System.out.flush();
        }
        Runtime.getRuntime().halt(0);
    }

    static void run(String[] a) throws Exception {
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
        Field fromF = OverviewPanel.class.getDeclaredField("bulkFrom");
        fromF.setAccessible(true);
        Field toF = OverviewPanel.class.getDeclaredField("bulkTo");
        toF.setAccessible(true);
        Field fbF = OverviewPanel.class.getDeclaredField("futureBar");
        fbF.setAccessible(true);
        Method rl = OverviewPanel.class.getDeclaredMethod("refreshTodoList");
        rl.setAccessible(true);
        Method openM = OverviewPanel.class.getDeclaredMethod("openTodos");
        openM.setAccessible(true);

        /* 1. 多选 + 自定义 11-11~11-15，全选 3 条 */
        msF.setBoolean(ov, true);
        fromF.set(ov, java.time.LocalDate.of(2026, 11, 11));
        toF.set(ov, java.time.LocalDate.of(2026, 11, 15));
        rgF.setInt(ov, 4);
        LinkedHashSet<Object> picked = (LinkedHashSet<Object>) pkF.get(ov);
        picked.clear();
        picked.addAll((List<Object>) openM.invoke(ov));
        run(rl, ov);
        Thread.sleep(700);
        layoutAll(frame.getContentPane());
        JPanel bar = (JPanel) fbF.get(ov);
        JPanel card = todoCardOf(bar);
        shot(card, card.getWidth(), card.getHeight(), "C:\\Users\\chen\\Desktop\\ui-75-bulk-custom.png");

        msF.setBoolean(ov, false);
        rgF.setInt(ov, 0);
        picked.clear();
        run(rl, ov);
        Thread.sleep(400);

        /* 2. 归档弹窗内容面板（搜索框 + 排序切换 + 清单） */
        Method apM = OverviewPanel.class.getDeclaredMethod("archivePanel", JDialog.class, int[].class);
        apM.setAccessible(true);
        System.out.println("calling archivePanel...");
        System.out.flush();
        JPanel panel = (JPanel) apM.invoke(ov, null, new int[]{-1});
        System.out.println("archivePanel ok");
        System.out.flush();
        Dimension dp = panel.getPreferredSize();
        panel.setSize(dp.width, dp.height);
        layoutAll(panel);
        shot(panel, dp.width, dp.height, "C:\\Users\\chen\\Desktop\\ui-76-archive-search.png");

        /* 3. 小窗柱图展开并点第 2 根柱（明天） */
        SwingUtilities.invokeAndWait(() -> MiniBoard.show());
        Thread.sleep(1500);
        Field instF = MiniBoard.class.getDeclaredField("inst");
        instF.setAccessible(true);
        Object board = instF.get(null);
        Field scF = MiniBoard.class.getDeclaredField("showChart");
        scF.setAccessible(true);
        Field chartF = MiniBoard.class.getDeclaredField("chart");
        chartF.setAccessible(true);
        Method br = MiniBoard.class.getDeclaredMethod("refresh");
        br.setAccessible(true);
        scF.setBoolean(board, true);
        run(br, board);
        Thread.sleep(700);

        JPanel chart = (JPanel) chartF.get(board);
        int pad = 8, gap = 6;
        int bw = (chart.getWidth() - pad * 2 - gap * 6) / 7;
        int x = pad + (bw + gap) * 1 + bw / 2;
        MouseEvent ev = new MouseEvent(chart, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
                0, x, 40, 1, false, MouseEvent.BUTTON1);
        for (java.awt.event.MouseListener ml : chart.getMouseListeners()) ml.mouseClicked(ev);
        Thread.sleep(900);

        for (Window w : Window.getWindows()) {
            if (w instanceof JFrame && "小工具箱 · 今日".equals(((JFrame) w).getTitle())) {
                layoutAll(w);
                shot(w, w.getWidth(), w.getHeight(), "C:\\Users\\chen\\Desktop\\ui-77-board-chart-click.png");
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
