#!/usr/bin/env python3
"""
Textures of the sea creatures of Atlantis (DC Universe expansion, v1.12):

* the giant manta ray (3 looks: abyssal, reef and coral), 256x128
* the giant seahorse (8 colors x 3 patterns: plain, spotted, striped), 128x128
* the Atlantean dolphins (6 colors) and their glowing markings (emissive layer), 128x64
* the three eggs (item icons, 16x16)

The box layouts match MantaRayModel, SeahorseModel and AtlanteanDolphinModel. Shares helpers with
generate_textures.py.

Usage:  pip install pillow numpy
        python tools/generate_atlantis_mounts.py
"""
import colorsys
import math
import random

from PIL import Image

from generate_textures import blank, box_faces, jitter, lerp, save

random.seed(1112)

GOLD = (236, 190, 64, 255)
GOLD_D = (168, 120, 30, 255)
TEAL = (40, 190, 180, 255)
TEAL_D = (18, 110, 110, 255)
EYE = (12, 14, 20, 255)
EYE_SHINE = (210, 230, 240, 255)


def px(img, x, y, c):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), c)


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (c[3],)


def hsv(h, s, v, a=255):
    r, g, b = colorsys.hsv_to_rgb(h % 1.0, max(0.0, min(1.0, s)), max(0.0, min(1.0, v)))
    return int(r * 255), int(g * 255), int(b * 255), a


def paint_face(img, rect, fn, noise=5):
    """fn(x, y, w, h) -> color, for every pixel of a face (x, y relative to the face)."""
    x0, y0, w, h = rect
    for y in range(h):
        for x in range(w):
            c = fn(x, y, w, h)
            if c is not None:
                px(img, x0 + x, y0 + y, jitter(c, noise) if noise and c[3] else c)


def saddle(img, u, v, w, h, d, cloth=TEAL, trim=GOLD):
    """An Atlantean saddle: teal cloth with a gold border and a gold trident-like mark on top."""
    f = box_faces(u, v, w, h, d)

    def top(x, y, fw, fh):
        if x in (0, fw - 1) or y in (0, fh - 1):
            return trim
        if x == fw // 2 and 1 < y < fh - 2:
            return GOLD_D
        if y == fh // 2 and abs(x - fw // 2) <= 2:
            return GOLD_D
        return cloth

    paint_face(img, f["top"], top, 4)
    for name in ("front", "back", "left", "right"):
        paint_face(img, f[name], lambda x, y, fw, fh: trim if y == 0 else shade(cloth, 0.8), 4)
    paint_face(img, f["bottom"], lambda *a: shade(cloth, 0.6), 3)


# ------------------------------------------------------------------------------------ manta ray

MANTAS = [
    # top, top accent (shoulder patches / spots), belly, spots that glow faintly
    {"top": (24, 28, 40, 255), "patch": (210, 214, 220, 255), "belly": (236, 238, 236, 255), "spot": (70, 230, 220, 255)},
    {"top": (22, 58, 120, 255), "patch": (90, 160, 220, 255), "belly": (226, 236, 244, 255), "spot": (246, 210, 90, 255)},
    {"top": (96, 32, 70, 255), "patch": (220, 110, 120, 255), "belly": (244, 226, 230, 255), "spot": (110, 240, 230, 255)},
]


def manta_top(look, wing_pos=0.0, span=1.0):
    """Top skin: dark, with pale shoulder patches near the body and scattered bright spots."""
    def fn(x, y, w, h):
        t = (wing_pos + x / max(1, w - 1) * span)
        c = look["top"]
        # Pale "shoulder" chevrons (like an oceanic manta), fading out along the wing.
        if 0.05 < t < 0.55 and y < h * 0.55 and (y + x * 0.4) % 9 < 5:
            c = lerp(c, look["patch"], 0.55 * (1 - t))
        if random.random() < 0.025:
            c = look["spot"]
        # Darker leading edge.
        if y < 1:
            c = shade(c, 0.75)
        return c
    return fn


def make_manta(i, look):
    img = blank(256, 128)
    belly = look["belly"]
    # Body 24x6x34 at (0,0).
    f = box_faces(0, 0, 24, 6, 34)
    paint_face(img, f["top"], manta_top(look, 0.0, 0.3))
    paint_face(img, f["bottom"], lambda x, y, w, h: shade(belly, 0.9) if (x - w // 2) ** 2 + (y - h * 0.3) ** 2 < 6 else belly, 4)
    for name in ("left", "right", "back"):
        paint_face(img, f[name], lambda x, y, w, h: look["top"] if y < h * 0.5 else belly)
    paint_face(img, f["front"], lambda x, y, w, h: look["top"] if y < h * 0.5 else belly)
    # Head 20x4x6 at (120,0): the wide mouth in front, eyes on the sides.
    f = box_faces(120, 0, 20, 4, 6)
    paint_face(img, f["top"], manta_top(look, 0.0, 0.2))
    paint_face(img, f["bottom"], lambda *a: belly, 4)

    def mouth(x, y, w, h):
        if y >= 1 and 2 <= x < w - 2:
            return (20, 16, 24, 255) if y < h - 1 else (60, 50, 60, 255)
        return look["top"]
    paint_face(img, f["front"], mouth, 3)
    for name in ("left", "right"):
        def side(x, y, w, h, name=name):
            ex = 1 if name == "right" else w - 2
            if y == 1 and x == ex:
                return EYE
            if y == 0 and x == ex:
                return EYE_SHINE
            return look["top"] if y < 2 else belly
        paint_face(img, f[name], side, 3)
    paint_face(img, f["back"], lambda *a: look["top"])
    # Horns 3x3x8 at (120,12) and (144,12).
    for u in (120, 144):
        f = box_faces(u, 12, 3, 3, 8)
        for name, rect in f.items():
            paint_face(img, rect, lambda x, y, w, h, name=name: belly if name == "bottom" else shade(look["top"], 1.15))
    # Saddle 12x2x12 at (176,0).
    saddle(img, 176, 0, 12, 2, 12, cloth=TEAL if i != 1 else (30, 120, 150, 255))
    # Wings: inner 20x3x28, mid 16x2x18, tip 12x1x8 (left, right).
    for (u, v, w, h, d, pos, span) in ((0, 42, 20, 3, 28, 0.25, 0.35), (96, 42, 20, 3, 28, 0.25, 0.35),
                                       (0, 76, 16, 2, 18, 0.6, 0.25), (68, 76, 16, 2, 18, 0.6, 0.25),
                                       (136, 76, 12, 1, 8, 0.85, 0.15), (176, 76, 12, 1, 8, 0.85, 0.15)):
        f = box_faces(u, v, w, h, d)
        paint_face(img, f["top"], manta_top(look, pos, span))
        # Belly of the wing: white with a dark trailing edge.
        paint_face(img, f["bottom"], lambda x, y, fw, fh: shade(look["top"], 1.2) if y > fh - 3 else belly, 4)
        for name in ("front", "back", "left", "right"):
            paint_face(img, f[name], lambda x, y, fw, fh: shade(look["top"], 0.85))
    # Tail 2x2x28 at (0,98).
    f = box_faces(0, 98, 2, 2, 28)
    for name, rect in f.items():
        paint_face(img, rect, lambda x, y, w, h: shade(look["top"], 0.9))
    save(img, "entity", "atlantis", f"manta_ray_{i}.png")


# ------------------------------------------------------------------------------------- seahorse

SEAHORSE_HUES = [0.06, 0.13, 0.78, 0.48, 0.92, 0.35, 0.99, 0.6]  # coral, gold, purple, turquoise, pink, emerald, crimson, blue
PATTERNS = ["plain", "spotted", "striped"]


def seahorse_skin(base, pattern, accent, rings=True, offset=0):
    """Ringed skin (the bony plates of a seahorse) with the chosen pattern."""
    light = shade(base, 1.25)
    dark = shade(base, 0.7)

    def fn(x, y, w, h):
        c = base
        yy = y + offset
        if rings and yy % 4 == 0:
            c = dark
        elif rings and yy % 4 == 1:
            c = light
        if pattern == "spotted":
            if (x * 7 + yy * 13 + offset) % 11 == 0 or (x * 5 + yy * 3) % 17 == 0:
                c = accent
        elif pattern == "striped":
            if (yy // 3) % 3 == 0:
                c = lerp(c, accent, 0.7)
        return c
    return fn


def box_skin(img, u, v, w, h, d, base, pattern, accent, offset=0, belly=None, rings=True):
    f = box_faces(u, v, w, h, d)
    for name in ("right", "left", "back", "top", "bottom"):
        paint_face(img, f[name], seahorse_skin(base, pattern, accent, rings, offset), 5)
    front = belly if belly else base
    paint_face(img, f["front"], seahorse_skin(front, pattern if not belly else "plain", accent, rings, offset), 5)
    return f


def make_seahorse(color, pattern):
    hue = SEAHORSE_HUES[color]
    base = hsv(hue, 0.75, 0.85)
    belly = hsv(hue + 0.04, 0.45, 0.98)
    accent = hsv(hue + 0.5, 0.55, 1.0) if pattern == "spotted" else hsv(hue - 0.05, 0.85, 0.5)
    if pattern == "spotted":
        accent = (250, 248, 236, 255) if color % 2 == 0 else hsv(hue + 0.5, 0.6, 1.0)
    fin = hsv(hue + 0.08, 0.35, 1.0, 230)
    img = blank(128, 128)
    # Torso 12x20x11 at (0,0); belly plates 10x16x3 at (48,0).
    box_skin(img, 0, 0, 12, 20, 11, base, PATTERNS[pattern], accent, belly=belly)
    box_skin(img, 48, 0, 10, 16, 3, belly, "plain", accent, offset=2)
    # Neck 8x8x8 at (76,0).
    box_skin(img, 76, 0, 8, 8, 8, base, PATTERNS[pattern], accent, offset=1, belly=belly)
    # Head 9x8x10 at (0,32): eyes on the sides.
    f = box_skin(img, 0, 32, 9, 8, 10, base, PATTERNS[pattern], accent, rings=False)
    for name in ("left", "right"):
        x0, y0, w, h = f[name]
        ex = x0 + (2 if name == "right" else w - 4)
        for dx in range(2):
            for dy in range(2):
                px(img, ex + dx, y0 + 2 + dy, EYE)
        px(img, ex + (0 if name == "right" else 1), y0 + 2, EYE_SHINE)
        # A gold ring around the eye.
        for dx in range(-1, 3):
            px(img, ex + dx, y0 + 1, GOLD_D if dx in (-1, 2) else shade(base, 1.2))
    # Snout 4x4x10 at (40,32): a tube with a dark tip.
    f = box_faces(40, 32, 4, 4, 10)
    for name, rect in f.items():
        paint_face(img, rect, lambda x, y, w, h, name=name: (40, 30, 30, 255) if name == "front"
                   else (shade(base, 0.9) if (x // 2) % 2 else base), 5)
    # Coronet 3x5x3 at (70,32): golden crown of Atlantis.
    f = box_faces(70, 32, 3, 5, 3)
    for rect in f.values():
        paint_face(img, rect, lambda x, y, w, h: GOLD if y % 2 == 0 else GOLD_D, 6)
    # Dorsal fin 1x12x5 at (84,32) and pectoral fins 1x4x4 at (100,32) / (112,32): translucent rays.
    for (u, v, w, h, d) in ((84, 32, 1, 12, 5), (100, 32, 1, 4, 4), (112, 32, 1, 4, 4)):
        f = box_faces(u, v, w, h, d)
        for rect in f.values():
            paint_face(img, rect, lambda x, y, fw, fh: shade(fin, 1.1) if x % 2 == 0 else fin, 4)
    # Saddle 9x3x8 at (48,50).
    saddle(img, 48, 50, 9, 3, 8, cloth=hsv(hue + 0.45, 0.6, 0.55))
    # Tail 9x8x9 (0,52), 7x7x7 (0,70), 5x6x5 (30,70), 4x5x4 (52,70), 3x4x3 (70,70).
    for k, (u, v, w, h, d) in enumerate(((0, 52, 9, 8, 9), (0, 70, 7, 7, 7), (30, 70, 5, 6, 5), (52, 70, 4, 5, 4), (70, 70, 3, 4, 3))):
        tb = lerp(base, belly, 0.1 * k)
        box_skin(img, u, v, w, h, d, tb, PATTERNS[pattern], accent, offset=k * 3, belly=belly)
    index = color * len(PATTERNS) + pattern
    save(img, "entity", "atlantis", f"giant_seahorse_{index}.png")


# -------------------------------------------------------------------------------------- dolphin

DOLPHINS = [
    # body, belly, glow (must match AtlanteanDolphinEntity.GLOW)
    ((40, 120, 200, 255), (200, 236, 250, 255), (79, 245, 255, 255)),   # azure
    ((96, 52, 170, 255), (220, 206, 246, 255), (199, 125, 255, 255)),   # violet
    ((200, 60, 150, 255), (250, 214, 236, 255), (255, 95, 210, 255)),   # magenta
    ((214, 150, 40, 255), (252, 236, 196, 255), (255, 211, 79, 255)),   # gold
    ((26, 140, 96, 255), (200, 246, 220, 255), (92, 255, 138, 255)),    # emerald
    ((220, 92, 60, 255), (252, 222, 206, 255), (255, 138, 79, 255)),    # coral
]

DOLPHIN_BOXES = {
    "body": (0, 0, 10, 10, 20),
    "head": (62, 0, 8, 7, 7),
    "beak": (94, 0, 3, 2, 4),
    "dorsal": (62, 16, 1, 5, 5),
    "left_flipper": (76, 16, 6, 1, 3),
    "right_flipper": (96, 16, 6, 1, 3),
    "tail": (0, 32, 6, 6, 10),
    "tail_end": (34, 32, 4, 4, 6),
    "fluke": (56, 32, 14, 1, 5),
    "saddle": (96, 32, 8, 2, 7),
}


def dolphin_side(body, belly):
    """Countershaded side: body color above, pale belly below, a soft gradient between."""
    def fn(x, y, w, h):
        t = y / max(1, h - 1)
        if t < 0.45:
            return lerp(shade(body, 0.85), body, t / 0.45)
        if t < 0.7:
            return lerp(body, belly, (t - 0.45) / 0.25)
        return belly
    return fn


def make_dolphin(i, colors):
    body, belly, glow = colors
    img = blank(128, 64)
    lum = blank(128, 64)
    for name, (u, v, w, h, d) in DOLPHIN_BOXES.items():
        if name == "saddle":
            saddle(img, u, v, w, h, d, cloth=shade(body, 0.6))
            continue
        f = box_faces(u, v, w, h, d)
        paint_face(img, f["top"], lambda x, y, fw, fh: shade(body, 0.9), 5)
        paint_face(img, f["bottom"], lambda *a: belly, 4)
        for side in ("left", "right", "front", "back"):
            paint_face(img, f[side], dolphin_side(body, belly), 4)
        # Glowing markings (emissive layer): a wavy stripe along each side, a line down the back,
        # rims on the fins and flukes.
        if name in ("body", "tail", "tail_end"):
            for side in ("left", "right"):
                x0, y0, fw, fh = f[side]
                for x in range(fw):
                    y = int(fh * 0.5 + math.sin((x + u) * 0.55) * fh * 0.15)
                    for dy in (0, 1):
                        px(lum, x0 + x, y0 + y + dy, glow)
                        px(img, x0 + x, y0 + y + dy, lerp(glow, (255, 255, 255, 255), 0.3))
            x0, y0, fw, fh = f["top"]
            for y in range(fh):
                if y % 3 != 2:
                    px(lum, x0 + fw // 2, y0 + y, glow)
                    px(img, x0 + fw // 2, y0 + y, glow)
        if name in ("dorsal", "fluke", "left_flipper", "right_flipper"):
            for rect_name in ("left", "right", "front", "back", "top"):
                x0, y0, fw, fh = f[rect_name]
                for x in range(fw):
                    for y in range(fh):
                        edge = (rect_name in ("left", "right") and y == 0) or (rect_name == "top" and (y == fh - 1 or x in (0, fw - 1)))
                        if edge or name == "fluke" and rect_name == "back":
                            px(lum, x0 + x, y0 + y, glow)
                            px(img, x0 + x, y0 + y, glow)
        if name == "head":
            # Eyes, and glowing dots above them.
            for side in ("left", "right"):
                x0, y0, fw, fh = f[side]
                ex = x0 + (1 if side == "right" else fw - 3)
                px(img, ex, y0 + 3, EYE)
                px(img, ex + 1, y0 + 3, EYE)
                px(img, ex + (0 if side == "right" else 1), y0 + 2, EYE_SHINE)
                for dx in (0, 2, 4):
                    gx = x0 + (1 + dx if side == "right" else fw - 2 - dx)
                    if x0 <= gx < x0 + fw:
                        px(lum, gx, y0 + 1, glow)
                        px(img, gx, y0 + 1, glow)
            # A smile along the front.
            x0, y0, fw, fh = f["front"]
            for x in range(1, fw - 1):
                px(img, x0 + x, y0 + fh - 2, shade(belly, 0.7))
    save(img, "entity", "atlantis", f"atlantean_dolphin_{i}.png")
    save(lum, "entity", "atlantis", f"atlantean_dolphin_glow_{i}.png")


# ----------------------------------------------------------------------------------------- eggs

EGG_SHAPE = [
    "......####......",
    ".....######.....",
    "....########....",
    "...##########...",
    "...##########...",
    "..############..",
    "..############..",
    ".##############.",
    ".##############.",
    ".##############.",
    ".##############.",
    ".##############.",
    "..############..",
    "..############..",
    "...##########...",
    ".....######.....",
]


def make_egg(name, base, spots, glow=False):
    img = blank(16, 16)
    rnd = random.Random(name)
    for y, row in enumerate(EGG_SHAPE):
        for x, ch in enumerate(row):
            if ch != "#":
                continue
            # Edge darkening and a highlight at the upper left, like vanilla eggs.
            left = row.index("#")
            right = row.rindex("#")
            edge = x in (left, right) or y in (0, 15) or EGG_SHAPE[y - 1][x] != "#" or (y < 15 and EGG_SHAPE[y + 1][x] != "#")
            c = shade(base, 0.7) if edge else base
            if not edge and x - left <= 1 and y < 8:
                c = shade(base, 1.25)
            img.putpixel((x, y), c)
    for _ in range(9):
        x, y = rnd.randint(3, 12), rnd.randint(3, 13)
        if EGG_SHAPE[y][x] == "#" and EGG_SHAPE[y][x + 1] == "#":
            img.putpixel((x, y), spots)
            img.putpixel((x + 1, y), shade(spots, 0.85))
    if glow:
        for (x, y) in ((6, 5), (9, 8), (5, 11), (10, 12)):
            img.putpixel((x, y), (255, 255, 255, 255))
    save(img, "item", name + ".png")


def main():
    for i, look in enumerate(MANTAS):
        make_manta(i, look)
    for color in range(len(SEAHORSE_HUES)):
        for pattern in range(len(PATTERNS)):
            make_seahorse(color, pattern)
    for i, colors in enumerate(DOLPHINS):
        make_dolphin(i, colors)
    make_egg("manta_ray_spawn_egg", (30, 36, 56, 255), (70, 230, 220, 255))
    make_egg("giant_seahorse_spawn_egg", (240, 120, 50, 255), (250, 214, 90, 255))
    make_egg("atlantean_dolphin_spawn_egg", (50, 120, 210, 255), (200, 120, 255, 255), glow=True)


if __name__ == "__main__":
    main()
