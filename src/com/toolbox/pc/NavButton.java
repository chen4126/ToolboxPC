package com.toolbox.pc;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** 侧边导航按钮：悬停高亮、选中圆角底色 + 左侧指示条 */
public class NavButton extends JButton {

    private boolean selected;
    private boolean hover;
    private boolean dragging;
    private Icon normalIcon;
    private Icon activeIcon;

    public NavButton(String text) {
        super(text);
        setOpaque(false);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 10));
        setHorizontalAlignment(SwingConstants.LEFT);
        setIconTextGap(12);
        setFont(Ui.font(13, false));
        setForeground(Ui.navText());
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                hover = true;
                repaint();
            }

            public void mouseExited(MouseEvent e) {
                hover = false;
                repaint();
            }
        });
    }

    /** 普通态 / 选中态两套图标 */
    public void setNavIcons(Icon normal, Icon active) {
        this.normalIcon = normal;
        this.activeIcon = active;
        setIcon(normal);
    }

    public void setSelectedNav(boolean s) {
        selected = s;
        if (activeIcon != null) setIcon(s ? activeIcon : normalIcon);
        setForeground(s ? Ui.navTextSelected() : Ui.navText());
        setFont(Ui.font(13, s));
        repaint();
    }

    public boolean isSelectedNav() {
        return selected;
    }

    /** 拖动排序中的视觉反馈 */
    public void setDragging(boolean d) {
        if (dragging == d) return;
        dragging = d;
        setCursor(Cursor.getPredefinedCursor(d ? Cursor.MOVE_CURSOR : Cursor.HAND_CURSOR));
        repaint();
    }

    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();

        if (dragging) {
            g2.setColor(new Color(Ui.PRIMARY.getRed(), Ui.PRIMARY.getGreen(), Ui.PRIMARY.getBlue(), 22));
            g2.fillRoundRect(0, 0, w, h, 12, 12);
            g2.setColor(Ui.PRIMARY);
            g2.setStroke(new BasicStroke(1.4f));
            g2.drawRoundRect(1, 1, w - 2, h - 2, 12, 12);
        } else if (selected) {
            g2.setColor(Ui.navSelectedBg());
            g2.fillRoundRect(0, 0, w, h, 12, 12);
            g2.setColor(Ui.PRIMARY);
            g2.fillRoundRect(2, h / 2 - 10, 3, 20, 3, 3);
        } else if (hover) {
            g2.setColor(Ui.navHoverBg());
            g2.fillRoundRect(0, 0, w, h, 12, 12);
        }
        g2.dispose();
        super.paintComponent(g);
    }
}
