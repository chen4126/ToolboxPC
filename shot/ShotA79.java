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

/** v1.79 真机核对：计算器/文本工具切换、日程页倒数日条、设置页久坐喝水区 */
public class ShotA79 {

    static Class<?> mc;
    static Object main;
    static JFrame frame;

    public static void main(String[] a) throws Exception {
        /* 先造三条倒数日：未来 38 天 / 今天 / 已过 10 天 */
        LocalDate today = LocalDate.now();
        List<String> days = new ArrayList<String>();
        days.add("四级考试\t" + today.plusDays(38));
        days.add("交材料\t" + today);
        days.add("上次体检\t" + today.minusDays(10));
        Store.writeLines("days.txt", days);

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

        /* 1. 计算器页（新的两个切换标签） */
        nav("calc");
        shot("ui-262-calc.png");

        /* 2. 文本工具卡 */
        Method pageM = mc.getDeclaredMethod("page", Class.class);
        pageM.setAccessible(true);
        Object cp = pageM.invoke(main, Class.forName("com.toolbox.pc.CalculatorPanel"));
        Method show = cp.getClass().getDeclaredMethod("showCard", String.class);
        show.setAccessible(true);
        SwingUtilities.invokeAndWait(() -> {
            try {
                show.invoke(cp, "text");
            } catch (Exception ignored) {
            }
        });
        Thread.sleep(700);
        shot("ui-263-text.png");

        /* 3. 日程页顶部倒数日 */
        nav("schedule");
        shot("ui-264-days.png");

        /* 4. 设置页「久坐 / 喝水」 */
        nav("settings");
        Object sp = pageM.invoke(main, Class.forName("com.toolbox.pc.SettingsPanel"));
        Field hlF = sp.getClass().getDeclaredField("healthStatLabel");
        hlF.setAccessible(true);
        JLabel hl = (JLabel) hlF.get(sp);
        JScrollPane pane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, hl);
        if (pane != null) {
            Component view = pane.getViewport().getView();
            Point p0 = SwingUtilities.convertPoint(hl, 0, 0, view);
            JScrollBar bar = pane.getVerticalScrollBar();
            int v = Math.max(0, Math.min(bar.getMaximum(), p0.y + 260));
            bar.setValue(v);
            Thread.sleep(600);
            System.out.println("scroll " + v + " max=" + bar.getMaximum());
        }
        Thread.sleep(500);
        shot("ui-265-health.png");

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

    static void shot(String name) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            frame.setAlwaysOnTop(true);
            frame.toFront();
        });
        Thread.sleep(500);
        Rectangle b = frame.getBounds();
        BufferedImage img = new Robot().createScreenCapture(b);
        File out = new File("C:\\Users\\chen\\Desktop\\" + name);
        ImageIO.write(img, "png", out);
        System.out.println("SHOT " + name + " " + out.length() + " " + img.getWidth() + "x" + img.getHeight());
    }
}
