import com.toolbox.pc.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** v2.0 真机核对：概览 todoStat / 番茄记录弹窗（今天 + 汇总视图）/ 设置页预设 */
public class ShotA85 {

    static Class<?> mc;
    static Object main;
    static JFrame frame;

    public static void main(String[] a) throws Exception {
        LocalDate t = LocalDate.now();
        String today = t.toString();
        String ws = PomodoroPanel.weekStart();

        List<String> pf = new ArrayList<String>();
        pf.add("25");
        pf.add("5");
        pf.add(today);
        pf.add("3");
        pf.add("1");
        pf.add("写周报");
        pf.add("8");
        Store.writeLines("pomodoro.txt", pf);

        /* 番茄日志：本周多条，两个任务 + 一条没写任务名 */
        List<String> plog = new ArrayList<String>();
        plog.add("1\t" + ws + " 09:10\t25\t专注\t整理需求清单");
        String mid = t.minusDays(1).toString();
        if (mid.compareTo(ws) < 0) mid = ws;
        plog.add("2\t" + mid + " 15:20\t50\t专注\t写周报");
        plog.add("3\t" + today + " 09:00\t25\t专注\t写周报");
        plog.add("4\t" + today + " 10:30\t25\t专注\t");
        plog.add("5\t" + today + " 11:05\t5\t休息\t");
        Store.writeLines("pomodoro_log.txt", plog);

        /* 今天的待办：3 条，1 条已完成 */
        List<String> todos = new ArrayList<String>();
        todos.add("1\t写实习周报\t" + today);
        todos.add("0\t给妈妈回电话\t" + today);
        todos.add("0\t晚上跑步 3 公里\t" + today);
        Store.writeLines("todo.txt", todos);

        List<String> memos = new ArrayList<String>();
        memos.add("0\t11-15 09:00\t买牛奶和鸡蛋\t生活");
        memos.add("0\t11-15 10:00\t周报模板在共享盘\t工作");
        Store.writeLines("memo.txt", memos);

        SwingUtilities.invokeAndWait(() -> Main.main(new String[0]));
        Thread.sleep(2600);
        mc = Class.forName("com.toolbox.pc.Main");
        Field selfF = mc.getDeclaredField("self");
        selfF.setAccessible(true);
        main = selfF.get(null);
        Field ff = mc.getDeclaredField("frame");
        ff.setAccessible(true);
        frame = (JFrame) ff.get(main);
        SwingUtilities.invokeAndWait(() -> {
            frame.setAlwaysOnTop(true);
            frame.toFront();
            frame.requestFocus();
        });
        Thread.sleep(1200);

        /* 1. 概览页：待办卡标题行有「共 3 项」 */
        nav("overview");
        shot("ui-288-overview-todostat.png");

        /* 2. 番茄记录弹窗：今天视图（底部三个按钮） */
        Object pp = page("com.toolbox.pc.PomodoroPanel");
        Method open = pp.getClass().getMethod("openHistory");
        SwingUtilities.invokeAndWait(() -> {
            try {
                open.invoke(pp);
            } catch (Exception ignored) {
            }
        });
        Thread.sleep(900);
        JDialog dlg = null;
        for (Window w : Window.getWindows()) {
            if (w instanceof JDialog && "番茄记录".equals(((JDialog) w).getTitle()) && w.isShowing()) {
                dlg = (JDialog) w;
            }
        }
        if (dlg != null) {
            shotComp(dlg.getRootPane(), "ui-289-pomo-history-v2.png");

            /* 3. 切到「汇总」视图 */
            final JDialog dd = dlg;
            SwingUtilities.invokeAndWait(() -> clickRadio(dd, "汇总"));
            Thread.sleep(600);
            shotComp(dd.getRootPane(), "ui-290-pomo-history-sum.png");
            SwingUtilities.invokeAndWait(dd::dispose);
        } else {
            System.out.println("NO DIALOG");
        }
        Thread.sleep(600);

        /* 4. 设置页：预设下拉与关于行 v2.0 */
        nav("settings");
        scrollTo(1.0);
        shot("ui-291-settings-preset-v2.png");

        Runtime.getRuntime().halt(0);
    }

    static void clickRadio(Container root, String text) {
        for (Component k : root.getComponents()) {
            if (k instanceof JRadioButton && text.equals(((JRadioButton) k).getText())) {
                ((JRadioButton) k).doClick();
                return;
            }
            if (k instanceof Container) clickRadio((Container) k, text);
        }
    }

    static void scrollTo(double ratio) throws Exception {
        Object sp = page("com.toolbox.pc.SettingsPanel");
        SwingUtilities.invokeAndWait(() -> {
            JScrollPane pane = findScroll((Container) sp);
            if (pane == null) return;
            JScrollBar bar = pane.getVerticalScrollBar();
            bar.setValue((int) ((bar.getMaximum() - bar.getVisibleAmount()) * ratio));
        });
        Thread.sleep(700);
    }

    static JScrollPane findScroll(Container c) {
        for (Component k : c.getComponents()) {
            if (k instanceof JScrollPane) return (JScrollPane) k;
            if (k instanceof Container) {
                JScrollPane r = findScroll((Container) k);
                if (r != null) return r;
            }
        }
        return null;
    }

    static Object page(String cls) throws Exception {
        Method pageM = mc.getDeclaredMethod("page", Class.class);
        pageM.setAccessible(true);
        return pageM.invoke(main, Class.forName(cls));
    }

    static void nav(String key) throws Exception {
        Method sel = mc.getDeclaredMethod("selectNav", String.class);
        sel.setAccessible(true);
        SwingUtilities.invokeAndWait(() -> {
            try {
                sel.invoke(main, key);
            } catch (Exception ignored) {
            }
        });
        Thread.sleep(900);
    }

    static void shot(String name) throws Exception {
        shotComp(frame.getContentPane(), name);
    }

    static void shotComp(Container root, String name) throws Exception {
        final java.awt.image.BufferedImage[] holder = new java.awt.image.BufferedImage[1];
        SwingUtilities.invokeAndWait(() -> {
            int w = Math.max(1, root.getWidth()), h = Math.max(1, root.getHeight());
            java.awt.image.BufferedImage bi =
                    new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = bi.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            root.printAll(g2);
            g2.dispose();
            holder[0] = bi;
        });
        File out = new File("C:\\Users\\chen\\Desktop\\" + name);
        ImageIO.write(holder[0], "png", out);
        System.out.println("SHOT " + name + " " + out.length() + " "
                + holder[0].getWidth() + "x" + holder[0].getHeight());
    }
}
