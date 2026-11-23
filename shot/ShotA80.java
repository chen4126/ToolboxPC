import com.toolbox.pc.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** v1.80 真机核对：习惯打卡页、工具页两卡、侧栏 12 项 */
public class ShotA80 {

    static Class<?> mc;
    static Object main;
    static JFrame frame;

    public static void main(String[] a) throws Exception {
        LocalDate t = LocalDate.now();

        /* 三个习惯：早起连续 4 天、喝水连续 2 天、运动只在前天打过 */
        List<String> hs = new ArrayList<String>();
        hs.add("早起");
        hs.add("喝水");
        hs.add("运动 20 分钟");
        Store.writeLines("habit.txt", hs);

        List<String> log = new ArrayList<String>();
        for (int i = 0; i < 4; i++) log.add(t.minusDays(i) + "\t早起");
        log.add(t + "\t喝水");
        log.add(t.minusDays(1) + "\t喝水");
        log.add(t.minusDays(2) + "\t运动 20 分钟");
        Store.writeLines("habit_log.txt", log);

        /* 六条速记 */
        List<String> clips = new ArrayList<String>();
        clips.add("11-12 09:12\t端口 8080 被占用，netstat -ano | findstr 8080 查 PID");
        clips.add("11-12 09:40\tSELECT * FROM t_order WHERE create_time > '2026-11-01'");
        clips.add("11-12 10:05\t今天下午三点跟产品对一下问卷系统的越南语字段");
        clips.add("11-12 10:33\tjavascript:void(0)");
        clips.add("11-12 11:02\t论文第二章的可行性分析要补一段竞品数据来源说明");
        clips.add("11-12 11:20\tscp -r ./dist root@1.2.3.4:/var/www/html");
        Store.writeLines("clip.txt", clips);

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

        /* 侧栏是不是被挤出了滚动条 */
        Field nsF = mc.getDeclaredField("navScroll");
        nsF.setAccessible(true);
        JScrollPane navScroll = (JScrollPane) nsF.get(main);
        JScrollBar vbar = navScroll.getVerticalScrollBar();
        System.out.println("navScroll 首选高=" + navScroll.getPreferredSize().height
                + " 可见条=" + vbar.isVisible()
                + " bar " + vbar.getValue() + "/" + vbar.getMaximum()
                + " 视口高=" + navScroll.getViewport().getHeight()
                + " 内容首选高=" + navScroll.getViewport().getView().getPreferredSize().height);

        /* 1. 习惯打卡页 */
        nav("habit");
        shot("ui-266-habit.png");

        /* 2. 工具页 - 剪贴板卡 */
        nav("toolbox");
        shot("ui-267-clip.png");

        /* 3. 工具页 - 文件整理卡（造一个乱目录做预览） */
        File tmp = new File(System.getProperty("java.io.tmpdir"), "toolbox-shot-tidy");
        delTree(tmp);
        tmp.mkdirs();
        for (String n : new String[]{"screenshot-2026.png", "报告-终稿.docx", "成绩汇总.xlsx",
                "素材包.zip", "开工视频.mp4", "接口文档.md", "Main.java", "说明.unknown"}) {
            FileWriter w = new FileWriter(new File(tmp, n));
            w.write("x");
            w.close();
        }
        Method pageM = mc.getDeclaredMethod("page", Class.class);
        pageM.setAccessible(true);
        Object tp = pageM.invoke(main, Class.forName("com.toolbox.pc.ToolboxPanel"));
        Method show = tp.getClass().getMethod("showCard", String.class);
        Method setDir = tp.getClass().getMethod("setCurrentDir", File.class);
        SwingUtilities.invokeAndWait(() -> {
            try {
                show.invoke(tp, "tidy");
                setDir.invoke(tp, tmp);
            } catch (Exception ignored) {
            }
        });
        Thread.sleep(900);
        shot("ui-268-tidy.png");

        /* 4. 整窗（12 项侧栏有没有溢出 / 设置是否还在最底部） */
        nav("overview");
        SwingUtilities.invokeAndWait(() -> {
            frame.setSize(1140, 760);
            frame.validate();
        });
        Thread.sleep(900);
        shot("ui-269-nav.png");

        delTree(tmp);
        Runtime.getRuntime().halt(0);
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
        SwingUtilities.invokeAndWait(() -> {
            if (!moved) {
                /* 挪到屏幕左上角，避免把桌面上别的置顶窗口一起截进来 */
                frame.setLocation(0, 0);
                moved = true;
            }
            frame.setAlwaysOnTop(true);
            frame.toFront();
        });
        Thread.sleep(600);
        Rectangle b = frame.getBounds();
        BufferedImage img = new Robot().createScreenCapture(b);
        File out = new File("C:\\Users\\chen\\Desktop\\" + name);
        ImageIO.write(img, "png", out);
        System.out.println("SHOT " + name + " " + out.length() + " " + img.getWidth() + "x" + img.getHeight());
    }

    static void delTree(File f) {
        if (!f.exists()) return;
        File[] fs = f.listFiles();
        if (fs != null) for (File c : fs) delTree(c);
        f.delete();
    }
}
