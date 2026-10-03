#!/usr/bin/env python3
"""
Procedural / hand-authored pixel art generator for the Green Lantern mod.

Every texture of the mod is produced by this script so that the art can be
tweaked and regenerated consistently in future updates.

Usage:  pip install pillow numpy
        python tools/generate_textures.py
"""
import math
import os
import random

from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources")
TEX = os.path.join(ROOT, "assets", "greenlantern", "textures")
random.seed(2814)

# --------------------------------------------------------------- palette
P = {
    ".": (0, 0, 0, 0),
    # greens
    "K": (6, 36, 14, 255),      # darkest outline
    "D": (12, 70, 28, 255),     # dark green
    "g": (20, 110, 42, 255),    # mid-dark green
    "G": (33, 168, 64, 255),    # lantern green
    "L": (72, 222, 104, 255),   # light green
    "W": (178, 255, 196, 255),  # glow white-green
    "H": (240, 255, 244, 255),  # highlight
    # blacks / metals
    "k": (14, 14, 16, 255),
    "b": (28, 28, 32, 255),
    "B": (46, 46, 52, 255),
    "m": (70, 76, 72, 255),     # metal dark
    "M": (120, 130, 124, 255),  # metal mid
    "N": (176, 186, 180, 255),  # metal light
    "s": (205, 214, 208, 255),  # silver highlight
    "e": (230, 250, 236, 255),  # eye lens
}


def from_map(rows, palette=P):
    h = len(rows)
    w = len(rows[0])
    img = Image.new("RGBA", (w, h))
    for y, row in enumerate(rows):
        assert len(row) == w, (y, row, len(row), w)
        for x, c in enumerate(row):
            img.putpixel((x, y), palette[c])
    return img


def save(img, *path):
    full = os.path.join(TEX, *path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    img.save(full)
    print("wrote", os.path.relpath(full))


def jitter(c, amt=10):
    r, g, b, a = c
    d = random.randint(-amt, amt)
    return (max(0, min(255, r + d)), max(0, min(255, g + d)), max(0, min(255, b + d)), a)


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(len(a)))


# --------------------------------------------------------------- items

RING = [
    "................",
    ".....KKKKKK.....",
    "....KgGLLGgK....",
    "...KgLWHHWLgK...",
    "...KGWLggLWGK...",
    "...KGWgDDgWGK...",
    "...KGWgDDgWGK...",
    "...KGWLggLWGK...",
    "...KgLWWWWLgK...",
    "...mKgGGGGgKm...",
    "..mNsKKKKKKsNm..",
    ".mNs........sNm.",
    ".mM..........Mm.",
    ".mNs........sNm.",
    "..mNNsssssNNNm..",
    "...mmmmmmmmmm...",
]

# Ring while a construct is being used: the gem burns white-hot.
RING_ACTIVE = [
    "....W......W....",
    ".W...KKKKKK...W.",
    "....KLWHHWLK....",
    "...KLWHHHHWLK...",
    "W..KWHWLLWHWK..W",
    "...KWHLWWLHWK...",
    "...KWHLWWLHWK...",
    "W..KWHWLLWHWK..W",
    "...KLWHHHHWLK...",
    "...mKLWWWWLKm...",
    "..mNsKKKKKKsNm..",
    ".mNs........sNm.",
    ".mM..........Mm.",
    ".mNs........sNm.",
    "..mNNsssssNNNm..",
    "...mmmmmmmmmm...",
]

LANTERN_ITEM = [
    "......mmmm......",
    ".....m....m.....",
    ".....m....m.....",
    "....KMNNNNMK....",
    "...KmMssssMmK...",
    "...KDDDDDDDDK...",
    "...mgLWHHWLgm...",
    "...mGWWLLWWGm...",
    "...NLWG..GWLN...",
    "...NLWG..GWLN...",
    "...mGWWLLWWGm...",
    "...mgLWWWWLgm...",
    "...KDDDDDDDDK...",
    "..KmMNNNNNNMmK..",
    "..KmmMMMMMMmmK..",
    "...KKKKKKKKKK...",
]

MASK_ITEM = [
    "................",
    "................",
    "................",
    "................",
    "..KK........KK..",
    ".KGLKK....KKLGK.",
    "KGLWLGKKKKGLWLGK",
    "KGeeeeLGGLeeeeGK",
    "KGeeeeeLLeeeeeGK",
    "KgGeeeeGGeeeeGgK",
    ".KgGGGgKKgGGGgK.",
    "..KKKKK..KKKKK..",
    "................",
    "................",
    "................",
    "................",
]

SUIT_ITEM = [
    "................",
    "...KKK....KKK...",
    "..KGGLKKKKLGGK..",
    ".KGLLGkbbkGLLGK.",
    ".KGGGkkWWkkGGGK.",
    ".KgGKkWGGWkKGgK.",
    "..KKkbWGGWbkKK..",
    "....kbkWWkbk....",
    "....kbbkkbbk....",
    "....kGGGGGGk....",
    "....kbbbbbbk....",
    "....kbbbbbbk....",
    "....kbbkkbbk....",
    "....kbBkkBbk....",
    ".....kk..kk.....",
    "................",
]

LEGS_ITEM = [
    "................",
    "....kkkkkkkk....",
    "....kGGGGGGk....",
    "....kbbbbbbk....",
    "....kGbbbbGk....",
    "....kGbkkbGk....",
    "....kGbkkbGk....",
    "....kGbkkbGk....",
    "....kGbkkbGk....",
    "....kGbkkbGk....",
    "....kGbkkbGk....",
    "....kGbkkbGk....",
    "....kGbkkbGk....",
    "....kGBkkBGk....",
    "....kkkkkkkk....",
    "................",
]

BOOTS_ITEM = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "...KKKK..KKKK...",
    "...KLGK..KGLK...",
    "...KLGK..KGLK...",
    "...KLGK..KGLK...",
    "...KLGK..KGLK...",
    "...KLGGK.KGLGK..",
    "..KWLGGGKKGLGGK.",
    "..KLGGGGKKGGGGK.",
    "..KDDDDDKKDDDDK.",
    "...KKKKK..KKKKK.",
    "................",
]


def make_items():
    save(from_map(RING), "item", "power_ring.png")
    save(from_map(RING_ACTIVE), "item", "power_ring_active.png")
    save(from_map(MASK_ITEM), "item", "lantern_mask.png")
    save(from_map(SUIT_ITEM), "item", "lantern_suit.png")
    save(from_map(LEGS_ITEM), "item", "lantern_leggings.png")
    save(from_map(BOOTS_ITEM), "item", "lantern_boots.png")


# --------------------------------------------------------------- lantern block

def make_lantern_block():
    """Textures for the 3D Power Battery block model (see models/block/power_battery.json)."""
    # Metal frame: dark gunmetal with a green tint, bevelled edges and rivets.
    img = blank(16, 16)
    for y in range(16):
        for x in range(16):
            edge = min(x, y, 15 - x, 15 - y)
            base = (52, 64, 58, 255) if edge > 1 else ((150, 164, 156, 255) if edge == 0 else (96, 110, 102, 255))
            if edge > 1 and (x + y) % 7 == 0:
                base = lerp(base, (70, 84, 76, 255), 0.6)
            img.putpixel((x, y), jitter(base, 4))
    for (x, y) in [(2, 2), (13, 2), (2, 13), (13, 13)]:
        img.putpixel((x, y), (190, 204, 196, 255))
        img.putpixel((x + 1, y + 1) if x < 8 else (x - 1, y + 1), (30, 36, 32, 255))
    save(img, "block", "power_battery_metal.png")

    # Glass: translucent green with a bright rim and the Corps emblem.
    img = blank(16, 16)
    for y in range(16):
        for x in range(16):
            edge = min(x, y, 15 - x, 15 - y)
            if edge == 0:
                c = (170, 255, 190, 230)
            else:
                t = abs(y - 7.5) / 7.5
                c = lerp((60, 230, 100, 120), (30, 170, 70, 150), t)
                if (x + y) % 5 == 0:
                    c = lerp(c, (140, 255, 170, 150), 0.35)
            img.putpixel((x, y), c)
    d = ImageDraw.Draw(img)
    d.ellipse([4, 4, 11, 11], outline=(225, 255, 232, 255), width=1)
    d.line([3, 2, 12, 2], fill=(225, 255, 232, 255))
    d.line([3, 13, 12, 13], fill=(225, 255, 232, 255))
    save(img, "block", "power_battery_glass.png")

    # Core: animated pulsing flame (4 frames stacked vertically, see .mcmeta).
    for name, bright in (("power_battery_core", 0.0), ("power_battery_core_charging", 1.0)):
        frames = 8
        sheet = blank(16, 16 * frames)
        for f in range(frames):
            phase = math.sin(f / frames * math.tau)
            for y in range(16):
                for x in range(16):
                    dx = (x - 7.5) / 7.5
                    dy = (y - 8.5 - phase) / 8.0
                    dist = math.sqrt(dx * dx * (1.6 - 0.4 * bright) + dy * dy)
                    k = max(0.0, 1.0 - dist)
                    glow = min(1.0, k * (1.4 + 0.3 * phase + 0.8 * bright))
                    c = lerp((20, 150, 55, 255), (245, 255, 245, 255), glow ** 1.3)
                    sheet.putpixel((x, f * 16 + y), c)
        save(sheet, "block", name + ".png")
        meta = os.path.join(TEX, "block", name + ".png.mcmeta")
        with open(meta, "w") as fh:
            fh.write('{\n  "animation": {\n    "frametime": %d,\n    "interpolate": true\n  }\n}\n' % (2 if bright else 4))


# --------------------------------------------------------------- armor (equipment)

def blank(w, h):
    return Image.new("RGBA", (w, h), (0, 0, 0, 0))


def fill_rect(img, x0, y0, w, h, color, noise=6):
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            img.putpixel((x, y), jitter(color, noise) if noise else color)


def box_faces(u, v, w, h, d):
    """Return dict of face rectangles for the vanilla box UV layout."""
    return {
        "top": (u + d, v, w, d),
        "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h),
        "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h),
        "back": (u + d + w + d, v + d, w, h),
    }


BLACK = P["b"]
BLACK2 = P["B"]
GREEN = P["G"]
GREEN_L = P["L"]
GREEN_D = P["g"]
WHITE = P["W"]


def make_armor():
    # ---- humanoid layer (helmet, chestplate, arms, boots) 64x32
    img = blank(64, 32)

    # HEAD box (u0,v0, 8x8x8) - only the domino mask around the eyes is opaque
    f = box_faces(0, 0, 8, 8, 8)
    fx, fy, _, _ = f["front"]
    mask_rows = [
        "........",
        "........",
        "........",
        "GLLGGLLG",
        "LeeGGeeL",
        "gGGggGGg",
        "........",
        "........",
    ]
    for y, row in enumerate(mask_rows):
        for x, c in enumerate(row):
            if c != ".":
                img.putpixel((fx + x, fy + y), P[c])
    # mask straps on the sides
    for face in ("right", "left"):
        sx, sy, sw, sh = f[face]
        for x in range(sw):
            img.putpixel((sx + x, sy + 3), P["g"])
            img.putpixel((sx + x, sy + 4), P["D"])
    bx, by, bw, bh = f["back"]
    for x in range(bw):
        img.putpixel((bx + x, by + 3), P["g"])
        img.putpixel((bx + x, by + 4), P["D"])

    # BODY box (u16,v16, 8x12x4)
    f = box_faces(16, 16, 8, 12, 4)
    for name, (x, y, w, h) in f.items():
        fill_rect(img, x, y, w, h, BLACK, 5)
    # green shoulders/chest top on front and back
    fx, fy, fw, fh = f["front"]
    chest = [
        "GLLLLLLG",
        "GGLLLLGG",
        "kGGWWGGk",
        "kgWGGWgk",
        "kbWGGWbk",
        "kbgWWgbk",
        "kbbggbbk",
        "bbbbbbbb",
        "GGGGGGGG",
        "bbbbbbbb",
        "bbbBBbbb",
        "bbbbbbbb",
    ]
    for y, row in enumerate(chest):
        for x, c in enumerate(row):
            img.putpixel((fx + x, fy + y), jitter(P[c], 4))
    bx, by, bw, bh = f["back"]
    back = [
        "GLLLLLLG",
        "GGLLLLGG",
        "kGGGGGGk",
        "kbGGGGbk",
        "kbbGGbbk",
        "kbbbbbbk",
        "bbbbbbbb",
        "bbbbbbbb",
        "GGGGGGGG",
        "bbbbbbbb",
        "bbbbbbbb",
        "bbbbbbbb",
    ]
    for y, row in enumerate(back):
        for x, c in enumerate(row):
            img.putpixel((bx + x, by + y), jitter(P[c], 4))
    for face in ("right", "left"):
        sx, sy, sw, sh = f[face]
        for x in range(sw):
            img.putpixel((sx + x, sy), GREEN_L)
            img.putpixel((sx + x, sy + 1), GREEN)
            img.putpixel((sx + x, sy + 8), GREEN)
    tx, ty, tw, th = f["top"]
    fill_rect(img, tx, ty, tw, th, GREEN, 5)

    # ARM box (u40,v16, 4x12x4): green upper shoulder, black arm, green gloves
    f = box_faces(40, 16, 4, 12, 4)
    for name, (x, y, w, h) in f.items():
        if name in ("top",):
            fill_rect(img, x, y, w, h, GREEN, 5)
        elif name == "bottom":
            fill_rect(img, x, y, w, h, GREEN_D, 5)
        else:
            for yy in range(h):
                for xx in range(w):
                    if yy < 3:
                        c = GREEN_L if yy == 0 else GREEN
                    elif yy >= 8:
                        c = GREEN if yy > 8 else GREEN_L  # glove with cuff
                    else:
                        c = BLACK
                    img.putpixel((x + xx, y + yy), jitter(c, 5))

    # BOOTS use the LEG box (u0,v16, 4x12x4) on this layer: green lower half
    f = box_faces(0, 16, 4, 12, 4)
    for name, (x, y, w, h) in f.items():
        if name == "top":
            continue
        if name == "bottom":
            fill_rect(img, x, y, w, h, GREEN_D, 4)
            continue
        for yy in range(h):
            for xx in range(w):
                if yy >= 6:
                    c = GREEN_L if yy == 6 else (GREEN_D if yy == 11 else GREEN)
                    img.putpixel((x + xx, y + yy), jitter(c, 5))
    save(img, "entity", "equipment", "humanoid", "green_lantern.png")

    # ---- humanoid_leggings layer 64x32: body (belt) + legs
    img = blank(64, 32)
    f = box_faces(16, 16, 8, 12, 4)
    for name, (x, y, w, h) in f.items():
        if name in ("top", "bottom"):
            continue
        for yy in range(h):
            for xx in range(w):
                if yy >= 7:
                    c = GREEN if yy == 8 else BLACK
                    if yy == 8 and name == "front" and xx in (3, 4):
                        c = WHITE
                    img.putpixel((x + xx, y + yy), jitter(c, 4))
    f = box_faces(0, 16, 4, 12, 4)
    for name, (x, y, w, h) in f.items():
        if name == "top":
            fill_rect(img, x, y, w, h, BLACK, 4)
            continue
        if name == "bottom":
            continue
        for yy in range(h):
            for xx in range(w):
                c = BLACK
                # green outer stripe
                if name in ("right", "left") and xx in (1, 2):
                    c = GREEN if xx == 1 else GREEN_D
                if yy == 0:
                    c = GREEN
                img.putpixel((x + xx, y + yy), jitter(c, 4))
    save(img, "entity", "equipment", "humanoid_leggings", "green_lantern.png")


# --------------------------------------------------------------- constructs

def make_construct_textures():
    # Hard-light panel: glowing border, soft inner gradient, subtle scanlines.
    n = 32
    img = blank(n, n)
    for y in range(n):
        for x in range(n):
            edge = min(x, y, n - 1 - x, n - 1 - y)
            if edge == 0:
                c = (230, 255, 236, 255)
            elif edge == 1:
                c = (140, 255, 170, 240)
            elif edge == 2:
                c = (70, 230, 110, 200)
            else:
                t = min(1.0, (edge - 3) / 10)
                base = lerp((50, 210, 90, 170), (30, 160, 60, 120), t)
                if y % 4 == 0:
                    base = lerp(base, (90, 240, 130, 170), 0.5)
                c = base
            img.putpixel((x, y), c)
    save(img, "entity", "construct", "hard_light.png")

    # Denser variant for small constructs (minigun) so they stay readable against a bright sky.
    img = blank(n, n)
    for y in range(n):
        for x in range(n):
            edge = min(x, y, n - 1 - x, n - 1 - y)
            if edge <= 1:
                c = (225, 255, 232, 255)
            elif edge <= 3:
                c = (90, 240, 125, 250)
            else:
                t = min(1.0, (edge - 4) / 10)
                c = lerp((30, 170, 60, 240), (14, 110, 36, 230), t)
                if y % 4 == 0:
                    c = lerp(c, (70, 220, 105, 240), 0.5)
            img.putpixel((x, y), c)
    save(img, "entity", "construct", "hard_light_solid.png")

    # Solid core (for projectiles / hammer core), radial glow
    img = blank(16, 16)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5) / 7.5
            if d > 1:
                continue
            c = lerp((250, 255, 250, 255), (40, 220, 90, 60), d ** 0.8)
            img.putpixel((x, y), c)
    save(img, "entity", "construct", "energy_core.png")

    # Bubble: hexagon-ish lattice with transparency
    n = 64
    img = blank(n, n)
    for y in range(n):
        for x in range(n):
            # hexagonal grid lines
            hx = x / 8.0
            hy = y / (8.0 * 0.866)
            row = int(hy)
            ox = 0.5 if row % 2 else 0.0
            cx = (hx + ox) % 1.0
            cy = hy % 1.0
            line = min(cx, 1 - cx, cy, 1 - cy)
            if line < 0.08:
                c = (170, 255, 190, 190)
            elif line < 0.16:
                c = (80, 230, 120, 110)
            else:
                c = (40, 200, 80, 55)
            img.putpixel((x, y), c)
    save(img, "entity", "construct", "bubble.png")


def make_saw_blade():
    """64x64 hard-light circular saw blade with teeth, spokes and a hub (alpha cut-out)."""
    n = 64
    img = blank(n, n)
    c0 = (n - 1) / 2
    teeth = 16
    for y in range(n):
        for x in range(n):
            dx, dy = x - c0, y - c0
            r = math.hypot(dx, dy)
            a = math.atan2(dy, dx) % math.tau
            seg = (a / math.tau * teeth) % 1.0
            # Saw-tooth profile: a steep face and a sloped back.
            tooth_r = 26 + 5.5 * (1.0 - seg)
            if r > tooth_r:
                continue
            if r > 25:
                c = (220, 255, 228, 255) if r > tooth_r - 1.3 else (120, 255, 150, 245)
            elif r > 23:
                c = (230, 255, 236, 255)
            elif r > 9:
                spoke = min((a / math.tau * 6) % 1.0, 1 - (a / math.tau * 6) % 1.0)
                if spoke < 0.06:
                    c = (200, 255, 214, 235)
                elif abs(r - 16) < 0.8:
                    c = (150, 255, 175, 200)
                else:
                    t = (r - 9) / 14
                    c = lerp((60, 225, 100, 150), (40, 190, 80, 110), t)
            elif r > 7:
                c = (235, 255, 240, 255)
            else:
                c = lerp((255, 255, 255, 255), (90, 240, 130, 230), r / 7)
            img.putpixel((x, y), c)
    save(img, "entity", "construct", "saw_blade.png")


def make_shockwave():
    n = 32
    img = blank(n, n)
    for y in range(n):
        for x in range(n):
            r = math.hypot(x - 15.5, y - 15.5) / 15.5
            if r > 1:
                continue
            k = math.exp(-((r - 0.86) / 0.07) ** 2)
            inner = 0.25 * math.exp(-((r - 0.6) / 0.25) ** 2)
            a = min(1.0, k + inner)
            if a < 0.02:
                continue
            c = lerp((60, 230, 100, 0), (235, 255, 240, 255), a)
            img.putpixel((x, y), c)
    save(img, "particle", "lantern_shockwave.png")


def make_flight_textures():
    """Aura (energy swirl, scrolled by the game) and the trail ribbon gradient."""
    # Aura: diagonal swirling streaks on black (the swirl render type is additive).
    w, h = 64, 32
    img = Image.new("RGBA", (w, h), (0, 0, 0, 255))
    for y in range(h):
        for x in range(w):
            u, v = x / w, y / h
            a = math.sin((u * 3 + v * 2) * math.tau + 1.7 * math.sin(v * math.tau * 2))
            b = math.sin((u * 5 - v * 3) * math.tau + 0.8)
            k = max(0.0, a) ** 3 * 0.85 + max(0.0, b) ** 6 * 0.6
            k = min(1.0, k)
            img.putpixel((x, y), (int(60 * k), int(255 * k), int(100 * k), 255))
    save(img, "entity", "aura.png")

    # Trail: bright core across V fading to transparent edges, constant along U.
    img = blank(16, 32)
    for y in range(32):
        v = (y + 0.5) / 32
        d = abs(v - 0.5) / 0.5
        alpha = math.exp(-(d / 0.55) ** 2)
        core = math.exp(-(d / 0.18) ** 2)
        c = lerp((50, 230, 95), (235, 255, 240), core)
        for x in range(16):
            img.putpixel((x, y), (c[0], c[1], c[2], int(255 * alpha)))
    save(img, "entity", "trail.png")


# --------------------------------------------------------------- particles

def make_particles():
    # 4-frame sparkle animation
    for i in range(4):
        img = blank(8, 8)
        r = [3.6, 3.0, 2.2, 1.4][i]
        for y in range(8):
            for x in range(8):
                dx, dy = x - 3.5, y - 3.5
                d = math.hypot(dx, dy)
                cross = min(abs(dx), abs(dy)) < 0.6 and d < r + 0.6
                if d < r * 0.45:
                    img.putpixel((x, y), (255, 255, 255, 255))
                elif cross:
                    img.putpixel((x, y), (160, 255, 185, 230))
                elif d < r * 0.8:
                    img.putpixel((x, y), (70, 230, 110, 150))
        save(img, "particle", f"lantern_spark_{i}.png")
    # soft glow orb frames
    for i in range(3):
        img = blank(8, 8)
        r = [3.8, 3.0, 2.0][i]
        for y in range(8):
            for x in range(8):
                d = math.hypot(x - 3.5, y - 3.5) / r
                if d <= 1:
                    img.putpixel((x, y), lerp((220, 255, 230, 255), (40, 210, 80, 0), d))
        save(img, "particle", f"lantern_glow_{i}.png")


# --------------------------------------------------------------- GUI

def make_gui():
    # Energy bar: 102x12 frame + 100x8 fill strips
    img = blank(128, 32)
    d = ImageDraw.Draw(img)
    # frame (0,0)-(102,12)
    d.rectangle([0, 0, 101, 11], fill=(8, 30, 12, 230))
    d.rectangle([0, 0, 101, 11], outline=(20, 90, 40, 255))
    d.rectangle([1, 1, 100, 10], outline=(10, 50, 20, 255))
    # fill (0,16)-(100,24): gradient with highlight
    for x in range(100):
        for y in range(8):
            t = y / 7
            c = lerp((190, 255, 205, 255), (20, 150, 50, 255), t)
            if y == 0:
                c = (235, 255, 240, 255)
            if x % 10 == 9:
                c = lerp(c, (10, 60, 20, 255), 0.4)
            img.putpixel((x, 16 + y), c)
    # low-energy fill (0,24)-(100,32)
    for x in range(100):
        for y in range(8):
            t = y / 7
            c = lerp((255, 220, 200, 255), (170, 40, 30, 255), t)
            if x % 10 == 9:
                c = lerp(c, (60, 10, 10, 255), 0.4)
            img.putpixel((x, 24 + y), c)
    # emblem 16x16 at (104,0)
    emblem = [
        "................",
        "..KKKKKKKKKKKK..",
        "..KWWWWWWWWWWK..",
        "..KKKKKKKKKKKK..",
        ".....KKKKKK.....",
        "....KWWWWWWK....",
        "...KWLKKKKLWK...",
        "...KWK....KWK...",
        "...KWK....KWK...",
        "...KWLKKKKLWK...",
        "....KWWWWWWK....",
        ".....KKKKKK.....",
        "..KKKKKKKKKKKK..",
        "..KWWWWWWWWWWK..",
        "..KKKKKKKKKKKK..",
        "................",
    ]
    e = from_map(emblem)
    img.paste(e, (104, 0), e)
    save(img, "gui", "energy_bar.png")

    # Construct icons: 16x16 icons in a strip (112x16) + selection frame (16x16 at 80)
    icons = {
        "blast": [
            "................",
            "................",
            "..........LW....",
            ".........LWHW...",
            "......GLLWHHWL..",
            "...gGGLWWHHHWL..",
            ".gGLLWWHHHHHWL..",
            "...gGGLWWHHHWL..",
            "......GLLWHHWL..",
            ".........LWHW...",
            "..........LW....",
            "................",
            "................",
            "................",
            "................",
            "................",
        ],
        "gun": [
            "................",
            "................",
            "................",
            "...KKKKKKKKKKK..",
            "..KLLLLLLLLLLWW.",
            "..KGGGGGGGGGGLL.",
            "..KgGGLLLLGGGK..",
            "..KgGKKKKKKKKK..",
            "..KgGK.KGK......",
            "..KgGK..KK......",
            "..KgGK..........",
            "..KDgK..........",
            "..KKKK..........",
            "................",
            "................",
            "................",
        ],
        "bubble": [
            "................",
            ".....LLLLLL.....",
            "...LLW....WLL...",
            "..LW........GL..",
            "..W..........G..",
            ".L............G.",
            ".L............G.",
            ".L.....WW.....g.",
            ".L.....WW.....g.",
            ".G............g.",
            ".G............g.",
            "..G..........g..",
            "..Gg........gg..",
            "...ggg....ggg...",
            ".....gggggg.....",
            "................",
        ],
        "saw": [
            "................",
            ".......L........",
            "...L..LWL..L....",
            "...LLLWWWLLL....",
            "....LWGGGWL.....",
            "..LLWG...GWLL...",
            ".LWWG.....GWWL..",
            "..LWG..W..GWL...",
            ".LWWG.....GWWL..",
            "..LLWG...GWLL...",
            "....LWGGGWL.....",
            "...LLLWWWLLL....",
            "...L..LWL..L....",
            ".......L........",
            "................",
            "................",
        ],
        "hammer": [
            "................",
            "..KKKKKKKKKK....",
            "..KWWWWWWWWK....",
            "..KLLLLLLLLK....",
            "..KGGGGGGGGK....",
            "..KgggggggKK....",
            "..KKKKLGKKK.....",
            "......LGK.......",
            "......LGK.......",
            "......LGK.......",
            "......LGK.......",
            "......LGK.......",
            ".....KWLGK......",
            ".....KGGgK......",
            "......KKK.......",
            "................",
        ],
        "drill": [
            "................",
            "................",
            "......KKK.......",
            ".....KLWLKK.....",
            "..KKKLGLWLLKK...",
            ".KgGKGLGLWLWLK..",
            ".KgGKLGLGLWLWHK.",
            ".KgGKGLGLWLWLK..",
            "..KKKLGLWLLKK...",
            ".....KLWLKK.....",
            "......KKK.......",
            "................",
            "................",
            "................",
            "................",
            "................",
        ],
    }
    # Icons are at 16 * iconIndex; slot 5 (x=80) holds the selection frame, so the drill uses slot 6.
    strip = blank(112, 16)
    for i, key in enumerate(["blast", "gun", "bubble", "saw", "hammer", None, "drill"]):
        if key is None:
            continue
        ic = from_map(icons[key])
        strip.paste(ic, (i * 16, 0), ic)
    # selection frame at x=80
    d = ImageDraw.Draw(strip)
    d.rectangle([80, 0, 95, 15], outline=(200, 255, 215, 255))
    d.rectangle([81, 1, 94, 14], outline=(40, 180, 70, 255))
    save(strip, "gui", "constructs.png")


# --------------------------------------------------------------- mod logo

def make_logo():
    s = 128
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    glow = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(glow)
    d.ellipse([34, 34, 94, 94], outline=(60, 255, 110, 255), width=12)
    d.rectangle([18, 18, 110, 30], fill=(60, 255, 110, 255))
    d.rectangle([18, 98, 110, 110], fill=(60, 255, 110, 255))
    blur = glow.filter(ImageFilter.GaussianBlur(6))
    img = Image.alpha_composite(img, blur)
    img = Image.alpha_composite(img, blur)
    d2 = ImageDraw.Draw(img)
    d2.ellipse([38, 38, 90, 90], outline=(210, 255, 220, 255), width=6)
    d2.rectangle([22, 21, 106, 27], fill=(210, 255, 220, 255))
    d2.rectangle([22, 101, 106, 107], fill=(210, 255, 220, 255))
    img.save(os.path.join(ROOT, "greenlantern_logo.png"))
    print("wrote logo")


if __name__ == "__main__":
    make_items()
    make_lantern_block()
    make_armor()
    make_construct_textures()
    make_saw_blade()
    make_particles()
    make_shockwave()
    make_flight_textures()
    make_gui()
    make_logo()
