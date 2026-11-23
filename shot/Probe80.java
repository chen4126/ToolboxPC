import com.toolbox.pc.*;

import javax.swing.*;
import java.awt.*;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.lang.reflect.Field;

/** 诊断：把窗口与侧栏坐标写进文件（避免 stdout 缓冲看不到） */
public class Probe80 {

    static PrintWriter out;

    public static void main(String[] a) throws Exception {
        out = new PrintWriter(new FileWriter("C:\\Users\\chen\\DevTools\\ToolboxPC\\shot\\probe80.txt"), true);
        line("start");
        SwingUtilities.invokeAndWait(() -> Main.main(new String[0]));
        Thread.sleep(3000);

        line("---- AWT 窗口 ----");
        try {
            for (Window w : Window.getWindows()) {
                String title = "";
                if (w instanceof Frame) title = ((Frame) w).getTitle();
                line(w.getClass().getSimpleName() + " vis=" + w.isVisible()
                        + " bounds=" + w.getBounds() + " title=" + title);
            }
        } catch (Throwable t) {
            line("窗口遍历异常 " + t);
        }

        Class<?> mc = Class.forName("com.toolbox.pc.Main");
        Field selfF = mc.getDeclaredField("self");
        selfF.setAccessible(true);
        Object main = selfF.get(null);
        Field ff = mc.getDeclaredField("frame");
        ff.setAccessible(true);
        JFrame frame = (JFrame) ff.get(main);
        line("frame bounds=" + frame.getBounds() + " insets=" + frame.getInsets());

        line("---- contentPane ----");
        dump(frame.getContentPane(), 0);

        Field nsF = mc.getDeclaredField("navScroll");
        nsF.setAccessible(true);
        JScrollPane ns = (JScrollPane) nsF.get(main);
        line("navScroll bounds=" + ns.getBounds() + " pref=" + ns.getPreferredSize());
        line("viewport bounds=" + ns.getViewport().getBounds()
                + " view bounds=" + ns.getViewport().getView().getBounds()
                + " view pref=" + ns.getViewport().getView().getPreferredSize());
        line("---- nav 子项 ----");
        dump(ns.getViewport().getView(), 1);

        line("done");
        out.close();
        Runtime.getRuntime().halt(0);
    }

    static void line(String s) {
        out.println(s);
    }

    static void dump(Component c, int depth) {
        StringBuilder pad = new StringBuilder();
        for (int i = 0; i < depth; i++) pad.append("  ");
        String extra = "";
        if (c instanceof JLabel) extra = " text=" + ((JLabel) c).getText();
        if (c instanceof AbstractButton) extra = " text=" + ((AbstractButton) c).getText();
        line(pad + c.getClass().getSimpleName() + " bounds=" + c.getBounds() + extra);
        if (c instanceof Container) {
            for (Component k : ((Container) c).getComponents()) dump(k, depth + 1);
        }
    }
}
