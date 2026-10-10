#!/usr/bin/env python3
"""
Textures of Wonder Woman (DC Universe expansion): the Tiara of Themyscira, the armor (red bodice
with the golden eagle, blue skirt with white stars, red and white boots, silver bracelets, black hair
under the tiara), the Amazon sword and shield and the power icons. Shares helpers with
generate_textures.py.

Usage:  pip install pillow numpy
        python tools/generate_wonder_woman_textures.py
"""
import math
import random

from generate_textures import blank, box_faces, fill_rect, from_map, jitter, save

random.seed(1941)

W = {
    ".": (0, 0, 0, 0),
    "O": (40, 8, 14, 255),      # outline
    "r": (122, 12, 26, 255),    # dark red
    "R": (200, 20, 46, 255),    # Amazon red
    "Q": (240, 74, 84, 255),    # red highlight
    "y": (168, 112, 12, 255),   # dark gold
    "Y": (242, 183, 28, 255),   # gold
    "Z": (255, 232, 140, 255),  # light gold
    "b": (16, 36, 110, 255),    # dark blue
    "B": (34, 72, 190, 255),    # blue
    "C": (90, 132, 236, 255),   # blue highlight
    "w": (246, 246, 250, 255),  # white
    "g": (120, 126, 138, 255),  # dark silver
    "G": (196, 202, 212, 255),  # silver
    "H": (236, 240, 246, 255),  # silver highlight
    "k": (16, 12, 14, 255),     # black hair
    "K": (52, 44, 50, 255),     # hair shine
    "s": (214, 160, 122, 255),  # skin
    "n": (110, 70, 40, 255),    # leather / bronze
    "N": (160, 112, 60, 255),   # bronze highlight
}

TIARA = [
    "................",
    "................",
    "................",
    "................",
    ".......ZZ.......",
    "......ZRRZ......",
    ".....ZYRRYZ.....",
    "..yYYYYQRYYYYy..",
    ".yYZZYYRRYYZZYy.",
    "yYy....YY....yYy",
    "Yy............yY",
    "y..............y",
    "................",
    "................",
    "................",
    "................",
]

HAIR_ITEM = [
    "................",
    "....kkkkkkkk....",
    "...kKkkkkkkKk...",
    "..kkkkkkkkkkkk..",
    "..kYYYYRRYYYYk..",
    "..kkkssssssskk..",
    ".kkkss.ss.sskkk.",
    ".kkkssssssssskk.",
    ".kkk.ssssssskkk.",
    ".kkk..sssss.kkk.",
    ".kkk........kkk.",
    ".kkk........kkk.",
    "..kk........kk..",
    "..k..........k..",
    "................",
    "................",
]

SUIT_ITEM = [
    "................",
    "................",
    "...sss....sss...",
    "..sGss....ssGs..",
    "..ORRROOOORRRO..",
    "..OYZYYYYYYZYO..",
    "..ORYZYRRYZYRO..",
    "..ORRYZRRZYRRO..",
    "..ORRRYYYYRRRO..",
    "...ORRRRRRRRO...",
    "...ORRRRRRRRO...",
    "...OYYZYYZYYO...",
    "...OBBBBBBBBO...",
    "....OOOOOOOO....",
    "................",
    "................",
]

LEGS_ITEM = [
    "................",
    "................",
    "....OOOOOOOO....",
    "....OYYZZYYO....",
    "...OBBwBBBwBO...",
    "...OBwwwBBBBO...",
    "..OBBBwBBwBBBO..",
    "..OBBBBBwwwBBO..",
    "..ObBwBBBwBBbO..",
    "..ObbbbbbbbbbO..",
    "...OOssOOssOO...",
    "....OssOOssO....",
    ".....OO..OO.....",
    "................",
    "................",
    "................",
]

BOOTS_ITEM = [
    "................",
    "................",
    "...wwww..wwww...",
    "...RwwR..RwwR...",
    "...RwRR..RRwR...",
    "...RwRR..RRwR...",
    "...RwRR..RRwR...",
    "...QRRR..RRRQ...",
    "...RRRR..RRRR...",
    "...RRRR..RRRR...",
    "...RRRRO.ORRRR..",
    "..ORRRRROORRRRRO",
    "..OQRRRROORRRRRO",
    "..OrrrrrOOrrrrrO",
    "...OOOOO..OOOOO.",
    "................",
]

SWORD = [
    "..............H.",
    ".............HGg",
    "............HGg.",
    "...........HGg..",
    "..........HGg...",
    ".........HGg....",
    "........HGg.....",
    ".......HGg......",
    "..y...HGg.......",
    "..yY.HGg........",
    "...yYGg.........",
    "....nYy.........",
    "...nNyY.........",
    "..nNn..y........",
    ".YNn............",
    "YZ..............",
]

SHIELD_ITEM = [
    "................",
    ".....GGGGGG.....",
    "...GGHRRRRHGG...",
    "..GHRRRRRRRRHG..",
    "..GRRRYYYYRRRG..",
    ".GRRRYRRRRYRRRG.",
    ".GRRYRRZZRRYRRG.",
    ".GRRYRZZZZRYRRG.",
    ".GRRYRZZZZRYRRG.",
    ".GRRYRRZZRRYRRG.",
    ".GRRRYRRRRYRRRG.",
    "..GRRRYYYYRRRG..",
    "..GgRRRRRRRRgG..",
    "...GGgRRRRgGG...",
    ".....GGGGGG.....",
    "................",
]


def make_items():
    save(from_map(TIARA, W), "item", "amazon_tiara.png")
    save(from_map(HAIR_ITEM, W), "item", "wonder_woman_hair.png")
    save(from_map(SUIT_ITEM, W), "item", "wonder_woman_suit.png")
    save(from_map(LEGS_ITEM, W), "item", "wonder_woman_leggings.png")
    save(from_map(BOOTS_ITEM, W), "item", "wonder_woman_boots.png")
    save(from_map(SWORD, W), "item", "amazon_sword.png")
    save(from_map(SHIELD_ITEM, W), "item", "amazon_shield.png")
    make_shield_model()


def make_shield_model():
    """Texture of the 3D shield held on the arm (32x32): the round face (top left 16x16), a silver strip
    for the rim and the bronze back (to its right)."""
    img = blank(32, 32)
    n = 16
    for y in range(n):
        for x in range(n):
            d = math.hypot(x - 7.5, y - 7.5)
            if d > 7.9:
                continue
            if d > 6.6:
                c = W["H"] if (x + y) % 5 == 0 else W["G"]
            elif d > 5.9:
                c = W["g"]
            elif d > 3.6:
                c = W["Q"] if d > 5.4 and y < 7 else W["R"]
            elif d > 2.7:
                c = W["Y"]
            else:
                c = W["R"]
            img.putpixel((x, y), jitter(c, 4))
    # The five-pointed star in the middle.
    for y in range(n):
        for x in range(n):
            px, py = x - 7.5, y - 7.5
            ang = math.atan2(py, px) + math.pi / 2
            r = math.hypot(px, py)
            k = (ang % (2 * math.pi / 5)) / (2 * math.pi / 5)
            if r < max(1.3, 2.9 - 1.6 * abs(1 - 2 * k) * 1.0):
                img.putpixel((x, y), jitter(W["Z"] if r < 1.3 else W["Y"], 3))
    # Rim strip (sides of the plate) and the bronze back.
    fill_rect(img, 16, 0, 16, 4, W["G"], 4)
    fill_rect(img, 16, 4, 16, 12, W["n"], 6)
    for x in range(16, 32):
        img.putpixel((x, 0), jitter(W["H"], 3))
    save(img, "item", "amazon_shield_model.png")


RED = W["R"]
RED_D = W["r"]
RED_L = W["Q"]
GOLD = W["Y"]
GOLD_L = W["Z"]
GOLD_D = W["y"]
BLUE = W["B"]
BLUE_D = W["b"]
BLUE_L = W["C"]
WHITE = W["w"]
SILVER = W["G"]
SILVER_D = W["g"]
SILVER_L = W["H"]
HAIR = W["k"]
HAIR_L = W["K"]


def paint(img, x0, y0, rows, noise=4):
    for y, row in enumerate(rows):
        for x, c in enumerate(row):
            if c != ".":
                img.putpixel((x0 + x, y0 + y), jitter(W[c], noise) if noise else W[c])


# The golden eagle on the chest (8 pixels wide): the wings spread out into a W shape.
EAGLE = [
    "ZYYYYYYZ",
    "RYZYYZYR",
    "RRYZZYRR",
    "RRRYYRRR",
]


def make_armor():
    # ---- humanoid layer: hair and tiara, bodice, bracelets, boots (64x32)
    img = blank(64, 32)
    f = box_faces(0, 0, 8, 8, 8)
    # Long black hair: the top, the whole back and the sides; the face stays the player's.
    x, y, w, h = f["top"]
    for yy in range(h):
        for xx in range(w):
            img.putpixel((x + xx, y + yy), jitter(HAIR_L if (xx * 2 + yy) % 7 == 0 else HAIR, 4))
    x, y, w, h = f["back"]
    for yy in range(h):
        for xx in range(w):
            img.putpixel((x + xx, y + yy), jitter(HAIR_L if (xx + yy * 2) % 9 == 0 else HAIR, 4))
    for name in ("right", "left"):
        x, y, w, h = f[name]
        for yy in range(h):
            for xx in range(w):
                # The hair falls behind the ears (the half towards the back).
                back = xx < 4 if name == "right" else xx >= 4
                if yy < 2 or back or yy >= 6:
                    img.putpixel((x + xx, y + yy), jitter(HAIR, 4))
        # The tiara's band runs round the sides.
        for xx in range(w):
            img.putpixel((x + xx, y + 2), jitter(GOLD, 3))
    fx, fy, _, _ = f["front"]
    paint(img, fx, fy, [
        "kkkkkkkk",
        "kKkYYkkk",
        "YYYRRYYY",
        "y..ZZ..y",
    ], 2)

    # BODY: the red bodice with the golden eagle, a gold belt; the shoulders stay bare.
    f = box_faces(16, 16, 8, 12, 4)
    for name, (x, y, w, h) in f.items():
        if name == "top":
            continue
        if name == "bottom":
            fill_rect(img, x, y, w, h, GOLD_D, 3)
            continue
        for yy in range(h):
            for xx in range(w):
                if yy < 1 and name in ("right", "left"):
                    continue
                if yy == 0 and name == "front" and xx in (0, 7):
                    continue
                c = RED_L if yy == 1 else (GOLD if yy in (9, 10) else RED)
                if yy == 11:
                    c = RED_D
                img.putpixel((x + xx, y + yy), jitter(c, 5))
    fx, fy, fw, fh = f["front"]
    paint(img, fx, fy + 2, EAGLE, 2)
    for xx in range(fw):
        img.putpixel((fx + xx, fy + 9), jitter(GOLD_L if xx in (3, 4) else GOLD, 3))
        img.putpixel((fx + xx, fy + 10), jitter(GOLD_D, 3))
    bx, by, bw, bh = f["back"]
    paint(img, bx, by, [
        "........",
        "QRRRRRRQ",
        "RRRRRRRR",
        "RRrRRrRR",
        "RRRRRRRR",
        "rRRRRRRr",
        "RRRRRRRR",
        "RRRRRRRR",
        "rRRRRRRr",
        "YYYYYYYY",
        "yyyyyyyy",
        "rrrrrrrr",
    ])

    # ARMS: bare, with the silver Bracelets of Submission on the forearms (a gold star on the outside).
    f = box_faces(40, 16, 4, 12, 4)
    for name, (x, y, w, h) in f.items():
        if name == "bottom":
            continue
        if name == "top":
            continue
        for yy in range(6, 10):
            for xx in range(w):
                c = SILVER_D if yy in (6, 9) else (SILVER_L if yy == 7 else SILVER)
                img.putpixel((x + xx, y + yy), jitter(c, 3))
        if name in ("right", "left"):
            img.putpixel((x + 1, y + 7), GOLD)
            img.putpixel((x + 2, y + 7), GOLD)
            img.putpixel((x + 1, y + 8), GOLD_D)
            img.putpixel((x + 2, y + 8), GOLD_D)

    # BOOTS (leg box on this layer): red knee-high boots with the white stripe and white tops.
    f = box_faces(0, 16, 4, 12, 4)
    for name, (x, y, w, h) in f.items():
        if name == "top":
            continue
        if name == "bottom":
            fill_rect(img, x, y, w, h, RED_D, 4)
            continue
        for yy in range(h):
            for xx in range(w):
                top = 3
                if yy < top:
                    continue
                if yy == top:
                    c = WHITE
                elif name == "front" and xx in (1, 2) and yy < 9:
                    c = WHITE
                else:
                    c = RED_L if yy == top + 1 else (RED_D if yy == 11 else RED)
                img.putpixel((x + xx, y + yy), jitter(c, 4))
    save(img, "entity", "equipment", "humanoid", "wonder_woman.png")

    # ---- leggings layer: the blue skirt with white stars (over the hips and the top of the legs).
    img = blank(64, 32)
    f = box_faces(16, 16, 8, 12, 4)
    for name, (x, y, w, h) in f.items():
        if name in ("top", "bottom"):
            continue
        for yy in range(h):
            for xx in range(w):
                if yy >= 9:
                    c = GOLD if yy == 9 else BLUE
                    if yy == 9 and name == "front" and xx in (3, 4):
                        c = GOLD_L
                    if yy == 10 and (xx + (1 if name == "back" else 0)) % 4 == 1:
                        c = WHITE
                    img.putpixel((x + xx, y + yy), jitter(c, 4))
    f = box_faces(0, 16, 4, 12, 4)
    for name, (x, y, w, h) in f.items():
        if name == "top":
            fill_rect(img, x, y, w, h, BLUE, 4)
            continue
        if name == "bottom":
            continue
        for yy in range(h):
            for xx in range(w):
                if yy >= 4:
                    continue
                c = BLUE_D if yy == 3 else BLUE
                if (xx * 3 + yy * 2 + (1 if name in ("left", "back") else 0)) % 7 == 2 and yy < 3:
                    c = WHITE
                img.putpixel((x + xx, y + yy), jitter(c, 4))
    save(img, "entity", "equipment", "humanoid_leggings", "wonder_woman.png")


def make_gui():
    """Power icons (16x16 each), in the order of AmazonPower, then the golden eagle emblem."""
    pal = dict(W)
    pal["L"] = (255, 214, 70, 255)   # lasso gold
    pal["l"] = (200, 140, 20, 255)   # lasso shade
    pal["a"] = (120, 120, 130, 255)  # creature
    pal["A"] = (80, 80, 90, 255)
    pal["e"] = (255, 240, 180, 255)  # energy
    pal["E"] = (255, 200, 80, 255)
    pal["j"] = (210, 240, 255, 255)  # jet edges
    pal["J"] = (120, 180, 230, 255)
    capture = [
        "................",
        ".....aaaaaa.....",
        "....aAaaaaAa....",
        "....aaaaaaaa....",
        "...LLLLLLLLLL...",
        "..LlaaaaaaaalL..",
        "...LLLLLLLLLL...",
        "....aaaaaaaa....",
        "....aaaaaaaa....",
        "...LLLLLLLLLL...",
        "..LlaaaaaaaalL..",
        "...LLLLLLLLLL...",
        "....aa....aa...L",
        "....aa....aa..L.",
        "....AA....AA.L..",
        "............L...",
    ]
    pull = [
        "................",
        "................",
        "....LLL.........",
        "...L...L........",
        "..L.....L.......",
        "..L.....L.......",
        "...L...LLLLLLLL.",
        "....LLL.........",
        "................",
        "..w.............",
        ".ww.............",
        "wwwwwwwwwwww....",
        ".ww.............",
        "..w.............",
        "................",
        "................",
    ]
    spin = [
        "................",
        ".....LLLLLL.....",
        "...LL......LL...",
        "..L..........L..",
        ".L....wwww....L.",
        ".L...w....w...L.",
        "L...w......w...L",
        "L...w..ss..w...L",
        "L...w..ss..w..lL",
        "L....w.ss.w...l.",
        ".L....wssw....L.",
        ".L.....ss.....L.",
        "..L....ss....L..",
        "...LL..ss..LL...",
        ".....LLLLLL.....",
        "................",
    ]
    guard = [
        "................",
        ".GG..........GG.",
        ".gGG........GGg.",
        "..gGG......GGg..",
        "...gGGH..HGGg...",
        "....gGGHHGGg....",
        ".....gGYYGg.....",
        "......GYYG......",
        ".....gGYYGg.....",
        "....gGGHHGGg....",
        "...gGGH..HGGg...",
        "..gGG......GGg..",
        ".gGG........GGg.",
        ".GG..........GG.",
        "................",
        "................",
    ]
    shockwave = [
        "................",
        "...E........E...",
        "..E..eeeeee..E..",
        ".E..e......e..E.",
        "E..e..GGGG..e..E",
        "E.e..GHHHHG..e.E",
        "E.e.GGYYYYGG.e.E",
        "E.e.GGGGGGGG.e.E",
        "E.e.GGYYYYGG.e.E",
        "E.e..GHHHHG..e.E",
        "E..e..GGGG..e..E",
        ".E..e......e..E.",
        "..E..eeeeee..E..",
        "...E........E...",
        "................",
        "................",
    ]
    sword_shield = [
        "...........H....",
        "..........HG....",
        ".........HGg....",
        "...GGGG.HGg.....",
        ".GGRRRRGGg......",
        ".GRRYYRHGg......",
        "GRRYZZHGgR......",
        "GRYZZHGgYRG.....",
        "GRYZHGgZYRG.....",
        "GRRYYYZYRRG.....",
        ".GRRYYRRRG......",
        ".GGnRRRRGG......",
        "..yYGGGG........",
        ".nNy............",
        "nNn.............",
        "Y...............",
    ]
    throw = [
        "................",
        "................",
        ".........GGGG...",
        ".......GGRRRRGG.",
        "..w...GRRYYYYRG.",
        "......GRYRZZRYRG",
        ".www.GRRYZZZYRRG",
        "......GRYRZZRYRG",
        "..w...GRRYYYYRG.",
        ".......GGRRRRGG.",
        ".www......GGGG..",
        "................",
        "..w.............",
        "................",
        "................",
        "................",
    ]
    jet = [
        "................",
        ".......jj.......",
        "......jJJj......",
        "......jJJj......",
        ".....jJJJJj.....",
        "....jJ.JJ.Jj....",
        "...jJ..JJ..Jj...",
        "..jJ...JJ...Jj..",
        ".jJ....JJ....Jj.",
        "jJjjjjjJJjjjjjJj",
        ".......JJ.......",
        "......jJJj......",
        ".....jJ..Jj.....",
        ".....jjEEjj.....",
        "......E..E......",
        "................",
    ]
    emblem = [
        "................",
        "................",
        "ZYYYY......YYYYZ",
        ".ZYYYY....YYYYZ.",
        "..ZYYYY..YYYYZ..",
        "..yZYYYYYYYYZy..",
        "...yZYYZZYYZy...",
        "....yZYZZYZy....",
        "....yYZYYZYy....",
        ".....yZYYZy.....",
        ".....yYZZYy.....",
        "......yZZy......",
        "......yYYy......",
        ".......yy.......",
        "................",
        "................",
    ]
    icons = (capture, pull, spin, guard, shockwave, sword_shield, throw, jet, emblem)
    img = blank(16 * len(icons), 16)
    for i, rows in enumerate(icons):
        img.paste(from_map(rows, pal), (i * 16, 0))
    save(img, "gui", "wonder_woman_powers.png")


if __name__ == "__main__":
    make_items()
    make_armor()
    make_gui()
