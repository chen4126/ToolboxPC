import javax.swing.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** 探针：Main 启动后 OverviewPanel 内存里到底有几条待办 */
public class TodosProbe {
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
        Field tf = ov.getClass().getDeclaredField("todos");
        tf.setAccessible(true);
        List<?> list = (List<?>) tf.get(ov);
        System.out.println("today=" + java.time.LocalDate.now() + " todos=" + list.size());
        for (Object o : list) {
            Field d = o.getClass().getDeclaredField("done");
            Field t = o.getClass().getDeclaredField("text");
            Field dt = o.getClass().getDeclaredField("date");
            d.setAccessible(true);
            t.setAccessible(true);
            dt.setAccessible(true);
            System.out.println("  done=" + d.get(o) + " text=" + t.get(o) + " date=" + dt.get(o));
        }
        Runtime.getRuntime().halt(0);
    }
}
