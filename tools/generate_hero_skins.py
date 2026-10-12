#!/usr/bin/env python3
"""
Skins of the first 9 heroes (stage 3, lot 2 of the master guide): the suit stops being drawn over
the player's skin and becomes the hero's skin, like the characters of the engine. One 64x64 image in
the player layout per skin (the Male and Female skeletons: thin 3-pixel arms for the Female ones),
with the volume of the suit on the outer layer (hair, cowl, belt, shoulder pads) and a second image
ending in _brilho with what glows in the dark (eyes, rings, symbols, lights).

The main skins follow the colours and the design of the suits of today, redrawn with more detail;
the 23 alternative skins are the ones of the catalog (stage 13). Original pixel art, drawn by code.

Writes src/main/resources/assets/greenlantern/textures/entity/skin/<id>.png and <id>_brilho.png, and
with --preview a contact sheet (front and back of every skin) at the path given.

Usage:  pip install pillow
        python tools/generate_hero_skins.py [--preview sheet.png]
"""
import argparse
import os
import random

from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "greenlantern", "textures", "entity", "skin")

# ---------------------------------------------------------------------------------------------- layout

# The modern 64x64 player layout: (u, v, w, h, d) of each part and of its outer layer.
PARTS = {
    "head": ((0, 0, 8, 8, 8), (32, 0, 8, 8, 8)),
    "body": ((16, 16, 8, 12, 4), (16, 32, 8, 12, 4)),
    "right_arm": ((40, 16, 4, 12, 4), (40, 32, 4, 12, 4)),
    "left_arm": ((32, 48, 4, 12, 4), (48, 48, 4, 12, 4)),
    "right_leg": ((0, 16, 4, 12, 4), (0, 32, 4, 12, 4)),
    "left_leg": ((16, 48, 4, 12, 4), (0, 48, 4, 12, 4)),
}
LIMBS = ("right_arm", "left_arm", "right_leg", "left_leg")
# How bright each face is: light from above and the front.
LIGHT = {"top": 1.10, "front": 1.0, "right": 0.88, "left": 0.88, "back": 0.80, "bottom": 0.66}


def faces(u, v, w, h, d):
    return {
        "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h), "back": (u + d + w + d, v + d, w, h),
    }


def rgb(value):
    value = int(value.lstrip("#"), 16)
    return ((value >> 16) & 255, (value >> 8) & 255, value & 255)


def shade(c, f):
    if f <= 1:
        return tuple(int(x * f) for x in c)
    t = min(1.0, f - 1)
    return tuple(int(x + (255 - x) * t) for x in c)


def mix(a, b, t):
    return tuple(int(round(x + (y - x) * t)) for x, y in zip(a, b))


SKIN_TONES = {"claro": "#E8B896", "medio": "#C68E66", "escuro": "#7A4E34"}

# ---------------------------------------------------------------------------------------------- canvas


class Skin:
    """One skin being drawn: the base layer, the outer layer and the glow, with colours by name."""

    def __init__(self, sid, colors, slim=False, seed=1):
        self.id = sid
        self.slim = slim
        self.colors = {k: rgb(v) for k, v in colors.items()}
        self.base = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        self.glow = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        self.random = random.Random(seed)
        # Matte colours get a little noise (cloth, scales); shiny ones stay smooth.
        self.smooth = set()

    def box(self, part, outer=False):
        u, v, w, h, d = PARTS[part][1 if outer else 0]
        if self.slim and part in ("right_arm", "left_arm"):
            w = 3
        return faces(u, v, w, h, d)

    def color(self, name, face, y, h, noise=True):
        c = self.colors[name]
        f = LIGHT[face]
        if face not in ("top", "bottom") and h > 1:
            f *= 1.05 - 0.10 * y / (h - 1)
        c = shade(c, f)
        if noise and name not in self.smooth:
            d = self.random.randint(-6, 6)
            c = tuple(max(0, min(255, x + d)) for x in c)
        return c

    def put(self, layer, x, y, c):
        img = self.glow if layer == "glow" else self.base
        if c is None:
            img.putpixel((x, y), (0, 0, 0, 0))
        else:
            img.putpixel((x, y), tuple(c) + (255,) if len(c) == 3 else c)

    def fill(self, part, fn, outer=False, only=None):
        """Paints every face of a part: fn(face, x, y, w, h) gives a colour name, or None to skip."""
        for face, (x0, y0, w, h) in self.box(part, outer).items():
            if only and face not in only:
                continue
            for y in range(h):
                for x in range(w):
                    name = fn(face, x, y, w, h)
                    if name is None:
                        continue
                    if name == "-":
                        self.put("base", x0 + x, y0 + y, None)
                    else:
                        self.put("base", x0 + x, y0 + y, self.color(name, face, y, h))

    def stamp(self, part, face, rows, key, outer=False, x0=0, y0=0, layer="base"):
        """Paints a small map on one face: key maps each letter to a colour name ('.' leaves it)."""
        fx, fy, w, h = self.box(part, outer)[face]
        for y, row in enumerate(rows):
            for x, ch in enumerate(row):
                if ch == "." or ch == " ":
                    continue
                name = key[ch]
                px, py = fx + x0 + x, fy + y0 + y
                if layer == "glow":
                    self.put("glow", px, py, self.colors[name] if name else None)
                elif name == "-":
                    self.put("base", px, py, None)
                else:
                    self.put("base", px, py, self.color(name, face, y0 + y, h))

    def glow_px(self, part, face, x, y, name, outer=False):
        fx, fy, w, h = self.box(part, outer)[face]
        self.put("glow", fx + x, fy + y, self.colors[name])

    def save(self, folder):
        os.makedirs(folder, exist_ok=True)
        self.base.save(os.path.join(folder, f"{self.id}.png"))
        self.glow.save(os.path.join(folder, f"{self.id}_brilho.png"))
        print("wrote", os.path.relpath(os.path.join(folder, f"{self.id}.png"), ROOT), "(+ _brilho)")

# ---------------------------------------------------------------------------------------------- pieces
#
# Colour names used by the pieces: k skin, K skin shadow, H hair, h hair light, e eye white, i iris,
# m mouth, and the suit colours each hero sets (main, dark, accent, trim...).


def head_skin(s, hair_top=True):
    """A plain head: skin all around, hair on the top and the back."""
    def fn(face, x, y, w, h):
        if face == "top":
            return "H" if hair_top else "k"
        if face == "back":
            return "H" if y < 6 else "K"
        if face in ("right", "left"):
            return "H" if y < 2 or (y < 4 and (x < 3 if face == "left" else x > 4)) else "k"
        if face == "bottom":
            return "K"
        return "k"
    s.fill("head", fn)


# Faces, 8x8 maps. W white of the eye, I iris, B brow, N nose shadow, M mouth, k skin, H hair.
FACE_MALE = [
    "HHHHHHHH",
    "HkkkkkkH",
    "kBBkkBBk",
    "kWIkkIWk",
    "kkkNNkkk",
    "kkkkkkkk",
    "kkMMMMkk",
    "kkkkkkkk",
]
FACE_FEMALE = [
    "HHHHHHHH",
    "HHkkkkHH",
    "HBBkkBBH",
    "HWIkkIWH",
    "kkkNNkkk",
    "kkkkkkkk",
    "kkkMMkkk",
    "kkkkkkkk",
]
FACE_KEY = {"H": "H", "k": "k", "B": "B", "W": "e", "I": "i", "N": "K", "M": "m"}


def face(s, rows, extra=None):
    key = dict(FACE_KEY)
    key.update(extra or {})
    s.stamp("head", "front", rows, key)


def hair_outer(s, length=0, top_rows=2, fringe=None):
    """Hair on the outer layer of the head: the top, a band at the sides and the back, longer if asked."""
    def fn(face, x, y, w, h):
        if face == "top":
            return "H"
        if face == "back":
            return "H" if y < top_rows + 1 + length else None
        if face in ("right", "left"):
            back = x < 3 if face == "left" else x > 4
            if y < top_rows:
                return "H"
            if length and back and y < top_rows + length:
                return "H"
            return None
        if face == "front":
            if fringe and y < len(fringe) and fringe[y][x] == "H":
                return "H"
            return None
        return None
    s.fill("head", fn, outer=True)


def long_hair_body(s, rows=5):
    """Long hair falling on the back of the body (outer layer)."""
    def fn(face, x, y, w, h):
        if face == "back" and y < rows and not (y == rows - 1 and x in (0, w - 1)):
            return "h" if (x + y) % 3 == 0 else "H"
        return None
    s.fill("body", fn, outer=True, only=("back",))


def cowl(s, color, eyes="e", open_rows=4, mouth=True, skin="k"):
    """A cowl that covers the head down to the nose, with lenses; the jaw shows below."""
    def fn(face, x, y, w, h):
        if face == "front":
            return color if y < open_rows else skin
        if face == "bottom":
            return color
        return color
    s.fill("head", fn)
    rows = ["........", "........", "........", ".ee..ee.", "........", "........", "..MMMM.." if mouth else "........", "........"]
    s.stamp("head", "front", rows, {"e": eyes, "M": "m"})


def plain_limbs(s, arm, leg, glove=None, glove_rows=4, boot=None, boot_rows=4, cuff=None, sleeve=None, sleeve_rows=0):
    """Arms and legs in the suit colours: sleeves (or bare arms), gloves, boots and a cuff line."""
    for part in ("right_arm", "left_arm"):
        def fa(face, x, y, w, h, part=part):
            if face == "bottom":
                return glove or arm
            if face == "top":
                return sleeve or arm
            if glove and y >= h - glove_rows:
                return cuff if (cuff and y == h - glove_rows) else glove
            if sleeve and y < sleeve_rows:
                return sleeve
            return arm
        s.fill(part, fa)
    for part in ("right_leg", "left_leg"):
        def fl(face, x, y, w, h, part=part):
            if face == "bottom":
                return boot or leg
            if face == "top":
                return leg
            if boot and y >= h - boot_rows:
                return boot
            return leg
        s.fill(part, fl)


def body_fill(s, fn):
    s.fill("body", fn)


def belt(s, color, row=8, buckle=None, height=1):
    def fn(face, x, y, w, h):
        if face in ("front", "back", "right", "left") and row <= y < row + height:
            return color
        return None
    s.fill("body", fn)
    if buckle:
        s.stamp("body", "front", ["BB"] * height, {"B": buckle}, x0=3, y0=row)


def outer_belt(s, color, row=8, buckle=None):
    """A belt on the outer layer: it stands out of the body like the real thing."""
    def fn(face, x, y, w, h):
        if face in ("front", "back", "right", "left") and y == row:
            return color
        return None
    s.fill("body", fn, outer=True)
    if buckle:
        s.stamp("body", "front", ["BB"], {"B": buckle}, outer=True, x0=3, y0=row)


def trunks(s, color, top=9):
    """Trunks over the hips and the top of the legs."""
    def fb(face, x, y, w, h):
        if face == "bottom":
            return color
        if face in ("front", "back", "right", "left") and y >= top:
            return color
        return None
    s.fill("body", fb)
    for part in ("right_leg", "left_leg"):
        def fl(face, x, y, w, h):
            if face == "top" or (face != "bottom" and y < 2):
                return color
            return None
        s.fill(part, fl)


def symbol(s, rows, key, y0=2, outer=False, glow=None, x0=None):
    """A chest symbol (8 wide or less, centred), on the base or outer layer, maybe glowing."""
    width = len(rows[0])
    x = (8 - width) // 2 if x0 is None else x0
    s.stamp("body", "front", rows, key, outer=outer, x0=x, y0=y0)
    if glow:
        s.stamp("body", "front", rows, {k: (v if k in glow else None) for k, v in key.items()}, outer=outer, x0=x, y0=y0, layer="glow")


def boots_outer(s, color, rows=3):
    for part in ("right_leg", "left_leg"):
        def fn(face, x, y, w, h):
            if face in ("front", "back", "right", "left") and y >= h - rows:
                return color
            return None
        s.fill(part, fn, outer=True)


def gauntlets_outer(s, color, rows=3, fins=None):
    for part in ("right_arm", "left_arm"):
        def fn(face, x, y, w, h, part=part):
            if face in ("front", "back", "right", "left") and h - rows - 1 <= y < h - 1:
                if fins and y == h - rows - 1:
                    outer_face = "right" if part == "right_arm" else "left"
                    return fins if face == outer_face else None
                return color
            return None
        s.fill(part, fn, outer=True)


def shoulder_pads(s, color, rows=3):
    for part in ("right_arm", "left_arm"):
        def fn(face, x, y, w, h):
            if face == "top" or (face in ("front", "back", "right", "left") and y < rows):
                return color
            return None
        s.fill(part, fn, outer=True)


def stripe_limb(s, parts, color, outer_side=True, col=None):
    """A vertical line down the outer side of limbs (the Lantern's green lines, a lightning streak)."""
    for part in parts:
        side = "right" if part.startswith("right") else "left"

        def fn(face, x, y, w, h, side=side):
            if face == side and x == (col if col is not None else w // 2):
                return color
            return None
        s.fill(part, fn)

# ---------------------------------------------------------------------------------------------- heroes
# Each function draws one skin; the variant changes colours and pieces.


def lantern(variant="lantern"):
    if variant == "lantern":
        c = {"main": "#1FA23A", "dark": "#121418", "trim": "#0E5E22", "glow": "#5CFF7A", "emb": "#F4F7F2", "glove": "#1FA23A"}
    elif variant == "lantern_lanterna_branco":
        c = {"main": "#F2F4F6", "dark": "#A9B0BA", "trim": "#7E8794", "glow": "#FFFFFF", "emb": "#E6ECF2", "glove": "#F2F4F6"}
    else:  # Parallax
        c = {"main": "#1E5A2C", "dark": "#0B1E10", "trim": "#E2C341", "glow": "#E8FF6A", "emb": "#2E8A40", "glove": "#163F20"}
    c.update({"k": SKIN_TONES["claro"], "K": "#C8987A", "H": "#5A3A22", "h": "#7A5434", "B": "#4A2E1A", "e": "#FFFFFF", "i": "#3A6A9A", "m": "#A8645A"})
    if variant == "lantern_parallax":
        c.update({"H": "#2B1A10", "h": "#E8E8E8", "e": "#E8FF6A", "i": "#E8FF6A"})
    s = Skin(variant, c, seed=11)
    s.smooth.update({"glow", "emb"})
    head_skin(s)
    face(s, FACE_MALE)
    # The domino mask across the eyes.
    s.stamp("head", "front", ["........", "........", "MMMMMMMM", "MeMMMMeM" if variant != "lantern_parallax" else "MeeMMeeM", ".MM..MM."], {"M": "main" if variant != "lantern_parallax" else "trim", "e": "glow" if variant == "lantern_parallax" else "e"})
    s.stamp("head", "front", ["........", "........", "........", ".W....W." if variant != "lantern_parallax" else ".WW..WW."], {"W": "glow"}, layer="glow")
    hair_outer(s, fringe=["HHHHHHHH", "H.HH...H"])
    if variant == "lantern_parallax":
        # The white streaks of the possessed Hal at the temples.
        s.stamp("head", "front", ["h......h", "h......h"], {"h": "h"}, outer=True)

    def body(face, x, y, w, h):
        if face == "top":
            return "main"
        if face == "bottom":
            return "dark"
        if y < 3:
            return "main"
        if face == "front":
            if y == 3 and 1 <= x <= 6:
                return "main"
            if x in (0, 7):
                return "main"
        if face in ("right", "left"):
            return "main" if x in (0, 3) else "dark"
        return "dark"
    body_fill(s, body)
    emblem = [
        "..OOOO..",
        ".OEEEEO.",
        "OEOOOOEO",
        "OEOGGOEO",
        "OEOOOOEO",
        ".OEEEEO.",
        "..OOOO..",
    ] if variant != "lantern_parallax" else [
        "..T..T..",
        ".TOOOOT.",
        "TOGGGGOT",
        "TOGEEGOT",
        "TOGGGGOT",
        ".TOOOOT.",
        "..T..T..",
    ]
    symbol(s, emblem, {"O": "dark", "E": "emb", "G": "main", "T": "trim"}, y0=1, outer=False, glow="E" if variant != "lantern" else "EG")
    plain_limbs(s, arm="dark", leg="dark", glove="glove", glove_rows=4, boot="main", boot_rows=4, sleeve="main", sleeve_rows=3)
    stripe_limb(s, LIMBS, "main")
    # The ring on the right hand, glowing.
    s.stamp("right_arm", "front", ["G"], {"G": "glow"}, x0=1, y0=10)
    s.stamp("right_arm", "front", ["G"], {"G": "glow"}, x0=1, y0=10, layer="glow")
    if variant == "lantern_parallax":
        shoulder_pads(s, "trim", rows=3)
        s.stamp("body", "front", ["T......T", "TT....TT"], {"T": "trim"}, outer=True, y0=0)
        boots_outer(s, "trim", rows=2)
    else:
        outer_belt(s, "main", row=9)
    return s


def flash(variant="flash"):
    c = {"main": "#C8102E", "dark": "#7E0A1C", "gold": "#F2C200", "goldd": "#B88A00", "emb": "#F8F4EC", "bolt": "#FFE066", "e": "#FFFFFF"}
    if variant == "flash_classico":
        c.update({"main": "#E0202E", "dark": "#981222"})
    elif variant == "flash_flash_negro":
        c.update({"main": "#16161C", "dark": "#0A0A0E", "gold": "#E8E8EE", "goldd": "#9A9AA8", "emb": "#16161C", "bolt": "#C8F0FF"})
    elif variant == "flash_ponto_de_ignicao":
        c.update({"main": "#A80E26", "dark": "#5E0816", "gold": "#C8CDD6", "goldd": "#7C8290", "bolt": "#E8F0FF"})
    c.update({"k": SKIN_TONES["claro"], "K": "#C8987A", "H": "#B88A3A", "m": "#A8645A", "i": "#4A7AB0"})
    s = Skin(variant, c, seed=22)
    s.smooth.update({"gold", "bolt", "emb"})
    # The cowl: the whole head but the mouth and chin.
    cowl(s, "main", eyes="e", open_rows=5)
    s.stamp("head", "front", ["........", "........", ".dd..dd.", "........", "...dd...", "kkkkkkkk"], {"d": "dark", "k": "k"})
    s.stamp("head", "front", ["........", "........", "........", ".ee..ee."], {"e": "bolt"}, layer="glow")
    # The lightning ears on the outer layer: bigger wings on the classic suit.
    wing = ["..G.", ".GG.", "GGGG", ".GG.", ".G.."] if variant != "flash_classico" else ["...G", "..GG", ".GGG", "GGGG", ".GGG", "..GG"]
    for side in ("right", "left"):
        rows = wing if side == "left" else [r[::-1] for r in wing]
        s.stamp("head", side, rows, {"G": "gold"}, outer=True, x0=2, y0=1)

    def body(face, x, y, w, h):
        if face == "bottom":
            return "dark"
        return "main"
    body_fill(s, body)
    emblem = [
        "..EEEE..",
        ".EEEbEE.",
        "EEEbbEEE",
        "EEbbbbEE",
        "EEEbbEEE",
        ".EEbEEE.",
        "..EEEE..",
    ]
    symbol(s, emblem, {"E": "emb", "b": "gold"}, y0=1, glow="b")
    # The lightning belt, a V of gold.
    s.stamp("body", "front", ["G......G", ".GG..GG.", "...GG..."], {"G": "gold"}, y0=8)
    s.stamp("body", "back", ["GGGGGGGG"], {"G": "gold"}, y0=8)
    plain_limbs(s, arm="main", leg="main", glove="main", boot="gold", boot_rows=4, cuff="gold")
    stripe_limb(s, ("right_arm", "left_arm"), "goldd")
    boots_outer(s, "gold", rows=2)
    return s


def aquaman(variant="aquaman"):
    c = {"scale": "#E87A1E", "scaled": "#B4520E", "green": "#2E8A3A", "greend": "#1C5A26", "gold": "#E2B43A", "goldd": "#A87E1C"}
    c.update({"k": SKIN_TONES["medio"], "K": "#A87452", "H": "#6A4426", "h": "#8A5E36", "B": "#4A2E18", "e": "#FFFFFF", "i": "#3A8AA0", "m": "#8E5242", "glow": "#7FE8FF"})
    if variant == "aquaman_classico":
        c.update({"k": SKIN_TONES["claro"], "K": "#C8987A", "H": "#E0B850", "h": "#F2D27A", "B": "#B8902E", "i": "#3A7AD0"})
    s = Skin(variant, c, seed=33)
    s.smooth.update({"gold", "glow"})
    head_skin(s)
    rows = list(FACE_MALE)
    if variant != "aquaman_classico":
        # The beard: chin and jaw in hair, the mouth inside it.
        rows = rows[:5] + ["HkkkkkkH", "HHMMMMHH", "HHHHHHHH"]
    face(s, rows)
    if variant == "aquaman_classico":
        hair_outer(s, length=0, fringe=["HHHHHHHH", "HhH..HhH"])
    else:
        hair_outer(s, length=4, fringe=["HHHHHHHH", "H.....hH"])
        long_hair_body(s, rows=3 if variant == "aquaman" else 6)
        if variant == "aquaman_mao_de_arpao":
            s.stamp("head", "front", ["........"] * 5 + ["H......H", "HH....HH", "HHHHHHHH"], {"H": "H"}, outer=True)

    def body(face, x, y, w, h):
        if face == "top":
            return "scale"
        if face == "bottom" or y >= 9:
            return "green"
        return "scaled" if (x + (y % 2)) % 2 == 0 and y % 2 == 1 else "scale"
    body_fill(s, body)
    belt(s, "gold", row=8, buckle="goldd")
    s.stamp("body", "front", ["AAAA"], {"A": "goldd"}, x0=2, y0=8)
    s.stamp("body", "front", ["GG"], {"G": "glow"}, x0=3, y0=8, layer="glow")

    def arm(face, x, y, w, h):
        if face == "bottom" or y >= 8:
            return "green"
        if face == "top" or y < 5:
            return "scaled" if (x + (y % 2)) % 2 == 0 and y % 2 == 1 else "scale"
        return "k"
    for part in ("right_arm", "left_arm"):
        s.fill(part, arm)

    def leg(face, x, y, w, h):
        if face == "bottom":
            return "greend"
        return "greend" if (x + (y % 2)) % 2 == 0 and y % 2 == 1 else "green"
    for part in ("right_leg", "left_leg"):
        s.fill(part, leg)
    gauntlets_outer(s, "green", rows=3)
    if variant == "aquaman_mao_de_arpao":
        # The harpoon in place of the left hand.
        steel = {"S": "#B8C0CC", "D": "#6A7280"}
        s.colors.update({k: rgb(v) for k, v in steel.items()})
        s.smooth.update(steel)

        def hook(face, x, y, w, h):
            if face == "bottom":
                return "D"
            if y >= 8:
                return "S" if (y + x) % 3 else "D"
            return None
        s.fill("left_arm", hook)
        s.stamp("left_arm", "front", ["S..S", "SSSS"], {"S": "S"}, outer=True, y0=10)
    return s


def batman(variant="batman"):
    c = {"suit": "#4A4E56", "suitd": "#33363C", "black": "#16171A", "gold": "#D8A92A", "goldd": "#9C7418", "e": "#F4F6F8"}
    if variant == "batman_classico":
        c.update({"suit": "#8E949E", "suitd": "#6A707A", "black": "#2A3E78", "gold": "#F2C230", "goldd": "#B08A1A"})
    elif variant == "batman_armadura_anti_superman":
        c.update({"suit": "#5C626C", "suitd": "#3A3E46", "black": "#24272C", "gold": "#7C838E", "goldd": "#4E545E"})
    elif variant == "batman_batman_do_futuro":
        c.update({"suit": "#121216", "suitd": "#0A0A0C", "black": "#1E1E24", "gold": "#C81E28", "goldd": "#7E1018", "e": "#FFFFFF"})
    elif variant == "batman_gotham_a_luz_de_lampiao":
        c.update({"suit": "#4A3A2C", "suitd": "#32261C", "black": "#241A12", "gold": "#B08A4A", "goldd": "#7A5E2E", "e": "#E8DCA8"})
    c.update({"k": SKIN_TONES["claro"], "K": "#C8987A", "m": "#9A5A50"})
    s = Skin(variant, c, seed=44)
    s.smooth.update({"e", "gold"})
    cowl(s, "black", eyes="e", open_rows=5)
    s.stamp("head", "front", ["........", "........", ".BB..BB.", "........", "...BB...", "kkkkkkkk"], {"B": "suitd", "k": "k"})
    s.stamp("head", "front", ["........", "........", "........", ".ee..ee."], {"e": "e"}, layer="glow")

    def body(face, x, y, w, h):
        if face == "bottom":
            return "black"
        if variant == "batman_batman_do_futuro":
            return "suit"
        return "suit" if face != "back" or y > 1 else "black"
    body_fill(s, body)
    bat = [
        "B..BB..B",
        "BBBBBBBB",
        ".BBBBBB.",
        "..B..B..",
    ]
    if variant == "batman_classico":
        symbol(s, [".YYYYYY.", "YBYBBYBY", "YBBBBBBY", "YYBYYBYY", ".YYYYYY."], {"Y": "gold", "B": "black"}, y0=2)
    elif variant == "batman_batman_do_futuro":
        symbol(s, ["R..RR..R", "RRRRRRRR", ".RRRRRR.", "..R..R.."], {"R": "gold"}, y0=2, glow="R")
    elif variant == "batman_gotham_a_luz_de_lampiao":
        # A coat buttoned down the front, no symbol.
        s.stamp("body", "front", ["...gg..."] * 12, {"g": "suitd"})
        s.stamp("body", "front", ["...G....", "........", "...G....", "........", "...G...."], {"G": "gold"}, y0=2)
    else:
        symbol(s, bat, {"B": "black"}, y0=2)
    if variant != "batman_gotham_a_luz_de_lampiao":
        outer_belt(s, "gold", row=8, buckle="goldd")
        s.stamp("body", "front", ["P.P..P.P"], {"P": "goldd"}, outer=True, y0=8)
    else:
        outer_belt(s, "goldd", row=8)
    trunk = "black" if variant in ("batman_classico", "batman") else None
    if trunk:
        trunks(s, trunk, top=10)
    plain_limbs(s, arm="suit", leg="suit", glove="black", glove_rows=5, boot="black", boot_rows=4)
    gauntlets_outer(s, "black", rows=4, fins="black" if variant not in ("batman_gotham_a_luz_de_lampiao",) else None)
    boots_outer(s, "black", rows=3)
    if variant == "batman_armadura_anti_superman":
        # Heavy plates: shoulders, chest and knees stand out.
        shoulder_pads(s, "suit", rows=4)
        s.stamp("body", "front", ["SSSSSSSS", "SBBSSBBS", "SSBBBBSS", "SSSBBSSS", "SSSSSSSS"], {"S": "suit", "B": "black"}, outer=True, y0=1)
        for part in ("right_leg", "left_leg"):
            s.stamp(part, "front", ["SSSS", "SSSS"], {"S": "suit"}, outer=True, y0=4)
    return s


def superman(variant="superman"):
    c = {"blue": "#1E4FB4", "blued": "#13357E", "red": "#C8202A", "redd": "#8A1018", "gold": "#F2C230", "S": "#C8202A"}
    if variant == "superman_traje_negro":
        c.update({"blue": "#18181E", "blued": "#0C0C10", "red": "#18181E", "redd": "#0C0C10", "gold": "#C8CDD6", "S": "#D8DDE6"})
    elif variant == "superman_reino_do_amanha":
        c.update({"blue": "#2A4E8E", "blued": "#1A3462", "gold": "#16161A", "S": "#C8202A"})
    elif variant == "superman_foice_e_martelo":
        c.update({"blue": "#6E747E", "blued": "#4A4F58", "red": "#B8202A", "redd": "#7E1018", "gold": "#E2B43A", "S": "#B8202A"})
    c.update({"k": SKIN_TONES["claro"], "K": "#C8987A", "H": "#16161E", "h": "#2A2A36", "B": "#16161E", "e": "#FFFFFF", "i": "#2E6ED0", "m": "#A8645A", "glow": "#FF5A3A"})
    if variant == "superman_reino_do_amanha":
        c.update({"H": "#3A3A40", "h": "#B8BCC4"})
    s = Skin(variant, c, seed=55)
    s.smooth.update({"gold", "S"})
    head_skin(s)
    face(s, FACE_MALE)
    if variant == "superman_traje_negro":
        hair_outer(s, length=3, fringe=["HHHHHHHH", "H.H....H"])
    elif variant == "superman_reino_do_amanha":
        hair_outer(s, fringe=["HHHHHHHH", "h.....hh"])
        s.stamp("head", "right", ["h", "h"], {"h": "h"}, outer=True, x0=5, y0=2)
        s.stamp("head", "left", ["h", "h"], {"h": "h"}, outer=True, x0=2, y0=2)
    else:
        # The curl on the forehead.
        hair_outer(s, fringe=["HHHHHHHH", "H..H...H", "...H...."])

    def body(face, x, y, w, h):
        if face == "bottom":
            return "blued"
        return "blue"
    body_fill(s, body)
    if variant == "superman_foice_e_martelo":
        symbol(s, [".YYYYYY.", "YRRRRRRY", "YRYYRYRY", "YRRYYRRY", "YRYRRYRY", ".YRRRRY.", "..YYYY.."], {"Y": "gold", "R": "S"}, y0=1, outer=True)
    elif variant == "superman_reino_do_amanha":
        symbol(s, [".KKKKKK.", "KRRRRRRK", "KRKKKKRK", ".KRRRRK.", "..KRRK..", "...KK..."], {"K": "gold", "R": "S"}, y0=1, outer=True)
    else:
        symbol(s, [".YYYYYY.", "YSSSSSSY", "YSYYYYSY", ".YSSSSY.", "..YYSY..", "...SY..."], {"Y": "gold", "S": "S"}, y0=1, outer=True)
    if variant == "superman_classico":
        trunks(s, "red", top=9)
        outer_belt(s, "gold", row=8, buckle="gold")
    else:
        belt(s, "red", row=9)
        s.stamp("body", "front", ["...GG..."], {"G": "gold"}, y0=9)
    plain_limbs(s, arm="blue", leg="blue", boot="red", boot_rows=5)
    boots_outer(s, "red", rows=2)
    s.stamp("right_arm", "front", ["dd"] * 1, {"d": "blued"}, x0=1, y0=11)
    return s


def wonder_woman(variant="wonder_woman"):
    c = {"red": "#C41E2A", "redd": "#8A121A", "blue": "#1E3E9E", "blued": "#122A6E", "gold": "#E8B830", "goldd": "#A8801A", "star": "#FFFFFF", "silver": "#C8CDD6"}
    if variant == "wonder_woman_armadura_dourada":
        c.update({"red": "#E2B43A", "redd": "#A8801A", "blue": "#C89A2A", "blued": "#8A6612", "gold": "#F8DC7A", "goldd": "#B89A3A"})
    elif variant == "wonder_woman_reino_do_amanha":
        c.update({"red": "#8A1A22", "redd": "#5A0E14", "blue": "#3A3E48", "blued": "#24272E", "gold": "#C8CDD6", "goldd": "#7C8290"})
    c.update({"k": "#D8A07A", "K": "#B8805A", "H": "#121218", "h": "#24242E", "B": "#121218", "e": "#FFFFFF", "i": "#3A5AA0", "m": "#B04A4A", "tiara_star": "#FF3A3A"})
    s = Skin(variant, c, slim=True, seed=66)
    s.smooth.update({"gold", "silver", "star"})
    head_skin(s)
    face(s, FACE_FEMALE)
    hair_outer(s, length=6, fringe=["HHHHHHHH", "HH....HH", "H......H", "H......H"])
    long_hair_body(s, rows=7)
    # The tiara with the red star.
    if variant != "wonder_woman_armadura_dourada":
        s.stamp("head", "front", ["........", "GGGRGGGG"], {"G": "gold", "R": "tiara_star"}, outer=True)
        s.stamp("head", "front", ["........", "...R...."], {"R": "tiara_star"}, outer=True, layer="glow")
    else:
        # The eagle helmet.
        s.stamp("head", "front", ["GGGGGGGG", "GGGGGGGG", "G......G"], {"G": "gold"}, outer=True)
        for side in ("right", "left"):
            s.stamp("head", side, ["GGGGGGGG", "GGGGGGGG", "GGG..GGG"], {"G": "gold"}, outer=True)

    skirt = variant in ("wonder_woman", "wonder_woman_reino_do_amanha")

    def body(face, x, y, w, h):
        if face == "top":
            return "k"
        if face == "bottom":
            return "blue"
        if y < 2:
            return "k" if face != "back" else "red"
        if y < 8:
            return "red"
        if y == 8:
            return "gold"
        return "blue"
    body_fill(s, body)
    # The golden eagle (the WW) on the chest.
    symbol(s, ["G.GGGG.G", "GG.GG.GG", ".GG..GG.", "..G..G.."], {"G": "gold"}, y0=2, outer=True)
    # The stars on the blue.
    s.stamp("body", "front", ["........", "........", ".W....W.", "...W...."], {"W": "star"}, y0=8)

    def arm(face, x, y, w, h):
        if face == "bottom":
            return "k"
        if 7 <= y <= 10:
            return "silver"
        return "k"
    for part in ("right_arm", "left_arm"):
        s.fill(part, arm)

    def leg(face, x, y, w, h):
        if face == "top":
            return "blue"
        if face == "bottom":
            return "redd"
        if y < 2:
            return "blue"
        if y >= 5:
            return "star" if y == 5 else "red"
        return "k"
    for part in ("right_leg", "left_leg"):
        s.fill(part, leg)
    gauntlets_outer(s, "silver", rows=4)
    boots_outer(s, "red", rows=4)
    if skirt:
        # A war skirt over the hips on the outer layer.
        def fs(face, x, y, w, h):
            if face in ("front", "back", "right", "left") and y >= 9:
                return "blued" if x % 2 else "blue"
            return None
        s.fill("body", fs, outer=True)
        for part in ("right_leg", "left_leg"):
            def fl(face, x, y, w, h):
                if face in ("front", "back", "right", "left") and y < 3:
                    return "blued" if x % 2 else "blue"
                return None
            s.fill(part, fl, outer=True)
    if variant == "wonder_woman_armadura_dourada":
        shoulder_pads(s, "gold", rows=2)
        s.stamp("body", "back", ["GGGGGGGG", "G.GGGG.G", "..G..G.."], {"G": "gold"}, outer=True, y0=1)
    if variant == "wonder_woman_reino_do_amanha":
        # The sword on the back.
        s.stamp("body", "back", [".....S..", "....S...", "...S....", "..S.....", ".GG.....", "G..G...."], {"S": "silver", "G": "gold"}, outer=True, y0=1)
    return s


def cyborg(variant="cyborg"):
    c = {"metal": "#D4D8DE", "metald": "#9AA0AA", "joint": "#3A3E46", "jointd": "#24272C", "red": "#E0202A", "glow": "#FF3A3A"}
    if variant == "cyborg_titas_classico":
        c.update({"metal": "#C8CDD6", "metald": "#8E949E"})
    c.update({"k": SKIN_TONES["escuro"], "K": "#5A3622", "H": "#16110E", "B": "#16110E", "e": "#FFFFFF", "i": "#3A2A1E", "m": "#5A2E22"})
    s = Skin(variant, c, seed=77)
    s.smooth.update({"metal", "red", "glow"})
    classic = variant == "cyborg_titas_classico"

    # Half the head in metal (the viewer's left: Victor's right), the red eye in it.
    def head(face, x, y, w, h):
        if face == "top":
            return "metal" if classic and x < 6 else ("metal" if not classic else "H")
        if face == "front":
            if classic:
                return "metal" if (x < 3 and y < 5) or y < 1 else None
            return "metal" if x < 4 or y < 1 else None
        if face == "right":
            return "metal" if not classic or y < 3 else "k"
        if face == "back":
            return "metald" if not classic or y < 3 else "k"
        if face == "left":
            return "metald" if y < 2 else "k"
        return "metald"
    head_skin(s, hair_top=classic)
    face(s, FACE_MALE)
    s.fill("head", head)
    s.stamp("head", "front", ["........", "........", "........", ".R......"], {"R": "red"})
    s.glow_px("head", "front", 1, 3, "glow")
    s.stamp("head", "front", ["...J...." if not classic else "..J.....", "...J....", "...J...."], {"J": "joint"}, y0=1)

    def body(face, x, y, w, h):
        if face == "bottom":
            return "joint"
        if face in ("right", "left"):
            return "joint" if y % 4 == 3 else "metald"
        if y in (4, 8) and face != "top":
            return "joint"
        if face == "front" and x in (0, 7) and not classic:
            return "metald"
        return "metal"
    body_fill(s, body)
    # The red core on the chest.
    symbol(s, [".JJJJ.", "JRRRRJ", "JRrrRJ", "JRRRRJ", ".JJJJ."], {"J": "joint", "R": "red", "r": "glow"}, y0=1, glow="Rr")
    if not classic:
        # Plates and lights on the outer layer.
        shoulder_pads(s, "metal", rows=3)
        s.stamp("body", "front", ["M......M", "MM....MM"], {"M": "metald"}, outer=True, y0=0)
        for part in ("right_arm", "left_arm"):
            s.glow_px(part, "front", 1, 5, "glow")
        for part in ("right_leg", "left_leg"):
            s.glow_px(part, "front", 1, 3, "glow")

    def limb(face, x, y, w, h):
        if face == "bottom":
            return "jointd"
        if y in (4, 5) and face != "top":
            return "joint"
        if face in ("right", "left") and x == 0:
            return "metald"
        return "metal"
    for part in LIMBS:
        s.fill(part, limb)
    for part in ("right_arm", "left_arm"):
        s.stamp(part, "front", ["R"], {"R": "red"}, x0=1, y0=5)
    for part in ("right_leg", "left_leg"):
        s.stamp(part, "front", ["R"], {"R": "red"}, x0=1, y0=3)
    if not classic:
        boots_outer(s, "metald", rows=3)
        gauntlets_outer(s, "metald", rows=3)
    return s


def supergirl(variant="supergirl"):
    c = {"blue": "#2A5ACC", "blued": "#183A8A", "red": "#D02030", "redd": "#8E1420", "gold": "#F2C230", "S": "#D02030"}
    if variant == "supergirl_anos_80":
        c.update({"red": "#2A5ACC", "redd": "#183A8A", "headband": "#D02030"})
    elif variant == "supergirl_lanterna_vermelha":
        c.update({"blue": "#1A1214", "blued": "#0E0A0A", "red": "#C0101A", "redd": "#7A0A10", "gold": "#2A1A1C", "S": "#FF2A2A"})
    c.update({"k": SKIN_TONES["claro"], "K": "#C8987A", "H": "#F2D27A", "h": "#FFE6A0", "B": "#C8A04A", "e": "#FFFFFF", "i": "#3A7AD0", "m": "#C05A5A", "glow": "#FF2A2A"})
    if variant == "supergirl_lanterna_vermelha":
        c.update({"e": "#FF2A2A", "i": "#FF2A2A"})
    s = Skin(variant, c, slim=True, seed=88)
    s.smooth.update({"gold", "S", "glow"})
    head_skin(s)
    face(s, FACE_FEMALE)
    hair_outer(s, length=6, fringe=["HHHHHHHH", "Hh....hH", "H......H", "H......H"])
    long_hair_body(s, rows=6)
    if variant == "supergirl_anos_80":
        s.stamp("head", "front", ["........", "RRRRRRRR"], {"R": "headband"}, outer=True)
        for side in ("right", "left", "back"):
            s.stamp("head", side, ["........", "RRRRRRRR"], {"R": "headband"}, outer=True)
    if variant == "supergirl_lanterna_vermelha":
        s.stamp("head", "front", ["........"] * 3 + [".WW..WW."], {"W": "glow"}, layer="glow")

    def body(face, x, y, w, h):
        if face == "bottom":
            return "red"
        if y >= 9:
            return "red"
        if y == 8:
            return "gold"
        return "blue"
    body_fill(s, body)
    if variant == "supergirl_lanterna_vermelha":
        symbol(s, ["..RRRR..", ".R.RR.R.", "R.RRRR.R", "R.R..R.R", ".R....R.", "..RRRR.."], {"R": "S"}, y0=1, glow="R")
    else:
        symbol(s, [".YYYYYY.", "YSSSSSSY", "YSYYYYSY", ".YSSSSY.", "..YYSY..", "...SY..."], {"Y": "gold", "S": "S"}, y0=1, outer=True)

    def arm(face, x, y, w, h):
        if face == "bottom":
            return "k"
        return "blue" if y < 11 else "k"
    for part in ("right_arm", "left_arm"):
        s.fill(part, arm)

    def leg(face, x, y, w, h):
        if face == "top":
            return "red"
        if face == "bottom":
            return "redd" if variant != "supergirl_anos_80" else "redd"
        if y < 2:
            return "red"
        if y >= 6:
            return "redd" if variant == "supergirl_lanterna_vermelha" else ("red" if variant != "supergirl_anos_80" else "S")
        return "k" if variant != "supergirl_lanterna_vermelha" else "blue"
    for part in ("right_leg", "left_leg"):
        s.fill(part, leg)
    # The skirt on the outer layer.
    def fs(face, x, y, w, h):
        if face in ("front", "back", "right", "left") and y >= 9:
            return "redd" if x % 3 == 0 else "red"
        return None
    s.fill("body", fs, outer=True)
    boots_outer(s, "S" if variant == "supergirl_anos_80" else ("red" if variant != "supergirl_lanterna_vermelha" else "redd"), rows=3)
    return s


def shazam(variant="shazam"):
    c = {"red": "#C41E2A", "redd": "#8A121A", "gold": "#F2C230", "goldd": "#B08A1A", "bolt": "#FFE066", "white": "#F4F2EC"}
    if variant == "shazam_capitao_marvel_classico":
        c.update({"red": "#D8202C", "redd": "#98141E"})
    elif variant == "shazam_shazam_negro":
        c.update({"red": "#18181E", "redd": "#0C0C10", "gold": "#C8CDD6", "goldd": "#7C8290", "bolt": "#E8F0FF"})
    c.update({"k": SKIN_TONES["claro"], "K": "#C8987A", "H": "#16161E", "h": "#2A2A36", "B": "#16161E", "e": "#FFFFFF", "i": "#4A3A2A", "m": "#A8645A"})
    s = Skin(variant, c, seed=99)
    s.smooth.update({"gold", "bolt"})
    head_skin(s)
    face(s, FACE_MALE)
    hair_outer(s, fringe=["HHHHHHHH", "HH.H..HH"])

    def body(face, x, y, w, h):
        if face == "bottom":
            return "redd"
        return "red"
    body_fill(s, body)
    # The lightning bolt across the chest.
    symbol(s, ["....BB..", "...BB...", "..BBBBB.", "....BB..", "...BB...", "..B....."], {"B": "bolt"}, y0=1, outer=True, glow="B")
    # The sash and the belt.
    outer_belt(s, "gold", row=8, buckle="goldd")
    s.stamp("body", "front", ["GGGGGGGG"], {"G": "gold"}, y0=8)

    def arm(face, x, y, w, h):
        if face == "bottom":
            return "gold"
        if y >= 8:
            return "gold" if y > 8 else "goldd"
        return "red"
    for part in ("right_arm", "left_arm"):
        s.fill(part, arm)
    plain_limbs_legs = ("right_leg", "left_leg")
    for part in plain_limbs_legs:
        def leg(face, x, y, w, h):
            if face == "bottom":
                return "goldd"
            if y >= 7:
                return "gold" if y > 7 else "goldd"
            return "red"
        s.fill(part, leg)
    gauntlets_outer(s, "gold", rows=3)
    boots_outer(s, "gold", rows=3)
    if variant == "shazam_capitao_marvel_classico":
        # The white collar of the short cape.
        s.stamp("body", "front", ["W......W"], {"W": "white"}, outer=True)
    return s


SKINS = [
    (lantern, ["lantern", "lantern_lanterna_branco", "lantern_parallax"]),
    (flash, ["flash", "flash_classico", "flash_flash_negro", "flash_ponto_de_ignicao"]),
    (aquaman, ["aquaman", "aquaman_classico", "aquaman_mao_de_arpao"]),
    (batman, ["batman", "batman_classico", "batman_armadura_anti_superman", "batman_batman_do_futuro", "batman_gotham_a_luz_de_lampiao"]),
    (superman, ["superman", "superman_classico", "superman_traje_negro", "superman_reino_do_amanha", "superman_foice_e_martelo"]),
    (wonder_woman, ["wonder_woman", "wonder_woman_classica", "wonder_woman_armadura_dourada", "wonder_woman_reino_do_amanha"]),
    (cyborg, ["cyborg", "cyborg_titas_classico"]),
    (supergirl, ["supergirl", "supergirl_anos_80", "supergirl_lanterna_vermelha"]),
    (shazam, ["shazam", "shazam_capitao_marvel_classico", "shazam_shazam_negro"]),
]

# ---------------------------------------------------------------------------------------------- preview


def flat_view(skin, back=False, scale=6):
    """The front (or back) of a skin standing, both layers, for the contact sheet."""
    img = Image.new("RGBA", (16, 32), (0, 0, 0, 0))
    face = "back" if back else "front"
    aw = 3 if skin.slim else 4
    # (part, x, y) on a 16x32 figure seen from the front; from the back the sides swap.
    spots = [("head", 4, 0), ("body", 4, 8),
             ("right_arm", 4 - aw, 8), ("left_arm", 12, 8),
             ("right_leg", 4, 20), ("left_leg", 8, 20)]
    for part, x, y in spots:
        if back:
            if part.startswith("right"):
                x = 12 if "arm" in part else 8
            elif part.startswith("left"):
                x = 4 - aw if "arm" in part else 4
        for outer in (False, True):
            fx, fy, w, h = skin.box(part, outer)[face]
            crop = skin.base.crop((fx, fy, fx + w, fy + h))
            img.alpha_composite(crop, (x, y))
    return img.resize((16 * scale, 32 * scale), Image.NEAREST)


def preview(path, scale=6):
    skins = [(fn, sid) for fn, ids in SKINS for sid in ids]
    cols = 8
    cell_w, cell_h = 2 * 16 * scale + 24, 32 * scale + 34
    rows = (len(skins) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * cell_w, rows * cell_h), (44, 46, 52, 255))
    draw = ImageDraw.Draw(sheet)
    for i, (fn, sid) in enumerate(skins):
        skin = fn(sid)
        x, y = (i % cols) * cell_w + 8, (i // cols) * cell_h + 6
        sheet.alpha_composite(flat_view(skin, False, scale), (x, y))
        sheet.alpha_composite(flat_view(skin, True, scale), (x + 16 * scale + 8, y))
        draw.text((x, y + 32 * scale + 6), sid, fill=(230, 230, 230, 255))
    sheet.save(path)
    print("wrote", path)


def main():
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("--preview", help="also write a contact sheet of every skin here")
    args = parser.parse_args()
    for fn, ids in SKINS:
        for sid in ids:
            fn(sid).save(OUT)
    if args.preview:
        preview(args.preview)


if __name__ == "__main__":
    main()
