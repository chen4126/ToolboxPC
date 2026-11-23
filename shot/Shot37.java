import com.toolbox.pc.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** v1.37 验收图：归档清单批量操作 / 这一档全部勾完 / 视图跨页保留 */
public class Shot37 {

    private static Object main;

    public static void main(String[] a) throws Exception {
        Theme.init();
        Ui.installDefaults();
        SwingUtilities.invokeAndWait(() -> {
            try {
                Class<?> c = Class.forName("com.toolbox.pc.Main");
                Object m = c.getDeclaredConstructor().newInstance();
                Method create = c.getDeclaredMethod("create");
                create.setAccessible(true);
                create.invoke(m);
                main = m;
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        Thread.sleep(2300);

        Class<?> mc = Class.forName("com.toolbox.pc.Main");
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

        Field fbF = OverviewPanel.class.getDeclaredField("futureBar");
        fbF.setAccessible(true);
        JPanel bar = (JPanel) fbF.get(ov);

        /* 1. 归档清单：两条 + 每条"删掉/恢复" + 底部三个按钮 */
        Method archM = OverviewPanel.class.getDeclaredMethod("archivedTodos");
        archM.setAccessible(true);
        List<?> list = (List<?>) archM.invoke(ov);
        Method boxM = OverviewPanel.class.getDeclaredMethod("archiveBox", List.class);
        boxM.setAccessible(true);
        JPanel box = (JPanel) boxM.invoke(ov, list);
        int boxH = 28 + list.size() * 32;
        box.setSize(new Dimension(452, boxH));
        box.setPreferredSize(new Dimension(452, boxH));
        layoutAll(box);

        JPanel page = new JPanel(new BorderLayout(0, 12));
        page.setBackground(Color.WHITE);
        page.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        page.add(box, BorderLayout.CENTER);
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setOpaque(false);
        btns.add(new JButton("全部恢复"));
        btns.add(new JButton("全部清空"));
        btns.add(new JButton("关闭"));
        page.add(btns, BorderLayout.SOUTH);
        page.setSize(new Dimension(500, boxH + 62));
        layoutAll(page);
        shot(page, page.getWidth(), page.getHeight(), "C:\\Users\\chen\\Desktop\\ui-61-archive-batch.png");

        /* 2. 只看很久没动 + 这一档全部勾完入口 */
        JLabel lv = findLabel(bar, "很久没动");
        click(lv);
        Thread.sleep(500);
        layoutAll(frame.getContentPane());
        JPanel card = todoCardOf(bar);
        shot(card, card.getWidth(), card.getHeight(), "C:\\Users\\chen\\Desktop\\ui-62-finish-filtered.png");

        /* 3. 切页再回来，视图仍在（换到"该处理了"演示） */
        JLabel lv2 = findLabel(bar, "很久没动 · 收起");
        click(lv2);
        Thread.sleep(300);
        JLabel lv3 = findLabel(bar, "该处理了");
        click(lv3);
        Thread.sleep(300);
        SwingUtilities.invokeAndWait(() -> {
            try {
                sel.invoke(main, "stats");
                Thread.sleep(700);
                sel.invoke(main, "overview");
            } catch (Exception e) {
            }
        });
        Thread.sleep(1200);
        layoutAll(frame.getContentPane());
        JPanel card2 = todoCardOf(bar);
        shot(card2, card2.getWidth(), card2.getHeight(), "C:\\Users\\chen\\Desktop\\ui-63-view-kept.png");

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

    static JLabel findLabel(JPanel bar, String text) {
        for (Component c : bar.getComponents()) {
            if (c instanceof JLabel && ((JLabel) c).getText().equals(text)) return (JLabel) c;
        }
        return null;
    }

    static void click(JLabel l) {
        if (l == null) return;
        java.awt.event.MouseEvent ev = new java.awt.event.MouseEvent(l,
                java.awt.event.MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 1, 1, 1, false);
        for (java.awt.event.MouseListener ml : l.getMouseListeners()) ml.mouseClicked(ev);
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
