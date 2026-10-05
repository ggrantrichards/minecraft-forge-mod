"""Phase 10 retexture: the commonest blocks made from vanilla's own textures. Each starts from the
vanilla block it is kin to and keeps that block's pixel pattern exactly; only the colours change
(a hue shift, or a gradient map onto a new ramp of tones).

  Pale End Stone    end stone, bleached cool grey
  Sunscorched Stone end stone (turned a quarter), sun-baked amber
  Chorus Moss       moss block, in violet
  Chorus Stalk      crimson stem (side and end), shifted to chorus magenta
  Chorus Bloom      cherry leaves, shifted to chorus violet
  Lumine Cap        shroomlight, in cool blue
  Glimmer Crystal   amethyst block, gone smoky grey violet
  Leviathan Bone    bone block (side and end), aged to a grey ivory
  Nightstone        blackstone's chunky rock, in dark violet (drawn, kept)
  Charred End Stone end stone's pits, scorched, two embers (drawn, kept)

The vanilla textures are fetched once into tools/textures/.vanilla (git-ignored); they are Mojang's,
and so are the recoloured results - see the credits in the README.

Imported by phase10_textures (its TEXTURES list goes last, so these replace the older drawings)."""
import colorsys
import urllib.request
from pathlib import Path

from PIL import Image

from pixelart import chunks, hexc, ramp

VANILLA_URL = "https://raw.githubusercontent.com/InventivetalentDev/minecraft-assets/1.21.4/assets/minecraft/textures/{}.png"
CACHE = Path(__file__).resolve().parent / ".vanilla"


def vanilla(name, kind="block"):
    """A vanilla block (or item) texture (the first frame, if it's animated)."""
    path = CACHE / (f"{name}.png" if kind == "block" else f"{kind}/{name}.png")
    if not path.exists():
        path.parent.mkdir(parents=True, exist_ok=True)
        with urllib.request.urlopen(VANILLA_URL.format(f"{kind}/{name}"), timeout=30) as r:
            path.write_bytes(r.read())
    return Image.open(path).convert("RGBA").crop((0, 0, 16, 16))


def shift(img, hue=0.0, sat=1.0, val=1.0, lift=0.0):
    """Every pixel's hue turned by `hue` (0..1 = a full turn), saturation and value scaled, value lifted."""
    out = img.copy()
    px = out.load()
    for y in range(16):
        for x in range(16):
            r, g, b, a = px[x, y]
            h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            h = (h + hue) % 1.0
            s = min(1.0, s * sat)
            v = min(1.0, max(0.0, v * val + lift))
            r, g, b = colorsys.hsv_to_rgb(h, s, v)
            px[x, y] = (round(r * 255), round(g * 255), round(b * 255), a)
    return out


def gradient(img, *stops):
    """A gradient map: each pixel's brightness, stretched over the texture's own range, picks a tone
    along `stops` (dark to light). The pattern and the number of tones stay vanilla's."""
    out = img.copy()
    px = out.load()

    def lum(p):
        return 0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2]
    ls = [lum(px[x, y]) for y in range(16) for x in range(16) if px[x, y][3]]
    lo, hi = min(ls), max(ls)
    cs = [hexc(c) for c in stops]
    for y in range(16):
        for x in range(16):
            p = px[x, y]
            if not p[3]:
                continue
            t = (lum(p) - lo) / (hi - lo) if hi > lo else 0.5
            k = min(len(cs) - 2, int(t * (len(cs) - 1)))
            f = t * (len(cs) - 1) - k
            d, l = cs[k], cs[k + 1]
            px[x, y] = tuple(round(d[i] + (l[i] - d[i]) * f) for i in range(3)) + (p[3],)
    return out


def ore(host, vanilla_ore, *stops):
    """An ore the vanilla way: the host stone, with the ore's clusters (the pixels where the vanilla
    ore differs from vanilla stone) laid over it, recoloured along `stops`."""
    src, stone = vanilla(vanilla_ore), vanilla("stone")
    clusters = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            if src.getpixel((x, y)) != stone.getpixel((x, y)):
                clusters.putpixel((x, y), src.getpixel((x, y)))
    out = host.copy()
    out.alpha_composite(gradient(clusters, *stops))
    return out


def pale_end_stone():
    return gradient(vanilla("end_stone"), "a6a2b4", "e4e2ec")


# Sunscorched Stone: the Eclipse's hell half, crimson - the Horsehead Nebula's glow - but End Stone's own
# pitted pattern and as few tones, so it reads as the End, not the Nether.
CRIMSON = ("4e1222", "6c1e32", "8c2c44", "a8405a")


def sunscorched_stone():
    return gradient(vanilla("end_stone").transpose(Image.Transpose.ROTATE_90), *CRIMSON)


def chorus_moss():
    """Moss in violet, split-toned: its shadows lean blue, its tufts lean pink - livelier than a recolour."""
    img = shift(vanilla("moss_block").transpose(Image.Transpose.FLIP_LEFT_RIGHT), hue=0.55, sat=0.62, val=1.25)
    px = img.load()
    for y in range(16):
        for x in range(16):
            r, g, b, a = px[x, y]
            h, s_, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            h = (h + (v - 0.55) * 0.12) % 1.0
            r, g, b = colorsys.hsv_to_rgb(h, s_, v)
            px[x, y] = (round(r * 255), round(g * 255), round(b * 255), a)
    return img


# Chorus wood: the crimson stem's braided veins (flipped, so they don't line up with crimson's), its
# bark in chorus purple and its veins in the lavender of chorus fruit, with a pale flower-white node
# where a few veins meet.
CHORUS_VEIN = [hexc("94609e"), hexc("ad7cb8"), hexc("c79ed0"), hexc("e4c8ea")]
_CRIMSON_VEIN = {(123, 0, 0): 0, (137, 15, 15): 1, (150, 21, 21): 2, (177, 39, 39): 3,
                 (148, 24, 24): 1, (172, 32, 32): 3}


def _chorus_wood(name, nodes=()):
    src = vanilla(name)
    img = shift(src, hue=-0.12, sat=0.48, val=1.45)
    px, sp = img.load(), src.load()
    for y in range(16):
        for x in range(16):
            i = _CRIMSON_VEIN.get(sp[x, y][:3])
            if i is not None:
                px[x, y] = CHORUS_VEIN[i]
    for (x, y) in nodes:
        px[x, y] = hexc("f6eafa")
    return img.transpose(Image.Transpose.FLIP_LEFT_RIGHT)


def chorus_stalk():
    return _chorus_wood("crimson_stem")


def chorus_stalk_top():
    return _chorus_wood("crimson_stem_top")


def stripped_chorus_stalk():
    return shift(vanilla("stripped_crimson_stem"), hue=-0.12, sat=0.42, val=1.6)


def stripped_chorus_stalk_top():
    return shift(vanilla("stripped_crimson_stem_top"), hue=-0.12, sat=0.42, val=1.6)


def chorus_wood(name, kind="block"):
    return lambda: shift(vanilla(name, kind), hue=-0.12, sat=0.48, val=1.45)


def chorus_bloom():
    """Not a tree's leaves: clusters of chorus blossoms. Cherry leaves' shape, its petals in the tones
    of the chorus flower (lilac to near-white), its little green leaves left open."""
    src = vanilla("cherry_leaves")
    img = src.copy()
    px = img.load()
    pal = [hexc("563a5e"), hexc("7c5b8a"), hexc("ab8ab4"), hexc("d6bbdc"), hexc("eed6f0")]
    tones = sorted({sum(p[:3]) for p in src.getdata() if p[3] and p[1] <= p[0]})
    for y in range(16):
        for x in range(16):
            r, g, b, a = px[x, y]
            if not a:
                continue
            if g > r:
                px[x, y] = (0, 0, 0, 0)               # a green leaf: left open
            else:
                px[x, y] = pal[tones.index(r + g + b) * len(pal) // len(tones)]
    return img


def lumine_cap():
    """Pale purpur, within the End's own range: few tones, low saturation, little contrast."""
    return gradient(vanilla("shroomlight"), "8c6e8e", "a084a2", "b49ab6", "c6b0c8")


# Glimmer Crystal: amethyst gone smoky - a dull grey violet, so it reads as part of the End's restricted
# palette and leaves the vivid violet to Void Crystal.
SMOKY = ("2a2434", "4e4660", "7c7290", "b4aac6")


def glimmer_crystal():
    return gradient(vanilla("amethyst_block"), *SMOKY)


def leviathan_bone_side():
    return gradient(vanilla("bone_block_side"), "aeaa9c", "e2dfd2")


def leviathan_bone_top():
    return gradient(vanilla("bone_block_top"), "9c988a", "e2dfd2")


def pale_end_stone_bricks():
    return gradient(vanilla("end_stone_bricks"), "a6a2b4", "e4e2ec")


def sunscorched_stone_bricks():
    return gradient(vanilla("end_stone_bricks"), *CRIMSON)


def polished_sunscorched_stone():
    return gradient(vanilla("sandstone_top"), *CRIMSON)


# Lumine wood: the warped fungus's stem and planks, cooled from teal to the cap's blue; the stem's
# dark bark turned a pale lavender grey, its veins the cap's glowing blue.
def lumine_wood(name, kind="block"):
    return lambda: shift(vanilla(name, kind), hue=0.08, sat=0.7, val=1.2)


def two_tone(img, is_a, stops_a, stops_b):
    """Gradient-maps the pixels where is_a(pixel) along stops_a, the rest along stops_b."""
    a = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    b = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            p = img.getpixel((x, y))
            (a if is_a(p) else b).putpixel((x, y), p)
    out = gradient(a, *stops_a)
    out.alpha_composite(gradient(b, *stops_b))
    return out


def _lumine_stem(name):
    """Cream, like End Stone and the chorus flower's pale petals."""
    return lambda: two_tone(vanilla(name), lambda p: p[1] > p[0], ("b6b094", "c6c0a2", "d4ceb0"),
                            ("bcb698", "cbc5a6", "dad4b6"))


def stardust_block():
    """Sand made of starlight: vanilla sand in pale violet, with a few glittering grains."""
    img = gradient(vanilla("sand"), "9a86c0", "c8b8e4", "e4dcf4")
    for (x, y) in ((3, 2), (12, 6), (7, 11), (14, 14)):
        img.putpixel((x, y), hexc("fff6d4"))
    return img


# Nullwood: dark oak, in the deep violet of the Hollow Isles.
NULL = ("120c22", "2e2150", "5a4590", "8a74c4")


def nullwood(name, kind="block"):
    return lambda: gradient(vanilla(name, kind), *NULL)


def hollow_vines(name):
    """The Deep Hollows' hanging vines: weeping vines in the forest's black violet, their tips a pale
    glowing violet (they light the Hollows' undersides)."""
    if name == "weeping_vines":
        return lambda: gradient(vanilla(name), "33245a", "8a70d0", "d8c8ff")
    return lambda: gradient(vanilla(name), "120a20", "33245a", "5e4a9c")


# The Eclipse's heaven half: a muted teal moss, only round the water of its oases (the rest is bare End
# Stone), and its breathing light.
TURF = ("34524e", "426660", "507870", "5c887e")


def living_turf_top():
    return gradient(vanilla("moss_block"), *TURF)


def living_turf_side():
    """End Stone with the moss's edge over its top: a ragged lip one to three pixels deep, not grass."""
    img = vanilla("end_stone").copy()
    top = living_turf_top()
    for x in range(16):
        depth = 1 + (x * 7 + 3) % 5 // 2      # 1..3, uneven
        for y in range(depth):
            img.putpixel((x, y), top.getpixel((x, (y + 5) % 16)))
    return img


BREATH = (("6a6082", "7c7296", "9086aa"), ("7a6e98", "9084ae", "a69ac4"), ("8c80b0", "a89cc8", "c6bcde"),
          ("a498c8", "cec4e8", "f0ecfc"))   # lavender, dim to bright


def breathcomb(stage):
    return lambda: gradient(vanilla("honeycomb_block"), *BREATH[stage])


def hushfleece():
    """A tuft of the Hushback's fleece: vanilla's string-white feather, pale and cool."""
    return gradient(vanilla("feather", "item"), "a8b4c0", "d8e2ea", "f8fcff")


def hushback_flank(cooked):
    return lambda: gradient(vanilla("cooked_mutton" if cooked else "mutton", "item"),
                            *(("4a2e24", "8a5a40", "c8946a", "e8c8a0") if cooked else ("6a4a58", "b08494", "dcb8c4", "f0e4ea")))


def curators_core():
    """What's left of the Curator: a heart of the sea gone obsidian-black, a violet slit of light in it."""
    return gradient(vanilla("heart_of_the_sea", "item"), "0c0814", "2a1a44", "6a3cc4", "d8b8ff")


def memory_moss():
    """Pale grey-violet moss: vanilla's moss block, drained of its green."""
    return gradient(vanilla("moss_block"), "6e6a7c", "9a96aa", "c4c0d2", "e0dcea")


def mimic_plant():
    """A dark shrub: vanilla's fern, near black, a little violet at the tips."""
    return gradient(vanilla("fern"), "0c0a12", "1e1a2c", "3a3254", "5c5080")


def purpur_bricks():
    return gradient(vanilla("stone_bricks"), "5e3e68", "8c6494", "ae86b4", "c8a6cc")


def nullwood_leaves():
    return gradient(vanilla("dark_oak_leaves"), "160f2a", "33265e", "6a54b0")


def meteoric_iron_ore():
    return ore(charred_end_stone(), "iron_ore", "2a2b34", "7f8494", "eef0f6")


def solarite_ore():
    return ore(sunscorched_stone(), "gold_ore", "6e2e04", "e0820f", "fff2b0")


def void_crystal_ore():
    """Precious, and it should look it: deepslate diamond ore's clusters, in a vivid violet running to
    white, each sunk in a dark rim cut into the Nightstone so it stands out from the rock."""
    host = nightstone()
    src, stone = vanilla("deepslate_diamond_ore"), vanilla("deepslate")
    lit = {(x, y) for y in range(16) for x in range(16) if src.getpixel((x, y)) != stone.getpixel((x, y))}
    hp = host.load()
    for (x, y) in lit:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            q = ((x + dx) % 16, (y + dy) % 16)
            if q not in lit:
                hp[q] = hexc("07040c")
    clusters = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for (x, y) in lit:
        clusters.putpixel((x, y), src.getpixel((x, y)))
    host.alpha_composite(gradient(clusters, "3a1478", "9a5cff", "d8b8ff", "ffffff"))
    return host


METEORIC = ("24232c", "6c6f80", "dfe2ec")


def meteoric(name, kind="block"):
    return lambda: gradient(vanilla(name, kind), *METEORIC)


def solarite(name, kind="block"):
    return lambda: shift(vanilla(name, kind), hue=-0.055, sat=1.05, val=1.0)


def void_crystal_block():
    return shift(vanilla("budding_amethyst"), hue=0.015, sat=1.1, val=0.72)


def glimmer_cluster():
    return gradient(vanilla("amethyst_cluster"), *SMOKY)


def whalelight_scale():
    return gradient(vanilla("pearlescent_froglight_side"), "7a8fd0", "c4d4f6", "f6f9ff")


def petrified_stalk_side():
    return gradient(chorus_stalk(), "3e3a40", "8a8488", "c2bcbc")


def petrified_stalk_top():
    return gradient(chorus_stalk_top(), "3e3a40", "8a8488", "c2bcbc")


def spore_lantern():
    """A froglight's soft glowing grain, in the teal of the spores it's made from."""
    return gradient(vanilla("verdant_froglight_side"), "1d5e58", "3fb8a4", "9ff2d8", "eafff6")


def voidgel():
    """A slime block of the void: the same see-through jelly, deep violet, its little core darker."""
    return gradient(vanilla("slime_block"), "2b1356", "6b3fc4", "b894ff")


def lodestar_side():
    return gradient(vanilla("lodestone_side"), "23242e", "5d6274", "c9cede")


def lodestar_top():
    """The lodestone's top in meteoric steel, a violet star in the middle where the iron points."""
    img = gradient(vanilla("lodestone_top"), "23242e", "5d6274", "c9cede")
    star = {(7, 7): "f0e2ff", (8, 7): "f0e2ff", (7, 8): "f0e2ff", (8, 8): "f0e2ff",
            (6, 7): "a77bff", (9, 8): "a77bff", (7, 6): "a77bff", (8, 9): "a77bff",
            (6, 8): "7a4ae0", (9, 7): "7a4ae0", (8, 6): "7a4ae0", (7, 9): "7a4ae0"}
    for (x, y), c in star.items():
        img.putpixel((x, y), hexc(c))
    return img


# ---- drawn, kept from the first pass
def nightstone():
    return chunks(4101, 14, ramp("1a1626", "6e6290", 6, hue_shift=0.02, sat_curve=0.06), base_index=2, light=1, dark=1)


def charred_end_stone():
    img = chunks(4501, 18, ramp("1a1210", "6a5244", 5, hue_shift=-0.02, sat_curve=0.05), base_index=2, light=1, dark=1)
    px = img.load()
    for (x, y) in ((4, 11), (12, 3)):
        px[x, y] = hexc("9a3a14")
    return img


TEXTURES = [
    ("block/pale_end_stone.png", pale_end_stone),
    ("block/sunscorched_stone.png", sunscorched_stone),
    ("block/chorus_moss.png", chorus_moss),
    ("block/lumine_cap.png", lumine_cap),
    ("block/glimmer_crystal.png", glimmer_crystal),
    ("block/leviathan_bone_side.png", leviathan_bone_side),
    ("block/leviathan_bone_top.png", leviathan_bone_top),
    ("block/nightstone.png", nightstone),
    ("block/charred_end_stone.png", charred_end_stone),
    ("block/lumine_stem.png", _lumine_stem("warped_stem")),
    ("block/lumine_stem_top.png", _lumine_stem("warped_stem_top")),
    ("block/stardust_block.png", stardust_block),
    ("block/nullwood_log.png", nullwood("dark_oak_log")),
    ("block/nullwood_log_top.png", nullwood("dark_oak_log_top")),
    ("block/stripped_nullwood_log.png", nullwood("stripped_dark_oak_log")),
    ("block/stripped_nullwood_log_top.png", nullwood("stripped_dark_oak_log_top")),
    ("block/nullwood_planks.png", nullwood("dark_oak_planks")),
    ("block/nullwood_door_top.png", nullwood("dark_oak_door_top")),
    ("block/nullwood_door_bottom.png", nullwood("dark_oak_door_bottom")),
    ("block/nullwood_trapdoor.png", nullwood("dark_oak_trapdoor")),
    ("item/nullwood_door.png", nullwood("dark_oak_door", "item")),
    ("block/nullwood_leaves.png", nullwood_leaves),
    ("block/hollow_vines.png", hollow_vines("weeping_vines")),
    ("block/living_turf_top.png", living_turf_top),
    ("item/hushfleece.png", hushfleece),
    ("block/memory_moss.png", memory_moss),
    ("block/mimic_plant.png", mimic_plant),
    ("block/purpur_bricks.png", purpur_bricks),
    ("item/curators_core.png", curators_core),
    ("item/hushback_flank.png", hushback_flank(False)),
    ("item/cooked_hushback_flank.png", hushback_flank(True)),
    ("block/living_turf_side.png", living_turf_side),
    *[(f"block/breathcomb_{i}.png", breathcomb(i)) for i in range(4)],
    ("block/hollow_vines_plant.png", hollow_vines("weeping_vines_plant")),
    ("block/pale_end_stone_bricks.png", pale_end_stone_bricks),
    ("block/sunscorched_stone_bricks.png", sunscorched_stone_bricks),
    ("block/polished_sunscorched_stone.png", polished_sunscorched_stone),
    ("block/meteoric_iron_ore.png", meteoric_iron_ore),
    ("block/solarite_ore.png", solarite_ore),
    ("block/void_crystal_ore.png", void_crystal_ore),
    ("block/raw_meteoric_iron_block.png", meteoric("raw_iron_block")),
    ("block/meteoric_iron_block.png", meteoric("iron_block")),
    ("item/raw_meteoric_iron.png", meteoric("raw_iron", "item")),
    ("item/meteoric_iron_ingot.png", meteoric("iron_ingot", "item")),
    ("item/meteoric_iron_nugget.png", meteoric("iron_nugget", "item")),
    ("block/raw_solarite_block.png", solarite("raw_gold_block")),
    ("block/solarite_block.png", solarite("gold_block")),
    ("item/raw_solarite.png", solarite("raw_gold", "item")),
    ("item/solarite_ingot.png", solarite("gold_ingot", "item")),
    ("block/void_crystal_block.png", void_crystal_block),
    ("block/glimmer_cluster.png", glimmer_cluster),
    ("block/whalelight_scale.png", whalelight_scale),
    ("block/petrified_stalk_side.png", petrified_stalk_side),
    ("block/petrified_stalk_top.png", petrified_stalk_top),
    ("block/spore_lantern.png", spore_lantern),
    ("block/voidgel.png", voidgel),
    ("block/lodestar_side.png", lodestar_side),
    ("block/lodestar_top.png", lodestar_top),
]
