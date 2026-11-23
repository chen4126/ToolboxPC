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

/** v1.38 验收图：待办多选批量 / 清空归档撤销 / 小窗顶部条数摘要 */
public class Shot38 {

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

        /* 1. 多选：选中两条 + 底部操作条 */
        Field msF = OverviewPanel.class.getDeclaredField("multiSelect");
        msF.setAccessible(true);
        Field pkF = OverviewPanel.class.getDeclaredField("picked");
        pkF.setAccessible(true);
        msF.setBoolean(ov, true);
        LinkedHashSet<Object> picked = (LinkedHashSet<Object>) pkF.get(ov);
        picked.clear();
        picked.add(findTodo(ov, "九天前没动"));
        picked.add(findTodo(ov, "四天前"));
        Method rl = OverviewPanel.class.getDeclaredMethod("refreshTodoList");
        rl.setAccessible(true);
        run(rl, ov);
        Thread.sleep(600);
        layoutAll(frame.getContentPane());
        JPanel card = todoCardOf(bar);
        shot(card, card.getWidth(), card.getHeight(), "C:\\Users\\chen\\Desktop\\ui-64-bulk-select.png");

        /* 复位多选 */
        msF.setBoolean(ov, false);
        picked.clear();
        run(rl, ov);
        Thread.sleep(400);

        /* 2. 清空归档 → 撤销入口 */
        Method delAll = OverviewPanel.class.getDeclaredMethod("deleteAllArchived", boolean.class);
        delAll.setAccessible(true);
        delAll.invoke(ov, false);
        Thread.sleep(300);
        run(rl, ov);
        Thread.sleep(500);
        layoutAll(frame.getContentPane());
        JPanel card2 = todoCardOf(bar);
        shot(card2, card2.getWidth(), card2.getHeight(), "C:\\Users\\chen\\Desktop\\ui-65-archive-undo.png");

        /* 3. 小窗顶部摘要 */
        SwingUtilities.invokeAndWait(() -> MiniBoard.show());
        Thread.sleep(1600);
        for (Window w : Window.getWindows()) {
            if (w instanceof JFrame && "小工具箱 · 今日".equals(((JFrame) w).getTitle())) {
                layoutAll(w);
                shot(w, w.getWidth(), w.getHeight(),
                        "C:\\Users\\chen\\Desktop\\ui-66-board-summary.png");
            }
        }
        System.exit(0);
    }

    static Object findTodo(Object ov, String text) throws Exception {
        Field tf = OverviewPanel.class.getDeclaredField("todos");
        tf.setAccessible(true);
        for (Object t : (List<?>) tf.get(ov)) {
            Field f = t.getClass().getDeclaredField("text");
            f.setAccessible(true);
            if (String.valueOf(f.get(t)).equals(text)) return t;
        }
        return null;
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
