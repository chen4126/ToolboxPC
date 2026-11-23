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

/** v1.83 真机核对：番茄钟自动记备忘录 / 设置页番茄钟与标签管理 / 统计小结三个按钮 */
public class ShotA83 {

    static Class<?> mc;
    static Object main;
    static JFrame frame;

    public static void main(String[] a) throws Exception {
        LocalDate t = LocalDate.now();
        String ym = t.toString().substring(0, 7);

        /* 番茄钟：目标 8，今天 3 个 */
        List<String> pf = new ArrayList<String>();
        pf.add("25");
        pf.add("5");
        pf.add(t.toString());
        pf.add("3");
        pf.add("1");
        pf.add("写周报");
        pf.add("8");
        Store.writeLines("pomodoro.txt", pf);

        /* 番茄日志：让小结条有「本月最专注」 */
        List<String> plog = new ArrayList<String>();
        long ts = System.currentTimeMillis() - 90000000L;
        String[] days = {ym + "-03", ym + "-04", ym + "-06"};
        int[] mins = {50, 75, 100};
        for (int i = 0; i < days.length; i++) {
            plog.add((ts + i) + "\t" + days[i] + " 09:30\t" + mins[i] + "\t专注\t写周报");
        }
        plog.add((ts + 10) + "\t" + ym + "-06 10:30\t25\t专注\t写周报");
        plog.add((ts + 11) + "\t" + ym + "-06 11:30\t25\t专注\t评审");
        for (int i = 0; i < 3; i++) {
            plog.add((ts + 20 + i) + "\t" + t + " 0" + (9 + i) + ":00\t25\t专注\t写周报");
        }
        Store.writeLines("pomodoro_log.txt", plog);

        /* 备忘录：带番茄自动记的那几条 */
        List<String> memos = new ArrayList<String>();
        memos.add("0\t11-12 09:00\t专注 25 分钟 · 写周报\t番茄");
        memos.add("0\t11-12 10:30\t专注 50 分钟 · 写周报\t番茄");
        memos.add("0\t11-11 20:00\t交房租，记得开发票\t生活,账单");
        memos.add("0\t11-12 08:00\t买牛奶和鸡蛋\t生活");
        memos.add("1\t11-12 08:30\t周报模板在共享盘 /团队/周报/\t工作");
        Store.writeLines("memo.txt", memos);

        /* settings：自动记备忘录默认开，另存点别的键 */
        List<String> set = new ArrayList<String>();
        set.add("sound=0");
        set.add("strong=0");
        set.add("pin=1");
        Store.writeLines("settings.txt", set);

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

        /* 1. 备忘录：自动记进来的番茄条目 + 标签下拉 */
        nav("memo");
        shot("ui-280-memo-pomo.png");

        /* 2. 设置页：滑到「番茄钟 / 备忘录标签」那一段 */
        nav("settings");
        scrollTo(0.72);
        shot("ui-281-settings-pomo-tag.png");

        /* 3. 设置页最底：标签管理 + 数据区 */
        scrollTo(1.0);
        shot("ui-282-settings-tag-data.png");

        /* 4. 统计小结明细弹窗：三个按钮（非模态显示只为了截图） */
        Object sp = page("com.toolbox.pc.StatsPanel");
        JPanel box = null;
        if (sp != null) {
            Method bm = sp.getClass().getMethod("buildFocusDetailBox");
            box = (JPanel) bm.invoke(sp);
        }
        if (box != null) {
            Object[] opts = {"复制这一天", "用这个任务再专注一轮", "关闭"};
            final JPanel content = box;
            final JDialog[] dh = new JDialog[1];
            SwingUtilities.invokeAndWait(() -> {
                JOptionPane pane = new JOptionPane(content, JOptionPane.PLAIN_MESSAGE,
                        JOptionPane.DEFAULT_OPTION, null, opts, opts[2]);
                JDialog d = pane.createDialog(frame, "专注小结");
                d.setModal(false);
                d.setAlwaysOnTop(true);
                d.pack();
                d.setLocation(frame.getX() + 260, frame.getY() + 150);
                d.setVisible(true);
                dh[0] = d;
            });
            Thread.sleep(900);
            shotComp(dh[0].getContentPane(), "ui-283-focus-detail.png");
            SwingUtilities.invokeAndWait(() -> dh[0].dispose());
            Thread.sleep(500);
        }

        Runtime.getRuntime().halt(0);
    }

    /** 截某个组件自己的样子（弹窗用） */
    static void shotComp(Container root, String name) throws Exception {
        final java.awt.image.BufferedImage[] holder = new java.awt.image.BufferedImage[1];
        SwingUtilities.invokeAndWait(() -> {
            int w = root.getWidth(), h = root.getHeight();
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

    /** 把设置页滚动条拖到比例处（0 顶 1 底） */
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
        final java.awt.image.BufferedImage[] holder = new java.awt.image.BufferedImage[1];
        SwingUtilities.invokeAndWait(() -> {
            Container root = frame.getContentPane();
            int w = root.getWidth(), h = root.getHeight();
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
