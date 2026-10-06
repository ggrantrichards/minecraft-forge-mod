#!/usr/bin/env python3
"""Textures for the caves (after beta.3): the Hollow Roots' Hollow Glowcap and Starsap (ore, block
and item), and the Deep Hollows' carving - the Hollow Thorn and the four faces of a Hollow Lantern.

    python3 tools/textures/cave_textures.py

Drawn the vanilla way: a few tones of one hue each, little noise. The ore is set into the real
Nightstone texture, as vanilla's ores are into their stone.
"""
import random
from pathlib import Path

from PIL import Image

TEX = Path(__file__).resolve().parents[2] / "src/main/resources/assets/astralend/textures"


def hexc(h, a=255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def draw(rows, pal):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in pal:
                img.putpixel((x, y), pal[ch])
    return img


# ---- Hollow Glowcap: a cluster of three small violet-lit caps on thin pale stems.
GLOWCAP = [
    "................",
    "................",
    "................",
    "......hHh.......",
    ".....hcccd......",
    ".....dcccd..hHh.",
    "......s.d..hcccd",
    "..hHh.s....dcccd",
    ".hcccd.s....sd..",
    ".dcccd.s....s...",
    "...s...s...s....",
    "...s....s..s....",
    "....s...s.s.....",
    "....s...ss......",
    ".....s..s.......",
    "................",
]
GLOWCAP_PAL = {"H": hexc("f0dcff"), "h": hexc("c9a6f4"), "c": hexc("9a6ae0"), "d": hexc("6a3fae"), "s": hexc("b8aec4")}

# ---- Starsap: a drop of old amber with a spark of light in it.
STARSAP = [
    "................",
    "................",
    ".......o........",
    "......oyo.......",
    "......oyo.......",
    ".....oyWyo......",
    ".....oyYyo......",
    "....oyYWYyo.....",
    "....oyYYyyo.....",
    "....oyyYyao.....",
    "....oayyyao.....",
    ".....oaaao......",
    "......ooo.......",
    "................",
    "................",
    "................",
]
STARSAP_PAL = {"o": hexc("5a2a0c"), "a": hexc("9a4a12"), "y": hexc("d07a1c"), "Y": hexc("f0a630"), "W": hexc("fff0b8")}

# ---- Hollow Thorn: a long, curved black thorn, a glint of violet down its edge.
THORN = [
    "................",
    "..............bv",
    ".............bv.",
    "............bv..",
    "...........bb...",
    "..........bv....",
    ".........bb.....",
    "........bv......",
    ".......bb.......",
    "......bv........",
    ".....bbd........",
    "....ddd.........",
    "...dkd..........",
    "..dkd...........",
    ".dkd............",
    "dd..............",
]
THORN_PAL = {"b": hexc("1a1420"), "v": hexc("8a5ad0"), "d": hexc("2e2236"), "k": hexc("4a3a56")}

# ---- Hollow Lantern faces: eyes and a mouth carved into a pumpkin, lit violet from inside.
FACES = [
    [   # grinning
        "................", "................", "................", "..##........##..", "..###......###..", "...##......##...",
        "................", "................", "................", "...#.#.##.#.#...", "...##########...", "....########....",
        ".....#.##.#.....", "................", "................", "................"],
    [   # screaming
        "................", "................", "...#........#...", "..###......###..", "..###......###..", "...#........#...",
        "................", ".......##.......", "......####......", "......####......", "......####......", "......####......",
        ".......##.......", "................", "................", "................"],
    [   # slit-eyed, sly
        "................", "................", "................", "..####....####..", "...###....###...", "................",
        "................", "................", "..#..........#..", "..###......###..", "...##########...", ".....######.....",
        "................", "................", "................", "................"],
    [   # the Horseman's: hollow sockets and a jagged maw
        "................", "................", "..###......###..", ".#####....#####.", ".#####....#####.", "..###......###..",
        "................", "................", "..#.#.#..#.#.#..", "..############..", "...##########...", "...#.#.##.#.#...",
        "................", "................", "................", "................"],
]
PUMPKIN = [hexc("8a4a0c"), hexc("b8650f"), hexc("d77f18"), hexc("e8962a")]
GLOW = [hexc("4a1c78"), hexc("8a4ad8"), hexc("d8b8ff")]


def pumpkin_face(face):
    rnd = random.Random(31)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            # vertical ribs, darker at the seams and the edges
            rib = x % 5 in (0,)
            t = 2 - (1 if rib else 0) - (1 if x in (0, 15) or y in (0, 15) else 0) + (1 if rnd.random() < 0.12 else 0)
            img.putpixel((x, y), PUMPKIN[max(0, min(3, t))])
    for y, row in enumerate(face):
        for x, ch in enumerate(row):
            if ch == "#":
                # a carved hole: bright at its heart, darker at its rim
                rim = any(not (0 <= x + dx < 16 and 0 <= y + dy < 16) or face[y + dy][x + dx] != "#"
                          for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
                img.putpixel((x, y), GLOW[1] if rim else GLOW[2])
                if y + 1 < 16 and face[y + 1][x] != "#":
                    img.putpixel((x, y + 1), GLOW[0])     # the cut edge of the rind
    return img


def ore(base, specks):
    img = Image.open(TEX / "block" / base).convert("RGBA")
    for x, y, c in specks:
        img.putpixel((x, y), c)
    return img


def main():
    (TEX / "item").mkdir(parents=True, exist_ok=True)
    draw(GLOWCAP, GLOWCAP_PAL).save(TEX / "block" / "hollow_glowcap.png")
    draw(STARSAP, STARSAP_PAL).save(TEX / "item" / "starsap.png")
    draw(THORN, THORN_PAL).save(TEX / "item" / "hollow_thorn.png")
    # Starsap Ore: amber blebs set into Nightstone.
    amber = [hexc("9a4a12"), hexc("d07a1c"), hexc("f0a630"), hexc("fff0b8")]
    specks = []
    for cx, cy in ((3, 4), (10, 3), (6, 10), (12, 11), (2, 13)):
        specks += [(cx, cy, amber[1]), (cx + 1, cy, amber[2]), (cx, cy + 1, amber[0]), (cx + 1, cy + 1, amber[1])]
        specks.append((cx + 1, cy, amber[3] if (cx + cy) % 3 == 0 else amber[2]))
    ore("nightstone.png", specks).save(TEX / "block" / "starsap_ore.png")
    # Block of Starsap: deep amber, lighter toward its middle, a frame of darker sap, motes caught in it.
    deep = [hexc("4e2208"), hexc("7a360c"), hexc("a24c12"), hexc("c46a1a"), hexc("e08e2c")]
    rnd = random.Random(7)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5))
            t = 0 if d > 7 else 1 if d > 6 else 3 - (1 if d > 4 else 0) + (1 if d < 2.5 else 0)
            if rnd.random() < 0.08:
                t -= 1
            img.putpixel((x, y), deep[max(0, min(4, t))])
    for x, y in ((5, 5), (10, 9), (7, 11)):
        img.putpixel((x, y), hexc("ffe6a8"))
    img.save(TEX / "block" / "starsap_block.png")
    for i, face in enumerate(FACES):
        pumpkin_face(face).save(TEX / "block" / f"hollow_lantern_{i}.png")
    print("Wrote the cave textures")


if __name__ == "__main__":
    main()
