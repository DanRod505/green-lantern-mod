#!/usr/bin/env python3
"""
Textures of Superman (DC Universe expansion): the Kryptonian Crystal, the suit (blue with the S
shield, red trunks, yellow belt, red boots, black hair), his particles, the power icons and the
see-through box of the X-ray vision. Shares helpers with generate_textures.py.

Usage:  pip install pillow numpy
        python tools/generate_superman_textures.py
"""
import math
import random

from generate_textures import blank, box_faces, fill_rect, from_map, jitter, lerp, save

random.seed(1938)

S = {
    ".": (0, 0, 0, 0),
    "O": (8, 14, 40, 255),      # outline
    "b": (18, 44, 120, 255),    # dark blue
    "B": (34, 82, 196, 255),    # Superman blue
    "C": (78, 132, 236, 255),   # blue highlight
    "r": (120, 10, 18, 255),    # dark red
    "R": (204, 22, 32, 255),    # red
    "Q": (240, 70, 70, 255),    # red highlight
    "y": (170, 120, 10, 255),   # dark gold
    "Y": (246, 196, 26, 255),   # yellow
    "Z": (255, 240, 150, 255),  # light yellow
    "k": (14, 14, 20, 255),     # black hair
    "K": (40, 42, 56, 255),     # hair shine
    "s": (226, 176, 140, 255),  # skin
    "w": (240, 250, 255, 255),  # white
    "c": (150, 250, 240, 255),  # crystal light
    "t": (60, 200, 220, 255),   # crystal
    "T": (20, 110, 150, 255),   # crystal dark
}

CRYSTAL = [
    "................",
    ".......w........",
    "......wcO.......",
    "......wcO.......",
    ".....wccTO......",
    ".....wctTO......",
    "....wcctTTO.....",
    "....wcttTTO.....",
    "....wcttTTO.....",
    "....wcttTTO.....",
    "....wcctTTO.....",
    ".....wctTO......",
    ".....wctTO......",
    "......wTO.......",
    "......wTO.......",
    ".......O........",
]

HAIR_ITEM = [
    "................",
    "................",
    "....kkkkkkkk....",
    "...kKKkkkkKkk...",
    "..kkkkkkkkkkkk..",
    "..kkkkkkkkkkkk..",
    "..kkkssssssskk..",
    "..kkss.ss.sskk..",
    "..kkssssssssk...",
    "...ksssskssss...",
    "....sssssss.....",
    ".....sssss......",
    "................",
    "................",
    "................",
    "................",
]

SUIT_ITEM = [
    "................",
    "...OOO....OOO...",
    "..OBBCOOOOCBBO..",
    ".OBCBRRRRRRBCBO.",
    ".OBBRYYYYYYRBBO.",
    ".OBbOYRRRRYOBbO.",
    "..OO.RYRYYR.OO..",
    "....ORYYRRYO....",
    "....OBRYYRBO....",
    "....OBBRRBBO....",
    "....OBBBBBBO....",
    "....YYZYYZYY....",
    "....ORRRRRRO....",
    "....ORrOOrRO....",
    ".....OO..OO.....",
    "................",
]

LEGS_ITEM = [
    "................",
    "....OOOOOOOO....",
    "....YZYYYYZY....",
    "....ORRRRRRO....",
    "....ORQRRQRO....",
    "....OBBOOBBO....",
    "....OBCOOBCO....",
    "....OBBOOBBO....",
    "....OBBOOBBO....",
    "....OBBOOBBO....",
    "....OBBOOBBO....",
    "....OBBOOBBO....",
    "....ObBOObBO....",
    "....ObbOObbO....",
    "....OOOOOOOO....",
    "................",
]

BOOTS_ITEM = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "...OOOO..OOOO...",
    "...RQRO..ORQR...",
    "...RRRO..ORRR...",
    "...QRRO..ORRQ...",
    "...RRRO..ORRR...",
    "...RRRRO.ORRRRO.",
    "..ORRRRROORRRRRO",
    "..OQRRRROORRRRRO",
    "..OrrrrrOOrrrrrO",
    "...OOOOO..OOOOO.",
    "................",
]

# The S shield, 8 pixels wide (rows of the chest).
SHIELD = [
    "YYYYYYYY",
    "YRRRRRRY",
    "YRYYYYRY",
    ".YRRRRY.",
    ".YYYYRY.",
    "..YRRY..",
    "...YY...",
]


def make_items():
    save(from_map(CRYSTAL, S), "item", "kryptonian_crystal.png")
    save(from_map(HAIR_ITEM, S), "item", "superman_hair.png")
    save(from_map(SUIT_ITEM, S), "item", "superman_suit.png")
    save(from_map(LEGS_ITEM, S), "item", "superman_leggings.png")
    save(from_map(BOOTS_ITEM, S), "item", "superman_boots.png")


BLUE = S["B"]
BLUE_D = S["b"]
BLUE_L = S["C"]
RED = S["R"]
RED_D = S["r"]
RED_L = S["Q"]
GOLD = S["Y"]
GOLD_L = S["Z"]
HAIR = S["k"]
HAIR_L = S["K"]


def paint(img, x0, y0, rows, noise=4):
    for y, row in enumerate(rows):
        for x, c in enumerate(row):
            if c != ".":
                img.putpixel((x0 + x, y0 + y), jitter(S[c], noise) if noise else S[c])


def make_armor():
    # ---- humanoid layer: hair, chest, arms, boots (64x32)
    img = blank(64, 32)
    f = box_faces(0, 0, 8, 8, 8)
    # Hair: black on top, the back and the upper sides; the face stays the player's, apart from the
    # famous curl on the forehead.
    x, y, w, h = f["top"]
    for yy in range(h):
        for xx in range(w):
            img.putpixel((x + xx, y + yy), jitter(HAIR_L if (xx + yy * 3) % 7 == 0 else HAIR, 4))
    x, y, w, h = f["back"]
    for yy in range(6):
        for xx in range(w):
            img.putpixel((x + xx, y + yy), jitter(HAIR if yy < 5 else (HAIR if xx % 2 else (0, 0, 0, 0)), 4))
    for name in ("right", "left"):
        x, y, w, h = f[name]
        for yy in range(4):
            for xx in range(w):
                if yy < 2 or (name == "right" and xx < 6 - yy) or (name == "left" and xx > 1 + yy):
                    img.putpixel((x + xx, y + yy), jitter(HAIR, 4))
    fx, fy, _, _ = f["front"]
    paint(img, fx, fy, [
        "kkkkkkkk",
        "kkKkkkkk",
        "k..k...k",
        "...k....",
    ])

    # BODY: blue with the S shield on the chest, the yellow belt below.
    f = box_faces(16, 16, 8, 12, 4)
    for name, (x, y, w, h) in f.items():
        fill_rect(img, x, y, w, h, BLUE, 5)
    fx, fy, fw, fh = f["front"]
    paint(img, fx, fy + 1, SHIELD, 2)
    for xx in range(fw):
        img.putpixel((fx + xx, fy), jitter(BLUE_L, 4))
        img.putpixel((fx + xx, fy + 9), jitter(GOLD_L if xx in (3, 4) else GOLD, 3))
        img.putpixel((fx + xx, fy + 10), jitter(RED, 4))
        img.putpixel((fx + xx, fy + 11), jitter(RED_D, 4))
    bx, by, bw, bh = f["back"]
    paint(img, bx, by, [
        "BCBBBBCB",
        "BBCBBCBB",
        "BBBBBBBB",
        "bBBBBBBb",
        "BBBBBBBB",
        "BBBBBBBB",
        "bBBBBBBb",
        "BBBBBBBB",
        "bBBBBBBb",
        "YYYYYYYY",
        "RRRRRRRR",
        "rRRRRRRr",
    ])
    for face in ("right", "left"):
        sx, sy, sw, sh = f[face]
        for xx in range(sw):
            img.putpixel((sx + xx, sy + 9), GOLD)
            img.putpixel((sx + xx, sy + 10), RED)
            img.putpixel((sx + xx, sy + 11), RED_D)

    # ARMS: blue sleeves down to the wrist, with a soft highlight on the shoulders.
    f = box_faces(40, 16, 4, 12, 4)
    for name, (x, y, w, h) in f.items():
        if name in ("top", "bottom"):
            fill_rect(img, x, y, w, h, BLUE if name == "top" else BLUE_D, 5)
            continue
        for yy in range(h):
            for xx in range(w):
                c = BLUE_L if yy == 0 else (BLUE_D if yy >= 10 else BLUE)
                img.putpixel((x + xx, y + yy), jitter(c, 5))

    # BOOTS (leg box on this layer): red boots up to below the knee, with the notch in front.
    f = box_faces(0, 16, 4, 12, 4)
    for name, (x, y, w, h) in f.items():
        if name == "top":
            continue
        if name == "bottom":
            fill_rect(img, x, y, w, h, RED_D, 4)
            continue
        for yy in range(h):
            for xx in range(w):
                top = 6
                if name == "front" and xx in (1, 2):
                    top = 7  # the V notch of the boot
                if yy >= top:
                    c = RED_L if yy == top else (RED_D if yy == 11 else RED)
                    img.putpixel((x + xx, y + yy), jitter(c, 5))
    save(img, "entity", "equipment", "humanoid", "superman.png")

    # ---- leggings layer: red trunks with the yellow belt, blue legs.
    img = blank(64, 32)
    f = box_faces(16, 16, 8, 12, 4)
    for name, (x, y, w, h) in f.items():
        if name in ("top", "bottom"):
            continue
        for yy in range(h):
            for xx in range(w):
                if yy >= 8:
                    c = GOLD if yy == 8 else RED
                    if yy == 8 and name == "front" and xx in (3, 4):
                        c = GOLD_L
                    img.putpixel((x + xx, y + yy), jitter(c, 4))
    f = box_faces(0, 16, 4, 12, 4)
    for name, (x, y, w, h) in f.items():
        if name == "top":
            fill_rect(img, x, y, w, h, RED, 4)
            continue
        if name == "bottom":
            continue
        for yy in range(h):
            for xx in range(w):
                if yy < 3:
                    c = RED if yy < 2 else RED_D  # the trunks cover the top of the legs
                else:
                    c = BLUE
                    if name in ("right", "left") and xx == 1:
                        c = BLUE_D
                    if name == "front" and yy in (5, 6) and xx in (1, 2):
                        c = BLUE_L  # knee
                img.putpixel((x + xx, y + yy), jitter(c, 4))
    save(img, "entity", "equipment", "humanoid_leggings", "superman.png")


def soft_dot(n, sharp=1.6, core=0.0):
    img = blank(n, n)
    for y in range(n):
        for x in range(n):
            d = math.hypot(x - n / 2 + 0.5, y - n / 2 + 0.5) / (n / 2)
            if d < 1:
                a = (1 - d) ** sharp
                a = min(1.0, a + (core if d < 0.3 else 0))
                img.putpixel((x, y), (255, 255, 255, int(255 * a)))
    return img


def make_particles():
    # White sparks (tinted red-hot or icy in game): a cross of light shrinking over the frames.
    for i in range(4):
        n = 8
        img = blank(n, n)
        reach = [3.6, 3.0, 2.2, 1.4][i]
        for y in range(n):
            for x in range(n):
                dx = abs(x - 3.5)
                dy = abs(y - 3.5)
                d = math.hypot(dx, dy)
                cross = max(0.0, 1 - min(dx, dy) / 0.9) * max(0.0, 1 - max(dx, dy) / reach)
                blob = max(0.0, 1 - d / (reach * 0.6))
                a = min(1.0, cross + blob)
                if a > 0.03:
                    img.putpixel((x, y), (255, 255, 255, int(255 * a)))
        save(img, "particle", f"super_spark_{i}.png")
    # Soft glows (sunlight motes, frost puffs).
    for i, sharp in enumerate((1.2, 1.7, 2.4)):
        save(soft_dot(8, sharp, 0.2 if i == 0 else 0.0), "particle", f"super_glow_{i}.png")
    # White ground shockwave (the lantern's is green): a dusty ring.
    n = 32
    img = blank(n, n)
    for y in range(n):
        for x in range(n):
            d = math.hypot(x - n / 2 + 0.5, y - n / 2 + 0.5) / (n / 2)
            ring = math.exp(-((d - 0.84) / 0.09) ** 2)
            inner = max(0.0, 1 - abs(d - 0.55) / 0.3) * 0.22 * (1 if d < 0.84 else 0)
            a = min(1.0, ring + inner) * (0.85 + 0.15 * random.random())
            if a > 0.02:
                img.putpixel((x, y), (255, 255, 255, int(255 * a)))
    save(img, "particle", "super_shockwave.png")


def make_misc():
    # X-ray box: bright edges, a faint fill, tinted per vertex by what the box shows.
    n = 16
    img = blank(n, n)
    for y in range(n):
        for x in range(n):
            e = min(x, y, n - 1 - x, n - 1 - y)
            if e == 0:
                a = 1.0
            elif e == 1:
                a = 0.55
            else:
                a = 0.12 + 0.05 * ((x + y) % 2)
            img.putpixel((x, y), (255, 255, 255, int(255 * a)))
    save(img, "misc", "xray_box.png")


def icon(rows, pal):
    return from_map(rows, pal)


def make_gui():
    """Power icons (16x16 each): heat vision, super punch, super breath, X-ray, then the S shield."""
    pal = dict(S)
    pal["e"] = (255, 60, 30, 255)
    pal["E"] = (255, 190, 120, 255)
    pal["i"] = (170, 230, 255, 255)
    pal["I"] = (90, 170, 240, 255)
    pal["x"] = (120, 200, 255, 255)
    pal["X"] = (40, 110, 220, 255)
    heat = [
        "................",
        "................",
        "..kkk......kkk..",
        ".kwwwk....kwwwk.",
        ".kwEek....keEwk.",
        "..kek......kek..",
        "...eE......Ee...",
        "....eE....Ee....",
        ".....eE..Ee.....",
        "......eEEe......",
        ".......EE.......",
        "......eEEe......",
        ".....eEwwEe.....",
        "......eEEe......",
        ".......ee.......",
        "................",
    ]
    punch = [
        "................",
        "..w..........w..",
        "...w...ww...w...",
        "........w.......",
        ".w...OOOOOO...w.",
        "....OBBBBBBO....",
        "...OBCBCBCBBO...",
        "...OBBBBBBBBO...",
        "...OBBBBBBBBO...",
        "..wOBBBBBBBBOw..",
        "...ObBBBBBBbO...",
        "....ObbbbbbO....",
        "....ORRRRRRO....",
        "...w.ORRRRO.w...",
        "..w...OOOO...w..",
        "................",
    ]
    breath = [
        "................",
        "................",
        "..........i.....",
        ".ss.....i...I...",
        "ssss..i..I..i...",
        "ssss.iiIIIIiiii.",
        "sssIiiIIiiIIIi..",
        "sssiwwwwwwwwwwi.",
        "sssIiiIIiiIIIi..",
        "ssss.iiIIIIiiii.",
        "ssss..i..I..i...",
        ".ss.....i...I...",
        "..........i.....",
        "................",
        "................",
        "................",
    ]
    xray = [
        "................",
        "..XXXXXXXXXXXX..",
        "..X..........X..",
        "..X...xxxx...X..",
        "..X..xwwwwx..X..",
        "..X..xwxxwx..X..",
        "..X..xwwwwx..X..",
        "..X...xwwx...X..",
        "..X..xxwwxx..X..",
        "..X.x.xwwx.x.X..",
        "..X...xwwx...X..",
        "..X..x.xx.x..X..",
        "..X..x....x..X..",
        "..X..........X..",
        "..XXXXXXXXXXXX..",
        "................",
    ]
    shield = [
        "................",
        "..OOOOOOOOOOOO..",
        ".OYYYYYYYYYYYYO.",
        ".OYRRRRRRRRRRYO.",
        ".OYRYYYYYYYYRYO.",
        ".OYRYRRRRRRRRYO.",
        "..OYRRRRRRRRYO..",
        "..OYYYYYYYYRYO..",
        "...OYRRRRRRYO...",
        "...OYRRRRRRYO...",
        "....OYYYYRYO....",
        ".....OYRRYO.....",
        "......OYYO......",
        ".......OO.......",
        "................",
        "................",
    ]
    img = blank(80, 16)
    for i, rows in enumerate((heat, punch, breath, xray, shield)):
        img.paste(icon(rows, pal), (i * 16, 0))
    save(img, "gui", "superman_powers.png")


if __name__ == "__main__":
    make_items()
    make_armor()
    make_particles()
    make_misc()
    make_gui()
