#!/usr/bin/env python3
"""
Textures of Aquaman (DC Universe expansion): the Atlantean Emblem, the suit (orange scale shirt,
green leggings and gloves, gold belt), the Trident of Atlantis (icon and the 3D model's palette),
the great white shark, the Kraken (skin, glowing eyes, water jet) and the power icons. Shares helpers with generate_textures.py.

Usage:  pip install pillow numpy
        python tools/generate_aquaman_textures.py
"""
import math
import random

from PIL import Image, ImageFilter

from generate_textures import blank, box_faces, fill_rect, from_map, jitter, lerp, save

random.seed(1941)

A = {
    ".": (0, 0, 0, 0),
    "O": (18, 30, 28, 255),     # outline
    "o": (138, 60, 10, 255),    # dark orange
    "R": (222, 112, 22, 255),   # scale orange
    "S": (250, 166, 48, 255),   # orange highlight
    "y": (150, 104, 18, 255),   # dark gold
    "Y": (236, 186, 46, 255),   # gold
    "Z": (255, 232, 140, 255),  # light gold
    "g": (16, 78, 46, 255),     # dark green
    "G": (30, 138, 74, 255),    # Atlantean green
    "H": (84, 196, 116, 255),   # green highlight
    "t": (8, 72, 84, 255),      # deep teal
    "T": (20, 150, 160, 255),   # teal
    "U": (110, 226, 220, 255),  # light teal
    "w": (240, 250, 248, 255),
}

EMBLEM = [
    "................",
    ".....yyyyyy.....",
    "...yyZYYYYZyy...",
    "..yZYttTTttYZy..",
    "..yYtTZZZZTtYy..",
    ".yZtTZYttYZTtZy.",
    ".yYtZYtTTtYZtYy.",
    ".yYtZYtTTtYZtYy.",
    ".yYtZZZZZZZZtYy.",
    ".yYtZYtTTtYZtYy.",
    ".yZtZYtTTtYZtZy.",
    "..yYtYytTtyYtYy.",
    "..yZYttTTttYZy..",
    "...yyZYYYYZyy...",
    ".....yyyyyy.....",
    "................",
]

SUIT_ITEM = [
    "................",
    "...ooo....ooo...",
    "..oRSRoooRSRSo..",
    ".oRSRSRSRSRSRSo.",
    ".oSRSRSRSRSRSRo.",
    ".oRSoRSRSRSoSRo.",
    "..gg.SRSRSR.gg..",
    "..GH.RSRSRS.HG..",
    "..gg.SRSRSR.gg..",
    ".....RSRSRS.....",
    ".....YZYYYZY....",
    ".....SRSRSR.....",
    "................",
    "................",
    "................",
    "................",
]

LEGS_ITEM = [
    "................",
    "....YZYYYYZY....",
    "....yYYtTYYy....",
    "....GHGGGGHG....",
    "....GGHGGHGG....",
    "....GHGggGHG....",
    "....GGHggHGG....",
    "....GHG..GHG....",
    "....GGH..HGG....",
    "....GHG..GHG....",
    "....GGH..HGG....",
    "....GHG..GHG....",
    "....gGg..gGg....",
    "................",
    "................",
    "................",
]

BOOTS_ITEM = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "...gG.....gG....",
    "..gGGG...gGGG...",
    "..GHGG...GHGG...",
    "..GGHG...GGHG...",
    "..GHGG...GHGG...",
    "..GGHGG..GGHGG..",
    ".gGHGGGG.GHGGGG.",
    ".gGGGGGg.gGGGGg.",
    "..gggggg.gggggg.",
    "................",
    "................",
]

TRIDENT_ITEM = [
    "...........Z...Z",
    "..........Y...Y.",
    ".........Y...Y..",
    "........Yy..Y...",
    ".........YyY...Z",
    "..........Yy..Y.",
    ".........Y.YyY..",
    "........Yy..Y...",
    ".......Yy.......",
    "......Yy........",
    ".....Tt.........",
    "....Tt..........",
    "...Tt...........",
    "..Yy............",
    ".Yy.............",
    "Zy..............",
]


def paint(img, x0, y0, rows, noise=4):
    for y, row in enumerate(rows):
        for x, c in enumerate(row):
            if c != ".":
                img.putpixel((x0 + x, y0 + y), jitter(A[c], noise) if noise else A[c])


def scale_color(x, y, base, light, dark):
    """Overlapping fish scales: a highlight on the top of each scale, a dark rim below it."""
    row = y // 2
    sx = (x + (row % 2)) % 2
    if y % 2 == 0:
        return light if sx == 0 else base
    return base if sx == 0 else dark


def make_items():
    save(from_map(EMBLEM, A), "item", "aquaman_emblem.png")
    save(from_map(SUIT_ITEM, A), "item", "aquaman_suit.png")
    save(from_map(LEGS_ITEM, A), "item", "aquaman_leggings.png")
    save(from_map(BOOTS_ITEM, A), "item", "aquaman_boots.png")
    save(from_map(TRIDENT_ITEM, A), "item", "aquaman_trident.png")

    # Palette of the 3D trident model: gold, dark grip, teal and the bright tips (8x8 quadrants).
    img = blank(16, 16)
    for y in range(8):
        for x in range(8):
            img.putpixel((x, y), lerp(A["Z"], A["y"], (x + y) / 14) if (x + y) % 5 else A["Y"])
            img.putpixel((8 + x, y), A["O"] if (x + y) % 3 == 0 else (44, 62, 58, 255))
            img.putpixel((x, 8 + y), lerp(A["U"], A["t"], y / 7))
            img.putpixel((8 + x, 8 + y), lerp(A["w"], A["Z"], (x + y) / 14))
    save(img, "item", "aquaman_trident_model.png")


def make_armor():
    orange, light, dark = A["R"], A["S"], A["o"]
    green, green_l, green_d = A["G"], A["H"], A["g"]
    gold, gold_l, gold_d = A["Y"], A["Z"], A["y"]

    # ---- humanoid layer: scale shirt, sleeves with green gloves, boots (64x32). No head piece.
    img = blank(64, 32)
    f = box_faces(16, 16, 8, 12, 4)
    for name, (x0, y0, w, h) in f.items():
        for y in range(h):
            for x in range(w):
                img.putpixel((x0 + x, y0 + y), jitter(scale_color(x, y, orange, light, dark), 4))
    # Gold collar line on the top rows of the shirt.
    for name in ("front", "back", "right", "left"):
        x0, y0, w, h = f[name]
        for x in range(w):
            img.putpixel((x0 + x, y0), jitter(gold if x % 3 else gold_l, 3))

    # ARMS: scale sleeves down to the elbow, then green gloves with a fin.
    f = box_faces(40, 16, 4, 12, 4)
    for name, (x0, y0, w, h) in f.items():
        for y in range(h):
            for x in range(w):
                if name == "top":
                    c = scale_color(x, y, orange, light, dark)
                elif name == "bottom":
                    c = green_d
                elif y < 6:
                    c = scale_color(x, y, orange, light, dark)
                elif y == 6:
                    c = gold if x % 2 else gold_l
                else:
                    c = green_l if (x + y) % 4 == 0 else (green_d if y == 11 else green)
                img.putpixel((x0 + x, y0 + y), jitter(c, 4))
    # Glove fins: a lighter spike on the outer side faces.
    for name in ("right", "left"):
        x0, y0, w, h = f[name]
        for i, y in enumerate(range(7, 11)):
            for x in range(min(w, 1 + i)):
                img.putpixel((x0 + (w - 1 - x if name == "left" else x), y0 + y), A["H"])

    # BOOTS (leg box on this layer): green boots with fins up the calf.
    f = box_faces(0, 16, 4, 12, 4)
    for name, (x0, y0, w, h) in f.items():
        if name == "top":
            continue
        if name == "bottom":
            fill_rect(img, x0, y0, w, h, green_d, 4)
            continue
        for y in range(6, h):
            for x in range(w):
                c = green_l if y == 6 else (green_d if y == 11 else green)
                if (x + y) % 4 == 0 and 6 < y < 11:
                    c = green_l
                img.putpixel((x0 + x, y0 + y), jitter(c, 4))
    save(img, "entity", "equipment", "humanoid", "aquaman.png")

    # ---- leggings layer: the gold belt with the "A" buckle and green scale legs.
    img = blank(64, 32)
    f = box_faces(16, 16, 8, 12, 4)
    for name, (x0, y0, w, h) in f.items():
        if name in ("top", "bottom"):
            continue
        for y in range(8, 12):
            for x in range(w):
                c = gold_l if y == 8 else (gold_d if y == 11 else gold)
                img.putpixel((x0 + x, y0 + y), jitter(c, 3))
    fx, fy, fw, fh = f["front"]
    paint(img, fx, fy + 8, [
        "YYZtTZYY",
        "YZtZZtZY",
        "YZtTTtZY",
        "yyZyyZyy",
    ], noise=0)
    f = box_faces(0, 16, 4, 12, 4)
    for name, (x0, y0, w, h) in f.items():
        if name == "bottom":
            continue
        for y in range(h):
            for x in range(w):
                c = scale_color(x, y, green, green_l, green_d)
                img.putpixel((x0 + x, y0 + y), jitter(c, 4))
    save(img, "entity", "equipment", "humanoid_leggings", "aquaman.png")


# ------------------------------------------------------------------------------------- shark

SHARK_TOP = (92, 106, 120, 255)
SHARK_TOP_D = (64, 76, 90, 255)
SHARK_BELLY = (232, 234, 228, 255)
MOUTH = (120, 28, 36, 255)
TOOTH = (250, 250, 240, 255)


def shark_side(img, x0, y0, w, h, line=0.55, front=None):
    """Countershading: grey-blue above a ragged line, white below."""
    for x in range(w):
        edge = line * h + math.sin((x0 + x) * 0.7) * 0.8 + random.uniform(-0.6, 0.6)
        for y in range(h):
            if y < edge:
                c = lerp(SHARK_TOP_D, SHARK_TOP, min(1.0, y / max(1.0, edge * 0.6)))
            else:
                c = SHARK_BELLY
            img.putpixel((x0 + x, y0 + y), jitter(c, 5))


def shark_box(img, u, v, w, h, d, line=0.55, belly=True):
    f = box_faces(u, v, w, h, d)
    fill_rect(img, *f["top"], SHARK_TOP_D, 5)
    fill_rect(img, *f["bottom"], SHARK_BELLY if belly else SHARK_TOP, 4)
    for name in ("right", "left", "front", "back"):
        shark_side(img, *f[name], line=line)
    return f


def make_shark():
    img = blank(256, 128)
    # Torso 18x18x32 at (0,0).
    shark_box(img, 0, 0, 18, 18, 32, line=0.5)
    # Skull 16x14x14 at (100,0): eyes near the front, gill slits at the back.
    f = shark_box(img, 100, 0, 16, 14, 14, line=0.55)
    for name in ("right", "left"):
        x0, y0, w, h = f[name]
        front_x = 2 if name == "right" else w - 3
        for dx in range(2):
            for dy in range(2):
                img.putpixel((x0 + front_x + dx, y0 + 4 + dy), (8, 8, 10, 255))
        img.putpixel((x0 + front_x + (1 if name == "right" else 0), y0 + 4), (180, 190, 200, 255))
        for i in range(4):
            gx = x0 + (w - 3 - i * 2 if name == "right" else 2 + i * 2)
            for gy in range(4, 10):
                img.putpixel((gx, y0 + gy), (52, 62, 72, 255))
    # Snout 12x9x8 at (160,0): nostrils on the front, mouth roof underneath.
    f = shark_box(img, 160, 0, 12, 9, 8, line=0.6)
    fill_rect(img, *f["bottom"], MOUTH, 6)
    fx, fy, fw, fh = f["front"]
    img.putpixel((fx + 3, fy + 3), (30, 34, 40, 255))
    img.putpixel((fx + fw - 4, fy + 3), (30, 34, 40, 255))
    # Upper teeth 11x2x8 at (160,17): a row of white triangles.
    f = box_faces(160, 17, 11, 2, 8)
    for name, (x0, y0, w, h) in f.items():
        for y in range(h):
            for x in range(w):
                tooth = name in ("top", "bottom") or (x + (1 if y else 0)) % 2 == 0
                img.putpixel((x0 + x, y0 + y), TOOTH if tooth else (0, 0, 0, 0))
    # Jaw 14x4x16 at (100,28): pale outside, red inside (top face).
    f = shark_box(img, 100, 28, 14, 4, 16, line=0.15)
    fill_rect(img, *f["top"], MOUTH, 6)
    # Lower teeth 12x2x12 at (164,50).
    f = box_faces(164, 50, 12, 2, 12)
    for name, (x0, y0, w, h) in f.items():
        for y in range(h):
            for x in range(w):
                tooth = name in ("top", "bottom") or (x + (0 if y else 1)) % 2 == 0
                img.putpixel((x0 + x, y0 + y), TOOTH if tooth else (0, 0, 0, 0))
    # Tail 14x14x16 at (0,50), tail end 8x8x14 at (60,50).
    shark_box(img, 0, 50, 14, 14, 16, line=0.55)
    shark_box(img, 60, 50, 8, 8, 14, line=0.6)
    # Fins: all grey-blue, darker towards the tips.
    for (u, v, w, h, d) in ((104, 50, 2, 18, 8), (124, 50, 2, 10, 6), (140, 50, 2, 14, 10)):
        f = box_faces(u, v, w, h, d)
        for name, (x0, y0, fw, fh) in f.items():
            for y in range(fh):
                for x in range(fw):
                    t = y / max(1, fh - 1)
                    tip = 1 - t if (u, v) == (124, 50) else t
                    c = lerp(SHARK_TOP_D, SHARK_TOP, tip) if (u, v) != (140, 50) else lerp(SHARK_TOP_D, SHARK_TOP, t)
                    img.putpixel((x0 + x, y0 + y), jitter(c, 5))
    for (u, v) in ((0, 80), (0, 91)):
        f = box_faces(u, v, 16, 2, 9)
        for name, (x0, y0, fw, fh) in f.items():
            for y in range(fh):
                for x in range(fw):
                    c = SHARK_BELLY if name == "bottom" else lerp(SHARK_TOP, SHARK_TOP_D, x / max(1, fw - 1))
                    img.putpixel((x0 + x, y0 + y), jitter(c, 5))
    save(img, "entity", "great_white_shark.png")


# -------------------------------------------------------------------------------------- kraken

KRAKEN_DARK = (58, 14, 30, 255)
KRAKEN_SKIN = (112, 26, 46, 255)
KRAKEN_MID = (150, 44, 60, 255)
KRAKEN_LIGHT = (196, 92, 88, 255)
KRAKEN_BELLY = (222, 150, 128, 255)
KRAKEN_SPOT = (236, 196, 160, 255)
SUCKER = (244, 206, 190, 255)
SUCKER_RIM = (176, 92, 96, 255)
SUCKER_HOLE = (96, 30, 44, 255)
HOOK = (250, 244, 226, 255)
EYE_GOLD = (250, 196, 40, 255)
EYE_GLOW = (255, 236, 120, 255)
EYE_RIM = (40, 8, 16, 255)


def kraken_noise(x, y, seed=0.0):
    """Cheap smooth mottling: a few sines, 0..1."""
    v = (math.sin(x * 0.55 + seed) + math.sin(y * 0.47 + seed * 1.7) + math.sin((x + y) * 0.31 + seed * 0.3)
         + math.sin((x - y) * 0.23 + seed * 2.1)) / 8.0 + 0.5
    return max(0.0, min(1.0, v))


def kraken_skin(img, x0, y0, w, h, shade=0.5, spots=True, seed=0.0, vertical=False):
    """Mottled crimson skin with darker veins and pale spots; `shade` darkens (0) or lightens (1)."""
    for y in range(h):
        for x in range(w):
            gx, gy = x0 + x, y0 + y
            n = kraken_noise(gx, gy, seed)
            t = n * 0.6 + shade * 0.5 - 0.1
            if vertical:
                t += (y / max(1, h - 1)) * 0.25 - 0.12
            if t < 0.35:
                c = lerp(KRAKEN_DARK, KRAKEN_SKIN, t / 0.35)
            elif t < 0.7:
                c = lerp(KRAKEN_SKIN, KRAKEN_MID, (t - 0.35) / 0.35)
            else:
                c = lerp(KRAKEN_MID, KRAKEN_LIGHT, min(1.0, (t - 0.7) / 0.3))
            # Veins: thin dark ridges where the mottling crosses the middle.
            if abs(kraken_noise(gx * 1.9, gy * 1.9, seed + 4.0) - 0.5) < 0.025:
                c = lerp(c, KRAKEN_DARK, 0.6)
            img.putpixel((gx, gy), jitter(c, 4))
    if spots:
        for _ in range(max(1, w * h // 90)):
            sx, sy = x0 + random.randrange(w), y0 + random.randrange(h)
            img.putpixel((sx, sy), jitter(KRAKEN_SPOT, 8))
            if random.random() < 0.4 and sx + 1 < x0 + w:
                img.putpixel((sx + 1, sy), jitter(lerp(KRAKEN_SPOT, KRAKEN_LIGHT, 0.5), 6))


def kraken_box(img, u, v, w, h, d, seed=0.0, belly_bottom=True):
    f = box_faces(u, v, w, h, d)
    kraken_skin(img, *f["top"], shade=0.25, seed=seed)
    if belly_bottom:
        for (x0, y0, fw, fh) in (f["bottom"],):
            for y in range(fh):
                for x in range(fw):
                    img.putpixel((x0 + x, y0 + y), jitter(lerp(KRAKEN_BELLY, KRAKEN_LIGHT, kraken_noise(x0 + x, y0 + y, seed) * 0.6), 5))
    else:
        kraken_skin(img, *f["bottom"], shade=0.4, seed=seed + 1)
    for name in ("right", "front", "left", "back"):
        kraken_skin(img, *f[name], shade=0.45, seed=seed + 2, vertical=True)
    return f


def sucker_column(img, x0, y0, w, h, big=False):
    """Two rows of suckers down a face (the underside of an arm)."""
    for y in range(h):
        for x in range(w):
            img.putpixel((x0 + x, y0 + y), jitter(lerp(KRAKEN_BELLY, KRAKEN_LIGHT, 0.25 + 0.2 * math.sin(y * 1.3)), 5))
    cols = [w // 2] if w <= 3 else [w // 4, w - 1 - w // 4]
    for row, y in enumerate(range(0, h, 2)):
        for i, cx in enumerate(cols):
            if (row + i) % 2 and len(cols) > 1:
                continue
            img.putpixel((x0 + cx, y0 + y), SUCKER_HOLE if big else SUCKER_RIM)
            if cx + 1 < w and big:
                img.putpixel((x0 + cx + 1, y0 + y), SUCKER_RIM)
            if y + 1 < h:
                img.putpixel((x0 + cx, y0 + y + 1), SUCKER)


def eye_face(img, x0, y0, w, h, glow=False):
    """A golden eye with a horizontal slit pupil (the eyes layer only keeps the bright parts)."""
    cx, cy = (w - 1) / 2.0, (h - 1) / 2.0
    for y in range(h):
        for x in range(w):
            r = math.hypot((x - cx) / (w / 2.0), (y - cy) / (h / 2.0))
            if glow:
                c = (0, 0, 0, 0) if r > 1.05 else lerp(EYE_GLOW, EYE_GOLD, min(1.0, r))
                if abs(y - cy) < 0.6 and abs(x - cx) < w * 0.4:
                    c = (0, 0, 0, 0)
            else:
                c = EYE_RIM if r > 0.95 else lerp(EYE_GLOW, EYE_GOLD, r)
                if abs(y - cy) < 0.6 and abs(x - cx) < w * 0.4:
                    c = (10, 4, 6, 255)
            img.putpixel((x0 + x, y0 + y), c)


def make_kraken():
    img = blank(256, 128)
    eyes = blank(256, 128)
    # Head 16x14x16 at (0,0) and its lower part 14x3x14 at (64,0).
    f = kraken_box(img, 0, 0, 16, 14, 16, seed=0.3)
    # Brow ridges over the eyes and a darker band around the front.
    fx, fy, fw, fh = f["front"]
    for x in range(fw):
        img.putpixel((fx + x, fy + 3), jitter(KRAKEN_DARK, 4))
        img.putpixel((fx + x, fy + 4), jitter(lerp(KRAKEN_DARK, KRAKEN_SKIN, 0.5), 4))
    kraken_box(img, 64, 0, 14, 3, 14, seed=1.1)
    # Eyes 2x5x5 at (120,0) and (134,0): the eye on both 5x5 side faces.
    for u in (120, 134):
        f = box_faces(u, 0, 2, 5, 5)
        for name in ("top", "bottom", "front", "back"):
            fill_rect(img, *f[name], EYE_RIM, 3)
        for name in ("right", "left"):
            eye_face(img, *f[name])
            eye_face(eyes, *f[name], glow=True)
    # Siphon 5x5x5 at (148,0), its lip 7x7x1 at (168,0): dark opening on the front.
    f = kraken_box(img, 148, 0, 5, 5, 5, seed=2.0)
    f = kraken_box(img, 168, 0, 7, 7, 1, seed=2.5)
    fx, fy, fw, fh = f["front"]
    for y in range(fh):
        for x in range(fw):
            r = math.hypot(x - 3, y - 3)
            if r < 2.2:
                img.putpixel((fx + x, fy + y), lerp(SUCKER_HOLE, (20, 4, 10, 255), 1 - r / 2.2))
            elif r < 3.2:
                img.putpixel((fx + x, fy + y), jitter(KRAKEN_LIGHT, 5))
    # Mantle: base 18x10x14 (0,30), mid 20x8x18 (0,56), top 16x6x14 (80,30), tip 10x5x9 (80,56), point 5x4x5 (120,56).
    kraken_box(img, 0, 30, 18, 10, 14, seed=3.0, belly_bottom=False)
    kraken_box(img, 0, 56, 20, 8, 18, seed=3.7, belly_bottom=False)
    kraken_box(img, 80, 30, 16, 6, 14, seed=4.4, belly_bottom=False)
    kraken_box(img, 80, 56, 10, 5, 9, seed=5.1, belly_bottom=False)
    kraken_box(img, 120, 56, 5, 4, 5, seed=5.8, belly_bottom=False)
    # Fins 9x1x12 at (140,30) and (140,44): thin, ragged trailing edge (cut out), veined.
    for v in (30, 44):
        f = box_faces(140, v, 9, 1, 12)
        for name, (x0, y0, w, h) in f.items():
            for y in range(h):
                for x in range(w):
                    t = x / max(1, w - 1) if name in ("top", "bottom") else 0.5
                    c = lerp(KRAKEN_MID, KRAKEN_LIGHT, t * 0.7)
                    if name in ("top", "bottom") and (y % 3 == 0):
                        c = lerp(c, KRAKEN_DARK, 0.4)
                    img.putpixel((x0 + x, y0 + y), jitter(c, 5))
            if name in ("top", "bottom"):
                # Ragged outer edge.
                outer = w - 1 if v == 30 else 0
                for y in range(h):
                    if (y * 7) % 5 < 2:
                        img.putpixel((x0 + outer, y0 + y), (0, 0, 0, 0))
    # Arms: six segments sharing one strip at v=84; the -Z (front) face is the sucker side.
    widths = (6, 5, 4, 3, 3, 2)
    lengths = (6, 6, 6, 5, 5, 4)
    us = (0, 24, 44, 60, 72, 84)
    for j, (w, l, u) in enumerate(zip(widths, lengths, us)):
        f = box_faces(u, 84, w, l, w)
        kraken_skin(img, *f["top"], shade=0.3, seed=6.0 + j)
        kraken_skin(img, *f["bottom"], shade=0.6, seed=6.5 + j, spots=False)
        for name in ("right", "left", "back"):
            kraken_skin(img, *f[name], shade=0.4 + j * 0.05, seed=7.0 + j)
        sucker_column(img, *f["front"], big=w >= 4)
        # A pale fringe along the edges of the sucker side.
        for name in ("right", "left"):
            x0, y0, fw, fh = f[name]
            edge = 0 if name == "right" else fw - 1
            for y in range(fh):
                img.putpixel((x0 + edge, y0 + y), jitter(KRAKEN_BELLY, 6))
    # Hunting tentacles: 4x5x4 (0,100), 3x5x3 (16,100), 2x5x2 (28,100) and the club 6x8x6 (36,100).
    for (u, w, l) in ((0, 4, 5), (16, 3, 5), (28, 2, 5)):
        f = box_faces(u, 100, w, l, w)
        for name in ("top", "bottom", "right", "left", "back"):
            kraken_skin(img, *f[name], shade=0.4, seed=9.0 + u)
        sucker_column(img, *f["front"])
    f = box_faces(36, 100, 6, 8, 6)
    for name in ("top", "right", "left", "back"):
        kraken_skin(img, *f[name], shade=0.5, seed=11.0)
    sucker_column(img, *f["front"], big=True)
    # Hooks around the club's suckers and on its tip.
    fx, fy, fw, fh = f["front"]
    for y in range(1, fh, 3):
        img.putpixel((fx, fy + y), HOOK)
        img.putpixel((fx + fw - 1, fy + y + 1 if y + 1 < fh else fy + y), HOOK)
    bx, by, bw, bh = f["bottom"]
    for y in range(bh):
        for x in range(bw):
            img.putpixel((bx + x, by + y), HOOK if (x + y) % 3 == 0 else jitter(KRAKEN_BELLY, 5))
    save(img, "entity", "kraken.png")
    save(eyes, "entity", "kraken_eyes.png")

    # The water jet: soft streaks that tile along the stream (white, tinted when drawn).
    jet = blank(16, 64)
    for x in range(16):
        edge = 1.0 - abs(x - 7.5) / 8.0
        phase = random.uniform(0, 6.28)
        freq = random.choice((1, 2, 3))
        for y in range(64):
            streak = 0.55 + 0.45 * math.sin(phase + y * 2 * math.pi * freq / 64)
            a = int(255 * max(0.0, min(1.0, edge * 1.4)) * (0.35 + 0.65 * streak))
            shade = int(200 + 55 * streak)
            jet.putpixel((x, y), (shade, shade, 255, a))
    save(jet, "entity", "kraken_jet.png")


# --------------------------------------------------------------------------------------- gui

def make_gui():
    """Power icons (16x16 each): trident, shark, call of the sea, portal to Atlantis, Kraken, then the emblem."""
    img = blank(96, 16)
    trident = [
        "..Z....Z....Z...",
        "..Y....Y....Y...",
        ".ZYZ..ZYZ..ZYZ..",
        "..Y....Y....Y...",
        "..Y....Y....Y...",
        "..YYYYYYYYYYY...",
        "...yYYYZYYYy....",
        ".......Y........",
        ".......Y........",
        "......TUT.......",
        ".......Y........",
        ".......Y........",
        ".......Y........",
        "......tTt.......",
        ".......Y........",
        "......ZYZ.......",
    ]
    shark = [
        "................",
        "......w.........",
        ".....wU.........",
        "....wUU.........",
        "...wUUU.........",
        "..wUUUU.........",
        ".wUUUUU.........",
        "wUUUUUUw........",
        "UUUUUUUUUww.....",
        "..TTTUUUUUUUww..",
        "TTTTTTTTTTTTTTT.",
        ".TtTTtTTTtTTtTTT",
        "..tt..tt..tt..t.",
        "................",
        "TtTTtTTTtTTTtTTt",
        "................",
    ]
    sea_call = [
        "................",
        "......wwww......",
        "....ww....ww....",
        "...w..UUUU..w...",
        "..w.UU....UU.w..",
        "..w.U..ww..U.w..",
        ".w.U..wUUw..U.w.",
        ".w.U.wU..Uw.U.w.",
        ".w.U.wU..Uw.U.w.",
        ".w.U..wUUw..U.w.",
        "..w.U..ww..U.w..",
        "..w.UU....UU.w..",
        "...w..UUUU..w...",
        "....ww....ww....",
        "......wwww......",
        "................",
    ]
    portal = [
        "................",
        ".....YYYYYY.....",
        "...YYttttttYY...",
        "..YttTTTTTTttY..",
        ".YtTTUUUUUUTTtY.",
        ".YtTUUwwwwUUTtY.",
        "YtTUUw.TT.wUUTtY",
        "YtTUw.TUUT.wUTtY",
        "YtTUw.TUUT.wUTtY",
        "YtTUUw.TT.wUUTtY",
        ".YtTUUwwwwUUTtY.",
        ".YtTTUUUUUUTTtY.",
        "..YttTTTTTTttY..",
        "...YYttttttYY...",
        ".....YYYYYY.....",
        "................",
    ]
    kraken = [
        "......RRRR......",
        ".....RSSSRR.....",
        "....RSSRRRRo....",
        "....RSRRRRRo....",
        "....RRRRRRoo....",
        "...YoRRRRRoY....",
        "...ZOooRRooZ....",
        "....ooRRRRoo....",
        "...oRoRooRoRo...",
        "..oR.oR..Ro.Ro..",
        "..R..oR..Ro..R..",
        ".oR.oR....Ro.Ro.",
        ".R..oR....Ro..R.",
        ".R.oR......Ro.R.",
        "..oR........Ro..",
        "..R..........R..",
    ]
    for i, rows in enumerate((trident, shark, sea_call, portal, kraken, EMBLEM)):
        icon = from_map(rows, A)
        if i < 5:
            # Deep sea glow behind the icon so it reads on any background.
            glow = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
            mask = icon.split()[3].filter(ImageFilter.MaxFilter(3))
            glow.paste((10, 90, 110, 200), (0, 0), mask)
            icon = Image.alpha_composite(glow, icon)
        img.paste(icon, (i * 16, 0), icon)
    save(img, "gui", "aquaman_powers.png")


if __name__ == "__main__":
    make_items()
    make_armor()
    make_shark()
    make_gui()
    make_kraken()
