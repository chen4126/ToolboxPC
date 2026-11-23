import com.toolbox.pc.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** v1.82 真机核对：番茄钟目标进度环 / 概览完成率 / 备忘录标签筛选 / 统计页专注小结 */
public class ShotA82 {

    static Class<?> mc;
    static Object main;
    static JFrame frame;

    public static void main(String[] a) throws Exception {
        LocalDate t = LocalDate.now();
        String ym = t.toString().substring(0, 7);

        /* 番茄钟：目标 8，今天已完成 3 个 */
        List<String> pf = new ArrayList<String>();
        pf.add("25");
        pf.add("5");
        pf.add(t.toString());
        pf.add("3");
        pf.add("1");
        pf.add("写周报");
        pf.add("8");
        Store.writeLines("pomodoro.txt", pf);

        /* 番茄日志：本月造几天，让「本月最专注」和「最长连续」都有内容 */
        List<String> plog = new ArrayList<String>();
        long ts = System.currentTimeMillis() - 90000000L;
        String[] days = {ym + "-02", ym + "-03", ym + "-04", ym + "-06"};
        int[] mins = {50, 75, 25, 100};
        for (int i = 0; i < days.length; i++) {
            plog.add((ts + i) + "\t" + days[i] + " 09:30\t" + mins[i] + "\t专注\t写代码");
        }
        /* 6 号再来两个，凑成本月最专注的一天（100 + 25 + 25 = 150） */
        plog.add((ts + 10) + "\t" + ym + "-06 10:30\t25\t专注\t改文档");
        plog.add((ts + 11) + "\t" + ym + "-06 11:30\t25\t专注\t开会");
        /* 今天 3 个 */
        for (int i = 0; i < 3; i++) {
            plog.add((ts + 20 + i) + "\t" + t + " 0" + (9 + i) + ":00\t25\t专注\t写周报");
        }
        Store.writeLines("pomodoro_log.txt", plog);

        /* 备忘录：4 条，3 个标签 */
        List<String> memos = new ArrayList<String>();
        memos.add("0\t11-10 09:00\t买牛奶和鸡蛋\t生活");
        memos.add("0\t11-11 20:00\t交房租，记得开发票\t生活,账单");
        memos.add("1\t11-12 08:00\t周报模板在共享盘 /团队/周报/\t工作");
        memos.add("0\t11-12 09:30\t看那篇讲 RAG 召回的论文\t学习,工作");
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

        /* 1. 番茄钟：进度环 + 今日已完成 3 / 8 + 目标调整 */
        nav("pomodoro");
        shot("ui-275-pomo-goal.png");

        /* 2. 概览：今日专注卡的完成率 */
        nav("overview");
        shot("ui-276-overview-rate.png");

        /* 3. 备忘录：按「生活」筛，列表右侧显示标签 */
        nav("memo");
        Object mp = page("com.toolbox.pc.MemoPanel");
        Method setTag = mp.getClass().getMethod("setTagFilter", String.class);
        SwingUtilities.invokeAndWait(() -> {
            try {
                setTag.invoke(mp, "生活");
            } catch (Exception ignored) {
            }
        });
        Thread.sleep(900);
        shot("ui-277-memo-tags.png");

        /* 4. 统计：专注小结条 */
        nav("stats");
        shot("ui-278-stats-focus.png");

        Runtime.getRuntime().halt(0);
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

    static boolean moved = false;

    static void shot(String name) throws Exception {
        /* 桌面上有全屏窗口压着，屏幕截图拿到的不是我们的界面；
           改成让组件自己 printAll 到 BufferedImage，内容才可信 */
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
