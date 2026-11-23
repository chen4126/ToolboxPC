import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** v1.75：隐藏模块后侧栏排序 —— 模块从左上往下排，设置固定钉在左下最底部 */
public class FeatureTest59 {

    static int pass = 0, fail = 0;
    static Object main;
    static Class<?> mc;

    public static void main(String[] a) throws Exception {
        SwingUtilities.invokeAndWait(() -> Main.main(new String[0]));
        Thread.sleep(2500);
        mc = Class.forName("com.toolbox.pc.Main");
        Field selfF = mc.getDeclaredField("self");
        selfF.setAccessible(true);
        main = selfF.get(null);
        check("侧栏面板存在", navPanel() != null);

        // ---------- 1) 停用 3 项后的顺序 ----------
        List<String> keys = navKeys();
        check("可见项 9 个（" + keys.size() + "）：" + keys, keys.size() == 9);
        check("顺序是 概览/日程提醒/习惯打卡/记账本/番茄钟/倒计时/工具/统计/设置[" + keys + "]",
                keys.equals(Arrays.asList("overview", "schedule", "habit", "ledger",
                        "pomodoro", "countdown", "toolbox", "stats", "settings")));
        check("设置是最后一个可见项", "settings".equals(keys.get(keys.size() - 1)));
        check("停用的三项不在侧栏", !keys.contains("calc") && !keys.contains("memo") && !keys.contains("counter"));

        // ---------- 2) 几何：模块贴顶、设置贴底 ----------
        JPanel nav = navPanel();
        JPanel brand = (JPanel) nav.getComponent(1);
        int navH = nav.getHeight();
        check("侧栏高度正常（" + navH + "）", navH > 600);
        check("品牌区没被拉高（" + brand.getHeight() + " / pref " + brand.getPreferredSize().height + "）",
                brand.getHeight() <= brand.getPreferredSize().height + 2);
        Component first = firstButton();
        check("第一项紧贴品牌下方（y=" + first.getY() + "）", first.getY() <= brand.getY() + brand.getHeight() + 32);
        Component last = lastButton();
        check("最后一项是设置", "settings".equals(((JComponent) last).getClientProperty("navKey")));
        check("设置贴底（底边 " + (last.getY() + last.getHeight()) + " / " + navH + "）",
                navH - (last.getY() + last.getHeight()) <= 40);
        int gapTop = first.getY() - (brand.getY() + brand.getHeight());
        int gapBottom = last.getY() - bottomOfNormalButtons();
        check("空白主要落在模块与设置之间（上 " + gapTop + " / 下 " + gapBottom + "）", gapBottom > gapTop + 100);

        // ---------- 3) 拖拽：普通项能排到最上，设置仍在最底 ----------
        Method move = mc.getDeclaredMethod("moveNav", String.class, int.class);
        move.setAccessible(true);
        SwingUtilities.invokeAndWait(() -> {
            try {
                move.invoke(main, "stats", 0);
            } catch (Exception ignored) {
            }
        });
        Thread.sleep(500);
        keys = navKeys();
        check("统计已被拖到最上[" + keys + "]", keys.get(0).equals("stats") && keys.size() == 9);
        check("拖完设置仍在最底[" + keys + "]", "settings".equals(keys.get(keys.size() - 1)));
        check("navOrder 末尾是 settings[" + Main.navOrder() + "]",
                "settings".equals(Main.navOrder().get(Main.navOrder().size() - 1)));
        check("拖动后设置依旧贴底", navPanel().getHeight() - (lastButton().getY() + lastButton().getHeight()) <= 40);

        // 拖设置本身：不生效
        SwingUtilities.invokeAndWait(() -> {
            try {
                move.invoke(main, "settings", 0);
            } catch (Exception ignored) {
            }
        });
        Thread.sleep(400);
        keys = navKeys();
        check("设置拖不动，仍在最底[" + keys + "]", "settings".equals(keys.get(keys.size() - 1)));
        check("设置没跑到第一位", !"settings".equals(keys.get(0)));

        // ---------- 4) 重新启用备忘录：出现在最后一个普通模块之后、设置之前 ----------
        Main.setEnabled("memo", true);
        Thread.sleep(600);
        keys = navKeys();
        check("备忘录回来了（" + keys.size() + " 项）", keys.contains("memo"));
        check("备忘录排在设置前面[" + keys + "]",
                keys.indexOf("memo") == keys.size() - 2 && "settings".equals(keys.get(keys.size() - 1)));

        // ---------- 5) 全部启用：10 项，设置仍最底 ----------
        Main.setEnabled("calc", true);
        Main.setEnabled("counter", true);
        Thread.sleep(700);
        keys = navKeys();
        check("十二项全回来（" + keys.size() + "）", keys.size() == 12);
        check("全启用时设置仍最底[" + keys + "]", "settings".equals(keys.get(keys.size() - 1)));
        check("全启用时第一项贴顶（y=" + firstButton().getY() + "）", firstButton().getY() <= 100);
        check("十项都在侧栏里放得下（底边 " + (lastButton().getY() + lastButton().getHeight())
                + " / " + navPanel().getHeight() + "）",
                lastButton().getY() + lastButton().getHeight() <= navPanel().getHeight() - 20);

        // ---------- 6) 设置页「导航排序」里设置是固定行，不给上下移 ----------
        Method pageM = mc.getDeclaredMethod("page", Class.class);
        pageM.setAccessible(true);
        Object st = pageM.invoke(main, Class.forName("com.toolbox.pc.SettingsPanel"));
        Field nbF = st.getClass().getDeclaredField("navOrderBox");
        nbF.setAccessible(true);
        JPanel nb = (JPanel) nbF.get(st);
        JPanel pinnedRow = null;
        JPanel firstSortable = null;
        for (Component c : nb.getComponents()) {
            if (!(c instanceof JPanel)) continue;
            for (Component k : ((JPanel) c).getComponents()) {
                if (!(k instanceof JLabel)) continue;
                String txt = ((JLabel) k).getText();
                if (txt.startsWith("设置（固定在左下最底部")) pinnedRow = (JPanel) c;
                if (txt.startsWith("1. ")) firstSortable = (JPanel) c;
            }
        }
        check("排序面板里有设置固定行", pinnedRow != null);
        check("设置行没有上移/下移按钮（" + countButtons(pinnedRow) + " 个）", countButtons(pinnedRow) == 0);
        check("普通行仍是 1. 概览并带上下移（" + countButtons(firstSortable) + " 个）",
                firstSortable != null && countButtons(firstSortable) == 2);
        check("排序面板里不要再出现「" + (10) + ". 设置」这种编号行", !hasNumberedSettings(nb));

        // 点最后一个普通项的 ↓：设置不会被换上来
        Method mi = st.getClass().getDeclaredMethod("moveItem", int.class, int.class);
        mi.setAccessible(true);
        List<String> before = new ArrayList<>(Main.navOrder());
        final int lastSortable = before.size() - 2;
        try {
            mi.invoke(st, lastSortable, 1);
        } catch (Exception ignored) {
        }
        List<String> after = new ArrayList<>(Main.navOrder());
        check("最后一个普通项点↓后顺序不变[" + after + "]", before.equals(after));
        check("试过之后设置仍在末尾", "settings".equals(after.get(after.size() - 1)));

        System.out.println(fail == 0 ? ("ALL PASS (" + pass + ")") : ("SOME FAIL " + fail + "/" + (pass + fail)));
        Runtime.getRuntime().halt(fail == 0 ? 0 : 1);
    }

    static int countButtons(JPanel row) {
        if (row == null) return -1;
        int n = 0;
        for (Component c : row.getComponents()) {
            if (c instanceof JButton) n++;
            if (c instanceof Container) {
                for (Component k : ((Container) c).getComponents()) if (k instanceof JButton) n++;
            }
        }
        return n;
    }

    static boolean hasNumberedSettings(JPanel nb) {
        for (Component c : nb.getComponents()) {
            if (!(c instanceof JPanel)) continue;
            for (Component k : ((JPanel) c).getComponents()) {
                if (!(k instanceof JLabel)) continue;
                String txt = ((JLabel) k).getText();
                if (txt.matches("\\d+\\. 设置")) return true;
            }
        }
        return false;
    }

    static JPanel navPanel() {
        try {
            Field f = main.getClass().getDeclaredField("navPanel");
            f.setAccessible(true);
            return (JPanel) f.get(main);
        } catch (Exception e) {
            return null;
        }
    }

    static List<String> navKeys() {
        List<String> out = new ArrayList<>();
        JPanel nav = navPanel();
        if (nav == null) return out;
        for (Component c : nav.getComponents()) {
            if (c instanceof NavButton) {
                Object k = ((JComponent) c).getClientProperty("navKey");
                if (k != null) out.add(String.valueOf(k));
            }
        }
        return out;
    }

    static Component firstButton() {
        JPanel nav = navPanel();
        for (Component c : nav.getComponents()) if (c instanceof NavButton) return c;
        return null;
    }

    static Component lastButton() {
        JPanel nav = navPanel();
        Component last = null;
        for (Component c : nav.getComponents()) if (c instanceof NavButton) last = c;
        return last;
    }

    static int bottomOfNormalButtons() {
        int max = 0;
        JPanel nav = navPanel();
        for (Component c : nav.getComponents()) {
            if (!(c instanceof NavButton)) continue;
            if ("settings".equals(((JComponent) c).getClientProperty("navKey"))) continue;
            max = Math.max(max, c.getY() + c.getHeight());
        }
        return max;
    }

    static void check(String name, boolean ok) {
        System.out.println((ok ? "[PASS] " : "[FAIL] ") + name);
        if (ok) pass++;
        else fail++;
    }
}
