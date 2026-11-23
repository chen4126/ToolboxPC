package com.toolbox.pc;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/** 文本小工具界面：左边输原文，右边出结果，一行按钮按工具切换 */
public class TextToolsPanel extends JPanel {

    private final JTextArea input = new JTextArea();
    private final JTextArea output = new JTextArea();
    private final JComboBox<String> toolBox = new JComboBox<String>(TextTools.TOOLS);
    private final JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    private String lastHint = "";

    public TextToolsPanel() {
        setLayout(new BorderLayout(0, 10));
        setOpaque(false);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        top.setOpaque(false);
        JLabel lab = new JLabel("工具");
        lab.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        toolBox.setPreferredSize(new Dimension(150, 28));
        toolBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                rebuildActions();
            }
        });
        top.add(lab);
        top.add(toolBox);
        add(top, BorderLayout.NORTH);

        input.setLineWrap(true);
        input.setWrapStyleWord(true);
        input.setFont(new Font("Consolas", Font.PLAIN, 13));
        input.setBorder(new EmptyBorder(8, 10, 8, 10));

        output.setLineWrap(true);
        output.setWrapStyleWord(true);
        output.setFont(new Font("Consolas", Font.PLAIN, 13));
        output.setEditable(false);
        output.setBackground(input.getBackground());
        output.setBorder(new EmptyBorder(8, 10, 8, 10));

        JPanel mid = new JPanel(new GridLayout(2, 1, 0, 8));
        mid.setOpaque(false);
        mid.add(boxOf("输入", input));
        mid.add(boxOf("结果", output));
        add(mid, BorderLayout.CENTER);

        actionRow.setOpaque(false);
        JButton copy = new JButton("复制结果");
        copy.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String t = output.getText();
                if (t == null || t.isEmpty()) {
                    Main.notifyMsg("没有结果", "先点一下转换按钮");
                    return;
                }
                Toolkit.getDefaultToolkit().getSystemClipboard()
                        .setContents(new StringSelection(t), null);
                Main.notifyMsg("已复制", t.length() > 30 ? t.substring(0, 30) + "…" : t);
            }
        });
        JButton clear = new JButton("清空");
        clear.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                input.setText("");
                output.setText("");
            }
        });
        /* 复制/清空要常驻右侧：rebuildActions() 会清空 actionRow，所以不能挂在它上面 */
        JPanel rightBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightBtns.setOpaque(false);
        rightBtns.add(copy);
        rightBtns.add(clear);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        left.add(actionRowWrapper());
        bottom.add(left, BorderLayout.WEST);
        bottom.add(actionRow, BorderLayout.CENTER);
        bottom.add(rightBtns, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        rebuildActions();
        setHint("时间戳转日期：粘 10 位或 13 位数字；日期转时间戳：粘 2026-11-12 14:30");
    }

    /** 占位，让按钮行真正贴在操作区左侧 */
    private JComponent actionRowWrapper() {
        JLabel l = new JLabel("操作");
        l.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        return l;
    }

    private JPanel boxOf(String title, JTextArea area) {
        JPanel wrap = new JPanel(new BorderLayout(0, 4));
        wrap.setOpaque(false);
        JLabel t = new JLabel(title);
        t.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
        t.setForeground(Theme.muted());
        t.setBorder(new EmptyBorder(0, 2, 0, 0));
        wrap.add(t, BorderLayout.NORTH);
        JScrollPane sp = new JScrollPane(area);
        sp.setBorder(BorderFactory.createLineBorder(Theme.border(), 1, true));
        sp.getViewport().setBackground(area.getBackground());
        wrap.add(sp, BorderLayout.CENTER);
        return wrap;
    }

    private void setHint(String s) {
        lastHint = s;
        output.setToolTipText(s);
        input.setToolTipText(s);
    }

    public String hint() {
        return lastHint;
    }

    /** 按当前工具换一行操作按钮 */
    public void rebuildActions() {
        actionRow.removeAll();
        int idx = toolBox == null ? 0 : toolBox.getSelectedIndex();
        if (idx == 0) {
            actionRow.add(btn("转成日期", () -> setOut(TextTools.tsToDate(input.getText()))));
            actionRow.add(btn("转成时间戳", () -> setOut(TextTools.dateToTs(input.getText()))));
            setHint("时间戳转日期：粘 10 位或 13 位数字；日期转时间戳：粘 2026-11-12 14:30");
        } else if (idx == 1) {
            actionRow.add(btn("格式化", () -> setOut(TextTools.jsonPretty(input.getText()))));
            actionRow.add(btn("压成一行", () -> setOut(TextTools.jsonMinify(input.getText()))));
            setHint("格式化会在逗号、冒号处换行缩进；压缩会去掉所有空白");
        } else if (idx == 2) {
            actionRow.add(btn("编码", () -> setOut(TextTools.b64Enc(input.getText()))));
            actionRow.add(btn("解码", () -> setOut(TextTools.b64Dec(input.getText()))));
            setHint("中文按 UTF-8 处理，编出来的串可以直接贴进代码里");
        } else if (idx == 3) {
            actionRow.add(btn("编码", () -> setOut(TextTools.urlEnc(input.getText()))));
            actionRow.add(btn("解码", () -> setOut(TextTools.urlDec(input.getText()))));
            setHint("编码用的 UTF-8，空格会变成 +");
        } else {
            actionRow.add(btn("全大写", () -> setOut(TextTools.upper(input.getText()))));
            actionRow.add(btn("全小写", () -> setOut(TextTools.lower(input.getText()))));
            actionRow.add(btn("驼峰", () -> setOut(TextTools.camel(input.getText()))));
            actionRow.add(btn("下划线", () -> setOut(TextTools.snake(input.getText()))));
            setHint("user name / user_name / user-name 都能转成 userName 或 user_name");
        }
        actionRow.revalidate();
        actionRow.repaint();
    }

    private void setOut(String s) {
        output.setText(s == null ? "" : s);
        output.setCaretPosition(0);
    }

    private JButton btn(String text, Runnable action) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
        return b;
    }

    /* 便于测试直接取用 */
    public JTextArea inputArea() {
        return input;
    }

    public JTextArea outputArea() {
        return output;
    }

    public JComboBox<String> toolCombo() {
        return toolBox;
    }

    public JPanel actionPanel() {
        return actionRow;
    }
}
