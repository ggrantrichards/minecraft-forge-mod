"""Draws the commonest blocks of the mod as a sheet - each as an inventory-style cube and as a 4x4
tiled face (to show how it repeats), with where it's found and the vanilla block it's kin to.

    python3 tools/textures/block_sheet.py   -> docs/images/phase10/common_blocks.png
"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageEnhance, ImageFont

ROOT = Path(__file__).resolve().parents[2]
T = ROOT / "src/main/resources/assets/astralend/textures/block"
BLOCKS = [  # (name, top texture, side texture, where, vanilla kin)
    ("Nightstone", "nightstone", "nightstone", "Hollow Isles, Glimmering Crags", "blackstone"),
    ("Chorus Moss", "chorus_moss", "chorus_moss", "Nebula Fields, Chorus Thicket", "moss block"),
    ("Pale End Stone", "pale_end_stone", "pale_end_stone", "Ashen Barrens", "end stone"),
    ("Charred End Stone", "charred_end_stone", "charred_end_stone", "Comet Wake, the Corona", "end stone, scorched"),
    ("Sunscorched Stone", "sunscorched_stone", "sunscorched_stone", "The Corona", "sandstone"),
    ("Chorus Stalk", "chorus_stalk_top", "chorus_stalk", "Chorus Thicket", "cherry / mangrove log"),
    ("Chorus Bloom", "chorus_bloom", "chorus_bloom", "Chorus Thicket", "cherry leaves"),
    ("Lumine Cap", "lumine_cap", "lumine_cap", "Nebula Fields", "shroomlight"),
    ("Glimmer Crystal", "glimmer_crystal", "glimmer_crystal", "Glimmering Crags, Resonant Spires", "amethyst block"),
    ("Leviathan Bone", "leviathan_bone_top", "leviathan_bone_side", "The Whalefall", "bone block"),
    ("Nightstone Bricks", "nightstone_bricks", "nightstone_bricks", "Resonant Spires", "deepslate bricks"),
]


def tex(name):
    return Image.open(T / f"{name}.png").convert("RGBA").crop((0, 0, 16, 16))


def cube(top, side):
    out = Image.new("RGBA", (192, 192), (0, 0, 0, 0))

    def face(img, data, shade):
        f = img.transform((192, 192), Image.AFFINE, data, resample=Image.NEAREST)
        r, g, b, a = f.split()
        rgb = ImageEnhance.Brightness(Image.merge("RGB", (r, g, b))).enhance(shade)
        out.alpha_composite(Image.merge("RGBA", (*rgb.split(), a)))
    face(side, (1 / 6, 0, 0, -1 / 12, 1 / 6, -8), 0.82)      # left, lit
    face(side, (1 / 6, 0, -16, 1 / 12, 1 / 6, -24), 0.62)    # right, shaded
    face(top, (1 / 12, 1 / 6, -8, -1 / 12, 1 / 6, 8), 1.0)
    return out


def tiled(img, n=4, px=192):
    o = Image.new("RGBA", (16 * n, 16 * n))
    for x in range(n):
        for y in range(n):
            o.paste(img, (x * 16, y * 16))
    return o.resize((px, px), Image.NEAREST)


def main():
    def font(size, bold=False):
        try:
            return ImageFont.truetype(f"/usr/share/fonts/truetype/dejavu/DejaVuSans{'-Bold' if bold else ''}.ttf", size)
        except OSError:
            return ImageFont.load_default()
    title, name_font, text = font(24, True), font(19, True), font(15)
    w, h, cols = 700, 240, 2
    rows = (len(BLOCKS) + cols - 1) // cols
    sheet = Image.new("RGB", (w * cols + 20, h * rows + 70), (34, 32, 42))
    d = ImageDraw.Draw(sheet)
    d.text((20, 18), "The Astral End - the commonest blocks", fill=(235, 230, 250), font=title)
    for i, (name, top, side, where, kin) in enumerate(BLOCKS):
        x0, y0 = 20 + (i % cols) * w, 60 + (i // cols) * h
        c = cube(tex(top), tex(side))
        sheet.paste(c, (x0, y0 + 10), c)
        sheet.paste(tiled(tex(side)), (x0 + 205, y0 + 10))
        d.text((x0 + 412, y0 + 22), name, fill=(240, 236, 250), font=name_font)
        lines = ["Found in:"] + [p.strip() for p in where.split(",")] + ["", "Vanilla kin:", kin]
        for k, line in enumerate(lines):
            d.text((x0 + 412, y0 + 56 + k * 20), line, fill=(190, 184, 210), font=text)
    out = ROOT / "docs/images/phase10/common_blocks.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(out)
    print("wrote", out.relative_to(ROOT))


if __name__ == "__main__":
    main()
