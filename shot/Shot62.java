import com.toolbox.pc.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** v1.62 验收：圆点分深浅、清单超 3 条二次确认、小窗节奏行新文案 */
public class Shot62 {

    static OverviewPanel ov;

    public static void main(String[] a) {
        try {
            run();
        } catch (Throwable t) {
            t.printStackTrace();
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
        ov = find(frame.getContentPane(), OverviewPanel.class);
        Method refresh = OverviewPanel.class.getDeclaredMethod("refresh");
        refresh.setAccessible(true);
        refresh.invoke(ov);
        Thread.sleep(400);

        frame.getContentPane().doLayout();
        layoutAll(frame.getContentPane());
        Container root = frame.getContentPane();
        shot(root, root.getWidth(), root.getHeight(), "C:\\Users\\chen\\Desktop\\ui-201-full.png");

        Field paceF = OverviewPanel.class.getDeclaredField("weekDots");
        paceF.setAccessible(true);
        JPanel dots = (JPanel) paceF.get(ov);
        Rectangle r = SwingUtilities.convertRectangle(dots.getParent().getParent(), dots.getParent().getBounds(), root);
        BufferedImage full = render(root);
        int w = Math.min(r.width, full.getWidth() - r.x);
        int h = Math.min(r.height, full.getHeight() - r.y);
        ImageIO.write(full.getSubimage(r.x, r.y, Math.max(1, w), Math.max(1, h)),
                "png", new File("C:\\Users\\chen\\Desktop\\ui-202-pace.png"));
        System.out.println("ui-202 " + w + "x" + h);

        /* 清单：勾 4 条后按钮进入"确认退回 4 条？" */
        Method dlM = OverviewPanel.class.getDeclaredMethod("doneListPanel",
                String.class, String.class, List.class, JDialog.class);
        dlM.setAccessible(true);
        List month = rowsOf("month");
        JPanel list = (JPanel) dlM.invoke(ov, "本月完成 · 共 4 条", "本周 3 条 · 上周 1 条", month, null);
        list.setSize(520, 270);
        layoutAll(list);
        java.util.List<JPanel> rows = doneRows(list);
        for (int i = 0; i < 4 && i < rows.size(); i++) {
            final JPanel t = rows.get(i);
            SwingUtilities.invokeAndWait(() -> t.dispatchEvent(new MouseEvent(t, MouseEvent.MOUSE_CLICKED,
                    System.currentTimeMillis(), InputEvent.CTRL_DOWN_MASK, 4, 4, 1, false, MouseEvent.BUTTON1)));
        }
        layoutAll(list);
        JButton btn = findButton(list, "退回选中的");
        System.out.println("before=" + btn.getText());
        SwingUtilities.invokeAndWait(btn::doClick);
        Thread.sleep(150);
        System.out.println("after=" + btn.getText());
        layoutAll(list);
        shot(list, 520, 270, "C:\\Users\\chen\\Desktop\\ui-203-confirm.png");

        /* 小窗 */
        MiniBoard.show();
        Thread.sleep(600);
        Field instF = MiniBoard.class.getDeclaredField("inst");
        instF.setAccessible(true);
        Object board = instF.get(null);
        Field bodyF = MiniBoard.class.getDeclaredField("body");
        bodyF.setAccessible(true);
        JPanel body = (JPanel) bodyF.get(board);
        body.doLayout();
        layoutAll(body);
        shot(body, body.getWidth(), body.getHeight(), "C:\\Users\\chen\\Desktop\\ui-204-mini.png");
    }

    static List<?> rowsOf(String which) throws Exception {
        java.time.LocalDate now = java.time.LocalDate.now();
        java.time.LocalDate from = now.withDayOfMonth(1);
        java.time.LocalDate to = now.withDayOfMonth(now.lengthOfMonth());
        Field tF = OverviewPanel.class.getDeclaredField("todos");
        tF.setAccessible(true);
        List<?> todos = (List<?>) tF.get(ov);
        java.util.List<String[]> out = new java.util.ArrayList<String[]>();
        for (Object t : todos) {
            Class<?> tc = t.getClass();
            Field doneF = tc.getDeclaredField("done");
            doneF.setAccessible(true);
            Field textF = tc.getDeclaredField("text");
            textF.setAccessible(true);
            Field dateF = tc.getDeclaredField("date");
            dateF.setAccessible(true);
            if (!doneF.getBoolean(t)) continue;
            String text = String.valueOf(textF.get(t));
            if (text.endsWith("（已归档）")) continue;
            java.time.LocalDate d;
            try {
                d = java.time.LocalDate.parse(String.valueOf(dateF.get(t)).trim());
            } catch (Exception e) {
                continue;
            }
            if (d.isBefore(from) || d.isAfter(to)) continue;
            out.add(new String[]{text, d.toString()});
        }
        java.util.Collections.sort(out, (x, y) -> y[1].compareTo(x[1]));
        return out;
    }

    static java.util.List<JPanel> doneRows(JPanel p) {
        java.util.List<JPanel> out = new java.util.ArrayList<JPanel>();
        for (Component c : all(p)) {
            if (!(c instanceof JPanel)) continue;
            JPanel row = (JPanel) c;
            if (row.getMaximumSize() == null || row.getMaximumSize().height != 24) continue;
            if (row.getComponentCount() != 2) continue;
            if (row.getComponent(0) instanceof JLabel && row.getComponent(1) instanceof JLabel) out.add(row);
        }
        return out;
    }

    static JButton findButton(Container p, String prefix) {
        for (Component c : all(p)) {
            if (c instanceof JButton && ((JButton) c).getText() != null
                    && ((JButton) c).getText().startsWith(prefix)) return (JButton) c;
        }
        return null;
    }

    static java.util.List<Component> all(Container p) {
        java.util.List<Component> out = new java.util.ArrayList<Component>();
        for (Component c : p.getComponents()) {
            out.add(c);
            if (c instanceof Container) out.addAll(all((Container) c));
        }
        return out;
    }

    static BufferedImage render(Component c) {
        BufferedImage img = new BufferedImage(Math.max(1, c.getWidth()), Math.max(1, c.getHeight()),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, c.getWidth(), c.getHeight());
        c.paint(g);
        g.dispose();
        return img;
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

    static <T extends Component> T find(Component c, Class<T> type) {
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
