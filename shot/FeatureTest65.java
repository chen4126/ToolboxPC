import com.toolbox.pc.*;

import javax.swing.*;
import java.io.File;
import java.io.FileWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** v1.80：习惯打卡（B4）、工具页 / 剪贴板速记（B6）、文件整理助手（B5）、导航扩到 12 项 */
public class FeatureTest65 {

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

    static void run() throws Exception {
        Store.writeLines("habit.txt", new ArrayList<String>());
        Store.writeLines("habit_log.txt", new ArrayList<String>());
        Store.writeLines("clip.txt", new ArrayList<String>());

        /* ===================== B4 习惯打卡：增删改 ===================== */
        check("初始没有习惯", Habit.habits().size() == 0, "" + Habit.habits().size());
        check("加第一个习惯", Habit.add("早起"), "");
        check("加第二个习惯", Habit.add("喝水"), "");
        check("同名不能重复加", !Habit.add("早起"), "");
        check("空白名字拒绝", !Habit.add("   "), "");
        check("超长名字拒绝", !Habit.add("一二三四五六七八九十一二三四五"), "");
        check("当时有两个习惯", Habit.habits().size() == 2, "" + Habit.habits());

        for (int i = 0; i < Habit.MAX_HABITS - 2; i++) Habit.add("习惯" + i);
        check("最多 8 个习惯", Habit.habits().size() == Habit.MAX_HABITS, "" + Habit.habits().size());
        check("满 8 个后拒绝再加", !Habit.add("多余"), "");

        /* 清回两个，方便后面算连续 */
        Store.writeLines("habit.txt", new ArrayList<String>());
        Habit.add("早起");
        Habit.add("喝水");

        /* ===================== B4：打卡与连续天数 ===================== */
        LocalDate today = LocalDate.now();
        check("今天还没打卡", !Habit.done("早起", today), "");
        boolean on = Habit.toggle("早起", today);
        check("打卡后返回 true", on, "");
        check("打卡后 done=true", Habit.done("早起", today), "");
        check("打卡不影响另一个习惯", !Habit.done("喝水", today), "");

        Habit.toggle("早起", today.minusDays(1));
        Habit.toggle("早起", today.minusDays(2));
        check("今天+昨天+前天 连续 3 天", Habit.streak("早起") == 3, "" + Habit.streak("早起"));

        /* 只昨天打过：今天没打，连续应从昨天算 1 天 */
        Habit.toggle("喝水", today.minusDays(1));
        check("今天没打则从昨天数", Habit.streak("喝水") == 1, "" + Habit.streak("喝水"));

        /* 断档：先把昨天的喝水记录撤掉，再造「今天 + 前天」→ 连续只有 1 天 */
        Habit.toggle("喝水", today.minusDays(1));
        Habit.toggle("喝水", today);
        Habit.toggle("喝水", today.minusDays(2));
        check("中间断一天就停", Habit.streak("喝水") == 1, "" + Habit.streak("喝水"));

        check("取消打卡返回 false", !Habit.toggle("喝水", today), "");
        check("取消后 done=false", !Habit.done("喝水", today), "");

        /* 本月完成数：今天只给早起打过卡 */
        int mDone = Habit.doneThisMonth("早起");
        int mDays = Habit.daysSoFarThisMonth();
        check("本月天数到今天就为止", mDays == today.getDayOfMonth(), "" + mDays);
        check("本月完成天数不超过已过天数", mDone <= mDays && mDone >= 1, mDone + "/" + mDays);
        check("本月统计按每天一条算", Habit.doneThisMonth("早起") >= 1, "" + Habit.doneThisMonth("早起"));

        /* 周一为 0 */
        check("本周第 0 天是周一", Habit.weekDay(0).getDayOfWeek().getValue() == 1, "" + Habit.weekDay(0));
        check("本周第 6 天是周日", Habit.weekDay(6).getDayOfWeek().getValue() == 7, "" + Habit.weekDay(6));
        check("本周第 0 天不晚于今天", !Habit.weekDay(0).isAfter(today), Habit.weekDay(0) + " vs " + today);

        /* 改名要把记录带过去 */
        Habit.rename(0, "晨起");
        check("改名后清单变了", Habit.habits().get(0).equals("晨起"), Habit.habits().get(0));
        check("改名后今天的记录跟着走", Habit.done("晨起", today), "");
        check("改名后旧名字没有记录", !Habit.done("早起", today), "");

        /* 删除要把记录一起清掉 */
        Habit.remove(0);
        check("删掉后只剩一个", Habit.habits().size() == 1, "" + Habit.habits());
        check("删掉后记录也没了", !Habit.done("晨起", today), "");

        check("今日概览文案", Habit.todayText().contains("已完成"), Habit.todayText());

        /* ===================== B6 剪贴板速记 ===================== */
        check("换行转义成两个字符", ClipNotes.esc("a\nb").equals("a\\nb"), ClipNotes.esc("a\nb"));
        check("转义能还原", ClipNotes.unesc("a\\nb").equals("a\nb"), ClipNotes.unesc("a\\nb"));
        check("Windows 换行也归一", ClipNotes.esc("a\r\nb").equals("a\\nb"), ClipNotes.esc("a\r\nb"));

        ClipNotes.clear();
        check("清空后 0 条", ClipNotes.size() == 0, "" + ClipNotes.size());
        ClipNotes.add("第一条速记");
        ClipNotes.add("第二条速记");
        check("加两条后 size=2", ClipNotes.size() == 2, "" + ClipNotes.size());
        ClipNotes.add("第二条速记");
        check("连续同一内容不重复记", ClipNotes.size() == 2, "" + ClipNotes.size());

        List<String[]> items = ClipNotes.items();
        check("列表是新的在前", items.get(0)[1].equals("第二条速记"), items.get(0)[1]);
        check("每条带时间", items.get(0)[0].matches("\\d{2}-\\d{2} \\d{2}:\\d{2}"), items.get(0)[0]);

        ClipNotes.add("含换行的\n速记");
        List<String[]> it2 = ClipNotes.items();
        check("换行内容能存能取回", it2.get(0)[1].contains("\n"), it2.get(0)[1]);

        ClipNotes.remove(0);
        check("删一条后 size=2", ClipNotes.size() == 2, "" + ClipNotes.size());
        check("删掉的是最新那条", !ClipNotes.items().get(0)[1].contains("含换行"), ClipNotes.items().get(0)[1]);

        ClipNotes.clear();
        for (int i = 0; i < ClipNotes.MAX + 5; i++) ClipNotes.add("批量" + i);
        check("超出上限按 150 条截断", ClipNotes.size() == ClipNotes.MAX, "" + ClipNotes.size());
        check("留下的是最新的", ClipNotes.items().get(0)[1].equals("批量" + (ClipNotes.MAX + 4)), ClipNotes.items().get(0)[1]);

        /* ===================== B5 文件整理：分类 ===================== */
        check("jpg 归图片", FileTidy.catOf("a.JPG").equals("图片"), FileTidy.catOf("a.JPG"));
        check("png 归图片", FileTidy.catOf("a.png").equals("图片"), "");
        check("docx 归文档", FileTidy.catOf("报告.docx").equals("文档"), "");
        check("xlsx 归表格", FileTidy.catOf("成绩.xlsx").equals("表格"), "");
        check("zip 归压缩包", FileTidy.catOf("包.7z").equals("压缩包"), FileTidy.catOf("包.7z"));
        check("mp4 归音视频", FileTidy.catOf("片子.mp4").equals("音视频"), "");
        check("java 归代码", FileTidy.catOf("Main.java").equals("代码"), "");
        check("无扩展名归其他", FileTidy.catOf("README").equals("其他"), FileTidy.catOf("README"));
        check("类别一共 7 个", FileTidy.CATS.length == 7, "" + FileTidy.CATS.length);

        /* ===================== B5：真造一个乱目录跑一遍 ===================== */
        File tmp = new File(System.getProperty("java.io.tmpdir"), "toolbox-tidy-test");
        delTree(tmp);
        tmp.mkdirs();
        new File(tmp, "keep").mkdirs();
        write(new File(tmp, "a.png"), "x");
        write(new File(tmp, "b.txt"), "x");
        write(new File(tmp, "c.zip"), "x");
        write(new File(tmp, "noext"), "x");

        List<String[]> plan = FileTidy.plan(tmp);
        check("预览只列顶层 4 个文件", plan.size() == 4, "" + plan.size());
        boolean hasKeep = false;
        for (String[] p : plan) if (p[0].equals("keep")) hasKeep = true;
        check("预览不把子目录算进去", !hasKeep, "");

        int[] cnt = FileTidy.counts(tmp);
        check("图片 1 个", cnt[0] == 1, "" + cnt[0]);
        check("文档 1 个", cnt[1] == 1, "" + cnt[1]);
        check("压缩包 1 个", cnt[3] == 1, "" + cnt[3]);
        check("其他 1 个", cnt[6] == 1, "" + cnt[6]);

        int moved = FileTidy.run(tmp);
        check("移动了 4 个文件", moved == 4, "" + moved);
        check("图片进 图片/a.png", new File(tmp, "图片/a.png").exists(), "");
        check("文档进 文档/b.txt", new File(tmp, "文档/b.txt").exists(), "");
        check("压缩包进 压缩包/c.zip", new File(tmp, "压缩包/c.zip").exists(), "");
        check("无扩展名进 其他/noext", new File(tmp, "其他/noext").exists(), "");
        check("顶层原文件已不在了", !new File(tmp, "a.png").exists(), "");
        check("子目录 keep 没被搬走", new File(tmp, "keep").isDirectory(), "");
        check("整理完顶层没有散落文件", FileTidy.plan(tmp).size() == 0, "" + FileTidy.plan(tmp).size());

        /* 重名要自动加 (1) */
        write(new File(tmp, "a.png"), "second");
        FileTidy.run(tmp);
        check("重名自动变成 a(1).png", new File(tmp, "图片/a(1).png").exists(), "");
        check("原来那张没被覆盖", new File(tmp, "图片/a.png").length() == 1, "" + new File(tmp, "图片/a.png").length());
        File u = FileTidy.unique(new File(tmp, "图片"), "a.png");
        check("unique 继续往 a(2) 递推", u.getName().equals("a(2).png"), u.getName());
        delTree(tmp);

        /* ===================== 界面：习惯页 ===================== */
        Store.writeLines("habit.txt", new ArrayList<String>());
        Store.writeLines("habit_log.txt", new ArrayList<String>());
        Habit.add("早起");
        Habit.add("喝水");
        Habit.toggle("早起", LocalDate.now());

        HabitPanel hp = new HabitPanel();
        hp.render();
        check("习惯页认得出两个习惯", hp.habitNames().size() == 2, "" + hp.habitNames());
        check("习惯页今日概览 1/2", Habit.todayText().contains("1 / 2"), Habit.todayText());

        /* ===================== 界面：工具页 ===================== */
        ToolboxPanel tp = new ToolboxPanel();
        check("工具页默认在剪贴板卡", "clip".equals(tp.currentCard()), tp.currentCard());
        tp.showCard("tidy");
        check("切到文件整理卡", "tidy".equals(tp.currentCard()), tp.currentCard());
        tp.showCard("clip");
        check("能切回剪贴板卡", "clip".equals(tp.currentCard()), tp.currentCard());
        check("速记列表条数跟数据一致", tp.clipListComponent().getModel().getSize() == ClipNotes.size(),
                tp.clipListComponent().getModel().getSize() + " vs " + ClipNotes.size());

        /* ===================== 导航扩到 12 项 ===================== */
        List<String> order = Main.navOrder();
        check("导航含习惯打卡", order.contains("habit"), "" + order);
        check("导航含工具页", order.contains("toolbox"), "" + order);
        check("设置仍然收尾", order.get(order.size() - 1).equals("settings"), "" + order);
        check("习惯在记账本前面", order.indexOf("habit") < order.indexOf("ledger"), "" + order);
        check("工具在统计前面", order.indexOf("toolbox") < order.indexOf("stats"), "" + order);
        check("导航标签 habit", Main.navLabel("habit").equals("习惯打卡"), Main.navLabel("habit"));
        check("导航标签 toolbox", Main.navLabel("toolbox").equals("工具"), Main.navLabel("toolbox"));
        check("新页默认启用", Main.isEnabled("habit") && Main.isEnabled("toolbox"), "");

        Store.writeLines("habit.txt", new ArrayList<String>());
        Store.writeLines("habit_log.txt", new ArrayList<String>());
        Store.writeLines("clip.txt", new ArrayList<String>());
    }

    static void write(File f, String text) throws Exception {
        FileWriter w = new FileWriter(f);
        w.write(text);
        w.close();
    }

    static void delTree(File f) {
        if (!f.exists()) return;
        File[] fs = f.listFiles();
        if (fs != null) for (File c : fs) delTree(c);
        f.delete();
    }
}
