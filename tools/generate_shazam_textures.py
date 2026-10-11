#!/usr/bin/env python3
"""
Textures of Shazam: the Wizard's Bolt, the suit pieces, the suit on the body and the power wheel
icons. Original pixel art on tools/herolib: red with the golden lightning bolt on the chest, a golden
sash, golden cuffs and boots, short black hair (the white cape is drawn in 3D by ShazamLayer).

Usage:  pip install pillow
        python3 tools/generate_shazam_textures.py
"""
from herolib.pixel import ARM, BODY, HEAD, LEG, Icon, Palette, blank, fill_box, fill_rect, from_map, mirror, paint_face, power_sheet, save

# x: black hair, X: hair highlight, c: cloud grey, C: cloud dark, e: eye white, E: eye dark
pal = Palette(main="#C81E1E", accent="#F2B705", glow="#FFF6B0", seed=1,
              extra={"x": "#1A1414", "X": "#3A2E2C", "c": "#9AA0AC", "C": "#5E6470", "e": "#F4F2EC", "E": "#2A2020"})

ITEM = [  # the Wizard's Bolt: a golden lightning bolt with a glowing edge
    "................",
    ".........OOOOO..",
    "........OhGGGO..",
    ".......OhGGGO...",
    "......OhGGGO....",
    ".....OhGGGO.....",
    "....OhGGGGOOOO..",
    "...OhGGGGGGGGO..",
    "..OOOOOGGGGGO...",
    "......OGGGGO....",
    ".....OGGGAO.....",
    "....OGGAAO......",
    "...OGAAAO.......",
    "..OGAaO.........",
    "..OaaO..........",
    "..OOO...........",
]

HAIR_ITEM = [
    "................",
    "....OOOOOOOO....",
    "...OxxXxxxXxO...",
    "..OxXxxxxxxxxO..",
    "..OxxxxxXxxxxO..",
    "..OxxOOOOOOxxO..",
    "..OxO......OxO..",
    "..OxO......OxO..",
    "..OOO......OOO..",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
]

SUIT_ITEM = [
    "................",
    "...OOO....OOO...",
    "..OMMMOOOOMMMO..",
    ".OMMMMMMMMGhMMO.",
    ".OMMMOMMMGhMOMO.",
    ".OMMOOMMGGGGOMO.",
    ".OAAOOMMMGGMOAO.",
    ".OAAOOMMGGMMOAO.",
    "..OO.OMMGMMMO.O.",
    ".....OAAAAAAO...",
    ".....OMMMMMMO...",
    ".....OOOOOOOO...",
    "................",
    "................",
    "................",
    "................",
]

LEGGINGS_ITEM = [
    "................",
    "....OOOOOOOO....",
    "....OAAAbAAO....",
    "....OaAAAAaO....",
    "....OMMMMMMO....",
    "....OMMMOMMO....",
    "....OMMO.OMMO...",
    "...OMMO..OMMO...",
    "...OMMO..OMMO...",
    "...OMMO..OMMO...",
    "...OMnO..OMnO...",
    "...OOOO..OOOO...",
    "................",
    "................",
    "................",
    "................",
]

BOOTS_ITEM = [
    "................",
    "................",
    "................",
    "................",
    "...OOOO..OOOO...",
    "...ObAO..ObAO...",
    "...OAAO..OAAO...",
    "...OAAO..OAAO...",
    "...OAAO..OAAO...",
    "...OAAOO.OAAOO..",
    "...OAAAAO.OAAAAO",
    "...OaaaaO.OaaaaO",
    "...OOOOOO.OOOOOO",
    "................",
    "................",
    "................",
]

CHEST_FRONT = [  # 8x12: the golden lightning bolt and the sash
    "MMMMMMMM",
    "MMMMMGhM",
    "MMMMGhMM",
    "MMMGGGGM",
    "MMMMMGMM",
    "MMMMGMMM",
    "MMMGMMMM",
    "MMGMMMMM",
    "MMMMMMMM",
    "AAAAAAAA",
    "aAAAAAAa",
    "MMMMMMMM",
]

CHEST_BACK = [
    "mMMMMMMm",
    "MMMMMMMM",
    "MMMMMMMM",
    "MMMMMMMM",
    "MMMMMMMM",
    "MMMMMMMM",
    "MMMMMMMM",
    "MMMMMMMM",
    "MMMMMMMM",
    "AAAAAAAA",
    "aAAAAAAa",
    "MMMMMMMM",
]

SIDE = ["MMMM"] * 9 + ["AAAA", "aAAa", "MMMM"]

ARM_FACE = ["MMMM"] * 7 + ["mMMm", "OOOO", "AbAA", "AAAA", "aAAa"]  # sleeve, then the golden cuff

BOOT_FACE = ["...."] * 5 + ["OOOO", "AbAA", "AAAA", "AAAA", "AAAA", "aAAa", "aaaa"]

HAIR_TOP = ["xXxxxxXx", "xxxXxxxx", "xXxxxXxx", "xxxxxxxx", "xxXxxxXx", "xxxxxxxx", "xXxxxxxx", "xxxxXxxx"]
HAIR_FRONT = ["xxXxxxxx", "xxxxxXxx", "x......x", "........", "........", "........", "........", "........"]
HAIR_SIDE = ["xxxxxxxx", "xXxxxxXx", "xxxxxx..", "xxxxx...", "xxx.....", "........", "........", "........"]
HAIR_BACK = ["xxXxxxxx", "xxxxxXxx", "xXxxxxxx", "xxxxxxXx", "xxxxxxxx", "xxx..xxx", "........", "........"]


def make_items():
    save(from_map(ITEM, pal), "item", "wizard_bolt.png")
    save(from_map(HAIR_ITEM, pal), "item", "shazam_mask.png")
    save(from_map(SUIT_ITEM, pal), "item", "shazam_suit.png")
    save(from_map(LEGGINGS_ITEM, pal), "item", "shazam_leggings.png")
    save(from_map(BOOTS_ITEM, pal), "item", "shazam_boots.png")


def make_armor():
    # Suit layer (64x32): the hair on the helmet, the red top with the bolt, the sleeves with golden cuffs and the golden boots.
    img = blank(64, 32)
    paint_face(img, HEAD, "top", HAIR_TOP, pal, 0)
    paint_face(img, HEAD, "front", HAIR_FRONT, pal, 0)
    paint_face(img, HEAD, "back", HAIR_BACK, pal, 0)
    paint_face(img, HEAD, "right", HAIR_SIDE, pal, 0)
    paint_face(img, HEAD, "left", mirror(HAIR_SIDE), pal, 0)
    fill_box(img, BODY, "M", pal)
    paint_face(img, BODY, "front", CHEST_FRONT, pal)
    paint_face(img, BODY, "back", CHEST_BACK, pal)
    paint_face(img, BODY, "right", SIDE, pal)
    paint_face(img, BODY, "left", SIDE, pal)
    fill_box(img, ARM, "M", pal)
    for face in ("front", "back", "right", "left"):
        paint_face(img, ARM, face, ARM_FACE, pal)
    x, y, w, h = ARM["bottom"]
    fill_rect(img, x, y, w, h, "a", pal)
    for face in ("front", "back", "right", "left"):
        paint_face(img, LEG, face, BOOT_FACE, pal)
    x, y, w, h = LEG["bottom"]
    fill_rect(img, x, y, w, h, "a", pal)
    save(img, "entity", "equipment", "humanoid", "shazam.png")

    # Leggings layer: red trousers and the golden sash at the waist.
    img = blank(64, 32)
    fill_box(img, LEG, "M", pal)
    for face in ("front", "back", "right", "left"):
        x, y, w, h = LEG[face]
        fill_rect(img, x, y, w, 1, "m", pal)
    for face in ("front", "back", "right", "left"):
        x, y, w, h = BODY[face]
        fill_rect(img, x, y + 8, w, 1, "M", pal)
        fill_rect(img, x, y + 9, w, 2, "A", pal)
        fill_rect(img, x, y + 11, w, 1, "M", pal)
    x, y, w, h = BODY["front"]
    fill_rect(img, x + 3, y + 9, 2, 2, "b", pal, 0)  # the knot of the sash
    save(img, "entity", "equipment", "humanoid_leggings", "shazam.png")


def bolt(icon, x, y, letter="G", edge="h"):
    """A small zigzag bolt with its top right corner at (x, y)."""
    icon.line(x, y, x - 3, y + 4, letter).line(x + 1, y, x - 2, y + 4, edge)
    icon.line(x - 3, y + 4, x + 1, y + 4, letter)
    icon.line(x + 1, y + 4, x - 3, y + 9, letter).line(x, y + 5, x - 3, y + 9, edge)
    return icon


def make_gui():
    zeus = Icon(pal)  # a golden bolt flying right with a crackle at the tip
    zeus.line(1, 12, 6, 7, "a").line(6, 7, 8, 10, "A").line(8, 10, 13, 4, "G").line(2, 12, 7, 8, "A")
    zeus.line(7, 8, 9, 11, "G").line(9, 11, 14, 5, "h")
    zeus.px(14, 3, "h").px(15, 5, "h").px(13, 2, "G").px(15, 2, "G")
    zeus.outline()

    thunder = Icon(pal)  # a storm cloud and lightning falling on the ground
    thunder.disc(5, 4, 3, "C").disc(9, 3.5, 3.5, "C").disc(12, 5, 2.5, "C").disc(8, 5, 3, "c")
    bolt(thunder, 9, 6)
    thunder.line(1, 15, 14, 15, "a").px(5, 14, "h").px(6, 14, "G")
    thunder.outline()

    mercury = Icon(pal)  # a red and gold blur with a winged trail
    for y, (x0, letter) in enumerate([(3, "M"), (1, "A"), (4, "n"), (0, "G"), (2, "M"), (5, "A")]):
        mercury.line(x0, 4 + y * 2, 11, 4 + y * 2, letter)
    mercury.disc(12.5, 9, 2.5, "G").disc(12.5, 9, 1.2, "h")
    mercury.outline()

    hercules = Icon(pal)  # a fist hitting the ground and the shockwave
    hercules.map(["...OOOO...",
                  "..OMMMMO..",
                  ".OMnMnMMO.",
                  ".OMMMMMMO.",
                  ".OMMMMMMO.",
                  "..OAAAAO..",
                  "..OaAAaO.."], 3, 1)
    hercules.line(0, 12, 15, 12, "a").line(1, 11, 4, 9, "G").line(14, 11, 11, 9, "G")
    hercules.line(0, 14, 5, 13, "A").line(15, 14, 10, 13, "A").px(7, 13, "h").px(8, 13, "h")

    achilles = Icon(pal)  # a battle cry: rings of sound spreading in a cone
    achilles.disc(3, 8, 2.5, "M").disc(3, 8, 1.2, "n")
    for r, letter in ((5, "G"), (8, "A"), (11, "h")):
        achilles.ring(3, 8, r, letter, 1.0)
    for y in range(16):
        for x in range(16):
            if x < 5 or abs(y - 8) > (x - 3) * 0.8:
                if (x, y) not in [(3, 8)] and (x - 3) ** 2 + (y - 8) ** 2 > 7:
                    achilles.image.putpixel((x, y), (0, 0, 0, 0))
    achilles.outline()

    atlas = Icon(pal)  # a mountain on the shoulders of a strong man
    for y in range(4, 14):
        half = (y - 4) * 0.75
        atlas.line(round(8 - half), y, round(7 + half), y, "a" if y > 9 else "A")
    atlas.line(8, 4, 8, 6, "h").px(7, 5, "b").px(9, 5, "b")
    atlas.line(1, 14, 14, 14, "M").line(2, 15, 13, 15, "m")
    atlas.outline()

    solomon = Icon(pal)  # the eye of wisdom with golden rays
    solomon.map(["....OOOOOO....",
                 "..OOeeeeeeOO..",
                 ".OeeeGGGGeeeO.",
                 "OeeeGGEEGGeeeO",
                 "OeeeGGEEGGeeeO",
                 ".OeeeGGGGeeeO.",
                 "..OOeeeeeeOO..",
                 "....OOOOOO...."], 1, 4)
    for x0, y0, x1, y1 in ((8, 0, 8, 2), (2, 1, 4, 3), (13, 1, 11, 3), (8, 13, 8, 15), (2, 14, 4, 12), (13, 14, 11, 12)):
        solomon.line(x0, y0, x1, y1, "A")

    emblem = Icon(pal).disc(7.5, 7.5, 7.5, "w").disc(7.5, 7.5, 6.5, "M").disc(7.5, 7.5, 5.5, "n").disc(7.5, 7.5, 5, "M")
    bolt(emblem, 9, 3)
    power_sheet("shazam", [zeus, thunder, mercury, hercules, achilles, atlas, solomon], emblem)


if __name__ == "__main__":
    make_items()
    make_armor()
    make_gui()
