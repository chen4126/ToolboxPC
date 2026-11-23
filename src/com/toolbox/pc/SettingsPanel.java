package com.toolbox.pc;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** 设置中心：外观 / 导航排序 / 提醒 / 护眼 / 数据 / 关于 */
public class SettingsPanel extends JPanel {

    private JPanel navOrderBox;
    private JLabel eyeStatLabel;
    private JLabel healthStatLabel;
    /** 设置页改标签时用的临时备忘录实例（主窗还没建好时用） */
    private MemoPanel tempMemo;
    /** 拖动排序的运行态：从哪个索引拖起、当前高亮的目标索引 */
    private int dragFrom = -1;
    private int dragHover = -1;
    private int dragPressY = 0;

    /** 护眼数据行跟着计时刷新 */
    void refreshEyeStat() {
        if (eyeStatLabel != null) eyeStatLabel.setText(EyeCare.todayText());
    }

    /** 久坐 / 喝水数据行跟着刷新 */
    void refreshHealthStat() {
        if (healthStatLabel != null) healthStatLabel.setText(HealthCare.todayText());
    }

    public SettingsPanel() {
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        setBackground(Theme.bg());

        JPanel list = new ScrollablePanel();
        list.setOpaque(false);
        list.setLayout(new StackLayout());

        /* ===== 外观 ===== */
        list.add(sectionTitle("外观"));
        list.add(Box.createVerticalStrut(8));

        JPanel themeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        themeRow.setOpaque(false);
        themeRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel themeLbl = new JLabel("主题：");
        themeLbl.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 14));
        themeRow.add(themeLbl);

        final JRadioButton lightRadio = new JRadioButton("浅色");
        final JRadioButton darkRadio = new JRadioButton("深色");
        final JRadioButton autoRadio = new JRadioButton("跟随系统");
        ButtonGroup themeGroup = new ButtonGroup();
        themeGroup.add(lightRadio);
        themeGroup.add(darkRadio);
        themeGroup.add(autoRadio);
        boolean isAuto = readBool("autoDark", false);
        boolean isDark = readBool("dark", false);
        autoRadio.setSelected(isAuto);
        if (!isAuto) {
            lightRadio.setSelected(!isDark);
            darkRadio.setSelected(isDark);
        }
        ActionListener themeL = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (lightRadio.isSelected()) {
                    save("dark", false);
                    save("autoDark", false);
                } else if (darkRadio.isSelected()) {
                    save("dark", true);
                    save("autoDark", false);
                } else {
                    save("autoDark", true);
                }
                Main.applyThemeNow();
            }
        };
        lightRadio.addActionListener(themeL);
        darkRadio.addActionListener(themeL);
        autoRadio.addActionListener(themeL);
        themeRow.add(lightRadio);
        themeRow.add(darkRadio);
        themeRow.add(autoRadio);
        list.add(themeRow);
        list.add(Box.createVerticalStrut(6));

        JLabel themeTip = new JLabel("跟随系统会读取 Windows 的应用主题，系统切深色时自动同步");
        themeTip.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 11));
        themeTip.setForeground(Theme.muted());
        themeTip.setAlignmentX(Component.LEFT_ALIGNMENT);
        list.add(themeTip);
        list.add(Box.createVerticalStrut(10));

        final JCheckBox pinBox = new JCheckBox("窗口置顶：始终显示在其他窗口之上");
        pinBox.setSelected(readBool("pin", false));
        pinBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                save("pin", pinBox.isSelected());
                Main.setPinned(pinBox.isSelected());
            }
        });
        list.add(option(pinBox));
        list.add(Box.createVerticalStrut(10));

        JPanel closeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        closeRow.setOpaque(false);
        closeRow.add(new JLabel("点关闭按钮时："));
        final JRadioButton trayRadio = new JRadioButton("最小化到托盘（提醒后台继续）");
        final JRadioButton exitRadio = new JRadioButton("直接退出");
        ButtonGroup g = new ButtonGroup();
        g.add(trayRadio);
        g.add(exitRadio);
        boolean trayOnClose = readBool("onClose", true);
        trayRadio.setSelected(trayOnClose);
        exitRadio.setSelected(!trayOnClose);
        ActionListener closeL = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Main.sTrayOnClose = trayRadio.isSelected();
                save("onClose", trayRadio.isSelected());
            }
        };
        trayRadio.addActionListener(closeL);
        exitRadio.addActionListener(closeL);
        closeRow.add(trayRadio);
        closeRow.add(exitRadio);
        JPanel closeWrap = new JPanel(new BorderLayout());
        closeWrap.setOpaque(false);
        closeWrap.add(closeRow, BorderLayout.WEST);
        list.add(closeWrap);
        list.add(Box.createVerticalStrut(10));

        final JCheckBox animBox = new JCheckBox("界面动效：启动浮现与切页淡出过渡");
        animBox.setSelected(readBool("anim", true));
        animBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                save("anim", animBox.isSelected());
            }
        });
        list.add(option(animBox));
        list.add(Box.createVerticalStrut(8));

        final JCheckBox boardBox = new JCheckBox("启动时自动显示今日小窗（桌面置顶展示今天的待办与日程）");
        boardBox.setSelected("1".equals(MiniBoard.readSetting("boardAuto")));
        boardBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                MiniBoard.writeSetting("boardAuto", boardBox.isSelected() ? "1" : "0");
                if (boardBox.isSelected()) MiniBoard.show();
            }
        });
        list.add(option(boardBox));
        list.add(Box.createVerticalStrut(8));

        JLabel rowsLbl = new JLabel("小窗显示今天的日程条数");
        rowsLbl.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
        rowsLbl.setForeground(Theme.muted());
        int curRows = 4;
        try {
            int v = Integer.parseInt(MiniBoard.readSetting("boardRows").trim());
            if (v >= 1 && v <= 6) curRows = v;
        } catch (Exception ignored) {
        }
        final JComboBox<String> rowsBox = new JComboBox<String>(
                new String[]{"1 条", "2 条", "3 条", "4 条", "5 条", "6 条"});
        rowsBox.setSelectedIndex(curRows - 1);
        rowsBox.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
        rowsBox.setPreferredSize(new Dimension(96, 28));
        rowsBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                MiniBoard.writeSetting("boardRows", String.valueOf(rowsBox.getSelectedIndex() + 1));
                if (MiniBoard.isVisibleNow()) MiniBoard.show();
            }
        });
        JPanel rowsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        rowsRow.setOpaque(false);
        rowsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        rowsRow.add(rowsLbl);
        rowsRow.add(rowsBox);
        list.add(rowsRow);
        list.add(Box.createVerticalStrut(10));

        JLabel arcLbl = new JLabel("过期多久自动收进归档");
        arcLbl.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
        arcLbl.setForeground(Theme.muted());
        final String[] arcOpts = {"15 天", "30 天", "60 天", "90 天"};
        int curArc = 30;
        try {
            int v = Integer.parseInt(MiniBoard.readSetting("archiveDays").trim());
            if (v == 15 || v == 30 || v == 60 || v == 90) curArc = v;
        } catch (Exception ignored) {
        }
        final JComboBox<String> arcBox = new JComboBox<String>(arcOpts);
        arcBox.setSelectedItem(curArc + " 天");
        arcBox.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
        arcBox.setPreferredSize(new Dimension(96, 28));
        arcBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String s = (String) arcBox.getSelectedItem();
                MiniBoard.writeSetting("archiveDays", s.replace(" 天", "").trim());
                Main.notifyMsg("归档规则已更新", "过期超过 " + s + " 的待办会自动收起");
            }
        });
        JPanel arcRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        arcRow.setOpaque(false);
        arcRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        arcRow.add(arcLbl);
        arcRow.add(arcBox);
        list.add(arcRow);
        list.add(Box.createVerticalStrut(20));

        /* ===== 功能模块 ===== */
        list.add(sectionTitle("功能模块"));
        list.add(Box.createVerticalStrut(6));
        JLabel modTip = new JLabel("取消勾选即从左侧导航移除，不占位置、不出现在概览入口；设置模块始终保留");
        modTip.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
        modTip.setForeground(Theme.muted());
        modTip.setAlignmentX(Component.LEFT_ALIGNMENT);
        list.add(modTip);
        list.add(Box.createVerticalStrut(8));

        String[] modules = {"overview", "calc", "ledger", "memo", "counter",
                "pomodoro", "schedule", "countdown", "stats"};
        JPanel modGrid = new JPanel(new GridLayout(0, 3, 14, 8));
        modGrid.setOpaque(false);
        modGrid.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (String key : modules) {
            final String k = key;
            final JCheckBox box = new JCheckBox(Main.navLabel(k));
            box.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
            box.setOpaque(false);
            box.setSelected(Main.isEnabled(k));
            box.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            box.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Main.setEnabled(k, box.isSelected());
                    renderNavOrder();
                }
            });
            modGrid.add(box);
        }
        list.add(modGrid);
        list.add(Box.createVerticalStrut(20));

        /* ===== 导航排序 ===== */
        list.add(sectionTitle("导航排序"));
        list.add(Box.createVerticalStrut(6));
        JLabel orderTip = new JLabel("↑ ↓ 调整，或直接按住左侧项目拖动排序，立即生效并自动记住");
        orderTip.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
        orderTip.setForeground(Theme.muted());
        orderTip.setAlignmentX(Component.LEFT_ALIGNMENT);
        list.add(orderTip);
        list.add(Box.createVerticalStrut(8));
        navOrderBox = new JPanel();
        navOrderBox.setLayout(new StackLayout());
        navOrderBox.setOpaque(false);
        navOrderBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        list.add(navOrderBox);
        renderNavOrder();
        list.add(Box.createVerticalStrut(20));

        /* ===== 提醒 ===== */
        list.add(sectionTitle("提醒"));
        list.add(Box.createVerticalStrut(8));

        final JCheckBox strongBox = new JCheckBox("强提醒弹窗：到点时弹出置顶醒目窗口（红框闪烁）");
        strongBox.setSelected(readBool("strong", true));
        strongBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                save("strong", strongBox.isSelected());
            }
        });
        list.add(option(strongBox));
        list.add(Box.createVerticalStrut(10));

        final JCheckBox soundBox = new JCheckBox("通知音：到点时播放提示音（音效在下面选）");
        soundBox.setSelected(readBool("sound", true));
        list.add(option(soundBox));
        list.add(Box.createVerticalStrut(6));

        JPanel soundRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        soundRow.setOpaque(false);
        soundRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel kindLbl = new JLabel("音效");
        kindLbl.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        final JComboBox<String> kindBox = new JComboBox<String>(Alerts.KIND_LABELS);
        for (int i = 0; i < Alerts.KINDS.length; i++) {
            if (Alerts.KINDS[i].equals(Alerts.kind())) kindBox.setSelectedIndex(i);
        }
        final JButton tryBtn = new JButton("试听");
        kindBox.setEnabled(soundBox.isSelected());
        tryBtn.setEnabled(soundBox.isSelected());
        kindBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int idx = kindBox.getSelectedIndex();
                if (idx < 0) return;
                Alerts.setKind(Alerts.KINDS[idx]);
            }
        });
        tryBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int idx = kindBox.getSelectedIndex();
                if (idx < 0) return;
                Alerts.preview(Alerts.KINDS[idx]);
            }
        });
        soundBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                save("sound", soundBox.isSelected());
                kindBox.setEnabled(soundBox.isSelected());
                tryBtn.setEnabled(soundBox.isSelected());
            }
        });
        soundRow.add(kindLbl);
        soundRow.add(kindBox);
        soundRow.add(tryBtn);
        list.add(soundRow);
        list.add(Box.createVerticalStrut(6));

        JLabel soundTip = new JLabel("五种音效都是程序合成的，不用装音频文件；改完立即生效，下次提醒就用新音效");
        soundTip.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
        soundTip.setForeground(Theme.muted());
        soundTip.setAlignmentX(Component.LEFT_ALIGNMENT);
        list.add(soundTip);
        list.add(Box.createVerticalStrut(20));

        /* ===== 护眼 ===== */
        list.add(sectionTitle("护眼"));
        list.add(Box.createVerticalStrut(8));

        final JCheckBox eyeBox = new JCheckBox("护眼提醒：连续用眼到点弹休息提示（20-20-20 原则）");
        eyeBox.setSelected(EyeCare.isOn());
        eyeBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                save("eyeOn", eyeBox.isSelected());
                EyeCare.reload();
                refreshEyeStat();
            }
        });
        list.add(option(eyeBox));
        list.add(Box.createVerticalStrut(10));

        JPanel eyeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        eyeRow.setOpaque(false);
        eyeRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel gapLbl = new JLabel("提醒间隔");
        gapLbl.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        final String[] gapVals = {"20 分钟", "30 分钟", "40 分钟", "45 分钟", "60 分钟"};
        final int[] gapNums = {20, 30, 40, 45, 60};
        JComboBox<String> gapBox = new JComboBox<String>(gapVals);
        gapBox.setSelectedItem(gapNums[0]);
        for (int i = 0; i < gapNums.length; i++) {
            if (gapNums[i] == EyeCare.gap()) gapBox.setSelectedIndex(i);
        }
        gapBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int idx = gapBox.getSelectedIndex();
                if (idx < 0) return;
                saveStr("eyeGap", String.valueOf(gapNums[idx]));
                EyeCare.reload();
                refreshEyeStat();
            }
        });
        JLabel restLbl = new JLabel("休息时长");
        restLbl.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        final String[] restVals = {"20 秒", "30 秒", "1 分钟", "2 分钟"};
        final int[] restNums = {20, 30, 60, 120};
        JComboBox<String> restBox = new JComboBox<String>(restVals);
        for (int i = 0; i < restNums.length; i++) {
            if (restNums[i] == EyeCare.rest()) restBox.setSelectedIndex(i);
        }
        restBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int idx = restBox.getSelectedIndex();
                if (idx < 0) return;
                saveStr("eyeRest", String.valueOf(restNums[idx]));
                EyeCare.reload();
                refreshEyeStat();
            }
        });
        eyeRow.add(gapLbl);
        eyeRow.add(gapBox);
        eyeRow.add(Box.createHorizontalStrut(10));
        eyeRow.add(restLbl);
        eyeRow.add(restBox);
        list.add(eyeRow);
        list.add(Box.createVerticalStrut(8));

        eyeStatLabel = new JLabel(EyeCare.todayText());
        eyeStatLabel.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        eyeStatLabel.setForeground(Theme.text());
        eyeStatLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        list.add(eyeStatLabel);

        JLabel eyeRound = new JLabel(EyeCare.roundText());
        eyeRound.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
        eyeRound.setForeground(Theme.muted());
        eyeRound.setAlignmentX(Component.LEFT_ALIGNMENT);
        list.add(Box.createVerticalStrut(4));
        list.add(eyeRound);

        JPanel eyeBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        eyeBtns.setOpaque(false);
        eyeBtns.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton restNow = new JButton("现在休息一下");
        restNow.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (EyeCare.on) {
                    EyeCare.reminding = true;
                    EyeCare.showRestWindow();
                } else {
                    Main.notifyMsg("护眼提醒已关闭", "在上面勾上「护眼提醒」再试");
                }
            }
        });
        JButton roundReset = new JButton("重置本次计时");
        roundReset.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                EyeCare.resetRound();
                refreshEyeStat();
                Main.notifyMsg("已重置", "本轮连续用眼从 0 开始算");
            }
        });
        eyeBtns.add(restNow);
        eyeBtns.add(roundReset);
        list.add(Box.createVerticalStrut(8));
        list.add(eyeBtns);

        JLabel eyeTip = new JLabel("连续 5 分钟没有任何鼠标或键盘操作，视为人不在，自动暂停计时；数据存在 eye.txt");
        eyeTip.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
        eyeTip.setForeground(Theme.muted());
        eyeTip.setAlignmentX(Component.LEFT_ALIGNMENT);
        list.add(Box.createVerticalStrut(6));
        list.add(eyeTip);
        list.add(Box.createVerticalStrut(20));

        /* ===== 久坐 / 喝水 ===== */
        list.add(sectionTitle("久坐 / 喝水"));
        list.add(Box.createVerticalStrut(8));

        final JCheckBox sitBox = new JCheckBox("久坐提醒：一直坐着每到点提醒起来活动");
        sitBox.setSelected(HealthCare.sitOn);
        list.add(option(sitBox));
        list.add(Box.createVerticalStrut(6));

        JPanel sitRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        sitRow.setOpaque(false);
        sitRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel sitLbl = new JLabel("坐下多久提醒");
        sitLbl.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        final String[] sitVals = {"30 分钟", "45 分钟", "60 分钟", "90 分钟"};
        final int[] sitNums = {30, 45, 60, 90};
        final JComboBox<String> sitGapBox = new JComboBox<String>(sitVals);
        for (int i = 0; i < sitNums.length; i++) {
            if (sitNums[i] == HealthCare.sitMin) sitGapBox.setSelectedIndex(i);
        }
        sitGapBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int idx = sitGapBox.getSelectedIndex();
                if (idx < 0) return;
                HealthCare.saveSetting("sitGap", String.valueOf(sitNums[idx]));
                HealthCare.reload();
                refreshHealthStat();
            }
        });
        sitRow.add(sitLbl);
        sitRow.add(sitGapBox);
        list.add(sitRow);
        list.add(Box.createVerticalStrut(8));

        final JCheckBox waterBox = new JCheckBox("喝水提醒：每到点提醒喝一杯水");
        waterBox.setSelected(HealthCare.waterOn);
        list.add(option(waterBox));
        list.add(Box.createVerticalStrut(6));

        JPanel waterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        waterRow.setOpaque(false);
        waterRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel waterLbl = new JLabel("多久提醒一次");
        waterLbl.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        final String[] waterVals = {"30 分钟", "45 分钟", "60 分钟"};
        final int[] waterNums = {30, 45, 60};
        final JComboBox<String> waterGapBox = new JComboBox<String>(waterVals);
        for (int i = 0; i < waterNums.length; i++) {
            if (waterNums[i] == HealthCare.waterMin) waterGapBox.setSelectedIndex(i);
        }
        waterGapBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int idx = waterGapBox.getSelectedIndex();
                if (idx < 0) return;
                HealthCare.saveSetting("waterGap", String.valueOf(waterNums[idx]));
                HealthCare.reload();
                refreshHealthStat();
            }
        });
        waterRow.add(waterLbl);
        waterRow.add(waterGapBox);
        list.add(waterRow);
        list.add(Box.createVerticalStrut(8));

        sitBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                save("sitOn", sitBox.isSelected());
                HealthCare.reload();
                HealthCare.resetAll();
                sitGapBox.setEnabled(sitBox.isSelected());
                refreshHealthStat();
            }
        });
        waterBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                save("waterOn", waterBox.isSelected());
                HealthCare.reload();
                HealthCare.resetAll();
                waterGapBox.setEnabled(waterBox.isSelected());
                refreshHealthStat();
            }
        });
        sitGapBox.setEnabled(sitBox.isSelected());
        waterGapBox.setEnabled(waterBox.isSelected());

        healthStatLabel = new JLabel(HealthCare.todayText());
        healthStatLabel.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        healthStatLabel.setForeground(Theme.text());
        healthStatLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        list.add(healthStatLabel);

        JLabel healthRound = new JLabel(HealthCare.roundText());
        healthRound.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
        healthRound.setForeground(Theme.muted());
        healthRound.setAlignmentX(Component.LEFT_ALIGNMENT);
        list.add(Box.createVerticalStrut(4));
        list.add(healthRound);

        JPanel healthBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        healthBtns.setOpaque(false);
        healthBtns.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton healthReset = new JButton("我刚起来 / 刚喝过水");
        healthReset.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                HealthCare.resetAll();
                refreshHealthStat();
                Main.notifyMsg("已重置", "久坐与喝水计时都从 0 重新开始");
            }
        });
        healthBtns.add(healthReset);
        list.add(Box.createVerticalStrut(8));
        list.add(healthBtns);

        JLabel healthTip = new JLabel("同样在连续 5 分钟没有鼠标或键盘操作时暂停计时；数据存在 health.txt");
        healthTip.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
        healthTip.setForeground(Theme.muted());
        healthTip.setAlignmentX(Component.LEFT_ALIGNMENT);
        list.add(Box.createVerticalStrut(6));
        list.add(healthTip);
        list.add(Box.createVerticalStrut(20));

        /* ===== 番茄钟 ===== */
        list.add(sectionTitle("番茄钟"));
        list.add(Box.createVerticalStrut(8));

        final JCheckBox pomoMemoBox = new JCheckBox("专注完成后自动在备忘录记一条（打上「番茄」标签）");
        pomoMemoBox.setSelected(Store.flag("pomoMemo", true));
        pomoMemoBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Store.setFlag("pomoMemo", pomoMemoBox.isSelected());
            }
        });
        list.add(option(pomoMemoBox));
        list.add(Box.createVerticalStrut(6));
        JLabel pomoTip = new JLabel("记的是「专注 25 分钟 · 任务名」这种，回头在备忘录用「番茄」标签就能把一天的专注捞出来");
        pomoTip.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
        pomoTip.setForeground(Theme.muted());
        pomoTip.setAlignmentX(Component.LEFT_ALIGNMENT);
        list.add(pomoTip);
        list.add(Box.createVerticalStrut(8));

        /* 时长预设：一下改专注 + 休息两个数 */
        int curF = 25, curB = 5;
        List<String> pf = Store.readLines("pomodoro.txt");
        try {
            if (pf.size() > 0) curF = Integer.parseInt(pf.get(0).trim());
        } catch (Exception ignored) {
        }
        try {
            if (pf.size() > 1) curB = Integer.parseInt(pf.get(1).trim());
        } catch (Exception ignored) {
        }
        final String[] presetNames = {"当前 " + curF + " + " + curB, "经典 25 + 5", "深度 50 + 10", "速战 15 + 3"};
        final int[][] presetNums = {{25, 5}, {25, 5}, {50, 10}, {15, 3}};
        JPanel presetRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        presetRow.setOpaque(false);
        presetRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel presetLbl = new JLabel("时长预设");
        presetLbl.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        final JComboBox<String> presetBox = new JComboBox<String>(presetNames);
        presetBox.setPreferredSize(new Dimension(130, 30));
        final JLabel presetInfo = new JLabel(" ");
        presetInfo.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
        presetInfo.setForeground(Theme.muted());
        presetBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int idx = presetBox.getSelectedIndex();
                if (idx <= 0) return;
                int f = presetNums[idx][0], b = presetNums[idx][1];
                PomodoroPanel pp = Main.page(PomodoroPanel.class);
                if (pp != null) {
                    pp.applyPreset(f, b);
                } else {
                    List<String> out = new ArrayList<String>(Store.readLines("pomodoro.txt"));
                    while (out.size() < 2) out.add("25");
                    out.set(0, String.valueOf(f));
                    out.set(1, String.valueOf(b));
                    Store.writeLines("pomodoro.txt", out);
                }
                presetInfo.setText("已改成 专注 " + f + " 分 · 休息 " + b + " 分");
                /* 第一项「当前 X + Y」跟着刷新，并把下拉复位回去，免得一直停在选过的档上 */
                presetBox.insertItemAt("当前 " + f + " + " + b, 0);
                presetBox.removeItemAt(1);
                presetBox.setSelectedIndex(0);
            }
        });
        presetRow.add(presetLbl);
        presetRow.add(presetBox);
        presetRow.add(presetInfo);
        JPanel presetWrap = new JPanel(new BorderLayout());
        presetWrap.setOpaque(false);
        presetWrap.add(presetRow, BorderLayout.WEST);
        list.add(presetWrap);
        list.add(Box.createVerticalStrut(6));
        JLabel presetTip = new JLabel("换预设会同时改专注和休息时长；番茄钟正在计时时会提示下一轮生效");
        presetTip.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
        presetTip.setForeground(Theme.muted());
        presetTip.setAlignmentX(Component.LEFT_ALIGNMENT);
        list.add(presetTip);
        list.add(Box.createVerticalStrut(20));

        /* ===== 备忘录标签 ===== */
        list.add(sectionTitle("备忘录标签"));
        list.add(Box.createVerticalStrut(8));

        final JComboBox<String> tagSrc = new JComboBox<String>();
        tagSrc.setPreferredSize(new Dimension(130, 30));
        final JTextField tagDst = new JTextField();
        tagDst.setPreferredSize(new Dimension(150, 30));
        tagDst.putClientProperty("JTextField.placeholderText", "新名字，留空=删掉");
        JButton tagApply = new JButton("改名 / 合并");
        final JLabel tagInfo = new JLabel(" ");
        tagInfo.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
        tagInfo.setForeground(Theme.muted());

        final Runnable[] refreshTags = new Runnable[1];
        refreshTags[0] = new Runnable() {
            public void run() {
                MemoPanel mp = memoPanelForSettings();
                tagSrc.removeAllItems();
                if (mp == null) {
                    tagInfo.setText("备忘录页还没准备好，稍后回来再看");
                    return;
                }
                java.util.Map<String, Integer> counts = mp.tagCounts();
                for (java.util.Map.Entry<String, Integer> e : counts.entrySet()) {
                    tagSrc.addItem(e.getKey() + " (" + e.getValue() + ")");
                }
                if (counts.isEmpty()) {
                    tagInfo.setText("现在还没有任何标签，去备忘录给几条打上标签再来");
                } else {
                    tagInfo.setText("共 " + counts.size() + " 个标签，选中一个改名字；新名字已经存在就等于合并");
                }
            }
        };
        refreshTags[0].run();

        tagApply.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                MemoPanel mp = memoPanelForSettings();
                if (mp == null) {
                    Main.notifyMsg("改不了", "备忘录页还没准备好");
                    return;
                }
                Object it = tagSrc.getSelectedItem();
                if (it == null) {
                    Main.notifyMsg("还没有标签", "先去备忘录给几条打上标签");
                    return;
                }
                String item = String.valueOf(it);
                String from = item;
                int p = item.lastIndexOf(" (");
                if (p > 0) from = item.substring(0, p);
                String to = tagDst.getText().trim();
                if (to.equalsIgnoreCase(from)) {
                    Main.notifyMsg("名字没变", "新名字和原来一样，什么都没改");
                    return;
                }
                int n = mp.renameTag(from, to);
                if (n <= 0) {
                    Main.notifyMsg("没改成", "没有条目在用「" + from + "」了");
                } else if (to.isEmpty()) {
                    Main.notifyMsg("已清掉标签", "「" + from + "」从 " + n + " 条备忘上移除了");
                } else {
                    Main.notifyMsg("标签已更新", "「" + from + "」→「" + to + "」，改了 " + n + " 条");
                }
                tagDst.setText("");
                MemoPanel live = Main.page(MemoPanel.class);
                if (live != null && live != mp) live.reloadFromDisk();
                refreshTags[0].run();
            }
        });

        JPanel tagRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        tagRow.setOpaque(false);
        tagRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel tagSrcLbl = new JLabel("现有标签");
        tagSrcLbl.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        JLabel tagDstLbl = new JLabel("改成");
        tagDstLbl.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        tagRow.add(tagSrcLbl);
        tagRow.add(tagSrc);
        tagRow.add(tagDstLbl);
        tagRow.add(tagDst);
        tagRow.add(tagApply);
        JPanel tagWrap = new JPanel(new BorderLayout());
        tagWrap.setOpaque(false);
        tagWrap.add(tagRow, BorderLayout.WEST);
        list.add(tagWrap);
        list.add(Box.createVerticalStrut(6));
        tagInfo.setAlignmentX(Component.LEFT_ALIGNMENT);
        list.add(tagInfo);
        list.add(Box.createVerticalStrut(20));

        /* ===== 数据 ===== */
        list.add(sectionTitle("数据"));
        list.add(Box.createVerticalStrut(8));

        JPanel dataRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        dataRow.setOpaque(false);
        JButton openBtn = new JButton("打开数据目录");
        openBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                openDataDir();
            }
        });
        JButton backupBtn = new JButton("导出到桌面");
        backupBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                backupData();
            }
        });
        JButton manageBtn = new JButton("备份管理");
        manageBtn.setToolTipText("自动快照按天存，误删/改错了可以回滚到前几天");
        manageBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                openBackupDialog();
            }
        });
        JButton clearBtn = new JButton("清空数据");
        clearBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                clearData();
            }
        });
        dataRow.add(openBtn);
        dataRow.add(backupBtn);
        dataRow.add(manageBtn);
        dataRow.add(clearBtn);
        JPanel dataWrap = new JPanel(new BorderLayout(0, 6));
        dataWrap.setOpaque(false);
        dataWrap.add(dataRow, BorderLayout.NORTH);
        JLabel backupInfo = new JLabel(backupStatusText());
        backupInfo.setFont(Ui.font(11, false));
        backupInfo.setForeground(Theme.muted());
        backupInfo.setAlignmentX(Component.LEFT_ALIGNMENT);
        dataWrap.add(backupInfo, BorderLayout.SOUTH);
        list.add(dataWrap);
        list.add(Box.createVerticalStrut(20));

        /* ===== 导入记忆 ===== */
        list.add(sectionTitle("导入记忆"));
        list.add(Box.createVerticalStrut(8));
        final JComboBox<String> mapBox = new JComboBox<String>();
        mapBox.setPreferredSize(new Dimension(210, 28));
        mapBox.setFont(Ui.font(12, false));
        final JComboBox<String> catBox = new JComboBox<String>();
        catBox.setPreferredSize(new Dimension(96, 28));
        catBox.setFont(Ui.font(12, false));
        for (String c : LedgerPanel.allCats()) catBox.addItem(c);

        final List<String[]> mapData = LedgerPanel.loadMap();
        for (String[] e : mapData) mapBox.addItem(e[0] + "   →   " + e[1]);
        final JLabel mapInfo = new JLabel();
        mapInfo.setFont(Ui.font(11, false));
        mapInfo.setForeground(Theme.muted());

        final Runnable[] reloadMap = new Runnable[1];
        reloadMap[0] = new Runnable() {
            public void run() {
                List<String[]> now = LedgerPanel.loadMap();
                mapBox.removeAllItems();
                for (String[] e : now) mapBox.addItem(e[0] + "   →   " + e[1]);
                mapInfo.setText(now.isEmpty()
                        ? "还没有记忆 · 在「记账本 → 导入」的预览表里改分类，就会自动攒下来"
                        : "共 " + now.size() + " 条 · 改过的分类，下次导入同样的写法会自动用上");
                boolean has = !now.isEmpty();
                mapBox.setEnabled(has);
                catBox.setEnabled(has);
            }
        };
        mapBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int i = mapBox.getSelectedIndex();
                List<String[]> now = LedgerPanel.loadMap();
                if (i < 0 || i >= now.size()) return;
                String c = now.get(i)[1];
                for (int k = 0; k < catBox.getItemCount(); k++) {
                    if (c.equals(catBox.getItemAt(k))) {
                        catBox.setSelectedIndex(k);
                        break;
                    }
                }
            }
        });

        JButton changeCatBtn = new JButton("改成这个分类");
        changeCatBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int i = mapBox.getSelectedIndex();
                List<String[]> now = LedgerPanel.loadMap();
                if (i < 0 || i >= now.size()) return;
                String kw = now.get(i)[0];
                String cat = (String) catBox.getSelectedItem();
                if (cat == null) return;
                LedgerPanel.setCat(kw, cat);
                reloadMap[0].run();
                if (mapBox.getItemCount() > i) mapBox.setSelectedIndex(i);
                Main.notifyMsg("记忆已更新", "「" + kw + "」以后都归类到「" + cat + "」");
            }
        });
        JButton forgetBtn = new JButton("删除这条");
        forgetBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int i = mapBox.getSelectedIndex();
                List<String[]> now = LedgerPanel.loadMap();
                if (i < 0 || i >= now.size()) return;
                String kw = now.get(i)[0];
                int r = JOptionPane.showConfirmDialog(SettingsPanel.this,
                        "以后导入「" + kw + "」不再自动套用记忆分类（仍会按规则猜）。删掉这条吗？",
                        "删除记忆", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (r != JOptionPane.YES_OPTION) return;
                LedgerPanel.forget(kw);
                reloadMap[0].run();
                Main.notifyMsg("记忆已删除", "「" + kw + "」不再自动归类");
            }
        });

        JPanel mapRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        mapRow.setOpaque(false);
        mapRow.add(mapBox);
        mapRow.add(new JLabel("改成"));
        mapRow.add(catBox);
        mapRow.add(changeCatBtn);
        mapRow.add(forgetBtn);
        JPanel mapWrap = new JPanel(new BorderLayout(0, 6));
        mapWrap.setOpaque(false);
        mapWrap.add(mapRow, BorderLayout.NORTH);
        mapWrap.add(mapInfo, BorderLayout.SOUTH);
        list.add(mapWrap);
        reloadMap[0].run();
        list.add(Box.createVerticalStrut(20));

        /* ===== 关于 ===== */
        list.add(sectionTitle("关于"));
        list.add(Box.createVerticalStrut(8));
        JLabel about = new JLabel("小工具箱 PC " + Main.VERSION + "  ·  概览 / 日程提醒 / 计算器 / 习惯打卡 / 记账本 / 备忘录 / 计数器 / 番茄钟 / 倒计时 / 工具 / 统计 / 设置");
        about.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
        about.setForeground(Theme.muted());
        about.setAlignmentX(Component.LEFT_ALIGNMENT);
        list.add(about);

        JScrollPane sp = new JScrollPane(list);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getVerticalScrollBar().setUnitIncrement(16);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        add(sp, BorderLayout.CENTER);
    }

    /** 设置页里改标签用：优先拿主窗里那个备忘录页，拿不到就临时开一个只用来读写文件的 */
    private MemoPanel memoPanelForSettings() {
        MemoPanel live = Main.page(MemoPanel.class);
        if (live != null) {
            tempMemo = null;
            return live;
        }
        if (tempMemo == null) tempMemo = new MemoPanel();
        else tempMemo.reloadFromDisk();
        return tempMemo;
    }

    /** 垂直堆叠布局：每行铺满宽度，按首选高度排列 */
    private static class StackLayout implements LayoutManager {
        public void addLayoutComponent(String name, Component comp) {
        }

        public void removeLayoutComponent(Component comp) {
        }

        public Dimension preferredLayoutSize(Container parent) {
            Insets in = parent.getInsets();
            int w = 0;
            int h = 0;
            for (Component c : parent.getComponents()) {
                if (!c.isVisible()) continue;
                Dimension d = c.getPreferredSize();
                w = Math.max(w, d.width);
                h += d.height;
            }
            return new Dimension(w + in.left + in.right, h + in.top + in.bottom);
        }

        public Dimension minimumLayoutSize(Container parent) {
            return preferredLayoutSize(parent);
        }

        public void layoutContainer(Container parent) {
            Insets in = parent.getInsets();
            int y = in.top;
            int w = parent.getWidth() - in.left - in.right;
            for (Component c : parent.getComponents()) {
                if (!c.isVisible()) continue;
                int h = c.getPreferredSize().height;
                c.setBounds(in.left, y, w, h);
                y += h;
            }
        }
    }

    /** 让内容宽度跟随滚动视口，避免整体居中 */
    private static class ScrollablePanel extends JPanel implements Scrollable {
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    private JPanel option(JCheckBox box) {
        box.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 14));
        box.setOpaque(false);
        box.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrap.add(box, BorderLayout.WEST);
        return wrap;
    }

    private JPanel sectionTitle(String s) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel bar = new JPanel();
        bar.setBackground(Ui.PRIMARY);
        bar.setPreferredSize(new Dimension(3, 16));
        JLabel l = new JLabel(s);
        l.setFont(Ui.font(14, true));
        l.setForeground(Theme.text());
        p.add(bar);
        p.add(l);
        return p;
    }

    private boolean readBool(String key, boolean def) {
        List<String> l = Store.readLines("settings.txt");
        for (String line : l) {
            String[] p = line.split("=", 2);
            if (p.length == 2 && p[0].trim().equals(key)) {
                return p[1].trim().equals("1");
            }
        }
        return def;
    }

    private void save(String key, boolean value) {
        List<String> l = Store.readLines("settings.txt");
        List<String> out = new ArrayList<String>(l);
        boolean found = false;
        for (int i = 0; i < out.size(); i++) {
            String[] p = out.get(i).split("=", 2);
            if (p.length == 2 && p[0].trim().equals(key)) {
                out.set(i, key + "=" + (value ? 1 : 0));
                found = true;
                break;
            }
        }
        if (!found) out.add(key + "=" + (value ? 1 : 0));
        Store.writeLines("settings.txt", out);
    }

    private File dataDir() {
        /* 跟着 Store.dir() 走：正常是 用户目录/ToolboxPC，测试/验证时可以切到临时目录 */
        return Store.dir();
    }

    private void openDataDir() {
        try {
            File dir = dataDir();
            if (!dir.exists()) dir.mkdirs();
            Desktop.getDesktop().open(dir);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "无法打开目录: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void backupData() {
        try {
            File src = dataDir();
            if (!src.exists()) src.mkdirs();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmm");
            File dst = new File(new File(System.getProperty("user.home"), "Desktop"),
                    "小工具箱数据备份-" + sdf.format(new Date()));
            copyDir(src, dst);
            JOptionPane.showMessageDialog(this, "备份完成：\n" + dst.getAbsolutePath());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "备份失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void copyDir(File src, File dst) throws IOException {
        if (dst.exists()) return;
        dst.mkdirs();
        File[] files = src.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) continue;
            Files.copy(f.toPath(), new File(dst, f.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** 数据区下面那行小字：有多少份自动快照、最近一份是哪天 */
    public static String backupStatusText() {
        List<String> bs = Backup.list();
        if (bs.isEmpty()) return "自动备份：每天第一次打开留一份，现在还没有快照";
        return "自动备份：每天第一次打开留一份 · 现有 " + bs.size() + " 份 · 最近 " + bs.get(0);
    }

    /** 备份管理弹窗：看有哪些快照、手动备份一份、回滚到选中那份 */
    void openBackupDialog() {
        JDialog dlg = buildBackupDialog();
        dlg.setVisible(true);
    }

    /** 从别的页面（统计页的数据卡）点过来：先切到设置页，再把备份管理弹出来 */
    public static void openBackupDialogFromAnywhere() {
        SettingsPanel sp = Main.page(SettingsPanel.class);
        if (sp == null) return;
        Main.go("settings");
        javax.swing.SwingUtilities.invokeLater(() -> sp.openBackupDialog());
    }

    /** 只建不显示，方便测试与出图核对 */
    JDialog buildBackupDialog() {
        final JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this), "备份管理",
                Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setLayout(new BorderLayout(0, 8));
        dlg.getRootPane().setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        JLabel tip = new JLabel("<html>每天第一次打开程序时自动留一份快照，最多存 10 份。"
                + "回滚会用选中的那份覆盖当前数据，覆盖前会先把现在的状态另存一份。</html>");
        tip.setFont(Ui.font(11, false));
        tip.setForeground(Theme.muted());
        tip.setAlignmentX(Component.LEFT_ALIGNMENT);
        tip.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));

        /* 备份范围：哪几个模块进快照，自己勾（改了从下一份开始生效） */
        final JLabel pickInfo = new JLabel("现在备份：" + Backup.pickText() + "（下一份开始生效）");
        pickInfo.setFont(Ui.font(11, false));
        pickInfo.setForeground(Theme.muted());
        pickInfo.setAlignmentX(Component.LEFT_ALIGNMENT);
        final List<JCheckBox> pickBoxes = new ArrayList<JCheckBox>();
        JPanel pickRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        pickRow.setOpaque(false);
        pickRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel pickTitle = new JLabel("备份内容");
        pickTitle.setFont(Ui.font(11, false));
        pickTitle.setForeground(Theme.muted());
        pickRow.add(pickTitle);
        for (final String[] g : Backup.GROUPS) {
            final JCheckBox cb = new JCheckBox(g[1] + (Backup.countIn(g[0]) > 0 ? "" : "（空）"));
            cb.setFont(Ui.font(11, false));
            cb.setOpaque(false);
            cb.setSelected(Backup.picked(g[0]));
            cb.setToolTipText("勾上就把「" + g[1] + "」的数据一起备份");
            cb.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    List<String> keys = new ArrayList<String>();
                    for (int i = 0; i < pickBoxes.size(); i++) {
                        if (pickBoxes.get(i).isSelected()) keys.add(Backup.GROUPS[i][0]);
                    }
                    Backup.setPick(keys);
                    pickInfo.setText("现在备份：" + Backup.pickText() + "（下一份开始生效）");
                    pickInfo.setForeground(keys.isEmpty() ? new Color(0xC62828) : Theme.muted());
                }
            });
            pickBoxes.add(cb);
            pickRow.add(cb);
        }

        JPanel north = new JPanel();
        north.setOpaque(false);
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(tip);
        north.add(Box.createVerticalStrut(6));
        north.add(pickRow);
        north.add(Box.createVerticalStrut(2));
        north.add(pickInfo);

        final javax.swing.DefaultListModel<String> lm = new javax.swing.DefaultListModel<String>();
        final List<String> names = new ArrayList<String>(Backup.list());
        for (String n : names) lm.addElement(n + "    " + Backup.describe(n));
        final JList<String> list = new JList<String>(lm);
        list.setFont(Ui.font(12, false));
        list.setFixedCellHeight(26);
        if (!names.isEmpty()) list.setSelectedIndex(0);
        JScrollPane sp = new JScrollPane(list);
        sp.setPreferredSize(new Dimension(430, 250));

        final JLabel info = new JLabel(names.isEmpty() ? "还没有快照，点「立即备份」就能留一份" : " ");
        info.setFont(Ui.font(11, false));
        info.setForeground(Theme.muted());

        JButton nowBtn = Ui.secondary("立即备份一份");
        nowBtn.setPreferredSize(new Dimension(120, 32));
        JButton restoreBtn = Ui.primary("回滚到选中");
        restoreBtn.setPreferredSize(new Dimension(116, 32));
        JButton openBtn = new JButton("打开备份目录");
        JButton closeBtn = new JButton("关闭");
        closeBtn.setPreferredSize(new Dimension(76, 32));

        final Runnable refresh = new Runnable() {
            public void run() {
                names.clear();
                names.addAll(Backup.list());
                lm.clear();
                for (String n : names) lm.addElement(n + "    " + Backup.describe(n));
                if (!names.isEmpty()) list.setSelectedIndex(0);
                info.setText(names.isEmpty() ? "还没有快照，点「立即备份」就能留一份" : " ");
            }
        };

        nowBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String n = Backup.manual();
                if (n == null) {
                    String msg = Backup.pick().isEmpty()
                            ? "「备份内容」一个都没勾，先勾上要备份的模块"
                            : "数据目录里还没有可备份的内容";
                    JOptionPane.showMessageDialog(dlg, msg, "备份", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                refresh.run();
                info.setText("已备份：" + n);
            }
        });
        openBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    Desktop.getDesktop().open(Backup.root());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dlg, "打不开备份目录: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        restoreBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int idx = list.getSelectedIndex();
                if (idx < 0 || idx >= names.size()) {
                    info.setText("先在列表里选一份快照");
                    return;
                }
                final String pick = names.get(idx);
                int r = JOptionPane.showConfirmDialog(dlg,
                        "把数据回滚到 " + pick + "？\n当前状态会先另存一份 pre-restore-" + "…，不会直接丢。",
                        "回滚确认", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (r != JOptionPane.YES_OPTION) return;
                int n = Backup.restore(pick);
                if (n < 0) {
                    info.setText("这份快照已经不在了");
                    return;
                }
                Main.reloadAll();
                refresh.run();
                Main.notifyMsg("回滚完成", "已从 " + pick + " 恢复 " + n + " 个文件");
                dlg.dispose();
            }
        });
        closeBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dlg.dispose();
            }
        });

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.add(nowBtn);
        right.add(openBtn);
        right.add(restoreBtn);
        right.add(closeBtn);
        JPanel south = new JPanel(new BorderLayout(8, 0));
        south.add(info, BorderLayout.WEST);
        south.add(right, BorderLayout.EAST);

        dlg.add(north, BorderLayout.NORTH);
        dlg.add(sp, BorderLayout.CENTER);
        dlg.add(south, BorderLayout.SOUTH);
        dlg.pack();
        dlg.setLocationRelativeTo(this);
        return dlg;
    }

    private void clearData() {
        int r = JOptionPane.showConfirmDialog(this,
                "将删除所有 记账、备忘录、计数器、番茄钟、日程 数据，且不可恢复。\n确定继续吗？",
                "清空数据", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r != JOptionPane.YES_OPTION) return;
        File dir = dataDir();
        String[] names = {"ledger.txt", "memo.txt", "counter.txt", "pomodoro.txt", "schedule.txt",
                "pomodoro_log.txt", "countdown_log.txt", "todo.txt", "countdown_presets.txt"};
        for (String n : names) {
            File f = new File(dir, n);
            if (f.exists()) f.delete();
        }
        JOptionPane.showMessageDialog(this, "数据已清空（设置保留）");
    }

    /* ---------- 导航排序 ---------- */

    private void renderNavOrder() {
        if (navOrderBox == null) return;
        navOrderBox.removeAll();
        List<String> order = Main.navOrder();
        int no = 0;
        for (int i = 0; i < order.size(); i++) {
            final int idx = i;
            String key = order.get(i);

            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
            row.setAlignmentX(Component.LEFT_ALIGNMENT);

            boolean enabled = Main.isEnabled(key);
            boolean pinned = "settings".equals(key);
            String label = pinned
                    ? Main.navLabel(key) + "（固定在左下最底部，不参与排序）"
                    : ((++no) + ". " + Main.navLabel(key) + (enabled ? "" : "（已停用）"));
            JLabel name = new JLabel(label);
            name.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
            name.setForeground((enabled || pinned) ? Theme.text() : Theme.muted());
            row.add(name, BorderLayout.CENTER);

            if (pinned) {
                /* 设置钉死在底部，不给上/下移，也拖不动；行高与其它行保持一致，避免看起来被压扁 */
                row.setPreferredSize(new Dimension(10, 28));
                navOrderBox.add(row);
                if (i < order.size() - 1) navOrderBox.add(Box.createVerticalStrut(2));
                continue;
            }

            JPanel btns = new JPanel(new GridLayout(1, 2, 6, 0));
            btns.setOpaque(false);
            btns.setPreferredSize(new Dimension(92, 28));
            JButton up = new JButton("↑");
            JButton down = new JButton("↓");
            up.setFont(Ui.font(13, true));
            down.setFont(Ui.font(13, true));
            up.setFocusPainted(false);
            down.setFocusPainted(false);
            up.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            down.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            up.setEnabled(i > 0);
            down.setEnabled(i < order.size() - 1
                    && !"settings".equals(order.get(i + 1)));
            up.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    moveItem(idx, -1);
                }
            });
            down.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    moveItem(idx, 1);
                }
            });
            btns.add(up);
            btns.add(down);
            row.add(btns, BorderLayout.EAST);
            attachDrag(row, idx);

            navOrderBox.add(row);
            if (i < order.size() - 1) navOrderBox.add(Box.createVerticalStrut(2));
        }
        navOrderBox.revalidate();
        navOrderBox.repaint();
    }

    private void moveItem(int idx, int delta) {
        List<String> order = Main.navOrder();
        if (idx < 0 || idx >= order.size()) return;
        if ("settings".equals(order.get(idx))) return;   // 设置固定底部，不参与排序
        int j = idx + delta;
        if (j < 0 || j >= order.size()) return;
        if ("settings".equals(order.get(j))) return;
        String tmp = order.get(idx);
        order.set(idx, order.get(j));
        order.set(j, tmp);
        saveStr("navOrder", String.join(",", order));
        Main.rebuildNav();
        renderNavOrder();
    }

    /* ---------- 拖动排序 ---------- */

    /** 可排序区间末位（"设置"钉底，不算在内） */
    private int sortableLast(List<String> order) {
        int last = order.size() - 1;
        if (last >= 0 && "settings".equals(order.get(last))) last--;
        return last;
    }

    /** 给一行挂上"按住拖动换位"的监听 */
    private void attachDrag(final JPanel row, final int idx) {
        row.putClientProperty("navIdx", Integer.valueOf(idx));
        row.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        row.setToolTipText("按住这一行上下拖动，松手就换到这个位置");
        row.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) return;
                dragFrom = idx;
                dragPressY = e.getY();
                dragHover = idx;
                paintDragState();
            }

            public void mouseReleased(MouseEvent e) {
                if (dragFrom < 0) return;
                int from = dragFrom;
                int to = targetIndexAt(SwingUtilities.convertPoint(row, e.getPoint(), navOrderBox).y);
                dragFrom = -1;
                dragHover = -1;
                paintDragState();
                if (to >= 0 && to != from) dragTo(from, to);
            }
        });
        row.addMouseMotionListener(new MouseAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (dragFrom < 0) return;
                int t = targetIndexAt(SwingUtilities.convertPoint(row, e.getPoint(), navOrderBox).y);
                if (t >= 0 && t != dragHover) {
                    dragHover = t;
                    paintDragState();
                }
            }
        });
    }

    /** 鼠标落点（navOrderBox 坐标系）对应的目标行索引；落在行间缝隙就取最近的一行 */
    int targetIndexAt(int y) {
        int best = -1, bestDist = Integer.MAX_VALUE;
        for (Component c : navOrderBox.getComponents()) {
            if (!(c instanceof JPanel)) continue;
            Object v = ((JPanel) c).getClientProperty("navIdx");
            if (v == null) continue;
            int idx = ((Integer) v).intValue();
            int top = c.getY(), h = c.getHeight();
            if (h <= 0) continue;
            if (y >= top && y < top + h) return idx;
            int d = Math.abs(y - (top + h / 2));
            if (d < bestDist) {
                bestDist = d;
                best = idx;
            }
        }
        return best;
    }

    /** 拖动中：被按住的行与目标行都铺一层悬停底色，松手即清除 */
    private void paintDragState() {
        if (navOrderBox == null) return;
        for (Component c : navOrderBox.getComponents()) {
            if (!(c instanceof JPanel)) continue;
            JPanel r = (JPanel) c;
            Object v = r.getClientProperty("navIdx");
            if (v == null) continue;
            int i = ((Integer) v).intValue();
            boolean active = dragFrom >= 0 && (i == dragFrom || i == dragHover);
            if (active) {
                r.setOpaque(true);
                r.setBackground(Ui.navHoverBg());
            } else {
                r.setOpaque(false);
            }
            r.repaint();
        }
    }

    /** 把第 from 项整段搬到第 to 位（设置不参与，越界自动夹到可排序区间内） */
    void dragTo(int from, int to) {
        List<String> order = Main.navOrder();
        int last = sortableLast(order);
        if (from < 0 || from > last) return;
        if (to < 0) to = 0;
        if (to > last) to = last;
        if (from == to) return;
        String key = order.remove(from);
        order.add(to, key);
        saveStr("navOrder", String.join(",", order));
        Main.rebuildNav();
        renderNavOrder();
    }

    private void saveStr(String key, String value) {
        List<String> out = new ArrayList<String>(Store.readLines("settings.txt"));
        boolean found = false;
        for (int i = 0; i < out.size(); i++) {
            if (out.get(i).startsWith(key + "=")) {
                out.set(i, key + "=" + value);
                found = true;
                break;
            }
        }
        if (!found) out.add(key + "=" + value);
        Store.writeLines("settings.txt", out);
    }
}
