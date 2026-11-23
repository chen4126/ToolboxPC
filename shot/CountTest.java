import com.toolbox.pc.CountDownPanel;
import com.toolbox.pc.PomodoroPanel;
import com.toolbox.pc.Store;

import javax.swing.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CountTest {

    public static void main(String[] args) throws Exception {
        final List<String> pomoBackup = new ArrayList<String>(Store.readLines("pomodoro.txt"));

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                try {
                    // ---------- 倒计时 ----------
                    CountDownPanel c = new CountDownPanel();
                    Field pf = CountDownPanel.class.getDeclaredField("presets");
                    pf.setAccessible(true);
                    List<String[]> presets = castList(pf.get(c));
                    presets.clear();
                    presets.add(new String[]{"煮蛋", "300"});
                    presets.add(new String[]{"泡茶", "180"});
                    Method save = CountDownPanel.class.getDeclaredMethod("savePresets");
                    save.setAccessible(true);
                    save.invoke(c);
                    System.out.println("presets written: " + Store.readLines("countdown_presets.txt"));

                    // 重新加载，验证持久化
                    CountDownPanel c2 = new CountDownPanel();
                    List<String[]> reloaded = castList(pf.get(c2));
                    System.out.println("presets reloaded count=" + reloaded.size());
                    for (String[] p : reloaded) System.out.println("  " + p[0] + " -> " + p[1] + "s");

                    // 第一次启动占主计时器，第二次转并行
                    Method startPreset = CountDownPanel.class.getDeclaredMethod("startPreset", String.class, long.class);
                    startPreset.setAccessible(true);
                    startPreset.invoke(c2, "煮蛋", 300000L);
                    startPreset.invoke(c2, "泡茶", 180000L);
                    startPreset.invoke(c2, "会议", 600000L);

                    Field tf = CountDownPanel.class.getDeclaredField("tasks");
                    tf.setAccessible(true);
                    List<?> tasks = (List<?>) tf.get(c2);
                    Field rf = CountDownPanel.class.getDeclaredField("running");
                    rf.setAccessible(true);
                    Field mf = CountDownPanel.class.getDeclaredField("remainMs");
                    mf.setAccessible(true);
                    Field nm = CountDownPanel.class.getDeclaredField("mainName");
                    nm.setAccessible(true);
                    System.out.println("par tasks=" + tasks.size() + " mainRunning=" + rf.get(c2) + " mainName=" + nm.get(c2));

                    Method tick = CountDownPanel.class.getDeclaredMethod("tick");
                    tick.setAccessible(true);
                    for (int i = 0; i < 8; i++) tick.invoke(c2);
                    System.out.println("main remain after 2s=" + mf.get(c2));

                    // ---------- 番茄钟 ----------
                    Store.writeLines("pomodoro.txt", Arrays.asList("25", "5", "2026-01-01", "0", "0"));
                    PomodoroPanel p = new PomodoroPanel();
                    Field af = PomodoroPanel.class.getDeclaredField("autoNext");
                    af.setAccessible(true);
                    System.out.println("autoNext loaded(0)=" + af.get(p));

                    Store.writeLines("pomodoro.txt", Arrays.asList("25", "5", "2026-01-01", "0", "1"));
                    PomodoroPanel p2 = new PomodoroPanel();
                    System.out.println("autoNext loaded(1)=" + af.get(p2));

                    // 清理测试预设
                    presets.clear();
                    save.invoke(c2);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        // 还原用户真实番茄数据
        Store.writeLines("pomodoro.txt", pomoBackup);
        System.out.println("pomodoro restored: " + Store.readLines("pomodoro.txt"));
        System.out.println("DONE");
        System.exit(0);
    }

    @SuppressWarnings("unchecked")
    private static List<String[]> castList(Object o) {
        return (List<String[]>) o;
    }
}
