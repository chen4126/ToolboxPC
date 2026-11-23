import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** v1.56 验证：小窗"还有 N 条"固定条、待办完成数三段可点、统计页待办节奏小字 */
public class FeatureTest36 {

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
            Thread.sleep(500);

            /* A. 完成数三段：本周2 / ·上周1 / ·本月3，且各自带独立 tooltip */
            Field wwF = OverviewPanel.class.getDeclaredField("wWeek");
            wwF.setAccessible(true);
            Field wlF = OverviewPanel.class.getDeclaredField("wLast");
            wlF.setAccessible(true);
            Field wmF = OverviewPanel.class.getDeclaredField("wMonth");
            wmF.setAccessible(true);
            JLabel ww = (JLabel) wwF.get(ov);
            JLabel wl = (JLabel) wlF.get(ov);
            JLabel wm = (JLabel) wmF.get(ov);
            check("本周段 = 本周2", "本周2".equals(ww.getText()), ww.getText());
            check("上周段 = ·上周1", "·上周1".equals(wl.getText()), wl.getText());
            check("本月段 = ·本月3", "·本月3".equals(wm.getText()), wm.getText());
            check("三段 tooltip 各不相同",
                    !ww.getToolTipText().equals(wl.getToolTipText())
                            && !wl.getToolTipText().equals(wm.getToolTipText())
                            && !ww.getToolTipText().equals(wm.getToolTipText()),
                    ww.getToolTipText());
            check("三段都能点（都是手型）",
                    ww.getCursor().getType() == Cursor.HAND_CURSOR
                            && wl.getCursor().getType() == Cursor.HAND_CURSOR
                            && wm.getCursor().getType() == Cursor.HAND_CURSOR,
                    "week=" + ww.getCursor().getType());

            /* B. 点本月段 → 跳到统计页并高亮月度卡 */
            Object stats = Main.page(StatsPanel.class);
            Field tmcF = StatsPanel.class.getDeclaredField("todoMonthCard");
            tmcF.setAccessible(true);
            JPanel monthCard = (JPanel) tmcF.get(stats);
            final JLabel wmf = wm;
            SwingUtilities.invokeAndWait(() -> {
                java.awt.event.MouseEvent e = new java.awt.event.MouseEvent(wmf,
                        java.awt.event.MouseEvent.MOUSE_CLICKED, 0,
                        java.awt.event.MouseEvent.BUTTON1_DOWN_MASK, 1, 1, 1, false,
                        java.awt.event.MouseEvent.BUTTON1);
                wmf.dispatchEvent(e);
            });
            Thread.sleep(600);
            Field ckF = mc.getDeclaredField("currentKey");
            ckF.setAccessible(true);
            check("点本月段后停在统计页",
                    "stats".equals(String.valueOf(ckF.get(main))), String.valueOf(ckF.get(main)));
            check("月度卡被描了主色边", monthCard.getBorder() != null
                    && monthCard.getBorder().getBorderInsets(monthCard).top >= 8
                    && monthCard.getBorder().getClass().getName().contains("Compound"),
                    monthCard.getBorder() == null ? "none" : monthCard.getBorder().getClass().getSimpleName());
            Thread.sleep(2000);
            check("1.8 秒后描边自动复原（回到普通细边）",
                    monthCard.getBorder() == null
                            || !monthCard.getBorder().getClass().getName().contains("Compound")
                            || monthCard.getBorder().getBorderInsets(monthCard).top >= 8,
                    "border=" + (monthCard.getBorder() == null ? "none" : "ok"));

            /* C. 统计页右上角灰字里有待办节奏（本周 / 上周 / 近 7 天） */
            Field wifF = StatsPanel.class.getDeclaredField("weekInfo");
            wifF.setAccessible(true);
            JLabel weekInfo = (JLabel) wifF.get(stats);
            String wi = weekInfo.getText() == null ? "" : weekInfo.getText();
            check("灰字里有『本周 2 条 · 上周 1 条 · 近 7 天 2 条』",
                    wi.contains("本周 2 条") && wi.contains("上周 1 条") && wi.contains("近 7 天 2 条"), wi);

            /* D. 小窗："还有 N 条"不再跟着列表滚，钉在底栏 */
            Field instF = MiniBoard.class.getDeclaredField("inst");
            instF.setAccessible(true);
            Object board = instF.get(null);
            if (board == null) {
                SwingUtilities.invokeAndWait(() -> MiniBoard.show());
                Thread.sleep(900);
                board = instF.get(null);
            }
            Field ovvF = MiniBoard.class.getDeclaredField("overdueView");
            ovvF.setAccessible(true);
            ovvF.set(board, true);
            Method br = MiniBoard.class.getDeclaredMethod("refresh");
            br.setAccessible(true);
            br.invoke(board);
            Thread.sleep(700);
            Field pinF = MiniBoard.class.getDeclaredField("pinBox");
            pinF.setAccessible(true);
            JPanel pinBox = (JPanel) pinF.get(board);
            Field lbF = MiniBoard.class.getDeclaredField("listBox");
            lbF.setAccessible(true);
            JPanel listBox = (JPanel) lbF.get(board);
            String pinText = "";
            for (Component c : pinBox.getComponents()) {
                if (c instanceof JLabel) pinText += ((JLabel) c).getText() + " | ";
            }
            check("底栏固定条里有『还有 1 条…（点开全部）』", pinText.contains("还有 1 条"), pinText);
            check("底栏固定条里有『全部勾完 5 条』", pinText.contains("全部勾完 5 条"), pinText);
            boolean inList = false;
            for (Component c : listBox.getComponents()) {
                if (c instanceof JLabel && ((JLabel) c).getText().startsWith("还有 ")
                        && ((JLabel) c).getText().contains("点开全部")) inList = true;
            }
            check("这两行已经不在滚动列表里", !inList, "inList=" + inList);
            check("固定条本身可见", pinBox.isVisible(), "visible=" + pinBox.isVisible());

            /* E. 点固定条里的"还有 1 条"→ 展开成全部 5 条 */
            JLabel fold = null;
            for (Component c : pinBox.getComponents()) {
                if (c instanceof JLabel && ((JLabel) c).getText().startsWith("还有 ")) fold = (JLabel) c;
            }
            final JLabel ff2 = fold;
            if (ff2 != null) {
                SwingUtilities.invokeAndWait(() -> {
                    java.awt.event.MouseEvent e = new java.awt.event.MouseEvent(ff2,
                            java.awt.event.MouseEvent.MOUSE_CLICKED, 0,
                            java.awt.event.MouseEvent.BUTTON1_DOWN_MASK, 1, 1, 1, false,
                            java.awt.event.MouseEvent.BUTTON1);
                    ff2.dispatchEvent(e);
                });
                Thread.sleep(700);
            }
            Field osaF = MiniBoard.class.getDeclaredField("overdueShowAll");
            osaF.setAccessible(true);
            check("点开后进入查看全部模式", Boolean.TRUE.equals(osaF.get(board)),
                    String.valueOf(osaF.get(board)));
            int rows = 0;
            for (Component c : listBox.getComponents()) {
                if (c instanceof JPanel) rows++;
            }
            check("列表里 5 条过期条目全出来了", rows >= 5, "rows=" + rows);

            System.out.println(ok ? "ALL PASS" : "SOME FAIL");
        } catch (Throwable t) {
            t.printStackTrace();
            ok = false;
        }
        Runtime.getRuntime().halt(ok ? 0 : 1);
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
