#!/usr/bin/env python3
"""The Eclipse's sky textures (after beta.4):

    python3 tools/textures/eclipse_textures.py

- environment/seam_star.png: the star over the Seam - white (tinted when drawn), a small hot core in a
  soft glow, with the six long diffraction spikes of a star seen through a big telescope's mirror, and
  two shorter, fainter ones across.
"""
import math
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
ENV = ROOT / "src/main/resources/assets/astralend/textures/environment"


def seam_star(size=64):
    img = Image.new("RGBA", (size, size), (255, 255, 255, 0))
    c = (size - 1) / 2.0
    for y in range(size):
        for x in range(size):
            dx, dy = (x - c) / c, (y - c) / c
            r = math.hypot(dx, dy)
            a = math.atan2(dy, dx)
            core = math.exp(-(r / 0.07) ** 2)
            glow = 0.45 * math.exp(-(r / 0.22) ** 2)
            spikes = 0.0
            for k in range(6):                       # six long spikes, 60 degrees apart, upright
                t = math.pi / 2 + k * math.pi / 3
                off = abs(math.sin(a - t)) * r       # distance from the spike's line
                along = math.cos(a - t)
                if along > 0:
                    spikes += math.exp(-(off / 0.018) ** 2) * max(0.0, 1.0 - r) ** 1.6
            for t in (0.0, math.pi):                 # two short, faint ones across
                off = abs(math.sin(a - t)) * r
                if math.cos(a - t) > 0:
                    spikes += 0.45 * math.exp(-(off / 0.014) ** 2) * max(0.0, 1.0 - r / 0.55) ** 2
            v = min(1.0, core + glow + spikes)
            img.putpixel((x, y), (255, 255, 255, int(v * 255)))
    return img


def main():
    seam_star().save(ENV / "seam_star.png")
    print("Drew seam_star.png")


if __name__ == "__main__":
    main()
