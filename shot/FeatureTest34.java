import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** v1.54 验证：另有 N 条可点、本月完成数、归档弹窗键盘流 */
public class FeatureTest34 {

    private static boolean ok = true;

    public static void main(String[] args) throws Exception {
        try {
            SwingUtilities.invokeAndWait(() -> Main.main(new String[0]));
            Thread.sleep(2500);

            Class<?> mc = Class.forName("com.toolbox.pc.Main");
            Field selfF = mc.getDeclaredField("self");
            selfF.setAccessible(true);
            Object main = selfF.get(null);
            Field ff = mc.getDeclaredField("frame");
            ff.setAccessible(true);
            JFrame frame = (JFrame) ff.get(main);
            Method sel = mc.getDeclaredMethod("selectNav", String.class);
            sel.setAccessible(true);
            SwingUtilities.invokeAndWait(() -> {
                try {
                    sel.invoke(main, "overview");
                } catch (Exception e) {
                }
            });
            Thread.sleep(1400);

            OverviewPanel ov = find(frame.getContentPane(), OverviewPanel.class);
            Method refresh = OverviewPanel.class.getDeclaredMethod("refresh");
            refresh.setAccessible(true);
            refresh.invoke(ov);
            Thread.sleep(600);

            /* A. "另有 N 条" 可点：手型 + tooltip 说明 + 点一下切到那天 */
            Field fbF = OverviewPanel.class.getDeclaredField("futureBar");
            fbF.setAccessible(true);
            JPanel futureBar = (JPanel) fbF.get(ov);
            JLabel rest = null;
            for (Component c : futureBar.getComponents()) {
                if (c instanceof JLabel && ((JLabel) c).getText().startsWith("另有 ")) {
                    rest = (JLabel) c;
                    break;
                }
            }
            check("找到「另有 N 条」标签", rest != null, rest == null ? "none" : rest.getText());
            check("另有条是手型光标", rest != null
                    && rest.getCursor().getType() == java.awt.Cursor.HAND_CURSOR, "cursor");
            check("tooltip 说明点一下只看那天", rest != null && rest.getToolTipText() != null
                    && rest.getToolTipText().contains("点一下只看"), rest == null ? "none" : rest.getToolTipText());

            final JLabel restFinal = rest;
            SwingUtilities.invokeAndWait(() -> {
                MouseEvent e = new MouseEvent(restFinal, MouseEvent.MOUSE_CLICKED, 0,
                        MouseEvent.BUTTON1_DOWN_MASK, 1, 1, 1, false, MouseEvent.BUTTON1);
                restFinal.dispatchEvent(e);
            });
            Thread.sleep(500);
            Field ofF = OverviewPanel.class.getDeclaredField("overdueFilter");
            ofF.setAccessible(true);
            Field vdF = OverviewPanel.class.getDeclaredField("viewDate");
            vdF.setAccessible(true);
            Field lvF = OverviewPanel.class.getDeclaredField("overdueLevel");
            lvF.setAccessible(true);
            Object filt = ofF.get(ov);
            check("点「另有」后切到那一天", filt != null && filt.toString().equals("2026-11-02"),
                    "filter=" + filt);
            check("切过去时退出了按天视图", vdF.get(ov) == null, "viewDate=" + vdF.get(ov));
            check("切过去时档位归零", (Integer) lvF.get(ov) == 0, "level=" + lvF.get(ov));

            Method refresh2 = OverviewPanel.class.getDeclaredMethod("refresh");
            refresh2.setAccessible(true);
            refresh2.invoke(ov);
            Thread.sleep(400);

            /* B. 本月完成数：本周 2、上周 1、本月 3（11-04 / 11-09 / 11-11 都在 11 月） */
            Method dmM = OverviewPanel.class.getDeclaredMethod("doneInMonth");
            dmM.setAccessible(true);
            int month = (Integer) dmM.invoke(ov);
            check("本月完成数 = 3", month == 3, "本月=" + month);
            Field wdF = OverviewPanel.class.getDeclaredField("weekDone");
            wdF.setAccessible(true);
            JLabel weekDone = (JLabel) wdF.get(ov);
            check("本周完成标签带本月", weekDone.getText().contains("本月3"), weekDone.getText());
            check("本周完成 tooltip 说这个月", weekDone.getToolTipText() != null
                    && weekDone.getToolTipText().contains("这个月"), weekDone.getToolTipText());
            check("本周完成仍可点跳统计页", weekDone.getCursor().getType() == java.awt.Cursor.HAND_CURSOR,
                    "cursor");

            /* C. 归档弹窗键盘流：↑↓ 选行、回车跳那天、Esc 清搜索/关窗 */
            Method apM = OverviewPanel.class.getDeclaredMethod("archivePanel", JDialog.class, int[].class);
            apM.setAccessible(true);
            JPanel panel = (JPanel) apM.invoke(ov, null, new int[]{-1});
            panel.setSize(680, 400);
            layoutAll(panel);
            Field asF = OverviewPanel.class.getDeclaredField("archSel");
            asF.setAccessible(true);
            Field arnF = OverviewPanel.class.getDeclaredField("archRowNames");
            arnF.setAccessible(true);
            List names = (List) arnF.get(ov);
            check("归档面板记录了每行 label", names.size() == 2, "rows=" + names.size());
            check("初始没有选中行", (Integer) asF.get(ov) == -1, "sel=" + asF.get(ov));

            Method mvM = OverviewPanel.class.getDeclaredMethod("moveArchSel", int.class);
            mvM.setAccessible(true);
            mvM.invoke(ov, 1);
            check("第一次按 ↓ 落在第一行", (Integer) asF.get(ov) == 0, "sel=" + asF.get(ov));
            JLabel first = (JLabel) names.get(0);
            check("选中行有浅蓝底做标记", first.isOpaque(), "opaque=" + first.isOpaque());
            mvM.invoke(ov, 1);
            check("再按 ↓ 落到第二行", (Integer) asF.get(ov) == 1, "sel=" + asF.get(ov));
            check("上一行的高亮收了", !((JLabel) names.get(0)).isOpaque(), "opaque");
            mvM.invoke(ov, 1);
            check("到底了不会越界", (Integer) asF.get(ov) == 1, "sel=" + asF.get(ov));
            mvM.invoke(ov, -1);
            mvM.invoke(ov, -1);
            check("到顶了不会越界", (Integer) asF.get(ov) == 0, "sel=" + asF.get(ov));

            Method oaM = OverviewPanel.class.getDeclaredMethod("openArchSel");
            oaM.setAccessible(true);
            int opened = (Integer) oaM.invoke(ov);
            Thread.sleep(500);
            check("回车打开选中那条", opened == 1, "ret=" + opened);
            check("回车后主窗跳到那条的日期", vdF.get(ov) != null
                            && vdF.get(ov).toString().startsWith("2026-10-"),
                    "viewDate=" + vdF.get(ov));
            check("跳完把选中态清掉", (Integer) asF.get(ov) == -1, "sel=" + asF.get(ov));

            System.out.println(ok ? "ALL PASS" : "SOME FAIL");
        } catch (Throwable t) {
            t.printStackTrace();
            ok = false;
        }
        Runtime.getRuntime().halt(ok ? 0 : 1);
    }

    static void layoutAll(Component c) {
        c.doLayout();
        if (c instanceof Container) for (Component k : ((Container) c).getComponents()) layoutAll(k);
    }

    static void check(String name, boolean pass, String detail) {
        if (!pass) ok = false;
        System.out.println((pass ? "[PASS] " : "[FAIL] ") + name + " → " + detail);
    }

    static <T extends Component> T find(Component c, Class<T> type) {
        if (type.isInstance(c)) return type.cast(c);
        if (c instanceof Container) {
            for (Component k : ((Container) c).getComponents()) {
                T t = find(k, type);
                if (t != null) return t;
            }
        }
        return null;
    }
}
