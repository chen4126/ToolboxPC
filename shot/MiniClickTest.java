import com.toolbox.pc.Main;
import com.toolbox.pc.MiniBoard;
import com.toolbox.pc.SchedulePanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** 验证小窗条目双击能跳到日程页 */
public class MiniClickTest {
    public static void main(String[] args) throws Exception {
        Theme0.init();
        SwingUtilities.invokeAndWait(() -> Main.main(new String[0]));
        Thread.sleep(2500);

        Method show = MiniBoard.class.getDeclaredMethod("show");
        show.setAccessible(true);
        SwingUtilities.invokeAndWait(() -> {
            try {
                show.invoke(null);
            } catch (Exception ignored) {
            }
        });
        Thread.sleep(1500);

        Field instF = MiniBoard.class.getDeclaredField("inst");
        instF.setAccessible(true);
        Object mb = instF.get(null);
        Field lbF = MiniBoard.class.getDeclaredField("listBox");
        lbF.setAccessible(true);
        JPanel lb = (JPanel) lbF.get(mb);

        JLabel target = null;
        for (Component c : lb.getComponents()) {
            if (c instanceof JLabel && ((JLabel) c).getMouseListeners().length > 0) {
                target = (JLabel) c;
                break;
            }
        }
        if (target == null) {
            System.out.println("FAIL 小窗条目没有绑定点击监听");
            System.exit(1);
        }
        Field selfF = Main.class.getDeclaredField("self");
        selfF.setAccessible(true);
        Main self = (Main) selfF.get(null);
        Field frameF = Main.class.getDeclaredField("frame");
        frameF.setAccessible(true);
        JFrame frame = (JFrame) frameF.get(self);

        Main.go("overview");
        Thread.sleep(600);
        boolean before = false;
        for (Component c : frame.getContentPane().getComponents()) before |= findSchedule(c);
        System.out.println("on overview, scheduleVisible=" + before + " (expect false)");
        System.out.println("label=[" + target.getText() + "] listeners=" + target.getMouseListeners().length
                + " cursor=" + target.getCursor().getType());

        final JLabel tg = target;
        MouseEvent dbl = new MouseEvent(target, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
                0, 5, 5, 2, false, MouseEvent.BUTTON1);
        SwingUtilities.invokeAndWait(() -> {
            for (java.awt.event.MouseListener l : tg.getMouseListeners()) l.mouseClicked(dbl);
        });
        Thread.sleep(1200);

        boolean visible = false;
        for (Component c : frame.getContentPane().getComponents()) {
            visible |= findSchedule(c);
        }
        System.out.println("after double click scheduleVisible=" + visible
                + " frameVisible=" + frame.isVisible() + " title=[" + frame.getTitle() + "]");
        System.exit(visible ? 0 : 1);
    }

    private static boolean findSchedule(Component c) {
        if (c instanceof SchedulePanel) return c.isShowing();
        if (c instanceof Container) {
            for (Component k : ((Container) c).getComponents()) if (findSchedule(k)) return true;
        }
        return false;
    }

    /** 主题初始化入口，避免直接依赖 Theme 的可见性 */
    static class Theme0 {
        static void init() throws Exception {
            Method m = com.toolbox.pc.Theme.class.getDeclaredMethod("init");
            m.setAccessible(true);
            m.invoke(null);
        }
    }
}
