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

UMBRAL = [hexc("201b2a"), hexc("2e283a"), hexc("3c3449"), hexc("4c425c"), hexc("5e5272")]   # lightened
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


def chorus_moss():
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


def upwell_core_frame(phase):
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



# ---------------------------------------------------------------- wood sets (Phase 3.5)

import hq as _hq  # the wood palettes live with the redrawn wood textures
LUMINE_STEM = _hq.LUMINE_STEM
LUMINE_GLOW = _hq.LUMINE_GLOW
LUMINE_PLANKS = _hq.LUMINE_PLANK
NULL_BARK = _hq.NULL_BARK
NULL_HEART = _hq.NULL_PLANK
NULL_LEAF = [hexc("21183a"), hexc("30245a"), hexc("403075"), hexc("53408f"), hexc("9a7ee6")]


def planks_tex(pal, seed):
    import hq
    return hq.planks(seed, pal)


def bark_tex(pal, seed, streak=None, streaks=0):
    """Vertical, furrowed bark like a log's side."""
    rnd = random.Random(seed)
    img = Image.new("RGBA", (16, 16), pal[2])
    px = img.load()
    for x in range(16):
        base = [pal[1], pal[2], pal[3], pal[2], pal[1], pal[0]][rnd.randrange(6)]
        for y in range(16):
            px[x, y] = base if rnd.random() > 0.25 else rnd.choice([pal[0], pal[1], pal[3], pal[4]])
    for _ in range(4):  # deep furrows
        x, y = rnd.randrange(16), rnd.randrange(16)
        for i in range(rnd.randint(4, 9)):
            px[x, (y + i) % 16] = pal[0]
    if streak:
        for _ in range(streaks):
            x, y = rnd.randrange(16), rnd.randrange(16)
            for i in range(rnd.randint(2, 5)):
                px[x, (y + i) % 16] = streak[1] if i % 3 else streak[2]
    return img


def log_top_tex(outer, inner, core):
    img = Image.new("RGBA", (16, 16), inner[2])
    px = img.load()
    for y in range(16):
        for x in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5))
            if d > 6.5:
                px[x, y] = outer[1] if (x + y) % 3 else outer[0]
            elif d > 5.5:
                px[x, y] = inner[1]
            elif int(d) % 2:
                px[x, y] = inner[3]
            else:
                px[x, y] = inner[2]
            if d < 1.5:
                px[x, y] = core
    return img


def cap_tex(seed):
    """A glowing Lumineshroom cap block: mottled teal flesh, pale lilac spots and a few bright motes."""
    rnd = random.Random(seed)
    shades = [hexc("1b6f70"), hexc("22827f"), hexc("2a968f"), hexc("33ab9f")]
    img = Image.new("RGBA", (16, 16), shades[1])
    px = img.load()
    for _ in range(40):
        x, y = rnd.randrange(16), rnd.randrange(16)
        c = rnd.choice(shades)
        for _ in range(rnd.randint(2, 5)):
            px[x % 16, y % 16] = c
            x += rnd.choice([-1, 0, 1])
            y += rnd.choice([-1, 0, 1])
    for _ in range(4):  # spots, like a mushroom cap
        cx, cy = rnd.randrange(16), rnd.randrange(16)
        for dx, dy in [(0, 0), (1, 0), (0, 1), (1, 1), (-1, 0), (0, -1)]:
            px[(cx + dx) % 16, (cy + dy) % 16] = hexc("c9bfe6") if (dx, dy) != (0, 0) else hexc("efe9ff")
    for _ in range(6):
        px[rnd.randrange(16), rnd.randrange(16)] = LUMINE_GLOW[2]
    return img


def door_tex(pal, frame, window, top):
    """A door half: plank boards in a frame; the top half has a small window."""
    img = planks_tex(pal, 900 + top).rotate(90)
    px = img.load()
    for i in range(16):
        px[0, i] = frame
        px[15, i] = frame
        px[1, i] = pal[3]
    if top:
        for i in range(16):
            px[i, 0] = frame
        for y in range(3, 8):
            for x in range(4, 12):
                px[x, y] = window if (x in (4, 11) or y in (3, 7)) is False else frame
        img2 = img
        return img2
    else:
        for i in range(16):
            px[i, 15] = frame
        for x in (11, 12):
            px[x, 3] = hexc("c9b27a")  # handle
        return img


def trapdoor_tex(pal, frame):
    img = planks_tex(pal, 950)
    px = img.load()
    for i in range(16):
        for j in (0, 15):
            px[i, j] = frame
            px[j, i] = frame
    for x in range(3, 13):
        px[x, 7] = frame
        px[x, 8] = frame
    for (x, y) in [(4, 4), (11, 4), (4, 11), (11, 11)]:
        px[x, y] = CLEAR
    return img


def leaves_tex(pal, seed, glints=0):
    rnd = random.Random(seed)
    img = new()
    px = img.load()
    for y in range(16):
        for x in range(16):
            r = rnd.random()
            if r < 0.14:
                continue  # holes, like vanilla leaves
            px[x, y] = pal[1] if r < 0.45 else pal[2] if r < 0.8 else pal[3]
    for _ in range(glints):
        px[rnd.randrange(16), rnd.randrange(16)] = pal[4]
    return img


NULLWOOD_SAPLING = [
    "................",
    "................",
    ".....ll.........",
    "....lLLl..l.....",
    "...lLllLlLLl....",
    "....llLLLLl.....",
    ".....lLbLll.....",
    "..lll.lb........",
    ".lLLLl.b..ll....",
    "..lllLlb.lLLl...",
    ".......bLLll....",
    ".......b.l......",
    ".......b........",
    "......bb........",
    ".......b........",
    ".......b........",
]
NULLWOOD_SAPLING_PAL = {"l": hexc("2c2148"), "L": hexc("4a3a78"), "b": hexc("1f1a29")}

NEBULA_TUFTS = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "..........p.....",
    "..p.......P.....",
    "..P...p..pP..p..",
    "..Pp..P..P...P..",
    "...P..Pp.P..pP..",
    ".p.P.pP..P.pP...",
    ".P.PpP..PP.P..p.",
    ".PpPP.P.P..P.pP.",
    "..PP..PPP.PP.P..",
    ".dPd.ddPddPdPd..",
    ".dddddddddddddd.",
]
TUFTS_PAL = {"p": hexc("b48cf0"), "P": hexc("8a5fd0"), "d": hexc("5a3a96")}

TENDRIL_PLANT = [
    ".......t........",
    ".......t........",
    "......tT........",
    "......T.........",
    "......Tt........",
    ".......T........",
    ".......Tg.......",
    "......tT........",
    "......T.........",
    "......Tt........",
    ".......T........",
    "......gT........",
    ".......Tt.......",
    "........T.......",
    ".......tT.......",
    ".......T........",
]
TENDRIL_TIP = [
    ".......t........",
    ".......t........",
    "......tT........",
    "......T.........",
    "......Tt........",
    ".......T........",
    ".......Tg.......",
    "......tT........",
    "......T.........",
    ".....gTg........",
    "......G.........",
    ".....gGg........",
    "......G.........",
    ".....gWg........",
    "......g.........",
    "................",
]
TENDRIL_PAL = {"t": hexc("5b2e8f"), "T": hexc("7a4ab8"), "g": hexc("5fe3d6"), "G": hexc("aefcf2"), "W": hexc("ffffff")}

SHIMMER_REED_TOP = [
    "................",
    "......w.........",
    ".....wsw........",
    "......s...w.....",
    "......s..wsw....",
    "..w...S...s.....",
    ".wsw..S...S.....",
    "..s...S..S......",
    "..S...S..S......",
    "...S..S.S.......",
    "...S..SSS.......",
    "....S.SS........",
    "....S.S.........",
    ".....SS.........",
    "......S.........",
    "......S.........",
]
SHIMMER_REED_BOTTOM = [
    "......S.........",
    "......S.........",
    ".....SS.........",
    "....S.S.........",
    "....S.S....S....",
    "...S..S...S.....",
    "...S..SS.S......",
    "......S.SS......",
    "......S.S.......",
    "......SS........",
    ".....gS.........",
    ".....gSg........",
    "....gGSg........",
    "....gGSGg.......",
    "...gGGSGgg......",
    "...gGGSGGg......",
]
REED_PAL = {"w": hexc("f4fbff"), "s": hexc("cfeeee"), "S": hexc("9fd6d8"), "g": hexc("6aa3b2"), "G": hexc("4f8296")}


# Null Lantern: the model shows the side's rows 6-13 and the end's centre 8x8.
NULL_LANTERN_SIDE = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "....rmmrrmmr....",
    "....mbbmmbbm....",
    "....bwbmmbwb....",
    "....bwwbbwwb....",
    "....bwwbbwwb....",
    "....mbwmmbbm....",
    "....mbbmmbbm....",
    "....rmmrrmmr....",
    "................",
    "................",
]
NULL_LANTERN_END = [
    "................",
    "................",
    "................",
    "................",
    "....rrmmmmrr....",
    "....rmbbbbmr....",
    "....mbbwwbbm....",
    "....mbwWWwbm....",
    "....mbwWWwbm....",
    "....mbbwwbbm....",
    "....rmbbbbmr....",
    "....rrmmmmrr....",
    "................",
    "................",
    "................",
    "................",
]
NULL_LANTERN_STEM = [
    "......s.........",
    ".......s........",
    "...ll..s..l.....",
    "..lLl..s.lLl....",
    "...l..ss..l.....",
    "......s.........",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
]
NULL_LANTERN_PAL = {"r": hexc("3b1766"), "m": hexc("7a2fb8"), "b": hexc("b25ae8"), "w": hexc("e6a6ff"),
                    "W": hexc("fff0ff"), "s": hexc("1f1a29"), "l": hexc("2c2148"), "L": hexc("4a3a78")}


def spore_lantern():
    """Glowing Spores behind a frame of Astral Glass, like a sea lantern."""
    rnd = random.Random(401)
    img = new()
    frame = [hexc("3e5f6b"), hexc("4d7480"), hexc("5b8793")]
    for y in range(16):
        for x in range(16):
            edge = min(x, y, 15 - x, 15 - y)
            if edge == 0:
                c = hexc("2a4049")
            elif edge == 1:
                c = rnd.choice(frame)
            else:
                t = 1.0 - (abs(x - 7.5) + abs(y - 7.5)) / 13.0
                base = tuple(int(a + (b - a) * t) for a, b in zip(hexc("1f8e8c")[:3], hexc("6ff2da")[:3])) + (255,)
                c = base
            img.putpixel((x, y), c)
    for _ in range(14):
        x, y = rnd.randint(3, 12), rnd.randint(3, 12)
        img.putpixel((x, y), hexc("e6fffa"))
        if rnd.random() < 0.5:
            img.putpixel((min(12, x + 1), y), hexc("aefcf2"))
    return img


def whalelight_scale():
    """Overlapping pale, pearly scales."""
    img = new()
    pal = [hexc("9dbaf5"), hexc("b5cfff"), hexc("d8e6ff"), hexc("f2f7ff"), hexc("7d92d0")]
    for y in range(16):
        for x in range(16):
            row = y // 4
            cx = (x + (2 if row % 2 else 0)) % 4
            cy = y % 4
            # each scale: bright at the top, darker toward its lower rim
            if cy == 3 or (cy == 2 and cx in (0, 3)):
                c = pal[4]
            elif cy == 0 and cx in (1, 2):
                c = pal[3]
            else:
                c = pal[1 + (cy + cx) % 2]
            img.putpixel((x, y), c)
    return img


def petrified_stalk_side():
    img = stone([hexc("5e5953"), hexc("6b6660"), hexc("7a746c"), hexc("8a847b"), hexc("a39c91")], 141, blotches=16, specks=10)
    px = img.load()
    for y in range(16):  # the fossil outline of a twisting vine running up the stalk
        x = 7 + int(3 * __import__("math").sin(y / 2.5))
        px[x, y] = hexc("4d4944")
        px[(x + 1) % 16, y] = hexc("a9a298")
    return img


def petrified_stalk_top():
    return log_top_tex([hexc("5e5953"), hexc("6b6660")], [hexc("5e5953"), hexc("6b6660"), hexc("7a746c"), hexc("8a847b")],
                       hexc("4d4944"))


# ---------------------------------------------------------------- write everything

def save(img, rel):
    path = TEX / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)
    return rel, img


def main():
    out = []
    # Nightstone family
    umbral = stone(UMBRAL, 1, glint=UMBRAL_GLINT, glints=3)
    out.append(save(umbral, "block/nightstone.png"))
    out.append(save(polished(UMBRAL, 2), "block/polished_nightstone.png"))
    out.append(save(bricks(UMBRAL, 3), "block/nightstone_bricks.png"))
    out.append(save(overlay(polished(UMBRAL, 4), ECLIPSE, {"D": UMBRAL[0], "L": hexc("6b58a0"), "d": hexc("0e0b12")}), "block/chiseled_nightstone.png"))

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
    out.append(save(chorus_moss(), "block/chorus_moss.png"))

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


    # ---- Phase 3: Ashen Barrens
    out.append(save(stone(PALE, 101, blotches=20, specks=14), "block/pale_end_stone.png"))
    out.append(save(bricks(PALE, 102), "block/pale_end_stone_bricks.png"))
    out.append(save(tiles(PALE, 103), "block/pale_end_stone_tiles.png"))
    out.append(save(from_map(SHIMMER_GRASS, SHIMMER_PAL), "block/shimmer_grass.png"))
    # ---- Comet Wake
    charred = stone(CHARRED, 111, blotches=24, specks=16, glint=CHARRED_EMBER, glints=3)
    out.append(save(charred, "block/charred_end_stone.png"))
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
    # ---- Shattered Observatory
    out.append(save(smooth(OBS_QUARTZ, 131), "block/observatory_quartz.png"))
    out.append(save(pillar_side(OBS_QUARTZ, 132), "block/observatory_quartz_pillar_side.png"))
    out.append(save(pillar_top(OBS_QUARTZ), "block/observatory_quartz_pillar_top.png"))
    out.append(save(overlay(smooth(OBS_QUARTZ, 133), STAR_INLAY, INLAY_PAL), "block/star_inlay_observatory_quartz.png"))
    # ---- Hollow Isles: the core of a Upwell (animated, 8 frames)
    frames = 8
    strip = Image.new("RGBA", (16, 16 * frames), CLEAR)
    for i in range(frames):
        strip.paste(upwell_core_frame(i / frames), (0, 16 * i))
    save(strip, "block/upwell_core.png")
    (TEX / "block/upwell_core.png.mcmeta").write_text(
        json.dumps({"animation": {"frametime": 3}}, indent=2) + "\n")
    out.append(("block/upwell_core.png (frame 1 of 8)", upwell_core_frame(0)))
    # ---- Phase 3 items
    out.append(save(from_map(RAW_SOLARITE, RAW_SOLAR), "item/raw_solarite.png"))
    out.append(save(from_map(SOLARITE_INGOT, SOLAR), "item/solarite_ingot.png"))

    # ---- Phase 3.5: Lumine wood (from giant Lumineshroom stems) and Nullwood (Hollow Isles trees)
    out.append(save(bark_tex(LUMINE_STEM, 201, LUMINE_GLOW, 5), "block/lumine_stem.png"))
    out.append(save(log_top_tex(LUMINE_STEM, LUMINE_STEM, LUMINE_GLOW[1]), "block/lumine_stem_top.png"))
    out.append(save(cap_tex(204), "block/lumine_cap.png"))
    out.append(save(bark_tex(NULL_BARK, 211, [None, NULL_HEART[2], NULL_HEART[4]], 3), "block/nullwood_log.png"))
    out.append(save(log_top_tex(NULL_BARK, NULL_HEART, NULL_HEART[4]), "block/nullwood_log_top.png"))
    out.append(save(bark_tex(NULL_HEART, 212), "block/stripped_nullwood_log.png"))
    out.append(save(log_top_tex(NULL_HEART, NULL_HEART, NULL_HEART[4]), "block/stripped_nullwood_log_top.png"))
    out.append(save(planks_tex(NULL_HEART, 213), "block/nullwood_planks.png"))
    out.append(save(leaves_tex(NULL_LEAF, 214, glints=6), "block/nullwood_leaves.png"))
    out.append(save(from_map(NULLWOOD_SAPLING, NULLWOOD_SAPLING_PAL), "block/nullwood_sapling.png"))
    out.append(save(door_tex(NULL_HEART, NULL_BARK[0], NULL_BARK[2], 1), "block/nullwood_door_top.png"))
    out.append(save(door_tex(NULL_HEART, NULL_BARK[0], NULL_BARK[2], 0), "block/nullwood_door_bottom.png"))
    out.append(save(trapdoor_tex(NULL_HEART, NULL_BARK[0]), "block/nullwood_trapdoor.png"))
    # item icons for the doors (like vanilla's)
    for wood, pal, frame in (("lumine", LUMINE_PLANKS, LUMINE_STEM[0]), ("nullwood", NULL_HEART, NULL_BARK[0])):
        icon = new()
        top = door_tex(pal, frame, LUMINE_GLOW[1] if wood == "lumine" else NULL_BARK[2], 1).resize((8, 8), Image.NEAREST)
        bottom = door_tex(pal, frame, frame, 0).resize((8, 8), Image.NEAREST)
        icon.paste(top, (4, 0))
        icon.paste(bottom, (4, 8))
        out.append(save(icon, f"item/{wood}_door.png"))
    # ---- new plants
    out.append(save(from_map(NEBULA_TUFTS, TUFTS_PAL), "block/nebula_tufts.png"))
    out.append(save(from_map(TENDRIL_TIP, TENDRIL_PAL), "block/nebula_tendril.png"))
    out.append(save(from_map(TENDRIL_PLANT, TENDRIL_PAL), "block/nebula_tendril_plant.png"))
    out.append(save(from_map(SHIMMER_REED_TOP, REED_PAL), "block/shimmer_reed_top.png"))
    out.append(save(from_map(SHIMMER_REED_BOTTOM, REED_PAL), "block/shimmer_reed_bottom.png"))
    out.append(save(from_map(NULL_LANTERN_SIDE, NULL_LANTERN_PAL), "block/null_lantern_side.png"))
    out.append(save(from_map(NULL_LANTERN_END, NULL_LANTERN_PAL), "block/null_lantern_end.png"))
    out.append(save(from_map(NULL_LANTERN_STEM, NULL_LANTERN_PAL), "block/null_lantern_stem.png"))
    # ---- Phase 4: mob drops and blocks
    from mob_items import ITEM_MAPS
    for name, (rows, colours) in ITEM_MAPS.items():
        out.append(save(from_map(rows, {k: hexc(v) for k, v in colours.items()}), f"item/{name}.png"))
    out.append(save(spore_lantern(), "block/spore_lantern.png"))
    out.append(save(whalelight_scale(), "block/whalelight_scale.png"))

    # ---- The redrawn textures (tools/textures/hq.py) replace the older versions above.
    import hq
    names = {rel for rel, _ in hq.TEXTURES}
    out = [(rel, img) for rel, img in out if rel not in names]
    for rel, make in hq.TEXTURES:
        out.append(save(make(), rel))
    # Animated textures: the frame strip replaces the still, plus the .mcmeta that animates it.
    for rel, make, frametime in hq.ANIMATED:
        save(make(), rel)
        (TEX / (rel + ".mcmeta")).write_text(json.dumps({"animation": {"frametime": frametime}}, indent=2) + "\n")
    # (Upgrade accents, worn-armour overlays and GUI sheets aren't 16x16 tiles: they stay out of the sheets.)
    write_preview([(rel, img) for rel, img in out if not rel.startswith(("item/starforge/", "entity/", "gui/"))])
    write_tiled_preview(hq.TEXTURES)
    print(f"Wrote {len(out)} textures to {TEX.relative_to(ROOT)} and a preview to {PREVIEW.relative_to(ROOT)}")


def write_tiled_preview(textures):
    """Each block texture tiled 3x3, to show seams and repeats (docs/images/textures_tiled.png)."""
    from PIL import ImageDraw
    from pixelart import tile3
    blocks = [(rel, make()) for rel, make in textures if rel.startswith("block/")]
    cols, cell = 6, 16 * 3 * 3 + 24
    sheet = Image.new("RGBA", (cols * cell, ((len(blocks) + cols - 1) // cols) * cell), (40, 40, 48, 255))
    draw = ImageDraw.Draw(sheet)
    for i, (rel, img) in enumerate(blocks):
        x, y = (i % cols) * cell, (i // cols) * cell
        t = tile3(img, 3)
        sheet.paste(t, (x + 4, y + 4), t)
        draw.text((x + 4, y + cell - 18), rel.split("/")[-1][:-4], fill=(230, 230, 236))
    sheet.save(ROOT / "docs/images/textures_tiled.png")


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
