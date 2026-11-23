import javax.swing.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** 探针：todos 条数 vs monthDoneCounts 非零点 */
public class MonthProbe {
    public static void main(String[] a) throws Exception {
        SwingUtilities.invokeAndWait(() -> com.toolbox.pc.Main.main(new String[0]));
        Thread.sleep(2500);
        Class<?> mc = Class.forName("com.toolbox.pc.Main");
        Field selfF = mc.getDeclaredField("self");
        selfF.setAccessible(true);
        Object main = selfF.get(null);
        Method page = mc.getDeclaredMethod("page", Class.class);
        page.setAccessible(true);
        Object ov = page.invoke(main, Class.forName("com.toolbox.pc.OverviewPanel"));
        Method sel = mc.getDeclaredMethod("selectNav", String.class);
        sel.setAccessible(true);
        sel.invoke(main, "overview");
        Thread.sleep(1500);
        Field tf = ov.getClass().getDeclaredField("todos");
        tf.setAccessible(true);
        List<?> list = (List<?>) tf.get(ov);
        System.out.println("today=" + java.time.LocalDate.now() + " todos=" + list.size());
        int doneCount = 0;
        for (Object o : list) {
            Field d = o.getClass().getDeclaredField("done");
            d.setAccessible(true);
            if (Boolean.TRUE.equals(d.get(o))) doneCount++;
        }
        System.out.println("done=" + doneCount);
        Method mdc = ov.getClass().getDeclaredMethod("monthDoneCounts");
        mdc.setAccessible(true);
        int[] n = (int[]) mdc.invoke(ov);
        System.out.println("month len=" + n.length);
        for (int i = 0; i < n.length; i++) if (n[i] > 0) System.out.println("  day " + (i + 1) + " = " + n[i]);
        System.out.println("ovIdentity=" + System.identityHashCode(ov));
        Runtime.getRuntime().halt(0);
    }
}
