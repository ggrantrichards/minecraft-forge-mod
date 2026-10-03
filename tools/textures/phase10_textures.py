"""Textures for Phase 10: the Chorus Flower on a Stick and the Lodestar. Imported by hq.py (which adds
them to its TEXTURES list); run on its own to write just these:

    python3 tools/textures/phase10_textures.py
"""
from pathlib import Path

from pixelart import from_map, hexc

# ------------------------------------------------------------------ Chorus Flower on a Stick
# A fishing rod (stick from the lower left, line from its tip) with a chorus flower tied on the end.
STICK = [
    "................",
    "...........ww...",
    "..........wsdw..",
    ".........wsd.l..",
    "........wsd..l..",
    ".......wsd...l..",
    "......wsd....l..",
    ".....wsd....pPp.",
    "....wsd....pPhPp",
    "...wsd.....PhHPd",
    "..wsd......pPPpd",
    ".wsd........pdd.",
    "wsd.............",
    "sd..............",
    "................",
    "................",
]
STICK_PAL = {"w": hexc("8a6a3c"), "s": hexc("6b4f2a"), "d": hexc("3e2c14"), "l": hexc("dcdcdc"),
             "p": hexc("8a5a8e"), "P": hexc("c08ac4"), "h": hexc("e6c4e8"), "H": hexc("fbeefc")}


def chorus_flower_on_a_stick():
    return from_map(STICK, STICK_PAL)


# ------------------------------------------------------------------ Lodestar
# A lodestone banded with Meteoric Iron, with a star set in its top that the comets steer around.
LODESTAR_SIDE = [
    "iiiiiiiiiiiiiiii",
    "iLLLLLLLLLLLLLLi",
    "iLmmmmmmmmmmmmDi",
    "iDDDDDDDDDDDDDDi",
    "sSSSSsSSSSSsSSSs",
    "sSttSsStttSsSttd",
    "sSttSdStttSdSttd",
    "ddddddddddddddd.",
    "iiiiiiiiiiiiiiii",
    "iLLLLLLLLLLLLLLi",
    "iLmmmmvmmvmmmmDi",
    "iDDDDDDDDDDDDDDi",
    "sSSSsSSSSSSsSSSs",
    "sStSsSttSStsSttd",
    "sSSSdSSSSSSdSSSd",
    "dddddddddddddddd",
]
LODESTAR_TOP = [
    "iiiiiiiiiiiiiiii",
    "iLLLLLLLLLLLLLDi",
    "iLmmmmmmmmmmmmDi",
    "iLmDDDDvvDDDDmDi",
    "iLmDdddvvdddDmDi",
    "iLmDddvVVvddDmDi",
    "iLmDdvVWWVvdDmDi",
    "iLmvvVWWWWVvvmDi",
    "iLmvvVWWWWVvvmDi",
    "iLmDdvVWWVvdDmDi",
    "iLmDddvVVvddDmDi",
    "iLmDdddvvdddDmDi",
    "iLmDDDDvvDDDDmDi",
    "iLmmmmmmmmmmmmDi",
    "iDDDDDDDDDDDDDDi",
    "iiiiiiiiiiiiiiii",
]
LODESTAR_PAL = {"i": hexc("2b2e36"), "L": hexc("a9b0c0"), "m": hexc("6c7282"), "D": hexc("454a56"),
                "s": hexc("5e5e63"), "S": hexc("7d7d83"), "t": hexc("9a9aa1"), "d": hexc("404044"),
                "v": hexc("6a48b8"), "V": hexc("a682f0"), "W": hexc("efe4ff")}


def lodestar_side():
    return from_map(LODESTAR_SIDE, LODESTAR_PAL)


def lodestar_top():
    return from_map(LODESTAR_TOP, LODESTAR_PAL)


TEXTURES = [
    ("item/chorus_flower_on_a_stick.png", chorus_flower_on_a_stick),
    ("block/lodestar_side.png", lodestar_side),
    ("block/lodestar_top.png", lodestar_top),
]


# ------------------------------------------------------------------ Batch C: the Chorus Thicket
# Chorus Wood: pale, glassy magenta, like a chorus fruit's flesh grown into timber.
from pixelart import CLEAR, SIZE, new, put, ramp  # noqa: E402

CHORUS_BARK = ramp("5a2a62", "d6b0dc", 5, hue_shift=0.02)
CHORUS_PLANK = ramp("8a5a92", "f4e2f6", 5, hue_shift=0.03)
CHORUS_GLOW = [None, hexc("fbeefc"), hexc("ffffff")]
CHORUS_BLOOM = {"d": hexc("4a1c54"), "s": hexc("7a3a86"), "m": hexc("a866b2"), "l": hexc("d39ad8"),
                "h": hexc("f6d6f8")}


def _hq():
    import hq
    return hq


def chorus_stalk():
    return _hq().bark(301, CHORUS_BARK, CHORUS_GLOW, 4)


def chorus_stalk_top():
    return _hq().log_top(CHORUS_BARK[1], [CHORUS_PLANK[3], CHORUS_PLANK[2], CHORUS_PLANK[2], CHORUS_PLANK[1]], CHORUS_PLANK[4])


def stripped_chorus_stalk():
    return _hq().bark(302, CHORUS_PLANK)


def stripped_chorus_stalk_top():
    return _hq().log_top(CHORUS_PLANK[1], [CHORUS_PLANK[3], CHORUS_PLANK[2], CHORUS_PLANK[2], CHORUS_PLANK[1]], CHORUS_PLANK[4])


def chorus_planks():
    return _hq().planks(303, CHORUS_PLANK)


def chorus_bloom():
    from pixelart import leaves
    return leaves(304, CHORUS_BLOOM, holes=0.12, count=50)


def _door(top):
    import make_textures as mt
    return mt.door_tex(CHORUS_PLANK, CHORUS_BARK[0], CHORUS_BLOOM["h"] if top else CHORUS_BARK[0], top)


def chorus_door_item():
    from PIL import Image
    icon = new()
    icon.paste(_door(1).resize((8, 8), Image.NEAREST), (4, 0))
    icon.paste(_door(0).resize((8, 8), Image.NEAREST), (4, 8))
    return icon


def chorus_trapdoor():
    import make_textures as mt
    return mt.trapdoor_tex(CHORUS_PLANK, CHORUS_BARK[0])


SPROUT = [
    "................",
    "................",
    "......hP........",
    ".....hPPp.......",
    "......pP..hP....",
    ".......s.hPPp...",
    ".......s..pP....",
    "...hP..s...s....",
    "..hPPp.s..s.....",
    "...pP..sss......",
    "....s..s........",
    ".....sss........",
    ".......s........",
    ".......s........",
    ".......d........",
    "................",
]


def chorus_sprout():
    return from_map(SPROUT, {"s": CHORUS_BARK[2], "d": CHORUS_BARK[0], "p": CHORUS_BLOOM["s"],
                             "P": CHORUS_BLOOM["l"], "h": CHORUS_BLOOM["h"]})


# ------------------------------------------------------------------ Batch C: the Glimmering Crags
GLIMMER = ramp("0b3436", "c4fff6", 7, hue_shift=-0.03)


def glimmer_crystal():
    return _hq().crystal_block(GLIMMER, 311)


CLUSTER = [
    "................",
    "................",
    ".......h........",
    "......hl........",
    "......hlm...h...",
    "..h...hlm..hl...",
    "..hl..lmm..lm...",
    "..lm..lmd.hlm...",
    "..lmd.lmd.lmd...",
    "...md.lmd.lmd...",
    "...md.lmd.md..h.",
    ".h.mdhlmdlmd.hl.",
    ".lm.dlmmdlmd.lm.",
    ".lmddlmmdlmddmd.",
    "..mddmmmdmmddd..",
    "................",
]


def glimmer_cluster():
    return from_map(CLUSTER, {"d": GLIMMER[1], "m": GLIMMER[3], "l": GLIMMER[5], "h": GLIMMER[6]})


# ------------------------------------------------------------------ Batch C: Voidgel
def voidgel():
    """A slab of dark violet jelly: a lighter rim, a darker inner cube, a few motes caught inside."""
    img = new()
    rim, body, core, mote = hexc("8a5ad8", 210), hexc("4a2c8a", 170), hexc("2a1658", 190), hexc("e2d4ff", 230)
    for y in range(SIZE):
        for x in range(SIZE):
            edge = min(x, y, SIZE - 1 - x, SIZE - 1 - y)
            c = rim if edge == 0 else body
            if 3 <= x <= 12 and 3 <= y <= 12:
                c = core if min(x - 3, y - 3, 12 - x, 12 - y) == 0 else body
            put(img, x, y, c)
    for (x, y) in ((5, 6), (9, 4), (10, 10), (6, 11)):
        put(img, x, y, mote)
    put(img, 1, 1, hexc("f0e8ff", 230))
    return img


# ------------------------------------------------------------------ Batch C: End Chimes
# Hung from a chain: End Rods of different lengths with amethyst shards between them.
CHIMES = [
    ".......cc.......",
    ".......cc.......",
    "...rrrrrrrrrr...",
    "...RRRRRRRRRR...",
    "...w..w..w..w...",
    "...w..w..w..w...",
    "...W..w..W..w...",
    "...w.aW..w.aW...",
    "...W.Aw..W.Aw...",
    "...w.a...w.aW...",
    "...W.....W..w...",
    "...w.....w..W...",
    ".........W......",
    ".........w......",
    "................",
    "................",
]


def end_chimes():
    return from_map(CHIMES, {"c": hexc("3a3a44"), "r": hexc("a679a6"), "R": hexc("6b436b"), "w": hexc("f4f0e8"),
                             "W": hexc("cfc6b8"), "a": hexc("8a62c8"), "A": hexc("cfa8ff")})


TEXTURES += [
    ("block/chorus_stalk.png", chorus_stalk),
    ("block/chorus_stalk_top.png", chorus_stalk_top),
    ("block/stripped_chorus_stalk.png", stripped_chorus_stalk),
    ("block/stripped_chorus_stalk_top.png", stripped_chorus_stalk_top),
    ("block/chorus_planks.png", chorus_planks),
    ("block/chorus_bloom.png", chorus_bloom),
    ("block/chorus_door_top.png", lambda: _door(1)),
    ("block/chorus_door_bottom.png", lambda: _door(0)),
    ("item/chorus_door.png", chorus_door_item),
    ("block/chorus_trapdoor.png", chorus_trapdoor),
    ("block/chorus_sprout.png", chorus_sprout),
    ("block/glimmer_crystal.png", glimmer_crystal),
    ("block/glimmer_cluster.png", glimmer_cluster),
    ("block/voidgel.png", voidgel),
    ("block/end_chimes.png", end_chimes),
]


# ------------------------------------------------------------------ Batch C: Upwell Mote and Upwelling
MOTE = [
    "................",
    ".......w........",
    "......wWw.......",
    ".....wWHWw......",
    "....w.lHl.w.....",
    ".......l........",
    "......vlv.......",
    ".....v.l.v......",
    ".......l........",
    "......vlv.......",
    ".....v.m.v......",
    ".......m........",
    "......m.m.......",
    ".......m........",
    "................",
    "................",
]


def upwell_mote():
    return from_map(MOTE, {"w": hexc("8fe8dc"), "W": hexc("c8fff4"), "H": hexc("ffffff"), "l": hexc("7fd0e8"),
                           "v": hexc("9a7ae8"), "m": hexc("5a3ab0")})


def upwelling_icon():
    """The effect icon (18x18): three chevrons rising."""
    from PIL import Image
    img = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    px = img.load()
    for i, (y0, c) in enumerate(((3, hexc("e6fffb")), (8, hexc("8fe8dc")), (13, hexc("5fb8c8")))):
        for d in range(6):
            for w in (0, 1):
                y = y0 + d + w - 1
                if 0 <= y < 18:
                    px[8 - d, y] = c
                    px[9 + d, y] = c
    return img


TEXTURES += [
    ("item/upwell_mote.png", upwell_mote),
    ("mob_effect/upwelling.png", upwelling_icon),
]


# ------------------------------------------------------------------ Batch C: the Resonance Vault's treasures
# Armour trim textures are greyscale in the trim palette's key (224, 192, 160, 128...) and get
# recoloured per material by the game (the .mcmeta names the palette).
from PIL import Image  # noqa: E402

TRIM_KEY = [224, 192, 160, 128, 96]


def _grey(img, x, y, i):
    v = TRIM_KEY[i]
    img.putpixel((x, y), (v, v, v, 255))


def eclipse_trim():
    """The Eclipse trim (humanoid layer): a ring with its dark disc across the chest, a crescent on the
    brow, bands round the arms and boots."""
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    # Chest: the eclipse - a ring, brighter on the upper left, at the middle of the body's front (20..27, 20..31).
    cx, cy = 23.5, 24.5
    for x in range(20, 28):
        for y in range(20, 30):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if 2.2 <= d <= 3.3:
                _grey(img, x, y, 0 if (x < cx and y < cy) else 1 if x < cx or y < cy else 2)
    for x in range(20, 28):        # the hem
        _grey(img, x, 31, 2)
    for x in (16, 17, 18, 19, 28, 29, 30, 31):   # the sides of the hem
        _grey(img, x, 31, 3)
    # Brow: a crescent across the helmet's front (8..15, 8..15).
    for x, y, i in ((9, 9, 1), (10, 10, 0), (11, 10, 0), (12, 10, 0), (13, 10, 0), (14, 9, 1), (10, 9, 2), (13, 9, 2)):
        _grey(img, x, y, i)
    # Arms: a band round each arm near the shoulder (front 44..47, sides 40..43 and 48..51, back 52..55).
    for x in range(40, 56):
        _grey(img, x, 22, 1 if 44 <= x < 48 else 2)
    # Boots: a band near the bottom of the legs (front 4..7, sides 0..3 and 8..11, back 12..15).
    for x in range(0, 16):
        _grey(img, x, 29, 1 if 4 <= x < 8 else 2)
        if 4 <= x < 8:
            _grey(img, x, 30, 3)
    return img


def eclipse_trim_leggings():
    """The Eclipse trim (leggings layer): a belt with a small ring at the buckle, and rings at the knees."""
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    for x in range(16, 40):
        _grey(img, x, 27, 1 if 20 <= x < 28 else 2)
    for (x, y, i) in ((23, 26, 0), (24, 26, 0), (22, 27, 0), (25, 27, 0), (23, 28, 1), (24, 28, 1)):
        _grey(img, x, y, i)
    for x in range(0, 16):
        _grey(img, x, 25, 1 if 4 <= x < 8 else 3)
    return img


def eclipse_template():
    """The Eclipse armour trim template: a tablet of Nightstone with a teal eclipse cut into it."""
    rows = [
        "................",
        "......oooo......",
        "....oossssoo....",
        "...ossmmmmsso...",
        "..osmmddddmmso..",
        "..osmdTTTTdmso..",
        ".osmdTtkktTdmso.",
        ".osmdTkkkkTdmso.",
        ".osmdTkkkkTdmso.",
        ".osmdTtkktTdmso.",
        "..osmdTTTTdmso..",
        "..osmmddddmmso..",
        "...ossmmmmsso...",
        "....oossssoo....",
        "......oooo......",
        "................",
    ]
    return from_map(rows, {"o": hexc("120e1a"), "s": hexc("3a3450"), "m": hexc("2a2440"), "d": hexc("1c1830"),
                           "T": hexc("5fe0d0"), "t": hexc("bffff4"), "k": hexc("0a0810")})


def _scroll(glyph):
    """A banner pattern item: a rolled parchment with the pattern's glyph on it, like vanilla's."""
    base = [
        "................",
        "..hhhhhhhhhhhh..",
        ".hrrrrrrrrrrrrh.",
        "..pppppppppppp..",
        "..pPPPPPPPPPPp..",
        "..pPPPPPPPPPPp..",
        "..pPPPPPPPPPPp..",
        "..pPPPPPPPPPPp..",
        "..pPPPPPPPPPPp..",
        "..pPPPPPPPPPPp..",
        "..pPPPPPPPPPPp..",
        "..pPPPPPPPPPPp..",
        "..pPPPPPPPPPPp..",
        "..pppppppppppp..",
        ".hrrrrrrrrrrrrh.",
        "..hhhhhhhhhhhh..",
    ]
    img = from_map(base, {"h": hexc("5a3a24"), "r": hexc("8a5a34"), "p": hexc("c8bca0"), "P": hexc("ece4cc")})
    for y, row in enumerate(glyph):
        for x, ch in enumerate(row):
            if ch == "#":
                put(img, 4 + x, 4 + y, hexc("3a2a50"))
    return img


WYRM_GLYPH = ["..####..", ".#....#.", ".#.##...", ".#..#...", "..##.#..", ".....#..", "...##..."]
SPIRE_GLYPH = ["...##...", "...##...", "..####..", "..#..#..", "..####..", ".##..##.", "########"]


def _banner(mask, shield=False):
    """A banner (64x64) or shield (64x64) pattern: white where the pattern is, on the flag's front and
    back faces. `mask` is drawn scaled to the face."""
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    fw, fh = (12, 22) if shield else (20, 40)
    faces = [(1, 1)] if shield else [(1, 1), (22, 1)]
    mh, mw = len(mask), len(mask[0])
    for ox, oy in faces:
        for y in range(fh):
            for x in range(fw):
                if mask[y * mh // fh][x * mw // fw] == "#":
                    img.putpixel((ox + x, oy + y), (255, 255, 255, 255))
    return img


# Banner masks, 10 wide x 20 tall: a serpent coiled down the banner; a tower with a bell in it.
WYRM_MASK = [
    "...####...", "..#....#..", "..#.##....", "...#..#...", "....#..#..", ".....#.#..", "...##..#..",
    "..#...#...", "..#..#....", "...##.....", "....#.....", "...#..##..", "..#..#..#.", "..#.#...#.",
    "...#...#..", "....###...", "..........", "..........", "..........", "..........",
]
SPIRE_MASK = [
    "....##....", "....##....", "...####...", "...#..#...", "...####...", "...#..#...", "...#..#...",
    "...#.##...", "..##..##..", "..#....#..", "..#.##.#..", "..#.##.#..", "..#....#..", ".##....##.",
    ".#..##..#.", ".#..##..#.", ".#......#.", "##########", "##########", "..........",
]


TEXTURES += [
    ("trims/entity/humanoid/eclipse.png", eclipse_trim),
    ("trims/entity/humanoid_leggings/eclipse.png", eclipse_trim_leggings),
    ("item/eclipse_armor_trim_smithing_template.png", eclipse_template),
    ("item/wyrm_banner_pattern.png", lambda: _scroll(WYRM_GLYPH)),
    ("item/spire_banner_pattern.png", lambda: _scroll(SPIRE_GLYPH)),
    ("entity/banner/wyrm.png", lambda: _banner(WYRM_MASK)),
    ("entity/banner/spire.png", lambda: _banner(SPIRE_MASK)),
    ("entity/shield/wyrm.png", lambda: _banner(WYRM_MASK, shield=True)),
    ("entity/shield/spire.png", lambda: _banner(SPIRE_MASK, shield=True)),
]
# Trim textures name the palette they're recoloured with.
MCMETA = {
    "trims/entity/humanoid/eclipse.png": {"palette": {"base_palette": "trim_base"}},
    "trims/entity/humanoid_leggings/eclipse.png": {"palette": {"base_palette": "trim_base"}},
}


# The Resonant Key: a key of Glimmer Crystal with a bell-shaped bow.
KEY = [
    "................",
    "....hHHh........",
    "...hTttTh.......",
    "..hTt..tTh......",
    "..hT....Th......",
    "..hTt..tTh......",
    "...hTttTh.......",
    "....hTTh........",
    ".....hTh........",
    ".....hTThhh.....",
    ".....hTTTTTh....",
    ".....hTh.hTh....",
    ".....hTThhh.....",
    ".....hTTTh......",
    "......hhh.......",
    "................",
]


def resonant_key():
    return from_map(KEY, {"h": hexc("0e3a3c"), "H": hexc("1e6a68"), "T": hexc("3fb8b0"), "t": hexc("c8fff6")})


TEXTURES += [("item/resonant_key.png", resonant_key)]


# The music disc: a black disc with a teal label and a star on it.
DISC = [
    "................",
    ".....kkkkkk.....",
    "...kkgkkkkgkk...",
    "..kgkkkkkkkkgk..",
    "..kkkkTTTTkkkk..",
    ".kkkkTTwwTTkkkk.",
    ".kgkTTwWWwTTkgk.",
    ".kkkTwWkkWwTkkk.",
    ".kkkTwWkkWwTkkk.",
    ".kgkTTwWWwTTkgk.",
    ".kkkkTTwwTTkkkk.",
    "..kkkkTTTTkkkk..",
    "..kgkkkkkkkkgk..",
    "...kkgkkkkgkk...",
    ".....kkkkkk.....",
    "................",
]


def music_disc():
    return from_map(DISC, {"k": hexc("15131c"), "g": hexc("3a3648"), "T": hexc("2fa89e"), "w": hexc("8ff0e4"),
                           "W": hexc("e8fffb")})


TEXTURES += [("item/music_disc_observing_the_star.png", music_disc)]


# The commonest blocks, redrawn closer to vanilla (retexture.py); last, so they replace older drawings.
from retexture import TEXTURES as _RETEXTURE  # noqa: E402
TEXTURES += _RETEXTURE


if __name__ == "__main__":
    out = Path(__file__).resolve().parents[2] / "src/main/resources/assets/astralend/textures"
    import json
    for rel, make in TEXTURES:
        (out / rel).parent.mkdir(parents=True, exist_ok=True)
        make().save(out / rel)
        print("wrote", rel)
    for rel, meta in MCMETA.items():
        (out / (rel + ".mcmeta")).write_text(json.dumps(meta, indent=2) + "\n")
