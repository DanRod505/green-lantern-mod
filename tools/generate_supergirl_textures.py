#!/usr/bin/env python3
"""
Textures of Supergirl (DC Universe expansion): the Argo Pendant, the suit (a lighter blue than
Superman's with the red S on a golden shield, long sleeves, blonde hair, red skirt and tall red
boots; the short cape, the skirt's flare and the hair falling over the shoulders are drawn in 3D by
SupergirlLayer), the armor icons, the power wheel icons, Krypto the Super-Dog (64x64: the dog's
layout on top, his cape, collar and S tag below) and the ring texture of Super Hearing's sound
waves. Original pixel art. Shares helpers with generate_textures.py.

Usage:  pip install pillow numpy
        python tools/generate_supergirl_textures.py
"""
import math
import random

from generate_textures import blank, box_faces, fill_rect, from_map, jitter, save

random.seed(1959)

C = {
    ".": (0, 0, 0, 0),
    "O": (22, 26, 52, 255),     # outline
    "b": (24, 70, 156, 255),    # blue dark
    "B": (42, 111, 224, 255),   # blue #2A6FE0
    "L": (104, 156, 244, 255),  # blue light
    "r": (148, 18, 28, 255),    # red dark
    "R": (216, 32, 46, 255),    # red #D8202E
    "E": (240, 92, 96, 255),    # red light
    "y": (196, 146, 30, 255),   # gold dark
    "Y": (255, 212, 71, 255),   # gold #FFD447
    "G": (255, 240, 176, 255),  # gold light
    "k": (242, 204, 176, 255),  # skin
    "K": (218, 172, 144, 255),  # skin shadow
    "p": (210, 112, 118, 255),  # lips
    "w": (250, 250, 250, 255),  # eye white
    "i": (54, 112, 196, 255),   # iris (blue)
    "h": (242, 210, 122, 255),  # hair
    "H": (255, 234, 170, 255),  # hair light
    "d": (196, 156, 78, 255),   # hair dark
    "W": (246, 246, 242, 255),  # white (Krypto)
    "s": (214, 214, 210, 255),  # white shade
    "S": (176, 176, 172, 255),  # grey
    "n": (34, 30, 32, 255),     # nose / dark
    "c": (210, 236, 255, 255),  # ice light
    "C": (150, 206, 245, 255),  # ice
    "I": (90, 150, 210, 255),   # ice dark
}

# ----------------------------------------------------------------------------------- items

MASK_ITEM = [  # her head: blonde hair, blue eyes
    "................",
    ".....OOOOOO.....",
    "....OHHHHhhO....",
    "...OHHhhhhhdO...",
    "...OHhhkkhhhdO..",
    "..OhhkkkkkkhdO..",
    "..OhkwikkiwkhdO.",
    "..OhkkkKKkkkhdO.",
    "..OhkkkkkkkkhdO.",
    "..OhhkkppkkhhdO.",
    "..OhhhkkkkhhhdO.",
    "..OhhdOkkOdhhdO.",
    "..OhddO..OddhO..",
    "...OdO....OdO...",
    "....O......O....",
    "................",
]

SUIT_ITEM = [  # the blue top with the S, the short red cape behind it
    "................",
    "..OOOO....OOOO..",
    ".ORBBBOOOOBBBRO.",
    ".ORBLBBBBBBLBRO.",
    ".ORBBYYYYYYBBRO.",
    ".OrBBYRRRRYBBrO.",
    ".OrOBYRYYYYBOrO.",
    ".OrOBBYRRYBBOrO.",
    "..OOBBYYYRYBOO..",
    "....OBBYRYBBO...",
    "....OBBBYBBBO...",
    "....OBBBBBBBO...",
    "....OLBBBBBLO...",
    "....OBBBBBBBO...",
    ".....OOOOOOO....",
    "................",
]

LEGS_ITEM = [  # the red skirt with the golden belt and the blue tights
    "................",
    "....OOOOOOOO....",
    "....OYYyYYYO....",
    "...ORRRRRRRRO...",
    "...OREReRRERO...".replace("e", "R"),
    "..ORRrRRrRRrRO..",
    "..OrRRrRRrRRrO..",
    "...OOBBOOBBOO...",
    "....OBBOOBBO....",
    "....OLBOOLBO....",
    "....OBBOOBBO....",
    "....OBbOObBO....",
    "....OBbOObBO....",
    "....ObbOObbO....",
    "....OOOOOOOO....",
    "................",
]

BOOTS_ITEM = [
    "................",
    "................",
    "................",
    "....OOOO.OOOO...",
    "....OERO.OERO...",
    "....ORRO.ORRO...",
    "....ORRO.ORRO...",
    "....ORRO.ORRO...",
    "....OErO.OErO...",
    "....ORRO.ORRO...",
    "....ORRrOORRrO..",
    "....ORRRROORRRRO",
    "....OrRRRrOrRRRO",
    "....OOOOOOOOOOOO",
    "................",
    "................",
]


def argo_pendant():
    """A golden chain and a pentagon pendant with the red S of the House of El."""
    img = blank(16, 16)
    # the chain: a loop of golden links
    for a in range(0, 360, 14):
        t = math.radians(a)
        x = 7.5 + 6.2 * math.cos(t)
        y = 4.2 + 3.6 * math.sin(t)
        if y > 6.5:
            continue
        img.putpixel((int(round(x)), int(round(y))), C["Y"] if a % 28 else C["y"])
    pendant = [
        "...OOOOOOOO...",
        "..OGYYYYYYYO..",
        ".OGYRRRRRRYyO.",
        ".OYRRYYYYYYyO.",
        ".OYRYyyyyyyyO.",
        "..OYRRRRRRyO..",
        "...OYyyyyRyO..",
        "....OYRRRRO...",
        ".....OYyyO....",
        "......OyO.....",
        ".......O......",
    ]
    for y, row in enumerate(pendant):
        for x, c in enumerate(row):
            if c != ".":
                img.putpixel((x + 1, y + 5), C[c])
    img.putpixel((7, 4), C["y"])
    img.putpixel((8, 4), C["y"])
    return img


def make_items():
    save(argo_pendant(), "item", "argo_pendant.png")
    save(from_map(MASK_ITEM, C), "item", "supergirl_mask.png")
    save(from_map(SUIT_ITEM, C), "item", "supergirl_suit.png")
    save(from_map(LEGS_ITEM, C), "item", "supergirl_leggings.png")
    save(from_map(BOOTS_ITEM, C), "item", "supergirl_boots.png")


# ----------------------------------------------------------------------------------- armor

def paint(img, x0, y0, rows, noise=3):
    for y, row in enumerate(rows):
        for x, c in enumerate(row):
            if c != ".":
                col = C[c]
                n = noise if c in "bBLrR" else (2 if c in "hHd" else 0)
                img.putpixel((x0 + x, y0 + y), jitter(col, n) if n else col)


def paint_face(img, f, name, rows, noise=3):
    x, y, w, h = f[name]
    assert len(rows) == h and all(len(r) == w for r in rows), (name, rows)
    paint(img, x, y, rows, noise)


HEAD_FRONT = [
    "HHhHHhHH",
    "hhHhhkhh",
    "hkkkkkkh",
    "hwikkiwh",
    "hkkKKkkh",
    "hkkkkkkd",
    "dkkppkkd",
    "dkkkkkkd",
]
HEAD_SIDE = [
    "HHhHHhHh",
    "hhhhhhhd",
    "hhhhhhkd",
    "hhhhhkkd",
    "hhhhhkKd",
    "dhhhhkkd",
    "dhhhhhkd",
    "ddhhhhhd",
]
HEAD_BACK = [
    "HHhHHhHH",
    "hhhHhhhh",
    "hhhhhhhh",
    "hHhhhhHh",
    "hhhhhhhh",
    "hhhdhhhh",
    "dhhhhhhd",
    "ddhhhhdd",
]
HEAD_TOP = [
    "hHhHHhHh",
    "HHhHHhHH",
    "hHHhHHhH",
    "HhHHhHHh",
    "hHHhHHhH",
    "HHhHHhHH",
    "hHhHHhHh",
    "hhhhhhhh",
]

BODY_FRONT = [  # blue top, the S on its golden shield
    "BBkkkkBB",
    "BYYYYYYB",
    "BYRRRRYB",
    "BYRYYYYB",
    "BBYRRYBB",
    "BBYYYRYB",
    "BBBRRYBB",
    "BBBBYBBB",
    "BBBBBBBB",
    "bBBBBBBb",
    "RRRRRRRR",
    "rRRrRRrR",
]
BODY_BACK = [
    "BBBBBBBB",
    "BLBBBBLB",
    "BBBBBBBB",
    "BBBBBBBB",
    "BBBBBBBB",
    "BBBBBBBB",
    "BBBBBBBB",
    "BBBBBBBB",
    "BBBBBBBB",
    "bBBBBBBb",
    "RRRRRRRR",
    "rRRrRRrR",
]
BODY_SIDE = [
    "LBBb",
    "BBBb",
    "BBBb",
    "BBBb",
    "BBBb",
    "BBBb",
    "BBBb",
    "BBBb",
    "BBBb",
    "BBBb",
    "RRRr",
    "RrRr",
]

ARM_FACE = [  # long blue sleeves, the hand below
    "LLLL",
    "BBBB",
    "BBBB",
    "BBBb",
    "BBBB",
    "BBBb",
    "BBBB",
    "BBBb",
    "BBBB",
    "bBBb",
    "kkkk",
    "KkkK",
]
ARM_TOP = [
    "LLLL",
    "LBBL",
    "LBBL",
    "LLLL",
]
ARM_BOTTOM = [
    "kkkk",
    "kKKk",
    "kKKk",
    "kkkk",
]

BOOT_FRONT = [  # the top of the tall boots, a lighter band, the toe
    "ERRE",
    "RRRR",
    "RRRR",
    "RERR",
    "RRRR",
    "rrrr",
]
BOOT_SIDE = [
    "ERRR",
    "RRRr",
    "RRRr",
    "RRRr",
    "RRRr",
    "rrrr",
]

LEG_FRONT = [  # the skirt's hem, then the blue tights (the boots cover the lower half)
    "RRRR",
    "rRRr",
    "BBBB",
    "BLBB",
    "BBBB",
    "BBBb",
    "BBBB",
    "BBBb",
    "BBBB",
    "BBBb",
    "BBBB",
    "bBBb",
]
LEG_SIDE = [
    "RRRr",
    "RrRr",
    "BBBb",
    "BBBb",
    "BBBb",
    "BBBb",
    "BBBb",
    "BBBb",
    "BBBb",
    "BBBb",
    "BBBb",
    "bBBb",
]
WAIST = [  # the golden belt and the top of the skirt
    "YYYYYYYY",
    "RRRRRRRR",
    "RrRRrRRr",
    "rRRrRRrR",
]


def make_armor():
    # ---- humanoid layer: hair and face, the top with the S, sleeves, boots (64x32)
    img = blank(64, 32)
    f = box_faces(0, 0, 8, 8, 8)
    paint_face(img, f, "front", HEAD_FRONT)
    paint_face(img, f, "right", HEAD_SIDE)
    paint_face(img, f, "left", [r[::-1] for r in HEAD_SIDE])
    paint_face(img, f, "back", HEAD_BACK)
    paint_face(img, f, "top", HEAD_TOP)
    x, y, w, h = f["bottom"]
    fill_rect(img, x, y, w, h, C["d"], 2)

    f = box_faces(16, 16, 8, 12, 4)
    paint_face(img, f, "front", BODY_FRONT)
    paint_face(img, f, "back", BODY_BACK)
    paint_face(img, f, "right", BODY_SIDE)
    paint_face(img, f, "left", [r[::-1] for r in BODY_SIDE])
    x, y, w, h = f["top"]
    fill_rect(img, x, y, w, h, C["B"], 3)
    x, y, w, h = f["bottom"]
    fill_rect(img, x, y, w, h, C["r"], 3)

    f = box_faces(40, 16, 4, 12, 4)
    for name in ("right", "front", "left", "back"):
        paint_face(img, f, name, ARM_FACE)
    paint_face(img, f, "top", ARM_TOP)
    paint_face(img, f, "bottom", ARM_BOTTOM)

    # BOOTS (the leg box on this layer): the lower half of the leg.
    f = box_faces(0, 16, 4, 12, 4)
    for name in ("right", "front", "left", "back"):
        x, y, w, h = f[name]
        paint(img, x, y + 6, BOOT_FRONT if name == "front" else BOOT_SIDE)
    x, y, w, h = f["bottom"]
    fill_rect(img, x, y, w, h, C["r"], 3)
    save(img, "entity", "equipment", "humanoid", "supergirl.png")

    # ---- leggings layer: the belt and the top of the skirt, then blue tights.
    img = blank(64, 32)
    f = box_faces(16, 16, 8, 12, 4)
    for name in ("front", "back", "right", "left"):
        x, y, w, h = f[name]
        paint(img, x, y + 8, [r[:w] for r in WAIST])
    f = box_faces(0, 16, 4, 12, 4)
    paint_face(img, f, "front", LEG_FRONT)
    paint_face(img, f, "right", LEG_SIDE)
    paint_face(img, f, "left", [r[::-1] for r in LEG_SIDE])
    paint_face(img, f, "back", LEG_FRONT)
    x, y, w, h = f["top"]
    fill_rect(img, x, y, w, h, C["R"], 3)
    save(img, "entity", "equipment", "humanoid_leggings", "supergirl.png")


# ----------------------------------------------------------------------------------- krypto

def make_krypto():
    """64x64: the dog's 64x32 layout (white fur, black nose, dark eyes) above; the cape, collar and tag below."""
    img = blank(64, 64)

    def fur(f, name, shade=0.0):
        x, y, w, h = f[name]
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                base = C["W"] if (xx * 7 + yy * 3) % 11 else C["s"]
                if shade:
                    base = tuple(int(c * (1 - shade)) if i < 3 else c for i, c in enumerate(base))
                img.putpixel((xx, yy), jitter(base, 4))

    def box(u, v, w, h, d, under=0.12):
        f = box_faces(u, v, w, h, d)
        for name in ("top", "right", "front", "left", "back"):
            fur(f, name)
        fur(f, "bottom", under)
        return f

    # head 6x6x4 at (0,0): eyes and a dark brow on the front face
    f = box(0, 0, 6, 6, 4)
    x, y, w, h = f["front"]
    for (dx, dy, c) in ((1, 2, "n"), (4, 2, "n"), (1, 1, "S"), (4, 1, "S"), (2, 3, "s"), (3, 3, "s")):
        img.putpixel((x + dx, y + dy), C[c])
    # ears 2x2x1 at (16,14)
    f = box(16, 14, 2, 2, 1)
    x, y, w, h = f["front"]
    for xx in range(x, x + w):
        for yy in range(y, y + h):
            img.putpixel((xx, yy), C["S"])
    # snout 3x3x4 at (0,10): the black nose on its tip
    f = box(0, 10, 3, 3, 4)
    x, y, w, h = f["front"]
    for xx in range(x, x + w):
        img.putpixel((xx, y), C["n"])
    img.putpixel((x + 1, y + 1), C["n"])
    x, y, w, h = f["top"]
    for xx in range(x, x + w):
        img.putpixel((xx, y), C["n"])
    x, y, w, h = f["bottom"]
    img.putpixel((x + 1, y + h - 1), C["p"])
    # body 6x9x6 at (18,14), mane 8x6x7 at (21,0)
    box(18, 14, 6, 9, 6)
    box(21, 0, 8, 6, 7)
    # legs 2x8x2 at (0,18) and tail 2x8x2 at (9,18)
    f = box(0, 18, 2, 8, 2)
    for name in ("right", "front", "left", "back"):
        x, y, w, h = f[name]
        for xx in range(x, x + w):
            img.putpixel((xx, y + h - 1), C["S"])
    box(9, 18, 2, 8, 2)

    # cape 8x1x11 at (0,32): red, the gold S on top near the shoulders
    f = box_faces(0, 32, 8, 1, 11)
    for name, col in (("top", "R"), ("bottom", "r"), ("right", "r"), ("left", "r"), ("front", "R"), ("back", "R")):
        x, y, w, h = f[name]
        fill_rect(img, x, y, w, h, C[col], 4)
    x, y, w, h = f["top"]
    emblem = [
        ".YYYYYY.",
        "YRRRRRRY",
        "YRYYYYYY",
        ".YRRRRY.",
        "..YYYRY.",
        "...RRY..",
        "....Y...",
    ]
    for yy, row in enumerate(emblem):
        for xx, c in enumerate(row):
            if c != ".":
                img.putpixel((x + xx, y + 2 + yy), C[c])
    for yy in range(y, y + h):
        img.putpixel((x, yy), C["r"])
        img.putpixel((x + w - 1, yy), C["r"])

    # collar 7x7x1 at (0,48): red with a lighter stitch
    f = box_faces(0, 48, 7, 7, 1)
    for name in f:
        x, y, w, h = f[name]
        fill_rect(img, x, y, w, h, C["R"], 4)
    x, y, w, h = f["back"]
    for xx in range(x, x + w):
        img.putpixel((xx, y + 3), C["E"])
    # tag 2x2x1 at (20,48): gold with a red mark
    f = box_faces(20, 48, 2, 2, 1)
    for name in f:
        x, y, w, h = f[name]
        fill_rect(img, x, y, w, h, C["Y"], 0)
    x, y, w, h = f["front"]
    img.putpixel((x, y), C["R"])
    img.putpixel((x + 1, y + 1), C["R"])
    save(img, "entity", "krypto.png")


# ----------------------------------------------------------------------------------- misc

def make_misc():
    """Super Hearing's ring: a soft white band, brightest in the middle (tinted by the code)."""
    img = blank(16, 16)
    for y in range(16):
        for x in range(16):
            t = abs(y - 7.5) / 7.5
            a = int(255 * max(0.0, 1.0 - t * t))
            img.putpixel((x, y), (255, 255, 255, a))
    save(img, "misc", "sound_ring.png")


# ----------------------------------------------------------------------------------- gui

def px(img, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        img.putpixel((x, y), C[c] if isinstance(c, str) else c)


def line(img, x0, y0, x1, y1, c):
    n = max(int(math.ceil(max(abs(x1 - x0), abs(y1 - y0)))), 1)
    for i in range(n + 1):
        px(img, round(x0 + (x1 - x0) * i / n), round(y0 + (y1 - y0) * i / n), c)


def disc(img, cx, cy, r, c):
    for y in range(16):
        for x in range(16):
            if math.hypot(x - cx, y - cy) <= r:
                px(img, x, y, c)


def icon_heat_bolts():
    img = blank(16, 16)
    # two eyes on the left, two short bolts racing right
    for (ey) in (4, 9):
        px(img, 1, ey, "O")
        px(img, 2, ey, "Y")
        px(img, 2, ey + 1, "O")
        line(img, 4, ey, 9, ey + 1, "r")
        line(img, 5, ey, 10, ey + 1, "R")
        line(img, 10, ey + 1, 12, ey + 1, "Y")
        disc(img, 13, ey + 1, 1.3, "G")
        px(img, 13, ey + 1, "w")
    return img


def icon_meteor_dash():
    img = blank(16, 16)
    # a comet diving down-right, its tail red and gold
    for i in range(9):
        c = "r" if i < 3 else ("R" if i < 6 else "Y")
        line(img, 1 + i, 1 + i // 2, 1 + i, 3 + i // 2, c)
    line(img, 2, 6, 9, 9, "R")
    line(img, 3, 9, 9, 11, "y")
    disc(img, 11.5, 10.5, 2.6, "Y")
    disc(img, 11.5, 10.5, 1.4, "G")
    px(img, 12, 10, "w")
    return img


def icon_thunder_clap():
    img = blank(16, 16)
    # two hands meeting in the middle, a shock burst between them
    for y in range(5, 12):
        for x in range(2, 6):
            px(img, x, y, "k" if (x + y) % 5 else "K")
            px(img, 15 - x, y, "k" if (x + y) % 5 else "K")
    for y in range(6, 11):
        px(img, 6, y, "K")
        px(img, 9, y, "K")
    for a in range(0, 360, 45):
        t = math.radians(a)
        line(img, 7.5 + 2 * math.cos(t), 8 + 2 * math.sin(t), 7.5 + 6.5 * math.cos(t), 8 + 6.5 * math.sin(t), "Y" if a % 90 else "G")
    px(img, 7, 8, "w")
    px(img, 8, 8, "w")
    return img


def icon_frost_wall():
    img = blank(16, 16)
    # a wall of ice blocks with a snowflake breath drifting in
    for y in range(6, 16):
        for x in range(4, 16):
            edge = (y - 6) % 4 == 0 or (x + (4 if ((y - 6) // 4) % 2 else 0)) % 6 == 4
            px(img, x, y, "I" if edge else ("c" if (x + y) % 4 == 0 else "C"))
    for i in range(-2, 3):
        px(img, 3 + i, 3, "c")
        px(img, 3, 3 + i, "c")
        px(img, 3 + i, 3 + i, "w")
        px(img, 3 + i, 3 - i, "w")
    return img


def icon_super_hearing():
    img = blank(16, 16)
    # an ear with golden sound waves
    ear = [
        "..kkkk..",
        ".kKKKkk.",
        "kK...Kk.",
        "kK.kk.k.",
        "kK.K..k.",
        "kKk..kk.",
        ".kK.kk..",
        ".kkkk...",
        "..kk....",
    ]
    for y, row in enumerate(ear):
        for x, c in enumerate(row):
            if c != ".":
                px(img, x + 1, y + 3, c)
    for r, c in ((4.5, "Y"), (7.0, "y"), (9.5, "Y")):
        for a in range(-50, 51, 6):
            t = math.radians(a)
            px(img, round(5 + r * math.cos(t)), round(7.5 + r * math.sin(t)), c)
    return img


def icon_throw():
    img = blank(16, 16)
    # a block flying up-right, speed lines behind it, her golden arc of the throw
    for y in range(2, 8):
        for x in range(8, 14):
            edge = x in (8, 13) or y in (2, 7)
            px(img, x, y, "O" if edge else ("S" if (x + y) % 3 else "s"))
    line(img, 1, 14, 6, 9, "Y")
    line(img, 2, 15, 7, 10, "y")
    line(img, 4, 7, 7, 5, "w")
    line(img, 3, 10, 7, 8, "w")
    px(img, 10, 4, "n")
    px(img, 11, 5, "n")
    return img


def icon_solar_flare():
    img = blank(16, 16)
    for a in range(0, 360, 30):
        t = math.radians(a)
        line(img, 7.5 + 3.5 * math.cos(t), 7.5 + 3.5 * math.sin(t), 7.5 + 7.4 * math.cos(t), 7.5 + 7.4 * math.sin(t), "Y" if a % 60 else "R")
    disc(img, 7.5, 7.5, 3.8, "Y")
    disc(img, 7.5, 7.5, 2.4, "G")
    disc(img, 7.5, 7.5, 1.2, "w")
    return img


def icon_emblem():
    """The S of the House of El, red on a golden shield, on her blue."""
    rows = [
        "................",
        ".OOOOOOOOOOOOOO.",
        "OYYYYYYYYYYYYYYO",
        "OYGRRRRRRRRRRYYO",
        "OYRRYYYYYYYYRRYO",
        ".OYRRYYYYYYYYYO.",
        ".OYYRRRRRRRRYYO.",
        "..OYYYYYYYYRRO..",
        "..OYRRYYYYYRRO..",
        "...OYRRRRRRYO...",
        "....OYYYYYYO....",
        ".....OYYYYO.....",
        "......OYYO......",
        ".......OO.......",
        "................",
        "................",
    ]
    return from_map(rows, C)


def make_gui():
    """Power icons (16x16 each), in the order of Supergirl's power wheel, then the emblem."""
    icons = (icon_heat_bolts(), icon_meteor_dash(), icon_thunder_clap(), icon_frost_wall(),
             icon_super_hearing(), icon_throw(), icon_solar_flare(), icon_emblem())
    img = blank(128, 16)
    for i, ic in enumerate(icons):
        img.paste(ic, (i * 16, 0))
    save(img, "gui", "supergirl_powers.png")


if __name__ == "__main__":
    make_items()
    make_armor()
    make_krypto()
    make_misc()
    make_gui()
