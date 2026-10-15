"""Renders a Leviathan (leviathan_shape.py, the mirror of LeviathanShape.java) as an isometric voxel picture with
the block textures, to judge its design without starting the game:

    python3 tools/structures/leviathan_preview.py [seed] [out.png] [block size] [se|nw]
"""
import math
import random
import sys
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageFilter

sys.path.insert(0, str(Path(__file__).resolve().parent))
import leviathan_shape as lev  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
TEX = ROOT / "src/main/resources/assets/astralend/textures/block"
seed = int(sys.argv[1]) if len(sys.argv) > 1 else 7
OUT = Path(sys.argv[2]) if len(sys.argv) > 2 else Path("lev.png")
B = int(sys.argv[3]) if len(sys.argv) > 3 else 9
VIEW = sys.argv[4] if len(sys.argv) > 4 else "se"
W, H = B * 0.866, B * 0.5
T = 16


def hexc(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def ramp(pal, t):
    cols = [hexc(c) for c in pal]
    t = max(0.0, min(1.0, t)) * (len(cols) - 1)
    i = min(int(t), len(cols) - 2)
    f = t - i
    return tuple(round(cols[i][k] + (cols[i + 1][k] - cols[i][k]) * f) for k in range(3))


def noise_tex(pal, seed, jit=0.5, bricks=False):
    r = random.Random(seed)
    img = Image.new("RGBA", (T, T))
    for y in range(T):
        for x in range(T):
            t = 0.5 + (r.random() - 0.5) * jit
            if bricks and (y % 8 == 7 or (x + (8 if (y // 8) % 2 else 0)) % 16 == 15):
                t = 0.08
            img.putpixel((x, y), ramp(pal, t) + (255,))
    return img


def load(name, frame=0):
    im = Image.open(TEX / f"{name}.png").convert("RGBA")
    return im.crop((0, frame * 16, 16, frame * 16 + 16))


PURPUR = ["#6e4a6e", "#8e648e", "#a97ca9", "#c098c0"]
ENDBRICK = ["#9a9a62", "#c4c482", "#dcdc9c", "#ecebb4"]
MAT = {
    "bone": (load("leviathan_bone_top"), load("leviathan_bone_side")),
    "weathered": (load("weathered_leviathan_bone_top"), load("weathered_leviathan_bone_side")),
    "lichened": (load("lichened_leviathan_bone_top"), load("lichened_leviathan_bone_side")),
    "purpur": (noise_tex(PURPUR, 1, 0.5, True), noise_tex(PURPUR, 2, 0.5, True)),
    "purpur_slab": (noise_tex(PURPUR, 3, 0.4), noise_tex(PURPUR, 3, 0.4)),
    "purpur_pillar": (noise_tex(PURPUR, 4, 0.3), noise_tex(["#7a557a", "#9a709a", "#b088b0"], 5, 0.25)),
    "endbrick": (noise_tex(ENDBRICK, 6, 0.4, True), noise_tex(ENDBRICK, 6, 0.4, True)),
    "endbrick_wall": (noise_tex(ENDBRICK, 7, 0.4, True), noise_tex(ENDBRICK, 7, 0.4, True)),
    "glass": (noise_tex(["#4a1e7a", "#6a2ea8", "#8a48c8"], 8, 0.3), noise_tex(["#4a1e7a", "#6a2ea8", "#8a48c8"], 8, 0.3)),
    "obsidian": (noise_tex(["#0a0614", "#160e28", "#2a1c46"], 9, 0.6), noise_tex(["#0a0614", "#160e28", "#2a1c46"], 9, 0.6)),
    "throat": (load("maw_throat"), load("maw_throat")),
    "eye": (load("maw_eye"), load("maw_eye")),
    "flesh": (load("leviathan_flesh"), load("leviathan_flesh")),
    "heart": (load("leviathan_heart", 2), load("leviathan_heart", 2)),
    "breathcomb": (load("breathcomb_3"), load("breathcomb_3")),
    "voidgel": (load("voidgel"), load("voidgel")),
}
EMIT = {"heart": ((200, 60, 255), 2.5, 0.6), "flesh": ((150, 50, 230), 1.0, 0.15), "breathcomb": ((190, 160, 255), 1.6, 0.25),
        "glass": ((170, 90, 255), 1.4, 0.25), "eye": ((160, 80, 255), 3, 0.7), "throat": ((90, 40, 160), 2, 0.3)}
SHADE = {"top": 1.0, "left": 0.78, "right": 0.6}


def face_image(tex, kind, shade):
    tex = Image.eval(tex.convert("RGB"), lambda v: int(v * shade)).convert("RGBA")
    if kind == "top":
        du, dv, size, o = (W, H), (-W, H), (2 * W, 2 * H), (W, 0)
    elif kind == "left":
        du, dv, size, o = (W, H), (0, B), (W, H + B), (0, 0)
    else:
        du, dv, size, o = (W, -H), (0, B), (W, H + B), (0, H)
    det = du[0] * dv[1] - dv[0] * du[1]
    inv = ((dv[1] / det, -dv[0] / det), (-du[1] / det, du[0] / det))
    a, b = T * inv[0][0], T * inv[0][1]
    d, e = T * inv[1][0], T * inv[1][1]
    c = -(a * o[0] + b * o[1])
    f = -(d * o[0] + e * o[1])
    return tex.transform((math.ceil(size[0]) + 1, math.ceil(size[1]) + 1), Image.AFFINE, (a, b, c, d, e, f),
                         resample=Image.NEAREST, fillcolor=(0, 0, 0, 0))


FACES = {n: {"top": face_image(t, "top", SHADE["top"]), "left": face_image(s, "left", SHADE["left"]),
             "right": face_image(s, "right", SHADE["right"])} for n, (t, s) in MAT.items()}

L = lev.Leviathan(seed)
vox = {}
for w in range(-122, 84):
    for u in range(-30, 31):
        for v in range(0, 112):
            m = L.block(u, v, w)
            if m and m != "air":
                vox[(w, v, u)] = m
for (u, v, w) in L.eyes():
    vox[(w, v, u)] = "eye"
counts = {}
for m in vox.values():
    counts[m] = counts.get(m, 0) + 1
print(counts)

# The view: x = w (head to the right), z = u. "se" looks from the +u side; "nw" from the -u side.
if VIEW == "nw":
    vox = {(-x, y, -z): m for (x, y, z), m in vox.items()}
xs = [k[0] for k in vox]
zs = [k[2] for k in vox]
ys = [k[1] for k in vox]
pad = 120
width = int((max(xs) - min(xs) + max(zs) - min(zs) + 3) * W) + 2 * pad
height = int((max(xs) - min(xs) + max(zs) - min(zs) + 3) * H + (max(ys) - min(ys) + 3) * B) + 2 * pad
OX = (max(zs) - min(xs) + 1.5) * W + pad
OY = (max(ys) + 2) * B - (min(xs) + min(zs)) * H + pad


def screen(x, y, z):
    return (OX + (x - z) * W, OY + (x + z) * H - y * B)


img = Image.new("RGBA", (width, height))
d = ImageDraw.Draw(img)
for y in range(height):
    d.line([(0, y), (width, y)], fill=ramp(["#06050c", "#0e0a1a", "#1a1028"], y / height) + (255,))
rnd = random.Random(3)
for _ in range(1500):
    x, y = rnd.randrange(width), rnd.randrange(height)
    c = rnd.randint(110, 230)
    img.putpixel((x, y), (c, c, min(255, c + 15), 255))
emit = []
for (x, y, z) in sorted(vox, key=lambda k: (k[0] + k[2], k[1])):
    m = vox[(x, y, z)]
    f = FACES[m]
    drew = False
    if (x, y + 1, z) not in vox:
        p = screen(x, y + 1, z)
        img.alpha_composite(f["top"], (int(p[0] - W), int(p[1])))
        drew = True
    if (x, y, z + 1) not in vox:
        p = screen(x, y + 1, z + 1)
        img.alpha_composite(f["left"], (int(p[0]), int(p[1])))
        drew = True
    if (x + 1, y, z) not in vox:
        p = screen(x + 1, y + 1, z + 1)
        img.alpha_composite(f["right"], (int(p[0]), int(p[1] - H)))
        drew = True
    if drew and m in EMIT:
        emit.append((screen(x + 0.5, y + 0.5, z + 0.5), EMIT[m]))
glow = Image.new("RGB", img.size, (0, 0, 0))
gd = ImageDraw.Draw(glow)
for (p, (col, r, s)) in emit:
    rr = r * B
    gd.ellipse((p[0] - rr, p[1] - rr, p[0] + rr, p[1] + rr), fill=tuple(int(c * s) for c in col))
glow = glow.filter(ImageFilter.GaussianBlur(B * 1.2))
img = ImageChops.add(img.convert("RGB"), glow)
img = img.crop(img.getbbox())
img.save(OUT)
print(OUT, img.size)
