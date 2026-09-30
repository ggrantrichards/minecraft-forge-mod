#!/usr/bin/env python3
"""Draws little block scenes with the mod's textures, to judge textures the way players see them:
on many blocks at once, lit like the game (top faces brightest), up close and far away.

    python3 tools/textures/scene.py            -> docs/images/texture_scenes.png

A texture can look fine as a single 16x16 square and still turn into noise or a grid when it
covers a hillside, or clash with the blocks next to it. Each scene is drawn twice: close
(each block 32 pixels wide) and far (each block 8 pixels, averaged down like the game's mipmaps).
"""
import math
import sys
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
TEX = ROOT / "src/main/resources/assets/astralend/textures/block"
OUT = ROOT / "docs/images/texture_scenes.png"
BG = (20, 16, 30, 255)            # the End's sky
SHADE = {"top": 1.0, "left": 0.78, "right": 0.62}   # like Minecraft's directional face shading


def texture(name):
    img = Image.open(TEX / f"{name}.png").convert("RGBA")
    return img.crop((0, 0, 16, 16))


def render(voxels, block=32, flats=None):
    """voxels: {(x, y, z): texture image or (top, side) pair}. Isometric, viewed from the south-east.
    flats: {(x, y, z): texture} drawn as flat overlays on top of the block below (carpets)."""
    flats = flats or {}
    xs = [v[0] for v in voxels] + [0]
    zs = [v[2] for v in voxels] + [0]
    ys = [v[1] for v in voxels] + [0]
    w = block * 0.866
    h = block * 0.5
    width = int((max(xs) - min(xs) + max(zs) - min(zs) + 3) * w) + block
    height = int((max(xs) - min(xs) + max(zs) - min(zs) + 3) * h + (max(ys) - min(ys) + 3) * block)
    img = Image.new("RGBA", (width, height), BG)
    draw = ImageDraw.Draw(img)
    ox = (max(zs) - min(xs) + 1.5) * w
    oy = (max(ys) + 2) * block - (min(xs) + min(zs)) * h

    def screen(x, y, z):
        return (ox + (x - z) * w, oy + (x + z) * h - y * block)

    def face(corner, du, dv, tex, shade):
        """Fills a parallelogram corner + a*du + b*dv (a, b in 0..1) with the texture."""
        px = tex.load()
        for ty in range(16):
            for tx in range(16):
                c = px[tx, ty]
                if c[3] < 128:
                    continue
                c = tuple(int(c[i] * shade) for i in range(3)) + (255,)
                a0, a1, b0, b1 = tx / 16, (tx + 1) / 16, ty / 16, (ty + 1) / 16
                pts = [(corner[0] + du[0] * a + dv[0] * b, corner[1] + du[1] * a + dv[1] * b)
                       for (a, b) in ((a0, b0), (a1, b0), (a1, b1), (a0, b1))]
                draw.polygon(pts, fill=c)

    def cube(x, y, z, top, side):
        # visible faces from this viewpoint: top (y+1), south (z+1) on the left, east (x+1) on the right
        if (x, y + 1, z) not in voxels:
            p = screen(x, y + 1, z)
            face(p, (w, h), (-w, h), top, SHADE["top"])
        if (x, y, z + 1) not in voxels:
            p = screen(x, y + 1, z + 1)
            face(p, (w, h), (0, block), side, SHADE["left"])
        if (x + 1, y, z) not in voxels:
            p = screen(x + 1, y + 1, z + 1)
            face(p, (w, -h), (0, block), side, SHADE["right"])

    for (x, y, z) in sorted(voxels, key=lambda v: (v[0] + v[2], v[1])):
        t = voxels[(x, y, z)]
        top, side = t if isinstance(t, tuple) else (t, t)
        cube(x, y, z, top, side)
        if (x, y + 1, z) in flats:
            face(screen(x, y + 1, z), (w, h), (-w, h), flats[(x, y + 1, z)], SHADE["top"])
    return img


def far(img, factor=4):
    """How the scene reads from a distance: averaged down, then scaled back up to view."""
    small = img.resize((img.width // factor, img.height // factor), Image.BOX)
    return small.resize((small.width * 2, small.height * 2), Image.NEAREST)


# ------------------------------------------------------------------ scenes

def ground(size, tex, y=0):
    return {(x, y, z): tex for x in range(size) for z in range(size)}


def mushroom_scene():
    moss, stem, cap = texture("nebula_moss"), texture("lumine_stem"), texture("lumine_cap")
    stem_top = texture("lumine_stem_top")
    v = ground(14, moss)
    for (cx, cz, r, hgt) in ((6, 6, 4, 7), (11, 2, 2, 4)):
        for y in range(1, hgt + 1):
            v[(cx, y, cz)] = (stem_top, stem)
        for dx in range(-r, r + 1):
            for dz in range(-r, r + 1):
                d = math.hypot(dx, dz)
                if d <= r + 0.4:
                    top = hgt + 1 + (1 if d < r * 0.55 else 0)
                    v[(cx + dx, top, cz + dz)] = cap
                    if d > r - 0.6:
                        v[(cx + dx, top - 1, cz + dz)] = cap   # the drooping rim
    return v, {}


def corona_scene():
    crust, lichen = texture("sunscorched_stone"), texture("sunscale_lichen")
    v = ground(14, crust)
    for x in range(8, 14):
        for z in range(0, 5):
            v[(x, 1, z)] = crust        # a step up
    flats = {}
    for (x, z) in ((2, 3), (3, 3), (5, 8), (9, 10), (10, 10), (1, 11), (12, 6)):
        flats[(x, 1, z)] = lichen
    return v, flats


SCENES = {"mushroom": mushroom_scene, "corona": corona_scene}


def main():
    wanted = sys.argv[1:] or list(SCENES)
    rows = []
    for name in wanted:
        voxels, flats = SCENES[name]()
        close = render(voxels, 32, flats)
        rows.append((close, far(close)))
    width = max(c.width + f.width + 30 for c, f in rows)
    height = sum(max(c.height, f.height) + 10 for c, f in rows)
    sheet = Image.new("RGBA", (width, height), (40, 40, 48, 255))
    y = 0
    for close, distant in rows:
        sheet.paste(close, (0, y))
        sheet.paste(distant, (close.width + 30, y))
        y += max(close.height, distant.height) + 10
    out = OUT if not sys.argv[1:] else OUT.with_name("texture_scenes_" + "_".join(wanted) + ".png")
    sheet.save(out)
    print("Wrote", out)


if __name__ == "__main__":
    main()
