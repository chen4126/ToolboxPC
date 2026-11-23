import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;

/** v1.76 护眼：连续用眼计时 / 到点提醒 / 离开暂停 / 休息结算 / 落盘 / 设置页文案 */
public class FeatureTest60 {

    static int pass = 0, fail = 0;

    public static void main(String[] a) throws Exception {
        Class<?> ec = Class.forName("com.toolbox.pc.EyeCare");

        Method reload = ec.getDeclaredMethod("reload");
        reload.setAccessible(true);
        Method tick = ec.getDeclaredMethod("tick", long.class, boolean.class);
        tick.setAccessible(true);
        Method loadDay = ec.getDeclaredMethod("loadDay");
        loadDay.setAccessible(true);
        Method finishRest = ec.getDeclaredMethod("finishRest");
        finishRest.setAccessible(true);
        Method beginRest = ec.getDeclaredMethod("beginRest");
        beginRest.setAccessible(true);
        Method snooze = ec.getDeclaredMethod("snooze", int.class);
        snooze.setAccessible(true);
        Method resetRound = ec.getDeclaredMethod("resetRound");
        resetRound.setAccessible(true);
        Method save = ec.getDeclaredMethod("save");
        save.setAccessible(true);
        Method span = ec.getDeclaredMethod("span", long.class);
        span.setAccessible(true);
        Method todayText = ec.getDeclaredMethod("todayText");
        todayText.setAccessible(true);
        Method roundText = ec.getDeclaredMethod("roundText");
        roundText.setAccessible(true);

        Field fOn = field(ec, "on");
        Field fGap = field(ec, "gapMin");
        Field fRest = field(ec, "restSec");
        Field fEye = field(ec, "eyeMs");
        Field fToday = field(ec, "todayMs");
        Field fCount = field(ec, "restCount");
        Field fPeak = field(ec, "peakMs");
        Field fRemind = field(ec, "reminding");
        Field fResting = field(ec, "resting");
        Field fDay = field(ec, "day");
        Field fLastActive = field(ec, "lastActive");
        Field fLastTick = field(ec, "lastTick");

        // ---------- 1) 配置读进来 ----------
        resetRound.invoke(null);
        reload.invoke(null);
        check("默认开启护眼提醒", fOn.getBoolean(null));
        check("默认每 40 分钟提醒 [" + fGap.getInt(null) + "]", fGap.getInt(null) == 40);
        check("默认休息 20 秒 [" + fRest.getInt(null) + "]", fRest.getInt(null) == 20);

        // 清掉今天的历史，从零开始
        fEye.setLong(null, 0L);
        fToday.setLong(null, 0L);
        fCount.setInt(null, 0);
        fPeak.setLong(null, 0L);
        fRemind.setBoolean(null, false);
        fResting.setBoolean(null, false);
        fDay.set(null, LocalDate.now().toString());

        // ---------- 2) 时长文案 ----------
        check("0 → 0 分钟", "0 分钟".equals(span.invoke(null, 0L)));
        check("59 分钟 → 59 分钟", "59 分钟".equals(span.invoke(null, 59L * 60000)));
        check("90 分钟 → 1 小时 30 分", "1 小时 30 分".equals(span.invoke(null, 90L * 60000)));

        // ---------- 3) 连续活跃 39 分不催 ----------
        long t0 = System.currentTimeMillis();
        long now = t0;
        fLastTick.setLong(null, now);
        fLastActive.setLong(null, now);
        for (int i = 0; i < 39 * 12; i++) {   // 39 分钟，每 5 秒一步
            now += 5000;
            tick.invoke(null, now, true);
        }
        check("连续 39 分钟还没到点（reminding=" + fRemind.getBoolean(null) + "）", !fRemind.getBoolean(null));
        long todayAfter39 = fToday.getLong(null);
        check("今日累计约 39 分钟（" + (todayAfter39 / 60000) + " 分）",
                todayAfter39 >= 38L * 60000 && todayAfter39 <= 40L * 60000);

        // ---------- 4) 到 40 分钟该提醒 ----------
        for (int i = 0; i < 12; i++) {
            now += 5000;
            tick.invoke(null, now, true);
        }
        check("连续 40 分钟触发提醒", fRemind.getBoolean(null));

        // ---------- 5) 开始休息后不再累计 ----------
        beginRest.invoke(null);
        long beforeRest = fToday.getLong(null);
        for (int i = 0; i < 4; i++) {
            now += 5000;
            tick.invoke(null, now, true);
        }
        check("休息中 20 秒不加用眼时长", fToday.getLong(null) == beforeRest);

        // ---------- 6) 休息结束：计一次、连续清零 ----------
        fResting.setBoolean(null, true);
        finishRest.invoke(null);
        check("休息次数 +1（" + fCount.getInt(null) + "）", fCount.getInt(null) == 1);
        check("休息后本轮连续归零", fEye.getLong(null) == 0);
        check("今日累计仍保留", fToday.getLong(null) >= 39L * 60000);

        // ---------- 7) 离开 5 分钟暂停计时 ----------
        now += 1000;
        fLastActive.setLong(null, now - 6L * 60000);   // 模拟 6 分钟没有任何输入
        long todayBeforeIdle = fToday.getLong(null);
        fEye.setLong(null, 20L * 60000);               // 假装又用了 20 分钟
        now += 5000;
        tick.invoke(null, now, false);
        check("离开 5 分钟后本轮连续清零", fEye.getLong(null) == 0);
        check("离开期间今日累计不再涨", fToday.getLong(null) == todayBeforeIdle);

        // ---------- 8) 回来继续计 ----------
        fLastActive.setLong(null, now);
        fLastTick.setLong(null, now);
        now += 5000;
        tick.invoke(null, now, true);
        check("回来后又开始累计（" + (fEye.getLong(null) / 1000) + " 秒）", fEye.getLong(null) > 0);

        // ---------- 9) 稍后 10 分钟 ----------
        fRemind.setBoolean(null, true);
        snooze.invoke(null, 10);
        check("稍后提醒会清掉当前催办", !fRemind.getBoolean(null));
        Field fSnooze = field(ec, "snoozeUntil");
        check("稍后 10 分钟内有截止时间", fSnooze.getLong(null) > System.currentTimeMillis() + 8L * 60000);

        // ---------- 10) 落盘 ----------
        fEye.setLong(null, 25L * 60000);
        fToday.setLong(null, 65L * 60000);
        fCount.setInt(null, 2);
        fPeak.setLong(null, 45L * 60000);
        fDay.set(null, LocalDate.now().toString());
        save.invoke(null);
        String file = com.toolbox.pc.Store.dir() + "\\eye.txt";
        List<String> lines = Files.readAllLines(Paths.get(file), StandardCharsets.UTF_8);
        String target = null;
        for (String line : lines) {
            if (line.startsWith(LocalDate.now().toString())) target = line;
        }
        check("eye.txt 写进了今天这一行 [ " + target + " ]", target != null);
        String[] parts = target == null ? new String[0] : target.split("\t");
        check("落盘字段 4 个（" + parts.length + "）", parts.length == 4);
        check("落盘累计 65 分", parts.length == 4 && Long.parseLong(parts[1]) == 65L * 60000);
        check("落盘休息 2 次", parts.length == 4 && parts[2].equals("2"));
        check("落盘最长 45 分", parts.length == 4 && Long.parseLong(parts[3]) == 45L * 60000);

        // 重新加载回来（模拟隔天启动）
        fToday.setLong(null, 0L);
        fCount.setInt(null, 0);
        fPeak.setLong(null, 0L);
        loadDay.invoke(null);
        check("重载后今日用眼恢复（" + (fToday.getLong(null) / 60000) + " 分）", fToday.getLong(null) == 65L * 60000);
        check("重载后休息次数恢复", fCount.getInt(null) == 2);

        // ---------- 11) 文案 ----------
        String tt = (String) todayText.invoke(null);
        check("今日数据文案 [ " + tt + " ]", tt.contains("今日用眼 1 小时 5 分") && tt.contains("休息 2 次")
                && tt.contains("最长连续 45 分钟"));
        fEye.setLong(null, 18L * 60000);
        String rt = (String) roundText.invoke(null);
        check("本轮进度文案 [ " + rt + " ]", rt.contains("本轮连续 18 分钟") && rt.contains("每 40 分钟"));

        // ---------- 12) 关掉提醒仍统计 ----------
        fOn.setBoolean(null, false);
        fEye.setLong(null, 50L * 60000);
        fRemind.setBoolean(null, false);
        fLastActive.setLong(null, System.currentTimeMillis());
        fLastTick.setLong(null, System.currentTimeMillis());
        tick.invoke(null, System.currentTimeMillis() + 5000, true);
        check("关掉提醒后不再弹", !fRemind.getBoolean(null));
        String offText = (String) todayText.invoke(null);
        check("关掉后文案带说明 [ " + offText + " ]", offText.contains("提醒已关闭"));
        fOn.setBoolean(null, true);

        System.out.println(fail == 0 ? ("ALL PASS (" + pass + ")") : ("SOME FAIL " + fail + "/" + (pass + fail)));
        Runtime.getRuntime().halt(fail == 0 ? 0 : 1);
    }

    static Field field(Class<?> c, String name) throws Exception {
        Field f = c.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }

    static void check(String name, boolean ok) {
        if (ok) {
            pass++;
            System.out.println("[PASS] " + name);
        } else {
            fail++;
            System.out.println("[FAIL] " + name);
        }
    }
}
