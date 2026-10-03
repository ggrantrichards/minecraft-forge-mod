"""Phase 10 retexture: the commonest blocks made from vanilla's own textures. Each starts from the
vanilla block it is kin to and keeps that block's pixel pattern exactly; only the colours change
(a hue shift, or a gradient map onto a new ramp of tones).

  Pale End Stone    end stone, bleached cool grey
  Sunscorched Stone end stone (turned a quarter), sun-baked amber
  Chorus Moss       moss block, in violet
  Chorus Stalk      crimson stem (side and end), shifted to chorus magenta
  Chorus Bloom      cherry leaves, shifted to chorus violet
  Lumine Cap        shroomlight, in cool blue
  Glimmer Crystal   amethyst block, in teal
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

VANILLA_URL = "https://raw.githubusercontent.com/InventivetalentDev/minecraft-assets/1.21.4/assets/minecraft/textures/block/{}.png"
CACHE = Path(__file__).resolve().parent / ".vanilla"


def vanilla(name):
    """A vanilla block texture (the first frame, if it's animated)."""
    path = CACHE / f"{name}.png"
    if not path.exists():
        CACHE.mkdir(exist_ok=True)
        with urllib.request.urlopen(VANILLA_URL.format(name), timeout=30) as r:
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


def gradient(img, dark, light):
    """A gradient map: each pixel's brightness, stretched over the texture's own range, picks a tone
    between `dark` and `light`. The pattern and the number of tones stay vanilla's."""
    out = img.copy()
    px = out.load()

    def lum(p):
        return 0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2]
    ls = [lum(px[x, y]) for y in range(16) for x in range(16) if px[x, y][3]]
    lo, hi = min(ls), max(ls)
    d, l = hexc(dark), hexc(light)
    for y in range(16):
        for x in range(16):
            p = px[x, y]
            t = (lum(p) - lo) / (hi - lo) if hi > lo else 0.5
            px[x, y] = tuple(round(d[i] + (l[i] - d[i]) * t) for i in range(3)) + (p[3],)
    return out


def pale_end_stone():
    return gradient(vanilla("end_stone"), "a6a2b4", "e4e2ec")


def sunscorched_stone():
    return gradient(vanilla("end_stone").transpose(Image.Transpose.ROTATE_90), "b48448", "efcf8e")


def chorus_moss():
    return shift(vanilla("moss_block"), hue=0.55, sat=0.62, val=1.25)


def chorus_stalk():
    return shift(vanilla("crimson_stem"), hue=-0.12, sat=0.48, val=1.45)


def chorus_stalk_top():
    return shift(vanilla("crimson_stem_top"), hue=-0.12, sat=0.48, val=1.45)


def chorus_bloom():
    """Cherry leaves in chorus violet; the little green leaves become a deep chorus purple."""
    img = shift(vanilla("cherry_leaves"), hue=-0.13, sat=1.0, val=0.92)
    src = vanilla("cherry_leaves").load()
    px = img.load()
    for y in range(16):
        for x in range(16):
            r, g, b, a = src[x, y]
            if a and g > r:   # a green leaf pixel
                v = (r + g + b) / (3 * 255)
                px[x, y] = (round(70 + 60 * v), round(36 + 30 * v), round(84 + 60 * v), a)
    return img


def lumine_cap():
    return shift(vanilla("shroomlight"), hue=0.50, sat=0.62, val=0.92)


def glimmer_crystal():
    return shift(vanilla("amethyst_block"), hue=-0.27, sat=0.85, val=1.0)


def leviathan_bone_side():
    return gradient(vanilla("bone_block_side"), "aeaa9c", "e2dfd2")


def leviathan_bone_top():
    return gradient(vanilla("bone_block_top"), "9c988a", "e2dfd2")


# ---- drawn, kept from the first pass
def nightstone():
    return chunks(4101, 14, ramp("0c0a12", "4a405e", 6, hue_shift=0.02, sat_curve=0.06), base_index=2, light=1, dark=1)


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
    ("block/chorus_stalk.png", chorus_stalk),
    ("block/chorus_stalk_top.png", chorus_stalk_top),
    ("block/chorus_bloom.png", chorus_bloom),
    ("block/lumine_cap.png", lumine_cap),
    ("block/glimmer_crystal.png", glimmer_crystal),
    ("block/leviathan_bone_side.png", leviathan_bone_side),
    ("block/leviathan_bone_top.png", leviathan_bone_top),
    ("block/nightstone.png", nightstone),
    ("block/charred_end_stone.png", charred_end_stone),
]
