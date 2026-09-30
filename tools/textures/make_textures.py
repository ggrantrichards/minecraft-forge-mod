#!/usr/bin/env python3
"""Draws every Astral End texture as 16x16 pixel art, in Minecraft's style.

Run from the project root:  python3 tools/textures/make_textures.py
Needs Pillow (pip install pillow). Output goes straight into src/main/resources,
plus a zoomed preview sheet at docs/images/textures.png.

Items and plants are drawn by hand as character maps (one character = one pixel).
Stone-like blocks are drawn from small palettes with seeded noise, so the same
seed always gives the same texture. To change a texture, edit it here and rerun,
or open the PNG in any pixel editor.
"""
import json
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
TEX = ROOT / "src/main/resources/assets/astralend/textures"
PREVIEW = ROOT / "docs/images/textures.png"


def hexc(h, a=255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


CLEAR = (0, 0, 0, 0)


def new():
    return Image.new("RGBA", (16, 16), CLEAR)


def from_map(rows, pal):
    """rows: 16 strings of 16 chars; pal: char -> colour. '.' is transparent."""
    assert len(rows) == 16, len(rows)
    img = new()
    for y, row in enumerate(rows):
        assert len(row) == 16, (y, row)
        for x, ch in enumerate(row):
            if ch != ".":
                img.putpixel((x, y), pal[ch])
    return img


# ---------------------------------------------------------------- stone helpers

def stone(pal, seed, blotches=22, specks=18, glint=None, glints=0):
    """Vanilla-style stone: a mid tone broken up by small lighter/darker blotches.
    pal = [darkest, dark, mid, light, lightest]."""
    rnd = random.Random(seed)
    img = Image.new("RGBA", (16, 16), pal[2])
    px = img.load()
    for _ in range(blotches):
        x, y = rnd.randrange(16), rnd.randrange(16)
        c = rnd.choice([pal[1], pal[1], pal[3], pal[3], pal[0], pal[4]])
        for _ in range(rnd.randint(2, 5)):
            px[x % 16, y % 16] = c
            x += rnd.choice([-1, 0, 1])
            y += rnd.choice([-1, 0, 1])
    for _ in range(specks):
        px[rnd.randrange(16), rnd.randrange(16)] = rnd.choice([pal[0], pal[4], pal[1], pal[3]])
    if glint:
        for _ in range(glints):
            px[rnd.randrange(16), rnd.randrange(16)] = glint
    return img


def polished(pal, seed):
    """Smooth centre with a bevelled 1px frame, like polished andesite/deepslate."""
    rnd = random.Random(seed)
    img = Image.new("RGBA", (16, 16), pal[2])
    px = img.load()
    for _ in range(14):
        px[rnd.randrange(2, 14), rnd.randrange(2, 14)] = rnd.choice([pal[1], pal[3]])
    for i in range(16):
        px[i, 0] = pal[3]
        px[0, i] = pal[3]
        px[i, 15] = pal[0]
        px[15, i] = pal[0]
    for i in range(1, 15):
        px[i, 1] = pal[4] if i % 5 else pal[3]
        px[1, i] = pal[3]
        px[i, 14] = pal[1]
        px[14, i] = pal[1]
    px[0, 15] = pal[1]
    px[15, 0] = pal[1]
    return img


def bricks(pal, seed):
    """Two rows of bricks per 8px, offset every row, with dark mortar and lit top edges."""
    rnd = random.Random(seed)
    img = Image.new("RGBA", (16, 16), pal[2])
    px = img.load()
    for y in range(16):
        for x in range(16):
            if rnd.random() < 0.18:
                px[x, y] = rnd.choice([pal[1], pal[3]])
    for row in range(4):
        y0 = row * 4
        mortar_y = y0 + 3
        for x in range(16):
            px[x, mortar_y] = pal[0]
            px[x, y0] = pal[3] if rnd.random() < 0.8 else pal[4]
        offset = 0 if row % 2 == 0 else 4
        for bx in (offset, offset + 8):
            x = bx % 16
            for y in range(y0, y0 + 3):
                px[x, y] = pal[0]
                px[(x + 1) % 16, y] = pal[3] if y > y0 else px[(x + 1) % 16, y]
    return img


def overlay(base, rows, pal):
    img = base.copy()
    top = from_map(rows, pal)
    img.alpha_composite(top)
    return img


# ---------------------------------------------------------------- palettes

UMBRAL = [hexc("131019"), hexc("1d1925"), hexc("272130"), hexc("332b3e"), hexc("41374f")]
UMBRAL_GLINT = hexc("4d3f66")
METEOR_ROCK = [hexc("1b1513"), hexc("261e1a"), hexc("30261f"), hexc("3b2f27"), hexc("4a3a2e")]
STARDUST = [hexc("a896c9"), hexc("b7a7d5"), hexc("c6b9e0"), hexc("d6cdea"), hexc("e6e0f4")]
BONE = [hexc("7c8891"), hexc("9aa6ad"), hexc("b6c0c5"), hexc("cdd5d8"), hexc("e0e6e8")]
MOSS = [hexc("2e1f52"), hexc("452e78"), hexc("573b92"), hexc("6b4aab"), hexc("3d7f86")]

VOID = {
    "o": hexc("190b31"),   # outline
    "d": hexc("34196a"),   # dark
    "m": hexc("5a33b3"),   # mid
    "l": hexc("8a66ea"),   # light
    "h": hexc("b9e9ff"),   # highlight (cool cyan-white)
    "w": hexc("ffffff"),
}
METAL = {
    "o": hexc("1f222a"),
    "d": hexc("3e4450"),
    "m": hexc("626b7b"),
    "l": hexc("8f99aa"),
    "h": hexc("c9d2e0"),
    "w": hexc("eef3fa"),
    "b": hexc("4f6e9c"),   # blue sheen
}
RAW_METAL = dict(METAL, r=hexc("6b4a35"), R=hexc("8a6246"))


# ---------------------------------------------------------------- items

VOID_CRYSTAL = [
    "................",
    "........o.......",
    ".......oho......",
    "......ohlmo.....",
    ".....ohllmdo....",
    "....ohlllmmdo...",
    "...ohhlllmmddo..",
    "..ohhwllmmmddo..",
    "..olhllmmmdddo..",
    "...olllmmmddo...",
    "....ollmmddo....",
    ".....olmmdo.....",
    "......omdo......",
    ".......oo.......",
    "................",
    "................",
]

METEORIC_INGOT = [
    "................",
    "................",
    "................",
    "................",
    ".......ooooooo..",
    ".....oowhhhhlo..",
    "...oohhlllllmdo.",
    "..ohwllllllmmdo.",
    ".ohlllllmmmmddo.",
    ".olllmmmmmbddo..",
    ".odmmmmmbddoo...",
    ".oddddddddo.....",
    "..oooooooo......",
    "................",
    "................",
    "................",
]

METEORIC_NUGGET = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "......oooo......",
    ".....ohhlmo.....",
    "....ohwllmdo....",
    "....olllmmdo....",
    "....odmmbddo....",
    ".....oddddo.....",
    "......oooo......",
    "................",
    "................",
    "................",
    "................",
]

RAW_METEORIC = [
    "................",
    "................",
    ".....ooo........",
    "....ohlmoo......",
    "...ohlmmrmo.....",
    "..ohllmrRmdoo...",
    "..olmmmmmmmhlo..",
    ".ohlmRrmmmllmdo.",
    ".olmmrmmmlmmddo.",
    ".odmmmmmmmrRddo.",
    "..oddmmmmmrddo..",
    "...oddddmmddo...",
    "....ooodddoo....",
    ".......ooo......",
    "................",
    "................",
]

COMET_CRESS = [
    "................",
    "......w...w.....",
    ".....wcw.wcw....",
    "......c...c..w..",
    "..w...c..c..wcw.",
    ".wcw..c..c...c..",
    "..c...c.c...c...",
    "...c..cc...c....",
    "....c.cc..c.....",
    ".....ccc.c......",
    "......ccc.......",
    "......gcg.......",
    ".....ggGgg......",
    "......gGg.......",
    ".......g........",
    "................",
]
CRESS_PAL = {
    "w": hexc("f4fbff"),
    "c": hexc("cfe3ea"),
    "g": hexc("7fa6b0"),
    "G": hexc("5d8591"),
}


# ---------------------------------------------------------------- plants

STARBLOOM = [
    "................",
    ".......y........",
    "......yYy.......",
    ".yyyyyYwYyyyyy..",
    "..yYYYwwwYYYy...",
    "...yYYwWwYYy....",
    "....yYwwwYy.....",
    "...yYYyYyYYy....",
    "..yYy..s..yYy...",
    "..yy...s...yy...",
    ".......s........",
    ".....l.s........",
    "......ls..l.....",
    ".......s.l......",
    ".......sl.......",
    ".......s........",
]
STARBLOOM_PAL = {
    "y": hexc("e0a93b"),
    "Y": hexc("f6d06a"),
    "w": hexc("fff3c4"),
    "W": hexc("ffffff"),
    "s": hexc("2f6b66"),
    "l": hexc("4c9088"),
}

# Lumineshroom: stem + glowing cap. Frames change the cap's brightness ("breathing").
LUMINESHROOM = [
    "................",
    "................",
    "................",
    "................",
    "......cccc......",
    "....cCCwCCcc....",
    "...cCCCCCwCCc...",
    "...cCwCCCCCCc...",
    "..ddcccccccccd..",
    "....ddddsddd....",
    ".......sS.......",
    ".......sS.......",
    "......ssS.......",
    ".......sS.......",
    "......sSSs......",
    "................",
]


def lumineshroom_frame(t):
    """t in 0..1: 0 = dim, 1 = brightest."""
    def mix(a, b):
        return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (255,)
    return {
        "c": mix(hexc("1f8e8c"), hexc("3fe7d3")),
        "C": mix(hexc("2fb7ad"), hexc("8ffff0")),
        "w": mix(hexc("9fe9df"), hexc("ffffff")),
        "d": mix(hexc("135a5c"), hexc("249c95")),
        "s": hexc("b8aecb"),
        "S": hexc("dcd5ea"),
    }


ASTRAL_VINE_PLANT = [
    "......v.........",
    "......vV........",
    ".......V....v...",
    ".......vV..vV...",
    "..v.....V.vV....",
    "..Vv....VvV.....",
    "...Vv...vV......",
    "....V..vV.......",
    "....vV.V........",
    ".....vVv........",
    "......V.....b...",
    "......vV...vV...",
    ".......V..vV....",
    ".......vVvV.....",
    "........V.......",
    "........v.......",
]
ASTRAL_VINE_TIP = [
    "................",
    "................",
    "......b.........",
    ".....bBb....b...",
    "......v....bBb..",
    "......vV....v...",
    ".......V...vV...",
    ".......vV.vV....",
    "..b.....VvV.....",
    ".bBb....vV......",
    "..v....vV.......",
    "..Vv...V........",
    "...Vv.vV........",
    "....VvV.........",
    ".....V..........",
    ".....v..........",
]
VINE_PAL = {
    "v": hexc("5b2e8f"),
    "V": hexc("8452c4"),
    "b": hexc("c7a8ff"),
    "B": hexc("f2eaff"),
}

CRESS_STAGES = [
    [
        "................", "................", "................", "................",
        "................", "................", "................", "................",
        "................", "................", "................", "................",
        "....w.......w...", "....c...w...c...", "....c...c...c...", "....g...g...g...",
    ],
    [
        "................", "................", "................", "................",
        "................", "................", "................", "................",
        "................", "...w.......w....", "...c...w...c..w.", "...c...c...c..c.",
        "..wc...c..wc..c.", "..cc..wc..cc..c.", "..gc..cc..gc..g.", "..gg..gg..gg..g.",
    ],
    [
        "................", "................", "................", "................",
        "................", "..w.....w.......", ".wcw...wcw..w...", "..c.....c..wcw..",
        "..c..w..c...c...", "..c.wcw.c...c.w.", "..c..c..c...cwcw", ".cc..c.cc..cc.c.",
        ".cg..c.cg..cg.c.", ".gg..cggg..gg.c.", ".gG..gGg...gG.g.", ".gG..gGg...gG.g.",
    ],
    [
        "................", "..w.......w.....", ".wcw.....wcw..w.", "..c...w...c..wcw",
        "..c..wcw..c...c.", "..c...c...c...c.", ".wc...c..cc..wc.", "wcw...c..c...cw.",
        ".c...cc..c...c..", ".c..wcc..c..cc..", ".cc.cwc.cc..c...", "..c.c.ccc..cc...",
        "..gcg..cg..cg...", "..gGg.gGg.gGg...", "..gGg.gGg.gGg...", "...g...g...g....",
    ],
]


# ---------------------------------------------------------------- blocks

def void_crystal_block():
    """A cut-gem block like vanilla's diamond/emerald blocks: bevelled frame, faceted face."""
    rows = [
        "hhhhhhhhhhhhhhhl",
        "hwlllllllllllldo",
        "hllmmmmmmmmmmddo",
        "hlmhllllllllmddo",
        "hlmlhllllllmmddo",
        "hlmllhlllllmdddo",
        "hlmlllmmmmmmdmdo",
        "hlmlllmhlmmmdmdo",
        "hlmlllmllmdmdmdo",
        "hlmlllmmmddmdmdo",
        "hlmllmmmmmdddmdo",
        "hlmlmmmmmmmddmdo",
        "hlmmddddddddddo.",
        "hldddddddddddddo",
        "hdddddddddddddoo",
        "loooooooooooooo.",
    ]
    rows = [r.replace(".", "o") for r in rows]
    return from_map(rows, VOID)


def metal_block(pal, seed):
    rnd = random.Random(seed)
    img = Image.new("RGBA", (16, 16), pal["m"])
    px = img.load()
    for y in range(16):
        for x in range(16):
            r = rnd.random()
            if r < 0.12:
                px[x, y] = pal["l"]
            elif r < 0.2:
                px[x, y] = pal["d"]
            elif r < 0.23:
                px[x, y] = pal["b"]
    for i in range(16):
        px[i, 0] = pal["h"]
        px[0, i] = pal["h"]
        px[i, 15] = pal["o"]
        px[15, i] = pal["o"]
        px[i, 1] = pal["l"]
        px[1, i] = pal["l"]
        px[i, 14] = pal["d"]
        px[14, i] = pal["d"]
    # riveted plate seams, like a forged block
    for i in range(2, 14):
        px[i, 8] = pal["d"]
        px[i, 7] = pal["l"]
    for x, y in [(3, 3), (12, 3), (3, 11), (12, 11)]:
        px[x, y] = pal["w"]
        px[x + 1, y + 1] = pal["o"]
    return img


def raw_metal_block(seed):
    rnd = random.Random(seed)
    img = Image.new("RGBA", (16, 16), RAW_METAL["m"])
    px = img.load()
    for _ in range(40):
        x, y = rnd.randrange(16), rnd.randrange(16)
        c = rnd.choice([RAW_METAL["l"], RAW_METAL["d"], RAW_METAL["r"], RAW_METAL["R"], RAW_METAL["h"], RAW_METAL["d"]])
        for _ in range(rnd.randint(2, 4)):
            px[x % 16, y % 16] = c
            x += rnd.choice([-1, 0, 1])
            y += rnd.choice([-1, 0, 1])
    return img


ORE_VOID = [
    "................",
    "..o.............",
    ".ohlo........o..",
    "..omdo......oho.",
    "...od......olmdo",
    ".............od.",
    "......o.........",
    ".....ohlo.......",
    "....ohlmdo......",
    ".....omdo.....o.",
    "......o......ohl",
    ".o...........omd",
    "ohlo..........o.",
    ".omdo....oho....",
    "..od....olmdo...",
    ".........od.....",
]

ORE_METEORIC = [
    "................",
    "...ol...........",
    "..olhm......oo..",
    "..ommd.....olhm.",
    "...od......ommdo",
    "............odd.",
    ".......oo.......",
    "......olhmo.....",
    "......ommbd.....",
    ".......oddo..ol.",
    ".............ohm",
    "..oo..........md",
    ".olhm...........",
    ".ommd.....olm...",
    "..od......obdo..",
    "...........od...",
]


def stardust_block():
    img = stone(STARDUST, 11, blotches=26, specks=30)
    px = img.load()
    rnd = random.Random(12)
    star, warm = hexc("ffffff"), hexc("fff1b8")
    for _ in range(5):
        x, y = rnd.randrange(1, 15), rnd.randrange(1, 15)
        px[x, y] = star
        if rnd.random() < 0.5:
            px[x + 1, y] = warm
    return img


def astral_glass():
    img = new()
    px = img.load()
    frame = hexc("2a2342")
    inner = hexc("3a3160")
    for i in range(16):
        px[i, 0] = frame
        px[0, i] = frame
        px[i, 15] = frame
        px[15, i] = frame
    for i in range(1, 15):
        px[i, 1] = inner
        px[1, i] = inner
    # a few "stars" caught in the glass, and a diagonal glint like vanilla glass
    for x, y, c in [(4, 5, "e9e2ff"), (11, 3, "b6a8ff"), (9, 10, "ffffff"), (5, 12, "b6a8ff"), (12, 12, "e9e2ff")]:
        px[x, y] = hexc(c)
    for i in range(3):
        px[3 + i, 9 - i] = hexc("8f84c9")
        px[10 + i, 7 - i] = hexc("8f84c9")
    return img


def leviathan_bone_side():
    """Fibrous, vertical grain like vanilla's bone block, with a few knuckle ridges."""
    img = Image.new("RGBA", (16, 16), BONE[3])
    px = img.load()
    rnd = random.Random(21)
    for x in range(16):
        col = [BONE[2], BONE[3], BONE[4], BONE[3], BONE[2], BONE[1]][x % 6]
        for y in range(16):
            px[x, y] = col if rnd.random() > 0.12 else rnd.choice([BONE[1], BONE[4]])
    for x0, y0 in [(2, 3), (9, 5), (5, 10), (12, 12)]:
        for dx in range(3):
            px[(x0 + dx) % 16, y0] = BONE[4]
            px[(x0 + dx) % 16, y0 + 1] = BONE[1]
    for i in range(16):
        px[i, 0] = BONE[1]
        px[i, 15] = BONE[1]
    return img


def leviathan_bone_top():
    img = Image.new("RGBA", (16, 16), BONE[3])
    px = img.load()
    for y in range(16):
        for x in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5))
            if d > 6.5:
                px[x, y] = BONE[1]
            elif d > 5.5:
                px[x, y] = BONE[4]
            elif d > 3.5:
                px[x, y] = BONE[2]
            elif d > 2.5:
                px[x, y] = BONE[1]
            elif d > 1:
                px[x, y] = hexc("56606a")
            else:
                px[x, y] = hexc("7fd6d0")   # a faint glow in the marrow
    return img


def nebula_moss():
    rnd = random.Random(31)
    img = Image.new("RGBA", (16, 16), MOSS[2])
    px = img.load()
    for _ in range(30):
        x, y = rnd.randrange(16), rnd.randrange(16)
        c = rnd.choice([MOSS[1], MOSS[3], MOSS[3], MOSS[4], MOSS[0]])
        for _ in range(rnd.randint(2, 5)):
            px[x % 16, y % 16] = c
            x += rnd.choice([-1, 0, 1])
            y += rnd.choice([-1, 0, 1])
    for _ in range(4):
        px[rnd.randrange(16), rnd.randrange(16)] = hexc("a5f0ea")
    return img


ECLIPSE = [
    "................",
    ".DDDDDDDDDDDDDD.",
    ".D............D.",
    ".D....LLLL....D.",
    ".D..LLddddLL..D.",
    ".D..LddddddL..D.",
    ".D.LddddddddL.D.",
    ".D.LddddddddL.D.",
    ".D.LddddddddL.D.",
    ".D.LddddddddL.D.",
    ".D..LddddddL..D.",
    ".D..LLddddLL..D.",
    ".D....LLLL....D.",
    ".D............D.",
    ".DDDDDDDDDDDDDD.",
    "................",
]



# ---------------------------------------------------------------- Phase 3 palettes

PALE = [hexc("9e9dab"), hexc("b4b3c0"), hexc("c8c7d2"), hexc("d9d8e1"), hexc("e8e7ee")]
CHARRED = [hexc("1f1c18"), hexc("2c2822"), hexc("3a352c"), hexc("4a4436"), hexc("5f5842")]
CHARRED_EMBER = hexc("8a5a2a")
ICE = [hexc("6f93c4"), hexc("86a9d6"), hexc("9fbfe4"), hexc("bcd6f0"), hexc("e3f0fc")]
SUNSCORCHED = [hexc("a8967a"), hexc("bfae90"), hexc("d3c4a7"), hexc("e3d7be"), hexc("f1e9d6")]
OBS_QUARTZ = [hexc("b3bccb"), hexc("c8d0dc"), hexc("dae0e9"), hexc("e8ecf2"), hexc("f5f7fa")]
SOLAR = {
    "o": hexc("4a1d05"),
    "d": hexc("a4400b"),
    "m": hexc("e67912"),
    "l": hexc("ffb52e"),
    "h": hexc("ffe88a"),
    "w": hexc("fffbe0"),
    "b": hexc("c85a0e"),
}
RAW_SOLAR = dict(SOLAR, r=hexc("6e5a44"), R=hexc("8c7657"))
ASH = {"c": hexc("5e5b58"), "C": hexc("77736e"), "w": hexc("8f8a84"), "d": hexc("47443f"),
       "s": hexc("8d8880"), "S": hexc("a39e95")}
PETRIFIED = {"v": hexc("6b6660"), "V": hexc("8a847b"), "b": hexc("a9a298"), "B": hexc("c4bdb1")}


def cracks(img, colour, seed, n=4, length=(3, 7)):
    """Thin wandering cracks, like cracked stone bricks or dripstone."""
    rnd = random.Random(seed)
    px = img.load()
    for _ in range(n):
        x, y = rnd.randrange(16), rnd.randrange(16)
        dx, dy = rnd.choice([(1, 0), (0, 1), (1, 1), (1, -1)])
        for _ in range(rnd.randint(*length)):
            px[x % 16, y % 16] = colour
            if rnd.random() < 0.35:
                dx, dy = rnd.choice([(1, 0), (0, 1), (1, 1), (1, -1), (-1, 1)])
            x += dx
            y += dy
    return img


def tiles(pal, seed):
    """Four 8x8 tiles with dark grout and lit top-left edges, like deepslate tiles."""
    rnd = random.Random(seed)
    img = Image.new("RGBA", (16, 16), pal[2])
    px = img.load()
    for y in range(16):
        for x in range(16):
            if rnd.random() < 0.2:
                px[x, y] = rnd.choice([pal[1], pal[3]])
    for t in (0, 8):
        for i in range(16):
            px[i, t + 7] = pal[0]
            px[t + 7, i] = pal[0]
            if i % 8 != 7:
                px[i, t] = pal[4]
                px[t, i] = pal[3]
    return img


def pillar_side(pal, seed):
    """Vertical fluting like a quartz pillar."""
    rnd = random.Random(seed)
    img = Image.new("RGBA", (16, 16), pal[2])
    px = img.load()
    shade = [pal[0], pal[4], pal[3], pal[2], pal[2], pal[3], pal[2], pal[1],
             pal[3], pal[2], pal[2], pal[3], pal[2], pal[2], pal[1], pal[0]]
    for x in range(16):
        for y in range(16):
            px[x, y] = shade[x] if rnd.random() > 0.08 else pal[1]
    return img


def pillar_top(pal):
    img = Image.new("RGBA", (16, 16), pal[2])
    px = img.load()
    for y in range(16):
        for x in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5))
            px[x, y] = pal[0] if d > 7 else pal[4] if d > 6 else pal[2] if d > 2.5 else pal[3] if d > 1 else pal[1]
    return img


def smooth(pal, seed, noise=0.12):
    """Near-flat stone like smooth quartz or calcite, with a faint grain."""
    rnd = random.Random(seed)
    img = Image.new("RGBA", (16, 16), pal[2])
    px = img.load()
    for y in range(16):
        for x in range(16):
            r = rnd.random()
            if r < noise:
                px[x, y] = pal[3]
            elif r < noise * 1.6:
                px[x, y] = pal[1]
    return img


STAR_INLAY = [
    "LLLLLLLLLLLLLLLL",
    "L..............D",
    "L..............D",
    "L.......g......D",
    "L.......g......D",
    "L......gGg.....D",
    "L...gggGyGggg..D",
    "L....ggGyGgg...D",
    "L......gGg.....D",
    "L.....g.g.g....D",
    "L....g..g..g...D",
    "L.......g......D",
    "L.......g......D",
    "L..............D",
    "L..............D",
    "DDDDDDDDDDDDDDDD",
]
INLAY_PAL = {"L": OBS_QUARTZ[4], "D": OBS_QUARTZ[0], "g": hexc("3b5a9a"), "G": hexc("6f8fd6"), "y": hexc("f3d77a")}

ORE_SOLAR = [
    "................",
    "..oo............",
    ".ohlo.......oo..",
    ".olmdo.....ohlo.",
    "..odd.....ohwmdo",
    "...........odd..",
    "......oo........",
    ".....ohlo.......",
    "....ohwmdo......",
    ".....olmdo....o.",
    "......odo....ohl",
    "..o..........omd",
    ".ohl..........o.",
    ".omd.....oo.....",
    "..o.....ohlo....",
    ".........odo....",
]

RAW_SOLARITE = [
    "................",
    "................",
    "......oo........",
    ".....ohlo.......",
    "....ohlmmoo.....",
    "...ohlmRrmdo....",
    "..ohwlmmmmhlo...",
    "..olmmrRmmlmdo..",
    ".ohlmmmmmmmmdo..",
    ".olmmRmmmrRmdo..",
    ".odmmrmmmmmddo..",
    "..oddmmmmmddo...",
    "...ooddddddo....",
    ".....oooooo.....",
    "................",
    "................",
]

SOLARITE_INGOT = [
    "................",
    "................",
    "................",
    "................",
    ".......ooooooo..",
    ".....oowhhhhlo..",
    "...oohhlllllmdo.",
    "..ohwllllllmmdo.",
    ".ohlllllmmmmddo.",
    ".olllmmmmmbddo..",
    ".odmmmmmbddoo...",
    ".oddddddddo.....",
    "..oooooooo......",
    "................",
    "................",
    "................",
]

SHIMMER_GRASS = [
    "................",
    "................",
    "................",
    "................",
    "..........w.....",
    "...w......s.....",
    "...s..w..sS..w..",
    "...Ss.s..S...s..",
    "....S.Ss.S..sS..",
    ".w..S..S.S..S...",
    ".s..Ss.S.S.sS...",
    ".Ss..S.SsS.S..w.",
    "..S..S..SS.S..s.",
    "..Sg.Sg.Sg.Sg.S.",
    "..gG.gG.gGgGg.g.",
    ".gGG.GG.GGGGG.G.",
]
SHIMMER_PAL = {"w": hexc("f4fbff"), "s": hexc("bfe8e6"), "S": hexc("8ccbd0"), "g": hexc("6aa3b2"), "G": hexc("4f8296")}

FLAREBLOOM_TOP = [
    "................",
    "................",
    "................",
    "................",
    ".......h........",
    "......hlh.......",
    ".....hlmlh......",
    ".....lmmml......",
    "....hlmdmlh.....",
    "....lmmdmml.....",
    "....lmddmml.....",
    ".....mdddm......",
    ".....mdddm......",
    "......ddd.......",
    ".......s........",
    ".......s........",
]
FLAREBLOOM_BOTTOM = [
    ".......s........",
    ".......s........",
    ".......S........",
    ".......s..ll....",
    "...l...sLll.....",
    "...lL..sL.......",
    "....lL.s........",
    ".....LLs........",
    ".......S........",
    ".......s..L.....",
    ".......s.LlL....",
    "...L...sLl......",
    "...lL..s........",
    "....lLLs........",
    ".......S........",
    ".......s........",
]
FLAREBLOOM_PAL = {"h": hexc("ffb347"), "l": hexc("e8612a"), "m": hexc("b8321c"), "d": hexc("6e1414"),
                  "s": hexc("7b6a4a"), "S": hexc("9b8762"), "L": hexc("a6865a")}


def sunscale_lichen():
    """Overlapping scales, gold at the edges and ember-red in the middle."""
    img = new()
    px = img.load()
    rnd = random.Random(61)
    edge, mid, deep, glow = hexc("f2c14e"), hexc("d98a2b"), hexc("9b4a1c"), hexc("ffe9a3")
    for y in range(16):
        for x in range(16):
            if rnd.random() < 0.18:
                continue  # gaps show the stone underneath
            cx = x % 4
            row = (y + (2 if (x // 4) % 2 else 0)) % 4
            px[x, y] = edge if row == 0 else glow if (row == 1 and cx == 1) else mid if row < 3 else deep
    return img


def ashen_lumineshroom():
    rows = [r.replace(".", ".") for r in LUMINESHROOM]
    rows[5] = "....cCCdCCcc...."
    rows[6] = "...cCCC.CcCCc..."
    rows[7] = "...cCcCCC..Cc..."
    return from_map(rows, ASH)


def pollinator_husk_side():
    """Bleached, segmented chitin, like a fossil shell."""
    img = Image.new("RGBA", (16, 16), BONE[3])
    px = img.load()
    rnd = random.Random(71)
    for y in range(16):
        seg = y % 5
        for x in range(16):
            c = [BONE[1], BONE[4], BONE[3], BONE[3], BONE[2]][seg]
            px[x, y] = c if rnd.random() > 0.1 else rnd.choice([BONE[2], BONE[4]])
    for y in range(16):
        px[0, y] = BONE[1]
        px[15, y] = BONE[0]
        if y % 5 == 2:
            px[7, y] = BONE[1]
            px[8, y] = BONE[1]
    return img


def pollinator_husk_top():
    img = Image.new("RGBA", (16, 16), BONE[2])
    px = img.load()
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            ring = int(d) % 3
            px[x, y] = BONE[1] if d > 7.2 else [BONE[4], BONE[3], BONE[1]][ring]
    for x, y in [(6, 6), (9, 6), (6, 9), (9, 9)]:
        px[x, y] = BONE[0]
    return img


def gravity_anomaly_frame(phase):
    """A dark core with violet light spiralling into it. phase 0..1 rotates the spiral."""
    import math
    img = Image.new("RGBA", (16, 16), UMBRAL[0])
    px = img.load()
    arm, glow, core = hexc("5b3fa8"), hexc("9c7bf0"), hexc("050308")
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            r = (dx * dx + dy * dy) ** 0.5
            a = math.atan2(dy, dx) / (2 * math.pi) + 0.5
            spiral = (a * 3 + r / 5 - phase) % 1.0
            if r < 2.0:
                px[x, y] = core
            elif r < 2.9:
                px[x, y] = arm if spiral < 0.5 else UMBRAL[1]   # the bright ring of the event horizon
            elif spiral < 0.2 and r < 8:
                px[x, y] = glow if r < 5.5 else arm
            elif spiral < 0.38 and r < 8:
                px[x, y] = arm
            elif (x in (0, 15)) or (y in (0, 15)):
                px[x, y] = UMBRAL[1]
    return img


# ---------------------------------------------------------------- write everything

def save(img, rel):
    path = TEX / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)
    return rel, img


def main():
    out = []
    # Umbral Stone family
    umbral = stone(UMBRAL, 1, glint=UMBRAL_GLINT, glints=3)
    out.append(save(umbral, "block/umbral_stone.png"))
    out.append(save(polished(UMBRAL, 2), "block/polished_umbral_stone.png"))
    out.append(save(bricks(UMBRAL, 3), "block/umbral_stone_bricks.png"))
    out.append(save(overlay(polished(UMBRAL, 4), ECLIPSE, {"D": UMBRAL[0], "L": hexc("6b58a0"), "d": hexc("0e0b12")}), "block/chiseled_umbral_stone.png"))

    # Ores and material blocks
    out.append(save(overlay(stone(UMBRAL, 5, glint=UMBRAL_GLINT, glints=2), ORE_VOID, VOID), "block/void_crystal_ore.png"))
    out.append(save(void_crystal_block(), "block/void_crystal_block.png"))
    out.append(save(overlay(stone(METEOR_ROCK, 6), ORE_METEORIC, METAL), "block/meteoric_iron_ore.png"))
    out.append(save(metal_block(METAL, 7), "block/meteoric_iron_block.png"))
    out.append(save(raw_metal_block(8), "block/raw_meteoric_iron_block.png"))

    # Other blocks
    out.append(save(stardust_block(), "block/stardust_block.png"))
    out.append(save(astral_glass(), "block/astral_glass.png"))
    out.append(save(leviathan_bone_side(), "block/leviathan_bone_side.png"))
    out.append(save(leviathan_bone_top(), "block/leviathan_bone_top.png"))
    out.append(save(nebula_moss(), "block/nebula_moss.png"))

    # Plants
    out.append(save(from_map(STARBLOOM, STARBLOOM_PAL), "block/starbloom.png"))
    out.append(save(from_map(ASTRAL_VINE_TIP, VINE_PAL), "block/astral_vine.png"))
    out.append(save(from_map(ASTRAL_VINE_PLANT, VINE_PAL), "block/astral_vine_plant.png"))
    for i, rows in enumerate(CRESS_STAGES):
        out.append(save(from_map(rows, CRESS_PAL), f"block/comet_cress_stage{i}.png"))

    # Lumineshroom: an animated strip of frames that "breathes" (dim -> bright -> dim).
    levels = [0.0, 0.25, 0.55, 0.85, 1.0, 0.85, 0.55, 0.25]
    strip = Image.new("RGBA", (16, 16 * len(levels)), CLEAR)
    for i, t in enumerate(levels):
        strip.paste(from_map(LUMINESHROOM, lumineshroom_frame(t)), (0, 16 * i))
    save(strip, "block/lumineshroom.png")
    (TEX / "block/lumineshroom.png.mcmeta").write_text(
        json.dumps({"animation": {"frametime": 10, "interpolate": True}}, indent=2) + "\n")
    out.append(("block/lumineshroom.png (frame 5 of 8)", from_map(LUMINESHROOM, lumineshroom_frame(1.0))))

    # Items
    out.append(save(from_map(VOID_CRYSTAL, VOID), "item/void_crystal.png"))
    out.append(save(from_map(RAW_METEORIC, RAW_METAL), "item/raw_meteoric_iron.png"))
    out.append(save(from_map(METEORIC_INGOT, METAL), "item/meteoric_iron_ingot.png"))
    out.append(save(from_map(METEORIC_NUGGET, METAL), "item/meteoric_iron_nugget.png"))
    out.append(save(from_map(COMET_CRESS, CRESS_PAL), "item/comet_cress.png"))


    # ---- Phase 3: Aurora Shallows
    out.append(save(stone(PALE, 101, blotches=20, specks=14), "block/pale_end_stone.png"))
    out.append(save(bricks(PALE, 102), "block/pale_end_stone_bricks.png"))
    out.append(save(tiles(PALE, 103), "block/pale_end_stone_tiles.png"))
    out.append(save(from_map(SHIMMER_GRASS, SHIMMER_PAL), "block/shimmer_grass.png"))
    # ---- Comet Wake
    charred = stone(CHARRED, 111, blotches=24, specks=16, glint=CHARRED_EMBER, glints=3)
    out.append(save(charred, "block/charred_end_stone.png"))
    out.append(save(cracks(stone(ICE, 112, blotches=16, specks=10), ICE[4], 113, n=5), "block/comet_ice.png"))
    # ---- The Corona
    out.append(save(cracks(stone(SUNSCORCHED, 121, blotches=18, specks=12), SUNSCORCHED[0], 122, n=4), "block/sunscorched_stone.png"))
    out.append(save(polished(SUNSCORCHED, 123), "block/polished_sunscorched_stone.png"))
    out.append(save(bricks(SUNSCORCHED, 124), "block/sunscorched_stone_bricks.png"))
    out.append(save(overlay(cracks(stone(SUNSCORCHED, 125, blotches=18, specks=12), SUNSCORCHED[0], 126, n=3), ORE_SOLAR, SOLAR),
                    "block/solarite_ore.png"))
    out.append(save(metal_block(SOLAR, 127), "block/solarite_block.png"))
    rs = raw_metal_block(128)
    rs_px = rs.load()
    for y in range(16):
        for x in range(16):
            c = rs_px[x, y]
            # recolour the raw-metal noise into Solarite's palette
            rs_px[x, y] = {RAW_METAL["m"]: SOLAR["m"], RAW_METAL["l"]: SOLAR["l"], RAW_METAL["d"]: SOLAR["d"],
                           RAW_METAL["r"]: RAW_SOLAR["r"], RAW_METAL["R"]: RAW_SOLAR["R"], RAW_METAL["h"]: SOLAR["h"]}.get(c, c)
    out.append(save(rs, "block/raw_solarite_block.png"))
    out.append(save(from_map(FLAREBLOOM_TOP, FLAREBLOOM_PAL), "block/flarebloom_top.png"))
    out.append(save(from_map(FLAREBLOOM_BOTTOM, FLAREBLOOM_PAL), "block/flarebloom_bottom.png"))
    out.append(save(sunscale_lichen(), "block/sunscale_lichen.png"))
    out.append(save(ashen_lumineshroom(), "block/ashen_lumineshroom.png"))
    out.append(save(pollinator_husk_side(), "block/pollinator_husk_side.png"))
    out.append(save(pollinator_husk_top(), "block/pollinator_husk_top.png"))
    out.append(save(from_map(ASTRAL_VINE_TIP, PETRIFIED), "block/petrified_astral_vine.png"))
    # ---- Shattered Observatory
    out.append(save(smooth(OBS_QUARTZ, 131), "block/observatory_quartz.png"))
    out.append(save(pillar_side(OBS_QUARTZ, 132), "block/observatory_quartz_pillar_side.png"))
    out.append(save(pillar_top(OBS_QUARTZ), "block/observatory_quartz_pillar_top.png"))
    out.append(save(overlay(smooth(OBS_QUARTZ, 133), STAR_INLAY, INLAY_PAL), "block/star_inlay_observatory_quartz.png"))
    # ---- Dark Matter Belt: the core of a gravity inversion pocket (animated, 8 frames)
    frames = 8
    strip = Image.new("RGBA", (16, 16 * frames), CLEAR)
    for i in range(frames):
        strip.paste(gravity_anomaly_frame(i / frames), (0, 16 * i))
    save(strip, "block/gravity_anomaly.png")
    (TEX / "block/gravity_anomaly.png.mcmeta").write_text(
        json.dumps({"animation": {"frametime": 3}}, indent=2) + "\n")
    out.append(("block/gravity_anomaly.png (frame 1 of 8)", gravity_anomaly_frame(0)))
    # ---- Phase 3 items
    out.append(save(from_map(RAW_SOLARITE, RAW_SOLAR), "item/raw_solarite.png"))
    out.append(save(from_map(SOLARITE_INGOT, SOLAR), "item/solarite_ingot.png"))

    write_preview(out)
    print(f"Wrote {len(out)} textures to {TEX.relative_to(ROOT)} and a preview to {PREVIEW.relative_to(ROOT)}")


def write_preview(items):
    """A zoomed sheet of every texture on a checkerboard, with names underneath."""
    from PIL import ImageDraw

    scale, cols, pad, label_h = 8, 6, 12, 26
    cell_w, cell_h = 16 * scale + pad * 2, 16 * scale + pad + label_h
    rows = (len(items) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * cell_w, rows * cell_h), hexc("1b1b1f"))
    draw = ImageDraw.Draw(sheet)
    for i, (name, img) in enumerate(items):
        cx, cy = (i % cols) * cell_w + pad, (i // cols) * cell_h + pad
        checker = Image.new("RGBA", (16 * scale, 16 * scale))
        cd = ImageDraw.Draw(checker)
        for y in range(0, 16 * scale, scale * 2):
            for x in range(0, 16 * scale, scale * 2):
                c = (58, 58, 64, 255) if (x // (scale * 2) + y // (scale * 2)) % 2 else (44, 44, 50, 255)
                cd.rectangle([x, y, x + scale * 2 - 1, y + scale * 2 - 1], fill=c)
        checker.alpha_composite(img.resize((16 * scale, 16 * scale), Image.NEAREST))
        sheet.paste(checker, (cx, cy))
        label = name.split("/")[-1].replace(".png", "")
        draw.text((cx, cy + 16 * scale + 5), label[:22], fill=(220, 220, 228, 255))
    PREVIEW.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(PREVIEW)


if __name__ == "__main__":
    main()
