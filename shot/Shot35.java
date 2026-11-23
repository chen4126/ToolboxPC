import com.toolbox.pc.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** v1.35 验收图：过期按天筛选 / 小窗全部勾完 / 归档清单 */
public class Shot35 {

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

        OverviewPanel ov = find(frame.getContentPane(), OverviewPanel.class);
        Method refresh = OverviewPanel.class.getDeclaredMethod("refresh");
        refresh.setAccessible(true);
        SwingUtilities.invokeAndWait(() -> {
            try {
                refresh.invoke(ov);
            } catch (Exception e) {
            }
        });
        Thread.sleep(500);
        shot(frame.getContentPane(), frame.getContentPane().getWidth(),
                frame.getContentPane().getHeight(), "C:\\Users\\chen\\Desktop\\ui-54-overdue-filter.png");

        /* 点 11-02 的 chip，只留那天 */
        Field fbF = OverviewPanel.class.getDeclaredField("futureBar");
        fbF.setAccessible(true);
        JPanel bar = (JPanel) fbF.get(ov);
        JLabel chip = null;
        for (Component c : bar.getComponents()) {
            if (c instanceof JLabel && ((JLabel) c).getText().startsWith("11-02 · 2 条")) chip = (JLabel) c;
        }
        if (chip != null) {
            java.awt.event.MouseEvent ev = new java.awt.event.MouseEvent(chip,
                    java.awt.event.MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 1, 1, 1, false);
            for (java.awt.event.MouseListener ml : chip.getMouseListeners()) ml.mouseClicked(ev);
        }
        Thread.sleep(500);
        layoutAll(frame.getContentPane());
        Component card = ((Component) bar).getParent().getParent();
        shot(card, card.getWidth(), card.getHeight(), "C:\\Users\\chen\\Desktop\\ui-55-filtered.png");

        /* 清掉筛选，回到正常 */
        JLabel clear = null;
        for (Component c : bar.getComponents()) {
            if (c instanceof JLabel && ((JLabel) c).getText().startsWith("11-02 · 2 条")) clear = (JLabel) c;
        }
        if (clear != null) {
            java.awt.event.MouseEvent ev = new java.awt.event.MouseEvent(clear,
                    java.awt.event.MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 1, 1, 1, false);
            for (java.awt.event.MouseListener ml : clear.getMouseListeners()) ml.mouseClicked(ev);
        }
        Thread.sleep(400);

        /* 小窗：过期视图 + 全部勾完 */
        SwingUtilities.invokeAndWait(() -> MiniBoard.toggle());
        Thread.sleep(900);
        Field instF = MiniBoard.class.getDeclaredField("inst");
        instF.setAccessible(true);
        Object mb = instF.get(null);
        Field winF = MiniBoard.class.getDeclaredField("win");
        winF.setAccessible(true);
        JFrame win = (JFrame) winF.get(mb);
        Field ovF = MiniBoard.class.getDeclaredField("overdueView");
        ovF.setAccessible(true);
        Method rf = MiniBoard.class.getDeclaredMethod("refresh");
        rf.setAccessible(true);
        SwingUtilities.invokeAndWait(() -> {
            try {
                ovF.setBoolean(mb, true);
                rf.invoke(mb);
            } catch (Exception e) {
            }
        });
        Thread.sleep(600);
        Container wc = win.getContentPane();
        layoutAll(wc);
        shot(wc, win.getWidth(), win.getHeight(), "C:\\Users\\chen\\Desktop\\ui-56-board-all.png");
        SwingUtilities.invokeAndWait(() -> MiniBoard.toggle());
        Thread.sleep(400);

        /* 归档清单（按对话框里的同一套控件复刻） */
        Method arch = OverviewPanel.class.getDeclaredMethod("archivedTodos");
        arch.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.List<Object> list = (java.util.List<Object>) arch.invoke(ov);
        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        JLabel tip = new JLabel("超过 30 天没动的条目已自动收起，点「恢复」会回到今天待办");
        tip.setFont(Ui.font(11, false));
        tip.setForeground(Theme.muted());
        tip.setAlignmentX(Component.LEFT_ALIGNMENT);
        tip.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        box.add(tip);
        for (Object t : list) {
            Field tf = t.getClass().getDeclaredField("text");
            tf.setAccessible(true);
            Field df = t.getClass().getDeclaredField("date");
            df.setAccessible(true);
            String when = String.valueOf(df.get(t));
            if (when.length() >= 10) when = when.substring(5);
            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setOpaque(false);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.setMaximumSize(new Dimension(340, 28));
            JLabel name = new JLabel(when + "　" + tf.get(t));
            name.setFont(Ui.font(11, false));
            JButton back = new JButton("恢复");
            back.setFont(Ui.font(10, false));
            row.add(name, BorderLayout.CENTER);
            row.add(back, BorderLayout.EAST);
            box.add(row);
        }
        box.setSize(420, 52 + list.size() * 28);
        layoutAll(box);
        shot(box, 420, 52 + list.size() * 28, "C:\\Users\\chen\\Desktop\\ui-57-archive.png");
        System.exit(0);
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
