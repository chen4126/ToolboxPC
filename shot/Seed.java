import java.time.LocalDate;

/** 回归用的标准 18 行测试数据：所有日期都按"系统当天"动态生成，不写死 */
public class Seed {

    public static LocalDate today() {
        return LocalDate.now();
    }

    public static LocalDate monday() {
        LocalDate t = today();
        return t.minusDays(t.getDayOfWeek().getValue() - 1);
    }

    /** 本月 15 号（已经过了就取下月 15 号） */
    public static LocalDate d15() {
        LocalDate t = today();
        LocalDate d = t.withDayOfMonth(15);
        return d.isBefore(t) ? d.plusMonths(1).withDayOfMonth(15) : d;
    }

    /** 本月 25 号（已经过了就取下月 25 号） */
    public static LocalDate d25() {
        LocalDate t = today();
        LocalDate d = t.withDayOfMonth(25);
        return d.isBefore(t) ? d.plusMonths(1).withDayOfMonth(25) : d;
    }

    public static String md(LocalDate d) {
        return String.format("%02d-%02d", d.getMonthValue(), d.getDayOfMonth());
    }

    public static String wk(LocalDate d) {
        return new String[]{"周日", "周一", "周二", "周三", "周四", "周五", "周六"}[d.getDayOfWeek().getValue() % 7];
    }

    /** 今天完成的条数：周一时「周一做的」也落在今天，2 → 3 */
    public static int todayDone() {
        return 2 + (today().getDayOfWeek().getValue() == 1 ? 1 : 0);
    }

    /** 本周完成条数：今天 2 + 周一 1，周一总在本周内 → 恒 3 */
    public static int weekDone() {
        return 3;
    }

    /** 上周完成条数：上周二、上周三各 1 → 恒 2 */
    public static int lastWeekDone() {
        return 2;
    }

    /** 上上周完成条数 → 恒 1 */
    public static int week2Done() {
        return 1;
    }

    /** 本月完成（不含归档）：上上周那条可能跨月，动态数 */
    public static int monthDone() {
        LocalDate t = today();
        int n = 0;
        for (String r : rows()) {
            String[] p = r.split("\t", -1);
            if (p.length < 3 || !"1".equals(p[0]) || p[1].endsWith("（已归档）")) continue;
            LocalDate d = LocalDate.parse(p[2].trim());
            if (d.getYear() == t.getYear() && d.getMonthValue() == t.getMonthValue()) n++;
        }
        return n;
    }

    /** 本周有完成记录的不同天数：周一时只有今天 1 天，其它时候 2 天 */
    public static int weekDays() {
        return today().getDayOfWeek().getValue() == 1 ? 1 : 2;
    }

    /** 本周最勤那天（就是周一）的完成条数 */
    public static int mondayDone() {
        return today().getDayOfWeek().getValue() == 1 ? 3 : 1;
    }

    /** 18 行标准数据（两条归档；缺了 FT46 的"归档的没被撤回"会挂） */
    public static String[] rows() {
        LocalDate t = today();
        LocalDate mon = monday();
        return new String[]{
                "0\t今天的事\t" + t,
                "0\t九天前没动\t" + t.minusDays(9),
                "0\t四天前\t" + t.minusDays(4),
                "0\t昨天没做\t" + t.minusDays(1),
                "0\t六天前\t" + t.minusDays(6),
                "0\t十天前\t" + t.minusDays(10),
                "0\t明天的事\t" + t.plusDays(1),
                "0\t十五号\t" + d15(),
                "0\t二十五号\t" + d25(),
                "0\t没定日子的\t-",
                "1\t今天已做完\t" + t,
                "1\t今天第二件\t" + t,
                "1\t周一做的\t" + mon,
                "1\t上周做的\t" + mon.minusDays(5),
                "1\t上周第二条\t" + mon.minusDays(6),
                "1\t上上周做的\t" + mon.minusDays(12),
                "1\t二十天前（已归档）\t" + t.minusDays(20),
                "1\t四十天前（已归档）\t" + t.minusDays(40)};
    }
}
