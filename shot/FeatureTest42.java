import com.toolbox.pc.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** v1.61 复核：节奏行七天圆点的实际像素颜色 + 放大图 */
public class FeatureTest42 {

    private static boolean ok = true;

    public static void main(String[] a) {
        try {
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

            Field dotsF = OverviewPanel.class.getDeclaredField("weekDots");
            dotsF.setAccessible(true);
            JPanel dots = (JPanel) dotsF.get(ov);
            dots.setSize(74, 18);
            BufferedImage img = new BufferedImage(74, 18, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, 74, 18);
            dots.paint(g);          // 组件非显示态也能直接画出来
            g.dispose();

            int d = 8, gap = 2;
            int prime = Ui.PRIMARY.getRGB();
            int idle = new Color(0xCFD9E8).getRGB();
            int c0 = img.getRGB(3 + 4, 5 + 4);            // 周一 11-09 有完成
            int c1 = img.getRGB(3 + (d + gap) + 4, 5 + 4); // 周二 没完成
            int c2 = img.getRGB(3 + 2 * (d + gap) + 4, 5 + 4); // 周三 今天，有完成
            int c3 = img.getRGB(3 + 3 * (d + gap) + 4, 5 + 4); // 周四 没完成
            check("周一圆点是主题蓝", c0 == prime, hex(c0) + " vs " + hex(prime));
            check("周二圆点是浅灰", c1 == idle, hex(c1) + " vs " + hex(idle));
            check("周三（今天）也是主题蓝", c2 == prime, hex(c2));
            check("周四圆点是浅灰", c3 == idle, hex(c3));

            /* 放大 6 倍导出，肉眼核对今天那圈描边 */
            int scale = 6;
            BufferedImage big = new BufferedImage(74 * scale, 18 * scale, BufferedImage.TYPE_INT_RGB);
            Graphics2D bg = big.createGraphics();
            bg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            bg.drawImage(img, 0, 0, 74 * scale, 18 * scale, null);
            bg.dispose();
            ImageIO.write(big, "png", new File("C:\\Users\\chen\\Desktop\\ui-195-dots-zoom.png"));
            System.out.println("saved ui-195 444x108");

            System.out.println(ok ? "ALL PASS" : "SOME FAIL");
        } catch (Throwable t) {
            t.printStackTrace();
            ok = false;
        }
        Runtime.getRuntime().halt(ok ? 0 : 1);
    }

    static String hex(int rgb) {
        return String.format("#%06X", rgb & 0xFFFFFF);
    }

    static void check(String name, boolean pass, String detail) {
        if (!pass) ok = false;
        System.out.println((pass ? "[PASS] " : "[FAIL] ") + name + " → " + detail);
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
