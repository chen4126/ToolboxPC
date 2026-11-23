import com.toolbox.pc.*;

import javax.swing.*;
import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** v1.81：习惯连续里程碑 + 概览「今日打卡」第 7 卡 + 文件整理按月/按年 + 剪贴板搜索与常用标记 */
public class FeatureTest66 {

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

    static void run() throws Exception {
        Store.writeLines("habit.txt", new ArrayList<String>());
        Store.writeLines("habit_log.txt", new ArrayList<String>());
        Store.writeLines("habit_mile.txt", new ArrayList<String>());
        Store.writeLines("clip.txt", new ArrayList<String>());

        milestones();
        overviewCard();
        tidyByDate();
        clipSearchAndPin();
    }

    /* ---------------- 1) 连续里程碑 ---------------- */

    static void milestones() throws Exception {
        System.out.println("---- 连续里程碑 ----");
        Habit.add("早起");
        Habit.add("喝水");
        List<String> logs = new ArrayList<String>();
        for (int i = 6; i >= 1; i--) logs.add(d(i) + "\t早起");
        Store.writeLines("habit_log.txt", logs);

        check("连打 6 天、今天还没打 → streak 是 6", Habit.streak("早起") == 6, "streak=" + Habit.streak("早起"));
        check("还没到 7 天，不弹庆祝", Habit.milestoneHit("早起") == 0);
        check("MARK 表是 7/30/100", Habit.MARKS.length == 3 && Habit.MARKS[0] == 7
                && Habit.MARKS[1] == 30 && Habit.MARKS[2] == 100);

        Habit.toggle("早起", LocalDate.now());
        check("今天打卡后 streak 变 7", Habit.streak("早起") == 7);
        check("正好第 7 天 → 命中 7 天里程碑", Habit.milestoneHit("早起") == 7);

        Habit.markMilestone("早起", 7);
        check("庆祝过就不再命中", Habit.milestoneHit("早起") == 0);
        check("里程碑记录已落盘", Store.readLines("habit_mile.txt").size() == 1);

        Habit.markMilestone("早起", 7);
        check("重复 mark 不会写两条", Store.readLines("habit_mile.txt").size() == 1);

        /* 撤销再打：已经庆祝过，不该再弹 */
        Habit.toggle("早起", LocalDate.now());
        check("撤销后 streak 回到 6（从昨天数）", Habit.streak("早起") == 6, "streak=" + Habit.streak("早起"));
        Habit.toggle("早起", LocalDate.now());
        check("再打回来也不重复庆祝", Habit.milestoneHit("早起") == 0);

        /* 12 天不属于任何里程碑 */
        List<String> l2 = new ArrayList<String>();
        for (int i = 12; i >= 1; i--) l2.add(d(i) + "\t喝水");
        Store.writeLines("habit_log.txt", l2);
        Habit.toggle("喝水", LocalDate.now());
        check("连到 12 天不命中里程碑", Habit.milestoneHit("喝水") == 0, "streak=" + Habit.streak("喝水"));

        /* 30 天：再造一份 */
        List<String> l30 = new ArrayList<String>();
        for (int i = 29; i >= 1; i--) l30.add(d(i) + "\t喝水");
        Store.writeLines("habit_log.txt", l30);
        check("29 天连打（今天没打）→ streak 29", Habit.streak("喝水") == 29);
        Habit.toggle("喝水", LocalDate.now());
        check("第 30 天命中 30 天里程碑", Habit.milestoneHit("喝水") == 30);
        Habit.markMilestone("喝水", 30);
        check("30 天庆祝落盘", Store.readLines("habit_mile.txt").size() == 2);
        check("7 天那条还在（不被覆盖）", Store.readLines("habit_mile.txt").get(0).contains("早起"));
    }

    /* ---------------- 2) 概览第 7 张卡 ---------------- */

    static void overviewCard() throws Exception {
        System.out.println("---- 概览「今日打卡」卡 ----");
        Class<?> oc = Class.forName("com.toolbox.pc.OverviewPanel");
        String[] keys = (String[]) field(oc, null, "KEYS");
        String[] titles = (String[]) field(oc, null, "TITLES");
        check("指标卡扩到 7 张", keys.length == 7 && titles.length == 7, "keys=" + keys.length);
        check("第 7 张是 habit / 今日打卡", "habit".equals(keys[6]) && "今日打卡".equals(titles[6]));

        OverviewPanel ov = new OverviewPanel();
        JLabel[] values = (JLabel[]) field(oc, ov, "values");
        JLabel[] subs = (JLabel[]) field(oc, ov, "subs");
        String[] subFull = (String[]) field(oc, ov, "subFull");
        Runnable[] fixers = (Runnable[]) field(oc, ov, "fixers");
        check("values/subs 数组长度 7", values.length == 7 && subs.length == 7);
        check("fixers 数组长度 7", fixers.length == 7);
        check("第 7 张卡真的有控件", values[6] != null && subs[6] != null);

        /* 3 个习惯，今天打 2 个 */
        Store.writeLines("habit.txt", arr("早起", "喝水", "运动"));
        List<String> doneTwo = new ArrayList<String>();
        doneTwo.add(d(0) + "\t早起");
        doneTwo.add(d(0) + "\t喝水");
        /* 早起连 3 天，喝水连 1 天 */
        doneTwo.add(d(1) + "\t早起");
        doneTwo.add(d(2) + "\t早起");
        Store.writeLines("habit_log.txt", doneTwo);
        refresh(oc, ov);
        check("打卡卡显示 2 / 3", "2 / 3".equals(values[6].getText()), values[6].getText());
        check("说明里带最长连击", subFull[6] != null && subFull[6].contains("最长 3 天"), String.valueOf(subFull[6]));

        /* 全打完 */
        List<String> allDone = new ArrayList<String>(doneTwo);
        allDone.add(d(0) + "\t运动");
        Store.writeLines("habit_log.txt", allDone);
        refresh(oc, ov);
        check("全打完显示 3 / 3", "3 / 3".equals(values[6].getText()), values[6].getText());
        check("全打完的说明", subFull[6] != null && subFull[6].contains("全打完"), String.valueOf(subFull[6]));

        /* 没建习惯 */
        Store.writeLines("habit.txt", new ArrayList<String>());
        Store.writeLines("habit_log.txt", new ArrayList<String>());
        refresh(oc, ov);
        check("没习惯时是 0 / 0", "0 / 0".equals(values[6].getText()), values[6].getText());
        check("没习惯时提示去建", subFull[6] != null && subFull[6].contains("还没建"), String.valueOf(subFull[6]));

        /* 2 行 4 列网格：7 张卡 + 1 个占位 */
        JPanel grid = findGrid(ov, 2, 4);
        check("找到 2×4 网格", grid != null);
        check("网格里有 8 个格子（7 卡 + 占位）", grid != null && grid.getComponentCount() == 8,
                grid == null ? "null" : String.valueOf(grid.getComponentCount()));

        /* 恢复测试数据，别影响后面的用例 */
        Store.writeLines("habit.txt", new ArrayList<String>());
        Store.writeLines("habit_log.txt", new ArrayList<String>());
        Store.writeLines("habit_mile.txt", new ArrayList<String>());
    }

    static JPanel findGrid(JPanel root, int rows, int cols) {
        for (java.awt.Component c : root.getComponents()) {
            if (c instanceof JPanel) {
                JPanel p = (JPanel) c;
                if (p.getLayout() instanceof java.awt.GridLayout) {
                    java.awt.GridLayout g = (java.awt.GridLayout) p.getLayout();
                    if (g.getRows() == rows && g.getColumns() == cols) return p;
                }
                JPanel deep = findGrid(p, rows, cols);
                if (deep != null) return deep;
            }
        }
        return null;
    }

    /* ---------------- 3) 文件整理按月 / 按年 ---------------- */

    static void tidyByDate() throws Exception {
        System.out.println("---- 文件整理按日期分档 ----");
        File dir = new File(System.getProperty("java.io.tmpdir"), "ft66-tidy");
        if (dir.exists()) delTree(dir);
        dir.mkdirs();
        make(dir, "a.png", 2026, 11, 5);
        make(dir, "b.jpg", 2026, 11, 20);
        make(dir, "c.txt", 2025, 3, 8);
        make(dir, "d.mp4", 2024, 12, 31);
        File sub = new File(dir, "已有子目录");
        sub.mkdirs();
        new File(sub, "不动.txt").createNewFile();

        List<String[]> typePlan = FileTidy.plan(dir, FileTidy.MODE_TYPE);
        check("按类型：列 4 个文件（子目录不算）", typePlan.size() == 4, "size=" + typePlan.size());
        check("a.png → 图片", "图片".equals(bucketOf(typePlan, "a.png")), bucketOf(typePlan, "a.png"));
        check("c.txt → 文档", "文档".equals(bucketOf(typePlan, "c.txt")), bucketOf(typePlan, "c.txt"));
        check("d.mp4 → 音视频", "音视频".equals(bucketOf(typePlan, "d.mp4")), bucketOf(typePlan, "d.mp4"));

        List<String[]> monthPlan = FileTidy.plan(dir, FileTidy.MODE_MONTH);
        check("按月：a.png 归 2026-11", "2026-11".equals(bucketOf(monthPlan, "a.png")), bucketOf(monthPlan, "a.png"));
        check("按月：c.txt 归 2025-03", "2025-03".equals(bucketOf(monthPlan, "c.txt")), bucketOf(monthPlan, "c.txt"));
        List<String[]> yearPlan = FileTidy.plan(dir, FileTidy.MODE_YEAR);
        check("按年：d.mp4 归 2024", "2024".equals(bucketOf(yearPlan, "d.mp4")), bucketOf(yearPlan, "d.mp4"));

        Map<String, Integer> gc = FileTidy.groupCounts(dir, FileTidy.MODE_MONTH);
        check("按月汇总有 3 个桶", gc.size() == 3, String.valueOf(gc));
        check("2026-11 里 2 个文件", gc.get("2026-11") != null && gc.get("2026-11") == 2, String.valueOf(gc.get("2026-11")));

        check("时间读不出来归「未标注」", "未标注".equals(FileTidy.bucketOf("x.txt", -1, FileTidy.MODE_MONTH)));

        /* 真跑：按月移动 */
        int moved = FileTidy.run(dir, FileTidy.MODE_MONTH);
        check("按月挪了 4 个文件", moved == 4, "moved=" + moved);
        check("2026-11 文件夹已建好", new File(dir, "2026-11").isDirectory());
        check("a.png 已经不在顶层", !new File(dir, "a.png").exists());
        check("a.png 在 2026-11 里", new File(new File(dir, "2026-11"), "a.png").exists());
        check("2026-11 里两个文件", new File(dir, "2026-11").listFiles().length == 2);
        check("子目录整体没动", new File(sub, "不动.txt").exists());
        check("顶层只剩子文件夹", FileTidy.plan(dir, FileTidy.MODE_MONTH).isEmpty());

        /* 重名：再放一个 a.png 进同一桶 */
        make(dir, "a.png", 2026, 11, 6);
        FileTidy.run(dir, FileTidy.MODE_MONTH);
        check("重名变成 a(1).png", new File(new File(dir, "2026-11"), "a(1).png").exists());
        check("原来的 a.png 还在", new File(new File(dir, "2026-11"), "a.png").exists());

        /* 界面：下拉换档 + 表格第二列跟着变 */
        ToolboxPanel tp = new ToolboxPanel();
        tp.setCurrentDir(dir);
        check("默认是「按类型」", tp.tidyMode() == FileTidy.MODE_TYPE);
        tp.setTidyMode(FileTidy.MODE_MONTH);
        check("切到「按月」", tp.tidyMode() == FileTidy.MODE_MONTH);
        make(dir, "e.png", 2026, 2, 2);
        tp.setTidyMode(FileTidy.MODE_MONTH);
        JTable table = null;
        for (java.awt.Component c : allChildren(tp)) {
            if (c instanceof JTable) table = (JTable) c;
        }
        check("找到整理预览表", table != null);
        check("表格里 1 行（只剩 e.png）", table != null && table.getRowCount() == 1,
                table == null ? "null" : String.valueOf(table.getRowCount()));
        check("第二列是按月的桶名 2026-02", table != null && "2026-02".equals(table.getValueAt(0, 1)),
                table == null ? "null" : String.valueOf(table.getValueAt(0, 1)));

        delTree(dir);
    }

    static String bucketOf(List<String[]> plan, String name) {
        for (String[] it : plan) {
            if (it[0].equals(name)) return it[1];
        }
        return "?";
    }

    static void make(File dir, String name, int y, int m, int day) throws Exception {
        File f = new File(dir, name);
        FileWriter w = new FileWriter(f);
        w.write("x");
        w.close();
        long t = LocalDateTime.of(y, m, day, 10, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        if (!f.setLastModified(t)) throw new IllegalStateException("setLastModified 失败：" + name);
    }

    static List<java.awt.Component> allChildren(java.awt.Container c) {
        List<java.awt.Component> out = new ArrayList<java.awt.Component>();
        for (java.awt.Component k : c.getComponents()) {
            out.add(k);
            if (k instanceof java.awt.Container) out.addAll(allChildren((java.awt.Container) k));
        }
        return out;
    }

    static void delTree(File f) {
        File[] fs = f.listFiles();
        if (fs != null) for (File k : fs) delTree(k);
        f.delete();
    }

    /* ---------------- 4) 剪贴板搜索与常用 ---------------- */

    static void clipSearchAndPin() throws Exception {
        System.out.println("---- 剪贴板搜索 / 常用标记 ----");
        Store.writeLines("clip.txt", new ArrayList<String>());
        ClipNotes.add("开会记得带电脑");
        ClipNotes.add("会议纪要模板在共享盘");
        ClipNotes.add("晚饭买西红柿");

        List<String[]> items = ClipNotes.items();
        check("记了 3 条", items.size() == 3, "size=" + items.size());
        check("新的在最前面", items.get(0)[1].startsWith("晚饭"), items.get(0)[1]);
        check("默认都不是常用", "0".equals(items.get(0)[2]) && "0".equals(items.get(2)[2]));

        List<String[]> hit = ClipNotes.search("会议");
        check("搜「会议」命中 1 条", hit.size() == 1, "size=" + hit.size());
        check("搜「会」命中 2 条（开会…、会议…）", ClipNotes.search("会").size() == 2,
                "size=" + ClipNotes.search("会").size());
        check("搜「西红柿」命中 1 条", ClipNotes.search("西红柿").size() == 1);
        check("搜不存在的词命中 0 条", ClipNotes.search("天书").isEmpty());
        check("空词返回全部", ClipNotes.search("  ").size() == 3);

        String[] target = items.get(2);       // 最旧的「开会记得带电脑」
        boolean nowPin = ClipNotes.togglePin(target[0], target[1]);
        check("标常用返回 true", nowPin);
        check("常用数变 1", ClipNotes.pinnedCount() == 1);
        List<String[]> pinList = ClipNotes.pinned();
        check("常用列表里就是那一条", pinList.size() == 1 && pinList.get(0)[1].equals("开会记得带电脑"));
        List<String[]> after = ClipNotes.items();
        check("标完之后标记位是 1", "1".equals(after.get(2)[2]), after.get(2)[2]);
        check("时间字段没被 ★ 污染（显示还是 11-12 12:00 样式）",
                after.get(2)[0].matches("\\d\\d-\\d\\d \\d\\d:\\d\\d"), after.get(2)[0]);

        boolean nowOff = ClipNotes.togglePin(after.get(2)[0], after.get(2)[1]);
        check("再点一下取消", !nowOff && ClipNotes.pinnedCount() == 0);

        /* 上限：160 条里最旧那条标了常用 → 截断后它还在，总数 150 */
        List<String> many = new ArrayList<String>();
        many.add(ClipNotes.STAR + "01-01 08:00\t老祖宗留下的常用句");
        for (int i = 1; i < 160; i++) many.add("01-01 08:" + String.format("%02d", i % 60) + "\t第" + i + "条");
        Store.writeLines("clip.txt", many);
        ClipNotes.add("最新一条");
        int sz = ClipNotes.size();
        check("超过 150 条被截到 150", sz == 150, "size=" + sz);
        check("标常用的那条没被挤掉", hasPinnedText("老祖宗留下的常用句"));
        check("截的是中间最旧的普通条目", !hasText("第1条"));

        /* 清空但留常用 */
        int dropped = ClipNotes.clearUnpinned();
        check("清空未常用：删 149 条", dropped == 149, "dropped=" + dropped);
        check("清完之后只剩常用 1 条", ClipNotes.size() == 1, "size=" + ClipNotes.size());
        check("留下的正是那条", hasPinnedText("老祖宗留下的常用句"));

        /* 按内容删 */
        List<String[]> one = ClipNotes.items();
        boolean removed = ClipNotes.removeBy(one.get(0)[0], one.get(0)[1]);
        check("removeBy 按内容删掉", removed && ClipNotes.size() == 0);

        /* 界面：搜索框 + 只看常用 */
        Store.writeLines("clip.txt", new ArrayList<String>());
        ClipNotes.add("周报模板");
        ClipNotes.add("周会纪要");
        ClipNotes.add("随便一句");
        ToolboxPanel tp = new ToolboxPanel();
        JList<String> list = tp.clipListComponent();
        check("列表默认 3 行", list.getModel().getSize() == 3, String.valueOf(list.getModel().getSize()));
        tp.clipSearchField().setText("周");
        check("输入「周」筛成 2 行", list.getModel().getSize() == 2, String.valueOf(list.getModel().getSize()));
        tp.clipSearchField().setText("不存在的");
        check("搜不到时列表为空", list.getModel().getSize() == 0);
        tp.clipSearchField().setText("");

        list.setSelectedIndex(1);             // 「周会纪要」
        tp.pinSelectedClipNow();
        check("界面标常用成功", ClipNotes.pinnedCount() == 1);
        check("列表里出现星标", list.getModel().getElementAt(1).startsWith("★"), list.getModel().getElementAt(1));
        tp.clipOnlyPinnedButton().doClick(0);   // setSelected 不触发监听，必须 doClick
        check("「只看常用」筛成 1 行", list.getModel().getSize() == 1, String.valueOf(list.getModel().getSize()));

        Store.writeLines("clip.txt", new ArrayList<String>());
    }

    static boolean hasText(String text) {
        for (String[] it : ClipNotes.items()) {
            if (it[1].equals(text)) return true;
        }
        return false;
    }

    static boolean hasPinnedText(String text) {
        for (String[] it : ClipNotes.items()) {
            if (it[1].equals(text) && "1".equals(it[2])) return true;
        }
        return false;
    }

    /* ---------------- 工具 ---------------- */

    static Object field(Class<?> c, Object inst, String name) throws Exception {
        Field f = c.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(inst);
    }

    static void refresh(Class<?> c, Object inst) throws Exception {
        Method m = c.getDeclaredMethod("refresh");
        m.setAccessible(true);
        m.invoke(inst);
    }

    static List<String> arr(String... s) {
        List<String> out = new ArrayList<String>();
        for (String k : s) out.add(k);
        return out;
    }
}
