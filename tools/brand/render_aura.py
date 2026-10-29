"""Renders the Aura TV brand: one geometry, every output.

The mark is defined ONCE below (MARK, in a 100-unit local box). Every output is derived from it, so a
change here re-renders the launcher icon, themed icon, splash, TV banner and previews together:

    python tools/brand/render_aura.py --res app/src/main/res --extras extras/brand/aura

Outputs
  res/mipmap-anydpi-v26/owntv_icon_eggshell.xml   adaptive icon (overrides the OwnTV one by name)
  res/drawable/aura_icon_foreground.xml           VECTOR foreground, scales to any density
  res/drawable/aura_icon_background.xml           VECTOR background (radial navy)
  res/drawable/aura_icon_monochrome.xml           VECTOR single-colour layer for Android 13 themed icons
  res/drawable/owntv_splash_static_eggshell.xml   splash icon (API < 31)
  res/drawable/owntv_splash_eggshell.xml          splash icon (API 31+), static instead of the flip
  res/drawable-{hdpi,xhdpi,xxhdpi}/owntv_banner_eggshell.png   TV banner 240x135 / 320x180 / 480x270
  res/drawable-*/owntv_splash_name_eggshell.png   wordmark under the Android 12+ splash icon
  extras/brand/aura/*.png                         1024 master, 512 store icon, previews

Adaptive-icon rules this follows (Android: 108dp canvas, 72dp visible after the launcher mask, 66dp
safe circle): full-bleed background with no baked-in rounded square, no text, mark inside the safe
circle. Needs Pillow and numpy.
"""
import argparse
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

# ---- palette -------------------------------------------------------------------------------------
BLUE, VIOLET, PINK, ORANGE = '#3E86FF', '#8B4DFF', '#FF4FA0', '#FF9A5A'
STOPS = [(0.0, BLUE), (0.42, VIOLET), (0.75, PINK), (1.0, ORANGE)]
PLAY_FILL = [(0.0, PINK), (1.0, ORANGE)]
PLAY_STROKE = '#FF7480'     # solid tone of the play's rounded edge (the midpoint of PLAY_FILL)
BG_CENTER, BG_EDGE = '#1A1F3F', '#070914'
HALO = '#141833'            # the "gap" between overlapping ribbons: the background's own tone
SPLASH_BG = '#040E0B'       # owntv_window_bg, so the splash halos vanish into the launch window

# ---- geometry (local 100-unit box) ---------------------------------------------------------------
# The left leg of the A flows down, turns at the foot and becomes the wave that sweeps right across the
# right leg: one continuous ribbon. The frame brackets are the "TV" behind it.
BRACKET_L = [('M', 33, 17), ('L', 26, 17), ('Q', 12, 17, 12, 31), ('L', 12, 58)]
BRACKET_R = [('M', 67, 17), ('L', 74, 17), ('Q', 88, 17, 88, 31), ('L', 88, 58)]
LEG_R = [('M', 50, 15), ('L', 79.5, 85)]
RIBBON = [('M', 50, 15), ('L', 27, 70), ('C', 24, 80, 28, 86, 38, 86),
          ('C', 52, 86, 58, 75, 70, 71), ('C', 80, 68, 86, 65, 91, 59)]
RIBBON_MONO = [('M', 50, 15), ('L', 27, 70), ('C', 24, 80, 28, 86, 38, 86), ('C', 47, 86, 52, 82, 57, 78)]
PLAY = [(44, 46), (58, 56), (44, 66)]
W_BRACKET, W_LEG, W_PLAY = 6.0, 12.0, 3.0
HALO_RIBBON = 3.6                    # extra width on each side of the ribbon where it crosses the right leg
STROKES = ((BRACKET_L, W_BRACKET), (BRACKET_R, W_BRACKET), (LEG_R, W_LEG), (RIBBON, W_LEG))


def _measure():
    """Centre and enclosing radius of the visible mark (stroke widths included, halos not)."""
    pts = []
    for path, w in STROKES:
        pts += [(x, y, w / 2) for x, y in sample(path, 0.4)]
    pts += [(x, y, W_PLAY / 2) for x, y in PLAY]
    x0, x1 = min(p[0] - p[2] for p in pts), max(p[0] + p[2] for p in pts)
    y0, y1 = min(p[1] - p[2] for p in pts), max(p[1] + p[2] for p in pts)
    cx, cy = (x0 + x1) / 2, (y0 + y1) / 2
    return cx, cy, max(math.hypot(x - cx, y - cy) + r for x, y, r in pts)


GRAD = (8, 88, 92, 12)     # one diagonal gradient across the whole mark: bottom-left -> top-right


def rgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


# ---- path sampling -------------------------------------------------------------------------------
def sample(path, step=0.3):
    """Dense polyline (local units) along an M/L/Q/C path."""
    pts, cur = [], None
    for seg in path:
        k = seg[0]
        if k == 'M':
            cur = (seg[1], seg[2]); pts.append(cur)
        elif k == 'L':
            p1 = (seg[1], seg[2]); pts += _line(cur, p1, step); cur = p1
        elif k == 'Q':
            c, p1 = (seg[1], seg[2]), (seg[3], seg[4])
            n = max(8, int(_len([cur, c, p1]) / step))
            for i in range(1, n + 1):
                t = i / n
                pts.append(((1 - t) ** 2 * cur[0] + 2 * (1 - t) * t * c[0] + t * t * p1[0],
                            (1 - t) ** 2 * cur[1] + 2 * (1 - t) * t * c[1] + t * t * p1[1]))
            cur = p1
        elif k == 'C':
            c1, c2, p1 = (seg[1], seg[2]), (seg[3], seg[4]), (seg[5], seg[6])
            n = max(8, int(_len([cur, c1, c2, p1]) / step))
            for i in range(1, n + 1):
                t = i / n; u = 1 - t
                pts.append((u ** 3 * cur[0] + 3 * u * u * t * c1[0] + 3 * u * t * t * c2[0] + t ** 3 * p1[0],
                            u ** 3 * cur[1] + 3 * u * u * t * c1[1] + 3 * u * t * t * c2[1] + t ** 3 * p1[1]))
            cur = p1
    return pts


def _len(ps):
    return sum(math.hypot(ps[i + 1][0] - ps[i][0], ps[i + 1][1] - ps[i][1]) for i in range(len(ps) - 1))


def _line(a, b, step):
    n = max(1, int(math.hypot(b[0] - a[0], b[1] - a[1]) / step))
    return [(a[0] + (b[0] - a[0]) * i / n, a[1] + (b[1] - a[1]) * i / n) for i in range(1, n + 1)]


CX, CY, R_MARK = _measure()


# ---- raster drawing ------------------------------------------------------------------------------
class Tf:
    """local units -> pixels: the mark's centre lands on (px, py), one local unit is `unit` px."""

    def __init__(self, px, py, unit):
        self.px, self.py, self.unit = px, py, unit

    def pt(self, x, y):
        return (self.px + (x - CX) * self.unit, self.py + (y - CY) * self.unit)


def stroke_mask(size, tf, path, width):
    m = Image.new('L', size, 0)
    d = ImageDraw.Draw(m)
    r = width / 2 * tf.unit
    for x, y in sample(path, step=max(0.15, 0.6 / max(tf.unit, 0.001) * 2)):
        px, py = tf.pt(x, y)
        d.ellipse((px - r, py - r, px + r, py + r), fill=255)
    return m


def polygon_mask(size, tf, pts, stroke_w):
    m = Image.new('L', size, 0)
    d = ImageDraw.Draw(m)
    p = [tf.pt(*q) for q in pts]
    d.polygon(p, fill=255)
    r = stroke_w / 2 * tf.unit
    for i in range(len(p)):
        a, b = p[i], p[(i + 1) % len(p)]
        d.line([a, b], fill=255, width=int(round(stroke_w * tf.unit)))
        d.ellipse((a[0] - r, a[1] - r, a[0] + r, a[1] + r), fill=255)
    return m


def gradient_img(size, tf, stops, line=GRAD):
    x0, y0, x1, y1 = line
    (ax, ay), (bx, by) = tf.pt(x0, y0), tf.pt(x1, y1)
    w, h = size
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    dx, dy = bx - ax, by - ay
    t = np.clip(((xx - ax) * dx + (yy - ay) * dy) / (dx * dx + dy * dy), 0, 1)
    out = np.zeros((h, w, 3), np.float32)
    offs = [s[0] for s in stops]
    cols = np.array([rgb(s[1]) for s in stops], np.float32)
    for c in range(3):
        out[..., c] = np.interp(t, offs, cols[:, c])
    return Image.fromarray(out.astype(np.uint8), 'RGB')


def paint(layer, mask, fill):
    """Alpha-composite `fill` (colour string or RGB image) through `mask` onto layer."""
    img = Image.new('RGB', layer.size, rgb(fill)) if isinstance(fill, str) else fill
    top = img.convert('RGBA')
    top.putalpha(mask)
    layer.alpha_composite(top)


def draw_mark(size, tf, halo=HALO, mono=None):
    """The mark on a transparent layer. `mono` (colour) draws a one-colour silhouette without halos."""
    layer = Image.new('RGBA', size, (0, 0, 0, 0))
    grad = gradient_img(size, tf, STOPS) if mono is None else mono
    play_grad = gradient_img(size, tf, PLAY_FILL, line=(44, 46, 58, 66)) if mono is None else mono
    ribbon = RIBBON if mono is None else RIBBON_MONO
    paint(layer, stroke_mask(size, tf, BRACKET_L, W_BRACKET), grad)
    paint(layer, stroke_mask(size, tf, BRACKET_R, W_BRACKET), grad)
    paint(layer, stroke_mask(size, tf, LEG_R, W_LEG), grad)
    if mono is None:
        paint(layer, stroke_mask(size, tf, ribbon, W_LEG + 2 * HALO_RIBBON), halo)
    paint(layer, stroke_mask(size, tf, ribbon, W_LEG), grad)
    paint(layer, polygon_mask(size, tf, PLAY, W_PLAY), play_grad)
    return layer


def render_mark(px, py, unit, w, h, ss=3, glow=0.0, halo=HALO, mono=None):
    """Supersampled render of the mark centred at (px, py) on a w x h transparent canvas, with an
    optional soft glow (strength 0..1) underneath."""
    size = (w * ss, h * ss)
    tf = Tf(px * ss, py * ss, unit * ss)
    layer = draw_mark(size, tf, halo, mono)
    if glow > 0:
        colour_only = Image.new('RGBA', size, (0, 0, 0, 0))
        g = gradient_img(size, tf, STOPS)
        for path, wd in STROKES:
            paint(colour_only, stroke_mask(size, tf, path, wd), g)
        blur = colour_only.filter(ImageFilter.GaussianBlur(radius=5 * unit * ss))
        a = blur.getchannel('A').point(lambda v: int(v * glow))
        blur.putalpha(a)
        base = Image.new('RGBA', size, (0, 0, 0, 0))
        base.alpha_composite(blur)
        base.alpha_composite(layer)
        layer = base
    return layer.resize((w, h), Image.LANCZOS)


def background_img(w, h, cx, cy, radius):
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    t = np.clip(np.hypot(xx - cx, yy - cy) / radius, 0, 1)
    c0, c1 = np.array(rgb(BG_CENTER), np.float32), np.array(rgb(BG_EDGE), np.float32)
    out = c0[None, None, :] * (1 - t[..., None]) + c1[None, None, :] * t[..., None]
    return Image.fromarray(out.astype(np.uint8), 'RGB').convert('RGBA')


# ---- android vector emission ---------------------------------------------------------------------
SAFE_R = 32.4             # dp. Android's safe circle is 66dp across; this leaves ~0.6dp of air.
ICON_UNIT = min(0.66, SAFE_R / R_MARK)   # local unit -> 108dp canvas units, derived from the geometry
ICON_CX, ICON_CY = 54.0, 54.0


def v(x):
    s = f'{x:.2f}'.rstrip('0').rstrip('.')
    return s if s not in ('-0', '') else '0'


def path_data(path, unit=ICON_UNIT, ox=ICON_CX, oy=ICON_CY):
    def X(x): return ox + (x - CX) * unit
    def Y(y): return oy + (y - CY) * unit
    out = []
    for seg in path:
        k = seg[0]
        pts = seg[1:]
        out.append(k + ' '.join(f'{v(X(pts[i]))},{v(Y(pts[i + 1]))}' for i in range(0, len(pts), 2)))
    return ''.join(out)


def grad_xml(stops, line, unit=ICON_UNIT, ox=ICON_CX, oy=ICON_CY, attr='android:strokeColor', indent='        '):
    x0, y0, x1, y1 = line
    f = lambda x, y: (ox + (x - CX) * unit, oy + (y - CY) * unit)
    (ax, ay), (bx, by) = f(x0, y0), f(x1, y1)
    items = ''.join(f'\n{indent}    <item android:offset="{o}" android:color="{c}"/>' for o, c in stops)
    return (f'\n{indent[:-4]}<aapt:attr name="{attr}">\n{indent}<gradient android:type="linear" '
            f'android:startX="{v(ax)}" android:startY="{v(ay)}" android:endX="{v(bx)}" android:endY="{v(by)}">'
            f'{items}\n{indent}</gradient>\n{indent[:-4]}</aapt:attr>')


def stroke_path(path, width, colour=None, gradient=None, unit=ICON_UNIT):
    head = (f'    <path android:pathData="{path_data(path, unit)}" android:strokeWidth="{v(width * unit)}" '
            f'android:strokeLineCap="round" android:strokeLineJoin="round"')
    if gradient:
        return head + '>' + grad_xml(STOPS, GRAD, unit) + '\n    </path>\n'
    return head + f' android:strokeColor="{colour}"/>\n'


def play_path(unit=ICON_UNIT, mono=None):
    d = 'M' + 'L'.join(f'{v(ICON_CX + (x - CX) * unit)},{v(ICON_CY + (y - CY) * unit)}' for x, y in PLAY) + 'Z'
    head = (f'    <path android:pathData="{d}" android:strokeWidth="{v(W_PLAY * unit)}" '
            f'android:strokeLineJoin="round" android:strokeColor="{mono or PLAY_STROKE}"')
    if mono:
        return head + f' android:fillColor="{mono}"/>\n'
    return head + '>' + grad_xml(PLAY_FILL, (44, 46, 58, 66), unit, attr='android:fillColor') + '\n    </path>\n'


VEC_HEAD = ('<?xml version="1.0" encoding="utf-8"?>\n<!-- Generated by tools/brand/render_aura.py. Do not edit by hand. -->\n'
            '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    xmlns:aapt="http://schemas.android.com/aapt"\n'
            '    android:width="108dp" android:height="108dp"\n'
            '    android:viewportWidth="108" android:viewportHeight="108">\n')


def mark_vector(halo=None, unit=None):
    unit = unit or ICON_UNIT
    s = VEC_HEAD
    s += stroke_path(BRACKET_L, W_BRACKET, gradient=True, unit=unit)
    s += stroke_path(BRACKET_R, W_BRACKET, gradient=True, unit=unit)
    s += stroke_path(LEG_R, W_LEG, gradient=True, unit=unit)
    s += stroke_path(RIBBON, W_LEG + 2 * HALO_RIBBON, colour=halo or HALO, unit=unit)
    s += stroke_path(RIBBON, W_LEG, gradient=True, unit=unit)
    s += play_path(unit)
    return s + '</vector>\n'


def mono_vector():
    s = VEC_HEAD
    for path, w in ((BRACKET_L, W_BRACKET), (BRACKET_R, W_BRACKET), (LEG_R, W_LEG), (RIBBON_MONO, W_LEG)):
        s += stroke_path(path, w, colour='#FFFFFFFF')
    s += play_path(mono='#FFFFFFFF')
    return s + '</vector>\n'


def background_vector():
    return (VEC_HEAD.replace('<vector', '<vector', 1)
            + '    <path android:pathData="M0,0H108V108H0Z">'
            + '\n        <aapt:attr name="android:fillColor">\n'
            + f'            <gradient android:type="radial" android:centerX="54" android:centerY="48" android:gradientRadius="80">\n'
            + f'                <item android:offset="0" android:color="{BG_CENTER}"/>\n'
            + f'                <item android:offset="1" android:color="{BG_EDGE}"/>\n'
            + '            </gradient>\n        </aapt:attr>\n    </path>\n</vector>\n')


ADAPTIVE = ('<?xml version="1.0" encoding="utf-8"?>\n<!-- Generated by tools/brand/render_aura.py. Do not edit by hand. -->\n'
            '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
            '    <background android:drawable="@drawable/aura_icon_background"/>\n'
            '    <foreground android:drawable="@drawable/aura_icon_foreground"/>\n'
            '    <monochrome android:drawable="@drawable/aura_icon_monochrome"/>\n'
            '</adaptive-icon>\n')


# ---- banner / master / previews -----------------------------------------------------------------
def wordmark(text, font_path, px_height, tracking=0.08):
    font = ImageFont.truetype(font_path, px_height)
    adv = [font.getlength(ch) for ch in text]
    gap = px_height * tracking
    w = int(sum(adv) + gap * (len(text) - 1)) + 4
    img = Image.new('L', (w, int(px_height * 1.4)), 0)
    d = ImageDraw.Draw(img)
    x = 2
    for ch, a in zip(text, adv):
        d.text((x, int(px_height * 0.1)), ch, font=font, fill=255)
        x += a + gap
    return img.crop(img.getbbox())


def render_banner(font_path, w=1920, h=1080):
    """320x180 design at 6x. Mark left, wordmark right, both on the brand background."""
    bg = background_img(w, h, w * 0.3, h * 0.45, w * 0.75)
    unit = h * 0.0063                            # mark height ~ 0.5 of the banner
    cx, cy = w * 0.205, h * 0.5
    mark = render_mark(cx, cy, unit * 1.0, w, h, ss=2, glow=0.55)
    bg.alpha_composite(mark)
    wm = wordmark('AURA TV', font_path, int(h * 0.20))
    x = int(w * 0.395)
    y = int(h * 0.5 - wm.height / 2)
    glow = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    gl = Image.new('RGBA', wm.size, (150, 170, 255, 255))
    gl.putalpha(wm)
    glow.paste(gl, (x, y), gl)
    glow = glow.filter(ImageFilter.GaussianBlur(h * 0.012))
    glow.putalpha(glow.getchannel('A').point(lambda a: int(a * 0.7)))
    bg.alpha_composite(glow)
    white = Image.new('RGBA', wm.size, (244, 246, 255, 255))
    white.putalpha(wm)
    bg.alpha_composite(white, (x, y))
    return bg.convert('RGB')


def render_icon(px, shape='full', ss=3, glow=0.5):
    """Launcher icon as the user sees it. shape: full (square, unmasked) | circle | squircle | round."""
    unit = px / 108 * ICON_UNIT
    bg = background_img(px, px, px / 2, px * 0.44, px * 0.74)
    bg.alpha_composite(render_mark(px / 2, px / 2, unit, px, px, ss=ss, glow=glow))
    if shape == 'full':
        return bg
    m = Image.new('L', (px * 4, px * 4), 0)
    d = ImageDraw.Draw(m)
    if shape == 'circle':
        d.ellipse((0, 0, px * 4 - 1, px * 4 - 1), fill=255)
    elif shape == 'round':
        d.rounded_rectangle((0, 0, px * 4 - 1, px * 4 - 1), radius=px * 4 * 0.22, fill=255)
    elif shape == 'squircle':
        n, r = 4.0, px * 2
        pts = [(r + r * np.sign(math.cos(t)) * abs(math.cos(t)) ** (2 / n),
                r + r * np.sign(math.sin(t)) * abs(math.sin(t)) ** (2 / n))
               for t in np.linspace(0, 2 * math.pi, 720)]
        d.polygon(pts, fill=255)
    m = m.resize((px, px), Image.LANCZOS)
    out = Image.new('RGBA', (px, px), (0, 0, 0, 0))
    out.paste(bg, (0, 0), m)
    return out


def launcher_view(px, shape):
    """What a launcher shows: the 72dp centre of the 108dp canvas, cut to `shape`, rendered at px."""
    full = render_icon(int(px * 108 / 72), 'full')
    off = int((full.width - px) / 2)
    cropped = full.crop((off, off, off + px, off + px))
    m = Image.new('L', (px * 4, px * 4), 0)
    d = ImageDraw.Draw(m)
    if shape == 'circle':
        d.ellipse((0, 0, px * 4 - 1, px * 4 - 1), fill=255)
    elif shape == 'round':
        d.rounded_rectangle((0, 0, px * 4 - 1, px * 4 - 1), radius=px * 4 * 0.24, fill=255)
    else:
        n, r = 4.0, px * 2
        pts = [(r + r * np.sign(math.cos(t)) * abs(math.cos(t)) ** (2 / n),
                r + r * np.sign(math.sin(t)) * abs(math.sin(t)) ** (2 / n))
               for t in np.linspace(0, 2 * math.pi, 720)]
        d.polygon(pts, fill=255)
    m = m.resize((px, px), Image.LANCZOS)
    out = Image.new('RGBA', (px, px), (0, 0, 0, 0))
    out.paste(cropped, (0, 0), m)
    return out


def render_splash_name(font_path, w, h):
    """The branding image Android 12+ shows under the splash icon: transparent, white wordmark."""
    k = 4
    wm = wordmark('AURA TV', font_path, int(h * k * 0.36))
    out = Image.new('RGBA', (w * k, h * k), (0, 0, 0, 0))
    white = Image.new('RGBA', wm.size, (244, 246, 255, 255))
    white.putalpha(wm)
    out.alpha_composite(white, ((w * k - wm.width) // 2, (h * k - wm.height) // 2))
    return out.resize((w, h), Image.LANCZOS)


def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        f.write(text)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--res', required=True, help='app/src/main/res')
    ap.add_argument('--extras', required=True, help='extras/brand/aura')
    ap.add_argument('--font', default='/usr/share/fonts/truetype/google-fonts/Poppins-Bold.ttf')
    a = ap.parse_args()

    # vectors + adaptive icon (override the OwnTV eggshell resources by name)
    write(f'{a.res}/drawable/aura_icon_foreground.xml', mark_vector())
    write(f'{a.res}/drawable/aura_icon_background.xml', background_vector())
    write(f'{a.res}/drawable/aura_icon_monochrome.xml', mono_vector())
    write(f'{a.res}/mipmap-anydpi-v26/owntv_icon_eggshell.xml', ADAPTIVE)
    splash = mark_vector(halo=SPLASH_BG)
    write(f'{a.res}/drawable/owntv_splash_static_eggshell.xml', splash)
    write(f'{a.res}/drawable/owntv_splash_eggshell.xml', splash)

    # TV banner
    big = render_banner(a.font)
    for dens, w in (('hdpi', 240), ('xhdpi', 320), ('xxhdpi', 480)):
        d = f'{a.res}/drawable-{dens}'
        os.makedirs(d, exist_ok=True)
        big.resize((w, w * 9 // 16), Image.LANCZOS).save(f'{d}/owntv_banner_eggshell.png', optimize=True)

    for dens, w in (('mdpi', 200), ('hdpi', 300), ('xhdpi', 400), ('xxhdpi', 600), ('xxxhdpi', 800)):
        d = f'{a.res}/drawable-{dens}'
        os.makedirs(d, exist_ok=True)
        render_splash_name(a.font, w, w * 2 // 5).save(f'{d}/owntv_splash_name_eggshell.png', optimize=True)

    # masters + previews
    os.makedirs(a.extras, exist_ok=True)
    render_icon(1024, 'full', ss=2).convert('RGB').save(f'{a.extras}/aura_icon_1024.png', optimize=True)
    render_icon(1024, 'round', ss=2).save(f'{a.extras}/aura_icon_1024_round.png', optimize=True)
    render_icon(512, 'full').convert('RGB').save(f'{a.extras}/aura_icon_512.png', optimize=True)
    big.save(f'{a.extras}/aura_banner_1920x1080.png', optimize=True)

    # preview sheet: launcher masks at several sizes + monochrome + safe-zone check
    sheet = Image.new('RGBA', (1500, 760), (46, 49, 58, 255))
    x = 30
    for shape in ('circle', 'squircle', 'round'):
        sheet.alpha_composite(launcher_view(288, shape), (x, 30)); x += 318
    x, y = 30, 360
    for px in (192, 144, 96, 72, 48, 36):
        sheet.alpha_composite(launcher_view(px, 'squircle'), (x, y + (192 - px) // 2)); x += px + 24
    mono = render_mark(144, 144, 144 / 108 * ICON_UNIT, 288, 288, ss=3, mono='#FFFFFF')
    mono_bg = Image.new('RGBA', (288, 288), (70, 74, 96, 255))
    mono_bg.alpha_composite(mono)
    sheet.alpha_composite(mono_bg, (1130, 30))
    safe = render_icon(432, 'full', ss=2)
    d = ImageDraw.Draw(safe)
    c = 216
    d.ellipse((c - 132, c - 132, c + 132, c + 132), outline=(255, 255, 255, 200), width=2)    # 66dp safe circle
    d.ellipse((c - 144, c - 144, c + 144, c + 144), outline=(255, 210, 90, 160), width=1)     # 72dp visible
    sheet.alpha_composite(safe.resize((300, 300), Image.LANCZOS), (1130, 400))
    sheet.convert('RGB').save(f'{a.extras}/aura_preview.png')
    print('done')


if __name__ == '__main__':
    main()
