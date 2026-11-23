import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

/** v2.4 界面出图：备份勾选 / 导入记忆 / 统计页数据备份卡 */
public class ShotV24 {

    public static void main(String[] a) throws Exception {
        File tmp = new File(System.getProperty("java.io.tmpdir"), "toolbox-shot-v24");
        wipe(tmp);
        tmp.mkdirs();
        System.setProperty("toolbox.data.dir", tmp.getAbsolutePath());

        Store.writeLines("settings.txt", new ArrayList<String>(Arrays.asList(
                "sound=1", "strong=0", "pin=0",
                "navOrder=overview,schedule,calc,ledger,memo,counter,pomodoro,countdown,habit,toolbox,stats,settings")));
        Store.writeLines("todo.txt", new ArrayList<String>(Arrays.asList(
                "0\t买菜\t2026-11-23", "1\t写周报\t2026-11-22")));
        Store.writeLines("ledger.txt", new ArrayList<String>(Arrays.asList(
                "0\t25.00\t午饭\t11-22 12:30\t餐饮", "0\t300.00\t房租\t11-01 09:00\t居住")));
        Store.writeLines("memo.txt", new ArrayList<String>(Arrays.asList("周五交周报")));
        Store.writeLines("habit.txt", new ArrayList<String>(Arrays.asList("早起 0 1")));
        LedgerPanel.remember("超市 买日用品 87.6", "购物");
        LedgerPanel.remember("美团外卖 32.5", "餐饮");
        LedgerPanel.remember("健身房月卡", "娱乐");
        Backup.manual();

        SwingUtilities.invokeAndWait(() -> Main.main(new String[0]));
        Thread.sleep(2500);

        // ---------- 1) 备份管理弹窗（带勾选行） ----------
        SettingsPanel sp = Main.page(SettingsPanel.class);
        if (sp == null) {
            SwingUtilities.invokeAndWait(() -> Main.go("settings"));
            Thread.sleep(800);
            sp = Main.page(SettingsPanel.class);
        }
        Method mk = SettingsPanel.class.getDeclaredMethod("buildBackupDialog");
        mk.setAccessible(true);
        JDialog bd = (JDialog) mk.invoke(sp);
        shot(bd, desk("ui-310-backup.png"), 0, 0);

        // ---------- 2) 设置页「导入记忆」区 ----------
        JButton mapBtn = findButton(sp, "改成这个分类");
        JScrollPane big = findScroll(sp);
        System.out.println("记忆按钮=" + (mapBtn != null) + " 滚动=" + (big != null));
        if (mapBtn != null && big != null) {
            final JButton mb = mapBtn;
            SwingUtilities.invokeAndWait(() -> mb.scrollRectToVisible(new Rectangle(0, 0, mb.getWidth(), mb.getHeight() + 80)));
            Thread.sleep(700);
            shotViewport(big, desk("ui-311-memory.png"));
        }

        // ---------- 3) 统计页顶部（含数据备份卡） ----------
        SwingUtilities.invokeAndWait(() -> Main.go("stats"));
        Thread.sleep(900);
        StatsPanel st = Main.page(StatsPanel.class);
        System.out.println("统计页=" + (st != null));
        if (st != null) {
            shotPanel(st, desk("ui-312-backup-card.png"));
        }

        Thread.sleep(200);
        Runtime.getRuntime().halt(0);
    }

    static String desk(String n) {
        return new File(System.getProperty("user.home"), "Desktop/" + n).getAbsolutePath();
    }

    static void shot(JDialog dlg, String path, int w0, int h0) throws Exception {
        if (w0 > 0) dlg.setSize(w0, h0);
        else dlg.pack();
        SwingUtilities.invokeLater(() -> dlg.setVisible(true));
        Thread.sleep(1400);
        JComponent root = (JComponent) dlg.getContentPane();
        int w = root.getWidth() > 0 ? root.getWidth() : (w0 > 0 ? w0 : root.getPreferredSize().width);
        int h = root.getHeight() > 0 ? root.getHeight() : (h0 > 0 ? h0 : root.getPreferredSize().height);
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(0xF7F8FC));
        g.fillRect(0, 0, w, h);
        root.printAll(g);
        g.dispose();
        javax.imageio.ImageIO.write(img, "png", new File(path));
        System.out.println("SHOT " + path + " " + new File(path).length() + " " + w + "x" + h);
        SwingUtilities.invokeAndWait(dlg::dispose);
    }

    static void shotPanel(JComponent p, String path) throws Exception {
        int w = p.getWidth(), h = p.getHeight();
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Theme.bg());
        g.fillRect(0, 0, w, h);
        p.printAll(g);
        g.dispose();
        javax.imageio.ImageIO.write(img, "png", new File(path));
        System.out.println("SHOT " + path + " " + new File(path).length() + " " + w + "x" + h);
    }

    static void shotViewport(JScrollPane sp, String path) throws Exception {
        JViewport vp = sp.getViewport();
        Component view = vp.getView();
        int w = vp.getWidth(), h = vp.getHeight();
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Theme.bg());
        g.fillRect(0, 0, w, h);
        Point p = vp.getViewPosition();
        g.translate(-p.x, -p.y);
        view.printAll(g);
        g.dispose();
        javax.imageio.ImageIO.write(img, "png", new File(path));
        System.out.println("SHOT " + path + " " + new File(path).length() + " " + w + "x" + h);
    }

    static JButton findButton(Container c, String text) {
        for (Component x : c.getComponents()) {
            if (x instanceof JButton && text.equals(((JButton) x).getText())) return (JButton) x;
            if (x instanceof Container) {
                JButton b = findButton((Container) x, text);
                if (b != null) return b;
            }
        }
        return null;
    }

    static JScrollPane findScroll(Container c) {
        JScrollPane best = null;
        int bestH = -1;
        for (Component x : c.getComponents()) {
            if (x instanceof JScrollPane) {
                JScrollPane s = (JScrollPane) x;
                Component v = s.getViewport().getView();
                int h = v == null ? 0 : v.getPreferredSize().height;
                if (h > bestH) {
                    best = s;
                    bestH = h;
                }
            }
            if (x instanceof Container) {
                JScrollPane s = findScroll((Container) x);
                if (s != null) {
                    Component v = s.getViewport().getView();
                    int h = v == null ? 0 : v.getPreferredSize().height;
                    if (h > bestH) {
                        best = s;
                        bestH = h;
                    }
                }
            }
        }
        return best;
    }

    static void wipe(File d) {
        if (!d.exists()) return;
        File[] fs = d.listFiles();
        if (fs != null) for (File f : fs) {
            if (f.isDirectory()) wipe(f);
            f.delete();
        }
    }
}
