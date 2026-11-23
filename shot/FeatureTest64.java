import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** v1.79：文本小工具（A2）、倒数日（A3）、久坐喝水提醒（A1） */
public class FeatureTest64 {

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
        Store.writeLines("days.txt", new ArrayList<String>());
        Store.writeLines("health.txt", new ArrayList<String>());

        /* ===================== A2 文本小工具 ===================== */
        String d10 = TextTools.tsToDate("1699999999");
        check("10 位秒级时间戳转出日期", d10.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"), d10);
        String d13 = TextTools.tsToDate("1699999999000");
        check("13 位毫秒与 10 位结果一致", d10.equals(d13), d10 + " vs " + d13);
        check("空输入给提示", TextTools.tsToDate("").contains("请输入"), TextTools.tsToDate(""));
        check("纯文字不当时间戳", TextTools.tsToDate("abc").contains("不是纯数字"), TextTools.tsToDate("abc"));

        String ts = TextTools.dateToTs("2026-11-12 14:30");
        check("日期转出秒和毫秒两行", ts.contains("秒: ") && ts.contains("毫秒: "), ts);
        check("斜杠分隔能认", TextTools.dateToTs("2026/11/12").contains("秒: "), TextTools.dateToTs("2026/11/12"));
        check("点分隔能认", TextTools.dateToTs("2026.11.12").contains("秒: "), TextTools.dateToTs("2026.11.12"));
        check("看不懂的日期给提示", TextTools.dateToTs("明天").contains("看不懂"), TextTools.dateToTs("明天"));

        String pretty = TextTools.jsonPretty("{\"a\":1,\"b\":{\"c\":[1,2]}}");
        check("JSON 格式化会换行", pretty.contains("\n"), pretty.replace("\n", "\\n"));
        check("JSON 格式化有两空格缩进", pretty.contains("  \"a\""), pretty.replace("\n", "\\n"));
        check("空对象不换行", !TextTools.jsonPretty("{}").contains("\n"),
                TextTools.jsonPretty("{}").replace("\n", "\\n"));
        check("字符串内的逗号冒号不拆行", TextTools.jsonPretty("{\"k\":\"a,b:c\"}").contains("\"a,b:c\""),
                TextTools.jsonPretty("{\"k\":\"a,b:c\"}").replace("\n", "\\n"));
        check("JSON 压缩去掉空白", TextTools.jsonMinify(" { \"a\" : 1 } ").equals("{\"a\":1}"),
                TextTools.jsonMinify(" { \"a\" : 1 } "));
        check("JSON 压缩保留字符串内空格", TextTools.jsonMinify("{\"k\":\"a b\"}").equals("{\"k\":\"a b\"}"),
                TextTools.jsonMinify("{\"k\":\"a b\"}"));

        String b64 = TextTools.b64Enc("小工具箱");
        check("Base64 编出非空串", b64.length() > 0, b64);
        check("Base64 往返还原", "小工具箱".equals(TextTools.b64Dec(b64)), TextTools.b64Dec(b64));
        check("非法 Base64 给提示", TextTools.b64Dec("@@@@").contains("不是合法"), TextTools.b64Dec("@@@@"));
        check("空输入不崩", TextTools.b64Enc("").contains("请输入"), TextTools.b64Enc(""));

        String ue = TextTools.urlEnc("中文 a");
        check("URL 编码出百分号", ue.contains("%"), ue);
        check("URL 往返还原", "中文 a".equals(TextTools.urlDec(ue)), TextTools.urlDec(ue));
        check("非法 URL 编码给提示", TextTools.urlDec("%%").contains("不是合法"), TextTools.urlDec("%%"));

        check("全大写", "ABC".equals(TextTools.upper("abc")), TextTools.upper("abc"));
        check("全小写", "abc".equals(TextTools.lower("ABC")), TextTools.lower("ABC"));
        check("下划线转驼峰", "userName".equals(TextTools.camel("user_name")), TextTools.camel("user_name"));
        check("空格转驼峰", "userName".equals(TextTools.camel("user name")), TextTools.camel("user name"));
        check("中划线转驼峰", "userName".equals(TextTools.camel("user-name")), TextTools.camel("user-name"));
        check("驼峰转下划线", "user_name".equals(TextTools.snake("userName")), TextTools.snake("userName"));
        check("空格转下划线", "user_name".equals(TextTools.snake("user name")), TextTools.snake("user name"));
        check("工具名一共 5 个", TextTools.TOOLS.length == 5, String.valueOf(TextTools.TOOLS.length));

        /* ===================== A3 倒数日 ===================== */
        LocalDate today = LocalDate.now();
        check("一开始没有倒数日", DaysLeft.items().isEmpty(), String.valueOf(DaysLeft.items().size()));
        check("未来的日子能加", DaysLeft.add("四级考试", today.plusDays(38).toString()), "");
        check("加完读得回来", DaysLeft.items().size() == 1, String.valueOf(DaysLeft.items().size()));
        check("未来的文案是还有 N 天", DaysLeft.text("四级考试", today.plusDays(38).toString()).contains("还有 38 天"),
                DaysLeft.text("四级考试", today.plusDays(38).toString()));
        check("今天的文案是就是今天", DaysLeft.text("交材料", today.toString()).contains("就是今天"),
                DaysLeft.text("交材料", today.toString()));
        check("过去的文案是已过 N 天", DaysLeft.text("生日", today.minusDays(12).toString()).contains("已过 12 天"),
                DaysLeft.text("生日", today.minusDays(12).toString()));
        check("同名同日期不重复加", !DaysLeft.add("四级考试", today.plusDays(38).toString()), "");
        check("非法日期加不进去", !DaysLeft.add("乱写", "明天"), "");
        check("空标题加不进去", !DaysLeft.add("   ", today.toString()), "");
        DaysLeft.remove(0);
        check("删掉后列表空了", DaysLeft.items().isEmpty(), String.valueOf(DaysLeft.items().size()));

        /* ===================== A1 久坐 / 喝水 ===================== */
        List<String> cfg = new ArrayList<String>();
        cfg.add("sound=0");
        cfg.add("strong=0");
        cfg.add("sitOn=1");
        cfg.add("sitGap=15");
        cfg.add("waterOn=0");
        cfg.add("waterGap=45");
        Store.writeLines("settings.txt", cfg);
        HealthCare.reload();
        check("读得到久坐开关", HealthCare.sitOn, String.valueOf(HealthCare.sitOn));
        check("读得到久坐间隔", HealthCare.sitMin == 15, String.valueOf(HealthCare.sitMin));
        check("喝水开关默认关", !HealthCare.waterOn, String.valueOf(HealthCare.waterOn));

        HealthCare.loadToday();
        HealthCare.day = LocalDate.now().toString();
        check("今日次数起步为 0", HealthCare.sitCount == 0 && HealthCare.waterCount == 0,
                HealthCare.sitCount + "/" + HealthCare.waterCount);

        long t0 = 5000L;
        HealthCare.lastTick = t0;
        HealthCare.lastActive = t0;
        HealthCare.sitMs = 0;
        HealthCare.tick(t0 + 15000L, false);
        check("人不在时不累计", HealthCare.sitMs == 0, String.valueOf(HealthCare.sitMs));

        HealthCare.lastTick = t0;
        HealthCare.lastActive = t0;
        HealthCare.sitMs = 0;
        for (int i = 0; i < 10; i++) HealthCare.tick(t0 + 15000L * (i + 1), true);
        check("在电脑前会累计（10 步 × 15 秒 = 150000）", HealthCare.sitMs == 150000L,
                String.valueOf(HealthCare.sitMs));

        HealthCare.sitMs = 0;
        HealthCare.sitCount = 0;
        HealthCare.lastTick = t0;
        HealthCare.lastActive = t0;
        long now = t0;
        for (int i = 0; i < 62; i++) {
            now += 15000L;
            HealthCare.tick(now, true);
        }
        check("满 15 分钟提醒一次", HealthCare.sitCount == 1, String.valueOf(HealthCare.sitCount));
        check("提醒后从 0 重新累计（超出 2 步 = 30 秒）", HealthCare.sitMs == 30000L, String.valueOf(HealthCare.sitMs));

        HealthCare.sitMs = 300000L;
        HealthCare.lastTick = t0;
        HealthCare.lastActive = t0;
        HealthCare.tick(t0 + 10 * 60000L, false);
        check("离开超 5 分钟本轮清零", HealthCare.sitMs == 0, String.valueOf(HealthCare.sitMs));
        check("离开不清零今日次数", HealthCare.sitCount == 1, String.valueOf(HealthCare.sitCount));

        HealthCare.saveToday();
        check("health.txt 一天一行", Store.readLines("health.txt").size() == 1,
                String.valueOf(Store.readLines("health.txt").size()));
        check("今日文案带次数", HealthCare.todayText().contains("次"), HealthCare.todayText());
        check("本轮文案含已连续坐", HealthCare.roundText().contains("已连续坐"), HealthCare.roundText());

        /* ===================== 界面：计算器切成两张卡 ===================== */
        CalculatorPanel cp = new CalculatorPanel();
        check("默认停在计算器", "calc".equals(cp.currentCard()), cp.currentCard());
        cp.showCard("text");
        check("能切到文本工具", "text".equals(cp.currentCard()), cp.currentCard());
        cp.showCard("calc");
        check("能切回计算器", "calc".equals(cp.currentCard()), cp.currentCard());

        TextToolsPanel tp = cp.textToolsPanel();
        check("工具下拉 5 项", tp.toolCombo().getItemCount() == 5,
                String.valueOf(tp.toolCombo().getItemCount()));
        tp.toolCombo().setSelectedIndex(0);
        check("时间戳工具 2 个按钮", tp.actionPanel().getComponentCount() == 2,
                String.valueOf(tp.actionPanel().getComponentCount()));
        tp.toolCombo().setSelectedIndex(4);
        check("大小写工具 4 个按钮", tp.actionPanel().getComponentCount() == 4,
                String.valueOf(tp.actionPanel().getComponentCount()));
        check("换工具提示会变", tp.hint().contains("userName"), tp.hint());

        tp.inputArea().setText("hello world");
        ((JButton) tp.actionPanel().getComponent(0)).doClick();
        check("点全大写出结果", "HELLO WORLD".equals(tp.outputArea().getText()), tp.outputArea().getText());
        ((JButton) tp.actionPanel().getComponent(2)).doClick();
        check("点驼峰出结果", "helloWorld".equals(tp.outputArea().getText()), tp.outputArea().getText());

        tp.toolCombo().setSelectedIndex(2);
        tp.inputArea().setText("小工具箱");
        ((JButton) tp.actionPanel().getComponent(0)).doClick();
        String enc = tp.outputArea().getText();
        check("Base64 按钮有输出", enc.length() > 0, enc);
        tp.inputArea().setText(enc);
        ((JButton) tp.actionPanel().getComponent(1)).doClick();
        check("Base64 解码回原文", "小工具箱".equals(tp.outputArea().getText()), tp.outputArea().getText());
    }
}
