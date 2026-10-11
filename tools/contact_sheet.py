#!/usr/bin/env python3
"""Contact sheet of a screenshot run: every screenshot, labelled, on one image.

    python3 tools/contact_sheet.py run/screenshots contact-sheet.png

Used by the Screenshots workflow so a whole hero can be reviewed at a glance. Needs Pillow.
"""
import math
import os
import sys

from PIL import Image, ImageDraw, ImageFont

THUMB_W = 320
COLUMNS = 4
LABEL_H = 22
GAP = 6


def main():
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    folder, out = sys.argv[1], sys.argv[2]
    names = sorted(n for n in os.listdir(folder) if n.lower().endswith('.png')) if os.path.isdir(folder) else []
    if not names:
        print('contact_sheet: no screenshots in ' + folder)
        return
    thumbs = []
    for name in names:
        with Image.open(os.path.join(folder, name)) as image:
            image = image.convert('RGB')
            h = round(image.height * THUMB_W / image.width)
            thumbs.append((name, image.resize((THUMB_W, h), Image.LANCZOS)))
    thumb_h = max(t.height for _, t in thumbs)
    rows = math.ceil(len(thumbs) / COLUMNS)
    sheet = Image.new('RGB', (COLUMNS * (THUMB_W + GAP) + GAP, rows * (thumb_h + LABEL_H + GAP) + GAP), (24, 26, 32))
    draw = ImageDraw.Draw(sheet)
    font = ImageFont.load_default()
    for i, (name, thumb) in enumerate(thumbs):
        x = GAP + (i % COLUMNS) * (THUMB_W + GAP)
        y = GAP + (i // COLUMNS) * (thumb_h + LABEL_H + GAP)
        sheet.paste(thumb, (x, y))
        label = name[3:-4] if name.startswith('gl_') else name[:-4]
        draw.text((x + 4, y + thumb_h + 4), label, fill=(235, 235, 235), font=font)
    sheet.save(out)
    print(f'contact_sheet: {len(thumbs)} screenshots -> {out}')


if __name__ == '__main__':
    main()
