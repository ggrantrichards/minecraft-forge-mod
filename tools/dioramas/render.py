"""Draws the survey's cut-outs (dioramas/<name>.txt, written by dev/Dioramas.java) in isometric, with the
game's own block textures, for the website:

    python3 tools/dioramas/render.py <dump dir> <out dir> <astralend block textures> [vanilla block textures]

In CI the vanilla textures come out of the game's jar and stay there: only the finished pictures are
published (astralend-diorama_<name>.png). Without them, vanilla blocks are drawn in flat colours close to
their own. The background is the website's void colour, so the pictures sit on the page seamlessly.
"""
import math
import random
import sys
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageFilter

VOID = (12, 10, 20)
T = 16

# Vanilla textures that are grey in the jar and tinted by the game.
TINTED = {"short_grass", "tall_grass_top", "tall_grass_bottom", "fern", "large_fern_top", "large_fern_bottom",
          "grass_block_top", "vine", "oak_leaves", "water_still", "water_flow"}
GRASS = {"biome_gilded_reach": "d2b04c"}
WATER = {"biome_gilded_reach": "2fbfb4"}
# Flat colours for vanilla blocks when their textures aren't at hand (local previews).
FALLBACK = {"end_stone": "dbdea0", "end_stone_bricks": "e2e5a8", "purpur_block": "a77aa7", "purpur_pillar": "ab82ab",
            "obsidian": "16101f", "crying_obsidian": "281040", "water": "2a6fd0", "end_rod": "f2eee6", "bone_block": "e3ddc6",
            "chorus_plant": "5d3a5d", "chorus_flower": "9c7a9c", "moss_block": "5a7a2c", "short_grass": "7a9a3a",
            "tall_grass": "7a9a3a", "lava": "e0701a", "magma_block": "8a3418", "blue_ice": "7aa8f2", "chest": "9a6a2a",
            "barrel": "8a6234", "glass": "c8e0e8", "purple_stained_glass": "7a3ab0", "end_portal_frame": "3a6a5a",
            "shulker_box": "8a5a8a", "purple_shulker_box": "6a3a8a", "lantern": "e8b048", "chain": "4a4a52", "vault": "3a3f48",
            "dragon_egg": "1a0c22", "sculk": "0a2a30", "blackstone": "2a2430", "basalt": "4a4850", "iron_bars": "6a6a6a",
            "ladder": "8a6a3a", "bookshelf": "7a5a3a", "end_gateway": "0a0814", "amethyst_block": "8a5ac0"}

STRIP = ("_stairs", "_slab", "_wall", "_fence_gate", "_fence", "_pressure_plate", "_button", "_trapdoor", "_door", "_pane")


def hexc(h):
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def flat_tex(colour, seed):
    r = random.Random(seed)
    base = hexc(colour)
    img = Image.new("RGBA", (T, T))
    for y in range(T):
        for x in range(T):
            k = 0.88 + 0.24 * r.random()
            img.putpixel((x, y), tuple(min(255, int(c * k)) for c in base) + (255,))
    return img


class Textures:
    def __init__(self, mod_dir, vanilla_dir, name):
        self.dirs = {"astralend": Path(mod_dir), "minecraft": Path(vanilla_dir) if vanilla_dir else None}
        self.name = name
        self.cache = {}
        self.missing = set()

    def _load(self, ns, stem):
        d = self.dirs.get(ns)
        if d is None:
            return None
        f = d / f"{stem}.png"
        if not f.exists():
            return None
        img = Image.open(f).convert("RGBA")
        img = img.crop((0, 0, img.width, img.width))          # an animation's first frame
        if img.width != T:
            img = img.resize((T, T), Image.NEAREST)
        if ns == "minecraft" and stem in TINTED:
            tint = hexc(WATER.get(self.name, "3f76e4") if stem.startswith("water") else GRASS.get(self.name, "7a9a3a"))
            r, g, b, a = img.split()
            grey = Image.merge("RGB", (r, g, b)).convert("L")
            coloured = Image.merge("RGB", [grey.point(lambda v, c=c: v * c // 255) for c in tint])
            img = Image.merge("RGBA", (*coloured.split(), a))
        return img

    def get(self, block, face):
        """A block's texture for 'top' or 'side' (16x16 RGBA)."""
        key = (block, face)
        if key in self.cache:
            return self.cache[key]
        ns, path = block.split(":", 1)
        bases = [path]
        for s in STRIP:
            if path.endswith(s):
                b = path[: -len(s)]
                bases += [b, b + "s", b + "_block", b + "_planks"]
                if b.endswith("_brick"):
                    bases.insert(1, b + "s")
        if path.endswith("_block"):
            bases.append(path[:-6])
        if path.startswith("purpur"):
            bases.append("purpur_block")
        if path in ("water", "lava"):
            bases = [path + "_still", path]
        if path == "tall_grass":
            bases = ["tall_grass_top"]
        order = ([f"{b}_top" for b in bases] + [f"{b}_end" for b in bases]) if face == "top" else ([f"{b}_side" for b in bases] + [f"{b}_front" for b in bases])
        order += bases + [f"{b}_0" for b in bases] + [f"{b}_stage3" for b in bases]
        img = None
        for stem in order:
            img = self._load(ns, stem)
            if img is not None:
                break
        if img is None:
            colour = None
            for b in bases:
                colour = FALLBACK.get(b) or colour
            if colour is None:
                self.missing.add(block)
                h = abs(hash(block)) % 0xFFFFFF
                colour = f"{(h >> 16) & 255 | 64:02x}{(h >> 8) & 255 | 64:02x}{h & 255 | 64:02x}"
            img = flat_tex(colour, abs(hash(block)) % 997)
        self.cache[key] = img
        return img


def read(path):
    lines = Path(path).read_text().splitlines()
    size = tuple(int(v) for v in lines[0].split()[1:4])
    n = int(lines[1].split()[1])
    palette = []
    for line in lines[2:2 + n]:
        block, kind, light, height = line.rsplit(" ", 3)
        axis = None
        if "[axis=" in block:
            block, axis = block[:-1].split("[axis=")
        palette.append((block, axis, kind, int(light), float(height)))
    vox = {}
    for line in lines[2 + n:]:
        x, y, z, p, faces = (int(v) for v in line.split())
        vox[(x, y, z)] = (p, faces)
    return size, palette, vox


def render(dump, out, textures_mod, textures_vanilla=None, width=1800):
    name = Path(dump).stem
    size, palette, vox = read(dump)
    tex = Textures(textures_mod, textures_vanilla, name)
    dx, dy, dz = size
    # Whole pixels per block (2:1 dimetric, like pixel art), so neighbouring faces meet without seams.
    H = max(2, min(8, round(width / ((dx + dz) * 2))))
    W, B = 2 * H, round(2.4 * H)
    shade = {"top": 1.0, "left": 0.80, "right": 0.62}

    def face_image(img, kind, s, height=1.0):
        if s < 1.0:
            r, g, b, a = img.split()
            img = Image.merge("RGBA", [c.point(lambda v: int(v * s)) for c in (r, g, b)] + [a])
        if kind == "top":
            du, dv, sz, o = (W, H), (-W, H), (2 * W, 2 * H), (W, 0)
        elif kind == "left":
            du, dv, sz, o = (W, H), (0, B * height), (W, H + B * height), (0, 0)
        else:
            du, dv, sz, o = (W, -H), (0, B * height), (W, H + B * height), (0, H)
        det = du[0] * dv[1] - dv[0] * du[1]
        inv = ((dv[1] / det, -dv[0] / det), (-du[1] / det, du[0] / det))
        a, b = T * inv[0][0], T * inv[0][1]
        d, e = T * inv[1][0], T * inv[1][1]
        c = -(a * o[0] + b * o[1])
        f = -(d * o[0] + e * o[1])
        return img.transform((math.ceil(sz[0]) + 1, math.ceil(sz[1]) + 1), Image.AFFINE, (a, b, c, d, e, f),
                             resample=Image.NEAREST, fillcolor=(0, 0, 0, 0))

    cache = {}

    def faces_for(i):
        if i in cache:
            return cache[i]
        block, axis, kind, light, height = palette[i]
        top, side = tex.get(block, "top"), tex.get(block, "side")
        h = height if kind == "p" else 1.0
        if kind == "w":
            h = 0.9
        if axis == "x":
            imgs = {"top": side.rotate(90), "left": side, "right": top}
        elif axis == "z":
            imgs = {"top": side, "left": top, "right": side.rotate(90)}
        else:
            imgs = {"top": top, "left": side, "right": side}
        made = {k: face_image(v, k, shade[k], h if k != "top" else 1.0) for k, v in imgs.items()}
        if kind in ("w", "l"):
            for k in made:
                r, g, b_, a = made[k].split()
                made[k] = Image.merge("RGBA", (r, g, b_, a.point(lambda v: int(v * (0.72 if kind == "w" else 1.0)))))
        sprite = side.resize((int(B * 1.0) + 1, int(B * 1.0) + 1), Image.NEAREST) if kind == "c" else None
        cache[i] = (made, sprite, h)
        return cache[i]

    xs = [k[0] for k in vox] or [0]
    zs = [k[2] for k in vox] or [0]
    ys = [k[1] for k in vox] or [0]
    pad = int(B * 6)
    w = int((max(xs) - min(xs) + max(zs) - min(zs) + 3) * W) + 2 * pad
    hgt = int((max(xs) - min(xs) + max(zs) - min(zs) + 3) * H + (max(ys) - min(ys) + 3) * B) + 2 * pad
    OX = (max(zs) - min(xs) + 1.5) * W + pad
    OY = (max(ys) + 2) * B - (min(xs) + min(zs)) * H + pad

    def screen(x, y, z):
        return (OX + (x - z) * W, OY + (x + z) * H - y * B)

    img = Image.new("RGBA", (w, hgt), VOID + (255,))
    glows = []
    for (x, y, z) in sorted(vox, key=lambda k: (k[0] + k[2], k[1])):
        i, faces = vox[(x, y, z)]
        block, axis, kind, light, height = palette[i]
        made, sprite, h = faces_for(i)
        if kind == "c":
            p = screen(x + 0.5, y + 1, z + 0.5)
            img.alpha_composite(sprite, (int(p[0] - sprite.width / 2), int(p[1] + H * 0.5)))
        elif kind == "f":
            p = screen(x, y + 0.08, z)
            img.alpha_composite(made["top"], (int(p[0] - W), int(p[1])))
        else:
            if faces & 2:
                p = screen(x, y + h, z)
                img.alpha_composite(made["top"], (int(p[0] - W), int(p[1])))
            if faces & 8:
                p = screen(x, y + h, z + 1)
                img.alpha_composite(made["left"], (int(p[0]), int(p[1])))
            if faces & 32:
                p = screen(x + 1, y + h, z + 1)
                img.alpha_composite(made["right"], (int(p[0]), int(p[1] - H)))
        if light > 0:
            col = tex.get(block, "top").convert("RGB").resize((1, 1), Image.BOX).getpixel((0, 0))
            glows.append((screen(x + 0.5, y + 0.5, z + 0.5), col, light))
    glow = Image.new("RGB", img.size, (0, 0, 0))
    gd = ImageDraw.Draw(glow)
    for (p, col, light) in glows:
        r = B * (0.6 + light / 15.0 * 1.8)
        s = 0.25 + 0.35 * light / 15.0
        gd.ellipse((p[0] - r, p[1] - r, p[0] + r, p[1] + r), fill=tuple(int(c * s) for c in col))
    glow = glow.filter(ImageFilter.GaussianBlur(B * 1.4))
    out_img = ImageChops.add(img.convert("RGB"), glow)
    bbox = ImageChops.difference(out_img, Image.new("RGB", out_img.size, VOID)).getbbox()
    if bbox:
        m = int(B * 3)
        out_img = out_img.crop((max(0, bbox[0] - m), max(0, bbox[1] - m), min(out_img.width, bbox[2] + m), min(out_img.height, bbox[3] + m)))
    out_img.save(out)
    return out_img.size, sorted(tex.missing)


if __name__ == "__main__":
    dump_dir, out_dir, mod_dir = Path(sys.argv[1]), Path(sys.argv[2]), sys.argv[3]
    vanilla = sys.argv[4] if len(sys.argv) > 4 else None
    out_dir.mkdir(parents=True, exist_ok=True)
    for dump in sorted(dump_dir.glob("*.txt")):
        size, missing = render(dump, out_dir / f"astralend-diorama_{dump.stem}.png", mod_dir, vanilla)
        print(f"diorama {dump.stem}: {size[0]}x{size[1]}" + (f"; no texture for {', '.join(missing)}" if missing else ""))
