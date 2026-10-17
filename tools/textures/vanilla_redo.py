"""The 1.0 pass: blocks and items redone from the vanilla block or item each is kin to, so they look
like Minecraft. Each keeps its vanilla base's shape, pixel pattern and shading; only the colours
change (a gradient map onto the mod's palette), and where the base has a part that isn't ours (a
carrot, a creeper face) that part alone is recoloured or redrawn.

    python3 tools/textures/vanilla_redo.py

Run after the other texture tools; it overwrites the textures it makes. The vanilla textures are
fetched once into tools/textures/.vanilla (git-ignored, see retexture.py); they are Mojang's, and so
are the recoloured results - see the credits in the README.
"""
import colorsys
import random
from pathlib import Path

from PIL import Image

from pixelart import hexc
from retexture import gradient, vanilla

TEX = Path(__file__).resolve().parents[2] / "src/main/resources/assets/astralend/textures"


def item(name):
    return vanilla(name, "item")


def hsv(p):
    return colorsys.rgb_to_hsv(p[0] / 255, p[1] / 255, p[2] / 255)


def only(img, keep, *stops):
    """`gradient`, but only on the pixels where keep(pixel) holds; the rest stay as they are."""
    part = Image.new("RGBA", img.size, (0, 0, 0, 0))
    for y in range(img.height):
        for x in range(img.width):
            p = img.getpixel((x, y))
            if p[3] and keep(p, x, y):
                part.putpixel((x, y), p)
    out = img.copy()
    if part.getbbox():
        out.alpha_composite(gradient(part, *stops))
    return out


def diff(a, b):
    """The pixels of `a` that `b` doesn't have (or has in another colour)."""
    out = Image.new("RGBA", a.size, (0, 0, 0, 0))
    for y in range(a.height):
        for x in range(a.width):
            if a.getpixel((x, y))[3] and a.getpixel((x, y)) != b.getpixel((x, y)):
                out.putpixel((x, y), a.getpixel((x, y)))
    return out


def put(img, points, colour):
    c = hexc(colour)
    for p in points:
        img.putpixel(p, c)
    return img


# ------------------------------------------------------------------ the Leviathan's bone variants
# The Leviathans are vanilla bone block; these are its weathered and lichened versions, as vanilla has
# mossy and cracked stone bricks.
AGED = ("6c6456", "8e8676", "aca492", "c6bfac")


def cracks():
    """Where cracked stone bricks are cracked: the pixels much darker than plain stone bricks'."""
    plain, cracked = vanilla("stone_bricks"), vanilla("cracked_stone_bricks")
    out = []
    for y in range(16):
        for x in range(16):
            a, b = plain.getpixel((x, y)), cracked.getpixel((x, y))
            if sum(b[:3]) < sum(a[:3]) - 60:
                out.append((x, y))
    return out


def lichened(face):
    img = vanilla(f"bone_block_{face}")
    lichen = vanilla("glow_lichen")
    if face == "side":   # creeping down from the top edge, thinning out
        rnd = random.Random(7)
        for y in range(16):
            for x in range(16):
                if y > 4 + rnd.randint(0, 6):
                    lichen.putpixel((x, y), (0, 0, 0, 0))
    img.alpha_composite(lichen)
    return img


# ------------------------------------------------------------------ blocks
def chiseled_nightstone():
    return gradient(vanilla("chiseled_polished_blackstone"), "15121f", "241e36", "352c4e", "5a4d7b")


FROST = ("3e5e7e", "7aa6c4", "bcdcf0", "f4fbff")


def comet_cress(stage):
    return gradient(vanilla(f"wheat_stage{(0, 3, 5, 7)[stage]}"), *FROST)


def end_chimes():
    """End Rods hung from a bar of chain: a chime of the End's own light."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    chain, rod = vanilla("chain"), vanilla("end_rod")
    dark = chain.getpixel((1, 1))
    for x in range(1, 15):
        img.putpixel((x, 1), hexc("2a2836"))
        img.putpixel((x, 2), hexc("3e3c4e"))
    for x, length in ((2, 9), (7, 12), (12, 7)):       # End Rods are two pixels wide
        for dx in (0, 1):
            img.putpixel((x + dx, 3), dark if dark[3] else hexc("3e3c4e"))
            for i in range(length):
                c = rod.getpixel((dx, min(15, 1 + i)))
                img.putpixel((x + dx, 4 + i), c if c[3] else hexc("e8e4f0"))
    return img


def astral_forge(face):
    """A smithing table, in purpur and obsidian's colours instead of dark oak's and iron's."""
    return gradient(vanilla(f"smithing_table_{face}"), "241a36", "4a3668", "7a5c9e", "c8b4ea")


def collector_pedestal(face):
    return gradient(vanilla(f"lodestone_{face}"), "5a4424", "947444", "c8a870", "efe0b8")


TURF = ("7a5c1c", "a8862c", "cca440", "e8cc6c")


def living_turf_top():
    return gradient(vanilla("grass_block_top"), *TURF)


def living_turf_side():
    img = vanilla("end_stone")
    img.alpha_composite(gradient(vanilla("grass_block_side_overlay"), *TURF))
    return img


# ------------------------------------------------------------------ inventory icons of the small plants
def shimmer_grass_item():
    return gradient(vanilla("short_grass"), "6e6a62", "8e897f", "c4beb0", "e4ded0")


def sunscale_lichen_item():
    return gradient(vanilla("glow_lichen"), "8a3a14", "c8661e", "eea040", "ffd890")


# ------------------------------------------------------------------ items
def bottled_flare():
    src = item("experience_bottle")
    green = lambda p, x, y: 0.15 < hsv(p)[0] < 0.45 and hsv(p)[1] > 0.3
    return only(src, green, "a0440a", "e8861c", "ffc848", "fff4b8")


def flare_bottle():
    """The empty bottle a flare is caught in: a glass bottle with a gold-banded neck."""
    img = item("glass_bottle")
    cork = lambda p, x, y: 0.02 < hsv(p)[0] < 0.12 and hsv(p)[1] > 0.4
    return only(img, cork, "7a5418", "c8a24c", "f0d88a")


def curators_core():
    return gradient(item("heart_of_the_sea"), "1e0a3a", "4a2486", "8a52d6", "d4b4ff")


def cracked_lens():
    """A round lens of pale glass, cracked across."""
    img = gradient(item("ender_pearl"), "5a7488", "9cbccc", "d8ecf4", "ffffff")
    return put(img, [(5, 4), (6, 5), (7, 6), (7, 7), (8, 8), (9, 9), (9, 10), (10, 11), (8, 7), (9, 6)], "3a4a58")


def starsap():
    return gradient(item("resin_clump"), "6a2e08", "b46612", "eea436", "ffe08c")


def starforge_alloy():
    return gradient(item("netherite_ingot"), "1c1630", "3a3058", "655494", "aa9ae0")


def chorus_flower_on_a_stick():
    """A carrot on a stick with a chorus flower on the line instead of the carrot."""
    src = item("carrot_on_a_stick")
    orange = lambda p, x, y: 0.03 < hsv(p)[0] < 0.13 and hsv(p)[1] > 0.55 and hsv(p)[2] > 0.5
    green = lambda p, x, y: 0.2 < hsv(p)[0] < 0.45 and hsv(p)[1] > 0.4
    out = only(src, orange, "6a3a6e", "a070aa", "d4acdc", "f2e2f6")
    return only(out, green, "3a1e4a", "5e3a72")


def starforge_template():
    return gradient(item("netherite_upgrade_smithing_template"), "1e1428", "3e2a56", "6a4c8e", "e8dcff")


def gilded_trim_template():
    return gradient(item("spire_armor_trim_smithing_template"), "5a3c0c", "9a7020", "d0a848", "f6e6a8")


def glowing_stew():
    """Suspicious stew's bowl, filled with something that glows teal."""
    stew, bowl = item("suspicious_stew"), item("bowl")
    out = bowl.copy()
    out.alpha_composite(gradient(diff(stew, bowl), "145858", "2e9e92", "6ee0cc", "d4fff4"))
    return out


def banner_base():
    """The banner pattern item with its emblem taken off (where vanilla's patterns disagree), the
    emblem's ink colour, and the box the emblems sit in."""
    pats = [item(f"{n}_banner_pattern") for n in ("creeper", "flower", "globe", "skull")]
    base = pats[0].copy()
    differ = [(x, y) for y in range(16) for x in range(16) if len({p.getpixel((x, y)) for p in pats}) > 1]
    agree = [pats[0].getpixel((x, y)) for y in range(16) for x in range(16)
             if (x, y) not in differ and pats[0].getpixel((x, y))[3]]
    paper = max(set(agree), key=agree.count)
    inks = [pats[0].getpixel(p) for p in differ if pats[0].getpixel(p) != paper]
    ink = max(set(inks), key=inks.count)
    for p in differ:
        base.putpixel(p, paper)
    xs, ys = [x for x, _ in differ], [y for _, y in differ]
    return base, ink, (min(xs), min(ys), max(xs), max(ys))


def banner(emblem):
    base, ink, (x0, y0, x1, y1) = banner_base()
    ox = x0 + (x1 - x0 + 1 - len(emblem[0])) // 2
    oy = y0 + (y1 - y0 + 1 - len(emblem)) // 2
    for y, row in enumerate(emblem):
        for x, ch in enumerate(row):
            if ch == "#":
                base.putpixel((ox + x, oy + y), ink)
    return base


SPIRE = ["...#...",
         "...#...",
         "..###..",
         "..#.#..",
         "..###..",
         ".##.##.",
         ".#####."]
WYRM = [".###...",
        "#...#..",
        "....#..",
        "..##...",
        ".#.....",
        ".#...#.",
        "..###.."]


# ------------------------------------------------------------------ drawn in vanilla's manner
# Where a recolour of a vanilla item would just be that item again, the mod's own drawing instead:
# 16x16, a dark outline in the item's own darkest shade, three or four tones lit from the top left.
def draw(rows, palette):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                img.putpixel((x, y), hexc(palette[ch]))
    return img


def lumineshroom_drawn(cap, stem):
    """A mushroom with a broad domed cap, in the colours the Lumineshrooms always had."""
    return draw(["................",
                 "................",
                 "................",
                 ".....aaaaaa.....",
                 "...aabcccbbaa...",
                 "..abccdddccbba..",
                 "..abcdddddcbba..",
                 ".aabbcccccbbbaa.",
                 ".aeeeeeeeeeeeea.",
                 "......fggf......",
                 "......fggf......",
                 "......fghf......",
                 "......fghf......",
                 ".....ffghff.....",
                 ".....ffffff.....",
                 "................"], {**cap, **stem})


def lumineshroom():
    return lumineshroom_drawn({"a": "0e4a4a", "b": "176a6a", "c": "30beb3", "d": "8ae8dc", "e": "125656"},
                              {"f": "8a82a0", "g": "dcd5ea", "h": "b8aecb"})


def ashen_lumineshroom():
    return lumineshroom_drawn({"a": "2c2a30", "b": "403d44", "c": "67626a", "d": "8b868e", "e": "343238"},
                              {"f": "6e6862", "g": "b4ae9e", "h": "a39d93"})


def charged_void_crystal():
    """The Void Crystal, charged: the same gem, lit up from inside, with a glint or two about it."""
    src = Image.open(TEX / "item/void_crystal.png").convert("RGBA")
    img = gradient(src, "4a2a98", "8a5ae8", "c8a8ff", "fbf6ff")
    return put(img, [(13, 2), (2, 12), (14, 11)], "fff4c8")


def flare_pollen():
    return draw(["................",
                 "................",
                 "..........e.....",
                 "....e...........",
                 "........e.......",
                 "................",
                 "................",
                 "................",
                 "................",
                 ".......aa.......",
                 ".....aacdaa.....",
                 "....acbcbdca....",
                 "...acbdbcbbca...",
                 "..abcbbcbdbbca..",
                 "..aaaaaaaaaaaa..",
                 "................"], {"a": "8a3e0c", "b": "d07a1c", "c": "f2b23a", "d": "ffe68a", "e": "fff4c8"})


def umbral_membrane():
    """A phantom membrane's shape, in the Umbral Stalker's own black and lilac."""
    return gradient(item("phantom_membrane"), "120a18", "2e1a36", "6a3a72", "d098d8")


def hollow_thorn():
    """A length of the Hollow Vines' stem, drawn as a stick is, black, with its thorns standing off it."""
    img = gradient(item("stick"), "0c0812", "1e1628", "342848", "5a4878")
    put(img, [(9, 4), (12, 7), (5, 8), (8, 11), (2, 11)], "2a2038")     # thorns, off both sides
    return put(img, [(8, 3), (13, 8), (4, 7), (9, 12), (1, 10)], "8a74b8")    # their pale points


def chart_fragment():
    """A torn-off corner of a Star Chart: its gold frame along two edges, night blue, three stars."""
    return draw(["................",
                 "................",
                 "..aaaaaaaa......",
                 "..aggggggga.....",
                 "..agnnnnnnna....",
                 "..agnsnnnnnna...",
                 "..agnnlnnnnna...",
                 "..agnnnlsnnnna..",
                 "..agnnnnnnnna...",
                 "..agnnsnnnnnna..",
                 "..agnnnlnnna....",
                 "..agnnnnsna.....",
                 "..agnnnna.......",
                 "..aaaaa.........",
                 "................",
                 "................"], {"a": "4a3010", "g": "e0b448", "n": "1e2a56", "s": "fff4c8", "l": "5a6c9e"})


def rift_pearl():
    """A pearl gone dark, a rift of light cracked across it."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d < 6.0:
                lit = (x - 7.5) + (y - 7.5) < -3
                c = "100820" if d > 5.0 else ("4a2a7a" if lit else "2a1a4a")
                img.putpixel((x, y), hexc(c))
    put(img, [(5, 4), (5, 5)], "6a4aa0")
    crack = [(4, 5), (5, 6), (6, 6), (7, 7), (7, 8), (8, 8), (9, 9), (10, 10), (11, 10)]
    put(img, crack, "e070ff")
    return put(img, [(7, 7), (8, 8)], "ffe8ff")


def portal_lens():
    """A round lens of violet glass, highlighted as vanilla's glass is, in a spyglass's copper rim."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d < 6.6:
                if d > 5.6:
                    c = "5a2c18"
                elif d > 4.6:
                    c = "e0905a" if (x - 7.5) + (y - 7.5) < 0 else "a8582c"
                else:
                    c = "7a4ab0" if (x - 7.5) + (y - 7.5) < -2 else "5a3290" if (x - 7.5) + (y - 7.5) < 3 else "42226e"
                img.putpixel((x, y), hexc(c))
    return put(img, [(5, 6), (6, 5), (5, 7), (7, 5), (9, 10), (10, 9)], "e8dcff")


def temporal_crystal():
    """A little cluster of pale teal crystal, flecked with gold, as an amethyst cluster grows."""
    return draw(["................",
                 "................",
                 ".......a........",
                 "......aca.......",
                 "......aca...a...",
                 "..a...abca.aca..",
                 ".aca..abca.acca.",
                 ".abca.abcga.bca.",
                 ".abca.abbca.bca.",
                 "..abcaabbcaabca.",
                 "..abbaabgbaabba.",
                 "..abbcabbbaabca.",
                 "...abbabbbabba..",
                 "...aaaaaaaaaaa..",
                 "................",
                 "................"], {"a": "0e3a44", "b": "3aa0a8", "c": "a8f0ec", "g": "f0d070"})


def star_chart():
    """A chart of the sky in a gold frame: night blue, a constellation of five stars joined up."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(1, 15):
        for x in range(1, 15):
            edge = x in (1, 14) or y in (1, 14)
            rim = x in (2, 13) or y in (2, 13)
            if edge:
                c = "5a3c10"
            elif rim:
                c = "f0d070" if x + y < 15 else "c8a24c"
            else:
                c = "2a3a6e" if x + y < 12 else "1e2850" if x + y < 20 else "141a38"
            img.putpixel((x, y), hexc(c))
    stars = [(4, 10), (6, 5), (8, 8), (10, 4), (11, 9)]
    for (x0, y0), (x1, y1) in zip(stars, stars[1:]):
        n = max(abs(x1 - x0), abs(y1 - y0))
        put(img, [(round(x0 + (x1 - x0) * i / n), round(y0 + (y1 - y0) * i / n)) for i in range(1, n)], "5a6c9e")
    put(img, stars, "fff4c8")
    put(img, [(8, 8)], "ffffff")
    return put(img, [(5, 12), (12, 12), (4, 4)], "8a9ac8")


def music_disc():
    """Vanilla's dark disc, its label a four-pointed star in lavender and white."""
    src = item("music_disc_13")
    body = ["101014", "1e1e26", "2e2e3a", "44445a"]
    out = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            p = src.getpixel((x, y))
            if p[3]:
                lum = (p[0] + p[1] + p[2]) / 765
                out.putpixel((x, y), hexc(body[min(3, int(lum * 4 * 1.6))]))
    label = [".....", "..s..", ".sws.", "..s..", "....."]
    for y, row in enumerate(label):
        for x, ch in enumerate(row):
            if ch != ".":
                out.putpixel((5 + x, 5 + y), hexc({"s": "b48ae8", "w": "ffffff"}[ch]))
    return out


def sunshade():
    """A window-frame of cut copper with a thin, tinted film stretched in it: you see the sky through it."""
    frame = vanilla("cut_copper")
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            if x in (0, 7, 8, 15) or y in (0, 7, 8, 15):
                img.putpixel((x, y), frame.getpixel((x, y)))
            else:
                sheen = (x + y) % 7 == 0
                img.putpixel((x, y), hexc("4a3a6a" if sheen else "2c2444", 190 if sheen else 150))
    return img


def maw_eye(lit):
    """The Leviathan's eye socket in a face of bone block: a deep pit; once the skull has woken, light
    welling up out of it and running out along cracks in the bone."""
    img = vanilla("bone_block_side")
    glow = ["f4d8ff", "d090f8", "a050e0", "6a1e9e", "3a0e5a"]
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d < 4.8:
                if d > 4.0:
                    c = "4a4234"
                elif not lit:
                    c = "0c0a0e" if d < 3.2 else "2a241e"
                else:
                    c = glow[min(4, int((d / 4.0 + 0.05) * 5))]
                img.putpixel((x, y), hexc(c))
    if lit:
        for line in ([(4, 4), (3, 3), (3, 2), (2, 1)], [(11, 4), (12, 3), (12, 2), (13, 1)],
                     [(4, 11), (3, 12), (2, 12), (1, 13)], [(11, 11), (12, 12), (13, 12), (14, 13)], [(8, 3), (8, 2), (9, 1)]):
            put(img, line[:2], "c070f0")
            put(img, line[2:], "7a2ec0")
    return img


def meteoric_iron_block():
    """Iron block's bevelled frame; inside, the long crossing bands an etched iron meteorite shows."""
    img = gradient(vanilla("iron_block"), "2e3040", "4c5064", "727890", "a4abc0")
    for y in range(2, 14):
        for x in range(2, 14):
            if (x + y) % 5 == 0:
                c = "a8b0c4"
            elif (x - y) % 5 == 0:
                c = "3c4052"
            else:
                c = "5a6074" if (x * 3 + y) % 4 else "666c80"
            img.putpixel((x, y), hexc(c))
    return img


def weathered(face):
    """Bone block, greyed with age, pitted, and cracked here and there."""
    img = gradient(vanilla(f"bone_block_{face}"), *AGED)
    rnd = random.Random(31 if face == "side" else 32)
    for _ in range(9):
        put(img, [(rnd.randrange(16), rnd.randrange(16))], "7e7666")
    crack = [(3, 2), (4, 3), (4, 4), (5, 5)] if face == "side" else [(9, 3), (10, 4), (10, 5)]
    crack += [(11, 10), (11, 11), (12, 12)] if face == "side" else [(4, 11), (5, 12)]
    return put(img, crack, "5a5446")


def nebula_tufts_item():
    """Vanilla grass's full-size shape, rose, with the cold blue tips the tufts have."""
    img = gradient(vanilla("short_grass"), "6e2046", "a43a68", "d8709c", "f4b8d4")
    tips = [(x, y) for y in range(16) for x in range(16)
            if img.getpixel((x, y))[3] and (y == 0 or not img.getpixel((x, y - 1))[3])]
    return put(img, tips, "d1ecfb")


def pedestal(name):
    """What a Collector Pedestal holds, as a block texture (block models can't use item textures)."""
    return item("glass_bottle") if name == "glass_bottle" else Image.open(TEX / f"item/{name}.png").convert("RGBA").copy()


TEXTURES = [
    ("block/weathered_leviathan_bone_side.png", lambda: weathered("side")),
    ("block/weathered_leviathan_bone_top.png", lambda: weathered("top")),
    ("block/lichened_leviathan_bone_side.png", lambda: lichened("side")),
    ("block/lichened_leviathan_bone_top.png", lambda: lichened("top")),
    ("block/maw_eye.png", lambda: maw_eye(False)),
    ("block/maw_eye_lit.png", lambda: maw_eye(True)),
    ("block/meteoric_iron_block.png", meteoric_iron_block),
    ("block/lumineshroom.png", lumineshroom),
    ("block/ashen_lumineshroom.png", ashen_lumineshroom),
    ("block/chiseled_nightstone.png", chiseled_nightstone),
    *[(f"block/comet_cress_stage{i}.png", (lambda i=i: comet_cress(i))) for i in range(4)],
    ("item/comet_cress.png", lambda: comet_cress(3)),
    ("block/sunshade.png", sunshade),
    ("block/end_chimes.png", end_chimes),
    ("block/astral_forge_front.png", lambda: astral_forge("front")),
    ("block/astral_forge_side.png", lambda: astral_forge("side")),
    ("block/astral_forge_top.png", lambda: astral_forge("top")),
    ("block/collector_pedestal_side.png", lambda: collector_pedestal("side")),
    ("block/collector_pedestal_top.png", lambda: collector_pedestal("top")),
    ("block/living_turf_top.png", living_turf_top),
    ("block/living_turf_side.png", living_turf_side),
    ("item/shimmer_grass.png", shimmer_grass_item),
    ("item/nebula_tufts.png", nebula_tufts_item),
    ("item/sunscale_lichen.png", sunscale_lichen_item),
    ("item/bottled_flare.png", bottled_flare),
    ("item/curators_core.png", curators_core),
    ("item/charged_void_crystal.png", charged_void_crystal),
    ("item/flare_pollen.png", flare_pollen),
    ("item/umbral_membrane.png", umbral_membrane),
    ("item/cracked_lens.png", cracked_lens),
    ("item/chart_fragment.png", chart_fragment),
    ("item/hollow_thorn.png", hollow_thorn),
    ("item/starsap.png", starsap),
    ("item/rift_pearl.png", rift_pearl),
    ("item/starforge_alloy.png", starforge_alloy),
    ("item/temporal_crystal.png", temporal_crystal),
    ("item/portal_lens.png", portal_lens),
    ("item/star_chart.png", star_chart),
    ("item/chorus_flower_on_a_stick.png", chorus_flower_on_a_stick),
    ("item/starforge_template.png", starforge_template),
    ("item/eclipse_armor_trim_smithing_template.png", gilded_trim_template),
    ("item/music_disc_observing_the_star.png", music_disc),
    ("item/glowing_stew.png", glowing_stew),
    ("item/spire_banner_pattern.png", lambda: banner(SPIRE)),
    *[(f"block/pedestal_{n}.png", (lambda n=n: pedestal(n))) for n in ("glass_bottle", "charged_void_crystal", "bottled_flare")],
    ("item/wyrm_banner_pattern.png", lambda: banner(WYRM)),
]


def main():
    for rel, make in TEXTURES:
        make().save(TEX / rel)
    print(f"Wrote {len(TEXTURES)} textures")


if __name__ == "__main__":
    main()
