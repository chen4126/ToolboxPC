import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** v1.83：番茄钟自动记备忘录 + 统计小结复制/接着专注 + 设置页标签改名合并 */
public class FeatureTest68 {

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

    static String d(int back) {
        return LocalDate.now().minusDays(back).toString();
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

    static void call(Class<?> c, Object o, String name, Class<?>[] types, Object... args) throws Exception {
        Method m = c.getDeclaredMethod(name, types);
        m.setAccessible(true);
        m.invoke(o, args);
    }

    static void run() throws Exception {
        storeFlags();
        autoMemo();
        tagRename();
        statsActions();
        uiBits();
    }

    /* ================= 1) settings 开关读写 ================= */

    static void storeFlags() {
        System.out.println("---- settings 开关 ----");
        Store.writeLines("settings.txt", new ArrayList<String>());

        check("没这个键时默认 true", Store.flag("pomoMemo", true));
        check("没这个键时默认 false", !Store.flag("pomoMemo", false));

        Store.setFlag("pomoMemo", false);
        check("写成 0", Store.flag("pomoMemo", true) == false);
        check("文件里出现 pomoMemo=0", Store.readLines("settings.txt").get(0).equals("pomoMemo=0"),
                String.valueOf(Store.readLines("settings.txt")));

        Store.setFlag("pomoMemo", true);
        check("再改成 1", Store.flag("pomoMemo", false));
        check("改值不新增行", Store.readLines("settings.txt").size() == 1,
                "lines=" + Store.readLines("settings.txt").size());

        /* 不动其它键 */
        List<String> base = new ArrayList<String>();
        base.add("sound=1");
        base.add("pin=0");
        base.add("navOrder=schedule,calc");
        Store.writeLines("settings.txt", base);
        Store.setFlag("pomoMemo", false);
        List<String> after = Store.readLines("settings.txt");
        check("其它键原样保留", after.get(0).equals("sound=1") && after.get(1).equals("pin=0")
                && after.get(2).equals("navOrder=schedule,calc"), String.valueOf(after));
        check("新键追加在末尾", after.size() == 4 && after.get(3).equals("pomoMemo=0"), String.valueOf(after));

        /* 值只认 1 */
        base.add("weird=2");
        Store.writeLines("settings.txt", base);
        check("值 2 不算开", !Store.flag("weird", true));
        check("str() 读原样", "2".equals(Store.str("weird", "x")), Store.str("weird", "x"));
        check("str() 缺键回默认", "def".equals(Store.str("nope", "def")));
    }

    /* ================= 2) 专注完成自动记备忘录 ================= */

    static void autoMemo() throws Exception {
        System.out.println("---- 番茄钟自动记备忘录 ----");
        List<String> memo = new ArrayList<String>();
        memo.add("1\t11-11 09:00\t老的一条\t生活");
        memo.add("0\t11-11 10:00\t第二条\t");
        Store.writeLines("memo.txt", memo);
        Store.setFlag("pomoMemo", true);

        PomodoroPanel pp = new PomodoroPanel();
        pp.setTaskName("写周报");
        call(PomodoroPanel.class, pp, "autoMemo", new Class<?>[]{int.class}, 25);

        List<String> l = Store.readLines("memo.txt");
        check("追加一条，不覆盖老数据", l.size() == 3, "lines=" + l.size());
        String[] p = l.get(2).split("\t", -1);
        check("四列格式", p.length == 4, "cols=" + p.length);
        check("第一列 0（不置顶）", "0".equals(p[0]));
        check("第二列是 MM-dd HH:mm", p[1].matches("\\d{2}-\\d{2} \\d{2}:\\d{2}"), p[1]);
        check("第三列带分钟和任务", p[2].equals("专注 25 分钟 · 写周报"), p[2]);
        check("第四列是「番茄」", "番茄".equals(p[3]), p[3]);
        check("老数据没被改", l.get(0).equals("1\t11-11 09:00\t老的一条\t生活"), l.get(0));

        /* 关掉开关就不写 */
        Store.setFlag("pomoMemo", false);
        call(PomodoroPanel.class, pp, "autoMemo", new Class<?>[]{int.class}, 30);
        check("关掉后不再追加", Store.readLines("memo.txt").size() == 3,
                "lines=" + Store.readLines("memo.txt").size());

        /* 没有任务名 */
        Store.setFlag("pomoMemo", true);
        pp.setTaskName("");
        call(PomodoroPanel.class, pp, "autoMemo", new Class<?>[]{int.class}, 5);
        String last = Store.readLines("memo.txt").get(3);
        check("没任务名时只写「专注 N 分钟」", last.split("\t", -1)[2].equals("专注 5 分钟"), last);
        check("不加多余的点号", !last.contains(" · "), last);

        /* 备忘录面板能立刻看到 */
        MemoPanel mp = new MemoPanel();
        check("备忘录读到 4 条", mp.totalCount() == 4, "n=" + mp.totalCount());
        check("新条带「番茄」标签", hasTagAnywhere(mp, "番茄"), "");
        mp.setTagFilter("番茄");
        check("按番茄筛出 2 条", mp.filteredCount() == 2, "n=" + mp.filteredCount());
        mp.setTagFilter(null);

        /* reloadFromDisk 读外部新写入 */
        Store.appendLine("memo.txt", "0\t11-12 18:00\t外面写进来的\t番茄");
        mp.reloadFromDisk();
        check("reloadFromDisk 读到外部新行", mp.totalCount() == 5, "n=" + mp.totalCount());
        check("列表也跟着刷新", mp.filteredCount() == 5, "n=" + mp.filteredCount());
    }

    /* ================= 3) 设置页标签改名 / 合并 ================= */

    static void tagRename() throws Exception {
        System.out.println("---- 标签改名合并 ----");
        List<String> l = new ArrayList<String>();
        l.add("0\t11-10 08:00\t买菜\t生活,账单");
        l.add("0\t11-10 09:00\t交房租\t生活");
        l.add("1\t11-10 10:00\t跑个番茄\t番茄");
        l.add("0\t11-10 11:00\t随手记\t");
        l.add("0\t11-10 12:00\t英文标签\tTAG");
        Store.writeLines("memo.txt", l);
        MemoPanel mp = new MemoPanel();

        Map<String, Integer> c0 = mp.tagCounts();
        check("counts 有 4 个标签", c0.size() == 4, String.valueOf(c0));
        check("生活 2 条", c0.get("生活") != null && c0.get("生活") == 2, String.valueOf(c0.get("生活")));
        check("没标签那条不计入", !c0.containsKey(""), String.valueOf(c0));

        /* 改名 */
        int n = mp.renameTag("生活", "日常");
        check("改名影响 2 条", n == 2, "n=" + n);
        List<String> after = Store.readLines("memo.txt");
        check("第一条变成 日常,账单", after.get(0).split("\t", -1)[3].equals("日常,账单"), after.get(0));
        check("第二条变成 日常", after.get(1).split("\t", -1)[3].equals("日常"), after.get(1));
        check("老的「生活」全没了", !Store.readText("memo.txt").contains("生活"), Store.readText("memo.txt"));
        check("文件和内存一致", mp.totalCount() == 5);

        /* 合并：把一个标签并进已有的 */
        int n2 = mp.renameTag("账单", "日常");
        check("合并影响 1 条", n2 == 1, "n=" + n2);
        check("合并后同一条不重复", Store.readLines("memo.txt").get(0).split("\t", -1)[3].equals("日常"),
                Store.readLines("memo.txt").get(0));

        /* 删标签：新名字留空 */
        int n3 = mp.renameTag("日常", "");
        check("清空标签影响 2 条", n3 == 2, "n=" + n3);
        check("删掉后标签列是空串", Store.readLines("memo.txt").get(0).split("\t", -1)[3].isEmpty(),
                Store.readLines("memo.txt").get(0));
        check("另一个标签不受影响", Store.readLines("memo.txt").get(2).split("\t", -1)[3].equals("番茄"));

        /* 不存在 / 空源 */
        check("改不存在的标签返回 0", mp.renameTag("根本没有", "x") == 0);
        check("源为空返回 0", mp.renameTag("", "x") == 0);
        check("源为 null 返回 0", mp.renameTag(null, "x") == 0);

        /* 大小写不敏感 */
        int n4 = mp.renameTag("tag", "英文");
        check("tAG 也能命中", n4 == 1, "n=" + n4);
        check("改完是 英文", Store.readLines("memo.txt").get(4).split("\t", -1)[3].equals("英文"),
                Store.readLines("memo.txt").get(4));

        /* 新名字带逗号 = 一次打多个标签 */
        int n5 = mp.renameTag("番茄", "番茄,复盘");
        check("改成两个标签", n5 == 1);
        check("多标签写对", Store.readLines("memo.txt").get(2).split("\t", -1)[3].equals("番茄,复盘"),
                Store.readLines("memo.txt").get(2));

        /* 筛选中的标签被改名，筛选跟着走 */
        MemoPanel m2 = new MemoPanel();
        m2.setTagFilter("番茄");
        check("筛选命中 1 条", m2.filteredCount() == 1, "n=" + m2.filteredCount());
        m2.renameTag("番茄", "专注");
        check("改名后筛选跟着换到新名字", "专注".equals(m2.tagFilterNow()), String.valueOf(m2.tagFilterNow()));
        check("改名后仍能筛到", m2.filteredCount() == 1, "n=" + m2.filteredCount());
    }

    /* ================= 4) 统计小结：复制 / 接着专注 ================= */

    static void statsActions() throws Exception {
        System.out.println("---- 统计小结动作 ----");
        String day = d(3);
        List<String> log = new ArrayList<String>();
        String t1 = day + " 09:00", t2 = day + " 14:00", t3 = day + " 16:30";
        log.add("1\t" + t1 + "\t25\t专注\t写周报");
        log.add("2\t" + t2 + "\t50\t专注\t写周报");
        log.add("3\t" + t3 + "\t25\t专注\t评审");
        log.add("4\t" + day + " 20:00\t5\t休息\t");
        log.add("5\t" + d(4) + " 10:00\t25\t专注\t别的日子");
        Store.writeLines("pomodoro_log.txt", log);

        StatsPanel sp = new StatsPanel();
        setField(StatsPanel.class, sp, "sumBestDay", day);
        setField(StatsPanel.class, sp, "sumBestMin", 100);
        setField(StatsPanel.class, sp, "sumBestCnt", 3);

        check("最常做的任务是写周报", "写周报".equals(sp.topTaskOf(day)), sp.topTaskOf(day));
        check("别的日子不串", "别的日子".equals(sp.topTaskOf(d(4))), sp.topTaskOf(d(4)));
        check("没记录的日子返回空", "".equals(sp.topTaskOf(d(9))), sp.topTaskOf(d(9)));

        String task = sp.topTaskOf(day);
        check("任务名取出现最多的，不是最后一条", "写周报".equals(task), task);

        /* 复制到剪贴板 */
        sp.copyBestDay();
        String clip = "";
        try {
            clip = (String) Toolkit.getDefaultToolkit().getSystemClipboard()
                    .getData(java.awt.datatransfer.DataFlavor.stringFlavor);
        } catch (Exception e) {
            clip = "";
        }
        check("剪贴板有内容", !clip.isEmpty(), "len=" + clip.length());
        check("第一行带日期和合计", clip.startsWith("专注记录 " + day), clip.split("\n")[0]);
        check("不带休息记录", !clip.contains("休息"), clip);
        check("带任务名", clip.contains("写周报"), clip);
        check("条数正确（3 条专注）", clip.split("\n").length == 4, String.valueOf(clip.split("\n").length));
    }

    /* ================= 5) 设置页与番茄钟接口 ================= */

    static void uiBits() throws Exception {
        System.out.println("---- 界面小件 ----");
        SettingsPanel set = new SettingsPanel();
        String all = collectText(set);
        check("设置页有「番茄钟」小节", all.contains("番茄钟"), "");
        check("设置页有自动记备忘录开关", all.contains("专注完成后自动在备忘录记一条"), "");
        check("设置页有「备忘录标签」小节", all.contains("备忘录标签"), "");
        check("设置页有改名按钮", all.contains("改名 / 合并"), "");
        check("设置页提示合并语义", all.contains("新名字已经存在就等于合并"), "");
        check("设置页仍有数据区", all.contains("打开数据目录"), "");

        PomodoroPanel pp = new PomodoroPanel();
        pp.setTaskName("接着用");
        JTextField tf = (JTextField) field(PomodoroPanel.class, pp, "taskField");
        check("任务名填进输入框", "接着用".equals(tf.getText()), tf.getText());
        check("currentTask 同步", "接着用".equals(pp.currentTask()), pp.currentTask());
        check("任务名存进 pomodoro.txt 第 6 行",
                Store.readLines("pomodoro.txt").size() >= 6
                        && Store.readLines("pomodoro.txt").get(5).trim().equals("接着用"),
                String.valueOf(Store.readLines("pomodoro.txt")));

        pp.startFocusNow();
        check("接着专注后真的在跑", (Boolean) field(PomodoroPanel.class, pp, "running"));
        check("跑的时候按钮变暂停", ((JButton) field(PomodoroPanel.class, pp, "startBtn")).getText().equals("暂停"),
                ((JButton) field(PomodoroPanel.class, pp, "startBtn")).getText());
        call(PomodoroPanel.class, pp, "reset", new Class<?>[]{});
        check("停掉后按钮回到开始", ((JButton) field(PomodoroPanel.class, pp, "startBtn")).getText().equals("开始"),
                ((JButton) field(PomodoroPanel.class, pp, "startBtn")).getText());
        check("停掉后 running=false", !(Boolean) field(PomodoroPanel.class, pp, "running"));
    }

    static boolean hasTagAnywhere(MemoPanel mp, String tag) {
        for (int i = 0; i < mp.totalCount(); i++) {
            String t = mp.tagAt(i);
            if (t != null && t.toLowerCase().contains(tag.toLowerCase())) return true;
        }
        return false;
    }

    static String collectText(Component c) {
        StringBuilder sb = new StringBuilder();
        collect(c, sb);
        return sb.toString();
    }

    static void collect(Component c, StringBuilder sb) {
        if (c instanceof JLabel) sb.append(((JLabel) c).getText()).append('|');
        if (c instanceof AbstractButton) sb.append(((AbstractButton) c).getText()).append('|');
        if (c instanceof JComboBox) {
            for (int i = 0; i < ((JComboBox<?>) c).getItemCount(); i++) {
                sb.append(String.valueOf(((JComboBox<?>) c).getItemAt(i))).append('|');
            }
        }
        if (c instanceof Container) {
            for (Component k : ((Container) c).getComponents()) collect(k, sb);
        }
    }
}
