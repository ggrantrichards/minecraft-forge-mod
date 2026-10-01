"""Textures for Phase 5 (Stellar Flares): the Flarebloom shut and open, charged Sunscale Lichen,
the Sunshade, the Collector Pedestal, flare cells, Charged Void Crystal, Flare Pollen and the Flare
Ward effect icon. Imported by hq.py, which adds them to its TEXTURES list."""
import math
from pathlib import Path

from PIL import Image

from pixelart import SIZE, from_map, hexc, new, put, ramp, rock, shade


# ------------------------------------------------------------------ Flarebloom, shut
# Between flares the bloom is a tight, dark-red bud; a seam of orange light shows where it will open.
BUD = [
    "................",
    "........o.......",
    ".......oFo......",
    "......orFdo.....",
    "......rRFrd.....",
    ".....orRfrdo....",
    ".....rRRfrrd....",
    ".....rRRfrrd....",
    ".....rRRfrrd....",
    ".....orRfrdo....",
    "....l.orfdo.l...",
    "...ll..oSo..ll..",
    "..l....sS....l..",
    ".......sS.......",
    ".......sS.......",
    ".......sS.......",
]
BUD_PAL = {"o": hexc("3a0e08"), "d": hexc("5e1a0e"), "r": hexc("8e2414"), "R": hexc("b8381c"),
           "f": hexc("e4621e"), "F": hexc("ffae40"), "s": hexc("3d1f14"), "S": hexc("5e3020"),
           "l": hexc("b23c1a")}


def flarebloom_shut():
    return from_map(BUD, BUD_PAL)


# ------------------------------------------------------------------ Sunscale Lichen, charged
def lichen(charged):
    """Round crusts of lichen; charged by a flare they burn bright gold for a while."""
    from pixelart import fbm
    img = new()
    wobble = fbm(161, ((4, 0.6), (8, 0.4)))
    if charged:
        rim, outer, inner, bright, heart = hexc("c2561c"), hexc("f5962e"), hexc("ffcf5e"), hexc("fff4b8"), hexc("ffe07a")
    else:
        rim, outer, inner, bright, heart = hexc("8e3a16"), hexc("cf6a22"), hexc("f2a23c"), hexc("ffd774"), hexc("b8742a")
    for (cx, cy, r) in ((3.5, 3.5, 3.4), (11.5, 10.5, 3.8), (12.0, 2.5, 2.0), (3.0, 12.0, 2.3), (8.0, 6.5, 1.4),
                        (15.0, 15.0, 1.2)):
        for y in range(SIZE):
            for x in range(SIZE):
                dx = min(abs(x + 0.5 - cx), SIZE - abs(x + 0.5 - cx))
                dy = min(abs(y + 0.5 - cy), SIZE - abs(y + 0.5 - cy))
                d = math.hypot(dx, dy) / (r * (0.8 + 0.4 * wobble[y][x]))
                if d > 1.0:
                    continue
                if d > 0.78:
                    c = rim
                elif d > 0.55:
                    c = outer
                elif d > 0.3 or r < 2.2:
                    c = inner if (x + y) % 3 else bright
                else:
                    c = heart
                put(img, x, y, c)
    return img


# ------------------------------------------------------------------ Sunshade
# A square of Skimmer Film stretched on a bronze frame, like a parasol: four translucent amber panels
# with the wing's veins still in them.
def sunshade():
    img = new()
    rib_d, rib, rib_l = hexc("5a3614"), hexc("8e5e2a"), hexc("c08a46")
    film = [hexc("d8762a", 200), hexc("e8913a", 200), hexc("f4ac52", 200)]
    vein = hexc("b85a1c", 215)
    for y in range(SIZE):
        for x in range(SIZE):
            if x in (0, 15) or y in (0, 15):
                c = rib_l if (x == 0 or y == 0) else rib_d
            elif x in (7, 8) or y in (7, 8):
                c = rib_l if (x == 7 or y == 7) else rib
            else:
                # panel-local coordinates, veins fanning out from the frame's middle
                px, py = (x - 1) % 7, (y - 1) % 7
                if (px + py) % 4 == 0 and 0 < px < 6:
                    c = vein
                else:
                    c = film[1] if (px * 3 + py) % 7 else film[2]
                    if px >= 5 or py >= 5:
                        c = film[0]
            put(img, x, y, c)
    return img


# ------------------------------------------------------------------ Collector Pedestal
SUN_STONE = ramp("8e6e44", "f1e2c2", 5, hue_shift=0.03)
GOLD = [hexc("6e3208"), hexc("b0620e"), hexc("e8971c"), hexc("ffcf52"), hexc("fff3b8")]


def pedestal_side():
    """Polished Sunscorched Stone with a solarite band at the top and a sun emblem in the middle."""
    img = rock(311, SUN_STONE[1:4], shares=[1, 5, 2], octaves=((4, 0.5), (8, 0.5)))
    for x in range(SIZE):
        put(img, x, 0, GOLD[4])
        put(img, x, 1, GOLD[3])
        put(img, x, 2, GOLD[2])
        put(img, x, 3, GOLD[0])
        put(img, x, 14, SUN_STONE[0])
        put(img, x, 15, shade(SUN_STONE[0], -0.2))
    for y in range(4, 14):
        put(img, 0, y, SUN_STONE[4])
        put(img, 15, y, SUN_STONE[0])
    emblem = [
        "...y...",
        ".y.y.y.",
        "..ooo..",
        "yyoGoyy",
        "..ooo..",
        ".y.y.y.",
        "...y...",
    ]
    pal = {"y": GOLD[2], "o": GOLD[1], "G": GOLD[4]}
    for dy, row in enumerate(emblem):
        for dx, ch in enumerate(row):
            if ch != ".":
                put(img, 5 + dx, 5 + dy, pal[ch])
    return img


def pedestal_top():
    """The top: a solarite ring round a dark socket where the crystal or cell sits."""
    img = rock(312, SUN_STONE[1:4], shares=[1, 5, 2], octaves=((4, 0.5), (8, 0.5)))
    for y in range(SIZE):
        for x in range(SIZE):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 2.6:
                put(img, x, y, hexc("3a1a08"))
            elif d < 3.6:
                put(img, x, y, GOLD[1])
            elif d < 4.6:
                put(img, x, y, GOLD[3] if (x + y) % 2 else GOLD[2])
            elif x in (0, 15) or y in (0, 15):
                put(img, x, y, GOLD[2] if x == 0 or y == 0 else GOLD[1])
    return img


# ------------------------------------------------------------------ items
CELL = [
    "................",
    "....oooooooo....",
    "....oBBBbbbo....",
    "....obbbbbDo....",
    ".....oooooo.....",
    ".....oHggGo.....",
    ".....oHggGo.....",
    ".....oHggGo.....",
    ".....oHggGo.....",
    ".....oHggGo.....",
    ".....oHggGo.....",
    ".....oooooo.....",
    "....oBBBbbbo....",
    "....obbbbbDo....",
    "....oooooooo....",
    "................",
]
CELL_METAL = {"o": hexc("2a1a10"), "B": hexc("e8b060"), "b": hexc("b07a34"), "D": hexc("6e4a1e"), "H": hexc("dff0ff")}


FLASK = [
    "................",
    "......oooo......",
    "......oBDo......",
    ".....oBBbDo.....",
    "......ohgo......",
    "......ohgo......",
    ".....ohgggo.....",
    "....ohhgggGo....",
    "...ohhggggGGo...",
    "...ohgggggGGo...",
    "...ogggggggGo...",
    "...ogggggGGGo...",
    "....oggGGGGo....",
    ".....oooooo.....",
    "................",
    "................",
]


def flare_bottle(charged):
    """A Flare Bottle: a round glass flask with a Solarite stopper - clear and empty, or, once a
    Stellar Flare has filled it on a Collector Pedestal, a Bottled Flare swirling with gold light."""
    pal = {"o": hexc("2a1a10"), "B": hexc("ffd36a"), "b": hexc("e0a030"), "D": hexc("8a5a18")}
    if charged:
        pal.update(g=hexc("ffb040"), G=hexc("e4621e"), h=hexc("fff4c0"))
        img = from_map(FLASK, pal)
        for x, y in ((7, 9), (8, 10), (6, 11), (9, 8)):
            put(img, x, y, hexc("fff8d8"))                  # sparks of the flare inside
        return img
    pal.update(g=hexc("b8cce0", 150), G=hexc("7f95b0", 170), h=hexc("f0f8ff", 200))
    return from_map(FLASK, pal)


CRYSTAL = [
    "..............w.",
    "........o....wyw",
    ".......oho....w.",
    "......ohlmo.....",
    ".....ohllmdo....",
    "....ohlllmmdo...",
    "...ohhlccmmddo..",
    "..ohhwlcCcmddo..",
    "..olhllccmdddo..",
    "...olllmmmddo...",
    "....ollmmddo....",
    ".w...olmmdo.....",
    "wyw...omdo......",
    ".w.....oo.......",
    "................",
    "................",
]


def charged_void_crystal():
    """Void Crystal after a flare: the violet shell holds a core of gold light, and it sparks."""
    pal = {"o": hexc("190b31"), "d": hexc("34196a"), "m": hexc("5a33b3"), "l": hexc("8a66ea"),
           "h": hexc("ffe6a0"), "w": hexc("ffffff"), "y": hexc("ffcf52"), "c": hexc("ffb030"), "C": hexc("fff4c0")}
    return from_map(CRYSTAL, pal)


POLLEN = [
    "................",
    "................",
    "..........w.....",
    ".........wyw....",
    "....w.....w.....",
    "...wyw......y...",
    "....w...........",
    "......oo....w...",
    ".....oYyo.......",
    "...ooYyyyoo.....",
    "..oYyyYyyyyo....",
    ".oYyyyyYyyyyo...",
    ".oyyyyyyyyyydo..",
    "..oddddddddddo..",
    "...oooooooooo...",
    "................",
]


def flare_pollen():
    return from_map(POLLEN, {"o": hexc("7a3a0c"), "d": hexc("b8641a"), "y": hexc("f0a830"), "Y": hexc("ffd86a"),
                             "w": hexc("fff6c8")})


WARD = [
    "..................",
    "........y.........",
    "...y....y....y....",
    "....y.......y.....",
    ".......ooooo......",
    "y....ooAAAAaoo...y",
    "....oAAWAAAAaao...",
    "...oAAWAAAAAAaao..",
    "..oAAAAAAAAAAAaao.",
    "..odddddddddddddo.",
    "...oo.oo.o.oo.oo..",
    "........oB........",
    "........oB........",
    "........oB........",
    "........oB........",
    "........oB........",
    "......oBBo........",
    ".......oo.........",
]


def flare_ward_icon():
    """The effect icon (18x18): a Skimmer-Film parasol with the flare's rays held off above it."""
    pal = {"o": hexc("5a2a0c"), "A": hexc("f4ac52"), "a": hexc("d8762a"), "W": hexc("fff0c8"), "d": hexc("9a4a16"),
           "B": hexc("c08a46"), "y": hexc("ffd24a")}
    img = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    for y, row in enumerate(WARD):
        assert len(row) == 18, (y, row)
        for x, ch in enumerate(row):
            if ch != ".":
                img.putpixel((x, y), pal[ch])
    return img


TEXTURES = [
    ("block/flarebloom_top.png", flarebloom_shut),
    ("block/sunscale_lichen_charged.png", lambda: lichen(True)),
    ("block/sunshade.png", sunshade),
    ("block/collector_pedestal_side.png", pedestal_side),
    ("block/collector_pedestal_top.png", pedestal_top),
    ("item/flare_bottle.png", lambda: flare_bottle(False)),
    ("item/bottled_flare.png", lambda: flare_bottle(True)),
    ("item/charged_void_crystal.png", charged_void_crystal),
    ("item/flare_pollen.png", flare_pollen),
    ("mob_effect/flare_ward.png", flare_ward_icon),
]

# What a Collector Pedestal holds is drawn on top of it with the item's own picture; block models can
# only use block textures, so the item pictures are copied (after they are drawn above).
_TEX = Path(__file__).resolve().parents[2] / "src/main/resources/assets/astralend/textures"
for _name in ("void_crystal", "charged_void_crystal", "flare_bottle", "bottled_flare"):
    TEXTURES.append((f"block/pedestal_{_name}.png", lambda n=_name: Image.open(_TEX / f"item/{n}.png").convert("RGBA").copy()))
