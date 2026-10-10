#!/usr/bin/env python3
"""
Textures of Oa: the Lanterns of many species (classic 64x32 skins wearing the
mod's uniform), their glow layer, the Guardians and the portal vortex.

Usage:  pip install pillow numpy
        python tools/generate_oa.py
"""
import math
import os
import random

from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "greenlantern", "textures")
OUT = os.path.join(ROOT, "entity", "oa")
SUIT = Image.open(os.path.join(ROOT, "entity", "equipment", "humanoid", "green_lantern.png")).convert("RGBA")
LEGS = Image.open(os.path.join(ROOT, "entity", "equipment", "humanoid_leggings", "green_lantern.png")).convert("RGBA")
rng = random.Random(2814)

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


def fill_box(img, box, color, noise=8, which=None):
    for name, (x0, y0, w, h) in faces(box).items():
        if which and name not in which:
            continue
        for y in range(y0, y0 + h):
            for x in range(x0, x0 + w):
                n = rng.randint(-noise, noise)
                img.putpixel((x, y), tuple(max(0, min(255, c + n)) for c in color[:3]) + (255,))


def px(img, face_box, face, x, y, color):
    x0, y0, w, h = faces(face_box)[face]
    if 0 <= x < w and 0 <= y < h:
        img.putpixel((x0 + x, y0 + y), color)


def suit_up(img):
    """Composites the uniform (leggings first, then suit, mask and boots) over a skin."""
    img.alpha_composite(LEGS)
    img.alpha_composite(SUIT)
    return img


def save(img, name):
    os.makedirs(OUT, exist_ok=True)
    path = os.path.join(OUT, name)
    img.save(path)
    print("wrote", os.path.relpath(path))


# ------------------------------------------------------------------ Lanterns

SPECIES = [
    # skin, hair, hair style, extras
    {"skin": (226, 178, 140), "hair": (92, 58, 34), "style": "short"},
    {"skin": (122, 78, 52), "hair": (24, 18, 16), "style": "buzz"},
    {"skin": (238, 196, 160), "hair": (232, 196, 96), "style": "long"},
    {"skin": (64, 164, 150), "hair": None, "style": "ridges"},
    {"skin": (138, 148, 166), "hair": None, "style": "rock"},
    {"skin": (238, 152, 60), "hair": None, "style": "antennae"},
    {"skin": (140, 96, 196), "hair": (240, 240, 250), "style": "crest"},
    {"skin": (178, 70, 58), "hair": (40, 20, 20), "style": "horns"},
]


def lantern(spec):
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    skin = spec["skin"]
    noise = 14 if spec["style"] == "rock" else 7
    for box in (HEAD, BODY, ARM, LEG):
        fill_box(img, box, skin, noise)
    dark = shade(skin, 0.7)
    # Face: mouth and nose (the mask covers the eyes).
    px(img, HEAD, "front", 3, 6, shade(skin, 0.55))
    px(img, HEAD, "front", 4, 6, shade(skin, 0.55))
    px(img, HEAD, "front", 3, 5, shade(skin, 0.85))
    px(img, HEAD, "front", 4, 5, shade(skin, 0.85))
    hair = spec["hair"]
    style = spec["style"]
    if hair:
        h = hair + (255,)
        for x in range(8):
            for y in range(8):
                px(img, HEAD, "top", x, y, shade(h, 0.9 + 0.2 * rng.random()))
        rows = {"short": 2, "buzz": 1, "long": 3, "crest": 0, "horns": 1}.get(style, 2)
        for face in ("front", "left", "right", "back"):
            for x in range(8):
                for y in range(rows):
                    px(img, HEAD, face, x, y, shade(h, 0.85 + 0.2 * rng.random()))
        if style == "long":
            for face in ("left", "right", "back"):
                for x in range(8):
                    for y in range(3, 8 if face == "back" else 6):
                        px(img, HEAD, face, x, y, shade(h, 0.8 + 0.2 * rng.random()))
        if style == "crest":
            # Skin-coloured scalp with a white crest down the middle.
            for x in range(8):
                for y in range(8):
                    px(img, HEAD, "top", x, y, shade(skin + (255,), 0.95))
            for y in range(8):
                for x in (3, 4):
                    px(img, HEAD, "top", x, y, h)
                    px(img, HAT, "top", x, y, h)
            for face in ("front", "back"):
                for x in (3, 4):
                    px(img, HEAD, face, x, 0, h)
                    px(img, HAT, face, x, 0, h)
                    px(img, HAT, face, x, 1, h)
    if style == "ridges":
        for x in range(8):
            for y in range(0, 8, 2):
                px(img, HEAD, "top", x, y, dark)
        for face in ("left", "right"):
            for y in range(0, 6, 2):
                for x in range(8):
                    px(img, HEAD, face, x, y, dark)
    if style == "rock":
        # Heavy brow and craggy cheeks.
        for x in range(8):
            px(img, HEAD, "front", x, 2, shade(skin, 0.6))
        for (x, y) in ((1, 6), (6, 6), (0, 4), (7, 4), (2, 7), (5, 7)):
            px(img, HEAD, "front", x, y, shade(skin, 0.75))
        for x in range(2, 6):
            px(img, HEAD, "front", x, 6, shade(skin, 0.45))
    if style == "antennae":
        tip = (255, 240, 120, 255)
        for x in (1, 6):
            for y in (0, 1, 2):
                px(img, HAT, "front", x, y, shade(skin, 0.8))
            px(img, HAT, "front", x, 0, tip)
        for x in range(8):
            px(img, HEAD, "front", x, 7, shade(skin, 0.9))
    if style == "horns":
        bone = (230, 220, 196, 255)
        for face in ("front",):
            for (x, y) in ((0, 0), (1, 1), (7, 0), (6, 1), (0, 1), (7, 1)):
                px(img, HAT, face, x, y, bone)
        for face in ("left", "right"):
            for (x, y) in ((3, 0), (4, 0), (4, 1)):
                px(img, HAT, face, x, y, bone)
    return suit_up(img)


def glow_layer(skins):
    """Emissive layer: the bright parts of the uniform (emblem, ring, lenses, piping)."""
    base = skins[0]
    img = Image.new("RGBA", base.size, (0, 0, 0, 0))
    for y in range(base.size[1]):
        for x in range(base.size[0]):
            r, g, b, a = SUIT.getpixel((x, y))
            if a > 0 and g > 190 and r < 230:
                k = 0.55 if r < 120 else 1.0
                img.putpixel((x, y), (int(r * k), int(g * k), int(b * k), 255))
    return img


# ------------------------------------------------------------------ Guardians

def guardian():
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    skin = (88, 128, 214)
    robe = (176, 30, 34)
    trim = (40, 12, 14)
    white = (236, 238, 244, 255)
    fill_box(img, HEAD, skin, 6)
    fill_box(img, BODY, robe, 8)
    fill_box(img, ARM, robe, 8)
    fill_box(img, LEG, robe, 8)
    # Hands.
    for x in range(4):
        for face in ("front", "back", "left", "right"):
            for y in (10, 11):
                px(img, ARM, face, x, y, shade(skin + (255,), 1.0))
        px(img, ARM, "bottom", x, 0, skin + (255,))
    # Face: bald dome, big dark eyes, wrinkled brow, white tufts above the ears.
    for x in range(8):
        px(img, HEAD, "front", x, 2, shade(skin + (255,), 0.8))
    for (x, y) in ((1, 3), (2, 3), (5, 3), (6, 3)):
        px(img, HEAD, "front", x, y, white)
    for (x, y) in ((2, 4), (1, 4), (5, 4), (6, 4)):
        px(img, HEAD, "front", x, y, (20, 22, 40, 255))
    px(img, HEAD, "front", 3, 5, shade(skin + (255,), 0.75))
    px(img, HEAD, "front", 4, 5, shade(skin + (255,), 0.75))
    for x in (2, 3, 4, 5):
        px(img, HEAD, "front", x, 7, shade(skin + (255,), 0.6))
    for face in ("left", "right"):
        for x in range(1, 7):
            for y in range(2, 6):
                if rng.random() < 0.85:
                    px(img, HEAD, face, x, y, white)
                    px(img, HAT, face, x, y, white)
    for x in range(8):
        for y in range(2, 5):
            px(img, HEAD, "back", x, y, white)
            if rng.random() < 0.6:
                px(img, HAT, "back", x, y, white)
    for x in (0, 7):
        for y in range(2, 6):
            px(img, HAT, "front", x, y, white)
    # Robe: dark collar, trim down the front, and the Corps emblem on the chest.
    for x in range(8):
        px(img, BODY, "front", x, 0, trim + (255,))
        px(img, BODY, "back", x, 0, trim + (255,))
        px(img, BODY, "front", x, 11, trim + (255,))
    for y in range(12):
        px(img, BODY, "front", 3, y, shade(robe + (255,), 0.75))
        px(img, BODY, "front", 4, y, shade(robe + (255,), 0.75))
    emblem = [(2, 2), (3, 2), (4, 2), (5, 2), (2, 3), (5, 3), (2, 4), (5, 4), (2, 5), (3, 5), (4, 5), (5, 5)]
    for (x, y) in emblem:
        px(img, BODY, "front", x, y, (200, 255, 210, 255))
    for x in range(4):
        px(img, LEG, "front", x, 11, trim + (255,))
        px(img, LEG, "back", x, 11, trim + (255,))
        px(img, LEG, "left", x, 11, trim + (255,))
        px(img, LEG, "right", x, 11, trim + (255,))
    return img


# ------------------------------------------------------------------ portal

def portal_swirl(size=128):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    c = (size - 1) / 2
    for y in range(size):
        for x in range(size):
            dx, dy = (x - c) / c, (y - c) / c
            r = math.hypot(dx, dy)
            if r > 1.0:
                continue
            th = math.atan2(dy, dx)
            arms = 0.5 + 0.5 * math.cos(4 * th + 10 * r)
            fine = 0.5 + 0.5 * math.cos(11 * th - 22 * r)
            core = max(0.0, 1.0 - r * 2.6)
            v = 0.3 + 0.5 * arms ** 2 + 0.2 * fine * r + core
            a = 0.55 + 0.35 * arms + 0.4 * core
            edge = min(1.0, (1.0 - r) * 12)
            v = min(1.0, v)
            img.putpixel((x, y), (int(150 + 105 * v), 255, int(170 + 85 * v), int(255 * min(1.0, a) * edge)))
    return img


def main():
    skins = [lantern(s) for s in SPECIES]
    for i, img in enumerate(skins):
        save(img, f"lantern_{i}.png")
    save(glow_layer(skins), "lantern_glow.png")
    save(guardian(), "guardian.png")
    save(portal_swirl(), "portal_swirl.png")


if __name__ == "__main__":
    main()
