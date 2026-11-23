import com.toolbox.pc.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** 截断诊断：概览/统计在窄宽度下的文字是否被裁 */
public class Shot67 {

    static Object main;
    static JFrame frame;

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
        main = selfF.get(null);
        Field ff = mc.getDeclaredField("frame");
        ff.setAccessible(true);
        frame = (JFrame) ff.get(main);

        for (String page : new String[]{"overview", "stats"}) {
            go(page);
            Thread.sleep(900);
            Container root = frame.getContentPane();
            for (int w : new int[]{1124, 960, 900}) {
                root.setSize(w, 721);
                layoutAll(root);
                shot(root, w, 721, "C:\\Users\\chen\\Desktop\\diag-" + page + "-" + w + ".png");
                report(root, page + "@" + w);
            }
        }
        MiniBoard.show();
        Thread.sleep(800);
        Class<?> mbc = Class.forName("com.toolbox.pc.MiniBoard");
        Field instF = mbc.getDeclaredField("inst");
        instF.setAccessible(true);
        MiniBoard board = (MiniBoard) instF.get(null);
        Field bodyF = MiniBoard.class.getDeclaredField("body");
        bodyF.setAccessible(true);
        JComponent body = (JComponent) bodyF.get(board);
        Method bh = MiniBoard.class.getDeclaredMethod("boardHeight");
        bh.setAccessible(true);
        int hgt = (Integer) bh.invoke(board);
        body.setSize(254, hgt);
        layoutAll(body);
        shot(body, 254, hgt, "C:\\Users\\chen\\Desktop\\diag-mini.png");
        report(body, "mini");
    }

    /** 打印所有文字被裁的 JLabel：文本宽度 > 标签可用宽度 */
    static void report(Container root, String tag) {
        List<JLabel> labels = new ArrayList<JLabel>();
        collect(root, labels);
        for (JLabel l : labels) {
            String txt = l.getText();
            if (txt == null || txt.isEmpty() || txt.startsWith("<html>")) continue;
            if (!l.isShowing() && l.getWidth() <= 0) continue;
            FontMetrics fm = l.getFontMetrics(l.getFont());
            int need = fm.stringWidth(txt);
            int have = l.getWidth() - l.getInsets().left - l.getInsets().right;
            if (have > 0 && need > have + 1) {
                System.out.println("[CLIP] " + tag + " \"" + txt + "\" need=" + need + " have=" + have);
            }
        }
    }

    static void collect(Container p, List<JLabel> out) {
        for (Component c : p.getComponents()) {
            if (c instanceof JLabel) out.add((JLabel) c);
            if (c instanceof Container) collect((Container) c, out);
        }
    }

    static void go(String key) throws Exception {
        Method sel = Class.forName("com.toolbox.pc.Main").getDeclaredMethod("selectNav", String.class);
        sel.setAccessible(true);
        SwingUtilities.invokeAndWait(() -> {
            try {
                sel.invoke(main, key);
            } catch (Exception ignored) {
            }
        });
    }

    static void shot(Component c, int w, int h, String path) throws Exception {
        BufferedImage img = new BufferedImage(Math.max(1, w), Math.max(1, h), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        c.paint(g);
        g.dispose();
        ImageIO.write(img, "png", new File(path));
        System.out.println("saved " + path);
    }

    static void layoutAll(Component c) {
        c.doLayout();
        if (c instanceof Container) for (Component k : ((Container) c).getComponents()) layoutAll(k);
    }
}
