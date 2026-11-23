import com.toolbox.pc.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** 真机核对：设置页「提醒」分区的通知音 + 音效下拉 + 试听 */
public class ShotSoundWin {

    public static void main(String[] a) throws Exception {
        SwingUtilities.invokeAndWait(() -> Main.main(new String[0]));
        Thread.sleep(2600);
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
                sel.invoke(main, "settings");
            } catch (Exception ignored) {
            }
        });
        Thread.sleep(1000);

        Method pageM = mc.getDeclaredMethod("page", Class.class);
        pageM.setAccessible(true);
        Object panel = pageM.invoke(main, Class.forName("com.toolbox.pc.SettingsPanel"));

        JCheckBox soundBox = findCheck((Component) panel, "通知音");
        System.out.println("soundBox=" + (soundBox != null));
        if (soundBox != null) {
            // 通知音没勾时先勾上，好让下拉是亮的（截图要看可用态）
            if (!soundBox.isSelected()) {
                SwingUtilities.invokeAndWait(soundBox::doClick);
                Thread.sleep(300);
            }
            JScrollPane sp = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, soundBox);
            if (sp != null) {
                Component view = sp.getViewport().getView();
                Point p0 = SwingUtilities.convertPoint(soundBox, 0, 0, view);
                JScrollBar bar = sp.getVerticalScrollBar();
                bar.setValue(Math.max(0, Math.min(bar.getMaximum(), p0.y - 180)));
                Thread.sleep(500);
            }
            Thread.sleep(600);
        }

        Rectangle b = frame.getBounds();
        BufferedImage img = new Robot().createScreenCapture(b);
        ImageIO.write(img, "png", new File("C:\\Users\\chen\\Desktop\\ui-261-sound-select.png"));
        System.out.println("SHOT " + new File("C:\\Users\\chen\\Desktop\\ui-261-sound-select.png").length());
        Runtime.getRuntime().halt(0);
    }

    static JCheckBox findCheck(Component root, String text) {
        if (root instanceof Container) {
            for (Component c : ((Container) root).getComponents()) {
                if (c instanceof JCheckBox && ((JCheckBox) c).getText().startsWith(text)) return (JCheckBox) c;
            }
            for (Component c : ((Container) root).getComponents()) {
                JCheckBox r = findCheck(c, text);
                if (r != null) return r;
            }
        }
        return null;
    }
}
