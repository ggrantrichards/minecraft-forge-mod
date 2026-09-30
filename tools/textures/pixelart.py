"""Pixel-art building blocks for The Astral End's textures, in the style of vanilla Minecraft.

Random pixels read as noise, not as a material. Vanilla textures get their look from a few
habits, and these helpers are those habits:

  - smooth, tileable noise quantized to a short colour ramp (4-6 tones), so tones clump into
    patches instead of scattering pixel by pixel;
  - colour ramps that shift hue: shadows go cooler and more saturated, highlights warmer;
  - "chunks" (stones, lumps, plates, leaves) lit from the top-left: a lighter top/left edge and a
    darker bottom/right edge give every shape volume;
  - small, deliberate details (ore nuggets, pits, cracks, glints) placed by hand-picked layouts
    rather than scattered at random;
  - everything wraps around, so a block tiles without seams.

All generators take a seed and are deterministic.
"""
import colorsys
import math
import random

from PIL import Image

SIZE = 16
CLEAR = (0, 0, 0, 0)


# ------------------------------------------------------------------ colour

def hexc(h, a=255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(4))


def ramp(dark, light, n, hue_shift=0.04, sat_curve=0.12):
    """n colours from `dark` to `light` (hex strings or tuples), interpolated in HSV. The middle
    tones get a little extra saturation and the hue bends by `hue_shift` across the ramp, the way
    pixel artists shade (cool shadows, warm lights)."""
    d = dark if isinstance(dark, tuple) else hexc(dark)
    l = light if isinstance(light, tuple) else hexc(light)
    hd, sd, vd = colorsys.rgb_to_hsv(*(c / 255 for c in d[:3]))
    hl, sl, vl = colorsys.rgb_to_hsv(*(c / 255 for c in l[:3]))
    if abs(hl - hd) > 0.5:  # interpolate hue the short way round
        hl += 1.0 if hd > hl else -1.0
    out = []
    for i in range(n):
        t = i / (n - 1) if n > 1 else 0.0
        h = (hd + (hl - hd) * t + hue_shift * (t - 0.5)) % 1.0
        s = min(1.0, sd + (sl - sd) * t + sat_curve * math.sin(math.pi * t))
        v = vd + (vl - vd) * t
        r, g, b = colorsys.hsv_to_rgb(h, s, v)
        out.append((int(round(r * 255)), int(round(g * 255)), int(round(b * 255)), 255))
    return out


def shade(c, amount):
    """Lighter (amount > 0, toward warm) or darker (amount < 0, toward cool) by `amount` 0..1."""
    h, s, v = colorsys.rgb_to_hsv(*(x / 255 for x in c[:3]))
    if amount >= 0:
        h = h + (0.12 - h) * 0.08 * amount if h < 0.62 else h - 0.03 * amount
        v = v + (1 - v) * amount
        s = s * (1 - 0.35 * amount)
    else:
        a = -amount
        h = h + (0.68 - h) * 0.10 * a
        v = v * (1 - a)
        s = min(1.0, s + (1 - s) * 0.25 * a)
    r, g, b = colorsys.hsv_to_rgb(h % 1.0, s, v)
    return (int(round(r * 255)), int(round(g * 255)), int(round(b * 255)), c[3])


# ------------------------------------------------------------------ fields (16x16 grids of floats)

def field(fill=0.0):
    return [[fill] * SIZE for _ in range(SIZE)]


def value_noise(seed, period):
    """Smooth noise that tiles: a period x period lattice of random values, smoothly interpolated."""
    rnd = random.Random(seed)
    lattice = [[rnd.random() for _ in range(period)] for _ in range(period)]
    f = field()
    for y in range(SIZE):
        for x in range(SIZE):
            gx, gy = x * period / SIZE, y * period / SIZE
            x0, y0 = int(gx), int(gy)
            tx, ty = gx - x0, gy - y0
            tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
            a = lattice[y0 % period][x0 % period]
            b = lattice[y0 % period][(x0 + 1) % period]
            c = lattice[(y0 + 1) % period][x0 % period]
            d = lattice[(y0 + 1) % period][(x0 + 1) % period]
            f[y][x] = (a + (b - a) * tx) * (1 - ty) + (c + (d - c) * tx) * ty
    return f


def fbm(seed, octaves=((2, 0.5), (4, 0.3), (8, 0.2)), stretch_x=1.0):
    """Sum of noise octaves (period, weight). stretch_x > 1 smears the pattern sideways (strata)."""
    f = field()
    for i, (period, weight) in enumerate(octaves):
        n = value_noise(seed * 31 + i * 7, period)
        for y in range(SIZE):
            for x in range(SIZE):
                sx = int(x / stretch_x) % SIZE if stretch_x != 1.0 else x
                f[y][x] += n[y][sx] * weight
    return normalize(f)


def normalize(f):
    lo = min(min(r) for r in f)
    hi = max(max(r) for r in f)
    span = (hi - lo) or 1.0
    return [[(v - lo) / span for v in r] for r in f]


def add(a, b, wb=1.0):
    return [[a[y][x] + b[y][x] * wb for x in range(SIZE)] for y in range(SIZE)]


def quantize(f, palette, shares=None):
    """Map a field to palette colours by rank, so each tone covers a chosen share of the texture
    (default: equal shares). Lowest values get palette[0]."""
    n = len(palette)
    shares = shares or [1] * n
    total = sum(shares)
    cuts, acc = [], 0.0
    for s in shares[:-1]:
        acc += s / total
        cuts.append(acc)
    flat = sorted(v for r in f for v in r)
    limits = [flat[min(len(flat) - 1, int(c * len(flat)))] for c in cuts]
    img = Image.new("RGBA", (SIZE, SIZE), CLEAR)
    px = img.load()
    for y in range(SIZE):
        for x in range(SIZE):
            v = f[y][x]
            i = sum(1 for l in limits if v > l)
            px[x, y] = palette[i]
    return img


# ------------------------------------------------------------------ cells (Voronoi on a torus)

def voronoi(seed, n, points=None):
    """Splits the tile into n cells around random (or given) points, wrapping at the edges.
    Returns (ids, border) where border[y][x] is how close the pixel is to another cell (0 = on
    the border)."""
    rnd = random.Random(seed)
    pts = points or [(rnd.random() * SIZE, rnd.random() * SIZE) for _ in range(n)]
    ids = [[0] * SIZE for _ in range(SIZE)]
    border = field()
    for y in range(SIZE):
        for x in range(SIZE):
            ds = []
            for i, (px, py) in enumerate(pts):
                dx = min(abs(x + 0.5 - px), SIZE - abs(x + 0.5 - px))
                dy = min(abs(y + 0.5 - py), SIZE - abs(y + 0.5 - py))
                ds.append((dx * dx + dy * dy, i))
            ds.sort()
            ids[y][x] = ds[0][1]
            border[y][x] = math.sqrt(ds[1][0]) - math.sqrt(ds[0][0])
    return ids, border


def cell_light(ids):
    """+1 on a cell's top/left edge (lit), -1 on its bottom/right edge (in shadow), else 0."""
    out = [[0] * SIZE for _ in range(SIZE)]
    for y in range(SIZE):
        for x in range(SIZE):
            me = ids[y][x]
            if ids[(y + 1) % SIZE][x] != me or ids[y][(x + 1) % SIZE] != me:
                out[y][x] = -1
            elif ids[(y - 1) % SIZE][x] != me or ids[y][(x - 1) % SIZE] != me:
                out[y][x] = 1
    return out


# ------------------------------------------------------------------ images

def new(fill=CLEAR):
    return Image.new("RGBA", (SIZE, SIZE), fill)


def from_map(rows, pal):
    """Pixel map: 16 strings of 16 characters; '.' is transparent; other characters index `pal`."""
    assert len(rows) == SIZE, len(rows)
    img = new()
    px = img.load()
    for y, row in enumerate(rows):
        assert len(row) == SIZE, (y, row)
        for x, ch in enumerate(row):
            if ch != ".":
                px[x, y] = pal[ch]
    return img


def put(img, x, y, c):
    img.load()[x % SIZE, y % SIZE] = c


def get(img, x, y):
    return img.load()[x % SIZE, y % SIZE]


def stamp(img, x, y, rows, pal):
    """Draws a small pixel map at (x, y), wrapping; '.' leaves the pixel alone."""
    for dy, row in enumerate(rows):
        for dx, ch in enumerate(row):
            if ch != ".":
                put(img, x + dx, y + dy, pal[ch])


def shadow_below(img, x, y, rows, amount=0.25):
    """Darkens the pixels just below/right of a stamped shape, as if it stood out of the surface."""
    h, w = len(rows), max(len(r) for r in rows)
    filled = {(x + dx, y + dy) for dy, r in enumerate(rows) for dx, ch in enumerate(r) if ch != "."}
    for (fx, fy) in filled:
        for (sx, sy) in ((fx + 1, fy + 1), (fx, fy + 1)):
            if (sx, sy) not in filled:
                put(img, sx, sy, shade(get(img, sx, sy), -amount))


# ------------------------------------------------------------------ materials

def rock(seed, palette, shares=None, octaves=((2, 0.45), (4, 0.35), (8, 0.2)), stretch_x=1.0):
    """Smooth mottled rock, like vanilla stone: tileable noise quantized to the palette."""
    return quantize(fbm(seed, octaves, stretch_x), palette, shares)


def chunks(seed, n, palette, base_index=None, light=1, dark=1, jitter_tone=True):
    """A surface of raised lumps (like raw iron blocks, cobble, moss): Voronoi cells, each its own
    tone, lit on the top-left edge and shadowed on the bottom-right."""
    rnd = random.Random(seed)
    ids, border = voronoi(seed, n)
    tones = {}
    k = len(palette)
    base = base_index if base_index is not None else k // 2
    for i in range(n):
        tones[i] = max(dark, min(k - 1 - light, base + (rnd.choice((-1, 0, 0, 1)) if jitter_tone else 0)))
    lit = cell_light(ids)
    img = new()
    px = img.load()
    for y in range(SIZE):
        for x in range(SIZE):
            t = tones[ids[y][x]] + lit[y][x] * (light if lit[y][x] > 0 else dark)
            px[x, y] = palette[max(0, min(k - 1, t))]
    return img


def speckle(img, seed, colours, count, avoid=None):
    """A few single-pixel specks (grit, glints), never two touching."""
    rnd = random.Random(seed)
    placed = set()
    tries = 0
    while len(placed) < count and tries < count * 40:
        tries += 1
        x, y = rnd.randrange(SIZE), rnd.randrange(SIZE)
        if any((x + dx) % SIZE == px and (y + dy) % SIZE == py for (px, py) in placed for dx in (-1, 0, 1) for dy in (-1, 0, 1)):
            continue
        if avoid and avoid(x, y):
            continue
        placed.add((x, y))
        put(img, x, y, rnd.choice(colours))
    return img


def pits(img, seed, count, depth=0.35):
    """Small round pits like vanilla End Stone's: a dark pixel or two with a lit lower-right rim."""
    rnd = random.Random(seed)
    shapes = [["d"], ["dd"], ["d", "d"], ["dd", "d."]]
    for _ in range(count):
        x, y = rnd.randrange(SIZE), rnd.randrange(SIZE)
        s = rnd.choice(shapes)
        cells = [(x + dx, y + dy) for dy, r in enumerate(s) for dx, ch in enumerate(r) if ch == "d"]
        for (cx, cy) in cells:
            put(img, cx, cy, shade(get(img, cx, cy), -depth))
        for (cx, cy) in cells:
            for (rx, ry) in ((cx + 1, cy), (cx, cy + 1)):
                if (rx, ry) not in cells:
                    put(img, rx, ry, shade(get(img, rx, ry), depth * 0.6))
    return img


def crack_lines(img, seed, colour, n=3, length=(3, 7), highlight=None):
    """Thin wandering cracks, optionally with a lit edge beside them."""
    rnd = random.Random(seed)
    for _ in range(n):
        x, y = rnd.randrange(SIZE), rnd.randrange(SIZE)
        dx, dy = rnd.choice(((1, 0), (0, 1), (1, 1), (1, -1)))
        for _ in range(rnd.randint(*length)):
            put(img, x, y, colour)
            if highlight:
                put(img, x + dy, y - dx, highlight) if rnd.random() < 0.5 else None
            if rnd.random() < 0.4:
                x += rnd.choice((-1, 0, 1)) if dx == 0 else 0
                y += rnd.choice((-1, 0, 1)) if dy == 0 else 0
            x += dx
            y += dy
    return img


# Ore nuggets, as in vanilla ores: small shaded clusters (h highlight, l light, m mid, d dark).
NUGGETS = [
    ["hl", "md"],
    [".l.", "lmm", ".md"],
    ["hlm", "mmd"],
    ["lm", "mm", ".d"],
    ["h", "d"],
    ["lm.", ".md"],
]
# Hand-picked, well spread positions for up to 7 nuggets (they wrap, so edges are fine).
ORE_LAYOUT = [(2, 2, 1), (9, 1, 0), (12, 6, 2), (4, 7, 3), (8, 10, 5), (1, 12, 0), (12, 13, 4)]


def ore(stone, pal, count=6, shadow=0.3, layout=ORE_LAYOUT):
    """Sets ore nuggets into a stone texture. `pal` maps h/l/m/d to colours."""
    img = stone.copy()
    for (x, y, shape) in layout[:count]:
        rows = NUGGETS[shape]
        shadow_below(img, x, y, rows, shadow)
        stamp(img, x, y, rows, pal)
    return img


def leaves(seed, pal, holes=0.1, count=34):
    """Leafy foliage like vanilla leaves: overlapping small leaves, each lit on its top-left, over a
    dark underlayer, with a few see-through gaps. `pal` has d (deep), s (shadow), m (mid), l (light),
    h (highlight)."""
    rnd = random.Random(seed)
    img = new(pal["d"])
    shapes = [
        [".lm", "lms", ".s."],
        ["lm", "ms"],
        ["hl.", "lms", ".ss"],
        [".l", "lm", "ms"],
        ["lmm", ".ss"],
    ]
    for _ in range(count):
        x, y = rnd.randrange(SIZE), rnd.randrange(SIZE)
        s = rnd.choice(shapes)
        stamp(img, x, y, s, pal)
    # Gaps: pixels still in the deep underlayer, chosen away from each other.
    deep = [(x, y) for y in range(SIZE) for x in range(SIZE) if get(img, x, y) == pal["d"]]
    rnd.shuffle(deep)
    target = int(SIZE * SIZE * holes)
    done = set()
    for (x, y) in deep:
        if len(done) >= target:
            break
        if any(abs(x - a) <= 1 and abs(y - b) <= 1 for (a, b) in done):
            continue
        done.add((x, y))
        put(img, x, y, CLEAR)
    return img


def tile3(img, scale=4):
    """A 3x3 tiling of a texture, scaled up: for checking seams and repetition."""
    out = Image.new("RGBA", (SIZE * 3 * scale, SIZE * 3 * scale), (40, 40, 48, 255))
    big = img.resize((SIZE * scale, SIZE * scale), Image.NEAREST)
    for i in range(3):
        for j in range(3):
            out.paste(big, (i * SIZE * scale, j * SIZE * scale), big)
    return out
