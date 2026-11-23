import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** v1.84：番茄记录弹窗（今天/本周 + 勾选进备忘录）、备忘录一键清筛选、设置页时长预设 */
public class FeatureTest69 {

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

    static String day(int back) {
        return LocalDate.now().minusDays(back).toString();
    }

    static void run() throws Exception {
        history();
        presets();
        memoFilterClear();
        settingsPreset();
    }

    /* ================= 1) 番茄记录：今天 / 本周 + 加进备忘录 ================= */

    static void history() throws Exception {
        System.out.println("---- 番茄记录 ----");
        String today = day(0);
        String ws = PomodoroPanel.weekStart();
        LocalDate monday = LocalDate.parse(ws);
        List<String> log = new ArrayList<String>();
        log.add("1\t" + today + " 09:00\t25\t专注\t写周报");
        log.add("2\t" + today + " 10:30\t50\t专注\t改文档");
        log.add("3\t" + today + " 11:00\t5\t休息\t");
        /* 本周内但今天之外的另一天：昨天若在本周用昨天，否则用周一本身；
           今天就是周一时没有这样的日子，跳过 */
        String other = null;
        if (!ws.equals(today)) {
            other = day(1).compareTo(ws) < 0 ? ws : day(1);
            log.add("4\t" + other + " 15:00\t25\t专注\t评审");
        }
        /* 上周日（一定在本周之外） */
        log.add("6\t" + monday.minusDays(1) + " 20:00\t25\t专注\t上周的事");
        Store.writeLines("pomodoro_log.txt", log);

        PomodoroPanel pp = new PomodoroPanel();

        List<String[]> td = pp.historyRows("today");
        check("今天 2 条专注", td.size() == 2, "n=" + td.size());
        check("休息不算进去", td.size() == 2, "n=" + td.size());
        int iEarly = -1, iLate = -1;
        for (int i = 0; i < td.size(); i++) {
            if (td.get(i)[1].contains("09:00")) iEarly = i;
            if (td.get(i)[1].contains("10:30")) iLate = i;
        }
        check("按时间倒序（10:30 在 09:00 前）", iLate >= 0 && iEarly >= 0 && iLate < iEarly,
                "iLate=" + iLate + " iEarly=" + iEarly);

        List<String[]> wk = pp.historyRows("week");
        int weekN = 2 + (other == null ? 0 : 1);
        check("本周 " + weekN + " 条（今天 2" + (other == null ? "" : " + 另一天 1") + "）",
                wk.size() == weekN, "n=" + wk.size());
        boolean hasLastWeek = false;
        for (String[] p : wk) {
            if (p[1].contains("上周的事")) hasLastWeek = true;
        }
        check("上周的不混进本周", !hasLastWeek, "");

        /* 显示文案与备忘录行：用固定记录，不依赖列表顺序 */
        String[] fixed = {"2", today + " 10:30", "50", "专注", "改文档"};
        check("显示文案带日期时间和任务", pp.historyText(fixed).endsWith("50 分钟 · 改文档"),
                pp.historyText(fixed));
        String[] noTask = {"1", today + " 12:00", "25", "专注"};
        check("没任务名时不带点号", pp.historyText(noTask).endsWith("  25 分钟"), pp.historyText(noTask));

        String memoLine = pp.memoLineOf(fixed);
        String[] mp = memoLine.split("\t", -1);
        check("备忘录行四列", mp.length == 4, "cols=" + mp.length);
        check("不置顶", "0".equals(mp[0]));
        check("时间是 MM-dd HH:mm", mp[1].matches("\\d{2}-\\d{2} \\d{2}:\\d{2}"), mp[1]);
        check("正文带分钟和任务", mp[2].equals("专注 50 分钟 · 改文档"), mp[2]);
        check("标签是番茄", "番茄".equals(mp[3]), mp[3]);

        /* 批量加进备忘录 */
        Store.writeLines("memo.txt", new ArrayList<String>(java.util.Arrays.asList("1\t11-01 08:00\t老的一条\t生活")));
        int n = pp.addToMemo(td);
        check("加进去 " + td.size() + " 条", n == td.size(), "n=" + n);
        List<String> memo = Store.readLines("memo.txt");
        check("备忘录从 1 条变 " + (1 + td.size()) + " 条", memo.size() == 1 + td.size(),
                "lines=" + memo.size());
        check("老数据没动", memo.get(0).startsWith("1\t11-01 08:00\t老的一条"), memo.get(0));
        check("新条带番茄标签", memo.get(1).endsWith("\t番茄"), memo.get(1));
        check("加 0 条返回 0", pp.addToMemo(new ArrayList<String[]>()) == 0);
        check("加 null 返回 0", pp.addToMemo(null) == 0);

        /* 记录为空时的文案由界面负责，这里只确认取数为空不报错 */
        Store.writeLines("pomodoro_log.txt", new ArrayList<String>());
        check("没记录时取到空列表", pp.historyRows("today").isEmpty());
        check("本周也没记录", pp.historyRows("week").isEmpty());
    }

    /* ================= 2) 时长预设 ================= */

    static void presets() throws Exception {
        System.out.println("---- 时长预设 ----");
        List<String> pf = new ArrayList<String>();
        pf.add("25");
        pf.add("5");
        pf.add(day(0));
        pf.add("0");
        pf.add("1");
        pf.add("写周报");
        pf.add("8");
        Store.writeLines("pomodoro.txt", pf);

        PomodoroPanel pp = new PomodoroPanel();
        pp.applyPreset(50, 10);
        List<String> after = Store.readLines("pomodoro.txt");
        check("第 1 行变 50", "50".equals(after.get(0).trim()), after.get(0));
        check("第 2 行变 10", "10".equals(after.get(1).trim()), after.get(1));
        check("内存 focusMin=50", ((Integer) field(PomodoroPanel.class, pp, "focusMin")) == 50);
        check("内存 breakMin=10", ((Integer) field(PomodoroPanel.class, pp, "breakMin")) == 10);
        check("剩余时间跟着重置成 50 分", ((Long) field(PomodoroPanel.class, pp, "remainMs")) == 50 * 60000L,
                String.valueOf(field(PomodoroPanel.class, pp, "remainMs")));
        check("其它行没被改", after.size() == 7 && after.get(5).equals("写周报"), String.valueOf(after));
        check("时间显示 50:00", ((JLabel) field(PomodoroPanel.class, pp, "timeLabel")).getText().equals("50:00"),
                ((JLabel) field(PomodoroPanel.class, pp, "timeLabel")).getText());

        /* 越界拒绝 */
        pp.applyPreset(0, 5);
        check("专注 0 分被拒", ((Integer) field(PomodoroPanel.class, pp, "focusMin")) == 50);
        pp.applyPreset(25, 121);
        check("休息 121 分被拒", ((Integer) field(PomodoroPanel.class, pp, "breakMin")) == 10);
        check("被拒时不写文件", Store.readLines("pomodoro.txt").get(0).trim().equals("50"));

        /* 换回经典 */
        pp.applyPreset(25, 5);
        check("换回 25+5", Store.readLines("pomodoro.txt").get(0).trim().equals("25")
                && Store.readLines("pomodoro.txt").get(1).trim().equals("5"));
    }

    /* ================= 3) 备忘录筛选与一键清除 ================= */

    static void memoFilterClear() throws Exception {
        System.out.println("---- 备忘录清筛选 ----");
        List<String> l = new ArrayList<String>();
        l.add("0\t11-10 08:00\t买菜\t生活");
        l.add("0\t11-10 09:00\t交房租\t生活,账单");
        l.add("0\t11-10 10:00\t改文档\t工作");
        Store.writeLines("memo.txt", l);

        MemoPanel mp = new MemoPanel();
        JLabel cnt = (JLabel) field(MemoPanel.class, mp, "countLabel");
        JButton clr = (JButton) field(MemoPanel.class, mp, "clearTagBtn");
        check("默认文案是「共 3 条」", cnt.getText().equals("共 3 条"), cnt.getText());
        check("没筛选时清除按钮不可点", !clr.isEnabled());

        mp.setTagFilter("生活");
        check("筛出 2 条", mp.filteredCount() == 2, "n=" + mp.filteredCount());
        check("文案写「标签 生活 · 2 条」", cnt.getText().equals("标签 生活 · 2 条"), cnt.getText());
        check("筛选中清除按钮可点", clr.isEnabled());

        SwingUtilities.invokeAndWait(() -> clr.doClick());
        Thread.sleep(200);
        check("点一下清掉筛选", mp.tagFilterNow() == null, String.valueOf(mp.tagFilterNow()));
        check("回到全部 3 条", mp.filteredCount() == 3, "n=" + mp.filteredCount());
        check("文案回到「共 3 条」", cnt.getText().equals("共 3 条"), cnt.getText());
        check("按钮又变灰", !clr.isEnabled());
        JComboBox<?> box = (JComboBox<?>) field(MemoPanel.class, mp, "tagBox");
        check("下拉回到「全部标签」", String.valueOf(box.getSelectedItem()).equals("全部标签"),
                String.valueOf(box.getSelectedItem()));

        /* 搜索 + 标签叠加时的文案 */
        mp.setTagFilter("生活");
        JTextField sf = (JTextField) field(MemoPanel.class, mp, "searchField");
        SwingUtilities.invokeAndWait(() -> sf.setText("房租"));
        Thread.sleep(250);
        check("叠加搜索后文案是「找到 1 条」", cnt.getText().equals("找到 1 条"), cnt.getText());
        check("叠加后筛出 1 条", mp.filteredCount() == 1, "n=" + mp.filteredCount());
    }

    /* ================= 4) 设置页的预设下拉 ================= */

    static void settingsPreset() throws Exception {
        System.out.println("---- 设置页预设 ----");
        List<String> pf = new ArrayList<String>();
        pf.add("25");
        pf.add("5");
        pf.add(day(0));
        pf.add("0");
        Store.writeLines("pomodoro.txt", pf);

        SettingsPanel set = new SettingsPanel();
        String all = collectText(set);
        check("设置页有「时长预设」", all.contains("时长预设"), "");
        check("有经典/深度/速战三档", all.contains("经典 25 + 5") && all.contains("深度 50 + 10")
                && all.contains("速战 15 + 3"), "");
        check("第一项显示当前值", all.contains("当前 25 + 5"), "");

        JComboBox<?> box = (JComboBox<?>) findCombo(set, "深度 50 + 10");
        check("找到预设下拉", box != null);
        if (box != null) {
            SwingUtilities.invokeAndWait(() -> box.setSelectedItem("深度 50 + 10"));
            Thread.sleep(400);
            List<String> after = Store.readLines("pomodoro.txt");
            check("选深度后第 1 行 50", "50".equals(after.get(0).trim()), after.get(0));
            check("选深度后第 2 行 10", "10".equals(after.get(1).trim()), after.get(1));
            check("提示行说改成 50 / 10", collectText(set).contains("已改成 专注 50 分 · 休息 10 分"), "");
        }
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
