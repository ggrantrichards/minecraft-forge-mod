"""Phase 10 retexture: the commonest blocks redrawn closer to vanilla's look - soft, low contrast, a
narrow tonal range, no bright specks - each after the vanilla block it sits beside:

  Nightstone        blackstone's chunky rock, in dark violet
  Chorus Moss       the moss block's soft mottle, in violet
  Chorus Stalk      cherry and mangrove logs' calm bark furrows, pale magenta
  Sunscorched Stone sandstone's soft, sun-bleached grain
  Charred End Stone end stone's pitted look, scorched, with a few embers
  Lumine Cap        shroomlight's glowing cells, in cool blue
  Pale End Stone    end stone's pitted grain, bleached pale

Imported by phase10_textures (its TEXTURES list goes last, so these replace the older drawings)."""
import random

from pixelart import SIZE, chunks, fbm, hexc, new, quantize, ramp


def _mottle(seed, pal, octaves=((2, 0.5), (4, 0.3), (8, 0.2)), stretch_x=1.0, shares=None):
    return quantize(fbm(seed, octaves, stretch_x), pal, shares)


# Nightstone: rounded dark lumps with a violet cast, like blackstone.
NIGHT = ramp("0c0a12", "4a405e", 6, hue_shift=0.02, sat_curve=0.06)


def nightstone():
    return chunks(4101, 14, NIGHT, base_index=2, light=1, dark=1)


# Chorus Moss: soft violet mottle, a little lighter on top of the clumps, like the moss block.
MOSS = ramp("3a2366", "9a78cc", 6, hue_shift=0.03, sat_curve=0.06)


def chorus_moss():
    img = _mottle(4201, MOSS[1:6], octaves=((8, 0.45), (16, 0.55)), shares=[1, 3, 4, 3, 1])
    rnd = random.Random(4202)
    px = img.load()
    for _ in range(5):                       # a few faint pale tips, nothing bright
        x, y = rnd.randrange(SIZE), rnd.randrange(SIZE)
        px[x, y] = MOSS[5]
    return img


# Chorus Stalk: pale magenta bark with long, soft furrows, like cherry and mangrove logs.
STALK = ramp("5a2c62", "c79acc", 5, hue_shift=0.02, sat_curve=0.05)


def chorus_stalk():
    import hq
    return hq.bark(4301, STALK[0:5])


# Sunscorched Stone: pale, sun-bleached sandstone grain.
SUN = ramp("b88a52", "f4dfae", 5, hue_shift=0.03, sat_curve=0.05)


def sunscorched_stone():
    img = _mottle(4401, SUN, octaves=((8, 0.5), (16, 0.5)), shares=[1, 3, 5, 3, 1])
    px = img.load()
    rnd = random.Random(4402)
    x, y = rnd.randrange(SIZE), rnd.randrange(SIZE)
    for i in range(7):                       # one hairline crack, wandering
        px[x % SIZE, y % SIZE] = SUN[0]
        x += 1
        y += rnd.choice((-1, 0, 1))
    return img


# Charred End Stone: end stone's pitted grain, scorched dark, with a couple of embers.
CHAR = ramp("1a1210", "6a5244", 5, hue_shift=-0.02, sat_curve=0.05)


def charred_end_stone():
    img = chunks(4501, 18, CHAR, base_index=2, light=1, dark=1)
    px = img.load()
    for (x, y) in ((4, 11), (12, 3)):
        px[x, y] = hexc("9a3a14")
    return img


# Lumine Cap: shroomlight's glowing cells, in cool blue, brightest at their middles.
CAP = ramp("1c2a6a", "c8f2ff", 7, hue_shift=-0.04, sat_curve=0.1)


def lumine_cap():
    return chunks(4601, 9, CAP, base_index=3, light=2, dark=2)


# Pale End Stone: end stone's small pitted grain, bleached to a cool pale grey.
PALE = ramp("8e8aa6", "eceaf6", 5, hue_shift=0.02, sat_curve=0.04)


def pale_end_stone():
    return chunks(4701, 22, PALE, base_index=2, light=1, dark=1)


TEXTURES = [
    ("block/pale_end_stone.png", pale_end_stone),
    ("block/nightstone.png", nightstone),
    ("block/chorus_moss.png", chorus_moss),
    ("block/chorus_stalk.png", chorus_stalk),
    ("block/sunscorched_stone.png", sunscorched_stone),
    ("block/charred_end_stone.png", charred_end_stone),
    ("block/lumine_cap.png", lumine_cap),
]
