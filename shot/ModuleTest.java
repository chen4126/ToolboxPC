import com.toolbox.pc.Main;
import com.toolbox.pc.NavButton;
import com.toolbox.pc.Store;
import com.toolbox.pc.Theme;
import com.toolbox.pc.Ui;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class ModuleTest {

    static Object inst;
    static Class<?> cls;

    public static void main(String[] a) throws Exception {
        List<String> bak = new ArrayList<String>(Store.readLines("settings.txt"));
        Theme.init();
        Ui.installDefaults();

        SwingUtilities.invokeAndWait(() -> {
            try {
                cls = Class.forName("com.toolbox.pc.Main");
                inst = cls.getDeclaredConstructor().newInstance();
                Method create = cls.getDeclaredMethod("create");
                create.setAccessible(true);
                create.invoke(inst);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        Thread.sleep(1500);

        System.out.println("initial nav=" + navCount() + " hasCalc=" + hasKey("calc"));

        Main.setEnabled("calc", false);
        Thread.sleep(300);
        System.out.println("disable calc -> nav=" + navCount() + " hasCalc=" + hasKey("calc")
                + " isEnabled=" + Main.isEnabled("calc"));

        System.out.println("current before=" + field("currentKey"));
        Main.setEnabled("overview", false);
        Thread.sleep(300);
        System.out.println("disable overview(current) -> current=" + field("currentKey")
                + " hasOverview=" + hasKey("overview") + " nav=" + navCount());

        Main.setEnabled("calc", true);
        Main.setEnabled("overview", true);
        Thread.sleep(300);
        System.out.println("restored -> nav=" + navCount() + " hasCalc=" + hasKey("calc")
                + " hasOverview=" + hasKey("overview") + " disabled=" + Main.disabledKeys());

        Store.writeLines("settings.txt", bak);
        System.out.println("settings restored, lines=" + Store.readLines("settings.txt").size());
        System.exit(0);
    }

    static Object field(String name) throws Exception {
        Field f = cls.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(inst);
    }

    @SuppressWarnings("unchecked")
    static JPanel navPanel() throws Exception {
        return (JPanel) field("navPanel");
    }

    static int navCount() throws Exception {
        int n = 0;
        for (Component c : navPanel().getComponents()) {
            if (c instanceof NavButton) n++;
        }
        return n;
    }

    static boolean hasKey(String key) throws Exception {
        for (Component c : navPanel().getComponents()) {
            if (c instanceof NavButton && key.equals(((JComponent) c).getClientProperty("navKey"))) return true;
        }
        return false;
    }
}
