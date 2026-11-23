import com.toolbox.pc.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.Map;

/** v1.32 验收图：深红该处理了 / 日历双击新建提示 / 小窗顶部过期红字 */
public class Shot32 {

    public static void main(String[] a) throws Exception {
        Theme.init();
        Ui.installDefaults();
        final Object[] holder = new Object[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                Class<?> c = Class.forName("com.toolbox.pc.Main");
                Object m = c.getDeclaredConstructor().newInstance();
                Method create = c.getDeclaredMethod("create");
                create.setAccessible(true);
                create.invoke(m);
                holder[0] = m;
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        Thread.sleep(2300);

        Class<?> mc = Class.forName("com.toolbox.pc.Main");
        Field ff = mc.getDeclaredField("frame");
        ff.setAccessible(true);
        final JFrame frame = (JFrame) ff.get(holder[0]);
        Method sel = mc.getDeclaredMethod("selectNav", String.class);
        sel.setAccessible(true);
        SwingUtilities.invokeAndWait(() -> {
            try {
                sel.invoke(holder[0], "overview");
            } catch (Exception e) {
            }
        });
        Thread.sleep(1200);
        shot(frame.getContentPane(), frame.getContentPane().getWidth(),
                frame.getContentPane().getHeight(), "C:\\Users\\chen\\Desktop\\ui-45-deep-overdue.png");

        OverviewPanel ov = find(frame.getContentPane(), OverviewPanel.class);
        Method titles = OverviewPanel.class.getDeclaredMethod("todoTitlesByDay");
        titles.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<LocalDate, java.util.List<String>> dayMap =
                (Map<LocalDate, java.util.List<String>>) titles.invoke(ov);
        Class<?> dp = Class.forName("com.toolbox.pc.DatePicker");
        Method build = dp.getDeclaredMethod("build", Window.class, LocalDate.class, Map.class,
                LocalDate[].class, boolean[].class);
        build.setAccessible(true);
        LocalDate[] out = {null};
        boolean[] create = {false};
        JDialog dlg = (JDialog) build.invoke(null, null, LocalDate.of(2026, 11, 11), dayMap, out, create);
        Container root = dlg.getContentPane();
        root.setSize(root.getPreferredSize());
        layoutAll(root);
        shot(root, root.getWidth(), root.getHeight(), "C:\\Users\\chen\\Desktop\\ui-46-pick-new.png");

        SwingUtilities.invokeAndWait(() -> MiniBoard.toggle());
        Thread.sleep(900);
        Field instF = MiniBoard.class.getDeclaredField("inst");
        instF.setAccessible(true);
        Object mb = instF.get(null);
        Field winF = MiniBoard.class.getDeclaredField("win");
        winF.setAccessible(true);
        JFrame win = (JFrame) winF.get(mb);
        Method rf = MiniBoard.class.getDeclaredMethod("refresh");
        rf.setAccessible(true);
        SwingUtilities.invokeAndWait(() -> {
            try {
                rf.invoke(mb);
            } catch (Exception e) {
            }
        });
        Thread.sleep(600);
        Container wc = win.getContentPane();
        layoutAll(wc);
        shot(wc, win.getWidth(), win.getHeight(),
                "C:\\Users\\chen\\Desktop\\ui-47-board-overdue.png");
        System.exit(0);
    }

    static void shot(Component c, int w, int h, String path) throws Exception {
        BufferedImage img = new BufferedImage(Math.max(1, w), Math.max(1, h), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        if (c instanceof JComponent) c.paint(g);
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
