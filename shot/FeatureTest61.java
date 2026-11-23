import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** v1.76 设置页「护眼」区块：控件齐不齐、文案是否完整，并出一张整页图核对 */
public class FeatureTest61 {

    static int pass = 0, fail = 0;

    public static void main(String[] a) throws Exception {
        SwingUtilities.invokeAndWait(() -> com.toolbox.pc.Main.main(new String[0]));
        Thread.sleep(2500);

        Class<?> mc = Class.forName("com.toolbox.pc.Main");
        Field selfF = mc.getDeclaredField("self");
        selfF.setAccessible(true);
        Object main = selfF.get(null);
        Method page = mc.getDeclaredMethod("page", Class.class);
        page.setAccessible(true);
        JPanel sp = (JPanel) page.invoke(main, Class.forName("com.toolbox.pc.SettingsPanel"));
        check("拿到设置页", sp != null);

        String[] found = new String[6];
        int comboCount = 0;
        JComboBox<?> gapBox = null, restBox = null;
        JCheckBox eyeBox = null;
        JLabel statLbl = null;
        int btnCount = 0;
        boolean buttonRest = false, buttonReset = false;

        for (Component c : allChildren(sp)) {
            if (c instanceof JLabel) {
                String t = ((JLabel) c).getText();
                if (t == null) continue;
                if (t.equals("护眼")) found[0] = t;
                if (t.contains("今日用眼")) statLbl = (JLabel) c;
                if (t.contains("5 分钟没有任何鼠标") && t.contains("eye.txt")) found[5] = t;
            }
            if (c instanceof JCheckBox) {
                String t = ((JCheckBox) c).getText();
                if (t != null && t.contains("护眼提醒")) eyeBox = (JCheckBox) c;
            }
            if (c instanceof JComboBox) {
                comboCount++;
                JComboBox<?> cb = (JComboBox<?>) c;
                int n = cb.getItemCount();
                if (n == 5) gapBox = cb;
                if (n == 4 && restBox == null && String.valueOf(cb.getItemAt(0)).contains("秒")) restBox = cb;
            }
            if (c instanceof JButton) {
                String t = ((JButton) c).getText();
                if (t == null) continue;
                if (t.contains("现在休息")) {
                    buttonRest = true;
                    btnCount++;
                }
                if (t.contains("重置本次计时")) {
                    buttonReset = true;
                    btnCount++;
                }
            }
        }

        check("有「护眼」分区标题", found[0] != null);
        check("有护眼提醒勾选框，默认选中", eyeBox != null && eyeBox.isSelected());
        check("提醒间隔下拉 5 档，默认 40 分钟 [" + (gapBox == null ? "null" : gapBox.getSelectedItem()) + "]",
                gapBox != null && "40 分钟".equals(String.valueOf(gapBox.getSelectedItem())));
        check("休息时长下拉 4 档，默认 20 秒 [" + (restBox == null ? "null" : restBox.getSelectedItem()) + "]",
                restBox != null && "20 秒".equals(String.valueOf(restBox.getSelectedItem())));
        check("有今日数据行 [ " + (statLbl == null ? "null" : statLbl.getText()) + " ]", statLbl != null);
        check("有「现在休息一下」按钮", buttonRest);
        check("有「重置本次计时」按钮", buttonReset);
        check("有 5 分钟无操作暂停的说明", found[5] != null);

        // 出图：整页 660 × 1500，护眼区在中间偏下
        sp.setDoubleBuffered(false);
        if (sp.getParent() != null) sp.getParent().remove(sp);
        int W = 620, H = 1480;
        sp.setSize(W, H);
        layoutAll(sp);
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, W, H);
        sp.paint(g);
        g.dispose();
        File out = new File("C:\\Users\\chen\\Desktop\\ui-256-eye-settings.png");
        javax.imageio.ImageIO.write(img, "png", out);
        System.out.println("SHOT " + out.length());

        System.out.println(fail == 0 ? ("ALL PASS (" + pass + ")") : ("SOME FAIL " + fail + "/" + (pass + fail)));
        Runtime.getRuntime().halt(fail == 0 ? 0 : 1);
    }

    static java.util.List<Component> allChildren(Component root) {
        java.util.List<Component> out = new java.util.ArrayList<Component>();
        collect(root, out);
        return out;
    }

    static void collect(Component c, java.util.List<Component> out) {
        if (!(c instanceof Container)) return;
        for (Component k : ((Container) c).getComponents()) {
            out.add(k);
            collect(k, out);
        }
    }

    static void layoutAll(Component c) {
        c.doLayout();
        if (c instanceof Container) for (Component k : ((Container) c).getComponents()) layoutAll(k);
    }

    static void check(String name, boolean ok) {
        if (ok) {
            pass++;
            System.out.println("[PASS] " + name);
        } else {
            fail++;
            System.out.println("[FAIL] " + name);
        }
    }
}
