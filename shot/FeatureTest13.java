import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;

/** v1.33 三项验证：过期分三档 / 新建带上下文 / 一键挪到今天 */
public class FeatureTest13 {

    private static boolean ok = true;

    public static void main(String[] args) throws Exception {
        try {
            Theme.init();
            SwingUtilities.invokeAndWait(() -> Main.main(new String[0]));
            Thread.sleep(2500);

            Class<?> mc = Class.forName("com.toolbox.pc.Main");
            Field selfF = mc.getDeclaredField("self");
            selfF.setAccessible(true);
            Field ff = mc.getDeclaredField("frame");
            ff.setAccessible(true);
            JFrame frame = (JFrame) ff.get(selfF.get(null));

            OverviewPanel ov = page(OverviewPanel.class);
            Method refresh = OverviewPanel.class.getDeclaredMethod("refresh");
            refresh.setAccessible(true);
            SwingUtilities.invokeAndWait(() -> {
                try {
                    refresh.invoke(ov);
                } catch (Exception ignored) {
                }
            });
            Thread.sleep(500);

            /* ---------- 1. 三档分级 ---------- */
            Field fbF = OverviewPanel.class.getDeclaredField("futureBar");
            fbF.setAccessible(true);
            JPanel bar = (JPanel) fbF.get(ov);
            StringBuilder sb = new StringBuilder();
            Color cAncient = null, cDeep = null, cNear = null;
            for (Component c : bar.getComponents()) {
                if (!(c instanceof JLabel)) continue;
                JLabel l = (JLabel) c;
                sb.append(l.getText()).append('|');
                if (l.getText().equals("很久没动")) cAncient = l.getForeground();
                if (l.getText().equals("该处理了")) cDeep = l.getForeground();
                if (l.getText().equals("过期未做")) cNear = l.getForeground();
            }
            System.out.println("标签=" + sb);
            check("7 天以上单独一档排最前", sb.toString().startsWith("很久没动|11-01 · 1 条|")
                    && cAncient != null && cAncient.getRGB() == new Color(0x7F1D1D).getRGB());
            check("3-6 天是中间档深红", sb.toString().contains("该处理了|11-06 · 1 条|")
                    && cDeep != null && cDeep.getRGB() == new Color(0x991B1B).getRGB());
            check("1-2 天仍是普通红", sb.toString().contains("过期未做|11-08 · 1 条|")
                    && cNear != null && cNear.getRGB() == new Color(0xDC2626).getRGB());
            check("末尾有一键挪到今天的入口", sb.toString().contains("全部挪到今天"));

            /* ---------- 2. 小窗红字也分档 ---------- */
            SwingUtilities.invokeAndWait(() -> MiniBoard.toggle());
            Thread.sleep(800);
            Field instF = MiniBoard.class.getDeclaredField("inst");
            instF.setAccessible(true);
            Object mb = instF.get(null);
            Field odF = MiniBoard.class.getDeclaredField("overdueLbl");
            odF.setAccessible(true);
            JLabel od = (JLabel) odF.get(mb);
            System.out.println("小窗红字=" + od.getText() + " 色=" + Integer.toHexString(od.getForeground().getRGB()));
            check("有 7 天以上的先说很久没动", od.isVisible()
                    && od.getText().equals("很久没动 1 条 · 最早 11-01")
                    && od.getForeground().getRGB() == new Color(0x7F1D1D).getRGB());

            MouseEvent ev = new MouseEvent(od, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
                    0, 1, 1, 1, false);
            for (java.awt.event.MouseListener ml : od.getMouseListeners()) ml.mouseClicked(ev);
            Thread.sleep(400);
            Field bdF = MiniBoard.class.getDeclaredField("boardDay");
            bdF.setAccessible(true);
            Field lbF = MiniBoard.class.getDeclaredField("listBox");
            lbF.setAccessible(true);
            JPanel listBox = (JPanel) lbF.get(mb);
            String texts = join(listBox);
            System.out.println("点红字后 boardDay=" + bdF.getInt(mb) + " 内容=" + texts);
            check("点红字翻到最早那天的", bdF.getInt(mb) == -9 && texts.contains("11-01")
                    && !texts.contains("今天的事"));

            bdF.setInt(mb, 0);
            Method mref = MiniBoard.class.getDeclaredMethod("refresh");
            mref.setAccessible(true);
            SwingUtilities.invokeAndWait(() -> {
                try {
                    mref.invoke(mb);
                } catch (Exception ignored) {
                }
            });
            Thread.sleep(300);

            /* ---------- 3. 新建待办带上下文 ---------- */
            Method ctx = OverviewPanel.class.getDeclaredMethod("newTodoContext", LocalDate.class, String.class);
            ctx.setAccessible(true);
            String c1 = (String) ctx.invoke(ov, LocalDate.of(2026, 11, 11), "2026-11-11");
            String c2 = (String) ctx.invoke(ov, LocalDate.of(2026, 11, 12), "2026-11-12");
            System.out.println("已有那天=" + c1);
            System.out.println("空白那天=" + c2);
            check("新建时说清这天已有几条", c1.startsWith("这天已有 1 条") && c1.contains("明天的事"));
            check("空白那天给的是鼓励语", c2.contains("还没有待办"));

            /* ---------- 4. 一键挪到今天 ---------- */
            Method move = OverviewPanel.class.getDeclaredMethod("moveOverdueToToday");
            move.setAccessible(true);
            int moved = (Integer) move.invoke(ov);
            System.out.println("挪动条数=" + moved);
            check("把三条过期都挪到今天", moved == 3);

            SwingUtilities.invokeAndWait(() -> {
                try {
                    refresh.invoke(ov);
                } catch (Exception ignored) {
                }
            });
            Thread.sleep(500);
            StringBuilder sb2 = new StringBuilder();
            for (Component c : bar.getComponents()) {
                if (c instanceof JLabel) sb2.append(((JLabel) c).getText()).append('|');
            }
            System.out.println("挪完标签=" + sb2);
            check("挪完就不再有过期提醒", !sb2.toString().contains("很久没动")
                    && !sb2.toString().contains("该处理了") && !sb2.toString().contains("过期未做")
                    && !sb2.toString().contains("全部挪到今天"));

            Field todosF = OverviewPanel.class.getDeclaredField("todos");
            todosF.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.List<Object> todos = (java.util.List<Object>) todosF.get(ov);
            Field dateF = todos.get(0).getClass().getDeclaredField("date");
            dateF.setAccessible(true);
            int todayCnt = 0;
            for (Object t : todos) {
                if ("2026-11-10".equals(String.valueOf(dateF.get(t)))) todayCnt++;
            }
            System.out.println("今天条数=" + todayCnt);
            check("四条（含不限日期两条）落在今天视图", todayCnt >= 3);

            /* 小窗红字也应消失 */
            SwingUtilities.invokeAndWait(() -> {
                try {
                    mref.invoke(mb);
                } catch (Exception ignored) {
                }
            });
            Thread.sleep(300);
            System.out.println("挪完小窗红字可见=" + od.isVisible());
            check("挪完小窗红字自动收起", !od.isVisible());

            System.out.println(ok ? "ALL PASS" : "HAS FAIL");
            System.exit(ok ? 0 : 1);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(2);
        }
    }

    private static String join(JPanel box) {
        StringBuilder sb = new StringBuilder();
        for (Component c : box.getComponents()) {
            if (c instanceof JLabel) sb.append(((JLabel) c).getText()).append('|');
            else if (c instanceof JPanel) {
                for (Component k : ((JPanel) c).getComponents()) {
                    if (k instanceof JLabel) sb.append(((JLabel) k).getText()).append('|');
                }
            }
        }
        return sb.toString();
    }

    private static void check(String name, boolean pass) {
        System.out.println((pass ? "[PASS] " : "[FAIL] ") + name);
        if (!pass) ok = false;
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
