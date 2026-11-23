import com.toolbox.pc.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** v1.57 验收：完成清单弹窗、小窗底栏三段、概览完成数三段 */
public class Shot57 {

    public static void main(String[] a) throws Exception {
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
        Thread.sleep(400);

        /* 1. 概览整窗：标题行右侧 本周2 · 上周1 · 本月3 三段 */
        frame.getContentPane().doLayout();
        layoutAll(frame.getContentPane());
        shot(frame.getContentPane(),
                frame.getContentPane().getWidth(), frame.getContentPane().getHeight(),
                "C:\\Users\\chen\\Desktop\\ui-151-overview-done-segs.png");

        /* 2. 完成清单弹窗：本周完成 2 条（11-11 / 11-09） */
        Method dlM = OverviewPanel.class.getDeclaredMethod("doneListPanel",
                String.class, List.class, JDialog.class);
        dlM.setAccessible(true);
        List rows = rowsOf(ov, "week");
        JPanel panel = (JPanel) dlM.invoke(ov, "本周完成 · 共 " + rows.size() + " 条", rows, null);
        panel.setSize(520, 178);
        layoutAll(panel);
        shot(panel, 520, 178, "C:\\Users\\chen\\Desktop\\ui-152-done-list-week.png");

        /* 3. 小窗过期视图底栏：还有 1 条… / 最早 11-01 / 全部勾完 5 条 */
        Field instF = MiniBoard.class.getDeclaredField("inst");
        instF.setAccessible(true);
        Object board = instF.get(null);
        if (board == null) {
            SwingUtilities.invokeAndWait(() -> MiniBoard.show());
            Thread.sleep(900);
            board = instF.get(null);
        }
        Field ovvF = MiniBoard.class.getDeclaredField("overdueView");
        ovvF.setAccessible(true);
        ovvF.set(board, true);
        Method br = MiniBoard.class.getDeclaredMethod("refresh");
        br.setAccessible(true);
        br.invoke(board);
        Thread.sleep(900);
        Field winF = MiniBoard.class.getDeclaredField("win");
        winF.setAccessible(true);
        Object win = winF.get(board);
        Container root = (win instanceof JFrame) ? ((JFrame) win).getContentPane()
                : (Container) ((java.awt.Window) win).getComponents()[0];
        root.doLayout();
        layoutAll(root);
        shot(root, root.getWidth(), root.getHeight(),
                "C:\\Users\\chen\\Desktop\\ui-153-miniboard-early.png");
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
