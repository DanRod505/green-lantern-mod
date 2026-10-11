"""Original pixel art for a hero: a palette made from the hero sheet's colours, character maps painted
onto items, the two suit layers (64x32, the vanilla humanoid layout) and 16x16 power icons, and the
power wheel sheet.

    from herolib.pixel import Palette, Icon, blank, box_faces, from_map, paint_face, power_sheet, save

    pal = Palette(main="#2A6FE0", accent="#D8202E", glow="#FFD447")   # letters m M n a A b g G h ...
    save(from_map(ITEM_ROWS, pal), "item", "argo_pendant.png")
    icon = Icon(pal); icon.disc(8, 8, 5, "g"); icon.line(2, 14, 14, 2, "G")
    power_sheet("supergirl", [icon.image, ...], emblem)

Needs Pillow (pip install pillow). Randomness comes from one seeded generator per Palette, so a
script writes the same files every time.
"""
import math
import os
import random

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")
TEX = os.path.join(ROOT, "src", "main", "resources", "assets", "greenlantern", "textures")
CLEAR = (0, 0, 0, 0)


def rgb(value):
    """'#2A6FE0', '2A6FE0' or 0x2A6FE0 as an (r, g, b, 255) tuple."""
    if isinstance(value, str):
        value = int(value.lstrip("#"), 16)
    return ((value >> 16) & 255, (value >> 8) & 255, value & 255, 255)


def shade(c, factor):
    """Darker below 1, lighter (towards white) above 1."""
    r, g, b, a = c
    if factor <= 1:
        return (int(r * factor), int(g * factor), int(b * factor), a)
    t = min(1.0, factor - 1)
    return (int(r + (255 - r) * t), int(g + (255 - g) * t), int(b + (255 - b) * t), a)


def lerp(a, b, t):
    return tuple(int(round(x + (y - x) * t)) for x, y in zip(a, b))


class Palette:
    """
    Colours by letter, for character maps. From the three colours of the hero sheet:

      m M n   main colour: dark, normal, light        a A b   accent: dark, normal, light
      g G h   glow: dark, normal, light               O       outline (main colour, very dark)
      k K     skin and skin shadow                    w W     white and light grey
      s S     steel and dark steel                    .       transparent

    More letters can be added with extra={"x": "#RRGGBB"}. Letters in noisy get a little random
    variation per pixel when painted (cloth and metal look less flat).
    """

    def __init__(self, main, accent, glow, extra=None, noisy="mMnaAbsS", seed=1):
        m, a, g = rgb(main), rgb(accent), rgb(glow)
        self.colors = {
            ".": CLEAR,
            "O": shade(m, 0.22),
            "m": shade(m, 0.65), "M": m, "n": shade(m, 1.35),
            "a": shade(a, 0.65), "A": a, "b": shade(a, 1.35),
            "g": shade(g, 0.75), "G": g, "h": shade(g, 1.5),
            "k": (238, 196, 160, 255), "K": (206, 160, 126, 255),
            "w": (248, 248, 246, 255), "W": (208, 210, 214, 255),
            "s": (150, 156, 166, 255), "S": (92, 98, 110, 255),
        }
        for letter, value in (extra or {}).items():
            self.colors[letter] = rgb(value) if not isinstance(value, tuple) else value
        self.noisy = noisy
        self.random = random.Random(seed)

    def __getitem__(self, letter):
        return self.colors[letter]

    def jitter(self, c, amount):
        d = self.random.randint(-amount, amount)
        r, g, b, a = c
        return (max(0, min(255, r + d)), max(0, min(255, g + d)), max(0, min(255, b + d)), a)

    def pixel(self, letter, noise=3):
        c = self.colors[letter]
        return self.jitter(c, noise) if noise and letter in self.noisy and c[3] else c


def blank(w, h):
    return Image.new("RGBA", (w, h), CLEAR)


def from_map(rows, palette, noise=0):
    """An image from a list of equal-length strings, one letter per pixel."""
    img = blank(len(rows[0]), len(rows))
    paint(img, 0, 0, rows, palette, noise)
    return img


def paint(img, x0, y0, rows, palette, noise=3):
    """Paints a character map at (x0, y0); '.' leaves what is there."""
    for y, row in enumerate(rows):
        for x, letter in enumerate(row):
            if letter != ".":
                img.putpixel((x0 + x, y0 + y), palette.pixel(letter, noise))


def fill_rect(img, x0, y0, w, h, letter, palette, noise=3):
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            img.putpixel((x, y), palette.pixel(letter, noise))


def box_faces(u, v, w, h, d):
    """The face rectangles (x, y, w, h) of a box in the vanilla UV layout."""
    return {
        "top": (u + d, v, w, d),
        "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h),
        "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h),
        "back": (u + d + w + d, v + d, w, h),
    }


# The boxes of the 64x32 suit layers (the vanilla humanoid armor layout).
HEAD = box_faces(0, 0, 8, 8, 8)
BODY = box_faces(16, 16, 8, 12, 4)
ARM = box_faces(40, 16, 4, 12, 4)
LEG = box_faces(0, 16, 4, 12, 4)


def paint_face(img, faces, name, rows, palette, noise=3):
    """Paints one face of a box; the map must be exactly the face's size."""
    x, y, w, h = faces[name]
    assert len(rows) == h and all(len(r) == w for r in rows), (name, w, h, rows)
    paint(img, x, y, rows, palette, noise)


def fill_box(img, faces, letter, palette, noise=3):
    """Every face of a box in one colour (a starting point to paint details over)."""
    for x, y, w, h in faces.values():
        fill_rect(img, x, y, w, h, letter, palette, noise)


def mirror(rows):
    """A map flipped left to right (the left side from the right one)."""
    return [r[::-1] for r in rows]


class Icon:
    """A 16x16 power icon drawn with simple shapes in palette letters."""

    def __init__(self, palette, size=16):
        self.palette = palette
        self.size = size
        self.image = blank(size, size)

    def px(self, x, y, letter):
        if 0 <= x < self.size and 0 <= y < self.size:
            self.image.putpixel((int(x), int(y)), self.palette.pixel(letter, 0))
        return self

    def line(self, x0, y0, x1, y1, letter):
        n = max(int(math.ceil(max(abs(x1 - x0), abs(y1 - y0)))), 1)
        for i in range(n + 1):
            self.px(round(x0 + (x1 - x0) * i / n), round(y0 + (y1 - y0) * i / n), letter)
        return self

    def disc(self, cx, cy, r, letter):
        for y in range(self.size):
            for x in range(self.size):
                if math.hypot(x - cx, y - cy) <= r:
                    self.px(x, y, letter)
        return self

    def ring(self, cx, cy, r, letter, width=1.0):
        for y in range(self.size):
            for x in range(self.size):
                if abs(math.hypot(x - cx, y - cy) - r) <= width / 2:
                    self.px(x, y, letter)
        return self

    def map(self, rows, x0=0, y0=0):
        paint(self.image, x0, y0, rows, self.palette, 0)
        return self

    def outline(self, letter="O"):
        """A one-pixel outline around everything drawn so far."""
        src = self.image.copy()
        for y in range(self.size):
            for x in range(self.size):
                if src.getpixel((x, y))[3]:
                    continue
                if any(0 <= x + dx < self.size and 0 <= y + dy < self.size and src.getpixel((x + dx, y + dy))[3]
                       for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                    self.px(x, y, letter)
        return self


def save(img, *path):
    """Saves under assets/greenlantern/textures, e.g. save(img, "item", "argo_pendant.png")."""
    full = os.path.join(TEX, *path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    img.save(full)
    print("wrote", os.path.relpath(full))


def power_sheet(hero_id, icons, emblem):
    """The power wheel sheet textures/gui/<hero_id>_powers.png: the power icons in wheel order, then the emblem."""
    images = [i.image if isinstance(i, Icon) else i for i in list(icons) + [emblem]]
    sheet = blank(16 * len(images), 16)
    for i, image in enumerate(images):
        sheet.paste(image, (i * 16, 0))
    save(sheet, "gui", f"{hero_id}_powers.png")
