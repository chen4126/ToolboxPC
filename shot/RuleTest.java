import com.toolbox.pc.SchedulePanel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class RuleTest {
    public static void main(String[] a) throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
        SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA);
        SimpleDateFormat out = new SimpleDateFormat("yyyy-MM-dd HH:mm EEE", Locale.CHINA);

        check(in, out, "2026-11-10 09:00", "weekly:1,3,5", "2026-11-11 09:00");  // 周二 -> 周三
        check(in, out, "2026-11-13 09:00", "workday", "2026-11-16 09:00");      // 周五 -> 下周一
        check(in, out, "2026-01-31 08:00", "monthly:31", "2026-02-28 08:00");   // 小月取月末
        check(in, out, "2026-11-10 09:00", "daily", "2026-11-11 09:00");
        check(in, out, "2026-11-10 09:00", "none", null);
        check(in, out, "2026-11-10 09:00", "weekly:2", "2026-11-17 09:00");     // 周二到下一个周二
    }

    static void check(SimpleDateFormat in, SimpleDateFormat out, String base, String rule, String expect) throws Exception {
        long t = in.parse(base).getTime();
        long next = SchedulePanel.nextOccurrence(t, rule);
        String got = next == 0 ? null : out.format(new Date(next));
        boolean ok = expect == null ? got == null : (got != null && got.startsWith(expect));
        System.out.println((ok ? "PASS " : "FAIL ") + rule + "  from " + base + "  -> " + got
                + (expect != null ? "  (expect " + expect + ")" : ""));
    }
}
