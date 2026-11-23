import com.toolbox.pc.*;
import javax.swing.*;
import java.lang.reflect.Method;

/** 验证：启动自动显示小窗的开关行为 */
public class AutoTest {

    static Method ws, rs;

    static void set(String k, String v) throws Exception {
        ws.invoke(null, k, v);
    }

    static String get(String k) throws Exception {
        return (String) rs.invoke(null, k);
    }

    public static void main(String[] args) throws Exception {
        Theme.init();
        Ui.installDefaults();
        ws = MiniBoard.class.getDeclaredMethod("writeSetting", String.class, String.class);
        ws.setAccessible(true);
        rs = MiniBoard.class.getDeclaredMethod("readSetting", String.class);
        rs.setAccessible(true);

        System.out.println("[1] boardAuto=1 → 应自动显示");
        set("boardAuto", "1");
        SwingUtilities.invokeAndWait(() -> MiniBoard.restoreIfOn());
        Thread.sleep(600);
        System.out.println("    visible = " + MiniBoard.isVisibleNow());

        System.out.println("[2] 关掉小窗 → 应隐藏且 board=0");
        SwingUtilities.invokeAndWait(() -> {
            try {
                java.lang.reflect.Field fi = MiniBoard.class.getDeclaredField("inst");
                fi.setAccessible(true);
                Object inst = fi.get(null);
                Method hide = MiniBoard.class.getDeclaredMethod("hide");
                hide.setAccessible(true);
                hide.invoke(inst);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        Thread.sleep(400);
        System.out.println("    visible = " + MiniBoard.isVisibleNow() + ", board = " + get("board"));

        System.out.println("[3] boardAuto=0 但 board=1 → 不弹");
        set("boardAuto", "0");
        set("board", "1");
        SwingUtilities.invokeAndWait(() -> MiniBoard.restoreIfOn());
        Thread.sleep(600);
        System.out.println("    visible = " + MiniBoard.isVisibleNow());

        System.out.println("[4] boardAuto 缺失、board=1 → 沿用上次，弹");
        set("board", "1");
        java.util.List<String> lines = Store.readLines("settings.txt");
        lines.removeIf(l -> l.startsWith("boardAuto="));
        Store.writeLines("settings.txt", lines);
        SwingUtilities.invokeAndWait(() -> MiniBoard.restoreIfOn());
        Thread.sleep(600);
        System.out.println("    visible = " + MiniBoard.isVisibleNow());
        System.exit(0);
    }
}
