#!/usr/bin/env python3
"""The sky's textures for Phase 5, written to assets/astralend/textures/environment/:

  remnant.png         the Remnant: the dim, dying ember of the collapsed star (a disc with a corona)
  remnant_flare.png   the glow that swells round it during a Sunstorm
  star.png            one soft star (tinted per star by the sky renderer)
  aurora.png          a curtain of aurora: bright lower hem, streaks fading upward
  flare_sheet.png     a sheet of flare light that rolls across the Corona's sky
  comet.png           a far-off comet: a bright head and a long fading tail
  silhouette_whale.png, silhouette_ring.png   huge shapes passing across the far sky

Pixelated like vanilla's sun and moon (which are 32x32 pixel art), not smooth gradients.
Run:  python3 tools/textures/sky_textures.py
"""
import math
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "src/main/resources/assets/astralend/textures/environment"


def hexc(h, a=255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def lerp(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(len(a)))


def remnant():
    """32x32: a deep red ember disc with darker, cooler patches and a hot rim, inside a soft corona."""
    size, r_disc = 32, 9.5
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    rnd = random.Random(7)
    spots = [(rnd.uniform(10, 22), rnd.uniform(10, 22), rnd.uniform(1.5, 3.2)) for _ in range(6)]
    ember = [hexc("4a0c08"), hexc("7a1a0c"), hexc("a8300f"), hexc("d65a18"), hexc("f59a3a"), hexc("ffd88a")]
    for y in range(size):
        for x in range(size):
            dx, dy = x + 0.5 - size / 2, y + 0.5 - size / 2
            d = math.hypot(dx, dy)
            if d <= r_disc:
                t = d / r_disc
                i = 3 if t > 0.85 else 2
                if t > 0.93:
                    i = 4                                         # the hot limb
                for (sx, sy, sr) in spots:
                    if math.hypot(x + 0.5 - sx, y + 0.5 - sy) < sr:
                        i -= 1                                    # cooler, darker patches
                if (x * 7 + y * 13) % 11 == 0 and t < 0.8:
                    i += 1                                        # a few bright granules
                img.putpixel((x, y), ember[max(0, min(5, i))])
            else:
                glow = max(0.0, 1.0 - (d - r_disc) / 6.0)
                if glow > 0:
                    a = int(150 * glow ** 1.6)
                    a = (a // 30) * 30                            # stepped, like vanilla's sunrise
                    if a > 0:
                        img.putpixel((x, y), hexc("e0501c", a))
    return img


def remnant_flare():
    """64x64: the swollen glow round the Remnant during a flare - a soft radial glow with faint rays.
    Glows are smooth (like vanilla's sunrise colours), not stepped: drawn huge, steps would show."""
    size = 64
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    for y in range(size):
        for x in range(size):
            dx, dy = x + 0.5 - size / 2, y + 0.5 - size / 2
            d = math.hypot(dx, dy) / (size / 2)
            if d >= 1.0:
                continue
            ray = 0.5 + 0.5 * math.cos(math.atan2(dy, dx) * 6)
            a = (1.0 - d) ** 2.2 * (0.8 + 0.2 * ray)
            colour = lerp(hexc("ff7a24"), hexc("fff2c4"), max(0.0, 1.0 - d * 1.6))
            img.putpixel((x, y), colour[:3] + (int(a * 255),))
    return img


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


def aurora():
    """32x128, white (tinted in the sky): a bright hem at the bottom and soft vertical streaks of
    different heights fading upward, like a curtain of light."""
    w, h = 32, 128
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    rnd = random.Random(3)
    heights = [rnd.uniform(0.4, 1.0) for _ in range(w)]
    # neighbouring columns blend, so streaks have soft edges
    heights = [(heights[i - 1] + 2 * heights[i] + heights[(i + 1) % w]) / 4 for i in range(w)]
    for x in range(w):
        for y in range(h):
            t = 1.0 - y / (h - 1)                                 # 0 at the bottom, 1 at the top
            if t > heights[x]:
                continue
            a = (1.0 - t / heights[x]) ** 1.6
            hem = max(0.0, 1.0 - t / 0.06)                        # the bright lower hem
            a = min(1.0, a * (0.75 + 0.25 * math.sin(x * 0.9)) + hem * 0.6)
            img.putpixel((x, y), (255, 255, 255, int(a * 220)))
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
    "remnant.png": remnant, "remnant_flare.png": remnant_flare, "star.png": star, "aurora.png": aurora,
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
