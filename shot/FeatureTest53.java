import com.toolbox.pc.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/** v1.69 深测：本周逐日明细面板内容 + 小窗整批退回/再一起勾上（真改数据） */
public class FeatureTest53 {

    static int pass = 0, fail = 0;
    static String todoFile = Store.dir() + "\\todo.txt";
    static final java.time.LocalDate TODAY = java.time.LocalDate.now();
    static final String TODAY_WK =
            new String[]{"周日", "周一", "周二", "周三", "周四", "周五", "周六"}[TODAY.getDayOfWeek().getValue() % 7];

    public static void main(String[] a) throws Exception {
        SwingUtilities.invokeAndWait(() -> Main.main(new String[0]));
        Thread.sleep(2500);
        Class<?> mc = Class.forName("com.toolbox.pc.Main");
        Field selfF = mc.getDeclaredField("self");
        selfF.setAccessible(true);
        Object main = selfF.get(null);
        Method pageM = mc.getDeclaredMethod("page", Class.class);
        Method sel = mc.getDeclaredMethod("selectNav", String.class);
        sel.setAccessible(true);
        SwingUtilities.invokeAndWait(() -> {
            try {
                sel.invoke(main, "overview");
            } catch (Exception ignored) {
            }
        });
        Thread.sleep(1500);

        // ---------- 1) 本周逐日明细面板 ----------
        Object st = pageM.invoke(main, Class.forName("com.toolbox.pc.StatsPanel"));
        Method build = st.getClass().getDeclaredMethod("buildWeekDailyPanel");
        build.setAccessible(true);
        final JPanel[] boxRef = {null};
        SwingUtilities.invokeAndWait(() -> {
            try {
                boxRef[0] = (JPanel) build.invoke(st);
            } catch (Exception ignored) {
            }
        });
        JPanel box = boxRef[0];
        check("面板构造成功", box != null);
        if (box != null) {
            List<String> texts = new ArrayList<>();
            collect(box, texts);
            String all = String.join(" | ", texts);
            int ti = TODAY.getDayOfWeek().getValue() - 1;   // 今天索引，周一=0
            check("有总述「本周共完成 " + Seed.weekDone() + " 条」", all.contains("本周共完成 " + Seed.weekDone() + " 条"));
            if (ti >= 2) {
                check("有天没完成的提示", all.contains("一条没完成"));
            } else {
                check("周一二没有空档提示", !all.contains("一条没完成"));
            }
            String monRow = "";
            for (String t0 : texts) {
                if (t0.contains("周一") && t0.matches(".*\\d{2}-\\d{2}.*")) monRow = t0;
            }
            check("周一那行在[" + monRow + "]",
                    monRow.contains(TODAY.minusDays(ti).toString().substring(5)));
            check("今天行标了（今天）[" + find(texts, "（今天）") + "]", !find(texts, "（今天）").isEmpty());
            check("今天 " + Seed.todayDone() + " 条[" + find(texts, TODAY_WK) + "]",
                    containsPair(box, TODAY_WK, Seed.todayDone() + " 条"));
            check("未来的日子显示「还没到」", all.contains("还没到"));
            check("周一 " + Seed.mondayDone() + " 条", containsPair(box, "周一", Seed.mondayDone() + " 条"));
            check("最勤的一天是" + TODAY_WK,
                    all.contains("最勤的一天：" + TODAY_WK + " " + Seed.todayDone() + " 条"));
            shot(box, "C:\\Users\\chen\\Desktop\\ui-245-week-daily.png");
        }

        // ---------- 2) 小窗整批退回 ----------
        Class<?> mb = Class.forName("com.toolbox.pc.MiniBoard");
        Field instF = mb.getDeclaredField("inst");
        instF.setAccessible(true);
        Method show = mb.getDeclaredMethod("show");
        if (instF.get(null) == null) SwingUtilities.invokeAndWait(() -> {
            try {
                show.invoke(null);
            } catch (Exception ignored) {
            }
        });
        Thread.sleep(1400);
        Object inst = instF.get(null);
        check("小窗已创建", inst != null);
        if (inst != null) {
            String[] three = {"今天的事", "没定日子的", "昨天没做"};
            for (String s : three) Store.markTodoDone(s);
            check("三条已标完成（前置）", doneOf(three) == 3);

            Method note = mb.getDeclaredMethod("noteJustDone", String.class);
            note.setAccessible(true);
            SwingUtilities.invokeAndWait(() -> {
                try {
                    for (String s : three) note.invoke(inst, s);
                } catch (Exception ignored) {
                }
            });
            Method lineM = mb.getDeclaredMethod("lastDoneLine");
            lineM.setAccessible(true);
            final JLabel[] lRef = {null};
            SwingUtilities.invokeAndWait(() -> {
                try {
                    lRef[0] = (JLabel) lineM.invoke(inst);
                } catch (Exception ignored) {
                }
            });
            JLabel l = lRef[0];
            check("提示条是「已勾 3 条」[" + (l == null ? "" : l.getText()) + "]",
                    l != null && l.getText().contains("已勾 3 条"));

            click(l);
            Thread.sleep(700);
            check("点一下整批退回，三条都变未完成（实际 " + doneOf(three) + "）", doneOf(three) == 0);

            SwingUtilities.invokeAndWait(() -> {
                try {
                    lRef[0] = (JLabel) lineM.invoke(inst);
                } catch (Exception ignored) {
                }
            });
            JLabel l2 = lRef[0];
            check("退回态提示条[" + (l2 == null ? "" : l2.getText()) + "]",
                    l2 != null && l2.getText().contains("已退回 3 条"));
            click(l2);
            Thread.sleep(700);
            check("再点一下整批勾回，三条又完成（实际 " + doneOf(three) + "）", doneOf(three) == 3);

            SwingUtilities.invokeAndWait(() -> {
                try {
                    lRef[0] = (JLabel) lineM.invoke(inst);
                } catch (Exception ignored) {
                }
            });
            if (lRef[0] != null) shot(lRef[0], "C:\\Users\\chen\\Desktop\\ui-246-mini-batch.png");
        }

        System.out.println(fail == 0 ? "ALL PASS (" + pass + ")" : "SOME FAILED (" + pass + " pass, " + fail + " fail)");
        Runtime.getRuntime().halt(fail == 0 ? 0 : 1);
    }

    static void click(JLabel l) {
        if (l == null) return;
        for (MouseListener ml : l.getMouseListeners()) {
            ml.mouseClicked(new MouseEvent(l, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
                    0, 0, 0, 2, false, MouseEvent.BUTTON1));
        }
    }

    static int doneOf(String[] names) throws Exception {
        int n = 0;
        for (String line : Files.readAllLines(Paths.get(todoFile))) {
            String[] p = line.split("\t", -1);
            if (p.length < 2) continue;
            for (String s : names) {
                if (s.equals(p[1])) {
                    if ("1".equals(p[0].trim())) n++;
                }
            }
        }
        return n;
    }

    static void collect(Container c, List<String> out) {
        for (Component k : c.getComponents()) {
            if (k instanceof JLabel) {
                String t = ((JLabel) k).getText();
                if (t != null && !t.trim().isEmpty()) out.add(t);
            }
            if (k instanceof Container) collect((Container) k, out);
        }
    }

    static String find(List<String> texts, String key) {
        for (String t : texts) if (t.contains(key)) return t;
        return "";
    }

    static boolean containsPair(JPanel box, String left, String right) {
        JPanel hit = null;
        for (Component k : box.getComponents()) {
            if (!(k instanceof JPanel)) continue;
            List<String> ts = new ArrayList<>();
            collect((Container) k, ts);
            if (ts.size() >= 2 && ts.get(0).contains(left)) {
                hit = (JPanel) k;
                break;
            }
        }
        if (hit == null) return false;
        List<String> ts = new ArrayList<>();
        collect(hit, ts);
        return ts.get(ts.size() - 1).contains(right);
    }

    static void shot(JComponent c, String path) throws Exception {
        final int w = Math.max(200, c.getPreferredSize().width + 8);
        final int h = Math.max(30, c.getPreferredSize().height + 8);
        final JPanel holder = new JPanel(new BorderLayout());
        holder.setOpaque(true);
        holder.setBackground(Color.WHITE);
        holder.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        holder.add(c, BorderLayout.CENTER);
        SwingUtilities.invokeAndWait(() -> {
            holder.setSize(w, h);
            layoutAll(holder);
        });
        Thread.sleep(200);
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        SwingUtilities.invokeAndWait(() -> holder.paint(g));
        g.dispose();
        ImageIO.write(img, "png", new File(path));
        System.out.println("[shot] " + path + " " + w + "x" + h);
    }

    static void layoutAll(Container c) {
        c.doLayout();
        for (Component k : c.getComponents()) {
            if (k instanceof Container) layoutAll((Container) k);
        }
    }

    static void check(String name, boolean ok) {
        System.out.println((ok ? "[PASS] " : "[FAIL] ") + name);
        if (ok) pass++;
        else fail++;
    }
}
