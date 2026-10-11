#!/usr/bin/env python3
"""
Textures of Cyborg (DC Universe expansion): the Mother Box, the armor (half human face and half
silver plate with the red cybernetic eye, silver-grey body plates with the red chest core, jet
thrusters on the back, arm cannon rings, a shoulder missile pod, silver legs with dark joints and red
light strips), the armor icons and the power wheel icons. Original pixel art. Shares helpers with
generate_textures.py.

Note: the vanilla 64x32 armor layout has a single arm and a single leg (the left ones are mirrored
copies), so the forearm cannon ring and the shoulder pod show on both arms.

Usage:  pip install pillow numpy
        python tools/generate_cyborg_textures.py
"""
import math
import random

from generate_textures import blank, box_faces, fill_rect, from_map, jitter, save

random.seed(1980)

C = {
    ".": (0, 0, 0, 0),
    "O": (33, 34, 36, 255),     # outline #212224
    "d": (55, 57, 60, 255),     # dark panel / joint #37393C
    "m": (110, 114, 120, 255),  # silver shade #6E7278
    "g": (146, 152, 160, 255),  # silver mid-dark
    "S": (184, 190, 200, 255),  # silver #B8BEC8
    "H": (210, 215, 222, 255),  # silver light
    "W": (232, 236, 240, 255),  # silver highlight #E8ECF0
    "r": (122, 16, 16, 255),    # dark red
    "R": (200, 30, 30, 255),    # red #C81E1E
    "E": (255, 90, 60, 255),    # glow #FF5A3C
    "Y": (255, 214, 170, 255),  # hot core
    "b": (24, 84, 150, 255),    # thruster blue dark
    "B": (79, 184, 255, 255),   # thruster blue #4FB8FF
    "c": (200, 240, 255, 255),  # blue-white core
    "k": (104, 66, 44, 255),    # skin (dark brown)
    "K": (74, 46, 30, 255),     # skin shadow
    "L": (132, 88, 60, 255),    # skin light
    "p": (62, 36, 26, 255),     # lips
    "w": (240, 240, 236, 255),  # eye white
    "i": (30, 20, 14, 255),     # iris
    "h": (22, 18, 18, 255),     # hair
}

# ----------------------------------------------------------------------------------- items

MASK_ITEM = [
    "................",
    "....OOOOOOOO....",
    "...OHWWWWWWHO...",
    "..OHWHHHHHHSmO..",
    "..OhhhhhdSSSmO..",
    "..OLkkkkdSrrmO..",
    "..OkwikkdREYRO..",
    "..OkkkkkdSrrmO..",
    "..OkkKkkdSgSmO..",
    "..OKpppkdSSSmO..",
    "...OkkkkdSgmO...",
    "....OKKKdmmO....",
    ".....OOOOOO.....",
    "................",
    "................",
    "................",
]

SUIT_ITEM = [
    "................",
    "...OOO....OOO...",
    "..OWHSOOOOdRdO..",
    ".OHSHSdSSdSHSHO.",
    ".OSSgSSddSSgSSO.",
    ".OSmOSrRRrSOmSO.",
    "..OO.SREYRS.OO..",
    "....OSRYERSO....",
    "....OSrRRrSO....",
    "....OHSddSHO....",
    "....OSHddHSO....",
    "....OddRRddO....",
    "....OgSddSgO....",
    "....OmmOOmmO....",
    ".....OO..OO.....",
    "................",
]

LEGS_ITEM = [
    "................",
    "....OOOOOOOO....",
    "....OddRRddO....",
    "....OSHddHSO....",
    "....OHSOOHSO....",
    "....OWSOOWSO....",
    "....ORSOORSO....",
    "....OSgOOSgO....",
    "....OddOOddO....",
    "....OmdOOmdO....",
    "....OHSOOHSO....",
    "....OSSOOSSO....",
    "....ORgOOgRO....",
    "....OmmOOmmO....",
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
    "...OHSO..OHSO...",
    "...OSRO..OSRO...",
    "...OSgO..OSgO...",
    "...OddO..OddO...",
    "...OHSSO.OHSSO..",
    "...OSSSSOOSSSSO.",
    "...OgRRgOOgRRgO.",
    "...OddddOOddddO.",
    "....OOOO..OOOO..",
    "................",
]


def mother_box():
    """A small silver cube (cavalier view) with glowing red-orange circuits and a bright core."""
    img = blank(16, 16)
    cells = {}
    depth = 4
    # front face x 0..11, y 4..15; top face slants up-right; right face drops down-right
    for y in range(4, 16):
        for x in range(0, 12):
            cells[(x, y)] = "front"
    for k in range(1, depth + 1):
        for x in range(k, 12 + k):
            cells.setdefault((x, 4 - k), "top")
        for y in range(4 - k, 16 - k):
            cells.setdefault((11 + k, y), "side")
    for (x, y), face in cells.items():
        edge = any((x + dx, y + dy) not in cells for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        if edge or x in (0, 15) or y in (0, 15):
            col = C["O"]
        elif face == "top":
            col = C["W"] if (x + y) % 5 else C["H"]
        elif face == "side":
            col = C["m"] if y > 6 else C["g"]
        else:
            col = C["S"] if y > 5 else C["H"]
            if x == 1 or y == 5:
                col = C["W"]
            if y == 14 or x == 10:
                col = C["g"]
        img.putpixel((x, y), col)
    # the edge where the top meets the front, and the front-right corner
    for x in range(1, 11):
        img.putpixel((x, 4), C["W"])
    # circuits on the front, running into the core
    for p in [(2, 7), (3, 7), (4, 7), (4, 8), (2, 12), (3, 12), (4, 12), (4, 11),
              (9, 7), (8, 7), (7, 7), (7, 8), (9, 12), (8, 12), (7, 12), (7, 11)]:
        img.putpixel(p, C["R"])
    for p in [(5, 9), (6, 9), (5, 10), (6, 10)]:
        img.putpixel(p, C["Y"])
    for p in [(5, 8), (6, 8), (4, 9), (4, 10), (7, 9), (7, 10), (5, 11), (6, 11)]:
        img.putpixel(p, C["E"])
    # circuits on the top and down the side
    for p in [(5, 2), (6, 2), (7, 2), (10, 1), (9, 2)]:
        img.putpixel(p, C["E"])
    for p in [(13, 4), (13, 5), (13, 6), (14, 8), (14, 9), (13, 10)]:
        img.putpixel(p, C["R"])
    img.putpixel((13, 7), C["E"])
    return img


def make_items():
    save(mother_box(), "item", "mother_box.png")
    save(from_map(MASK_ITEM, C), "item", "cyborg_mask.png")
    save(from_map(SUIT_ITEM, C), "item", "cyborg_suit.png")
    save(from_map(LEGS_ITEM, C), "item", "cyborg_leggings.png")
    save(from_map(BOOTS_ITEM, C), "item", "cyborg_boots.png")


# ----------------------------------------------------------------------------------- armor

def paint(img, x0, y0, rows, noise=3):
    for y, row in enumerate(rows):
        for x, c in enumerate(row):
            if c != ".":
                col = C[c]
                # glows and skin stay clean, the metal gets a light grain
                n = noise if c in "dmgSHWO" else 0
                img.putpixel((x0 + x, y0 + y), jitter(col, n) if n else col)


def paint_face(img, f, name, rows, noise=3):
    x, y, w, h = f[name]
    assert len(rows) == h and all(len(r) == w for r in rows), (name, rows)
    paint(img, x, y, rows, noise)


HEAD_FRONT = [  # image left = the human side, image right = the metal plate with the red eye
    "SHWWWWHS",
    "hhhhdSHg",
    "kLkkdSSg",
    "kwiKdYEm",
    "kkkkdrSm",
    "kkKkdSgm",
    "KpppdgSm",
    "KkkkdmOm",
]
HEAD_RIGHT = [  # the human side: short hair and an ear; metal wraps the back of the head
    "gSHHHWWS",
    "mSShhhhh",
    "mSSdkkkL",
    "gSHdKkkk",
    "mSSdKKkk",
    "gSSdkkkk",
    "mggdkkkK",
    "OmmdKkkK",
]
HEAD_LEFT = [  # the metal side: panel lines and a red audio sensor
    "SWWHHHSg",
    "gSHSdSSm",
    "SSdddSHm",
    "SSdERdSm",
    "SSdRrdSm",
    "gSSddSgm",
    "dddSSddd",
    "mgSmmSgO",
]
HEAD_BACK = [
    "gSHWWHSg",
    "SSHddHSS",
    "SSSdgSSS",
    "dddddddd",
    "SHSdSHSm",
    "SSRdRSSm",
    "gSSdSSgm",
    "mmgdgmmO",
]
HEAD_TOP = [
    "SHHHWWHS",
    "SHHdWWHS",
    "SHHdHWHS",
    "gSSdSHSg",
    "SHSdSWHS",
    "SHHdHWHS",
    "hhhdHWHS",
    "hhhdSHSg",
]

BODY_FRONT = [
    "gHWWWWHg",
    "SHSddSHS",
    "SSrRRrSS",
    "SdREYRdS",
    "SdRYERdS",
    "SSrRRrSS",
    "gSSddSSg",
    "dddSSddd",
    "mSHddHSm",
    "mSSddSSm",
    "dddddddd",
    "gmRddRmg",
]
BODY_BACK = [
    "gHWWWWHg",
    "SOOSSOOS",
    "ObcOOcbO",
    "OcBOOBcO",
    "ObBOOBbO",
    "SOOSSOOS",
    "SRSddSRS",
    "gRSddSRg",
    "gRRRRRRg",
    "mSSRRSSm",
    "dddddddd",
    "gmSddSmg",
]
BODY_SIDE = [
    "HWWH",
    "SHSm",
    "SSSm",
    "SdSm",
    "SRSm",
    "SdSm",
    "gSSm",
    "dddd",
    "SHSm",
    "gSSm",
    "dddd",
    "mggm",
]

ARM_OUTER = [  # the shoulder carries the missile pod
    "dddd",
    "dRER",
    "dddd",
    "SHHm",
    "dddd",
    "SHSm",
    "gSSm",
    "dOOd",
    "SHSm",
    "OddO",
    "dEEd",
    "mggm",
]
ARM_FACE = [
    "HWWH",
    "SHHS",
    "SSSg",
    "gSSm",
    "dddd",
    "SHSm",
    "gSSm",
    "dOOd",
    "SHSm",
    "OddO",
    "dEEd",
    "mggm",
]
ARM_TOP = [
    "mddm",
    "dEEd",
    "dRRd",
    "mddm",
]
ARM_BOTTOM = [  # the cannon muzzle
    "OddO",
    "dEYd",
    "dYEd",
    "OddO",
]

BOOT_FRONT = [
    "HWWH",
    "SHSm",
    "gRRg",
    "dddd",
    "SHHS",
    "mggm",
]
BOOT_SIDE = [
    "HWWH",
    "SHSm",
    "SRSm",
    "dddd",
    "SHSm",
    "mggm",
]

LEG_FRONT = [
    "SHHS",
    "SWHS",
    "SHSm",
    "gSSm",
    "dmmd",
    "dHHd",
    "dddd",
    "SHSm",
    "SSSm",
    "gRRg",
    "gSSm",
    "mggm",
]
LEG_SIDE = [
    "SHSm",
    "SRSm",
    "SRSm",
    "gSSm",
    "dddd",
    "dSSd",
    "dddd",
    "SHSm",
    "SRSm",
    "gSSm",
    "gSSm",
    "mggm",
]
LEG_BACK = [
    "SHHS",
    "SSSS",
    "gSSg",
    "gSSg",
    "dddd",
    "dmmd",
    "dddd",
    "SHHS",
    "SSSS",
    "gSSg",
    "gSSg",
    "mggm",
]
WAIST_FRONT = [
    "dddRRddd",
    "gSHddHSg",
    "mSSddSSm",
    "mgSddSgm",
]
WAIST_BACK = [
    "dddddddd",
    "gSHddHSg",
    "mSSddSSm",
    "mgSddSgm",
]
WAIST_SIDE = [
    "dddd",
    "SHSm",
    "gSSm",
    "mggm",
]


def make_armor():
    # ---- humanoid layer: helmet, chest, arms, boots (64x32)
    img = blank(64, 32)
    f = box_faces(0, 0, 8, 8, 8)
    paint_face(img, f, "front", HEAD_FRONT)
    paint_face(img, f, "right", HEAD_RIGHT)
    paint_face(img, f, "left", HEAD_LEFT)
    paint_face(img, f, "back", HEAD_BACK)
    paint_face(img, f, "top", HEAD_TOP)

    f = box_faces(16, 16, 8, 12, 4)
    paint_face(img, f, "front", BODY_FRONT)
    paint_face(img, f, "back", BODY_BACK)
    paint_face(img, f, "right", BODY_SIDE)
    paint_face(img, f, "left", [r[::-1] for r in BODY_SIDE])
    x, y, w, h = f["top"]
    fill_rect(img, x, y, w, h, C["H"], 3)
    x, y, w, h = f["bottom"]
    fill_rect(img, x, y, w, h, C["d"], 3)

    f = box_faces(40, 16, 4, 12, 4)
    paint_face(img, f, "right", ARM_OUTER)
    paint_face(img, f, "front", ARM_FACE)
    paint_face(img, f, "left", [r[::-1] for r in ARM_FACE])
    paint_face(img, f, "back", ARM_FACE)
    paint_face(img, f, "top", ARM_TOP)
    paint_face(img, f, "bottom", ARM_BOTTOM)

    # BOOTS (leg box on this layer): the lower half of the leg, ankle joint and a red light.
    f = box_faces(0, 16, 4, 12, 4)
    for name in ("right", "front", "left", "back"):
        x, y, w, h = f[name]
        rows = BOOT_FRONT if name == "front" else BOOT_SIDE
        if name == "back":
            rows = [r.replace("R", "S") for r in BOOT_SIDE]
        paint(img, x, y + 6, rows)
    x, y, w, h = f["bottom"]
    fill_rect(img, x, y, w, h, C["d"], 3)
    save(img, "entity", "equipment", "humanoid", "cyborg.png")

    # ---- leggings layer: armored waist and the silver legs with knee joints.
    img = blank(64, 32)
    f = box_faces(16, 16, 8, 12, 4)
    for name, rows in (("front", WAIST_FRONT), ("back", WAIST_BACK),
                       ("right", WAIST_SIDE), ("left", WAIST_SIDE)):
        x, y, w, h = f[name]
        paint(img, x, y + 8, rows)
    f = box_faces(0, 16, 4, 12, 4)
    paint_face(img, f, "front", LEG_FRONT)
    paint_face(img, f, "right", LEG_SIDE)
    paint_face(img, f, "left", [r[::-1] for r in LEG_SIDE])
    paint_face(img, f, "back", LEG_BACK)
    x, y, w, h = f["top"]
    fill_rect(img, x, y, w, h, C["S"], 3)
    save(img, "entity", "equipment", "humanoid_leggings", "cyborg.png")


# ----------------------------------------------------------------------------------- gui

def px(img, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        img.putpixel((x, y), C[c] if isinstance(c, str) else c)


def ring(img, cx, cy, r, c, width=0.5, test=None):
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - cx, y - cy)
            if abs(d - r) <= width and (test is None or test(x, y)):
                px(img, x, y, c)


def icon_sonic_cannon():
    img = blank(16, 16)
    # arm cannon pointing right
    for x in range(0, 8):
        px(img, x, 5, "O")
        px(img, x, 10, "O")
        px(img, x, 6, "W")
        px(img, x, 7, "H")
        px(img, x, 8, "S")
        px(img, x, 9, "m")
    for y in range(6, 10):
        px(img, 3, y, "d")
    for y in range(4, 12):
        px(img, 7, y, "O")
        px(img, 6, y, "O" if y in (4, 11) else "g")
    px(img, 6, 6, "H")
    px(img, 7, 7, "E")
    px(img, 7, 8, "E")
    # sonic rings spreading out
    for r, col in ((3.0, "c"), (5.2, "B"), (7.4, "b")):
        ring(img, 6.5, 7.5, r, col, 0.5, lambda x, y: x >= 8 and abs(y - 7.5) <= (x - 6.5) * 1.6)
    return img


def rocket(img, c, tail, size):
    """A rocket flying up-right along the diagonal x + y = c, with a flame trail behind it.
    t runs along the rocket in diagonal pixels (0 at the tail, towards the nose), v across it."""
    for y in range(16):
        for x in range(16):
            t, v = (x - y - tail) / 2, x + y - c
            col = None
            if 0 <= t <= size and abs(v) <= 1:
                col = ("W", "S", "m")[v + 1]
                if t == 0:
                    col = "d"
                elif abs(t - (size - 1.5)) < 0.3:
                    col = "d"   # dark band under the nose
            elif size < t <= size + 1.5 and abs(v) <= 1:
                col = "E" if t > size + 1 else "R"
            elif 0 <= t <= 1.5 and abs(v) == 2:
                col = "O"   # fins
            elif -1.5 <= t < 0 and abs(v) <= 1:
                col = "Y" if v == 0 else "E"
            elif -3.5 <= t < -1.5 and abs(v) <= 1 and (v == 0 or t > -2.6):
                col = "E" if t > -2.6 else "R"
            elif -5 <= t < -3.5 and v == 0:
                col = "g" if t > -4.6 else "m"
            if col:
                px(img, x, y, col)


def icon_missiles():
    img = blank(16, 16)
    rocket(img, 11, -5, 5)
    rocket(img, 20, -1, 4)
    return img


def icon_tech_scan():
    img = blank(16, 16)
    ring(img, 7.5, 7.5, 6.3, "R", 0.55)
    ring(img, 7.5, 7.5, 6.3, "E", 0.25)
    ring(img, 7.5, 7.5, 3.4, "B", 0.45, lambda x, y: (x < 7.5) != (y < 7.5))
    for i in range(0, 4):
        for c in (7, 8):
            px(img, c, i, "W" if i % 2 == 0 else "H")
            px(img, c, 15 - i, "W" if i % 2 == 0 else "H")
            px(img, i, c, "W" if i % 2 == 0 else "H")
            px(img, 15 - i, c, "W" if i % 2 == 0 else "H")
    for x, y in ((7, 7), (8, 7), (7, 8), (8, 8)):
        px(img, x, y, "Y")
    for x, y in ((6, 7), (6, 8), (9, 7), (9, 8), (7, 6), (8, 6), (7, 9), (8, 9)):
        px(img, x, y, "E")
    return img


def icon_hack():
    img = from_map([
        "................",
        "......OOOO......",
        ".....OWHHSO.....",
        "....OHO..OSO....",
        "....OSO...OO....",
        "....OSO.........",
        "....OSO.........",
        "..OOOOOOOOOOOO..",
        "..OWWWWWWWWWHO..",
        "..OHSSSEESSSmO..",
        "..OSSSEYYESSmO..",
        "..OSSSSEESSSmO..",
        "..OSSSSEESSSmO..",
        "..OgSSSSSSSgmO..",
        "..OOOOOOOOOOOO..",
        "................",
    ], C)
    # circuit traces and binary bits flying off
    for p in [(0, 9), (1, 9), (0, 12), (1, 12), (14, 10), (15, 10), (15, 11), (14, 13), (15, 13)]:
        img.putpixel(p, C["B"])
    for p in [(12, 3), (14, 2), (13, 5), (15, 4)]:
        img.putpixel(p, C["c"])
    img.putpixel((13, 3), C["B"])
    img.putpixel((14, 5), C["B"])
    return img


def icon_emp():
    img = blank(16, 16)
    ring(img, 7.5, 7.5, 6.8, "b", 0.5)
    ring(img, 7.5, 7.5, 6.0, "B", 0.45)
    ring(img, 7.5, 7.5, 3.9, "c", 0.3, lambda x, y: (x + y) % 2 == 0)
    bolt = [(9, 2), (8, 3), (8, 4), (7, 5), (7, 6), (6, 7), (7, 7), (8, 7), (9, 7), (9, 8),
            (8, 9), (8, 10), (7, 11), (7, 12), (6, 13)]
    for x, y in bolt:
        px(img, x, y, "W")
    for x, y in ((10, 2), (9, 3), (9, 4), (8, 5), (8, 6), (10, 7), (10, 8), (9, 9), (9, 10), (8, 11), (8, 12)):
        px(img, x, y, "Y")
    for x, y in ((6, 6), (5, 7), (7, 13), (6, 14)):
        px(img, x, y, "c")
    return img


def icon_repair():
    img = from_map([
        "................",
        "........OOO.OOO.",
        "........OWO.OHO.",
        "........OHO.OSO.",
        "........OSOOOSO.",
        ".......OHSSSSmO.",
        "......OHSSSSmO..",
        ".....OHSmOOOO...",
        "....OHSmO.......",
        "...OHSmO........",
        "..OHSmO.........",
        ".OHSmO..........",
        ".OSmO...........",
        ".OOO............",
        "................",
        "................",
    ], C)
    green, green_l = (90, 230, 110, 255), (220, 255, 220, 255)
    for x, y in ((3, 1), (3, 2), (3, 3), (3, 4), (3, 5), (1, 3), (2, 3), (4, 3), (5, 3)):
        px(img, x, y, green)
    px(img, 3, 3, green_l)
    # nanobot sparkles
    for x, y, c in ((12, 10, "c"), (14, 12, "B"), (11, 13, "W"), (14, 8, "B"), (9, 11, "B"), (13, 14, "c")):
        px(img, x, y, c)
    return img


def icon_boom_tube():
    img = blank(16, 16)
    outer = (12, 40, 110, 255)
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            d = math.hypot(dx, dy)
            if d > 7.6:
                continue
            a = math.atan2(dy, dx)
            t = d / 7.6
            band = math.sin(3 * a + d * 1.3)
            base = (255, 255, 255, 255) if t < 0.22 else None
            if base is None:
                light = (1 - t) * 0.9 + (0.35 if band > 0.2 else 0)
                light = max(0.0, min(1.0, light))
                r = int(outer[0] + (210 - outer[0]) * light)
                g = int(outer[1] + (240 - outer[1]) * light)
                b = int(outer[2] + (255 - outer[2]) * light)
                base = (r, g, b, 255)
            if d > 6.9:
                base = (40, 120, 220, 255)
            img.putpixel((x, y), base)
    return img


def icon_emblem():
    img = blank(16, 16)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d > 7.7:
                continue
            if d > 6.8:
                col = C["O"]
            elif x <= 7:
                col = C["k"] if d < 6 or y > 3 else C["K"]
                if y <= 3:
                    col = C["h"]
            else:
                col = C["W"] if y < 5 else (C["S"] if y < 11 else C["m"])
            img.putpixel((x, y), col)
    for y in range(1, 15):
        px(img, 8, y, "d")
    for x in range(9, 15):
        px(img, x, 10, "d" if x < 14 else "O")
    # human eye on the left, glowing red eye on the right
    px(img, 4, 6, "w")
    px(img, 5, 6, "i")
    px(img, 4, 5, "K")
    px(img, 5, 5, "K")
    for x, y, c in ((10, 6, "E"), (11, 6, "Y"), (12, 6, "E"), (11, 5, "R"), (11, 7, "R"),
                    (10, 5, "r"), (12, 5, "r"), (10, 7, "r"), (12, 7, "r")):
        px(img, x, y, c)
    px(img, 4, 10, "p")
    px(img, 5, 10, "p")
    px(img, 6, 10, "p")
    return img


def make_gui():
    """Power icons (16x16 each), in the order of the Cyborg power wheel, then the emblem."""
    icons = (icon_sonic_cannon(), icon_missiles(), icon_tech_scan(), icon_hack(),
             icon_emp(), icon_repair(), icon_boom_tube(), icon_emblem())
    img = blank(128, 16)
    for i, ic in enumerate(icons):
        img.paste(ic, (i * 16, 0))
    save(img, "gui", "cyborg_powers.png")


if __name__ == "__main__":
    make_items()
    make_armor()
    make_gui()
