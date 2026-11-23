package com.toolbox.pc;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

/** 代码绘制的线性图标（无需外部图片资源） */
public class IconFactory {

    private static final int S = 20;

    public static Icon nav(String key, Color color) {
        BufferedImage img = new BufferedImage(S, S, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(color);
        g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        if ("overview".equals(key)) {
            g.drawRoundRect(2, 2, 7, 7, 2, 2);
            g.drawRoundRect(11, 2, 7, 7, 2, 2);
            g.drawRoundRect(2, 11, 7, 7, 2, 2);
            g.drawRoundRect(11, 11, 7, 7, 2, 2);
        } else if ("calc".equals(key)) {
            g.drawRoundRect(4, 2, 12, 16, 3, 3);
            g.drawLine(4, 7, 16, 7);
            g.fillOval(6, 10, 2, 2);
            g.fillOval(11, 10, 2, 2);
            g.fillOval(6, 14, 2, 2);
            g.fillOval(11, 14, 2, 2);
        } else if ("ledger".equals(key)) {
            g.drawRoundRect(3, 2, 14, 16, 2, 2);
            g.drawLine(7, 2, 7, 18);
            g.drawLine(9, 6, 14, 6);
            g.drawLine(9, 10, 14, 10);
            g.drawLine(9, 14, 12, 14);
        } else if ("memo".equals(key)) {
            Path2D p = new Path2D.Float();
            p.moveTo(4, 3);
            p.lineTo(12, 3);
            p.lineTo(16, 7);
            p.lineTo(16, 17);
            p.lineTo(4, 17);
            p.closePath();
            g.draw(p);
            g.drawLine(12, 3, 12, 7);
            g.drawLine(12, 7, 16, 7);
            g.drawLine(7, 10, 13, 10);
            g.drawLine(7, 13, 13, 13);
        } else if ("counter".equals(key)) {
            g.drawOval(3, 3, 14, 14);
            g.drawLine(10, 6, 10, 14);
            g.drawLine(6, 10, 14, 10);
        } else if ("pomodoro".equals(key)) {
            g.drawOval(4, 6, 12, 12);
            g.drawLine(7, 2, 13, 2);
            g.drawLine(10, 2, 10, 6);
            g.drawLine(10, 12, 10, 9);
            g.drawLine(10, 12, 13, 14);
        } else if ("schedule".equals(key)) {
            g.drawRoundRect(3, 4, 14, 14, 3, 3);
            g.drawLine(3, 8, 17, 8);
            g.drawLine(7, 6, 7, 2);
            g.drawLine(13, 6, 13, 2);
            g.fillOval(6, 11, 2, 2);
            g.fillOval(12, 11, 2, 2);
        } else if ("countdown".equals(key)) {
            g.drawLine(5, 3, 15, 3);
            g.drawLine(5, 17, 15, 17);
            g.drawLine(5, 3, 10, 10);
            g.drawLine(15, 3, 10, 10);
            g.drawLine(5, 17, 10, 10);
            g.drawLine(15, 17, 10, 10);
        } else if ("habit".equals(key)) {
            g.drawRoundRect(3, 4, 14, 14, 3, 3);
            g.drawLine(3, 8, 17, 8);
            g.drawLine(7, 6, 7, 2);
            g.drawLine(13, 6, 13, 2);
            g.drawLine(6, 12, 9, 15);
            g.drawLine(9, 15, 14, 10);
        } else if ("toolbox".equals(key)) {
            g.drawRoundRect(2, 8, 16, 9, 2, 2);
            g.drawLine(2, 12, 18, 12);
            g.drawRoundRect(7, 3, 6, 5, 2, 2);
        } else if ("stats".equals(key)) {
            g.drawLine(3, 17, 17, 17);
            g.drawRoundRect(4, 10, 3, 7, 1, 1);
            g.drawRoundRect(9, 6, 3, 11, 1, 1);
            g.drawRoundRect(14, 3, 3, 14, 1, 1);
        } else if ("settings".equals(key)) {
            g.drawOval(7, 7, 6, 6);
            for (int i = 0; i < 8; i++) {
                double a = Math.toRadians(i * 45);
                int x1 = (int) Math.round(10 + Math.cos(a) * 5);
                int y1 = (int) Math.round(10 + Math.sin(a) * 5);
                int x2 = (int) Math.round(10 + Math.cos(a) * 8);
                int y2 = (int) Math.round(10 + Math.sin(a) * 8);
                g.drawLine(x1, y1, x2, y2);
            }
        }
        g.dispose();
        return new ImageIcon(img);
    }
}
