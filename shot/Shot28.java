import com.toolbox.pc.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** v1.28 三张验收图：待办右键(含改期子菜单) / 记账右键 / 并行超时 5 分 */
public class Shot28 {

    static PrintWriter log;

    public static void main(String[] a) throws Exception {
        log = new PrintWriter(new java.io.FileWriter("C:\\Users\\chen\\Desktop\\shot28.log"), true);
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
                e.printStackTrace(log);
            }
        });
        Thread.sleep(2300);

        Class<?> mc = Class.forName("com.toolbox.pc.Main");
        Field ff = mc.getDeclaredField("frame");
        ff.setAccessible(true);
        final JFrame frame = (JFrame) ff.get(holder[0]);
        Method sel = mc.getDeclaredMethod("selectNav", String.class);
        sel.setAccessible(true);
        int w = frame.getContentPane().getWidth();
        int h = frame.getContentPane().getHeight();
        log.println("frame " + w + "x" + h);

        /* ---------- 1. 待办右键菜单 + 改期子菜单 ---------- */
        SwingUtilities.invokeAndWait(() -> {
            try {
                sel.invoke(holder[0], "overview");
            } catch (Exception e) {
            }
        });
        Thread.sleep(1000);
        OverviewPanel ov = find(frame.getContentPane(), OverviewPanel.class);
        Field tbF = OverviewPanel.class.getDeclaredField("todoBox");
        tbF.setAccessible(true);
        JPanel box = (JPanel) tbF.get(ov);
        JPanel row0 = (JPanel) box.getComponent(0);
        JCheckBox cb = (JCheckBox) row0.getComponent(0);
        JPopupMenu menu = cb.getComponentPopupMenu();
        log.println("menu components=" + menu.getComponentCount());
        for (Component c : menu.getComponents()) log.println("  item: " + (c instanceof JMenuItem
                ? ((JMenuItem) c).getText() : c.getClass().getSimpleName()));

        BufferedImage img = base(frame, w, h);
        Graphics2D g = img.createGraphics();
        menu.setSize(menu.getPreferredSize());
        menu.doLayout();
        for (Component c : menu.getComponents()) if (c instanceof JComponent) ((JComponent) c).doLayout();
        Point pt = SwingUtilities.convertPoint(cb, 0, 0, frame.getContentPane());
        int mx = pt.x + 30;
        int my = pt.y - menu.getHeight() - 8;          // 贴着屏幕底时菜单向上展开
        if (my < 60) my = pt.y + 18;
        log.println("menu at " + mx + "," + my + " size=" + menu.getWidth() + "x" + menu.getHeight());
        g.translate(mx, my);
        menu.paint(g);
        g.translate(-mx, -my);
        JMenu defer = null;
        for (Component c : menu.getComponents()) if (c instanceof JMenu) defer = (JMenu) c;
        if (defer != null) {
            JPopupMenu sub = defer.getPopupMenu();
            sub.setSize(sub.getPreferredSize());
            sub.doLayout();
            for (Component c : sub.getComponents()) if (c instanceof JComponent) ((JComponent) c).doLayout();
            int sx = mx + menu.getWidth() - 3;
            int sy = my + defer.getY();
            g.translate(sx, sy);
            sub.paint(g);
            g.translate(-sx, -sy);
            log.println("submenu items=" + sub.getComponentCount() + " at " + sx + "," + sy);
        } else {
            log.println("submenu not found");
        }
        g.dispose();
        ImageIO.write(img, "png", new File("C:\\Users\\chen\\Desktop\\ui-33-todo-menu.png"));
        log.println("saved ui-33");

        /* ---------- 2. 记账右键菜单 ---------- */
        SwingUtilities.invokeAndWait(() -> {
            try {
                sel.invoke(holder[0], "ledger");
            } catch (Exception e) {
            }
        });
        Thread.sleep(1000);
        LedgerPanel lp = find(frame.getContentPane(), LedgerPanel.class);
        Field tf = LedgerPanel.class.getDeclaredField("table");
        tf.setAccessible(true);
        final JTable table = (JTable) tf.get(lp);
        SwingUtilities.invokeAndWait(() -> table.setRowSelectionInterval(0, 0));
        Method utl = LedgerPanel.class.getDeclaredMethod("updateToggleLabel");
        utl.setAccessible(true);
        utl.invoke(lp);
        JPopupMenu rm = table.getComponentPopupMenu();
        log.println("ledger menu components=" + rm.getComponentCount());
        for (Component c : rm.getComponents()) log.println("  item: " + (c instanceof JMenuItem
                ? ((JMenuItem) c).getText() : c.getClass().getSimpleName()));
        rm.setSize(rm.getPreferredSize());
        rm.doLayout();
        for (Component c : rm.getComponents()) if (c instanceof JComponent) ((JComponent) c).doLayout();
        BufferedImage img2 = base(frame, w, h);
        Graphics2D g2 = img2.createGraphics();
        Rectangle cell = table.getCellRect(0, 0, true);
        Point cp = SwingUtilities.convertPoint(table, cell.x + 120, cell.y + cell.height, frame.getContentPane());
        g2.translate(cp.x, cp.y);
        rm.paint(g2);
        g2.dispose();
        ImageIO.write(img2, "png", new File("C:\\Users\\chen\\Desktop\\ui-34-ledger-menu.png"));
        log.println("saved ui-34");

        /* ---------- 3. 并行超时满 5 分 ---------- */
        SwingUtilities.invokeAndWait(() -> {
            try {
                sel.invoke(holder[0], "countdown");
            } catch (Exception e) {
            }
        });
        Thread.sleep(1000);
        CountDownPanel cd = find(frame.getContentPane(), CountDownPanel.class);
        Field tasksF = CountDownPanel.class.getDeclaredField("tasks");
        tasksF.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Object> tasks = (List<Object>) tasksF.get(cd);
        Class<?> tc = Class.forName("com.toolbox.pc.CountDownPanel$Task");
        java.lang.reflect.Constructor<?> ctor = tc.getDeclaredConstructor();
        ctor.setAccessible(true);
        Object task = ctor.newInstance();
        set(tc, task, "name", "泡面");
        set(tc, task, "remainMs", 0L);
        set(tc, task, "plannedMs", 60000L);
        set(tc, task, "over", 302000L);
        set(tc, task, "overNotified", true);
        set(tc, task, "overStrong", true);
        tasks.clear();
        tasks.add(task);
        SwingUtilities.invokeAndWait(() -> {
            try {
                Method rt = CountDownPanel.class.getDeclaredMethod("renderTasks");
                rt.setAccessible(true);
                rt.invoke(cd);
            } catch (Exception e) {
                e.printStackTrace(log);
            }
        });
        Thread.sleep(800);
        BufferedImage img3 = base(frame, w, h);
        Graphics2D g3 = img3.createGraphics();
        frame.getContentPane().paint(g3);
        g3.dispose();
        ImageIO.write(img3, "png", new File("C:\\Users\\chen\\Desktop\\ui-35-countdown-5min.png"));
        log.println("saved ui-35");
        tasks.clear();

        log.println("done");
        log.close();
        System.exit(0);
    }

    static BufferedImage base(JFrame frame, int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        frame.getContentPane().paint(g);
        return img;
    }

    static void set(Class<?> cls, Object o, String name, Object v) throws Exception {
        Field f = cls.getDeclaredField(name);
        f.setAccessible(true);
        if (v instanceof Long) f.setLong(o, (Long) v);
        else if (v instanceof Boolean) f.setBoolean(o, (Boolean) v);
        else f.set(o, v);
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
