#!/usr/bin/env python3
"""The sky's textures for Phase 5, written to assets/astralend/textures/environment/:

  star.png            one soft star (tinted per star by the sky renderer)
  flare_sheet.png     a sheet of flare light that rolls across the Eclipse's sky in a Sunstorm
  comet.png           a far-off comet: a bright head and a long fading tail
  silhouette_whale.png, silhouette_ring.png   huge shapes passing across the far sky

Pixelated like vanilla's sun and moon (which are 32x32 pixel art), not smooth gradients.
Run:  python3 tools/textures/sky_textures.py
"""
import math
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "src/main/resources/assets/astralend/textures/environment"


def hexc(h, a=255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def lerp(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(len(a)))


def star():
    """8x8: a white star with a bright core and four short points."""
    rows = [
        "........",
        "...a....",
        "...b....",
        ".abwba..",
        "...b....",
        "...a....",
        "........",
        "........",
    ]
    pal = {"w": (255, 255, 255, 255), "b": (255, 255, 255, 170), "a": (255, 255, 255, 70)}
    img = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                img.putpixel((x, y), pal[ch])
    return img


def flare_sheet():
    """32x64: a broad, soft band of light with a brighter leading edge (tinted gold in the sky)."""
    w, h = 32, 64
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    for y in range(h):
        t = y / (h - 1)
        a = math.sin(t * math.pi) ** 1.2
        a = min(1.0, a + 0.35 * math.exp(-((t - 0.66) / 0.04) ** 2))
        for x in range(w):
            aa = a * (0.85 + 0.15 * math.sin(x * 0.6 + y * 0.05))
            img.putpixel((x, y), (255, 255, 255, int(aa * 220)))
    return img


def comet():
    """64x8: a white head on the left and a tail that thins and fades to the right."""
    w, h = 64, 8
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    for x in range(w):
        t = x / (w - 1)
        half = max(0.5, 3.0 * (1 - t))
        for y in range(h):
            d = abs(y + 0.5 - h / 2)
            if d > half:
                continue
            a = (1 - t) ** 1.5 * (1 - d / (half + 0.5))
            if x < 3:
                a = 1.0
            a = int(a * 255) // 32 * 32
            if a > 0:
                img.putpixel((x, y), (255, 255, 255, a))
    return img


WHALE = [
    "................................................................",
    "................................................................",
    "........................................XX......................",
    ".......................................XXXX.....................",
    "...................XXXXXXXXXXXXXXXXXXXXXXXXXX...................",
    ".............XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX..............",
    ".........XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX..........",
    ".......XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX......XX",
    "......XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX.XXXX",
    ".....XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX.",
    ".....XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX..",
    "......XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX.",
    ".......XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX..XXXX",
    ".........XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX.......XXX",
    "...........XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX.............",
    "..............XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX...................",
    "..................XXXXXXXXX..XXXXXXXXX..........................",
    "....................XXXXX.....XXXXXX............................",
    ".....................XXX.......XXX..............................",
    "................................................................",
]


WHALE = [row[:64].ljust(64, ".") for row in WHALE]


def silhouette_whale():
    """64x20: the shape of a vast whale far away, as a dark, faintly lit cut-out."""
    img = Image.new("RGBA", (64, len(WHALE)), (0, 0, 0, 0))
    for y, row in enumerate(WHALE):
        assert len(row) == 64, (y, len(row))
        for x, ch in enumerate(row):
            if ch == "X":
                edge = any(WHALE[yy][xx] != "X" for yy, xx in ((y - 1, x), (y, x - 1)) if 0 <= yy and 0 <= xx)
                img.putpixel((x, y), hexc("3a2f63", 255) if edge else hexc("0b0918", 255))
    return img


def silhouette_ring():
    """32x32: a vast broken ring, like the great ring of the Observatory, seen edge-on at an angle."""
    size = 32
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    for y in range(size):
        for x in range(size):
            dx, dy = (x + 0.5 - 16) / 15.0, (y + 0.5 - 16) / 6.0
            d = math.hypot(dx, dy)
            gap = 0.35 < math.atan2(dy, dx) < 0.75
            if 0.82 < d < 1.0 and not gap:
                img.putpixel((x, y), hexc("3a2f63") if dy < 0 else hexc("0b0918"))
    return img


TEXTURES = {
    "star.png": star,
    "flare_sheet.png": flare_sheet, "comet.png": comet, "silhouette_whale.png": silhouette_whale,
    "silhouette_ring.png": silhouette_ring,
}


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for name, make in TEXTURES.items():
        make().save(OUT / name)
    print(f"Wrote {len(TEXTURES)} sky textures to {OUT.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
