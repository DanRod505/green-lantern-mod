#!/usr/bin/env python3
"""
Textures of the Trench (v1.13.0):

* the creature and the brute (64x64), with their glowing eyes and lateral lines (emissive layers)
* the cocoon that holds a captured villager (128x64, translucent membrane)
* the two spawn eggs (item icons, 16x16)

Everything the Trench wears is the opposite of Atlantis: black-blue, scarred, wet skin instead of
gold and teal, pale needle teeth, sickly yellow-green (or, for the brute, red) eyes.
The box layouts match TrenchCreatureModel and TrenchCocoonModel.

Usage:  pip install pillow numpy
        python tools/generate_trench_textures.py
"""
import math
import random

from generate_atlantis_mounts import EGG_SHAPE, make_egg, paint_face, px, shade
from generate_textures import blank, box_faces, lerp, save

random.seed(1313)

CLEAR = (0, 0, 0, 0)
TOOTH = (226, 222, 196, 255)
TOOTH_D = (170, 160, 130, 255)
GUM = (70, 18, 26, 255)
MOUTH = (14, 6, 10, 255)

LOOKS = {
    "creature": {"skin": (34, 46, 58, 255), "dark": (14, 20, 28, 255), "belly": (92, 104, 104, 255), "spot": (60, 76, 84, 255),
                 "fin": (46, 34, 54, 255), "eye": (226, 255, 120, 255), "line": (120, 255, 200, 255)},
    "brute": {"skin": (26, 26, 34, 255), "dark": (8, 8, 12, 255), "belly": (72, 64, 66, 255), "spot": (58, 40, 46, 255),
              "fin": (70, 20, 28, 255), "eye": (255, 70, 40, 255), "line": (255, 120, 60, 255)},
}


def skin(look, scars=False):
    """Mottled, wet skin: dark blotches, pale speckles, and (brute) old scars."""
    def fn(x, y, w, h):
        n = math.sin(x * 1.7 + y * 0.9) + math.sin(x * 0.6 - y * 1.3 + 2) + random.uniform(-0.6, 0.6)
        c = look["skin"]
        if n > 1.1:
            c = look["spot"]
        elif n < -1.2:
            c = look["dark"]
        if random.random() < 0.03:
            c = shade(look["belly"], 0.9)
        if scars and (x + 2 * y) % 11 == 0 and random.random() < 0.5:
            c = (120, 96, 96, 255)
        return c
    return fn


def ribbed(look):
    """The belly of the torso: pale skin over ribs."""
    def fn(x, y, w, h):
        c = look["belly"]
        if y % 3 == 0:
            c = shade(c, 0.65)
        if x in (0, w - 1):
            c = look["skin"]
        return c
    return fn


def box(img, u, v, w, h, d, look, front=None, scars=False):
    f = box_faces(u, v, w, h, d)
    for name, rect in f.items():
        fn = front if name == "front" and front else skin(look, scars)
        paint_face(img, rect, fn, 5)


def teeth_face(x, y, w, h, upper=True):
    """A row of needle teeth: pale spikes with gaps (cutout)."""
    tip = h - 1 if upper else 0
    if x % 2 == 1:
        return CLEAR if y == tip else (TOOTH_D if random.random() < 0.3 else TOOTH)
    return TOOTH if y != tip and random.random() < 0.6 else CLEAR


def make_creature(kind, look, scars=False):
    img = blank(64, 64)
    glow = blank(64, 64)
    # Torso 8x11x5 at (0,0): ribbed belly in front, a ridge of dark spines on the back.
    box(img, 0, 0, 8, 11, 5, look, ribbed(look), scars)
    f = box_faces(0, 0, 8, 11, 5)
    bx, by, bw, bh = f["back"]
    for y in range(bh):
        px(img, bx + bw // 2, by + y, look["dark"])
    # Lateral lines: glowing dots down the sides.
    for side in ("left", "right"):
        sx, sy, sw, sh = f[side]
        for y in range(1, sh, 2):
            px(glow, sx + sw // 2, sy + y, look["line"])
    # Jaw 6x2x7 at (26,0): skin outside, gums and throat inside (top face).
    box(img, 26, 0, 6, 2, 7, look, scars=scars)
    f = box_faces(26, 0, 6, 2, 7)
    paint_face(img, f["top"], lambda x, y, w, h: GUM if 0 < x < w - 1 else MOUTH, 3)
    # Dorsal fin 1x10x3 at (52,0): torn membrane with spines.
    f = box_faces(52, 0, 1, 10, 3)
    for name, rect in f.items():
        paint_face(img, rect, lambda x, y, w, h: CLEAR if (x + y) % 4 == 3 and x > 0 else (look["dark"] if x == 0 else look["fin"]), 4)
    # Head 7x6x8 at (0,16): the eyes on the front and sides glow; the underside is the roof of the mouth.
    box(img, 0, 16, 7, 6, 8, look, scars=scars)
    f = box_faces(0, 16, 7, 6, 8)
    paint_face(img, f["bottom"], lambda x, y, w, h: GUM if 0 < x < w - 1 and y < h - 2 else MOUTH, 3)
    fx, fy, fw, fh = f["front"]
    # Two big eyes and two small ones above them; dark sockets.
    for (ex, ey, big) in ((1, 2, True), (fw - 3, 2, True), (2, 0, False), (fw - 3, 0, False)):
        for dx in range(2 if big else 1):
            for dy in range(2 if big else 1):
                px(img, fx + ex + dx, fy + ey + dy, (10, 10, 8, 255))
                px(glow, fx + ex + dx, fy + ey + dy, look["eye"])
    for side in ("left", "right"):
        sx, sy, sw, sh = f[side]
        ex = sw - 3 if side == "right" else 1
        px(img, sx + ex, sy + 2, (10, 10, 8, 255))
        px(glow, sx + ex, sy + 2, look["eye"])
        px(img, sx + ex + 1, sy + 2, (10, 10, 8, 255))
        px(glow, sx + ex + 1, sy + 2, shade(look["eye"], 0.8))
        # Gill slits.
        for gy in range(1, sh - 1):
            if gy % 2 == 0:
                px(img, sx + (2 if side == "right" else sw - 3), sy + gy, look["dark"])
    # Upper teeth 6x2x6 at (30,16) and lower teeth 5x2x5 at (30,24): cutout spikes.
    f = box_faces(30, 16, 6, 2, 6)
    for name in ("front", "left", "right", "back"):
        paint_face(img, f[name], lambda x, y, w, h: teeth_face(x, y, w, h, True), 0)
    paint_face(img, f["top"], lambda *a: GUM, 0)
    paint_face(img, f["bottom"], lambda *a: CLEAR, 0)
    f = box_faces(30, 24, 5, 2, 5)
    for name in ("front", "left", "right", "back"):
        paint_face(img, f[name], lambda x, y, w, h: teeth_face(x, y, w, h, False), 0)
    paint_face(img, f["top"], lambda *a: CLEAR, 0)
    paint_face(img, f["bottom"], lambda *a: GUM, 0)
    # Crest 1x4x9 at (0,30): a ragged fin, its tips faintly glowing.
    f = box_faces(0, 30, 1, 4, 9)
    for name, rect in f.items():
        paint_face(img, rect, lambda x, y, w, h: CLEAR if y == 0 and x % 2 == 0 else look["fin"], 4)
    lx, ly, lw, lh = f["left"]
    for x in range(1, lw, 2):
        px(glow, lx + x, ly + 1, shade(look["line"], 0.8))
    # Upper arm 2x9x2 (20,30), forearm 2x8x2 (28,31): sinewy.
    box(img, 20, 30, 2, 9, 2, look, scars=scars)
    box(img, 28, 31, 2, 8, 2, look, scars=scars)
    # Hand 3x2x2 (36,31) and the claws, a 3x4 plane (46,31): long hooked talons.
    box(img, 36, 31, 3, 2, 2, look)
    f = box_faces(46, 31, 3, 4, 0)
    for name, rect in f.items():
        paint_face(img, rect, lambda x, y, w, h: (TOOTH if y == h - 1 else TOOTH_D) if (x % 2 == 0 or y < 1) else CLEAR, 0)
    # Thigh 3x7x3 (52,31), shin 2x7x2 (36,36), webbed flipper 4x5 plane (44,36).
    box(img, 52, 31, 3, 7, 3, look, scars=scars)
    box(img, 36, 36, 2, 7, 2, look, scars=scars)
    f = box_faces(44, 36, 4, 5, 0)
    for name, rect in f.items():
        paint_face(img, rect, lambda x, y, w, h: look["dark"] if x in (0, w - 1) or x == w // 2 else (look["fin"] if y < h - 1 else CLEAR), 4)
    save(img, "entity", "trench", kind + ".png")
    save(glow, "entity", "trench", kind + "_glow.png")


def make_cocoon():
    img = blank(128, 64)
    rnd = random.Random(77)

    def membrane(x, y, w, h, solid=0.0):
        # Translucent flesh with darker veins; the bottom is thicker.
        vein = abs(math.sin(x * 0.9 + math.sin(y * 0.35) * 2.5)) < 0.18 or abs(math.sin(y * 0.6 + x * 0.25)) < 0.08
        if vein:
            return (70, 10, 30, 235)
        base = (128, 40, 66)
        a = int(110 + solid * 120 + rnd.uniform(-15, 15))
        c = lerp(base + (255,), (150, 70, 90, 255), rnd.random() * 0.3)
        return (c[0], c[1], c[2], max(60, min(255, a)))

    layers = [((0, 0), (8, 4, 8), 0.8), ((0, 12), (12, 6, 12), 0.4), ((0, 30), (14, 12, 14), 0.0), ((64, 0), (12, 8, 12), 0.2),
              ((64, 20), (8, 5, 8), 0.6)]
    for (u, v), (w, h, d), solid in layers:
        for name, rect in box_faces(u, v, w, h, d).items():
            paint_face(img, rect, lambda x, y, fw, fh, s=solid: membrane(x + u, y + v, fw, fh, s), 0)
    # Stalk 3x24x3 (112,0) and tendrils 2x16x2 (96,20): dark, sinewy and opaque.
    for (u, v, w, h, d) in ((112, 0, 3, 24, 3), (96, 20, 2, 16, 2)):
        for name, rect in box_faces(u, v, w, h, d).items():
            paint_face(img, rect, lambda x, y, fw, fh: (60, 14, 28, 255) if (x + y) % 3 else (34, 8, 18, 255), 6)
    save(img, "entity", "trench", "cocoon.png")


def main():
    make_creature("creature", LOOKS["creature"])
    make_creature("brute", LOOKS["brute"], scars=True)
    make_cocoon()
    make_egg("trench_creature_spawn_egg", (30, 40, 52, 255), (220, 255, 120, 255))
    make_egg("trench_brute_spawn_egg", (20, 18, 24, 255), (255, 80, 50, 255))


if __name__ == "__main__":
    main()
