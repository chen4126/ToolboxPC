import com.toolbox.pc.*;
import javax.swing.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/** 验证：番茄记录带任务名、统计区间切换、小窗的今日日程判定 */
public class MixTest2 {
    public static void main(String[] args) throws Exception {
        System.out.println("=== 番茄任务名 ===");
        PomodoroPanel p = new PomodoroPanel();
        Field tf = PomodoroPanel.class.getDeclaredField("taskField");
        tf.setAccessible(true);
        JTextField field = (JTextField) tf.get(p);
        field.setText("写实习周报");
        Method log = PomodoroPanel.class.getDeclaredMethod("logRecord", int.class, String.class);
        log.setAccessible(true);
        log.invoke(p, 25, "专注");
        log.invoke(p, 5, "休息");
        List<String> lines = Store.readLines("pomodoro_log.txt");
        for (String l : lines) System.out.println("  " + l.replace("\t", " | "));

        System.out.println("=== 统计区间 ===");
        StatsPanel sp = new StatsPanel();
        Method refresh = StatsPanel.class.getDeclaredMethod("refresh");
        refresh.setAccessible(true);
        Field chartF = StatsPanel.class.getDeclaredField("chart");
        chartF.setAccessible(true);
        Object chart = chartF.get(sp);
        Field vals = chart.getClass().getDeclaredField("values");
        vals.setAccessible(true);
        System.out.println("默认区间柱数 = " + ((int[]) vals.get(chart)).length);
        Field r30 = StatsPanel.class.getDeclaredField("range30");
        r30.setAccessible(true);
        JToggleButton b30 = (JToggleButton) r30.get(sp);
        b30.setSelected(true);
        Field rf = StatsPanel.class.getDeclaredField("range");
        rf.setAccessible(true);
        rf.set(sp, 30);
        refresh.invoke(sp);
        System.out.println("切 30 天后柱数 = " + ((int[]) vals.get(chart)).length);
        Field donutF = StatsPanel.class.getDeclaredField("donut");
        donutF.setAccessible(true);
        Object donut = donutF.get(sp);
        Field dt = donut.getClass().getDeclaredField("title");
        dt.setAccessible(true);
        System.out.println("环形图标题 = " + dt.get(donut));

        System.out.println("=== 小窗今日日程判定 ===");
        Method hm = MiniBoard.class.getDeclaredMethod("todayHM", long.class, String.class, Calendar.class);
        hm.setAccessible(true);
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 9);
        today.set(Calendar.MINUTE, 30);
        long ms = today.getTimeInMillis();
        System.out.println("daily   → " + hm.invoke(null, ms, "daily", Calendar.getInstance()));
        System.out.println("workday → " + hm.invoke(null, ms, "workday", Calendar.getInstance()));
        System.out.println("none    → " + hm.invoke(null, ms, "none", Calendar.getInstance()));
        System.out.println("weekly:3→ " + hm.invoke(null, ms, "weekly:3", Calendar.getInstance()));
        System.out.println("monthly:" + Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
                + " → " + hm.invoke(null, ms, "monthly:" + Calendar.getInstance().get(Calendar.DAY_OF_MONTH),
                Calendar.getInstance()));
        System.exit(0);
    }
}
