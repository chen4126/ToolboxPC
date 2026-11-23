import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** v1.25 三项验证：概览点条跳记账本定位 / 备忘时间列高亮 / 倒计时超时分显示 */
public class FeatureTest5 {

    private static boolean ok = true;

    public static void main(String[] args) throws Exception {
        try {
            Theme.init();
            SwingUtilities.invokeAndWait(() -> Main.main(new String[0]));
            Thread.sleep(2500);

            /* ---------- 1. 概览“最近记账”点单条 → 记账本定位 ---------- */
            OverviewPanel ov = page(OverviewPanel.class);
            Method rf = OverviewPanel.class.getDeclaredMethod("refresh");
            rf.setAccessible(true);
            SwingUtilities.invokeAndWait(() -> {
                try {
                    rf.invoke(ov);
                } catch (Exception ignored) {
                }
            });
            Thread.sleep(500);
            Field rbF = OverviewPanel.class.getDeclaredField("recentBox");
            rbF.setAccessible(true);
            JPanel rb = (JPanel) rbF.get(ov);
            int total = com.toolbox.pc.Store.readLines("ledger.txt").size();
            Component row0 = rb.getComponent(0);
            System.out.println("最近记账行数=" + rb.getComponentCount() + " 总条数=" + total);
            SwingUtilities.invokeAndWait(() -> row0.dispatchEvent(new MouseEvent(
                    row0, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0,
                    5, 5, 1, false, MouseEvent.BUTTON1)));
            Thread.sleep(1500);

            Field ckF = Main.class.getDeclaredField("currentKey");
            ckF.setAccessible(true);
            Object mainObj = null;
            Field selfF = Main.class.getDeclaredField("self");
            selfF.setAccessible(true);
            mainObj = selfF.get(null);
            String curKey = (String) ckF.get(mainObj);

            LedgerPanel lp = page(LedgerPanel.class);
            Field tf = LedgerPanel.class.getDeclaredField("table");
            tf.setAccessible(true);
            JTable table = (JTable) tf.get(lp);
            Field r2i = LedgerPanel.class.getDeclaredField("rowToIndex");
            r2i.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<Integer> map = (List<Integer>) r2i.get(lp);
            int sel = table.getSelectedRow();
            int lineIdx = sel >= 0 && sel < map.size() ? map.get(sel) : -999;
            System.out.println("当前页=" + curKey + " 选中行=" + sel + " 对应ledger行号=" + lineIdx);
            check("点概览某条跳到记账本并选中该行", "ledger".equals(curKey) && sel == 0
                    && lineIdx == total - 1);

            /* ---------- 2. 备忘搜索：时间列也高亮 ---------- */
            MemoPanel mp = page(MemoPanel.class);
            Method hl = MemoPanel.class.getDeclaredMethod("highlight", String.class, Color.class);
            hl.setAccessible(true);
            Field qF = MemoPanel.class.getDeclaredField("query");
            qF.setAccessible(true);
            qF.set(mp, "11-05");
            Method reload = MemoPanel.class.getDeclaredMethod("reloadList");
            reload.setAccessible(true);
            SwingUtilities.invokeAndWait(() -> {
                try {
                    reload.invoke(mp);
                } catch (Exception ignored) {
                }
            });
            Field listF = MemoPanel.class.getDeclaredField("list");
            listF.setAccessible(true);
            JList rawList = (JList) listF.get(mp);
            System.out.println("搜时间命中条数=" + rawList.getModel().getSize());
            Component cell = rawList.getCellRenderer().getListCellRendererComponent(
                    rawList, rawList.getModel().getElementAt(0), 0, false, false);
            List<JLabel> labels = new ArrayList<JLabel>();
            collect(cell, labels);
            JLabel timeLbl = null, textLbl = null;
            for (JLabel l : labels) {
                String s = l.getText() == null ? "" : l.getText();
                if (s.contains("11-05")) timeLbl = l;
                else if (s.contains("预算")) textLbl = l;
            }
            System.out.println("时间标签=" + (timeLbl == null ? "无" : timeLbl.getText()));
            System.out.println("正文标签=" + (textLbl == null ? "无" : textLbl.getText()));
            check("搜索命中时间列也标黄", timeLbl != null
                    && timeLbl.getText().contains("background:#FEF3C7"));
            check("未命中的正文不套 HTML", textLbl != null
                    && !textLbl.getText().contains("<html"));
            qF.set(mp, "");
            SwingUtilities.invokeAndWait(() -> {
                try {
                    reload.invoke(mp);
                } catch (Exception ignored) {
                }
            });

            /* ---------- 3. 倒计时超时满 1 分后追加“已超时 N 分” ---------- */
            CountDownPanel cd = page(CountDownPanel.class);
            Field overF = CountDownPanel.class.getDeclaredField("over");
            overF.setAccessible(true);
            Field omF = CountDownPanel.class.getDeclaredField("overMs");
            omF.setAccessible(true);
            Field stF = CountDownPanel.class.getDeclaredField("statusLabel");
            stF.setAccessible(true);
            Method ut = CountDownPanel.class.getDeclaredMethod("updateTime");
            ut.setAccessible(true);
            overF.set(cd, true);
            omF.set(cd, 30000L);
            SwingUtilities.invokeAndWait(() -> {
                try {
                    ut.invoke(cd);
                } catch (Exception ignored) {
                }
            });
            JLabel st = (JLabel) stF.get(cd);
            System.out.println("30 秒超时状态=" + st.getText());
            check("不足 1 分不显示分钟", !st.getText().contains("已超时"));

            omF.set(cd, 65000L);
            SwingUtilities.invokeAndWait(() -> {
                try {
                    ut.invoke(cd);
                } catch (Exception ignored) {
                }
            });
            System.out.println("65 秒超时状态=" + st.getText());
            check("满 1 分追加分钟数", st.getText().contains("已超时 1 分"));
            overF.set(cd, false);
            omF.set(cd, 0L);

            System.out.println(ok ? "ALL PASS" : "HAS FAIL");
            System.exit(ok ? 0 : 1);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(2);
        }
    }

    private static void check(String name, boolean pass) {
        System.out.println((pass ? "[PASS] " : "[FAIL] ") + name);
        if (!pass) ok = false;
    }

    private static void collect(Component c, List<JLabel> out) {
        if (c instanceof JLabel) out.add((JLabel) c);
        if (c instanceof Container) {
            for (Component k : ((Container) c).getComponents()) collect(k, out);
        }
    }

    private static <T extends Component> T page(Class<T> type) {
        for (Frame f : Frame.getFrames()) {
            T t = scan(f, type);
            if (t != null) return t;
        }
        return null;
    }

    private static <T extends Component> T scan(Component c, Class<T> type) {
        if (type.isInstance(c)) return type.cast(c);
        if (c instanceof Container) {
            for (Component k : ((Container) c).getComponents()) {
                T t = scan(k, type);
                if (t != null) return t;
            }
        }
        return null;
    }
}
