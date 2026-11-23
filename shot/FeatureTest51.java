import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** 过期筛选行尾部不留悬空分隔符 + 小窗底栏脚注按宽度降级 */
public class FeatureTest51 {

    static int pass = 0, fail = 0;

    public static void main(String[] a) throws Exception {
        SwingUtilities.invokeAndWait(() -> Main.main(new String[0]));
        Thread.sleep(2500);
        Class<?> mc = Class.forName("com.toolbox.pc.Main");
        Field selfF = mc.getDeclaredField("self");
        selfF.setAccessible(true);
        Object main = selfF.get(null);
        Method pageM = mc.getDeclaredMethod("page", Class.class);
        Object ov = pageM.invoke(main, Class.forName("com.toolbox.pc.OverviewPanel"));
        Method sel = mc.getDeclaredMethod("selectNav", String.class);
        sel.setAccessible(true);
        SwingUtilities.invokeAndWait(() -> {
            try {
                sel.invoke(main, "overview");
            } catch (Exception ignored) {
            }
        });
        Thread.sleep(1800);

        // ---------- 1) 过期筛选行：末尾不能是悬空的"｜" ----------
        Field fbf = ov.getClass().getDeclaredField("futureBar");
        fbf.setAccessible(true);
        JPanel futureBar = (JPanel) fbf.get(ov);
        int n = futureBar.getComponentCount();
        check("过期行有内容", n > 0);
        Component last = n > 0 ? futureBar.getComponent(n - 1) : null;
        String lastTxt = last instanceof JLabel ? ((JLabel) last).getText() : "";
        check("末尾不是悬空分隔符(实际[" + lastTxt + "])", !"｜".equals(lastTxt));
        // 三档标签都还在（只是去掉了尾巴）
        boolean hasAnci = false, hasDeep = false, hasNear = false;
        for (int i = 0; i < n; i++) {
            Component c = futureBar.getComponent(i);
            if (!(c instanceof JLabel)) continue;
            String t = ((JLabel) c).getText();
            if (t.contains("很久没动")) hasAnci = true;
            if (t.contains("该处理了")) hasDeep = true;
            if (t.contains("过期未做")) hasNear = true;
        }
        check("三档标签齐全", hasAnci && hasDeep && hasNear);

        // ---------- 2) 小窗底栏脚注 ----------
        Class<?> mb = Class.forName("com.toolbox.pc.MiniBoard");
        Field instF = mb.getDeclaredField("inst");
        instF.setAccessible(true);
        Object inst = instF.get(null);
        Method show = mb.getDeclaredMethod("show");
        if (inst == null) SwingUtilities.invokeAndWait(() -> {
            try {
                show.invoke(null);
            } catch (Exception ignored) {
            }
        });
        Thread.sleep(1500);
        inst = instF.get(null);
        check("小窗已创建", inst != null);
        if (inst == null) {
            System.out.println(fail == 0 ? "ALL PASS (" + pass + ")" : "SOME FAILED");
            Runtime.getRuntime().halt(fail == 0 ? 0 : 1);
            return;
        }
        Field wf = mb.getDeclaredField("win");
        wf.setAccessible(true);
        JFrame win = (JFrame) wf.get(inst);
        JLabel foot = findFoot(win.getContentPane());
        check("找到底栏脚注", foot != null);

        int[] widths = {254, 200, 160};
        for (int w : widths) {
            final int ww = w;
            SwingUtilities.invokeAndWait(() -> {
                win.setSize(ww, win.getHeight());
                win.validate();
            });
            Thread.sleep(700);
            if (foot == null) continue;
            FontMetrics fm = foot.getFontMetrics(foot.getFont());
            int need = fm.stringWidth(foot.getText());
            check("宽 " + w + " 脚注不裁[" + foot.getText() + "] " + need + "<=" + foot.getWidth(),
                    need <= foot.getWidth() + 1);
            check("宽 " + w + " 脚注有内容", foot.getText() != null && !foot.getText().isEmpty());
        }
        // 全文案在提示里
        check("脚注 tooltip 保留全文", foot != null && foot.getToolTipText() != null
                && foot.getToolTipText().contains("双击标题"));

        SwingUtilities.invokeAndWait(() -> {
            win.setSize(254, win.getHeight());
            win.validate();
        });
        Thread.sleep(600);
        check("回到 254 宽恢复完整文案[" + (foot == null ? "" : foot.getText()) + "]",
                foot != null && foot.getText().contains("双击标题"));

        System.out.println(fail == 0 ? "ALL PASS (" + pass + ")" : "SOME FAILED (" + pass + " pass, " + fail + " fail)");
        Runtime.getRuntime().halt(fail == 0 ? 0 : 1);
    }

    static JLabel findFoot(Container c) {
        // 底栏 = 同时含"＋"和脚注的容器；列表里"○ 今天的事"这类行也是 ○ 开头，不能只看文本
        for (Component k : c.getComponents()) {
            if (!(k instanceof Container)) continue;
            Container ct = (Container) k;
            boolean hasPlus = false;
            JLabel cand = null;
            for (Component x : ct.getComponents()) {
                if (!(x instanceof JLabel)) continue;
                String t = ((JLabel) x).getText();
                if ("＋".equals(t)) hasPlus = true;
                else if (t != null && cand == null && (t.startsWith("点") || t.startsWith("○"))) cand = (JLabel) x;
            }
            if (hasPlus && cand != null) return cand;
            JLabel r = findFoot(ct);
            if (r != null) return r;
        }
        return null;
    }

    static void check(String name, boolean ok) {
        System.out.println((ok ? "[PASS] " : "[FAIL] ") + name);
        if (ok) pass++;
        else fail++;
    }
}
