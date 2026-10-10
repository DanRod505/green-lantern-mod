#!/usr/bin/env python3
"""
Textures of Atlantis: the Atlantis Gate and the Atlantean Respirator items, the respirator worn on
the face, and the people of Atlantis (classic 64x32 skins): citizens in sea-green and gold robes
and the royal guard in scale armor with a finned helmet.

Usage:  pip install pillow numpy
        python tools/generate_atlantis_textures.py
"""
import random

from PIL import Image

from generate_textures import from_map, save

rng = random.Random(1941)

A = {
    ".": (0, 0, 0, 0),
    "O": (14, 26, 30, 255),     # outline
    "y": (150, 104, 18, 255),   # dark gold
    "Y": (236, 186, 46, 255),   # gold
    "Z": (255, 232, 140, 255),  # light gold
    "t": (8, 72, 84, 255),      # deep teal
    "T": (20, 150, 160, 255),   # teal
    "U": (110, 226, 220, 255),  # light teal
    "b": (16, 50, 120, 255),    # deep blue
    "B": (40, 110, 220, 255),   # blue
    "w": (230, 250, 255, 255),  # foam
    "s": (240, 214, 190, 255),  # shell
    "S": (214, 168, 140, 255),  # shell shade
    "g": (110, 120, 130, 255),  # glass grey
}

GATE = [
    "................",
    "........YY......",
    "......YYZZY.....",
    ".....YZYYYZY....",
    "....YZYbbBYZY...",
    "...YZYbBwwBYY...",
    "...YYbBwUUwBY...",
    "..YZYbBUbbUBYY..",
    "..YYbBwUbbUwBY..",
    "..yYbBBwUUwBBY..",
    "..yYYbBBwwBBYy..",
    "...yYYbbBBbYy...",
    "....yYYYYYYy....",
    ".....yyTTyy.....",
    "......yTTy......",
    ".......yy.......",
]

RESPIRATOR = [
    "................",
    "................",
    "...TT......TT...",
    "..TUTt....tTUT..",
    "..TUTtYYYYtTUT..",
    "..TUTYZZZZYTUT..",
    "..TUTYZYYZYTUT..",
    "..tTtYYttYYtTt..",
    "...t.yYTTYy.t...",
    "......yUUy......",
    "......yUUy......",
    ".......yy.......",
    "................",
    "................",
    "................",
    "................",
]

# Classic 64x32 skin layout: (u, v, w, h, d) of each box.
HEAD = (0, 0, 8, 8, 8)
HAT = (32, 0, 8, 8, 8)
BODY = (16, 16, 8, 12, 4)
ARM = (40, 16, 4, 12, 4)
LEG = (0, 16, 4, 12, 4)


def faces(box):
    u, v, w, h, d = box
    return {
        "top": (u + d, v, w, d),
        "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h),
        "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h),
        "back": (u + d + w + d, v + d, w, h),
    }


def shade(c, k):
    return tuple(max(0, min(255, int(x * k))) for x in c[:3]) + (255,)


def noisy(c, n):
    return tuple(max(0, min(255, x + rng.randint(-n, n))) for x in c[:3]) + (255,)


def fill_box(img, box, color, noise=7, which=None, rows=None):
    for name, (x0, y0, w, h) in faces(box).items():
        if which and name not in which:
            continue
        for y in range(y0, y0 + h):
            if rows and not (rows[0] <= y - y0 < rows[1]) and name not in ("top", "bottom"):
                continue
            for x in range(x0, x0 + w):
                img.putpixel((x, y), noisy(color, noise))


def px(img, box, face, x, y, color):
    x0, y0, w, h = faces(box)[face]
    if 0 <= x < w and 0 <= y < h:
        img.putpixel((x0 + x, y0 + y), color)


def side_faces():
    return ("front", "back", "left", "right")


def face(img, skin, eyes):
    """Eyes (sea-coloured, as all Atlanteans have), nose and mouth."""
    px(img, HEAD, "front", 2, 4, (250, 250, 250, 255))
    px(img, HEAD, "front", 5, 4, (250, 250, 250, 255))
    px(img, HEAD, "front", 1, 4, eyes)
    px(img, HEAD, "front", 6, 4, eyes)
    px(img, HEAD, "front", 3, 5, shade(skin, 0.85))
    px(img, HEAD, "front", 4, 5, shade(skin, 0.85))
    for x in (3, 4):
        px(img, HEAD, "front", x, 6, shade(skin, 0.6))


def hair(img, color, style):
    for x in range(8):
        for y in range(8):
            px(img, HEAD, "top", x, y, noisy(color, 10))
    rows = {"short": 2, "long": 3, "braid": 2, "crest": 1}[style]
    for f in side_faces():
        for x in range(8):
            for y in range(rows):
                px(img, HEAD, f, x, y, noisy(color, 10))
    if style == "long":
        for f in ("left", "right", "back"):
            for x in range(8):
                for y in range(3, 8):
                    px(img, HEAD, f, x, y, noisy(color, 10))
    if style == "braid":
        for y in range(2, 8):
            for x in (3, 4):
                px(img, HEAD, "back", x, y, noisy(color, 12))
    if style == "crest":
        # A fin of hair down the middle, on the hat layer.
        for y in range(8):
            for x in (3, 4):
                px(img, HAT, "top", x, y, noisy(shade(color, 1.1), 8))
        for x in (3, 4):
            px(img, HAT, "front", x, 0, noisy(color, 8))
            px(img, HAT, "back", x, 0, noisy(color, 8))


SKINS = [(232, 186, 150), (196, 140, 104), (140, 96, 70), (96, 64, 48), (214, 196, 170), (170, 200, 196)]
HAIRS = [((236, 196, 90), "long"), ((40, 30, 26), "short"), ((180, 70, 40), "braid"), ((20, 20, 26), "crest"),
         ((236, 236, 240), "long"), ((30, 110, 100), "short")]
ROBES = [(30, 138, 120), (24, 96, 140), (60, 150, 90), (20, 120, 150), (90, 70, 150), (30, 140, 140)]
EYES = [(40, 190, 210, 255), (30, 150, 120, 255), (60, 120, 220, 255)]


def citizen(i):
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    skin = SKINS[i]
    for box in (HEAD, BODY, ARM, LEG):
        fill_box(img, box, skin)
    face(img, skin, EYES[i % 3])
    hair(img, *HAIRS[i])
    robe = ROBES[i]
    gold = (236, 186, 46)
    # Robe: the body, the upper arms and a long skirt over the legs.
    fill_box(img, BODY, robe, noise=10)
    fill_box(img, ARM, robe, noise=10, rows=(0, 6))
    fill_box(img, LEG, shade(robe, 0.85), noise=10, rows=(0, 10))
    # Gold trims: collar, belt with a shell clasp, hems.
    for f in side_faces():
        x0, y0, w, h = faces(BODY)[f]
        for x in range(w):
            img.putpixel((x0 + x, y0 + 7), noisy(gold, 8))
            img.putpixel((x0 + x, y0), noisy(shade(gold, 0.9), 8))
        x0, y0, w, h = faces(ARM)[f]
        for x in range(w):
            img.putpixel((x0 + x, y0 + 5), noisy(gold, 8))
        x0, y0, w, h = faces(LEG)[f]
        for x in range(w):
            img.putpixel((x0 + x, y0 + 9), noisy(gold, 8))
    px(img, BODY, "front", 3, 7, (255, 240, 200, 255))
    px(img, BODY, "front", 4, 7, (255, 240, 200, 255))
    # A wave pattern across the chest.
    for x in range(8):
        px(img, BODY, "front", x, 3 + (x % 4 == 1) - (x % 4 == 3), shade(robe, 1.35))
    # Sandals.
    for f in side_faces():
        x0, y0, w, h = faces(LEG)[f]
        for x in range(w):
            img.putpixel((x0 + x, y0 + 11), noisy((120, 80, 40), 8))
    return img


def guard(i):
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    skin = SKINS[(i * 2 + 1) % len(SKINS)]
    for box in (HEAD, BODY, ARM, LEG):
        fill_box(img, box, skin)
    face(img, skin, EYES[i % 3])
    gold = (236, 186, 46)
    scale_a = (222, 112, 22)
    scale_b = (250, 166, 48)
    green = (30, 138, 74)
    # Scale armor of orange and gold on the body and the arms (like the King's).
    for box, rows in ((BODY, (0, 12)), (ARM, (0, 8))):
        for f in side_faces():
            x0, y0, w, h = faces(box)[f]
            for y in range(rows[0], rows[1]):
                for x in range(w):
                    c = scale_b if (x + (y // 2)) % 2 == 0 and y % 2 == 0 else scale_a
                    img.putpixel((x0 + x, y0 + y), noisy(c, 10))
    # Green scaled leggings with gold greaves.
    fill_box(img, LEG, green, noise=12)
    for f in side_faces():
        x0, y0, w, h = faces(LEG)[f]
        for y in range(6, 11):
            for x in range(w):
                img.putpixel((x0 + x, y0 + y), noisy(gold if y in (6, 10) else shade(gold, 0.85), 8))
    # Gold belt and bracers.
    for f in side_faces():
        x0, y0, w, h = faces(BODY)[f]
        for x in range(w):
            img.putpixel((x0 + x, y0 + 10), noisy(gold, 6))
        x0, y0, w, h = faces(ARM)[f]
        for y in range(8, 11):
            for x in range(w):
                img.putpixel((x0 + x, y0 + y), noisy(gold, 8))
    # Helmet on the hat layer: gold dome with a teal fin crest and cheek guards.
    for x in range(8):
        for y in range(8):
            px(img, HAT, "top", x, y, noisy(gold, 10))
    for f in side_faces():
        for x in range(8):
            for y in range(3):
                px(img, HAT, f, x, y, noisy(gold if y < 2 else shade(gold, 0.8), 10))
    for f in ("left", "right", "back"):
        for x in range(8):
            for y in range(3, 6):
                if f != "back" or y < 5:
                    px(img, HAT, f, x, y, noisy(shade(gold, 0.85), 8))
    for y in range(8):
        for x in (3, 4):
            px(img, HAT, "top", x, y, noisy((20, 150, 160), 10))
    for x in (3, 4):
        px(img, HAT, "front", x, 0, (110, 226, 220, 255))
        px(img, HAT, "back", x, 0, (110, 226, 220, 255))
    px(img, HAT, "front", 0, 3, noisy(gold, 6))
    px(img, HAT, "front", 7, 3, noisy(gold, 6))
    return img


def respirator_layer():
    """Neutral, lightly textured surface tinted by the renderer."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            v = 225 + rng.randint(-18, 18) - (12 if x in (0, 15) or y in (0, 15) else 0)
            img.putpixel((x, y), (v, v, v, 255))
    return img


def main():
    save(from_map(GATE, A), "item", "atlantis_gate.png")
    save(from_map(RESPIRATOR, A), "item", "atlantean_respirator.png")
    save(respirator_layer(), "entity", "atlantis", "respirator.png")
    for i in range(6):
        save(citizen(i), "entity", "atlantis", f"citizen_{i}.png")
    for i in range(3):
        save(guard(i), "entity", "atlantis", f"guard_{i}.png")


if __name__ == "__main__":
    main()
