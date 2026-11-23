import com.toolbox.pc.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class TestShot {

    public static void main(String[] a) throws Exception {
        Theme.init();
        Ui.installDefaults();
        final Object[] holder = new Object[1];

        SwingUtilities.invokeAndWait(() -> {
            try {
                Class<?> c = Class.forName("com.toolbox.pc.Main");
                Object m = c.getDeclaredConstructor().newInstance();
                Method create = c.getDeclaredMethod("create");
                create.setAccessible(true);
                create.invoke(m);
                holder[0] = m;
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        Thread.sleep(2000);

        Class<?> c = Class.forName("com.toolbox.pc.Main");
        Field f = c.getDeclaredField("frame");
        f.setAccessible(true);
        final JFrame frame = (JFrame) f.get(holder[0]);
        frame.setAlwaysOnTop(true);
        frame.toFront();
        Thread.sleep(1200);

        Rectangle r = frame.getBounds();
        Robot robot = new Robot();
        render(frame.getContentPane(), "C:\\Users\\chen\\Desktop\\ui-1-overview.png");

        Method sel = c.getDeclaredMethod("selectNav", String.class);
        sel.setAccessible(true);

        String[] pages = {"schedule", "stats", "settings"};
        for (int i = 0; i < pages.length; i++) {
            final String key = pages[i];
            SwingUtilities.invokeAndWait(() -> {
                try {
                    sel.invoke(holder[0], key);
                } catch (Exception e) {
                }
            });
            Thread.sleep(1200);
            frame.setAlwaysOnTop(true);
            frame.toFront();
            Thread.sleep(600);
            render(frame.getContentPane(), "C:\\Users\\chen\\Desktop\\ui-" + (i + 2) + "-" + key + ".png");
        }

        // 日程页三种视图
        final SchedulePanel spv = findSchedule(frame.getContentPane());
        if (spv != null) {
            java.lang.reflect.Field vf = SchedulePanel.class.getDeclaredField("viewBtns");
            vf.setAccessible(true);
            final JToggleButton[] vb = (JToggleButton[]) vf.get(spv);
            SwingUtilities.invokeAndWait(() -> {
                try {
                    sel.invoke(holder[0], "schedule");
                } catch (Exception e) {
                }
            });
            Thread.sleep(700);
            for (int i = 1; i <= 2; i++) {
                final int idx = i;
                SwingUtilities.invokeAndWait(() -> vb[idx].doClick());
                Thread.sleep(700);
                render(frame.getContentPane(),
                        "C:\\Users\\chen\\Desktop\\ui-1" + (i + 1) + "-schedule-" + (idx == 1 ? "week" : "month") + ".png");
            }
            // 折叠“明天”分组再看效果
            final String tomKey = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.CHINA)
                    .format(new java.util.Date(System.currentTimeMillis() + 86400000L));
            final java.lang.reflect.Field ff = SchedulePanel.class.getDeclaredField("folded");
            ff.setAccessible(true);
            final java.lang.reflect.Method rf = SchedulePanel.class.getDeclaredMethod("refreshTable");
            rf.setAccessible(true);
            SwingUtilities.invokeAndWait(() -> {
                try {
                    vb[1].doClick();
                    @SuppressWarnings("unchecked")
                    java.util.Set<String> fs = (java.util.Set<String>) ff.get(spv);
                    fs.add(tomKey);
                    rf.invoke(spv);
                } catch (Exception ignored) {
                }
            });
            Thread.sleep(700);
            render(frame.getContentPane(), "C:\\Users\\chen\\Desktop\\ui-14-schedule-folded.png");
            SwingUtilities.invokeAndWait(() -> {
                try {
                    @SuppressWarnings("unchecked")
                    java.util.Set<String> fs = (java.util.Set<String>) ff.get(spv);
                    fs.clear();
                    rf.invoke(spv);
                } catch (Exception ignored) {
                }
            });
            // “全部收起”按钮
            final java.lang.reflect.Field bf = SchedulePanel.class.getDeclaredField("foldBtn");
            bf.setAccessible(true);
            SwingUtilities.invokeAndWait(() -> {
                try {
                    vb[1].doClick();
                    ((javax.swing.JButton) bf.get(spv)).doClick();
                } catch (Exception ignored) {
                }
            });
            Thread.sleep(700);
            render(frame.getContentPane(), "C:\\Users\\chen\\Desktop\\ui-15-schedule-foldall.png");
            SwingUtilities.invokeAndWait(() -> {
                try {
                    ((javax.swing.JButton) bf.get(spv)).doClick();
                } catch (Exception ignored) {
                }
            });
        }

        // 新建提醒对话框
        Field navField = c.getDeclaredField("cards");
        navField.setAccessible(true);
        JPanel cardsPanel = (JPanel) navField.get(holder[0]);
        SchedulePanel sp = null;
        for (Component comp : cardsPanel.getComponents()) {
            SchedulePanel tmp = findSchedule(comp);
            if (tmp != null) sp = tmp;
        }
        if (sp != null) {
            final SchedulePanel target = sp;
            SwingUtilities.invokeAndWait(() -> {
                try {
                    Method oe = SchedulePanel.class.getDeclaredMethod("openEditor", boolean.class);
                    oe.setAccessible(true);
                    oe.invoke(target, false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            Thread.sleep(900);
            JDialog dlg = null;
            for (Window w : Window.getWindows()) {
                if (w instanceof JDialog && w.isVisible()) {
                    dlg = (JDialog) w;
                    break;
                }
            }
            if (dlg != null) {
                final JDialog d = dlg;
                render(d.getContentPane(), "C:\\Users\\chen\\Desktop\\ui-9-editor.png");
                final JComboBox<?> cb = findCombo(d);
                if (cb != null) {
                    SwingUtilities.invokeAndWait(() -> {
                        cb.setSelectedIndex(2);
                        d.pack();
                        d.setLocationRelativeTo(null);
                    });
                    Thread.sleep(500);
                    render(d.getContentPane(), "C:\\Users\\chen\\Desktop\\ui-10-week.png");
                    SwingUtilities.invokeAndWait(() -> {
                        cb.setSelectedIndex(3);
                        d.pack();
                        d.setLocationRelativeTo(null);
                    });
                    Thread.sleep(500);
                    render(d.getContentPane(), "C:\\Users\\chen\\Desktop\\ui-11-month.png");
                }
            }
        }

        dumpScrolls(frame.getContentPane());

        /* 统计页：环形图点选“购物”后的筛选态 */
        StatsPanel stp = findStats(frame.getContentPane());
        if (stp != null) {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    sel.invoke(holder[0], "stats");
                } catch (Exception e) {
                }
            });
            Thread.sleep(900);
            java.lang.reflect.Field df = StatsPanel.class.getDeclaredField("donut");
            df.setAccessible(true);
            final Component dc = (Component) df.get(stp);
            SwingUtilities.invokeAndWait(() -> {
                java.awt.event.MouseEvent e = new java.awt.event.MouseEvent(dc,
                        java.awt.event.MouseEvent.MOUSE_CLICKED,
                        System.currentTimeMillis(), 0, 71, 48, 1, false,
                        java.awt.event.MouseEvent.BUTTON1);
                dc.dispatchEvent(e);
            });
            Thread.sleep(800);
            render(frame.getContentPane(), "C:\\Users\\chen\\Desktop\\ui-16-stats-filter.png");
            SwingUtilities.invokeAndWait(() -> {
                java.awt.event.MouseEvent e = new java.awt.event.MouseEvent(dc,
                        java.awt.event.MouseEvent.MOUSE_CLICKED,
                        System.currentTimeMillis(), 0, 71, 48, 1, false,
                        java.awt.event.MouseEvent.BUTTON1);
                dc.dispatchEvent(e);
            });
            Thread.sleep(500);
        }

        /* 记账页：正常态与“按分类筛选”态 */
        LedgerPanel led = findLedger(frame.getContentPane());
        if (led != null) {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    sel.invoke(holder[0], "ledger");
                } catch (Exception e) {
                }
            });
            Thread.sleep(900);
            render(frame.getContentPane(), "C:\\Users\\chen\\Desktop\\ui-17-ledger.png");

            java.lang.reflect.Field cf = LedgerPanel.class.getDeclaredField("monthCatsBox");
            cf.setAccessible(true);
            final JPanel catBox = (JPanel) cf.get(led);
            final JLabel shop = findLabelText(catBox, "购物");
            if (shop != null) {
                SwingUtilities.invokeAndWait(() -> shop.dispatchEvent(new java.awt.event.MouseEvent(
                        shop, java.awt.event.MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0,
                        5, 5, 1, false, java.awt.event.MouseEvent.BUTTON1)));
                Thread.sleep(700);
                render(frame.getContentPane(), "C:\\Users\\chen\\Desktop\\ui-18-ledger-filter.png");
                SwingUtilities.invokeAndWait(() -> shop.dispatchEvent(new java.awt.event.MouseEvent(
                        shop, java.awt.event.MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0,
                        5, 5, 1, false, java.awt.event.MouseEvent.BUTTON1)));
                Thread.sleep(400);
            }
        }

        /* 备忘页：搜索关键词高亮 */
        MemoPanel memo = findMemo(frame.getContentPane());
        if (memo != null) {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    sel.invoke(holder[0], "memo");
                } catch (Exception e) {
                }
            });
            Thread.sleep(800);
            render(frame.getContentPane(), "C:\\Users\\chen\\Desktop\\ui-19-memo.png");
            final JTextField search = findTextField(memo);
            if (search != null) {
                SwingUtilities.invokeAndWait(() -> search.setText("预算"));
                Thread.sleep(700);
                render(frame.getContentPane(), "C:\\Users\\chen\\Desktop\\ui-20-memo-search.png");
                SwingUtilities.invokeAndWait(() -> search.setText(""));
                Thread.sleep(300);
            }
            // 搜时间也能命中时间列
            if (search != null) {
                SwingUtilities.invokeAndWait(() -> search.setText("11-05"));
                Thread.sleep(700);
                render(frame.getContentPane(), "C:\\Users\\chen\\Desktop\\ui-24-memo-time.png");
                SwingUtilities.invokeAndWait(() -> search.setText(""));
                Thread.sleep(300);
            }
        }

        /* 倒计时页：到点后的超时计时 */
        CountDownPanel cdp = findCountDown(frame.getContentPane());
        if (cdp != null) {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    sel.invoke(holder[0], "countdown");
                } catch (Exception e) {
                }
            });
            Thread.sleep(800);
            final java.lang.reflect.Field hs = CountDownPanel.class.getDeclaredField("hourSp");
            hs.setAccessible(true);
            final java.lang.reflect.Field ms2 = CountDownPanel.class.getDeclaredField("minSp");
            ms2.setAccessible(true);
            final java.lang.reflect.Field ss = CountDownPanel.class.getDeclaredField("secSp");
            ss.setAccessible(true);
            final java.lang.reflect.Method stm = CountDownPanel.class.getDeclaredMethod("start");
            stm.setAccessible(true);
            SwingUtilities.invokeAndWait(() -> {
                try {
                    ((JSpinner) hs.get(cdp)).setValue(0);
                    ((JSpinner) ms2.get(cdp)).setValue(0);
                    ((JSpinner) ss.get(cdp)).setValue(1);
                    stm.invoke(cdp);
                } catch (Exception e) {
                }
            });
            Thread.sleep(3200);
            render(frame.getContentPane(), "C:\\Users\\chen\\Desktop\\ui-21-countdown-over.png");
            final java.lang.reflect.Field sb = CountDownPanel.class.getDeclaredField("startBtn");
            sb.setAccessible(true);
            final JButton stopBtn = (JButton) sb.get(cdp);
            SwingUtilities.invokeAndWait(() -> stopBtn.doClick());
            Thread.sleep(400);
            render(frame.getContentPane(), "C:\\Users\\chen\\Desktop\\ui-22-countdown-reset.png");

            // 超时满 1 分钟时的状态行（直接把超时值设成 65 秒，免等）
            final java.lang.reflect.Field omF = CountDownPanel.class.getDeclaredField("overMs");
            omF.setAccessible(true);
            final java.lang.reflect.Field ovF = CountDownPanel.class.getDeclaredField("over");
            ovF.setAccessible(true);
            final java.lang.reflect.Method utm = CountDownPanel.class.getDeclaredMethod("updateTime");
            utm.setAccessible(true);
            SwingUtilities.invokeAndWait(() -> {
                try {
                    ovF.set(cdp, true);
                    omF.set(cdp, 65000L);
                    ((JButton) sb.get(cdp)).setText("停止");
                    utm.invoke(cdp);
                } catch (Exception e) {
                }
            });
            Thread.sleep(400);
            render(frame.getContentPane(), "C:\\Users\\chen\\Desktop\\ui-25-countdown-overmin.png");
            SwingUtilities.invokeAndWait(() -> {
                try {
                    ovF.set(cdp, false);
                    omF.set(cdp, 0L);
                    ((JButton) sb.get(cdp)).setText("开始");
                    utm.invoke(cdp);
                } catch (Exception e) {
                }
            });
        }

        /* 概览“最近记账”点第一条 → 记账本定位选中 */
        OverviewPanel ovp = findOverview(frame.getContentPane());
        if (ovp != null) {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    sel.invoke(holder[0], "overview");
                } catch (Exception e) {
                }
            });
            Thread.sleep(900);
            java.lang.reflect.Field rbF = OverviewPanel.class.getDeclaredField("recentBox");
            rbF.setAccessible(true);
            final JPanel rb = (JPanel) rbF.get(ovp);
            final Component row0 = rb.getComponent(0);
            SwingUtilities.invokeAndWait(() -> row0.dispatchEvent(new java.awt.event.MouseEvent(
                    row0, java.awt.event.MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0,
                    5, 5, 1, false, java.awt.event.MouseEvent.BUTTON1)));
            Thread.sleep(1200);
            render(frame.getContentPane(), "C:\\Users\\chen\\Desktop\\ui-23-ledger-located.png");
        }

        /* 待办右键 → 在备忘录中查找 */
        if (ovp != null) {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    sel.invoke(holder[0], "overview");
                } catch (Exception e) {
                }
            });
            Thread.sleep(900);
            java.lang.reflect.Field tbF = OverviewPanel.class.getDeclaredField("todoBox");
            tbF.setAccessible(true);
            JPanel todoBox = (JPanel) tbF.get(ovp);
            JPanel trow = (JPanel) todoBox.getComponent(0);
            JCheckBox tcb = (JCheckBox) trow.getComponent(0);
            JPopupMenu tmenu = tcb.getComponentPopupMenu();
            if (tmenu != null) {
                final JMenuItem m0 = (JMenuItem) tmenu.getComponent(0);
                SwingUtilities.invokeAndWait(() -> m0.doClick());
                Thread.sleep(1200);
                render(frame.getContentPane(), "C:\\Users\\chen\\Desktop\\ui-26-todo-to-memo.png");
            }
        }

        /* 记账双击改金额/备注后的表格与统计 */
        LedgerPanel lp2 = findLedger(frame.getContentPane());
        if (lp2 != null) {
            java.lang.reflect.Field lfF = LedgerPanel.class.getDeclaredField("lines");
            lfF.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.List<String> lines = (java.util.List<String>) lfF.get(lp2);
            final int lastIdx = lines.size() - 1;
            java.lang.reflect.Method ae = LedgerPanel.class.getDeclaredMethod(
                    "applyEdit", int.class, String.class, String.class);
            ae.setAccessible(true);
            SwingUtilities.invokeAndWait(() -> {
                try {
                    ae.invoke(lp2, lastIdx, "66.50", "改过的备注");
                    sel.invoke(holder[0], "ledger");
                } catch (Exception e) {
                }
            });
            Thread.sleep(900);
            render(frame.getContentPane(), "C:\\Users\\chen\\Desktop\\ui-27-ledger-edited.png");
        }

        /* 并行倒计时超时满 1 分 */
        CountDownPanel cdp2 = findCountDown(frame.getContentPane());
        if (cdp2 != null) {
            java.lang.reflect.Field tasksF = CountDownPanel.class.getDeclaredField("tasks");
            tasksF.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.List<Object> tasks = (java.util.List<Object>) tasksF.get(cdp2);
            Class<?> taskCls = Class.forName("com.toolbox.pc.CountDownPanel$Task");
            java.lang.reflect.Constructor<?> ctor = taskCls.getDeclaredConstructor();
            ctor.setAccessible(true);
            final Object task = ctor.newInstance();
            java.lang.reflect.Field nf = taskCls.getDeclaredField("name");
            nf.setAccessible(true);
            nf.set(task, "泡面");
            java.lang.reflect.Field of = taskCls.getDeclaredField("over");
            of.setAccessible(true);
            of.setLong(task, 65000L);
            java.lang.reflect.Field rmm = taskCls.getDeclaredField("remainMs");
            rmm.setAccessible(true);
            rmm.setLong(task, 0L);
            java.lang.reflect.Field pmm = taskCls.getDeclaredField("plannedMs");
            pmm.setAccessible(true);
            pmm.setLong(task, 60000L);
            tasks.clear();
            tasks.add(task);
            SwingUtilities.invokeAndWait(() -> {
                try {
                    java.lang.reflect.Method rt = CountDownPanel.class.getDeclaredMethod("renderTasks");
                    rt.setAccessible(true);
                    rt.invoke(cdp2);
                    sel.invoke(holder[0], "countdown");
                } catch (Exception e) {
                }
            });
            Thread.sleep(900);
            render(frame.getContentPane(), "C:\\Users\\chen\\Desktop\\ui-28-countdown-parallel.png");
            tasks.clear();
        }

        /* 待办右键菜单：内容离屏渲染 + 真实菜单浮层合成 */
        if (ovp != null) {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    sel.invoke(holder[0], "overview");
                } catch (Exception e) {
                }
            });
            Thread.sleep(900);
            java.lang.reflect.Field tbF2 = OverviewPanel.class.getDeclaredField("todoBox");
            tbF2.setAccessible(true);
            JPanel todoBox2 = (JPanel) tbF2.get(ovp);
            JPanel trow2 = (JPanel) todoBox2.getComponent(0);
            JCheckBox tcb2 = (JCheckBox) trow2.getComponent(0);
            JPopupMenu tmenu2 = tcb2.getComponentPopupMenu();
            if (tmenu2 != null) {
                int w = frame.getContentPane().getWidth();
                int h = frame.getContentPane().getHeight();
                BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = img.createGraphics();
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, w, h);
                frame.getContentPane().paint(g);
                tmenu2.setSize(tmenu2.getPreferredSize());
                tmenu2.doLayout();
                Point pt = SwingUtilities.convertPoint(tcb2, 0, 0, frame.getContentPane());
                int mx = pt.x + 40, my = pt.y + 20;
                g.translate(mx, my);
                tmenu2.paint(g);
                g.translate(-mx, -my);
                /* 把“改期到…”子菜单也展开画在右侧 */
                JMenu defer = null;
                for (Component cc : tmenu2.getComponents()) {
                    if (cc instanceof JMenu) defer = (JMenu) cc;
                }
                if (defer != null) {
                    JPopupMenu sub = defer.getPopupMenu();
                    sub.setSize(sub.getPreferredSize());
                    sub.doLayout();
                    defer.setSize(defer.getWidth() > 0 ? defer.getWidth() : tmenu2.getWidth(), defer.getHeight());
                    int sx = mx + tmenu2.getWidth() - 2;
                    int sy = my + defer.getY();
                    g.translate(sx, sy);
                    sub.paint(g);
                    g.translate(-sx, -sy);
                    System.out.println("子菜单项 " + sub.getComponentCount() + " 位于 " + sx + "," + sy);
                }
                g.dispose();
                ImageIO.write(img, "png", new File("C:\\Users\\chen\\Desktop\\ui-29-todo-menu.png"));
                System.out.println("saved ui-29 菜单项 " + tmenu2.getSubElements().length);
            }
        }

        /* 记账页右键删除确认框（真实 JOptionPane 外观渲染） */
        LedgerPanel lp3 = findLedger(frame.getContentPane());
        if (lp3 != null) {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    sel.invoke(holder[0], "ledger");
                } catch (Exception e) {
                }
            });
            Thread.sleep(900);
            Object[] opts = {"是", "否"};
            JOptionPane op = new JOptionPane(
                    "确认删除这条记录？\n\n11-07 12:00　支出 · 餐饮 · 午饭　-¥90.00\n删除后无法恢复。",
                    JOptionPane.WARNING_MESSAGE, JOptionPane.YES_NO_OPTION, null, opts, opts[1]);
            JDialog d = op.createDialog(frame, "删除确认");
            d.pack();
            JComponent cp = (JComponent) d.getContentPane();
            int w = frame.getContentPane().getWidth();
            int h = frame.getContentPane().getHeight();
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, w, h);
            frame.getContentPane().paint(g);
            cp.setSize(cp.getPreferredSize());
            cp.doLayout();
            for (Component k : cp.getComponents()) {
                if (k instanceof JComponent) ((JComponent) k).doLayout();
            }
            g.translate((w - cp.getWidth()) / 2, (h - cp.getHeight()) / 2);
            cp.paint(g);
            g.dispose();
            d.dispose();
            ImageIO.write(img, "png", new File("C:\\Users\\chen\\Desktop\\ui-30-ledger-delete.png"));
            System.out.println("saved ui-30 确认框 " + cp.getWidth() + "x" + cp.getHeight());

            /* 记账右键菜单（复制 / 改成收入 / 删除） */
            try {
                java.lang.reflect.Field tblF = LedgerPanel.class.getDeclaredField("table");
                tblF.setAccessible(true);
                JTable tbl = (JTable) tblF.get(lp3);
                SwingUtilities.invokeAndWait(() -> tbl.setRowSelectionInterval(0, 0));
                java.lang.reflect.Method utl = LedgerPanel.class.getDeclaredMethod("updateToggleLabel");
                utl.setAccessible(true);
                utl.invoke(lp3);
                JPopupMenu lmenu = tbl.getComponentPopupMenu();
                lmenu.setSize(lmenu.getPreferredSize());
                lmenu.doLayout();
                BufferedImage img2 = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2 = img2.createGraphics();
                g2.setColor(Color.WHITE);
                g2.fillRect(0, 0, w, h);
                frame.getContentPane().paint(g2);
                Rectangle cell = tbl.getCellRect(0, 0, true);
                Point tp = SwingUtilities.convertPoint(tbl, cell.x + cell.width / 2, cell.y, frame.getContentPane());
                g2.translate(tp.x, tp.y + 40);
                lmenu.paint(g2);
                g2.dispose();
                ImageIO.write(img2, "png", new File("C:\\Users\\chen\\Desktop\\ui-31-ledger-menu.png"));
                System.out.println("saved ui-31 ledger menu items=" + lmenu.getComponentCount());
            } catch (Throwable t) {
                java.io.PrintWriter pw = new java.io.PrintWriter(
                        new java.io.FileWriter("C:\\Users\\chen\\Desktop\\ui-31-error.txt"), true);
                t.printStackTrace(pw);
                pw.close();
                System.out.println("ui-31 failed see ui-31-error.txt");
            }

            /* 并行超时满 5 分 */
            CountDownPanel cdp3 = findCountDown(frame.getContentPane());
            if (cdp3 != null) try {
                java.lang.reflect.Field tasksF = CountDownPanel.class.getDeclaredField("tasks");
                tasksF.setAccessible(true);
                @SuppressWarnings("unchecked")
                java.util.List<Object> tasks = (java.util.List<Object>) tasksF.get(cdp3);
                Class<?> taskCls = Class.forName("com.toolbox.pc.CountDownPanel$Task");
                java.lang.reflect.Constructor<?> ctor = taskCls.getDeclaredConstructor();
                ctor.setAccessible(true);
                Object task = ctor.newInstance();
                java.lang.reflect.Field nf = taskCls.getDeclaredField("name");
                nf.setAccessible(true);
                nf.set(task, "泡面");
                java.lang.reflect.Field of = taskCls.getDeclaredField("over");
                of.setAccessible(true);
                of.setLong(task, 302000L);
                java.lang.reflect.Field rmm = taskCls.getDeclaredField("remainMs");
                rmm.setAccessible(true);
                rmm.setLong(task, 0L);
                java.lang.reflect.Field pmm = taskCls.getDeclaredField("plannedMs");
                pmm.setAccessible(true);
                pmm.setLong(task, 60000L);
                tasks.clear();
                tasks.add(task);
                SwingUtilities.invokeAndWait(() -> {
                    try {
                        java.lang.reflect.Method rt = CountDownPanel.class.getDeclaredMethod("renderTasks");
                        rt.setAccessible(true);
                        rt.invoke(cdp3);
                        sel.invoke(holder[0], "countdown");
                    } catch (Exception e) {
                    }
                });
                Thread.sleep(900);
                render(frame.getContentPane(), "C:\\Users\\chen\\Desktop\\ui-32-countdown-5min.png");
                tasks.clear();
            } catch (Throwable t) {
                System.out.println("ui-32 failed: " + t);
                t.printStackTrace();
            }
        }

        System.exit(0);
    }

    static OverviewPanel findOverview(Component c) {
        if (c instanceof OverviewPanel) return (OverviewPanel) c;
        if (c instanceof Container) {
            for (Component k : ((Container) c).getComponents()) {
                OverviewPanel s = findOverview(k);
                if (s != null) return s;
            }
        }
        return null;
    }

    static CountDownPanel findCountDown(Component c) {
        if (c instanceof CountDownPanel) return (CountDownPanel) c;
        if (c instanceof Container) {
            for (Component k : ((Container) c).getComponents()) {
                CountDownPanel s = findCountDown(k);
                if (s != null) return s;
            }
        }
        return null;
    }

    static MemoPanel findMemo(Component c) {
        if (c instanceof MemoPanel) return (MemoPanel) c;
        if (c instanceof Container) {
            for (Component k : ((Container) c).getComponents()) {
                MemoPanel s = findMemo(k);
                if (s != null) return s;
            }
        }
        return null;
    }

    static JTextField findTextField(Component c) {
        if (c instanceof JTextField) return (JTextField) c;
        if (c instanceof Container) {
            for (Component k : ((Container) c).getComponents()) {
                JTextField s = findTextField(k);
                if (s != null) return s;
            }
        }
        return null;
    }

    static LedgerPanel findLedger(Component c) {
        if (c instanceof LedgerPanel) return (LedgerPanel) c;
        if (c instanceof Container) {
            for (Component k : ((Container) c).getComponents()) {
                LedgerPanel s = findLedger(k);
                if (s != null) return s;
            }
        }
        return null;
    }

    static JLabel findLabelText(Container c, String text) {
        for (Component k : c.getComponents()) {
            if (k instanceof JLabel && text.equals(((JLabel) k).getText())) return (JLabel) k;
            if (k instanceof Container) {
                JLabel r = findLabelText((Container) k, text);
                if (r != null) return r;
            }
        }
        return null;
    }

    static StatsPanel findStats(Component c) {
        if (c instanceof StatsPanel) return (StatsPanel) c;
        if (c instanceof Container) {
            for (Component k : ((Container) c).getComponents()) {
                StatsPanel s = findStats(k);
                if (s != null) return s;
            }
        }
        return null;
    }

    static SchedulePanel findSchedule(Component c) {
        if (c instanceof SchedulePanel) return (SchedulePanel) c;
        if (c instanceof Container) {
            for (Component k : ((Container) c).getComponents()) {
                SchedulePanel s = findSchedule(k);
                if (s != null) return s;
            }
        }
        return null;
    }

    static JComboBox<?> findCombo(Component c) {
        if (c instanceof JComboBox) return (JComboBox<?>) c;
        if (c instanceof Container) {
            for (Component k : ((Container) c).getComponents()) {
                JComboBox<?> s = findCombo(k);
                if (s != null) return s;
            }
        }
        return null;
    }

    static void dumpScrolls(Component c) {
        if (c instanceof JScrollPane) {
            JScrollPane sp = (JScrollPane) c;
            Component v = sp.getViewport().getView();
            System.out.println("SCROLL sp=" + sp.getBounds() + " vp=" + sp.getViewport().getBounds()
                    + " view=" + v.getClass().getSimpleName() + " " + v.getBounds()
                    + " tracksW=" + (v instanceof Scrollable ? ((Scrollable) v).getScrollableTracksViewportWidth() : "n/a"));
            if (v instanceof Container && v.getClass().getSimpleName().contains("Scrollable")) {
                for (Component k : ((Container) v).getComponents()) {
                    System.out.println("   child " + k.getClass().getSimpleName() + " " + k.getBounds() + " alignX=" + k.getAlignmentX());
                }
            }
        }
        if (c instanceof Container) {
            for (Component k : ((Container) c).getComponents()) dumpScrolls(k);
        }
    }

    static void shot(Robot robot, Rectangle r, String path) throws Exception {
        BufferedImage img = robot.createScreenCapture(r);
        ImageIO.write(img, "png", new File(path));
        System.out.println("saved " + path + " " + img.getWidth() + "x" + img.getHeight());
    }

    /** 组件离屏渲染：不受其他前台窗口遮挡影响 */
    static void render(Component c, String path) throws Exception {
        int w = Math.max(1, c.getWidth());
        int h = Math.max(1, c.getHeight());
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        c.paint(g);
        g.dispose();
        ImageIO.write(img, "png", new File(path));
        System.out.println("saved " + path + " " + w + "x" + h);
    }
}
