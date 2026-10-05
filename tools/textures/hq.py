"""The Astral End's textures, drawn with the pixel-art helpers in pixelart.py.

Each function returns one 16x16 texture. make_textures.py saves them (TEXTURES at the bottom
lists which file each one becomes). Keep to the house style when adding textures:
  - 4-6 tone ramps built with ramp(), not scattered random colours;
  - shapes lit from the top-left; details placed deliberately;
  - check the result with the tiled preview (tools/textures/make_textures.py writes
    docs/images/textures_tiled.png) - seams and obvious repeats show up there.
"""
import math
import random

from pixelart import (CLEAR, SIZE, cell_light, chunks, crack_lines, fbm, from_map, get, hexc, leaves, mix, new, ore,
                      pits, put, quantize, ramp, rock, shade, shadow_below, speckle, stamp, voronoi)


# ------------------------------------------------------------------ stones and ground

SUNSCORCHED = ramp("6e4521", "f4dca6", 7, hue_shift=0.05)


def sunscorched_stone():
    """Sun-baked crust: pale ochre plates split by dark, sharp-edged cracks, like dried mud."""
    pal = SUNSCORCHED
    img = rock(121, pal[3:6], shares=[2, 4, 2], octaves=((4, 0.5), (8, 0.5)))
    ids, border = voronoi(122, 7)
    for y in range(SIZE):
        for x in range(SIZE):
            me = ids[y][x]
            right, down = ids[y][(x + 1) % SIZE], ids[(y + 1) % SIZE][x]
            up, left = ids[(y - 1) % SIZE][x], ids[y][(x - 1) % SIZE]
            if right != me or down != me:
                # the crack: dark where it runs deep, a shade lighter where it's shallow
                put(img, x, y, pal[1] if border[y][x] < 0.25 else pal[2])
            elif up != me or left != me:
                put(img, x, y, shade(get(img, x, y), 0.08))   # sunlit lip of the plate
    speckle(img, 123, [pal[2], pal[6]], 6)
    return img


def sunscorched_polished():
    pal = SUNSCORCHED
    img = rock(124, pal[4:6], shares=[3, 2], octaves=((8, 0.6), (16, 0.4)))
    for i in range(SIZE):
        put(img, i, 0, pal[6])
        put(img, 0, i, pal[6])
        put(img, i, SIZE - 1, pal[1])
        put(img, SIZE - 1, i, pal[1])
    put(img, 0, SIZE - 1, pal[3])
    put(img, SIZE - 1, 0, pal[3])
    return img


def bricks(seed, pal, rows=4, mortar=0):
    """Vanilla-style bricks: courses of 4, offset every other row, each brick lit top-left, with
    mortar in pal[mortar]."""
    img = rock(seed, pal[2:5], shares=[1, 3, 1], octaves=((4, 0.6), (8, 0.4)))
    h = SIZE // rows
    for r in range(rows):
        y0 = r * h
        offset = 0 if r % 2 == 0 else SIZE // 4
        for x in range(SIZE):
            put(img, x, y0 + h - 1, pal[mortar])        # mortar under the course
            put(img, x, y0, shade(get(img, x, y0), 0.25))  # lit top edge of each brick
        for bx in range(offset, SIZE + offset, SIZE // 2):
            for y in range(y0, y0 + h):
                put(img, bx - 1, y, pal[mortar])        # mortar between bricks
                if y < y0 + h - 1:
                    put(img, bx, y, shade(get(img, bx, y), 0.2))
                    put(img, bx - 2, y, shade(get(img, bx - 2, y), -0.18))
    return img


CHARRED = ramp("140e0c", "6e574a", 6, hue_shift=-0.03)
EMBER = [hexc("7a2410"), hexc("c24818"), hexc("f08a2a")]


def charred_end_stone():
    """End Stone scorched by the comets: sooty, pitted, with a few embers still in the cracks."""
    img = rock(111, CHARRED[1:6], shares=[1, 2, 3, 2, 1])
    pits(img, 112, 9, depth=0.45)
    crack_lines(img, 113, EMBER[0], n=2, length=(3, 5))
    for (x, y) in ((4, 5), (11, 12), (13, 3)):
        put(img, x, y, EMBER[1])
    put(img, 12, 12, EMBER[2])
    return img


UMBRAL = ramp("16121f", "6e6292", 6, hue_shift=0.03)   # lightened: the Crags read black


def nightstone():
    """Dark violet-black stone, faintly layered, with rare glints of light caught inside it."""
    img = rock(1, UMBRAL[1:5], shares=[2, 4, 3, 1], octaves=((4, 0.5), (8, 0.35), (16, 0.15)))
    ids, _ = voronoi(4, 9)
    lit = cell_light(ids)
    for y in range(SIZE):
        for x in range(SIZE):
            if lit[y][x] < 0 and (x + 2 * y) % 4:
                put(img, x, y, UMBRAL[0])
            elif lit[y][x] > 0 and (x + y) % 2:
                put(img, x, y, shade(get(img, x, y), 0.12))
    for (x, y) in ((3, 11), (12, 4)):
        put(img, x, y, hexc("8e7ad0"))
        put(img, x + 1, y + 1, UMBRAL[3])
    return img


PALE = ramp("8d8aa8", "f7f6fc", 6, hue_shift=0.02)


def pale_end_stone():
    """End Stone bleached almost white by the aurora: soft mottling and small pits."""
    img = rock(101, PALE[1:6], shares=[1, 2, 4, 3, 1], octaves=((4, 0.45), (8, 0.35), (16, 0.2)))
    pits(img, 102, 10, depth=0.3)
    return img


ICE = ramp("3d6ea8", "eef8ff", 6, hue_shift=-0.02)



STARDUST = ramp("6a5b8e", "fbf6ff", 6, hue_shift=0.04)


def stardust_block():
    """Fine, pale violet grains with glittering specks, like sand made of starlight."""
    f = fbm(141, ((4, 0.4), (8, 0.35), (16, 0.25)))
    img = quantize(f, STARDUST[1:5], shares=[1, 3, 3, 1])
    speckle(img, 142, [STARDUST[5], hexc("fff3c4")], 7)
    speckle(img, 143, [STARDUST[0]], 5)
    return img


def chorus_moss():
    """Soft violet moss in small cushions, flecked with cyan glow."""
    pal = ramp("2a1a4d", "b69af0", 6, hue_shift=0.03)
    img = chunks(151, 26, pal[1:], base_index=2, light=1, dark=1)
    speckle(img, 152, [hexc("6ff2da"), hexc("aefcf2")], 5)
    return img


# ------------------------------------------------------------------ lichen (a carpet)

def sunscale_lichen():
    """Round crusts of glowing lichen of different sizes: a dark rim, gold toward the middle, and an
    older, duller heart. Transparent between the patches so the stone shows through, like vanilla's
    glow lichen."""
    img = new()
    wobble = fbm(161, ((4, 0.6), (8, 0.4)))
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


# ------------------------------------------------------------------ foliage

def lumine_cap():
    """The cap of a giant Lumineshroom: deep indigo like the night sky, softly mottled, freckled with
    glowing cyan spots of a few sizes (the light it gives off). Low contrast in the indigo so the
    caps read cleanly from a distance; the spots are what catch the eye."""
    base = ramp("2c2a74", "5552b8", 4, hue_shift=0.02)
    img = rock(302, base, shares=[1, 4, 4, 1], octaves=((2, 0.6), (4, 0.4)))
    glow = {"m": hexc("3fc8d8"), "l": hexc("a6f6ff"), "h": hexc("ffffff"), "d": hexc("2f8aa8")}
    shapes = {
        "big": [".mm.", "mhlm", "mllm", ".mm."],
        "mid": [".m.", "mhm", ".m."],
        "small": ["hm", "md"],
        "tiny": ["l"],
    }
    for (x, y, k) in ((2, 3, "big"), (10, 9, "mid"), (12, 1, "small"), (5, 12, "tiny"), (14, 13, "tiny"),
                      (8, 5, "tiny"), (1, 9, "small"), (14, 6, "tiny")):
        stamp(img, x, y, shapes[k], glow)
    return img


NULL_LEAVES = {"d": hexc("1a1230"), "s": hexc("33245c"), "m": hexc("4e3a88"), "l": hexc("6f58b4"),
               "h": hexc("a48ce8")}


def nullwood_leaves():
    return leaves(214, NULL_LEAVES, holes=0.08, count=58)


# ------------------------------------------------------------------ ores, raw materials

METEOR_ROCK = ramp("1d1719", "7a6a66", 6, hue_shift=-0.02)
METEORIC = {"h": hexc("eef0f6"), "l": hexc("b9bdc9"), "m": hexc("7f8494"), "d": hexc("3e3f4a")}


def meteoric_iron_ore():
    stone = rock(6, METEOR_ROCK[1:6], shares=[1, 2, 3, 2, 1])
    pits(stone, 7, 5, depth=0.35)
    return ore(stone, METEORIC, count=6)


SOLAR = {"h": hexc("fff6c2"), "l": hexc("ffd45a"), "m": hexc("f29a1c"), "d": hexc("a4520c")}


def solarite_ore():
    stone = rock(125, SUNSCORCHED[3:6], shares=[2, 4, 2], octaves=((4, 0.5), (8, 0.5)))
    pits(stone, 126, 4, depth=0.2)
    return ore(stone, SOLAR, count=5, shadow=0.35)


VOID = {"h": hexc("f0e2ff"), "l": hexc("b98cff"), "m": hexc("7a44e0"), "d": hexc("3a1a78")}


def void_crystal_ore():
    return ore(nightstone(), VOID, count=5, shadow=0.2)


RAW_METEORIC = ramp("1f1d24", "d7d4de", 7, hue_shift=0.03)
RUST = [hexc("7a3c1f"), hexc("a95a2c"), hexc("c97a3f")]


def raw_meteoric_iron_block():
    """Lumps of raw meteoric iron fused together: steel-grey with a blue sheen and rust."""
    img = chunks(8, 10, RAW_METEORIC, base_index=3, light=2, dark=2)
    rnd = random.Random(9)
    for _ in range(9):
        x, y = rnd.randrange(SIZE), rnd.randrange(SIZE)
        put(img, x, y, rnd.choice(RUST))
    return img


RAW_SOLAR = ramp("5a2a08", "fff0a8", 7, hue_shift=0.06)


def raw_solarite_block():
    img = chunks(128, 10, RAW_SOLAR, base_index=3, light=2, dark=2)
    speckle(img, 129, [hexc("fffbe0")], 4)
    return img


# A raw ore chunk, like vanilla raw iron: three fused lumps, lit from the top-left.
RAW_CHUNK = [
    "................",
    "................",
    "......oooo......",
    ".....ohhllo.....",
    "....ohllllmoooo.",
    "..ooollmmmmohllo",
    ".ohhlolmmmdollmo",
    ".ohlllommddommdo",
    ".olllmmmodddmmdo",
    ".olmmmmmdooddddo",
    "..ommmmddo.oooo.",
    "..odmmddddo.....",
    "...oddddoo......",
    "....oooo........",
    "................",
    "................",
]


def raw_meteoric_iron():
    pal = {"o": hexc("17151b"), "d": hexc("4b4755"), "m": hexc("737080"), "l": hexc("a4a1b0"), "h": hexc("dcdae3")}
    img = from_map(RAW_CHUNK, pal)
    for (x, y) in ((8, 9), (12, 7), (5, 11)):
        put(img, x, y, RUST[1])
    put(img, 9, 9, RUST[0])
    return img


def raw_solarite():
    pal = {"o": hexc("3d1a04"), "d": hexc("b0560c"), "m": hexc("ee921c"), "l": hexc("ffc64a"), "h": hexc("fff3b8")}
    return from_map(RAW_CHUNK, pal)


# ------------------------------------------------------------------ plants

def _star(cx, cy, outer, inner, points=5, rotation=-math.pi / 2):
    verts = []
    for i in range(points * 2):
        r = outer if i % 2 == 0 else inner
        a = rotation + i * math.pi / points
        verts.append((cx + math.cos(a) * r, cy + math.sin(a) * r))
    return verts


def _inside(poly, x, y):
    inside = False
    j = len(poly) - 1
    for i in range(len(poly)):
        xi, yi = poly[i]
        xj, yj = poly[j]
        if (yi > y) != (yj > y) and x < (xj - xi) * (y - yi) / (yj - yi) + xi:
            inside = not inside
        j = i
    return inside


def starbloom():
    """A golden five-pointed flower on a slender stem, with a glowing white heart."""
    img = new()
    stem = {"s": hexc("2f6b52"), "S": hexc("468f6c"), "l": hexc("3f8a5e"), "L": hexc("63b37d")}
    # stem and two leaves
    stamp(img, 0, 0, [
        "................", "................", "................", "................",
        "................", "................", "................", "................",
        "................", "................",
        ".......S........",
        ".......s..LL....",
        "..LL...s.Ll.....",
        "...lL..sSl......",
        "....lls.........",
        ".......s........",
    ], stem)
    petal = {"e": hexc("b9781a"), "m": hexc("f2b73a"), "l": hexc("ffd966"), "h": hexc("fff0a8")}
    poly = _star(7.5, 5.2, 5.3, 2.3)
    for y in range(SIZE):
        for x in range(SIZE):
            if _inside(poly, x + 0.5, y + 0.5):
                d = math.hypot(x + 0.5 - 7.5, y + 0.5 - 5.2)
                edge = not _inside(poly, x + 1.5, y + 0.5) or not _inside(poly, x + 0.5, y + 1.5)
                if d < 1.0:
                    c = hexc("ffffff")
                elif d < 1.9:
                    c = hexc("ff9f2e")
                elif edge:
                    c = petal["e"]
                elif (x + 0.5 - 7.5) + (y + 0.5 - 5.2) < -1.5:
                    c = petal["h"] if d > 3.2 else petal["l"]
                else:
                    c = petal["l"] if d < 3.0 else petal["m"]
                put(img, x, y, c)
    return img


# ------------------------------------------------------------------ wood

def planks(seed, pal):
    """Vanilla-style planks: four boards with long grain, a dark seam under each, and one end joint
    per board. `pal` is 5 tones, darkest first."""
    grain = fbm(seed, ((2, 0.3), (4, 0.4), (16, 0.3)), stretch_x=5.0)
    img = new()
    rnd = random.Random(seed + 1)
    joints = [rnd.choice((2, 3, 4)), rnd.choice((10, 11, 12)), rnd.choice((6, 7)), rnd.choice((13, 14, 1))]
    for board in range(4):
        y0 = board * 4
        base = 2 if board % 2 == 0 else 3
        for dy in range(4):
            y = y0 + dy
            for x in range(SIZE):
                if dy == 3:
                    c = pal[0]                                     # seam under the board
                else:
                    g = grain[y][x]
                    t = base + (1 if g > 0.72 else -1 if g < 0.3 else 0)
                    if dy == 0:
                        t += 1                                     # the lit top edge of the board
                    c = pal[max(1, min(4, t))]
                put(img, x, y, c)
        for _ in range(3):                                         # grain streaks
            gx, gy = rnd.randrange(SIZE), y0 + rnd.randint(1, 2)
            for k in range(rnd.randint(2, 4)):
                put(img, gx + k, gy, pal[1])
        j = joints[board]
        for dy in range(3):
            put(img, j, y0 + dy, pal[0])
            put(img, j + 1, y0 + dy, pal[min(4, base + 1)])
    return img


def bark(seed, pal, glow=None, glows=0):
    """Bark with vertical grain and deep fissures (pal: 5 tones, darkest first). `glow` adds short
    glowing streaks (for the Lumineshroom's stem)."""
    f = fbm(seed, ((8, 0.5), (16, 0.5)), stretch_x=2.0)
    f = [[f[x][y] for x in range(SIZE)] for y in range(SIZE)]      # turn the grain vertical
    img = quantize(f, pal[1:5], shares=[2, 3, 3, 1])
    rnd = random.Random(seed + 1)
    for x in (1, 5, 8, 12, 14):                                    # fissures
        y = rnd.randrange(SIZE)
        for _ in range(rnd.randint(4, 9)):
            put(img, x, y, pal[0])
            put(img, x - 1, y, shade(get(img, x - 1, y), 0.15))
            y += 1
            if rnd.random() < 0.2:
                x += rnd.choice((-1, 1))
    if glow:
        for _ in range(glows):
            x, y = rnd.randrange(SIZE), rnd.randrange(SIZE)
            for i in range(rnd.randint(2, 4)):
                put(img, x, y + i, glow[1] if i else glow[2])
    return img


def log_top(bark_colour, rings, core):
    """Log end: a bark rim and rounded growth rings alternating two tones, darker toward the rim."""
    img = new()
    for y in range(SIZE):
        for x in range(SIZE):
            dx, dy = abs(x - 7.5), abs(y - 7.5)
            if max(dx, dy) > 6.6:
                put(img, x, y, bark_colour)
                continue
            d = max(dx, dy) * 0.55 + math.hypot(dx, dy) * 0.45
            if d < 1.2:
                c = core
            elif d > 5.6:
                c = rings[3] if int(d * 2) % 2 else rings[2]      # darker sapwood by the bark
            else:
                c = rings[2] if int(d) % 2 else rings[0]          # thin dark growth rings
                if c == rings[0] and (x + y) % 5 == 0:
                    c = rings[1]
            put(img, x, y, c)
    return img


LUMINE_STEM = ramp("463d5e", "c4bcd6", 5, hue_shift=0.02)
LUMINE_PLANK = ramp("4a4064", "c8c0dc", 5, hue_shift=0.03)
LUMINE_GLOW = [hexc("2fb7ad"), hexc("5fe3d6"), hexc("c8fff6")]
NULL_BARK = ramp("07050b", "3d3354", 5, hue_shift=0.02)
NULL_PLANK = ramp("24133c", "8a62c8", 5, hue_shift=0.04)


# ------------------------------------------------------------------ plants, drawn blade by blade

def _blade(img, x, height, colours, lean=0, sparkle=None):
    """One blade of grass from the bottom row up: colours run from base to tip."""
    y = SIZE - 1
    for i in range(height):
        t = i / max(1, height - 1)
        c = colours[min(len(colours) - 1, int(t * len(colours)))]
        put(img, x, y - i, c)
        if lean and i > height // 2 and i % 3 == 0:
            x += lean
    if sparkle:
        put(img, x, y - height + 1, sparkle)


def shimmer_grass():
    img = new()
    colours = [hexc("3e6f80"), hexc("5a91a2"), hexc("80b9c4"), hexc("aee0e2"), hexc("d6f4f2")]
    for (x, h, lean, sp) in ((1, 7, 0, None), (3, 11, 1, hexc("ffffff")), (5, 6, 0, None), (6, 13, -1, None),
                             (8, 9, 0, hexc("f4fbff")), (10, 12, 1, None), (12, 7, 0, None), (13, 10, -1, hexc("ffffff")),
                             (15, 5, 0, None)):
        _blade(img, x, h, colours, lean, sp)
    return img


def nebula_tufts():
    img = new()
    colours = [hexc("4a2d82"), hexc("6a44b0"), hexc("8c62d6"), hexc("b48cf0"), hexc("e0c8ff")]
    for (x, h, lean, sp) in ((1, 5, 0, None), (2, 8, 1, hexc("f2e4ff")), (4, 6, 0, None), (6, 9, -1, None),
                             (7, 6, 0, hexc("7df5e6")), (9, 8, 1, None), (11, 5, 0, None), (12, 9, 0, hexc("f2e4ff")),
                             (14, 6, -1, None)):
        _blade(img, x, h, colours, lean, sp)
    return img


FLAME = [hexc("7a160c"), hexc("b8281a"), hexc("e4521e"), hexc("f98a26"), hexc("ffc244"), hexc("fff0b0")]


def _flame(img, cx, base_y, top_y, width, lean):
    """A tapering flame-shaped petal from base_y up to top_y, shaded dark at its root to bright at its tip."""
    height = base_y - top_y
    for i in range(height + 1):
        y = base_y - i
        t = i / max(1, height)
        w = max(1, int(round(width * (1 - t * 0.85))))
        x = int(round(cx + lean * t * t * 2))
        c = FLAME[min(5, int(t * 5.6))]
        for dx in range(w):
            put(img, x - w // 2 + dx, y, c if dx < w - 1 or w == 1 else shade(c, -0.25))


def flarebloom_top():
    """The bloom: tongues of petals like flames rising from a dark cup, on the stem's upper half."""
    img = new()
    stem = {"s": hexc("3d1f14"), "S": hexc("5e3020")}
    for y in range(10, SIZE):
        put(img, 7, y, stem["s"])
        put(img, 8, y, stem["S"])
    for (cx, top, width, lean) in ((4, 5, 2, -1), (11, 5, 2, 1), (6, 2, 3, -0.5), (9, 3, 3, 0.5), (7.5, 0, 3, 0)):
        _flame(img, cx, 9, top, width, lean)
    for x in range(5, 11):  # the cup the petals rise from
        put(img, x, 10, hexc("4a1810") if x in (5, 10) else hexc("7a2414"))
    for x in range(6, 10):
        put(img, x, 11, hexc("4a1810"))
    return img


def _leaf(img, base, tip, colours, under):
    """A two-pixel-thick leaf from base to tip: darker at the base, lighter toward the tip, with a
    shadowed underside."""
    (x0, y0), (x1, y1) = base, tip
    steps = int(max(abs(x1 - x0), abs(y1 - y0)) * 3) + 1
    for i in range(steps + 1):
        t = i / steps
        x = int(round(x0 + (x1 - x0) * t))
        y = int(round(y0 + (y1 - y0) * t - math.sin(t * math.pi) * 1.2))
        put(img, x, y, colours[min(len(colours) - 1, int(t * len(colours)))])
        if t < 0.8:
            put(img, x, y + 1, under)


def flarebloom_bottom():
    """The lower half: a scorched stem with long, curling rust-red leaves."""
    img = new()
    for y in range(SIZE):
        put(img, 7, y, hexc("3d1f14"))
        put(img, 8, y, hexc("5e3020"))
    colours = [hexc("8e2e16"), hexc("b23c1a"), hexc("d85a22"), hexc("f08a36")]
    under = hexc("4e160c")
    for (base, tip) in (((6, 13), (1, 9)), ((9, 10), (14, 5)), ((6, 6), (2, 2)), ((9, 3), (12, 0))):
        _leaf(img, base, tip, colours, under)
    return img

# ------------------------------------------------------------------ storage blocks

def metal_block(pal, seed):
    """A block of refined metal like vanilla's iron or gold block: a bevelled frame, a brushed plate
    with two faint seams, and a couple of glints. `pal` is 6 tones, darkest first."""
    img = rock(seed, pal[2:5], shares=[1, 4, 2], octaves=((8, 0.3), (16, 0.7)), stretch_x=3.0)
    for i in range(SIZE):
        put(img, i, 0, pal[5])
        put(img, 0, i, pal[5])
        put(img, i, SIZE - 1, pal[1])
        put(img, SIZE - 1, i, pal[1])
        put(img, i, 1, pal[4])
        put(img, 1, i, pal[4])
        put(img, i, SIZE - 2, pal[2])
        put(img, SIZE - 2, i, pal[2])
    for y in (5, 10):
        for x in range(2, SIZE - 2):
            put(img, x, y, pal[2])
            put(img, x, y + 1, pal[4])
    put(img, 0, SIZE - 1, pal[3])
    put(img, SIZE - 1, 0, pal[3])
    for (x, y) in ((4, 3), (11, 8), (6, 13)):
        put(img, x, y, pal[5])
    return img


def crystal_block(pal, seed):
    """Fused crystal facets, like vanilla's amethyst block."""
    img = chunks(seed, 9, pal, base_index=3, light=2, dark=2)
    speckle(img, seed + 1, [pal[-1]], 4)
    return img


# ------------------------------------------------------------------ Corona remains, whale scales

PETRIFIED = ramp("3e3a37", "b3aca2", 5, hue_shift=-0.02)


def petrified_stalk_side():
    """The stone-hard trunk of a dead giant vine: bark turned to grey rock, with the fossil of a
    thinner vine spiralling up it."""
    img = bark(141, PETRIFIED)
    for y in range(SIZE):
        x = 7 + int(round(3 * math.sin(y / 2.6)))
        put(img, x, y, PETRIFIED[0])
        put(img, x + 1, y, PETRIFIED[4])
        if y % 4 == 1:
            put(img, x + 2, y, PETRIFIED[3])
    return img


def petrified_stalk_top():
    return log_top(PETRIFIED[0], [PETRIFIED[3], PETRIFIED[2], PETRIFIED[2], PETRIFIED[1]], PETRIFIED[4])


ASHEN_SHROOM = [
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


def ashen_lumineshroom():
    """A Lumineshroom that burned out in the Corona: the same shape, grey and cracked."""
    img = from_map(ASHEN_SHROOM, {"c": hexc("67626a"), "C": hexc("8b868e"), "w": hexc("b9b4ba"), "d": hexc("403d44"),
                                  "s": hexc("7c766e"), "S": hexc("a39d93")})
    for (x, y) in ((6, 5), (7, 6), (9, 7)):
        put(img, x, y, hexc("403d44"))   # cracks in the dead cap
    return img


def whalelight_scale():
    """Overlapping pearly scales in staggered rows, each lit at its top-left with a darker rim."""
    img = new()
    pal = ramp("6c7fc4", "f6f9ff", 5, hue_shift=-0.03)
    for y in range(SIZE):
        for x in range(SIZE):
            row = y // 4
            lx = (x + (2 if row % 2 else 0)) % 4
            ly = y % 4
            dx, dy = lx - 1.5, ly - 0.8
            r = math.hypot(dx, dy * 1.2)
            if r > 1.9 or ly == 3 and lx in (0, 3):
                c = pal[0]                          # the rim where the next scale overlaps
            elif dx + dy < -1.0:
                c = pal[4]                          # sheen at the top-left
            elif r < 1.0:
                c = pal[3]
            else:
                c = pal[2] if (dx + dy) < 0.8 else pal[1]
            put(img, x, y, c)
    return img


TEXTURES = [
    ("block/sunscorched_stone.png", sunscorched_stone),
    ("block/polished_sunscorched_stone.png", sunscorched_polished),
    ("block/sunscorched_stone_bricks.png", lambda: bricks(125, SUNSCORCHED, mortar=1)),
    ("block/charred_end_stone.png", charred_end_stone),
    ("block/nightstone.png", nightstone),
    ("block/nightstone_bricks.png", lambda: bricks(3, UMBRAL, mortar=0)),
    ("block/pale_end_stone.png", pale_end_stone),
    ("block/pale_end_stone_bricks.png", lambda: bricks(102, PALE, mortar=1)),
    ("block/stardust_block.png", stardust_block),
    ("block/chorus_moss.png", chorus_moss),
    ("block/sunscale_lichen.png", sunscale_lichen),
    ("block/lumine_cap.png", lumine_cap),
    ("block/nullwood_leaves.png", nullwood_leaves),
    ("block/meteoric_iron_ore.png", meteoric_iron_ore),
    ("block/solarite_ore.png", solarite_ore),
    ("block/void_crystal_ore.png", void_crystal_ore),
    ("block/raw_meteoric_iron_block.png", raw_meteoric_iron_block),
    ("block/raw_solarite_block.png", raw_solarite_block),
    ("item/raw_meteoric_iron.png", raw_meteoric_iron),
    ("item/raw_solarite.png", raw_solarite),
    ("block/starbloom.png", starbloom),
    ("block/lumine_stem.png", lambda: bark(201, LUMINE_STEM, LUMINE_GLOW, 5)),
    ("block/lumine_stem_top.png", lambda: log_top(LUMINE_STEM[1], [LUMINE_PLANK[3], LUMINE_PLANK[2], LUMINE_PLANK[2], LUMINE_PLANK[1]],
                                                LUMINE_GLOW[1])),
    ("block/stripped_lumine_stem_top.png", lambda: log_top(LUMINE_PLANK[2], [LUMINE_PLANK[3], LUMINE_PLANK[2], LUMINE_PLANK[2],
                                                                            LUMINE_PLANK[1]], LUMINE_GLOW[0])),
    ("block/nullwood_planks.png", lambda: planks(213, NULL_PLANK)),
    ("block/nullwood_log.png", lambda: bark(211, NULL_BARK, [None, NULL_PLANK[3], NULL_PLANK[4]], 3)),
    ("block/nullwood_log_top.png", lambda: log_top(NULL_BARK[1], [NULL_PLANK[3], NULL_PLANK[2], NULL_PLANK[2], NULL_PLANK[1]],
                                                 NULL_PLANK[4])),
    ("block/stripped_nullwood_log.png", lambda: bark(212, NULL_PLANK)),
    ("block/stripped_nullwood_log_top.png", lambda: log_top(NULL_PLANK[1], [NULL_PLANK[3], NULL_PLANK[2], NULL_PLANK[2],
                                                                           NULL_PLANK[1]], NULL_PLANK[4])),
    ("block/shimmer_grass.png", shimmer_grass),
    ("block/nebula_tufts.png", nebula_tufts),
    ("block/flarebloom_top_open.png", flarebloom_top),
    ("block/flarebloom_bottom.png", flarebloom_bottom),
    ("block/meteoric_iron_block.png", lambda: metal_block(ramp("3a3d4a", "eef0f6", 6, hue_shift=0.02), 7)),
    ("block/solarite_block.png", lambda: metal_block(ramp("8a3e06", "fff4b0", 6, hue_shift=0.07), 127)),
    ("block/void_crystal_block.png", lambda: crystal_block(ramp("1e0c44", "e6d4ff", 7, hue_shift=0.03), 9)),
    ("block/petrified_stalk_side.png", petrified_stalk_side),
    ("block/petrified_stalk_top.png", petrified_stalk_top),
    ("block/ashen_lumineshroom.png", ashen_lumineshroom),
    ("block/whalelight_scale.png", whalelight_scale),
]

# Phase 5: Sunstorms
from flare_textures import TEXTURES as _FLARE  # noqa: E402
TEXTURES += _FLARE

# Phase 6: structures
from structure_textures import ANIMATED, TEXTURES as _STRUCTURES  # noqa: E402,F401
TEXTURES += _STRUCTURES

# Phase 8: Starforging
from starforge_textures import TEXTURES as _STARFORGE  # noqa: E402
TEXTURES += _STARFORGE

# Phase 10: the Chorus Flower on a Stick, the Lodestar.
from phase10_textures import TEXTURES as _PHASE10  # noqa: E402
TEXTURES += _PHASE10
