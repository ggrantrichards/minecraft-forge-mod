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


if __name__ == "__main__":
    out = Path(__file__).resolve().parents[2] / "src/main/resources/assets/astralend/textures"
    for rel, make in TEXTURES:
        make().save(out / rel)
        print("wrote", rel)
