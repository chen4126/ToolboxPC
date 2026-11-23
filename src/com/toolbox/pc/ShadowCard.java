package com.toolbox.pc;

import javax.swing.*;
import java.awt.*;

/** 圆角阴影卡片容器 */
public class ShadowCard extends JPanel {

    private final int arc;
    private static final int SHADOW = 8;

    public ShadowCard(LayoutManager lm) {
        this(lm, 18);
    }

    public ShadowCard(LayoutManager lm, int arc) {
        super(lm);
        this.arc = arc;
        setOpaque(false);
    }

    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth() - SHADOW;
        int h = getHeight() - SHADOW;

        // 柔和投影：多层半透明叠出渐变
        int layers = 7;
        for (int i = layers; i >= 1; i--) {
            int alpha = Theme.isDark() ? 26 : 9;
            g2.setColor(new Color(15, 23, 42, alpha));
            g2.fillRoundRect(i, i + 2, w - 2 * i, h - 2 * i, arc, arc);
        }

        // 卡片底色
        g2.setColor(Theme.bg());
        g2.fillRoundRect(0, 0, w, h, arc, arc);

        // 细边框
        g2.setColor(Theme.isDark() ? new Color(0x3A3F4B) : new Color(0xE8EDF5));
        g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);

        g2.dispose();
        super.paintComponent(g);
    }
}
