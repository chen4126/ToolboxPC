import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;

/** 探针：打印左侧导航每个子组件的位置，定位"隐藏模块后整体偏下"的原因 */
public class ProbeNav {
    public static void main(String[] a) throws Exception {
        Theme.init();
        Ui.installDefaults();
        SwingUtilities.invokeAndWait(() -> Main.main(new String[0]));
        Thread.sleep(2500);

        Class<?> mc = Class.forName("com.toolbox.pc.Main");
        Field selfF = mc.getDeclaredField("self");
        selfF.setAccessible(true);
        Object self = selfF.get(null);
        Field f = mc.getDeclaredField("navPanel");
        f.setAccessible(true);
        JPanel nav = (JPanel) f.get(self);
        System.out.println("nav size=" + nav.getWidth() + "x" + nav.getHeight()
                + " pref=" + nav.getPreferredSize().width + "x" + nav.getPreferredSize().height);
        int i = 0;
        for (Component c : nav.getComponents()) {
            String name = c.getClass().getSimpleName();
            String extra = "";
            if (c instanceof JComponent && ((JComponent) c).getClientProperty("navKey") != null)
                extra = " key=" + ((JComponent) c).getClientProperty("navKey");
            if (c instanceof NavButton) extra += " text=" + ((NavButton) c).getText();
            System.out.println("[" + i + "] " + name + " y=" + c.getY() + " h=" + c.getHeight()
                    + " vis=" + c.isVisible() + " maxH=" + c.getMaximumSize().height + extra);
            i++;
        }

        /* 出图核对：整条侧栏按真实 220×721 画一遍 */
        nav.setDoubleBuffered(false);
        if (nav.getParent() != null) nav.getParent().remove(nav);
        nav.setSize(220, nav.getHeight() > 0 ? nav.getHeight() : 721);
        nav.doLayout();
        layoutAll(nav);
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(nav.getWidth(), nav.getHeight(),
                java.awt.image.BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        nav.paint(g);
        g.dispose();
        String out = "C:\\Users\\chen\\Desktop\\ui-254-nav.png";
        javax.imageio.ImageIO.write(img, "png", new java.io.File(out));
        System.out.println("SHOT " + new java.io.File(out).length());
        System.exit(0);
    }

    static void layoutAll(Component c) {
        c.doLayout();
        if (c instanceof Container) for (Component k : ((Container) c).getComponents()) layoutAll(k);
    }
}
