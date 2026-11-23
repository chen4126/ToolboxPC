import com.toolbox.pc.MiniBoard;
import com.toolbox.pc.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** 验证小窗拖到屏幕边缘会吸附，并记住坐标 */
public class MiniSnapTest {
    public static void main(String[] args) throws Exception {
        try {
            Method init = Theme.class.getDeclaredMethod("init");
            init.setAccessible(true);
            init.invoke(null);
            SwingUtilities.invokeAndWait(() -> com.toolbox.pc.Main.main(new String[0]));
            Thread.sleep(2500);

            Method show = MiniBoard.class.getDeclaredMethod("show");
            show.setAccessible(true);
            SwingUtilities.invokeAndWait(() -> {
                try {
                    show.invoke(null);
                } catch (Exception ignored) {
                }
            });
            Thread.sleep(1200);

            Field instF = MiniBoard.class.getDeclaredField("inst");
            instF.setAccessible(true);
            Object mb = instF.get(null);
            Field winF = MiniBoard.class.getDeclaredField("win");
            winF.setAccessible(true);
            Window win = (Window) winF.get(mb);

            Rectangle sb = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
            int w = win.getWidth(), h = win.getHeight();
            win.setLocation(sb.x + sb.width - w - 12, sb.y + sb.height - h - 10);
            System.out.println("before x=" + win.getX() + " y=" + win.getY() + " size=" + w + "x" + h);

            List<java.awt.event.MouseListener> ls = new ArrayList<>();
            Container root = (Container) win.getComponent(0);
            collect(root, ls);
            System.out.println("drag listeners=" + ls.size());
            for (java.awt.event.MouseListener l : ls) {
                if (!l.getClass().getName().startsWith("com.toolbox.pc.MiniBoard")) continue;
                l.mouseReleased(new MouseEvent(root, MouseEvent.MOUSE_RELEASED,
                        System.currentTimeMillis(), 0, 5, 5, 1, false, MouseEvent.BUTTON1));
            }
            Thread.sleep(300);

            int expectX = sb.x + sb.width - w;
            int expectY = sb.y + sb.height - h;
            Method rs = MiniBoard.class.getDeclaredMethod("readSetting", String.class);
            rs.setAccessible(true);
            String bx = (String) rs.invoke(null, "boardX");
            String by = (String) rs.invoke(null, "boardY");
            System.out.println("after  x=" + win.getX() + " y=" + win.getY()
                    + " expect=" + expectX + "," + expectY
                    + " saved=" + bx + "," + by);
            boolean ok = win.getX() == expectX && win.getY() == expectY
                    && String.valueOf(win.getX()).equals(bx) && String.valueOf(win.getY()).equals(by);
            System.out.println(ok ? "SNAP OK" : "SNAP FAIL");
            System.exit(ok ? 0 : 1);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(2);
        }
    }

    private static void collect(Component c, List<java.awt.event.MouseListener> out) {
        for (java.awt.event.MouseListener l : c.getMouseListeners()) out.add(l);
        if (c instanceof Container) {
            for (Component k : ((Container) c).getComponents()) collect(k, out);
        }
    }
}
