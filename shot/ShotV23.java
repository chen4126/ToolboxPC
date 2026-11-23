import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** v2.3 界面出图：设置页备份区 / 备份管理弹窗 / 导入预览的分类列 */
public class ShotV23 {

    public static void main(String[] a) throws Exception {
        File tmp = new File(System.getProperty("java.io.tmpdir"), "toolbox-shot-v23");
        wipe(tmp);
        tmp.mkdirs();
        System.setProperty("toolbox.data.dir", tmp.getAbsolutePath());

        Store.writeLines("settings.txt", new ArrayList<String>(Arrays.asList(
                "sound=1", "strong=0", "pin=0",
                "navOrder=overview,schedule,calc,ledger,memo,counter,pomodoro,countdown,habit,toolbox,stats,settings")));
        Store.writeLines("todo.txt", new ArrayList<String>(Arrays.asList(
                "0\t买菜\t2026-11-19", "1\t写周报\t2026-11-18")));
        Store.writeLines("ledger.txt", new ArrayList<String>(Arrays.asList(
                "0\t25.00\t午饭\t11-18 12:30\t餐饮", "0\t300.00\t房租\t11-01 09:00\t居住")));
        LedgerPanel.remember("超市 买日用品 87.6", "购物");

        SwingUtilities.invokeAndWait(() -> Main.main(new String[0]));
        Thread.sleep(2500);

        // ---------- 1) 备份管理弹窗 ----------
        Backup.manual();
        Thread.sleep(1100);
        Backup.manual();
        SettingsPanel sp = Main.page(SettingsPanel.class);
        Method mk = SettingsPanel.class.getDeclaredMethod("buildBackupDialog");
        mk.setAccessible(true);
        JDialog bd = (JDialog) mk.invoke(sp);
        shot(bd, new File(System.getProperty("user.home"), "Desktop/ui-300-backup.png").getAbsolutePath(), 0, 0);

        // ---------- 2) 设置页「数据」区 ----------
        SettingsPanel sp2 = Main.page(SettingsPanel.class);
        JButton manage = findButton(sp2, "备份管理");
        System.out.println("找到备份管理按钮: " + (manage != null));
        JScrollPane big = findScroll(sp2);
        System.out.println("找到设置页滚动: " + (big != null));
        if (manage != null && big != null) {
            final JButton m2 = manage;
            SwingUtilities.invokeAndWait(() -> m2.scrollRectToVisible(new Rectangle(0, 0, m2.getWidth(), m2.getHeight() + 90)));
            Thread.sleep(700);
            shotViewport(big, new File(System.getProperty("user.home"), "Desktop/ui-302-settings-data.png").getAbsolutePath());
        }

        // ---------- 3) 记账导入预览（含「记忆」标记） ----------
        LedgerPanel lp = Main.page(LedgerPanel.class);
        Method cid = LedgerPanel.class.getDeclaredMethod("createImportDialog");
        cid.setAccessible(true);
        JDialog id = (JDialog) cid.invoke(lp);
        JButton parseBtn = findButton(id.getContentPane(), "解析");
        if (parseBtn != null) SwingUtilities.invokeAndWait(parseBtn::doClick);
        Thread.sleep(600);
        JTable pt = findTable(id.getContentPane());
        if (pt != null) {
            javax.swing.table.JTableHeader th = pt.getTableHeader();
            JScrollPane psp = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, pt);
            System.out.println("header " + th.getWidth() + "x" + th.getHeight()
                    + " vis=" + th.isVisible() + " table=" + (th.getTable() == pt)
                    + " fg=" + th.getForeground() + " font=" + th.getFont()
                    + " col0=" + pt.getColumnName(0)
                    + " sp=" + (psp == null ? "null" : psp.getWidth() + "x" + psp.getHeight()
                    + " ch=" + (psp.getColumnHeader() == null ? "null" : psp.getColumnHeader().getHeight() + "h")
                    + " ext=" + psp.getViewport().getExtentSize()));
        } else {
            System.out.println("没找到预览表");
        }
        shot(id, new File(System.getProperty("user.home"), "Desktop/ui-301-import.png").getAbsolutePath(), 824, 574);

        Thread.sleep(200);
        Runtime.getRuntime().halt(0);
    }

    static void shot(JDialog dlg, String path, int w0, int h0) throws Exception {
        /* 真正显示一次再截：不 show 的窗口里 JScrollPane 的表头/滚动条不会拿到尺寸，截出来是假象 */
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

    static void layoutAll(Component c) {
        if (!(c instanceof Container)) return;
        Container ct = (Container) c;
        ct.doLayout();
        for (Component x : ct.getComponents()) layoutAll(x);
    }

    static JTable findTable(Container c) {
        for (Component x : c.getComponents()) {
            if (x instanceof JTable) return (JTable) x;
            if (x instanceof Container) {
                JTable t = findTable((Container) x);
                if (t != null) return t;
            }
        }
        return null;
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
        for (Component x : c.getComponents()) {
            if (x instanceof JScrollPane) {
                JScrollPane s = (JScrollPane) x;
                if (best == null || s.getViewport().getView() instanceof JPanel) best = s;
            }
            if (x instanceof Container) {
                JScrollPane s = findScroll((Container) x);
                if (s != null && s.getViewport().getView() instanceof JPanel
                        && ((JPanel) s.getViewport().getView()).getComponentCount() > 5) best = s;
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
