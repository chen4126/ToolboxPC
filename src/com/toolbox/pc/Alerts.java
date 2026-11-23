package com.toolbox.pc;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

/** 强提醒：置顶醒目弹窗 + 可选提示音。所有工具到点提醒统一走这里。 */
public class Alerts {

    private static final Color RED = new Color(0xE53935);
    private static final Color LIGHT_RED = new Color(0xFFEBEE);

    /** 触发一次提醒 */
    public static void fire(String title, String msg) {
        boolean sound = readBool("sound", true);
        boolean strong = readBool("strong", true);
        if (sound) playSound();
        if (strong) {
            showPopup(title, msg);
        } else {
            Main.notifyMsg(title, msg);
        }
    }

    /** 提前预告：只走托盘气泡，不响铃、不弹强提醒窗口 */
    public static void pre(String title, String msg) {
        lastPre = title + "｜" + msg;
        Main.notifyMsg(title, msg);
    }

    /** 最近一条提前预告的文案（“标题｜正文”），用于排查 */
    static String lastPre = "";

    private static boolean readBool(String key, boolean def) {
        List<String> l = Store.readLines("settings.txt");
        for (String line : l) {
            String[] p = line.split("=", 2);
            if (p.length == 2 && p[0].trim().equals(key)) {
                return p[1].trim().equals("1");
            }
        }
        return def;
    }

    /* ---------- 提示音：程序合成的几种音效，设置页里多选一 ---------- */

    /** 可选音效的键，与 KIND_LABELS 一一对应；第一个是默认 */
    public static final String[] KINDS = {"triple", "chime", "single", "alarm", "soft"};
    public static final String[] KIND_LABELS = {"经典三声", "叮咚双音", "单声短提示", "上升三连", "轻柔两声"};

    /** 当前选中的音效键；设置里没写、或写了不认识的值都回落经典三声 */
    public static String kind() {
        for (String line : Store.readLines("settings.txt")) {
            String[] p = line.split("=", 2);
            if (p.length == 2 && p[0].trim().equals("soundKind")) {
                String v = p[1].trim();
                for (String k : KINDS) {
                    if (k.equals(v)) return k;
                }
                break;
            }
        }
        return KINDS[0];
    }

    public static String kindLabel(String k) {
        for (int i = 0; i < KINDS.length; i++) {
            if (KINDS[i].equals(k)) return KIND_LABELS[i];
        }
        return KIND_LABELS[0];
    }

    /** 保存选中的音效（settings.txt 写 soundKind=xxx，非法值落回默认） */
    public static void setKind(String k) {
        String val = KINDS[0];
        for (String x : KINDS) {
            if (x.equals(k)) val = x;
        }
        List<String> out = new ArrayList<String>(Store.readLines("settings.txt"));
        boolean found = false;
        for (int i = 0; i < out.size(); i++) {
            String[] p = out.get(i).split("=", 2);
            if (p.length == 2 && p[0].trim().equals("soundKind")) {
                out.set(i, "soundKind=" + val);
                found = true;
                break;
            }
        }
        if (!found) out.add("soundKind=" + val);
        Store.writeLines("settings.txt", out);
    }

    /** 试听：不管"通知音"有没有勾上，点了就响一遍 */
    public static void preview(String k) {
        playKind(k);
    }

    private static void playSound() {
        playKind(kind());
    }

    /** 按音效键播一遍 */
    public static void playKind(String k) {
        final String kk = k == null ? KINDS[0] : k;
        new Thread(new Runnable() {
            public void run() {
                try {
                    if ("chime".equals(kk)) {
                        tone(1046, 0.5, 3.0);
                        Thread.sleep(150);
                        tone(784, 0.6, 2.2);
                    } else if ("single".equals(kk)) {
                        tone(988, 0.25, 6.0);
                    } else if ("alarm".equals(kk)) {
                        tone(659, 0.18, 5.0);
                        Thread.sleep(120);
                        tone(880, 0.18, 5.0);
                        Thread.sleep(120);
                        tone(1108, 0.30, 4.0);
                    } else if ("soft".equals(kk)) {
                        tone(523, 0.6, 2.0);
                        Thread.sleep(380);
                        tone(523, 0.6, 2.0);
                    } else {
                        for (int i = 0; i < 3; i++) {
                            tone(880, 0.45, 4.0);
                            Thread.sleep(500);
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }).start();
    }

    private static void tone(double freq, double dur, double decay) {
        try {
            byte[] wav = makeToneWav(freq, dur, decay);
            AudioInputStream ais = AudioSystem.getAudioInputStream(new ByteArrayInputStream(wav));
            Clip clip = AudioSystem.getClip();
            clip.open(ais);
            clip.start();
        } catch (Exception ignored) {
        }
    }

    /** 合成一段单音 WAV（频率 / 时长秒 / 衰减系数） */
    static byte[] makeToneWav(double freq, double dur, double decay) {
        int sr = 44100;
        int n = (int) (sr * dur);
        byte[] data = new byte[44 + n * 2];
        byte[] h = "RIFF".getBytes();
        System.arraycopy(h, 0, data, 0, 4);
        putInt(data, 4, 36 + n * 2);
        System.arraycopy("WAVE".getBytes(), 0, data, 8, 4);
        System.arraycopy("fmt ".getBytes(), 0, data, 12, 4);
        putInt(data, 16, 16);
        putShort(data, 20, 1);
        putShort(data, 22, 1);
        putInt(data, 24, sr);
        putInt(data, 28, sr * 2);
        putShort(data, 32, 2);
        putShort(data, 34, 16);
        System.arraycopy("data".getBytes(), 0, data, 36, 4);
        putInt(data, 40, n * 2);
        for (int i = 0; i < n; i++) {
            double t = (double) i / sr;
            double env = Math.exp(-decay * t);
            double v = Math.sin(2 * Math.PI * freq * t) * 0.5 * env;
            short s = (short) (v * Short.MAX_VALUE);
            data[44 + i * 2] = (byte) (s & 0xff);
            data[44 + i * 2 + 1] = (byte) ((s >> 8) & 0xff);
        }
        return data;
    }

    private static void putInt(byte[] b, int off, int v) {
        b[off] = (byte) (v & 0xff);
        b[off + 1] = (byte) ((v >> 8) & 0xff);
        b[off + 2] = (byte) ((v >> 16) & 0xff);
        b[off + 3] = (byte) ((v >> 24) & 0xff);
    }

    private static void putShort(byte[] b, int off, int v) {
        b[off] = (byte) (v & 0xff);
        b[off + 1] = (byte) ((v >> 8) & 0xff);
    }

    /* ---------- 强提醒弹窗：置顶 + 红框闪烁 + 大字 ---------- */

    private static void showPopup(String title, String msg) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                final JDialog d = new JDialog((Frame) null, "提醒", false);
                d.setAlwaysOnTop(true);
                d.setResizable(false);

                final JPanel p = new JPanel(new BorderLayout(0, 0));
                p.setBackground(LIGHT_RED);
                p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

                JLabel icon = new JLabel("⏰", SwingConstants.CENTER);
                icon.setFont(new Font("SansSerif", Font.PLAIN, 40));
                icon.setAlignmentX(Component.CENTER_ALIGNMENT);
                p.add(Box.createVerticalStrut(16));
                p.add(icon);

                JLabel t = new JLabel(title, SwingConstants.CENTER);
                t.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 24));
                t.setForeground(RED);
                t.setAlignmentX(Component.CENTER_ALIGNMENT);
                p.add(t);

                JLabel m = new JLabel(msg, SwingConstants.CENTER);
                m.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 34));
                m.setForeground(new Color(0x212121));
                m.setAlignmentX(Component.CENTER_ALIGNMENT);
                p.add(Box.createVerticalStrut(6));
                p.add(m);

                p.add(Box.createVerticalStrut(20));

                JButton ok = new JButton("我知道了");
                ok.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 16));
                ok.setBackground(RED);
                ok.setForeground(Color.WHITE);
                ok.setAlignmentX(Component.CENTER_ALIGNMENT);
                ok.setMaximumSize(new Dimension(200, 44));
                ok.setPreferredSize(new Dimension(200, 44));
                p.add(ok);
                p.add(Box.createVerticalStrut(16));

                final Timer blink = new Timer(500, new ActionListener() {
                    boolean on = false;

                    public void actionPerformed(ActionEvent e) {
                        on = !on;
                        p.setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(on ? RED : new Color(0xFF8A80), 5),
                                BorderFactory.createEmptyBorder(10, 14, 10, 14)));
                    }
                });
                p.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(RED, 5),
                        BorderFactory.createEmptyBorder(10, 14, 10, 14)));
                blink.start();

                ok.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        blink.stop();
                        d.dispose();
                    }
                });
                d.addWindowListener(new java.awt.event.WindowAdapter() {
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        blink.stop();
                    }
                });

                d.setContentPane(p);
                d.setSize(480, 300);
                d.setLocationRelativeTo(null);
                d.setVisible(true);
                d.toFront();
                d.requestFocus();
            }
        });
    }
}
