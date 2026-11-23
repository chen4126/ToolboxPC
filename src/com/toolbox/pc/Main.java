package com.toolbox.pc;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {

    public static final String VERSION = "v2.4";
    public static boolean sTrayOnClose = true;
    private static Main self;

    private static TrayIcon sTray;
    private static boolean sTrayOk = false;

    private JFrame frame;
    private JPanel navPanel;
    private JScrollPane navScroll;
    private JPanel cards;
    private CardLayout cardLayout;
    private StatsPanel statsPanel;
    private OverviewPanel overviewPanel;
    private String currentKey = "overview";

    private static final Color PRIMARY = Ui.PRIMARY;
    private static final String[] DEFAULT_ORDER =
            {"overview", "pomodoro", "schedule", "habit", "ledger", "memo", "counter",
                    "calc", "countdown", "toolbox", "stats", "settings"};

    public static void main(String[] args) {
        Theme.init();
        Ui.installDefaults();
        try {
            Backup.daily();   // 每天第一次启动留一份当日数据快照（最多留 10 份）
        } catch (Throwable ignored) {
        }
        SwingUtilities.invokeLater(() -> new Main().create());
    }

    private void create() {
        self = this;
        frame = new JFrame("小工具箱 PC版");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (sTrayOnClose) hideToTray();
                else System.exit(0);
            }
        });
        frame.getContentPane().setBackground(Theme.panelBg());
        frame.setAlwaysOnTop(Theme.readBool("pin", false));

        navPanel = new JPanel();
        buildNavItems(navPanel);
        navScroll = wrapNavScroll(navPanel);
        frame.add(navScroll, BorderLayout.WEST);

        cardLayout = new CardLayout();
        cards = new JPanel(cardLayout);
        cards.setBackground(Theme.panelBg());
        cards.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        overviewPanel = new OverviewPanel();
        cards.add(wrapCard(overviewPanel, "概览", "今日数据 · 快捷入口", "overview"), "overview");
        cards.add(wrapCard(new CalculatorPanel(), "计算器", "四则运算 · 百分比", "calc"), "calc");
        cards.add(wrapCard(new LedgerPanel(), "记账本", "收支记录 · 结余统计", "ledger"), "ledger");
        cards.add(wrapCard(new MemoPanel(), "备忘录", "随手记录 · 本地保存", "memo"), "memo");
        cards.add(wrapCard(new CounterPanel(), "计数器", "库存点算 · 步长调节", "counter"), "counter");
        cards.add(wrapCard(new PomodoroPanel(), "番茄钟", "专注计时 · 到点提醒", "pomodoro"), "pomodoro");
        cards.add(wrapCard(new SchedulePanel(), "日程提醒", "到点强提醒 · 每周/每月/工作日", "schedule"), "schedule");
        cards.add(wrapCard(new HabitPanel(), "习惯打卡", "每天一下 · 连续天数 · 本周节奏", "habit"), "habit");
        cards.add(wrapCard(new CountDownPanel(), "倒计时", "多预设并存 · 可并行计时", "countdown"), "countdown");
        cards.add(wrapCard(new ToolboxPanel(), "工具", "剪贴板速记 · 文件整理助手", "toolbox"), "toolbox");
        statsPanel = new StatsPanel();
        cards.add(wrapCard(statsPanel, "统计", "数据记录 · 汇总", "stats"), "stats");
        cards.add(wrapCard(new SettingsPanel(), "设置", "外观 · 模块 · 排序 · 提醒 · 数据", "settings"), "settings");
        frame.add(cards, BorderLayout.CENTER);

        frame.setSize(1140, 760);
        frame.setMinimumSize(new Dimension(980, 660));
        frame.setLocationRelativeTo(null);
        try {
            frame.setIconImage(AppIcon.image(128));
        } catch (Exception ignored) {
        }
        initGlass();
        setupTray();
        Theme.watchSystem(frame);
        frame.setVisible(true);
        startFade();
        selectNav(firstEnabledKey(), false);
        MiniBoard.restoreIfOn();
        EyeCare.start();
        HealthCare.start();
        ClipNotes.start();
    }

    /* ---------- 过渡与图标 ---------- */

    private JPanel glass;

    private void initGlass() {
        glass = new JPanel();
        glass.setOpaque(false);
        glass.setLayout(null);
        frame.setGlassPane(glass);
        glass.setVisible(false);
    }

    /** 启动：内容区从背景色中浮现 */
    private void startFade() {
        if (glass == null || !animEnabled()) return;
        int w = cards.getWidth();
        int h = cards.getHeight();
        if (w <= 0 || h <= 0) return;

        final Color bgc = Theme.panelBg();
        final float[] alpha = {1f};
        final JComponent mask = new JComponent() {
            protected void paintComponent(Graphics g) {
                if (alpha[0] <= 0f) return;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha[0]));
                g2.setColor(bgc);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        mask.setBounds(SwingUtilities.convertPoint(cards, 0, 0, glass).x,
                SwingUtilities.convertPoint(cards, 0, 0, glass).y, w, h);
        glass.removeAll();
        glass.add(mask);
        glass.setVisible(true);

        final long start = System.currentTimeMillis();
        javax.swing.Timer t = new javax.swing.Timer(16, null);
        t.addActionListener(e -> {
            float p = (System.currentTimeMillis() - start) / 420f;
            if (p >= 1f) {
                t.stop();
                glass.setVisible(false);
                glass.removeAll();
            } else {
                alpha[0] = 1f - p;
                mask.repaint();
            }
        });
        t.start();
    }

    /** 切换页面：旧画面淡出，露出新页面 */
    private void playSwitchFade(BufferedImage img, int x, int y) {
        if (glass == null || img == null) return;
        final float[] alpha = {1f};
        final JComponent mask = new JComponent() {
            protected void paintComponent(Graphics g) {
                if (alpha[0] <= 0f) return;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha[0]));
                g2.drawImage(img, 0, 0, null);
                g2.dispose();
            }
        };
        mask.setBounds(x, y, img.getWidth(), img.getHeight());
        glass.removeAll();
        glass.add(mask);
        glass.setVisible(true);

        final long start = System.currentTimeMillis();
        javax.swing.Timer t = new javax.swing.Timer(16, null);
        t.addActionListener(e -> {
            float p = (System.currentTimeMillis() - start) / 200f;
            if (p >= 1f) {
                t.stop();
                glass.setVisible(false);
                glass.removeAll();
            } else {
                alpha[0] = 1f - p;
                mask.repaint();
            }
        });
        t.start();
    }

    private BufferedImage snapshot(JComponent c) {
        if (c.getWidth() <= 0 || c.getHeight() <= 0) return null;
        BufferedImage img = new BufferedImage(c.getWidth(), c.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            c.paint(g);
        } finally {
            g.dispose();
        }
        return img;
    }

    /** 内容卡片：圆角 + 投影标题区 + 模块徽标 */
    private JPanel wrapCard(JPanel inner, String title, String subTitle, String key) {
        inner.setOpaque(false);

        ShadowCard card = new ShadowCard(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0, 0, 8, 8),
                BorderFactory.createEmptyBorder(24, 26, 22, 26)));

        JPanel head = new JPanel(new BorderLayout(0, 14));
        head.setOpaque(false);

        JPanel titleRow = new JPanel(new BorderLayout(14, 0));
        titleRow.setOpaque(false);
        titleRow.add(cardBadge(key), BorderLayout.WEST);

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        JLabel t = new JLabel(title);
        t.setFont(Ui.font(20, true));
        t.setForeground(Theme.text());
        JLabel s = new JLabel(subTitle);
        s.setFont(Ui.font(11, false));
        s.setForeground(Theme.muted());
        titles.add(t);
        titles.add(Box.createVerticalStrut(4));
        titles.add(s);

        JPanel titlesWrap = new JPanel(new GridBagLayout());
        titlesWrap.setOpaque(false);
        GridBagConstraints tg = new GridBagConstraints();
        tg.anchor = GridBagConstraints.WEST;
        tg.weightx = 1;
        tg.fill = GridBagConstraints.HORIZONTAL;
        titlesWrap.add(titles, tg);
        titleRow.add(titlesWrap, BorderLayout.CENTER);

        head.add(titleRow, BorderLayout.NORTH);
        head.add(Ui.hline(), BorderLayout.SOUTH);

        card.add(head, BorderLayout.NORTH);
        card.add(inner, BorderLayout.CENTER);
        return card;
    }

    /** 模块徽标：淡色圆角底 + 彩色线性图标 */
    private JLabel cardBadge(String key) {
        Color c = keyColor(key);
        BufferedImage img = new BufferedImage(42, 42, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), Theme.isDark() ? 40 : 26));
        g.fillRoundRect(0, 0, 42, 42, 12, 12);
        Icon ic = IconFactory.nav(key, c);
        g.drawImage(((ImageIcon) ic).getImage(), 11, 11, null);
        g.dispose();
        return new JLabel(new ImageIcon(img));
    }

    private static Color keyColor(String key) {
        switch (key) {
            case "overview": return new Color(0x4C6EF5);
            case "calc": return new Color(0x4C6EF5);
            case "ledger": return new Color(0x10B981);
            case "memo": return new Color(0xF59E0B);
            case "counter": return new Color(0xEF4444);
            case "pomodoro": return new Color(0x06B6D4);
            case "schedule": return new Color(0x8B5CF6);
            case "countdown": return new Color(0x14B8A6);
            case "habit": return new Color(0xF97316);
            case "toolbox": return new Color(0x0EA5E9);
            case "stats": return new Color(0x6366F1);
            case "settings": return new Color(0x64748B);
            default: return PRIMARY;
        }
    }

    private void addNav(JPanel nav, String label, final String key) {
        NavButton b = new NavButton(label);
        Color active = Theme.isDark() ? new Color(0xAFC0FF) : Ui.PRIMARY;
        b.setNavIcons(IconFactory.nav(key, Ui.navIcon()), IconFactory.nav(key, active));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        b.setPreferredSize(new Dimension(196, 44));
        b.putClientProperty("navKey", key);
        b.addActionListener(e -> selectNav(key));
        if ("settings".equals(key)) b.setToolTipText("设置固定在左下最底部，不参与拖动排序");
        else installDragSort(b);
        nav.add(Box.createVerticalStrut(3));
        nav.add(b);
    }

    /** 拖动导航项调整顺序（松手后立即重排并保存） */
    private void installDragSort(final NavButton b) {
        MouseAdapter ma = new MouseAdapter() {
            private boolean dragging;
            private int startY;

            public void mousePressed(MouseEvent e) {
                startY = e.getY();
                dragging = false;
            }

            public void mouseDragged(MouseEvent e) {
                if (Math.abs(e.getY() - startY) > 8) dragging = true;
                if (dragging) b.setDragging(true);
            }

            public void mouseReleased(MouseEvent e) {
                boolean was = dragging;
                dragging = false;
                b.setDragging(false);
                if (!was) return;
                Point p = SwingUtilities.convertPoint(b, e.getPoint(), navPanel);
                moveNav((String) b.getClientProperty("navKey"), dropIndex(p.y));
            }
        };
        b.addMouseListener(ma);
        b.addMouseMotionListener(ma);
    }

    private int dropIndex(int y) {
        int idx = 0;
        for (Component c : navPanel.getComponents()) {
            if (!(c instanceof NavButton)) continue;
            /* 设置钉在底部，不参与排序落点 */
            if ("settings".equals(((JComponent) c).getClientProperty("navKey"))) continue;
            if (y > c.getY() + c.getHeight() / 2) idx++;
        }
        return idx;
    }

    private void moveNav(String key, int target) {
        if (key == null || "settings".equals(key)) return;   // 设置固定在最底部，拖它不生效
        List<String> all = navOrder();
        List<String> vis = new ArrayList<String>();
        for (String k : all) {
            if ("settings".equals(k)) continue;
            if (isEnabled(k)) vis.add(k);
        }
        int from = vis.indexOf(key);
        if (from < 0) return;
        if (target > from) target--;
        if (target < 0) target = 0;
        if (target > vis.size() - 1) target = vis.size() - 1;
        if (target == from) return;
        vis.remove(from);
        vis.add(target, key);
        /* 停用的模块按原顺序沉到最后，设置永远收尾 */
        List<String> order = new ArrayList<String>(vis);
        for (String k : all) {
            if (!vis.contains(k) && !"settings".equals(k)) order.add(k);
        }
        order.add("settings");
        saveNavOrder(order);
        rebuildNav();
        selectNav(key, false);
    }

    static void saveNavOrder(List<String> order) {
        List<String> out = new ArrayList<String>(Store.readLines("settings.txt"));
        boolean found = false;
        for (int i = 0; i < out.size(); i++) {
            if (out.get(i).startsWith("navOrder=")) {
                out.set(i, "navOrder=" + String.join(",", order));
                found = true;
                break;
            }
        }
        if (!found) out.add("navOrder=" + String.join(",", order));
        Store.writeLines("settings.txt", out);
    }

    /** 界面动效开关（设置页可关闭） */
    public static boolean animEnabled() {
        for (String line : Store.readLines("settings.txt")) {
            if (line.startsWith("anim=")) return line.substring("anim=".length()).trim().equals("1");
        }
        return true;
    }

    /* ---------- 模块启用 ---------- */

    /** 被停用的模块（settings.txt 的 disabled=key1,key2） */
    public static List<String> disabledKeys() {
        List<String> out = new ArrayList<String>();
        for (String line : Store.readLines("settings.txt")) {
            if (line.startsWith("disabled=")) {
                for (String k : line.substring("disabled=".length()).split(",")) {
                    String t = k.trim();
                    if (!t.isEmpty() && !out.contains(t)) out.add(t);
                }
            }
        }
        return out;
    }

    /** 设置模块永远可用，避免把自己关掉无法恢复 */
    public static boolean isEnabled(String key) {
        if ("settings".equals(key)) return true;
        return !disabledKeys().contains(key);
    }

    /** 设置页勾选/取消功能模块，立即重建导航 */
    public static void setEnabled(String key, boolean on) {
        if ("settings".equals(key)) return;
        List<String> dis = disabledKeys();
        if (on) dis.remove(key);
        else if (!dis.contains(key)) dis.add(key);
        List<String> out = new ArrayList<String>(Store.readLines("settings.txt"));
        boolean found = false;
        for (int i = 0; i < out.size(); i++) {
            if (out.get(i).startsWith("disabled=")) {
                out.set(i, "disabled=" + String.join(",", dis));
                found = true;
                break;
            }
        }
        if (!found) out.add("disabled=" + String.join(",", dis));
        Store.writeLines("settings.txt", out);
        rebuildNav();
    }

    /** 第一个可用的模块（启动页与停用当前页后的落点） */
    static String firstEnabledKey() {
        for (String k : navOrder()) {
            if (isEnabled(k)) return k;
        }
        return "settings";
    }

    /** 首页卡片点击跳转 */
    public static void go(String key) {
        if (self == null) return;
        if (!isEnabled(key)) {
            JOptionPane.showMessageDialog(self.frame,
                    "「" + navLabel(key) + "」已在设置中停用。\n可在「设置 → 功能模块」里重新启用。",
                    "模块已停用", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        self.selectNav(key);
    }

    /** 跨页联动：按类型取已创建的页面实例（页面外面包了一层卡片，需要递归找） */
    public static <T extends Component> T page(Class<T> type) {
        if (self == null || self.cards == null) return null;
        for (Component c : self.cards.getComponents()) {
            T t = findPage(c, type);
            if (t != null) return t;
        }
        return null;
    }

    private static <T extends Component> T findPage(Component c, Class<T> type) {
        if (type.isInstance(c)) return type.cast(c);
        if (c instanceof Container) {
            for (Component k : ((Container) c).getComponents()) {
                T t = findPage(k, type);
                if (t != null) return t;
            }
        }
        return null;
    }

    private void selectNav(String key) {
        selectNav(key, true);
    }

    private void selectNav(String key, boolean animate) {
        if (!isEnabled(key)) key = firstEnabledKey();
        String oldKey = currentKey;
        currentKey = key;
        for (Component c : navPanel.getComponents()) {
            if (c instanceof NavButton) {
                NavButton b = (NavButton) c;
                b.setSelectedNav(key.equals(b.getClientProperty("navKey")));
            }
        }
        if ("stats".equals(key) && statsPanel != null) statsPanel.refresh();
        if ("overview".equals(key) && overviewPanel != null) overviewPanel.refresh();
        if ("settings".equals(key)) refreshSettingsEye();

        boolean canFade = animate && animEnabled() && !key.equals(oldKey) && glass != null
                && !glass.isVisible() && cards.getWidth() > 0;
        if (canFade) {
            BufferedImage snap = snapshot(cards);
            Point pt = SwingUtilities.convertPoint(cards, 0, 0, glass);
            cardLayout.show(cards, key);
            playSwitchFade(snap, pt.x, pt.y);
        } else {
            if (glass != null) {
                glass.setVisible(false);
                glass.removeAll();
            }
            cardLayout.show(cards, key);
        }
    }

    private JLabel logoIcon() {
        BufferedImage img = new BufferedImage(36, 36, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0, 0, Ui.PRIMARY, 36, 36, new Color(0x7C4DFF)));
        g.fillRoundRect(0, 0, 36, 36, 11, 11);
        g.setColor(Color.WHITE);
        g.fillRoundRect(8, 8, 8, 8, 2, 2);
        g.fillRoundRect(20, 8, 8, 8, 2, 2);
        g.fillRoundRect(8, 20, 8, 8, 2, 2);
        g.setColor(new Color(255, 255, 255, 150));
        g.fillRoundRect(20, 20, 8, 8, 2, 2);
        g.dispose();
        return new JLabel(new ImageIcon(img));
    }

    private void setupTray() {
        if (!SystemTray.isSupported()) return;
        try {
            SystemTray tray = SystemTray.getSystemTray();
            BufferedImage img = AppIcon.image(16);
            sTray = new TrayIcon(img, "小工具箱 PC版");
            sTray.setImageAutoSize(true);
            PopupMenu menu = new PopupMenu();
            MenuItem show = new MenuItem("显示窗口");
            show.addActionListener(e -> showWindow());
            MenuItem board = new MenuItem("今日小窗");
            board.addActionListener(e -> SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    MiniBoard.toggle();
                }
            }));
            MenuItem exit = new MenuItem("退出");
            exit.addActionListener(e -> System.exit(0));
            menu.add(show);
            menu.add(board);
            menu.add(exit);
            sTray.setPopupMenu(menu);
            tray.add(sTray);
            sTrayOk = true;
        } catch (Exception ignored) {
        }
    }

    private void hideToTray() {
        if (sTrayOk) {
            frame.setVisible(false);
            notifyMsg("小工具箱", "已最小化到系统托盘，点击右下角图标可恢复");
        } else {
            frame.dispose();
        }
    }

    private void showWindow() {
        frame.setVisible(true);
        frame.toFront();
    }

    /**
     * 标题栏闪烁提醒：倒计时超时后用，闪 8 下自动恢复原标题。
     * 只在主窗口存在时生效，窗口最小化时任务栏悬停也能看到变化。
     */
    public static void flashTitle(String msg) {
        if (self == null || self.frame == null) return;
        final JFrame f = self.frame;
        final String base = "小工具箱 PC版";
        /* 最小化或收到托盘里时，先把窗口拉出来，否则标题栏根本看不见 */
        if (f.getState() == java.awt.Frame.ICONIFIED) f.setState(java.awt.Frame.NORMAL);
        if (!f.isVisible()) f.setVisible(true);
        f.toFront();
        final javax.swing.Timer t = new javax.swing.Timer(700, null);
        final int[] n = {0};
        t.addActionListener(e -> {
            n[0]++;
            if (n[0] > 8) {
                f.setTitle(base);
                t.stop();
                return;
            }
            f.setTitle(n[0] % 2 == 1 ? "● " + msg : base);
        });
        t.start();
    }

    /** 设置页的护眼数据行跟着计时刷新 */
    public static void refreshSettingsEye() {
        SettingsPanel sp = page(SettingsPanel.class);
        if (sp != null) sp.refreshEyeStat();
    }

    /** 统一通知入口：右下角自绘卡片，同一句话 2 秒内只弹一次（不再走系统托盘气泡） */    public static void notifyMsg(final String title, final String msg) {
        Toast.show(title, msg, new Runnable() {
            public void run() {
                showMain();
            }
        });
    }

    /** 带跳转的通知：点卡片直接切到指定页面，同时把主窗拉到前台 */
    public static void notifyJump(final String title, final String msg, final String navKey) {
        Toast.show(title, msg, new Runnable() {
            public void run() {
                go(navKey);
                showMain();
            }
        });
    }

    /** 备份回滚后用：让各页重新从磁盘读数据，导航顺序与设置也跟着重读 */
    public static void reloadAll() {
        OverviewPanel ov = page(OverviewPanel.class);
        if (ov != null) ov.refresh();
        StatsPanel st = page(StatsPanel.class);
        if (st != null) st.refresh();
        MemoPanel mp = page(MemoPanel.class);
        if (mp != null) mp.reloadFromDisk();
        LedgerPanel lp = page(LedgerPanel.class);
        if (lp != null) lp.reloadTable();
        HealthCare.reload();
        EyeCare.reload();
        rebuildNav();
    }

    /** 从桌面小窗/托盘把主窗口调出来 */
    public static void showMain() {        if (self != null) {
            self.frame.setVisible(true);
            self.frame.toFront();
            self.frame.requestFocus();
        }
    }

    static void applyThemeNow() {
        if (self == null) return;
        Theme.apply(self.frame);
        self.buildNavItems(self.navPanel);
        self.selectNav(self.currentKey);
    }

    static void setPinned(boolean on) {
        if (self != null) self.frame.setAlwaysOnTop(on);
    }

    /** 当前导航顺序（来自 settings.txt 的 navOrder，缺失项自动补齐） */
    public static List<String> navOrder() {
        List<String> order = new ArrayList<String>();
        List<String> def = Arrays.asList(DEFAULT_ORDER);
        for (String line : Store.readLines("settings.txt")) {
            if (line.startsWith("navOrder=")) {
                for (String k : line.substring("navOrder=".length()).split(",")) {
                    String t = k.trim();
                    if (def.contains(t) && !order.contains(t)) order.add(t);
                }
            }
        }
        for (String k : def) {
            if (!order.contains(k)) {
                int at = Math.max(0, Math.min(order.size(), def.indexOf(k)));
                order.add(at, k);
            }
        }
        return order;
    }

    public static String navLabel(String key) {
        switch (key) {
            case "overview": return "概览";
            case "calc": return "计算器";
            case "ledger": return "记账本";
            case "memo": return "备忘录";
            case "counter": return "计数器";
            case "pomodoro": return "番茄钟";
            case "schedule": return "日程提醒";
            case "countdown": return "倒计时";
            case "habit": return "习惯打卡";
            case "toolbox": return "工具";
            case "stats": return "统计";
            case "settings": return "设置";
            default: return key;
        }
    }

    /** 侧栏套一层滚动容器：模块越加越多时（12 项以上）窄窗口里也能滚到最底部的设置 */
    private static JScrollPane wrapNavScroll(JPanel nav) {
        JScrollPane sp = new JScrollPane(nav,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setBorder(null);
        sp.setPreferredSize(new Dimension(220, 0));
        sp.getVerticalScrollBar().setUnitIncrement(18);
        sp.getViewport().setOpaque(true);
        sp.getViewport().setBackground(Ui.sideBg());
        sp.setBackground(Ui.sideBg());
        return sp;
    }

    /** 排序变化后重建左侧导航，立即生效 */
    static void rebuildNav() {
        if (self == null) return;
        Container cp = self.frame.getContentPane();
        if (self.navScroll != null) cp.remove(self.navScroll);
        else cp.remove(self.navPanel);
        JPanel nav = new JPanel();
        self.buildNavItems(nav);
        self.navPanel = nav;
        self.navScroll = wrapNavScroll(nav);
        cp.add(self.navScroll, BorderLayout.WEST);
        cp.revalidate();
        cp.repaint();
        self.selectNav(self.currentKey);
    }

    /** 按顺序构建导航栏：品牌区 + 图标导航项 */
    private void buildNavItems(JPanel nav) {
        nav.removeAll();
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        nav.setBackground(Ui.sideBg());
        nav.setBorder(BorderFactory.createEmptyBorder(20, 12, 14, 12));
        nav.setPreferredSize(new Dimension(220, 0));

        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        brand.setOpaque(false);
        brand.setAlignmentX(Component.LEFT_ALIGNMENT);
        brand.add(logoIcon());

        JPanel brandText = new JPanel();
        brandText.setOpaque(false);
        brandText.setLayout(new BoxLayout(brandText, BoxLayout.Y_AXIS));
        JLabel name = new JLabel("小工具箱");
        name.setFont(Ui.font(17, true));
        name.setForeground(Theme.text());
        JLabel sub = new JLabel("TOOLBOX · PC");
        sub.setFont(new Font("SansSerif", Font.PLAIN, 9));
        sub.setForeground(Theme.muted());
        brandText.add(name);
        brandText.add(sub);
        brand.add(brandText);
        /* 品牌区高度锁死：否则 BoxLayout 会把多余空间全喂给它，下面的导航项被整体压下去 */
        brand.setMaximumSize(new Dimension(Integer.MAX_VALUE, brand.getPreferredSize().height));

        nav.add(Box.createVerticalStrut(2));
        nav.add(brand);
        nav.add(Box.createVerticalStrut(24));

        /* 普通模块从品牌下方一路往下排，多余的空白全部落在最后一项与设置之间 */
        for (String key : navOrder()) {
            if ("settings".equals(key)) continue;
            if (!isEnabled(key)) continue;
            addNav(nav, navLabel(key), key);
        }
        nav.add(Box.createVerticalGlue());
        nav.add(Box.createVerticalStrut(6));
        /* 「设置」固定钉在左下最底部，永远不参与排序 */
        addNav(nav, navLabel("settings"), "settings");
        nav.add(Box.createVerticalStrut(4));
        JLabel tip = new JLabel("点 × 最小化到托盘");
        tip.setFont(Ui.font(10, false));
        tip.setForeground(Theme.muted());
        tip.setAlignmentX(Component.LEFT_ALIGNMENT);
        nav.add(tip);
    }
}
