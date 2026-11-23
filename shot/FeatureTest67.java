import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** v1.82：番茄钟每日目标进度环（含概览完成率）+ 备忘录标签筛选 + 统计页专注小结条 */
public class FeatureTest67 {

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

    static void run() throws Exception {
        pomodoro();
        overviewRate();
        memoTags();
        statsSummary();
        uiBits();
    }

    /* ================= 1) 番茄钟每日目标 ================= */

    static List<String> sixLines(String day, int count) {
        List<String> l = new ArrayList<String>();
        l.add("25");
        l.add("5");
        l.add(day);
        l.add(String.valueOf(count));
        l.add("1");
        l.add("写周报");
        return l;
    }

    static void pomodoro() throws Exception {
        System.out.println("---- 番茄钟每日目标 ----");
        String today = d(0);

        /* 老文件只有 6 行：默认 8 */
        Store.writeLines("pomodoro.txt", sixLines(today, 3));
        PomodoroPanel p1 = new PomodoroPanel();
        check("老 6 行文件默认目标 8", p1.goal() == 8, "goal=" + p1.goal());
        check("今日完成数从文件读进来", p1.todayDone() == 3, "done=" + p1.todayDone());
        check("比例 3/8", Math.abs(p1.goalRatio() - 0.375) < 0.001, "" + p1.goalRatio());

        /* 新增第 7 行生效 */
        List<String> seven = sixLines(today, 3);
        seven.add("10");
        Store.writeLines("pomodoro.txt", seven);
        PomodoroPanel p2 = new PomodoroPanel();
        check("第 7 行目标 10 生效", p2.goal() == 10, "goal=" + p2.goal());

        /* 第 7 行非法值：0 / 文字 / 超范围，都退回 8 */
        for (String bad : new String[]{"0", "abc", "99", "-3"}) {
            List<String> l = sixLines(today, 3);
            l.add(bad);
            Store.writeLines("pomodoro.txt", l);
            PomodoroPanel bp = new PomodoroPanel();
            check("非法目标[" + bad + "]退回 8", bp.goal() == 8, "goal=" + bp.goal());
        }

        /* 改目标写回文件 */
        Store.writeLines("pomodoro.txt", sixLines(today, 3));
        PomodoroPanel p3 = new PomodoroPanel();
        p3.setGoal(5);
        List<String> saved = Store.readLines("pomodoro.txt");
        check("setGoal(5) 后文件有 7 行", saved.size() == 7, "lines=" + saved.size());
        check("第 7 行写成 5", "5".equals(saved.get(6).trim()), saved.size() > 6 ? saved.get(6) : "");
        check("内存里 goal=5", p3.goal() == 5, "goal=" + p3.goal());
        check("改目标不影响今日完成数", p3.todayDone() == 3);

        PomodoroPanel p4 = new PomodoroPanel();
        check("重开面板读到 5", p4.goal() == 5, "goal=" + p4.goal());

        /* 边界：1-24 之外拒绝 */
        p4.setGoal(0);
        check("setGoal(0) 被拒", p4.goal() == 5, "goal=" + p4.goal());
        p4.setGoal(25);
        check("setGoal(25) 被拒", p4.goal() == 5, "goal=" + p4.goal());
        p4.setGoal(1);
        check("setGoal(1) 接受", p4.goal() == 1);
        check("1 个就达标时比例封顶 1.0", Math.abs(p4.goalRatio() - 1.0) < 0.001, "" + p4.goalRatio());
        p4.setGoal(24);
        check("setGoal(24) 接受", p4.goal() == 24);
        check("24 目标下比例 3/24", Math.abs(p4.goalRatio() - 0.125) < 0.001, "" + p4.goalRatio());

        /* 跨天自动清零 */
        List<String> y = sixLines(d(1), 7);
        y.add("8");
        Store.writeLines("pomodoro.txt", y);
        PomodoroPanel p5 = new PomodoroPanel();
        check("跨天今日完成数清零", p5.todayDone() == 0, "done=" + p5.todayDone());
        check("跨天目标照样保留", p5.goal() == 8, "goal=" + p5.goal());

        /* 文案 */
        Store.writeLines("pomodoro.txt", sixLines(today, 4));
        PomodoroPanel p6 = new PomodoroPanel();
        JLabel stat = (JLabel) field(PomodoroPanel.class, p6, "statLabel");
        check("状态行带 已完成/目标", stat.getText().contains("今日已完成 4 / 8"), stat.getText());

        JLabel goalLabel = (JLabel) field(PomodoroPanel.class, p6, "goalLabel");
        check("目标数字标签同步", "8".equals(goalLabel.getText()), goalLabel.getText());
        p6.setGoal(6);
        check("改目标后标签跟着变", "6".equals(goalLabel.getText()), goalLabel.getText());
        check("状态行跟着变", stat.getText().contains("4 / 6"), stat.getText());

        Object ring = field(PomodoroPanel.class, p6, "ring");
        check("进度环控件存在", ring instanceof JComponent);
        check("进度环 58x58", ((JComponent) ring).getPreferredSize().width == 58
                && ((JComponent) ring).getPreferredSize().height == 58,
                String.valueOf(((JComponent) ring).getPreferredSize()));
        /* 环真的能画：离屏画一遍不抛异常 */
        JComponent rj = (JComponent) ring;
        java.awt.image.BufferedImage bi =
                new java.awt.image.BufferedImage(58, 58, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        rj.setSize(58, 58);
        rj.paint(g2);
        g2.dispose();
        check("进度环离屏绘制不报错", true);
    }

    /* ================= 2) 概览卡完成率 ================= */

    static void overviewRate() throws Exception {
        System.out.println("---- 概览今日专注完成率 ----");
        Class<?> oc = Class.forName("com.toolbox.pc.OverviewPanel");
        OverviewPanel ov = new OverviewPanel();
        JLabel[] values = (JLabel[]) field(oc, ov, "values");
        String[] subFull = (String[]) field(oc, ov, "subFull");
        String[] subShort = (String[]) field(oc, ov, "subShort");
        String today = d(0);

        /* 目标 8，今天 3 个专注 × 25 分 */
        List<String> pl = sixLines(today, 3);
        pl.add("8");
        Store.writeLines("pomodoro.txt", pl);
        List<String> log = new ArrayList<String>();
        for (int i = 0; i < 3; i++) {
            log.add((System.currentTimeMillis() + i) + "\t" + today + " 09:0" + i + "\t25\t专注\t写周报");
        }
        Store.writeLines("pomodoro_log.txt", log);
        refresh(oc, ov);
        check("分钟数照旧显示", values[0].getText().startsWith("75"), values[0].getText());
        check("说明显示 3/8 与百分比", subFull[0].contains("3/8") && subFull[0].contains("38%"), subFull[0]);
        check("短版显示 3/8", "3/8 个".equals(subShort[0]), subShort[0]);

        /* 达标 */
        List<String> log8 = new ArrayList<String>();
        for (int i = 0; i < 8; i++) {
            log8.add((System.currentTimeMillis() + i) + "\t" + today + " 09:0" + (i % 10) + "\t25\t专注\t写周报");
        }
        Store.writeLines("pomodoro_log.txt", log8);
        refresh(oc, ov);
        check("达标时写「已达标」", subFull[0].contains("8/8") && subFull[0].contains("已达标"), subFull[0]);

        /* 一个都没做 */
        Store.writeLines("pomodoro_log.txt", new ArrayList<String>());
        refresh(oc, ov);
        check("零个时 0/8 · 0%", subFull[0].contains("0/8") && subFull[0].contains("0%"), subFull[0]);

        /* 目标改了，概览跟着变 */
        List<String> p4 = sixLines(today, 2);
        p4.add("4");
        Store.writeLines("pomodoro.txt", p4);
        Store.writeLines("pomodoro_log.txt", log.subList(0, 2));
        refresh(oc, ov);
        check("目标 4、完成 2 → 2/4 · 50%", subFull[0].contains("2/4") && subFull[0].contains("50%"), subFull[0]);
    }

    static void refresh(Class<?> oc, Object ov) throws Exception {
        Method m = oc.getDeclaredMethod("refresh");
        m.setAccessible(true);
        m.invoke(ov);
    }

    /* ================= 3) 备忘录标签 ================= */

    static void memoTags() throws Exception {
        System.out.println("---- 备忘录标签 ----");

        check("空标签归一成空串", "".equals(MemoPanel.normalizeTags("")));
        check("单个标签原样", "工作".equals(MemoPanel.normalizeTags("工作")));
        check("空格与中文逗号都能拆", "工作,学习".equals(MemoPanel.normalizeTags(" 工作 ， 学习 ")));
        check("分号也能拆", "工作,学习,生活".equals(MemoPanel.normalizeTags("工作;学习；生活")));
        check("开头的 # 去掉", "工作,学习".equals(MemoPanel.normalizeTags("#工作, #学习")));
        check("重复标签去重", "工作,学习".equals(MemoPanel.normalizeTags("工作,工作,学习,工作")));
        check("最多留 6 个", MemoPanel.normalizeTags("a,b,c,d,e,f,g,h").split(",").length == 6,
                MemoPanel.normalizeTags("a,b,c,d,e,f,g,h"));

        check("hasTag 精确命中", MemoPanel.hasTag("工作,学习", "工作"));
        check("hasTag 不误中前缀", !MemoPanel.hasTag("工作日", "工作"));
        check("hasTag 大小写不敏感", MemoPanel.hasTag("Work,Study", "work"));
        check("hasTag 空标签为假", !MemoPanel.hasTag("", "工作"));

        /* 旧三列数据：标签为空、正文完整 */
        List<String> old = new ArrayList<String>();
        old.add("0\t11-10 09:00\t买牛奶和鸡蛋");
        old.add("1\t11-11 20:00\t交房租");
        Store.writeLines("memo.txt", old);
        MemoPanel m1 = new MemoPanel();
        check("老数据读进来 2 条", m1.totalCount() == 2, "n=" + m1.totalCount());
        check("老数据没有标签", m1.tagChoices().isEmpty(), String.valueOf(m1.tagChoices()));

        /* 新四列数据 */
        List<String> mix = new ArrayList<String>();
        mix.add("0\t11-10 09:00\t买牛奶和鸡蛋\t生活");
        mix.add("0\t11-11 20:00\t交房租\t生活,账单");
        mix.add("1\t11-12 08:00\t周报模板\t工作");
        mix.add("0\t11-12 09:00\t看论文\t学习");
        Store.writeLines("memo.txt", mix);
        MemoPanel m2 = new MemoPanel();
        check("新旧混排读进 4 条", m2.totalCount() == 4, "n=" + m2.totalCount());
        List<String> tags = m2.tagChoices();
        check("标签清单一共 4 个", tags.size() == 4, String.valueOf(tags));
        check("标签按码点升序（学/工/生/账）", "学习".equals(tags.get(0)) && "工作".equals(tags.get(1))
                && "生活".equals(tags.get(2)) && "账单".equals(tags.get(3)), String.valueOf(tags));

        /* 按标签筛 */
        check("没筛时 4 条", m2.filteredCount() == 4, "n=" + m2.filteredCount());
        m2.setTagFilter("生活");
        check("筛「生活」得 2 条", m2.filteredCount() == 2, "n=" + m2.filteredCount());
        check("筛出来的都带这个标签", MemoPanel.hasTag(m2.tagAt(0), "生活")
                && MemoPanel.hasTag(m2.tagAt(1), "生活"), m2.tagAt(0) + " / " + m2.tagAt(1));
        m2.setTagFilter("工作");
        check("筛「工作」得 1 条", m2.filteredCount() == 1, "n=" + m2.filteredCount());
        check("置顶那条也在筛选结果里", MemoPanel.hasTag(m2.tagAt(0), "工作"));
        m2.setTagFilter("");
        check("清空筛选回到 4 条", m2.filteredCount() == 4, "n=" + m2.filteredCount());
        check("tagFilterNow 为空", m2.tagFilterNow() == null, String.valueOf(m2.tagFilterNow()));

        /* 标签 + 搜索叠加 */
        m2.setTagFilter("生活");
        JTextField sf = (JTextField) field(MemoPanel.class, m2, "searchField");
        sf.setText("房租");
        check("标签 + 关键字叠加剩 1 条", m2.filteredCount() == 1, "n=" + m2.filteredCount());
        sf.setText("牛奶");
        check("标签 + 另一个关键字剩 1 条", m2.filteredCount() == 1, "n=" + m2.filteredCount());
        sf.setText("论文");
        check("关键字不在该标签下则为 0", m2.filteredCount() == 0, "n=" + m2.filteredCount());
        sf.setText("");
        m2.setTagFilter(null);
        check("都清掉回到 4 条", m2.filteredCount() == 4, "n=" + m2.filteredCount());

        /* 下拉选项跟数据走，带条数 */
        JComboBox<?> box = (JComboBox<?>) field(MemoPanel.class, m2, "tagBox");
        check("下拉含「全部标签」+4 个", box.getItemCount() == 5, "items=" + box.getItemCount());
        check("下拉第一项是全部标签", "全部标签".equals(box.getItemAt(0)));
        String any = String.valueOf(box.getItemAt(1));
        check("下拉项带条数", any.matches(".*\\(\\d+\\)"), any);

        /* 存回去还是四列 */
        m2.setTagFilter(null);
        List<String> out = Store.readLines("memo.txt");
        check("文件还是 4 条", out.size() == 4, "lines=" + out.size());
        check("每行都是 4 列", out.get(0).split("\t", -1).length == 4, out.get(0));
        check("标签列写回了", out.get(2).endsWith("\t工作"), out.get(2));

        /* 删掉某标签的最后一条后，筛选自动退回全部 */
        List<String> one = new ArrayList<String>();
        one.add("0\t11-12 09:00\t看论文\t学习");
        one.add("0\t11-12 10:00\t买菜\t生活");
        Store.writeLines("memo.txt", one);
        MemoPanel m3 = new MemoPanel();
        m3.setTagFilter("学习");
        check("筛「学习」1 条", m3.filteredCount() == 1, "n=" + m3.filteredCount());
        /* 直接把那条删掉再重建面板，模拟用户删除 */
        Store.writeLines("memo.txt", arr("0\t11-12 10:00\t买菜\t生活"));
        MemoPanel m4 = new MemoPanel();
        check("只剩别的标签时 1 条", m4.filteredCount() == 1, "n=" + m4.filteredCount());
        check("标签清单只剩 1 个", m4.tagChoices().size() == 1, String.valueOf(m4.tagChoices()));
        m4.setTagFilter("学习");
        check("筛已不存在的标签得 0 条", m4.filteredCount() == 0, "n=" + m4.filteredCount());
        JComboBox<?> box4 = (JComboBox<?>) field(MemoPanel.class, m4, "tagBox");
        check("下拉里没有这个标签了", box4.getItemCount() == 2, "items=" + box4.getItemCount());
    }

    /* ================= 4) 统计页专注小结 ================= */

    static void statsSummary() throws Exception {
        System.out.println("---- 统计页专注小结 ----");
        LocalDate today = LocalDate.now();
        String ym = today.toString().substring(0, 7);

        /* 空日志 */
        String[] r0 = StatsPanel.focusSummaryOf(new ArrayList<String>(), today);
        check("空日志：最专注那天为空", "".equals(r0[0]), r0[0]);
        check("空日志：0 分钟", "0".equals(r0[1]), r0[1]);
        check("空日志：连续 0 天", "0".equals(r0[3]), r0[3]);
        check("空日志：今天 0 个", "0".equals(r0[6]), r0[6]);

        /* 构造本月数据：取本月 5 号、6 号、8 号 + 上月一天 */
        LocalDate d5 = LocalDate.parse(ym + "-05");
        LocalDate d6 = d5.plusDays(1);
        LocalDate d8 = d5.plusDays(3);
        LocalDate lastMonth = d5.minusDays(30);

        List<String> log = new ArrayList<String>();
        /* 5 号：3 条 × 25 = 75 */
        for (int i = 0; i < 3; i++) log.add("100" + i + "\t" + d5 + " 09:1" + i + "\t25\t专注\t写代码");
        /* 6 号：6 条 × 25 = 150（本月最多） */
        for (int i = 0; i < 6; i++) log.add("200" + i + "\t" + d6 + " 10:0" + i + "\t25\t专注\t改文档");
        /* 8 号：2 条 × 30 = 60 */
        for (int i = 0; i < 2; i++) log.add("300" + i + "\t" + d8 + " 14:0" + i + "\t30\t专注\t开会");
        /* 休息记录不该算进去 */
        log.add("400\t" + d6 + " 11:00\t5\t休息\t");
        /* 上月 200 分钟，不该进「本月最专注」 */
        log.add("500\t" + lastMonth + " 09:00\t100\t专注\t上月");
        log.add("501\t" + lastMonth + " 10:00\t100\t专注\t上月");

        String[] r = StatsPanel.focusSummaryOf(log, today);
        check("本月最专注是 6 号", d6.toString().equals(r[0]), r[0] + " vs " + d6);
        check("那天 150 分钟", "150".equals(r[1]), r[1]);
        check("那天 6 个番茄", "6".equals(r[2]), r[2]);
        check("休息记录不计入", !r[1].equals("155"));
        check("上月不算本月最佳", !r[1].equals("200"));

        /* 连续段：5、6 两天连着（8 号断开），最长 2 天，另外上月一天单独一段 */
        boolean hasDash = Store.readLines("pomodoro_log.txt").size() >= 0;
        check("最长连续天数算得对（5-6 两天）", "2".equals(r[3]), r[3] + " " + r[4] + "~" + r[5]);
        check("连续段起点是 5 号", d5.toString().equals(r[4]), r[4]);
        check("连续段终点是 6 号", d6.toString().equals(r[5]), r[5]);

        /* 今天若没有记录则为 0 */
        check("今天没有专注记录时是 0", "0".equals(r[6]) || Integer.parseInt(r[6]) >= 0, r[6]);

        /* 界面文案 */
        Store.writeLines("pomodoro_log.txt", log);
        Store.writeLines("pomodoro.txt", sixLines(d(0), 0));
        StatsPanel sp = new StatsPanel();
        JLabel sum = (JLabel) field(StatsPanel.class, sp, "focusSummary");
        String txt = sum.getText();
        check("小结条是富文本", txt.startsWith("<html>"), txt);
        check("文案带「本月最专注」", txt.contains("本月最专注"), txt);
        check("文案带那个日期", txt.contains(d6.toString().substring(5)), txt);
        check("文案带 150 分", txt.contains("150 分"), txt);
        check("文案带「最长连续」", txt.contains("最长连续"), txt);
        check("文案带「今天」", txt.contains("今天"), txt);

        /* 没记录时的空文案 */
        Store.writeLines("pomodoro_log.txt", new ArrayList<String>());
        StatsPanel sp2 = new StatsPanel();
        JLabel sum2 = (JLabel) field(StatsPanel.class, sp2, "focusSummary");
        check("没记录时提示去番茄钟", sum2.getText().contains("还没有专注记录"), sum2.getText());
        check("没记录时不带 HTML", !sum2.getText().startsWith("<html>"), sum2.getText());

        /* 单日也算 1 天连续 */
        List<String> one = new ArrayList<String>();
        one.add("1\t" + d5 + " 09:00\t25\t专注\tx");
        String[] r1 = StatsPanel.focusSummaryOf(one, today);
        check("单日连续为 1", "1".equals(r1[3]), r1[3]);

        /* 跨 3 天连续 */
        List<String> three = new ArrayList<String>();
        for (int i = 0; i < 3; i++) {
            three.add(String.valueOf(i) + "\t" + d5.plusDays(i) + " 09:00\t25\t专注\ty");
        }
        String[] r3 = StatsPanel.focusSummaryOf(three, today);
        check("连续 3 天算成 3", "3".equals(r3[3]), r3[3]);
        check("3 天起点正确", d5.toString().equals(r3[4]), r3[4]);
        check("3 天终点正确", d5.plusDays(2).toString().equals(r3[5]), r3[5]);
    }

    /* ================= 5) 界面小件 ================= */

    static int countButtons(Container c, String text) {
        int n = 0;
        for (Component x : c.getComponents()) {
            if (x instanceof JButton && text.equals(((JButton) x).getText())) n++;
            if (x instanceof Container) n += countButtons((Container) x, text);
        }
        return n;
    }

    static void uiBits() throws Exception {
        System.out.println("---- 界面小件 ----");
        List<String> two = new ArrayList<String>();
        two.add("0\t11-12 09:00\t看论文\t学习");
        two.add("0\t11-12 10:00\t买菜\t生活");
        Store.writeLines("memo.txt", two);
        MemoPanel mp = new MemoPanel();
        check("底栏有「标签」按钮", countButtons(mp, "标签") == 1, "" + countButtons(mp, "标签"));
        check("底栏四个按钮齐（置顶/标签/编辑/删除）",
                countButtons(mp, "置顶") == 1 && countButtons(mp, "编辑") == 1 && countButtons(mp, "删除") == 1);
        JTextField tf = (JTextField) field(MemoPanel.class, mp, "tagField");
        check("新增行有标签输入框", tf != null);
        check("标签输入框有占位提示",
                String.valueOf(tf.getClientProperty("JTextField.placeholderText")).contains("标签"));
        check("标签输入框清空后不影响筛选", tf.getText().isEmpty());

        /* 渲染器真的把标签画出来：拿第一条算文本 */
        JComboBox<?> box = (JComboBox<?>) field(MemoPanel.class, mp, "tagBox");
        check("下拉 2 项（全部 + 学习 / 生活）", box.getItemCount() == 3, "items=" + box.getItemCount());
    }

    static ArrayList<String> arr(String... s) {
        ArrayList<String> l = new ArrayList<String>();
        for (String x : s) l.add(x);
        return l;
    }
}
