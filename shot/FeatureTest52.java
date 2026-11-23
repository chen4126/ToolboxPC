import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/** v1.69：清单行"复制一条到明天" + 统计页本周逐日明细入口 + 小窗连勾合并撤回 */
public class FeatureTest52 {

    static int pass = 0, fail = 0;
    static Object ov;
    static Object main;

    public static void main(String[] a) throws Exception {
        SwingUtilities.invokeAndWait(() -> Main.main(new String[0]));
        Thread.sleep(2500);
        Class<?> mc = Class.forName("com.toolbox.pc.Main");
        Field selfF = mc.getDeclaredField("self");
        selfF.setAccessible(true);
        main = selfF.get(null);
        Method pageM = mc.getDeclaredMethod("page", Class.class);
        ov = pageM.invoke(main, Class.forName("com.toolbox.pc.OverviewPanel"));
        Method sel = mc.getDeclaredMethod("selectNav", String.class);
        sel.setAccessible(true);
        SwingUtilities.invokeAndWait(() -> {
            try {
                sel.invoke(main, "overview");
            } catch (Exception ignored) {
            }
        });
        Thread.sleep(1500);

        String todoFile = Store.dir() + "\\todo.txt";

        // ---------- 1) 清单行右键：复制一条到明天 ----------
        JPopupMenu rowMenu = findRowMenu(((JPanel) ov).getComponents());
        check("找到清单行的右键菜单", rowMenu != null);
        if (rowMenu != null) {
            List<String> items = new ArrayList<>();
            for (Component c : rowMenu.getComponents()) {
                if (c instanceof JMenuItem) items.add(((JMenuItem) c).getText());
            }
            check("菜单里有「复制一条到明天」", items.contains("复制一条到明天"));
            int before = Files.readAllLines(Paths.get(todoFile)).size();
            JMenuItem target = null;
            for (Component c : rowMenu.getComponents()) {
                if (c instanceof JMenuItem && "复制一条到明天".equals(((JMenuItem) c).getText())) target = (JMenuItem) c;
            }
            final JMenuItem tt = target;
            if (tt != null) {
                SwingUtilities.invokeAndWait(tt::doClick);
                Thread.sleep(900);
                int after = Files.readAllLines(Paths.get(todoFile)).size();
                check("复制后条目数 +1（" + before + "->" + after + "）", after == before + 1);
                String last = Files.readAllLines(Paths.get(todoFile)).get(after - 1);
                java.time.LocalDate tomorrow = java.time.LocalDate.now().plusDays(1);
                check("新条目未完成、日期是明天[" + last + "]",
                        last.startsWith("0\t") && last.endsWith("\t" + tomorrow));
                String src = Files.readAllLines(Paths.get(todoFile)).get(0);
                check("原条目没被动过（首行还是原样）", src.startsWith("0\t") && src.endsWith("\t" + java.time.LocalDate.now()));
            }
        }

        // ---------- 2) 统计页：本周逐日明细入口 ----------
        Object st = pageM.invoke(main, Class.forName("com.toolbox.pc.StatsPanel"));
        Field wcf = st.getClass().getDeclaredField("todoWeekCard");
        wcf.setAccessible(true);
        JPanel weekCard = (JPanel) wcf.get(st);
        JPopupMenu cm = weekCard.getComponentPopupMenu();
        check("统计页待办卡挂了右键菜单", cm != null);
        if (cm != null) {
            List<String> items = new ArrayList<>();
            for (Component c : cm.getComponents()) {
                if (c instanceof JMenuItem) items.add(((JMenuItem) c).getText());
            }
            check("菜单里有「看本周逐日明细」", items.contains("看本周逐日明细"));
            check("菜单仍保留「看具体是哪几条」", items.contains("看具体是哪几条"));
        }
        boolean hasMethod = false;
        for (Method m : st.getClass().getDeclaredMethods()) if (m.getName().equals("showWeekDaily")) hasMethod = true;
        check("showWeekDaily 方法存在", hasMethod);
        Method wdc = ov.getClass().getDeclaredMethod("weekDoneCounts");
        wdc.setAccessible(true);
        int[] counts = (int[]) wdc.invoke(ov);
        check("本周 7 天计数长度 7", counts.length == 7);
        int sum = 0;
        for (int v : counts) sum += v;
        check("本周计数合计 = 3（数据 周一1 + 今天2）[" + sum + "]", sum == 3);

        // ---------- 3) 小窗连勾合并 ----------
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
            Method note = mb.getDeclaredMethod("noteJustDone", String.class);
            note.setAccessible(true);
            Method lineM = mb.getDeclaredMethod("lastDoneLine");
            lineM.setAccessible(true);
            Field batchF = mb.getDeclaredField("doneBatch");
            batchF.setAccessible(true);
            Field undoneF = mb.getDeclaredField("lastDoneWasUndone");
            undoneF.setAccessible(true);
            Field atF = mb.getDeclaredField("lastDoneAt");
            atF.setAccessible(true);

            SwingUtilities.invokeAndWait(() -> {
                try {
                    note.invoke(inst, "今天的事");
                } catch (Exception ignored) {
                }
            });
            check("单条时文案不变[" + txtOf(lineM, inst) + "]", txtOf(lineM, inst).startsWith("已勾「今天的事」"));

            SwingUtilities.invokeAndWait(() -> {
                try {
                    note.invoke(inst, "没定日子的");
                } catch (Exception ignored) {
                }
            });
            check("连勾 2 条 -> 合并文案[" + txtOf(lineM, inst) + "]",
                    txtOf(lineM, inst).contains("已勾 2 条") && txtOf(lineM, inst).contains("一起撤回"));

            SwingUtilities.invokeAndWait(() -> {
                try {
                    note.invoke(inst, "九天前没动");
                } catch (Exception ignored) {
                }
            });
            check("连勾 3 条 -> 已勾 3 条[" + txtOf(lineM, inst) + "]", txtOf(lineM, inst).contains("已勾 3 条"));
            check("批次记录 3 条", ((List<?>) batchF.get(inst)).size() == 3);

            SwingUtilities.invokeAndWait(() -> {
                try {
                    undoneF.set(inst, true);
                } catch (Exception ignored) {
                }
            });
            check("退回态文案[" + txtOf(lineM, inst) + "]",
                    txtOf(lineM, inst).contains("已退回 3 条") && txtOf(lineM, inst).contains("再一起勾上"));
            SwingUtilities.invokeAndWait(() -> {
                try {
                    undoneF.set(inst, false);
                } catch (Exception ignored) {
                }
            });

            // 超过 6 秒窗口后再勾一条 -> 新的一批只有 1 条
            SwingUtilities.invokeAndWait(() -> {
                try {
                    atF.set(inst, System.currentTimeMillis() - 10000);
                    note.invoke(inst, "昨天没做");
                } catch (Exception ignored) {
                }
            });
            check("超时后重新起一批，只剩 1 条", ((List<?>) batchF.get(inst)).size() == 1);
            check("超时后文案回到单条[" + txtOf(lineM, inst) + "]", txtOf(lineM, inst).startsWith("已勾「昨天没做」"));
        }

        System.out.println(fail == 0 ? "ALL PASS (" + pass + ")" : "SOME FAILED (" + pass + " pass, " + fail + " fail)");
        Runtime.getRuntime().halt(fail == 0 ? 0 : 1);
    }

    static String txtOf(Method lineM, Object inst) {
        final String[] out = {""};
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    Object l = lineM.invoke(inst);
                    out[0] = l instanceof JLabel ? ((JLabel) l).getText() : "";
                } catch (Exception ignored) {
                }
            });
        } catch (Exception ignored) {
        }
        return out[0];
    }

    static JPopupMenu findRowMenu(Component[] cs) {
        for (Component c : cs) {
            JPopupMenu m = c instanceof JComponent ? ((JComponent) c).getComponentPopupMenu() : null;
            if (m != null) {
                for (Component k : m.getComponents()) {
                    if (k instanceof JMenuItem && "复制一条到明天".equals(((JMenuItem) k).getText())) return m;
                }
            }
            if (c instanceof Container) {
                JPopupMenu r = findRowMenu(((Container) c).getComponents());
                if (r != null) return r;
            }
        }
        return null;
    }

    static void check(String name, boolean ok) {
        System.out.println((ok ? "[PASS] " : "[FAIL] ") + name);
        if (ok) pass++;
        else fail++;
    }
}
