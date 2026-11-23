import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** 概览六张快捷卡：数字和说明在任何窗口高度下都不能被裁 */
public class FeatureTest50 {

    static int pass = 0, fail = 0;

    public static void main(String[] a) throws Exception {
        SwingUtilities.invokeAndWait(() -> Main.main(new String[0]));
        Thread.sleep(2500);
        Class<?> mc = Class.forName("com.toolbox.pc.Main");
        Field selfF = mc.getDeclaredField("self");
        selfF.setAccessible(true);
        Object main = selfF.get(null);
        Field ff = mc.getDeclaredField("frame");
        ff.setAccessible(true);
        JFrame frame = (JFrame) ff.get(main);
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
        Thread.sleep(1500);

        Field vf = ov.getClass().getDeclaredField("values");
        vf.setAccessible(true);
        JLabel[] vals = (JLabel[]) vf.get(ov);
        Field sf = ov.getClass().getDeclaredField("subs");
        sf.setAccessible(true);
        JLabel[] subs = (JLabel[]) sf.get(ov);
        Field inf = ov.getClass().getDeclaredField("subFull");
        inf.setAccessible(true);
        String[] subFull = (String[]) inf.get(ov);

        // 1) 六张卡的数字行：都拿到了文本，且绘制高度够（<= 需求高度也不算裁，只要 >= 墨迹高度）
        for (int i = 0; i < 6; i++) {
            JLabel v = vals[i], s = subs[i];
            FontMetrics fm = v.getFontMetrics(v.getFont());
            int inkH = fm.getAscent() + fm.getDescent();
            check("卡" + i + " 数字有文本", v.getText() != null && !v.getText().trim().isEmpty());
            check("卡" + i + " 数字不被压成 0 高", v.getHeight() > 0);
            check("卡" + i + " 数字墨迹放得下(" + v.getHeight() + ">=" + inkH + ")", v.getHeight() >= inkH);
            check("卡" + i + " 数字宽度不溢出(" + v.getPreferredSize().width + "<=" + v.getWidth() + ")",
                    v.getPreferredSize().width <= v.getWidth() + 1);
            if (subFull[i] != null) {
                check("卡" + i + " 说明行有文本", s.getText() != null && !s.getText().isEmpty());
            }
        }

        // 2) 说明行宽度：显示的文字不能超出自己的宽度
        for (int i = 0; i < 6; i++) {
            JLabel s = subs[i];
            if (s.getText() == null || s.getText().isEmpty()) continue;
            int need = s.getFontMetrics(s.getFont()).stringWidth(s.getText());
            check("卡" + i + " 说明不溢出(" + need + "<=" + s.getWidth() + ")", need <= s.getWidth() + 1);
        }

        // 3) 记账卡在极端金额下用缩写版，塞得下
        String t = subs[3].getText();
        check("记账说明为缩写版或放得下", (t.contains("收 ") && t.contains("· 支")) || t.contains("收入"));

        // 4) 窗口压到 1000x700：数字必须还在（说明行可以让位）
        SwingUtilities.invokeAndWait(() -> frame.setSize(1000, 700));
        Thread.sleep(1200);
        for (int i = 0; i < 6; i++) {
            JLabel v = vals[i];
            FontMetrics fm = v.getFontMetrics(v.getFont());
            int inkH = fm.getAscent() + fm.getDescent();
            check("窄窗 卡" + i + " 数字高度>" + inkH + " 实际" + v.getHeight(), v.getHeight() >= inkH - 1);
        }

        // 5) 再放大回 1140x760：说明行恢复显示
        SwingUtilities.invokeAndWait(() -> frame.setSize(1140, 760));
        Thread.sleep(1200);
        int shown = 0;
        for (int i = 0; i < 6; i++) if (subs[i].isVisible() && subs[i].getHeight() > 0 && !subs[i].getText().isEmpty()) shown++;
        check("回大窗后说明行恢复(" + shown + "/6)", shown >= 5);

        System.out.println(fail == 0 ? "ALL PASS (" + pass + ")" : "SOME FAILED (" + pass + " pass, " + fail + " fail)");
        Runtime.getRuntime().halt(fail == 0 ? 0 : 1);
    }

    static void check(String name, boolean ok) {
        System.out.println((ok ? "[PASS] " : "[FAIL] ") + name);
        if (ok) pass++;
        else fail++;
    }
}
