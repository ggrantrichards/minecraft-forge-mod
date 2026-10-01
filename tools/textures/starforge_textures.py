"""Textures for Phase 8 (Starforging).

Each Starforge upgrade leaves a colour accent on the item, the way an armour trim does: a few
shaded pixels in the upgrade's colour, placed where trims don't go (the guard of a sword, the
binding of a tool head, the grip of a bow, a gem on the brow of a helmet...). An accent is drawn as
a *mask* of shade levels 1-4 and painted with each upgrade's palette, so all six share one shape.
The same goes for armour you wear: a mask on the 64x32 armour layout, drawn over the armour like a
trim.
"""
from PIL import Image

from pixelart import from_map, hexc

# Darkest to lightest; index 0 is the outline-ish shadow, 4 the glint.
UPGRADES = {
    "voidwalker": ["2a1660", "4a2aa8", "7a56f0", "a98cff", "e0d4ff"],
    "meteoric": ["4a1208", "8a2410", "c8401c", "f07a3c", "ffc890"],
    "stardust": ["1c3a66", "2f66b0", "5fa4f0", "a8d4ff", "f0f8ff"],
    "temporal": ["4a2406", "8a4a10", "c87a22", "eea650", "ffe0a8"],
    "singularity": ["400a3a", "7a1470", "c02aa8", "ee62d4", "ffc8f2"],
    "radiant": ["8a6a10", "d0a818", "f8dc40", "fff8a0", "ffffff"],
}

# ------------------------------------------------------------------ item accents (16x16 masks)
# '1'..'4' are shades (1 darkest); '.' leaves the item as it is.
ITEM_ACCENTS = {
    # The sword's crossguard, inlaid.
    "sword": [
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "..34............",
        "...3............",
        "....2...........",
        "....12..........",
        "................",
        "................",
        "................",
        "................",
        ".2..............",
        "................",
    ],
    # Tools: the binding where the head meets the handle, and a fleck on the head.
    "pickaxe": [
        "................",
        "................",
        "................",
        "...........4....",
        "................",
        "................",
        ".........32.....",
        "........2.......",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
    ],
    "axe": [
        "................",
        "................",
        "................",
        "........4.......",
        "................",
        "................",
        "........32......",
        "........1.......",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
    ],
    "shovel": [
        "................",
        "................",
        "................",
        "...........4....",
        "................",
        "................",
        ".........3......",
        "........32......",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
    ],
    "hoe": [
        "................",
        "................",
        "........4.......",
        "................",
        "................",
        "................",
        ".........32.....",
        "........2.......",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
    ],
    # Ranged weapons: the bow's grip, the crossbow's stock, the trident's collar.
    "bow": [
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "...34...........",
        "...23...........",
        "...12...........",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
    ],
    "crossbow": [
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "........3.......",
        ".......32.......",
        "......21........",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
    ],
    "trident": [
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "......43........",
        ".....32.........",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
    ],
    "mace": [
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "......43........",
        ".....32.........",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
    ],
    # Armour: a gem on the brow, a clasp at the collar, a belt buckle, ankle bands. (Trims edge
    # the pieces; these sit inside them.)
    "helmet": [
        "................",
        "................",
        "................",
        "................",
        ".......34.......",
        "......2442......",
        ".......12.......",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
    ],
    "chestplate": [
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        ".......34.......",
        "......3442......",
        ".......12.......",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
    ],
    "leggings": [
        "................",
        "................",
        "................",
        ".......34.......",
        ".......21.......",
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
        "................",
    ],
    "boots": [
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "....34....34....",
        "....21....21....",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
    ],
}


def paint_mask(rows, palette):
    pal = {str(i): hexc(c) for i, c in enumerate(palette)}
    return from_map(rows, pal)


def item_accent(kind, upgrade):
    return paint_mask(ITEM_ACCENTS[kind], UPGRADES[upgrade])


# ------------------------------------------------------------------ worn armour accents (64x32)
# Pixel runs on the vanilla humanoid armour layout: (x, y, shade). Head front is x 8-15, y 8-15;
# body front x 20-27, y 20-31; right arm front x 44-47, y 20-31; legs front x 4-7, y 20-31.
def _runs(points):
    return [(x, y, s) for x, y, s in points]


WORN = {
    # Helmet gem on the brow; chestplate clasp at the collar and bands on the shoulders; boots bands.
    "humanoid": _runs([
        (11, 9, 3), (12, 9, 4), (10, 10, 2), (11, 10, 4), (12, 10, 4), (13, 10, 2), (11, 11, 1), (12, 11, 2),
        (23, 21, 3), (24, 21, 4), (22, 22, 2), (23, 22, 4), (24, 22, 4), (25, 22, 2), (23, 23, 1), (24, 23, 2),
        (44, 21, 2), (45, 21, 3), (46, 21, 3), (47, 21, 2), (40, 21, 1), (41, 21, 2), (42, 21, 2), (43, 21, 1),
        (4, 27, 2), (5, 27, 3), (6, 27, 3), (7, 27, 2), (0, 27, 1), (1, 27, 2), (2, 27, 2), (3, 27, 1),
    ]),
    # Leggings: the belt buckle and knee bands.
    "humanoid_leggings": _runs([
        (23, 21, 3), (24, 21, 4), (23, 22, 2), (24, 22, 1),
        (4, 25, 2), (5, 25, 3), (6, 25, 3), (7, 25, 2), (0, 25, 1), (1, 25, 2), (2, 25, 2), (3, 25, 1),
    ]),
}


def worn_accent(layer, upgrade):
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    pal = [hexc(c) for c in UPGRADES[upgrade]]
    for x, y, s in WORN[layer]:
        img.putpixel((x, y), pal[s])
    return img


TEXTURES = []
for _kind in ITEM_ACCENTS:
    for _up in UPGRADES:
        TEXTURES.append((f"item/starforge/{_kind}_{_up}.png", (lambda k=_kind, u=_up: item_accent(k, u))))


for _layer in WORN:
    for _up in UPGRADES:
        TEXTURES.append((f"entity/equipment/starforge/{_layer}_{_up}.png", (lambda l=_layer, u=_up: worn_accent(l, u))))


# ------------------------------------------------------------------ items
import math  # noqa: E402
from pathlib import Path  # noqa: E402

ASSETS = Path(__file__).resolve().parents[2] / "src/main/resources/assets/astralend/textures"


def _load(rel):
    return Image.open(ASSETS / rel).convert("RGBA")


VIOLET = ["2a1660", "4a2aa8", "7a56f0", "a98cff", "e0d4ff"]


def starforge_template():
    """A slab of Observatory Quartz like a smithing template, with the four-pointed star of the
    Cartographers inlaid in Void Crystal and a gold heart."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    o, d, m, l, h = hexc("2c3044"), hexc("6a7088"), hexc("9aa0b8"), hexc("c0c6d6"), hexc("e2e6ee")
    rows = {1: (4, 12), 13: (4, 12), 14: (5, 9)}
    for y in range(1, 15):
        x0, x1 = rows.get(y, (3, 13))
        for x in range(x0, x1 + 1):
            edge = x in (x0, x1) or y in (1, 14) or (y == 13 and x > 9) or (y == 13 and x == x0)
            if edge:
                c = o
            elif x == x0 + 1 or y == 2:
                c = h if (x + y) % 5 else l
            elif x == x1 - 1 or y == 12:
                c = d
            else:
                c = l if (x * 3 + y * 5) % 11 == 0 else m
            img.putpixel((x, y), c)
    v = [hexc(c) for c in VIOLET]
    gold = hexc("ffe49a")
    star = {(8, 4): v[1], (8, 5): v[2], (7, 6): v[1], (8, 6): v[3], (9, 6): v[1],
            (5, 7): v[1], (6, 7): v[2], (7, 7): v[3], (8, 7): gold, (9, 7): v[3], (10, 7): v[2], (11, 7): v[1],
            (7, 8): v[1], (8, 8): v[3], (9, 8): v[1], (8, 9): v[2], (8, 10): v[1]}
    for (x, y), c in star.items():
        img.putpixel((x, y), c)
    return img


def starforge_alloy():
    """Starforge Alloy: an ingot of meteoric iron and void crystal, deep indigo steel flecked with
    starlight."""
    base = _load("item/meteoric_iron_ingot.png")
    pal = [hexc(c) for c in ["120e22", "241c44", "3a3070", "5a4ca0", "8a7cd0", "c8bcf4"]]
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            r, g, b, a = base.getpixel((x, y))
            if a == 0:
                continue
            lum = (r * 3 + g * 6 + b) / 2550
            img.putpixel((x, y), pal[min(5, int(lum * 6.2))])
    for x, y, c in ((6, 7, "fff4c8"), (9, 8, "9ff0ff"), (11, 7, "fff4c8")):
        if img.getpixel((x, y))[3]:
            img.putpixel((x, y), hexc(c))
    return img


def rift_pearl():
    """A Rift Pearl: a sphere of the void, black-violet, with a tear of magenta light across it."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    pal = [hexc(c) for c in ["0e0618", "1e0c34", "34165a", "52288a", "7a4cc0"]]
    cx, cy, r = 7.5, 8.0, 5.6
    for y in range(16):
        for x in range(16):
            dx, dy = x - cx, y - cy
            d = math.hypot(dx, dy)
            if d > r:
                continue
            if d > r - 1.0:
                img.putpixel((x, y), pal[0])
                continue
            nz = math.sqrt(max(0.0, 1 - (d / r) ** 2))
            light = (-dx * 0.55 - dy * 0.65 + nz * 0.9) / 1.4
            img.putpixel((x, y), pal[max(1, min(4, int(1 + light * 3.6)))])
    for x, y, c in ((5, 9, "c02aa8"), (6, 8, "ee62d4"), (7, 8, "ffc8f2"), (8, 7, "ee62d4"), (9, 7, "c02aa8"), (10, 6, "7a1470"),
                    (8, 8, "c02aa8"), (5, 5, "e0d4ff")):
        img.putpixel((x, y), hexc(c))
    return img


def rift_tile(age):
    """A tile of rift: a dark, star-flecked pane edged in violet light. It cracks as it ages."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rim, inner, deep = hexc("a98cff", 230), hexc("4a2aa8", 200), hexc("1e0c44", 190)
    for y in range(16):
        for x in range(16):
            edge = min(x, y, 15 - x, 15 - y)
            img.putpixel((x, y), rim if edge == 0 else inner if edge == 1 else deep)
    for x, y in ((4, 5), (10, 3), (12, 10), (6, 11), (9, 7), (3, 13)):
        img.putpixel((x, y), hexc("e0d4ff", 230))
    cracks = [[(7, 2), (7, 3), (8, 4), (8, 5)], [(2, 8), (3, 8), (4, 9), (5, 9), (6, 10)],
              [(13, 6), (12, 7), (11, 8), (11, 9), (10, 10), (9, 11)]]
    for crack in cracks[:age]:
        for x, y in crack:
            img.putpixel((x, y), hexc("0a0414", 140))
    return img


def _metal_band(img, y0, y1):
    metal = _load("block/meteoric_iron_block.png")
    for y in range(y0, y1):
        for x in range(16):
            img.putpixel((x, y), metal.getpixel((x, y)))


def astral_forge_side(front=False):
    """The Astral Forge's sides: Observatory Quartz under a band of Meteoric Iron; the front opens on
    a forge mouth glowing with void light."""
    img = _load("block/observatory_quartz.png").copy()
    _metal_band(img, 0, 5)
    for x in range(16):
        img.putpixel((x, 5), hexc("2c3044"))
    if front:
        glow = [hexc(c) for c in ["1e0c44", "4a2aa8", "7a56f0", "c8b4ff"]]
        frame = hexc("3a3f52")
        for y in range(8, 15):
            for x in range(4, 12):
                arch = y == 8 and x in (4, 11)
                if arch:
                    continue
                if x in (4, 11) or y == 8 or (y == 9 and x in (5, 10)):
                    img.putpixel((x, y), frame)
                else:
                    heat = max(0.0, 1 - abs(x - 7.5) / 3.5) * (y - 8) / 6
                    img.putpixel((x, y), glow[min(3, int(heat * 4))])
    else:
        v = [hexc(c) for c in VIOLET]
        for (x, y), c in {(8, 7): v[1], (8, 8): v[2], (6, 10): v[1], (7, 10): v[2], (8, 10): v[4], (9, 10): v[2],
                          (10, 10): v[1], (8, 11): v[3], (8, 12): v[2], (8, 13): v[1]}.items():
            img.putpixel((x, y), c)
    return img


def astral_forge_top():
    """The working face: a plate of Meteoric Iron framed in quartz, a Void Crystal star set in it."""
    img = _load("block/meteoric_iron_block.png").copy()
    quartz = hexc("c0c6d6")
    for i in range(16):
        for x, y in ((i, 0), (i, 15), (0, i), (15, i)):
            img.putpixel((x, y), quartz)
    v = [hexc(c) for c in VIOLET]
    for (x, y), c in {(7, 4): v[2], (8, 4): v[1], (7, 5): v[3], (8, 5): v[2], (6, 6): v[2], (7, 6): v[4], (8, 6): v[3], (9, 6): v[1],
                      (4, 7): v[1], (5, 7): v[2], (6, 7): v[3], (7, 7): v[4], (8, 7): v[4], (9, 7): v[3], (10, 7): v[2], (11, 7): v[1],
                      (4, 8): v[1], (5, 8): v[2], (6, 8): v[3], (7, 8): v[4], (8, 8): v[3], (9, 8): v[2], (10, 8): v[2], (11, 8): v[1],
                      (6, 9): v[2], (7, 9): v[3], (8, 9): v[2], (9, 9): v[1], (7, 10): v[2], (8, 10): v[1], (7, 11): v[1], (8, 11): v[1]}.items():
        img.putpixel((x, y), c)
    return img


TEXTURES += [
    ("item/starforge_template.png", starforge_template),
    ("item/starforge_alloy.png", starforge_alloy),
    ("item/rift_pearl.png", rift_pearl),
    ("block/astral_forge_side.png", lambda: astral_forge_side(False)),
    ("block/astral_forge_front.png", lambda: astral_forge_side(True)),
    ("block/astral_forge_top.png", astral_forge_top),
] + [(f"block/rift_tile_{age}.png", (lambda a=age: rift_tile(a))) for age in range(4)]


# ------------------------------------------------------------------ the Astral Forge's screen
# Empty-slot icons: outlines in vanilla's slot-icon grey.
SLOT_ICONS = {
    "starforge_template": [
        "................",
        "....aaaaaaaaa...",
        "...a.........a..",
        "...a....a....a..",
        "...a....a....a..",
        "...a...a.a...a..",
        "...a.aa...aa.a..",
        "...a...a.a...a..",
        "...a....a....a..",
        "...a....a....a..",
        "...a.........a..",
        "...a.........a..",
        "...a.........a..",
        "....a.....aaa...",
        ".....aaaaa......",
        "................",
    ],
    "bow": [
        "................",
        "...........aaaa.",
        "........aaaa..a.",
        "......aa.....a..",
        ".....a......a...",
        "....a......a....",
        "...a......a.....",
        "...a.....a......",
        "..a.....a.......",
        "..a....a........",
        "..a...a.........",
        ".a...a..........",
        ".a..a...........",
        ".a.a............",
        ".aa.............",
        "................",
    ],
    "trident": [
        "................",
        ".........a......",
        "........a.a.aa..",
        ".......a.aa..a..",
        ".......a.....a..",
        ".......a...aa.a.",
        "........a.a..a..",
        ".......a.aaaa...",
        "......a.a.......",
        ".....a.a........",
        "....a.a.........",
        "...a.a..........",
        "..a.a...........",
        ".a.a............",
        "a.a.............",
        "aa..............",
    ],
    "void_crystal": [
        "................",
        "................",
        "........a.......",
        ".......a.a......",
        "......a...a.....",
        ".....a.....a....",
        "....a.......a...",
        "...a.........a..",
        "....a.......a...",
        ".....a.....a....",
        "......a...a.....",
        ".......a.a......",
        "........a.......",
        "................",
        "................",
        "................",
    ],
    "umbral_membrane": [
        "................",
        "................",
        "..a..........a..",
        "..aa........aa..",
        "..a.a......a.a..",
        "..a..aaaaaa..a..",
        "..a..........a..",
        "...a........a...",
        "...a........a...",
        "....a......a....",
        "....a..aa..a....",
        ".....aa..aa.....",
        "................",
        "................",
        "................",
        "................",
    ],
    "stardust": [
        "................",
        "................",
        "..........a.....",
        ".........a.a....",
        "..a.......a.....",
        ".a.a............",
        "..a.....a.......",
        ".......a.a......",
        "........a.......",
        "...a........a...",
        "..a.a......a.a..",
        "...a........a...",
        "......a.........",
        ".....a.a........",
        "......a.........",
        "................",
    ],
    "temporal_crystal": [
        "................",
        "....aaaaaaaa....",
        "....a......a....",
        ".....a....a.....",
        "......a..a......",
        ".......aa.......",
        ".......aa.......",
        "......a..a......",
        ".....a....a.....",
        "....a......a....",
        "....a......a....",
        "....aaaaaaaa....",
        "................",
        "................",
        "................",
        "................",
    ],
    "singularity_shard": [
        "................",
        "................",
        "........a.......",
        ".......a.a......",
        "......a...a.....",
        ".....a.....a....",
        ".....a.....a....",
        "....a.......a...",
        "....a.......a...",
        "....a.......a...",
        ".....a.....a....",
        ".....a.....a....",
        "......a...a.....",
        ".......aaa......",
        "................",
        "................",
    ],
    "solarite": [
        "................",
        "................",
        "................",
        "........aaaa....",
        "......aa....a...",
        "....aa.......a..",
        "...a.........a..",
        "..a.........a...",
        "..a.......aa....",
        "...a....aa......",
        "....aaaa........",
        "................",
        "................",
        "................",
        "................",
        "................",
    ],
}


def slot_icon(name):
    return from_map(SLOT_ICONS[name], {"a": hexc("555555")})


TEXTURES += [(f"gui/sprites/container/slot/{name}.png", (lambda n=name: slot_icon(n))) for name in SLOT_ICONS]


def _slot(img, x, y, size=18):
    """A vanilla-style slot: a dark top-left edge, a white bottom-right edge, grey inside."""
    dark, light, fill = hexc("373737"), hexc("ffffff"), hexc("8b8b8b")
    for yy in range(y, y + size):
        for xx in range(x, x + size):
            img.putpixel((xx, yy), fill)
    for i in range(size - 1):
        img.putpixel((x + i, y), dark)
        img.putpixel((x, y + i), dark)
        img.putpixel((x + size - 1, y + 1 + i), light)
        img.putpixel((x + 1 + i, y + size - 1), light)


def _panel(img, w, h):
    """A vanilla-style window: black rounded outline, white top-left bevel, grey bottom-right bevel."""
    bg, hi, lo, ol = hexc("c6c6c6"), hexc("ffffff"), hexc("555555"), hexc("000000")
    for y in range(h):
        for x in range(w):
            img.putpixel((x, y), bg)
    for x in range(2, w - 2):
        img.putpixel((x, 0), ol)
        img.putpixel((x, h - 1), ol)
        img.putpixel((x, 1), hi)
        img.putpixel((x, 2), hi)
        img.putpixel((x, h - 2), lo)
        img.putpixel((x, h - 3), lo)
    for y in range(2, h - 2):
        img.putpixel((0, y), ol)
        img.putpixel((w - 1, y), ol)
        img.putpixel((1, y), hi)
        img.putpixel((2, y), hi)
        img.putpixel((w - 2, y), lo)
        img.putpixel((w - 3, y), lo)
    for x, y in ((1, 1), (w - 2, h - 2)):
        img.putpixel((x, y), ol)
    for x, y in ((0, 0), (1, 0), (0, 1), (w - 1, 0), (w - 2, 0), (w - 1, 1), (0, h - 1), (1, h - 1), (0, h - 2),
                 (w - 1, h - 1), (w - 2, h - 1), (w - 1, h - 2)):
        img.putpixel((x, y), (0, 0, 0, 0))
    img.putpixel((w - 2, 1), bg)
    img.putpixel((w - 3, 2), bg)
    img.putpixel((1, h - 2), bg)
    img.putpixel((2, h - 3), bg)
    img.putpixel((w - 2, 2), lo)
    img.putpixel((2, h - 2), lo)


def forge_icon():
    """The window's emblem: a four-pointed Void Crystal star blazing over a Meteoric Iron anvil."""
    img = Image.new("RGBA", (28, 28), (0, 0, 0, 0))
    o, m, g, W = hexc("23263a"), hexc("5a5f74"), hexc("8a90a8"), hexc("c0c6d6")
    px = img.load()

    def rect(x0, y0, x1, y1, c):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                px[x, y] = c
    # The anvil: outline first, then the body over it.
    rect(3, 14, 26, 19, o)
    rect(0, 15, 4, 17, o)
    rect(9, 19, 19, 23, o)
    rect(6, 22, 22, 27, o)
    rect(4, 15, 25, 18, m)
    rect(1, 16, 4, 16, m)
    rect(4, 15, 25, 15, W)
    rect(1, 16, 3, 16, g)
    rect(5, 16, 24, 16, g)
    rect(10, 19, 18, 22, m)
    rect(10, 19, 10, 22, g)
    rect(7, 23, 21, 26, m)
    rect(7, 23, 21, 23, g)
    rect(7, 26, 21, 26, hexc("44485c"))
    # The star above it.
    v = [hexc(c) for c in VIOLET]
    star = {}
    for i in range(1, 7):
        c = v[max(0, 3 - i // 2)] if i > 1 else v[3]
        for x, y in ((13 - i, 6), (14 + i, 6), (13, 6 - i), (13, 6 + i)):
            star[(x, y)] = c
        for x, y in ((14, 6 - i), (14, 6 + i)):
            if i < 4:
                star[(x, y)] = v[max(0, 2 - i // 2)]
    for x, y in ((13, 6), (14, 6), (13, 7), (14, 7)):
        star[(x, y)] = hexc("ffffff")
    for x, y in ((12, 5), (15, 5), (12, 8), (15, 8)):
        star[(x, y)] = v[3]
    for x, y in ((7, 0), (21, 2), (4, 9), (23, 10)):
        star[(x, y)] = v[3]
    for (x, y), c in star.items():
        if 0 <= x < 28 and 0 <= y < 28:
            px[x, y] = c
    return img


def forge_gui():
    """The Astral Forge's window (176 x 166 on a 256 x 256 sheet): the smithing table's layout with
    five input slots, an arrow and the result; a star over an anvil in the corner."""
    img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    panel = Image.new("RGBA", (176, 166), (0, 0, 0, 0))
    _panel(panel, 176, 166)
    for x in (7, 25, 43, 61, 79):
        _slot(panel, x, 47)
    _slot(panel, 133, 47)
    for row in range(3):
        for col in range(9):
            _slot(panel, 7 + col * 18, 83 + row * 18)
    for col in range(9):
        _slot(panel, 7 + col * 18, 141)
    # The arrow, in the smithing table's place.
    arrow = hexc("8b8b8b")
    for y in range(52, 59):
        for x in range(103, 116):
            if 54 <= y <= 56:
                panel.putpixel((x, y), arrow)
    for i in range(6):
        for y in range(52 - 1 + i, 60 - i):
            panel.putpixel((116 + i, y), arrow)
    panel.alpha_composite(forge_icon(), (9, 7))
    img.paste(panel, (0, 0))
    return img


TEXTURES += [("gui/container/astral_forge.png", forge_gui)]
