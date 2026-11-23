package com.toolbox.pc;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;

/** 应用图标：品牌渐变圆角方块 + 白色四格 */
public class AppIcon {

    public static BufferedImage image(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        int arc = (int) (size * 0.26);
        g.setPaint(new GradientPaint(0, 0, new Color(0x4C6EF5), size, size, new Color(0x7C4DFF)));
        g.fillRoundRect(0, 0, size, size, arc, arc);

        int pad = (int) (size * 0.22);
        int gap = Math.max(2, (int) (size * 0.06));
        int cell = (int) ((size - pad * 2 - gap) / 2.0);
        int r = Math.max(2, (int) (cell * 0.18));

        g.setColor(Color.WHITE);
        g.fillRoundRect(pad, pad, cell, cell, r, r);
        g.fillRoundRect(pad + cell + gap, pad, cell, cell, r, r);
        g.fillRoundRect(pad, pad + cell + gap, cell, cell, r, r);
        g.setColor(new Color(255, 255, 255, 160));
        g.fillRoundRect(pad + cell + gap, pad + cell + gap, cell, cell, r, r);

        g.dispose();
        return img;
    }

    /** 写出 Windows ICO（内嵌 PNG 压缩） */
    public static void writeIco(BufferedImage img, File out) throws Exception {
        ByteArrayOutputStream pngBos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", pngBos);
        byte[] png = pngBos.toByteArray();

        int w = img.getWidth();
        int h = img.getHeight();
        ByteBuffer bb = ByteBuffer.allocate(22 + png.length);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putShort((short) 0);
        bb.putShort((short) 1);
        bb.putShort((short) 1);
        bb.put((byte) (w >= 256 ? 0 : w));
        bb.put((byte) (h >= 256 ? 0 : h));
        bb.put((byte) 0);
        bb.put((byte) 0);
        bb.putShort((short) 1);
        bb.putShort((short) 32);
        bb.putInt(png.length);
        bb.putInt(22);
        bb.put(png);

        Files.write(out.toPath(), bb.array());
    }
}
