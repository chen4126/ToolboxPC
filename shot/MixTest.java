import com.toolbox.pc.*;
import javax.swing.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** 验证：备忘置顶排序/搜索、计数器多组、统计环形图数据 */
public class MixTest {
    public static void main(String[] args) throws Exception {
        System.out.println("=== 备忘 ===");
        MemoPanel mp = new MemoPanel();
        Field fq = MemoPanel.class.getDeclaredField("query");
        fq.setAccessible(true);
        Field fm = MemoPanel.class.getDeclaredField("model");
        fm.setAccessible(true);
        DefaultListModel<?> model = (DefaultListModel<?>) fm.get(mp);
        Method reload = MemoPanel.class.getDeclaredMethod("reloadList");
        reload.setAccessible(true);
        System.out.println("条数 = " + model.getSize());
        for (int i = 0; i < model.getSize(); i++) System.out.println("  " + describe(model.getElementAt(i)));

        fq.set(mp, "邮件");
        reload.invoke(mp);
        System.out.println("搜索「邮件」= " + model.getSize() + " 条");
        for (int i = 0; i < model.getSize(); i++) System.out.println("  " + describe(model.getElementAt(i)));

        fq.set(mp, "zzz不存在");
        reload.invoke(mp);
        System.out.println("搜索「zzz不存在」= " + model.getSize() + " 条");

        fq.set(mp, "");
        reload.invoke(mp);

        System.out.println("=== 计数器 ===");
        CounterPanel cp = new CounterPanel();
        Field fg = CounterPanel.class.getDeclaredField("groups");
        fg.setAccessible(true);
        List<?> groups = (List<?>) fg.get(cp);
        System.out.println("组数 = " + groups.size());
        for (Object g : groups) System.out.println("  " + fld(g, "name") + " 值=" + fld(g, "value") + " 步长=" + fld(g, "step"));
        Field fcur = CounterPanel.class.getDeclaredField("cur");
        fcur.setAccessible(true);
        Field fbox = CounterPanel.class.getDeclaredField("groupBox");
        fbox.setAccessible(true);
        JComboBox<?> box = (JComboBox<?>) fbox.get(cp);
        box.setSelectedIndex(2);
        Object cur = fcur.get(cp);
        System.out.println("切到第 3 组 → " + fld(cur, "name") + " 值=" + fld(cur, "value") + " 步长=" + fld(cur, "step"));

        System.out.println("=== 统计环形图 ===");
        StatsPanel sp = new StatsPanel();
        Field fd = StatsPanel.class.getDeclaredField("donut");
        fd.setAccessible(true);
        Object donut = fd.get(sp);
        Object total = fld(donut, "total");
        Object names = fld(donut, "names");
        System.out.println("本月支出合计 = " + total);
        System.out.println("分类数 = " + ((String[]) names).length);
        for (String n : (String[]) names) System.out.println("  " + n);
    }

    static Object fld(Object o, String name) throws Exception {
        Field f = o.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(o);
    }

    static String describe(Object m) throws Exception {
        return "[" + fld(m, "pinned") + "] " + fld(m, "time") + "  " + fld(m, "text");
    }
}
