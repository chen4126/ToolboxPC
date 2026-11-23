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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/** v1.81 真机核对：概览 7 张卡、打卡里程碑提示、文件整理按月、剪贴板搜索+常用 */
public class ShotA81 {

    static Class<?> mc;
    static Object main;
    static JFrame frame;

    public static void main(String[] a) throws Exception {
        LocalDate t = LocalDate.now();

        /* 早起连续 6 天（今天还没打，点一下正好第 7 天）；喝水今天已打；运动昨天打过 */
        List<String> hs = new ArrayList<String>();
        hs.add("早起");
        hs.add("喝水");
        hs.add("运动 20 分钟");
        Store.writeLines("habit.txt", hs);
        Store.writeLines("habit_mile.txt", new ArrayList<String>());

        List<String> log = new ArrayList<String>();
        for (int i = 6; i >= 1; i--) log.add(t.minusDays(i) + "\t早起");
        log.add(t + "\t喝水");
        log.add(t.minusDays(1) + "\t运动 20 分钟");
        Store.writeLines("habit_log.txt", log);

        /* 速记：一条已标常用 */
        List<String> clips = new ArrayList<String>();
        clips.add("11-12 09:12\t端口 8080 被占用，netstat -ano | findstr 8080 查 PID");
        clips.add("11-12 09:40\tSELECT * FROM t_order WHERE create_time > '2026-11-01'");
        clips.add("11-12 10:05\t今天下午三点跟产品对一下问卷系统的越南语字段");
        clips.add(ClipNotes.STAR + "11-12 10:33\t周报模板在共享盘 /团队/周报/");
        clips.add("11-12 11:02\t周三下午的周会改成线上");
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

        /* 1. 概览：7 张快捷卡（含「今日打卡 1 / 3」） */
        nav("overview");
        shot("ui-270-overview7.png");

        /* 2. 打卡里程碑：点第一个习惯的勾选框，看有没有弹出庆祝提示 */
        nav("habit");
        Object hp = page("com.toolbox.pc.HabitPanel");
        final JCheckBox box = firstCheck(hp);
        if (box == null) {
            System.out.println("NO-CHECKBOX");
        } else {
            Thread.sleep(3200);                 // 等之前的提示消失，好数这次新弹的
            int before = toastCount();
            SwingUtilities.invokeAndWait(() -> box.doClick(0));
            Thread.sleep(500);
            System.out.println("TOAST before=" + before + " after=" + toastCount());
            shot("ui-271-mile.png");
        }

        /* 3. 工具页：文件整理改成按月（造几个不同修改时间的文件） */
        nav("toolbox");
        File tmp = new File(System.getProperty("java.io.tmpdir"), "toolbox-shot-tidy81");
        delTree(tmp);
        tmp.mkdirs();
        make(tmp, "screenshot-2026.png", 2026, 11, 3);
        make(tmp, "报告-终稿.docx", 2026, 11, 9);
        make(tmp, "成绩汇总.xlsx", 2026, 10, 18);
        make(tmp, "素材包.zip", 2026, 9, 2);
        make(tmp, "开工视频.mp4", 2025, 12, 24);
        make(tmp, "接口文档.md", 2025, 6, 11);
        Object tp = page("com.toolbox.pc.ToolboxPanel");
        Method show = tp.getClass().getMethod("showCard", String.class);
        Method setDir = tp.getClass().getMethod("setCurrentDir", File.class);
        Method setMode = tp.getClass().getMethod("setTidyMode", int.class);
        SwingUtilities.invokeAndWait(() -> {
            try {
                show.invoke(tp, "tidy");
                setDir.invoke(tp, tmp.getAbsoluteFile());
                setMode.invoke(tp, FileTidy.MODE_MONTH);
            } catch (Exception e) {
                System.out.println("ERR " + e);
            }
        });
        Thread.sleep(1000);
        shot("ui-272-tidy-month.png");

        /* 4. 剪贴板：搜索「周」+ 只看常用关掉，点高亮呢 */
        SwingUtilities.invokeAndWait(() -> {
            try {
                show.invoke(tp, "clip");
            } catch (Exception ignored) {
            }
        });
        Thread.sleep(700);
        JTextField search = (JTextField) tp.getClass().getMethod("clipSearchField").invoke(tp);
        final JTextField sf = search;
        SwingUtilities.invokeAndWait(() -> sf.setText("周"));
        Thread.sleep(800);
        shot("ui-273-clip-search.png");

        delTree(tmp);
        Runtime.getRuntime().halt(0);
    }

    static Object page(String cls) throws Exception {
        Method pageM = mc.getDeclaredMethod("page", Class.class);
        pageM.setAccessible(true);
        return pageM.invoke(main, Class.forName(cls));
    }

    static JCheckBox firstCheck(Object panel) {
        for (java.awt.Component c : all((java.awt.Container) panel)) {
            if (c instanceof JCheckBox) return (JCheckBox) c;
        }
        return null;
    }

    static List<java.awt.Component> all(java.awt.Container c) {
        List<java.awt.Component> out = new ArrayList<java.awt.Component>();
        for (java.awt.Component k : c.getComponents()) {
            out.add(k);
            if (k instanceof java.awt.Container) out.addAll(all((java.awt.Container) k));
        }
        return out;
    }

    static void make(File dir, String name, int y, int m, int day) throws Exception {
        File f = new File(dir, name);
        FileWriter w = new FileWriter(f);
        w.write("x");
        w.close();
        long t = LocalDateTime.of(y, m, day, 10, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        System.out.println("MOD " + name + " " + f.setLastModified(t));
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

    static void shotToast(String name) throws Exception {
        JWindow found = null;
        for (Window w : Window.getWindows()) {
            if (!(w instanceof JWindow) || !w.isVisible()) continue;
            String txt = texts(w);
            System.out.println("JWIN " + w.getBounds() + " [" + txt + "]");
            /* Toast 是右下角那条，宽度 300 出头；别把悬浮便签也当成它 */
            if (w.getWidth() > 260 && w.getWidth() < 420 && w.getY() > 300) found = (JWindow) w;
        }
        if (found == null) {
            System.out.println("NO-TOAST");
            return;
        }
        /* 右下角那个位置常被桌面上别的窗口压着，先把它挪到屏幕中间再截 */
        final JWindow fw = found;
        SwingUtilities.invokeAndWait(() -> fw.setLocation(400, 300));
        Thread.sleep(300);
        Rectangle b = found.getBounds();
        b.grow(8, 8);
        BufferedImage img = new Robot().createScreenCapture(b);
        File out = new File("C:\\Users\\chen\\Desktop\\" + name);
        ImageIO.write(img, "png", out);
        System.out.println("SHOT " + name + " " + out.length() + " " + img.getWidth() + "x" + img.getHeight());
    }

    static String texts(java.awt.Container c) {
        StringBuilder b = new StringBuilder();
        for (java.awt.Component k : all(c)) {
            if (k instanceof JLabel) b.append(((JLabel) k).getText()).append(" | ");
        }
        return b.toString();
    }

    static int toastCount() throws Exception {
        Method cm = Class.forName("com.toolbox.pc.Toast").getDeclaredMethod("count");
        cm.setAccessible(true);
        return (Integer) cm.invoke(null);
    }

    static void delTree(File f) {
        if (!f.exists()) return;
        File[] fs = f.listFiles();
        if (fs != null) for (File c : fs) delTree(c);
        f.delete();
    }
}
