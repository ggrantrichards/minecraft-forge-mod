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


if __name__ == "__main__":
    out = Path(__file__).resolve().parents[2] / "src/main/resources/assets/astralend/textures"
    for rel, make in TEXTURES:
        make().save(out / rel)
        print("wrote", rel)
