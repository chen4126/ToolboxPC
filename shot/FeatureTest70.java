import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * v2.0：番茄记录弹窗单例 + 汇总视图 + 再专注一轮、预设下拉复位刷新、
 * 概览 todoStat 挂载、归档按日期搜索高亮、窄窗待办卡保底高度、今日专注卡联动。
 */
public class FeatureTest70 {

    static int pass = 0, fail = 0;

    public static void main(String[] a) throws Exception {
        try {
            run();
        } catch (Throwable t) {
            t.printStackTrace();
            fail++;
        }
        System.out.println(fail == 0 ? ("ALL PASS (" + pass + ")") : ("SOME FAIL " + fail + "/" + (pass + fail)));
        Runtime.getRuntime().halt(fail == 0 ? 0 : 1);
    }

    static void check(String name, boolean ok) {
        check(name, ok, "");
    }

    static void check(String name, boolean ok, String detail) {
        System.out.println((ok ? "[PASS] " : "[FAIL] ") + name + (ok || detail.isEmpty() ? "" : " → " + detail));
        if (ok) pass++;
        else fail++;
    }

    static Object field(Class<?> c, Object o, String name) throws Exception {
        Field f = c.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(o);
    }

    static void setField(Class<?> c, Object o, String name, Object v) throws Exception {
        Field f = c.getDeclaredField(name);
        f.setAccessible(true);
        f.set(o, v);
    }

    static String day(int back) {
        return LocalDate.now().minusDays(back).toString();
    }

    static void run() throws Exception {
        summary();
        dialogSingleton();
        presetReset();
        overviewFixes();
    }

    /* ================= 1) 本周按任务汇总 ================= */

    static void summary() throws Exception {
        System.out.println("---- 任务汇总 ----");
        String today = day(0);
        String ws = PomodoroPanel.weekStart();
        LocalDate monday = LocalDate.parse(ws);
        String mid = monday.plusDays(1).toString();
        if (mid.compareTo(today) > 0) mid = ws;

        List<String> log = new ArrayList<String>();
        log.add("1\t" + today + " 09:00\t25\t专注\t写周报");
        log.add("2\t" + today + " 10:30\t50\t专注\t写周报");
        log.add("3\t" + mid + " 15:00\t25\t专注\t评审");
        log.add("4\t" + today + " 11:00\t25\t专注\t");          // 没写任务名
        log.add("5\t" + today + " 11:30\t5\t休息\t写周报");     // 休息不计
        log.add("6\t" + monday.minusDays(1) + " 20:00\t25\t专注\t写周报"); // 上周不计
        Store.writeLines("pomodoro_log.txt", log);

        PomodoroPanel pp = new PomodoroPanel();
        List<String[]> sum = pp.weekSummary();

        check("汇总 3 组", sum.size() == 3, "n=" + sum.size());
        check("写周报排最前", "写周报".equals(sum.get(0)[0]), sum.get(0)[0]);
        check("写周报 2 次", "2".equals(sum.get(0)[1]), sum.get(0)[1]);
        check("写周报 75 分钟", "75".equals(sum.get(0)[2]), sum.get(0)[2]);
        boolean hasEmpty = false, hasReview = false;
        for (String[] s : sum) {
            if ("（没写任务名）".equals(s[0])) hasEmpty = true;
            if ("评审".equals(s[0])) hasReview = true;
        }
        check("空任务名归一组", hasEmpty);
        check("评审在汇总里", hasReview);
        check("上周的不进汇总", sum.size() == 3, "n=" + sum.size());
        check("休息不进汇总", sum.get(0)[1].equals("2"));

        check("汇总显示格式", pp.summaryText(sum.get(0)).equals("写周报   2 次 · 75 分钟"),
                pp.summaryText(sum.get(0)));

        String line = pp.summaryMemoLine();
        check("汇总备忘非空", line != null);
        String[] cols = line == null ? new String[0] : line.split("\t", -1);
        check("汇总备忘四列", cols.length == 4, "cols=" + cols.length);
        check("不置顶", "0".equals(cols[0]));
        check("正文有「本周专注汇总」", cols[2].startsWith("本周专注汇总："), cols[2]);
        check("正文带合计 125 分钟", cols[2].contains("合计 125 分钟"), cols[2]);
        check("写周报 75 分钟（2 次）", cols[2].contains("写周报 75 分钟（2 次）"), cols[2]);
        check("标签是番茄", "番茄".equals(cols[3]), cols[3]);

        check("taskOf 正常取值", "评审".equals(pp.taskOf(new String[]{"1", today + " 15:00", "25", "专注", "评审"})));
        check("taskOf 短数组返回空", "".equals(pp.taskOf(new String[]{"1", "x"})));
        check("taskOf null 返回空", "".equals(pp.taskOf(null)));

        /* 本周没有记录时 */
        Store.writeLines("pomodoro_log.txt", new ArrayList<String>());
        check("没记录汇总为空", pp.weekSummary().isEmpty());
        check("没记录汇总备忘为 null", pp.summaryMemoLine() == null);
    }

    /* ================= 2) 弹窗单例 ================= */

    static void dialogSingleton() throws Exception {
        System.out.println("---- 弹窗单例 ----");
        String today = day(0);
        Store.writeLines("pomodoro_log.txt", new ArrayList<String>(Arrays.asList(
                "1\t" + today + " 09:00\t25\t专注\t写周报")));

        final PomodoroPanel pp = new PomodoroPanel();
        SwingUtilities.invokeAndWait(pp::openHistory);
        Thread.sleep(500);
        JDialog first = (JDialog) field(PomodoroPanel.class, pp, "historyDlg");
        check("第一次打开有弹窗", first != null && first.isShowing());
        check("标题是番茄记录", first != null && "番茄记录".equals(first.getTitle()));

        SwingUtilities.invokeAndWait(pp::openHistory);
        Thread.sleep(300);
        JDialog second = (JDialog) field(PomodoroPanel.class, pp, "historyDlg");
        check("再点不叠窗（还是同一个）", second == first);

        int cnt = 0;
        for (Window w : Window.getWindows()) {
            if (w instanceof JDialog && "番茄记录".equals(((JDialog) w).getTitle()) && w.isShowing()) cnt++;
        }
        check("屏幕上只有一个番茄记录", cnt == 1, "cnt=" + cnt);

        /* 关掉后再点是新窗 */
        SwingUtilities.invokeAndWait(first::dispose);
        Thread.sleep(300);
        check("关掉后引用清空", field(PomodoroPanel.class, pp, "historyDlg") == null);
        SwingUtilities.invokeAndWait(pp::openHistory);
        Thread.sleep(300);
        JDialog third = (JDialog) field(PomodoroPanel.class, pp, "historyDlg");
        check("关掉再点是新窗", third != null && third != first && third.isShowing());
        SwingUtilities.invokeAndWait(third::dispose);
    }

    /* ================= 3) 预设下拉复位刷新 ================= */

    static void presetReset() throws Exception {
        System.out.println("---- 预设复位 ----");
        List<String> pf = new ArrayList<String>(Arrays.asList("25", "5", day(0), "0"));
        Store.writeLines("pomodoro.txt", pf);

        SettingsPanel set = new SettingsPanel();
        JComboBox<?> box = findCombo(set, "深度 50 + 10");
        check("找到预设下拉", box != null);
        if (box == null) return;
        check("第一项显示当前 25 + 5", "当前 25 + 5".equals(String.valueOf(box.getItemAt(0))),
                String.valueOf(box.getItemAt(0)));

        SwingUtilities.invokeAndWait(() -> box.setSelectedItem("深度 50 + 10"));
        Thread.sleep(400);
        List<String> after = Store.readLines("pomodoro.txt");
        check("文件改成 50/10", "50".equals(after.get(0).trim()) && "10".equals(after.get(1).trim()),
                after.get(0) + "/" + after.get(1));
        check("选完下拉复位到第一项", box.getSelectedIndex() == 0, "idx=" + box.getSelectedIndex());
        check("第一项刷成「当前 50 + 10」", "当前 50 + 10".equals(String.valueOf(box.getItemAt(0))),
                String.valueOf(box.getItemAt(0)));
        check("四项还是四项", box.getItemCount() == 4, "n=" + box.getItemCount());

        /* 再换一档继续正确 */
        SwingUtilities.invokeAndWait(() -> box.setSelectedItem("速战 15 + 3"));
        Thread.sleep(400);
        List<String> after2 = Store.readLines("pomodoro.txt");
        check("换速战文件 15/3", "15".equals(after2.get(0).trim()) && "3".equals(after2.get(1).trim()),
                after2.get(0) + "/" + after2.get(1));
        check("第一项刷成「当前 15 + 3」", "当前 15 + 3".equals(String.valueOf(box.getItemAt(0))),
                String.valueOf(box.getItemAt(0)));
        check("又复位了", box.getSelectedIndex() == 0);
    }

    /* ================= 4) 概览三处修复 ================= */

    static void overviewFixes() throws Exception {
        System.out.println("---- 概览修复 ----");
        OverviewPanel op = new OverviewPanel();

        /* todoStat 终于挂上去了 */
        JLabel todoStat = (JLabel) field(OverviewPanel.class, op, "todoStat");
        check("todoStat 有父容器", todoStat.getParent() != null);
        Container anc = SwingUtilities.getAncestorOfClass(JPanel.class, todoStat);
        check("todoStat 能连到面板树", anc != null);

        /* 归档按日期搜索时高亮 */
        setField(OverviewPanel.class, op, "archQuery", "10-2");
        Method archLine = OverviewPanel.class.getDeclaredMethod("archLine", String.class, boolean.class);
        archLine.setAccessible(true);
        String hl = (String) archLine.invoke(op, "10-02　买牛奶", false);
        check("10-2 命中 10-02 能高亮", hl != null, "null");
        check("高亮包住 10-02", hl != null && hl.contains("background:#FDE68A") && hl.contains("10-02"),
                String.valueOf(hl));
        String hlMiss = (String) archLine.invoke(op, "10-03　买牛奶", false);
        check("10-2 不命中 10-03", hlMiss == null, String.valueOf(hlMiss));
        setField(OverviewPanel.class, op, "archQuery", "11");
        String hl2 = (String) archLine.invoke(op, "11-02　买牛奶", false);
        check("单数字 11 命中月份能高亮", hl2 != null, "null");
        setField(OverviewPanel.class, op, "archQuery", "牛奶");
        String hl3 = (String) archLine.invoke(op, "10-02　买牛奶", false);
        check("按文字搜仍然高亮文字", hl3 != null && hl3.contains("牛奶"), String.valueOf(hl3));
        setField(OverviewPanel.class, op, "archQuery", "");
        check("清空搜索词后不高亮", archLine.invoke(op, "10-02　买牛奶", false) == null);

        /* 待办卡列表保底高度 */
        Object todoBox = field(OverviewPanel.class, op, "todoBox");
        JScrollPane sp = findScrollOf(op, (Component) todoBox);
        check("找到待办卡的滚动区", sp != null);
        check("列表保底 108 高", sp != null && sp.getMinimumSize().height >= 108,
                sp == null ? "null" : String.valueOf(sp.getMinimumSize().height));

        /* 今日专注卡联动：openHistory 已是 public，概览第一张卡就是番茄钟 */
        String[] keys = (String[]) field(OverviewPanel.class, null, "KEYS");
        check("第一张卡是番茄钟", "pomodoro".equals(keys[0]));
        Method open = PomodoroPanel.class.getMethod("openHistory");
        check("openHistory 公开可调", open != null);
    }

    static JScrollPane findScrollOf(Container root, Component view) {
        for (Component k : root.getComponents()) {
            if (k instanceof JScrollPane) {
                JScrollPane s = (JScrollPane) k;
                if (s.getViewport() != null && s.getViewport().getView() == view) return s;
            }
            if (k instanceof Container) {
                JScrollPane r = findScrollOf((Container) k, view);
                if (r != null) return r;
            }
        }
        return null;
    }

    static JComboBox<?> findCombo(Component c, String anyItem) {
        if (c instanceof JComboBox) {
            JComboBox<?> b = (JComboBox<?>) c;
            for (int i = 0; i < b.getItemCount(); i++) {
                if (anyItem.equals(String.valueOf(b.getItemAt(i)))) return b;
            }
        }
        if (c instanceof Container) {
            for (Component k : ((Container) c).getComponents()) {
                JComboBox<?> r = findCombo(k, anyItem);
                if (r != null) return r;
            }
        }
        return null;
    }
}
