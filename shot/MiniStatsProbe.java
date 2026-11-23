import com.toolbox.pc.StatsPanel;
import com.toolbox.pc.Theme;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Array;
import java.lang.reflect.Field;

/** 打印真实主窗统计页环形图数据，排查“另有 N 类” */
public class MiniStatsProbe {
    public static void main(String[] args) throws Exception {
        Theme.init();
        SwingUtilities.invokeAndWait(() -> com.toolbox.pc.Main.main(new String[0]));
        Thread.sleep(2200);
        StatsPanel sp = null;
        for (Frame f : Frame.getFrames()) {
            sp = deep(f, StatsPanel.class);
            if (sp != null) break;
        }
        if (sp == null) {
            System.out.println("NO STATS");
            System.exit(3);
        }
        Field df = StatsPanel.class.getDeclaredField("donut");
        df.setAccessible(true);
        Object donut = df.get(sp);
        for (Field f : donut.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            Object v = f.get(donut);
            if (v != null && v.getClass().isArray()) {
                StringBuilder sb = new StringBuilder(f.getName() + " = ");
                int n = Array.getLength(v);
                for (int i = 0; i < n; i++) sb.append(Array.get(v, i)).append(" | ");
                System.out.println(sb);
            } else {
                System.out.println(f.getName() + " = " + v);
            }
        }
        System.exit(0);
    }

    private static <T> T deep(Component c, Class<T> cls) {
        if (cls.isInstance(c)) return cls.cast(c);
        if (c instanceof Container) {
            for (Component k : ((Container) c).getComponents()) {
                T r = deep(k, cls);
                if (r != null) return r;
            }
        }
        return null;
    }
}
