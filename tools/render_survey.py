#!/usr/bin/env python3
"""Turns the worldgen survey's text maps into pictures for the README.

The survey (src/main/java/.../dev/WorldgenSurvey.java) prints a biome map of the End and a
top-down view of every astral biome it generated. Give this script the survey's log (CI prints
it in the "Survey the generated End" step, and uploads it as survey.log):

    python3 tools/render_survey.py survey.log

It writes docs/images/end-biome-map.png and docs/images/biome-<name>.png. Needs Pillow.
"""
import re
import sys
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "docs/images"
TAG = "[AstralEnd survey] "

BIOME_COLOURS = {
    "O": (230, 224, 160), "N": (122, 82, 196), "A": (150, 215, 225), "W": (120, 96, 88), "D": (48, 36, 70),
    "C": (240, 150, 60), "H": (196, 190, 130), "M": (166, 160, 110), "B": (130, 126, 90),
    "s": (100, 98, 76), "?": (255, 0, 255),
}
BIOME_NAMES = {
    "O": "central island", "N": "Nebula Fields", "A": "Ashen Barrens", "W": "Comet Wake", "D": "Hollow Isles",
    "C": "The Corona", "H": "Highlands", "M": "Midlands", "B": "Barrens", "s": "Small Islands",
}
# Top-down symbols, as printed by WorldgenSurvey.topSymbol (upper case = inside the surveyed biome).
BLOCK_COLOURS = {
    " ": (12, 10, 20), "E": (219, 222, 158), "Y": (140, 90, 140), "M": (98, 66, 160), "L": (80, 230, 215),
    "S": (246, 208, 106), "V": (150, 90, 210), "P": (208, 207, 216), "G": (160, 220, 222), "B": (175, 174, 188),
    "J": (230, 235, 238), "H": (60, 55, 45), "I": (160, 195, 235), "T": (215, 205, 234), "O": (140, 150, 170),
    "U": (92, 76, 118), "X": (138, 102, 234), "Z": (255, 0, 200), "Q": (232, 236, 242), "K": (200, 208, 222),
    "C": (220, 205, 175), "R": (255, 170, 40), "F": (220, 70, 40), "N": (240, 180, 70), "A": (120, 116, 110),
    "D": (138, 132, 123), "W": (205, 213, 216), "#": (255, 0, 255),
}


def survey_lines(path):
    """Survey lines from either the server log (prefixed with TAG) or CI's printout (prefix removed)."""
    for line in Path(path).read_text(errors="replace").splitlines():
        yield line.split(TAG, 1)[1] if TAG in line else line


def dim(colour, factor=0.45):
    return tuple(int(c * factor) for c in colour)


def render_biome_map(rows, path):
    scale = 6
    w, h = len(rows[0]), len(rows)
    img = Image.new("RGB", (w * scale, h * scale * 2 + 118), (20, 18, 28))
    draw = ImageDraw.Draw(img)
    for y, row in enumerate(rows):
        for x, c in enumerate(row):
            # Each map row covers two sample rows (the survey steps z twice as far as x).
            draw.rectangle([x * scale, y * scale * 2, x * scale + scale - 1, y * scale * 2 + scale * 2 - 1],
                           fill=BIOME_COLOURS.get(c, (255, 0, 255)))
    ly = h * scale * 2 + 10
    for i, key in enumerate(BIOME_NAMES):
        lx = 10 + (i % 3) * 160
        yy = ly + (i // 3) * 26
        draw.rectangle([lx, yy, lx + 14, yy + 14], fill=BIOME_COLOURS[key])
        draw.text((lx + 20, yy + 1), BIOME_NAMES[key], fill=(230, 230, 236))
    img.save(path)


def render_top_view(rows, title, path):
    scale = 4
    size = len(rows)
    img = Image.new("RGB", (size * scale, size * scale + 24), (20, 18, 28))
    draw = ImageDraw.Draw(img)
    for y, row in enumerate(rows):
        for x, c in enumerate(row):
            inside = not c.islower()
            colour = BLOCK_COLOURS.get(c.upper(), BLOCK_COLOURS["#"])
            if not inside and c != " ":
                colour = dim(colour)
            draw.rectangle([x * scale, y * scale, x * scale + scale - 1, y * scale + scale - 1], fill=colour)
    draw.text((6, size * scale + 5), title + "  (dimmed: outside the biome)", fill=(230, 230, 236))
    img.save(path)


def main():
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    lines = list(survey_lines(sys.argv[1]))
    OUT.mkdir(parents=True, exist_ok=True)
    i = 0
    written = []
    while i < len(lines):
        line = lines[i]
        if line.startswith("Biome map of the End"):
            rows = []
            j = i + 2  # skip the legend line
            while j < len(lines) and lines[j].startswith("  ") and not lines[j].startswith("  |"):
                rows.append(lines[j][2:])
                j += 1
            render_biome_map(rows, OUT / "end-biome-map.png")
            written.append("end-biome-map.png")
            i = j
            continue
        m = re.match(r"Top-down view of (.+?) around", line)
        if m:
            name = m.group(1)
            rows = []
            j = i + 1
            while j < len(lines) and lines[j].startswith("  |"):
                rows.append(lines[j][3:-1])
                j += 1
            slug = re.sub(r"[^a-z]+", "-", name.lower().replace("the ", "")).strip("-")
            render_top_view(rows, name, OUT / f"biome-{slug}.png")
            written.append(f"biome-{slug}.png")
            i = j
            continue
        i += 1
    print("Wrote " + ", ".join(written) if written else "No survey maps found in that log.")


if __name__ == "__main__":
    main()
