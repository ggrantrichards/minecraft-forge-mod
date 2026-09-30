#!/usr/bin/env python3
"""Renders each mob's model with its texture, from a few angles, without starting the game.

    python3 tools/mobs/preview_mobs.py [mob ...]      -> docs/images/mob_models.png

It reads the same model description as make_mobs.py and places parts the way Minecraft does
(ModelPart.translateAndRotate: move to the pivot, then rotate Z*Y*X), so a pose that looks wrong
here looks wrong in the game. The rest pose only - animations aren't simulated. Good for quick
checks while designing; the in-game screenshots in CI are the final word.
"""
import math
import sys
from pathlib import Path

from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).resolve().parent))
import make_mobs  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "docs/images/mob_models.png"


def rot_zyx(v, xr, yr, zr):
    x, y, z = v
    # x rotation
    y, z = y * math.cos(xr) - z * math.sin(xr), y * math.sin(xr) + z * math.cos(xr)
    # y rotation
    x, z = x * math.cos(yr) + z * math.sin(yr), -x * math.sin(yr) + z * math.cos(yr)
    # z rotation
    x, y = x * math.cos(zr) - y * math.sin(zr), x * math.sin(zr) + y * math.cos(zr)
    return (x, y, z)


def faces_of(part, transform, out):
    p = part.pose
    px, py, pz = p[0], p[1], p[2]
    xr, yr, zr = (p[3], p[4], p[5]) if len(p) == 6 else (0.0, 0.0, 0.0)

    def local(v):
        return transform(tuple(a + b for a, b in zip(rot_zyx(v, xr, yr, zr), (px, py, pz))))

    for b in part.boxes:
        g = getattr(b, "inflate", 0.0)
        x0, y0, z0 = b.x - g, b.y - g, b.z - g
        x1, y1, z1 = b.x + b.w + g, b.y + b.h + g, b.z + b.d + g
        rects = b.faces()
        # (corners in texture order: top-left, top-right, bottom-right, bottom-left), texture rect
        quads = {
            "north": ([(x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0)], rects["north"]),
            "south": ([(x1, y0, z1), (x0, y0, z1), (x0, y1, z1), (x1, y1, z1)], rects["south"]),
            "east": ([(x0, y0, z1), (x0, y0, z0), (x0, y1, z0), (x0, y1, z1)], rects["east"]),
            "west": ([(x1, y0, z0), (x1, y0, z1), (x1, y1, z1), (x1, y1, z0)], rects["west"]),
            "up": ([(x0, y0, z1), (x1, y0, z1), (x1, y0, z0), (x0, y0, z0)], rects["up"]),
            "down": ([(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)], rects["down"]),
        }
        for name, (corners, rect) in quads.items():
            if rect[2] <= 0 or rect[3] <= 0:
                continue
            if b.mirror:
                rect = (rect[0] + rect[2], rect[1], -rect[2], rect[3])
            out.append(([local(c) for c in corners], rect))
    for c in part.children:
        faces_of(c, local, out)


def render(mob, texture, yaw, pitch, size=260, glow=None):
    faces = []
    for part in mob.parts:
        faces_of(part, lambda v: v, faces)

    def view(v):
        x, y, z = v
        x, z = x * math.cos(yaw) - z * math.sin(yaw), x * math.sin(yaw) + z * math.cos(yaw)
        y, z = y * math.cos(pitch) - z * math.sin(pitch), y * math.sin(pitch) + z * math.cos(pitch)
        return (x, y, z)

    faces = [([view(c) for c in corners], rect) for corners, rect in faces]
    xs = [c[0] for f, _ in faces for c in f]
    ys = [c[1] for f, _ in faces for c in f]
    span = max(max(xs) - min(xs), max(ys) - min(ys)) or 1
    scale = (size - 20) / span
    cx, cy = (max(xs) + min(xs)) / 2, (max(ys) + min(ys)) / 2
    img = Image.new("RGBA", (size, size), (46, 44, 58, 255))
    px = img.load()
    depth = [[1e9] * size for _ in range(size)]
    tex = texture.load()
    gl = glow.load() if glow else None
    tw, th = texture.size
    for corners, (u, v, w, h) in faces:
        pts = [((c[0] - cx) * scale + size / 2, (c[1] - cy) * scale + size / 2, c[2]) for c in corners]
        uvs = [(u, v), (u + w, v), (u + w, v + h), (u, v + h)]
        for tri in ((0, 1, 2), (0, 2, 3)):
            (ax, ay, az), (bx, by, bz), (qx, qy, qz) = (pts[i] for i in tri)
            (au, av), (bu, bv), (qu, qv) = (uvs[i] for i in tri)
            den = (by - qy) * (ax - qx) + (qx - bx) * (ay - qy)
            if abs(den) < 1e-9:
                continue
            for yy in range(max(0, int(min(ay, by, qy))), min(size, int(max(ay, by, qy)) + 1)):
                for xx in range(max(0, int(min(ax, bx, qx))), min(size, int(max(ax, bx, qx)) + 1)):
                    sx, sy = xx + 0.5, yy + 0.5
                    l1 = ((by - qy) * (sx - qx) + (qx - bx) * (sy - qy)) / den
                    l2 = ((qy - ay) * (sx - qx) + (ax - qx) * (sy - qy)) / den
                    l3 = 1 - l1 - l2
                    if min(l1, l2, l3) < -1e-6:
                        continue
                    z = l1 * az + l2 * bz + l3 * qz
                    if z >= depth[yy][xx]:
                        continue
                    tu = l1 * au + l2 * bu + l3 * qu
                    tv = l1 * av + l2 * bv + l3 * qv
                    ix = min(tw - 1, max(0, int(math.floor(tu - 1e-6 if tu > u + abs(w) - 1e-6 else tu))))
                    iy = min(th - 1, max(0, int(math.floor(tv - 1e-6 if tv > v + h - 1e-6 else tv))))
                    c = tex[ix, iy]
                    if c[3] < 20:
                        continue
                    if gl and gl[ix, iy][3] > 20:
                        c = gl[ix, iy][:3] + (255,)       # glowing parts, at full brightness
                    depth[yy][xx] = z
                    base = px[xx, yy]
                    a = c[3] / 255
                    px[xx, yy] = tuple(int(c[i] * a + base[i] * (1 - a)) for i in range(3)) + (255,)
    return img


def main():
    wanted = sys.argv[1:]
    mobs = [m for m in make_mobs.MOBS if not wanted or m.name in wanted]
    views = [(-0.7, 0.35), (math.pi / 2, 0.15), (2.4, 0.4)]
    size = 260
    sheet = Image.new("RGBA", (size * len(views), (size + 18) * len(mobs)), (30, 30, 38, 255))
    draw = ImageDraw.Draw(sheet)
    for row, mob in enumerate(mobs):
        folder = make_mobs.TEX / "entity" / mob.name
        texture = Image.open(folder / f"{mob.name}.png").convert("RGBA")
        glow_file = folder / f"{mob.name}_glow.png"
        glow = Image.open(glow_file).convert("RGBA") if glow_file.exists() else None
        for col, (yaw, pitch) in enumerate(views):
            sheet.paste(render(mob, texture, yaw, pitch, size, glow), (col * size, row * (size + 18)))
        draw.text((6, row * (size + 18) + size + 2), mob.name, fill=(230, 230, 236))
    OUT.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(OUT if not wanted else OUT.with_name("mob_models_" + "_".join(wanted) + ".png"))
    print("Wrote", OUT if not wanted else OUT.with_name("mob_models_" + "_".join(wanted) + ".png"))


if __name__ == "__main__":
    main()
