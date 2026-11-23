import com.toolbox.pc.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** v1.61 验收：节奏行七天圆点（整窗 + 局部）、清单多选退回按钮、小窗本周完成行 */
public class Shot61 {

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
        OverviewPanel ov = find(frame.getContentPane(), OverviewPanel.class);
        Method refresh = OverviewPanel.class.getDeclaredMethod("refresh");
        refresh.setAccessible(true);
        refresh.invoke(ov);
        Thread.sleep(500);

        frame.getContentPane().doLayout();
        layoutAll(frame.getContentPane());
        Container root = frame.getContentPane();
        shot(root, root.getWidth(), root.getHeight(),
                "C:\\Users\\chen\\Desktop\\ui-191-overview-dots.png");

        Field paceF = OverviewPanel.class.getDeclaredField("weekDots");
        paceF.setAccessible(true);
        JPanel dots = (JPanel) paceF.get(ov);
        Rectangle r = SwingUtilities.convertRectangle(dots.getParent().getParent(),
                dots.getParent().getBounds(), root);
        BufferedImage full = render(root);
        int w = Math.min(r.width, full.getWidth() - r.x);
        int h = Math.min(r.height, full.getHeight() - r.y);
        ImageIO.write(full.getSubimage(r.x, r.y, Math.max(1, w), Math.max(1, h)),
                "png", new File("C:\\Users\\chen\\Desktop\\ui-192-pace-row.png"));
        System.out.println("saved ui-192 " + w + "x" + h);

        /* 清单窗口：带"退回选中的"按钮 */
        Method subM = OverviewPanel.class.getDeclaredMethod("doneSubTitle");
        subM.setAccessible(true);
        String sub = (String) subM.invoke(ov);
        Method dlM = OverviewPanel.class.getDeclaredMethod("doneListPanel",
                String.class, String.class, List.class, JDialog.class);
        dlM.setAccessible(true);
        List month = rowsOf(ov, "month");
        JPanel list = (JPanel) dlM.invoke(ov, "本月完成 · 共 3 条", sub, month, null);
        list.setSize(520, 244);
        layoutAll(list);
        shot(list, 520, 244, "C:\\Users\\chen\\Desktop\\ui-193-done-multi.png");

        /* 小窗：第四行本周完成 */
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
        shot(body, body.getWidth(), body.getHeight(),
                "C:\\Users\\chen\\Desktop\\ui-194-mini-pace.png");
    }

    static List<?> rowsOf(OverviewPanel ov, String which) throws Exception {
        java.time.LocalDate now = java.time.LocalDate.now();
        java.time.LocalDate from, to;
        if ("last".equals(which)) {
            java.time.LocalDate mon = now.with(java.time.DayOfWeek.MONDAY).minusWeeks(1);
            from = mon;
            to = mon.plusDays(6);
        } else if ("month".equals(which)) {
            from = now.withDayOfMonth(1);
            to = now.withDayOfMonth(now.lengthOfMonth());
        } else {
            java.time.LocalDate mon = now.with(java.time.DayOfWeek.MONDAY);
            from = mon;
            to = mon.plusDays(6);
        }
        Field tF = OverviewPanel.class.getDeclaredField("todos");
        tF.setAccessible(true);
        List<?> todos = (List<?>) tF.get(ov);
        java.util.List<String[]> rows = new java.util.ArrayList<String[]>();
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
            rows.add(new String[]{text, d.toString()});
        }
        java.util.Collections.sort(rows, (x, y) -> y[1].compareTo(x[1]));
        return rows;
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
