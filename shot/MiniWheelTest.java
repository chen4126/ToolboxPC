import com.toolbox.pc.MiniBoard;
import com.toolbox.pc.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** 验证小窗滚轮能增减显示条数：窗口高度随条数变化，且写入配置 */
public class MiniWheelTest {
    public static void main(String[] args) throws Exception {
        try {
            Theme.init();
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
            Thread.sleep(1000);

            Field instF = MiniBoard.class.getDeclaredField("inst");
            instF.setAccessible(true);
            Object mb = instF.get(null);
            Field winF = MiniBoard.class.getDeclaredField("win");
            winF.setAccessible(true);
            Window win = (Window) winF.get(mb);
            final JScrollPane sp = findScroll(win.getComponent(0));
            Method rs = MiniBoard.class.getDeclaredMethod("readSetting", String.class);
            rs.setAccessible(true);
            Method ws = MiniBoard.class.getDeclaredMethod("writeSetting", String.class, String.class);
            ws.setAccessible(true);
            Method rf = MiniBoard.class.getDeclaredMethod("refresh");
            rf.setAccessible(true);

            // 从 4 条开始
            ws.invoke(null, "boardRows", "4");
            SwingUtilities.invokeAndWait(() -> {
                try {
                    rf.invoke(mb);
                } catch (Exception ignored) {
                }
            });
            Thread.sleep(500);
            System.out.println("rows=4 h=" + win.getHeight());

            wheel(sp, -1);
            System.out.println("wheel up  rows=" + rs.invoke(null, "boardRows") + " h=" + win.getHeight());
            wheel(sp, -1);
            System.out.println("wheel up  rows=" + rs.invoke(null, "boardRows") + " h=" + win.getHeight());

            for (int i = 0; i < 3; i++) wheel(sp, 1);
            System.out.println("wheel down rows=" + rs.invoke(null, "boardRows") + " h=" + win.getHeight());

            boolean ok = "3".equals(rs.invoke(null, "boardRows"));
            System.out.println(ok ? "WHEEL OK" : "WHEEL FAIL");
            System.exit(ok ? 0 : 1);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(2);
        }
    }

    private static void wheel(final JScrollPane sp, final int rot) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            MouseWheelEvent e = new MouseWheelEvent(sp, MouseEvent.MOUSE_WHEEL, System.currentTimeMillis(),
                    0, 50, 50, 0, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, rot);
            for (MouseWheelListener l : sp.getMouseWheelListeners()) l.mouseWheelMoved(e);
        });
        Thread.sleep(400);
    }

    private static JScrollPane findScroll(Component c) {
        if (c instanceof JScrollPane) return (JScrollPane) c;
        if (c instanceof Container) {
            for (Component k : ((Container) c).getComponents()) {
                JScrollPane r = findScroll(k);
                if (r != null) return r;
            }
        }
        return null;
    }
}
