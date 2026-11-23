import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class DragTest {
    public static void main(String[] a) throws Exception {
        Theme.init();
        Ui.installDefaults();
        final Object[] h = new Object[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                Class<?> c = Class.forName("com.toolbox.pc.Main");
                Object m = c.getDeclaredConstructor().newInstance();
                Method create = c.getDeclaredMethod("create");
                create.setAccessible(true);
                create.invoke(m);
                h[0] = m;
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        Thread.sleep(1500);

        Class<?> c = Class.forName("com.toolbox.pc.Main");
        Method move = c.getDeclaredMethod("moveNav", String.class, int.class);
        move.setAccessible(true);

        SwingUtilities.invokeAndWait(() -> {
            try {
                move.invoke(h[0], "settings", 0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        Thread.sleep(300);
        System.out.println("navOrder after move: " + Main.navOrder());

        Field f = c.getDeclaredField("navPanel");
        f.setAccessible(true);
        JPanel nav = (JPanel) f.get(h[0]);
        StringBuilder sb = new StringBuilder();
        for (Component k : nav.getComponents()) {
            if (k instanceof JButton) sb.append(((JButton) k).getText()).append(" | ");
        }
        System.out.println("nav UI: " + sb);
        System.exit(0);
    }
}
