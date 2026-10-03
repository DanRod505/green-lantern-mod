#!/usr/bin/env python3
"""
Textures of the Flash (DC Universe expansion): ring, suit, Speed Force particles, the tornado wind,
the speed trail and the power icons. Shares helpers with generate_textures.py.

Usage:  pip install pillow numpy
        python tools/generate_flash_textures.py
"""
import math
import random

from PIL import Image, ImageDraw, ImageFilter

from generate_textures import blank, box_faces, fill_rect, from_map, jitter, lerp, save

random.seed(1940)

F = {
    ".": (0, 0, 0, 0),
    "O": (46, 6, 4, 255),       # outline
    "r": (122, 12, 10, 255),    # dark red
    "R": (196, 24, 18, 255),    # Flash red
    "S": (232, 62, 40, 255),    # red highlight
    "y": (176, 112, 14, 255),   # dark gold
    "Y": (250, 196, 36, 255),   # gold
    "Z": (255, 236, 140, 255),  # light gold
    "w": (250, 246, 232, 255),  # emblem white
    "k": (20, 12, 10, 255),
}

RING = [
    "................",
    ".....OOOOOO.....",
    "....OrRRRRrO....",
    "...OrRwwwwRrO...",
    "...ORwwYYwwRO...",
    "...ORwYYwwwRO...",
    "...ORwwYYYwRO...",
    "...ORwwwYYwRO...",
    "...OrRwwYwRrO...",
    "...yOrRRRRrOy...",
    "..yZYOOOOOOYZy..",
    ".yZY........YZy.",
    ".yY..........Yy.",
    ".yZY........YZy.",
    "..yZZYYYYYZZZy..",
    "...yyyyyyyyyy...",
]

MASK_ITEM = [
    "................",
    "................",
    "....OOOOOOOO....",
    "...ORRRSSRRRO...",
    "..ORRSSSSSSRRO..",
    "YYORSRRRRRRSROYY",
    ".ZYRRRRRRRRRRYZ.",
    "..YRkkRRRRkkRY..",
    "...ORkkRRkkRO...",
    "...ORRRRRRRRO...",
    "...OrR....RrO...",
    "....Or....rO....",
    "................",
    "................",
    "................",
    "................",
]

SUIT_ITEM = [
    "................",
    "...OOO....OOO...",
    "..ORRSOOOOSRRO..",
    ".ORSRRwwwwRRSRO.",
    ".ORRRwwYYwwRRRO.",
    ".ORrOwYYwwwOrRO.",
    "..OO.wwYYYw.OO..",
    "....OwwwYYwO....",
    "....ORwwYwRO....",
    "....ORRRRRRO....",
    "....YZYYYYZY....",
    "....ORRRRRRO....",
    "....ORRrrRRO....",
    "....ORrOOrRO....",
    ".....OO..OO.....",
    "................",
]

LEGS_ITEM = [
    "................",
    "....OOOOOOOO....",
    "....YZYYYYZY....",
    "....ORRRRRRO....",
    "....ORSRRSRO....",
    "....ORSOORSO....",
    "....ORSOORSO....",
    "....ORROORRO....",
    "....ORROORRO....",
    "....ORROORRO....",
    "....ORROORRO....",
    "....ORROORRO....",
    "....OrROORrO....",
    "....OrrOOrrO....",
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
    "..ZYZYO..OYZYZ..",
    "...YYYO..OYYY...",
    "...ZYYO..OYYZ...",
    "...YYYO..OYYY...",
    "...YYYYO.OYYYYO.",
    "..OZYYYYOOYYYYYO",
    "..OYYYYYOOYYYYYO",
    "..OyyyyyOOyyyyyO",
    "...OOOOO..OOOOO.",
    "................",
]


def make_items():
    save(from_map(RING, F), "item", "flash_ring.png")
    save(from_map(MASK_ITEM, F), "item", "flash_mask.png")
    save(from_map(SUIT_ITEM, F), "item", "flash_suit.png")
    save(from_map(LEGS_ITEM, F), "item", "flash_leggings.png")
    save(from_map(BOOTS_ITEM, F), "item", "flash_boots.png")


RED = F["R"]
RED_D = F["r"]
RED_L = F["S"]
GOLD = F["Y"]
GOLD_L = F["Z"]
GOLD_D = F["y"]


def paint(img, x0, y0, rows, noise=4):
    for y, row in enumerate(rows):
        for x, c in enumerate(row):
            if c != ".":
                img.putpixel((x0 + x, y0 + y), jitter(F[c], noise) if noise else F[c])


def make_armor():
    # ---- humanoid layer: cowl, chest, arms, boots (64x32)
    img = blank(64, 32)
    f = box_faces(0, 0, 8, 8, 8)
    # Cowl: red all around, eyes and the lower face (mouth, chin) left open.
    for name in ("top", "back", "right", "left"):
        x, y, w, h = f[name]
        fill_rect(img, x, y, w, h, RED, 5)
    fx, fy, _, _ = f["front"]
    paint(img, fx, fy, [
        "RRRSSRRR",
        "RRSRRSRR",
        "RRRRRRRR",
        "R..RR..R",
        "RR.RR.RR",
        "r......r",
        "........",
        "........",
    ])
    # Lightning "wings" on the sides of the cowl.
    for name, flip in (("right", False), ("left", True)):
        sx, sy, sw, sh = f[name]
        wing = [
            "......ZY",
            "....ZYY.",
            "..YYYYZ.",
            ".ZYYY...",
            "YYY.....",
        ]
        for y, row in enumerate(wing):
            for x, c in enumerate(row):
                if c != ".":
                    xx = sw - 1 - x if flip else x
                    img.putpixel((sx + xx, sy + 1 + y), F[c])
        # Lower sides stay open (cheeks).
        for y in range(6, 8):
            for x in range(sw):
                img.putpixel((sx + x, sy + y), (0, 0, 0, 0))
    bx, by, bw, bh = f["back"]
    for y in range(7, 8):
        for x in range(bw):
            img.putpixel((bx + x, by + y), RED_D)
    bt = f["bottom"]
    # (bottom stays transparent: the neck)

    # BODY: red with the emblem and the lightning belt.
    f = box_faces(16, 16, 8, 12, 4)
    for name, (x, y, w, h) in f.items():
        fill_rect(img, x, y, w, h, RED, 5)
    fx, fy, fw, fh = f["front"]
    paint(img, fx, fy, [
        "RSRRRRSR",
        "RRwwwwRR",
        "RwwYYwwR",
        "RwYYwwwR",
        "RwwYYYwR",
        "RwwwYYwR",
        "RRwwYwRR",
        "rRRwwRRr",
        "YZYYYYZY",
        "RRRYYRRR",
        "RRRRRRRR",
        "rRRRRRRr",
    ])
    bx, by, bw, bh = f["back"]
    paint(img, bx, by, [
        "RSRRRRSR",
        "RRSRRSRR",
        "RRRSSRRR",
        "RRRRRRRR",
        "RRRRRRRR",
        "rRRRRRRr",
        "rRRRRRRr",
        "RRRRRRRR",
        "YZYYYYZY",
        "RRRRRRRR",
        "RRRRRRRR",
        "rRRRRRRr",
    ])
    for face in ("right", "left"):
        sx, sy, sw, sh = f[face]
        for x in range(sw):
            img.putpixel((sx + x, sy + 8), GOLD)

    # ARMS: red sleeves with a gold lightning band at the wrist.
    f = box_faces(40, 16, 4, 12, 4)
    for name, (x, y, w, h) in f.items():
        if name in ("top", "bottom"):
            fill_rect(img, x, y, w, h, RED if name == "top" else RED_D, 5)
            continue
        for yy in range(h):
            for xx in range(w):
                if yy == 8:
                    c = GOLD_L if (xx + yy) % 2 == 0 else GOLD
                elif yy == 0:
                    c = RED_L
                elif yy >= 10:
                    c = RED_D
                else:
                    c = RED
                img.putpixel((x + xx, y + yy), jitter(c, 5))

    # BOOTS (leg box on this layer): gold boots with a lightning flare.
    f = box_faces(0, 16, 4, 12, 4)
    for name, (x, y, w, h) in f.items():
        if name == "top":
            continue
        if name == "bottom":
            fill_rect(img, x, y, w, h, GOLD_D, 4)
            continue
        for yy in range(h):
            for xx in range(w):
                if yy >= 6:
                    c = GOLD_L if yy == 6 else (GOLD_D if yy == 11 else GOLD)
                    if name in ("right", "left") and yy == 7 and xx in (1, 2):
                        c = GOLD_L
                    img.putpixel((x + xx, y + yy), jitter(c, 5))
    save(img, "entity", "equipment", "humanoid", "flash.png")

    # ---- leggings layer: belt area and red legs with seams.
    img = blank(64, 32)
    f = box_faces(16, 16, 8, 12, 4)
    for name, (x, y, w, h) in f.items():
        if name in ("top", "bottom"):
            continue
        for yy in range(h):
            for xx in range(w):
                if yy >= 7:
                    c = GOLD if yy == 8 else RED
                    if yy == 8 and xx % 3 == 0:
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
                c = RED
                if name in ("right", "left") and xx == 1:
                    c = RED_D
                if name == "front" and yy in (4, 5) and xx in (1, 2):
                    c = RED_L  # knee
                img.putpixel((x + xx, y + yy), jitter(c, 4))
    save(img, "entity", "equipment", "humanoid_leggings", "flash.png")


def make_particles():
    # Crackling sparks: little zigzags of lightning.
    shapes = [
        ["...Z....", "...ZY...", "..YwZ...", ".YwwwZ..", "..ZwwY..", "...ZY...", "...Y....", "........"],
        ["....Y...", "...Z....", "..Zw....", "..wwZ...", "....wZ..", "....Z...", "...Y....", "........"],
        ["........", ".Y......", "..Zw....", "...wZ...", "...Zw...", "....wZ..", "......Y.", "........"],
        ["........", "........", "...ZY...", "..ZwwZ..", "..ZwwZ..", "...YZ...", "........", "........"],
    ]
    for i, rows in enumerate(shapes):
        save(from_map(rows, F), "particle", f"speed_spark_{i}.png")
    # Streaks of displaced air: soft horizontal smears, orange to white.
    for i in range(3):
        img = blank(8, 8)
        length = [7.5, 6, 4][i]
        for y in range(8):
            for x in range(8):
                dy = abs(y - 3.5) / 1.6
                dx = abs(x - 3.5) / (length / 2)
                d = math.hypot(dx, dy)
                if d < 1:
                    c = lerp((255, 255, 240), (255, 120, 30), d)
                    img.putpixel((x, y), (c[0], c[1], c[2], int(255 * (1 - d) ** 0.7)))
        save(img, "particle", f"speed_streak_{i}.png")
    # White ring (tinted gold in game) for the sound barrier and landings.
    n = 64
    img = blank(n, n)
    for y in range(n):
        for x in range(n):
            d = math.hypot(x - n / 2 + 0.5, y - n / 2 + 0.5) / (n / 2)
            ring = math.exp(-((d - 0.82) / 0.07) ** 2)
            inner = max(0.0, 1 - abs(d - 0.6) / 0.25) * 0.25
            a = min(1.0, ring + inner * (1 if d < 0.82 else 0))
            if a > 0.01:
                img.putpixel((x, y), (255, 255, 255, int(255 * a)))
    save(img, "particle", "speed_ring.png")


def make_entity_textures():
    # Neutral trail: white core across V, fading to transparent edges (tinted per vertex).
    img = blank(16, 32)
    for y in range(32):
        v = (y + 0.5) / 32
        d = abs(v - 0.5) / 0.5
        alpha = math.exp(-(d / 0.55) ** 2)
        for x in range(16):
            img.putpixel((x, y), (255, 255, 255, int(255 * alpha)))
    save(img, "entity", "speed_trail.png")

    # Glow: soft white radial blob.
    n = 32
    img = blank(n, n)
    for y in range(n):
        for x in range(n):
            d = math.hypot(x - n / 2 + 0.5, y - n / 2 + 0.5) / (n / 2)
            if d < 1:
                a = (1 - d) ** 1.8
                img.putpixel((x, y), (255, 255, 255, int(255 * a)))
    save(img, "entity", "speed_glow.png")

    # Wind: horizontal streaks that tile along U (wrapped around the funnel), faint dust between.
    w, h = 128, 64
    img = blank(w, h)
    px = img.load()
    streaks = []
    for _ in range(46):
        streaks.append((random.random() * w, random.random() * h, 18 + random.random() * 60, 0.8 + random.random() * 2.2,
                        0.35 + random.random() * 0.65))
    acc = [[0.0] * w for _ in range(h)]
    for sx, sy, length, thick, strength in streaks:
        for y in range(h):
            for xo in range(int(length)):
                x = int(sx + xo) % w
                yy = sy + xo * 0.12  # slight slant: the wind climbs while it spins
                dy = abs(((y - yy) + h / 2) % h - h / 2)
                if dy < thick * 2:
                    t = xo / length
                    k = math.exp(-(dy / thick) ** 2) * math.sin(t * math.pi) * strength
                    acc[y][x] += k
    for y in range(h):
        for x in range(w):
            k = min(1.0, acc[y][x] + 0.08 + 0.05 * random.random())
            px[x, y] = (255, 255, 255, int(255 * k))
    save(img, "entity", "speed_wind.png")


def make_gui():
    """Power icons (16x16 each): tornado, phasing, lightning, then the lightning emblem."""
    img = blank(64, 16)
    tornado = [
        "................",
        ".ZwwwwwwwwwwwwZ.",
        "..YZZwwwwwwZZY..",
        "...Y.ZZwwZZ.Y...",
        "....YZwwwwZY....",
        ".....YZZZZY.....",
        "....Y.ZwwZ.Y....",
        ".....YZwwZY.....",
        "......ZwwZ......",
        ".....Y.ZZ.Y.....",
        "......YZZY......",
        ".......ZZ.......",
        "......Y.ZY......",
        ".......ZY.......",
        ".......Y........",
        "................",
    ]
    phase = [
        "..Y....ww....Y..",
        ".Y....wwww....Y.",
        "Y.....wwww.....Y",
        ".Y.....ww.....Y.",
        "..Y..wwwwww..Y..",
        ".Y..wwwwwwww..Y.",
        "Y..ww.wwww.ww..Y",
        ".Y.ww.wwww.ww.Y.",
        "..Y...wwww...Y..",
        ".Y....w..w....Y.",
        "Y....ww..ww....Y",
        ".Y...w....w...Y.",
        "..Y..w....w..Y..",
        ".Y..ww....ww..Y.",
        "Y..............Y",
        "................",
    ]
    bolt = [
        "..........ZZY...",
        ".........ZwY....",
        "........ZwY.....",
        ".......ZwY......",
        "......ZwwY......",
        ".....ZwwwwwwZ...",
        "....YYYYwwwZ....",
        ".........wZ.....",
        "........wZ......",
        ".......wZ.......",
        "......wZ........",
        ".....wZ.........",
        "....ZZ..........",
        "...Z............",
        "..Y.............",
        "................",
    ]
    emblem = [
        ".....OOOOOO.....",
        "...OOwwwwwwOO...",
        "..OwwwwwwYYwwO..",
        ".OwwwwwwYYwwwwO.",
        ".OwwwwwYYwwwwwO.",
        "OwwwwwYYwwwwwwwO",
        "OwwwwYYYYYYYwwwO",
        "OwwwwwwwYYYwwwwO",
        "OwwwwwwYYYwwwwwO",
        "OwwwwwYYYwwwwwwO",
        ".OwwwwYYwwwwwwO.",
        ".OwwwYYwwwwwwwO.",
        "..OwwYwwwwwwwO..",
        "...OOwwwwwwOO...",
        ".....OOOOOO.....",
        "................",
    ]
    for i, rows in enumerate((tornado, phase, bolt, emblem)):
        icon = from_map(rows, F)
        if i < 3:
            # Red glow behind the white/gold icon so it reads on any background.
            glow = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
            mask = icon.split()[3].filter(ImageFilter.MaxFilter(3))
            glow.paste((200, 30, 20, 200), (0, 0), mask)
            icon = Image.alpha_composite(glow, icon)
        img.paste(icon, (i * 16, 0), icon)
    save(img, "gui", "flash_powers.png")


if __name__ == "__main__":
    make_items()
    make_armor()
    make_particles()
    make_entity_textures()
    make_gui()
