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


# ------------------------------------------------------------------ Batch C: the Rift Maw and the Constellation Maze
import math  # noqa: E402
import random  # noqa: E402

from PIL import Image  # noqa: E402

from pixelart import SIZE, fbm, mix, new, put, quantize, ramp, rock  # noqa: E402

BONE = [hexc("7c8891"), hexc("9aa6ad"), hexc("b6c0c5"), hexc("cdd5d8"), hexc("e0e6e8")]


def _socket(lit):
    """A Maw's eye socket: a thick rim of Leviathan Bone, bevelled inward (lit from the top-left),
    around a deep, dark hollow - or, once woken, a Charged Void Crystal glowing in it."""
    img = new()
    glow = ramp("2a1252", "e9c8ff", 5, hue_shift=-0.05)
    for y in range(SIZE):
        for x in range(SIZE):
            dx, dy = x - 7.5, y - 7.5
            r = math.hypot(dx, dy * 1.08)
            grain = [BONE[2], BONE[3], BONE[3], BONE[2], BONE[1]][(x + (y // 5)) % 5]
            if r > 7.2:
                c = BONE[1] if (x in (0, 15) or y in (0, 15)) else grain
            elif r > 6.2:
                c = grain
            elif r > 4.6:                              # the bevel: light on the lower right inside edge
                facing = (dx + dy) / max(r, 0.01)
                c = BONE[4] if facing > 0.55 else BONE[0] if facing < -0.4 else BONE[2]
                if lit and facing > -0.4:
                    c = mix(c, glow[2], 0.35)
            else:                                      # the hollow
                depth = 1 - r / 4.6
                if lit:
                    c = glow[min(4, int(depth * 4.2) + (1 if dx + dy < -1 else 0))]
                else:
                    c = mix(hexc("2b2533"), hexc("0b0910"), depth)
            put(img, x, y, c)
    if lit:   # the crystal's facets and its gold heart, like the Charged Void Crystal item
        for x, y, c in [(7, 5, "f6ecff"), (6, 6, "f6ecff"), (8, 7, "ffe6a0"), (7, 8, "ffcf52"), (8, 8, "fff4c0"),
                        (9, 9, "8a66ea"), (6, 9, "5a33b3"), (9, 6, "b89af4")]:
            put(img, x, y, hexc(c))
    else:     # a little grit at the bottom of the hollow
        for x, y in [(6, 9), (9, 8), (8, 10)]:
            put(img, x, y, hexc("3d3546"))
    return img


def maw_eye():
    return _socket(False)


def maw_eye_lit():
    return _socket(True)


def maw_throat():
    """The dormant throat: smooth, very dark stone with faint violet veins curling through it."""
    pal = [hexc("0d0a14"), hexc("16111f"), hexc("1f182b"), hexc("2a2139"), hexc("362a4a")]
    img = rock(71, pal, shares=[0.12, 0.28, 0.3, 0.2, 0.1])
    veins = fbm(72, octaves=((2, 0.6), (4, 0.4)))
    for y in range(SIZE):
        for x in range(SIZE):
            v = veins[y][x]
            if 0.47 < v < 0.53:
                put(img, x, y, hexc("4b2f78"))
            elif 0.45 < v < 0.55 and (x + y) % 2 == 0:
                put(img, x, y, hexc("33224f"))
    return img


def wormhole_frames(n=32):
    """The woken throat: a churning starfield. Two nebula layers drift against each other and two
    layers of stars slide past at different speeds, all tiling, all looping after n frames."""
    neb_a = fbm(81, octaves=((2, 0.55), (4, 0.3), (8, 0.15)))
    neb_b = fbm(82, octaves=((2, 0.5), (4, 0.35), (8, 0.15)))
    rnd = random.Random(83)
    far = [(rnd.randrange(16), rnd.randrange(16)) for _ in range(9)]
    near = [(rnd.randrange(16), rnd.randrange(16)) for _ in range(4)]
    tones = ramp("06040f", "8a4fd8", 6, hue_shift=-0.06)
    strip = Image.new("RGBA", (SIZE, SIZE * n))
    for f in range(n):
        t = f / n
        img = new()
        ax, ay = int(round(t * 16)), int(round(t * 16))
        bx, by = int(round(-t * 16)), int(round(t * 32)) % 16
        for y in range(SIZE):
            for x in range(SIZE):
                v = neb_a[(y + ay) % 16][(x + ax) % 16] * 0.6 + neb_b[(y + by) % 16][(x + bx) % 16] * 0.55
                put(img, x, y, tones[max(0, min(5, int(v * 5.2) - 1))])
        for (sx, sy) in far:
            put(img, (sx + int(round(t * 16))) % 16, (sy - int(round(t * 16))) % 16, hexc("b9a6ff"))
        for i, (sx, sy) in enumerate(near):
            x, y = (sx - int(round(t * 32))) % 16, (sy + int(round(t * 16))) % 16
            put(img, x, y, hexc("ffffff"))
            if (f + i * 5) % 8 < 3:                     # the bright ones twinkle
                put(img, (x + 1) % 16, y, hexc("d9ccff"))
                put(img, x, (y + 1) % 16, hexc("d9ccff"))
        strip.paste(img, (0, f * SIZE))
    return strip


def wormhole():
    return wormhole_frames().crop((0, 0, SIZE, SIZE))


def star_bridge():
    """A lit star-bridge: a pane of faint starlight (translucent), its edge brighter, with a few
    stars caught in it."""
    img = new()
    for y in range(SIZE):
        for x in range(SIZE):
            edge = x in (0, 15) or y in (0, 15)
            put(img, x, y, (205, 222, 255, 150) if edge else (170, 196, 255, 70))
    for x, y in [(3, 4), (11, 3), (7, 9), (12, 12), (4, 12)]:
        put(img, x, y, (255, 255, 255, 235))
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            put(img, x + dx, y + dy, (220, 232, 255, 140))
    return img


def telescope_tube():
    """The Maze Telescope's tube: polished bronze, round (a bright line along the top, dark
    underneath), with two dark rings and riveted collars at the ends."""
    img = new()
    bronze = [hexc("2e1f12"), hexc("5a3a1c"), hexc("8c5e2a"), hexc("bf8a3e"), hexc("e8be6c"), hexc("fff0c8")]
    profile = [3, 4, 5, 4, 3, 3, 2, 2, 2, 2, 2, 1, 1, 1, 0, 0]       # top of the tube to its underside
    for y in range(SIZE):
        for x in range(SIZE):
            tone = profile[y]
            if x in (5, 10):                                          # the rings
                tone = max(0, tone - 2)
            elif x in (0, 15):                                        # the collars
                tone = max(0, tone - 1)
            put(img, x, y, bronze[tone])
    for x in (0, 15):
        for y in (3, 8, 12):
            put(img, x, y, bronze[4])                                 # rivets
    return img


PORTAL_LENS = [
    "................",
    ".....oooooo.....",
    "....obbBBbbo....",
    "...obgghhggbo...",
    "..obgghwhggbbo..",
    "..obgghhgggdbo..",
    "..obgggggggdbo..",
    "..obggggggddbo..",
    "..obgggggdddbo..",
    "...obgggdddbo...",
    "....obbbbbbo....",
    ".....oooooHo....",
    "..........oHo...",
    "...........oHo..",
    "............oo..",
    "................",
]


def portal_lens():
    """The Portal Lens: a heavy bronze ring around a disc of violet glass with a star in it."""
    return from_map(PORTAL_LENS, {"o": hexc("2a1a0c"), "b": hexc("a8792e"), "B": hexc("e6be6c"),
                                  "g": hexc("5a33b3"), "h": hexc("8a66ea"), "w": hexc("f6ecff"),
                                  "d": hexc("34196a"), "H": hexc("c49a48")})


TEXTURES += [
    ("block/maw_eye.png", maw_eye),
    ("block/maw_eye_lit.png", maw_eye_lit),
    ("block/maw_throat.png", maw_throat),
    ("block/wormhole.png", wormhole),
    ("block/star_bridge.png", star_bridge),
    ("block/telescope_tube.png", telescope_tube),
    ("item/portal_lens.png", portal_lens),
]

# Animated textures: (path, frame strip, ticks per frame). make_textures.py writes the strip and its
# .mcmeta after the still textures (the still above stands in for previews).
ANIMATED = [
    ("block/wormhole.png", wormhole_frames, 2),
]


# ------------------------------------------------------------------ Phase 7: live comets
MOTE = [
    "................",
    "................",
    "...........w....",
    "..........wyw...",
    "...........w....",
    ".......h........",
    "......hlh.......",
    "....hhlWlhh.....",
    "......hlh.......",
    ".......h........",
    "...w............",
    "..wyw.....h.....",
    "...w.....hWh....",
    "..........h.....",
    "................",
    "................",
]


def stardust_mote():
    """A Stardust Mote: a speck of comet-light, a bright four-pointed star with two smaller glints."""
    return from_map(MOTE, {"h": hexc("7fb8e8"), "l": hexc("c8e8ff"), "W": hexc("ffffff"),
                           "w": hexc("dcd4ff"), "y": hexc("fff4c8")})


TEXTURES += [("item/stardust_mote.png", stardust_mote)]
