import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** v1.77：通知提示音改成"多选一"（五种程序合成音效 + 试听） */
public class FeatureTest63 {

    static int pass = 0, fail = 0;
    static Object main;

    public static void main(String[] a) throws Exception {
        try {
            run();
        } catch (Throwable t) {
            t.printStackTrace();
            fail++;
        }
        System.out.println(fail == 0 ? ("ALL PASS (" + pass + ")") : ("SOME FAIL " + fail + "/" + (pass + fail)));
        Runtime.getRuntime().halt(fail == 0 ? 0 : 1);
    }

    static void check(String name, boolean ok) {
        check(name, ok, "");
    }

    static void check(String name, boolean ok, String detail) {
        System.out.println((ok ? "[PASS] " : "[FAIL] ") + name + (ok || detail.isEmpty() ? "" : " → " + detail));
        if (ok) pass++;
        else fail++;
    }

    static void run() throws Exception {
        /* 先把 settings 里的 soundKind 清掉，验证"没配过就是默认音" */
        List<String> base = Store.readLines("settings.txt");
        List<String> clean = new ArrayList<String>();
        for (String l : base) {
            if (!l.startsWith("soundKind=")) clean.add(l);
        }
        Store.writeLines("settings.txt", clean);

        check("音效一共 5 种", Alerts.KINDS.length == 5, String.valueOf(Alerts.KINDS.length));
        check("每种都有中文名", Alerts.KIND_LABELS.length == 5, String.valueOf(Alerts.KIND_LABELS.length));
        check("没配过时默认是经典三声", "triple".equals(Alerts.kind()), Alerts.kind());
        check("名称映射正确（chime → 叮咚双音）", "叮咚双音".equals(Alerts.kindLabel("chime")),
                Alerts.kindLabel("chime"));
        check("名称映射兜底（乱写 → 经典三声）", "经典三声".equals(Alerts.kindLabel("zzz")),
                Alerts.kindLabel("zzz"));

        /* 保存与读回 */
        Alerts.setKind("chime");
        check("选了叮咚双音能读回来", "chime".equals(Alerts.kind()), Alerts.kind());
        check("音效写进了 settings.txt", hasSetting("soundKind=chime"), hasSetting("soundKind=chime") ? "" : "missing");
        Alerts.setKind("zzz");
        check("写进非法值时存的是默认音", hasSetting("soundKind=triple"), "");
        check("非法值读回来也是默认音", "triple".equals(Alerts.kind()), "");

        /* 波形生成：长度与头部 */
        Method mk = Alerts.class.getDeclaredMethod("makeToneWav", double.class, double.class, double.class);
        mk.setAccessible(true);
        byte[] wav = (byte[]) mk.invoke(null, 880.0, 0.45, 4.0);
        int expect = 44 + (int) (44100 * 0.45) * 2;
        check("单音波形长度正确（" + expect + "）[" + wav.length + "]", wav.length == expect + 0 || Math.abs(wav.length - expect) <= 2,
                wav.length + " vs " + expect);
        check("WAV 头是 RIFF", new String(wav, 0, 4).equals("RIFF"), new String(wav, 0, 4));
        check("WAV 类型是 WAVE", new String(wav, 8, 4).equals("WAVE"), new String(wav, 8, 4));

        /* ---- 设置页 UI ---- */
        SwingUtilities.invokeAndWait(() -> Main.main(new String[0]));
        Thread.sleep(2500);
        Class<?> mc = Class.forName("com.toolbox.pc.Main");
        Field selfF = mc.getDeclaredField("self");
        selfF.setAccessible(true);
        main = selfF.get(null);
        Method pageM = mc.getDeclaredMethod("page", Class.class);
        pageM.setAccessible(true);
        Object panel = pageM.invoke(main, Class.forName("com.toolbox.pc.SettingsPanel"));
        Method sel = mc.getDeclaredMethod("selectNav", String.class);
        sel.setAccessible(true);
        SwingUtilities.invokeAndWait(() -> {
            try {
                sel.invoke(main, "settings");
            } catch (Exception ignored) {
            }
        });
        Thread.sleep(900);

        JComboBox<?> kindBox = findCombo((Component) panel, "叮咚双音");
        check("设置页有音效下拉框", kindBox != null, kindBox == null ? "null" : "ok");
        check("下拉里是 5 种音效", kindBox != null && kindBox.getItemCount() == 5,
                kindBox == null ? "null" : String.valueOf(kindBox.getItemCount()));
        check("下拉默认选中经典三声", kindBox != null && "经典三声".equals(kindBox.getSelectedItem()),
                kindBox == null ? "null" : String.valueOf(kindBox.getSelectedItem()));

        JButton tryBtn = findButton((Component) panel, "试听");
        check("设置页有「试听」按钮", tryBtn != null);

        JCheckBox soundBox = findCheck((Component) panel, "通知音");
        check("设置页有通知音勾选框", soundBox != null);

        if (soundBox != null && kindBox != null) {
            boolean on = soundBox.isSelected();
            check("勾选框当前状态与设置一致（sound=" + (on ? 1 : 0) + "）",
                    hasSetting("sound=" + (on ? 1 : 0)), "");
            check("下拉可用状态跟着勾选框走", kindBox.isEnabled() == on, "on=" + on);
            check("试听按钮可用状态跟着勾选框走", tryBtn != null && tryBtn.isEnabled() == on, "on=" + on);

            /* 点一下翻转，验证联动与落盘 */
            SwingUtilities.invokeAndWait(soundBox::doClick);
            Thread.sleep(250);
            boolean now = soundBox.isSelected();
            check("点一下后勾选框翻转了", now != on, now + "");
            check("翻转后下拉可用状态跟着变", kindBox.isEnabled() == now, now + "");
            check("翻转后试听按钮可用状态跟着变", tryBtn != null && tryBtn.isEnabled() == now, now + "");
            check("翻转后写进了 settings", hasSetting("sound=" + (now ? 1 : 0)), "");

            /* 再点回来，恢复原状态 */
            SwingUtilities.invokeAndWait(soundBox::doClick);
            Thread.sleep(250);
            check("再点一次回到原状态", soundBox.isSelected() == on, "");
            check("回到原状态后下拉也还原", kindBox.isEnabled() == on, "");
        }

        /* 切换选项要落盘 */
        if (kindBox != null) {
            kindBox.setSelectedIndex(3);
            Thread.sleep(300);
            check("切到「上升三连」后设置值为 alarm", "alarm".equals(Alerts.kind()), Alerts.kind());
            check("切到上升三连写进了 settings", hasSetting("soundKind=alarm"), "");
            check("下拉显示的就是上升三连", "上升三连".equals(kindBox.getSelectedItem()),
                    String.valueOf(kindBox.getSelectedItem()));
        }

        /* 出图：设置页提醒区（含新音效行） */
        shot((JComponent) panel, "C:\\Users\\chen\\Desktop\\ui-261-sound-select.png", 760, 620);
    }

    static boolean hasSetting(String needle) {
        for (String l : Store.readLines("settings.txt")) {
            if (l.trim().equals(needle)) return true;
        }
        return false;
    }

    static JComboBox<?> findCombo(Component root, String item) {
        for (Component c : kids(root)) {
            if (c instanceof JComboBox) {
                JComboBox<?> b = (JComboBox<?>) c;
                for (int i = 0; i < b.getItemCount(); i++) {
                    if (item.equals(String.valueOf(b.getItemAt(i)))) return b;
                }
            }
        }
        for (Component c : kids(root)) {
            if (c instanceof JComboBox) continue;
            JComboBox<?> r = findCombo(c, item);
            if (r != null) return r;
        }
        return null;
    }

    static JButton findButton(Component root, String text) {
        for (Component c : kids(root)) {
            if (c instanceof JButton && text.equals(((JButton) c).getText())) return (JButton) c;
        }
        for (Component c : kids(root)) {
            if (c instanceof JButton) continue;
            JButton r = findButton(c, text);
            if (r != null) return r;
        }
        return null;
    }

    static JCheckBox findCheck(Component root, String text) {
        for (Component c : kids(root)) {
            if (c instanceof JCheckBox && ((JCheckBox) c).getText().startsWith(text)) return (JCheckBox) c;
        }
        for (Component c : kids(root)) {
            if (c instanceof JCheckBox) continue;
            JCheckBox r = findCheck(c, text);
            if (r != null) return r;
        }
        return null;
    }

    static Component[] kids(Component c) {
        if (c instanceof Container) return ((Container) c).getComponents();
        return new Component[0];
    }

    static void layoutAll(Component c) {
        c.doLayout();
        for (Component x : kids(c)) layoutAll(x);
    }

    static void shot(JComponent p, String path, int w, int h) throws Exception {
        p.setSize(w, h);
        layoutAll(p);
        p.setDoubleBuffered(false);
        java.awt.image.BufferedImage img =
                new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Theme.bg());
        g.fillRect(0, 0, w, h);
        p.paint(g);
        g.dispose();
        javax.imageio.ImageIO.write(img, "png", new File(path));
        System.out.println("SHOT " + new File(path).length());
    }
}
