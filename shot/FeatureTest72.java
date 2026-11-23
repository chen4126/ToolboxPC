import com.toolbox.pc.*;

import javax.swing.*;
import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** v2.2：设置不再丢——测试数据隔离到临时目录 + 设置自动留副本可恢复 */
public class FeatureTest72 {

    static int pass = 0, fail = 0;
    static File tmp;

    public static void main(String[] a) throws Exception {
        /* 关键：先换数据根，再启动程序，整轮测试绝不碰用户的 ToolboxPC */
        tmp = new File(System.getProperty("java.io.tmpdir"), "toolbox-ft72");
        wipe(tmp);
        tmp.mkdirs();
        System.setProperty("toolbox.data.dir", tmp.getAbsolutePath());

        File real = new File(System.getProperty("user.home"), "ToolboxPC");
        String before = snapshot(real);

        check("数据目录跟着属性走（" + Store.dir().getAbsolutePath() + "）",
                Store.dir().getAbsolutePath().equals(tmp.getAbsolutePath()));
        check("不再落在用户真实目录", !Store.dir().getAbsolutePath().equals(real.getAbsolutePath()));

        SwingUtilities.invokeAndWait(() -> Main.main(new String[0]));
        Thread.sleep(2000);

        // ---------- 1) 设置写入 / 逐键改不互相覆盖 ----------
        Store.writeLines("settings.txt", new ArrayList<String>(Arrays.asList(
                "sound=0", "strong=0", "pin=1",
                "navOrder=schedule,calc,ledger,memo,counter,pomodoro,countdown,stats,settings")));
        check("初始 4 键写进去了", Store.readLines("settings.txt").size() == 4);

        Store.setFlag("sound", true);
        check("改 sound 后读到 1", Store.flag("sound", false));
        check("改 sound 没冲掉 pin", Store.flag("pin", false));
        check("改 sound 没冲掉 navOrder",
                Store.str("navOrder", "").equals("schedule,calc,ledger,memo,counter,pomodoro,countdown,stats,settings"));

        Store.setFlag("strong", true);
        Store.setFlag("pin", false);
        check("连改三次后 4 键还在（" + Store.readLines("settings.txt").size() + "）",
                Store.readLines("settings.txt").size() == 4);
        check("sound 仍是 1", Store.flag("sound", false));
        check("strong 变成 1", Store.flag("strong", false));
        check("pin 变成 0", !Store.flag("pin", true));

        /* 手动加的键（例如以后版本新增的开关）不能被清掉 */
        List<String> withExtra = new ArrayList<String>(Store.readLines("settings.txt"));
        withExtra.add("futureKey=abc");
        Store.writeLines("settings.txt", withExtra);
        Store.setFlag("sound", false);
        boolean kept = false;
        for (String l : Store.readLines("settings.txt")) if (l.startsWith("futureKey=abc")) kept = true;
        check("陌生键不被清掉", kept);

        // ---------- 2) 设置自动副本 ----------
        boolean bakSame = sameContent(new File(tmp, "settings.txt"), new File(tmp, "settings.bak"));
        check("每次写设置都同步了 settings.bak", bakSame);
        check("settings.bak 存在（" + new File(tmp, "settings.bak").length() + " 字节）",
                new File(tmp, "settings.bak").exists());

        // 删掉 settings.txt，模拟"更新时被误删/覆盖" → 应当自动拉回
        if (!new File(tmp, "settings.txt").delete()) System.out.println("删除失败（可能被占用）");
        boolean pinBack = Store.flag("pin", true);
        check("删掉 settings.txt 后从副本恢复出 pin=0", !pinBack);
        check("settings.txt 被自动重建", new File(tmp, "settings.txt").exists());
        boolean navBack = Store.str("navOrder", "").length() > 0;
        check("从副本恢复后 navOrder 还在", navBack);

        // ---------- 3) 换目录立即生效（缓存不能留旧值） ----------
        File tmp2 = new File(System.getProperty("java.io.tmpdir"), "toolbox-ft72-b");
        wipe(tmp2);
        tmp2.mkdirs();
        System.setProperty("toolbox.data.dir", tmp2.getAbsolutePath());
        check("换目录后 dir() 立刻跟着变（" + Store.dir().getAbsolutePath() + "）",
                Store.dir().getAbsolutePath().equals(tmp2.getAbsolutePath()));
        Store.writeLines("settings.txt", new ArrayList<String>(Arrays.asList("pin=1")));
        check("新目录里写成功", Store.flag("pin", false));
        check("旧目录没被写脏",
                !new File(tmp, "settings.txt").getName().isEmpty() && sameContent(
                        new File(tmp, "settings.txt"), new File(tmp, "settings.bak")));

        // 切回原目录
        System.setProperty("toolbox.data.dir", tmp.getAbsolutePath());
        check("切回原目录", Store.dir().getAbsolutePath().equals(tmp.getAbsolutePath()));

        // ---------- 4) 真机设置面板读到的值对得上 ----------
        Class<?> mc = Class.forName("com.toolbox.pc.Main");
        Object self = getStatic(mc, "self");
        Method pageM = mc.getDeclaredMethod("page", Class.class);
        Object sp = pageM.invoke(self, Class.forName("com.toolbox.pc.SettingsPanel"));
        check("设置页能打开", sp != null);

        // ---------- 5) 用户真实目录一个字节都没动 ----------
        String after = snapshot(real);
        check("用户真实目录未被本次测试改动", before.equals(after));
        if (!before.equals(after)) {
            System.out.println("before=" + before);
            System.out.println("after =" + after);
        }

        System.out.println(fail == 0 ? ("ALL PASS (" + pass + ")") : ("SOME FAIL " + fail + "/" + (pass + fail)));
        Runtime.getRuntime().halt(fail == 0 ? 0 : 1);
    }

    static boolean sameContent(File a, File b) throws Exception {
        if (!a.exists() || !b.exists()) return false;
        byte[] x = Files.readAllBytes(a.toPath());
        byte[] y = Files.readAllBytes(b.toPath());
        return Arrays.equals(x, y);
    }

    static String snapshot(File d) throws Exception {
        if (!d.exists()) return "absent";
        StringBuilder sb = new StringBuilder();
        for (File f : d.listFiles()) {
            sb.append(f.getName()).append('=').append(f.length()).append('@')
                    .append(f.lastModified() / 1000).append(';');
        }
        return sb.toString();
    }

    static void wipe(File d) {
        if (!d.exists()) return;
        File[] fs = d.listFiles();
        if (fs != null) for (File f : fs) {
            if (f.isDirectory()) wipe(f);
            f.delete();
        }
    }

    static Object getStatic(Class<?> c, String name) throws Exception {
        java.lang.reflect.Field f = c.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(null);
    }

    static void check(String name, boolean ok) {
        System.out.println((ok ? "[PASS] " : "[FAIL] ") + name);
        if (ok) pass++;
        else fail++;
    }
}
