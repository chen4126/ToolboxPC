import com.toolbox.pc.*;
import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;

public class ProbeCombo {
    public static void main(String[] a) throws Exception {
        Main.main(new String[0]);
        Thread.sleep(3000);
        Field f = Main.class.getDeclaredField("frame");
        f.setAccessible(true);
        JFrame fr = (JFrame) f.get(null);
        SettingsPanel sp = null;
        for (Window w : Window.getWindows()) {
            if (w instanceof JFrame) {
                JFrame jf = (JFrame) w;
                for (Component c : jf.getContentPane().getComponents()) {
                    SettingsPanel s = findSp(c);
                    if (s != null) sp = s;
                }
            }
        }
        if (sp == null) { System.out.println("未找到设置页"); Runtime.getRuntime().halt(1); }
        dump(sp, 0);
        Runtime.getRuntime().halt(0);
    }

    static SettingsPanel findSp(Component c) {
        if (c instanceof SettingsPanel) return (SettingsPanel) c;
        if (c instanceof Container) {
            for (Component k : ((Container) c).getComponents()) {
                SettingsPanel s = findSp(k);
                if (s != null) return s;
            }
        }
        return null;
    }

    static void dump(Component c, int depth) {
        if (c instanceof JComboBox) {
            JComboBox<?> cb = (JComboBox<?>) c;
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < cb.getItemCount(); i++) b.append("[").append(cb.getItemAt(i)).append("]");
            System.out.println("COMBO n=" + cb.getItemCount() + " sel=" + cb.getSelectedItem() + " items=" + b);
        }
        if (c instanceof Container) {
            for (Component k : ((Container) c).getComponents()) dump(k, depth + 1);
        }
    }
}
