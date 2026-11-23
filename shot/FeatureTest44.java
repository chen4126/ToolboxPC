import com.toolbox.pc.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** v1.62 复核：完成 2 条的那天颜色更深，今天那圈还是正蓝；附带放大图 */
public class FeatureTest44 {

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
            Thread.sleep(300);

            Field dotsF = OverviewPanel.class.getDeclaredField("weekDots");
            dotsF.setAccessible(true);
            JPanel dots = (JPanel) dotsF.get(ov);
            dots.setSize(74, 18);
            BufferedImage img = new BufferedImage(74, 18, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, 74, 18);
            dots.paint(g);
            g.dispose();

            int d = 8, gap = 2;
            int deep = new Color(0x3730A3).getRGB();
            int prime = Ui.PRIMARY.getRGB();
            int idle = new Color(0xCFD9E8).getRGB();
            int c0 = img.getRGB(3 + 4, 9);                    // 周一 1 条
            int c1 = img.getRGB(3 + (d + gap) + 4, 9);        // 周二 0 条
            int c2 = img.getRGB(3 + 2 * (d + gap) + 4, 9);    // 周三 2 条（今天）
            check("周一 1 条 = 主题蓝", c0 == prime, hex(c0));
            check("周二 0 条 = 浅灰", c1 == idle, hex(c1));
            check("周三 2 条 = 更深靛蓝", c2 == deep, hex(c2));
            check("深色比主题蓝更深", luminance(deep) < luminance(prime),
                    String.format("deep=%.1f prime=%.1f", luminance(deep), luminance(prime)));

            int scale = 6;
            BufferedImage big = new BufferedImage(74 * scale, 18 * scale, BufferedImage.TYPE_INT_RGB);
            Graphics2D bg = big.createGraphics();
            bg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            bg.drawImage(img, 0, 0, 74 * scale, 18 * scale, null);
            bg.dispose();
            ImageIO.write(big, "png", new File("C:\\Users\\chen\\Desktop\\ui-205-dots2x.png"));
            System.out.println("saved ui-205 444x108");
            System.out.println(ok ? "ALL PASS" : "SOME FAIL");
        } catch (Throwable t) {
            t.printStackTrace();
            ok = false;
        }
        Runtime.getRuntime().halt(ok ? 0 : 1);
    }

    static double luminance(int rgb) {
        return 0.299 * ((rgb >> 16) & 0xFF) + 0.587 * ((rgb >> 8) & 0xFF) + 0.114 * (rgb & 0xFF);
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
