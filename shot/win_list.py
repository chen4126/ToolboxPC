import ctypes
from ctypes import wintypes

u = ctypes.windll.user32
u.SetProcessDPIAware()
EnumProc = ctypes.WINFUNCTYPE(wintypes.BOOL, wintypes.HWND, wintypes.LPARAM)

rows = []

def cb(h, l):
    if not u.IsWindowVisible(h):
        return True
    r = wintypes.RECT()
    u.GetWindowRect(h, ctypes.byref(r))
    n = u.GetWindowTextLengthW(h)
    name = ''
    if n:
        buf = ctypes.create_unicode_buffer(n + 1)
        u.GetWindowTextW(h, buf, n + 1)
        name = buf.value
    cls = ctypes.create_unicode_buffer(256)
    u.GetClassNameW(h, cls, 256)
    if name.strip() or 'SunAwt' in cls.value or 'JFrame' in cls.value:
        rows.append((r.left, r.top, r.right - r.left, r.bottom - r.top, cls.value, name))
    return True

u.EnumWindows(EnumProc(cb), 0)
for x, y, w, h, c, t in rows:
    if w < 1 or h < 1:
        continue
    if '工具箱' in t or 'SunAwt' in c or 'JFrame' in c or 'JWindow' in c or 'Dialog' in c:
        print(f"x={x:5d} y={y:5d} w={w:5d} h={h:5d}  [{c}]  {t}")
