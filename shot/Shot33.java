import com.toolbox.pc.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;

/** v1.33 验收图：三档过期 / 小窗很久没动 / 新建待办上下文 */
public class Shot33 {

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
                frame.getContentPane().getHeight(), "C:\\Users\\chen\\Desktop\\ui-48-three-tier.png");

        /* 小窗：很久没动 */
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
        shot(wc, win.getWidth(), win.getHeight(), "C:\\Users\\chen\\Desktop\\ui-49-board-ancient.png");

        /* 新建待办弹窗里的上下文行 */
        OverviewPanel ov = find(wc.getParent() == null ? frame.getContentPane() : frame.getContentPane(),
                OverviewPanel.class);
        Method ctx = OverviewPanel.class.getDeclaredMethod("newTodoContext", LocalDate.class, String.class);
        ctx.setAccessible(true);
        String txt = (String) ctx.invoke(ov, LocalDate.of(2026, 11, 11), "2026-11-11");
        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        JLabel l = new JLabel(txt);
        l.setFont(Ui.font(11, false));
        l.setForeground(Theme.muted());
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        JTextField f = new JTextField("（这里输入待办内容）");
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        f.setPreferredSize(new Dimension(300, 30));
        f.setMaximumSize(new Dimension(300, 30));
        box.add(l);
        box.add(f);
        box.setSize(330, 74);
        layoutAll(box);
        shot(box, 330, 74, "C:\\Users\\chen\\Desktop\\ui-50-new-todo-ctx.png");
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
