"""Spawn eggs that look like their mobs, the way vanilla's do since 1.21.5.

Each egg is the same 16x16 egg shape painted in the mob's own colours with its most telling
feature on it (the Watcher's eye, the Wretch's round mouth, the Pollinator's tentacles), and
the parts that would stick out of an egg drawn past its edge (fins, frills, crystals, spines).
The edge of everything drawn is then darkened to an outline, and the egg is lit from the top
left, as vanilla's are.

    python3 tools/mobs/spawn_eggs.py      # writes textures/item/<mob>_spawn_egg.png
"""
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "src/main/resources/assets/astralend/textures/item"

# The egg: the inclusive x range of each row, y 1 to 15.
ROWS = {1: (6, 9), 2: (5, 10), 3: (5, 10), 4: (4, 11), 5: (4, 11), 6: (3, 12), 7: (3, 12), 8: (3, 12),
        9: (3, 12), 10: (3, 12), 11: (3, 12), 12: (3, 12), 13: (4, 11), 14: (5, 10), 15: (6, 9)}
EGG = {(x, y) for y, (a, b) in ROWS.items() for x in range(a, b + 1)}


def hexc(s):
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4))


def mix(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


class Egg:
    def __init__(self, base):
        self.px = {p: hexc(base) for p in EGG}
        self.lit = set()      # pixels that glow: kept bright, not shaded

    def put(self, points, colour, glow=False):
        c = hexc(colour)
        for p in points:
            self.px[p] = c
            if glow:
                self.lit.add(p)
        return self

    def cut(self, points):
        for p in points:
            self.px.pop(p, None)
        return self

    def rows(self, y0, y1, colour):
        return self.put([(x, y) for (x, y) in EGG if y0 <= y <= y1], colour)

    def image(self):
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        drawn = set(self.px)
        for (x, y), c in self.px.items():
            if (x, y) not in self.lit:
                # Lit from the top left: lighter up there, darker down to the bottom right.
                t = ((x - 7.5) + (y - 8.0)) / 16.0
                c = mix(c, (255, 255, 255), -t * 0.35) if t < 0 else mix(c, (0, 0, 0), t * 0.35)
            edge = any((x + dx, y + dy) not in drawn for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            if edge and (x, y) not in self.lit:
                # The egg's own edge is a dark outline; what sticks out of it only shades a little,
                # or a part one pixel thick would be all outline.
                c = mix(c, (0, 0, 0), 0.55 if (x, y) in EGG else 0.2)
            img.putpixel((x, y), c + (255,))
        return img


def grid(rows, x0=0, y0=0):
    """The points of a little ASCII drawing, by character."""
    out = {}
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                out.setdefault(ch, []).append((x0 + x, y0 + y))
    return out


def hushback():
    e = Egg("2c2648")
    e.put([(x, 3) for x in range(5, 11)], "b89a5a")                       # the pale stripe down its back
    g = grid(["..bb..bb..",
              ".bbbbbbbb.",
              ".b.bb.bb..",
              "..b..b..b."], 3, 7)
    e.put(g["b"], "5a3f30")                                              # bones through its clear hide
    e.put([(5, 5), (10, 5)], "d8d0e8")                                   # eyes
    return e


def hollowback():
    e = Egg("4a4558")
    e.put([(5, 4), (10, 4)], "a8a0b0")                                   # dull eyes
    g = grid(["tttttttt",
              "mmmmmmmm",
              "mmMMMMmm",
              "mmMMMMmm",
              "mmmmmmmm",
              "tttttttt"], 4, 6)
    e.put(g["m"], "3a1050").put(g["M"], "8a3ad0", glow=True)             # the throat
    e.put([p for p in g["t"] if (p[0] + p[1]) % 2 == 0], "e8dcc0")      # fangs
    e.put([p for p in g["t"] if (p[0] + p[1]) % 2], "3a1050")
    e.put([(3, 6), (12, 6), (3, 11), (12, 11)], "4a4558")
    return e


def drift_whale():
    e = Egg("3a5ec8")
    e.rows(11, 15, "b8c8ec")                                              # pale belly
    e.put([(3, 10), (4, 10), (11, 10), (12, 10)], "b8c8ec")
    e.put([(1, 9), (2, 9), (2, 10), (13, 9), (14, 9), (13, 10)], "24408e")  # fins
    e.put([(6, 3), (9, 4), (5, 6), (10, 7), (7, 8), (4, 8)], "c8f4ff", glow=True)  # glowing spots
    e.put([(4, 11)], "1a2a5a")                                            # eye
    return e


def hydragrip():
    e = Egg("201c52")
    e.put([(2, 2), (1, 3), (2, 3), (1, 4), (2, 4), (3, 4), (2, 5), (3, 5),
           (13, 2), (13, 3), (14, 3), (12, 4), (13, 4), (14, 4), (12, 5), (13, 5)], "f0809a")  # frills
    for y in (5, 7, 9, 11, 13):
        e.put([(5, y), (10, y)], "5ef0f0", glow=True)                      # its cyan spots
    e.put([(6, 3), (9, 3)], "e8e8f8")                                    # eyes
    return e


def mirror_wretch():
    """Grey and segmented like a silverfish's egg, with the Wretch's round mouth of teeth up top,
    little dark legs down its sides and spines along its back."""
    e = Egg("74707e")
    for y in (7, 10, 13):
        e.put([(x, y) for x in range(3, 13) if (x, y) in EGG], "56525f")       # segments
        e.put([(2, y), (13, y)], "2a2630")                                     # legs
    g = grid([".rrrr.",
              "rTkkTr",
              "rkkkkr",
              ".rTTr."], 5, 2)
    e.put(g["r"], "9a5468").put(g["k"], "140a10").put(g["T"], "ece0cc")      # its mouth
    e.put([(6, 0), (9, 0)], "2a2630")                                          # spines
    return e


def pollinator():
    """A jellyfish: a teal bell with a rim of white lights, and its tentacles hanging free below."""
    e = Egg("74cfc6")
    e.rows(1, 3, "a8ece4")
    e.rows(7, 7, "56aaa2")
    e.put([(x, 8) for x in range(2, 14)], "8adcd2")                             # the bell's rim, wider
    e.put([(x, 8) for x in range(3, 13, 2)], "ffffff", glow=True)               # its lights
    e.cut([(x, y) for x in range(16) for y in range(9, 16)])
    for x, end, col in ((4, 13, "c8a4f4"), (6, 15, "a07ad8"), (9, 15, "c8a4f4"), (11, 13, "a07ad8")):
        e.put([(x, y) for y in range(9, end)], col)                             # tentacles
        e.put([(x, end)], "ffffff")                                             # white tips
    return e


def shardback():
    e = Egg("3a2e4c")
    g = grid(["c.....c",
              "cc.C.cc",
              ".cCCCc.",
              ".cCCCc.",
              "..ccc.."], 4, 0)
    e.put(g["c"], "d8bfee").put(g["C"], "fbf4ff")                         # crystals on its back
    e.put([(5, 10), (6, 10), (9, 10), (10, 10), (5, 12), (10, 12)], "c8a8f8", glow=True)  # eyes
    return e


def umbral_stalker():
    e = Egg("16111f")
    e.put([(3, 0), (4, 1), (4, 2), (5, 2), (12, 0), (11, 1), (11, 2), (10, 2)], "e8b8f0")  # horns
    e.put([(5, 7), (6, 8), (10, 7), (9, 8)], "f4f0ff", glow=True)       # narrowed eyes
    e.put([(6, 11), (6, 12), (9, 11), (9, 12)], "e8e0d8")                # fangs
    e.put([(2, 12), (2, 13), (13, 12), (13, 13)], "2a2236")              # claws
    return e


def watcher():
    e = Egg("9c9cac")
    e.put([(6, 0), (9, 0)], "8a8a98")                                    # the pillars on its head
    e.put([(x, 3) for x in range(4, 12)], "c8a24c")                      # gold bands
    e.put([(x, 12) for x in range(4, 12)], "c8a24c")
    g = grid([".www.",
              "wcccw",
              "wckcw",
              "wcccw",
              ".www."], 5, 6)
    e.put(g["w"], "f0f0f4").put(g["c"], "5ee8f0", glow=True).put(g["k"], "101018")  # its eye
    e.put([(3, 7), (3, 9), (12, 8), (12, 10)], "5ee8f0", glow=True)       # glyphs
    return e


def curator():
    e = Egg("343244")
    e.put([(1, 6), (2, 6), (2, 7), (13, 6), (14, 6), (13, 7)], "4a4658")  # shoulders
    e.put([(2, 5), (13, 5)], "b07aa8")
    e.put([(6, 3), (7, 3), (8, 3), (9, 3)], "b070ff", glow=True)          # visor
    g = grid(["pppp",
              "pPPp",
              "pPPp",
              "pppp"], 6, 7)
    e.put(g["p"], "6a2cc8", glow=True).put(g["P"], "e0c0ff", glow=True)   # its core, in its glass case
    e.put([(x, 12) for x in range(4, 12)], "8a6a98")
    return e


def spire_wyrm():
    e = Egg("1c1428")
    e.put([(7, 0), (8, 0), (7, 1)], "c49cff")
    e.put([(7, y) for y in range(3, 14, 2)] + [(8, y) for y in range(2, 14, 2)], "c49cff", glow=True)  # crystal ridge
    e.put([(5, 5), (10, 5)], "f0d8ff", glow=True)
    return e


EGGS = {"hushback": hushback, "hollowback": hollowback, "drift_whale": drift_whale, "hydragrip": hydragrip,
        "mirror_wretch": mirror_wretch, "pollinator": pollinator, "shardback": shardback,
        "umbral_stalker": umbral_stalker, "watcher": watcher, "curator": curator, "spire_wyrm": spire_wyrm}


def paint(name):
    return EGGS[name]().image() if name in EGGS else None


def main():
    for name, make in EGGS.items():
        make().image().save(OUT / f"{name}_spawn_egg.png")
    print(f"Wrote {len(EGGS)} spawn eggs")


if __name__ == "__main__":
    main()
