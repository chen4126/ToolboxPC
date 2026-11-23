package com.toolbox.pc;

import javax.swing.*;
import java.awt.*;

/** 统一设计系统：颜色、字体、按钮、间距 */
public class Ui {

    public static final Color PRIMARY = new Color(0x4C6EF5);
    public static final Color PRIMARY_DEEP = new Color(0x3B5BDB);
    public static final Color SUCCESS = new Color(0x22C55E);
    public static final Color DANGER = new Color(0xEF4444);

    /** 全局 FlatLaf / UIManager 配置，主题切换后需重新调用 */
    public static void installDefaults() {
        UIManager.put("defaultFont", font(13, false));
        UIManager.put("Button.arc", 10);
        UIManager.put("Component.arc", 12);
        UIManager.put("Component.focusWidth", 0);
        UIManager.put("Button.focusWidth", 0);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("TextComponent.margin", new Insets(8, 12, 8, 12));
        UIManager.put("Table.rowHeight", 36);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("Table.intercellSpacing", new Dimension(0, 0));
        UIManager.put("TableHeader.height", 38);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
        UIManager.put("ScrollPane.smoothScrolling", true);
        UIManager.put("TitlePane.unifiedBackground", true);
    }

    public static Font font(int size, boolean bold) {
        return new Font("Microsoft YaHei UI", bold ? Font.BOLD : Font.PLAIN, size);
    }

    public static Color sideBg() {
        return Theme.isDark() ? new Color(0x1A1C24) : Color.WHITE;
    }

    public static Color navSelectedBg() {
        return Theme.isDark() ? new Color(0x2B3350) : new Color(0xEEF2FF);
    }

    public static Color navHoverBg() {
        return Theme.isDark() ? new Color(0x232637) : new Color(0xF4F7FE);
    }

    public static Color navText() {
        return Theme.isDark() ? new Color(0xC9D1E0) : new Color(0x475467);
    }

    public static Color navTextSelected() {
        return Theme.isDark() ? new Color(0xAFC0FF) : PRIMARY;
    }

    public static Color navIcon() {
        return Theme.isDark() ? new Color(0x9DA8C0) : new Color(0x667085);
    }

    /** 主按钮：实色品牌底 + 白字 */
    public static JButton primary(String text) {
        JButton b = new JButton(text);
        b.setFont(font(13, true));
        b.setBackground(PRIMARY);
        b.setForeground(Color.WHITE);
        b.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    /** 次按钮：浅底深字 */
    public static JButton secondary(String text) {
        JButton b = new JButton(text);
        b.setFont(font(13, false));
        b.setBorder(BorderFactory.createEmptyBorder(7, 16, 7, 16));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    /** 弱按钮：透明底 */
    public static JButton ghost(String text) {
        JButton b = new JButton(text);
        b.setFont(font(13, false));
        b.setContentAreaFilled(false);
        b.setBorder(BorderFactory.createEmptyBorder(7, 14, 7, 14));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    /** 统一主按钮外观（用于已有按钮改造） */
    public static void asPrimary(JButton b) {
        b.setFont(font(13, true));
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public static JPanel hline() {
        JPanel line = new JPanel();
        line.setOpaque(true);
        line.setBackground(Theme.border());
        line.setPreferredSize(new Dimension(1, 1));
        return line;
    }
}
