#!/usr/bin/env python3
"""
Textures of the character engine (version 2.0.0): the ability class icons shared by every character
until it has icons of its own (textures/gui/engine/abilities.png: the 10 classes in AbilityKind
order, then a generic emblem), the Heroes' Wardrobe block and the inventory icons of the suit pieces
(nothing draws them on the body: the skin is the look).

With --test it also writes the test characters of the gametests (src/gametest/resources): their
skins (64x64 player layout plus the _brilho glow layer), item icons and item files. Original pixel
art.

Usage:  pip install pillow
        python tools/generate_engine_textures.py [--test]
"""
import json
import math
import os
import sys

from herolib.pixel import Icon, Palette, blank, box_faces, fill_box, fill_rect, from_map, paint_face, save

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
PAL = Palette(main="#3C6EB4", accent="#C8323C", glow="#F2B705", seed=2000)

# ------------------------------------------------------------------------- ability class icons


def strike():  # golpe: a fist
    return Icon(PAL).map([
        "................",
        "................",
        "....OOOOOOO.....",
        "...OAAOAAOAO....",
        "...OAAOAAOAAO...",
        "...OAAOAAOAAO...",
        "..OOAAAAAAAAO...",
        ".OAAOAAAAAAAO...",
        ".OAAAOAAAAAAO...",
        ".OaAAAAAAAAaO...",
        "..OaaAAAAAaO....",
        "...OOaaaaaO.....",
        "....OsssssO.....",
        "....OSSSSSO.....",
        ".....OOOOO......",
        "................",
    ])


def shot():  # disparo: a bolt flying right
    return Icon(PAL).line(1, 8, 9, 8, "g").line(2, 6, 7, 6, "G").line(2, 10, 7, 10, "G").disc(11, 8, 3.2, "G").disc(11, 8, 1.6, "h").outline()


def blast():  # explosao: a burst
    icon = Icon(PAL)
    for dx, dy in ((0, -6), (0, 6), (-6, 0), (6, 0), (-4, -4), (4, 4), (-4, 4), (4, -4)):
        icon.line(8, 8, 8 + dx, 8 + dy, "A")
    return icon.disc(8, 8, 3.5, "G").disc(8, 8, 1.8, "h").outline()


def movement():  # movimento: a wing
    return Icon(PAL).map([
        "................",
        "................",
        ".........OOO....",
        "......OOOnnnO...",
        "....OOnnnMMnO...",
        "...OnnMMMMMnO...",
        "..OnMMMMMMMnO...",
        "..OMMmmMMMnO....",
        "..OMmOOmMMnO....",
        "...OO..OmMO.....",
        "........OmO.....",
        ".........OO.....",
        "................",
        "...OOOOOOOOOO...",
        "................",
        "................",
    ])


def defense():  # defesa: a shield
    return Icon(PAL).map([
        "................",
        "...OOOOOOOOOO...",
        "..OnnnnnnnnnnO..",
        "..OnMMMMMMMMnO..",
        "..OnMMMGGMMMnO..",
        "..OnMMMGGMMMnO..",
        "..OnMGGGGGGMnO..",
        "..OnMGGGGGGMnO..",
        "..OnMMMGGMMMnO..",
        "...OnMMGGMMnO...",
        "...OnMMMMMMnO...",
        "....OnMMMMnO....",
        ".....OnMMnO.....",
        "......OnnO......",
        ".......OO.......",
        "................",
    ])


def control():  # controle: a spiral
    icon = Icon(PAL)
    for i in range(60):
        t = i / 60 * 4.2
        r = 1.2 + t * 1.45
        icon.px(round(8 + math.cos(t * 2.4) * r), round(8 + math.sin(t * 2.4) * r), "h" if i < 20 else "G")
    return icon.outline()


def summon():  # invocacao: a ring with a star
    return Icon(PAL).ring(8, 8, 6, "M", 1.4).map([
        "...G...",
        "..GhG..",
        "GGhhhGG",
        ".GhhhG.",
        "..GGG..",
        ".GG.GG.",
        ".G...G.",
    ], 5, 5).outline()


def evasion():  # evasao: a dash with trails
    return Icon(PAL).line(1, 5, 7, 5, "W").line(0, 8, 6, 8, "W").line(1, 11, 7, 11, "W").disc(11, 8, 3.5, "n").disc(11, 8, 2, "h").outline()


def empower():  # fortalecimento: an arrow up
    return Icon(PAL).map([
        "................",
        ".......OO.......",
        "......OAAO......",
        ".....OAbbAO.....",
        "....OAbbbbAO....",
        "...OAAbbbbAAO...",
        "..OOOAbbbbAOOO..",
        "....OAbbbbAO....",
        "....OAbbbbAO....",
        "....OAbbbbAO....",
        "....OAAAAAAO....",
        "....OaaaaaaO....",
        "....OOOOOOOO....",
        "...OGGGGGGGGO...",
        "...OOOOOOOOOO...",
        "................",
    ])


def supreme():  # suprema: a crown with a star
    return Icon(PAL).map([
        "................",
        ".......OO.......",
        "......OhhO......",
        ".O....OGGO....O.",
        "OGO..OGGGGO..OGO",
        "OGGO.OGGGGO.OGGO",
        "OGGGOGGGGGGOGGGO",
        "OGGGGGGhhGGGGGGO",
        "OGGGGGhAAhGGGGGO",
        "OgGGGGGhhGGGGGgO",
        "OggGGGGGGGGGGggO",
        "OOOOOOOOOOOOOOOO",
        "OgAgGgAgGgAgGgAO",
        "OOOOOOOOOOOOOOOO",
        "................",
        "................",
    ])


def emblem():  # a generic hero emblem: a star in a diamond
    return Icon(PAL).map([
        ".......OO.......",
        "......OnnO......",
        ".....OnMMnO.....",
        "....OnMGGMnO....",
        "...OnMMGGMMnO...",
        "..OnMGGhhGGMnO..",
        ".OnMMMGhhGMMMnO.",
        "OnMMMMGGGGMMMMnO",
        "OnMMMGGMMGGMMMnO",
        ".OnMGGMMMMGGMnO.",
        "..OnMMMMMMMMnO..",
        "...OnMMMMMMnO...",
        "....OnMMMMnO....",
        ".....OnMMnO.....",
        "......OnnO......",
        ".......OO.......",
    ])


def ability_sheet():
    icons = [strike(), shot(), blast(), movement(), defense(), control(), summon(), evasion(), empower(), supreme(), emblem()]
    sheet = blank(16 * len(icons), 16)
    for i, icon in enumerate(icons):
        sheet.paste(icon.image, (i * 16, 0))
    save(sheet, "gui", "engine", "abilities.png")

# ------------------------------------------------------------------------- the wardrobe

WOOD = Palette(main="#7A5230", accent="#C9A227", glow="#E8E0D0", seed=7)


def wardrobe():
    front = blank(16, 16)
    fill_rect(front, 0, 0, 16, 16, "M", WOOD)
    fill_rect(front, 1, 1, 14, 14, "m", WOOD)
    for x0 in (2, 8):  # two doors with a hero emblem each
        fill_rect(front, x0, 2, 6, 12, "n", WOOD, 4)
        fill_rect(front, x0 + 1, 3, 4, 10, "M", WOOD, 4)
        fill_rect(front, x0 + 2, 6, 2, 2, "A", WOOD, 0)
    fill_rect(front, 7, 7, 1, 2, "b", WOOD, 0)
    fill_rect(front, 8, 7, 1, 2, "b", WOOD, 0)
    save(front, "block", "heroes_wardrobe_front.png")

    side = blank(16, 16)
    fill_rect(side, 0, 0, 16, 16, "M", WOOD)
    for y in (0, 5, 10, 15):
        fill_rect(side, 0, y, 16, 1, "m", WOOD)
    fill_rect(side, 0, 0, 1, 16, "m", WOOD)
    fill_rect(side, 15, 0, 1, 16, "m", WOOD)
    save(side, "block", "heroes_wardrobe_side.png")

    top = blank(16, 16)
    fill_rect(top, 0, 0, 16, 16, "n", WOOD)
    fill_rect(top, 1, 1, 14, 14, "M", WOOD)
    fill_rect(top, 6, 6, 4, 4, "A", WOOD, 0)
    save(top, "block", "heroes_wardrobe_top.png")

# ------------------------------------------------------------------------- suit piece icons

SUIT = Palette(main="#8C8C9A", accent="#F2B705", glow="#FFFFFF", seed=3)
SUIT_ICONS = {
    "cabeca": [
        "................",
        "................",
        "....OOOOOOOO....",
        "...OnnnnnnnnO...",
        "..OnMMMMMMMMnO..",
        "..OMMMMMMMMMMO..",
        "..OMMOOMMOOMMO..",
        "..OMMhOMMOhMMO..",
        "..OMMMMMMMMMMO..",
        "..OmMMO..OMMmO..",
        "..OmmO....OmmO..",
        "...OO......OO...",
        "................",
        "................",
        "................",
        "................",
    ],
    "peito": [
        "................",
        "..OOOO....OOOO..",
        ".OMMMMOOOOMMMMO.",
        ".OMnMMMMMMMMnMO.",
        ".OMMMMAAAAMMMMO.",
        ".OmMMMAhhAMMMmO.",
        ".OmOMMAAAAMMOmO.",
        ".OmOMMMMMMMMOmO.",
        "..OOMMMMMMMMOO..",
        "....OMMMMMMO....",
        "....OmMMMMmO....",
        "....OmmmmmmO....",
        "....OOOOOOOO....",
        "................",
        "................",
        "................",
    ],
    "pernas": [
        "................",
        "...OOOOOOOOOO...",
        "...OAAAAAAAAO...",
        "...OMMMMMMMMO...",
        "...OMMMOOMMMO...",
        "...OMMMOOMMMO...",
        "...OMMMOOMMMO...",
        "...OMMMOOMMMO...",
        "...OmMMOOMMmO...",
        "...OmMMOOMMmO...",
        "...OmmMOOMmmO...",
        "...OmmmOOmmmO...",
        "...OOOOOOOOOO...",
        "................",
        "................",
        "................",
    ],
    "pes": [
        "................",
        "................",
        "................",
        "................",
        "................",
        "...OOO....OOO...",
        "...OMO....OMO...",
        "...OMO....OMO...",
        "...OMO....OMO...",
        "..OMMO....OMMO..",
        ".OMMMO....OMMMO.",
        ".OmmmO....OmmmO.",
        ".OOOOO....OOOOO.",
        "................",
        "................",
        "................",
    ],
}


def suit_icons():
    for slot, rows in SUIT_ICONS.items():
        save(from_map(rows, SUIT, 3), "item", f"skin_suit_{slot}.png")

# ------------------------------------------------------------------------- test characters (gametests)

TEST_ASSETS = os.path.join(ROOT, "src", "gametest", "resources", "assets", "greenlantern")

# The modern 64x64 player layout: (u, v) of each box, its overlay, and the arm width.
BOXES = {
    "head": ((0, 0, 8, 8, 8), (32, 0, 8, 8, 8)),
    "body": ((16, 16, 8, 12, 4), (16, 32, 8, 12, 4)),
    "right_leg": ((0, 16, 4, 12, 4), (0, 32, 4, 12, 4)),
    "left_leg": ((16, 48, 4, 12, 4), (0, 48, 4, 12, 4)),
}


def arms(slim):
    w = 3 if slim else 4
    return {"right_arm": (40, 16, w, 12, 4), "left_arm": (32, 48, w, 12, 4)}


def test_skin(skin_id, main, accent, glow, slim):
    pal = Palette(main=main, accent=accent, glow=glow, seed=len(skin_id))
    img = blank(64, 64)
    lit = blank(64, 64)
    head = box_faces(*BOXES["head"][0])
    fill_box(img, head, "k", pal, 0)
    for name in ("top", "back", "right", "left"):
        x, y, w, h = head[name]
        fill_rect(img, x, y, w, h if name == "top" else 3, "m", pal)  # hair / cowl
    face = ["mmmmmmmm", "mMMMMMMm", "MwOMMOwM", "kkkkkkkk", "kkkKKkkk", "kkkkkkkk", "kkKKKKkk", "kkkkkkkk"]
    paint_face(img, head, "front", face, pal, 0)
    paint_face(lit, head, "front", ["........", "........", ".h....h.", "........", "........", "........", "........", "........"], pal, 0)

    body = box_faces(*BOXES["body"][0])
    fill_box(img, body, "M", pal)
    chest = ["MMMMMMMM", "MMMMMMMM", "MMMGGMMM", "MMGhhGMM", "MMMGGMMM", "MMMMMMMM", "MMMMMMMM", "MMMMMMMM",
             "AAAAAAAA", "aaaGGaaa", "MMMMMMMM", "MMMMMMMM"]
    paint_face(img, body, "front", chest, pal)
    paint_face(lit, body, "front", ["........"] * 2 + ["...GG...", "..GhhG..", "...GG..."] + ["........"] * 7, pal, 0)
    for name, (u, v, w, h, d) in arms(slim).items():
        faces = box_faces(u, v, w, h, d)
        fill_box(img, faces, "M", pal)
        for face_name in ("front", "back", "right", "left", "bottom"):
            x, y, fw, fh = faces[face_name]
            if face_name == "bottom":
                fill_rect(img, x, y, fw, fh, "a", pal)
            else:
                fill_rect(img, x, y + fh - 4, fw, 4, "A", pal)  # gloves
    for name in ("right_leg", "left_leg"):
        faces = box_faces(*BOXES[name][0])
        fill_box(img, faces, "m", pal)
        for face_name in ("front", "back", "right", "left"):
            x, y, fw, fh = faces[face_name]
            fill_rect(img, x, y + fh - 5, fw, 5, "A", pal)  # boots
        x, y, fw, fh = faces["bottom"]
        fill_rect(img, x, y, fw, fh, "a", pal)
    for folder, image, suffix in ((TEST_ASSETS, img, ""), (TEST_ASSETS, lit, "_brilho")):
        path = os.path.join(folder, "textures", "entity", "skin", f"{skin_id}{suffix}.png")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        image.save(path)
        print("wrote", os.path.relpath(path, ROOT))


def write_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")
    print("wrote", os.path.relpath(path, ROOT))


def test_character(char_id, main, accent, glow, slim):
    test_skin(char_id, main, accent, glow, slim)
    pal = Palette(main=main, accent=accent, glow=glow, seed=5)
    icon = from_map([
        ".......OO.......",
        "......OMMO......",
        ".....OMMMMO.....",
        "....OMMGGMMO....",
        "...OMMGhhGMMO...",
        "..OMMMGhhGMMMO..",
        ".OMMMMMGGMMMMMO.",
        "OMMMMMAAAAMMMMMO",
        "OmMMMMAAAAMMMMmO",
        ".OmMMMMMMMMMMmO.",
        "..OmMMMMMMMMmO..",
        "...OmMMMMMMmO...",
        "....OmMMMMmO....",
        ".....OmmmmO.....",
        "......OmmO......",
        ".......OO.......",
    ], pal, 3)
    path = os.path.join(TEST_ASSETS, "textures", "item", f"{char_id}.png")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    icon.save(path)
    write_json(os.path.join(TEST_ASSETS, "models", "item", f"{char_id}.json"),
               {"parent": "minecraft:item/generated", "textures": {"layer0": f"greenlantern:item/{char_id}"}})
    write_json(os.path.join(TEST_ASSETS, "items", f"{char_id}.json"),
               {"model": {"type": "minecraft:model", "model": f"greenlantern:item/{char_id}"}})
    for slot in SUIT_ICONS:
        write_json(os.path.join(TEST_ASSETS, "items", f"{char_id}_{slot}.json"),
                   {"model": {"type": "minecraft:model", "model": f"greenlantern:item/skin_suit_{slot}"}})


def main():
    ability_sheet()
    wardrobe()
    suit_icons()
    if "--test" in sys.argv:
        test_character("prototipo", "#2E8B57", "#1C1C24", "#7CFFB0", False)
        test_character("prototipo_colosso", "#7A3E9D", "#3A2A1A", "#FFB347", False)
        test_character("prototipo_alado", "#D8D8E8", "#3C6EB4", "#FFF4A0", False)
        test_skin("prototipo_noite", "#20202C", "#5A5A6E", "#FF5050", False)


if __name__ == "__main__":
    main()
