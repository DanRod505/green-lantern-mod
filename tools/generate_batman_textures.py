#!/usr/bin/env python3
"""
Textures of Batman (DC Universe expansion): the Utility Belt, the suit (gray body with the black
bat on a yellow oval, black cowl, gloves and boots, yellow belt), the gadget icons and the plain
grainy texture the cape, the Batmobile and the gadgets are tinted on. Shares helpers with
generate_textures.py.

Usage:  pip install pillow numpy
        python tools/generate_batman_textures.py
"""
import random

from PIL import Image, ImageFilter

from generate_textures import blank, box_faces, fill_rect, from_map, jitter, lerp, save

random.seed(1939)

B = {
    ".": (0, 0, 0, 0),
    "K": (14, 14, 17, 255),     # black
    "D": (38, 39, 45, 255),     # dark gray
    "G": (92, 94, 104, 255),    # suit gray
    "L": (150, 152, 162, 255),  # light gray
    "W": (236, 240, 246, 255),  # white (lenses)
    "Y": (236, 194, 38, 255),   # yellow
    "y": (156, 120, 18, 255),   # dark yellow
    "Z": (255, 236, 140, 255),  # light yellow
    "S": (214, 168, 132, 255),  # skin (the chin below the cowl)
    "E": (110, 150, 190, 255),  # smoked glass
}

COWL_ITEM = [
    "................",
    "....K......K....",
    "....KK....KK....",
    "....KDK..KDK....",
    "....KDDKKDDK....",
    "...KDDDDDDDDK...",
    "...KDDDDDDDDK...",
    "...KDDDDDDDDK...",
    "...KDWWDDWWDK...",
    "...KDDDDDDDDK...",
    "...KDDDDDDDDK...",
    "...KDDSSSSDDK...",
    "....KDSSSSDK....",
    ".....KKSSKK.....",
    "................",
    "................",
]

SUIT_ITEM = [
    "................",
    "...KKKK..KKKK...",
    "..KGGGGKKGGGGK..",
    ".KGGGGGGGGGGGGK.",
    ".KGGYYYYYYYYGGK.",
    ".KGYKYYKKYYKYGK.",
    ".KGYKKKKKKKKYGK.",
    "..KGYKKYYKKYGK..",
    "..KGGYYYYYYGGK..",
    "..KGGGGGGGGGGK..",
    "..KGLGGGGGGLGK..",
    "..KYyYYyYYyYYK..",
    "..KGGGGGGGGGGK..",
    "..KKKKKKKKKKKK..",
    "................",
    "................",
]

LEGS_ITEM = [
    "................",
    "....KKKKKKKK....",
    "....YyYYyYYy....",
    "....KDDDDDDK....",
    "....KGGGGGGK....",
    "....KGGKKGGK....",
    "....KGGKKGGK....",
    "....KGLKKGLK....",
    "....KGGKKGGK....",
    "....KGGKKGGK....",
    "....KGGKKGGK....",
    "....KGGKKGGK....",
    "....KKK..KKK....",
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
    "...KKKK..KKKK...",
    "...KDDK..KDDK...",
    "...KDDK..KDDK...",
    "...KDLK..KDLK...",
    "...KDDK..KDDK...",
    "..KKDDK.KKDDK...",
    ".KDDDDK.KDDDDK..",
    ".KKKKKK.KKKKKK..",
    "................",
    "................",
    "................",
]

BELT_ITEM = [
    "................",
    "................",
    "................",
    "................",
    ".yyy.yyy..yyy.yy",
    "yYZYyYZYyyYZYyYZ",
    "yYYYyYYYZZYYYyYY",
    "YYYYYYYZKKZYYYYY",
    "ZZZZZZZYKKYZZZZZ",
    "yYYYyYYYZZYYYyYY",
    "yYYYyYYYyyYYYyYY",
    ".yyy.yyy..yyy.yy",
    "................",
    "................",
    "................",
    "................",
]

BAT_SMALL = [
    "....X..X....",
    "X...XXXX...X",
    "XX.XXXXXX.XX",
    "XXXXXXXXXXXX",
    ".XXXXXXXXXX.",
    "..XX.XX.XX..",
    "...X....X...",
]


def paint(img, x0, y0, rows, palette, noise=4):
    for y, row in enumerate(rows):
        for x, c in enumerate(row):
            if c != ".":
                img.putpixel((x0 + x, y0 + y), jitter(palette[c], noise) if noise else palette[c])


def make_items():
    save(from_map(BELT_ITEM, B), "item", "utility_belt.png")
    save(from_map(COWL_ITEM, B), "item", "batman_cowl.png")
    save(from_map(SUIT_ITEM, B), "item", "batman_suit.png")
    save(from_map(LEGS_ITEM, B), "item", "batman_leggings.png")
    save(from_map(BOOTS_ITEM, B), "item", "batman_boots.png")


def make_armor():
    black, dark, gray, light = B["K"], B["D"], B["G"], B["L"]
    yellow, yellow_d, yellow_l = B["Y"], B["y"], B["Z"]

    # ---- humanoid layer: cowl, gray body with the emblem, arms with gloves, boots (64x32).
    img = blank(64, 32)
    f = box_faces(0, 0, 8, 8, 8)
    for name, (x0, y0, w, h) in f.items():
        for y in range(h):
            for x in range(w):
                if name == "front":
                    if y >= 5 and 1 <= x <= 6:
                        continue  # mouth and chin uncovered
                    if y == 3 and x in (1, 2, 5, 6):
                        img.putpixel((x0 + x, y0 + y), B["W"])
                        continue
                    if y == 2 and x in (3, 4):
                        img.putpixel((x0 + x, y0 + y), black)
                        continue
                c = dark if (x + y) % 5 else black
                if name == "top" and (x + y) % 3 == 0:
                    c = (46, 47, 54, 255)
                img.putpixel((x0 + x, y0 + y), jitter(c, 3))

    f = box_faces(16, 16, 8, 12, 4)
    for name, (x0, y0, w, h) in f.items():
        for y in range(h):
            for x in range(w):
                c = gray
                # Subtle muscle shading: lighter chest, darker flanks.
                if name == "front" and 1 <= y <= 5 and 1 <= x <= 6:
                    c = lerp(gray, light, 0.18)
                if name in ("right", "left") or x in (0, w - 1):
                    c = lerp(gray, dark, 0.3)
                img.putpixel((x0 + x, y0 + y), jitter(c, 4))
    # The bat on its yellow oval, across the chest.
    fx, fy, fw, fh = f["front"]
    paint(img, fx, fy + 1, [
        ".yYYYYy.",
        "yKYKKYKy",
        "YKKKKKKY",
        "yYKYYKYy",
        ".yYYYYy.",
    ], B, noise=0)
    # Black bat on the back (no oval).
    bx, by, bw, bh = f["back"]
    paint(img, bx, by + 2, [
        "K..KK..K",
        "KKKKKKKK",
        ".KKKKKK.",
        "..K..K..",
    ], B, noise=2)

    f = box_faces(40, 16, 4, 12, 4)
    for name, (x0, y0, w, h) in f.items():
        for y in range(h):
            for x in range(w):
                if name == "top":
                    c = gray
                elif name == "bottom":
                    c = black
                elif y < 6:
                    c = gray if name != "back" else lerp(gray, dark, 0.25)
                else:
                    c = black if (x + y) % 4 else dark
                img.putpixel((x0 + x, y0 + y), jitter(c, 3))
    # The gauntlets' three fins on the outer side.
    for name in ("right", "left"):
        x0, y0, w, h = f[name]
        for i, y in enumerate((6, 8, 10)):
            for x in range(2):
                img.putpixel((x0 + (w - 1 - x if name == "left" else x), y0 + y), light)

    f = box_faces(0, 16, 4, 12, 4)
    for name, (x0, y0, w, h) in f.items():
        if name == "top":
            continue
        if name == "bottom":
            fill_rect(img, x0, y0, w, h, black, 3)
            continue
        for y in range(5, h):
            for x in range(w):
                c = dark if y == 5 else (black if (x + y) % 5 else dark)
                img.putpixel((x0 + x, y0 + y), jitter(c, 3))
    save(img, "entity", "equipment", "humanoid", "batman.png")

    # ---- leggings layer: the yellow utility belt with its pouches, gray legs and dark trunks.
    img = blank(64, 32)
    f = box_faces(16, 16, 8, 12, 4)
    for name, (x0, y0, w, h) in f.items():
        if name in ("top", "bottom"):
            continue
        for y in range(8, 12):
            for x in range(w):
                pouch = (x % 3 == 1) and 9 <= y <= 10
                c = yellow_l if y == 8 else (yellow_d if y == 11 or pouch else yellow)
                img.putpixel((x0 + x, y0 + y), jitter(c, 3))
    fx, fy, fw, fh = f["front"]
    paint(img, fx + 3, fy + 8, ["ZZ", "KK", "ZZ"], B, noise=0)
    f = box_faces(0, 16, 4, 12, 4)
    for name, (x0, y0, w, h) in f.items():
        if name == "bottom":
            continue
        for y in range(h):
            for x in range(w):
                c = dark if y < 3 else gray
                if name in ("right", "left") and y >= 3:
                    c = lerp(gray, dark, 0.2)
                img.putpixel((x0 + x, y0 + y), jitter(c, 4))
    save(img, "entity", "equipment", "humanoid_leggings", "batman.png")


def make_plain():
    """Near-white with a fine grain: the cape and the car are tinted on it per vertex."""
    img = blank(16, 16)
    for y in range(16):
        for x in range(16):
            v = random.randint(228, 255)
            img.putpixel((x, y), (v, v, v, 255))
    save(img, "entity", "batman", "plain.png")


def mask_icon(rows, fill, edge):
    """Icon from an X mask: filled, with a darker one-pixel rim."""
    img = blank(16, 16)
    h = len(rows)
    w = len(rows[0])
    for y in range(h):
        for x in range(w):
            if rows[y][x] != "X":
                continue
            rim = any(not (0 <= x + dx < w and 0 <= y + dy < h) or rows[y + dy][x + dx] != "X"
                      for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            img.putpixel((x, y), edge if rim else fill(x, y))
    return img


def make_gui():
    img = blank(80, 16)
    batarang = [
        "................",
        "................",
        "................",
        "................",
        "......X..X......",
        "X.....XXXX.....X",
        "XX...XXXXXX...XX",
        "XXX.XXXXXXXX.XXX",
        "XXXXXXXXXXXXXXXX",
        "XXXXXXXXXXXXXXXX",
        ".XXXXXXXXXXXXXX.",
        "..XXX.XXXX.XXX..",
        "...X...XX...X...",
        "................",
        "................",
        "................",
    ]
    steel = mask_icon(batarang, lambda x, y: lerp(B["L"], B["G"], y / 12), B["K"])
    grapple = from_map([
        "............L.L.",
        "...........LKKL.",
        "............KL..",
        "...........K....",
        "..........K.....",
        ".........K......",
        "........K.......",
        "..DDDDDDK.......",
        ".DGGGGGGD.......",
        ".DGLLLLGDD......",
        ".DDDDDGGD.......",
        "....DGGD........",
        "....DGD.........",
        "...DGGD.........",
        "...DDD..........",
        "................",
    ], B)
    swarm = blank(16, 16)
    for (ox, oy) in ((0, 1), (6, 5), (2, 9)):
        for y, row in enumerate(BAT_SMALL[:5]):
            for x, c in enumerate(row[:10]):
                if c == "X" and ox + x < 16 and oy + y < 16:
                    swarm.putpixel((ox + x, oy + y), B["K"])
    car = from_map([
        "................",
        "................",
        "................",
        "................",
        "............K...",
        "...........KK...",
        ".....KKKK..KKK..",
        "....KEEEKKKKKKK.",
        "..KKKKKKKKKKKKKK",
        ".KKDDDDDDDDDDDKK",
        "KKDDDDDDDDDDDDDK",
        "YKKKKKKKKKKKKKKy",
        ".KKGGKKKKKKGGKK.",
        "..KGGK....KGGK..",
        "...KK......KK...",
        "................",
    ], B)
    emblem = blank(16, 16)
    for y in range(16):
        for x in range(16):
            dx = (x - 7.5) / 7.6
            dy = (y - 7.5) / 5.6
            d = dx * dx + dy * dy
            if d <= 1.0:
                emblem.putpixel((x, y), B["y"] if d > 0.72 else lerp(B["Z"], B["Y"], min(1.0, d * 1.6)))
    for y, row in enumerate(BAT_SMALL):
        for x, c in enumerate(row):
            if c == "X":
                emblem.putpixel((2 + x, 5 + y), B["K"])
    for i, icon in enumerate((steel, grapple, swarm, car, emblem)):
        if i < 4:
            # Yellow glow behind the icon so the black shapes read on the dark wheel.
            glow = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
            mask = icon.split()[3].filter(ImageFilter.MaxFilter(3))
            glow.paste((236, 194, 38, 170), (0, 0), mask)
            icon = Image.alpha_composite(glow, icon)
        img.paste(icon, (i * 16, 0), icon)
    save(img, "gui", "batman_powers.png")


if __name__ == "__main__":
    make_items()
    make_armor()
    make_plain()
    make_gui()
