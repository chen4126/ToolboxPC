import io, re

p = r"C:\Users\chen\DevTools\ToolboxPC\src\com\toolbox\pc\OverviewPanel.java"
s = io.open(p, encoding="utf-8").read()

# 1) 三处 setVisible/revalidate/repaint 统一走 finishFutureBar()
pat = re.compile(
    r"futureBar\.setVisible\(true\);\s*\n\s*futureBar\.revalidate\(\);\s*\n\s*futureBar\.repaint\(\);")
s, n = pat.subn("finishFutureBar();", s)
print("finish replace:", n)

# 2) 新增 finishFutureBar + trimTrailSep
anchor = """    private JLabel sepLabel() {
        JLabel l = new JLabel("\uff5c");
        l.setFont(Ui.font(10, false));
        l.setForeground(Theme.muted());
        l.setBorder(BorderFactory.createEmptyBorder(1, 2, 1, 2));
        return l;
    }
"""
add = anchor + """
    /** 过期筛选行收尾：把末尾悬着的"｜"去掉，再统一做可见性刷新 */
    private void finishFutureBar() {
        int n = futureBar.getComponentCount();
        while (n > 0) {
            java.awt.Component c = futureBar.getComponent(n - 1);
            if (c instanceof JLabel && "\uff5c".equals(((JLabel) c).getText())) {
                futureBar.remove(n - 1);
                n--;
            } else {
                break;
            }
        }
        futureBar.setVisible(futureBar.getComponentCount() > 0);
        futureBar.revalidate();
        futureBar.repaint();
    }
"""
assert anchor in s, "sepLabel anchor not found"
s = s.replace(anchor, add, 1)

io.open(p, "w", encoding="utf-8", newline="\n").write(s)
print("ok, size", len(s))
