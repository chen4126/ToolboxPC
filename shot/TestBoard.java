import com.toolbox.pc.*;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** 截取桌面小窗（离屏渲染，避免被前台窗口遮挡） */
public class TestBoard {
    public static void main(String[] args) throws Exception {
        Theme.init();
        Ui.installDefaults();
        SwingUtilities.invokeAndWait(() -> MiniBoard.show());
        Thread.sleep(900);

        java.lang.reflect.Field fi = MiniBoard.class.getDeclaredField("inst");
        fi.setAccessible(true);
        Object inst = fi.get(null);
        java.lang.reflect.Field fw = MiniBoard.class.getDeclaredField("win");
        fw.setAccessible(true);
        Window win = (Window) fw.get(inst);
        System.out.println("board bounds = " + win.getBounds());

        Component root = win.getComponent(0);
        int w = Math.max(1, root.getWidth());
        int h = Math.max(1, root.getHeight());
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        root.paint(g);
        g.dispose();
        ImageIO.write(img, "png", new File("C:\\Users\\chen\\Desktop\\ui-board.png"));
        System.out.println("saved ui-board.png " + w + "x" + h);
        System.exit(0);
    }
}
