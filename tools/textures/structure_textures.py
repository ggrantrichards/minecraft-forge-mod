"""Textures for Phase 6 (structures): the Temporal Crystal. Imported by hq.py, which adds them to its
TEXTURES list."""
from pixelart import from_map, hexc

# ------------------------------------------------------------------ Temporal Crystal
# A pale, double-ended crystal pinched at the waist like an hourglass, with gold "sand" of light
# trickling from the top half into the bottom. Lit from the upper left like the other crystals.
TEMPORAL = [
    "................",
    "....oooooooo....",
    "...ohhllllmdo...",
    "...ohlYYYlmdo...",
    "....ohlyymdo....",
    ".....ohymdo.....",
    "......oYdo......",
    ".......oo....w..",
    "......ohYo..wyw.",
    ".....ohlYmo..w..",
    "....ohllYmdo....",
    "...ohlyYYymdo...",
    "...ohyYYyyydo...",
    "...osssssssdo...",
    "....oooooooo....",
    "................",
]
TEMPORAL_PAL = {"o": hexc("163a44"), "d": hexc("2c6670"), "m": hexc("4f9aa2"), "l": hexc("8fd2cc"),
                "h": hexc("d8f6ee"), "y": hexc("e2b04a"), "Y": hexc("ffe49a"), "s": hexc("a87826"),
                "w": hexc("fff4c8")}


def temporal_crystal():
    return from_map(TEMPORAL, TEMPORAL_PAL)


TEXTURES = [
    ("item/temporal_crystal.png", temporal_crystal),
]
