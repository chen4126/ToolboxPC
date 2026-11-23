package com.toolbox.pc;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

public class CalculatorPanel extends JPanel {

    private JLabel display;
    private StringBuilder expr = new StringBuilder();
    private boolean justEvaluated = false;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardHost = new JPanel(cardLayout);
    private JToggleButton calcTab;
    private JToggleButton textTab;
    private TextToolsPanel textTools;

    public CalculatorPanel() {
        setLayout(new BorderLayout(0, 12));
        setOpaque(false);

        cardHost.setOpaque(false);
        cardHost.add(buildCalc(), "calc");
        textTools = new TextToolsPanel();
        cardHost.add(textTools, "text");
        add(cardHost, BorderLayout.CENTER);

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        bar.setOpaque(false);
        calcTab = tab("计算器");
        textTab = tab("文本工具");
        calcTab.setSelected(true);
        calcTab.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showCard("calc");
            }
        });
        textTab.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showCard("text");
            }
        });
        ButtonGroup group = new ButtonGroup();
        group.add(calcTab);
        group.add(textTab);
        bar.add(calcTab);
        bar.add(textTab);
        add(bar, BorderLayout.NORTH);
    }

    /** 顶部的小切换标签（计算器 / 文本工具） */
    private JToggleButton tab(String text) {
        JToggleButton b = new JToggleButton(text);
        b.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(88, 28));
        return b;
    }

    /** 切到计算器或文本工具 */
    public void showCard(String key) {
        cardLayout.show(cardHost, key);
        calcTab.setSelected("calc".equals(key));
        textTab.setSelected("text".equals(key));
    }

    /** 当前显示的卡片，测试用 */
    public String currentCard() {
        return calcTab.isSelected() ? "calc" : "text";
    }

    public TextToolsPanel textToolsPanel() {
        return textTools;
    }

    private JPanel buildCalc() {
        JPanel calcBody = new JPanel(new BorderLayout(0, 14));
        calcBody.setOpaque(false);

        JPanel screen = new JPanel(new BorderLayout()) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.isDark() ? new Color(0x111318) : new Color(0x1B2030));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        screen.setOpaque(false);
        screen.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        display = new JLabel("0", SwingConstants.RIGHT);
        display.setFont(new Font("SansSerif", Font.BOLD, 32));
        display.setForeground(new Color(0xF8FAFC));
        screen.add(display, BorderLayout.CENTER);
        calcBody.add(screen, BorderLayout.NORTH);

        Color opBg = Theme.isDark() ? new Color(0x2B3350) : new Color(0xEEF2FF);
        Color opFg = Theme.isDark() ? new Color(0xAFC0FF) : Ui.PRIMARY;
        Color clearBg = Theme.isDark() ? new Color(0x3A2328) : new Color(0xFEF2F2);
        Color clearFg = Theme.isDark() ? new Color(0xFCA5A5) : Ui.DANGER;

        String[][] rows = {
                {"C", "←", "%", "÷"},
                {"7", "8", "9", "×"},
                {"4", "5", "6", "-"},
                {"1", "2", "3", "+"},
                {"0", ".", "=", ""}
        };
        JPanel grid = new JPanel(new GridLayout(rows.length, 4, 8, 8));
        grid.setOpaque(false);
        for (String[] row : rows) {
            for (String key : row) {
                if (key.isEmpty()) continue;
                JButton b = new JButton(key);
                b.setFont(new Font("SansSerif", Font.BOLD, 19));
                b.setFocusPainted(false);
                b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                if (key.equals("=")) {
                    b.setBackground(Ui.PRIMARY);
                    b.setForeground(Color.WHITE);
                } else if (key.equals("C") || key.equals("←")) {
                    b.setBackground(clearBg);
                    b.setForeground(clearFg);
                } else if ("÷×-+%".contains(key)) {
                    b.setBackground(opBg);
                    b.setForeground(opFg);
                } else {
                    b.setForeground(Theme.isDark() ? new Color(0xE6E8F0) : Theme.text());
                }
                b.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        onKey(key);
                    }
                });
                grid.add(b);
            }
        }
        calcBody.add(grid, BorderLayout.CENTER);
        return calcBody;
    }

    private void onKey(String key) {
        if (key.equals("C")) {
            expr.setLength(0);
            display.setText("0");
            justEvaluated = false;
            return;
        }
        if (key.equals("⌫")) {
            if (expr.length() > 0) expr.deleteCharAt(expr.length() - 1);
            justEvaluated = false;
            refresh();
            return;
        }
        if (key.equals("=")) {
            if (justEvaluated) return;
            double r = evaluate(expr.toString());
            if (Double.isNaN(r) || Double.isInfinite(r)) {
                display.setText("错误");
                expr.setLength(0);
            } else {
                String s = fmt(r);
                display.setText(s);
                expr.setLength(0);
                expr.append(s);
                justEvaluated = true;
            }
            return;
        }
        if (justEvaluated && !"÷×-+%".contains(key)) {
            expr.setLength(0);
        }
        justEvaluated = false;

        if ("÷×-+".contains(key)) {
            if (expr.length() == 0) return;
            char last = expr.charAt(expr.length() - 1);
            if ("÷×-+".indexOf(last) >= 0) expr.deleteCharAt(expr.length() - 1);
            expr.append(key);
        } else if (key.equals("%")) {
            int i = expr.length() - 1;
            while (i >= 0 && !"÷×-+".contains(expr.charAt(i) + "")) i--;
            String numStr = expr.substring(i + 1);
            if (numStr.isEmpty() || numStr.equals("-") || numStr.equals(".")) return;
            double v = Double.parseDouble(numStr) / 100.0;
            expr.delete(i + 1, expr.length());
            expr.append(fmt(v));
        } else if (key.equals(".")) {
            int i = expr.length() - 1;
            while (i >= 0 && !"÷×-+".contains(expr.charAt(i) + "")) i--;
            String numStr = expr.substring(i + 1);
            if (numStr.contains(".")) return;
            expr.append(numStr.isEmpty() ? "0." : ".");
        } else {
            if (!(expr.length() == 0 && key.equals("0"))) expr.append(key);
        }
        refresh();
    }

    private void refresh() {
        display.setText(expr.length() == 0 ? "0" : expr.toString());
    }

    private String fmt(double v) {
        if (v == Math.floor(v) && !Double.isInfinite(v)) return String.valueOf((long) v);
        String s = String.valueOf(v);
        return s.length() > 12 ? s.substring(0, 12) : s;
    }

    private double evaluate(String s) {
        if (s == null || s.isEmpty()) return 0;
        String t = s.replace('÷', '/').replace('×', '*');
        try {
            ArrayList<Double> nums = new ArrayList<Double>();
            ArrayList<Character> ops = new ArrayList<Character>();
            StringBuilder num = new StringBuilder();
            boolean expectNum = true;
            for (int i = 0; i < t.length(); i++) {
                char c = t.charAt(i);
                if (c == '-' && expectNum) {
                    num.append(c);
                } else if (Character.isDigit(c) || c == '.') {
                    num.append(c);
                    expectNum = false;
                } else if ("+-*/".indexOf(c) >= 0) {
                    if (num.length() == 0) return Double.NaN;
                    nums.add(Double.parseDouble(num.toString()));
                    num.setLength(0);
                    ops.add(c);
                    expectNum = true;
                } else {
                    return Double.NaN;
                }
            }
            if (num.length() == 0) return Double.NaN;
            nums.add(Double.parseDouble(num.toString()));

            ArrayList<Double> rn = new ArrayList<Double>();
            ArrayList<Character> ro = new ArrayList<Character>();
            rn.add(nums.get(0));
            for (int i = 0; i < ops.size(); i++) {
                char op = ops.get(i);
                double next = nums.get(i + 1);
                if (op == '*' || op == '/') {
                    double last = rn.remove(rn.size() - 1);
                    rn.add(op == '*' ? last * next : last / next);
                } else {
                    rn.add(next);
                    ro.add(op);
                }
            }
            double acc = rn.get(0);
            for (int i = 0; i < ro.size(); i++) {
                acc = ro.get(i) == '+' ? acc + rn.get(i + 1) : acc - rn.get(i + 1);
            }
            return acc;
        } catch (Exception e) {
            return Double.NaN;
        }
    }
}
