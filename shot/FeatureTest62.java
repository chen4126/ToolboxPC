import com.toolbox.pc.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** v1.77：设置页导航排序支持"按住项目直接拖动"，松手即换位、立即生效并落盘 */
public class FeatureTest62 {

    static int pass = 0, fail = 0;
    static Object main;
    static JPanel box;
    static Object panel;

    public static void main(String[] a) throws Exception {
        try {
            run();
        } catch (Throwable t) {
            t.printStackTrace();
            fail++;
        }
        System.out.println(fail == 0 ? ("ALL PASS (" + pass + ")") : ("SOME FAIL " + fail + "/" + (pass + fail)));
        Runtime.getRuntime().halt(fail == 0 ? 0 : 1);
    }

    static void check(String name, boolean ok) {
        check(name, ok, "");
    }

    static void check(String name, boolean ok, String detail) {
        System.out.println((ok ? "[PASS] " : "[FAIL] ") + name + (ok || detail.isEmpty() ? "" : " → " + detail));
        if (ok) pass++;
        else fail++;
    }

    static void run() throws Exception {
        SwingUtilities.invokeAndWait(() -> Main.main(new String[0]));
        Thread.sleep(2500);
        Class<?> mc = Class.forName("com.toolbox.pc.Main");
        Field selfF = mc.getDeclaredField("self");
        selfF.setAccessible(true);
        main = selfF.get(null);
        Method pageM = mc.getDeclaredMethod("page", Class.class);
        pageM.setAccessible(true);
        panel = pageM.invoke(main, Class.forName("com.toolbox.pc.SettingsPanel"));
        Method sel = mc.getDeclaredMethod("selectNav", String.class);
        sel.setAccessible(true);
        SwingUtilities.invokeAndWait(() -> {
            try {
                sel.invoke(main, "settings");
            } catch (Exception ignored) {
            }
        });
        Thread.sleep(900);

        Field boxF = panel.getClass().getDeclaredField("navOrderBox");
        boxF.setAccessible(true);
        box = (JPanel) boxF.get(panel);
        check("导航排序列表面板存在", box != null);

        box.setSize(660, 420);
        layoutAll(box);
        Thread.sleep(200);

        List<JPanel> rows = navRows();
        List<String> order = Main.navOrder();
        int sortable = order.size() - ("settings".equals(order.get(order.size() - 1)) ? 1 : 0);
        check("可拖动行数 = 可排序模块数（" + sortable + "）[" + rows.size() + "]", rows.size() == sortable);
        check("每行都挂上了拖动监听",
                !rows.isEmpty() && rows.get(0).getMouseListeners().length > 0
                        && rows.get(rows.size() - 1).getMouseListeners().length > 0,
                rows.isEmpty() ? "rows=0" : String.valueOf(rows.get(0).getMouseListeners().length));
        check("每行都有拖动用的鼠标动作监听",
                !rows.isEmpty() && rows.get(0).getMouseMotionListeners().length > 0,
                rows.isEmpty() ? "rows=0" : String.valueOf(rows.get(0).getMouseMotionListeners().length));
        check("可拖行光标是移动手掌", !rows.isEmpty()
                && rows.get(0).getCursor().getType() == Cursor.MOVE_CURSOR,
                rows.isEmpty() ? "rows=0" : String.valueOf(rows.get(0).getCursor().getType()));
        check("可拖行带提示文字", !rows.isEmpty() && rows.get(0).getToolTipText() != null
                && rows.get(0).getToolTipText().contains("拖动"), rows.isEmpty() ? "" : String.valueOf(rows.get(0).getToolTipText()));

        /* 设置那行是固定行：不给拖 */
        JPanel pinRow = null;
        for (Component c : box.getComponents()) {
            if (!(c instanceof JPanel)) continue;
            Object v = ((JPanel) c).getClientProperty("navIdx");
            if (v == null && c instanceof JPanel && ((JPanel) c).getComponentCount() > 0) pinRow = (JPanel) c;
        }
        check("设置固定行没挂拖动监听", pinRow != null && pinRow.getMouseListeners().length == 0,
                pinRow == null ? "pinRow=null" : String.valueOf(pinRow.getMouseListeners().length));

        /* ---------- 按住第一行往下拖到第 4 行 ---------- */
        String moved = order.get(0);
        int fromY = rows.get(3).getY() + rows.get(3).getHeight() / 2;
        press(rows.get(0), 5, 10);
        check("按下后被按住那行铺上悬停底色", rows.get(0).isOpaque()
                && rows.get(0).getBackground().equals(Ui.navHoverBg()),
                String.valueOf(rows.get(0).getBackground()));
        drag(rows.get(0), 5, fromY);
        Thread.sleep(120);
        JPanel target = rows.get(3);
        check("拖到第 4 行时它高亮成目标", target.isOpaque()
                && target.getBackground().equals(Ui.navHoverBg()),
                String.valueOf(target.getBackground()));
        check("中途没松手顺序还没变", order.get(0).equals(moved), Main.navOrder().toString());
        release(rows.get(0), 5, fromY);
        Thread.sleep(400);

        List<String> after = Main.navOrder();
        check("松手后第 4 位变成刚拖的那项 [" + after + "]", after.get(3).equals(moved), after.toString());
        check("其余项顺次前移", after.get(0).equals(order.get(1)), after.get(0));
        check("设置仍钉在最末", "settings".equals(after.get(after.size() - 1)), after.get(after.size() - 1));
        check("顺序写进了 settings.txt", savedOrder().equals(String.join(",", after)), savedOrder());

        List<JPanel> rows2 = navRows();
        check("面板已重建（行数不变）[" + rows2.size() + "]", rows2.size() == rows.size());
        check("重建后第一行换成原来第二项", labelOf(rows2.get(0)).contains(Main.navLabel(order.get(1))),
                labelOf(rows2.get(0)));
        check("松手后高亮全部撤掉", !rows2.get(0).isOpaque() && !rows2.get(3).isOpaque());

        /* ---------- 往上拖：把最后一项拖到第 1 位 ---------- */
        List<JPanel> rows3 = navRows();
        String lastKey = Main.navOrder().get(rows3.size() - 1);
        int toY = rows3.get(0).getY() + 6;
        press(rows3.get(rows3.size() - 1), 5, 10);
        drag(rows3.get(rows3.size() - 1), 5, toY);
        release(rows3.get(rows3.size() - 1), 5, toY);
        Thread.sleep(400);
        check("往上拖也能换位，最后一项到第 1 位", Main.navOrder().get(0).equals(lastKey), Main.navOrder().toString());

        /* ---------- 越界：拖到设置那行，必须夹在可排序区间里 ---------- */
        List<JPanel> rows4 = navRows();
        int pinY = -1;
        for (Component c : box.getComponents()) {
            if (!(c instanceof JPanel)) continue;
            if (((JPanel) c).getClientProperty("navIdx") == null && ((JPanel) c).getComponentCount() > 0) {
                pinY = c.getY() + c.getHeight() / 2;
            }
        }
        if (pinY >= 0) {
            String firstKey = Main.navOrder().get(0);
            press(rows4.get(0), 5, 10);
            drag(rows4.get(0), 5, pinY);
            release(rows4.get(0), 5, pinY);
            Thread.sleep(400);
            List<String> nowOrder = Main.navOrder();
            int sortableLast = nowOrder.size() - 1;
            if ("settings".equals(nowOrder.get(sortableLast))) sortableLast--;
            check("拖到设置行会被夹到最末可排序位", nowOrder.get(sortableLast).equals(firstKey), nowOrder.toString());
            check("设置还是最后一项", "settings".equals(nowOrder.get(nowOrder.size() - 1)), nowOrder.toString());
        }

        /* ---------- targetIndexAt 落点换算 ---------- */
        Method tia = panel.getClass().getDeclaredMethod("targetIndexAt", int.class);
        tia.setAccessible(true);
        List<JPanel> rows5 = navRows();
        int yMidRow2 = rows5.get(2).getY() + rows5.get(2).getHeight() / 2;
        check("落点在第 3 行正中 → 目标 2", (Integer) tia.invoke(panel, yMidRow2) == 2,
                String.valueOf(tia.invoke(panel, yMidRow2)));
        int yGap = rows5.get(2).getY() + rows5.get(2).getHeight() + 1;   // 行间 2px 缝隙
        int near = (Integer) tia.invoke(panel, yGap);
        check("落在行间缝隙取最近一行（2 或 3）[" + near + "]", near == 2 || near == 3);
        check("落点在最上方 → 第 1 行", (Integer) tia.invoke(panel, -50) == 0, "");
        int last = rows5.size() - 1;
        check("落点在最下方 → 最后一行", (Integer) tia.invoke(panel, rows5.get(last).getY() + 400) == last, "");

        /* ---------- 出图：按住拖动中的高亮态 ---------- */
        List<JPanel> rows6 = navRows();
        press(rows6.get(0), 5, 10);
        drag(rows6.get(0), 5, rows6.get(2).getY() + rows6.get(2).getHeight() / 2);
        Thread.sleep(150);
        shot(box, "C:\\Users\\chen\\Desktop\\ui-258-drag-order.png", 660, Math.max(260, box.getComponentCount() * 36 + 10));
        release(rows6.get(0), 5, rows6.get(2).getY() + rows6.get(2).getHeight() / 2);
        Thread.sleep(300);
    }

    static void press(JPanel row, int x, int y) {
        row.dispatchEvent(new MouseEvent(row, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
                MouseEvent.BUTTON1_DOWN_MASK, x, y, 1, false, MouseEvent.BUTTON1));
    }

    static void drag(JPanel row, int xInBox, int yInBox) {
        Point p = SwingUtilities.convertPoint(box, new Point(xInBox, yInBox), row);
        row.dispatchEvent(new MouseEvent(row, MouseEvent.MOUSE_DRAGGED, System.currentTimeMillis(),
                MouseEvent.BUTTON1_DOWN_MASK, p.x, p.y, 1, false, MouseEvent.BUTTON1));
    }

    static void release(JPanel row, int xInBox, int yInBox) {
        Point p = SwingUtilities.convertPoint(box, new Point(xInBox, yInBox), row);
        row.dispatchEvent(new MouseEvent(row, MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(),
                0, p.x, p.y, 1, false, MouseEvent.BUTTON1));
    }

    static List<JPanel> navRows() {
        List<JPanel> out = new ArrayList<JPanel>();
        for (Component c : box.getComponents()) {
            if (!(c instanceof JPanel)) continue;
            if (((JPanel) c).getClientProperty("navIdx") != null) out.add((JPanel) c);
        }
        return out;
    }

    static String labelOf(JPanel row) {
        for (Component c : row.getComponents()) {
            if (c instanceof JLabel) return ((JLabel) c).getText();
        }
        return "";
    }

    static String savedOrder() {
        for (String l : Store.readLines("settings.txt")) {
            if (l.startsWith("navOrder=")) return l.substring("navOrder=".length());
        }
        return "";
    }

    static void layoutAll(Component c) {
        c.doLayout();
        if (c instanceof Container) {
            for (Component x : ((Container) c).getComponents()) layoutAll(x);
        }
    }

    static void shot(JPanel p, String path, int w, int h) throws Exception {
        Dimension pref = p.getPreferredSize();
        int hh = Math.max(h, pref.height + 4);
        p.setSize(w, hh);
        layoutAll(p);
        p.setDoubleBuffered(false);
        BufferedImage img = new BufferedImage(w, hh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Theme.bg());
        g.fillRect(0, 0, w, hh);
        p.paint(g);
        g.dispose();
        ImageIO.write(img, "png", new File(path));
        System.out.println("SHOT " + new File(path).length() + " h=" + hh);
    }
}
