import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** 生成仓库 README 用的界面图（概览页 / 记账本页），输出到工程 docs/ 目录 */
public class ShotRepo {

    static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static void main(String[] a) throws Exception {
        File out = new File(a.length > 0 ? a[0] : "docs");
        out.mkdirs();

        File tmp = new File(System.getProperty("java.io.tmpdir"), "toolbox-shot-repo");
        wipe(tmp);
        tmp.mkdirs();
        System.setProperty("toolbox.data.dir", tmp.getAbsolutePath());

        LocalDate t = LocalDate.now();
        java.util.Calendar cal = java.util.Calendar.getInstance();
        String hm = (cal.get(java.util.Calendar.HOUR_OF_DAY) < 10 ? "0" : "") + cal.get(java.util.Calendar.HOUR_OF_DAY)
                + ":" + (cal.get(java.util.Calendar.MINUTE) < 10 ? "0" : "") + cal.get(java.util.Calendar.MINUTE);
        String md = String.format("%02d-%02d", t.getMonthValue(), t.getDayOfMonth());
        String mdy = String.format("%02d-%02d", t.minusDays(1).getMonthValue(), t.minusDays(1).getDayOfMonth());

        Store.writeLines("settings.txt", new ArrayList<String>(Arrays.asList(
                "sound=1", "strong=0", "pin=0",
                "navOrder=overview,schedule,calc,ledger,memo,counter,pomodoro,countdown,habit,toolbox,stats,settings")));

        Store.writeLines("todo.txt", new ArrayList<String>(Arrays.asList(
                "0\t改简历\t" + t,
                "0\t交周报\t" + t,
                "0\t回邮件\t" + t,
                "0\t取快递\t" + t.minusDays(1),
                "1\t早起跑步\t" + t,
                "1\t试记账导入\t" + t,
                "1\t整理桌面\t" + t.minusDays(3))));

        Store.writeLines("ledger.txt", new ArrayList<String>(Arrays.asList(
                "0\t26.00\t午饭 · 螺蛳粉\t" + md + " 12:20\t餐饮",
                "0\t4.00\t地铁\t" + md + " 08:10\t交通",
                "0\t128.00\t超市采购\t" + md + " 19:40\t购物",
                "0\t35.00\t咖啡续命\t" + md + " 15:05\t餐饮",
                "0\t300.00\t房租\t" + mdy + " 09:00\t居住",
                "1\t1200.00\t兼职工资\t" + mdy + " 18:00\t工资")));

        Store.writeLines("pomodoro_log.txt", new ArrayList<String>(Arrays.asList(
                (System.currentTimeMillis() - 3600_000L) + "\t" + t + " 10:05\t25\t专注\t改简历",
                (System.currentTimeMillis() - 3300_000L) + "\t" + t + " 10:40\t25\t专注\t改简历",
                (System.currentTimeMillis() - 1200_000L) + "\t" + t + " 14:10\t25\t专注\t写项目文档",
                (System.currentTimeMillis() - 900_000L) + "\t" + t + " 14:45\t5\t休息\t",
                (System.currentTimeMillis() - 86400_000L) + "\t" + t.minusDays(1) + " 21:00\t25\t专注\t看八股")));

        Store.writeLines("habit.txt", new ArrayList<String>(Arrays.asList("早起", "阅读", "运动")));
        Store.writeLines("habit_log.txt", new ArrayList<String>(Arrays.asList(
                t + "\t早起", t + "\t阅读",
                t.minusDays(1) + "\t早起", t.minusDays(1) + "\t阅读", t.minusDays(1) + "\t运动",
                t.minusDays(2) + "\t早起", t.minusDays(2) + "\t运动")));

        Store.writeLines("memo.txt", new ArrayList<String>(Arrays.asList(
                "周五前把周报交了",
                "面试：提前 10 分钟上线",
                "记账本导入格式：日期 金额 备注")));

        SwingUtilities.invokeAndWait(() -> Main.main(new String[0]));
        Thread.sleep(2600);

        JFrame frame = findFrame();
        if (frame != null) {
            SwingUtilities.invokeAndWait(() -> {
                frame.setSize(1280, 820);
                frame.validate();
                frame.repaint();
            });
            Thread.sleep(1200);
        }

        shotComponent(frame == null ? null : frame.getContentPane(), new File(out, "screenshot-overview.png"));

        SwingUtilities.invokeAndWait(() -> Main.go("ledger"));
        Thread.sleep(1200);
        shotComponent(frame == null ? null : frame.getContentPane(), new File(out, "screenshot-ledger.png"));

        SwingUtilities.invokeAndWait(() -> Main.go("stats"));
        Thread.sleep(1200);
        shotComponent(frame == null ? null : frame.getContentPane(), new File(out, "screenshot-stats.png"));

        SwingUtilities.invokeAndWait(() -> Main.go("pomodoro"));
        Thread.sleep(1200);
        shotComponent(frame == null ? null : frame.getContentPane(), new File(out, "screenshot-pomodoro.png"));

        Thread.sleep(200);
        Runtime.getRuntime().halt(0);
    }

    static JFrame findFrame() {
        for (Window w : Window.getWindows()) {
            if (w instanceof JFrame && w.isVisible()) return (JFrame) w;
        }
        return null;
    }

    static void shotComponent(Component c, File f) throws Exception {
        if (c == null) return;
        int w = c.getWidth(), h = c.getHeight();
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Theme.bg());
        g.fillRect(0, 0, w, h);
        c.printAll(g);
        g.dispose();
        javax.imageio.ImageIO.write(img, "png", f);
        System.out.println("SHOT " + f.getAbsolutePath() + " " + f.length() + " " + w + "x" + h);
    }

    static void wipe(File d) {
        if (!d.exists()) return;
        File[] fs = d.listFiles();
        if (fs != null) for (File f : fs) {
            if (f.isDirectory()) wipe(f);
            f.delete();
        }
    }
}
